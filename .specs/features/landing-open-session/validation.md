# Sessão Aberta na Landing e no Login Validation

## Validation round 3: FAIL

**Verdict**: FAIL

**Date**: 2026-10-21
**Spec**: `.specs/features/landing-open-session/spec.md` (SESS-01 AC2 revised in bb43165)
**Diff range**: round-3 delta `61210d7..HEAD` (bb43165 code + spec, 8e16f8d docs only); full feature `933b872..HEAD`
**Verifier**: independent sub-agent (author ≠ verifier), round 3 of 3

The revised AC2 is implemented as written: with a session open, the menu and footer keep "Entrar" pointing to `/login`, and the test checks label, `href` and the absence of "Voltar ao painel" in both places. Every other AC and edge case still holds, and all 16 regression and label/href mutants are killed. The verdict is FAIL on one clause of the revised AC2 that no test exercises: "sem encerrar a sessão no clique". A mutant that makes "Entrar" call `signOut()` on click survives, in both nav+footer and nav-only variants. The fix only needs a test; production code is correct today.

---

### Task Completion

No `tasks.md` (Medium scope, inline steps). bb43165 restores the plain `<Link href="/login">Entrar</Link>` in `frontend/components/landing/LandingPage.tsx:40` (nav) and `:77` (footer), removes `SignInLink` from `frontend/components/landing/SessionLinks.tsx` (no remaining references), rewrites the AC2 test, and updates spec AC2 and the "Texto na landing com sessão" assumption row.

---

### Spec-Anchored Acceptance Criteria (re-derived)

#### SESS-01: Voltar à sessão pela landing

Each landing row runs for a restored demo session and a regular in-memory login (`frontend/components/landing/LandingSession.test.tsx:33-36`, `:55`).

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 "Ver demonstração" → "Voltar ao painel" to `/painel`, no `POST /api/auth/demo` | label; push `/painel`; zero demo calls | `LandingSession.test.tsx:45`, `:59-60`, `:65` `toEqual([['/painel'], ['/painel']])`, `:66` `demoCalls(fetcher)).toHaveLength(0)` | ✅ PASS |
| AC2 (revised) "Entrar" in nav and footer stays "Entrar" to `/login`... | label "Entrar"; `href="/login"`; no "Voltar ao painel" | `LandingSession.test.tsx:73`, `:76` `getByRole('link', { name: 'Entrar' })).toHaveAttribute('href', '/login')`; `:74`, `:77` "Voltar ao painel" link absent | ✅ PASS |
| AC2 (revised) ...without ending the session on click | after clicking "Entrar", the session is still open (token kept / landing still in session state) | no evidence: the test never clicks "Entrar". N6 and N7 survive | ❌ GAP |
| AC2 (revised) /login shows the SESS-02 AC1 notice | covered by SESS-02 AC1 | `frontend/app/login/session.test.tsx:33-34` (real provider, demo session) | ✅ PASS |
| AC3 "Entrar na minha conta" → "Sair e entrar com outra conta", signs out, `/login` | label; session ended; push `/login` | `LandingSession.test.tsx:84`, `:86`, `:88` `toHaveBeenCalledWith('/login')`, `:89` token `toBeNull()`, `:90-91` landing back to no-session state | ✅ PASS |
| AC4 No session → landing unchanged | "Ver demonstração", "Entrar" and "Entrar na minha conta" to `/login` | `frontend/components/landing/LandingPage.test.tsx:23`, `:69` `['Entrar', '/login']`; `:38`, `:202` "Ver demonstração" enabled; `:40`, `:204` "Entrar na minha conta" `href='/login'` | ✅ PASS |

#### SESS-02: Login com sessão aberta (unchanged in round 3)

