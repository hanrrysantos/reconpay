# Layouts and Statement Validation

**Result**: PASS

**Date**: 2026-10-07
**Spec**: `.specs/features/layouts-and-statement/spec.md`
**Diff range**: `033e64d..HEAD` (HEAD `5f9a488`)
**Verifier**: independent sub-agent (author ≠ verifier)

28 of 28 acceptance criteria assert the spec outcome. 0 spec-precision gaps. 4 of 4 edge cases match. `./mvnw -B verify` passed 316 tests. Five behavior mutants were killed, including a 413-to-400 swap and a concurrent-duplicate handler that returned 201 for both. LAY-04, LAY-06, LAY-13, and LAY-26 are closed by those assertions.

`server.tomcat.max-swallow-size: -1` is necessary for a real upload over 5MB to come back as HTTP 413. It does not raise the 5MB accept limit.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 | Done | Every "Done when" box is checked. |
| T2 | Done | Every "Done when" box is checked. |
| T3 | Done | Every "Done when" box is checked. |
| T4 | Done | Every "Done when" box is checked. |
| T5 | Done | Every "Done when" box is checked. |
| T6 | Done | Every "Done when" box is checked. |
| T7 | Done | Every "Done when" box is checked. |
| T8 | Done | Every "Done when" box is checked. |
| T9 | Done | Every "Done when" box is checked. |
| T10 | Done | Every "Done when" box is checked. |
| T11 | Done | Every "Done when" box is checked. |
| T12 | Done | Every "Done when" box is checked. |
| T13 | Done | Every "Done when" box is checked. |
| T14 | Done | Every "Done when" box is checked. |

No task is blocked or partial. The tasks header still says "In Progress".

Requirement IDs follow `spec.md`: LAY-01–LAY-07 settlement import, LAY-08–LAY-13 statement import, LAY-14–LAY-24 pairing, LAY-25–LAY-28 the edge cases in the order written.

---

## Spec-Anchored Acceptance Criteria

