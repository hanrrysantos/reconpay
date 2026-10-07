# API Docs Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/api-docs/design.md`
**Status**: In Progress

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `AGENTS.md`, `README.md` (Testes), `pom.xml` (JaCoCo 85% linha / 75% ramo no `verify`; `**/openapi/**` excluído), `.github/workflows/ci.yml`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Customizer | unit | Ordem das tags, description, e exemplos de `page`/`size`/`sort`/UUID sem apagar example já posto | `src/test/java/**/openapi/*Test.java` | `./mvnw -B test -Dtest=ClassName` |
| Documento OpenAPI | integration | Cada operação tocada pela tarefa: códigos da tabela da spec, exemplo, security, intro e tags quando a tarefa os altera. Testcontainers | `src/test/java/**/openapi/ApiDocsIntegrationTest.java` | `./mvnw -B verify` |
| Anotação compartilhada / constante de tag | none | Gate de compilação. O efeito entra no documento quando uma interface passa a usá-la | — | `./mvnw -B -DskipTests compile` |
| Interface `*Api` e controller | integration | A tarefa que cria a interface atualiza `ApiDocsIntegrationTest` no mesmo commit | `src/test/java/**/openapi/ApiDocsIntegrationTest.java` | `./mvnw -B verify` |
| Forma das interfaces | unit | Os nove controllers implementam uma `*Api` e o método da classe não tem `@Operation` | `src/test/java/**/openapi/ApiDocumentationInterfacesTest.java` | `./mvnw -B test -Dtest=ApiDocumentationInterfacesTest` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Customizer ou teste de reflexão | `./mvnw -B test -Dtest=ClassName` |
| Full | Documento gerado ou fim de fase com interface | `./mvnw -B verify` |
| Build | Constante ou anotação sem comportamento próprio | `./mvnw -B -DskipTests compile` |

---

## Execution Plan

Fases em ordem. Dentro da fase, uma tarefa por vez.

### Phase 1: Catalog

```
T1 → T2 → T3 → T4 → T5 → T6 → T7 → T8
```

### Phase 2: Auth and session

```
T9 → T10
```

### Phase 3: Users and merchants

```
T11 → T12
```

### Phase 4: Operations

```
T13 → T14 → T15 → T16
```

### Phase 5: Reconciliation

```
T17 → T18
```

---

## Task Breakdown

### Phase 1: Catalog

### T1: Name the tags in flow order

**What**: `OpenApiTags` ganha `BANK_STATEMENTS` e a lista ordenada das nove tags com description em português.
**Where**: `src/main/java/br/com/hanrry/reconpay/openapi/OpenApiTags.java`
**Depends on**: None
**Reuses**: as constantes já existentes
**Requirement**: DOCS-01

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] A lista expõe, nesta ordem: Authentication, Session, Users, Merchants, Fee Rules, Transactions, External Settlements, Bank Statements, Reconciliations
- [x] Cada item tem description não vazia em português
- [x] Gate: `./mvnw -B -DskipTests compile`
- [x] Nenhum teste removido

**Tests**: none
**Gate**: build

**Commit**: `feat(openapi): order tags for the operational flow`

---

### T2: Force tag order on the built document

**What**: `OpenApiTagOrderCustomizer` substitui `openApi.tags` pela lista de T1.
**Where**: `src/main/java/br/com/hanrry/reconpay/openapi/OpenApiTagOrderCustomizer.java`
**Depends on**: T1
**Reuses**: `GlobalOpenApiCustomizer`
**Requirement**: DOCS-01

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] Dado um `OpenAPI` com tags fora de ordem, `customise` grava as nove na ordem de T1, com as descriptions
- [x] O teste de unidade cobre essa substituição
- [x] Gate: `./mvnw -B test -Dtest=OpenApiTagOrderCustomizerTest`
- [x] Nenhum teste removido

**Tests**: unit
**Gate**: quick

**Commit**: `feat(openapi): pin Swagger tag order`

