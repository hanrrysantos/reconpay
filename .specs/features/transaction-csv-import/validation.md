# Import CSV de Transações Validation

**Date**: 2026-10-16
**Spec**: `.specs/features/transaction-csv-import/spec.md`
**Diff range**: `f808eb2..10b9679` (spec/design `cbd5e1c`, `7c8a77b`; implementation `424b818..af2c5e1`; iteration reports `86821c0`, `068fa3a`, `dbcda41`, `6eeae68`; test-only fixes `f248e0d`, `7ab6276`, `4bc5ebe`, `f1e3ac4`, `10b9679`), branch `feat/transaction-csv-import`
**Verifier**: independent sub-agent (author ≠ verifier), iteration 5. The normal bound is 3. The user explicitly authorized iterations 4 and 5.

## Validation: transaction-csv-import - PASS ✅

The three iteration-4 gaps are closed. W7 (renamed `merchantId` on `GET /transactions/imports/{importId}`) is killed by both parts of the new `MerchantRouteAccessIntegrationTest`. W4 (header columns in any order) and W8 (lower-case status) are killed by `10b9679`. The same rename applied to a route of another module (`ExternalSettlementController`, GET and POST) is killed too, so the route-access check covers the whole project, not just this feature. The earlier survivors M1 (iteration 1), N19 (iteration 2) and V11 (iteration 3) are still killed. I ran 11 new behavior-level mutants on paths not touched in iterations 1-4, and all 11 were killed. The gate passes: 630 tests, 0 failures, JaCoCo met.

Since iteration 4 no production code changed (`git diff 4bc5ebe 10b9679 -- backend/src/main` is empty). `f1e3ac4` adds `security/integration/MerchantRouteAccessIntegrationTest.java` (2 tests). `10b9679` adds 3 lines at `PT:107-109` and 1 case at `PT:144`. `IT`, `ST`, `RT` and `CT` are unchanged, so their citations from iteration 4 still hold. `PT` citations below were taken again from `10b9679` (old lines 107-140 moved +3, old lines 141 and after moved +4).

File abbreviations (all under `backend/src/test/java/br/com/hanrry/reconpay/`):

- `PT` = `transaction/service/TransactionCsvParserTest.java`
- `ST` = `transaction/service/TransactionImportServiceTest.java`
- `IT` = `transaction/integration/TransactionImportIntegrationTest.java`
- `RT` = `transaction/integration/TransactionImportRepositoryIntegrationTest.java`
- `CT` = `feerule/service/ExpectedNetAmountCalculatorTest.java`
- `MRA` = `security/integration/MerchantRouteAccessIntegrationTest.java`

---

## Gap History

