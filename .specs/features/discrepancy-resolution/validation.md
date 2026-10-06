# Discrepancy Resolution Validation

## Validation

**Result**: PASS

**Date**: 2026-10-06
**Spec**: `.specs/features/discrepancy-resolution/spec.md`
**Diff range**: `a875414..6480ef2` (HEAD `6480ef2`)
**Verifier**: independent sub-agent (author ≠ verifier)

23 of 23 acceptance criteria have an assertion that matches the spec-defined outcome. 0 spec-precision gaps. The mandated gate passed. All three mutants were killed.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1–T15 | Done | Every checkbox in `tasks.md` is marked done. The tasks header still says "In Progress". |

No task is blocked or partial in the checklist.

---

## Spec-Anchored Acceptance Criteria

### P1: Fechar a divergência no run vigente

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| RES-01 WHEN a reconciliation run stores a new discrepancy THEN status is OPEN, with no adjustment and no history | `OPEN`, empty adjustments, empty transitions | `src/test/java/br/com/hanrry/reconpay/reconciliation/service/ReconciliationEngineTest.java:78` - `assertThat(discrepancy.getStatus()).isEqualTo(DiscrepancyStatus.OPEN)`; `:79` - `assertThat(discrepancy.getAdjustments()).isEmpty()`; `:80` - `assertThat(discrepancy.getTransitions()).isEmpty()` | PASS |
| RES-02 WHEN an OPERATOR with a grant submits ACCEPTED or WRITTEN_OFF on the PATCH THEN HTTP 200, that status, one history entry, transaction and item snapshot unchanged | HTTP 200 from the granted operator; `ACCEPTED` and `WRITTEN_OFF`; one history entry; item snapshot and transaction values unchanged | `DiscrepancyResolutionIntegrationTest.java:98` - `status().isOk()` with `operatorToken`; `:100` - `jsonPath("$.status").value("ACCEPTED")`; `:101` - `jsonPath("$.transitions.length()").value(1)`; `:121` - `jsonPath("$.content[0].expectedNetAmount").value(96.50)`; `:122` - `jsonPath("$.content[0].transactionStatus").value("APPROVED")`. WRITTEN_OFF: `:266` - `status().isOk()` with `operatorToken`; `:267` - `jsonPath("$.status").value("WRITTEN_OFF")`; `:268` - `jsonPath("$.transitions.length()").value(1)`; `:269` - `jsonPath("$.adjustments.length()").value(0)`. Sale fields: `DiscrepancyResolutionServiceTest.java:120` - `assertThat(openCase.transaction().getStatus()).isEqualTo(TransactionStatus.APPROVED)`; `:121` - `assertThat(openCase.transaction().getExpectedNetAmount()).isEqualByComparingTo("120.00")`; `:123` - `assertThat(openCase.item().getExpectedNetAmount()).isEqualByComparingTo("120.00")`; WRITTEN_OFF transaction `:160` - `assertThat(openCase.transaction().getStatus()).isEqualTo(TransactionStatus.APPROVED)`; `:161` - `assertThat(openCase.transaction().getExpectedNetAmount()).isEqualByComparingTo("120.00")` | PASS |
| RES-03 WHEN that OPERATOR submits ADJUSTED THEN HTTP 200, status ADJUSTED, one non-voided adjustment with that amount, actor user id, and a timestamp, one history entry, sale unchanged | HTTP 200; `ADJUSTED`; amount `-1.50`; `voided` false; `createdBy` is the actor; `createdAt` present; one history entry | `DiscrepancyResolutionIntegrationTest.java:284` - `status().isOk()` with `operatorToken`; `:285` - `jsonPath("$.status").value("ADJUSTED")`; `:287` - `jsonPath("$.adjustments[0].amount").value(-1.50)`; `:288` - `jsonPath("$.adjustments[0].voided").value(false)`; `:289` - `jsonPath("$.transitions.length()").value(1)`. Stored actor and timestamp: `DiscrepancyResolutionServiceTest.java:203` - `assertThat(adjustments.getValue().getCreatedBy().getId()).isEqualTo(actorId)`; `:204` - `assertThat(adjustments.getValue().getCreatedAt()).isNotNull()`; sale `:194` - `assertThat(openCase.transaction().getExpectedNetAmount()).isEqualByComparingTo("120.00")`; `:195` - `assertThat(openCase.item().getExpectedNetAmount()).isEqualByComparingTo("120.00")` | PASS |
| RES-17 WHEN an authenticated ADMIN with no user_merchants row submits a valid transition THEN the same outcome as an OPERATOR with grant | HTTP 200 and `ACCEPTED` after that admin's `user_merchants` rows are deleted | `DiscrepancyResolutionIntegrationTest.java:307` - `userMerchantAccessRepository.deleteAllByUserId(ADMIN_ID)`; `:313` - `status().isOk()` with `adminToken`; `:314` - `jsonPath("$.status").value("ACCEPTED")`; `:315` - `jsonPath("$.transitions.length()").value(1)`. `ADMIN_ID` is the seeded admin `a0000000-0000-4000-8000-000000000101` | PASS |
| RES-22 The system emits DISCREPANCY_STATUS_CHANGED with the discrepancy id, previous status, and new status, only after the transition commits | Event name, discrepancy id, `OPEN -> ACCEPTED`; no audit record when save throws; log appears only after commit | `DiscrepancyResolutionServiceTest.java:133` - `verify(auditLogger).record("DISCREPANCY_STATUS_CHANGED", "discrepancy", openCase.discrepancyId(), "OPEN -> ACCEPTED")`; `:425` - `verify(auditLogger, never()).record(any(), any(), any(), any())` when adjustment save throws; `:445` - the same `never()` when history save throws. After commit: `AuditLoggerTest.java:43` - `assertThat(events.list).isEmpty()` inside the transaction; `:47` - `assertThat(events.list).hasSize(1)` after commit | PASS |
| RES-23 The system computes each run's match results from internal transactions and external settlements alone | A second reconcile of the same inputs stays `DIVERGENT` / `MISSING_SETTLEMENT` / `OPEN` with no adjustments after the first result was marked `ADJUSTED` | `ReconciliationEngineTest.java:83` - `setStatus(DiscrepancyStatus.ADJUSTED)` on the first result; `:87` - `assertThat(again.getFirst().getResult()).isEqualTo(ReconciliationResult.DIVERGENT)`; `:92` - `assertThat(discrepancy.getStatus()).isEqualTo(DiscrepancyStatus.OPEN)`; `:93` - `assertThat(discrepancy.getAdjustments()).isEmpty()` | PASS |

