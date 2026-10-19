# Cadastro e Verificação de E-mail Validation

**Verdict**: PASS

**Date**: 2026-10-19
**Round**: 3 of max 3 (re-verification after fix commit `e2c1720`)
**Spec**: `.specs/features/register-flow/spec.md`
**Diff range**: `48b1ad4..HEAD` (e6a8a58, 296d5b9, beabe00, f090860, 29c9747, 2982726, e8ef329, c8c244d, 03e91a4, 719a9da, e2c1720)
**Verifier**: independent sub-agent (author ≠ verifier)

---

## Round history

| Round | Verdict | Gap | Closed by | Evidence it is closed |
| --- | --- | --- | --- | --- |
| 1 | FAIL | RSD-01 AC3 "conta ativa": mutant B1 (`.filter(user -> !user.isActive())` removed) survived because the test passed through the missing-token branch | `719a9da` (+9/−0, tests only) | `backend/src/test/java/br/com/hanrry/reconpay/auth/service/EmailVerificationServiceTest.java:176-184` leaves every other condition open and asserts `assertNothingSent()`; B1 killed in round 2 |
| 1 | FAIL | RSD-01 AC6: tag "Authentication" not asserted on the resend operation | `719a9da` | `backend/src/test/java/br/com/hanrry/reconpay/openapi/ApiDocsIntegrationTest.java:194` `assertOperationTag(document, "post", "/api/auth/resend-verification", "Authentication")`; T1 killed in round 2 |
| 2 | FAIL | CAD-02 AC4: ranges "A–Z" / "0–9" tested at one interior value only; N7 (`/[0-9]/`→`/[1-9]/`) and N13 (`/[A-Z]/`→`/[B-Z]/`) survived | `e2c1720` (+4/−0, tests only) | `frontend/lib/password.test.ts:23-24` (`'Areconpay1'`, `'reconpayZ1'` → `true`) and `:30-31` (`'Reconpay0'`, `'Reconpay9'` → `true`); N7, N13, `[0-8]`, `[A-Y]` all killed in round 3 |
| 3 | PASS | - | - | - |

`git show e2c1720` adds four `expect(...).toBe(true)` lines to the two existing range tests and removes or loosens nothing. The original interior checks (`:21-22`, `:28-29`) are still there. Each new input has exactly one character of the class, and it sits at the range end, so the test cannot pass through another character.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 | ✅ Done | composer, link factory, Resend/Logging senders |
| T2 | ✅ Done | round 1 gap closed in `719a9da` (B1 killed) |
| T3 | ✅ Done | round 1 gap closed in `719a9da` (tag asserted) |
| T4 | ✅ Done | round 2 gap closed in `e2c1720` (range endpoints asserted; N7, N13 killed) |
| T5 | ✅ Done | login tests unchanged |
| T6 | ✅ Done | - |
| T7 | ✅ Done | - |
| T8 | ✅ Done | - |

---

## Spec-Anchored Acceptance Criteria

Round 3 re-derived every row against HEAD `e2c1720`. Since round 2 only `frontend/lib/password.test.ts` changed (+4/−0), so every citation outside that file is unchanged; the `password.test.ts` citations below are updated for the new lines.

### CAD-01 Visual do cadastro

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 ≥1024 px | marca, title, lead, footer; h1 "Criar conta", subtitle | `frontend/app/register/page.test.tsx:36-43` - `within(panel).getByText('reconpay')`, heading name exact, lead exact, footer exact, `getByRole('heading', { level: 1, name: 'Criar conta' })`, subtitle exact, panel precedes main | ✅ PASS |
| AC2 <1024 px | text and footer hidden, brand and title kept | `frontend/app/register/page.test.tsx:50-52` - `toHaveClass('hidden', 'lg:block')`, `toHaveClass('hidden', 'lg:flex')`, heading `not.toHaveClass('hidden')`; `frontend/components/auth/AuthPanel.test.tsx:19-22` (N8 killed) | ✅ PASS (class-level; jsdom has no layout) |
| AC3 login keeps title/text | login copy unchanged | `frontend/app/login/page.test.tsx:37-38` exact texts | ✅ PASS |

