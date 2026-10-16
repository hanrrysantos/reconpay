# Import CSV de Transações Validation

**Date**: 2026-10-16
**Spec**: `.specs/features/transaction-csv-import/spec.md`
**Diff range**: `f808eb2..4bc5ebe` (spec/design `cbd5e1c`, `7c8a77b`; implementation `424b818..af2c5e1`; iteration-1 report `86821c0`; fix `f248e0d`; iteration-2 report `068fa3a`; fix `7ab6276`; iteration-3 report `dbcda41`; fix `4bc5ebe`), branch `feat/transaction-csv-import`
**Verifier**: independent sub-agent (author ≠ verifier), iteration 4. The normal bound is 3. The user explicitly authorized this extra round after iteration 3 failed.

## Validation: transaction-csv-import - FAIL ❌

The four iteration-3 gaps are closed. V15 (default sort `createdAt` → `fileName`) is killed in both directions, and so is the `totalRows` variant in both directions. V11 (fee rules not filtered by `active`), V16 (`equalsIgnoreCase` header) and V7 (`endsWith("csv")`) are killed by the tests added in `4bc5ebe`. The earlier survivors M1 (iteration 1) and N19 (iteration 2) are still killed. The gate passes: 627 tests, 0 failures, JaCoCo met.

The verdict is still FAIL. I ran 12 new mutants on paths not touched in iterations 1-3. 3 of them survived the **full** suite. Each was confirmed as a real behavior change: in the scratch worktree, a probe integration test passes on the unmutated code and fails on the mutant.

1. **W7 (gap, major, security): no access check on `GET /transactions/imports/{importId}`.** `TransactionController.java:99` `@PathVariable UUID merchantId` → `@PathVariable("merchantId") UUID ownerId`. `MerchantAccessAspect` finds the merchant by the parameter **name** `merchantId`. With the rename it skips `MerchantAccessGuard`, so an operator without access to the merchant gets `200` with its batch. The edge case "IF o usuário não tem acesso à empresa THEN the system SHALL responder `403`" applies. `IT:292-300` checks `403` only on the import and the list. Iterations 1-3 kept "403 on `GET /imports/{importId}` not tested" as a note because no mutant had shown it. This one does, so it is now a gap.
2. **W4 (gap, minor): header columns accepted in any order.** `TransactionCsvParser.java:95` `HEADER[i].equals(cell)` → `Arrays.asList(HEADER).contains(cell)`. A permuted header (`amount,externalReference,...`) passes the header check, and the rows are then read by fixed position. TIMP-04 requires "o cabeçalho exato" and `400` "Cabeçalho CSV inválido...". The mutant answers row errors (or, with a header that repeats a column name, accepts it). `PT:97-110` tests missing, renamed, extra and letter-case headers, but no reordered one.
3. **W8 (gap, minor): status accepted in lower case.** `TransactionCsvParser.java:193` `TransactionStatus.valueOf(raw)` → `valueOf(raw.toUpperCase())`. `approved` is saved as `APPROVED`. TIMP-10 #12 and the Assumptions table limit status to `APPROVED`, `CANCELLED`, `REFUNDED`, `CHARGEBACK` ("Status inválido" otherwise). The method column has this exact case (`PT:133` "método minúsculo"); the status column does not.

File abbreviations (all under `backend/src/test/java/br/com/hanrry/reconpay/`):

- `PT` = `transaction/service/TransactionCsvParserTest.java`
- `ST` = `transaction/service/TransactionImportServiceTest.java`
- `IT` = `transaction/integration/TransactionImportIntegrationTest.java`
- `RT` = `transaction/integration/TransactionImportRepositoryIntegrationTest.java`
- `CT` = `feerule/service/ExpectedNetAmountCalculatorTest.java`

