# ReconPay

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

---

## Visão Geral

O **ReconPay** é uma aplicação backend para **conciliação financeira**, criada para simular um problema real enfrentado por empresas de pagamento, fintechs, bancos e marketplaces: comparar transações internas com registros externos de liquidação para identificar divergências financeiras.

A proposta do projeto é construir uma API robusta e evolutiva usando **Java, Spring Boot, PostgreSQL, Flyway, Docker e autenticação JWT**, aplicando boas práticas de arquitetura, separação de responsabilidades, validações, persistência relacional e regras de negócio reais.

O ReconPay está sendo desenvolvido inicialmente como um **monólito modular**, permitindo uma base mais simples de evoluir no MVP, mas com organização suficiente para crescer futuramente para processamento assíncrono, mensageria e arquitetura distribuída.

---

## Contexto de Negócio

Empresas que recebem pagamentos por cartão, PIX, boleto ou gateways precisam verificar se os valores vendidos foram realmente recebidos corretamente.

Durante o processo de liquidação financeira, podem ocorrer divergências como:

- Liquidação ausente
- Valor líquido recebido incorreto
- Taxa cobrada diferente da taxa configurada
- Pagamento duplicado
- Chargeback
- Venda cancelada ou estornada
- Data de liquidação divergente
- Status externo diferente do status interno

A conciliação manual desses dados é lenta, suscetível a erros e difícil de escalar. O ReconPay automatiza esse processo, permitindo identificar inconsistências de forma auditável.

---

## Status Atual

**Sprint 2 concluída** — transações internas por merchant, com cálculo de valor líquido esperado e controle de status.

Implementado:

- Cadastro e autenticação de usuários com JWT
- Roles `ADMIN` e `FINANCIAL_ANALYST` com autorização por endpoint
- CRUD de usuários (admin), merchants e fee rules
- **Transações internas aninhadas em merchants** (`/api/merchants/{merchantId}/transactions`)
- **Cálculo de `expectedNetAmount`** com base na fee rule ativa
- **Atualização de status** (`APPROVED` → `CANCELLED` | `REFUNDED` | `CHARGEBACK`)
- **Filtros opcionais** na listagem (status, paymentMethod, fromDate, toDate)
- Fee rules aninhadas em merchants (`/api/merchants/{merchantId}/fee-rules`)
- Paginação em listagens (`Page`, padrão 20 itens)
- Soft delete para merchants, users e fee rules
- Validações com Bean Validation (`@Valid` nos controllers)
- Tratamento global de erros com respostas padronizadas (`StandardError`)
- Modelagem com PostgreSQL e migrations Flyway (V1–V7)
- Seed de usuários admin e analista em dev/test
- Swagger/OpenAPI (`/swagger-ui.html`)
- Health check (`/actuator/health`)
- Profiles `dev`, `prod` e `test`
- Testes de integração com Testcontainers
- CI com GitHub Actions
- Ambiente local com Docker Compose

Próximo módulo (Sprint 3):

- Importação de liquidações externas via CSV

---

## Funcionalidades Principais

**Autenticação com JWT:** cadastro, login, geração de token e proteção dos endpoints da aplicação.

**Gerenciamento de usuários:** listagem, busca, atualização e desativação lógica de usuários.

**Gerenciamento de merchants:** cadastro e manutenção das empresas que terão suas transações conciliadas.

**Regras de taxa:** configuração de taxas por merchant, método de pagamento e número de parcelas.

**Transações internas:** registro de vendas/pagamentos por merchant, com valor líquido esperado calculado a partir das fee rules.

**Soft delete:** exclusão lógica de merchants, users e fee rules para preservar histórico e auditabilidade.

**Tratamento global de erros:** padronização das respostas de erro da API por meio de um handler global.

**Migrations versionadas:** controle da evolução do banco de dados com Flyway.

**Arquitetura modular:** organização por domínio para facilitar manutenção e evolução do projeto.

---

## Módulos Principais

| Módulo | Responsabilidade |
| :--- | :--- |
| `auth` | Cadastro, login e autenticação de usuários |
| `security` | Filtros JWT, configuração de segurança e usuário autenticado |
| `merchant` | Cadastro e gerenciamento de merchants |
| `feeRule` | Configuração das regras de taxa por merchant |
| `transaction` | Registro e consulta de transações internas por merchant |
| `exception` | Tratamento global e padronização de erros |
| `config` | Configurações gerais da aplicação |
| `shared` | Estruturas comuns que podem ser reutilizadas futuramente |