---

### T3: Fill examples for generated parameters

**What**: `ApiDocsParameterCustomizer` põe example em `page` (`0`), `size` (`20`) e `sort` (o `@PageableDefault` do método), e no path UUID que ainda não tem example.
**Where**: `src/main/java/br/com/hanrry/reconpay/openapi/ApiDocsParameterCustomizer.java`
**Depends on**: T2
**Reuses**: `OperationCustomizer` e `default-flat-param-object`
**Requirement**: DOCS-02

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] `page`, `size` e `sort` sem example recebem os valores acima
- [x] Path UUID sem example recebe `3fa85f64-5717-4562-b3fc-2c963f66afa6`
- [x] Example já presente não é substituído
- [x] Gate: `./mvnw -B test -Dtest=ApiDocsParameterCustomizerTest`
- [x] Nenhum teste removido

**Tests**: unit
**Gate**: quick

**Commit**: `feat(openapi): add examples for page and path ids`

---

### T4: Document missing bearer as 401

**What**: Anotação `ApiUnauthenticatedResponse` para 401 `UNAUTHORIZED` com a frase de token ausente ou inválido.
**Where**: `src/main/java/br/com/hanrry/reconpay/openapi/ApiUnauthenticatedResponse.java`
**Depends on**: T3
**Reuses**: `ApiUnauthorizedResponse`
**Requirement**: DOCS-03

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] O exemplo JSON tem `"status": 401` e `"error": "UNAUTHORIZED"`
- [x] A description não diz "credenciais inválidas"
- [x] Gate: `./mvnw -B -DskipTests compile`
- [x] Nenhum teste removido

**Tests**: none
**Gate**: build

**Commit**: `feat(openapi): describe missing bearer as unauthorized`

---

### T5: Document forbidden access

**What**: Anotação `ApiForbiddenResponse` para 403 `FORBIDDEN`.
**Where**: `src/main/java/br/com/hanrry/reconpay/openapi/ApiForbiddenResponse.java`
**Depends on**: T4
**Reuses**: `ApiConflictResponse`
**Requirement**: DOCS-03

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] O exemplo JSON tem `"status": 403` e `"error": "FORBIDDEN"`
- [x] A description em português explica falta de papel ou de grant
- [x] Gate: `./mvnw -B -DskipTests compile`
- [x] Nenhum teste removido

**Tests**: none
**Gate**: build

**Commit**: `feat(openapi): describe forbidden responses`

---

### T6: Document not found

**What**: Anotação `ApiNotFoundResponse` para 404 `NOT_FOUND`.
**Where**: `src/main/java/br/com/hanrry/reconpay/openapi/ApiNotFoundResponse.java`
**Depends on**: T5
**Reuses**: `ApiConflictResponse`
**Requirement**: DOCS-03

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] O exemplo JSON tem `"status": 404` e `"error": "NOT_FOUND"`
- [x] A description em português explica recurso inexistente
- [x] Gate: `./mvnw -B -DskipTests compile`
- [x] Nenhum teste removido

**Tests**: none
**Gate**: build

**Commit**: `feat(openapi): describe not-found responses`

---

### T7: Document oversized CSV

**What**: Anotação `ApiPayloadTooLargeResponse` para 413 `VALIDATION_ERROR` com a frase do limite de 5MB.
**Where**: `src/main/java/br/com/hanrry/reconpay/openapi/ApiPayloadTooLargeResponse.java`
**Depends on**: T6
**Reuses**: `ApiValidationErrorResponse`
**Requirement**: DOCS-03

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] O exemplo JSON tem `"status": 413` e `"error": "VALIDATION_ERROR"`
- [x] A description cita o máximo de 5MB
- [x] Gate: `./mvnw -B -DskipTests compile`
- [x] Nenhum teste removido

**Tests**: none
**Gate**: build

**Commit**: `feat(openapi): describe oversized CSV uploads`

---

### T8: Shorten the Swagger intro

