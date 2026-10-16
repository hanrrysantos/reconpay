# Import CSV de Transações Validation

**Date**: 2026-10-16
**Spec**: `.specs/features/transaction-csv-import/spec.md`
**Diff range**: `f808eb2..f248e0d` (spec/design `cbd5e1c`, `7c8a77b`; implementation `424b818..af2c5e1`; iteration-1 report `86821c0`; fix `f248e0d`), branch `feat/transaction-csv-import`
**Verifier**: independent sub-agent (author ≠ verifier), iteration 2 of 3

## Validation: transaction-csv-import - FAIL ❌

Both iteration-1 gaps are closed. M1 (17-digit bound `> 17` → `> 16`) and M6 (duplicate skipped when the row already has the length error) were re-applied exactly as before, and the new tests in `f248e0d` kill both. All 18 ACs, the 16 TIMP-10 texts and the 7 edge cases still trace to `file:line` assertions that check the spec outcome. The gate passes: 626 tests, 0 failures, JaCoCo met.

The verdict is still FAIL because one of the 20 new sensor mutants survived:

- **N19 (gap 1, minor).** The spec's "Listagem de lotes" decision is "Paginada, mais recente primeiro (`createdAt,desc`), 20 por página por padrão". I changed the default page size in `TransactionController.java:93` from `size = 20` to `size = 10`. The targeted tests and the **full** test suite both still pass. Nothing asserts the default page size of `GET /transactions/imports`. Newest-first is pinned (M18 in iteration 1); the size is not.

File abbreviations (all under `backend/src/test/java/br/com/hanrry/reconpay/`):

- `PT` = `transaction/service/TransactionCsvParserTest.java`
- `ST` = `transaction/service/TransactionImportServiceTest.java`
- `IT` = `transaction/integration/TransactionImportIntegrationTest.java`
- `RT` = `transaction/integration/TransactionImportRepositoryIntegrationTest.java`
- `CT` = `feerule/service/ExpectedNetAmountCalculatorTest.java`

Since iteration 1 only `PT` changed (`git diff --stat af2c5e1..HEAD -- backend/src/test` = 1 file, +22). The `ST`, `IT`, `RT` and `CT` line numbers below were checked again and have not moved. `PT` lines after 146 moved by +22.

---

## Gap History

| Iteration | Commit verified | Gaps | Outcome |
| --- | --- | --- | --- |
| 1 | `af2c5e1` | G1 (M1): accepted side of the 17-integer-digit bound not tested. G2 (M6): a row with both the length error and a duplicate not tested | FAIL; fix routed |
| fix | `f248e0d` | Test only: `PT:149-156` accepts `12345678901234567.00`, `12345678901234567`, `0.01`; `PT:158-169` a 101-char reference repeated → length error on rows 2 and 3, duplicate on row 3 | - |
| 2 | `f248e0d` | G1 ✅ closed (M1 killed). G2 ✅ closed (M6 killed). **New G3 (N19)**: default page size of the batch listing not pinned | FAIL; one minor fix |

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
| Fix G1/G2 | ✅ Done | `f248e0d` (tests only, no production change) |

---

## Re-check of the iteration-1 survivors

| Mutant | Re-applied at | Mutation | Killed by | Result |
| --- | --- | --- | --- | --- |
| M1 | `TransactionCsvParser.java:152` | `value.precision() - value.scale() > 17` → `> 16` | `PT:149-156` `acceptsAnAmountAtTheSeventeenIntegerDigitBound` [1] `12345678901234567.00`, [2] `12345678901234567` → `errors()).isEmpty()`, `amount` `containsExactly(new BigDecimal(amount))` | ✅ Killed |
| M6 | `TransactionCsvParser.java:119` | `if (!referencesInFile.add(ref))` → `if (errors.size() == errorsBefore && !referencesInFile.add(ref))` (duplicate skipped once the row has the length error) | `PT:158-169` `aRepeatedReferenceThatIsAlsoTooLongReportsBothErrors` → `containsExactly((2, length), (3, length), (3, "Referência externa duplicada no arquivo: " + ref))` | ✅ Killed |