| Criterion | `file:line` + assertion | Result |
| --- | --- | --- |
| AC1 notice, e-mail, "Ir para o painel" `/painel`, switch button, no auto-redirect | `frontend/app/login/page.test.tsx:179-192`; `frontend/app/login/session.test.tsx:33-34` | ✅ PASS (R7, R9 killed) |
| AC2 switch → signs out, shows the form | `page.test.tsx:200` `signOut` called once; `session.test.tsx:38-41` | ✅ PASS (R10 killed) |
| AC3 successful submit → `/painel` without the notice | `session.test.tsx:52-53` | ✅ PASS (R8 killed) |
| AC4 no session → form | `page.test.tsx:204-210` | ✅ PASS |

**Status**: ❌ 1 gap (AC2 click clause). No spec-precision gaps: the revised AC2 states the outcome precisely.

**Test integrity**: the AC2 test was rewritten in place (same count). The removed assertions (`href='/painel'`, "Entrar" absent) belonged to the superseded AC2 wording; the new ones assert the revised outcome at the same strength. No other test changed.

---

### Edge Cases

- [x] Regular account, landing: the AC2 test runs for `ana` too (`LandingSession.test.tsx:35`, `:55`); R3 and R6 killed.
- [x] Regular account, login: `page.test.tsx:179`.
- [x] Expired demo token → no-session landing: `LandingSession.test.tsx:96-105`, including nav "Entrar" to `/login` (`:104`).

---

### Discrimination Sensor

Scratch: `git worktree add --detach` at bb43165 under the session scratchpad (`verifier-sess-wt3`), real `frontend/node_modules` symlinked. A script applied each mutant alone, ran `vitest run components/landing app/login` (52 tests), and restored the file. Scratch porcelain showed only the symlink afterward. The worktree was removed with `git worktree remove --force`; the real tree's `git status --porcelain` was empty before and after, and `git worktree list` no longer lists the scratch.

| # | File | Mutation | Killed? |
| --- | --- | --- | --- |
| N1 | `frontend/components/landing/LandingPage.tsx:40` | menu "Entrar" `href="/painel"` | ✅ Killed (4) |
| N2 | `LandingPage.tsx:77` | footer "Entrar" `href="/painel"` | ✅ Killed (3) |
| N3 | `LandingPage.tsx:40`, `:77` + `SessionLinks.tsx` | round-2 `SignInLink` restored: "Voltar ao painel" to `/painel` with a session, nav and footer | ✅ Killed (2) |
| N4 | same | switch in the footer only | ✅ Killed (2) |
| N5 | same | switch in the menu only | ✅ Killed (2) |
| N6 | same | "Entrar" keeps label and `/login` but calls `signOut()` on click, nav and footer | ❌ Survived |
| N7 | same | nav "Entrar" calls `signOut()` on click only when a session is open | ❌ Survived |
| N8 | same | "Entrar" hidden with a session | ✅ Killed (2) |
| R1 | `frontend/components/landing/DemoButton.tsx` | `if (user)` → `if (false)` | ✅ Killed (8) |
| R2 | `DemoButton.tsx` | back button calls `open` (creates a demo) | ✅ Killed (2) |
| R3 | `DemoButton.tsx` | back button only for a demo session | ✅ Killed (4) |
| R4 | `frontend/components/landing/SessionLinks.tsx` | `AccountLink` skips `signOut()` | ✅ Killed (4) |
| R5 | `SessionLinks.tsx` | `AccountLink` skips `router.push('/login')` | ✅ Killed (4) |
| R6 | `SessionLinks.tsx` | `AccountLink` switches only for a demo session | ✅ Killed (2) |
| R7 | `frontend/app/login/page.tsx` | notice never shown | ✅ Killed (3) |
| R8 | `page.tsx` | notice shown after a successful submit (drop `!submitted`) | ✅ Killed (1) |
| R9 | `page.tsx` | `useEffect` auto-redirect to `/painel` restored | ✅ Killed (3) |
| R10 | `page.tsx` | notice switch button does not sign out | ✅ Killed (2) |

**Sensor depth**: expanded (18 manual mutations; auth-adjacent session flow)
**Result**: 16/18 killed - FAIL

---

### Gate Check

