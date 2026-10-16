# Import CSV de Transações Validation

**Date**: 2026-10-16
**Spec**: `.specs/features/transaction-csv-import/spec.md`
**Diff range**: `f808eb2..af2c5e1` (spec/design `cbd5e1c`, `7c8a77b`; implementation `424b818..af2c5e1`), branch `feat/transaction-csv-import`, 8 commits
**Verifier**: independent sub-agent (author ≠ verifier), iteration 1

## Validation: transaction-csv-import - FAIL ❌

All 18 ACs, all 16 TIMP-10 texts and all 7 edge cases trace to `file:line` assertions that check the spec outcome. The gate passes: 622 tests, 0 failures, JaCoCo met. The verdict is FAIL because the discrimination sensor found a surviving mutant that matters:

- **M1 (gap 1).** No test sends an `amount` with exactly 17 integer digits, so the accepted side of the TIMP-10 bound "no máximo 17 dígitos inteiros" is not pinned. When I changed the check from `> 17` to `> 16`, every test still passed, so legal 17-digit amounts would be refused unnoticed.
- **M6 (gap 2, minor).** This mutant skips the in-file duplicate check when the same row already has a reference-length error. It survived because no test has a row that is both over 100 characters and a repeat. The row is still refused either way, but one of the "todos os erros" messages would go missing.

File abbreviations (all under `backend/src/test/java/br/com/hanrry/reconpay/`):

- `PT` = `transaction/service/TransactionCsvParserTest.java`
- `ST` = `transaction/service/TransactionImportServiceTest.java`
- `IT` = `transaction/integration/TransactionImportIntegrationTest.java`
- `RT` = `transaction/integration/TransactionImportRepositoryIntegrationTest.java`
- `CT` = `feerule/service/ExpectedNetAmountCalculatorTest.java`

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 ExpectedNetAmountCalculator | ✅ Done | `424b818`; `TransactionService` now calls the calculator, and the moved body is byte-identical (see TIMP-03) |
| T2 Lote no banco | ✅ Done | `23a5ac4`; `V24__create_transaction_imports.sql`, entity, repository, nullable `import_id` |
| T3 TransactionCsvParser | ✅ Done | `e8fccfc` |
| T4 TransactionImportService + exceptions + handlers | ✅ Done | `c5981fd` |
| T5 Endpoints | ✅ Done | `037d0ff` |
| T6 README | ✅ Done | `af2c5e1` |

---

## Spec-Anchored Acceptance Criteria