**What**: `OpenApiConfig` troca o roteiro numerado por propósito do produto e pelos papéis ADMIN e OPERATOR. O teste de integração passa a ler `/v3/api-docs`.
**Where**: `src/main/java/br/com/hanrry/reconpay/config/OpenApiConfig.java`
**Depends on**: T7
**Reuses**: o bean `OpenAPI` atual
**Requirement**: DOCS-01

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] `info.description` está em português, nomeia ADMIN e OPERATOR, e não contém uma lista numerada de chamadas
- [x] `ApiDocsIntegrationTest` vê as nove tags na ordem de T1, cada uma com description
- [x] `application.yaml` não define `springdoc.swagger-ui.tags-sorter`
- [x] Gate: `./mvnw -B verify`
- [x] Nenhum teste removido

**Tests**: integration
**Gate**: full

**Commit**: `docs(openapi): shorten the Swagger introduction`

---

### Phase 2: Auth and session

### T9: Document auth with try-it-out examples

**What**: `AuthControllerApi` ganha exemplo de request e o corpo de resposta de cada código. Login usa o seed de dev. Verify HTML fica `text/html`. 204 não tem schema.
**Where**: `src/main/java/br/com/hanrry/reconpay/auth/openapi/AuthControllerApi.java`
**Depends on**: T8
**Reuses**: `ApiValidationErrorResponse`, `ApiUnauthorizedResponse`, `ApiConflictResponse`
**Requirement**: DOCS-02, DOCS-03, DOCS-04

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] POST `/api/auth/login` documenta 200, 400 e 401, com exemplo `admin@reconpay.local` / `DevAdmin@2026` que passa na Bean Validation
- [ ] POST `/api/auth/register` documenta 201, 400 e 409, com exemplo JSON válido
- [ ] POST `/api/auth/verify-email` documenta 204 sem schema e 400
- [ ] GET `/api/auth/verify-email` documenta 200 e 400 como `text/html`, sem `StandardError`
- [ ] As quatro operações saem com security vazio
- [ ] `AuthController` segue sem `@Operation` nos métodos
- [ ] Gate: `./mvnw -B verify`
- [ ] Nenhum teste removido

**Tests**: integration
**Gate**: full

**Commit**: `docs(auth): add Swagger examples for public auth`

---

### T10: Move session docs onto an interface

**What**: `MeControllerApi` recebe o que hoje está em `MeController`. GET `/api/me/merchants` documenta 400 e 401 de token ausente.
**Where**: `src/main/java/br/com/hanrry/reconpay/auth/openapi/MeControllerApi.java`
**Depends on**: T9
**Reuses**: `MeController` e `ApiUnauthenticatedResponse`
**Requirement**: DOCS-02, DOCS-03, DOCS-04

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] GET `/api/me` documenta 200 e 401, com exemplo JSON de sucesso, e exige `Bearer Authentication`
- [ ] GET `/api/me/merchants` documenta 200, 400 e 401, com example em `page`, `size` e `sort`
- [ ] A description de merchants cita grant do OPERATOR e a visão total do ADMIN
- [ ] `MeController` implementa a interface e não declara `@Operation` nem `@GetMapping`
- [ ] Gate: `./mvnw -B verify`
- [ ] Nenhum teste removido

**Tests**: integration
**Gate**: full

**Commit**: `docs(auth): document the session endpoints`

---

### Phase 3: Users and merchants

### T11: Document user administration

**What**: `UserControllerApi` cobre as nove rotas de `/api/users` com os códigos da tabela da spec.
**Where**: `src/main/java/br/com/hanrry/reconpay/auth/openapi/UserControllerApi.java`
**Depends on**: T10
**Reuses**: `UserController`, `ApiForbiddenResponse`, `ApiNotFoundResponse`
**Requirement**: DOCS-02, DOCS-03, DOCS-04

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] Cada rota de usuário documenta exatamente os códigos da tabela da spec, com description em português
- [ ] Todo request JSON tem um exemplo que passa na Bean Validation
- [ ] Todo response JSON tem exemplo; o de erro traz `status` e `error` iguais ao código
- [ ] DELETE documenta 204 sem schema
- [ ] As rotas exigem `Bearer Authentication`
- [ ] `UserController` implementa a interface e não declara `@Operation` nem mapping
- [ ] Gate: `./mvnw -B verify`
- [ ] Nenhum teste removido