- **Commands**: `corepack pnpm test`, `corepack pnpm typecheck`, `corepack pnpm build` (all three CI steps)
- **Outcome**: 88 files, 1090 tests passed, 0 failed, 0 skipped; typecheck clean; build succeeds
- **Test delta**: 1090 → 1090 (AC2 test rewritten in place)

---

### Code Quality

| Principle | Status |
| --- | --- |
| Minimum code / surgical changes | ✅ reverts to plain links; dead `SignInLink` removed |
| No scope creep | ✅ |
| Matches patterns | ✅ |
| Spec-anchored outcome check | ❌ AC2 click clause has no assertion |
| Every test maps to a spec requirement | ✅ |
| Documented guidelines followed: `AGENTS.md`, `frontend/README.md` | ✅ |

---

### Fix Plans

#### Fix 1: Assert that clicking "Entrar" keeps the session

- **Root cause**: the revised AC2 adds "sem encerrar a sessão no clique", but `LandingSession.test.tsx:69-78` only inspects label and `href`, so a click handler that signs out (N6, N7) passes.
- **Fix task**: in the AC2 test (both session kinds), click the nav "Entrar" and the footer "Entrar", then assert the session is still open: the hero still shows the "Voltar ao painel" button and, for the demo kind, `sessionStorage.getItem('reconpay.demoToken')` is still `'demo-jwt'`. Test only; no production change.
- **Done when**: N6 and N7 are killed; the full suite, typecheck and build pass.
- **Priority**: Major (an unverified spec clause on the exact path that lost the production demo)

---

### Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| SESS-01 | Implemented (AC2 revisado) | ❌ Needs Fix (AC2 click clause untested) |
| SESS-02 | Verified | ✅ Verified |

---

### Summary

**Overall**: ❌ Not Ready

**Spec-anchored check**: 8 ACs; 1 clause of SESS-01 AC2 without evidence; 3/3 edge cases tested; 0 spec-precision gaps
**Sensor**: 16/18 mutations killed (N6, N7 survived)
**Gate**: 1090 passed, typecheck clean, build passes

**Lessons**: L-063 (candidate) recorded from N6/N7.

**Next steps**: Fix 1 (test only), then a round-4 Verifier run. This was the third fix→re-verify round, so the user decides whether to run another.

---


## Round 2 (history, commit 16f9b71)

**Round 2 verdict**: PASS

**Date**: 2026-10-21
**Spec**: `.specs/features/landing-open-session/spec.md`
**Diff range**: `933b872..HEAD` (af21800, 1b09cb2, 7968f94, c33e7d5, 16f9b71); round-2 delta `c33e7d5..16f9b71` touches only `frontend/components/landing/LandingSession.test.tsx`
**Verifier**: independent sub-agent (author ≠ verifier), round 2 of 3

The round-1 gap is closed. `LandingSession.test.tsx` now runs every open-session case for both a restored demo session and an in-memory regular login (`ana@lojacentro.com.br`, `demoExpiresAt: null`), so the spec's "conta comum" edge is tested on the landing. All 8 ACs and both edge cases trace to assertions on the spec-defined outcomes, and 23 of 23 mutants are killed, including round-1 survivors M16 and M17. No production code changed since round 1.

---

### Task Completion

No `tasks.md` (Medium scope, inline steps). Round-1 Fix 1 is done in 16f9b71 (test only).

---

### Spec-Anchored Acceptance Criteria (re-derived)

#### SESS-01: Voltar à sessão pela landing

