# Period Close Validation

**Date**: 2026-10-07
**Spec**: `.specs/features/period-close/spec.md`
**Diff range**: `90904df..HEAD`
**Verifier**: independent sub-agent, iteration 2 (author ≠ verifier)

## Validation

**Result**: PASS

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 | Done | Period lock table |
| T2 | Done | Period lock entity |
| T3 | Done | Period lock repository |
| T4 | Done | Period conflict exception |
| T5 | Done | Period not found exception |
| T6 | Done | HTTP mapping |
| T7 | Done | Open amount |
| T8 | Done | Merchant row lock |
| T9 | Done | Period guard |
| T10 | Done | Period service |
| T11 | Done | Period endpoints |
| T12 | Done | Locked run |
| T13 | Done | Locked discrepancy |
| T14 | Done | Locked transaction |
| T15 | Done | Locked settlement import |
| T16 | Done | Locked bank import |
| T17 | Done | HTTP close |
| T18 | Done | OpenAPI |
| T19 | Done | `lockedAt` truncated to microseconds before flush |

No task is blocked or partial. `tasks.md` has no unchecked box.

---

## Spec-Anchored Acceptance Criteria

### P1: Ler a janela

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| PER-01 WHEN an OPERATOR with a grant GETs the period for a COMPLETED run with null supersededAt and no PENDING or RUNNING run THEN HTTP 200 with runId, fromDate, toDate, totalItems, matchedCount, divergentCount, matchRate, openAmount, locked, lockedAt, and closeDurationSeconds | HTTP 200 and those eleven fields | `PeriodIntegrationTest.java:72` - `status().isOk()`; `:73` - `jsonPath("$.runId").value(runId)`; `:74` - `jsonPath("$.fromDate").value(FROM)`; `:75` - `jsonPath("$.toDate").value(TO)`; `:76` - `jsonPath("$.totalItems").value(0)`; `:77` - `jsonPath("$.matchedCount").value(0)`; `:78` - `jsonPath("$.divergentCount").value(0)`; `:79` - `jsonPath("$.matchRate").value(nullValue())`; `:80` - `jsonPath("$.openAmount").value(0.00)`; `:81` - `jsonPath("$.locked").value(false)`; `:82` - `jsonPath("$.lockedAt").value(nullValue())`; `:83` - `jsonPath("$.closeDurationSeconds").value(nullValue())`. Non-zero snapshot: `PeriodServiceTest.java:126` - `assertThat(response.runId()).isEqualTo(runId)`; `:132` - `matchRate` `0.5000`; `:133` - `openAmount` `2.80` | Pass |
| PER-02 WHEN totalItems is greater than zero THEN matchRate is matchedCount / totalItems, half up, 4 decimal places | `1/6` = `0.1667` | `PeriodServiceTest.java:147` - `assertThat(response.matchRate()).isEqualTo(new BigDecimal("0.1667"))` with `:148` - `totalItems` `6` and `:149` - `matchedCount` `1` | Pass |
| PER-03 WHEN totalItems is zero THEN matchRate is null | `matchRate` null | `PeriodServiceTest.java:159` - `assertThat(response.totalItems()).isZero()`; `:160` - `assertThat(response.matchRate()).isNull()` | Pass |
| PER-04 openAmount is the half-up scale-2 sum of each OPEN discrepancy by the type table; a null amount adds 0.00; any other status adds nothing | Exact amount per type, scale 2 half up, nulls as 0.00, non-OPEN excluded | `OpenAmountCalculatorTest.java:32` - `isEqualTo(new BigDecimal("97.00"))` MISSING_SETTLEMENT expected net; `:41` - `80.00` ORPHAN_SETTLEMENT settlement net; `:50` - `2.40` INCORRECT_AMOUNT from transaction `10.00` and settlement `12.40`; `:59` - `2.80` FEE_DIVERGENCE from expected net `100.00` and settlement net `97.20`; `:68` - `40.00` MISSING_BANK_CREDIT; `:76` - linked `actualValue` `1.23` and `:77` - `8.77` BANK_AMOUNT_MISMATCH against settlement net `10.00`; `:85` - `actualValue` `1.23` and `:86` - `1.23` ORPHAN_BANK_CREDIT; `:93` - `0.00` STATUS_MISMATCH; `:100` - `0.00` PAYMENT_METHOD_MISMATCH; `:107` - `0.00` INSTALLMENTS_MISMATCH; `:117` - `0.00` AMBIGUOUS_BANK_MATCH; `:128` - `0.00` for a null expected net; `:150` - `1.25` with ACCEPTED, ADJUSTED, and WRITTEN_OFF ignored; `:165` - `1.01` and `:166` - `0.01` half up to scale 2 | Pass |
| PER-05 WHEN the window is not locked THEN locked is false, lockedAt is null, and closeDurationSeconds is null | Those three values | `PeriodServiceTest.java:171` - `assertThat(response.locked()).isFalse()`; `:172` - `assertThat(response.lockedAt()).isNull()`; `:173` - `assertThat(response.closeDurationSeconds()).isNull()` | Pass |
| PER-06 WHEN the window is locked THEN locked is true, lockedAt is the lock timestamp, and closeDurationSeconds is whole seconds from finishedAt to lockedAt, truncated toward zero | `29` seconds from `10:00:00.900Z` to `10:00:30.400Z` | `PeriodServiceTest.java:134` - `assertThat(response.locked()).isTrue()`; `:135` - `assertThat(response.lockedAt()).isEqualTo(LOCKED_AT)`; `:136` - `assertThat(response.closeDurationSeconds()).isEqualTo(CLOSE_SECONDS)` where `CLOSE_SECONDS` is `29L` (`PeriodServiceTest.java:58`-`:61`) | Pass |
| PER-07 WHEN a discrepancy on that run changes status THEN matchRate stays matchedCount / totalItems | `0.5000` from matchedCount 1 and totalItems 2 while openAmount is `0.00` | `PeriodServiceTest.java:190` - `assertThat(response.matchRate()).isEqualTo(new BigDecimal("0.5000"))`; `:191` - `matchedCount` `1`; `:192` - `totalItems` `2`; `:193` - `openAmount` `0.00` for an ACCEPTED fee gap | Pass |
| PER-08 WHEN the newest run is FAILED, a COMPLETED run still has null supersededAt, and no PENDING or RUNNING run exists THEN HTTP 200 for that COMPLETED run | The COMPLETED run, not the FAILED run | `PeriodServiceTest.java:207` - `assertThat(response.runId()).isEqualTo(completed.getId())`; `:208` - `isNotEqualTo(failed.getId())`; `:211` - verify the COMPLETED finder; `:213` - `never()` the FAILED finder. The same GET returns HTTP 200: `PeriodIntegrationTest.java:72` - `status().isOk()` and `PeriodController.java:30` returns `ResponseEntity.ok` | Pass |
| PER-09 IF no COMPLETED run with null supersededAt exists THEN HTTP 404 NOT_FOUND | HTTP 404, error `NOT_FOUND` | `PeriodServiceTest.java:226` - `isInstanceOf(PeriodNotFoundException.class)`; `:228` - `verifyNoInteractions(periodLockRepository)`. Mapping: `PeriodExceptionHandlerTest.java:47` - `status().isNotFound()`; `:49` - `jsonPath("$.error").value("NOT_FOUND")` | Pass |
| PER-10 WHILE a PENDING or RUNNING run exists THEN GET returns HTTP 409 CONFLICT | HTTP 409, error `CONFLICT` | `PeriodServiceTest.java:236` - `isInstanceOf(PeriodConflictException.class)` when the in-flight query is true; `:248` - the same for the running-named case. The query list is PENDING and RUNNING (`PeriodServiceTest.java:62`-`:64`). Mapping: `PeriodExceptionHandlerTest.java:38` - `status().isConflict()`; `:40` - `jsonPath("$.error").value("CONFLICT")` | Pass |

