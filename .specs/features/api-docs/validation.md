# API Docs Validation

**Date**: 2026-10-07
**Spec**: `.specs/features/api-docs/spec.md`
**Diff range**: `586b455..HEAD` (`89ac931`..`e43516d`)
**Verifier**: independent sub-agent (author ≠ verifier)

## Validation: PASS

Every acceptance criterion maps to an assertion of the spec outcome. The full gate is green. The three behavior mutants were killed.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 | ✅ Done | Nine tags in operational order |
| T2 | ✅ Done | Customizer replaces tags; unit test locks order |
| T3 | ✅ Done | `page`/`size`/`sort` and path UUID examples |
| T4 | ✅ Done | GET `/api/me` 401 is the missing-bearer phrase, not login credentials |
| T5 | ✅ Done | 403 example is `FORBIDDEN` |
| T6 | ✅ Done | 404 example is `NOT_FOUND` |
| T7 | ✅ Done | 413 example is `VALIDATION_ERROR` |
| T8 | ✅ Done | Intro, tag order on `/v3/api-docs`, and no `tags-sorter` |
| T9 | ✅ Done | Login seed, codes, empty security, HTML, 204 without schema or example, login 401 wording |
| T10 | ✅ Done | Session codes, bearer, and `page`/`size`/`sort` examples |
| T11 | ✅ Done | Nine user routes match the response table |
| T12 | ✅ Done | Five merchant routes match the response table |
| T13 | ✅ Done | Five fee-rule routes match the response table |
| T14 | ✅ Done | Four transaction routes match the response table; 409 text is asserted |
| T15 | ✅ Done | Settlement codes, CSV header, `file`, `RECONPAY`, `rowErrors`, `conflictingReferences` |
| T16 | ✅ Done | Bank-statement codes, CSV header, `file`, tag name |
| T17 | ✅ Done | Reconciliation codes and export header column order |
| T18 | ✅ Done | Reflection test locks nine `*Api` interfaces and no `@Operation` on controller methods |

T1–T18 are checked done in `tasks.md`. None are blocked or partial.

---

## Spec-Anchored Acceptance Criteria

### P1: Achar a rota e entender o produto

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| WHEN a client gets `/v3/api-docs` THEN the system SHALL list the tags in this order, each with a non-empty Portuguese description: Authentication, Session, Users, Merchants, Fee Rules, Transactions, External Settlements, Bank Statements, Reconciliations | Exact order of those nine names. Each description is non-empty and matches the Portuguese accent pattern | Order: `src/test/java/br/com/hanrry/reconpay/openapi/ApiDocsIntegrationTest.java:109` - `assertThat(tag.get("name").asText()).isEqualTo(TAG_ORDER.get(index))` with `TAG_ORDER` at lines 63–73. Non-empty: `:110` - `isNotBlank()`. Portuguese: `:111` - `containsPattern(PORTUGUESE)` where `PORTUGUESE` is `[áàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ]` at `:75`. Same order on the customizer: `OpenApiTagOrderCustomizerTest.java:34` - `containsExactlyElementsOf(OPERATIONAL_ORDER)` | ✅ PASS |
| The system SHALL set `info.description` in Portuguese, name the roles ADMIN and OPERATOR, and omit any numbered list of endpoint calls | Portuguese intro naming `ADMIN` and `OPERATOR`, with no numbered `GET`/`POST`/`PUT`/`PATCH`/`DELETE` list | `ApiDocsIntegrationTest.java:115` - `contains("ADMIN")`; `:116` - `contains("OPERATOR")`; `:117` - `containsPattern("[áàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ]")`; `:118` - `doesNotContainPattern("\\d+\\.\\s*(GET\|POST\|PUT\|PATCH\|DELETE)\\b")` | ✅ PASS |
| The system SHALL keep Swagger UI from sorting those tags alphabetically | Do not set alphabetical tag sorting | `ApiDocsIntegrationTest.java:126` - `assertThat(yaml).doesNotContain("tags-sorter")` on classpath `application.yaml` | ✅ PASS |