### P1: Reabrir

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| RES-08 WHEN an authorized actor submits OPEN for ACCEPTED or WRITTEN_OFF THEN HTTP 200, status OPEN, one history entry, and no adjustment | HTTP 200; `OPEN`; history appended; no adjustment; both source statuses | WRITTEN_OFF at HTTP: `DiscrepancyResolutionIntegrationTest.java:274` - `status().isOk()`; `:275` - `jsonPath("$.status").value("OPEN")`; `:276` - `jsonPath("$.transitions.length()").value(2)`; `:277` - `jsonPath("$.adjustments.length()").value(0)`. ACCEPTED: `DiscrepancyResolutionServiceTest.java:273` - `assertThat(response.status()).isEqualTo(DiscrepancyStatus.OPEN)`; `:275` - `assertThat(response.adjustments()).isEmpty()`; `:277` - `assertThat(transition.fromStatus()).isEqualTo(DiscrepancyStatus.ACCEPTED)`; `:278` - `assertThat(transition.toStatus()).isEqualTo(DiscrepancyStatus.OPEN)`; `:280` - `verify(adjustmentRepository, never()).save(any())`. WRITTEN_OFF history ends: `:297` - `assertThat(transition.fromStatus()).isEqualTo(DiscrepancyStatus.WRITTEN_OFF)`; `:298` - `assertThat(transition.toStatus()).isEqualTo(DiscrepancyStatus.OPEN)` | PASS |
| RES-09 WHEN an authorized actor submits OPEN for ADJUSTED THEN HTTP 200, status OPEN, voidedAt on the active adjustment, that adjustment still readable, no other active adjustment, one history entry | HTTP 200; `OPEN`; `voided` true; amount still `-1.50`; one adjustment; history appended; `voidedAt` set | `DiscrepancyResolutionIntegrationTest.java:295` - `status().isOk()`; `:296` - `jsonPath("$.status").value("OPEN")`; `:297` - `jsonPath("$.adjustments.length()").value(1)`; `:298` - `jsonPath("$.adjustments[0].voided").value(true)`; `:299` - `jsonPath("$.adjustments[0].amount").value(-1.50)`; `:300` - `jsonPath("$.transitions.length()").value(2)`. `DiscrepancyResolutionServiceTest.java:319` - `assertThat(active.getVoidedAt()).isNotNull()`; `:324` - `assertThat(openCase.discrepancy().getAdjustments()).hasSize(1)` | PASS |
| RES-10 IF the target equals the current status, or both statuses are terminal THEN HTTP 409 with error code CONFLICT and no persisted change | HTTP 409; error code `CONFLICT`; status stays `ACCEPTED`; no save | `DiscrepancyResolutionServiceTest.java:348` - `isInstanceOf(DiscrepancyResolutionConflictException.class)` for ACCEPTED to ADJUSTED; `:350` - the same for repeated `ACCEPTED`; `:352` - `assertThat(discrepancy.getStatus()).isEqualTo(DiscrepancyStatus.ACCEPTED)`; `:353` - `verify(discrepancyRepository, never()).save(any())`. HTTP mapping: `DiscrepancyResolutionErrorTest.java:47` - `status().isConflict()`; `:48` - `jsonPath("$.error").value("CONFLICT")` | PASS |

