# Tela de Empresas Validation

**Date**: 2026-10-15
**Spec**: `.specs/features/merchants-screen/spec.md`
**Diff range**: `aa914ef..df2066c` (spec/design `d4af405`, `08f0622`; implementation `6767d18..e4d9250`; fixes `91310bf`, `f0c30e4`, `df2066c`), branch `feat/merchants-screen`, 17 commits, 45 files
**Verifier**: independent sub-agent (author ≠ verifier), re-verification iteration 2 of 3

## Validation: merchants-screen - PASS ✅

---

## Iteration 1 gaps and how they were closed

| Iteration 1 gap | Fix | Iteration 2 evidence |
| --- | --- | --- |
| Gap 1 (Major, MDOC-02/03): `@Size(max = 30)` next to `@ValidCnpj` made the 400 message for a long invalid document random | `91310bf` removed `@Size` from `MerchantRequestDTO.document` (`backend/src/main/java/br/com/hanrry/reconpay/merchant/dto/MerchantRequestDTO.java:13-15` now `@NotBlank` + `@ValidCnpj` only; `@ValidCnpj` passes blank, `CnpjValidator.java:11-13`, so at most one constraint fails for any input). Added `MIT:170` cases `"1234567800019599999999999999999"` and `"x".repeat(31)` | Scratch probe through the real HTTP path (MockMvc + Spring + Postgres Testcontainer): 6 inputs > 30 chars (31 digits, 31 `x`, 36 alnum, invalid CNPJ + 20 spaces, 200 digits, `12A…` + 40 `-`) × 10 rounds = 60 requests, every one `400 VALIDATION_ERROR "CNPJ inválido"`. Probe file deleted with the scratch |
| Gap 2 (Minor, MDOC-01): valid CNPJ padded with spaces beyond 30 chars got the `@Size` message | Same commit; new `MIT:128-133` | `MIT:131-133` `status().isCreated()`, `jsonPath("$.document").value(digits)` for `"  " + formatted + 20 spaces` |
| Gap 3 (Minor, MLIST-07): network failure on the list not exercised | `f0c30e4` added `MPT:196-209` | `MPT:202` alert `toHaveTextContent('Não foi possível conectar à API. Tente novamente.')`, `MPT:206-208` retry renders the list and the alert is gone |
| Spec-precision gap (MCAD-05): spec did not name the messages | `df2066c` states "Informe o nome da empresa" / "Use no máximo 150 caracteres" in MCAD-05 | `MPT:297` and `MPT:308` assert exactly those texts; sensor F2/F3 killed |

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 `TestCnpj` | ✅ Done | - |
| T2 `CnpjRules` | ✅ Done | - |
| T3 `@ValidCnpj` | ✅ Done | `@Size` conflict removed in `91310bf` |
| T4 digits + duplicate | ✅ Done | - |
| T5 `lib/cnpj.ts` | ✅ Done | - |
| T6 API functions | ✅ Done | - |
| T7 hooks | ✅ Done | - |
| T8 list | ✅ Done | network-failure case added in `f0c30e4` |
| T9 create drawer | ✅ Done | - |
| T10 rename | ✅ Done | - |
| T11 deactivate | ✅ Done | - |
| T12 docs | ✅ Done | - |

---

## Spec-Anchored Acceptance Criteria

Abbreviations: `MIT` = `backend/src/test/java/br/com/hanrry/reconpay/merchant/integration/MerchantIntegrationTest.java`, `MST` = `backend/src/test/java/br/com/hanrry/reconpay/merchant/service/MerchantServiceTest.java`, `CRT` = `backend/src/test/java/br/com/hanrry/reconpay/shared/CnpjRulesTest.java`, `MPT` = `frontend/components/merchants/MerchantsPage.test.tsx`, `QMT` = `frontend/lib/queries.merchants.test.tsx`, `CNT` = `frontend/lib/cnpj.test.ts`, `AMT` = `frontend/lib/api.merchants.test.ts`. Line numbers re-derived at `df2066c`.