---

## Regras de Negócio Atuais

### Usuários

- Usuários possuem status ativo ou inativo.
- Usuários inativos não devem autenticar na aplicação.
- A exclusão de usuários é feita por soft delete.
- O e-mail do usuário deve ser único.

### Merchants

- Merchants representam empresas que recebem pagamentos.
- Apenas merchants ativos devem ser retornados nas consultas principais.
- A exclusão de merchants é feita por soft delete.
- O documento do merchant deve ser único.

### Fee Rules

- Uma regra de taxa pertence a um merchant.
- Uma regra define taxa por método de pagamento e número de parcelas.
- Não deve existir mais de uma regra ativa para o mesmo merchant, método de pagamento e número de parcelas.
- A exclusão de regras de taxa é feita por soft delete.
- Regras inativas permanecem no banco para histórico.

### Transações Internas

- Uma transação pertence a um merchant ativo.
- A referência externa (`externalReference`) deve ser única por merchant.
- Fee rule ativa obrigatória para o par `(paymentMethod, installments)`.
- O valor líquido esperado é calculado na criação: `amount - taxa percentual - taxa fixa`.
- PIX, boleto e débito não permitem parcelamento (`installments > 1`).
- Status inicial: `APPROVED`. Transições permitidas: `CANCELLED`, `REFUNDED`, `CHARGEBACK`.
- Estados terminais não permitem reversão. Transações não possuem soft delete.

---

## Tecnologias Utilizadas

**Backend**

Java 21, Spring Boot 3, Spring Web, Spring Data JPA, Spring Security, JWT, Bean Validation, MapStruct, Lombok, Maven

**Banco de Dados**

PostgreSQL, Flyway, Hibernate

**Infraestrutura**

Docker, Docker Compose

**Testes**

JUnit 5, Spring Boot Test, MockMvc, AssertJ, Testcontainers (PostgreSQL)

---

## Arquitetura

O ReconPay utiliza uma arquitetura inicial baseada em **monólito modular**, separando o sistema por domínios de negócio.

Cada módulo tende a seguir uma estrutura com responsabilidades bem definidas:

```text
module/
|-- api/
|-- dto/
|-- entity/
|-- enums/
|-- exception/
|-- mapper/
|-- repository/
`-- service/
```

A ideia é manter o projeto simples o suficiente para o MVP, mas organizado o bastante para permitir evolução futura.

### Camadas

| Camada | Responsabilidade |
| :--- | :--- |
| `api` | Controllers e exposição dos endpoints REST |
| `dto` | Objetos de entrada e saída da API |
| `service` | Regras de aplicação e orquestração dos casos de uso |
| `entity` | Mapeamento das entidades persistidas no banco |
| `repository` | Acesso ao banco de dados via Spring Data JPA |
| `mapper` | Conversão entre entidades e DTOs |
| `exception` | Exceptions específicas do domínio |

---

## Estrutura do Projeto

```text
reconpay/
|-- .github/workflows/
|-- src/
|   |-- main/
|   |   |-- java/
|   |   |   `-- br/com/hanrry/reconpay/
|   |   |       |-- auth/
|   |   |       |-- config/
|   |   |       |-- exception/
|   |   |       |-- feeRule/
|   |   |       |-- merchant/
|   |   |       |-- security/
|   |   |       |-- shared/
|   |   |       |-- transaction/
|   |   |       `-- StarterApplication.java
|   |   `-- resources/
|   |       |-- db/migration/
|   |       |-- application.yaml
|   |       |-- application-dev.yaml
|   |       `-- application-prod.yaml
|   `-- test/
|       |-- java/
|       `-- resources/
|           `-- application-test.yaml
|-- docker-compose.yaml
|-- mvnw
|-- pom.xml
`-- README.md
```

---

## Migrations de Banco de Dados

O schema do banco é versionado com Flyway.

Migrations atuais:

| Migration | Descrição |
| :--- | :--- |
| `V1__create_merchants_table.sql` | Criação da tabela de merchants |
| `V2__create_users_table.sql` | Criação da tabela de usuários |
| `V3__create_feerules_table.sql` | Criação da tabela de regras de taxa |
| `V4__align_user_roles.sql` | Alinhamento de roles (`ADMIN`, `FINANCIAL_ANALYST`) |
| `V5__seed_admin_user.sql` | Seed do usuário administrador (dev/test) |
| `V6__create_internal_transactions_table.sql` | Criação da tabela de transações internas |
| `V7__seed_analyst_user.sql` | Seed do usuário analista financeiro (dev/test) |

A tabela `fee_rules` utiliza um índice único parcial para impedir duplicidade entre regras ativas com a mesma combinação de:

- Merchant
- Método de pagamento
- Número de parcelas

Isso permite manter registros antigos inativos no banco sem bloquear a criação de uma nova regra ativa equivalente.

---

## Segurança

A aplicação utiliza autenticação baseada em JWT com sessão stateless.

Regras atuais de acesso:

| Rota | Acesso |
| :--- | :--- |
| `/api/auth/**` | Público |
| `/swagger-ui/**`, `/v3/api-docs/**` | Público |
| `/actuator/health` | Público |
| `/api/users/**` | `ADMIN` |
| `GET /api/merchants/*/transactions/**` | `ADMIN`, `FINANCIAL_ANALYST` |
| `POST/PATCH /api/merchants/*/transactions/**` | `ADMIN` |
| `/api/merchants/**` (demais rotas) | `ADMIN` |
| Demais rotas autenticadas | `ADMIN` ou `FINANCIAL_ANALYST` |

Usuário admin seed (dev/test):

| Campo | Valor |
| :--- | :--- |
| E-mail | `admin@reconpay.local` |
| Senha | `Admin@123` |
| Role | `ADMIN` |

Usuário analista seed (dev/test):

| Campo | Valor |
| :--- | :--- |
| E-mail | `analyst@reconpay.local` |
| Senha | `Analyst@123` |
| Role | `FINANCIAL_ANALYST` |

---

## Principais Endpoints

### Auth

| Método | Endpoint | Descrição |
| :---: | :--- | :--- |
| POST | `/api/auth/register` | Registra um novo usuário |
| POST | `/api/auth/login` | Autentica usuário e retorna token JWT |

### Users

| Método | Endpoint | Descrição |
| :---: | :--- | :--- |
| POST | `/api/users` | Cria usuário com role (admin) |
| GET | `/api/users` | Lista usuários ativos (paginado) |
| GET | `/api/users/{id}` | Busca usuário ativo por id |
| GET | `/api/users/email?email=example@email.com` | Busca usuário ativo por e-mail |
| PUT | `/api/users/{id}` | Atualiza nome do usuário |
| DELETE | `/api/users/{id}` | Desativa usuário |

### Merchants

| Método | Endpoint | Descrição |
| :---: | :--- | :--- |
| POST | `/api/merchants` | Cadastra um merchant |
| GET | `/api/merchants` | Lista merchants ativos (paginado) |
| GET | `/api/merchants/{id}` | Busca merchant ativo por id |
| PUT | `/api/merchants/{id}` | Atualiza merchant |
| DELETE | `/api/merchants/{id}` | Desativa merchant |

### Fee Rules

| Método | Endpoint | Descrição |
| :---: | :--- | :--- |
| POST | `/api/merchants/{merchantId}/fee-rules` | Cria uma regra de taxa |
| GET | `/api/merchants/{merchantId}/fee-rules` | Lista regras ativas do merchant (paginado) |
| GET | `/api/merchants/{merchantId}/fee-rules/{id}` | Busca regra de taxa ativa por id |
| PUT | `/api/merchants/{merchantId}/fee-rules/{id}` | Atualiza uma regra de taxa |
| DELETE | `/api/merchants/{merchantId}/fee-rules/{id}` | Desativa uma regra de taxa |

### Transactions

| Método | Endpoint | Descrição |
| :---: | :--- | :--- |
| POST | `/api/merchants/{merchantId}/transactions` | Registra uma transação interna |
| GET | `/api/merchants/{merchantId}/transactions` | Lista transações (paginado, filtros opcionais) |
| GET | `/api/merchants/{merchantId}/transactions/{id}` | Busca transação por id |
| PATCH | `/api/merchants/{merchantId}/transactions/{id}/status` | Atualiza status da transação |

Filtros opcionais na listagem: `status`, `paymentMethod`, `fromDate`, `toDate`.

---

## Exemplos de Payload

### Cadastro de Merchant

```json
{
  "name": "Empresa Exemplo LTDA",
  "document": "12345678000199"
}
```

### Cadastro de Regra de Taxa

```json
{
  "paymentMethod": "CREDIT_CARD",
  "installments": 1,
  "feePercentage": 3.00,
  "fixedFee": 0.50
}
```

> O `merchantId` é informado na URL: `POST /api/merchants/{merchantId}/fee-rules`

### Cadastro de Transação Interna

```json
{
  "externalReference": "TXN-12345",
  "amount": 150.00,
  "paymentMethod": "CREDIT_CARD",
  "installments": 3,
  "transactionDate": "2026-07-29"
}
```

> Requer fee rule ativa para o merchant, método e parcelas. Retorna `expectedNetAmount` calculado.

### Atualização de Status

```json
{
  "status": "REFUNDED"
}
```

### Login

```json
{
  "email": "admin@reconpay.local",
  "password": "Admin@123"
}
```

---

## Instalação e Execução Local

### Pré-requisitos

- Java 21
- Docker e Docker Compose (recomendado)
- Maven não é obrigatório — o projeto inclui Maven Wrapper (`./mvnw`)

### 1. Clone o repositório

```bash
git clone https://github.com/hanrrysantos/reconpay.git
cd reconpay
```

### 2. Configure as variáveis de ambiente

Crie um arquivo `.env` na raiz do projeto (obrigatório para subir a API em dev ou via Docker Compose):

```env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=1234
JWT_SECRET=sua-chave-secreta-com-pelo-menos-32-caracteres
JWT_EXPIRATION=604800
```

> `JWT_SECRET` é obrigatório — não há valor padrão em dev/prod. Os testes de integração usam profile `test` com configuração própria em `src/test/resources/application-test.yaml`.

### 3. Suba o ambiente com Docker

```bash
docker compose up -d banco-reconpay
```

Para subir API + banco:

```bash
docker compose up --build
```

### 4. Execute localmente (sem Docker na API)

Com o PostgreSQL rodando via Docker Compose (porta `5433`, padrão do projeto):

```bash
export JWT_SECRET=sua-chave-secreta-com-pelo-menos-32-caracteres
./mvnw spring-boot:run
```

O `application.yaml` já aponta para `localhost:5433`. Profile padrão: `dev`. Para produção: `SPRING_PROFILES_ACTIVE=prod`.

### 5. Testes

```bash
./mvnw verify
```

Os testes de integração sobem PostgreSQL via Testcontainers automaticamente.

### 6. Acesse a aplicação

| Recurso | URL |
| :--- | :--- |
| API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Health | `http://localhost:8080/actuator/health` |
| PostgreSQL (Docker Compose) | `localhost:5433` |

---

## CI (GitHub Actions)

Pipeline em `.github/workflows/ci.yml`, disparado em **push** e **pull request** para a branch `main`.

| Etapa | O que faz |
| :--- | :--- |
| **Checkout** | Clona o repositório no runner Ubuntu |
| **Set up JDK 21** | Instala Temurin 21 e cacheia dependências Maven |
| **Build and run tests** | Executa `./mvnw -B verify` — compila, roda testes de integração (Testcontainers + PostgreSQL) e valida o build |

**Concurrency:** execuções simultâneas na mesma branch cancelam a anterior para economizar minutos de CI.

Os testes usam profile `test` e não dependem do `.env` local — o JWT de teste fica isolado em `application-test.yaml`.

---

## Roadmap

### MVP

- [x] Auth com JWT
- [x] Users
- [x] Merchants
- [x] Fee Rules
- [x] Transações internas
- [x] Testes de integração
- [x] Swagger/OpenAPI
- [x] CI com GitHub Actions
- [ ] Importação de liquidações externas via CSV
- [ ] Criação de lotes de conciliação
- [ ] Execução síncrona da conciliação
- [ ] Identificação de divergências
- [ ] Relatórios CSV
- [ ] Testes unitários

### Evolução futura

- RabbitMQ para processamento assíncrono
- Spring Batch para importação de arquivos grandes
- Retry com backoff
- Dead Letter Queue
- Observabilidade com Spring Actuator (health)
- Prometheus e Grafana
- Pipeline CI/CD com GitHub Actions (CI básico)
- Deploy em ambiente cloud

---

## Objetivo do Projeto

O ReconPay não tem como objetivo ser apenas uma API CRUD.

A proposta é desenvolver um projeto backend com:

- Domínio financeiro real
- Regras de negócio relevantes
- Modelagem relacional consistente
- Segurança com JWT
- Boas práticas de arquitetura
- Evolução técnica planejada
- Código organizado para portfólio profissional

---

## Autor

**Hanrry Santos**  
Desenvolvedor Backend Java em formação, com foco em Spring Boot, APIs REST, microsserviços e arquitetura de software.

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/hanrrysantos)
[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/hanrrysantos)

---

## Licença

Este projeto é de uso livre para fins de estudo e portfólio.