### P1: Testar a rota com o exemplo preenchido

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| WHEN a client gets `/v3/api-docs` THEN the system SHALL give every `/api/**` operation a non-empty Portuguese `summary` and `description` | Portuguese summary and description on all 43 operations in the response table | `ApiDocsIntegrationTest.java:696` - `assertThat(operation.path("summary").asText()).containsPattern(PORTUGUESE)`; `:697` - same on `description`. `assertOperation` is called once per table row | ✅ PASS |
| WHEN an operation has a JSON request body THEN the system SHALL publish one request example that contains every required property and that passes that DTO's Bean Validation | Exactly one JSON example; every `@NotNull` / `@NotBlank` / `@NotEmpty` property present; `Validator` reports no violations | `ApiDocsIntegrationTest.java:841` - `assertThat(examples).hasSize(1)`; `:848` - `assertThat(example.has(component.getName())).isTrue()`; `:853` - `assertThat(validator.validate(dto)).isEmpty()` | ✅ PASS |
| WHEN the operation is POST `/api/auth/login` THEN the system SHALL use the request example email `admin@reconpay.local` and password `DevAdmin@2026` | Those two literal values | `ApiDocsIntegrationTest.java:140` supplies `Map.of("email", "admin@reconpay.local", "password", "DevAdmin@2026")`; `:745` - `assertThat(example.path(name).asText()).isEqualTo(value)` | ✅ PASS |
| The system SHALL publish a UUID example on every path parameter and an example on every query parameter | Path example `3fa85f64-5717-4562-b3fc-2c963f66afa6`; every query parameter has a non-empty example | `ApiDocsIntegrationTest.java:757` - `assertThat(example).isEqualTo(UUID_EXAMPLE)` for `in=path`; `:759` - `assertThat(example).isNotBlank()` for `in=query`. Constant at `:76` | ✅ PASS |
| WHEN the operation is POST settlement import or POST bank-statement import THEN the system SHALL document multipart part `file` and include a minimal valid CSV in the description. Settlement `layout` SHALL be `RECONPAY` | Part name `file`; settlement header `externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate`; bank header `lineReference,externalReference,amount,movementDate`; layout example `RECONPAY` | Settlement: `:515` description `contains` that header via `:699`; `:518` / `:944` - `assertThat(actual).containsExactly("RECONPAY")`; `:909` - `schema.properties.file` is present; `:910` - `required` contains `file`. Bank: `:577` description `contains` `lineReference,externalReference,amount,movementDate`; `:580` calls `assertFilePart` | ✅ PASS |
| The system SHALL publish the four `/api/auth` operations with an empty security requirement and every other `/api/**` operation with the scheme `Bearer Authentication` | Auth security is an empty array. Every other operation's security names `Bearer Authentication` | Auth (`bearer=false`): `ApiDocsIntegrationTest.java:738` - `assertThat(security).isEmpty()` after `:736`–`:737` require a non-null array. Others: `:77` `BEARER = "Bearer Authentication"`; `:835` - `assertThat(found).isTrue()` when `requirement.has(BEARER)` | ✅ PASS |

### P1: Ler o que cada código significa

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| WHEN a client gets `/v3/api-docs` THEN the system SHALL document, for each `/api/**` operation, exactly the success status and the error statuses from the response table, each with a non-empty Portuguese description | Exact code set per row of the 43-row table. Each description is non-empty, matches the Portuguese accent pattern, and is not the English HTTP reason phrase. POST `/api/auth/login` 401 contains `credenciais inválidas` and does not contain `token Bearer ausente`. GET `/api/me` 401 contains `token Bearer ausente ou inválido` and does not contain `credenciais inválidas` | Codes: `ApiDocsIntegrationTest.java:703` - `assertThat(fieldNames(responses)).containsExactlyInAnyOrderElementsOf(op.codes)`, with each set at `:137`, `:151`, `:165`, `:179`, `:201`, `:215`, `:241`, `:247`, `:256`, `:262`, `:269`, `:275`, `:281`, `:287`, `:293`, `:318`, `:325`, `:334`, `:340`, `:346`, `:374`, `:383`, `:389`, `:396`, `:403`, `:432`, `:445`, `:451`, `:458`, `:492`, `:506`, `:512`, `:524`, `:532`, `:563`, `:574`, `:618`, `:625`, `:634`, `:640`, `:650`, `:658`, `:664`. Descriptions: `:707` - `isNotBlank()`; `:708` - `containsPattern(PORTUGUESE)`; `:709` - `isNotEqualTo(HttpStatus reason phrase)`. Login 401: `:711` - `contains("credenciais inválidas")`; `:712` - `doesNotContain("token Bearer ausente")`. Session 401: `:715` - `contains("token Bearer ausente ou inválido")`; `:716` - `doesNotContain("credenciais inválidas")` | ✅ PASS |
| WHEN a documented response has a JSON body THEN the system SHALL include one example whose `status` and `error` match that response for `StandardError`, and one example of the success DTO for a JSON success body | Error example `status` equals the code and `error` is `VALIDATION_ERROR` (400 and 413), `UNAUTHORIZED` (401), `FORBIDDEN` (403), `NOT_FOUND` (404), or `CONFLICT` (409). Success example is a non-null DTO (or the first page element) | `ApiDocsIntegrationTest.java:808` - `assertThat(example.path("status").asInt()).isEqualTo(Integer.parseInt(code))`; `:809` - `assertThat(example.path("error").asText()).isEqualTo(ERROR_CODES.get(code))` with the map at `:78`–`:85`. Success: `:816` - `assertThat(examples).isNotEmpty()`; `:820`–`:821` - each success field `has(field)` and is non-null | ✅ PASS |
| IF the success status is 204 THEN the system SHALL document the description and no response schema | Description present; no schema on 204 for POST `/api/auth/verify-email`, DELETE `/api/users/{id}`, DELETE `/api/merchants/{id}`, DELETE fee-rule by id | `ApiDocsIntegrationTest.java:707` - description `isNotBlank()`; `:172`, `:295`, `:347`, `:405` pass `noSchemaCodes = Set.of("204")`; `:793` - `assertThat(media.has("schema")).isFalse()` | ✅ PASS |
| WHEN the operation is GET `/api/auth/verify-email` THEN the system SHALL document 200 and 400 as `text/html` with a description and without a `StandardError` schema | Both codes are only `text/html` and the media node does not name `StandardError` | `ApiDocsIntegrationTest.java:187` sets `htmlCodes` to `200` and `400`; `:782` - `containsExactly("text/html")`; `:783` - `doesNotContain("StandardError")` | ✅ PASS |
| WHEN the operation is GET reconciliation export THEN the system SHALL document 200 as `text/csv` with the CSV header line as the example | `text/csv` example equals `externalReference,result,discrepancyTypes,internalTransactionId,externalSettlementId,transactionAmount,expectedNetAmount,settlementAmount,settlementNetAmount,paymentMethod,installments,transactionStatus,settlementStatus,transactionDate,settlementDate` | `ApiDocsIntegrationTest.java:614` stores that header; `:766` requires `content.text/csv`; `:776` - `assertThat(example).isEqualTo(header)`. Column order matches `ReconciliationCsvExporter` `HEADER` | ✅ PASS |