### P1: Validar a entrada

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| RES-04 IF the note has more than 500 characters THEN HTTP 400 with error code VALIDATION_ERROR and no persisted change | HTTP 400; `VALIDATION_ERROR`; discrepancy stays `OPEN` with no history | `DiscrepancyResolutionIntegrationTest.java:235` - note of 501 `x`; `:249` - `status().isBadRequest()`; `:250` - `jsonPath("$.error").value("VALIDATION_ERROR")`; `:255` - `jsonPath("$.status").value("OPEN")`; `:256` - `jsonPath("$.transitions.length()").value(0)` | PASS |
| RES-05 IF ADJUSTED and correctionAmount is absent, zero, has more than 2 decimal places, or has more than 17 integer digits THEN HTTP 400 VALIDATION_ERROR and no persisted change | HTTP 400; `VALIDATION_ERROR`; still `OPEN`; all four amount faults | Same test, bodies `:236` absent, `:237` `0`, `:238` `1.001`, `:239` 18 integer digits; `:249` - `status().isBadRequest()`; `:250` - `jsonPath("$.error").value("VALIDATION_ERROR")`; `:255` - `jsonPath("$.status").value("OPEN")` | PASS |
| RES-06 IF ACCEPTED, WRITTEN_OFF, or OPEN and correctionAmount is present THEN HTTP 400 VALIDATION_ERROR and no persisted change | HTTP 400; `VALIDATION_ERROR`; still `OPEN`; all three targets | Bodies `:240` `ACCEPTED`, `:241` `WRITTEN_OFF`, `:242` `OPEN`, each with `correctionAmount` `1.00`; `:249` - `status().isBadRequest()`; `:250` - `jsonPath("$.error").value("VALIDATION_ERROR")`; `:255` - `jsonPath("$.status").value("OPEN")` | PASS |
| RES-07 WHEN the note is empty or whitespace and the transition is otherwise valid THEN the history note is null | Stored history note is null | `DiscrepancyResolutionServiceTest.java:401` - `extracting(DiscrepancyDetailResponseDTO.Transition::note)`; `:403` - `.isNull()` for note `"   "` | PASS |