Every row below runs twice through `describe.each(sessions)` (`frontend/components/landing/LandingSession.test.tsx:55`): demo session restored from `sessionStorage` (`:34`) and regular login in the same `AuthProvider` (`:26-30`, `:35`).

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 "Ver demonstração" → "Voltar ao painel" to `/painel`, no `POST /api/auth/demo` | label; push `/painel`; zero demo calls | `LandingSession.test.tsx:45` waits for hero button "Voltar ao painel"; `:59-60` `queryByRole('button', { name: 'Ver demonstração' })).not.toBeInTheDocument()` (hero, final CTA); `:65` `expect(navigation.push.mock.calls).toEqual([['/painel'], ['/painel']])`; `:66` `expect(demoCalls(fetcher)).toHaveLength(0)` (stub answers 429 to any demo call, `:19`) | ✅ PASS |
| AC2 "Entrar" in nav and footer → "Voltar ao painel" to `/painel` | link label + `href="/painel"`; no "Entrar" | `LandingSession.test.tsx:73` and `:76` `getByRole('link', { name: 'Voltar ao painel' })).toHaveAttribute('href', '/painel')`; `:74`, `:77` "Entrar" absent | ✅ PASS |
| AC3 "Entrar na minha conta" (hero + final CTA) → "Sair e entrar com outra conta", signs out, goes to `/login` | label; session ended; push `/login` | `LandingSession.test.tsx:80` `it.each` over both regions; `:84` old label gone; `:86` clicks the new button; `:88` `toHaveBeenCalledWith('/login')`; `:89` demo token `toBeNull()`; `:90-91` landing back to "Ver demonstração" and "Entrar na minha conta" `href='/login'` (proves `user` cleared for both kinds; for the regular kind `:89` is vacuous but `:90` carries the check) | ✅ PASS |
| AC4 No session → landing unchanged | "Ver demonstração", "Entrar" and "Entrar na minha conta" to `/login` | `frontend/components/landing/LandingPage.test.tsx:23`, `:69` `['Entrar', '/login']` (nav, footer); `:38`, `:202` "Ver demonstração" `toBeEnabled()`; `:40`, `:204` "Entrar na minha conta" `toHaveAttribute('href', '/login')` | ✅ PASS |

#### SESS-02: Login com sessão aberta (replaces LOG-05 AC1)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 Open session → notice, e-mail, "Ir para o painel" (`/painel`), "Sair e entrar com outra conta", no auto-redirect | h1 text; e-mail; link `/painel`; button; no form; `replace` not called | `frontend/app/login/page.test.tsx:179-192` - h1 `toHaveTextContent('Você já está em uma sessão')`, `toHaveTextContent('Conectado como ana@lojacentro.com.br.')`, link `href='/painel'`, button present, `queryByLabelText('E-mail')` absent, `navigation.replace).not.toHaveBeenCalled()`; real provider with demo: `frontend/app/login/session.test.tsx:33-34` | ✅ PASS |
| AC2 "Sair e entrar com outra conta" → signs out, shows the form | session cleared; form | `page.test.tsx:200` `expect(auth.signOut).toHaveBeenCalledTimes(1)`; `session.test.tsx:38` token `toBeNull()`; `:39-40` h1 "Entrar" and `getByLabelText('E-mail')`; `:41` no redirect | ✅ PASS |
| AC3 Successful submit → `/painel` without the notice | `replace('/painel')`; notice absent | `session.test.tsx:52` `toHaveBeenCalledWith('/painel')`; `:53` `queryByText('Você já está em uma sessão')).not.toBeInTheDocument()` (real `AuthProvider`, so `user` is set) | ✅ PASS |
| AC4 No session → form as today | form; no notice; no redirect | `page.test.tsx:204-210` h1 "Entrar", e-mail field, notice absent, `replace` not called | ✅ PASS |

**Status**: ✅ 8/8 ACs covered on the spec-defined outcomes. No spec-precision gaps.

---

### Edge Cases

- [x] Regular account (não demo), landing: `LandingSession.test.tsx:35` + `:55` run AC1–AC3 with `ana` (`:12`, `demoExpiresAt: null`) signed in through `useAuth().signIn` (`:26-30`). M16, M17, M19, M20, M21 killed.
- [x] Regular account, login: `page.test.tsx:179` uses `ana` (no `demoExpiresAt`); M18 killed.
- [x] Expired demo token → no-session landing: `LandingSession.test.tsx:96-105` (401 on `/api/me`, token cleared, "Ver demonstração", "Entrar na minha conta" and "Entrar" to `/login`).

---

### Discrimination Sensor