The new tests check the exact spec outcome. TIMP-10 #6 allows "no máximo 17 dígitos inteiros", and the test sends 17 integer digits with and without decimals and checks that the parsed amount is kept. TIMP-09 says "todos os erros de todas as linhas", and the test expects all three messages in order. `0.01` (precision − scale = −1) shows that the smallest positive amount passes the digit check.

---

## Spec-Anchored Acceptance Criteria

### P1: Importar transações por CSV

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| TIMP-01 | `201` with `id`, `merchantId`, `fileName`, `totalRows` (= rows saved), `createdAt` | `IT:64-68` `status().isCreated()`, `$.merchantId` `merchantId`, `$.fileName` `"vendas-julho.csv"`, `$.totalRows` `2`, `$.createdAt` exists; `IT:70,85` `$.id` equals every saved transaction's `importBatch.id`; `ST:129` `getTotalRows()` `2` | ✅ PASS (N8 killed) |
| TIMP-02 | Each row saved with trimmed `externalReference`, `amount`, `paymentMethod`, `installments`, `status`, `transactionDate` | `IT:63` sends `" PED-2 "`; `IT:76-82` `contains("PED-1","PED-2")`, `(100.00, 50.00)`, `("CREDIT_CARD","PIX")`, `(3, 1)`, `("APPROVED","CANCELLED")`, `("2026-07-10","2026-07-11")`; `ST:133-142` every field `containsExactly(tuple(...))`; `ST:144` `getMerchant()` `isSameAs(merchant)` | ✅ PASS |
| TIMP-03 | `expectedNetAmount` = `amount − amount×%÷100 − fixa`, 2 places, `HALF_UP`, same as the single create | `CT:18-23,29-30` `96.71`, `9.67`, `0.99` (HALF_UP vs HALF_EVEN), `250.00`, `net.scale()` `2`; `IT:78` `contains(96.71, 49.50)`; `IT:89-103` import vs `POST` with the same data → `contains(119.46, 119.46)`; `ST:139-141` | ✅ PASS (N10, N14, N15 killed) |
| TIMP-04 | `400 VALIDATION_ERROR` "Cabeçalho CSV inválido. Esperado: externalReference,amount,paymentMethod,installments,status,transactionDate" | `IT:130-132` `isBadRequest()`, `$.error` `"VALIDATION_ERROR"`, `$.message` exact; `PT:98-106` missing, renamed and extra column → `hasMessage(...)` exact | ✅ PASS (N4, N17 killed) |
| TIMP-05 | 0 bytes → `400` "Arquivo CSV é obrigatório"; no `file` → `400 VALIDATION_ERROR` "Requisição inválida. Verifique os campos e parâmetros enviados." | `IT:146-150` `isBadRequest()`, message exact; `IT:156-160` `isBadRequest()`, `$.error` `VALIDATION_ERROR`, message exact; `ST:224-226` | ✅ PASS (N7 killed) |
| TIMP-06 | Not `.csv` (any case) → `400` "Arquivo deve ser um CSV (.csv)" | `IT:152-154` `vendas.txt` → `isBadRequest()`, message exact; `ST:232-239` `vendas.txt` refused, `VENDAS.CSV` saved | ✅ PASS |
| TIMP-07 | Header only, or header plus blank lines → `400` "CSV não contém registros" | `IT:138-140` `isBadRequest()`, message exact; `PT:110-117` `HEADER` and `HEADER,"", " "` → `hasMessage("CSV não contém registros")` | ✅ PASS (N1 killed) |
| TIMP-08 | > 5 MB → `413 VALIDATION_ERROR` "Arquivo CSV excede o tamanho máximo permitido de 5MB" | `IT:174-178` `statusCode()==413`, `$.error`, message exact, `assertNothingSaved` | ✅ PASS |
| TIMP-09 | `400 VALIDATION_ERROR` "Erro na importação do CSV", `rowErrors` with `row` (header = 1) and `message` for every error of every row, nothing saved | `IT:113-123` `isBadRequest()`, `$.error`, `$.message` exact, `rowErrors[*].row` `contains(3,4,5,5)`, `rowErrors[*].message` exact, `assertNothingSaved`; `ST:171-181`; `PT:196-208` five errors in row 2 plus one in row 3; `PT:158-169` two errors on one field | ✅ PASS (M6, N18 killed) |
| TIMP-10 | 16 exact row-error texts | TIMP-10 table below: 16/16 | ✅ PASS (M1 now killed) |
| TIMP-11 | `409 CONFLICT` "Referências externas já cadastradas para este comerciante: <ref1>, <ref2>" + `conflictingReferences`, nothing saved | `IT:186-193` `isConflict()`, `$.error` `CONFLICT`, message `"...: PED-1, PED-2"`, `details.conflictingReferences` `contains("PED-1","PED-2")`, batch count stays `1`; `ST:197-207` | ✅ PASS (N13 killed) |
| TIMP-12 | `409 CONFLICT` "Janela travada de <de> a <até>", nothing saved | `IT:198-212` real lock, `isConflict()`, `$.error` `CONFLICT`, `$.message` `"Janela travada de 2026-07-01 a 2026-07-15"`, `assertNothingSaved`; `ST:212-219` | ✅ PASS |
| TIMP-13 | Same reference as another merchant's transaction is accepted | `IT:216-220` `PED-1` for merchant A, then B → `isCreated()` | ✅ PASS |