### P1: Importar a liquidação com a receita

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| LAY-01 WHEN an OPERATOR with a grant posts the settlement import without `layout` or with `layout=RECONPAY`, the RECONPAY header, and valid new rows THEN HTTP 201, one batch and one settlement per row, and `SETTLEMENTS_IMPORTED` only after commit | HTTP 201; one batch; one settlement; event `SETTLEMENTS_IMPORTED` after commit | `ExternalSettlementIntegrationTest.java:93` - `status().isCreated()`; `:95` - `jsonPath("$.totalRows").value(1)`; `:118` - `jsonPath("$.content[0].externalReference").value(externalReference)`; `:253` - `status().isCreated()` with `layout=RECONPAY`; `:259` - `jsonPath("$.content[0].externalReference").value(externalReference)`. Persist and event order: `ExternalSettlementServiceTest.java:124` - `assertThat(capturedImport.getTotalRows()).isEqualTo(1)`; `:131` - `assertThat(capturedSettlement.getExternalReference()).isEqualTo("EXT-001")`; `:139` - `inOrder.verify(auditLogger).record("SETTLEMENTS_IMPORTED", ...)`. After commit: `AuditLoggerTest.java:43` - `assertThat(events.list).isEmpty()` inside the transaction; `:47` - `assertThat(events.list).hasSize(1)` after commit; `:58` - `assertThat(events.list).isEmpty()` on rollback | PASS |
| LAY-02 WHEN `layout=ACQUIRER` and the acquirer header is sent THEN each row is stored with externalReference, amount, netAmount, paymentMethod, installments, status, and settlementDate from those columns in that order | `nsu` → externalReference, `valor_bruto` → amount, `valor_liquido` → netAmount, and the remaining columns in header order | `ExternalSettlementIntegrationTest.java:276` - `status().isCreated()`; `:283` - `jsonPath("$.content[0].externalReference").value(nsu)`; `:284` - `jsonPath("$.content[0].amount").value(200.00)`; `:285` - `jsonPath("$.content[0].netAmount").value(190.50)`; `:286` - `jsonPath("$.content[0].paymentMethod").value("PIX")`; `:287` - `jsonPath("$.content[0].installments").value(1)`; `:288` - `jsonPath("$.content[0].status").value("APPROVED")`; `:289` - `jsonPath("$.content[0].settlementDate").value("2026-07-29")`. Parser: `SettlementCsvParserTest.java:189` - `assertThat(row.externalReference()).isEqualTo("NSU-001")`; `:190` - `assertThat(row.amount()).isEqualByComparingTo("150.00")`; `:191` - `assertThat(row.netAmount()).isEqualByComparingTo("145.00")` | PASS |
| LAY-03 IF `layout` is neither RECONPAY nor ACQUIRER, or the header does not match THEN HTTP 400 `VALIDATION_ERROR` and neither batch nor row is stored | HTTP 400 `VALIDATION_ERROR`; zero settlements and zero imports | `ExternalSettlementIntegrationTest.java:303` - `status().isBadRequest()`; `:304` - `jsonPath("$.error").value("VALIDATION_ERROR")`; `:306` - `assertNothingPersisted` (`:601` and `:605` - `jsonPath("$.totalElements").value(0)`). Wrong header: `:320` - `status().isBadRequest()`; `:321` - `jsonPath("$.error").value("VALIDATION_ERROR")`; `:322` - message contains `nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao`; `:325` - `assertNothingPersisted` | PASS |
| LAY-04 IF a row breaks settlement value rules, repeats an externalReference in the file, or the file is empty, missing, or not a `.csv` THEN HTTP 400 `VALIDATION_ERROR` and nothing is stored. A row error includes `rowErrors` | HTTP 400 `VALIDATION_ERROR`; `rowErrors` on a row error; nothing stored, including an empty file | Value rules: `ExternalSettlementIntegrationTest.java:184` - `status().isBadRequest()`; `:185` - `jsonPath("$.error").value("VALIDATION_ERROR")`; `:186` - `jsonPath("$.details.rowErrors[0].message").value("netAmount não pode ser maior que amount")`; `:189` - `assertNothingPersisted`. In-file duplicate: `:359` - `status().isBadRequest()`; `:360` - `jsonPath("$.error").value("VALIDATION_ERROR")`; `:361` - `jsonPath("$.details.rowErrors[0].row").value(3)`; `:365` - `assertNothingPersisted`. Missing and non-csv: `:372` - `status().isBadRequest()`; `:373` - `jsonPath("$.error").value("VALIDATION_ERROR")`; `:384` - `status().isBadRequest()`; `:385` - `jsonPath("$.error").value("VALIDATION_ERROR")`; `:388` - `assertNothingPersisted`. Empty file: `:402` - `status().isBadRequest()`; `:403` - `jsonPath("$.error").value("VALIDATION_ERROR")`; `:406` - `assertNothingPersisted` (`:601` and `:605` - `jsonPath("$.totalElements").value(0)`) | PASS |
| LAY-05 IF an externalReference already exists for the merchant THEN HTTP 409 `CONFLICT` and nothing new is stored | HTTP 409 `CONFLICT`; the first batch remains; the second adds no row | `ExternalSettlementIntegrationTest.java:149` - `status().isCreated()`; `:159` - `status().isConflict()`; `:160` - `jsonPath("$.error").value("CONFLICT")`; `:166` - `jsonPath("$.totalElements").value(1)` on imports; `:170` - `jsonPath("$.totalElements").value(1)` on settlements | PASS |
| LAY-06 IF the file is larger than 5MB THEN HTTP 413 `VALIDATION_ERROR` and nothing is stored | HTTP 413 `VALIDATION_ERROR`; nothing stored | `ExternalSettlementIntegrationTest.java:418` - `assertThat(response.statusCode()).isEqualTo(413)`; `:420` - `isEqualTo("VALIDATION_ERROR")`; `:424` - `assertNothingPersisted`. Body is `DataSize.ofMegabytes(5).toBytes() + 1` sent with `HttpClient` to the live port (`IntegrationTestUtils.java:110`, `:113`) | PASS |
| LAY-07 Unauthenticated import is HTTP 401, an OPERATOR without a grant is HTTP 403, and an ADMIN with no `user_merchants` row gets HTTP 201 | HTTP 401; HTTP 403; HTTP 201 | `ExternalSettlementIntegrationTest.java:504` - `status().isUnauthorized()`; `:506` - `assertNothingPersisted`; `:521` - `status().isForbidden()`; `:522` - `jsonPath("$.error").value("FORBIDDEN")`; `:556` - admin `merchantIds` does not contain this merchant; `:568` - `status().isCreated()`; `:574` - `jsonPath("$.content[0].externalReference").value(externalReference)` | PASS |

