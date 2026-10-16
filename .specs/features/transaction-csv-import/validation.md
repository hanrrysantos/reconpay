# Import CSV de Transações Validation

**Date**: 2026-10-16
**Spec**: `.specs/features/transaction-csv-import/spec.md`
**Diff range**: `f808eb2..7ab6276` (spec/design `cbd5e1c`, `7c8a77b`; implementation `424b818..af2c5e1`; iteration-1 report `86821c0`; fix `f248e0d`; iteration-2 report `068fa3a`; fix `7ab6276`), branch `feat/transaction-csv-import`
**Verifier**: independent sub-agent (author ≠ verifier), iteration 3 of 3 (last before escalation)

## Validation: transaction-csv-import - FAIL ❌

The iteration-2 gap is closed. N19 (`size = 20` → `size = 10`) and the new variant N19b (the `size` attribute removed, which falls back to `@PageableDefault`'s own default of 10) are both killed by `IT:240` `jsonPath("$.size").value(20)`. M1 and M6 from iteration 1 are still killed. The gate passes: 626 tests, 0 failures, JaCoCo met. All 18 ACs, the 16 TIMP-10 texts and the 7 edge cases still trace to `file:line` assertions on the spec outcome.

The verdict is still FAIL. I ran 16 new mutants on paths not touched in iterations 1-2, and 4 of them survived the **full** test suite. Each one changes a behavior the spec states:

1. **V15 (gap, major): wrong sort key for the batch list.** `TransactionController.java:93` `sort = "createdAt"` → `sort = "fileName"` (still `DESC`). TLOTE-02 says "mais recente primeiro por padrão" and the Assumptions table fixes `createdAt,desc`. The only list test (`IT:224-240`) uses `primeiro.csv`/`segundo.csv` with 1 and 2 rows. Sorted descending by `fileName` (and by `totalRows`), these come out in the same order as newest first, so a wrong sort key gives the expected order by chance.
2. **V11 (gap, major): inactive fee rules are used.** `IFeeRuleRepository.java:17` `findAllByMerchant_IdAndActiveTrue(UUID)` was given a `@Query` without the `active` filter, so the import also uses deactivated rules. TIMP-03 ("regra de taxa ativa") and TIMP-10 #15 ("sem regra de taxa ativa ... Regra de taxa ativa não encontrada") require only active rules. No test imports a row whose only rule for that method and installment count is deactivated. The service test mocks the repository, and the integration tests only create active rules.
3. **V16 (gap, minor): header matched ignoring case.** `TransactionCsvParser.java:95` `equals` → `equalsIgnoreCase`. TIMP-04 requires "o cabeçalho exato", so `externalreference,AMOUNT,...` must get `400`. `PT:96-107` checks only a missing, renamed or extra column.
4. **V7 (gap, minor): extension check without the dot.** `TransactionImportService.java:136` `endsWith(".csv")` → `endsWith("csv")`. TIMP-06 refuses a name that does not end in `.csv`. A name such as `vendascsv` is now accepted. The tests use only `vendas.txt` (refused) and `VENDAS.CSV` (accepted).

File abbreviations (all under `backend/src/test/java/br/com/hanrry/reconpay/`):

- `PT` = `transaction/service/TransactionCsvParserTest.java`
- `ST` = `transaction/service/TransactionImportServiceTest.java`
- `IT` = `transaction/integration/TransactionImportIntegrationTest.java`
- `RT` = `transaction/integration/TransactionImportRepositoryIntegrationTest.java`
- `CT` = `feerule/service/ExpectedNetAmountCalculatorTest.java`

Since iteration 2, only `IT` changed (`7ab6276`, +1 line at `IT:240`). `IT` lines after 239 moved by +1. Every other citation was checked again and has not moved.

---

## Gap History

| Iteration | Commit verified | Gaps | Outcome |
| --- | --- | --- | --- |
| 1 | `af2c5e1` | G1 (M1): accepted side of the 17-integer-digit bound not tested. G2 (M6): a row with both the length error and a duplicate not tested | FAIL; fix routed |
| fix | `f248e0d` | Test only: `PT:149-156` accepts 17 integer digits and `0.01`; `PT:158-169` a 101-char reference repeated gives both errors | - |
| 2 | `f248e0d` | G1 ✅ closed. G2 ✅ closed. New G3 (N19): default page size of the batch list not tested | FAIL; fix routed |
| fix | `7ab6276` | Test only: `IT:240` `jsonPath("$.size").value(20)` | - |
| 3 | `7ab6276` | G3 ✅ closed (N19, N19b killed). M1, M6 still killed. **New G4 (V15)** sort key, **G5 (V11)** inactive rule, **G6 (V16)** header case, **G7 (V7)** extension dot | FAIL; **iteration limit reached → escalate to user** |

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

---

## Re-check of earlier survivors

| Mutant | Re-applied at | Mutation | Killed by | Result |
| --- | --- | --- | --- | --- |
| N19 | `TransactionController.java:93` | `@PageableDefault(size = 20, ...)` → `size = 10` | `IT:240` `listsTheBatchesNewestFirstAndFindsOneById` `jsonPath("$.size").value(20)` | ✅ Killed |
| N19b | `TransactionController.java:93` | `size = 20` attribute removed (`@PageableDefault` own default is 10) | `IT:240` same test | ✅ Killed |
| M1 | `TransactionCsvParser.java:152` | `precision - scale > 17` → `> 16` | `PT:149-156` [1], [2] | ✅ Killed |
| M6 | `TransactionCsvParser.java:119` | duplicate check skipped once the row has the length error | `PT:158-169` | ✅ Killed |

The `IT:240` assertion targets the exact spec default ("20 por página por padrão") on a request with no `page`/`size`/`sort` parameter (`IT:234-235`).

---

## Spec-Anchored Acceptance Criteria

### P1: Importar transações por CSV

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| TIMP-01 | `201` with `id`, `merchantId`, `fileName`, `totalRows` (= rows saved), `createdAt` | `IT:64-68` `status().isCreated()`, `$.merchantId` `merchantId`, `$.fileName` `"vendas-julho.csv"`, `$.totalRows` `2`, `$.createdAt` exists; `IT:70,85` `$.id` equals every saved transaction's `importBatch.id` | ✅ PASS (N8, N20, V10 killed) |
| TIMP-02 | Each row saved with trimmed `externalReference`, `amount`, `paymentMethod`, `installments`, `status`, `transactionDate` | `IT:63` sends `" PED-2 "`; `IT:76-82` `contains("PED-1","PED-2")`, `(100.00, 50.00)`, `("CREDIT_CARD","PIX")`, `(3, 1)`, `("APPROVED","CANCELLED")`, `("2026-07-10","2026-07-11")`; `ST:133-144` | ✅ PASS (V1 killed) |
| TIMP-03 | `expectedNetAmount` = `amount − amount×%÷100 − fixa`, 2 places, `HALF_UP`, same as the single create, **with the active rule** | `CT:18-23,29-30` `96.71`, `9.67`, `0.99`, `250.00`, `scale()` `2`; `IT:78` `contains(96.71, 49.50)`; `IT:89-103` import vs `POST` → `contains(119.46, 119.46)` | ⚠️ Formula ✅ (V12 killed); "ativa" not tested → **G5 (V11)** |
| TIMP-04 | Exact header; otherwise `400 VALIDATION_ERROR` "Cabeçalho CSV inválido. Esperado: ..." | `IT:127-132` `isBadRequest()`, `$.error`, `$.message` exact; `PT:96-107` missing, renamed, extra column → `hasMessage(...)` | ⚠️ Message ✅; "exato" with letter case not tested → **G6 (V16)** |
| TIMP-05 | 0 bytes → `400` "Arquivo CSV é obrigatório"; no `file` → `400 VALIDATION_ERROR` "Requisição inválida. Verifique os campos e parâmetros enviados." | `IT:146-150`, `IT:156-160` message exact; `ST:222-228` | ✅ PASS |
| TIMP-06 | Name not ending in `.csv` (any case) → `400` "Arquivo deve ser um CSV (.csv)" | `IT:152-154` `vendas.txt`; `ST:230-240` `vendas.txt` refused, `VENDAS.CSV` saved | ⚠️ Case ✅ (M11 killed); the dot is not tested → **G7 (V7)** |
| TIMP-07 | Header only, or header plus blank lines → `400` "CSV não contém registros" | `IT:138-140`; `PT:109-117` | ✅ PASS (V3 killed) |
| TIMP-08 | > 5 MB → `413 VALIDATION_ERROR` "Arquivo CSV excede o tamanho máximo permitido de 5MB" | `IT:174-178` `statusCode()==413`, `$.error`, message exact, `assertNothingSaved` | ✅ PASS |
| TIMP-09 | `400 VALIDATION_ERROR` "Erro na importação do CSV", `rowErrors` with `row` (header = 1) and `message` for every error, nothing saved | `IT:113-123` `isBadRequest()`, `$.error`, `$.message`, `rowErrors[*].row` `contains(3,4,5,5)`, messages exact, `assertNothingSaved`; `ST:162-182`; `PT:195-209`; `PT:158-169` | ✅ PASS (V4, V9, V14 killed) |
| TIMP-10 | 16 exact row-error texts | Table below: 16/16 texts | ✅ texts (#15 rests on "ativa", see G5) |
| TIMP-11 | `409 CONFLICT` "Referências externas já cadastradas para este comerciante: <ref1>, <ref2>" + `conflictingReferences`, nothing saved | `IT:186-193` `isConflict()`, `$.error` `CONFLICT`, message exact, `conflictingReferences` `contains("PED-1","PED-2")`, batch count `1`; `ST:195-208` stored `{C, A}` of `A,B,C` → `containsExactly("A","C")` | ✅ PASS (V8, V13 killed) |
| TIMP-12 | `409 CONFLICT` "Janela travada de <de> a <até>", nothing saved | `IT:197-212` real lock, `isConflict()`, `$.error`, message exact, `assertNothingSaved`; `ST:210-220` | ✅ PASS |
| TIMP-13 | Same reference as another merchant's transaction is accepted | `IT:215-221` → `isCreated()` | ✅ PASS |

#### TIMP-10 row-error texts (16/16)

The parameterized cases assert `containsExactly(new ImportRowErrorDTO(3, message))` at `PT:145`, and that the valid row next to the bad one is kept (`PT:146`).

| # | Spec text | Evidence | Result |
| --- | --- | --- | --- |
| 1 | "Número de colunas inválido. Esperado: 6" | `PT:121` (5), `PT:122` (7) | ✅ |
| 2 | "Referência externa é obrigatória" | `PT:123` | ✅ (V3 killed) |
| 3 | "Referência externa deve ter no máximo 100 caracteres" | `PT:171-179` 101 refused, 100 accepted; `PT:165-167` | ✅ |
| 4 | "Referência externa duplicada no arquivo: <referência>" | `PT:188-191`; `PT:168` | ✅ |
| 5 | "amount inválido" | `PT:124`; `PT:202` | ✅ |
| 6 | "amount deve ter no máximo 17 dígitos inteiros e 2 casas decimais" | `PT:125-126` refused; `PT:149-156` accepted | ✅ |
| 7 | "amount deve ser maior que zero" | `PT:127` `0.00`, `PT:128` `-5.00`; `PT:189`; `IT:118` | ✅ (V2 killed) |
| 8 | "Método de pagamento inválido" | `PT:129-130` | ✅ |
| 9 | "Número de parcelas inválido" | `PT:131-132` | ✅ |
| 10 | "Número de parcelas deve ser no mínimo 1" | `PT:133`; `PT:204` | ✅ |
| 11 | "Método de pagamento <MÉTODO> não permite parcelamento" | `PT:134-136`; `PT:207`; `IT:120` | ✅ |
| 12 | "Status inválido" | `PT:137`; `IT:121` | ✅ |
| 13 | "Data da transação inválida. Formato esperado: yyyy-MM-dd" | `PT:138` `10/07/2026`, `PT:139` `2026-02-30` | ✅ (V6 killed) |
| 14 | "Data da transação não pode ser futura" | `PT:140` today + 1 | ✅ (V5 killed) |
| 15 | "Regra de taxa ativa não encontrada para <MÉTODO> em <N>x" | `ST:173,175`; `ST:190`; `IT:119` (no rule at all) | ✅ text; ⚠️ a deactivated rule is not tested (G5) |
| 16 | "Valor líquido esperado deve ser maior que zero" | `ST:176`; `CT:33-38` | ✅ (V9 killed) |

### P1: Lote rastreável

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| TLOTE-01 | Batch saved with the original file name; each transaction linked to it | `IT:66` `$.fileName`; `IT:84-85` `getImportBatch().getId()` equals `$.id`; `RT:48-49` | ✅ PASS (V10 killed) |
| TLOTE-02 | `200`, merchant page, **newest first by default** (`createdAt,desc`), 20 per page, items with `id`, `merchantId`, `fileName`, `totalRows`, `createdAt` | `IT:234-240` no params → `isOk()`, `$.content[*].id` `contains(second, first)`, `fileName`, `totalRows` `(2, 1)`, `$.size` `20`; `RT:61-62` | ⚠️ Size ✅ (N19, N19b killed), direction ✅ (M18 killed); sort key not discriminated → **G4 (V15)** |
| TLOTE-03 | `200` with the batch | `IT:242-249` `isOk()`, `id`, `merchantId`, `fileName` `"primeiro.csv"`, `totalRows` `1`, `createdAt` exists | ✅ PASS |
| TLOTE-04 | Missing or another merchant's batch → `404 NOT_FOUND` "Importação não encontrada com id: <importId>" | `IT:258-262` `isNotFound()`, `$.error`, message exact; `ST:274-277` | ✅ PASS |
| TLOTE-05 | Transactions from `POST /transactions` have no batch | `RT:82-92` `getImportBatch()` `isNull()` | ✅ PASS |

**Status**: every AC has a `file:line` citation and an assertion on the spec outcome. 0 spec-precision gaps. ❌ 4 ACs (TIMP-03, TIMP-04, TIMP-06, TLOTE-02) have a stated behavior that a surviving mutant breaks without any test failing.

---

## Edge Cases

- [x] Unknown or deactivated merchant → `404` "Comerciante não encontrado com id: <merchantId>": `IT:277-279` import (exact message), `IT:280-282` list; `ST:242-251`; `ST:280-287`.
- [x] No access → `403`: `IT:268-274` import and list.
- [x] No token → `401`: `IT:287-291` import.
- [x] Blank lines skipped, real line numbers kept: `PT:87-94`.
- [x] Padded fields accepted: `PT:77-85`; `IT:63`/`IT:76` (V1 killed).
- [x] Duplicate whose first occurrence has another error → both reported: `PT:181-193`.
- [x] `transactionDate` today accepted: `PT:72-75`.

---

## Discrimination Sensor

Scratch: `git worktree add --detach` at `/tmp/claude-1000/-home-hanrry-orca-workspaces-reconpay-alinhamento-backend-x-frontend/486a7ab6-2e48-4c98-bf3f-aceaa163f4d5/scratchpad/sensor` (HEAD `7ab6276`). A runner script kept outside the worktree applied each mutant by exact-string replacement (it checks the old text matches exactly once). It ran `./mvnw -q -Djacoco.skip=true test -Dtest=TransactionCsvParserTest,TransactionImportServiceTest,TransactionImportIntegrationTest,TransactionImportRepositoryIntegrationTest,ExpectedNetAmountCalculatorTest,TransactionServiceTest,HttpErrorContractTest`, then reverted with `git checkout -- backend/src/main`. Every survivor was run again against the **full** suite (`./mvnw -q -Djacoco.skip=true test`) and survived again. The worktree was removed with `git worktree remove --force` plus `git worktree prune`. The real tree's `git status --porcelain` before and after is exactly `?? frontend/AGENTS.md` (`diff` empty).

### Re-applied (N19, N19b, M1, M6)

See "Re-check of earlier survivors" above: 4/4 killed.

### New mutants (paths not mutated in iterations 1-2)

| # | File:line | Mutation | Killed by | Result |
| --- | --- | --- | --- | --- |
| V1 | `TransactionCsvParser.java:108` | `externalReference` not trimmed | `PT` trimsEveryField; `ST` savesTheBatch...; `IT` importsTheFile... | ✅ Killed |
| V2 | `TransactionCsvParser.java:157` | amount `<= 0` → `< 0` (zero accepted) | `PT` [7], skipsBlankLines..., marksEveryRepeated...; `ST` joinsFormat...; `IT` answersEveryRowError... | ✅ Killed |
| V3 | `TransactionCsvParser.java:217` | blank row `allMatch` → `anyMatch` (a row with one empty field is skipped) | `PT` reportsEachRowError... [3] | ✅ Killed |
| V4 | `TransactionCsvParser.java:141` | a row with exactly one error is kept as valid (`> errorsBefore + 1`) | `PT` 11 cases; `IT` answersEveryRowError... | ✅ Killed |
| V5 | `TransactionCsvParser.java:203` | future check `isAfter(today)` → `isAfter(today + 1)` | `PT` [20] data futura | ✅ Killed |
| V6 | `TransactionCsvParser.java:44` | `ResolverStyle.STRICT` → `SMART` (`2026-02-30` accepted) | `PT` [19] data inexistente | ✅ Killed |
| V7 | `TransactionImportService.java:136` | `endsWith(".csv")` → `endsWith("csv")` | - (targeted and **full suite** pass) | ❌ Survived (G7) |
| V8 | `TransactionImportService.java:161` | every reference in the file reported as conflicting once any is stored | `ST` answersEveryReferenceAlreadyStored... | ✅ Killed |
| V9 | `TransactionImportService.java:84` | net-amount error swallowed (`continue` instead of `errors.add`) | `ST` joinsFormatAndFeeRuleErrors... | ✅ Killed |
| V10 | `TransactionImportService.java:101` | batch `fileName` → constant `"import.csv"` | `ST` savesTheBatch...; `IT` importsTheFile..., listsTheBatches... | ✅ Killed |
| V11 | `IFeeRuleRepository.java:17` | `@Query("select f from FeeRuleEntity f where f.merchant.id = ?1")` on `findAllByMerchant_IdAndActiveTrue(UUID)` (inactive rules used) | - (targeted and **full suite** pass) | ❌ Survived (G5) |
| V12 | `ExpectedNetAmountCalculator.java:24` | `.subtract(percentageFee)` → `.add(percentageFee)` | `CT` 4 cases; `ST` 2; `IT` 2; `TransactionServiceTest` 2 | ✅ Killed |
| V13 | `GlobalExceptionHandler.java:189` | duplicate-reference handler `ApiErrorCode.CONFLICT` → `VALIDATION_ERROR` | `IT` sendingTheSameFileAgain... | ✅ Killed |
| V14 | `GlobalExceptionHandler.java:265` | row-error handler `ApiErrorCode.VALIDATION_ERROR` → `CONFLICT` | `IT` answersEveryRowErrorAtOnce... | ✅ Killed |
| V15 | `TransactionController.java:93` | default sort `createdAt` → `fileName` (still `DESC`) | - (targeted and **full suite** pass) | ❌ Survived (G4) |
| V16 | `TransactionCsvParser.java:95` | header `equals` → `equalsIgnoreCase` | - (targeted and **full suite** pass) | ❌ Survived (G6) |

**Sensor depth**: expanded (P0-style, data-integrity path): 4 re-applied + 16 new across the parser, service, calculator, repository query, handlers and controller
**Result**: 16/20 killed - FAIL ❌ (V7, V11, V15, V16 survived; all 4 change a spec-stated behavior, 0 classified as equivalent)

Classification of the survivors:

- **V15 → gap.** TLOTE-02 and the Assumptions table fix the default order (`createdAt,desc`). The fixture has `fileName` and `totalRows` that sort the same way as `createdAt`, so the test cannot tell the keys apart. Related to candidate L-012 (fixtures whose other order contradicts the expected one).
- **V11 → gap.** TIMP-03 and TIMP-10 #15 say "ativa". With an inactive PIX 1x rule and no active one, the mutant computes a net from the deactivated rule and saves the row instead of answering the row error. In money terms, this is the most serious of the four.
- **V16 → gap (minor).** TIMP-04 requires the exact header. Trimming the header is a reasonable reading of "espaços nas pontas ... ignorados" (see notes), but accepting any letter case is not covered by any decision in the spec.
- **V7 → gap (minor).** TIMP-06 says the name must end in `.csv`. `vendascsv` does not, but the mutant accepts it.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ Mirrors `SettlementCsvParser` / `ExternalSettlementService` |
| Surgical changes | ✅ Fix commits `f248e0d`, `7ab6276` change tests only |
| No scope creep | ✅ |
| Matches patterns | ✅ Same handlers, `details` keys and `@PageableDefault` as the settlement import |
| Spec-anchored outcome check | ✅ Exact texts, statuses, row numbers, order, page size |
| Per-layer coverage expectation | ❌ Route: default sort key not discriminated (V15). Domain: active-rule filter (V11), exact header case (V16), extension dot (V7) not tested |
| Every test maps to a requirement | ✅ |
| No test weakened | ✅ `7ab6276` adds one assertion and removes nothing |
| Documented guidelines followed | ✅ `AGENTS.md` (JUnit 5, AssertJ, MockMvc, Testcontainers via `AbstractIntegrationTest`, Flyway, Conventional Commits) |

### Notes (not gaps, kept from iterations 1-2)

- **Deactivated merchant via HTTP**: only covered through the mocked `findByIdAndActiveTrue` (`ST:242-251`).
- **403/401 on `GET /imports/{importId}`**: no test. Import and list have them (`IT:268-274`, `IT:287-291`).
- **Header trimming**: `TransactionCsvParser.java:95` trims header cells, so a padded header is accepted (`PT:77-85`). This is a reading of "espaços nas pontas de cada campo são ignorados" and matches the settlement parser. It is separate from V16 (letter case).
- **Untested out-of-spec file errors**: "Arquivo CSV vazio" (`TransactionCsvParser.java:52`) and "Erro ao ler arquivo CSV" (`:88`, `TransactionImportService.java:145`) have no test. The spec defines neither.
- **`createdAt` precision**: `Instant.now()` is not truncated to microseconds (candidate L-007). No AC requires the `201` body and a later GET to match.
- **Concurrency untested**: two simultaneous imports of the same new reference rely on `uk_internal_transactions_merchant_external_reference` and the existing integrity handler (candidate L-002). It is a sweep claim, not an AC.
- **`Location` header**: asserted (`IT:232`) though the spec does not state it.

---

## Gate Check

- **Gate command**: `cd backend && ./mvnw verify`
- **Result**: exit 0, `Tests run: 626, Failures: 0, Errors: 0, Skipped: 0`, "All coverage checks have been met." (JaCoCo 85% line / 75% branch), BUILD SUCCESS
- **Feature tests**: `TransactionCsvParserTest` 42, `TransactionImportServiceTest` 12, `TransactionImportIntegrationTest` 14, `TransactionImportRepositoryIntegrationTest` 3, `ExpectedNetAmountCalculatorTest` 8 (79 new)
- **Test count before feature**: 547; after iteration 1: 622; after iteration 2: 626; now: 626 (`7ab6276` adds an assertion to an existing test, not a test)
- **Skipped tests**: none
- **Failures**: none
- **validate_state.py**: `python3 -I <skill>/scripts/validate_state.py transaction-csv-import --root <repo>` → exit 1 ("verdict is FAIL"), expected for a FAIL verdict.

---

## Fix Plans (escalated: iteration limit reached)

The 3 fix and re-verify rounds are used up. These go to the user to decide: fix now, accept, or change the spec.

### Fix 4: Discriminate the default sort key of the batch list (V15) - Major

- **Root cause**: `IT:224-240` fixtures sort the same way by `createdAt`, `fileName` and `totalRows`.
- **Fix task**: in the list test, give the older batch the name that sorts later and more rows (for example, first `z-antigo.csv` with 2 rows, then `a-novo.csv` with 1 row), and assert `$.content[*].id` `contains(newer, older)` with no `sort` parameter.
- **Verify**: V15 (`sort = "fileName"`) and `sort = "totalRows"` at `TransactionController.java:93` must fail.

### Fix 5: Ignore deactivated fee rules in the import (V11) - Major

- **Root cause**: no test imports a row whose method and installment count has only a deactivated rule.
- **Fix task**: integration test: create a PIX 1x rule, deactivate it through the existing fee-rule endpoint, import a PIX 1x row, and assert `400` with `rowErrors` `(2, "Regra de taxa ativa não encontrada para PIX em 1x")` and nothing saved. Optionally also a repository test: `findAllByMerchant_IdAndActiveTrue(UUID)` excludes inactive rules.
- **Verify**: V11 must fail.

### Fix 6: Exact header letter case (V16) - Minor

- **Fix task**: add a case to `PT` `rejectsAWrongHeader` with `externalreference,amount,paymentMethod,installments,status,transactionDate` → `hasMessage("Cabeçalho CSV inválido. Esperado: ...")`.
- **Verify**: V16 must fail.

### Fix 7: Extension must include the dot (V7) - Minor

- **Fix task**: in `ST` `rejectsAFileThatIsNotCsv...`, also refuse `vendascsv` with "Arquivo deve ser um CSV (.csv)".
- **Verify**: V7 must fail.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| TIMP-01, TIMP-02, TIMP-05, TIMP-07..TIMP-13 | Implementing | ✅ Verified |
| TIMP-03 | Implementing | ❌ Needs Fix (active-rule filter, Fix 5) |
| TIMP-04 | Implementing | ❌ Needs Fix (exact header case, Fix 6) |
| TIMP-06 | Implementing | ❌ Needs Fix (extension dot, Fix 7) |
| TLOTE-01, TLOTE-03..TLOTE-05 | Implementing | ✅ Verified |
| TLOTE-02 | Implementing | ❌ Needs Fix (sort key, Fix 4); page size ✅ |

(Not edited in `spec.md`; the orchestrator updates it after the user decides.)

---

## Lessons

- **L-027** (iteration 2): followed by `7ab6276`; N19 and N19b killed. No recurrence.
- **L-025, L-026**: M1 and M6 still killed. No recurrence.
- New candidates, one per survivor: L-028 (V15), L-029 (V11), L-030 (V16), L-031 (V7). See `.specs/LESSONS.md`.

## Summary

**Overall**: ❌ Not Ready, escalated to the user (iteration 3 of 3)

**Spec-anchored check**: 18/18 ACs cited with spec-outcome assertions, 16/16 TIMP-10 texts, 0 spec-precision gaps; 4 ACs with an untested stated behavior (G4-G7)
**Sensor**: 16/20 killed (N19, N19b, M1, M6 killed; V7, V11, V15, V16 survived)
**Gate**: 626 passed, 0 failed, JaCoCo met

**What works**: everything from iterations 1-2, plus the pinned default page size. 12 of the 16 new mutants were killed: trimming, zero amount, blank rows, partial row errors, future and impossible dates, the conflict filter, the swallowed net error, the batch file name, the net formula and both handler error codes.

**Issues found**: the default sort key (V15), the active-rule filter (V11), header letter case (V16) and the `.csv` dot (V7) are not tested.

**Next steps**: the user decides on Fixes 4-7 (all test-only, 4 small tests).