#### TIMP-10 row-error texts (16/16)

The parameterized cases assert `containsExactly(new ImportRowErrorDTO(3, message))` at `PT:145`, and that the valid row next to the bad one is kept (`PT:146`).

| # | Spec text | Evidence | Result |
| --- | --- | --- | --- |
| 1 | "Número de colunas inválido. Esperado: 6" | `PT:121` (5), `PT:122` (7) | ✅ (N5 killed) |
| 2 | "Referência externa é obrigatória" | `PT:123` | ✅ |
| 3 | "Referência externa deve ter no máximo 100 caracteres" | `PT:171-179` 101 chars → `(2, ...)`, 100 chars accepted (`hasSize(1)`); `PT:165-167` | ✅ |
| 4 | "Referência externa duplicada no arquivo: <referência>" | `PT:188-191` rows 3 and 4 `"...: PED-1"`; `PT:168` 101-char reference | ✅ (M6 killed) |
| 5 | "amount inválido" | `PT:124`; `PT:202` | ✅ |
| 6 | "amount deve ter no máximo 17 dígitos inteiros e 2 casas decimais" | refused: `PT:125` `10.001`, `PT:126` 18 integer digits; accepted: `PT:149-156` 17 integer digits with and without decimals | ✅ (M1, N2 killed) |
| 7 | "amount deve ser maior que zero" | `PT:127` `0.00`, `PT:128` `-5.00`; `PT:189`; `IT:118` | ✅ |
| 8 | "Método de pagamento inválido" | `PT:129` `CHEQUE`, `PT:130` `pix` | ✅ |
| 9 | "Número de parcelas inválido" | `PT:131` `um`, `PT:132` `1.5` | ✅ |
| 10 | "Número de parcelas deve ser no mínimo 1" | `PT:133` `0`; `PT:204` | ✅ (N3 killed) |
| 11 | "Método de pagamento <MÉTODO> não permite parcelamento" | `PT:134` PIX, `PT:135` BOLETO, `PT:136` DEBIT_CARD; `PT:207`; `IT:120` | ✅ (N6 killed) |
| 12 | "Status inválido" | `PT:137`; `IT:121` | ✅ |
| 13 | "Data da transação inválida. Formato esperado: yyyy-MM-dd" | `PT:138` `10/07/2026`, `PT:139` `2026-02-30` | ✅ |
| 14 | "Data da transação não pode ser futura" | `PT:140` today + 1 (clock `2026-07-20`) | ✅ |
| 15 | "Regra de taxa ativa não encontrada para <MÉTODO> em <N>x" | `ST:173,175` `BOLETO em 1x`, `CREDIT_CARD em 6x`; `ST:190`; `IT:119` | ✅ (N10 killed) |
| 16 | "Valor líquido esperado deve ser maior que zero" | `ST:176` row 5; `CT:34-38` net zero and negative → `hasMessage` exact | ✅ |