### P1: Importar o extrato

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| LAY-08 WHEN an OPERATOR with a grant posts a valid new bank CSV THEN HTTP 201 with batch id, file name, and row count, one line per row, and `BANK_STATEMENTS_IMPORTED` only after commit | HTTP 201; id, file name, row count; one line per row; event after commit | `BankStatementIntegrationTest.java:81` - `status().isCreated()`; `:82` - `jsonPath("$.id").isNotEmpty()`; `:83` - `jsonPath("$.fileName").value("statement.csv")`; `:84` - `jsonPath("$.totalRows").value(2)`. Service: `BankStatementServiceTest.java:121` - `assertThat(savedLines).hasSize(2)`; `:145` - `inOrder.verify(auditLogger).record("BANK_STATEMENTS_IMPORTED", ...)`. After commit: `AuditLoggerTest.java:43` - `assertThat(events.list).isEmpty()`; `:47` - `assertThat(events.list).hasSize(1)` | PASS |
| LAY-09 WHEN externalReference is blank THEN it is stored as null and lineReference, amount, and movementDate are kept | `externalReference` null; the other three fields kept | `BankStatementIntegrationTest.java:113` - `jsonPath("$.content[1].lineReference").value(withoutCode)`; `:114` - `jsonPath("$.content[1].externalReference").value(nullValue())`; `:115` - `jsonPath("$.content[1].amount").value(80.00)`; `:116` - `jsonPath("$.content[1].movementDate").value("2026-07-29")`. Parser: `BankStatementCsvParserTest.java:48` - `assertThat(rows.get(0).externalReference()).isNull()`; `:47` - `assertThat(rows.get(0).lineReference()).isEqualTo("LN-001")`; `:49` - `assertThat(rows.get(0).amount()).isEqualByComparingTo("150.50")`; `:50` - `assertThat(rows.get(0).movementDate()).isEqualTo(LocalDate.parse("2026-07-30"))` | PASS |
| LAY-10 WHEN the OPERATOR lists bank statements THEN only that merchant's lines come back, each with id, lineReference, externalReference, amount, movementDate, and import id | Those six fields; the other merchant's line is absent | `BankStatementIntegrationTest.java:107` - `jsonPath("$.content[0].id").isNotEmpty()`; `:108` - `jsonPath("$.content[0].lineReference").value(withCode)`; `:109` - `jsonPath("$.content[0].externalReference").value("TXN-001")`; `:110` - `jsonPath("$.content[0].amount").value(150.50)`; `:111` - `jsonPath("$.content[0].movementDate").value("2026-07-30")`; `:112` - `jsonPath("$.content[0].importId").value(importId)`; `:118` - `jsonPath("$.content[?(@.lineReference=='" + otherLine + "')]").isEmpty()`; `:124` - the other merchant lists only `otherLine` | PASS |
| LAY-11 IF lineReference is blank, longer than 100, or repeated in the file, externalReference is longer than 100, amount is not positive or exceeds 17 integer digits or 2 decimals, or movementDate is not a strict `uuuu-MM-dd` on or before today THEN HTTP 400 `VALIDATION_ERROR` with `rowErrors` and nothing stored | HTTP 400 `VALIDATION_ERROR`; `rowErrors`; nothing stored | HTTP and persist-nothing: `BankStatementIntegrationTest.java:136` - `status().isBadRequest()`; `:137` - `jsonPath("$.error").value("VALIDATION_ERROR")`; `:138` - `jsonPath("$.details.rowErrors[0].row").value(2)`; `:141` - `assertNothingPersisted` (`:419` - `jsonPath("$.totalElements").value(0)`). In-file duplicate: `:153` - `status().isBadRequest()`; `:154` - `jsonPath("$.error").value("VALIDATION_ERROR")`; `:155` - `jsonPath("$.details.rowErrors[0].row").value(3)`; `:159` - `assertNothingPersisted`. Same row-error exception: `BankStatementCsvParserTest.java:95` - `containsExactly(tuple(2, "Referência da linha é obrigatória"))`; `:107` - length `no máximo 100 caracteres`; `:122` - duplicate `tuple(3, "Referência da linha duplicada no arquivo: LN-001")`; `:134` - externalReference length; `:268` via `:146` and `:152` - non-positive `amount deve ser maior que zero`; `:162` - 18 integer digits; `:172` - `10.001`; `:192` - `30/07/2026`; `:198` - `2026-02-29`; `:208` - future `2026-08-02` | PASS |
| LAY-12 IF lineReference already exists for the merchant THEN HTTP 409 `CONFLICT` and nothing new is stored | HTTP 409 `CONFLICT`; first import remains; counts stay 1 | `BankStatementIntegrationTest.java:172` - `status().isCreated()`; `:180` - `status().isConflict()`; `:181` - `jsonPath("$.error").value("CONFLICT")`; `:187` - `jsonPath("$.totalElements").value(1)`; `:193` - `assertThat(countImports(merchantId)).isEqualTo(1L)`; `:194` - `assertThat(countLines(merchantId)).isEqualTo(1L)` | PASS |
| LAY-13 The statement import uses the same authentication, grant, ADMIN, file presence, `.csv` suffix, and 5MB responses as the settlement import | HTTP 401, 403, 201, 400 for missing/empty/non-csv, and HTTP 413 for a file over 5MB, with nothing stored on the errors | `BankStatementIntegrationTest.java:338` - `status().isUnauthorized()`; `:353` - `status().isForbidden()`; `:354` - `jsonPath("$.error").value("FORBIDDEN")`; `:375` - `jsonPath("$.merchantIds").isEmpty()`; `:384` - `status().isCreated()`; `:217` - missing file `status().isBadRequest()` and `:218` - `VALIDATION_ERROR`; `:228` - empty file `status().isBadRequest()`; `:229` - `VALIDATION_ERROR`; `:240` - non-csv `status().isBadRequest()`; `:244` - `assertNothingPersisted`. Over 5MB: `:256` - `assertThat(response.statusCode()).isEqualTo(413)`; `:258` - `isEqualTo("VALIDATION_ERROR")`; `:262` - `assertNothingPersisted` | PASS |