**Tests**: integration
**Gate**: full

**Commit**: `docs(auth): document user administration`

---

### T12: Document merchants

**What**: `MerchantControllerApi` cobre as cinco rotas de `/api/merchants`.
**Where**: `src/main/java/br/com/hanrry/reconpay/merchant/openapi/MerchantControllerApi.java`
**Depends on**: T11
**Reuses**: `MerchantController`
**Requirement**: DOCS-02, DOCS-03, DOCS-04

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] POST documenta 201, 400, 401, 403 e 409. A description cita o auto-grant de quem cria
- [ ] GET lista documenta 200, 400, 401 e 403
- [ ] GET, PUT e DELETE por id documentam 404. DELETE é 204 sem schema
- [ ] Exemplos JSON de entrada passam na Bean Validation e path id usa o UUID do design
- [ ] `MerchantController` implementa a interface e não declara `@Operation` nem mapping
- [ ] Gate: `./mvnw -B verify`
- [ ] Nenhum teste removido

**Tests**: integration
**Gate**: full

**Commit**: `docs(merchant): document merchant endpoints`

---

### Phase 4: Operations

### T13: Document fee rules

**What**: `FeeRuleControllerApi` cobre as cinco rotas de fee rules, incluindo 404 quando o merchant não existe e 409 na regra repetida.
**Where**: `src/main/java/br/com/hanrry/reconpay/feerule/openapi/FeeRuleControllerApi.java`
**Depends on**: T12
**Reuses**: `FeeRuleController`
**Requirement**: DOCS-02, DOCS-03, DOCS-04

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] Os códigos batem com a tabela: listas e criação incluem 404; POST e PUT incluem 409; DELETE é 204 sem schema
- [ ] Exemplos JSON de entrada passam na Bean Validation
- [ ] `FeeRuleController` implementa a interface e não declara `@Operation` nem mapping
- [ ] Gate: `./mvnw -B verify`
- [ ] Nenhum teste removido

**Tests**: integration
**Gate**: full

**Commit**: `docs(feerule): document fee rule endpoints`

---

### T14: Document transactions

**What**: `TransactionControllerApi` cobre lista, detalhe, criação e troca de status.
**Where**: `src/main/java/br/com/hanrry/reconpay/transaction/openapi/TransactionControllerApi.java`
**Depends on**: T13
**Reuses**: `TransactionController`
**Requirement**: DOCS-02, DOCS-03, DOCS-04

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] POST documenta 201, 400, 401, 403, 404 e 409. A description do 409 cita referência repetida e taxa ativa ausente
- [ ] PATCH de status documenta 200, 400, 401, 403 e 404
- [ ] Listas e detalhe seguem a tabela da spec
- [ ] Exemplos JSON de entrada passam na Bean Validation
- [ ] `TransactionController` implementa a interface e não declara `@Operation` nem mapping
- [ ] Gate: `./mvnw -B verify`
- [ ] Nenhum teste removido

**Tests**: integration
**Gate**: full

**Commit**: `docs(transaction): document transaction endpoints`

---

### T15: Document settlement import

