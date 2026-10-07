# API Docs Specification

## Problem Statement

Quem abre o Swagger encontra a autenticação descrita e o resto das rotas só com o nome da tag e o cadeado do JWT. Não há exemplo para executar a chamada, nem a lista do que cada código de resposta significa. A página inicial ainda ensina um fluxo numerado que não inclui o extrato bancário, e as tags não seguem a ordem em que a API é usada.

## Goals

- [ ] Cada operação em `/api/**` abre no Swagger com exemplo preenchido para teste e com cada código de resposta que essa operação produz, explicado
- [ ] As tags seguem o fluxo operacional e a página inicial descreve o produto e os dois papéis, sem roteiro numerado de teste

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Mudar status HTTP, payload, validação ou regra de negócio | A documentação descreve o contrato que já existe |
| Reescrever o README | O guia de subida e os seeds continuam lá |
| Coleção Postman ou outra UI no lugar do Swagger | O leitor usa o Swagger UI já publicado |
| Documentação em inglês | Os textos atuais e os desta feature ficam em português |
| HTTP 500, optimistic lock e conflito genérico de integridade por operação | Não são cenários para testar no Swagger; o handler global já os cobre |
| Actuator | Fora de `/api/**` |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Profundidade | Exemplo de request pronto para Try it out; cada status da operação tem descrição e, se houver corpo JSON, um exemplo | Pedido explícito: a pessoa não fica perdida e entende cada código | y |
| Onde a anotação mora | Interface `*Api` implementada pelo controller, uma por controller | Mesmo desenho de `AuthControllerApi`; o volume de exemplo não entra no método | y |
| Ordem das tags | Authentication, Session, Users, Merchants, Fee Rules, Transactions, External Settlements, Bank Statements, Reconciliations | Fluxo operacional confirmado | y |
| Página inicial | Propósito do produto e os papéis ADMIN e OPERATOR, sem lista numerada de chamadas | O passo a passo passa a viver no exemplo de cada rota | y |
| Idioma | Português nos summaries, descriptions e textos de resposta. Nomes de campo JSON permanecem os do contrato | O Swagger já está em português | y (agent) |
| Exemplo de login | `admin@reconpay.local` / `DevAdmin@2026` | Seed de dev já publicado no README; Try it out autentica sem inventar usuário | y (agent) |
| Demais JSON de entrada | Um exemplo por operação, com todo campo obrigatório preenchido com valor que passa na Bean Validation | Try it out não nasce inválido | y (agent) |
| IDs de path | UUID ilustrativo `3fa85f64-5717-4562-b3fc-2c963f66afa6` em todo path param | O id real vem da resposta anterior; o exemplo mostra o formato | y (agent) |
| Upload CSV | Parte `file` como seletor de arquivo. A description inclui um CSV mínimo válido para a pessoa salvar e enviar. Campo `layout` do import de liquidação exemplifica `RECONPAY` | Swagger não pré-anexa binário | y (agent) |
| Códigos por operação | Só o status de sucesso do método e os erros que essa operação alcança: 400 quando há body, arquivo, query ou path; 401 em rota protegida e no login; 403 em rota protegida; 404 quando a operação busca recurso por id; 409 quando o serviço dessa operação lança conflito de domínio; 413 nos dois imports acima de 5MB | "Possíveis" significa o que o caller consegue provocar nessa rota | y (agent) |
| 204 e HTML | 204 só com descrição, sem schema. GET `/api/auth/verify-email` documenta `text/html` em 200 e 400, sem `StandardError` | Esses retornos não são JSON | y (agent) |
| Export CSV | 200 `text/csv` com a linha de cabeçalho do arquivo no exemplo | O método grava o stream, não um DTO | y (agent) |
| Segurança no documento | As quatro operações de `/api/auth` ficam com security vazio. As demais exigem o scheme `Bearer Authentication` | O item global de segurança não pode travar o Try it out do login | y (agent) |
| Ordenação dentro da tag | A ordem dos métodos no controller | Não há preferência por ordem alfabética | y (agent) |
| Concorrência, expiração, fila e serviço externo | N/A porque a feature não altera persistência, TTL, execução nem integração | Só o documento OpenAPI muda | y (agent) |
| Rate limit | N/A porque a API não limita taxa | Nada novo para documentar | y (agent) |

**Open questions:** none

---

## User Stories

### P1: Achar a rota e entender o produto ⭐ MVP

**User Story**: As a person testing the API in Swagger, I want the tags in operational order and a short intro so that I know what the API does and where to start.

**Why P1**: Sem ordem e sem intro curta, o exemplo de cada rota não tem contexto.

**Acceptance Criteria**:

1. WHEN a client gets `/v3/api-docs` THEN the system SHALL list the tags in this order, each with a non-empty Portuguese description: Authentication, Session, Users, Merchants, Fee Rules, Transactions, External Settlements, Bank Statements, Reconciliations.
2. The system SHALL set `info.description` in Portuguese, name the roles ADMIN and OPERATOR, and omit any numbered list of endpoint calls.
3. The system SHALL keep Swagger UI from sorting those tags alphabetically.

**Independent Test**: Open `/v3/api-docs` and read `tags` and `info.description`.

---

### P1: Testar a rota com o exemplo preenchido ⭐ MVP

**User Story**: As a person testing the API in Swagger, I want each operation to open with a valid example so that I can run it without inventing the payload.

**Why P1**: O pedido é que a pessoa não fique perdida na hora de testar.

**Acceptance Criteria**:

1. WHEN a client gets `/v3/api-docs` THEN the system SHALL give every `/api/**` operation a non-empty Portuguese `summary` and `description`.
2. WHEN an operation has a JSON request body THEN the system SHALL publish one request example that contains every required property and that passes that DTO's Bean Validation.
3. WHEN the operation is POST `/api/auth/login` THEN the system SHALL use the request example email `admin@reconpay.local` and password `DevAdmin@2026`.
4. The system SHALL publish a UUID example on every path parameter and an example on every query parameter.
5. WHEN the operation is POST `/api/merchants/{merchantId}/external-settlements/import` or POST `/api/merchants/{merchantId}/bank-statements/import` THEN the system SHALL document the multipart part `file` and include, in the operation description, a minimal valid CSV for that import. The settlement import example for `layout` SHALL be `RECONPAY`.
6. The system SHALL publish the four `/api/auth` operations with an empty security requirement and every other `/api/**` operation with the scheme `Bearer Authentication`.

**Independent Test**: Load `/v3/api-docs`, parse each JSON request example, and run Bean Validation on the matching DTO. Confirm the login example matches the dev seed.

---

### P1: Ler o que cada código significa ⭐ MVP

**User Story**: As a person testing the API in Swagger, I want every status that operation can return, with a sentence and a sample body, so that I know what happened.

**Why P1**: O pedido inclui os códigos possíveis e a explicação de cada um.

**Acceptance Criteria**:

1. WHEN a client gets `/v3/api-docs` THEN the system SHALL document, for each `/api/**` operation, exactly the success status that the controller method returns and the error statuses from the response table below, each with a non-empty Portuguese description.
2. WHEN a documented response has a JSON body THEN the system SHALL include one example whose `status` and `error` match that response for errors shaped as `StandardError`, and one example of the success DTO for a JSON success body.
3. IF the success status is 204 THEN the system SHALL document the description and no response schema.
4. WHEN the operation is GET `/api/auth/verify-email` THEN the system SHALL document 200 and 400 as `text/html` with a description and without a `StandardError` schema.
5. WHEN the operation is GET `/api/merchants/{merchantId}/reconciliations/{runId}/export` THEN the system SHALL document 200 as `text/csv` with the CSV header line as the example.

**Response table** (method, path, codes):