### P1: Cadastrar empresa

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| MCAD-01 | Drawer opens with empty Nome and CNPJ | `MPT:240-241` `expect(nameField()).toHaveValue('')`, `expect(cnpjField()).toHaveValue('')`; from the empty state `MPT:249-250` | ✅ PASS |
| MCAD-02 | Mask `00.000.000/0000-00`, at most 14 digits | `MPT:256` `toHaveValue('12.345')`, `MPT:258` `toHaveValue('12.345.678/0001-95')` after 16 digits typed; `CNT:23` progressive table, `CNT:27` `maskCnpj('123456780001951')` → `'12.345.678/0001-95'` | ✅ PASS |
| MCAD-03 | `POST /api/merchants`, `name` trimmed, `document` = 14 digits | `MPT:276` `expect(posts()[0].body).toEqual({ name: 'Loja Teste', document: '12345678000195' })` (input `'  Loja Teste  '`); URL/method/body `AMT:29-30` | ✅ PASS |
| MCAD-04 | 201 → drawer closes, merchant in list and top menu, selected | `MPT:285` dialog gone; `MPT:286` `row('Loja Teste')` `aria-current='true'`; `MPT:288` `menuButton()` `toHaveTextContent('Loja Teste')`; `MPT:290` option `aria-selected='true'`; hook `QMT:75-78` `menuCache()` `[alfa, beta]`, `selected.merchantId` `'m-2'` | ✅ PASS |
| MCAD-05 | Empty (after trim) → "Informe o nome da empresa"; > 150 → "Use no máximo 150 caracteres"; no API call | `MPT:297` `getByText('Informe o nome da empresa')`, `MPT:298` `aria-invalid='true'`, `MPT:299` `posts()` length 0; `MPT:308` `getByText('Use no máximo 150 caracteres')`, `MPT:309` length 0 | ✅ PASS (spec-precision gap from iteration 1 closed by `df2066c`) |
| MCAD-06 | Invalid CNPJ → "CNPJ inválido", no API call | `MPT:325` `getByText('CNPJ inválido')`, `MPT:326` `aria-invalid`, `MPT:327` `posts()` length 0 (13 digits, wrong check digit, all equal) | ✅ PASS |
| MCAD-07 | 409 → drawer stays with typed values, `StandardError` message | `MPT:335` alert `toHaveTextContent('Já existe uma empresa com o documento 12345678000195')`, `MPT:336-337` values kept | ✅ PASS |
| MCAD-08 | Other error or network failure → drawer stays, values kept, error message | `MPT:340-350` it.each 500 and `TypeError('Failed to fetch')`; `MPT:348` alert text; `MPT:349-350` values | ✅ PASS |
| MCAD-09 | Button disabled while sending; repeated clicks → one `POST` | `MPT:360` `toBeDisabled()`, `MPT:361` and `MPT:365` `posts()` length 1 after `dblClick` + click | ✅ PASS |
| MCAD-10 | Esc or Cancelar closes without API | `MPT:375` dialog gone, `MPT:376` `posts()` length 0 (both cases) | ✅ PASS |

### P1: CNPJ normalizado e validado no backend

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| MDOC-01 | Punctuation (`.`, `/`, `-`, spaces) removed; only 14 digits stored | `MIT:120` `jsonPath("$.document").value(digits)` after POST of formatted CNPJ; `MIT:133` same for `"  " + formatted + 20 spaces` (> 30 chars); `MST:152` `entityCaptor.getValue().getDocument()` `isEqualTo("12345678000195")`; spaces `CRT:14` | ✅ PASS |
| MDOC-02 | Not 14 digits / bad check digit → `400` `VALIDATION_ERROR` "CNPJ inválido" | `MIT:181-183` `status().isBadRequest()`, `$.error` `"VALIDATION_ERROR"`, `$.message` `"CNPJ inválido"` for `1234567800019`, `12345678000199`, `1234567800019599999999999999999` (`MIT:169-170`); determinism probe 60/60 (see above) | ✅ PASS (iteration 1 gap closed) |
| MDOC-03 | Non-digit, non-punctuation char → `400` `VALIDATION_ERROR` "CNPJ inválido" | `MIT:181-183` for `12A45678000195` and `"x".repeat(31)`; unit `CRT:36-37` (`12A…`, `12345678_000195`); probe inputs with letters and > 30 chars all "CNPJ inválido" | ✅ PASS (iteration 1 gap closed) |
| MDOC-04 | Same digits as any stored merchant (active, deactivated, legacy formatted) → `409` `CONFLICT` | `MIT:123-124` (digits after formatted), `MIT:145-146` (legacy row saved with punctuation), `MIT:164-165` (deactivated), each `status().isConflict()` + `$.error` `"CONFLICT"`; `MST:163` message with digits | ✅ PASS |
| MDOC-05 | 14 equal digits invalid | `MIT:169` `@ValueSource` includes `"11111111111111"`, `"00000000000000"` → `MIT:183` "CNPJ inválido"; `CRT:34-35`; frontend `CNT:56-57` | ✅ PASS |