| Iteration | Commit verified | Gaps | Outcome |
| --- | --- | --- | --- |
| 1 | `af2c5e1` | G1 (M1): accepted side of the 17-integer-digit bound not tested. G2 (M6): a row with both the length error and a duplicate not tested | FAIL; fix routed |
| fix | `f248e0d` | Test only: amount at the 17-digit bound accepted; a 101-char reference repeated gives both errors | - |
| 2 | `f248e0d` | G1 ✅ closed. G2 ✅ closed. New G3 (N19): default page size of the batch list not tested | FAIL; fix routed |
| fix | `7ab6276` | Test only: `jsonPath("$.size").value(20)` | - |
| 3 | `7ab6276` | G3 ✅ closed. New G4 (V15) sort key, G5 (V11) inactive rule, G6 (V16) header case, G7 (V7) extension dot | FAIL; iteration limit (3) reached, escalated |
| user decision | - | The user authorized a 4th fix and re-verify round | - |
| fix | `4bc5ebe` | Test only: three batches asserted `a, c, b`; deactivated PIX rule → `400`; header differing only in case refused; `vendascsv` refused | - |
| 4 | `4bc5ebe` | G4 ✅, G5 ✅, G6 ✅, G7 ✅ closed. New G8 (W7) no 403 on batch-by-id, G9 (W4) permuted header, G10 (W8) lower-case status | FAIL; beyond the authorized extra round → back to the user |
| user decision | - | The user authorized a 5th fix and re-verify round | - |
| fix | `f1e3ac4`, `10b9679` | Test only: project-wide route-access test (static check of the `merchantId` parameter + `403` on every merchant GET route); reordered header refused; `approved` refused | - |
| 5 | `10b9679` | G8 ✅, G9 ✅, G10 ✅ closed (W7, W4, W8 killed; W7-like renames R1, R2 in another module killed). M1, N19, V11 still killed. 11/11 new mutants killed. No new gap | **PASS** |

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 ExpectedNetAmountCalculator | ✅ Done | `424b818` |
| T2 Lote no banco | ✅ Done | `23a5ac4`; `V24__create_transaction_imports.sql` |
| T3 TransactionCsvParser | ✅ Done | `e8fccfc` |
| T4 TransactionImportService + exceptions + handlers | ✅ Done | `c5981fd` |
| T5 Endpoints | ✅ Done | `037d0ff` |
| T6 README | ✅ Done | `af2c5e1` |
| Fix G1/G2 | ✅ Done | `f248e0d` (tests only) |
| Fix G3 | ✅ Done | `7ab6276` (tests only) |
| Fix G4-G7 | ✅ Done | `4bc5ebe` (tests only) |
| Fix G8-G10 | ✅ Done | `f1e3ac4`, `10b9679` (tests only, no production code changed) |

---

## Re-check of earlier survivors

| Mutant | Re-applied at | Mutation | Killed by | Result |
| --- | --- | --- | --- | --- |
| W7 (iter. 4) | `TransactionController.java:99` | `@PathVariable UUID merchantId` → `@PathVariable("merchantId") UUID ownerId` | `MRA` `everyMerchantScopedRouteHandsTheAspectAUuidParameterNamedMerchantId` **and** `everyGetRouteOfAMerchantAnswersForbiddenToAnOperatorWithoutAccess` | ✅ Killed (both parts) |
| W4 (iter. 4) | `TransactionCsvParser.java:95` | header cell checked by `Arrays.asList(HEADER).contains` | `PT:107-109` `rejectsAWrongHeader` (`amount,externalReference,...` → `hasMessage("Cabeçalho CSV inválido. Esperado: " + HEADER)`) | ✅ Killed |
| W8 (iter. 4) | `TransactionCsvParser.java:193` | `TransactionStatus.valueOf(raw.toUpperCase())` | `PT:144` `reportsEachRowErrorWithItsLineAndText` [status minúsculo] → `containsExactly(new ImportRowErrorDTO(3, "Status inválido"))` (`PT:152`) | ✅ Killed |
| V11 (iter. 3) | `IFeeRuleRepository.java:17` | `@Query` without `active` filter | `IT:258-276` `aDeactivatedFeeRuleDoesNotPriceTheImport` | ✅ Killed |
| N19 (iter. 2) | `TransactionController.java:93` | `size = 20` → `size = 10` | `IT:246` `jsonPath("$.size").value(20)` | ✅ Killed |
| M1 (iter. 1) | `TransactionCsvParser.java:152` | `precision - scale > 17` → `> 16` | `PT:157-163` `acceptsAnAmountAtTheSeventeenIntegerDigitBound` | ✅ Killed |

---

## Route-access test (`MRA`, `f1e3ac4`) - critical review

**What it does.** It reads every handler from `RequestMappingHandlerMapping`. Part (a) is static: every route whose pattern starts with `/api/merchants/{merchantId}` must have a `UUID` parameter named `merchantId`, which is what `MerchantAccessAspect` (`security/MerchantAccessAspect.java`) keys on. Part (b) is dynamic: every GET route under `/api/merchants/{` is called by an operator without access to a fresh merchant, and any status other than `403` is collected and reported.