### P1: Cruzar extrato e liquidação

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| LAY-14 WHEN a bank line in the extended window shares the settlement reference and `abs(amount - netAmount)` is less than or equal to `amount-tolerance` THEN they pair and the pair has no bank discrepancy | Pair; `MATCHED`; no bank discrepancy, including when the difference equals the tolerance and the dates differ | `BankStatementMatcherTest.java:54` - `assertThat(settlement.getResult()).isEqualTo(ReconciliationResult.MATCHED)` with tolerance `0.05` and amounts `100.00` / `100.05`; `:55` - `assertThat(settlement.getDiscrepancies()).isEmpty()`; `:41` - `MATCHED` when dates differ and amounts are equal; `:42` - `assertThat(settlement.getDiscrepancies()).isEmpty()`. Completed run: `ReconciliationIntegrationTest.java:225` - `assertThat(types(items, exactReference)).isEmpty()`; `:226` - `assertThat(resultOf(items, exactReference)).isEqualTo("MATCHED")`; `:236` - lagged line inside the extended window `resultOf(...).isEqualTo("MATCHED")` | PASS |
| LAY-15 WHEN the reference matches and the amount difference is greater than `amount-tolerance` THEN `BANK_AMOUNT_MISMATCH` with expectedValue = settlement netAmount and actualValue = bank amount, and not `MISSING_BANK_CREDIT` | `BANK_AMOUNT_MISMATCH` only; expected `10.50` / actual `10.56` at scale 2; the fallback line is not used for that settlement | `BankStatementMatcherTest.java:72` - `containsExactly(DiscrepancyType.BANK_AMOUNT_MISMATCH)`; `:75` - `assertThat(mismatchDiscrepancy.getExpectedValue()).isEqualTo("10.50")`; `:76` - `assertThat(mismatchDiscrepancy.getActualValue()).isEqualTo("10.56")`. Run: `ReconciliationIntegrationTest.java:227` - `assertDiscrepancy(..., "BANK_AMOUNT_MISMATCH", "145.00", "100.00")`; `:228` - `containsExactly("BANK_AMOUNT_MISMATCH")` | PASS |
| LAY-16 WHEN the line has a null reference, the dates match, the amount is within tolerance, and no other unpaired line or settlement shares that eligibility THEN they pair with no bank discrepancy | `MATCHED`; no discrepancy; difference equal to tolerance still pairs | `BankStatementMatcherTest.java:102` - `assertThat(settlement.getResult()).isEqualTo(ReconciliationResult.MATCHED)`; `:103` - `assertThat(settlement.getDiscrepancies()).isEmpty()` for a null reference, same date, amounts `100.00` / `100.05`, tolerance `0.05` | PASS |
| LAY-17 IF a settlement has no paired line and is not a `BANK_AMOUNT_MISMATCH` THEN `MISSING_BANK_CREDIT` when zero lines are eligible, or `AMBIGUOUS_BANK_MATCH` when more than one is eligible, and none of those lines is paired | `MISSING_BANK_CREDIT` for zero eligible lines; `AMBIGUOUS_BANK_MATCH` for two lines; line items have no settlement | Zero eligible: `BankStatementMatcherTest.java:116` - `containsExactly(DiscrepancyType.MISSING_BANK_CREDIT)` when the date differs; `:144` - the same when `100.06` exceeds tolerance `0.05`; `:150` - the line is `ORPHAN_BANK_CREDIT`. Two lines: `:169` - `containsExactly(DiscrepancyType.AMBIGUOUS_BANK_MATCH)`; `:378` - `containsExactly(DiscrepancyType.AMBIGUOUS_BANK_MATCH)` on the line; `:375` - `assertThat(lineItem.getExternalSettlement()).isNull()` | PASS |
| LAY-18 IF a bank line in the window has no paired settlement THEN `ORPHAN_BANK_CREDIT` when zero settlements are eligible, or `AMBIGUOUS_BANK_MATCH` when more than one is eligible | `ORPHAN_BANK_CREDIT` or `AMBIGUOUS_BANK_MATCH` on an item whose reference is `lineReference` | `BankStatementMatcherTest.java:204` - `assertThat(orphan.getExternalReference()).isEqualTo("L-ORPHAN")`; `:210` - `containsExactly(DiscrepancyType.ORPHAN_BANK_CREDIT)`; `:214` - `assertThat(discrepancy.getActualValue()).isEqualTo("10.50")`. Two settlements: `:193` - `assertAmbiguousBankLine` which at `:378` expects `AMBIGUOUS_BANK_MATCH` and at `:382` `actualValue` `100.00` | PASS |
| LAY-19 The system ignores a bank line whose reference matches no settlement in the run and matches a transaction only outside the run window | The outside line is not an item | `BankStatementMatcherTest.java:227` - `containsExactly("TXN-1")`; `:230` - the in-window settlement is `MISSING_BANK_CREDIT`. Run: `ReconciliationIntegrationTest.java:237` - `assertThat(references(items)).doesNotContain(otherLine, lateLine, ignoredLine, outsideSale)` | PASS |
| LAY-20 A transaction with no settlement stays free of `MISSING_BANK_CREDIT`, `ORPHAN_BANK_CREDIT`, `BANK_AMOUNT_MISMATCH`, and `AMBIGUOUS_BANK_MATCH` | Only `MISSING_SETTLEMENT` on that sale | `BankStatementMatcherTest.java:247` - `containsExactly(DiscrepancyType.MISSING_SETTLEMENT)`; `:243` - `assertThat(sale.getExternalSettlement()).isNull()`. Empty statement: `:309` - `containsExactly(DiscrepancyType.MISSING_SETTLEMENT)`. Run: `ReconciliationIntegrationTest.java:151` - `containsExactly("MISSING_SETTLEMENT")` | PASS |
| LAY-21 Existing sale-versus-settlement discrepancy types still come from transactions and settlements alone | `FEE_DIVERGENCE` and `ORPHAN_SETTLEMENT` remain, including when the bank amount matches | `BankStatementMatcherTest.java:270` - `containsExactly(DiscrepancyType.FEE_DIVERGENCE)` when the bank amount matches the net; `:303` - `containsExactly(DiscrepancyType.FEE_DIVERGENCE, DiscrepancyType.MISSING_BANK_CREDIT)`; `:314` - `containsExactly(DiscrepancyType.ORPHAN_SETTLEMENT, DiscrepancyType.MISSING_BANK_CREDIT)`. Run: `ReconciliationIntegrationTest.java:148` - `containsExactlyInAnyOrder("FEE_DIVERGENCE", "MISSING_BANK_CREDIT")`; `:150` - `containsExactlyInAnyOrder("ORPHAN_SETTLEMENT", "MISSING_BANK_CREDIT")` | PASS |
| LAY-22 Item result is `MATCHED` only with no discrepancy, and `DIVERGENT` when any discrepancy exists, including a bank discrepancy | `MATCHED` iff the discrepancy list is empty; bank discrepancy → `DIVERGENT` | `BankStatementMatcherTest.java:41` - `MATCHED` with `:42` empty discrepancies; `:69` - `DIVERGENT` with `BANK_AMOUNT_MISMATCH`; `:113` - `DIVERGENT` with `MISSING_BANK_CREDIT`; `:207` - `DIVERGENT` with `ORPHAN_BANK_CREDIT`. Run: `ReconciliationIntegrationTest.java:226` - `MATCHED`; `:232` - `assertThat(resultOf(items, orphanLine)).isEqualTo("DIVERGENT")`; `:221` - `jsonPath("$.matchedCount").value(2)`; `:222` - `jsonPath("$.divergentCount").value(3)` | PASS |
| LAY-23 Each new bank discrepancy is stored as `OPEN` | `DiscrepancyStatus.OPEN` | `BankStatementMatcherTest.java:77` - `assertThat(mismatchDiscrepancy.getStatus()).isEqualTo(DiscrepancyStatus.OPEN)`; `:120` - missing credit `OPEN`; `:215` - orphan `OPEN`; `:174` - ambiguous `OPEN`. Run: `ReconciliationIntegrationTest.java:312` - `jsonPath("$.content[0].discrepancies[0].status").value("OPEN")` on `MISSING_BANK_CREDIT` | PASS |
| LAY-24 The reconciliation CSV header stays unchanged | The current 15 columns, in order. A new type may appear in `discrepancyTypes` | `ReconciliationIntegrationTest.java:246` - `assertThat(rows.getFirst()).containsExactly("externalReference", "result", "discrepancyTypes", ... "settlementDate")`; `:262` - `assertThat(row(rows, mismatchReference)[2]).isEqualTo("BANK_AMOUNT_MISMATCH")` | PASS |