### CAD-02 Formulário

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 | placeholders, button, "Já tem conta?" + "Entrar" → `/login` | `frontend/app/register/page.test.tsx:58-63` | ✅ PASS |
| AC2 | required ×3, autocomplete, `type="email"`, minLength 8, pattern | `frontend/app/register/page.test.tsx:69-78` - `toBeRequired()`, `toHaveAttribute('pattern', '(?=.*[A-Z])(?=.*[0-9]).{8,}')` (N15 killed); `frontend/lib/password.test.ts:34-39` `matches(...)` true/false | ✅ PASS |
| AC3 | show/hide, label, `aria-pressed`, value kept | `frontend/app/register/page.test.tsx:86-94` (N11 killed) | ✅ PASS |
| AC4 | each rule marked as met/pending while typing: "8 caracteres" (8+), "Uma letra maiúscula" (A–Z), "Um número" (0–9) (`.specs/features/register-flow/spec.md:96`) | `frontend/lib/password.test.ts:16-17` 7 → `false`, 8 → `true`; uppercase `:21` `test('reconpay20')).toBe(false)`, `:22` `'reconPay20'` → `true`, **`:23` `test('Areconpay1')).toBe(true)`**, **`:24` `test('reconpayZ1')).toBe(true)`**; digit `:28` `'Reconpay'` → `false`, `:29` `'Reconpay7'` → `true`, **`:30` `test('Reconpay0')).toBe(true)`**, **`:31` `test('Reconpay9')).toBe(true)`**. Each new value has exactly one character of the class, at the range end. UI met/pending and screen-reader state: `frontend/app/register/page.test.tsx:100-122` (F2, F3 killed). Mutants N7 `[1-9]`, `[0-8]`, N13 `[B-Z]`, `[A-Y]` all killed | ✅ PASS |

### CAD-03 Enviar o cadastro

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 | `{name, email, password}` | `frontend/app/register/page.test.tsx:132` `toHaveBeenCalledWith({...})` | ✅ PASS |
| AC2 | disabled, "Criando conta...", one send | `frontend/app/register/page.test.tsx:142-147` | ✅ PASS |
| AC3 | confirmation texts, `/login`, form gone | `frontend/app/register/page.test.tsx:159-165` | ✅ PASS |

### CAD-04 Erro no cadastro

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 | API message in alert above button, fields kept | `frontend/app/register/page.test.tsx:176-180` (N10 killed) | ✅ PASS |
| AC2 | default message | `frontend/app/register/page.test.tsx:191` | ✅ PASS |
| AC3 | alert cleared before resend | `frontend/app/register/page.test.tsx:211-212` | ✅ PASS |

### MAIL-01 E-mail de verificação

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 | subject + HTML parts | `backend/src/test/java/br/com/hanrry/reconpay/auth/email/VerificationEmailComposerTest.java:22` `isEqualTo("Confirme seu e-mail — ReconPay")`; `:24-33` `.contains(...)`; `backend/src/test/java/br/com/hanrry/reconpay/auth/email/ResendEmailSenderTest.java:31-33` `jsonPath(...)` | ✅ PASS |
| AC2 | "24 horas" default; "1 hora" | `backend/src/test/java/br/com/hanrry/reconpay/auth/email/VerificationEmailComposerTest.java:30`, `:41-44` | ✅ PASS |
| AC3 | plain text with link | `backend/src/test/java/br/com/hanrry/reconpay/auth/email/VerificationEmailComposerTest.java:51-57` (N6 killed at `:54`) | ✅ PASS |
| AC4 | escape `< > & " '` | `backend/src/test/java/br/com/hanrry/reconpay/auth/email/VerificationEmailComposerTest.java:65` `contains("Olá, &lt;b&gt;&quot;Zé&quot; &amp; &#39;Ana&#39;&lt;/b&gt;!")` (N1, N4 killed) | ✅ PASS |
| AC5 | `<base>/verificar-email?token=<token>` | `backend/src/test/java/br/com/hanrry/reconpay/auth/email/EmailVerificationLinkFactoryTest.java:15`, `:24` | ✅ PASS |