### P1: Importar transações por CSV

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| TIMP-01 | `201` with `id`, `merchantId`, `fileName`, `totalRows` (= rows saved), `createdAt` | `IT:64-68` `status().isCreated()`, `$.merchantId` `merchantId`, `$.fileName` `"vendas-julho.csv"`, `$.totalRows` `2`, `$.createdAt` exists; `IT:70,85` `$.id` read and equal to every saved transaction's `importBatch.id`; `ST:129` `getTotalRows()` `2` | ✅ PASS |
| TIMP-02 | Each row saved with trimmed `externalReference`, `amount`, `paymentMethod`, `installments`, `status`, `transactionDate` | `IT:63` sends `" PED-2 "`; `IT:76-82` `contains("PED-1","PED-2")`, `(100.00, 50.00)`, `("CREDIT_CARD","PIX")`, `(3, 1)`, `("APPROVED","CANCELLED")`, `("2026-07-10","2026-07-11")`; `ST:133-142` all 7 fields per entity `containsExactly(tuple(...))`; `ST:144` `getMerchant()` `isSameAs(merchant)` | ✅ PASS (payload/conjunction: every field asserted by value) |
| TIMP-03 | `expectedNetAmount` = `amount − amount×%÷100 − fixa`, 2 places, `HALF_UP`, same as the single create | `CT:18-23,29` `96.71`, `9.67`, `0.99` (HALF_UP vs HALF_EVEN case), `250.00`, `scale()==2`; `IT:78` `expectedNetAmount` `contains(96.71, 49.50)`; `IT:90-102` import `CSV-1` and `POST` `FORM-1`, both 123.45 Crédito 3x → `contains(119.46, 119.46)`; `ST:139-141` `96.71`, `49.50` | ✅ PASS |
| TIMP-04 | `400 VALIDATION_ERROR` "Cabeçalho CSV inválido. Esperado: externalReference,amount,paymentMethod,installments,status,transactionDate" | `IT:130-132` `isBadRequest()`, `$.error` `"VALIDATION_ERROR"`, `$.message` exact; `PT:98-106` missing column, renamed column, extra column → `hasMessage(...)` exact | ✅ PASS |
| TIMP-05 | 0 bytes → `400` "Arquivo CSV é obrigatório"; no `file` field → `400 VALIDATION_ERROR` "Requisição inválida. Verifique os campos e parâmetros enviados." | `IT:146-150` `isBadRequest()`, message exact; `IT:156-160` `isBadRequest()`, `$.error` `VALIDATION_ERROR`, message exact; `ST:224-226` | ✅ PASS (the 0-byte case does not assert `$.error`; that is covered by the shared handler path, `IT:131`) |
| TIMP-06 | Not `.csv` (case-insensitive) → `400` "Arquivo deve ser um CSV (.csv)" | `IT:152-154` `vendas.txt` → `isBadRequest()`, message exact; `ST:232-239` `vendas.txt` refused, `VENDAS.CSV` saved | ✅ PASS |
| TIMP-07 | Header only, or header plus blank lines → `400` "CSV não contém registros" | `IT:138-140` `isBadRequest()`, message exact; `PT:111-116` `HEADER` and `HEADER,"", " "` → `hasMessage("CSV não contém registros")` | ✅ PASS |
| TIMP-08 | > 5 MB → `413 VALIDATION_ERROR` "Arquivo CSV excede o tamanho máximo permitido de 5MB" | `IT:174-178` `statusCode()==413`, `$.error` `VALIDATION_ERROR`, message exact, `assertNothingSaved` | ✅ PASS |
| TIMP-09 | `400 VALIDATION_ERROR` "Erro na importação do CSV", `rowErrors` with `row` (header = 1) and `message` for every error of every row, nothing saved | `IT:113-123` `isBadRequest()`, `$.error`, `$.message` exact, `rowErrors[*].row` `contains(3,4,5,5)`, `rowErrors[*].message` exact, `assertNothingSaved`; `ST:171-181` parser and fee-rule errors merged and sorted, `assertNothingSaved()`, no reference or period lookup; `PT:179-185` five errors in one row plus one in the next | ✅ PASS |
| TIMP-10 | 16 exact row-error texts | See TIMP-10 table below: 16/16 | ✅ PASS (bound gap M1, see Sensor) |
| TIMP-11 | `409 CONFLICT` "Referências externas já cadastradas para este comerciante: <ref1>, <ref2>" + `conflictingReferences`, nothing saved | `IT:186-193` `isConflict()`, `$.error` `CONFLICT`, message `"...: PED-1, PED-2"`, `details.conflictingReferences` `contains("PED-1","PED-2")`, batch count still `1`; `ST:197-207` store returns `{C, A}`, answer in file order `"A, C"`, `assertNothingSaved()` | ✅ PASS |
| TIMP-12 | `409 CONFLICT` "Janela travada de <de> a <até>", nothing saved | `IT:198-212` real lock through `/periods/lock`, then `isConflict()`, `$.error` `CONFLICT`, `$.message` `"Janela travada de 2026-07-01 a 2026-07-15"`, `assertNothingSaved`; `ST:212-219` | ✅ PASS |
| TIMP-13 | Same reference as another merchant's transaction is accepted | `IT:218-220` `PED-1` for merchant A, then for merchant B → `isCreated()` | ✅ PASS |

#### TIMP-10 row-error texts (16/16)

All parser cases assert `containsExactly(new ImportRowErrorDTO(3, message))` at `PT:145`, so the row is line 3 with the header as line 1, and they also assert the valid row next to it is kept (`PT:146`).