**Is it non-vacuous?** Yes. A probe in the scratch worktree listed the routes the test sees on `10b9679`:

- 55 routes in total. **Part (a) checks 31 routes** (19 GET, 12 POST/PUT/PATCH/DELETE). **Part (b) calls 20 GET routes** (the 19 scoped ones plus `GET /api/merchants/{id}`).
- For each of the 20 GET routes the probe also called the route as an operator **with** access and as admin. Every one answered something other than `403` (`200` or `404`; the reconciliation `export` route throws, see notes). So the `403` that part (b) gets for the operator without access comes from the merchant access check, not from a role rule in the filter chain. Part (b) would see an aspect bypass on any of them.
- Both parts also pin the inventory with `contains(...)` (`/transactions/imports/{importId}`, `/fee-rules/{id}`, `/reconciliations/{runId}/discrepancies/{discrepancyId}`, `/periods/summary`, `/bank-statements/import`, `/api/merchants/{id}`), so an empty enumeration cannot pass.

**Could it pass wrongly?** Not on the paths checked:

- A GET route that answers `400` or `404` before the aspect runs is **reported**, not hidden: the test fails on any status other than `403`. A route that does that would need a fix or an explicit exclusion, which shows in review.
- If parameter names were missing (no `-parameters`), `getParameterNames` returns `null` and part (a) reports every route as unguarded, so this fails loudly too.
- Mutation evidence: W7 (this feature) fails both parts. R1 (`ExternalSettlementController.findById`, a GET in another module, same rename) fails both parts. R2 (`ExternalSettlementController.importCsv`, a POST in another module) fails part (a). So the check covers the whole project, and part (a) covers routes part (b) does not call.

**Residual limits (notes, not gaps; none is a spec-stated behavior of this feature):**

- Part (a) matches the literal prefix `{merchantId}`. A new **non-GET** route written as `/api/merchants/{mid}/...` escapes both parts (part (b) would catch it only as a GET).
- Part (a) checks the parameter name, not that the controller is inside the aspect pointcut `within(br.com.hanrry.reconpay..controller..*)`. A merchant route in a class outside a `controller` package would pass (a) without being guarded; part (b) catches that only for GET.
- Part (a) does not check that the `merchantId` parameter is the one bound to the `{merchantId}` path variable (`@PathVariable("other") UUID merchantId` would pass). This is unlikely and not a real pattern in the code base.

---

## Spec-Anchored Acceptance Criteria