Allowed values (L-018): every method with an allowed installment count is accepted (`PT:54-60`), and so is every status (`PT:64-69` `@EnumSource`).

### P1: Lote rastreável

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| TLOTE-01 | Batch saved with the original file name; each transaction is linked to it | `IT:66` `$.fileName` `"vendas-julho.csv"`; `IT:84-85` every transaction `getImportBatch().getId()` equals `$.id`; `ST:128,145`; `RT:48-49` | ✅ PASS |
| TLOTE-02 | `200`, merchant page, newest first by default, items with `id`, `merchantId`, `fileName`, `totalRows`, `createdAt` | `IT:234-239` no `sort` → `isOk()`, `$.content[*].id` `contains(second, first)`, `fileName`, `totalRows` `(2, 1)`; `RT:61-62` other merchant excluded | ✅ PASS (the AC text names no page size; the "20 por página" default is in the Assumptions table, see N19) |
| TLOTE-03 | `200` with the batch | `IT:241-248` `isOk()`, `id`, `merchantId`, `fileName` `"primeiro.csv"`, `totalRows` `1`, `createdAt` exists | ✅ PASS (N20 killed) |
| TLOTE-04 | Missing or another merchant's batch → `404 NOT_FOUND` "Importação não encontrada com id: <importId>" | `IT:257-261` `isNotFound()`, `$.error` `NOT_FOUND`, message exact; `ST:275-277` | ✅ PASS (N12, N16 killed) |
| TLOTE-05 | Transactions from `POST /transactions` have no batch | `RT:82-92` `getImportBatch()` `isNull()` | ✅ PASS |

**Status**: ✅ 18/18 ACs and 16/16 TIMP-10 texts match the spec outcome. ❌ One surviving mutant on a spec-stated default that is outside the AC text (N19).

---

## Edge Cases

- [x] Unknown or deactivated merchant → `404` "Comerciante não encontrado com id: <merchantId>": `IT:276-278` import (exact message), `IT:279-281` list; `ST:247-250`; `ST:285-286`. N11 killed.
- [x] No access → `403`: `IT:267-273` import and list.
- [x] No token → `401`: `IT:287-289` import.
- [x] Blank lines skipped, real line numbers kept: `PT:88-93`.
- [x] Padded fields accepted: `PT:78-84`; `IT:63`/`IT:76`.
- [x] Duplicate whose first occurrence has another error → both reported: `PT:182-192`.
- [x] `transactionDate` today accepted: `PT:73-74`.

---

## Discrimination Sensor

Scratch: `git worktree add --detach` at `.../scratchpad/sensor` (HEAD `f248e0d`). Each mutant was applied by exact-string replacement (asserted to match once), run with `./mvnw -q -Djacoco.skip=true test -Dtest=<targets>`, and reverted with `git checkout -- backend/src/main`. N19 was also run against the whole test suite. Then `git worktree remove --force` and `git worktree prune`. The real tree's `git status --porcelain` before and after is exactly `?? frontend/AGENTS.md` (`diff` empty).

### Iteration-1 survivors (re-applied)

| # | File:line | Mutation | Killed by | Result |
| --- | --- | --- | --- | --- |
| M1 | `TransactionCsvParser.java:152` | `> 17` → `> 16` | `PT` acceptsAnAmountAtTheSeventeenIntegerDigitBound | ✅ Killed |
| M6 | `TransactionCsvParser.java:119` | duplicate check skipped when the row already has the length error | `PT` aRepeatedReferenceThatIsAlsoTooLongReportsBothErrors | ✅ Killed |