### P1: Guardas de acesso, run e concorrência

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| RES-11 WHILE the run is not COMPLETED or supersededAt is not null THEN HTTP 409 CONFLICT and no persisted change | HTTP 409; `CONFLICT`; status stays `OPEN`; no history | `DiscrepancyResolutionIntegrationTest.java:183` - `status().isConflict()`; `:184` - `jsonPath("$.error").value("CONFLICT")`; `:189` - `jsonPath("$.status").value("OPEN")`; `:190` - `jsonPath("$.transitions.length()").value(0)` for a superseded run. Unfinished runs: `DiscrepancyResolutionServiceTest.java:368` - `isInstanceOf(DiscrepancyResolutionConflictException.class)` for `PENDING`, `RUNNING`, `FAILED`; `:370` - `assertThat(openCase.discrepancy().getStatus()).isEqualTo(DiscrepancyStatus.OPEN)` | PASS |
| RES-12 IF the caller is unauthenticated THEN HTTP 401 and no persisted change | HTTP 401; a real discrepancy stays `OPEN` | `DiscrepancyResolutionIntegrationTest.java:132` - `status().isUnauthorized()` on `openDiscrepancy()`; `:137` - `jsonPath("$.status").value("OPEN")`; `:138` - `jsonPath("$.transitions.length()").value(0)` | PASS |
| RES-13 IF an authenticated OPERATOR has no grant THEN HTTP 403 with error code FORBIDDEN and no persisted change | HTTP 403; `FORBIDDEN`; a real discrepancy stays `OPEN` | `DiscrepancyResolutionIntegrationTest.java:151` - `status().isForbidden()`; `:152` - `jsonPath("$.error").value("FORBIDDEN")`; `:157` - `jsonPath("$.status").value("OPEN")` after `openDiscrepancy()` on a merchant the operator was not granted | PASS |
| RES-14 IF no discrepancy exists for that merchantId, runId, and discrepancyId THEN HTTP 404 with error code NOT_FOUND | HTTP 404; `NOT_FOUND`; wrong merchant and wrong run do not return the row | `DiscrepancyResolutionIntegrationTest.java:169` - `status().isNotFound()`; `:170` - `jsonPath("$.error").value("NOT_FOUND")` for the real discrepancy id on another run. `DiscrepancyLookupIntegrationTest.java:117` - `findByIdAndRunAndMerchant(..., UUID.randomUUID())` `.isEmpty()`; `:120` - another run `.isEmpty()` | PASS |
| RES-15 WHILE two status updates for the same discrepancy run concurrently THEN one transition persists and the other receives HTTP 409 with error code CONFLICT | Statuses `200` and `409`; loser body contains `CONFLICT`; stored status `ACCEPTED` with one history row | `DiscrepancyResolutionIntegrationTest.java:211` - `containsExactlyInAnyOrder(200, 409)`; `:219` - `assertThat(conflict).contains("CONFLICT")`; `:227` - `jsonPath("$.status").value("ACCEPTED")`; `:228` - `jsonPath("$.transitions.length()").value(1)` | PASS |
| RES-16 IF persisting the adjustment or the history entry fails THEN the previous status stays unchanged | Status remains `OPEN`; discrepancy is not saved | `DiscrepancyResolutionServiceTest.java:423` - `assertThat(openCase.discrepancy().getStatus()).isEqualTo(DiscrepancyStatus.OPEN)` when adjustment save throws; `:442` - the same assertion when history save throws | PASS |

### P1: Ler o desfecho

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| RES-18 WHEN an authorized client GETs the discrepancy THEN HTTP 200 with status, every adjustment amount and voided flag, and history in ascending timestamp order, each entry with actor user id, previous status, new status, note, and timestamp | HTTP 200; note and timestamp on the body GET returns; actor, order, amount, and voided flag | `DiscrepancyResolutionIntegrationTest.java:104` - `jsonPath("$.transitions[0].note").value("ok")`; `:105` - `jsonPath("$.transitions[0].createdAt").isNotEmpty()`; `:112` - `status().isOk()` on GET; `:117` - `assertThat(fetched).isEqualTo(patched)`. `DiscrepancyResolutionServiceTest.java:476` - `extracting(Adjustment::amount).containsExactly(...)`; `:478` - `extracting(Adjustment::voided).containsExactly(true, false)`; `:480` - `extracting(Transition::createdAt).containsExactly(...)`; `:482` - `extracting(Transition::actorUserId).containsExactly(actorId, actorId)` | PASS |
| RES-19 WHEN an authorized client GETs items THEN each discrepancy includes id and current status and omits transition history | `id` present; `status` `OPEN`; `transitions` absent | `ReconciliationIntegrationTest.java:130` - `jsonPath("$.content[0].discrepancies[0].id").isNotEmpty()`; `:131` - `jsonPath("$.content[0].discrepancies[0].status").value("OPEN")`; `:133` - `jsonPath("$.content[0].discrepancies[0].transitions").doesNotExist()` | PASS |
| RES-20 WHEN a transition returns HTTP 200 THEN the response body matches the GET representation | PATCH body equals the GET body | `DiscrepancyResolutionIntegrationTest.java:117` - `assertThat(fetched).isEqualTo(patched)` | PASS |
| RES-21 The system omits resolution status and correction amount from the reconciliation CSV export | Header has neither a resolution-status column nor a correction-amount column | `ReconciliationCsvExporterTest.java:24` - `assertThat(reader.readNext()).containsExactly("externalReference", "result", "discrepancyTypes", "internalTransactionId", "externalSettlementId", "transactionAmount", "expectedNetAmount", "settlementAmount", "settlementNetAmount", "paymentMethod", "installments", "transactionStatus", "settlementStatus", "transactionDate", "settlementDate")` | PASS |

