# Reconciliation Capacity Guard Validation

**Date**: 2026-10-20
**Spec**: `.specs/features/reconciliation-capacity-guard/spec.md`
**Diff range**: `9753786..8070c14` (627bbff, 1d37734, 3dc995c, 1a7bfa3, 8070c14)
**Verifier**: independent sub-agent (author ≠ verifier)

**Verdict**: PASS

The spec-anchored outcome check matches every AC except one. That one is CAP-01 AC5, where the default of 300000 is backed only by inspecting the config, with no test asserting it. The discrimination sensor killed 15 of 15 mutants.

Test paths below are relative to `backend/src/test/java/br/com/hanrry/reconpay/reconciliation/`. Production paths are relative to `backend/src/main/java/br/com/hanrry/reconpay/reconciliation/`.

---

## Task Completion

Medium feature with inline steps and no `tasks.md`. All three requirements are implemented across 1d37734 (CAP-01), 3dc995c (CAP-02), 1a7bfa3 (CAP-03) and 8070c14 (README).

---

## Spec-Anchored Acceptance Criteria

### CAP-01: Recusar a janela grande demais no pedido

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1: sum over the limit is refused | `400`, `VALIDATION_ERROR`, exact message with pt-BR numbers | `ReconciliationCapacityIntegrationTest.java:60-63` - `status().isBadRequest()`, `$.error == "VALIDATION_ERROR"`, `$.message == "A janela tem 11 registros para conciliar; o limite é 10. Divida o período em janelas menores."`; pt-BR thousands separator in `service/ReconciliationCapacityGuardTest.java:69-70` - `contains("A janela tem 300.001 registros ...; o limite é 300.000. ...")` | ✅ PASS |
| AC2: no run is created | run count unchanged | `ReconciliationCapacityIntegrationTest.java:64` - `$.totalElements == 1` (only the earlier accepted run); `:74` - `$.totalElements == 0`; `service/ReconciliationServiceTest.java:164` - `never().saveAndFlush(any())` | ✅ PASS |
| AC3: sum equal to the limit | `202` | `ReconciliationCapacityIntegrationTest.java:53` - 10 records at limit 10 → `status().isAccepted()`; `service/ReconciliationCapacityGuardTest.java:53-55` - 300000 → `isEmpty()` | ✅ PASS |
| AC4: only this merchant's records count | other merchant over the limit does not block | `ReconciliationCapacityIntegrationTest.java:73-75` - crowded merchant `400`, quiet merchant `isAccepted()` | ✅ PASS |
| AC5: limit read from `reconpay.reconciliation.max-window-records`, default 300000 | property binds; default 300000 | Binding: `ReconciliationCapacityIntegrationTest.java:33` and `RunCapacityRejectionIntegrationTest.java:46` lower it and the outcomes follow; minimum 1: `service/ReconciliationConfigurationTest.java:18` (`max-window-records=0` blocks startup). Default: inspection only, `config/ReconciliationProperties.java:55` `@DefaultValue("300000")` and `backend/src/main/resources/application.yaml:77` `${RECONCILIATION_MAX_WINDOW_RECORDS:300000}`. No test asserts the default | ⚠️ Default untested (minor) |

### CAP-02: Falhar de forma limpa na execução

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1: run over the limit fails | `FAILED`, `errorMessage` equal to the request message, `finishedAt` set | `RunCapacityRejectionIntegrationTest.java:68` - awaits `FAILED`; `:71` - `getErrorMessage().isEqualTo("A janela tem 4 registros para conciliar; o limite é 3. ...")`; `:72` - `getFinishedAt().isNotNull()` | ✅ PASS |
| AC2: no items, window not loaded | zero items; transactions, settlements and bank lines not loaded | `RunCapacityRejectionIntegrationTest.java:73` - `findIdsByRunId(...).isEmpty()`; `:74` - `verify(transactions, never()).findAll(any(Specification.class))`. Settlements and bank lines load after transactions (`service/ReconciliationRunProcessor.java:92-108`), so skipping them follows from the early return at `:86-90` (mutant M15 confirms this) | ✅ PASS |
| AC3: audit `RECONCILIATION_REJECTED` with total and limit | event carries both numbers | `RunCapacityRejectionIntegrationTest.java:75-78` - `record(eq("RECONCILIATION_REJECTED"), eq("reconciliationRun"), eq(runId), contains("limit=3"))` and `contains("4 registros")` | ✅ PASS |
| AC4: run within the limit | `COMPLETED` as before | `RunCapacityRejectionIntegrationTest.java:86-90` - awaits `COMPLETED`, `errorMessage` null, `totalItems == 3` (at the limit) | ✅ PASS |