### P1: Importar transações por CSV

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| TIMP-01 | `201` with `id`, `merchantId`, `fileName`, `totalRows` (= rows saved), `createdAt` | `IT:64-68` `status().isCreated()`, `$.merchantId`, `$.fileName` `"vendas-julho.csv"`, `$.totalRows` `2`, `$.createdAt` exists; `IT:70,85` `$.id` = every saved transaction's `importBatch.id` | ✅ PASS |
| TIMP-02 | Each row saved with trimmed `externalReference`, `amount`, `paymentMethod`, `installments`, `status`, `transactionDate` | `IT:63` `" PED-2 "`; `IT:76-82` `contains("PED-1","PED-2")`, `(100.00, 50.00)`, `("CREDIT_CARD","PIX")`, `(3, 1)`, `("APPROVED","CANCELLED")`, `("2026-07-10","2026-07-11")`; `ST:133-142` | ✅ PASS |
| TIMP-03 | `expectedNetAmount` = `amount − amount×%÷100 − fixa`, 2 places, `HALF_UP`, same as the single create, with the active rule | `CT:18-23,29-30` `96.71`, `9.67`, `0.99`, `250.00`, `scale()` `2`; `IT:78` `contains(96.71, 49.50)`; `IT:89-102` import vs `POST` → `contains(119.46, 119.46)`; `IT:258-276` deactivated rule → row error | ✅ PASS (Y10 killed) |
| TIMP-04 | Exact header; otherwise `400 VALIDATION_ERROR` "Cabeçalho CSV inválido. Esperado: ..." | `IT:127-133` `isBadRequest()`, `$.error`, `$.message` exact; `PT:97-113` missing, renamed, letter case, **reordered** (`PT:107-109`), extra column → `hasMessage(...)` | ✅ PASS (W4, V16, Y8 killed) |
| TIMP-05 | 0 bytes → `400` "Arquivo CSV é obrigatório"; no `file` → `400 VALIDATION_ERROR` "Requisição inválida. Verifique os campos e parâmetros enviados." | `IT:146-150`, `IT:156-160` message exact; `ST:222-228` | ✅ PASS (Y11 killed) |
| TIMP-06 | Name not ending in `.csv` (any case) → `400` "Arquivo deve ser um CSV (.csv)" | `IT:152-154` `vendas.txt`; `ST:231-243` `vendas.txt`, `vendascsv` refused, `VENDAS.CSV` saved | ✅ PASS |
| TIMP-07 | Header only, or header plus blank lines → `400` "CSV não contém registros" | `IT:137-141`; `PT:116-123` | ✅ PASS (Y5 killed) |
| TIMP-08 | > 5 MB → `413 VALIDATION_ERROR` "Arquivo CSV excede o tamanho máximo permitido de 5MB" | `IT:174-178` `statusCode()==413`, `$.error`, message exact, `assertNothingSaved` | ✅ PASS (Y7 killed) |
| TIMP-09 | `400 VALIDATION_ERROR` "Erro na importação do CSV", `rowErrors` with `row` (header = 1) and `message` for every error, nothing saved | `IT:113-123` `isBadRequest()`, `$.error`, `$.message`, `rowErrors[*].row` `contains(3,4,5,5)`, messages exact, `assertNothingSaved`; `ST:162-182`; `PT:203-216`; `PT:165-176` | ✅ PASS (Y6 killed) |
| TIMP-10 | 16 exact row-error texts | Table below: 16/16 texts | ✅ PASS (W8 killed) |
| TIMP-11 | `409 CONFLICT` "Referências externas já cadastradas para este comerciante: <ref1>, <ref2>" + `conflictingReferences`, nothing saved, only when no row error | `IT:186-193` `isConflict()`, `$.error` `CONFLICT`, message exact, `conflictingReferences` `contains("PED-1","PED-2")`, batch count `1`; `ST:195-207` file order `containsExactly("A","C")`; `ST:180` `never().findExistingReferences` on row errors | ✅ PASS |
| TIMP-12 | Only with no error and no conflict: `409 CONFLICT` "Janela travada de <de> a <até>", nothing saved | `IT:197-212` real lock, `isConflict()`, `$.error`, message exact, `assertNothingSaved`; `ST:210-220`; `ST:207` `verifyNoInteractions(periodGuard)` on a conflict | ✅ PASS (Y2 killed) |
| TIMP-13 | Same reference as another merchant's transaction is accepted | `IT:215-221` → `isCreated()` | ✅ PASS |

#### TIMP-10 row-error texts (16/16)

The parameterized cases assert `containsExactly(new ImportRowErrorDTO(3, message))` at `PT:152`, and that the valid row next to the bad one is kept (`PT:153`).