**Status**: All ACs covered

---

## Edge Cases

- [x] LAY-25 Two eligible lines for one settlement, or one line for two settlements: pair none, mark each involved side `AMBIGUOUS_BANK_MATCH`. `BankStatementMatcherTest.java:169` - `containsExactly(DiscrepancyType.AMBIGUOUS_BANK_MATCH)`; `:375` - `assertThat(lineItem.getExternalSettlement()).isNull()`; `:191` - `assertAmbiguousSettlement` on both settlements; `:193` - `assertAmbiguousBankLine`
- [x] LAY-26 Two imports of the same new reference at the same time: one HTTP 201, the other HTTP 409 `CONFLICT`, exactly one batch and one row. A second request after the first commit does not count. Settlement: `ExternalSettlementIntegrationTest.java:441` throws if the duplicate check already saw a committed row; `:446` both callers wait until both have passed that check; `:473` - `containsExactlyInAnyOrder(201, 409)`; `:480` - `isEqualTo("CONFLICT")`; `:487` - imports `jsonPath("$.totalElements").value(1)`; `:491` - settlements `jsonPath("$.totalElements").value(1)`. Statement: `BankStatementIntegrationTest.java:279` same committed-row guard; `:311` - `containsExactlyInAnyOrder(201, 409)`; `:318` - `isEqualTo("CONFLICT")`; `:325` - `jsonPath("$.totalElements").value(1)`; `:327` - `assertThat(countImports(merchantId)).isEqualTo(1L)`; `:328` - `assertThat(countLines(merchantId)).isEqualTo(1L)`
- [x] LAY-27 No bank line imported: every settlement in the run is `MISSING_BANK_CREDIT`. `BankStatementMatcherTest.java:293` - `containsExactly(DiscrepancyType.MISSING_BANK_CREDIT)` with an empty statement list. Run: `ReconciliationIntegrationTest.java:143` - `containsExactly("MISSING_BANK_CREDIT")` and `:146` - `assertDiscrepancy(..., "MISSING_BANK_CREDIT", "88.00", null)`
- [x] LAY-28 A bank line of another merchant is not a candidate. `ReconciliationIntegrationTest.java:209` imports `otherLine` on another merchant; `:237` - `assertThat(references(items)).doesNotContain(otherLine, ...)`