Since iteration 3, `4bc5ebe` changed `IT` (list test rewritten, new test `IT:258-276`; later lines moved +26), `PT` (+3 lines at `PT:104-106`; later lines moved +3) and `ST` (+3 lines at `ST:235-237`; later lines moved +3). Every citation below was taken again from `4bc5ebe`.

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
| fix | `4bc5ebe` | Test only: three batches `b.csv`/`c.csv`/`a.csv` asserted `a, c, b`; deactivated PIX rule → `400` + nothing saved; header differing only in case refused; `vendascsv` refused | - |
| 4 | `4bc5ebe` | G4 ✅, G5 ✅, G6 ✅, G7 ✅ closed (V15, V15asc, V15b, V15basc, V11, V16, V7 killed). M1, N19 still killed. **New G8 (W7)** no 403 on batch-by-id, **G9 (W4)** permuted header, **G10 (W8)** lower-case status | FAIL; beyond the authorized extra round → back to the user |

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
| Fix G4-G7 | ✅ Done | `4bc5ebe` (tests only, no production code changed) |

---

## Re-check of earlier survivors

| Mutant | Re-applied at | Mutation | Killed by | Result |
| --- | --- | --- | --- | --- |
| V15 | `TransactionController.java:93` | `sort = "createdAt"` → `"fileName"`, `DESC` | `IT:243-245` `listsTheBatchesNewestFirstAndFindsOneById` `contains(third, second, first)` | ✅ Killed |
| V15asc | `TransactionController.java:93` | `sort = "fileName"`, `ASC` | `IT:243` same | ✅ Killed |
| V15b | `TransactionController.java:93` | `sort = "totalRows"`, `DESC` | `IT:243` same | ✅ Killed |
| V15basc | `TransactionController.java:93` | `sort = "totalRows"`, `ASC` | `IT:243` same | ✅ Killed |
| V11 | `IFeeRuleRepository.java:17` | `@Query("select f from FeeRuleEntity f where f.merchant.id = ?1")` (inactive rules used) | `IT:258-276` `aDeactivatedFeeRuleDoesNotPriceTheImport` | ✅ Killed |
| V16 | `TransactionCsvParser.java:95` | header `equals` → `equalsIgnoreCase` | `PT:104-106` `rejectsAWrongHeader` | ✅ Killed |
| V7 | `TransactionImportService.java:136` | `endsWith(".csv")` → `endsWith("csv")` | `ST:235-237` `rejectsAFileThatIsNotCsvAndAcceptsTheExtensionInAnyCase` | ✅ Killed |
| M1 (iter. 1) | `TransactionCsvParser.java:152` | `precision - scale > 17` → `> 16` | `PT:153-159` `acceptsAnAmountAtTheSeventeenIntegerDigitBound` | ✅ Killed |
| N19 (iter. 2) | `TransactionController.java:93` | `size = 20` → `size = 10` | `IT:246` `jsonPath("$.size").value(20)` | ✅ Killed |

The list fixture (`IT:225-246`) creates `b.csv` (2 rows), `c.csv` (3 rows), `a.csv` (1 row). Newest first is `a, c, b`. Sorting by name gives `a,b,c` or `c,b,a`, and sorting by row count gives `a,b,c` or `c,b,a`, so none of them matches. The deactivation test uses the real `DELETE /fee-rules/{id}`, which is a soft delete (`FeeRuleService.java:107` `setActive(false)`). It asserts `400`, `rowErrors` `(2, "Regra de taxa ativa não encontrada para PIX em 1x")` and `assertNothingSaved`.

---

## Spec-Anchored Acceptance Criteria