### P1: Listar empresas

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| MLIST-01 | `GET /api/merchants?page=0&size=20&sort=name,asc`; name, formatted CNPJ, created date | `MPT:112` `search.toString()` `'page=0&size=20&sort=name%2Casc'`, `MPT:113` `sort` `'name,asc'`; `MPT:114` `'12.345.678/0001-95'`; `MPT:115` `'01/10/2026'`; order `MPT:118`; `AMT:24` URL | ✅ PASS |
| MLIST-02 | Selected merchant marked | `MPT:124` `aria-current='true'`, `MPT:125` "Selecionada", `MPT:126-127` other row not marked | ✅ PASS |
| MLIST-03 | Anterior/Próxima with > 1 page; disabled at the ends | `MPT:134-135` first page; `MPT:139` request `page` `'1'`; `MPT:140-141` last page; hidden with one page `MPT:147-148` | ✅ PASS |
| MLIST-04 | Click on name selects it in the top menu | `MPT:157` `menuButton()` `toHaveTextContent('Beta Comércio')`, `MPT:158` `aria-current` | ✅ PASS |
| MLIST-05 | Empty list → "Nenhuma empresa no seu acesso" + "Cadastrar empresa" | `MPT:164-165` heading + button, `MPT:166` no table | ✅ PASS |
| MLIST-06 | Loading state instead of table | `MPT:173` `status` "Carregando empresas", `MPT:174` no table, `MPT:178` gone after load | ✅ PASS |
| MLIST-07 | Fetch failure → error message + "Tentar de novo" refetches | API error: `MPT:186` alert `'Parâmetro de ordenação inválido'`, `MPT:190-193` retry → `listRequests()` length 2, alert gone. Network failure: `MPT:202` alert `'Não foi possível conectar à API. Tente novamente.'`, `MPT:203` no table, `MPT:206-208` retry → list row rendered, alert gone | ✅ PASS (iteration 1 minor gap closed) |
| MLIST-08 | Document without 14 digits shown as is | `MPT:215` `getByText('ABC-123')`; `CNT:41-42` | ✅ PASS |

### P2: Renomear empresa

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| MEDIT-01 | Drawer with current name editable, formatted CNPJ read-only | `MPT:401-402` value + not readonly; `MPT:404-405` `'12.345.678/0001-95'` + `readonly` | ✅ PASS |
| MEDIT-02 | `PUT /api/merchants/{id}` with `{ name: trimmed }` | `MPT:412` path `'/api/merchants/m-1'`, `MPT:413` body `toEqual({ name: 'Alfa Matriz' })`; `AMT:38` | ✅ PASS |
| MEDIT-03 | 200 → drawer closes; new name in list and top menu | `MPT:421` dialog gone, `MPT:422-423` list renamed, `MPT:424` `menuButton()` `'Alfa Matriz'`; hook `QMT:94-97` | ✅ PASS |
| MEDIT-04 | Empty / > 150 blocks submit, no API | `MPT:436` exact message (`MPT:428-429`: "Informe o nome da empresa" / "Use no máximo 150 caracteres"), `MPT:437` `puts()` length 0 | ✅ PASS |
| MEDIT-05 | Error or network failure → drawer stays with typed name, message | `MPT:440-449` 500 + `TypeError`, `MPT:447` alert text, `MPT:448` value `'Alfa Matriz'` | ✅ PASS |
| MEDIT-06 | Save button disabled while sending | `MPT:456` `toBeDisabled()`, `MPT:458` `puts()` length 1 | ✅ PASS |