**What**: `ExternalSettlementControllerApi` cobre lista, detalhe, imports e o upload, com CSV mínimo `RECONPAY` na description.
**Where**: `src/main/java/br/com/hanrry/reconpay/externalsettlement/openapi/ExternalSettlementControllerApi.java`
**Depends on**: T14
**Reuses**: `ExternalSettlementController` e `SettlementLayout.RECONPAY`
**Requirement**: DOCS-02, DOCS-03, DOCS-04

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] POST import documenta 201, 400, 401, 403, 404, 409 e 413
- [ ] A description contém um CSV mínimo com o cabeçalho `externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate` e o example de `layout` é `RECONPAY`
- [ ] O 400 do import exemplifica `details.rowErrors`. O 409 exemplifica `details.conflictingReferences`
- [ ] A parte multipart se chama `file`
- [ ] As outras quatro rotas seguem a tabela da spec
- [ ] `ExternalSettlementController` implementa a interface e não declara `@Operation` nem mapping
- [ ] Gate: `./mvnw -B verify`
- [ ] Nenhum teste removido

**Tests**: integration
**Gate**: full

**Commit**: `docs(externalsettlement): document settlement import`

---

### T16: Document bank statements

**What**: `BankStatementControllerApi` cobre a lista e o import do extrato, com o CSV mínimo do parser.
**Where**: `src/main/java/br/com/hanrry/reconpay/bankstatement/openapi/BankStatementControllerApi.java`
**Depends on**: T15
**Reuses**: `BankStatementController` e o cabeçalho de `BankStatementCsvParser`
**Requirement**: DOCS-02, DOCS-03, DOCS-04

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] GET documenta 200, 400, 401, 403 e 404, com example em `importId` quando o parâmetro aparece
- [ ] POST import documenta 201, 400, 401, 403, 404, 409 e 413, parte `file`, CSV mínimo `lineReference,externalReference,amount,movementDate`, `rowErrors` no 400 e `conflictingReferences` no 409
- [ ] A tag da operação é `Bank Statements`
- [ ] `BankStatementController` implementa a interface e não declara `@Operation` nem mapping
- [ ] Gate: `./mvnw -B verify`
- [ ] Nenhum teste removido

**Tests**: integration
**Gate**: full

**Commit**: `docs(bankstatement): document bank statement import`

---

### Phase 5: Reconciliation

### T17: Document reconciliation runs