### P1: Importar transações por CSV

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| TIMP-01 | `201` with `id`, `merchantId`, `fileName`, `totalRows` (= rows saved), `createdAt` | `IT:64-68` `status().isCreated()`, `$.merchantId`, `$.fileName` `"vendas-julho.csv"`, `$.totalRows` `2`, `$.createdAt` exists; `IT:70,85` `$.id` = every saved transaction's `importBatch.id` | ✅ PASS (W12 killed) |
| TIMP-02 | Each row saved with trimmed `externalReference`, `amount`, `paymentMethod`, `installments`, `status`, `transactionDate` | `IT:63` `" PED-2 "`; `IT:76-82` `contains("PED-1","PED-2")`, `(100.00, 50.00)`, `("CREDIT_CARD","PIX")`, `(3, 1)`, `("APPROVED","CANCELLED")`, `("2026-07-10","2026-07-11")`; `ST:133-142` | ✅ PASS (W5, W10 killed) |
| TIMP-03 | `expectedNetAmount` = `amount − amount×%÷100 − fixa`, 2 places, `HALF_UP`, same as the single create, with the **active** rule | `CT:18-23,29-30` `96.71`, `9.67`, `0.99`, `250.00`, `scale()` `2`; `IT:78` `contains(96.71, 49.50)`; `IT:89-102` import vs `POST` → `contains(119.46, 119.46)`; `IT:258-276` deactivated rule → row error | ✅ PASS (V11 killed) |
| TIMP-04 | Exact header; otherwise `400 VALIDATION_ERROR` "Cabeçalho CSV inválido. Esperado: ..." | `IT:127-133` `isBadRequest()`, `$.error`, `$.message` exact; `PT:97-110` missing, renamed, letter case, extra column → `hasMessage(...)` | ⚠️ Message and case ✅ (V16 killed); column **order** not tested → **G9 (W4)** |
| TIMP-05 | 0 bytes → `400` "Arquivo CSV é obrigatório"; no `file` → `400 VALIDATION_ERROR` "Requisição inválida. Verifique os campos e parâmetros enviados." | `IT:146-150`, `IT:156-160` message exact; `ST:222-228` | ✅ PASS |
| TIMP-06 | Name not ending in `.csv` (any case) → `400` "Arquivo deve ser um CSV (.csv)" | `IT:152-154` `vendas.txt`; `ST:231-243` `vendas.txt` and `vendascsv` refused, `VENDAS.CSV` saved | ✅ PASS (V7, M11 killed) |
| TIMP-07 | Header only, or header plus blank lines → `400` "CSV não contém registros" | `IT:137-141`; `PT:113-120` | ✅ PASS |
| TIMP-08 | > 5 MB → `413 VALIDATION_ERROR` "Arquivo CSV excede o tamanho máximo permitido de 5MB" | `IT:174-178` `statusCode()==413`, `$.error`, message exact, `assertNothingSaved` | ✅ PASS |
| TIMP-09 | `400 VALIDATION_ERROR` "Erro na importação do CSV", `rowErrors` with `row` (header = 1) and `message` for every error, nothing saved | `IT:113-123` `isBadRequest()`, `$.error`, `$.message`, `rowErrors[*].row` `contains(3,4,5,5)`, messages exact, `assertNothingSaved`; `ST:162-182`; `PT:199-212`; `PT:161-172` | ✅ PASS (W3 killed) |
| TIMP-10 | 16 exact row-error texts | Table below: 16/16 texts | ✅ texts; ⚠️ #12 lower-case status accepted → **G10 (W8)** |
| TIMP-11 | `409 CONFLICT` "Referências externas já cadastradas para este comerciante: <ref1>, <ref2>" + `conflictingReferences`, nothing saved, only when no row error | `IT:186-193` `isConflict()`, `$.error` `CONFLICT`, message exact, `conflictingReferences` `contains("PED-1","PED-2")`, batch count `1`; `ST:195-207` file order `containsExactly("A","C")`; `ST:180` `never().findExistingReferences` on row errors | ✅ PASS (W1, W3 killed) |
| TIMP-12 | Only with no error and no conflict: `409 CONFLICT` "Janela travada de <de> a <até>", nothing saved | `IT:197-212` real lock, `isConflict()`, `$.error`, message exact, `assertNothingSaved`; `ST:210-220`; `ST:207` `verifyNoInteractions(periodGuard)` on a conflict | ✅ PASS (W2 killed) |
| TIMP-13 | Same reference as another merchant's transaction is accepted | `IT:215-221` → `isCreated()` | ✅ PASS (W1 killed) |

#### TIMP-10 row-error texts (16/16)

The parameterized cases assert `containsExactly(new ImportRowErrorDTO(3, message))` at `PT:148`, and that the valid row next to the bad one is kept (`PT:149`).