**Status**: All 23 criteria match the spec outcome. 0 spec-precision gaps.

---

## Discrimination Sensor

Scratch: `git worktree add --detach /tmp/reconpay-dr-sensor-verify HEAD`. Each mutant was applied alone, tests were run, then `git checkout --` restored the service file before the next mutant. The worktree was removed with `git worktree remove --force`. Real-repo `git status --porcelain` was `?? .specs/features/discrepancy-resolution/validation.md` before the sensor and the same after the worktree was removed.

Command each time: `./mvnw -B test -Dtest=DiscrepancyResolutionServiceTest`

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ----------- | ------- |
| 1 | `src/main/java/br/com/hanrry/reconpay/reconciliation/service/DiscrepancyResolutionService.java:98` | Removed `discrepancy.setStatus(target)` | Killed. 17 tests, 6 failures. `acceptedFromOpenAppendsOneTransitionAndLeavesTheSaleUnchanged:111` expected `ACCEPTED` but was `OPEN`; `writtenOffFromOpenAppendsOneTransitionWithoutAnAdjustment:151` expected `WRITTEN_OFF` but was `OPEN`; `adjustedFromOpenStoresOneNonVoidedCorrectionAndLeavesExpectedNetAmount:186` expected `ADJUSTED` but was `OPEN`; `reopenAcceptedReturnsToOpenWithoutCreatingAnAdjustment:273` expected `OPEN` but was `ACCEPTED`; `reopenWrittenOffReturnsToOpenWithoutCreatingAnAdjustment:294` expected `OPEN` but was `WRITTEN_OFF`; `reopenAdjustedVoidsTheOnlyActiveAdjustment:318` expected `OPEN` but was `ADJUSTED`. |
| 2 | `DiscrepancyResolutionService.java:141` | `activeAdjustmentToVoid` always returns null | Killed. 17 tests, 1 failure. `reopenAdjustedVoidsTheOnlyActiveAdjustment:319` - `assertThat(active.getVoidedAt()).isNotNull()`. |
| 3 | `DiscrepancyResolutionService.java:131` | `ensureTransition` also returns early when both current and target are in `ACCEPTED`, `ADJUSTED`, `WRITTEN_OFF` | Killed. 17 tests, 1 failure. `terminalJumpAndRepeatedStatusDoNotSave:348` expected `DiscrepancyResolutionConflictException` but was `IllegalStateException`. |

**Sensor depth**: lightweight, the three mandated behavior faults
**Sensor outcome**: 3/3 killed

---

## Interactive UAT Results

Not run. The feature is a backend API. Automated checks are the verification for this slice.

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code | Pass |
| Surgical changes | Pass |
| No scope creep | Pass |
| Matches patterns | Pass |
| Spec-anchored outcome check (asserted values match spec) | Pass. 23/23 criteria cite the spec status code, error code, field, or unchanged state. |
| Per-layer coverage expectation met (domain 1:1 ACs; routes happy + edge + error) | Pass. PATCH and GET cover 200, 400, 401, 403, 404, and 409. Domain tests cover close, reopen, void, conflict, and rollback. |
| Every test maps to a spec requirement, listed edge case, or Done-when criterion | Pass. New tests map to RES-01 through RES-23. The one edited assertion in `ReconciliationIntegrationTest` added id, status, and omitted history; it did not drop the existing type assertion. |
| Documented guidelines followed: `AGENTS.md`, `README.md` (Testes), `pom.xml` JaCoCo 85% line / 75% branch | Pass. No `SPEC_DEVIATION` marker in the diff. |