| # | Spec text | Evidence | Result |
| --- | --- | --- | --- |
| 1 | "Número de colunas inválido. Esperado: 6" | `PT:127` (5), `PT:128` (7) | ✅ |
| 2 | "Referência externa é obrigatória" | `PT:129` | ✅ |
| 3 | "Referência externa deve ter no máximo 100 caracteres" | `PT:179-186` 101 refused, 100 accepted; `PT:173-174` | ✅ |
| 4 | "Referência externa duplicada no arquivo: <referência>" | `PT:197-198`; `PT:175` | ✅ |
| 5 | "amount inválido" | `PT:130`; `PT:209` | ✅ |
| 6 | "amount deve ter no máximo 17 dígitos inteiros e 2 casas decimais" | `PT:131-132` refused; `PT:157-163` accepted | ✅ (M1 killed) |
| 7 | "amount deve ser maior que zero" | `PT:133` `0.00`, `PT:134` `-5.00`; `PT:196`; `IT:118` | ✅ |
| 8 | "Método de pagamento inválido" | `PT:135` `CHEQUE`, `PT:136` `pix` | ✅ |
| 9 | "Número de parcelas inválido" | `PT:137-138` | ✅ |
| 10 | "Número de parcelas deve ser no mínimo 1" | `PT:139`; `PT:211` | ✅ |
| 11 | "Método de pagamento <MÉTODO> não permite parcelamento" | `PT:140-142`; `PT:214`; `IT:120` | ✅ |
| 12 | "Status inválido" | `PT:143` `PAID`, `PT:144` `approved`; `IT:121`; `PT:63-70` each listed status accepted | ✅ (W8 killed) |
| 13 | "Data da transação inválida. Formato esperado: yyyy-MM-dd" | `PT:145` `10/07/2026`, `PT:146` `2026-02-30` | ✅ |
| 14 | "Data da transação não pode ser futura" | `PT:147` today + 1 (fixed clock `PT:29`) | ✅ (Y4 killed) |
| 15 | "Regra de taxa ativa não encontrada para <MÉTODO> em <N>x" | `ST:173,175`; `ST:190`; `IT:119`; `IT:271-273` | ✅ (Y10 killed) |
| 16 | "Valor líquido esperado deve ser maior que zero" | `ST:176`; `CT:33-38` | ✅ |

### P1: Lote rastreável

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| TLOTE-01 | Batch saved with the original file name; each transaction linked to it | `IT:66` `$.fileName`; `IT:84-85` `getImportBatch().getId()` = `$.id`; `ST:127-128,145`; `RT:48-49` | ✅ PASS |
| TLOTE-02 | `200`, merchant page, newest first by default (`createdAt,desc`), 20 per page, items with `id`, `merchantId`, `fileName`, `totalRows`, `createdAt` | `IT:240-246` no params → `isOk()`, `$.content[*].id` `contains(third, second, first)`, `fileName` `("a.csv","c.csv","b.csv")`, `totalRows` `(1,3,2)`, `$.size` `20`; `RT:55-64` merchant scope; `ST` `listsTheMerchantBatches` (pageable passed through) | ✅ PASS (Y3 killed) |
| TLOTE-03 | `200` with the batch | `IT:248-255` `isOk()`, `id`, `merchantId`, `fileName` `"b.csv"`, `totalRows` `2`, `createdAt` exists | ✅ PASS |
| TLOTE-04 | Missing or another merchant's batch → `404 NOT_FOUND` "Importação não encontrada com id: <importId>" | `IT:284-288` `isNotFound()`, `$.error`, message exact; `ST:278-280` | ✅ PASS |
| TLOTE-05 | Transactions from `POST /transactions` have no batch | `RT:82-92` `getImportBatch()` `isNull()` | ✅ PASS |

**Status**: ✅ 18/18 ACs covered with a `file:line` citation and an assertion on the spec outcome. 16/16 TIMP-10 texts. 0 spec-precision gaps. 0 gaps.

---

## Edge Cases

- [x] Unknown or deactivated merchant → `404` "Comerciante não encontrado com id: <merchantId>": `IT:302-305` import (exact message), `IT:306-308` list; `ST:246-254` import; `ST:284-290` both batch queries (Y1, Y9 killed).
- [x] No access → `403`: `IT:292-300` import and list; `MRA` every merchant GET route, including `GET /transactions/imports/{importId}`, and the static parameter check on all 31 merchant-scoped routes (W7, R1, R2 killed).
- [x] No token → `401`: `IT:313-317` import.
- [x] Blank lines skipped, real line numbers kept: `PT:87-94` (Y5 killed).
- [x] Padded fields accepted: `PT:77-85`; `IT:63`/`IT:76` (Y8 killed).
- [x] Duplicate whose first occurrence has another error → both reported: `PT:189-200`.
- [x] `transactionDate` today accepted: `PT:72-75`.