Scratch: `git worktree add --detach` at 16f9b71 under the session scratchpad (`verifier-sess-wt2`), real `frontend/node_modules` symlinked. A script applied each mutant alone, ran `vitest run components/landing app/login` (6 files, 52 tests), and restored the file. Scratch porcelain showed only the symlink afterward. The worktree was removed with `git worktree remove --force`; the real tree's `git status --porcelain` was empty before and after, and `git worktree list` no longer lists the scratch.

| # | File | Mutation | Killed? |
| --- | --- | --- | --- |
| M1 | `frontend/components/landing/DemoButton.tsx` | `if (user)` → `if (false)` | ✅ Killed (8) |
| M2 | `DemoButton.tsx` | back button pushes `/login` | ✅ Killed (2) |
| M3 | `DemoButton.tsx` | back button calls `open` (creates a demo) | ✅ Killed (2) |
| M4 | `frontend/components/landing/SessionLinks.tsx` | "Voltar ao painel" link `href="/login"` | ✅ Killed (2) |
| M5 | `SessionLinks.tsx` | `SignInLink` ignores `user` | ✅ Killed (2) |
| M6 | `SessionLinks.tsx` | `AccountLink` skips `signOut()` | ✅ Killed (4) |
| M7 | `SessionLinks.tsx` | `AccountLink` skips `router.push('/login')` | ✅ Killed (4) |
| M8 | `SessionLinks.tsx` | `AccountLink` pushes `/painel` | ✅ Killed (4) |
| M9 | `SessionLinks.tsx` | `AccountLink` ignores `user` | ✅ Killed (4) |
| M10 | `frontend/app/login/page.tsx` | drop `!submitted` | ✅ Killed (1) |
| M11 | `page.tsx` | notice button does not sign out | ✅ Killed (2) |
| M12 | `page.tsx` | "Ir para o painel" `href="/"` | ✅ Killed (1) |
| M13 | `page.tsx` | restore the `useEffect` auto-redirect to `/painel` | ✅ Killed (3) |
| M14 | `page.tsx` | notice omits the e-mail | ✅ Killed (2) |
| M15 | `page.tsx` | notice never shown | ✅ Killed (3) |
| M16 | `DemoButton.tsx` | `if (user)` → `if (user?.demoExpiresAt)` (round-1 survivor) | ✅ Killed (4) |
| M17 | `SessionLinks.tsx` | `SignInLink` switches only for a demo session (round-1 survivor) | ✅ Killed (1) |
| M18 | `page.tsx` | notice only for a demo session | ✅ Killed (2) |
| M19 | `SessionLinks.tsx` | `AccountLink` switches only for a demo session (new) | ✅ Killed (2) |
| M20 | `SessionLinks.tsx` | `AccountLink` signs out only a demo session (new) | ✅ Killed (2) |
| M21 | `DemoButton.tsx` | back button goes to `/painel` only for a demo, else creates a demo (new) | ✅ Killed (1) |
| M22 | `SessionLinks.tsx` | no-session "Entrar na minha conta" `href="/painel"` (new, AC4) | ✅ Killed (7) |
| M23 | `SessionLinks.tsx` | no-session "Entrar" `href="/painel"` (new, AC4) | ✅ Killed (3) |

**Sensor depth**: expanded (23 manual mutations; auth-adjacent session flow)
**Outcome**: 23/23 killed - PASS

---

### Gate Check

- **Gate command**: `cd frontend && corepack pnpm exec vitest run` (CI `pnpm test`) and `corepack pnpm typecheck`
- **Outcome**: 88 files, 1090 tests passed, 0 failed, 0 skipped; typecheck clean. `pnpm build` not re-run: no production file changed since round 1.
- **Test delta**: round 1 1086 → round 2 1090 (+4: the three landing session tests now run per session kind). No test removed or weakened; the demo cases keep every round-1 assertion.

---

### Code Quality

