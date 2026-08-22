# ReconPay

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

Fintechs, gateways e marketplaces vendem todos os dias, mas nem sempre recebem exatamente o que deveriam. Liquidações atrasadas, taxas incorretas, valores líquidos divergentes e chargebacks passam batido quando a conciliação é manual, lenta e difícil de auditar.

O **ReconPay** ataca esse problema com uma API backend que centraliza o fluxo de conciliação financeira: registra transações internas, calcula o valor líquido esperado com base nas regras de taxa de cada merchant, importa liquidações externas via CSV e prepara o terreno para cruzar automaticamente os dois lados.

**Como funciona hoje**

1. Configuração do merchant e suas fee rules por método de pagamento e parcelas
2. Registro de transações internas com `expectedNetAmount` calculado
3. Importação de liquidações externas com validação linha a linha e rastreio por lote
4. Conciliação automática entre transações internas e liquidações externas
5. Detecção de divergências e exportação de relatórios CSV para auditoria

**Próximos resultados**

1. Spring Batch para arquivos grandes
2. Rotação e revogação de tokens JWT
3. Evolução para arquitetura distribuída

Construído como **monólito modular** em Java 21 + Spring Boot, com domínio financeiro real, regras de negócio na aplicação e conciliação executada em background.

---

## Status do projeto

**Sprint 5 concluída:** conciliação assíncrona com status e isolamento de dados por merchant.

| Área | Entregue |
| :--- | :--- |
| **Auth & usuários** | JWT, roles (`ADMIN`, `FINANCIAL_ANALYST`), CRUD de usuários, acesso por merchant |
| **Merchants & taxas** | CRUD com soft delete, fee rules por método de pagamento e parcelas |
| **Transações internas** | Registro, cálculo de `expectedNetAmount`, controle de status, filtros |
| **Liquidações externas** | Importação CSV (OpenCSV), lotes de importação, consulta com filtros |
| **Conciliação** | Execução assíncrona com status, detecção de divergências, consulta de resultados, exportação CSV |
| **Infra & qualidade** | Flyway (V1–V15), Swagger, Testcontainers, CI no GitHub Actions |

**MVP concluído** — todas as funcionalidades planejadas para a primeira versão estão implementadas.

---

## Stack

| Camada | Tecnologias |
| :--- | :--- |
| Backend | Java 21, Spring Boot 3, Spring Web, Data JPA, Security, Bean Validation, MapStruct, Lombok |
| Banco | PostgreSQL, Flyway, Hibernate |
| Segurança | JWT (stateless) |
| Testes | JUnit 5, Mockito, MockMvc, AssertJ, Testcontainers |
| Infra | Docker, Docker Compose, GitHub Actions |

---

## Módulos

| Módulo | Responsabilidade |
| :--- | :--- |
| `auth` | Cadastro, login, gerenciamento de usuários e concessão de acesso a merchants |
| `security` | JWT, filtros, configuração de segurança e guard de acesso por merchant |
| `merchant` | Cadastro e gerenciamento de merchants |
| `feeRule` | Regras de taxa por merchant |
| `transaction` | Transações internas por merchant |
| `externalsettlement` | Importação e consulta de liquidações externas |
| `reconciliation` | Motor de conciliação, divergências e relatórios CSV |
| `exception` | Tratamento global e respostas padronizadas (`StandardError`) |
| `config` / `shared` | Configurações e utilitários compartilhados |

Estrutura interna de cada módulo:

```text
module/
├── controller/
├── dto/
├── entity/
├── mapper/
├── repository/
└── service/
```

---

## Regras de negócio

### Usuários
- E-mail único; soft delete; usuários inativos não autenticam.

### Merchants
- Documento único; soft delete; consultas retornam apenas merchants ativos.

### Fee rules
- Uma regra ativa por combinação `(merchant, paymentMethod, installments)`.
- Índice único parcial no banco permite histórico de regras inativas.

### Transações internas
- Referência externa única por merchant.
- Fee rule ativa obrigatória; `expectedNetAmount = amount - taxa percentual - taxa fixa`.
- PIX, boleto e débito não permitem parcelamento.
- Status inicial `APPROVED`; transições para `CANCELLED`, `REFUNDED` ou `CHARGEBACK` (sem reversão).

### Liquidações externas
- Importação via CSV (máx. 5 MB) com validação linha a linha.
- Colunas esperadas: `externalReference`, `amount`, `netAmount`, `paymentMethod`, `installments`, `status`, `settlementDate`.
- Rejeita duplicidade no arquivo e no banco; `netAmount` não pode ser maior que `amount`.
- Cada importação gera um lote rastreável (`settlement_imports`).