---

## Discrimination Sensor

Scratch: `git worktree add --detach` at `/tmp/claude-1000/-home-hanrry-orca-workspaces-reconpay-alinhamento-backend-x-frontend/486a7ab6-2e48-4c98-bf3f-aceaa163f4d5/scratchpad/sensor5` (HEAD `10b9679`). A runner script kept outside the worktree applied each mutant by exact-string replacement (it checks the old text matches exactly once). It ran `./mvnw -q -Djacoco.skip=true test -Dtest=TransactionCsvParserTest,TransactionImportServiceTest,TransactionImportIntegrationTest,TransactionImportRepositoryIntegrationTest,ExpectedNetAmountCalculatorTest,TransactionServiceTest,HttpErrorContractTest,MerchantRouteAccessIntegrationTest`, then reverted with `git checkout -- backend/src/main`. A probe test written only in the scratch (`Probe5IT`) produced the route inventory above and checked four behaviors on the unmutated code: a file whose **first** row is in a locked window → `409`; `GET /imports?size=1` → `$.size` 1; a padded header → `201`; a deactivated merchant (`DELETE /api/merchants/{id}`) → `404` "Comerciante não encontrado...". All 5 probes passed. The worktree was removed with `git worktree remove --force` plus `git worktree prune`. The real tree's `git status --porcelain` before and after is exactly `?? frontend/AGENTS.md` (`diff` empty).

### Re-applied

6/6 killed (W7, W4, W8, V11, N19, M1). See "Re-check of earlier survivors".

### Project-wide check of the W7 class

| # | File:line | Mutation | Killed by | Result |
| --- | --- | --- | --- | --- |
| R1 | `ExternalSettlementController.java:49` | `GET /external-settlements/{id}`: `@PathVariable UUID merchantId` → `@PathVariable("merchantId") UUID ownerId` | `MRA` both tests | ✅ Killed |
| R2 | `ExternalSettlementController.java:56` | `POST /external-settlements/import`: same rename | `MRA` `everyMerchantScopedRouteHandsTheAspectAUuidParameterNamedMerchantId` | ✅ Killed |

### New mutants (paths not mutated in iterations 1-4)

| # | File:line | Mutation | Killed by | Result |
| --- | --- | --- | --- | --- |
| Y1 | `TransactionImportService.java:123` | `ensureMerchantExists` removed from `findImportById` (unknown merchant → batch 404 instead of merchant 404) | `ST` batchQueriesRejectAMissingMerchant | ✅ Killed |
| Y2 | `TransactionImportService.java:97` | period lock checked only with the **last** row's date | `ST` savesTheBatchAndEveryRowWithTheExpectedNetOfItsRule | ✅ Killed |
| Y3 | `TransactionImportService.java:117` | batch list ignores the client's `Pageable` (fixed `PageRequest.of(0, 20, createdAt DESC)`) | `ST` listsTheMerchantBatches | ✅ Killed |
| Y4 | `TransactionCsvParser.java:203` | future check uses `LocalDate.now()` instead of the injected clock | `PT` reportsEachRowError... [data futura] | ✅ Killed |
| Y5 | `TransactionCsvParser.java:65-74` | blank-row check moved after the column-count check | `PT` rejectsAFileWithOnlyTheHeader, skipsBlankLinesAndKeepsTheRealLineNumbers | ✅ Killed |
| Y6 | `TransactionImportService.java:90` | row errors sorted by row **descending** | `IT` answersEveryRowErrorAtOnceAndSavesNothing | ✅ Killed |
| Y7 | `GlobalExceptionHandler.java:202` | over-5MB handler `PAYLOAD_TOO_LARGE` → `BAD_REQUEST` | `IT` rejectsAFileLargerThanFiveMegabytes | ✅ Killed |
| Y8 | `TransactionCsvParser.java:95` | header cells not trimmed | `PT` trimsEveryField (header error) | ✅ Killed |
| Y9 | `TransactionImportService.java:63` | import loads the merchant with `findById` (deactivated merchant accepted) | `ST` 9 tests (incl. rejectsAnInactiveOrMissingMerchantBeforeReadingTheFile) | ✅ Killed |
| Y10 | `TransactionImportService.java:74` | fee rule looked up by installments only (method ignored) | `IT` answersEveryRowErrorAtOnceAndSavesNothing; `ST` aFeeRuleErrorAloneAlsoRejectsTheWholeFile | ✅ Killed |
| Y11 | `TransactionController.java:81` | `@RequestParam("file")` → `required = false` (missing field gives "Arquivo CSV é obrigatório") | `IT` rejectsAnEmptyFileAFileThatIsNotCsvAndARequestWithoutFile | ✅ Killed |