---

## max-swallow-size

Necessary. Tomcat's default `maxSwallowSize` is 2MB. `application.yaml` keeps `max-file-size` and `max-request-size` at 5MB (`application.yaml:12-13`). A real body over 5MB is already larger than that 2MB default, so Tomcat closes the socket before the 413 response is written. `application.yaml:5` sets `server.tomcat.max-swallow-size: -1`. The oversized tests post `5MB + 1` bytes with `HttpClient` to the live port and assert `statusCode() == 413`. That assertion is what this setting makes observable.

The property sits on the shared connector, and `application-prod.yaml` does not override it. `-1` means an aborted request has no swallow ceiling. Accepted uploads stay capped at 5MB, and both import tests assert that an oversized file stores nothing. The spec requires HTTP 413 for every file larger than 5MB, with no smaller ceiling, so a finite swallow would hide 413 again once the body passed that ceiling. This is the setting that makes LAY-06 and LAY-13 true on a real socket. It is not a separate product behavior.

---

## Discrimination Sensor

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ----------- | ------- |
| 1 | `BankStatementMatcher.java:187` | `compareTo(...) <= 0` changed to `< 0`, so a difference equal to tolerance no longer pairs | Killed. `BankStatementMatcherTest` 6 failures, including `:54` expected `MATCHED` but was `DIVERGENT` |
| 2 | `BankStatementMatcher.java:137` | Empty eligible-line branch no longer adds `MISSING_BANK_CREDIT` | Killed. `shouldMarkEverySettlementMissingBankCreditWhenStatementIsEmpty:290` expected `DIVERGENT` but was `MATCHED` |
| 3 | `BankStatementMatcher.java:132` | `eligibleLines.size() == 1` changed to `>= 1`, so several eligible lines are treated as a pair | Killed. `shouldRecordAmbiguousMatchWhenSeveralLinesAreEligibleAndPairNone:165` expected `L-1` in the ambiguous set and did not find it |
| 4 | `GlobalExceptionHandler.java:177` | Over-size handler returns `HttpStatus.BAD_REQUEST` (400) instead of `PAYLOAD_TOO_LARGE` (413) | Killed. `ExternalSettlementIntegrationTest.java:418` expected 413 but was 400. `BankStatementIntegrationTest` failed the same way |
| 5 | `GlobalExceptionHandler.java:197` | Unique-index violation returns `HttpStatus.CREATED` (201) instead of `CONFLICT` (409), so both simultaneous imports answer 201 | Killed. `ExternalSettlementIntegrationTest.java:473` expected `[201, 409]` but was `[201, 201]`. The bank race test failed the same way |

