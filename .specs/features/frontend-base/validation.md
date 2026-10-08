# Frontend base validation

**Result: PASS** (2026-10-08). All specified acceptance criteria are supported by source review and focused tests; the test, typecheck and production build gates pass. Browser interaction and pixel comparison remain unverified.

## Acceptance criteria

| Criterion | Result | Evidence |
| --- | --- | --- |
| Access 1: login loads `/api/me` and shows name/role | PASS (code + unit test) | `frontend/lib/api.ts:79-85` loads the profile before returning the token; `frontend/lib/api.test.ts:7-25` checks ordering and authorization; `frontend/components/dashboard.tsx:106-113` renders name, role and email. |
| Access 2: registration instructs email verification | PASS (code) | `frontend/lib/api.ts:88-90` posts to the backend; `frontend/app/register/page.tsx:21-25,42-46` shows the verification instruction only after success; backend contract: `backend/src/main/java/br/com/hanrry/reconpay/auth/openapi/AuthControllerApi.java:118-137`. |
| Access 3: failed login/profile cannot expose dashboard and has actionable error | PASS (code + unit test) | `frontend/lib/api.ts:79-85` only returns a session after profile success; `frontend/lib/auth.tsx:20-27` only saves that result; `frontend/app/login/page.tsx:24-30,46-50` presents the error and permits resubmission; `frontend/app/page.tsx:64` gates the dashboard. Profile failure is tested at `frontend/lib/api.test.ts:27-33`. |
| Access 4: reload discards token | PASS (code) | `frontend/lib/auth.tsx:16-18` stores the token only in React state; `frontend/app/page.tsx:19-23,64` redirects without one. No storage or cookie writes are present in frontend source. Browser reload was not exercised. |
| Browse 1: all accessible merchant pages | PASS (code + unit test) | `frontend/lib/api.ts:92-103` follows `totalPages` from `/api/me/merchants`; `frontend/lib/api.test.ts:35-52` checks two pages; `frontend/components/dashboard.tsx:82-90` renders returned merchants. Backend page contract: `backend/src/main/java/br/com/hanrry/reconpay/auth/openapi/MeControllerApi.java:252-290`. |
| Browse 2: up to five selected merchant transactions | PASS (code + regression test) | `frontend/lib/api.ts:105-115` requests `size=5` for the chosen ID; `frontend/app/page.tsx:43-62` ignores stale responses after effect cleanup and tags results with the merchant ID. `frontend/app/page.tsx:66-75` immediately hides a previous merchant's result on selection change; `frontend/components/dashboard.tsx:76,132-135` filters rows by merchant ID. `frontend/components/dashboard.test.ts:27-45` checks the mismatch case. |
| Browse 3: merchant/transaction error and retry | PASS (code) | `frontend/app/page.tsx:31-39,52-60` records errors and `frontend/components/dashboard.tsx:129-132` shows the message and retry button; `frontend/app/page.tsx:77` restarts requests. Network error normalization is tested at `frontend/lib/api.test.ts:85-89`. |
| Browse 4: 401 ends session | PASS (code) | `frontend/app/page.tsx:33-36,54-57` signs out and redirects on protected data 401; `frontend/lib/auth.tsx:18,24-27` clears session; dashboard is gated at `frontend/app/page.tsx:64`. This flow lacks a mounted-component test. |
| Visual 1: v0 sidebar/topbar/cards/panels and branding | PASS for source structure; pixel fidelity unverified | `frontend/components/dashboard.tsx:79-127` retains the sidebar, topbar, metric cards and content panels from `/tmp/reconpay-concilia-analysis/app/page.tsx:52-79`, with ReconPay branding. `frontend/app/globals.css:11-16,35-46,74-76` retains the v0 layout CSS from `/tmp/reconpay-concilia-analysis/app/globals.css:11-16,36-76`; an unused progress rule with a fictional percentage was removed. Mobile names and focus indicators are at `frontend/components/dashboard.tsx:95-107` and `frontend/app/globals.css:105-113`. |
| Visual 2: unavailable aggregates and chart | PASS (code) | `frontend/components/dashboard.tsx:36-41,120-127` uses `—` and explicit unavailable text for cards, chart and source health; no sample financial totals are rendered. |
| Visual 3: empty transactions | PASS (code) | `frontend/components/dashboard.tsx:129-135` gives an empty state and no rows; no-merchant state is asserted at `frontend/components/dashboard.test.ts:11-25`. |
| Visual 4: exact decimal amounts | PASS (code + unit and mutation checks) | `frontend/lib/api.ts:62-76,105-115` parses decimal JSON tokens as strings; `frontend/components/dashboard.tsx:61-66` formats with string operations; large-value checks are at `frontend/lib/api.test.ts:54-76` and `frontend/components/dashboard.test.ts:6-9`. Backend represents amounts as `BigDecimal` in `backend/src/main/java/br/com/hanrry/reconpay/transaction/dto/TransactionResponseDTO.java:11-17`. |

## Edge cases and remaining gaps

The unreachable API path displays a connection message via `frontend/lib/api.ts:46-60`; merchant and transaction loading provide retry (`frontend/components/dashboard.tsx:129-132`). A user with no accessible merchants receives an explicit state (`frontend/components/dashboard.tsx:86-88,132`). Mobile CSS reduces the sidebar to icons and keeps the merchant selector operable with a caret and visible focus outline (`frontend/app/globals.css:75,105-113`); the icon controls have explicit accessible names (`frontend/components/dashboard.tsx:95-107`).

**P3 test gap:** The 10 tests cover API parsing/pagination and dashboard markup, including a merchant mismatch, but do not mount the provider and page to exercise logout, 401, reload, retry, or the full switch sequence (`frontend/lib/api.test.ts:1-90`, `frontend/components/dashboard.test.ts:1-45`). No real backend/browser session or mobile viewport was available for this review.

## Commands and discrimination check

- `./node_modules/.bin/vitest run` in `frontend/`: **PASS**, 2 files and 10 tests (rerun after final CSS edit).
- `./node_modules/.bin/tsc --noEmit` in `frontend/`: **PASS**, exit 0.
- `./node_modules/.bin/next build --webpack` in `frontend/`: **PASS**, webpack compiled, TypeScript finished, 5 static pages generated after the final CSS edit. A concurrent build blocked one earlier attempt; the rerun passed.
- `corepack pnpm test` and `corepack pnpm exec tsc --noEmit` could not start because Corepack tried to create `/home/hanrry/.cache/node/corepack/v1` outside the writable workspace. The installed package binaries above ran the same test and typecheck tools.
- **Safe discrimination check:** copied `frontend/lib/api.ts` and `frontend/lib/api.test.ts` to `/tmp/reconpay-verifier-JqJ2PV`, replaced only the copied lossless parser with `JSON.parse`, then ran Vitest from that temporary root. Result: 1 of 7 tests failed at `api.test.ts:62`; `9007199254740993.27` was rounded to `9007199254740994`. Product files were not changed.
- **Merchant switch discrimination check:** copied `frontend/components/dashboard.tsx` and its test to `/tmp/reconpay-dashboard-verifier-iquEFi`, removed the merchant filter only in the copied component, then ran Vitest there. The new mismatch test failed at `dashboard.test.ts:42`, while the other 2 tests passed. Product files were not changed.

## Scope and limits

Review covered frontend source/tests, the v0 source in `/tmp/reconpay-concilia-analysis`, and the backend auth, merchant-page and transaction contracts. No real login, registration email, backend integration, mobile viewport, or screenshot-based pixel comparison was performed. The frontend tree was untracked during review; this report changes only `validation.md`.