### P1: Travar a janela

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| PER-11 WHEN an authorized actor locks a clean COMPLETED window THEN HTTP 200, locked true, openAmount 0.00, and closeDurationSeconds from that run's finishedAt | HTTP 200, `locked` true, `openAmount` `0.00`, duration from finishedAt | `PeriodIntegrationTest.java:109` - `status().isOk()`; `:111` - `jsonPath("$.locked").value(true)`; `:113` - `jsonPath("$.openAmount").value(0.00)`; `:124` - `assertThat(seconds.longValue()).isEqualTo(Duration.between(Instant.parse(finishedAt), lockedAt).toSeconds())`. Service: `PeriodServiceTest.java:283` - `locked` true; `:285` - `openAmount` `0.00`; `:287` - `closeDurationSeconds` `CLOSE_SECONDS` | Pass |
| PER-12 WHEN that run has totalItems zero and no discrepancy THEN lock it with matchRate null and openAmount 0.00 | `matchRate` null, `openAmount` `0.00`, locked | `PeriodServiceTest.java:339` - `assertThat(response.totalItems()).isZero()`; `:340` - `assertThat(response.matchRate()).isNull()`; `:341` - `assertThat(response.openAmount()).isEqualTo(new BigDecimal("0.00"))`; `:342` - `assertThat(response.locked()).isTrue()` | Pass |
| PER-13 IF any discrepancy is OPEN THEN HTTP 409 CONFLICT and the window stays unlocked | HTTP 409 `CONFLICT`, no save, no audit | `PeriodServiceTest.java:360` - `isInstanceOf(PeriodConflictException.class)`; `:362` - `verify(periodLockRepository, never()).saveAndFlush(any())`; `:363` - `verifyNoInteractions(auditLogger)`. Mapping: `PeriodExceptionHandlerTest.java:38`-`:40` | Pass |
| PER-14 IF no COMPLETED run with null supersededAt exists THEN HTTP 404 NOT_FOUND and store no lock | HTTP 404 `NOT_FOUND`, no save | `PeriodServiceTest.java:376` - `isInstanceOf(PeriodNotFoundException.class)`; `:378` - `verify(periodLockRepository, never()).saveAndFlush(any())`. Mapping: `PeriodExceptionHandlerTest.java:47`-`:49` | Pass |
| PER-15 WHILE a PENDING or RUNNING run exists THEN lock returns HTTP 409 CONFLICT and stores no lock | HTTP 409 `CONFLICT`, no save | `PeriodServiceTest.java:389` - `isInstanceOf(PeriodConflictException.class)`; `:391` - `verify(periodLockRepository, never()).saveAndFlush(any())`. Mapping: `PeriodExceptionHandlerTest.java:38`-`:40` | Pass |
| PER-16 IF the window is already locked THEN HTTP 409 CONFLICT and the original lockedAt remains | HTTP 409 `CONFLICT`, `lockedAt` unchanged, no save | `PeriodServiceTest.java:406` - `isInstanceOf(PeriodConflictException.class)`; `:408` - `assertThat(existing.getLockedAt()).isEqualTo(ORIGINAL_LOCKED_AT)`; `:409` - `never().saveAndFlush`. Mapping: `PeriodExceptionHandlerTest.java:38`-`:40` | Pass |
| PER-17 Emit PERIOD_LOCKED with merchantId, fromDate, toDate, and runId only after the lock commits | Event name and those four ids after flush; the logger emits only after commit | `PeriodServiceTest.java:298` - `order.verify(periodLockRepository).saveAndFlush` before `:299` - `order.verify(auditLogger).record(eq("PERIOD_LOCKED"), eq("periodLock"), eq(lockId), detail.capture())`; `:305` - `detail` contains merchantId, fromDate, toDate, and runId. After commit: `AuditLoggerTest.java:43` - `assertThat(events.list).isEmpty()` inside the transaction; `:47` - `assertThat(events.list).hasSize(1)` after commit; `:58` - `assertThat(events.list).isEmpty()` on rollback | Pass |