| Principle | Status |
| --- | --- |
| Minimum code / surgical changes | ✅ round-2 delta is one test file |
| No scope creep | ✅ |
| Matches patterns | ✅ same `stubApi` shape as `app/login/session.test.tsx` |
| Spec-anchored outcome check | ✅ |
| Per-layer coverage (happy + edge + error) | ✅ |
| Every test maps to a spec requirement | ✅ names cite SESS-01 ACs or the edge |
| Documented guidelines followed: `AGENTS.md`, `frontend/README.md` | ✅ |

---

### Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| SESS-01 | Implemented (round 1: Needs Fix) | ✅ Verified |
| SESS-02 | Implemented | ✅ Verified |

---

### Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 8/8 ACs matched the spec outcome; 3/3 edge-case checks tested; 0 spec-precision gaps
**Sensor**: 23/23 mutations killed
**Gate**: 1090 passed, typecheck clean

**Lessons**: no new signal in round 2. The round-1 gap is already recorded as L-060 (confirmed, 2 features).

---

## Round 1 (history, commit c33e7d5)

**Round 1 verdict**: FAIL

**Date**: 2026-10-21
**Spec**: `.specs/features/landing-open-session/spec.md`
**Diff range**: `933b872..HEAD` (af21800, 1b09cb2, 7968f94)
**Verifier**: independent sub-agent (author ≠ verifier)

The feature works as the spec describes, and 16 of 18 mutants were killed. It fails on one edge case with no test: the landing with a regular (non-demo) session. Two mutants that limit the landing's session handling to demo accounts survive. The fix only needs a test; no production code changes.

---

### Task Completion

No `tasks.md` (Medium scope, inline steps). The three commits in range cover spec (af21800), landing (1b09cb2) and login (7968f94). Traceability in `spec.md` marks SESS-01 and SESS-02 as Implemented.

---

### Spec-Anchored Acceptance Criteria

#### SESS-01: Voltar à sessão pela landing

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 "Ver demonstração" → "Voltar ao painel", goes to `/painel` without `POST /api/auth/demo` | label "Voltar ao painel"; nav `/painel`; zero demo calls | `frontend/components/landing/LandingSession.test.tsx:43-44` - `queryByRole('button', { name: 'Ver demonstração' })).not.toBeInTheDocument()` (hero + final CTA); `:49` - `expect(navigation.push.mock.calls).toEqual([['/painel'], ['/painel']])`; `:50` - `expect(demoCalls(fetcher)).toHaveLength(0)`; `:51` token kept | ✅ PASS |
| AC2 "Entrar" in nav and footer → "Voltar ao painel" to `/painel` | link label + `href="/painel"`, no "Entrar" | `LandingSession.test.tsx:58` - `getByRole('link', { name: 'Voltar ao painel' })).toHaveAttribute('href', '/painel')` (nav); `:61` (footer); `:59`, `:62` no "Entrar" | ✅ PASS |
| AC3 "Entrar na minha conta" (hero + final CTA) → "Sair e entrar com outra conta", signs out and goes to `/login` | label; session cleared; push `/login` | `LandingSession.test.tsx:65` `it.each` over both regions; `:69` old label gone; `:73` - `toHaveBeenCalledWith('/login')`; `:74` - `sessionStorage.getItem('reconpay.demoToken')).toBeNull()`; `:75-76` landing back to the no-session state | ✅ PASS |
| AC4 No session → landing unchanged | "Ver demonstração", "Entrar", "Entrar na minha conta" to `/login` | `frontend/components/landing/LandingPage.test.tsx:23`, `:69` `['Entrar', '/login']` (nav, footer); `:38`, `:202` "Ver demonstração" enabled; `:40`, `:204` "Entrar na minha conta" `href='/login'` | ✅ PASS |