| # | Spec text | Evidence | Result |
| --- | --- | --- | --- |
| 1 | "Número de colunas inválido. Esperado: 6" | `PT:121` (5 columns), `PT:122` (7 columns) | ✅ |
| 2 | "Referência externa é obrigatória" | `PT:123` | ✅ |
| 3 | "Referência externa deve ter no máximo 100 caracteres" | `PT:151-156` 101 chars → `ImportRowErrorDTO(2, ...)`; 100 chars accepted (`hasSize(1)`) | ✅ |
| 4 | "Referência externa duplicada no arquivo: <referência>" | `PT:166-169` rows 3 and 4 `"...: PED-1"`, first occurrence (row 2) has its own error | ✅ |
| 5 | "amount inválido" | `PT:124` `dez`; `PT:180` | ✅ |
| 6 | "amount deve ter no máximo 17 dígitos inteiros e 2 casas decimais" | `PT:125` `10.001`, `PT:126` 18 integer digits | ✅ text / ⚠️ accepted bound (17 digits) not tested, M1 survived |
| 7 | "amount deve ser maior que zero" | `PT:127` `0.00`, `PT:128` `-5.00`; `PT:93` `0`; `IT:118` | ✅ |
| 8 | "Método de pagamento inválido" | `PT:129` `CHEQUE`, `PT:130` `pix` | ✅ |
| 9 | "Número de parcelas inválido" | `PT:131` `um`, `PT:132` `1.5` | ✅ |
| 10 | "Número de parcelas deve ser no mínimo 1" | `PT:133` `0`; `PT:182` | ✅ |
| 11 | "Método de pagamento <MÉTODO> não permite parcelamento" | `PT:134` PIX, `PT:135` BOLETO, `PT:136` DEBIT_CARD (all three listed methods); `IT:120` | ✅ |
| 12 | "Status inválido" | `PT:137` `PAID`; `IT:121` | ✅ |
| 13 | "Data da transação inválida. Formato esperado: yyyy-MM-dd" | `PT:138` `10/07/2026`, `PT:139` `2026-02-30` | ✅ |
| 14 | "Data da transação não pode ser futura" | `PT:140` today + 1 (`2026-07-21`, clock fixed at `2026-07-20`) | ✅ |
| 15 | "Regra de taxa ativa não encontrada para <MÉTODO> em <N>x" | `ST:173,175` `BOLETO em 1x`, `CREDIT_CARD em 6x`; `ST:190` `DEBIT_CARD em 1x`; `IT:119` | ✅ |
| 16 | "Valor líquido esperado deve ser maior que zero" | `ST:176` row 5 (`0.30` Crédito 3x); `CT:34-38` net zero and negative → `hasMessage` exact | ✅ |

Allowed values (L-018): every method with an allowed installment count is accepted (`PT:54-60`: CREDIT_CARD 1x and 12x, DEBIT_CARD, PIX, BOLETO), and every status is accepted (`PT:64-69` `@EnumSource(TransactionStatus.class)`).

### P1: Lote rastreável

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| TLOTE-01 | Batch saved with the original file name; each saved transaction is linked to it | `IT:66` `$.fileName` `"vendas-julho.csv"`; `IT:84-85` every merchant transaction `getImportBatch().getId()` equals `$.id`; `ST:128,145` `getFileName()` `"vendas.csv"`, `getImportBatch()` `isSameAs(batch)`; `RT:48-49` | ✅ PASS |
| TLOTE-02 | `200`, merchant page, newest first by default, each item with `id`, `merchantId`, `fileName`, `totalRows`, `createdAt` | `IT:234-239` no `sort` param → `isOk()`, `$.content[*].id` `contains(second, first)` (insertion order is the reverse of the expected order, see L-012), `fileName`, `totalRows` `(2, 1)`; `RT:61-62` other merchant's batch excluded | ✅ PASS (list items do not assert `merchantId`/`createdAt`; the same DTO and mapper are asserted at `IT:245,248`) |
| TLOTE-03 | `200` with the batch | `IT:241-248` `isOk()`, `id`, `merchantId`, `fileName` `"primeiro.csv"`, `totalRows` `1`, `createdAt` exists | ✅ PASS |
| TLOTE-04 | Missing batch, or another merchant's batch → `404 NOT_FOUND` "Importação não encontrada com id: <importId>" | `IT:257-261` another merchant's batch → `isNotFound()`, `$.error` `NOT_FOUND`, message exact; `ST:275-277` missing id → `TransactionImportNotFoundException` with exact message | ✅ PASS |
| TLOTE-05 | Transactions created by `POST /transactions` have no batch | `RT:82-92` created via HTTP → `getImportBatch()` `isNull()` | ✅ PASS |