### P1: Reabrir a janela

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| PER-18 WHEN an authorized actor unlocks a locked window THEN HTTP 200, locked false, lockedAt null, closeDurationSeconds null, and the same runId and matchRate | HTTP 200 and those fields | `PeriodIntegrationTest.java:131` - `status().isOk()`; `:132` - `jsonPath("$.runId").value(runId)`; `:133` - `jsonPath("$.matchRate").value(nullValue())`; `:134` - `jsonPath("$.locked").value(false)`; `:135` - `jsonPath("$.lockedAt").value(nullValue())`; `:136` - `jsonPath("$.closeDurationSeconds").value(nullValue())`. Same non-null rate: `PeriodServiceTest.java:427` - `runId` equal; `:428` - `matchRate` `0.5000`; `:429` - `locked` false; `:430` - `lockedAt` null; `:431` - `closeDurationSeconds` null | Pass |
| PER-19 IF the window is not locked THEN HTTP 409 CONFLICT and persist no change | HTTP 409 `CONFLICT`, no delete, no audit | `PeriodServiceTest.java:447` - `isInstanceOf(PeriodConflictException.class)`; `:449` - `never().delete`; `:451` - `verifyNoInteractions(auditLogger)`. Mapping: `PeriodExceptionHandlerTest.java:38`-`:40` | Pass |
| PER-20 WHEN the same COMPLETED run is unlocked and locked again with zero OPEN THEN closeDurationSeconds is from that run's finishedAt to the new lockedAt | New `lockedAt`, duration `29` from the same `finishedAt`, same matchRate | `PeriodServiceTest.java:480` - `assertThat(locked.lockedAt()).isEqualTo(LOCKED_AT)`; `:481` - `closeDurationSeconds` `CLOSE_SECONDS`; `:482` - `assertThat(locked.matchRate()).isEqualTo(unlocked.matchRate())`; `:483` - `assertThat(run.getFinishedAt()).isEqualTo(FINISHED_AT)` | Pass |
| PER-21 Emit PERIOD_UNLOCKED with merchantId, fromDate, and toDate only after the unlock commits | Event after delete and flush; detail contains those three ids | `PeriodServiceTest.java:435` - `order.verify(periodLockRepository).delete(existing)`; `:436` - `flush()`; `:437` - `record(eq("PERIOD_UNLOCKED"), ...)`; `:438` - detail contains merchantId, fromDate, and toDate. After commit: `AuditLoggerTest.java:43` and `:47` | Pass |

