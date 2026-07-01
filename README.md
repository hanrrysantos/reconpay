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

O projeto está atualmente na fase de fundação do MVP.

Implementado até agora:

- Cadastro e autenticação de usuários
- Segurança baseada em JWT
- Autorização por roles
- Gerenciamento de usuários ativos
- Gerenciamento de merchants
- Gerenciamento de regras de taxa por merchant
- Relacionamento entre merchants e fee rules
- Soft delete para preservação de histórico
- Tratamento global de exceptions
- Uso de DTOs para entrada e saída de dados
- Uso de mappers para conversão entre entidades e DTOs
- Validações com Bean Validation
- Modelagem inicial com PostgreSQL
- Migrations com Flyway
- Ambiente local com Docker Compose

Próximo módulo planejado:

- Transações internas

---

## Funcionalidades Principais

**Autenticação com JWT:** cadastro, login, geração de token e proteção dos endpoints da aplicação.

**Gerenciamento de usuários:** listagem, busca, atualização e desativação lógica de usuários.

**Gerenciamento de merchants:** cadastro e manutenção das empresas que terão suas transações conciliadas.

**Regras de taxa:** configuração de taxas por merchant, método de pagamento e número de parcelas.

**Soft delete:** exclusão lógica de registros importantes para preservar histórico e auditabilidade.

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

---

## Tecnologias Utilizadas

**Backend**

Java 21, Spring Boot 3, Spring Web, Spring Data JPA, Spring Security, JWT, Bean Validation, MapStruct, Lombok, Maven

**Banco de Dados**

PostgreSQL, Flyway, Hibernate

**Infraestrutura**

Docker, Docker Compose

**Testes**

JUnit 5, Mockito, AssertJ, MockMvc e Testcontainers planejados para evolução do MVP

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
|-- src/
|   |-- main/
|   |   |-- java/
|   |   |   `-- br/com/hanrry/reconpay/
|   |   |       |-- auth/
|   |   |       |-- config/
|   |   |       |-- exception/
|   |   |       |-- feeRule/
|   |   |       |-- merchant/
|   |   |       `-- security/
|   |   `-- resources/
|   |       |-- db/migration/
|   |       `-- application.yaml
|   `-- test/
|-- docker-compose.yaml
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
| `/api/users/**` | ADMIN |
| `/api/merchants/**` | ADMIN |
| `/api/fee-rules/**` | ADMIN |
| Demais rotas | Usuário autenticado |

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
| GET | `/api/users` | Lista usuários ativos |
| GET | `/api/users/{id}` | Busca usuário ativo por id |
| GET | `/api/users/email?email=example@email.com` | Busca usuário ativo por e-mail |
| PUT | `/api/users/{id}` | Atualiza nome do usuário |
| DELETE | `/api/users/{id}` | Desativa usuário |

### Merchants

| Método | Endpoint | Descrição |
| :---: | :--- | :--- |
| POST | `/api/merchants` | Cadastra um merchant |
| GET | `/api/merchants` | Lista merchants ativos |
| GET | `/api/merchants/{id}` | Busca merchant ativo por id |
| PUT | `/api/merchants/{id}` | Atualiza merchant |
| DELETE | `/api/merchants/{id}` | Desativa merchant |

### Fee Rules

| Método | Endpoint | Descrição |
| :---: | :--- | :--- |
| POST | `/api/fee-rules` | Cria uma regra de taxa |
| GET | `/api/fee-rules/{id}` | Busca regra de taxa ativa por id |
| GET | `/api/fee-rules/by-merchant/{merchantId}` | Lista regras de taxa ativas por merchant |
| PUT | `/api/fee-rules/{id}` | Atualiza uma regra de taxa |
| DELETE | `/api/fee-rules/{id}` | Desativa uma regra de taxa |

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
  "merchantId": "uuid-do-merchant",
  "paymentMethod": "CREDIT_CARD",
  "installments": 1,
  "feePercentage": 3.00,
  "fixedFee": 0.50
}
```

### Login

```json
{
  "email": "admin@email.com",
  "password": "123456"
}
```

---

## Instalação e Execução Local

### Pré-requisitos

- Java 21
- Maven
- Docker
- Docker Compose
- PostgreSQL, caso rode fora do Docker

### 1. Clone o repositório

```bash
git clone https://github.com/hanrrysantos/reconpay.git
cd reconpay
```

### 2. Configure as variáveis de ambiente

Crie um arquivo `.env` na raiz do projeto:

```env
DB_URL=jdbc:postgresql://localhost:5432/reconpay
DB_USER=postgres
DB_PASSWORD=1234
JWT_SECRET=sua-chave-secreta
JWT_EXPIRATION=86400000
```

### 3. Suba o ambiente com Docker

```bash
docker-compose up --build
```

### 4. Acesse a aplicação

| Aplicação | URL |
| :--- | :--- |
| API | `http://localhost:8080` |
| PostgreSQL | `localhost:5432` |

---

## Roadmap

### MVP

- Auth com JWT
- Users
- Merchants
- Fee Rules
- Transações internas
- Importação de liquidações externas via CSV
- Criação de lotes de conciliação
- Execução síncrona da conciliação
- Identificação de divergências
- Relatórios CSV
- Testes unitários e de integração
- Swagger/OpenAPI

### Evolução futura

- RabbitMQ para processamento assíncrono
- Spring Batch para importação de arquivos grandes
- Retry com backoff
- Dead Letter Queue
- Observabilidade com Spring Actuator
- Prometheus e Grafana
- Pipeline CI/CD com GitHub Actions
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