### CAP-03: Reiniciar em vez de travar

| Criterion | Spec-defined outcome | `file:line` + evidence | Result |
| --- | --- | --- | --- |
| AC1: JVM flag | `-XX:+ExitOnOutOfMemoryError` while keeping `-XX:MaxRAMPercentage=75.0` | `backend/Dockerfile:26` - `ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]` (inspection; this is infra with no automated test, and the spec's Independent Test is a manual replica run) | ✅ PASS (inspection) |

**Status**: every AC is covered by evidence. One minor gap is flagged: the AC5 default is backed by inspection only.

---

## Edge Cases

- [x] Empty window accepted: `service/ReconciliationCapacityGuardTest.java:59-63` - `records(0,0,0)` → `isEmpty()`.
- [x] Another merchant over the limit does not block: `ReconciliationCapacityIntegrationTest.java:67-76`.
- [x] Settlements and bank lines up to `settlement-lag-days` after the window are counted: the fixture at `ReconciliationCapacityIntegrationTest.java:78-117` places a settlement and a bank line on Aug 5 (inside the lag) and on Aug 6 (outside it), so the exact-limit `202` and the `11`-record refusal pin both boundaries. Unit check at `service/ReconciliationCapacityGuardTest.java:92` - `verify(...).countByMerchant_IdAndMovementDateBetween(merchantId, FROM, TO.plusDays(5))`.

---

## Discrimination Sensor

Scratch: a temporary detached `git worktree` under the session scratchpad, mutated one fault at a time and restored with `git checkout -- <file>` between mutants. Tests run: `ReconciliationCapacityGuardTest, ReconciliationServiceTest, ReconciliationCapacityIntegrationTest, RunCapacityRejectionIntegrationTest` (`./mvnw -q test -Djacoco.skip=true`). Before any mutation the same set passed (21 tests, 0 failures).

| # | File:line | Mutation | Killed? |
| --- | --- | --- | --- |
| M1 | `service/ReconciliationCapacityGuard.java:37` | `total <= limit` → `total < limit` | ✅ Killed (CapacityIntegration ×2) |
| M2 | `service/ReconciliationCapacityGuard.java:48` | drop transaction count | ✅ Killed |
| M3 | `service/ReconciliationCapacityGuard.java:50` | drop settlement count | ✅ Killed |
| M4 | `service/ReconciliationCapacityGuard.java:52` | drop bank-line count | ✅ Killed |
| M5 | `service/ReconciliationCapacityGuard.java:51` | settlements over the un-extended window (`toDate`) | ✅ Killed |
| M6 | `service/ReconciliationCapacityGuard.java:52` | bank lines over the un-extended window | ✅ Killed |
| M7 | `service/ReconciliationCapacityGuard.java:40` | `Locale.US` number format | ✅ Killed (GuardTest) |
| M8 | `service/ReconciliationCapacityGuard.java:42` | limit rendered as the total | ✅ Killed |
| M9 | `service/ReconciliationService.java:77-79` | remove the request-time check | ✅ Killed |
| M10 | `service/ReconciliationRunProcessor.java:87` | drop the worker recheck (`if (false)`) | ✅ Killed (RunCapacityRejection) |
| M11 | `service/ReconciliationRunProcessor.java:146` | skip `setErrorMessage` | ✅ Killed |
| M12 | `service/ReconciliationRunProcessor.java:147` | skip `setFinishedAt` | ✅ Killed |
| M13 | `service/ReconciliationRunProcessor.java:150` | drop the `RECONCILIATION_REJECTED` audit | ✅ Killed |
| M14 | `service/ReconciliationRunProcessor.java:153` | audit without `limit=` | ✅ Killed |
| M15 | `service/ReconciliationRunProcessor.java:89` | no `return` after reject (loads and completes) | ✅ Killed |

Not injected: changing the default 300000. No test reads the default, so that mutant would survive. This is the AC5 gap above.

**Sensor depth**: expanded (15 mutants; this guards API availability). **Result**: 15/15 killed - PASS. The scratch worktree was removed (`git worktree remove --force`). Real-tree `git status --porcelain` was empty before and after the sensor, matching the baseline.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ one guard component reused by the service and the processor |
| Surgical changes | ✅ edits to the other tests only add the new constructor argument / record field |
| No scope creep | ✅ no frontend changes, no batching, as the spec's Out of Scope says |
| Matches patterns | ✅ `InvalidReconciliationWindowException` → `400 VALIDATION_ERROR` (`exception/handler/GlobalExceptionHandler.java:260`); properties follow the `@Min` + `@DefaultValue` convention; audit follows the existing `RECONCILIATION_*` events |
| Spec-anchored outcome check | ✅ (AC5 default flagged) |
| Per-layer coverage | ✅ unit (guard, service) + integration (POST route happy, edge and error paths; worker path) |
| Every test maps to a requirement | ✅ |
| Documented guidelines followed | ✅ `AGENTS.md` (Testcontainers via `AbstractIntegrationTest`, `StandardError`, YAML config, Conventional Commits) |

---

## Gate Check

- **Command**: `./mvnw -q test -Dtest=ReconciliationCapacityGuardTest,ReconciliationServiceTest,ReconciliationCapacityIntegrationTest,RunCapacityRejectionIntegrationTest,ReconciliationConfigurationTest -Djacoco.skip=true` (scoped to the feature; the full `./mvnw verify` runs in CI)
- **Result**: 21 passed, 0 failed, 0 skipped (Guard 5, Service 4, CapacityIntegration 2, RunCapacityRejection 2, Configuration 8)
- **Test integrity**: no tests deleted or weakened. The 8 test files outside the feature only gained the new `ReconciliationProperties` argument.

---

## Fix Plans

### Fix 1 (minor, optional): assert the default limit

- **Root cause**: CAP-01 AC5 says the default is 300000, but no test binds `ReconciliationProperties` without overrides and checks `maxWindowRecords()`.
- **Fix task**: in `ReconciliationConfigurationTest`, add a case that builds the context with no `max-window-records` property and asserts `maxWindowRecords() == 300000`.
- **Priority**: Minor. The default is a literal that inspection confirms in two places, and it does not block PASS.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| CAP-01 | Implemented | ✅ Verified (default backed by inspection only) |
| CAP-02 | Implemented | ✅ Verified |
| CAP-03 | Implemented | ✅ Verified (inspection) |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 11 of 12 ACs match the spec outcome in a test. The AC5 default is confirmed by inspection only.
**Sensor**: 15/15 killed.
**Gate**: 21 passed.

**What works**: refusal at request time with the exact pt-BR message and no run created; the exact limit is accepted; counting is per merchant and over the lag-extended window; at the worker, the run ends `FAILED` with the message, `finishedAt`, no items and no window load, plus the audit event; a run within the limit completes; the Dockerfile has the OOM exit flag.

**Issues found**: the AC5 default value has no test (Fix 1, minor).

**Next steps**: the user decides whether to add Fix 1 before merging.


## Gap follow-up

CAP-01 AC5 (default 300000) closed by `ReconciliationConfigurationTest.windowRecordLimitDefaultsTo300Thousand`, which asserts both the record `@DefaultValue` and the `application.yaml` placeholder default. Changing either default to 600000 fails the test (2/2 mutants killed).