### VER-01 / VER-02 / VER-03 Página de confirmação e reenvio

| Criterion | `file:line` + assertion | Result |
| --- | --- | --- |
| VER-01 AC1 | `frontend/components/auth/VerifyEmail.test.tsx:38-40`; `frontend/lib/api.test.ts:96-98` | ✅ PASS |
| VER-01 AC2 | `frontend/components/auth/VerifyEmail.test.tsx:62-64` | ✅ PASS |
| VER-02 AC3 | `frontend/components/auth/VerifyEmail.test.tsx:71-77`, `:84-86`; `frontend/app/verificar-email/page.test.tsx:39-40` | ✅ PASS |
| VER-02 AC4 | `frontend/components/auth/VerifyEmail.test.tsx:93-109` | ✅ PASS |
| VER-03 AC1 | `frontend/components/auth/VerifyEmail.test.tsx:118-121`; `frontend/lib/api.test.ts:112-114` | ✅ PASS |
| VER-03 AC2 | `frontend/components/auth/VerifyEmail.test.tsx:133-138` | ✅ PASS |
| VER-03 AC3 | `frontend/components/auth/VerifyEmail.test.tsx:150-151`, `:173` (N12 killed) | ✅ PASS |

### RSD-01 Reenvio na API

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 | `202`, no body | `backend/src/test/java/br/com/hanrry/reconpay/auth/controller/ResendVerificationIntegrationTest.java:45-46` `status().isAccepted()`, `content().string("")`; `:59-60`; `:71-72` | ✅ PASS |
| AC2 | `400` `VALIDATION_ERROR` | `backend/src/test/java/br/com/hanrry/reconpay/auth/controller/ResendVerificationIntegrationTest.java:81-82` (N3 killed) | ✅ PASS |
| AC3 | send only when the account exists, is inactive, has a token and never consumed one | send `backend/src/test/java/br/com/hanrry/reconpay/auth/service/EmailVerificationServiceTest.java:136-142` (`verify(emailSender).sendEmailVerification("ana@lojacentro.com.br", "Ana", "fresh")`); no account `:164-166`; **active `:176-184` (isolated, B1 killed)**; never received `:192-196`; consumed `:203-207`; integration path `backend/src/test/java/br/com/hanrry/reconpay/auth/controller/ResendVerificationIntegrationTest.java:49` (N5 killed) | ✅ PASS |
| AC4 | 60 s cooldown | `backend/src/test/java/br/com/hanrry/reconpay/auth/service/EmailVerificationServiceTest.java:216-220` (59 s → nothing; N14 killed); `:130` + `:142` (exactly 60 s → sent) | ✅ PASS |
| AC5 | old links invalidated | `backend/src/test/java/br/com/hanrry/reconpay/auth/controller/ResendVerificationIntegrationTest.java:52-53` | ✅ PASS |
| AC6 | tag "Authentication", 202/400, public | `backend/src/test/java/br/com/hanrry/reconpay/openapi/ApiDocsIntegrationTest.java:180-193` `Op("post", "/api/auth/resend-verification", Set.of("202","400"), false, ...)`; `:194` `assertOperationTag(..., "Authentication")` (T1 killed) | ✅ PASS |

### VER-04 Rotas públicas

| Criterion | `file:line` + assertion | Result |
| --- | --- | --- |
| AC1 | `frontend/app/verificar-email/page.test.tsx:29-32`; `frontend/app/register/page.test.tsx:36-42`; `backend/src/test/java/br/com/hanrry/reconpay/auth/controller/ResendVerificationIntegrationTest.java:45` | ✅ PASS |

**Status**: ✅ All ACs covered with spec-anchored assertions. Round 1 and round 2 gaps closed.

---

## Edge Cases

- [x] Token with spaces sent as-is: `frontend/components/auth/VerifyEmail.test.tsx:56`, `frontend/lib/api.test.ts:98`.
- [x] Opened twice (Strict Mode) → one call: `frontend/components/auth/VerifyEmail.test.tsx:49`.
- [x] Resend e-mail trimmed: `frontend/lib/api.test.ts:114`; backend `backend/src/test/java/br/com/hanrry/reconpay/auth/service/EmailVerificationServiceTest.java:155-157`.