### New mutants (paths not mutated in iteration 1)

| # | File:line | Mutation | Killed by | Result |
| --- | --- | --- | --- | --- |
| N1 | `TransactionCsvParser.java:82` | `rows.isEmpty() && errors.isEmpty()` → `rows.isEmpty()` (an all-error file reads as "CSV não contém registros") | `PT` reportsEveryErrorOfARowAndOfEveryRow, marksEveryRepeatedReference..., aRepeatedReferenceThatIsAlsoTooLong... | ✅ Killed |
| N2 | `TransactionCsvParser.java:152` | `scale() > 2` → `> 3` | `PT` reportsEachRowErrorWithItsLineAndText (`10.001`) | ✅ Killed |
| N3 | `TransactionCsvParser.java:180` | `installments < 1` → `< 0` | `PT` reportsEachRowError... (`0`), reportsEveryErrorOfARow... | ✅ Killed |
| N4 | `TransactionCsvParser.java:93` | header `length == 6` → `length >= 6` (extra column accepted) | `PT` rejectsAWrongHeader | ✅ Killed |
| N5 | `TransactionCsvParser.java:69` | column count `!= 6` → `< 6` (7 columns accepted) | `PT` reportsEachRowError... (colunas a mais) | ✅ Killed |
| N6 | `TransactionCsvParser.java:130-132` | installment rule applied only to PIX | `PT` reportsEachRowError... (boleto/débito parcelado) | ✅ Killed |
| N7 | `TransactionImportService.java:131` | `file.isEmpty()` check removed | `ST` rejectsAnEmptyFile; `IT` rejectsAnEmptyFileAFileThatIsNotCsv... | ✅ Killed |
| N8 | `TransactionImportService.java:102` | `totalRows` = rows + errors + 1 | `ST` savesTheBatch...; `IT` importsTheFile..., listsTheBatches... | ✅ Killed |
| N9 | `TransactionImportService.java:108` | `TRANSACTIONS_IMPORTED` audit call removed | `ST` savesTheBatchAndEveryRow... (`ST:148` `verify(auditLogger).record(...)`) | ✅ Killed |
| N10 | `TransactionImportService.java:74` | fee rule looked up with installments `1` | `ST` joinsFormatAndFeeRuleErrors..., savesTheBatch...; `IT` importsTheFile..., theExpectedNetIsTheSame... | ✅ Killed |
| N11 | `TransactionImportService.java:116` | `ensureMerchantExists` removed from `findAllImports` | `ST` batchQueriesRejectAMissingMerchant; `IT` anOperatorWithoutAccess...UnknownMerchantIsNotFound | ✅ Killed |
| N12 | `TransactionImportService.java:124` | `findByIdAndMerchant_Id` → `findById` (cross-merchant leak) | `ST` findsABatchOfTheMerchantOrAnswersNotFound; `IT` aBatchOfAnotherMerchantIsNotFound | ✅ Killed |
| N13 | `TransactionImportService.java:161` | only the first conflicting reference reported (`limit(1)`) | `ST` answersEveryReferenceAlreadyStored...; `IT` sendingTheSameFileAgain... | ✅ Killed |
| N14 | `ExpectedNetAmountCalculator.java:25` | fixed fee not subtracted | `CT` (3 tests); `ST` (2 tests); `TransactionServiceTest` | ✅ Killed |
| N15 | `ExpectedNetAmountCalculator.java:26` | `setScale(2)` → `setScale(3)` | `CT` subtractsThePercentage... (`scale()==2`); `ST` savesTheBatch... | ✅ Killed |
| N16 | `GlobalExceptionHandler.java:64` | `TransactionImportNotFoundException` removed from the 404 handler | `IT` aBatchOfAnotherMerchantIsNotFound | ✅ Killed |
| N17 | `GlobalExceptionHandler.java:234` | `InvalidTransactionImportException` removed from the 400 business handler | `IT` rejectsAWrongHeader, rejectsAFileWithOnlyTheHeader, rejectsAnEmptyFile... | ✅ Killed |
| N18 | `GlobalExceptionHandler.java:264` | row-error handler `BAD_REQUEST` → `UNPROCESSABLE_ENTITY` | `IT` answersEveryRowErrorAtOnceAndSavesNothing | ✅ Killed |
| N19 | `TransactionController.java:93` | `@PageableDefault(size = 20, ...)` → `size = 10` for `GET /transactions/imports` | - (targeted `IT` and the **full suite** pass) | ❌ Survived |
| N20 | `TransactionController.java:87` | `created(uri).body(importBatch)` → `.body(null)` | `IT` importsTheFile..., listsTheBatches..., aBatchOfAnotherMerchant... | ✅ Killed |