| # | Spec text | Evidence | Result |
| --- | --- | --- | --- |
| 1 | "Número de colunas inválido. Esperado: 6" | `PT:124` (5), `PT:125` (7) | ✅ |
| 2 | "Referência externa é obrigatória" | `PT:126` | ✅ |
| 3 | "Referência externa deve ter no máximo 100 caracteres" | `PT:175-182` 101 refused, 100 accepted; `PT:169-170` | ✅ |
| 4 | "Referência externa duplicada no arquivo: <referência>" | `PT:193-194`; `PT:171` | ✅ |
| 5 | "amount inválido" | `PT:127`; `PT:205` | ✅ |
| 6 | "amount deve ter no máximo 17 dígitos inteiros e 2 casas decimais" | `PT:128-129` refused; `PT:153-159` accepted | ✅ (M1 killed) |
| 7 | "amount deve ser maior que zero" | `PT:130` `0.00`, `PT:131` `-5.00`; `PT:192`; `IT:118` | ✅ |
| 8 | "Método de pagamento inválido" | `PT:132` `CHEQUE`, `PT:133` `pix` | ✅ (W11 killed) |
| 9 | "Número de parcelas inválido" | `PT:134-135` | ✅ |
| 10 | "Número de parcelas deve ser no mínimo 1" | `PT:136`; `PT:207` | ✅ |
| 11 | "Método de pagamento <MÉTODO> não permite parcelamento" | `PT:137-139`; `PT:210`; `IT:120` | ✅ |
| 12 | "Status inválido" | `PT:140` `PAID`; `IT:121`; `PT:63-70` each listed status accepted | ⚠️ text ✅; `approved` accepted under W8 → **G10** |
| 13 | "Data da transação inválida. Formato esperado: yyyy-MM-dd" | `PT:141` `10/07/2026`, `PT:142` `2026-02-30` | ✅ |
| 14 | "Data da transação não pode ser futura" | `PT:143` today + 1 | ✅ |
| 15 | "Regra de taxa ativa não encontrada para <MÉTODO> em <N>x" | `ST:173,175`; `ST:190`; `IT:119` (no rule); `IT:271-273` (deactivated rule) | ✅ (V11 killed) |
| 16 | "Valor líquido esperado deve ser maior que zero" | `ST:176`; `CT:33-38` | ✅ |

### P1: Lote rastreável

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| TLOTE-01 | Batch saved with the original file name; each transaction linked to it | `IT:66` `$.fileName`; `IT:84-85` `getImportBatch().getId()` = `$.id`; `ST:127-128,145`; `RT:48-49` | ✅ PASS (W12 killed) |
| TLOTE-02 | `200`, merchant page, newest first by default (`createdAt,desc`), 20 per page, items with `id`, `merchantId`, `fileName`, `totalRows`, `createdAt` | `IT:240-246` no params → `isOk()`, `$.content[*].id` `contains(third, second, first)`, `fileName` `("a.csv","c.csv","b.csv")`, `totalRows` `(1,3,2)`, `$.size` `20`; `RT:55-64` merchant scope | ✅ PASS (V15 ×4, N19, M18, W6 killed) |
| TLOTE-03 | `200` with the batch | `IT:248-255` `isOk()`, `id`, `merchantId`, `fileName` `"b.csv"`, `totalRows` `2`, `createdAt` exists | ✅ PASS |
| TLOTE-04 | Missing or another merchant's batch → `404 NOT_FOUND` "Importação não encontrada com id: <importId>" | `IT:284-288` `isNotFound()`, `$.error`, message exact; `ST:278-280` | ✅ PASS |
| TLOTE-05 | Transactions from `POST /transactions` have no batch | `RT:82-92` `getImportBatch()` `isNull()` | ✅ PASS |