**What**: `ReconciliationControllerApi` cobre run, listas, detalhe, export CSV e o PATCH da divergência.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/openapi/ReconciliationControllerApi.java`
**Depends on**: T16
**Reuses**: `ReconciliationController` e `ReconciliationCsvExporter.HEADER`
**Requirement**: DOCS-02, DOCS-03, DOCS-04

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] POST do run documenta 202, 400, 401, 403 e 404, com exemplo de janela que passa na Bean Validation
- [ ] GET export documenta 200 `text/csv` cujo exemplo é a linha de cabeçalho do exporter, mais 400, 401, 403 e 404
- [ ] PATCH da divergência documenta 200, 400, 401, 403, 404 e 409
- [ ] As outras rotas seguem a tabela da spec
- [ ] `ReconciliationController` implementa a interface e não declara `@Operation` nem mapping
- [ ] Gate: `./mvnw -B verify`
- [ ] Nenhum teste removido

**Tests**: integration
**Gate**: full

**Commit**: `docs(reconciliation): document reconciliation endpoints`

---

### T18: Lock controllers to documentation interfaces

**What**: Teste de reflexão exige que os nove controllers implementem uma interface `*Api` e que `@Operation` não esteja nos métodos da classe.
**Where**: `src/test/java/br/com/hanrry/reconpay/openapi/ApiDocumentationInterfacesTest.java`
**Depends on**: T17
**Reuses**: os nove controllers
**Requirement**: DOCS-04

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] O teste falha se algum dos nove controllers deixar de implementar interface cujo nome termina em `Api`
- [ ] O teste falha se um método declarado na classe do controller tiver `@Operation`
- [ ] Gate: `./mvnw -B test -Dtest=ApiDocumentationInterfacesTest`
- [ ] Nenhum teste removido

**Tests**: unit
**Gate**: quick

**Commit**: `test(openapi): assert controllers implement documentation interfaces`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5

Phase 1: T1 → T2 → T3 → T4 → T5 → T6 → T7 → T8
Phase 2: T9 → T10
Phase 3: T11 → T12
Phase 4: T13 → T14 → T15 → T16
Phase 5: T17 → T18
```

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: Name the tags in flow order | 1 arquivo | ✅ Granular |
| T2: Force tag order on the built document | 1 customizer + teste no mesmo commit | ✅ Granular |
| T3: Fill examples for generated parameters | 1 customizer + teste no mesmo commit | ✅ Granular |
| T4: Document missing bearer as 401 | 1 anotação | ✅ Granular |
| T5: Document forbidden access | 1 anotação | ✅ Granular |
| T6: Document not found | 1 anotação | ✅ Granular |
| T7: Document oversized CSV | 1 anotação | ✅ Granular |
| T8: Shorten the Swagger intro | 1 bean + asserção no teste de documento | ✅ Granular |
| T9: Document auth with try-it-out examples | 1 interface | ✅ Granular |
| T10: Move session docs onto an interface | 1 interface | ✅ Granular |
| T11: Document user administration | 1 interface | ✅ Granular |
| T12: Document merchants | 1 interface | ✅ Granular |
| T13: Document fee rules | 1 interface | ✅ Granular |
| T14: Document transactions | 1 interface | ✅ Granular |
| T15: Document settlement import | 1 interface | ✅ Granular |
| T16: Document bank statements | 1 interface | ✅ Granular |
| T17: Document reconciliation runs | 1 interface | ✅ Granular |
| T18: Lock controllers to documentation interfaces | 1 teste | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | início da fase 1 | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | T4 | T4 → T5 | ✅ Match |
| T6 | T5 | T5 → T6 | ✅ Match |
| T7 | T6 | T6 → T7 | ✅ Match |
| T8 | T7 | T7 → T8 | ✅ Match |
| T9 | T8 | T8 é a fase anterior; na fase 2, T9 inicia | ✅ Match |
| T10 | T9 | T9 → T10 | ✅ Match |
| T11 | T10 | T10 é a fase anterior; na fase 3, T11 inicia | ✅ Match |
| T12 | T11 | T11 → T12 | ✅ Match |
| T13 | T12 | T12 é a fase anterior; na fase 4, T13 inicia | ✅ Match |
| T14 | T13 | T13 → T14 | ✅ Match |
| T15 | T14 | T14 → T15 | ✅ Match |
| T16 | T15 | T15 → T16 | ✅ Match |
| T17 | T16 | T16 é a fase anterior; na fase 5, T17 inicia | ✅ Match |
| T18 | T17 | T17 → T18 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1 | Anotação compartilhada / constante de tag | none | none | ✅ OK |
| T2 | Customizer | unit | unit | ✅ OK |
| T3 | Customizer | unit | unit | ✅ OK |
| T4 | Anotação compartilhada / constante de tag | none | none | ✅ OK |
| T5 | Anotação compartilhada / constante de tag | none | none | ✅ OK |
| T6 | Anotação compartilhada / constante de tag | none | none | ✅ OK |
| T7 | Anotação compartilhada / constante de tag | none | none | ✅ OK |
| T8 | Documento OpenAPI | integration | integration | ✅ OK |
| T9 | Interface `*Api` e controller | integration | integration | ✅ OK |
| T10 | Interface `*Api` e controller | integration | integration | ✅ OK |
| T11 | Interface `*Api` e controller | integration | integration | ✅ OK |
| T12 | Interface `*Api` e controller | integration | integration | ✅ OK |
| T13 | Interface `*Api` e controller | integration | integration | ✅ OK |
| T14 | Interface `*Api` e controller | integration | integration | ✅ OK |
| T15 | Interface `*Api` e controller | integration | integration | ✅ OK |
| T16 | Interface `*Api` e controller | integration | integration | ✅ OK |
| T17 | Interface `*Api` e controller | integration | integration | ✅ OK |
| T18 | Forma das interfaces | unit | unit | ✅ OK |
