# Sessão Aberta na Landing e no Login Validation

**Verdict**: FAIL

**Date**: 2026-10-21
**Spec**: `.specs/features/landing-open-session/spec.md`
**Diff range**: `933b872..HEAD` (af21800, 1b09cb2, 7968f94)
**Verifier**: independent sub-agent (author ≠ verifier)

The feature works as the spec describes, and 16 of 18 mutants were killed. It fails on one edge case with no test: the landing with a regular (non-demo) session. Two mutants that limit the landing's session handling to demo accounts survive. The fix only needs a test; no production code changes.

---

## Task Completion

No `tasks.md` (Medium scope, inline steps). The three commits in range cover spec (af21800), landing (1b09cb2) and login (7968f94). Traceability in `spec.md` marks SESS-01 and SESS-02 as Implemented.

---

## Spec-Anchored Acceptance Criteria

### SESS-01: Voltar à sessão pela landing

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 "Ver demonstração" → "Voltar ao painel", goes to `/painel` without `POST /api/auth/demo` | label "Voltar ao painel"; nav `/painel`; zero demo calls | `frontend/components/landing/LandingSession.test.tsx:43-44` - `queryByRole('button', { name: 'Ver demonstração' })).not.toBeInTheDocument()` (hero + final CTA); `:49` - `expect(navigation.push.mock.calls).toEqual([['/painel'], ['/painel']])`; `:50` - `expect(demoCalls(fetcher)).toHaveLength(0)`; `:51` token kept | ✅ PASS |
| AC2 "Entrar" in nav and footer → "Voltar ao painel" to `/painel` | link label + `href="/painel"`, no "Entrar" | `LandingSession.test.tsx:58` - `getByRole('link', { name: 'Voltar ao painel' })).toHaveAttribute('href', '/painel')` (nav); `:61` (footer); `:59`, `:62` no "Entrar" | ✅ PASS |
| AC3 "Entrar na minha conta" (hero + final CTA) → "Sair e entrar com outra conta", signs out and goes to `/login` | label; session cleared; push `/login` | `LandingSession.test.tsx:65` `it.each` over both regions; `:69` old label gone; `:73` - `toHaveBeenCalledWith('/login')`; `:74` - `sessionStorage.getItem('reconpay.demoToken')).toBeNull()`; `:75-76` landing back to the no-session state | ✅ PASS |
| AC4 No session → landing unchanged | "Ver demonstração", "Entrar", "Entrar na minha conta" to `/login` | `frontend/components/landing/LandingPage.test.tsx:23`, `:69` `['Entrar', '/login']` (nav, footer); `:38`, `:202` "Ver demonstração" enabled; `:40`, `:204` "Entrar na minha conta" `href='/login'` | ✅ PASS |

### SESS-02: Login com sessão aberta (replaces LOG-05 AC1)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 Open session → notice with e-mail, "Ir para o painel" (`/painel`), "Sair e entrar com outra conta", no auto-redirect | h1 "Você já está em uma sessão"; e-mail; link `/painel`; button; `replace` not called | `frontend/app/login/page.test.tsx:179` test: h1 text, `toHaveTextContent('Conectado como ana@lojacentro.com.br.')`, `getByRole('link', { name: 'Ir para o painel' })).toHaveAttribute('href', '/painel')`, button present, form absent, `navigation.replace).not.toHaveBeenCalled()`; real provider: `frontend/app/login/session.test.tsx:33-34` | ✅ PASS |
| AC2 "Sair e entrar com outra conta" → signs out and shows the form | session cleared; form visible | `page.test.tsx:194` - `expect(auth.signOut).toHaveBeenCalledTimes(1)`; `session.test.tsx:38` - token `toBeNull()`; `:39-40` h1 "Entrar" + `getByLabelText('E-mail')` | ✅ PASS |
| AC3 Successful submit → `/painel` without the notice | `replace('/painel')`; no notice | `session.test.tsx:52` - `toHaveBeenCalledWith('/painel')`; `:53` - `queryByText('Você já está em uma sessão')).not.toBeInTheDocument()` (real `AuthProvider`, so `user` is set after sign-in) | ✅ PASS |
| AC4 No session → form as today | form, no notice, no redirect | `page.test.tsx:204` test - h1 "Entrar", e-mail field, no notice, `replace` not called | ✅ PASS |

**Status**: all 8 ACs are covered with assertions on the outcomes the spec defines. No spec-precision gaps.

---

## Edge Cases

- [x] Expired demo token → landing in the no-session state: `LandingSession.test.tsx:79-87` (401 on `/api/me`, token cleared, "Ver demonstração", "Entrar na minha conta" and "Entrar" to `/login`).
- [x] Regular account on the login page: `page.test.tsx:179` uses a non-demo user (`ana@lojacentro.com.br`, no `demoExpiresAt`); mutant M18 killed.
- [ ] **Regular account on the landing: NOT tested.** Every `LandingSession.test.tsx` case restores a demo session. M16 and M17 (session handling only for `demoExpiresAt`) survive. The state can happen: `AuthProvider` sits in `app/providers.tsx`, so an in-memory regular login survives client navigation to `/` (for example "Voltar para o início" on the login notice).

---

## Discrimination Sensor

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
**Result**: 16/18 killed - FAIL

---

## Gate Check

- **Gate command**: `cd frontend && corepack pnpm exec vitest run` (CI `pnpm test`) and `corepack pnpm typecheck`
- **Result**: 88 files, 1086 tests passed, 0 failed, 0 skipped; typecheck clean
- **Test delta in range**: +8 tests (`LandingSession.test.tsx` +5 including both `it.each` cases; `session.test.tsx` +2; `page.test.tsx` 2 LOG-05 AC1 tests replaced by 3 SESS-02 tests, +1). No test was weakened: the old LOG-05 AC1 redirect test was rewritten for SESS-02 AC1, which replaces it in the spec.

---

## Code Quality

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

## Fix Plans

### Fix 1: Landing test with a regular-account session

- **Root cause**: the spec's edge "conta comum (não demo)" has no landing test. All landing session tests restore a demo token, so limiting the change to demo sessions goes unnoticed (M16, M17).
- **Fix task**: in `frontend/components/landing/LandingSession.test.tsx`, add a case with a regular session open (for example, render `LoginPage` and `LandingPage` under one `AuthProvider`, or sign in through `useAuth().signIn` with the stubbed `/api/auth/login` returning a user with `demoExpiresAt: null`). Assert that the hero shows "Voltar ao painel" and clicking it pushes `/painel` with zero `/api/auth/demo` calls, that nav and footer show "Voltar ao painel" with `href="/painel"`, and that "Sair e entrar com outra conta" is shown.
- **Done when**: M16 and M17 are killed; gate green.
- **Priority**: Major (spec edge case untested; production code is correct)

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| SESS-01 | Implemented | ❌ Needs Fix (regular-account edge untested on the landing) |
| SESS-02 | Implemented | ✅ Verified |

---

## Summary

**Overall**: ❌ Not Ready (one test gap)

**Spec-anchored check**: 8/8 ACs matched the spec outcome; 0 spec-precision gaps; 1 of 3 edge-case checks untested
**Sensor**: 16/18 mutations killed
**Gate**: 1086 passed, typecheck clean

**What works**: the landing leads an open demo back to `/painel` without creating an account. The secondary CTA signs out and goes to `/login`. The login shows the notice instead of redirecting, and its switch-account button clears the token and shows the form. A successful submit still goes to `/painel`.

**Issues found**: no landing test with a regular-account session (Fix 1).

**Next steps**: implement Fix 1, then re-verify (round 1 of 3).