### P1: Segurar a janela travada

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| PER-22 WHILE a window is locked, WHEN reconciliations are posted for the same dates THEN HTTP 409 CONFLICT and no run is created | HTTP 409 `CONFLICT`, no `saveAndFlush` | `PeriodCloseIntegrationTest.java:118` - `status().isConflict()`; `:119` - `jsonPath("$.error").value("CONFLICT")`. `ReconciliationServiceTest.java:98` - `isInstanceOf(PeriodConflictException.class)`; `:101` - `verify(reconciliationRunRepository, never()).saveAndFlush(any())` | Pass |
| PER-23 WHEN reconciliations are posted with a different fromDate or toDate THEN HTTP 202 even if the dates overlap a locked window | HTTP 202; an overlapping pair is still saved as PENDING | `PeriodCloseIntegrationTest.java:109` - `status().isAccepted()` for `2026-08-01`/`2026-08-15`. Overlap: `ReconciliationServiceTest.java:140` - `assertThat(saved.getValue().getStatus()).isEqualTo(ReconciliationRunStatus.PENDING)` for `2026-07-10`/`2026-07-20`; `:138` - `assertWindowOpen` is called with that pair | Pass |
| PER-24 WHILE transactionDate is inside a locked window THEN a valid transaction POST returns HTTP 409 CONFLICT and persists no transaction | HTTP 409 `CONFLICT`; the inside sale is absent from the list | `PeriodCloseIntegrationTest.java:67` - `status().isConflict()`; `:68` - `jsonPath("$.error").value("CONFLICT")`; `:86` - `jsonPath("$.totalElements").value(1)` and `:87` - only `TXN-OUTSIDE`. `TransactionServiceTest.java:326` - `isInstanceOf(PeriodConflictException.class)`; `:329` - `verify(transactionRepository, never()).save(any())` | Pass |
| PER-25 WHILE a transaction's transactionDate is inside a locked window THEN a valid status PATCH returns HTTP 409 CONFLICT and leaves the status unchanged | HTTP 409 `CONFLICT`; status stays `APPROVED` | `TransactionServiceTest.java:410` - `isInstanceOf(PeriodConflictException.class)`; `:412` - `assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.APPROVED)`; `:413` - `never().save`. Mapping: `PeriodExceptionHandlerTest.java:38`-`:40` | Pass |
| PER-26 WHILE a valid settlement import has any settlementDate inside a locked window THEN HTTP 409 CONFLICT and neither the batch nor any row is persisted | HTTP 409 `CONFLICT`; no import save and no row save | `ExternalSettlementServiceTest.java:422` - `isInstanceOf(PeriodConflictException.class)`; `:425` - `verify(settlementImportRepository, never()).save(any())`; `:426` - `verify(externalSettlementRepository, never()).saveAll(anyList())`. Mapping: `PeriodExceptionHandlerTest.java:38`-`:40` | Pass |
| PER-27 WHILE a valid bank-statement import has any movementDate inside a locked window THEN HTTP 409 CONFLICT and neither the batch nor any row is persisted | HTTP 409 `CONFLICT`; no import save and no line save | `BankStatementServiceTest.java:198` - `isInstanceOf(PeriodConflictException.class)`; `:201` - `verify(bankStatementImportRepository, never()).save(any())`; `:202` - `verify(bankStatementLineRepository, never()).saveAll(anyList())`. Mapping: `PeriodExceptionHandlerTest.java:38`-`:40` | Pass |
| PER-28 WHILE a window is locked THEN a PATCH of a discrepancy of that run returns HTTP 409 CONFLICT and leaves the status unchanged | HTTP 409 `CONFLICT`; status stays `OPEN`; no save | `DiscrepancyResolutionServiceTest.java:511` - `isInstanceOf(PeriodConflictException.class)`; `:513` - `assertThat(openCase.discrepancy().getStatus()).isEqualTo(DiscrepancyStatus.OPEN)`; `:514` - `never().save`. Mapping: `PeriodExceptionHandlerTest.java:38`-`:40` | Pass |
| PER-29 IF the transaction, settlement import, or bank-statement import payload is invalid THEN HTTP 400 VALIDATION_ERROR and persist nothing, including when a date in the payload also falls inside a locked window | HTTP 400 `VALIDATION_ERROR`, nothing saved, lock guard not consulted | Transaction: `TransactionIntegrationTest.java:195` - `status().isBadRequest()`; `:196` - `jsonPath("$.error").value("VALIDATION_ERROR")` for PIX with 3 installments. Ordering: `TransactionServiceTest.java:386` - `isInstanceOf(InvalidInstallmentsForPaymentMethodException.class)`; `:389` - `verifyNoInteractions(periodGuard)`; `:390` - `never().save`. Settlement: `ExternalSettlementIntegrationTest.java:203` - `status().isBadRequest()`; `:204` - `jsonPath("$.error").value("VALIDATION_ERROR")`. With a covered date in the file: `ExternalSettlementServiceTest.java:444` - `isInstanceOf(SettlementImportValidationException.class)`; `:447` - `verifyNoInteractions(periodGuard)`; `:448` - `never().save`. Bank statement: `BankStatementIntegrationTest.java:205` - `status().isBadRequest()`; `:206` - `jsonPath("$.error").value("VALIDATION_ERROR")`. With a covered date in the file: `BankStatementServiceTest.java:220` - `isInstanceOf(InvalidSettlementImportException.class)`; `:223` - `verifyNoInteractions(periodGuard)`; `:224` - `never().save` | Pass |
| PER-30 WHEN every settlementDate is outside every locked window and one settlementDate is toDate plus one day THEN HTTP 201 and the batch is persisted | HTTP 201; settlement date `2026-07-16`; one stored row | `PeriodCloseIntegrationTest.java:80` - `status().isCreated()`; `:81` - `jsonPath("$.totalRows").value(1)`; `:93` - `jsonPath("$.totalElements").value(1)`; `:94` - `jsonPath("$.content[0].settlementDate").value("2026-07-16")` | Pass |
| PER-31 WHEN transactionDate is fromDate or toDate of a locked window THEN HTTP 409 CONFLICT | HTTP 409 `CONFLICT` for `2026-07-01` and `2026-07-15` | `TransactionServiceTest.java:317` - dates include `2026-07-01` and `2026-07-15`; `:326` - `isInstanceOf(PeriodConflictException.class)`. Guard bounds: `PeriodGuardTest.java:96` - `isInstanceOf(PeriodConflictException.class)` for `fromDate`; `:110` - the same for `toDate`. Mapping: `PeriodExceptionHandlerTest.java:38`-`:40` | Pass |
| PER-32 WHEN transactionDate is outside every locked window and the payload is valid THEN HTTP 201 | HTTP 201 for `2026-07-20` | `PeriodCloseIntegrationTest.java:74` - `status().isCreated()`; `:75` - `jsonPath("$.transactionDate").value("2026-07-20")` | Pass |
| PER-33 The system accepts fee-rule and merchant updates while one window is locked | HTTP 200 for both updates; the window stays locked | `PeriodCloseIntegrationTest.java:141` - `status().isOk()` and `:142` - `jsonPath("$.feePercentage").value(2.5000)`; `:151` - `status().isOk()` and `:152` - `jsonPath("$.name").value("Merchant Atualizado")`; `:156` - `jsonPath("$.locked").value(true)` | Pass |
| PER-34 WHEN a discrepancy belongs to the current run of a window that is not locked THEN the existing resolution rules apply | Status becomes `ACCEPTED` and the transition is saved after the open-window check | `DiscrepancyResolutionServiceTest.java:563` - `assertThat(response.status()).isEqualTo(DiscrepancyStatus.ACCEPTED)`; `:564` - stored status `ACCEPTED`; `:566` - `assertWindowOpen` then `:567` - `transitionRepository.save` | Pass |