### P2: Desativar empresa

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| MDES-01 | Confirmation with name and "CNPJ não poderá ser cadastrado de novo" | `MPT:494` `toHaveAccessibleName('Desativar Alfa Pagamentos?')`, `MPT:495` description matches `/O CNPJ 12\.345\.678\/0001-95 não poderá ser cadastrado de novo\./` | ✅ PASS |
| MDES-02 | Confirm → `DELETE /api/merchants/{id}` | `MPT:503` path `'/api/merchants/m-1'`, `MPT:504` body undefined; `AMT:44` | ✅ PASS |
| MDES-03 | 204 → dialog closes; removed from list and top menu | `MPT:511-512` dialog and row gone; `MPT:514` no menu option | ✅ PASS |
| MDES-04 | Selected deactivated → first remaining, or "sem empresa" | `MPT:522` `menuButton()` `'Beta Comércio'`, `MPT:523` `aria-current`; `MPT:530-531` empty state + menu "Nenhuma empresa"; hook `QMT:116` `merchantId` `'m-1'`, `QMT:131-132` `null` | ✅ PASS |
| MDES-05 | Cancel or Esc closes without API | `MPT:540-541` dialog gone + `deletes()` length 0 (both) | ✅ PASS |
| MDES-06 | Error or network failure → dialog stays, message | `MPT:552` alert text, `MPT:553` dialog still present (500 and `TypeError`) | ✅ PASS |

**Status**: ✅ All 35 ACs covered and matching the spec-defined outcome; 0 spec-precision gaps

**Payload/conjunction rule**: request bodies asserted by value (`MPT:276`, `MPT:314`, `MPT:413`, `AMT:24-44`); selection asserted on rendered/provider state (`MPT:286-290`, `MPT:522-523`, `QMT:78`, `QMT:116`), not on mock calls. Conjunctive outcomes ("na lista **e** no menu", "fechar **e** remover") asserted on both parts.

---

## Edge Cases

- [x] 401 in any action ends the session: global `frontend/lib/queries.ts:22-25` (`QueryCache`/`MutationCache` `onError`); `frontend/lib/queries.test.tsx:231` (query) and `:238` (mutation) `expect(onUnauthorized).toHaveBeenCalledTimes(1)`. Merchant hooks use the same client.
- [x] 403/404 on edit or deactivate → message + list reload: `MPT:472-474` (404 on edit) and `MPT:564-566` (403 on deactivate); shared `isGone` (`frontend/components/merchants/MerchantDrawer.tsx:17`).
- [x] Pasted formatted CNPJ accepted and reformatted: `MPT:265`, `MPT:268`.
- [x] Name with exactly 150 chars accepted: `MPT:313-314` body `{ name: 'a'.repeat(150), … }`.

---

## Discrimination Sensor

Scratch: `git worktree add --detach <scratchpad>/verifier-wt2 HEAD` (`frontend/node_modules` symlinked); each mutant reverted with `git checkout --` inside the scratch. Real-tree `git status --porcelain` baseline: ` M .specs/LESSONS.md`, ` M .specs/lessons.json`, `?? .specs/features/merchants-screen/validation.md`. After `git worktree remove --force` + `git worktree prune`: identical (`diff` empty) ✅.

Focused on the areas fixed since iteration 1:

| # | File:line | Mutation | Tests run | Killed? |
| --- | --- | --- | --- | --- |
| B1 | `backend/src/main/java/br/com/hanrry/reconpay/merchant/dto/MerchantRequestDTO.java:14` | Reintroduced `@Size(max = 30, message = "Documento deve ter no máximo 30 caracteres")` beside `@ValidCnpj` | `MerchantIntegrationTest,CnpjValidatorTest,CnpjRulesTest` | ✅ Killed (`MIT:132` `Status expected:<201> but was:<400>`). Note: in this run the two > 30-char invalid cases in `shouldRejectInvalidCnpj` happened to get "CNPJ inválido" and passed; the padded-valid case is the deterministic killer |
| B2 | same | Conflicting constraint with the spec message: `@Size(max = 30, message = "CNPJ inválido")` | same | ✅ Killed (`MIT:132` 201 expected, 400) |
| B3 | `backend/src/main/java/br/com/hanrry/reconpay/merchant/validation/ValidCnpj.java:22` | Default message `"CNPJ inválido"` → `"Documento inválido"` | same | ✅ Killed (7/7 `shouldRejectInvalidCnpj` cases, `MIT:183`) |
| F1 | `frontend/components/merchants/MerchantsPage.tsx:14` | Network `ApiError` (no status) gets the generic fallback instead of its message | `MerchantsPage.test.tsx` | ✅ Killed (MLIST-07 network case, `MPT:202`) |
| F2 | `frontend/components/merchants/MerchantDrawer.tsx:11` | `'Informe o nome da empresa'` → `'Informe o nome'` | same | ✅ Killed (MCAD-05 `MPT:297`, MEDIT-04) |
| F3 | `MerchantDrawer.tsx:12` | `Use no máximo 150 caracteres` → `Máximo de 150 caracteres` | same | ✅ Killed (MCAD-05 `MPT:308`, MEDIT-04) |
| F4 | `MerchantsPage.tsx:54` | "Tentar de novo" no longer calls `list.refetch()` | same | ✅ Killed (both MLIST-07 cases) |