---

## Discrimination Sensor (round 3)

Scratch: `git worktree add --detach /home/hanrry/.claude/jobs/3748ece6/tmp/scratch3 HEAD`, with `frontend/node_modules` symlinked from the real tree. A script applied each mutant (exact single-occurrence string replace), ran `corepack pnpm exec vitest run <covering files>` in the scratch, and reverted with `git checkout`. The scratch was removed with `git worktree remove --force` and `git worktree prune`. Real-tree `git status --porcelain` before = after: ` M .specs/LESSONS.md`, ` M .specs/lessons.json`, `?? .specs/features/register-flow/validation.md`. The md5 of LESSONS.md (`8dae5327…`) and lessons.json (`7390877c…`) did not change, so isolation is confirmed.

| # | Mutation | File:line | Killed? | Killing test / reason |
| --- | --- | --- | --- | --- |
| N7 (re-run) | digit rule `/[0-9]/` → `/[1-9]/` | `frontend/lib/password.ts:5` | ✅ Killed | `frontend/lib/password.test.ts:30` "exige um número de 0 a 9" |
| N7b | digit rule `/[0-9]/` → `/[0-8]/` | `frontend/lib/password.ts:5` | ✅ Killed | `frontend/lib/password.test.ts:31` |
| N13 (re-run) | uppercase rule `/[A-Z]/` → `/[B-Z]/` | `frontend/lib/password.ts:4` | ✅ Killed | `frontend/lib/password.test.ts:23` "exige uma letra maiúscula de A a Z" |
| N13b | uppercase rule `/[A-Z]/` → `/[A-Y]/` | `frontend/lib/password.ts:4` | ✅ Killed | `frontend/lib/password.test.ts:24` |
| F1 | `if (busy) return` guard removed | `frontend/app/register/page.tsx:19` | ✅ Killed | register "desabilita o botão com "Criando conta..." e não envia de novo (CAD-03 AC2)" |
| F2 | rule state inverted `met = !rule.test(password)` | `frontend/app/register/page.tsx:106` | ✅ Killed | register "marca cada regra da senha enquanto o usuário digita (CAD-02 AC4)" |
| F3 | screen-reader text swapped `'pendente'` ↔ `'cumprida'` | `frontend/app/register/page.tsx:113` | ✅ Killed | register CAD-02 AC4 |
| F4 | `setError('')` before resend removed | `frontend/app/register/page.tsx:22` | ✅ Killed | register "tira o aviso antes do novo envio... (CAD-04 AC3)" |
| F5 | confirmation shows a constant instead of the e-mail used | `frontend/app/register/page.tsx:26` | ✅ Killed | register CAD-03 AC3, CAD-04 AC3 |
| F6 | invalid-token status `400` → `404` | `frontend/components/auth/VerifyEmail.tsx:27` | ✅ Killed | VerifyEmail VER-02 AC3, VER-03 AC1-AC3 (5 tests) |
| F7 | `onSent(email.trim())` → `onSent(email)` | `frontend/components/auth/VerifyEmail.tsx:109` | ➖ Survived - equivalent | `<input type="email">` value sanitization (HTML spec, same in jsdom and browsers) strips leading/trailing spaces before `FormData` reads it. Proof: under the mutant the code passes the raw `email` to the mocked `resendVerification` and `frontend/components/auth/VerifyEmail.test.tsx:118` `toHaveBeenCalledWith('ana@lojacentro.com.br')` still holds after typing `'  ana@lojacentro.com.br  '` at `:115`. The `.trim()` is defensive; the edge case (`spec.md:222`) stays pinned at `:118` and `:121` |
| F8 | "Tentar de novo" made a no-op | `frontend/components/auth/VerifyEmail.tsx:74` | ✅ Killed | VerifyEmail "mostra o erro de rede com "Tentar de novo"... (VER-02 AC4)" |
| F9 | `requested.current = token` removed (Strict Mode guard) | `frontend/components/auth/VerifyEmail.tsx:34` | ✅ Killed | VerifyEmail "chama a API uma vez só em modo estrito (edge case)" |
| F10 | page reads `?t=` instead of `?token=` | `frontend/app/verificar-email/page.tsx:8` | ✅ Killed | `/verificar-email` "abre sem sessão e confirma o token da URL (VER-04, VER-01 AC1)" |
| F11 | resend button never shows "Enviando..." | `frontend/components/auth/VerifyEmail.tsx:134` | ✅ Killed | VerifyEmail "desabilita "Enviar novo link"... (VER-03 AC2)" |

