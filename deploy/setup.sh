#!/usr/bin/env bash
# =============================================================================
#  CRM - provisionamento completo de uma EC2 Ubuntu (22.04 ou 24.04)
#
#  Instala e configura, sem intervencao manual:
#    - PostgreSQL (somente em localhost: a porta 5432 nunca fica exposta)
#    - Java 17 + Maven -> build da API Spring Boot, rodando como servico systemd
#    - Node 22 -> build de producao do Angular, servido pelo Nginx
#    - Nginx na porta 80: entrega o front (com fallback das rotas SPA) e
#      repassa /api para a API em 127.0.0.1:8080
#
#  Todas as credenciais (senha do banco, chave do JWT, senha do admin) sao
#  geradas aqui e gravadas apenas em /etc/crm/crm.env (chmod 600). Nada sensivel
#  fica no codigo nem no Git.
#
#  Uso (na EC2, como ubuntu):
#     curl -fsSL https://raw.githubusercontent.com/camilaarodriguez/CRM-BackEnd/main/deploy/setup.sh -o setup.sh
#     sudo bash setup.sh
#
#  Variaveis opcionais (ex.: sudo BRANCH=develop ADMIN_EMAIL=eu@x.com bash setup.sh):
#     BRANCH           branch dos dois repositorios (padrao: main)
#     FRONTEND_BRANCH  branch so do front (padrao: igual a BRANCH)
#     BACKEND_REPO     URL do repositorio da API
#     FRONTEND_REPO    URL do repositorio do front
#     ADMIN_EMAIL      e-mail do administrador inicial (padrao: admin@crm.local)
#     ADMIN_PASSWORD   senha do administrador inicial (padrao: gerada)
#     DOMINIO          dominio apontado para a instancia, se houver
#
#  Pode ser executado de novo a qualquer momento: atualiza o codigo, refaz os
#  builds e reinicia os servicos, mantendo o banco e as credenciais ja geradas.
# =============================================================================
set -Eeuo pipefail

BACKEND_REPO="${BACKEND_REPO:-https://github.com/camilaarodriguez/CRM-BackEnd.git}"
FRONTEND_REPO="${FRONTEND_REPO:-https://github.com/camilaarodriguez/CRM-FrontEnd.git}"
BRANCH="${BRANCH:-main}"
FRONTEND_BRANCH="${FRONTEND_BRANCH:-$BRANCH}"
DOMINIO="${DOMINIO:-}"

APP_USER="crm"
APP_DIR="/opt/crm"
SRC_DIR="$APP_DIR/src"
WEB_DIR="/var/www/crm"
ENV_DIR="/etc/crm"
ENV_FILE="$ENV_DIR/crm.env"
LOG_DIR="/var/log/crm-deploy"
DB_NAME="crmdb"
DB_USER="crm"
NODE_MAJOR="22"

INICIO=$(date +%s)
ETAPA=0
TOTAL_ETAPAS=10

etapa() {
    ETAPA=$((ETAPA + 1))
    printf '\n\033[1;34m[%s/%s] %s\033[0m\n' "$ETAPA" "$TOTAL_ETAPAS" "$1"
}
info() { printf '      %s\n' "$1"; }
falha() { printf '\n\033[1;31mERRO: %s\033[0m\n' "$1" >&2; exit 1; }
# Roda um build mandando toda a saida para um arquivo de log; so mostra o final
# dele se der erro, para a tela da demonstracao ficar limpa.
build_com_log() {
    local nome="$1"; shift
    local log="$LOG_DIR/$nome.log"
    if ! "$@" >"$log" 2>&1; then
        tail -n 40 "$log" >&2
        falha "o build '$nome' falhou. Log completo em $log"
    fi
}
trap 'falha "o script parou na linha $LINENO. Veja a mensagem acima."' ERR

# -----------------------------------------------------------------------------
if [[ "$EUID" -ne 0 ]]; then
    exec sudo --preserve-env=BRANCH,FRONTEND_BRANCH,BACKEND_REPO,FRONTEND_REPO,ADMIN_EMAIL,ADMIN_PASSWORD,DOMINIO bash "$0" "$@"
fi
command -v apt-get >/dev/null || falha "este script e para Ubuntu/Debian (apt-get nao encontrado)"
export DEBIAN_FRONTEND=noninteractive NG_CLI_ANALYTICS=false CI=true
# Evita o aviso "could not change directory" quando comandos rodam como o usuario postgres.
cd /
mkdir -p "$LOG_DIR"