#### SESS-02: Login com sessão aberta (replaces LOG-05 AC1)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 Open session → notice with e-mail, "Ir para o painel" (`/painel`), "Sair e entrar com outra conta", no auto-redirect | h1 "Você já está em uma sessão"; e-mail; link `/painel`; button; `replace` not called | `frontend/app/login/page.test.tsx:179` test: h1 text, `toHaveTextContent('Conectado como ana@lojacentro.com.br.')`, `getByRole('link', { name: 'Ir para o painel' })).toHaveAttribute('href', '/painel')`, button present, form absent, `navigation.replace).not.toHaveBeenCalled()`; real provider: `frontend/app/login/session.test.tsx:33-34` | ✅ PASS |
| AC2 "Sair e entrar com outra conta" → signs out and shows the form | session cleared; form visible | `page.test.tsx:194` - `expect(auth.signOut).toHaveBeenCalledTimes(1)`; `session.test.tsx:38` - token `toBeNull()`; `:39-40` h1 "Entrar" + `getByLabelText('E-mail')` | ✅ PASS |
| AC3 Successful submit → `/painel` without the notice | `replace('/painel')`; no notice | `session.test.tsx:52` - `toHaveBeenCalledWith('/painel')`; `:53` - `queryByText('Você já está em uma sessão')).not.toBeInTheDocument()` (real `AuthProvider`, so `user` is set after sign-in) | ✅ PASS |
| AC4 No session → form as today | form, no notice, no redirect | `page.test.tsx:204` test - h1 "Entrar", e-mail field, no notice, `replace` not called | ✅ PASS |

**Status**: all 8 ACs are covered with assertions on the outcomes the spec defines. No spec-precision gaps.

---

### Edge Cases

- [x] Expired demo token → landing in the no-session state: `LandingSession.test.tsx:79-87` (401 on `/api/me`, token cleared, "Ver demonstração", "Entrar na minha conta" and "Entrar" to `/login`).
- [x] Regular account on the login page: `page.test.tsx:179` uses a non-demo user (`ana@lojacentro.com.br`, no `demoExpiresAt`); mutant M18 killed.
- [ ] **Regular account on the landing: NOT tested.** Every `LandingSession.test.tsx` case restores a demo session. M16 and M17 (session handling only for `demoExpiresAt`) survive. The state can happen: `AuthProvider` sits in `app/providers.tsx`, so an in-memory regular login survives client navigation to `/` (for example "Voltar para o início" on the login notice).

---

### Discrimination Sensor

Scratch: temporary `git worktree` at HEAD (7968f94) with a symlink to the real `frontend/node_modules`. Each mutant was applied alone, tested with the five feature test files (M16–M18 with `components/landing/` and `app/login/`), then reverted. The scratch was removed with `git worktree remove --force`. `git status --porcelain` on the real tree was empty before and after.

| # | File:line | Mutation | Killed? |
| --- | --- | --- | --- |
| M1 | `frontend/components/landing/DemoButton.tsx:40` | `if (user)` → `if (false)` (always creates a demo) | ✅ Killed (4) |
| M2 | `DemoButton.tsx:42` | back button pushes `/login` | ✅ Killed |
| M3 | `DemoButton.tsx:42` | back button calls `open` (`startDemo`) | ✅ Killed |
| M4 | `frontend/components/landing/SessionLinks.tsx:11` | "Voltar ao painel" link `href="/login"` | ✅ Killed |
| M5 | `SessionLinks.tsx:10` | `SignInLink` ignores `user` | ✅ Killed |
| M6 | `SessionLinks.tsx:23` | `AccountLink` skips `signOut()` | ✅ Killed (2) |
| M7 | `SessionLinks.tsx:24` | `AccountLink` skips `router.push('/login')` | ✅ Killed (2) |
| M8 | `SessionLinks.tsx:24` | `AccountLink` pushes `/painel` | ✅ Killed (2) |
| M9 | `SessionLinks.tsx:20` | `AccountLink` ignores `user` | ✅ Killed (2) |
| M10 | `frontend/app/login/page.tsx:46` | drop `!submitted` (notice after successful submit) | ✅ Killed |
| M11 | `page.tsx:47` | notice button does not call `signOut` | ✅ Killed (2) |
| M12 | `page.tsx:120` | "Ir para o painel" `href="/"` | ✅ Killed |
| M13 | `page.tsx:19` | restore `useEffect` auto-redirect to `/painel` | ✅ Killed (3) |
| M14 | `page.tsx:116` | notice omits the e-mail | ✅ Killed (2) |
| M15 | `page.tsx:46` | notice never shown | ✅ Killed (3) |
| M16 | `DemoButton.tsx:40` | `if (user)` → `if (user?.demoExpiresAt)` (regular session creates a demo) | ❌ **Survived** |
| M17 | `SessionLinks.tsx:10` | `SignInLink` switches only for a demo session | ❌ **Survived** |
| M18 | `page.tsx:46` | notice only for a demo session | ✅ Killed (2) |

