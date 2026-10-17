# LESSONS - auto-maintained by scripts/lessons.py

> Machine-owned. Do NOT hand-edit. Changes are overwritten on the next `lessons.py` write.
> Canonical state lives in `.specs/lessons.json`. Edit lessons only via the script.
> promote_threshold=2 distinct features · window_days=45 · quarantine_threshold=2

## Confirmed (load these at Specify/Design)

Corroborated across multiple features. Safe to apply as guidance.

### L-018 - When an acceptance criterion lists several allowed values, exercise each listed value at least once in the tests
- signal: `ac_gap` · recurrence: 2 feature(s) · scope: `tests` · harmful: 0
- features: fee-rules-screen, discrepancies-screen
- evidence: FREG-03 - backend/src/test/java/br/com/hanrry/reconpay/feerule/integration/FeeRuleIntegrationTest.java:168 (tests) (+1 more)
- last seen: 2026-10-17T01:51:23Z

### L-036 - To test that state survives a reset, set every kept value away from its default before triggering the reset
- signal: `surviving_mutant` · recurrence: 2 feature(s) · scope: `frontend` · harmful: 0
- features: transactions-screen, discrepancies-screen
- evidence: validation.md M31; TransactionsPage.test.tsx:248-272 (frontend) (+1 more)
- last seen: 2026-10-17T01:22:31Z

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

### L-017 - Test a numeric upper bound with a whole number above it as well as a fraction above it, so every branch of the bound check is pinned
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `frontend-validation` · harmful: 0
- features: fee-rules-screen
- evidence: M3 - frontend/lib/fee.ts:23 (FCAD-08) (frontend-validation)
- last seen: 2026-10-16T01:28:09Z

### L-019 - When a bound check branches on digit count, test an input of each digit count past the bound, not only the shortest one
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `frontend-validation` · harmful: 0
- features: fee-rules-screen
- evidence: S4 - frontend/lib/fee.ts:23 (FCAD-08) (frontend-validation)
- last seen: 2026-10-16T01:34:24Z

### L-020 - Test that an inclusive bound value is accepted when written with trailing zero decimals, not only as a whole number
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `frontend-validation` · harmful: 0
- features: fee-rules-screen
- evidence: S6 - frontend/lib/fee.ts:23 (FCAD-08) (frontend-validation)
- last seen: 2026-10-16T01:40:23Z

### L-021 - Test an edit-form prefill with a stored value that uses every stored decimal place, so a prefill that drops precision is caught
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `frontend-validation` · harmful: 0
- features: fee-rules-screen
- evidence: validation.md V1 (fee.ts:44 toInputDecimal, FEDIT-01) (frontend-validation)
- last seen: 2026-10-16T01:51:44Z

### L-022 - Test a trailing-zero-stripping formatter with a value that has an inner zero, so stripping every zero is caught
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `frontend-format` · harmful: 0
- features: fee-rules-screen
- evidence: validation.md V2 (fee.ts:36 formatPercent, FLIST-03) (frontend-format)
- last seen: 2026-10-16T01:51:44Z

### L-023 - When a bound check scans decimal digits as text, test above-bound fractions with the nonzero digit in each position and with high digit values
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `frontend-validation` · harmful: 0
- features: fee-rules-screen
- evidence: validation.md V3, V4 (fee.ts:23 parseFeePercent, FCAD-08) (frontend-validation)
- last seen: 2026-10-16T01:51:44Z

### L-024 - Build generated test inputs from every digit value and every leading digit past the bound, not a hand-picked digit sample
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `frontend-validation` · harmful: 0
- features: fee-rules-screen
- evidence: validation.md sensor N1b, N2 (frontend/lib/fee.reference.test.ts:10-11) (frontend-validation)
- last seen: 2026-10-16T02:06:03Z