**Sensor depth**: P0-full (data integrity). Scratch worktree `/tmp/reconpay-lay-verify` at HEAD, then `git worktree remove --force`. No `git stash`.
**Isolation**: `git status --porcelain` after removal matched the pre-sensor baseline.
**Result**: 5/5 killed - PASS

---

## Interactive UAT Results

| # | Test | Result | Details |
| - | ---- | ------ | ------- |
| 1 | Interactive UAT | Skip | Backend only. Automated checks cover the routes. |

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code | Pass |
| Surgical changes | Pass. The `5f9a488` surface is the two import tests, `IntegrationTestUtils`, and the swallow setting those tests need. |
| No scope creep | Pass. `max-swallow-size: -1` is the connector setting required for LAY-06 and LAY-13. Storage limits stay 5MB. |
| Matches patterns | Pass |
| Spec-anchored outcome check (asserted values match spec) | Pass |
| Per-layer Coverage Expectation met (domain 1:1 ACs; routes happy+edge+error) | Pass. Import routes cover 201, 400, 401, 403, 409, and 413. The run covers match, mismatch, missing credit, and orphan. |
| Every test maps to a spec requirement - no unclaimed tests | Pass for the feature diff. Pre-existing reconciliation tests outside this spec were not treated as new claims. |
| Documented guidelines followed: `AGENTS.md`, `.cursor/skills/tlc-spec-driven/references/coding-principles.md` | Pass |