**Sensor depth**: expanded (P0-style, data-integrity path): 2 re-applied + 20 new across all 5 target classes
**Result**: 21/22 killed - FAIL ❌ (N19 survived)

- **N19 is a real but minor gap.** The spec's Assumptions table sets the batch listing to "20 por página por padrão". The AC (TLOTE-02) names only pagination and newest first, so all 18 ACs still pass. Changing the default to 10 passes every test in the repo, so the stated default is not tested. Spring's own default page size is also 20, so even deleting `size = 20` would pass unnoticed. No other listing in the repo pins its default page size, so this pattern is common in the codebase, but this feature's spec states the value.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ Mirrors `SettlementCsvParser` / `ExternalSettlementService` |
| Surgical changes | ✅ The fix commit adds tests only; no production change since iteration 1 |
| No scope creep | ✅ |
| Matches patterns | ✅ Same `StandardError` details keys and 413 handler as the settlement import; same `@PageableDefault(size = 20, sort = "createdAt", DESC)` as `ExternalSettlementController.java:70` |
| Spec-anchored outcome check | ✅ Exact texts, statuses, row numbers and order |
| Per-layer coverage expectation | ⚠️ Default page size of the list route not covered (N19) |
| Every test maps to a requirement | ✅ The new `PT` tests map to TIMP-10 #6 and TIMP-09/#3-#4 |
| No test weakened | ✅ `f248e0d` adds 4 test executions (3 parameterized + 1) and removes nothing |
| Documented guidelines followed | ✅ `AGENTS.md` (JUnit 5, AssertJ, MockMvc, Testcontainers via `AbstractIntegrationTest`, Flyway, Conventional Commits) |

### Notes from iteration 1 (not gaps, still true)

- **Deactivated merchant via HTTP**: only covered through the mocked `findByIdAndActiveTrue` (`ST:247-250`); there is no HTTP test with a deactivated merchant.
- **403/401 on `GET /imports/{importId}`**: the single-batch GET has no `403` or `401` test. Import and list do (`IT:267-273`, `IT:287-289`).
- **Header trimming**: the header is compared after `trim()` (`TransactionCsvParser.java:95`), so a padded header is accepted (`PT:78-84`). TIMP-04 says "cabeçalho exato". This is a reading of "espaços nas pontas de cada campo são ignorados" and matches the settlement parser.
- **Untested out-of-spec file errors**: "Arquivo CSV vazio" (first line blank, file not 0 bytes, `TransactionCsvParser.java:52`) and "Erro ao ler arquivo CSV" (`:88`, `TransactionImportService.java:145`) have no test. The spec defines neither case.
- **`createdAt` precision**: `TransactionImportEntity.createdAt` uses `Instant.now()` without truncating to microseconds, so the `201` body can differ in nanoseconds from a later GET (candidate L-007). No AC requires them to match.
- **Concurrency untested**: two simultaneous imports of the same new reference rely on the unique index `uk_internal_transactions_merchant_external_reference` (`V6__create_internal_transactions_table.sql:22-23`) and the existing integrity handler (`409`). No test exercises this (candidate L-002). It is a sweep claim, not an AC.