Reviewed surface: resolution service, controller DTO, validator, engine factory, migration, and the tests cited above. Changes stay inside reconciliation resolution. `coding-principles.md` is not in this repo; `AGENTS.md` is the project guideline.

---

## Edge Cases

- [x] Note above 500 characters, amount absent or zero on ADJUSTED, amount present on ACCEPTED, WRITTEN_OFF, or OPEN: HTTP 400, `VALIDATION_ERROR`, discrepancy stays `OPEN` (RES-04, RES-05, RES-06).
- [x] Same status, or a jump between ACCEPTED, ADJUSTED, and WRITTEN_OFF: conflict and no save (RES-10).
- [x] Run PENDING, RUNNING, FAILED, or COMPLETED already superseded: conflict and status stays `OPEN` (RES-11).
- [x] Discrepancy of another run: HTTP 404 `NOT_FOUND`. Another merchant or another run returns empty from the repository (RES-14).
- [x] Two simultaneous updates: one persists, the loser body contains `CONFLICT`, one history row remains (RES-15).
- [x] Failure while saving the adjustment or the history: previous status remains `OPEN` and audit is not recorded (RES-16, RES-22).

---

## Gate Check

- **Gate command**: `./mvnw -B test -Dtest=DiscrepancyResolutionIntegrationTest,DiscrepancyResolutionServiceTest,ReconciliationEngineTest,DiscrepancyStatusRequestValidatorTest,DiscrepancyResolutionErrorTest,AuditLoggerTest`
- **Result**: 55 passed, 0 failed, 0 skipped
- **Test count before feature**: 175 `@Test` / `@ParameterizedTest` annotations at `a875414`
- **Test count after feature**: 213 annotations at `6480ef2`
- **Delta**: +38 annotations
- **Skipped tests**: none
- **Failures**: none
- **Surefire breakdown**: `ReconciliationEngineTest` 14, `DiscrepancyResolutionErrorTest` 2, `DiscrepancyResolutionServiceTest` 17, `DiscrepancyStatusRequestValidatorTest` 10, `DiscrepancyResolutionIntegrationTest` 9, `AuditLoggerTest` 3

---

## Fix Plans

None. No surviving mutant and no unmatched acceptance criterion.

---

## Requirement Traceability Update

`spec.md` was not edited. This verifier may only write the report file. Statuses below are the verification outcome.

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| RES-01 | Implementing | Verified |
| RES-02 | Implementing | Verified |
| RES-03 | Implementing | Verified |
| RES-04 | Implementing | Verified |
| RES-05 | Implementing | Verified |
| RES-06 | Implementing | Verified |
| RES-07 | Implementing | Verified |
| RES-08 | Implementing | Verified |
| RES-09 | Implementing | Verified |
| RES-10 | Implementing | Verified |
| RES-11 | Implementing | Verified |
| RES-12 | Implementing | Verified |
| RES-13 | Implementing | Verified |
| RES-14 | Implementing | Verified |
| RES-15 | Implementing | Verified |
| RES-16 | Implementing | Verified |
| RES-17 | Implementing | Verified |
| RES-18 | Implementing | Verified |
| RES-19 | Implementing | Verified |
| RES-20 | Implementing | Verified |
| RES-21 | Implementing | Verified |
| RES-22 | Implementing | Verified |
| RES-23 | Implementing | Verified |

---

## Summary

**Overall**: Ready

**Spec-anchored check**: 23/23 criteria matched the spec outcome. 0 spec-precision gaps.
**Sensor**: 3/3 mutations killed
**Gate**: 55 passed, 0 failed, 0 skipped

**What works**: Granted operator accept, write-off, and adjust; admin accept after `user_merchants` deletion; validation 400 that leaves the discrepancy open; reopen of accepted, written-off, and adjusted; 401, 403, 404, and the concurrent 409; GET matches PATCH, including note and timestamp; audit is skipped when save throws and the shared audit logger emits only after commit; a second reconcile ignores a prior adjusted status.

**Issues found**: none

**Next steps**: none for this verification. `spec.md` traceability rows were left as Implementing because this pass may only write `validation.md`.
