# Testes e qualidade de código

190 testes em 22 classes, cobrindo a API inteira. A build reprova sozinha se a
cobertura cair ou se aparecer código duplicado.

## Como rodar

Suíte completa, com relatório de cobertura e checagem de duplicação:

```bash
mvn clean verify
```

Só os testes, sem os portões de qualidade:

```bash
mvn test
```

O relatório de cobertura fica em `target/site/jacoco/index.html`.

## Resultado atual

Medido no SonarQube 9.9.8 LTS Community e no relatório do JaCoCo.

| Medida | Exigido | Obtido |
|---|---|---|
| Cobertura de linhas (JaCoCo) | ≥ 90% | **100%** |
| Cobertura de instruções | ≥ 90% | **100%** |
| Cobertura de branches | ≥ 90% | **100%** |
| Duplicação de código | ≤ 2% | **0%** |
| Bugs (SonarQube) | 0 | **0** |
| Vulnerabilidades | 0 | **0** |
| Code smells | 0 | **0** |
| Rating de manutenibilidade | A | **A** |

## Os portões da build

Dois plugins reprovam a build, para que a qualidade não dependa de alguém
lembrar de conferir:

- **JaCoCo `check`** — piso de 90% em instruções, linhas e branches. O valor
  fica na propriedade `jacoco.minimo`, no `pom.xml`.
- **PMD CPD** — reprova se encontrar blocos repetidos de 100 tokens ou mais.
  Os DTOs ficam fora dessa contagem: um `record` não herda declaração de campo,
  então `ClienteCreate` e `ClienteUpdate` repetem as anotações de validação por
  imposição da linguagem, e não podem virar um só porque o de cadastro aceita a
  etapa inicial do funil que o de edição recusa.

## Estratégia: cada camada no seu nível

Nenhum teste sobe a aplicação inteira com `@SpringBootTest`. Cada camada é
testada no menor contexto que ainda prova alguma coisa:

| Camada | Recurso | O que se verifica |
|---|---|---|
| Controllers | `@WebMvcTest` + `@MockBean` | Rota, código de status, validação do corpo, formato do JSON |
| Repositories | `@DataJpaTest` + H2 | Consultas derivadas, Specifications e restrições da tabela, executadas de verdade |
| Services | Mockito puro | Regra de negócio, com todos os vizinhos mockados |
| Mappers, callbacks, config | JUnit puro | Conversões e valores padrão, sem contexto nenhum |

Os testes de repositório rodam contra um H2 em memória no modo de compatibilidade
com PostgreSQL, configurado em `src/test/resources/application.properties`. O
Flyway fica desligado ali: as migrações são escritas em dialeto PostgreSQL e o
schema dos testes sai das próprias entidades.

A dupla cobre o mesmo assunto por dois ângulos. `SpecificationsTest` confere
**como** o predicado é montado, com o `CriteriaBuilder` mockado;
`ClienteRepositoryTest` confere **o que** ele filtra, com clientes de verdade no
banco. Um pega erro de montagem, o outro pega erro de semântica.

## Estrutura dos testes

Todos seguem Arrange-Act-Assert, com os três passos marcados por comentário, e
usam `@DisplayName` para que o relatório seja lido como uma lista de regras do
sistema em vez de uma lista de nomes de método. Os casos ficam agrupados por
operação com `@Nested`.

```
UsuarioServiceImpl: gestão da equipe
├── Cadastro
│   ├── cadastra o usuário quando o e-mail está livre
│   ├── recusa e-mail já cadastrado sem tocar no banco
│   └── converte falha do banco em erro de negócio
└── Tratamento da senha
    ├── grava o hash BCrypt, nunca o texto digitado
    ├── gera hashes diferentes para duas pessoas com a mesma senha
    ├── troca a senha quando a atual confere
    └── recusa a troca quando a senha atual está errada
```

### Caminhos tristes

Cada endpoint tem teste para o que dá errado, e não só para o caminho feliz:
400 para corpo inválido, parâmetro com tipo errado e JSON quebrado; 404 para
recurso inexistente; 409 para regra de negócio violada e para conflito de
integridade; 500 apenas para falha inesperada, e sem expor a mensagem interna.

## SonarQube local

A instância é a **LTS Community** (SonarQube 9.9.x). Para criar o container pela
primeira vez:

```bash
docker run -d --name sonarqube -p 9000:9000 -v sonarqube_data:/opt/sonarqube/data -v sonarqube_logs:/opt/sonarqube/logs -v sonarqube_extensions:/opt/sonarqube/extensions sonarqube:lts-community
```

Depois disso, basta ligar:

```bash
docker start sonarqube
```

O painel abre em <http://localhost:9000>. Com o servidor de pé, a análise sai de:

```bash
mvn clean verify sonar:sonar -Dsonar.login=SEU_TOKEN
```

Na 9.9 o parâmetro é `sonar.login`; `sonar.token` só vale das versões mais novas
em diante. O token se gera em **My Account → Security**, ou pela API:

```bash
curl -u admin:admin -X POST "http://localhost:9000/api/user_tokens/generate?name=analise-crm"
```

O `pom.xml` já aponta o relatório do JaCoCo para o Sonar, então a cobertura
aparece junto com a análise, sem configuração extra.

> O conjunto de regras muda entre versões do SonarQube. A 9.9 LTS, por exemplo,
> aponta `java:S1710` nos `@ApiResponses({...})` dos controllers, que as versões
> mais novas não apontam. Os controllers usam a forma repetida `@ApiResponse`,
> que o compilador remonta no mesmo bytecode e satisfaz as duas.