---

## Gate Check

- **Gate command**: `cd backend && ./mvnw verify`
- **Result**: exit 0, `Tests run: 626, Failures: 0, Errors: 0, Skipped: 0`, "All coverage checks have been met." (JaCoCo 85% line / 75% branch), BUILD SUCCESS
- **Feature tests**: `TransactionCsvParserTest` 42 (+4 since iteration 1), `TransactionImportServiceTest` 12, `TransactionImportIntegrationTest` 14, `TransactionImportRepositoryIntegrationTest` 3, `ExpectedNetAmountCalculatorTest` 8 (79 new)
- **Test count before feature**: 547; after iteration 1: 622; now: 626
- **Delta**: +79 over the pre-feature count, +4 since iteration 1, none deleted or weakened
- **Skipped tests**: none
- **Failures**: none
- **validate_state.py**: `python3 -I <skill>/scripts/validate_state.py transaction-csv-import --root <repo>` → exit 1 ("validation.md verdict is FAIL"). This is expected for a FAIL verdict.

---

## Fix Plans

### Fix 3: Pin the default page size of the batch listing (N19) - Minor

- **Root cause**: `IT:234-239` lists batches without `size` and checks the order and content only. No assertion reads the page size, so the "20 por página por padrão" default can change unnoticed.
- **Fix task**: In `TransactionImportIntegrationTest.listsTheBatchesNewestFirstAndFindsOneById` (or a new test), call `GET /api/merchants/{merchantId}/transactions/imports` with no `page`/`size`/`sort` parameters and assert `$.size` (or `$.pageable.pageSize`, whichever the `Page` JSON exposes) equals `20`.
- **Verify**: re-run N19 (`size = 20` → `size = 10` at `TransactionController.java:93`). It must fail.

### Optional hardening (not counted as gaps)

- HTTP test for a deactivated merchant; `403`/`401` on `GET /imports/{importId}`.
- Concurrency test for two simultaneous imports with the same new reference (candidate L-002).

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| TIMP-01..TIMP-13 | Implementing (TIMP-10 Needs Fix) | ✅ Verified (TIMP-10 bound and stacked errors now pinned) |
| TLOTE-01, TLOTE-03..TLOTE-05 | Implementing | ✅ Verified |
| TLOTE-02 | Implementing | ✅ Verified against the AC text; ⚠️ default page size from the Assumptions table not tested (Fix 3) |

(Not edited in `spec.md`; the orchestrator updates it after the fix round.)

---

## Lessons

- **L-025** and **L-026** (iteration 1, candidates): the fix follows both. The 17-digit accepted bound and the two-errors-on-one-field case are now tested, and M1/M6 are killed. They did not recur, so no penalty and no new evidence.
- **L-027** (new candidate, N19): "Assert the default page size of a paginated endpoint when the spec states one" (`surviving_mutant`, scope `routes`).
- **L-002** (concurrency) is still an untested sweep claim and is kept as a note, not a validation failure, as in iteration 1.

## Summary

**Overall**: ❌ Not Ready (one minor fix round, iteration 3 of 3 left)

**Spec-anchored check**: 18/18 ACs and 16/16 TIMP-10 texts match the spec outcome; 0 spec-precision gaps
**Sensor**: 21/22 killed (M1, M6 now killed; N19 survived)
**Gate**: 626 passed, 0 failed, JaCoCo met

**What works**: everything from iteration 1, plus the accepted 17-digit amount bound and the stacked length and duplicate errors. All 20 new mutants on the parser, service, calculator, handlers and controller were killed except N19.

**Issues found**: N19, the batch listing's default page size of 20 (a spec decision) is not tested (Fix 3).

**Next steps**: route Fix 3 (one assertion, test only) to an implementer, then re-verify (iteration 3 of 3).
