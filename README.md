# CRM — API (Spring Boot)

API REST do CRM: gestão da equipe, carteira de clientes com funil de vendas, distribuição de
leads entre vendedores com histórico, central de conversas e notificações pelo Telegram.
O front-end em Angular fica em [CRM-FrontEnd](https://github.com/camilaarodriguez/CRM-FrontEnd).

| Recurso | Versão |
|---|---|
| Java | 17 |
| Spring Boot | 3.3 (Web, Data JPA, Validation, Security) |
| JWT | jjwt 0.12 (HMAC-SHA256) |
| Banco | PostgreSQL + Flyway |
| Documentação | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5, Mockito, MockMvc, H2 — cobertura mínima de 90% (JaCoCo) |

## Sumário

1. [Deploy na AWS com o script `.sh`](#deploy-na-aws-com-o-script-sh)
2. [Security Groups](#security-groups)
3. [Logs na EC2](#logs-na-ec2)
4. [Rodar localmente](#rodar-localmente)
5. [Variáveis de ambiente](#variáveis-de-ambiente)
6. [Segurança](#segurança)
7. [Arquitetura](#arquitetura)
8. [Endpoints](#endpoints)

---

## Deploy na AWS com o script `.sh`

O arquivo [`deploy/setup.sh`](deploy/setup.sh) transforma uma EC2 Ubuntu recém-criada na
aplicação completa, sem passos manuais:

1. cria um swap de 2 GB (a t2/t3.micro tem só 1 GB de RAM);
2. instala Java 17, Maven, Node 22, PostgreSQL, Nginx e Git;
3. gera a senha do banco, a chave do JWT e a senha do administrador em `/etc/crm/crm.env` (`chmod 600`);
4. cria o banco `crmdb` e o usuário `crm` no PostgreSQL, que escuta só em `localhost`;
5. clona os dois repositórios e faz o build da API (Maven) e do front (Angular, produção);
6. instala a API como serviço **systemd** (`crm-backend`), que roda em segundo plano e reinicia sozinho;
7. configura o **Nginx** na porta 80: entrega o build do Angular, com fallback das rotas SPA, e repassa `/api` para a API;
8. confere se tudo respondeu e mostra o endereço público no final.

### Passo a passo

1. **Crie o Security Group** como na [tabela abaixo](#security-groups).
2. **Crie a instância EC2**: Ubuntu Server 24.04 LTS, tipo `t2.micro` ou `t3.micro` (free tier),
   com o Security Group do passo 1 e um par de chaves para SSH.
3. **Conecte por SSH** e rode o script:

   ```bash
   ssh -i sua-chave.pem ubuntu@IP_PUBLICO_DA_EC2
   curl -fsSL https://raw.githubusercontent.com/camilaarodriguez/CRM-BackEnd/main/deploy/setup.sh -o setup.sh
   sudo bash setup.sh
   ```

4. Ao final, o script mostra o endereço. Abra `http://IP_PUBLICO_DA_EC2`, entre com o
   administrador inicial ou crie uma conta em **Criar conta**.

   A senha do administrador fica guardada na própria instância:

   ```bash
   sudo grep ADMIN_PASSWORD /etc/crm/crm.env
   ```

A primeira execução leva de 5 a 10 minutos numa micro, e o build é a parte mais demorada.

### Opções

Todas são opcionais e vão antes do comando:

```bash
sudo BRANCH=main ADMIN_EMAIL=admin@empresa.com bash setup.sh
```

| Variável | Padrão | Para que serve |
|---|---|---|
| `BRANCH` | `main` | branch dos dois repositórios |
| `FRONTEND_BRANCH` | igual a `BRANCH` | branch só do front |
| `ADMIN_EMAIL` | `admin@crm.local` | e-mail do administrador inicial |
| `ADMIN_PASSWORD` | gerada | senha do administrador inicial |
| `DOMINIO` | — | domínio apontado para a instância (entra no CORS) |
| `BACKEND_REPO` / `FRONTEND_REPO` | repositórios do projeto | para testar a partir de um fork |

### Atualizar a aplicação

Rode o mesmo script de novo. Ele baixa o código mais recente, refaz os builds e reinicia os
serviços, mantendo o banco e as credenciais já geradas.

### Alternativa: criar tudo pelo AWS CloudShell

O [`deploy/criar-ec2.sh`](deploy/criar-ec2.sh) cria o Security Group, o par de chaves e a EC2 de
uma vez, e a instância já roda o `setup.sh` sozinha ao ligar. Abra o **CloudShell** (ícone `>_`
no topo do console da AWS) e rode, trocando pelo IP do seu computador
(veja em <https://checkip.amazonaws.com>):

```bash
curl -fsSL https://raw.githubusercontent.com/camilaarodriguez/CRM-BackEnd/main/deploy/criar-ec2.sh -o criar-ec2.sh
MEU_IP=SEU.IP.AQUI bash criar-ec2.sh
```

No final, o script mostra o endereço da aplicação, o comando SSH e como apagar a instância
depois da apresentação. A chave SSH fica em `~/crm-chave.pem` no CloudShell: baixe pelo menu
**Actions → Download file**.

### Sem SSH: user data

O conteúdo de `setup.sh` também pode ser colado em **Advanced details → User data** ao criar a
instância. A EC2 roda o script sozinha na primeira inicialização, e o log fica em
`/var/log/cloud-init-output.log`.

## Security Groups

Libere **apenas** o necessário. A API (8080) e o PostgreSQL (5432) ficam acessíveis só de dentro
da instância: o Nginx conversa com a API por `127.0.0.1`.

| Tipo | Porta | Origem | Por quê |
|---|---|---|---|
| SSH | 22 | **Meu IP** | administração e consulta de logs |
| HTTP | 80 | `0.0.0.0/0` | acesso à aplicação |
| HTTPS | 443 | `0.0.0.0/0` | só se configurar um certificado (opcional) |
| — | 8080 | **não liberar** | a API fica atrás do Nginx |
| PostgreSQL | 5432 | **nunca liberar** | o banco aceita apenas conexões locais |

Pelo **AWS CloudShell**, que já vem autenticado e não exige chaves de acesso no seu computador:

```bash
SG_ID=$(aws ec2 create-security-group --group-name crm-sg \
  --description "CRM: SSH do meu IP e HTTP" --query GroupId --output text)
aws ec2 authorize-security-group-ingress --group-id "$SG_ID" --protocol tcp --port 80 --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --group-id "$SG_ID" --protocol tcp --port 22 --cidr SEU_IP/32
```

Para conferir que o banco não está exposto, rode do seu computador `nc -vz IP_DA_EC2 5432`
(ou `Test-NetConnection IP_DA_EC2 -Port 5432` no PowerShell). A conexão deve falhar.

## Logs na EC2

```bash
sudo systemctl status crm-backend        # o servico esta rodando?
sudo journalctl -u crm-backend -f        # log da API em tempo real
sudo journalctl -u crm-backend -n 200    # ultimas 200 linhas
sudo journalctl -u crm-backend --since "10 min ago"
sudo tail -f /var/log/nginx/access.log   # requisicoes que chegaram ao Nginx
sudo tail -f /var/log/nginx/error.log    # erros do Nginx
sudo systemctl restart crm-backend       # reiniciar a API
sudo ls /var/log/crm-deploy/             # logs dos builds feitos pelo setup.sh
```

Um login com senha errada, por exemplo, aparece no log da API como
`Tentativa de login malsucedida para o email: ...`, e a requisição aparece no `access.log` com status 401.

## Rodar localmente

Pré-requisitos: Java 17, Maven 3.9 e um PostgreSQL local com um banco vazio.

```sql
CREATE ROLE crm WITH LOGIN PASSWORD 'SUA_SENHA_LOCAL';
CREATE DATABASE crmdb OWNER crm;
```

As credenciais vêm de variáveis de ambiente. Veja o modelo em [`.env.example`](.env.example).

```powershell
# PowerShell
$env:DB_USERNAME="crm"; $env:DB_PASSWORD="SUA_SENHA_LOCAL"
$env:JWT_SECRET="TROQUE_POR_UMA_CHAVE_COM_32_CARACTERES_OU_MAIS"
$env:ADMIN_EMAIL="admin@crm.local"; $env:ADMIN_PASSWORD="ESCOLHA_UMA_SENHA"
mvn spring-boot:run
```

```bash
# Linux / macOS
export DB_USERNAME=crm DB_PASSWORD=SUA_SENHA_LOCAL
export JWT_SECRET=TROQUE_POR_UMA_CHAVE_COM_32_CARACTERES_OU_MAIS
export ADMIN_EMAIL=admin@crm.local ADMIN_PASSWORD=ESCOLHA_UMA_SENHA
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`, e o Swagger fica em `http://localhost:8080/swagger-ui.html`.
O Flyway cria as tabelas na primeira execução.

### Testes

```bash
mvn verify
```

Roda os testes, gera o relatório de cobertura em `target/site/jacoco/index.html` e reprova a build
se a cobertura ficar abaixo de 90% ou se houver código duplicado. Os testes usam H2 em memória e
não precisam do PostgreSQL. Detalhes em [TESTES.md](TESTES.md).

## Variáveis de ambiente

Nenhuma credencial fica no código ou no Git. Na EC2, todas ficam em `/etc/crm/crm.env`.

| Variável | Obrigatória | Descrição |
|---|---|---|
| `DB_URL` | não | padrão `jdbc:postgresql://localhost:5432/crmdb` |
| `DB_USERNAME` | **sim** | usuário do banco |
| `DB_PASSWORD` | **sim** | senha do banco |
| `JWT_SECRET` | **sim** | chave HMAC do JWT, com no mínimo 32 caracteres (`openssl rand -base64 48`) |
| `JWT_EXPIRATION_MS` | não | validade do token; padrão de 2 horas (`7200000`) |
| `CORS_ORIGINS` | não | origens liberadas, separadas por vírgula; padrão `http://localhost:4200` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | não | criam o primeiro administrador, se ainda não houver nenhum |
| `TELEGRAM_BOT_TOKEN` / `TELEGRAM_CHAT_ID` | não | sem eles, as notificações são ignoradas |
| `SHOW_SQL` | não | mostra o SQL no log; padrão `false` |

Sem `DB_PASSWORD` ou sem `JWT_SECRET`, a aplicação nem sobe. Uma chave do JWT curta demais também
impede a subida.

## Segurança

- **JWT stateless**: `POST /api/auth/login` e `POST /api/auth/registro` devolvem
  `{ token, tipo: "Bearer", expiraEm, usuario }`. As demais rotas exigem o cabeçalho
  `Authorization: Bearer <token>`.
- **Expiração**: 2 horas por padrão. O filtro recarrega o usuário a cada requisição, então
  desativar alguém corta o acesso na hora.
- **401 e 403 em JSON** (`RespostaErroSeguranca`), no mesmo formato do `GlobalExceptionHandler`:
  - sem token, ou com token inválido/expirado: **401**;
  - perfil sem permissão: **403**;
  - login com senha errada: **401**; usuário inativo: **403**.
- **Perfis**:

  | Rota | Quem acessa |
  |---|---|
  | `/api/auth/**`, Swagger | público |
  | `GET /api/usuarios/**`, `PATCH /api/usuarios/{id}/senha` | qualquer usuário autenticado |
  | demais operações em `/api/usuarios/**` | `ADMIN`, `GERENTE` |
  | `PATCH /api/clientes/{id}/reatribuir` | `ADMIN`, `GERENTE` |
  | todo o resto | qualquer usuário autenticado |

- **Cadastro público**: sempre cria `VENDEDOR`. O perfil não é aceito na requisição.
- **Senhas** com BCrypt (sal aleatório por senha). **CORS** restrito às origens de `CORS_ORIGINS`.

## Arquitetura

```
src/main/java/com/crmapi/sistemacrm/
├── controller/      endpoints REST (entrada e saida por DTO)
├── service/         regras de negocio (interfaces + impl/)
├── repository/      Spring Data JPA + specification/ para filtros dinamicos
├── model/           entidades JPA e enums
├── dto/             contratos de entrada e saida da API
├── mapper/          conversao entidade <-> DTO
├── security/        JwtService, filtro do Bearer token e respostas 401/403
├── config/          Spring Security, CORS, OpenAPI e administrador inicial
├── exception/       GlobalExceptionHandler e excecoes de negocio
└── client/          cliente Feign do Telegram
src/main/resources/db/migration/   migracoes Flyway
deploy/              setup.sh, unidade systemd e configuracao do Nginx
```

Além do CRUD, a API tem:

- **Funil de vendas**: cada cliente avança pelas etapas `NOVO → EM_CONTATO → NEGOCIACAO →
  FECHADO` (ou `PERDIDO`).
- **Distribuição de carteira**: reatribuição de clientes entre vendedores, registrada no
  histórico `atribuicoes_log` (de quem, para quem, quem fez a troca e o motivo).
- **Central de conversas**: uma conversa é aberta automaticamente para cada cliente, com
  mensagens, contagem de não lidas e status de entrega.
- **Notificações no Telegram** quando um cliente é atribuído ou reatribuído.
- **Filtros combináveis e paginação** em todas as listagens.

## Endpoints

A documentação completa, com exemplos, está no Swagger (`/swagger-ui.html`). Use o botão
**Authorize** com o token do login.

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/login` | login, devolve o JWT |
| POST | `/api/auth/registro` | cria uma conta (VENDEDOR) e devolve o JWT |
| GET/POST | `/api/usuarios` | lista com filtros / cadastra integrante |
| GET/PUT/DELETE | `/api/usuarios/{id}` | consulta, edita, exclui |
| PATCH | `/api/usuarios/{id}/status` · `/senha` | ativa/desativa · troca a senha |
| GET/POST | `/api/clientes` | lista com filtros / cadastra cliente |
| GET/PUT/DELETE | `/api/clientes/{id}` | consulta, edita, inativa |
| PATCH | `/api/clientes/{id}/status-funil` | move no funil |
| PATCH | `/api/clientes/{id}/reatribuir` | troca o vendedor (gestão) |
| GET | `/api/conversas` · `/api/conversas/{id}` | fila de atendimento |
| GET/POST | `/api/mensagens/conversa/{id}` · `/api/mensagens` | histórico · envio |

## Licença

[MIT](LICENSE)