**Status**: ✅ 18/18 ACs and 16/16 TIMP-10 texts matched the spec outcome. ⚠️ One bound in TIMP-10 #6 has no test on its accepted side (sensor M1).

---

## Edge Cases

- [x] Unknown or deactivated merchant → `404` "Comerciante não encontrado com id: <merchantId>". Import: `IT:276-278` (exact message). Batch list: `IT:279-281` (`isNotFound()`, message not asserted). `ST:247-250` asserts the exact message and `assertNothingSaved()`; `ST:285-286` checks both batch queries by exception type. A *deactivated* merchant is only covered through the mocked `findByIdAndActiveTrue`, with no HTTP test. That is a minor gap.
- [x] No access → `403`: `IT:267-273` for import and list. Single-batch GET is not tested (minor).
- [x] No token → `401`: `IT:287-289` for import only (minor).
- [x] Blank lines in the middle or at the end are skipped and real line numbers are kept: `PT:89-93`. Blank line at row 2 and `,,,,,` at row 4, so the valid row is row 3 and the error is `ImportRowErrorDTO(5, ...)`. Killed M2.
- [x] Padded fields are accepted: `PT:79-84` (`"  PED-1 , 100.00 , PIX , 1 , APPROVED , 2026-07-10 "`, padded header too); `IT:63`/`IT:76` `" PED-2 "` → `"PED-2"`. Killed M4.
- [x] Duplicate whose first occurrence also has an error → both reported: `PT:166-169` (row 2 amount error, rows 3 and 4 duplicate). Killed M6b.
- [x] `transactionDate` today is accepted: `PT:74` (`2026-07-20` with the clock fixed at `2026-07-20`). Killed M3.

---

## Specific scrutiny

1. **"Nothing saved" is proven by state on every failure path.** Integration `assertNothingSaved` (`IT:302-306`) reads the database: zero batches for the merchant and no transaction of the merchant. It runs after 400 row errors (`IT:123`), wrong header (`IT:133`), header only (`IT:141`), empty/not-csv/no field (`IT:162`), 413 (`IT:178`), locked period (`IT:212`) and 403/404 (`IT:282`). For the 409 references case, `IT:192-193` asserts the batch count stays `1`. The extra transactions are not counted, but the unit test `ST:206` proves `saveAll` is never called on that path. At the unit level, `ST:103-107` also checks that `save`, `saveAll` and the audit log are never called.
2. **TIMP-03 is proven.** Import and single create share `ExpectedNetAmountCalculator.calculate`. `TransactionService` calls it at `TransactionService.java:81`, and `TransactionImportService.java:82` calls it too. `IT:89-103` compares the import against a real `POST` with the same data (`119.46` = `119.46`). The refactor preserves behavior: `git diff 424b818` moves `calculateExpectedNetAmount` byte for byte (same divide scale 4 `HALF_UP`, same `setScale(2, HALF_UP)`, same message). `TransactionServiceTest` and `TransactionIntegrationTest` were not changed in the range and pass in the gate. Mutants M12, M13 and M14 were killed.
3. **TIMP-09 ordering is tested both ways.** Both kinds of error in different rows: `ST:164-176` gives fee-rule row 2, parser row 3, fee-rule rows 4 and 5, so sorting is required. `IT:107-121` gives parser row 3, fee-rule row 4, parser row 5 twice, and expects the order `3,4,5,5`. Fee-rule only: `ST:186-190` (row 3 DEBIT_CARD 1x, with `assertNothingSaved`). Mutant M7 (sort removed) was killed by both. Note: one row cannot carry both kinds of error, because rows with format errors never reach the fee-rule check. The "stable sort" comment at `TransactionImportService.java:89` describes a case that cannot happen, which is harmless.
4. **Edge cases**: all four specific ones are covered and each one killed a targeted mutant (M2, M4, M6b, M3). See Edge Cases above.
5. **`HttpErrorContractTest`**: `git diff f808eb2..HEAD` shows exactly 2 changed lines, both adding `mock(TransactionImportService.class)` to a `new TransactionController(...)` call. No assertion was added, removed or changed. ✅
6. **Concurrency claim is untested.** The unique index `uk_internal_transactions_merchant_external_reference` exists (`V6__create_internal_transactions_table.sql:22-23`). The existing `DataIntegrityViolationException` handler returns `409 CONFLICT` "Conflito com um registro existente. Tente novamente." (`GlobalExceptionHandler.java:214-227`). No test runs two simultaneous imports of the same new reference, so this sweep claim rests on reasoning, not evidence. This matches candidate L-002. It is not an AC, so it is not counted in the 18 or as a gap. See Lessons.