# -----------------------------------------------------------------------------
etapa "Preparando memoria (swap)"
# Uma t2/t3.micro tem 1 GB de RAM: sem swap, o build do Maven ou do Angular e morto por falta de memoria.
if [[ "$(swapon --show --noheadings | wc -l)" -eq 0 ]]; then
    fallocate -l 2G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile >/dev/null
    swapon /swapfile
    grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
    info "swap de 2 GB criado"
else
    info "swap ja existe, nada a fazer"
fi

# -----------------------------------------------------------------------------
etapa "Instalando pacotes do sistema (Java 17, Maven, PostgreSQL, Nginx, Git)"
apt-get update -qq
apt-get install -y -qq openjdk-17-jdk-headless maven postgresql nginx git curl ca-certificates openssl xz-utils >/dev/null
# O pacote do Maven pode trazer outro JDK como dependencia: fixa o Java 17 como padrao.
JAVA17=$(find /usr/lib/jvm -maxdepth 3 -path '*java-17-openjdk*/bin/java' | head -n 1)
[[ -n "$JAVA17" ]] || falha "Java 17 nao encontrado apos a instalacao"
update-alternatives --set java "$JAVA17" >/dev/null
export JAVA_HOME="${JAVA17%/bin/java}"
info "$(java -version 2>&1 | head -n 1)"
info "$(mvn -v 2>/dev/null | head -n 1)"

# -----------------------------------------------------------------------------
etapa "Instalando Node.js $NODE_MAJOR"
if command -v node >/dev/null && [[ "$(node -v)" == v${NODE_MAJOR}.* ]]; then
    info "Node $(node -v) ja instalado"
else
    case "$(uname -m)" in
        x86_64) ARQ="x64" ;;
        aarch64) ARQ="arm64" ;;
        *) falha "arquitetura $(uname -m) nao suportada" ;;
    esac
    BASE_NODE="https://nodejs.org/dist/latest-v${NODE_MAJOR}.x"
    ARQUIVO_NODE=$(curl -fsSL "$BASE_NODE/SHASUMS256.txt" | awk '{print $2}' | grep -E "^node-v[0-9.]+-linux-${ARQ}\.tar\.xz$")
    TMP_NODE=$(mktemp -d)
    curl -fsSL "$BASE_NODE/$ARQUIVO_NODE" -o "$TMP_NODE/$ARQUIVO_NODE"
    # Confere o SHA-256 publicado pelo projeto Node antes de instalar.
    (cd "$TMP_NODE" && curl -fsSL "$BASE_NODE/SHASUMS256.txt" | grep " $ARQUIVO_NODE\$" | sha256sum -c - >/dev/null)
    rm -rf /opt/node && mkdir -p /opt/node
    tar -xJf "$TMP_NODE/$ARQUIVO_NODE" -C /opt/node --strip-components=1
    ln -sf /opt/node/bin/node /usr/local/bin/node
    ln -sf /opt/node/bin/npm /usr/local/bin/npm
    ln -sf /opt/node/bin/npx /usr/local/bin/npx
    rm -rf "$TMP_NODE"
    info "Node $(node -v) instalado em /opt/node"
fi