### Conciliação
- Cruzamento por `(merchant, externalReference)` entre transações internas e liquidações externas.
- Janela obrigatória (`fromDate`, `toDate`) aplicada sobre a data da transação, limitada a `max-window-days`.
- O lado da liquidação lê a janela estendida por `settlement-lag-days`, porque uma venda no fim do período liquida no período seguinte. Liquidações cuja transação está fora da janela são ignoradas em vez de reportadas como órfãs.
- Tipos de divergência: liquidação ausente, liquidação órfã, valor bruto incorreto, taxa divergente, status inconsistente, método de pagamento divergente, parcelas divergentes. Todos são avaliados de forma independente.
- Comparação de valores aceita `amount-tolerance` (padrão `0.00`, ou seja, comparação exata).
- Cada item guarda um **snapshot** dos dois lados no momento da execução, então alterar uma transação depois não reescreve o resultado de um run passado.
- Uma janela tem no máximo um run vigente: ao concluir, o run marca o anterior como `supersededAt`.
- A execução é assíncrona: o POST devolve `202 Accepted` com o run em `PENDING` e o `Location` para acompanhar. O run passa por `RUNNING` e termina em `COMPLETED` ou `FAILED` (com `errorMessage`).
- Enquanto houver um run `PENDING` ou `RUNNING` para a mesma janela, uma nova execução é rejeitada com `409`.
- Exportação CSV dos resultados para auditoria, escrita em streaming.

Ajustáveis por `reconpay.reconciliation.*` ou pelas variáveis `RECONCILIATION_AMOUNT_TOLERANCE`, `RECONCILIATION_SETTLEMENT_LAG_DAYS`, `RECONCILIATION_MAX_WINDOW_DAYS`, `RECONCILIATION_ASYNC`, `RECONCILIATION_WORKERS` e `RECONCILIATION_QUEUE_CAPACITY`.

---

## API

### Auth

| Método | Endpoint | Descrição |
| :---: | :--- | :--- |
| POST | `/api/auth/register` | Registra usuário (inativo até aprovação de um ADMIN) |
| POST | `/api/auth/login` | Autentica e retorna JWT |

### Users *(ADMIN)*

| Método | Endpoint | Descrição |
| :---: | :--- | :--- |
| POST | `/api/users` | Cria usuário com role |
| GET | `/api/users` | Lista usuários ativos |
| GET | `/api/users/{id}` | Busca por id |
| GET | `/api/users/email?email=` | Busca por e-mail |
| PUT | `/api/users/{id}` | Atualiza nome |
| PATCH | `/api/users/{id}/activation` | Ativa conta pendente |
| GET | `/api/users/{id}/merchants` | Lista merchants que o usuário enxerga |
| PUT | `/api/users/{id}/merchants` | Substitui a lista de merchants concedidos |
| DELETE | `/api/users/{id}` | Desativa usuário |

### Merchants *(ADMIN)*

| Método | Endpoint | Descrição |
| :---: | :--- | :--- |
| POST | `/api/merchants` | Cadastra merchant |
| GET | `/api/merchants` | Lista merchants ativos |
| GET | `/api/merchants/{id}` | Busca por id |
| PUT | `/api/merchants/{id}` | Atualiza merchant |
| DELETE | `/api/merchants/{id}` | Desativa merchant |

### Fee rules *(ADMIN)*

| Método | Endpoint | Descrição |
| :---: | :--- | :--- |
| POST | `/api/merchants/{merchantId}/fee-rules` | Cria regra de taxa |
| GET | `/api/merchants/{merchantId}/fee-rules` | Lista regras ativas |
| GET | `/api/merchants/{merchantId}/fee-rules/{id}` | Busca por id |
| PUT | `/api/merchants/{merchantId}/fee-rules/{id}` | Atualiza regra |
| DELETE | `/api/merchants/{merchantId}/fee-rules/{id}` | Desativa regra |

### Transactions

| Método | Endpoint | Acesso | Descrição |
| :---: | :--- | :--- | :--- |
| POST | `/api/merchants/{merchantId}/transactions` | ADMIN | Registra transação interna |
| GET | `/api/merchants/{merchantId}/transactions` | ADMIN, ANALYST | Lista com filtros |
| GET | `/api/merchants/{merchantId}/transactions/{id}` | ADMIN, ANALYST | Busca por id |
| PATCH | `/api/merchants/{merchantId}/transactions/{id}/status` | ADMIN | Atualiza status |