---

## Discrimination Sensor

Scratch: a `git worktree add --detach` under the session scratchpad (`.../scratchpad/sensor`). Each mutant was applied there, run with `./mvnw -q -Djacoco.skip=true test -Dtest=<targets>`, and reverted with `git checkout`. Then `git worktree remove --force` and `git worktree prune`. The real tree's `git status --porcelain` before and after is exactly `?? frontend/AGENTS.md` (`diff` empty).

| # | File | Mutation | Killed by | Result |
| --- | --- | --- | --- | --- |
| M1 | `TransactionCsvParser.java:152` | integer-digit bound `> 17` → `> 16` | - | ❌ Survived |
| M2 | `TransactionCsvParser.java:65` | blank rows not counted (`rowNumber--`) | `PT` skipsBlankLinesAndKeepsTheRealLineNumbers | ✅ Killed |
| M3 | `TransactionCsvParser.java:203` | `isAfter(today)` → `!isBefore(today)` (today rejected) | `PT` acceptsToday | ✅ Killed |
| M4 | `TransactionCsvParser.java:127` | `paymentMethod` not trimmed | `PT` trimsEveryField | ✅ Killed |
| M5 | `TransactionCsvParser.java:114` | `length() > 100` → `>= 100` | `PT` rejectsAReferenceLongerThan100Characters | ✅ Killed |
| M6 | `TransactionCsvParser.java:119` | duplicate check skipped when the row already has an error (the length error) | - | ❌ Survived |
| M6b | `TransactionCsvParser.java:119,145` | reference registered only when its row is valid | `PT` marksEveryRepeatedReference... | ✅ Killed |
| M7 | `TransactionImportService.java:90` | `errors.sort(...)` removed | `ST` joinsFormatAndFeeRuleErrors...; `IT` answersEveryRowErrorAtOnce... | ✅ Killed |
| M8 | `TransactionImportService.java:161` | conflicting references sorted descending (not file order) | `ST` answersEveryReferenceAlreadyStored...; `IT` sendingTheSameFileAgain... | ✅ Killed |
| M9 | `TransactionImportService.java:176` | `setImportBatch` removed | `ST` savesTheBatch...; `IT` importsTheFileAndAnswersTheBatch | ✅ Killed |
| M10 | `TransactionImportService.java:95` | `periodGuard.assertDatesOpen` skipped | `ST` stopsOnALockedPeriod..., savesTheBatch...; `IT` aLockedPeriod... | ✅ Killed |
| M11 | `TransactionImportService.java:136` | extension check case-sensitive | `ST` rejectsAFileThatIsNotCsvAndAcceptsTheExtensionInAnyCase | ✅ Killed |
| M12 | `ExpectedNetAmountCalculator.java:26` | final rounding `HALF_UP` → `HALF_EVEN` | `CT` [3] (`1.00`, 1.5% → `0.99`) | ✅ Killed |
| M13 | `ExpectedNetAmountCalculator.java:27` | `signum() <= 0` → `< 0` | `CT` rejectsAZeroOrNegativeExpectedNet [1], [3] | ✅ Killed |
| M14 | `ExpectedNetAmountCalculator.java:21` | percentage fee scale 4 → 2 | `CT` [3] | ✅ Killed |
| M15 | `GlobalExceptionHandler.java:268` | `rowErrors` key renamed to `errors` | `IT` answersEveryRowErrorAtOnce... | ✅ Killed |
| M16 | `GlobalExceptionHandler.java:188` | duplicate references `CONFLICT` → `BAD_REQUEST` | `IT` sendingTheSameFileAgain... | ✅ Killed |
| M19 | `GlobalExceptionHandler.java:192` | `conflictingReferences` detail dropped | `IT` sendingTheSameFileAgain... | ✅ Killed |
| M17 | `TransactionController.java:87` | `created(uri)` → `ok` (200) | `IT` importsTheFile..., listsTheBatches..., +2 | ✅ Killed |
| M18 | `TransactionController.java:93` | default sort `DESC` → `ASC` | `IT` listsTheBatchesNewestFirstAndFindsOneById | ✅ Killed |