### P1: Manter o controller legível

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| The system SHALL declare operation metadata on an interface that the controller implements, one interface per controller: Auth, Me, User, Merchant, FeeRule, Transaction, ExternalSettlement, BankStatement, and Reconciliation | Those nine controllers, each implementing exactly one interface whose name ends with `Api` | `src/test/java/br/com/hanrry/reconpay/openapi/ApiDocumentationInterfacesTest.java:37` - `assertThat(CONTROLLERS).hasSize(9)` (list at `:23`–`:33`); `:44` - `assertThat(apiInterfaces).isEqualTo(1)` | ✅ PASS |
| The system SHALL leave those controllers without `@Operation` on the class methods | No declared controller method carries `@Operation` | `ApiDocumentationInterfacesTest.java:52` - `assertThat(method.isAnnotationPresent(Operation.class)).isFalse()`. Mapping annotations are also absent: `ApiDocsIntegrationTest.java:971` - `isAnnotationPresent(annotation)` is false for `@Operation` and the HTTP mappings | ✅ PASS |

**Status**: ✅ All ACs covered. 16/16 acceptance criteria match the spec outcome.

---

## Discrimination Sensor

Scratch worktree `/tmp/reconpay-api-docs-sensor` at `e43516d`. Mutations stayed in that worktree. No `git stash`. The real worktree was not edited. `git status --porcelain` before the worktree and after `git worktree remove --force` was:

```
 M .specs/LESSONS.md
 M .specs/lessons.json
?? .specs/features/api-docs/validation.md
```

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ----------- | ------- |
| 1 | `src/main/java/br/com/hanrry/reconpay/openapi/ApiUnauthorizedResponse.java:20` | Login 401 description `Não autenticado: credenciais inválidas` changed to `Não autenticado: senha recusada` | ✅ Killed by `publicAuthDocumentsCodesExamplesAndOpenSecurity`. Actual was `Não autenticado: senha recusada`; expected to contain `credenciais inválidas` at `ApiDocsIntegrationTest.java:711`. Tests run: 11, failures: 1 |
| 2 | `src/main/java/br/com/hanrry/reconpay/openapi/OpenApiTags.java:20` | Dropped the accent from the Authentication tag: `verificação` became `verificacao` | ✅ Killed by `apiDocsListsOperationalTagsAndNamesBothRoles`. Actual `Login, cadastro e verificacao de e-mail, sem token.` did not match `PORTUGUESE` at `ApiDocsIntegrationTest.java:111`. Tests run: 11, failures: 1 |
| 3 | `src/main/java/br/com/hanrry/reconpay/auth/openapi/AuthControllerApi.java:145` | POST `/api/auth/verify-email` 204 gained `content` with `examples = @ExampleObject(name = "vazio", value = "{}")` | ✅ Killed by `publicAuthDocumentsCodesExamplesAndOpenSecurity`. `assertNoSchema` expected `has("example") \|\| has("examples")` to be false and it was true at `ApiDocsIntegrationTest.java:798`. Tests run: 11, failures: 1 |

