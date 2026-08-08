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

**Próximos resultados**

1. Motor de conciliação automática entre transações internas e liquidações externas
2. Detecção de divergências: liquidação ausente, valor incorreto, taxa divergente, status inconsistente
3. Relatórios exportáveis para análise financeira e auditoria

Construído como **monólito modular** em Java 21 + Spring Boot, com domínio financeiro real, regras de negócio na aplicação e base preparada para evoluir para processamento assíncrono.

---

## Status do projeto

**Sprint 3 concluída:** transações internas e importação de liquidações externas via CSV.

| Área | Entregue |
| :--- | :--- |
| **Auth & usuários** | JWT, roles (`ADMIN`, `FINANCIAL_ANALYST`), CRUD de usuários |
| **Merchants & taxas** | CRUD com soft delete, fee rules por método de pagamento e parcelas |
| **Transações internas** | Registro, cálculo de `expectedNetAmount`, controle de status, filtros |
| **Liquidações externas** | Importação CSV (OpenCSV), lotes de importação, consulta com filtros |
| **Infra & qualidade** | Flyway (V1–V8), Swagger, Testcontainers, CI no GitHub Actions |

**Próximo passo:** motor de conciliação, cruzar transações internas com liquidações externas e identificar divergências.

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
| `auth` | Cadastro, login e gerenciamento de usuários |
| `security` | JWT, filtros e configuração de segurança |
| `merchant` | Cadastro e gerenciamento de merchants |
| `feeRule` | Regras de taxa por merchant |
| `transaction` | Transações internas por merchant |
| `externalsettlement` | Importação e consulta de liquidações externas |
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

---

## API

### Auth

| Método | Endpoint | Descrição |
| :---: | :--- | :--- |
| POST | `/api/auth/register` | Registra usuário |
| POST | `/api/auth/login` | Autentica e retorna JWT |

### Users *(ADMIN)*

| Método | Endpoint | Descrição |
| :---: | :--- | :--- |
| POST | `/api/users` | Cria usuário com role |
| GET | `/api/users` | Lista usuários ativos |
| GET | `/api/users/{id}` | Busca por id |
| GET | `/api/users/email?email=` | Busca por e-mail |
| PUT | `/api/users/{id}` | Atualiza nome |
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

Autenticação JWT stateless. Rotas públicas: `/api/auth/**`, Swagger, `/actuator/health`.

| Recurso | Leitura | Escrita |
| :--- | :--- | :--- |
| `/api/users/**` | ADMIN | ADMIN |
| `/api/merchants/**` | ADMIN | ADMIN |
| `.../transactions/**` | ADMIN, ANALYST | ADMIN |
| `.../external-settlements/**` | ADMIN, ANALYST | ADMIN (import) |

Usuários seed (dev/test):

| Role | E-mail | Senha |
| :--- | :--- | :--- |
| ADMIN | `admin@reconpay.local` | `Admin@123` |
| FINANCIAL_ANALYST | `analyst@reconpay.local` | `Analyst@123` |

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

A CI executa `./mvnw -B verify` em push e pull request para `main`.

---

## Banco de dados

Migrations Flyway:

| Migration | Descrição |
| :--- | :--- |
| V1 | Tabela `merchants` |
| V2 | Tabela `users` |
| V3 | Tabela `fee_rules` |
| V4 | Alinhamento de roles |
| V5 | Seed admin |
| V6 | Tabela `internal_transactions` |
| V7 | Seed analista |
| V8 | Tabelas `settlement_imports` e `external_settlements` |

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

> Na **Opção A**, o `.env` configura apenas o PostgreSQL no Docker. Na **Opção B**, também configura a API (incluindo `JWT_SECRET`).

### 2. Escolha como subir a aplicação

Há duas formas. Em ambas o PostgreSQL roda na porta **5433** e a API fica em **http://localhost:8080**.

#### Opção A - Desenvolvimento local

Docker apenas para o banco; a API roda na sua máquina com hot reload.

```bash
docker compose up -d banco-reconpay
./mvnw spring-boot:run
```

Ideal para desenvolvimento, debug e execução de testes.

#### Opção B - Tudo via Docker

Sobe banco e API em containers usando o `.env` automaticamente. Não precisa instalar Java localmente.

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
- [ ] Motor de conciliação
- [ ] Identificação de divergências
- [ ] Relatórios CSV

### Evolução futura

- Processamento assíncrono (RabbitMQ)
- Spring Batch para arquivos grandes
- Observabilidade (Prometheus, Grafana)
- Deploy em cloud

---

## Autor

**Hanrry Santos**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/hanrrysantos)
[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/hanrrysantos)

---