### P1: Acesso, validação e concorrência

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| PER-35 IF the caller is unauthenticated THEN HTTP 401 on GET, lock, and unlock, and persist no lock | HTTP 401 on all three; the window stays unlocked | `PeriodIntegrationTest.java:149` - `status().isUnauthorized()` on GET; `:154` - `status().isUnauthorized()` on lock; `:159` - `status().isUnauthorized()` on unlock; `:164` - `assertThat(after).isEqualTo(before)`; `:165` - `assertThat(JsonPath.<Boolean>read(after, "$.locked")).isFalse()` | Pass |
| PER-36 IF an OPERATOR has no grant THEN HTTP 403 FORBIDDEN and persist no lock | HTTP 403 `FORBIDDEN` on GET, lock, and unlock; locked stays false | `PeriodIntegrationTest.java:174` - `status().isForbidden()`; `:175` - `jsonPath("$.error").value("FORBIDDEN")` on GET; `:180`-`:181` on lock; `:186`-`:187` on unlock; `:191` - `jsonPath("$.locked").value(false)` | Pass |
| PER-37 WHEN an ADMIN with no user_merchants row calls GET, lock, or unlock on a valid window THEN the same success outcome as an OPERATOR with a grant | GET bodies equal; admin lock is HTTP 200 with the same run | `PeriodIntegrationTest.java:89` - admin `status().isOk()` after `removeAdminMerchantRows()`; `:94` - `assertThat(adminBody).isEqualTo(operatorBody)`; `:109` - admin lock `status().isOk()`; `:110` - `jsonPath("$.runId").value(runId)`; `:111` - `jsonPath("$.locked").value(true)`. Unlock of that window: `:131` - `status().isOk()`; `:134` - `jsonPath("$.locked").value(false)` | Pass |
| PER-38 IF merchantId is not an active merchant THEN HTTP 404 NOT_FOUND | HTTP 404 `NOT_FOUND` on GET, lock, and unlock | `PeriodIntegrationTest.java:274` - `status().isNotFound()`; `:275` - `jsonPath("$.error").value("NOT_FOUND")` on GET; `:280`-`:281` on lock; `:286`-`:287` on unlock | Pass |
| PER-39 IF fromDate or toDate is absent, or fromDate is after toDate THEN HTTP 400 VALIDATION_ERROR on GET, lock, and unlock | HTTP 400 `VALIDATION_ERROR` for missing and inverted dates on all three | `PeriodIntegrationTest.java:198` - `status().isBadRequest()`; `:199` - `jsonPath("$.error").value("VALIDATION_ERROR")` and the same pair at `:203`-`:204`, `:209`-`:210`, `:215`-`:216` for absent dates; `:225`-`:226`, `:231`-`:232`, `:237`-`:238` for `fromDate` after `toDate` | Pass |
| PER-40 IF the inclusive day count exceeds maxWindowDays THEN HTTP 400 VALIDATION_ERROR | HTTP 400 `VALIDATION_ERROR` on GET, lock, and unlock | `PeriodIntegrationTest.java:247` - `status().isBadRequest()`; `:248` - `jsonPath("$.error").value("VALIDATION_ERROR")`; `:253`-`:254` and `:259`-`:260` for lock and unlock | Pass |
| PER-41 WHILE two lock requests for the same window run concurrently THEN one lock persists, one response is HTTP 200, and the other is HTTP 409 CONFLICT | Statuses `200` and `409`; loser body contains `CONFLICT`; one locked row; the single `lockedAt` on the 200 body equals the `lockedAt` read back after the PostgreSQL commit | `PeriodCloseIntegrationTest.java:168` - `containsExactlyInAnyOrder(200, 409)`; `:171` - `assertThat(conflict).contains("CONFLICT")`; `:176` - `jsonPath("$.locked").value(true)`; `:181`-`:182` - `assertThat(Instant.parse(JsonPath.read(period, "$.lockedAt")).truncatedTo(ChronoUnit.MICROS)).isEqualTo(Instant.parse(lockedAt).truncatedTo(ChronoUnit.MICROS))`. `period` is a later GET. The class extends `AbstractIntegrationTest` (`postgres:16-alpine`) and is not `@Transactional`, so that GET reloads the committed row. This method passed in the gate (`PeriodCloseIntegrationTest`: 6 tests, 0 failures) | Pass |
| PER-42 WHILE two unlock requests for the same locked window run concurrently THEN one unlocked state remains, one response is HTTP 200, and the other is HTTP 409 CONFLICT | Statuses `200` and `409`; `CONFLICT`; stored `locked` false and `lockedAt` null | `PeriodCloseIntegrationTest.java:195` - `containsExactlyInAnyOrder(200, 409)`; `:197` - `contains("CONFLICT")`; `:198` - `locked` false on the 200 body; `:202` - `jsonPath("$.locked").value(false)`; `:203` - `jsonPath("$.lockedAt").value(nullValue())` | Pass |
| PER-43 WHILE a lock and a mutation blocked by that lock run concurrently THEN exactly one commits and the other receives HTTP 409 CONFLICT | Exactly one 409 with `CONFLICT`; the winner's state matches (locked, or a 202 run and unlocked) | `PeriodCloseIntegrationTest.java:218` - `assertThat(statuses).contains(409)`; `:219` - `filter(status -> status == 409).count()` `isEqualTo(1)`; `:220` - `contains("CONFLICT")`; `:225` - run `isEqualTo(409)` when lock is 200 and `:228` - `locked` true; `:230` - lock `isEqualTo(409)` and `:231` - run `isEqualTo(202)` and `:236` - `locked` false | Pass |
| PER-44 IF persisting the lock fails THEN the window stays unlocked and no PERIOD_LOCKED event is emitted | The save failure propagates; the audit logger is not called | `PeriodServiceTest.java:493` - `assertThatThrownBy(() -> periodService.lock(...)).isSameAs(failure)`; `:494` - `verifyNoInteractions(auditLogger)` | Pass |