**Sensor depth**: expanded (P0-style, data-integrity and access path): 6 re-applied + 2 project-wide renames + 11 new across the parser, service, controller, exception handler and access wiring
**Result**: 19/19 killed - PASS ✅ (0 survivors, so no equivalence probes were needed)

Kill-path notes: Y2, Y3 and Y9 are killed by mocked service tests (`ST` verifies the arguments passed to `periodGuard` and to the repository, and stubs only `findByIdAndActiveTrue`). The matching behaviors were also confirmed end to end by the scratch probes (first-row lock → `409`; `size=1` honored; deactivated merchant → `404`), but those probes are not in the suite.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ Mirrors `SettlementCsvParser` / `ExternalSettlementService` |
| Surgical changes | ✅ Fix commits `f248e0d`, `7ab6276`, `4bc5ebe`, `f1e3ac4`, `10b9679` change tests only |
| No scope creep | ✅ `MRA` is outside the feature folder, but it closes the W7 class, which the user asked for project-wide |
| Matches patterns | ✅ Same handlers, `details` keys and `@PageableDefault` as the settlement import; `MRA` extends `AbstractIntegrationTest` |
| Spec-anchored outcome check | ✅ Exact texts, statuses, row numbers, order, page size |
| Per-layer coverage expectation | ✅ Domain 1:1 with ACs; routes cover happy, edge and error paths, including `403` on every merchant GET route |
| Every test maps to a requirement | ✅ `MRA` maps to the `403` edge case |
| No test weakened | ✅ `10b9679` and `f1e3ac4` only add assertions and tests |
| Documented guidelines followed | ✅ `AGENTS.md` (JUnit 5, AssertJ, MockMvc, Testcontainers via `AbstractIntegrationTest`, Flyway, Conventional Commits) |

### Notes (not gaps)

- **`MRA` residual limits**: literal `{merchantId}` prefix for the static part; no check that the controller is inside the aspect pointcut; no check of the `@PathVariable` binding name. Each only matters for a non-GET route written in an unusual way. See the review section.
- **Reconciliation export with an unknown run** (outside this feature): in the probe, `GET /api/merchants/{merchantId}/reconciliations/{runId}/export` with an operator who has access, or with admin, and a random `runId` raised `ReconciliationNotFoundException` out of MockMvc as a `ServletException` instead of answering `404`. `MRA` is not affected (the operator without access gets `403` first). Worth a look in the reconciliation module.
- **Deactivated merchant via HTTP**: covered by mocked `ST` tests (Y9 killed); the scratch probe confirms `404` end to end, but no suite test does it over HTTP.
- **401 on `GET /imports` and `GET /imports/{importId}`**: no test (only the import, `IT:313-317`). Authentication is global (filter chain), so no feature-code mutant can remove it.
- **Header trimming**: a padded header is accepted (`PT:77-85`, Y8 killed). It reads "espaços nas pontas de cada campo são ignorados" as applying to the header, like the settlement parser.
- **Untested out-of-spec file errors**: "Arquivo CSV vazio" (`TransactionCsvParser.java:52`) and "Erro ao ler arquivo CSV" (`:88`, `TransactionImportService.java:145`). The spec defines neither.
- **Scientific notation**: `new BigDecimal(raw)` accepts `1E+2`. The spec says "ponto decimal (100.50)" but does not list this as an error; same as the settlement parser.
- **`createdAt` precision**: `Instant.now()` is not truncated to microseconds (candidate L-007). No AC requires the `201` body and a later GET to match.
- **Concurrency untested**: two simultaneous imports of the same new reference rely on `uk_internal_transactions_merchant_external_reference` and the existing integrity handler (candidate L-002). It is a sweep claim, not an AC.
- **`Location` header**: asserted (`IT:238`) though the spec does not state it.

