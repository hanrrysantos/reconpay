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

## Quarantined (failed when applied - ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_