**Sensor depth**: expanded (20 mutants across all 5 target classes; this is a data-integrity path)
**Result**: 18/20 killed - FAIL ❌

- **M1 is a real gap.** TIMP-10 #6 allows up to 17 integer digits. The tests only check 18 digits (refused, `PT:126`) and small amounts (accepted). Nothing sends a 17-digit amount, so a bound that is one digit too strict passes the whole suite. The digit counts 17 and 16 are both untested on the accepted side.
- **M6 is a minor gap.** A row whose reference is over 100 characters and also repeats an earlier one should report both messages (TIMP-09, "todos os erros de todas as linhas"). No test combines the two, so dropping the duplicate message in that case passes.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ Mirrors `SettlementCsvParser` / `ExternalSettlementService`; the calculator is shared by two callers, not a one-use abstraction |
| Surgical changes | ✅ `TransactionService` change is the extraction only; `HttpErrorContractTest` gets a constructor argument only; `IFeeRuleRepository` gets one list finder |
| No scope creep | ✅ No `importId` filter, no undo, no status transition check (all out of scope) |
| Matches patterns | ✅ Same `StandardError` details keys (`rowErrors`, `conflictingReferences`) and the same 413 handler as the settlement import |
| Spec-anchored outcome check | ✅ Exact texts, status codes, row numbers and order asserted |
| Per-layer coverage expectation | ⚠️ Parser bound gap (M1, M6); otherwise every route has happy, error and auth paths |
| Every test maps to a requirement | ✅ `loadsTheActiveRulesOnceForTheWholeFile` maps to the T4 Done-when ("regras ativas carregadas numa só consulta") |
| Documented guidelines followed | ✅ `AGENTS.md` (JUnit 5, Mockito, MockMvc, AssertJ, Testcontainers via `AbstractIntegrationTest`, Flyway V24, no `ddl-auto`) |

Observations that are not gaps (spec is silent):

- The header is compared after `trim()` (`TransactionCsvParser.java:95`), so `" externalReference , amount ,..."` is accepted (`PT:79`). TIMP-04 says "cabeçalho exato". The spec's "espaços nas pontas de cada campo são ignorados" can be read to cover the header, and the settlement parser follows the same pattern. Reported as a reading, not a gap.
- Two file-level messages are not in the spec: "Arquivo CSV vazio" (the first line is blank but the file is not 0 bytes, `TransactionCsvParser.java:52`) and "Erro ao ler arquivo CSV" (read failure, `:88`, `TransactionImportService.java:145`). Neither has a test. The spec defines neither case.
- `TransactionImportEntity.createdAt` uses `Instant.now()` without truncating to microseconds. So the `createdAt` in the `201` body can differ in nanoseconds from the later GET (see candidate L-007). No AC requires the two to match.

---

## Gate Check