**Status**: every AC has a `file:line` citation and an assertion on the spec outcome. 0 spec-precision gaps. ❌ 2 ACs (TIMP-04, TIMP-10 #12) and 1 edge case (403) have a stated behavior that a surviving mutant breaks without any test failing.

---

## Edge Cases

- [x] Unknown or deactivated merchant → `404` "Comerciante não encontrado com id: <merchantId>": `IT:302-305` import (exact message), `IT:306-308` list; `ST:246-254`; `ST:284-290`.
- [ ] **No access → `403`**: `IT:292-300` import and list only. `GET /imports/{importId}` is not tested, and the W7 mutant opens it → **G8**.
- [x] No token → `401`: `IT:313-317` import.
- [x] Blank lines skipped, real line numbers kept: `PT:87-94`.
- [x] Padded fields accepted: `PT:77-85`; `IT:63`/`IT:76`.
- [x] Duplicate whose first occurrence has another error → both reported: `PT:185-196`.
- [x] `transactionDate` today accepted: `PT:72-75`.

---

## Discrimination Sensor

Scratch: `git worktree add --detach` at `/tmp/claude-1000/-home-hanrry-orca-workspaces-reconpay-alinhamento-backend-x-frontend/486a7ab6-2e48-4c98-bf3f-aceaa163f4d5/scratchpad/sensor` (HEAD `4bc5ebe`). A runner script kept outside the worktree applied each mutant by exact-string replacement (it checks the old text matches exactly once). It ran `./mvnw -q -Djacoco.skip=true test -Dtest=TransactionCsvParserTest,TransactionImportServiceTest,TransactionImportIntegrationTest,TransactionImportRepositoryIntegrationTest,ExpectedNetAmountCalculatorTest,TransactionServiceTest,HttpErrorContractTest`, then reverted with `git checkout -- backend/src/main`. Every survivor was run again against the **full** suite (`./mvnw -q -Djacoco.skip=true test`) and survived again. Each survivor was then checked with a probe test written only in the scratch (`ProbeIT`: operator without access on `GET /imports/{id}` → `403`; permuted header → header error; `approved` → "Status inválido"). The probes pass on the unmutated code and fail on each mutant, which shows each survivor is a real behavior change, not an equivalent mutant. The worktree was removed with `git worktree remove --force` plus `git worktree prune`. The real tree's `git status --porcelain` before and after is exactly `?? frontend/AGENTS.md` (`diff` empty).

### Re-applied

See "Re-check of earlier survivors" above: 9/9 killed (V15, V15asc, V15b, V15basc, V11, V16, V7, M1, N19).

### New mutants (paths not mutated in iterations 1-3)

| # | File:line | Mutation | Killed by | Result |
| --- | --- | --- | --- | --- |
| W1 | `IInternalTransactionRepository.java:23` | stored-reference query ignores the merchant (`merchant.id = :id OR merchant.id <> :id`) | `IT` sendingTheSameFileAgain..., theSameReferenceIsAcceptedForAnotherMerchant | ✅ Killed |
| W2 | `TransactionImportService.java:94-97` | period lock checked before stored references | `ST` answersEveryReferenceAlreadyStored... (`ST:207`) | ✅ Killed |
| W3 | `TransactionImportService.java:88` | stored-reference check runs before the row-error throw | `ST` joinsFormatAndFeeRuleErrors... (`ST:180`) | ✅ Killed |
| W4 | `TransactionCsvParser.java:95` | header cell checked by membership (`Arrays.asList(HEADER).contains`), so column order is ignored | - (targeted and **full suite** pass; probe fails) | ❌ Survived (G9) |
| W5 | `TransactionImportService.java:182` | status saved as constant `APPROVED` | `IT` importsTheFile...; `ST` savesTheBatch... | ✅ Killed |
| W6 | `ITransactionImportRepository.java:13` | batch list ignores the merchant (`@Query ... merchant.id = ?1 or merchant.id <> ?1`) | `IT` listsTheBatches..., sendingTheSameFileAgain...; `RT` findsBatchesOnlyForTheirMerchant | ✅ Killed |
| W7 | `TransactionController.java:99` | `@PathVariable UUID merchantId` → `@PathVariable("merchantId") UUID ownerId` (aspect skips the access guard) | - (targeted and **full suite** pass; probe fails) | ❌ Survived (G8) |
| W8 | `TransactionCsvParser.java:193` | `TransactionStatus.valueOf(raw)` → `valueOf(raw.toUpperCase())` | - (targeted and **full suite** pass; probe fails) | ❌ Survived (G10) |
| W9 | `TransactionImportService.java:109` | audit detail `rows=` uses the error count | `ST` savesTheBatch... (`ST:148-149`) | ✅ Killed |
| W10 | `TransactionImportService.java:183` | `transactionDate` saved one day earlier | `IT` importsTheFile...; `ST` savesTheBatch... | ✅ Killed |
| W11 | `TransactionCsvParser.java:170` | `PaymentMethod.valueOf(raw.toUpperCase())` | `PT` reportsEachRowError... [método minúsculo] | ✅ Killed |
| W12 | `TransactionImportService.java:100` | `importBatch.setMerchant(merchant)` removed | `IT` 6 tests; `ST` savesTheBatch... | ✅ Killed |

**Sensor depth**: expanded (P0-style, data-integrity and access path): 9 re-applied + 12 new across the parser, service, repositories (fee rule, transaction, batch), controller and access wiring
**Result**: 18/21 killed - FAIL ❌ (W4, W7, W8 survived; all 3 change a spec-stated behavior, 0 classified as equivalent)

Classification of the survivors:

- **W7 → gap (major).** The 403 edge case is stated in the spec without limiting it to some endpoints. The guard is applied by parameter name, so it is fragile, and the batch-by-id route is the one merchant-scoped route in this feature that has no 403 test. The mutant lets an operator read another merchant's batch.
- **W4 → gap (minor).** TIMP-04 says "cabeçalho exato". A reordered header is not exact. The mutant answers row errors instead of the header error, and accepts a header that repeats a column name.
- **W8 → gap (minor).** TIMP-10 #12 lists the four status values. `approved` is not one of them. The tests already have this case for the method column (`PT:133`) but not for status.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ Mirrors `SettlementCsvParser` / `ExternalSettlementService` |
| Surgical changes | ✅ Fix commits `f248e0d`, `7ab6276`, `4bc5ebe` change tests only |
| No scope creep | ✅ |
| Matches patterns | ✅ Same handlers, `details` keys and `@PageableDefault` as the settlement import |
| Spec-anchored outcome check | ✅ Exact texts, statuses, row numbers, order, page size |
| Per-layer coverage expectation | ❌ Route: no 403 on `GET /imports/{importId}` (W7). Domain: header column order (W4), status letter case (W8) |
| Every test maps to a requirement | ✅ |
| No test weakened | ✅ `4bc5ebe` rewrites the list fixture with stronger assertions (3 batches, every column) and adds assertions; nothing removed |
| Documented guidelines followed | ✅ `AGENTS.md` (JUnit 5, AssertJ, MockMvc, Testcontainers via `AbstractIntegrationTest`, Flyway, Conventional Commits) |

### Notes (not gaps)

- **Deactivated merchant via HTTP**: only covered through the mocked `findByIdAndActiveTrue` (`ST:246-254`).
- **401 on `GET /imports` and `GET /imports/{importId}`**: no test (only the import, `IT:313-317`). Authentication is global (Spring Security filter chain), not per-handler, so no feature-code mutant can remove it.
- **Header trimming**: `TransactionCsvParser.java:95` trims header cells, so a padded header is accepted (`PT:77-85`). This is a reading of "espaços nas pontas de cada campo são ignorados" and matches the settlement parser.
- **Untested out-of-spec file errors**: "Arquivo CSV vazio" (`TransactionCsvParser.java:52`) and "Erro ao ler arquivo CSV" (`:88`, `TransactionImportService.java:145`) have no test. The spec defines neither.
- **`createdAt` precision**: `Instant.now()` is not truncated to microseconds (candidate L-007). No AC requires the `201` body and a later GET to match.
- **Concurrency untested**: two simultaneous imports of the same new reference rely on `uk_internal_transactions_merchant_external_reference` and the existing integrity handler (candidate L-002). It is a sweep claim, not an AC.
- **`Location` header**: asserted (`IT:238`) though the spec does not state it.
- **W6 kill path**: `IT`'s `assertNothingSaved` helper calls the mutated repository method itself, so part of the W6 kill comes from the helper. `IT:243` (`contains` exactly 3 ids) and `RT:55-64` also kill it on their own.

---

## Gate Check

- **Gate command**: `cd backend && ./mvnw verify`
- **Result**: exit 0, `Tests run: 627, Failures: 0, Errors: 0, Skipped: 0`, "All coverage checks have been met." (JaCoCo 85% line / 75% branch), BUILD SUCCESS
- **Feature tests**: `TransactionCsvParserTest` 42, `TransactionImportServiceTest` 12, `TransactionImportIntegrationTest` 15, `TransactionImportRepositoryIntegrationTest` 3, `ExpectedNetAmountCalculatorTest` 8 (80 new)
- **Test count before feature**: 547; after iteration 1: 622; after iteration 2: 626; after iteration 3: 626; now: 627 (`4bc5ebe` adds `aDeactivatedFeeRuleDoesNotPriceTheImport`)
- **Skipped tests**: none
- **Failures**: none
- **validate_state.py**: `python3 -I <skill>/scripts/validate_state.py transaction-csv-import --root <repo>` → exit 1 ("verdict is FAIL"), expected for a FAIL verdict.

---

## Fix Plans (back to the user: beyond the authorized extra round)

All three are test-only, and they need no production change.

### Fix 8: 403 on the batch-by-id route (W7) - Major

- **Root cause**: `IT:292-300` checks the operator-without-access `403` on import and list only.
- **Fix task**: in `anOperatorWithoutAccessIsForbiddenAndAnUnknownMerchantIsNotFound` (or a new test), import a batch as admin, then `GET /api/merchants/{merchantId}/transactions/imports/{importId}` with the operator token → `status().isForbidden()`.
- **Verify**: W7 (`@PathVariable("merchantId") UUID ownerId` at `TransactionController.java:99`) must fail.

### Fix 9: Header column order (W4) - Minor

- **Fix task**: add to `PT` `rejectsAWrongHeader` a header with the 6 right names in another order (for example `amount,externalReference,paymentMethod,installments,status,transactionDate`) → `hasMessage("Cabeçalho CSV inválido. Esperado: " + HEADER)`.
- **Verify**: W4 must fail.

### Fix 10: Status letter case (W8) - Minor

- **Fix task**: add a `PT` `reportsEachRowErrorWithItsLineAndText` case `"status minúsculo|PED-1,10.00,PIX,1,approved,2026-07-10|Status inválido"`.
- **Verify**: W8 must fail.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| TIMP-01..TIMP-03, TIMP-05..TIMP-09, TIMP-11..TIMP-13 | Implementing | ✅ Verified |
| TIMP-04 | Implementing | ❌ Needs Fix (column order, Fix 9) |
| TIMP-10 | Implementing | ❌ Needs Fix (status case, Fix 10); 16/16 texts ✅ |
| TLOTE-01..TLOTE-05 | Implementing | ✅ Verified |
| Edge case 403 | - | ❌ Needs Fix (batch-by-id, Fix 8) |

(Not edited in `spec.md`; the orchestrator updates it after the user decides.)

---

## Lessons

- **L-028, L-029, L-030, L-031** (iteration 3): followed by `4bc5ebe`; V15 (×4), V11, V16, V7 killed. No recurrence.
- **L-025, L-027**: M1 and N19 still killed. No recurrence.
- New candidates, one per survivor: L-032 (W7), L-033 (W4), L-034 (W8). See `.specs/LESSONS.md`.

## Summary

**Overall**: ❌ Not Ready (iteration 4, the user's extra round)

**Spec-anchored check**: 18/18 ACs cited with spec-outcome assertions, 16/16 TIMP-10 texts, 0 spec-precision gaps; 3 stated behaviors untested (G8 edge case 403, G9 TIMP-04 order, G10 TIMP-10 #12 case)
**Sensor**: 18/21 killed (all 9 re-applied killed; W4, W7, W8 survived)
**Gate**: 627 passed, 0 failed, JaCoCo met

**What works**: everything from iterations 1-3, plus the default sort key, the active-rule filter, the exact header case and the `.csv` dot. 9 of the 12 new mutants were killed: merchant scope of the stored-reference and batch-list queries, check order (row errors → references → period), saved status and date, the batch's merchant, the audit detail and the method letter case.

**Issues found**: the batch-by-id route has no access test (W7), and no test checks header column order (W4) or status letter case (W8).

**Next steps**: the user decides on Fixes 8-10 (all test-only, 3 small additions), or accepts them.