### L-025 - Test the largest value an inclusive bound accepts, not only the first value past it
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `parsers` · harmful: 0
- features: transaction-csv-import
- evidence: validation.md M1 TransactionCsvParser.java:152 (TIMP-10 #6) (parsers)
- last seen: 2026-10-16T12:47:11Z

### L-026 - Test an input that fails two checks on the same field, so an error dropped when another already fired is caught
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `parsers` · harmful: 0
- features: transaction-csv-import
- evidence: validation.md M6 TransactionCsvParser.java:119 (TIMP-09/TIMP-10 #3-#4) (parsers)
- last seen: 2026-10-16T12:47:11Z

### L-027 - Assert the default page size of a paginated endpoint when the spec states one
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `routes` · harmful: 0
- features: transaction-csv-import
- evidence: validation.md N19 TransactionController.java:93 (TLOTE-02 / Assumptions 'Listagem de lotes') (routes) (routes)
- last seen: 2026-10-16T13:01:22Z

### L-028 - Give sort-order fixtures values in every other sortable column that order the opposite way, so a wrong sort key cannot produce the expected order by chance
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `routes` · harmful: 0
- features: transaction-csv-import
- evidence: validation.md V15 TransactionController.java:93 (routes)
- last seen: 2026-10-16T13:21:12Z

### L-029 - When the spec says only active records apply, test with a deactivated record that would otherwise match
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `imports` · harmful: 0
- features: transaction-csv-import
- evidence: validation.md V11 IFeeRuleRepository.java:17 (imports)
- last seen: 2026-10-16T13:21:12Z

### L-030 - When the spec requires an exact text match, test a variant that differs only in letter case
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `parsers` · harmful: 0
- features: transaction-csv-import
- evidence: validation.md V16 TransactionCsvParser.java:95 (parsers)
- last seen: 2026-10-16T13:21:12Z

### L-031 - Test a file extension check with a name that ends in the extension letters without the dot
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `imports` · harmful: 0
- features: transaction-csv-import
- evidence: validation.md V7 TransactionImportService.java:136 (imports)
- last seen: 2026-10-16T13:21:12Z

### L-032 - Assert the 403 for a user without access on every merchant-scoped route, because the access guard is matched by parameter name and a renamed parameter skips it
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `routes` · harmful: 0
- features: transaction-csv-import
- evidence: W7 TransactionController.java:99 (routes)
- last seen: 2026-10-16T13:49:42Z

### L-033 - When the spec requires an exact header, test the right column names in a different order
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `parsers` · harmful: 0
- features: transaction-csv-import
- evidence: W4 TransactionCsvParser.java:95 (parsers)
- last seen: 2026-10-16T13:49:42Z

### L-034 - Test every enumerated column with a lower-case value, not only one of them
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `parsers` · harmful: 0
- features: transaction-csv-import
- evidence: W8 TransactionCsvParser.java:193 (parsers)
- last seen: 2026-10-16T13:49:43Z

### L-035 - Assert page text inside the page's own region when the app shell renders the same text elsewhere
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `frontend` · harmful: 0
- features: transactions-screen
- evidence: validation.md M25; TransactionsPage.test.tsx:123 (frontend)
- last seen: 2026-10-16T18:09:53Z

### L-037 - When polling stops on a parent state change, serve the child data by that state in the test fixture and assert the final child data shows after polling stops
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `frontend` · harmful: 0
- features: reconciliations-screen
- evidence: REC-09 AC8 - frontend/components/reconciliations/RunDetailPage.test.tsx:55 (validation.md Defeito principal) (frontend)
- last seen: 2026-10-17T00:14:15Z

### L-038 - To test that a value resets, move it away from its default before triggering the reset
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `frontend` · harmful: 0
- features: discrepancies-screen
- evidence: validation.md M6, M14; DiscrepanciesPage.test.tsx:187, DiscrepanciesPage.test.tsx:285 (frontend)
- last seen: 2026-10-17T01:22:31Z

### L-039 - Test a key-based remount reset by switching to a target whose data is already cached, so a loading state cannot remount the component for you
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `frontend` · harmful: 0
- features: discrepancies-screen
- evidence: validation.md M13; DiscrepanciesPage.test.tsx:285-307 (frontend)
- last seen: 2026-10-17T01:22:31Z

### L-040 - When a page wires a callback of a reused component, drive that callback through the page in a test instead of relying on the component's own test with a mocked callback
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `frontend` · harmful: 0
- features: discrepancies-screen
- evidence: validation.md M20; DiscrepanciesPage.test.tsx:237-256 (frontend) (+1 more)
- last seen: 2026-10-17T01:31:48Z

### L-041 - Spec the exact text of composite cells, including when optional parts are omitted, instead of only naming the parts
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `frontend` · harmful: 0
- features: discrepancies-screen
- evidence: validation.md M19; DiscrepancyList.tsx:21 (frontend)
- last seen: 2026-10-17T01:22:32Z

### L-042 - When a list shows one row per child record, assert counts and positions with a fixture where one parent yields several visible rows
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `frontend` · harmful: 0
- features: discrepancies-screen
- evidence: validation.md iteração 3, Q3 (DiscrepanciesPage.tsx:119) (frontend)
- last seen: 2026-10-17T01:39:02Z

### L-043 - When an acceptance criterion lists several allowed values, exercise each listed value at least once in the tests
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `tests` · harmful: 0
- features: discrepancies-screen
- evidence: validation.md iteração 4, F7 DiscrepanciesPage.tsx:91 (tests)
- last seen: 2026-10-17T01:51:09Z

### L-044 - Spec whether pagination controls are disabled on the first and last page, and test both edges
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `frontend` · harmful: 0
- features: discrepancies-screen
- evidence: validation.md iteração 4, F1/F2 DiscrepancyList.tsx:116,120 (frontend)
- last seen: 2026-10-17T01:51:09Z

### L-045 - When a state reset depends on a key remount, test the switch with the target data already cached so a loading unmount cannot hide a missing key
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `frontend` · harmful: 0
- features: closing-screen
- evidence: M27 frontend/components/closing/ClosingPage.tsx:97 (frontend)
- last seen: 2026-10-17T12:01:19Z

## Quarantined (failed when applied - ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_