Filtros: `status`, `paymentMethod`, `fromDate`, `toDate`.

### External settlements

| Método | Endpoint | Acesso | Descrição |
| :---: | :--- | :--- | :--- |
| POST | `/api/merchants/{merchantId}/external-settlements/import` | ADMIN | Importa CSV |
| GET | `/api/merchants/{merchantId}/external-settlements/imports` | ADMIN, ANALYST | Lista lotes de importação |
| GET | `/api/merchants/{merchantId}/external-settlements/imports/{importId}` | ADMIN, ANALYST | Detalhe do lote |
| GET | `/api/merchants/{merchantId}/external-settlements` | ADMIN, ANALYST | Lista liquidações |
| GET | `/api/merchants/{merchantId}/external-settlements/{id}` | ADMIN, ANALYST | Busca por id |

Filtros: `status`, `paymentMethod`, `fromDate`, `toDate`, `importId`.

### Reconciliations

| Método | Endpoint | Acesso | Descrição |
| :---: | :--- | :--- | :--- |
| POST | `/api/merchants/{merchantId}/reconciliations` | ADMIN | Agenda conciliação (`202 Accepted`) |
| GET | `/api/merchants/{merchantId}/reconciliations` | ADMIN, ANALYST | Lista execuções |
| GET | `/api/merchants/{merchantId}/reconciliations/{runId}` | ADMIN, ANALYST | Detalhe da execução |
| GET | `/api/merchants/{merchantId}/reconciliations/{runId}/items` | ADMIN, ANALYST | Itens com filtros |
| GET | `/api/merchants/{merchantId}/reconciliations/{runId}/export` | ADMIN, ANALYST | Exporta relatório CSV |

Filtros de itens: `result` (`MATCHED`, `DIVERGENT`), `discrepancyType`.

Corpo obrigatório da execução:

```json
POST /api/merchants/{merchantId}/reconciliations

{
  "fromDate": "2026-07-01",
  "toDate": "2026-07-31"
}
```

A resposta é `202 Accepted` com o run em `PENDING` e o header `Location` apontando para `GET /api/merchants/{merchantId}/reconciliations/{runId}`, que é onde o resultado final aparece.

---

### Exemplo - transação interna

```json
POST /api/merchants/{merchantId}/transactions

{
  "externalReference": "TXN-12345",
  "amount": 150.00,
  "paymentMethod": "CREDIT_CARD",
  "installments": 3,
  "transactionDate": "2026-07-29"
}
```

Retorna `expectedNetAmount` calculado com base na fee rule ativa.

---

## Segurança

Autenticação JWT stateless. Rotas públicas: `POST /api/auth/login`, `POST /api/auth/register`, Swagger, `/actuator/health`.

O auto-cadastro cria a conta com perfil `FINANCIAL_ANALYST` **inativa**. Ela não autentica até que um ADMIN aprove em `PATCH /api/users/{id}/activation`.

| Recurso | Leitura | Escrita |
| :--- | :--- | :--- |
| `/api/users/**` | ADMIN | ADMIN |
| `/api/merchants/**` | ADMIN | ADMIN |
| `.../transactions/**` | ADMIN, ANALYST | ADMIN |
| `.../external-settlements/**` | ADMIN, ANALYST | ADMIN (import) |
| `.../reconciliations/**` | ADMIN, ANALYST | ADMIN (execução) |

O papel diz **o que** um usuário pode fazer; ele não diz **de quem**. Todo endpoint sob `/api/merchants/{merchantId}/**` passa por um guard que nega o acesso por padrão: um analista só enxerga os merchants que um ADMIN concedeu em `PUT /api/users/{id}/merchants`. Merchants criados depois ficam invisíveis até serem concedidos. O ADMIN alcança todos.

Usuários seed. As migrations de seed vivem em `db/seed` e são carregadas apenas pelos profiles `dev` e `test` (via `spring.flyway.locations`), nunca em produção.

| Role | E-mail | Senha |
| :--- | :--- | :--- |
| ADMIN | `admin@reconpay.local` | `DevAdmin@2026` |
| FINANCIAL_ANALYST | `analyst@reconpay.local` | `DevAnalyst@2026` |

---

## Testes

Estratégia com JUnit 5:

| Tipo | Ferramentas | Escopo |
| :--- | :--- | :--- |
| **Unitário** | JUnit 5, Mockito, MockMvc | Services, parsers, mappers e controllers (`@WebMvcTest` com dependências mockadas) |
| **Integração** | Testcontainers (PostgreSQL), MockMvc | Fluxos completos de ponta a ponta com banco real |

