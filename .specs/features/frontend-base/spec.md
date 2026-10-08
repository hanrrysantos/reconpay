# Frontend base specification

## Problem Statement

ReconPay has a working API but no browser interface. The v0 repository supplies a visual dashboard with fictional data. This feature establishes a functional Next.js frontend under `frontend/` while preserving the reference layout and showing only data returned by the API.

## Goals

- [x] A user can register, log in, inspect their account and select an accessible merchant in a browser.
- [x] The dashboard keeps the v0 visual structure and displays real recent transactions or honest empty states.
- [x] The frontend builds with TypeScript checking enabled.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Aggregated dashboard metrics and chart | The backend endpoint contract is not available yet. |
| Bank and connector health or one-click sync | The API has no such resources. |
| Merchant management, imports, reconciliation workflow and admin screens | They are later frontend slices. |
| Frontend email verification landing | Current email links target the backend HTML endpoint. |

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| API base URL | `NEXT_PUBLIC_API_URL`, default `http://localhost:8080` | Matches local Next and Spring development. | n |
| Session lifetime | JWT in browser memory only; reload requires login | User selected this policy. | y |
| Dashboard without metrics endpoint | Preserve card and panel layout with `—` and explicit unavailable state | Never present invented financial figures. | y |
| Recent transactions scope | First five latest transactions of selected merchant | Existing endpoint is merchant scoped; no consolidated endpoint yet. | n |

**Open questions:** none - the defaults above are sufficient for this slice.

## User Stories

### P1: Access the application

**User Story**: As an operator, I want to create an account and log in so that I can see my allowed merchants.

**Why P1**: Protected API data cannot be used without a session.

**Acceptance Criteria**:

1. WHEN valid credentials are submitted THEN the frontend SHALL load `/api/me` and show the user's name and role in the shell.
2. WHEN registration succeeds THEN the frontend SHALL tell the user to verify their email before login.
3. IF login or profile loading fails THEN the frontend SHALL keep the protected dashboard inaccessible and display an actionable error.
4. WHILE the page is reloaded THEN the frontend SHALL discard the in-memory token and require login again.

**Independent Test**: Log in, inspect the profile, reload and observe a return to login.

### P1: Browse accessible data

**User Story**: As an authenticated operator, I want to select an accessible merchant and see its latest transactions.

**Why P1**: It makes the reference dashboard useful with the existing API.

**Acceptance Criteria**:

1. WHEN the dashboard opens THEN the frontend SHALL list only merchants returned by `/api/me/merchants`, including all pages.
2. WHEN an accessible merchant is selected THEN the frontend SHALL display up to five transactions returned by that merchant's transaction endpoint.
3. IF merchant or transaction loading fails THEN the frontend SHALL display the error and a retry action.
4. IF the API responds with 401 THEN the frontend SHALL end the session and return to login.

**Independent Test**: Log in with merchants, switch merchants and observe the transaction table update.

### P1: Preserve the reference honestly

**User Story**: As a viewer, I want the ReconPay dashboard to resemble the v0 design without implying that fictional financial data is real.

**Why P1**: The reference is the approved visual target; financial values must be trustworthy.

**Acceptance Criteria**:

1. The frontend SHALL use the v0 sidebar, topbar, metric cards and content-panel layout with ReconPay branding.
2. WHILE aggregated metrics are unavailable THEN the frontend SHALL show `—` and an explicit unavailable state for metrics and chart panels.
3. IF a merchant has no transactions THEN the frontend SHALL show an empty state in the transaction table.
4. The frontend SHALL render transaction amounts without passing decimal values through binary floating-point numbers.

**Independent Test**: Compare the page with the v0 reference and inspect an empty merchant and a transaction with a large decimal amount.

## Edge Cases

- IF the API is unreachable THEN the frontend SHALL show a connection error and offer retry where the data is requested.
- IF the authenticated user has no accessible merchants THEN the frontend SHALL show an empty merchant state and no fabricated transactions.

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| FBASE-01 | Access the application | Auth | Verified |
| FBASE-02 | Browse accessible data | Data | Verified |
| FBASE-03 | Preserve the reference honestly | Visual | Verified |

**Coverage:** 3 total, 3 mapped to tasks, 0 unmapped.

## Success Criteria

- [x] `pnpm test`, `pnpm typecheck` and `pnpm build` pass.
- [x] Login, registration, merchant switching and recent transactions use the existing API.
- [x] No hardcoded financial values remain in the dashboard.