F1-F11 are fresh: none of them is in the round 1 or round 2 tables (round 2: B1, T1, T2, N1-N15). Backend mutants were not re-run this round because no backend file changed since round 2 (`git diff --stat 719a9da..HEAD` lists only `frontend/lib/password.test.ts`). Round 2 backend results stand: B1, T1, N1, N3, N4, N5, N6, N14 killed; T2, N2 equivalent.

**Sensor depth**: P0-expanded (auth). Round 3: 15 mutants (4 on the password ranges, 11 fresh on the register page, the verify component and the verify page).
**Result**: 14 killed, 0 non-equivalent survivors, 1 equivalent (F7). ✅ PASS

### Earlier rounds (summary)

- Round 2: 18 mutants; 13 killed, 2 survived (N7, N13, now killed), 3 equivalent (T2 extra method tag merged by springdoc; N2 `UriComponentsBuilder` already collapses `//`; N9 `!token` guard covered by the `requested` ref).
- Round 1: B1 survived (killed in round 2 after `719a9da`).

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code / surgical / no scope creep / patterns | ✅ (production code unchanged since round 1; `719a9da` and `e2c1720` touch tests only) |
| No weakened tests | ✅ `git show 719a9da`: +9/−0; `git show e2c1720`: +4/−0 |
| Spec-anchored outcome check | ✅ CAD-02 AC4 ranges now pinned at both ends |
| Per-layer coverage | ✅ |
| Every test maps to a spec requirement | ✅ (the new `e2c1720` lines live in the existing CAD-02 AC4 range tests) |
| Documented guidelines (`AGENTS.md`) | ✅ |

---

## Gate Check

- **Full (backend)**: `cd backend && ./mvnw verify` → exit 0; 651 tests, 0 failures, 0 errors, 0 skipped (surefire reports); JaCoCo checks met (line 96.13%, branch 87.58%; thresholds 85% / 75%).
- **Build (frontend)**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build` → exit 0; 71 files, 915 tests passed; typecheck OK; build OK (`/register`, `/verificar-email` static).
- **Test count**: backend 651, frontend 915. Both match round 2: `e2c1720` adds assertions to existing tests rather than new tests. No test removed.
- **Skipped tests**: none.

---

## Fix Plans

None. The round 2 fix (Fix 1, CAD-02 AC4 range endpoints) is done in `e2c1720` and confirmed above.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| CAD-01 | ✅ Verified | ✅ Verified |
| CAD-02 | ❌ Needs Fix | ✅ Verified |
| CAD-03 | ✅ Verified | ✅ Verified |
| CAD-04 | ✅ Verified | ✅ Verified |
| MAIL-01 | ✅ Verified | ✅ Verified |
| VER-01 | ✅ Verified | ✅ Verified |
| VER-02 | ✅ Verified | ✅ Verified |
| VER-03 | ✅ Verified | ✅ Verified |
| RSD-01 | ✅ Verified | ✅ Verified |
| VER-04 | ✅ Verified | ✅ Verified |

---

## Summary

**Overall**: ✅ Ready

**Round 2 gap**: closed (`frontend/lib/password.test.ts:23-24`, `:30-31`; N7, N13, `[0-8]`, `[A-Y]` killed).
**Spec-anchored check**: every AC matches its spec outcome; no spec-precision gaps.
**Sensor**: 14/14 non-equivalent mutants killed (1 equivalent: F7).
**Gate**: backend 651 passed with coverage met; frontend 915 passed, typecheck and build OK.

**Next steps**: none for validation. The feature can close; the user decides on push/PR.