```bash
./mvnw verify
```

A CI executa `./mvnw -B verify` em push e pull request para `main`, com gate de cobertura JaCoCo (85% de linhas, 75% de ramos) e um scan de vulnerabilidades em dependências.

---

## Banco de dados

Migrations Flyway:

| Migration | Descrição |
| :--- | :--- |
| V1 | Tabela `merchants` |
| V2 | Tabela `users` |
| V3 | Tabela `fee_rules` |
| V4 | Alinhamento de roles |
| V5 | Seed admin *(revogado pela V10)* |
| V6 | Tabela `internal_transactions` |
| V7 | Seed analista *(revogado pela V10)* |
| V8 | Tabelas `settlement_imports` e `external_settlements` |
| V9 | Tabelas `reconciliation_runs`, `reconciliation_items` e `reconciliation_discrepancies` |
| V10 | Remove os usuários semeados pelas V5/V7, que rodavam também em produção |
| V11 | Colunas de snapshot em `reconciliation_items` |
| V12 | Unicidade por `(run, externalReference)` e índices de FK |
| V13 | `superseded_at` em `reconciliation_runs` com índice único parcial por janela |
| V14 | Tabela `user_merchants` (acesso concedido de usuário a merchant) |
| V15 | `status`, `started_at`, `finished_at` e `error_message` em `reconciliation_runs`, com índices de janela vigente e execução em andamento |

Os seeds de desenvolvimento vivem em `db/seed` (`V900`) e só são carregados pelos profiles `dev` e `test`.

---

## Como executar

**Pré-requisitos:** Java 21, Docker e Docker Compose. Maven via wrapper (`./mvnw`).

### 1. Clone e configure o ambiente

```bash
git clone https://github.com/hanrrysantos/reconpay.git
cd reconpay
```

Crie um arquivo `.env` na raiz do projeto:

```env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=1234
JWT_SECRET=sua-chave-secreta-com-pelo-menos-32-caracteres
JWT_EXPIRATION=604800
```

> `JWT_SECRET` é obrigatório e não tem valor padrão em nenhum profile. Na **Opção A** o profile `dev` importa o `.env` diretamente; na **Opção B** o Compose o injeta no container.

### 2. Escolha como subir a aplicação

Há duas formas. Em ambas o PostgreSQL roda na porta **5433** e a API fica em **http://localhost:8080**.

#### Opção A - Desenvolvimento local

Docker apenas para o banco; a API roda na sua máquina no profile `dev`, com os usuários de seed carregados.

```bash
docker compose up -d banco-reconpay
./mvnw spring-boot:run
```

Ideal para desenvolvimento, debug e execução de testes.

#### Opção B - Tudo via Docker

Sobe banco e API em containers no profile `prod`, usando o `.env` automaticamente. Não precisa instalar Java localmente, e **não há usuários de seed** — crie o primeiro ADMIN diretamente no banco.

```bash
docker compose up --build
```

Ideal para validar o projeto rapidamente ou demonstrar o ambiente completo.

### 3. Acesse

| Recurso | URL |
| :--- | :--- |
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| Métricas (autenticado) | http://localhost:8080/actuator/prometheus |

---

## Roadmap

### MVP

- [x] Auth com JWT
- [x] Users, Merchants, Fee Rules
- [x] Transações internas
- [x] Importação de liquidações externas via CSV
- [x] Testes de integração (Testcontainers)
- [x] Testes unitários (JUnit + Mockito)
- [x] Swagger/OpenAPI
- [x] CI com GitHub Actions
- [x] Motor de conciliação
- [x] Identificação de divergências
- [x] Relatórios CSV
- [x] Runs imutáveis por snapshot e política de reexecução
- [x] Observabilidade (log estruturado, auditoria, Prometheus, tracing)
- [x] Isolamento por merchant com concessão explícita de acesso
- [x] Execução assíncrona da conciliação com status no run

### Evolução futura

- Retomada de runs interrompidos por reinício da aplicação
- Fila externa no lugar do pool em memória, para distribuir a execução entre instâncias
- Spring Batch para arquivos grandes
- Rotação e revogação de tokens JWT (refresh token, denylist)
- Rate limiting no login e no auto-cadastro
- Deploy em cloud

---

## Autor

**Hanrry Santos**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/hanrrysantos)
[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/hanrrysantos)

---