| Method | Path | Codes |
| ------ | ---- | ----- |
| POST | `/api/auth/login` | 200, 400, 401 |
| POST | `/api/auth/register` | 201, 400, 409 |
| POST | `/api/auth/verify-email` | 204, 400 |
| GET | `/api/auth/verify-email` | 200, 400 |
| GET | `/api/me` | 200, 401 |
| GET | `/api/me/merchants` | 200, 400, 401 |
| POST | `/api/users` | 201, 400, 401, 403, 409 |
| GET | `/api/users` | 200, 400, 401, 403 |
| GET | `/api/users/{id}` | 200, 400, 401, 403, 404 |
| GET | `/api/users/email` | 200, 400, 401, 403, 404 |
| PATCH | `/api/users/{id}/activation` | 200, 400, 401, 403, 404 |
| GET | `/api/users/{id}/merchants` | 200, 400, 401, 403, 404 |
| PUT | `/api/users/{id}/merchants` | 200, 400, 401, 403, 404 |
| PUT | `/api/users/{id}` | 200, 400, 401, 403, 404 |
| DELETE | `/api/users/{id}` | 204, 400, 401, 403, 404 |
| POST | `/api/merchants` | 201, 400, 401, 403, 409 |
| GET | `/api/merchants` | 200, 400, 401, 403 |
| GET | `/api/merchants/{id}` | 200, 400, 401, 403, 404 |
| PUT | `/api/merchants/{id}` | 200, 400, 401, 403, 404 |
| DELETE | `/api/merchants/{id}` | 204, 400, 401, 403, 404 |
| GET | `/api/merchants/{merchantId}/fee-rules` | 200, 400, 401, 403, 404 |
| GET | `/api/merchants/{merchantId}/fee-rules/{id}` | 200, 400, 401, 403, 404 |
| POST | `/api/merchants/{merchantId}/fee-rules` | 201, 400, 401, 403, 404, 409 |
| PUT | `/api/merchants/{merchantId}/fee-rules/{id}` | 200, 400, 401, 403, 404, 409 |
| DELETE | `/api/merchants/{merchantId}/fee-rules/{id}` | 204, 400, 401, 403, 404 |
| GET | `/api/merchants/{merchantId}/transactions` | 200, 400, 401, 403, 404 |
| GET | `/api/merchants/{merchantId}/transactions/{id}` | 200, 400, 401, 403, 404 |
| POST | `/api/merchants/{merchantId}/transactions` | 201, 400, 401, 403, 404, 409 |
| PATCH | `/api/merchants/{merchantId}/transactions/{id}/status` | 200, 400, 401, 403, 404 |
| GET | `/api/merchants/{merchantId}/external-settlements` | 200, 400, 401, 403, 404 |
| GET | `/api/merchants/{merchantId}/external-settlements/{id}` | 200, 400, 401, 403, 404 |
| POST | `/api/merchants/{merchantId}/external-settlements/import` | 201, 400, 401, 403, 404, 409, 413 |
| GET | `/api/merchants/{merchantId}/external-settlements/imports` | 200, 400, 401, 403, 404 |
| GET | `/api/merchants/{merchantId}/external-settlements/imports/{importId}` | 200, 400, 401, 403, 404 |
| GET | `/api/merchants/{merchantId}/bank-statements` | 200, 400, 401, 403, 404 |
| POST | `/api/merchants/{merchantId}/bank-statements/import` | 201, 400, 401, 403, 404, 409, 413 |
| POST | `/api/merchants/{merchantId}/reconciliations` | 202, 400, 401, 403, 404 |
| GET | `/api/merchants/{merchantId}/reconciliations` | 200, 400, 401, 403, 404 |
| GET | `/api/merchants/{merchantId}/reconciliations/{runId}` | 200, 400, 401, 403, 404 |
| GET | `/api/merchants/{merchantId}/reconciliations/{runId}/items` | 200, 400, 401, 403, 404 |
| GET | `/api/merchants/{merchantId}/reconciliations/{runId}/export` | 200, 400, 401, 403, 404 |
| GET | `/api/merchants/{merchantId}/reconciliations/{runId}/discrepancies/{discrepancyId}` | 200, 400, 401, 403, 404 |
| PATCH | `/api/merchants/{merchantId}/reconciliations/{runId}/discrepancies/{discrepancyId}` | 200, 400, 401, 403, 404, 409 |

**Independent Test**: Load `/v3/api-docs` and compare each operation's response codes with the table. For each JSON response, assert an example is present and the error example's `status` equals the code.

---

### P1: Manter o controller legível ⭐ MVP

**User Story**: As a maintainer, I want the OpenAPI text on an interface next to the controller so that the route method stays the HTTP mapping.

**Why P1**: O volume de exemplos foi a razão de não anotar o método.

**Acceptance Criteria**:

1. The system SHALL declare operation summary, description, request examples, and response metadata on an interface that the controller implements, one interface per controller: Auth, Me, User, Merchant, FeeRule, Transaction, ExternalSettlement, BankStatement, and Reconciliation.
2. The system SHALL leave those controllers without `@Operation` on the class methods.

**Independent Test**: Each of the nine controllers implements exactly one `*Api` interface, and `@Operation` appears on the interface rather than on the controller methods.

---

## Edge Cases

- IF an operation returns 204 THEN the system SHALL document the description and omit the response body example.
- IF the import file is the part under test THEN the system SHALL describe a minimal CSV in the operation description, because Try it out cannot preload a file.
- IF a path id in the example does not exist in the database THEN the system SHALL still show a syntactically valid UUID. The description SHALL say that the id comes from the previous create or list response.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| DOCS-01 | P1: Achar a rota e entender o produto | Tasks | Implementing |
| DOCS-02 | P1: Testar a rota com o exemplo preenchido | Tasks | In Tasks |
| DOCS-03 | P1: Ler o que cada código significa | Tasks | In Tasks |
| DOCS-04 | P1: Manter o controller legível | Tasks | In Tasks |

**Coverage:** 4 total, 4 mapped to tasks, 0 unmapped

---

## Success Criteria

- [ ] `/v3/api-docs` lists the nine tags in operational order and the intro names ADMIN and OPERATOR without a numbered call list
- [ ] Every `/api/**` operation has a Portuguese summary, a request example when it has a JSON body, and the status codes in the response table, each described
- [ ] The login example is the dev admin seed, and every JSON request example passes Bean Validation
- [ ] The nine controllers implement a documentation interface and do not carry `@Operation` on their methods
