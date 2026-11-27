#!/usr/bin/env bash
# =============================================================================
#  CRM - cria a infraestrutura na AWS a partir do AWS CloudShell
#
#  Faz, sem passos manuais no console:
#    1. Security Group "crm-sg": porta 80 aberta e porta 22 somente para o seu IP
#       (8080 e 5432 nunca sao liberadas)
#    2. Par de chaves "crm-chave" para o SSH (se ainda nao existir)
#    3. EC2 Ubuntu 24.04 com 16 GB de disco, que roda o deploy/setup.sh sozinha
#       na primeira inicializacao (user data)
#
#  Uso, no AWS CloudShell (ja vem autenticado, sem chaves de acesso no seu PC):
#     curl -fsSL https://raw.githubusercontent.com/camilaarodriguez/CRM-BackEnd/main/deploy/criar-ec2.sh -o criar-ec2.sh
#     MEU_IP=SEU.IP.AQUI bash criar-ec2.sh
#
#  O MEU_IP e o IP publico do SEU computador (abra https://checkip.amazonaws.com
#  no navegador). Nao da para descobrir pelo CloudShell, porque ele roda na AWS.
#
#  Variaveis opcionais:
#     TIPO     tipo da instancia (padrao: o primeiro do free tier entre t3.micro e t2.micro)
#     BRANCH   branch que o setup.sh vai instalar (padrao: main)
#     NOME     prefixo dos recursos (padrao: crm)
# =============================================================================
set -Eeuo pipefail

NOME="${NOME:-crm}"
BRANCH="${BRANCH:-main}"
REPO_RAW="https://raw.githubusercontent.com/camilaarodriguez/CRM-BackEnd"
SG_NOME="$NOME-sg"
CHAVE_NOME="$NOME-chave"
AMI_SSM="/aws/service/canonical/ubuntu/server/24.04/stable/current/amd64/hvm/ebs-gp3/ami-id"

info() { printf '\033[1;34m==>\033[0m %s\n' "$1"; }
falha() { printf '\033[1;31mERRO: %s\033[0m\n' "$1" >&2; exit 1; }

command -v aws >/dev/null || falha "AWS CLI nao encontrada. Rode este script no AWS CloudShell."
[[ -n "${MEU_IP:-}" ]] || falha "informe o IP do seu computador: MEU_IP=1.2.3.4 bash criar-ec2.sh (veja em https://checkip.amazonaws.com)"
[[ "$MEU_IP" =~ ^[0-9]{1,3}(\.[0-9]{1,3}){3}$ ]] || falha "MEU_IP invalido: '$MEU_IP'"

REGIAO="${AWS_REGION:-$(aws configure get region 2>/dev/null || true)}"
[[ -n "$REGIAO" ]] || falha "regiao nao definida. Ex.: AWS_REGION=us-east-1 bash criar-ec2.sh"
export AWS_REGION="$REGIAO" AWS_PAGER=""
info "regiao: $REGIAO | conta: $(aws sts get-caller-identity --query Account --output text)"

# -----------------------------------------------------------------------------
if [[ -z "${TIPO:-}" ]]; then
    ELEGIVEIS=$(aws ec2 describe-instance-types --filters Name=free-tier-eligible,Values=true \
        --query 'InstanceTypes[].InstanceType' --output text 2>/dev/null || true)
    for candidato in t3.micro t2.micro; do
        if [[ " $ELEGIVEIS " == *" $candidato "* ]]; then TIPO="$candidato"; break; fi
    done
    TIPO="${TIPO:-t3.micro}"
fi
info "tipo da instancia: $TIPO"

# -----------------------------------------------------------------------------
VPC_ID=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)
[[ "$VPC_ID" != "None" && -n "$VPC_ID" ]] || falha "a conta nao tem VPC padrao nesta regiao"

SG_ID=$(aws ec2 describe-security-groups \
    --filters Name=group-name,Values="$SG_NOME" Name=vpc-id,Values="$VPC_ID" \
    --query 'SecurityGroups[0].GroupId' --output text)