**Status**: All 44 criteria match the spec outcome. 0 spec-precision gaps.

---

## Discrimination Sensor

Scratch worktree `/tmp/period-close-sensor-iter2` at HEAD `ebb4ca0`. Each mutant was applied alone, the cited unit test was run with `./mvnw -B test -Dtest=...`, then the file was restored from the worktree HEAD. The worktree was removed with `git worktree remove --force`. The real worktree was not mutated and `git stash` was not used. Porcelain after cleanup matches the baseline.

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ----------- | ------- |
| 1 | `PeriodService.java:84` | `lockedAt` stored as `clock.instant()` with no `truncatedTo(MICROS)` | Killed. `PeriodServiceTest#shouldStoreLockedAtTruncatedToMicroseconds` failed |
| 2 | `PeriodService.java:75` | Lock condition `status == OPEN` flipped to `status != OPEN` | Killed. `PeriodServiceTest#shouldRejectLockWhenADiscrepancyIsOpenAndStoreNothing` failed at `PeriodServiceTest.java:360` |
| 3 | `PeriodGuard.java:40` | Inclusive date check `!isBefore(from) && !isAfter(to)` replaced with exclusive `isAfter(from) && isBefore(to)` | Killed. `PeriodGuardTest#shouldRejectADateEqualToTheLockStart` and `#shouldRejectADateEqualToTheLockEnd` failed at `:96` and `:110` |
| 4 | `OpenAmountCalculator.java:33` | `FEE_DIVERGENCE` returns `BigDecimal.ZERO` | Killed. `OpenAmountCalculatorTest#shouldAddAbsoluteNetDifferenceForOpenFeeDivergence` failed at `:59` (`expected: 2.80 but was: 0.00`) |
| 5 | `PeriodService.java:174` | `closeDuration` uses `Math.round(millis / 1000.0)` instead of `Duration.toSeconds()` | Killed. `PeriodServiceTest#shouldReturnFrozenMatchRateOpenAmountAndCloseDuration` failed at `:136` (`expected: 29L but was: 30L`) |