**Sensor depth**: expanded (7 manual behavior-level mutations on the fixed areas; iteration 1 ran 19 on the rest, 18 killed + 1 equivalent survivor, and that code is unchanged since)
**Result**: 7/7 killed - PASS ✅

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ (fix is a one-line removal) |
| Surgical changes | ✅ (`91310bf`: DTO + its integration test; `f0c30e4`: one test; `df2066c`: one spec line) |
| No scope creep | ✅ |
| Matches patterns | ✅ |
| Spec-anchored outcome check | ✅ |
| Per-layer Coverage Expectation met | ✅ (`POST /api/merchants`: 201, 400 incl. > 30 chars, 409 legacy/deactivated) |
| Every test maps to a spec requirement | ✅ |
| Documented guidelines followed: `AGENTS.md`, `tasks.md` Test Coverage Matrix | ✅ |

Observation (not a gap, outside the spec): with `@Size` gone the document has no length bound before validation; a 200-digit input is rejected cheaply (probe) and anything stored is 14 digits, so the column limit is safe. Request-body size is bounded by the server defaults.

**Test integrity**: since iteration 1, backend +3 (two `@ValueSource` cases, one test), frontend +1; no test removed or weakened. The `UnderConstruction.test.tsx` row removal justified in iteration 1 still applies.

---

## Gate Check

- **Backend**: `cd backend && ./mvnw verify` → exit 0; surefire/failsafe reports: 532 tests, 0 failures, 0 errors, 0 skipped; JaCoCo check bound to `verify` passed (build exit 0)
- **Frontend**: `corepack pnpm test` → exit 0, 29 files / 322 tests passed; `corepack pnpm typecheck` → exit 0; `corepack pnpm build` → exit 0 (`/empresas` prerendered static)
- **Test count before feature**: backend 497, frontend 239
- **Test count after feature**: backend 532, frontend 322
- **Delta**: backend +35, frontend +83
- **Skipped tests**: none
- **Failures**: none
- **State gate**: `python3 -I <skill-dir>/scripts/validate_state.py merchants-screen --root <repo>` → see Summary

---

## Fix Plans

None.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| MCAD-01..10 | Implementing | ✅ Verified |
| MDOC-01..05 | Implementing (MDOC-02/03 Needs Fix in iteration 1) | ✅ Verified |
| MLIST-01..08 | Implementing | ✅ Verified |
| MEDIT-01..06 | Implementing | ✅ Verified |
| MDES-01..06 | Implementing | ✅ Verified |

(`spec.md` statuses left for the orchestrator to update; the Verifier only writes this report.)

---

## Summary

**Overall**: ✅ Ready (interactive UAT for the user-facing flow still up to the orchestrator/user)

**Spec-anchored check**: 35/35 ACs matched spec outcome; 0 gaps; 0 spec-precision gaps
**Sensor**: 7/7 mutations killed (iteration 2, fixed areas)
**Gate**: backend 532 passed, frontend 322 passed, 0 failed; typecheck and build green
**validate_state.py**: exit 0

**What works**: every invalid document, whatever its length, gets a single deterministic `400 VALIDATION_ERROR "CNPJ inválido"`; valid CNPJ with any amount of punctuation/whitespace is stored as 14 digits; list error state covers API and network failures with a working retry; name messages pinned to the spec texts.

**Issues found**: none.

**Next steps**: update `spec.md` traceability to Verified, offer UAT, commit the report.