# -----------------------------------------------------------------------------
etapa "Gerando credenciais e variaveis de ambiente ($ENV_FILE)"
mkdir -p "$ENV_DIR"
chmod 700 "$ENV_DIR"
# Numa nova execucao reaproveita o que ja foi gerado: trocar a senha do banco ou a
# chave do JWT a cada deploy derrubaria a conexao e deslogaria todo mundo.
if [[ -f "$ENV_FILE" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
    info "arquivo existente reaproveitado"
fi
DB_PASSWORD="${DB_PASSWORD:-$(openssl rand -hex 24)}"
JWT_SECRET="${JWT_SECRET:-$(openssl rand -base64 48 | tr -d '\n')}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@crm.local}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-$(openssl rand -base64 12 | tr -d '\n/+=')}"

# IP publico pelo servico de metadados da EC2 (IMDSv2), com alternativa externa.
IMDS_TOKEN=$(curl -fsS -m 3 -X PUT "http://169.254.169.254/latest/api/token" \
    -H "X-aws-ec2-metadata-token-ttl-seconds: 60" 2>/dev/null || true)
IP_PUBLICO=$(curl -fsS -m 3 -H "X-aws-ec2-metadata-token: $IMDS_TOKEN" \
    "http://169.254.169.254/latest/meta-data/public-ipv4" 2>/dev/null || true)
[[ -n "$IP_PUBLICO" ]] || IP_PUBLICO=$(curl -fsS -m 5 https://checkip.amazonaws.com 2>/dev/null | tr -d '[:space:]' || true)
[[ -n "$IP_PUBLICO" ]] || falha "nao foi possivel descobrir o IP publico da instancia"

DNS_PUBLICO=$(curl -fsS -m 3 -H "X-aws-ec2-metadata-token: $IMDS_TOKEN" \
    "http://169.254.169.254/latest/meta-data/public-hostname" 2>/dev/null || true)

CORS_ORIGINS="http://$IP_PUBLICO"
[[ -n "$DNS_PUBLICO" ]] && CORS_ORIGINS="$CORS_ORIGINS,http://$DNS_PUBLICO"
[[ -n "$DOMINIO" ]] && CORS_ORIGINS="$CORS_ORIGINS,http://$DOMINIO,https://$DOMINIO"
info "origens liberadas no CORS: $CORS_ORIGINS"

umask 077
cat > "$ENV_FILE" <<EOF
# Gerado por deploy/setup.sh em $(date -u +%Y-%m-%dT%H:%M:%SZ). Nao commitar.
DB_URL=jdbc:postgresql://localhost:5432/$DB_NAME
DB_USERNAME=$DB_USER
DB_PASSWORD=$DB_PASSWORD
JWT_SECRET=$JWT_SECRET
JWT_EXPIRATION_MS=${JWT_EXPIRATION_MS:-7200000}
CORS_ORIGINS=$CORS_ORIGINS
ADMIN_NOME=${ADMIN_NOME:-Administrador}
ADMIN_EMAIL=$ADMIN_EMAIL
ADMIN_PASSWORD=$ADMIN_PASSWORD
TELEGRAM_BOT_TOKEN=${TELEGRAM_BOT_TOKEN:-}
TELEGRAM_CHAT_ID=${TELEGRAM_CHAT_ID:-}
SHOW_SQL=false
# A API so aceita conexoes da propria maquina: quem fala com ela e o Nginx.
SERVER_ADDRESS=127.0.0.1
EOF
umask 022
chmod 600 "$ENV_FILE"
info "credenciais gravadas somente em $ENV_FILE (chmod 600)"

# -----------------------------------------------------------------------------
etapa "Configurando o PostgreSQL (banco $DB_NAME, acesso apenas local)"
systemctl enable --now postgresql >/dev/null 2>&1
psql_admin() { sudo -u postgres psql -v ON_ERROR_STOP=1 -qtAX "$@"; }
if [[ "$(psql_admin -c "SELECT 1 FROM pg_roles WHERE rolname='$DB_USER'")" == "1" ]]; then
    psql_admin -c "ALTER ROLE $DB_USER WITH LOGIN PASSWORD '$DB_PASSWORD'" >/dev/null
    info "usuario $DB_USER ja existia: senha sincronizada com $ENV_FILE"
else
    psql_admin -c "CREATE ROLE $DB_USER WITH LOGIN PASSWORD '$DB_PASSWORD'" >/dev/null
    info "usuario $DB_USER criado"
fi
if [[ "$(psql_admin -c "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'")" != "1" ]]; then
    psql_admin -c "CREATE DATABASE $DB_NAME OWNER $DB_USER" >/dev/null
    info "banco $DB_NAME criado"
else
    info "banco $DB_NAME ja existe: dados preservados"
fi
ESCUTA=$(psql_admin -c "SHOW listen_addresses")
info "PostgreSQL escutando em: $ESCUTA (a porta 5432 nao deve ser liberada no Security Group)"

# -----------------------------------------------------------------------------
etapa "Baixando o codigo (API: $BRANCH, front: $FRONTEND_BRANCH)"
id "$APP_USER" >/dev/null 2>&1 || useradd --system --home-dir "$APP_DIR" --shell /usr/sbin/nologin "$APP_USER"
mkdir -p "$SRC_DIR"
atualizar_repo() {
    local url="$1" branch="$2" destino="$3"
    if [[ -d "$destino/.git" ]]; then
        git -C "$destino" fetch --quiet --depth 1 origin "$branch"
        git -C "$destino" checkout --quiet -B "$branch" FETCH_HEAD
        git -C "$destino" reset --quiet --hard FETCH_HEAD
    else
        git clone --quiet --depth 1 --branch "$branch" "$url" "$destino"
    fi
    info "$(basename "$destino"): $(git -C "$destino" log -1 --format='%h %s')"
}
atualizar_repo "$BACKEND_REPO" "$BRANCH" "$SRC_DIR/backend"
atualizar_repo "$FRONTEND_REPO" "$FRONTEND_BRANCH" "$SRC_DIR/frontend"

# -----------------------------------------------------------------------------
etapa "Build da API (Maven)"
info "baixando dependencias e compilando: leva de 2 a 5 minutos numa micro..."
build_com_log api env -C "$SRC_DIR/backend" MAVEN_OPTS="-Xmx512m" mvn -B -ntp -DskipTests package
JAR=$(find "$SRC_DIR/backend/target" -maxdepth 1 -name '*.jar' ! -name '*.original' | head -n 1)
[[ -n "$JAR" ]] || falha "o build da API nao gerou o .jar"
install -o "$APP_USER" -g "$APP_USER" -m 644 "$JAR" "$APP_DIR/sistema-crm.jar"
chown "$APP_USER:$APP_USER" "$APP_DIR"
info "jar publicado em $APP_DIR/sistema-crm.jar"

# -----------------------------------------------------------------------------
etapa "Build de producao do front (Angular)"
info "instalando pacotes do npm e gerando o build: leva de 2 a 4 minutos numa micro..."
build_com_log front-npm env -C "$SRC_DIR/frontend" npm ci --no-audit --no-fund
build_com_log front-build env -C "$SRC_DIR/frontend" NODE_OPTIONS="--max-old-space-size=1024" \
    npx --no-install ng build --configuration production
DIST="$SRC_DIR/frontend/dist/crm-frontend/browser"
[[ -f "$DIST/index.html" ]] || falha "o build do front nao gerou $DIST/index.html"
mkdir -p "$WEB_DIR"
find "$WEB_DIR" -mindepth 1 -delete
cp -r "$DIST"/. "$WEB_DIR"/
chown -R www-data:www-data "$WEB_DIR"
info "front publicado em $WEB_DIR"

# -----------------------------------------------------------------------------
etapa "Servico systemd da API (crm-backend)"
install -m 644 "$SRC_DIR/backend/deploy/crm-backend.service" /etc/systemd/system/crm-backend.service
systemctl daemon-reload
systemctl enable crm-backend >/dev/null 2>&1
systemctl restart crm-backend
info "aguardando a API responder (a primeira subida cria as tabelas pelo Flyway)..."
for _ in $(seq 1 90); do
    if curl -fsS -o /dev/null "http://127.0.0.1:8080/v3/api-docs" 2>/dev/null; then
        info "API no ar"
        break
    fi
    sleep 2
done
curl -fsS -o /dev/null "http://127.0.0.1:8080/v3/api-docs" \
    || falha "a API nao subiu. Veja o log com: journalctl -u crm-backend -n 100 --no-pager"

# -----------------------------------------------------------------------------
etapa "Nginx (front + proxy /api)"
install -m 644 "$SRC_DIR/backend/deploy/nginx-crm.conf" /etc/nginx/sites-available/crm
ln -sf /etc/nginx/sites-available/crm /etc/nginx/sites-enabled/crm
rm -f /etc/nginx/sites-enabled/default
nginx -t -q
systemctl enable nginx >/dev/null 2>&1
# restart (e nao reload): so retorna quando o Nginx ja esta com a configuracao nova,
# entao as conferencias abaixo nao pegam a pagina padrao antiga por engano.
systemctl restart nginx
curl -fsS -o /dev/null "http://127.0.0.1/" || falha "o Nginx nao respondeu na porta 80"
curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1/api/clientes" | grep -q '^401$' \
    || falha "o Nginx nao esta repassando /api para a API"
curl -fsS -o /dev/null "http://127.0.0.1/clientes" || falha "o fallback das rotas SPA nao funcionou"
info "Nginx servindo o front e repassando /api"

# -----------------------------------------------------------------------------
DURACAO=$(( $(date +%s) - INICIO ))
trap - ERR
cat <<EOF

=============================================================================
 CRM instalado em $((DURACAO / 60))min $((DURACAO % 60))s

  Aplicacao:   http://$IP_PUBLICO
  Swagger:     http://$IP_PUBLICO/swagger-ui.html

  Administrador inicial (criado apenas se o banco ainda nao tinha um):
    e-mail:  $ADMIN_EMAIL
    senha:   guardada em $ENV_FILE  ->  sudo grep ADMIN_PASSWORD $ENV_FILE

  Logs e status:
    sudo systemctl status crm-backend
    sudo journalctl -u crm-backend -f
    sudo tail -f /var/log/nginx/access.log /var/log/nginx/error.log

  Security Group: libere apenas 22 (seu IP) e 80. Nunca abra 5432 nem 8080.
=============================================================================
EOF