---

## Gate Check

- **Gate command**: `./mvnw -B verify`
- **Result**: 316 passed, 0 failed, 0 skipped. BUILD SUCCESS.
- **Test count before feature**: 207 `@Test` methods at `033e64d`
- **Test count after feature**: 292 `@Test` methods at HEAD. Surefire executed 316 tests (6 `@ParameterizedTest` methods, unchanged from the base).
- **Delta**: +85 `@Test` methods. None removed. No `@Disabled` or `@Ignore`.
- **Skipped tests**: none
- **Failures**: none

---

## Requirement Traceability Update

Spec statuses were not edited. This table is the verification record.

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| LAY-01 | Implementing | Verified |
| LAY-02 | Implementing | Verified |
| LAY-03 | Implementing | Verified |
| LAY-04 | Implementing | Verified |
| LAY-05 | Implementing | Verified |
| LAY-06 | Implementing | Verified |
| LAY-07 | Implementing | Verified |
| LAY-08 | Implementing | Verified |
| LAY-09 | Implementing | Verified |
| LAY-10 | Implementing | Verified |
| LAY-11 | Implementing | Verified |
| LAY-12 | Implementing | Verified |
| LAY-13 | Implementing | Verified |
| LAY-14 | Implementing | Verified |
| LAY-15 | Implementing | Verified |
| LAY-16 | Implementing | Verified |
| LAY-17 | Implementing | Verified |
| LAY-18 | Implementing | Verified |
| LAY-19 | Implementing | Verified |
| LAY-20 | Implementing | Verified |
| LAY-21 | Implementing | Verified |
| LAY-22 | Implementing | Verified |
| LAY-23 | Implementing | Verified |
| LAY-24 | Implementing | Verified |
| LAY-25 | Implementing | Verified |
| LAY-26 | Implementing | Verified |
| LAY-27 | Implementing | Verified |
| LAY-28 | Implementing | Verified |

---

## Summary

**Overall**: Ready

**Spec-anchored check**: 28/28 ACs matched spec outcome | 0 spec-precision gaps
**Sensor**: 5/5 mutations killed
**Gate**: 316 passed, 0 failed

**What works**: Empty settlement and statement files return HTTP 400 `VALIDATION_ERROR` and leave both lists empty. A file of 5MB+1 bytes returns HTTP 413 `VALIDATION_ERROR` on both import routes and stores nothing. Two overlapping imports of the same new reference return 201 and 409, with one batch and one row. The pairing outcomes in the spec are asserted with the exact discrepancy type, values, and `OPEN` status.

**Issues found**: none

**Next steps**: none