- **Gate command**: `cd backend && ./mvnw verify`
- **Result**: exit 0, `Tests run: 622, Failures: 0, Errors: 0, Skipped: 0`, "All coverage checks have been met." (JaCoCo 85% line / 75% branch), BUILD SUCCESS
- **Feature tests**: `TransactionCsvParserTest` 38, `TransactionImportServiceTest` 12, `TransactionImportIntegrationTest` 14, `TransactionImportRepositoryIntegrationTest` 3, `ExpectedNetAmountCalculatorTest` 8 (75 new)
- **Test count before feature**: 547 (622 − 75)
- **Delta**: +75, with no tests deleted or weakened (the only edited test file is `HttpErrorContractTest`, constructor argument only)
- **Skipped tests**: none
- **Failures**: none
- **validate_state.py**: `python3 -I <skill>/scripts/validate_state.py transaction-csv-import --root <repo>` → exit 1 ("validation.md verdict is FAIL"), as expected for a FAIL verdict

---

## Fix Plans

### Fix 1: Pin the accepted side of the 17-integer-digit bound (M1) - Major

- **Root cause**: `PT:125-126` only checks values past the bound. No accepted amount has 17 (or 16) integer digits.
- **Fix task**: In `TransactionCsvParserTest`, add accepted rows `12345678901234567.00` (17 integer digits) and `12345678901234567` (17 digits, no decimals), each producing no error and the parsed `amount`. Optionally add an integration case that imports a 17-digit amount and gets `201`.
- **Verify**: re-run M1 (`> 17` → `> 16` at `TransactionCsvParser.java:152`). It must fail.

### Fix 2: Report both the length error and the in-file duplicate on the same row (M6) - Minor

- **Root cause**: No test has a row that is both over 100 characters and a repeat of an earlier reference.
- **Fix task**: Add a parser test with two rows whose reference is the same 101-character string. Expect row 2 `"...no máximo 100 caracteres"` and row 3 `"...no máximo 100 caracteres"` followed by `"Referência externa duplicada no arquivo: <ref>"`.
- **Verify**: re-run M6. It must fail.

### Optional hardening (not counted as gaps)

- An HTTP test for a deactivated merchant (`404`), and `403`/`401` on `GET /imports/{importId}`.
- A concurrency test for two simultaneous imports with the same new reference: one `201`, one `409`, one batch saved (sweep claim, candidate L-002).

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| TIMP-01..TIMP-09, TIMP-11..TIMP-13 | Implementing | ✅ Verified |
| TIMP-10 | Implementing | ❌ Needs Fix (M1 bound and M6; all 16 texts verified) |
| TLOTE-01..TLOTE-05 | Implementing | ✅ Verified |

(Not edited in `spec.md`; the orchestrator updates it after the fix round.)

---

## Lessons

- **L-025** (candidate, `surviving_mutant`, source M1): "Test the largest value an inclusive bound accepts, not only the first value past it". This is related to L-019 and L-020 but not the same rule. L-019 is about the rejected side and L-020 about trailing-zero spelling, so I corroborated neither.
- **L-026** (candidate, `surviving_mutant`, source M6): "Test an input that fails two checks on the same field, so an error dropped when another already fired is caught".
- **L-002** (two simultaneous imports of the same reference) describes the same untested sweep claim. I did not corroborate it, because this report classifies the claim as optional hardening, not a validation failure.
- **L-001 and L-003** (413 / empty file with persist-nothing) were followed here: `IT:174-178`, `IT:146-150`, `IT:162`. **L-012** was followed at `IT:237`. **L-016** and **L-018** were followed: exact texts in the spec, and every method and status tested.

## Summary

**Overall**: ❌ Not Ready (one fix round)

**Spec-anchored check**: 18/18 ACs and 16/16 TIMP-10 texts matched the spec outcome; 0 spec-precision gaps
**Sensor**: 18/20 mutations killed (M1, M6 survived)
**Gate**: 622 passed, 0 failed, JaCoCo met

**What works**: the all-or-nothing import, proven by database state on every failure path; merged and sorted row errors; the shared expected-net calculator, proven equal to the single create; 409 references in file order; locked period; 413; batch list and lookup with merchant isolation; all auth boundaries on import.

**Issues found**: M1, the 17-integer-digit amount accepted bound is not tested (Fix 1). M6, a row with both the length error and a duplicate is not tested (Fix 2).

**Next steps**: route Fix 1 and Fix 2 to an implementer (tests only), then re-verify (iteration 2 of 3).