**Sensor depth**: P0 / data integrity (5 behavior mutations, including the lock timestamp path)
**Result**: 5/5 killed

---

## Interactive UAT Results (if performed)

Skipped. This feature is a backend API. No user-facing flow.

---

## Code Quality

Reviewed against `.cursor/skills/tlc-spec-driven/references/coding-principles.md`, `AGENTS.md`, `README.md` (Testes), and `pom.xml` (JaCoCo 85% line / 75% branch on `verify`). No `// SPEC_DEVIATION` in the feature diff. JaCoCo reported that all coverage checks were met.

| Principle | Status |
| --------- | ------ |
| Minimum code | Pass. The diff is the lock table, period read/lock/unlock, writer guards, and truncation of `lockedAt` to microseconds before flush. |
| Surgical changes | Pass. The timestamp fix is one `truncatedTo(ChronoUnit.MICROS)` on the instant that is saved, plus the unit test that asserts that value. |
| No scope creep | Pass. No frontend, no civil-month period, no lock history. |
| Matches patterns | Pass. Exceptions, `StandardError`, Flyway, and `AbstractIntegrationTest` match the existing modules. Microsecond truncation matches the existing `Instant` persistence pattern. |
| Spec-anchored outcome check (asserted values match spec) | Pass. See the criterion table. |
| Per-layer Coverage Expectation met (domain 1:1 ACs; routes happy+edge+error) | Pass. Period routes cover 200, 400, 401, 403, 404, and 409. Writer conflicts are service-level plus the handler mapping, with HTTP proof for the transaction, settlement-day-after, and reconciliation cases in `PeriodCloseIntegrationTest`. |
| Every test maps to a spec requirement - no unclaimed tests | Pass. New period tests map to PER-01–PER-44 or to a task Done-when (repository uniqueness, merchant row lock, OpenAPI, microsecond truncation). |
| Documented guidelines followed: `AGENTS.md`, `README.md` (Testes), `pom.xml` JaCoCo | Pass. `./mvnw -B verify` met the coverage check. |