**Sensor depth**: expanded (18 manual mutations; auth-adjacent session flow)
**Outcome**: 16/18 killed - FAIL

---

### Gate Check

- **Gate command**: `cd frontend && corepack pnpm exec vitest run` (CI `pnpm test`) and `corepack pnpm typecheck`
- **Outcome**: 88 files, 1086 tests passed, 0 failed, 0 skipped; typecheck clean
- **Test delta in range**: +8 tests (`LandingSession.test.tsx` +5 including both `it.each` cases; `session.test.tsx` +2; `page.test.tsx` 2 LOG-05 AC1 tests replaced by 3 SESS-02 tests, +1). No test was weakened: the old LOG-05 AC1 redirect test was rewritten for SESS-02 AC1, which replaces it in the spec.

---

### Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ `SessionLinks.tsx` has two small components used in three places |
| Surgical changes | ✅ the login diff is mostly re-indentation under the new conditional |
| No scope creep | ✅ `/register`, backend and dashboard "Sair" untouched (Out of Scope) |
| Matches patterns | ✅ reuses `useAuth`, `authPrimaryButton`, `BackHomeLink` |
| Spec-anchored outcome check | ✅ |
| Per-layer coverage | ⚠️ the landing has no case for the regular-account edge |
| Every test maps to a spec requirement | ✅ test names cite SESS-01/SESS-02 ACs or the edge |
| Documented guidelines followed: `AGENTS.md`, `frontend/README.md` | ✅ |

Note (not a gap): `submitted` stays `true` after a failed submit. That is harmless because a failed sign-in leaves `user` null.

---

### Fix Plans

#### Fix 1: Landing test with a regular-account session

- **Root cause**: the spec's edge "conta comum (não demo)" has no landing test. All landing session tests restore a demo token, so limiting the change to demo sessions goes unnoticed (M16, M17).
- **Fix task**: in `frontend/components/landing/LandingSession.test.tsx`, add a case with a regular session open (for example, render `LoginPage` and `LandingPage` under one `AuthProvider`, or sign in through `useAuth().signIn` with the stubbed `/api/auth/login` returning a user with `demoExpiresAt: null`). Assert that the hero shows "Voltar ao painel" and clicking it pushes `/painel` with zero `/api/auth/demo` calls, that nav and footer show "Voltar ao painel" with `href="/painel"`, and that "Sair e entrar com outra conta" is shown.
- **Done when**: M16 and M17 are killed; gate green.
- **Priority**: Major (spec edge case untested; production code is correct)

---

### Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| SESS-01 | Implemented | ❌ Needs Fix (regular-account edge untested on the landing) |
| SESS-02 | Implemented | ✅ Verified |

---

### Summary

**Overall**: ❌ Not Ready (one test gap)

**Spec-anchored check**: 8/8 ACs matched the spec outcome; 0 spec-precision gaps; 1 of 3 edge-case checks untested
**Sensor**: 16/18 mutations killed
**Gate**: 1086 passed, typecheck clean

**What works**: the landing leads an open demo back to `/painel` without creating an account. The secondary CTA signs out and goes to `/login`. The login shows the notice instead of redirecting, and its switch-account button clears the token and shows the form. A successful submit still goes to `/painel`.

**Issues found**: no landing test with a regular-account session (Fix 1).

**Next steps**: implement Fix 1, then re-verify (round 1 of 3).