**Sensor depth**: lightweight (3 behavior-level mutations)
**Result**: 3/3 killed - PASS

---

## Interactive UAT Results (if performed)

| # | Test | Result | Details |
| - | ---- | ------ | ------- |
| 1 | Swagger UI session | ⏭️ Skip | API-document verification. No UI session. The contract is checked on `/v3/api-docs` |

---

## Code Quality

Checked against `.cursor/skills/tlc-spec-driven/references/coding-principles.md` and `AGENTS.md`. Diff surface is the OpenAPI interfaces, response annotations, two customizers, `OpenApiConfig`, controller mapping moves onto those interfaces, and the openapi tests. No `// SPEC_DEVIATION`. No test deleted or weakened. JaCoCo on `./mvnw -B verify` reported that all coverage checks were met (`**/openapi/**` stays excluded, as the task matrix already stated).

| Principle | Status |
| --------- | ------ |
| Minimum code | ✅ |
| Surgical changes | ✅ |
| No scope creep | ✅ |
| Matches patterns | ✅ |
| Spec-anchored outcome check (asserted values match spec) | ✅ |
| Per-layer Coverage Expectation met (domain 1:1 ACs; routes happy+edge+error) | ✅ |
| Every test maps to a spec requirement - no unclaimed tests | ✅ |
| Documented guidelines followed: `coding-principles.md`, `AGENTS.md` | ✅ |

---

## Edge Cases

- [x] IF an operation returns 204 THEN document the description and omit the response body example. Description is `isNotBlank()` at `ApiDocsIntegrationTest.java:707`. `assertNoSchema` rejects a schema (`:793`) and rejects `example` or `examples` (`:796`–`:798`) on every media entry. The four 204 codes are `noSchemaCodes` at `:172`, `:295`, `:347`, and `:405`. Mutant 3, which added `examples` on the verify-email 204, failed at `:798`.
- [x] IF the import file is the part under test THEN describe a minimal CSV in the operation description. Settlement and bank descriptions contain `CSV mínimo` and the parser headers (`:515`, `:577`, asserted at `:699`). The part name is `file` (`:909`–`:910`).
- [x] IF a path id in the example does not exist THEN still show UUID `3fa85f64-5717-4562-b3fc-2c963f66afa6`, and the description says the id comes from the previous create or list response. Path examples are equal to that UUID (`:757`). Operations with a path id require the description to contain `criação` and `listagem` (`:237` and `:699`).

---

## Gate Check

- **Gate command**: `./mvnw -B verify`
- **Result**: 334 passed, 0 failed, 0 skipped
- **Test count before feature**: 316
- **Test count after feature**: 334
- **Delta**: +18
- **Skipped tests**: none
- **Failures**: none

`@Test` methods are 292 at `586b455` and 310 at `HEAD`. Those 18 methods are the new openapi tests. Surefire reported 334 runs and 0 skipped. The 24 runs above the 310 `@Test` methods are pre-existing parameterized expansions. Before = 334 − 18 = 316. No test file was deleted in `586b455..HEAD`.

---

## Fix Plans (if issues found)

None.

---

## Requirement Traceability Update

`spec.md` was not modified. Statuses below belong only to this report.

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| DOCS-01 | Implementing | ✅ Verified |
| DOCS-02 | Implementing | ✅ Verified |
| DOCS-03 | Implementing | ✅ Verified |
| DOCS-04 | Implementing | ✅ Verified |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 16/16 ACs matched the spec outcome
**Sensor**: 3/3 mutations killed
**Gate**: 334 passed, 0 failed, 0 skipped

**What works**: Tag order and Portuguese tag descriptions, intro roles without a numbered call list, login seed, path UUID, empty auth security, `Bearer Authentication` on the other routes, the 43-row status table, Portuguese response descriptions, login 401 versus missing-bearer 401, JSON error `status`/`error`, 204 without a schema or example, HTML verify-email without `StandardError`, export CSV column order, both import CSV headers, and layout `RECONPAY`.

**Issues found**: none

**Next steps**: none. This run adds no lesson. The earlier failure already recorded the wording and accent gaps, and this pass leaves no new surviving mutant or spec deviation.