---

## Edge Cases

- [x] Janela sem run `COMPLETED` vigente: HTTP 404 (PER-09, PER-14)
- [x] Run `PENDING` ou `RUNNING` na mesma janela: GET e lock respondem HTTP 409 (PER-10, PER-15)
- [x] Run `FAILED` com um `COMPLETED` anterior ainda vigente: GET devolve esse `COMPLETED` (PER-08)
- [x] `totalItems` zero: matchRate null, trava permitida, openAmount 0.00 (PER-03, PER-12)
- [x] Divergência `OPEN`: lock responde HTTP 409 e a janela segue aberta (PER-13)
- [x] Segunda trava: HTTP 409 e o `lockedAt` original permanece (PER-16)
- [x] Reabertura sem trava: HTTP 409 (PER-19)
- [x] Data igual a `fromDate` ou `toDate`: a mutação recebe HTTP 409 (PER-31)
- [x] `settlementDate` no dia seguinte a `toDate`: a importação válida recebe HTTP 201 (PER-30)
- [x] Arquivo inválido com uma data também travada: HTTP 400 e nada gravado (PER-29)
- [x] Duas travas simultâneas: uma persiste, a outra recebe HTTP 409 (PER-41). The following GET `lockedAt` equals the HTTP 200 `lockedAt` after the PostgreSQL round-trip
- [x] Duas reaberturas simultâneas: uma persiste, a outra recebe HTTP 409 (PER-42)
- [x] Falha ao gravar a trava: a janela segue aberta e não há audit (PER-44)

---

## Gate Check

- **Gate command**: `./mvnw -B verify` (repo root; Full gate from `tasks.md`)
- **Result**: 419 passed, 0 failed, 0 skipped
- **Test count before feature**: `@Test` / `@ParameterizedTest` annotations: 316 at `90904df`
- **Test count after feature**: 401 annotations at HEAD. This run executed 419 tests (parameterized methods expand past the annotation count)
- **Delta**: +85 annotations. No test file was deleted
- **Skipped tests**: none
- **Failures**: none
- **Coverage**: JaCoCo check passed (`All coverage checks have been met`)
- **PER-41**: `PeriodCloseIntegrationTest` ran 6 tests, 0 failures, including `twoConcurrentLocksPersistOneAndConflictTheOther`

---

## Fix Plans (if issues found)

None.

---

## Requirement Traceability Update

`spec.md` was not edited. This report is the only file written.

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| PER-01–PER-44 | Verified or Implementing in `spec.md` | Verified |

---

## Summary

**Overall**: Ready

**Spec-anchored check**: 44/44 ACs matched the spec outcome. 0 spec-precision gaps
**Sensor**: 5/5 mutations killed
**Gate**: 419 passed, 0 failed, 0 skipped

**What works**: Period read, lock, unlock, the open-amount table, inclusive dates, writer refusals, and the concurrent lock. The HTTP 200 `lockedAt` and the `lockedAt` read back by GET are equal after the PostgreSQL round-trip.

**Issues found**: none

**Next steps**: none. No new lesson. The earlier one-microsecond gate failure is already L-007.