if [[ "$SG_ID" == "None" || -z "$SG_ID" ]]; then
    SG_ID=$(aws ec2 create-security-group --group-name "$SG_NOME" --vpc-id "$VPC_ID" \
        --description "CRM: HTTP publico e SSH apenas do IP do administrador" \
        --query GroupId --output text)
    info "Security Group criado: $SG_ID"
else
    info "Security Group ja existia: $SG_ID"
fi
liberar() {
    local porta="$1" origem="$2" descricao="$3"
    aws ec2 authorize-security-group-ingress --group-id "$SG_ID" --ip-permissions \
        "[{\"IpProtocol\":\"tcp\",\"FromPort\":$porta,\"ToPort\":$porta,\"IpRanges\":[{\"CidrIp\":\"$origem\",\"Description\":\"$descricao\"}]}]" \
        >/dev/null 2>&1 && info "porta $porta liberada para $origem" \
        || info "porta $porta ja estava liberada para $origem"
}
liberar 80 "0.0.0.0/0" "HTTP do CRM"
liberar 22 "$MEU_IP/32" "SSH do administrador"
info "portas 8080 (API) e 5432 (PostgreSQL) continuam fechadas"

# -----------------------------------------------------------------------------
if aws ec2 describe-key-pairs --key-names "$CHAVE_NOME" >/dev/null 2>&1; then
    info "par de chaves $CHAVE_NOME ja existia (use o .pem que voce ja baixou)"
else
    aws ec2 create-key-pair --key-name "$CHAVE_NOME" --key-type ed25519 \
        --query KeyMaterial --output text > "$HOME/$CHAVE_NOME.pem"
    chmod 400 "$HOME/$CHAVE_NOME.pem"
    info "chave criada em ~/$CHAVE_NOME.pem (baixe pelo menu Actions > Download file do CloudShell)"
fi

# -----------------------------------------------------------------------------
USER_DATA=$(mktemp)
cat > "$USER_DATA" <<EOF
#!/bin/bash
# Executado pela EC2 na primeira inicializacao (como root).
curl -fsSL "$REPO_RAW/$BRANCH/deploy/setup.sh" -o /root/setup.sh
BRANCH="$BRANCH" bash /root/setup.sh > /var/log/crm-setup.log 2>&1
EOF

INSTANCIA=$(aws ec2 run-instances \
    --image-id "resolve:ssm:$AMI_SSM" \
    --instance-type "$TIPO" \
    --key-name "$CHAVE_NOME" \
    --security-group-ids "$SG_ID" \
    --user-data "file://$USER_DATA" \
    --metadata-options HttpTokens=required,HttpEndpoint=enabled \
    --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":16,"VolumeType":"gp3"}}]' \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$NOME}]" \
    --query 'Instances[0].InstanceId' --output text)
rm -f "$USER_DATA"
info "instancia criada: $INSTANCIA (aguardando ficar em execucao...)"
aws ec2 wait instance-running --instance-ids "$INSTANCIA"
IP=$(aws ec2 describe-instances --instance-ids "$INSTANCIA" \
    --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)

info "instancia no ar em $IP. O setup.sh esta rodando la dentro (5 a 10 minutos)..."
for _ in $(seq 1 90); do
    if curl -fsS -m 5 -o /dev/null "http://$IP/" 2>/dev/null \
        && [[ "$(curl -s -m 5 -o /dev/null -w '%{http_code}' "http://$IP/api/clientes")" == "401" ]]; then
        PRONTO=1
        break
    fi
    sleep 10
done

cat <<EOF

=============================================================================
 Instancia: $INSTANCIA ($TIPO, $REGIAO)
 Aplicacao: http://$IP $([[ -n "${PRONTO:-}" ]] && echo "(no ar)" || echo "(ainda instalando: abra em alguns minutos)")

 Acompanhar a instalacao e ver os logs:
   ssh -i $CHAVE_NOME.pem ubuntu@$IP
   sudo tail -f /var/log/crm-setup.log
   sudo journalctl -u crm-backend -f

 Senha do administrador inicial (admin@crm.local):
   sudo grep ADMIN_PASSWORD /etc/crm/crm.env

 Para apagar tudo depois da apresentacao (evita cobranca):
   aws ec2 terminate-instances --instance-ids $INSTANCIA
=============================================================================
EOF
