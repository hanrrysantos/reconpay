# LESSONS - auto-maintained by scripts/lessons.py

> Machine-owned. Do NOT hand-edit. Changes are overwritten on the next `lessons.py` write.
> Canonical state lives in `.specs/lessons.json`. Edit lessons only via the script.
> promote_threshold=2 distinct features · window_days=45 · quarantine_threshold=2

## Confirmed (load these at Specify/Design)

Corroborated across multiple features. Safe to apply as guidance.

_none_

## Candidates (under observation - do NOT load as guidance yet)

Seen once or not yet corroborated. Tracked, not trusted.

### L-001 - Assert the spec HTTP status and persist-nothing for an over-limit upload, including 413, instead of assuming an existing handler.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `imports` · harmful: 0
- features: layouts-and-statement
- evidence: LAY-06 (imports) (+1 more)
- last seen: 2026-10-07T13:31:28Z

### L-002 - Assert two simultaneous imports of the same new reference persist one record and return 409 to the other.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `imports` · harmful: 0
- features: layouts-and-statement
- evidence: LAY-26 (imports)
- last seen: 2026-10-07T13:31:28Z

### L-003 - Assert HTTP 400 VALIDATION_ERROR and persist-nothing for an empty settlement file, not only the thrown exception type.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `imports` · harmful: 0
- features: layouts-and-statement
- evidence: ExternalSettlementServiceTest.java:336 (imports)
- last seen: 2026-10-07T13:31:28Z

### L-004 - Assert Portuguese copy with a language check, not only that the text is non-blank.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `openapi` · harmful: 0
- features: api-docs
- evidence: ApiDocsIntegrationTest.java:110 (openapi)
- last seen: 2026-10-07T15:34:26Z

### L-005 - Assert the login 401 phrase separately from the missing-bearer 401 phrase, because both share status 401 and error UNAUTHORIZED.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `openapi` · harmful: 0
- features: api-docs
- evidence: ApiDocsIntegrationTest.java:706 (openapi)
- last seen: 2026-10-07T15:34:26Z

### L-006 - Assert a 204 response omits its example, not only that it omits a schema.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `openapi` · harmful: 0
- features: api-docs
- evidence: ApiDocsIntegrationTest.java:776 (openapi)
- last seen: 2026-10-07T15:34:26Z

### L-007 - Truncate Instant values to microseconds before persisting them so the API response matches the timestamp PostgreSQL stores.
- signal: `gate_fail` · recurrence: 1 feature(s) · scope: `reconciliation` · harmful: 0
- features: period-close
- evidence: PeriodCloseIntegrationTest.java:182 PER-41 (reconciliation)
- last seen: 2026-10-08T00:18:47Z

### L-008 - Add the backend field a specified count needs instead of showing a different count under a SPEC_DEVIATION.
- signal: `spec_deviation` · recurrence: 1 feature(s) · scope: `frontend` · harmful: 0
- features: dashboard-redesign
- evidence: frontend/components/dashboard/MatchRingCard.tsx:37 (DASH-03 AC1) (frontend)
- last seen: 2026-10-14T10:44:01Z

### L-009 - Cover each page-level action handler with a page test that asserts the endpoint called, not only the child component callback.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `frontend` · harmful: 0
- features: dashboard-redesign
- evidence: F6 frontend/components/dashboard/DashboardPage.tsx:152 (frontend)
- last seen: 2026-10-14T10:44:01Z

### L-010 - Cover each page-level action handler with a page test that asserts the endpoint called, not only the child component callback.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `frontend` · harmful: 0
- features: dashboard-redesign
- evidence: DASH-07 AC3 (frontend)
- last seen: 2026-10-14T10:44:01Z

### L-011 - Test a derived-state rule once for every input it reads, not only for a subset of them.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `frontend` · harmful: 0
- features: dashboard-redesign
- evidence: F3 frontend/components/dashboard/view.ts:37 (frontend)
- last seen: 2026-10-14T10:44:01Z

### L-012 - Build tiebreak fixtures whose insertion order contradicts the expected order so the database cannot return it by chance.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `reconciliation` · harmful: 0
- features: dashboard-redesign
- evidence: B7 backend/src/main/java/br/com/hanrry/reconpay/reconciliation/repository/ReconciliationItemSpecifications.java:94 (reconciliation)
- last seen: 2026-10-14T10:44:01Z

### L-013 - State in the spec whether the no-arithmetic rule covers count-based rates before computing any rate or rate difference in the browser.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `frontend` · harmful: 0
- features: dashboard-redesign
- evidence: DASH-03 AC10 (frontend/components/dashboard/format.ts:38, DailyHeatmap.tsx:26) (frontend)
- last seen: 2026-10-14T10:44:01Z

### L-014 - Back a theme-token acceptance criterion with a test that reads the theme file.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `frontend` · harmful: 0
- features: dashboard-redesign
- evidence: DASH-01 AC1 (frontend/app/globals.css:3) (frontend)
- last seen: 2026-10-14T10:44:01Z

### L-015 - When a request field carries several Bean Validation constraints, make sure only one can fail for a given input or give them the same message, because the 400 handler reports the first violation in unspecified order
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `backend-validation` · harmful: 0
- features: merchants-screen
- evidence: MDOC-02/MDOC-03 - backend/src/main/java/br/com/hanrry/reconpay/merchant/dto/MerchantRequestDTO.java:14 (backend-validation)
- last seen: 2026-10-15T19:48:05Z

### L-016 - State the exact text of each field validation message in the acceptance criterion instead of referring to the field message
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `spec` · harmful: 0
- features: merchants-screen
- evidence: MCAD-05 (spec)
- last seen: 2026-10-15T19:48:05Z

## Quarantined (failed when applied - ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_