---

## Gate Check

- **Gate command**: `cd backend && ./mvnw verify`
- **Result**: exit 0, `Tests run: 630, Failures: 0, Errors: 0, Skipped: 0`, "All coverage checks have been met." (JaCoCo 85% line / 75% branch), BUILD SUCCESS
- **Feature tests**: `TransactionCsvParserTest` 43, `TransactionImportServiceTest` 12, `TransactionImportIntegrationTest` 15, `TransactionImportRepositoryIntegrationTest` 3, `ExpectedNetAmountCalculatorTest` 8, `MerchantRouteAccessIntegrationTest` 2 (83 new)
- **Test count before feature**: 547; after iteration 1: 622; iteration 2: 626; iteration 3: 626; iteration 4: 627; now: 630 (`f1e3ac4` +2, `10b9679` +1 parameterized case)
- **Skipped tests**: none
- **Failures**: none
- **validate_state.py**: see the end of this report.

---

## Fix Plans

None. No gap is open.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| TIMP-01..TIMP-13 | Implementing | ✅ Verified |
| TLOTE-01..TLOTE-05 | Implementing | ✅ Verified |
| Edge case 403 | ❌ Needs Fix | ✅ Verified |

(Not edited in `spec.md`; the orchestrator updates it.)

---

## Lessons

- **L-032, L-033, L-034** (iteration 4): followed by `f1e3ac4` and `10b9679`; W7, W4, W8 killed. No recurrence.
- **L-025, L-027, L-029**: M1, N19, V11 still killed. No recurrence.
- **L-032 class**: `MRA` closes it for every GET route under `/api/merchants/{...}` and for every route of any method under `/api/merchants/{merchantId}`. It does this project-wide (R1, R2 killed) and without listing routes by hand. It leaves the narrow cases in the notes.
- No new candidates: this run has no surviving mutant, AC gap, spec-precision gap or `SPEC_DEVIATION`.

## Summary

**Overall**: ✅ Ready (iteration 5, the user's second extra round)

**Spec-anchored check**: 18/18 ACs with spec-outcome assertions, 16/16 TIMP-10 texts, 0 spec-precision gaps, 0 gaps
**Sensor**: 19/19 killed (6 re-applied, 2 project-wide renames, 11 new)
**Gate**: 630 passed, 0 failed, JaCoCo met

**What works**: everything from iterations 1-4, plus the merchant access check on every merchant route (static and live), exact header order and status letter case. The 11 new mutants cover these paths and all were killed: merchant 404 on batch-by-id, inactive merchant on import, period lock on every date, client paging, injected clock, blank-row order, error order, 413 status, header trim, fee-rule key, and the required `file` field.

**Next steps**: the orchestrator updates `spec.md` traceability and `.specs/STATE.md`. Optional: look at the reconciliation export unknown-run behavior (outside this feature).

---

**validate_state.py**: `python3 -I /home/hanrry/.claude/skills/tlc-spec-driven/scripts/validate_state.py transaction-csv-import --root <repo>` → exit 0
