# Tela de Regras de Taxa Validation

**Date**: 2026-10-15
**Spec**: `.specs/features/fee-rules-screen/spec.md`
**Diff range**: `1b31a86..094330d` (spec/design `22eb692`, `6b4f5a3`; implementation `bd063cf..010c4bc`; `fb9bfef` pins the transaction message; iteration-1 fixes `65040f5`, `b944fe1`; iteration-2 fix `0d58242`; docs `0def19c`; iteration-3 fix `094330d`), branch `feat/fee-rules-screen`, 18 commits
**Verifier**: independent sub-agent (author ≠ verifier), iteration 4. This is one more than the normal bound of 3. The user authorized it after iteration 3 failed.

## Validation: fee-rules-screen - FAIL ❌

The iteration-3 gap is closed. `094330d` adds a boundary table (`FEE:31-50`, `FEE:58-66`) and a `100,00` accepted row (`FRT:421`). S6 is now killed with 9 failures, and S1, S4 and S5 are still killed. No production code changed after `010c4bc`.

I then designed 10 mutations of my own on `parseFeePercent`, `parseFixedFee`, `formatPercent` and `toInputDecimal`. Six survived (247/247 tests in `lib` + `components/fee-rules`). Four of them are spec-anchored gaps across three ACs:

1. **FEDIT-01 (V1, Major).** If `toInputDecimal` drops decimals 3 and 4, a stored `2.9999` rule opens in the edit drawer as `2,99`. Saving it, for example to change only the fixed fee, would silently send `PUT {"feePercentage":2.99,...}`, and the backend accepts that. The only prefill test uses `2.9900` and `0.30`, which already fit in 2 decimals.
2. **FLIST-03 (V2, Minor).** If `formatPercent` strips every `0` instead of only trailing zeros, `2.0500` shows as `2,5%`. Every percent tested (`2.9900`, `0.0000`, `100.0000`, `1.5000`) lacks an inner zero.
3. **FCAD-08 (V3 + V4, Minor).** The "100 with a non-zero decimal" check is pinned only for decimals whose non-zero digit is the **last** digit, and only for the digits `1` and `5` (`100,0001`, `100,1`, `100,01`, `100.5`). Two mutants survive. Anchoring the regex (`/[1-9]$/`) accepts `100,10` and `100,50`. Narrowing it (`/[1-5]/`) accepts `100,9` and `100,06`. The screen would then send them to the API instead of blocking them. The backend still answers `400` (`@DecimalMax("100.0000")`), so no bad data is stored, but FCAD-08 says the screen blocks "sem chamar a API".

The other two survivors are not counted as gaps. V5 (fixed fee not trimmed) affects only whitespace, which the spec does not define. V10 (`0.00` prefilled as `0`) keeps the same value. Production code is correct on HEAD for every probed input. All gaps are missing tests.

File abbreviations used below:

- `FRT` = `frontend/components/fee-rules/FeeRulesPage.test.tsx`
- `FIT` = `backend/src/test/java/br/com/hanrry/reconpay/feerule/integration/FeeRuleIntegrationTest.java`
- `FST` = `backend/src/test/java/br/com/hanrry/reconpay/feerule/service/FeeRuleServiceTest.java`
- `MMT` = `frontend/components/shell/MerchantMenu.test.tsx`
- `AFT` = `frontend/lib/api.feeRules.test.ts`
- `FEE` = `frontend/lib/fee.test.ts`
- `QFT` = `frontend/lib/queries.feeRules.test.tsx`

---

## Gap history

| Iter. | Gap | Finding | Fix commit | Evidence now | Status |
| --- | --- | --- | --- | --- | --- |
| 1 | FCAD-08 | No test for a 3-digit integer percent above 100; M3 (drop `digits.length === 3 && digits > '100'`) survived | `65040f5` | `FEE:22-23`, `FEE:44` (`'101'`, `'101,00'`, `'199'`, `'200'`, `'999,99'`); `FRT:342` `['101']`. S1 killed (9 failures) | ✅ Closed |
| 1 | FREG-03 | No evidence for BOLETO 1x → 201 | `b944fe1` | `FIT:167-184` `@CsvSource` 5 combinations → `isCreated()` | ✅ Closed |
| 2 | FCAD-08 | No test for a 4+-digit integer percent; S4 (`> 3` → `> 4`) survived | `0d58242` | `FEE:24-26`, `FEE:45` (`'1000'`, `'1000,00'`, `'10000'`, `'12345'`, `'99999999'`); `FRT:342` `['1000']`. S4 killed (4 failures) | ✅ Closed |
| 3 | FCAD-08 (+ edge case "é `100`") | No test that `100` with zero decimals (`100,00`) is accepted; S6 survived | `094330d` | `FEE:36-37` `100,0` … `100,0000`, `100.00`, `100.0000`, `00100,00`, `'  100,00  '` → exact value; `FRT:421` `'100,00'` → body `"feePercentage":100.00`. S6 killed (9 failures) | ✅ Closed |
| 4 | FEDIT-01 | Prefill not pinned for stored values with 3-4 significant decimals; V1 survived | - | none | ❌ Open |
| 4 | FLIST-03 | Trailing-zero stripping not pinned against an inner zero; V2 survived | - | none | ❌ Open |
| 4 | FCAD-08 | `100` + non-zero decimal pinned only with the digit last and only for the digits 1 and 5; V3 and V4 survived | - | none | ❌ Open |

**User decisions.**

- After iteration 3 (FAIL, the last iteration in the bound), the user first accepted the FCAD-08 gap as a known gap. Commit `0def19c` recorded that in the committed `validation.md`, in `STATE.md`, as FCAD-08 traceability "Implementing (lacuna aceita…)" and as tasks.md Status "Done (lacuna de teste aceita em FCAD-08…)".
- The user then chose to close the gap. `094330d` added the boundary table, and the user explicitly authorized this fourth verification.
- Iteration 4 is outside the normal loop. The new gaps go back to the user. The verifier does not start a fifth iteration.

All fix commits change tests only (`094330d`: 2 files, +32/-0). No assertion was removed or weakened.

**Docs state (reported, not edited).** `spec.md:197` still reads FCAD-08 "Implementing (lacuna aceita: `100,00` sem teste…)", and `tasks.md:12` reads "Done (lacuna de teste aceita em FCAD-08…)". Both are stale: that specific gap is now closed. The traceability for FCAD-08, FEDIT-01 and FLIST-03 should read "Needs Fix" (see the update table). Line 216 ("33 verified, 1 com lacuna aceita") is stale for the same reason.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 backend installments rule | ✅ Done | `FeeRuleService.java:130-135` `ensureInstallmentsAllowed`, used on create and update |
| T2 `SideDrawer` / `ConfirmDialog` shells | ✅ Done | - |
| T3 `lib/fee.ts` | ⚠️ Done, test gaps | V1 (`toInputDecimal`), V2 (`formatPercent`), V3/V4 (`parseFeePercent` 100 + fraction) |
| T4 API functions | ✅ Done | - |
| T5 hooks | ✅ Done | - |
| T6 list + menu item | ✅ Done | FLIST-03 formatting test gap via `formatPercent` (V2) |
| T7 create drawer | ✅ Done | - |
| T8 edit rates | ⚠️ Done, test gap | FEDIT-01 prefill precision (V1) |
| T9 deactivate | ✅ Done | - |
| T10 docs | ✅ Done | README + frontend/README |

---

## Spec-Anchored Acceptance Criteria

`094330d` inserts one line at `FRT:421`, so every `FRT` citation after line 421 moved by +1 from iteration 3. The ranges below have been updated. Re-read in this iteration: `FEE:1-96`, `fee.ts:1-45`, `FeeRuleDrawer.tsx:50-92`, `FRT:146-154`, `FRT:417-430`, `FRT:446-499`, `FRT:596-611`, `FIT:147-185` and `FeeRuleService.java:125-140`. No backend file changed since iteration 2.

### P1: Método sem parcelamento recusado na API

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FREG-01 | `POST` PIX/BOLETO/DEBIT_CARD with installments > 1 → `400 VALIDATION_ERROR` "Método de pagamento <MÉTODO> não permite parcelamento" | `FIT:147-165` `@ValueSource` PIX, BOLETO, DEBIT_CARD, `installments: 2`: `status().isBadRequest()`, `jsonPath("$.error").value("VALIDATION_ERROR")`, `jsonPath("$.message").value("Método de pagamento " + paymentMethod + " não permite parcelamento")`; `FST:243-259` `hasMessage(...)` + `verify(feeRuleRepository, never()).save(any())` | ✅ PASS |
| FREG-02 | `PUT` producing PIX/BOLETO/DEBIT_CARD with > 1 installment → same `400` + message, rule unchanged | `FIT:186-209` CREDIT_CARD 3x → `{"paymentMethod":"PIX",...}`: `isBadRequest()`, `VALIDATION_ERROR`, message `"Método de pagamento PIX não permite parcelamento"`, then `GET` `$.paymentMethod` `CREDIT_CARD`, `$.installments` `3`, `$.feePercentage` `2.99`; `FIT:211-229` PIX 1x → `{"installments":2}` 400 + message, then `GET` `$.installments` `1`; `FST:262-318` entity unchanged + `never().save` | ✅ PASS |
| FREG-03 | CREDIT_CARD with installments ≥ 1, or PIX/BOLETO/DEBIT_CARD with 1 → `201` | `FIT:167-184` `@CsvSource({"CREDIT_CARD, 1", "CREDIT_CARD, 12", "PIX, 1", "BOLETO, 1", "DEBIT_CARD, 1"})`: `status().isCreated()`, `jsonPath("$.paymentMethod").value(paymentMethod)`, `jsonPath("$.installments").value(installments)` | ✅ PASS |

### P1: Listar regras da empresa selecionada

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FLIST-01 | Menu footer item "Regras de taxa da <empresa>" closes the menu and opens `/taxas` | `MMT:86-96` link `{ name: 'Regras de taxa da Beta Comércio' }` `toHaveAttribute('href', '/taxas')`, footer order `toEqual([...])`, after click `queryByRole('listbox')).not.toBeInTheDocument()`; `MMT:98-104` no item without a merchant | ✅ PASS |
| FLIST-02 | `GET /api/merchants/{id}/fee-rules?page=0&size=100&sort=paymentMethod,asc&sort=installments,asc`; title "Regras de taxa" + merchant name | `FRT:133-144` path `toBe('/api/merchants/m-1/fee-rules')`, `search.toString()` `toBe('page=0&size=100&sort=paymentMethod%2Casc&sort=installments%2Casc')`, `getAll('sort')` `toEqual(['paymentMethod,asc','installments,asc'])`, h1 "Regras de taxa", `getByText('Alfa Pagamentos')`; `AFT:30-38` exact URL + `GET` + Bearer | ✅ PASS |
| FLIST-03 | Row shows Crédito/Débito/Pix/Boleto, `1x`/`3x`, percent with comma and **no trailing zeros** (`2,99%`, `0%`), fixed fee `R$ 0,30` | `FRT:146-154` `rowTexts()` `toEqual([['Crédito','3x','2,99%','R$ 0,30'],['Pix','1x','0%','R$ 0,00']])`; `FRT:156-179` Boleto/Débito rows; `FEE:76-83` `formatPercent('2.9900')` `'2,99%'`, `('0.0000')` `'0%'`, `('100.0000')` `'100%'`, `('1.5000')` `'1,5%'`. **No input has an inner zero**: V2 (`replace(/0+$/, '')` → `replace(/0/g, '')`, so `2.0500` → `2,5%`) survived, 247/247 | ❌ GAP (gap 2, Minor) |
| FLIST-04 | Merchant switch fetches and shows the new merchant's rules, never the previous ones | `FRT:156-179` while pending no table and no `'Crédito'`; then Beta's rows; last path `/api/merchants/m-2/fee-rules`; `QFT:40-49` `data` `toBeUndefined()` after rerender | ✅ PASS |
| FLIST-05 | Empty: "Nenhuma regra de taxa", exact helper text, "Cadastrar regra" | `FRT:181-189` heading, `getByText('Transações só entram com uma regra ativa para o método e as parcelas.')`, button, no table; `FRT:270-276` button opens the drawer | ✅ PASS |
| FLIST-06 | No merchant: "Nenhuma empresa no seu acesso" + "Cadastrar empresa" | `FRT:191-198` heading, link `href` `/empresas?cadastrar=1`, `listRequests()).toHaveLength(0)` | ✅ PASS |
| FLIST-07 | Loading state "Carregando regras de taxa" instead of the table | `FRT:200-214` `getByRole('status', { name: 'Carregando regras de taxa' })`, no table, then table replaces it | ✅ PASS |
| FLIST-08 | Error message + "Tentar de novo" re-fetches | `FRT:216-232` alert `toHaveTextContent('Parâmetro de ordenação inválido')`, click "Tentar de novo", table shown, `listRequests()).toHaveLength(2)` | ✅ PASS |

### P1: Cadastrar regra

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FCAD-01 | Drawer "Cadastrar regra de taxa" with Método, Parcelas, Percentual, Taxa fixa; no method; Parcelas `1` | `FRT:258-268` dialog name, method `toHaveValue('')`, options `['Escolha o método','Crédito','Débito','Pix','Boleto']`, installments `toHaveValue('1')`, rates `''` | ✅ PASS |
| FCAD-02 | Pix/Boleto/Débito: Parcelas `1`, disabled | `FRT:278-285` `it.each` Pix/Boleto/Débito: `toHaveValue('1')`, `toBeDisabled()`; `FEE:92-95` set equality | ✅ PASS |
| FCAD-03 | Crédito enables Parcelas, any integer ≥ 1 | `FRT:287-298` Pix→Crédito `toBeEnabled()`, sends `installments: 12` | ✅ PASS |
| FCAD-04 | `POST` with `paymentMethod`, integer `installments`, decimals as exact JSON numbers (comma → dot) | `FRT:300-308` body `toBe('{"paymentMethod":"CREDIT_CARD","installments":3,"feePercentage":2.99,"fixedFee":0.30}')`; `FRT:417-430` literals `2.9999`, `100`, `100.00`, `0`; `AFT:40-45`; `FEE:5-8`, `FEE:34-37` (exact text values), `FEE:53-62` | ✅ PASS |
| FCAD-05 | `201` closes the drawer and shows the new rule | `FRT:310-320` dialog gone, rows include `['Crédito','3x','2,99%','R$ 0,30']`; `QFT:53-65` | ✅ PASS |
| FCAD-06 | No method: block, no API call, "Escolha o método de pagamento" | `FRT:322-330` exact text, `aria-invalid` `true`, `createRequests()).toHaveLength(0)` | ✅ PASS |
| FCAD-07 | Installments not an integer ≥ 1: block, exact message | `FRT:332-340` `'0'`, `'1,5'`, `'abc'`, `''`: `getByText('Informe um número inteiro de parcelas a partir de 1')`, `aria-invalid`, 0 POSTs | ✅ PASS |
| FCAD-08 | Percent empty / NaN / < 0 / **> 100** / > 4 decimals: block with "Informe um percentual de 0 a 100 com até 4 casas decimais" | Acceptance at the inclusive bound is now pinned: `FEE:33-40` (`100`, `100,0` … `100,0000`, `100.00`, `00100,00`, …), `FRT:421`. Rejection: `FRT:342-350` exact message, `aria-invalid`, 0 POSTs for `''`, `'abc'`, `'-1'`, `'100,01'`, `'101'`, `'1000'`, `'2,99999'`; `FEE:17-28`, `FEE:42-49` 28 rejected forms. S1, S4, S5, S6, V6, V9 killed. **Not pinned**: `100` + a fraction whose non-zero digit is not last (`100,10`, `100,50`) or is not `1`/`5` (`100,9`, `100,06`). V3 (`/[1-9]/` → `/[1-9]$/`) and V4 (`/[1-9]/` → `/[1-5]/`) survived, 247/247 | ❌ GAP (gap 3, Minor; backend `@DecimalMax` still rejects with 400) |
| FCAD-09 | Fixed fee empty / NaN / negative / > 2 decimals: block with "Informe uma taxa fixa a partir de 0 com até 2 casas decimais" | `FRT:352-360` `''`, `'x'`, `'-0,01'`, `'0,301'`: exact text, `aria-invalid`, 0 POSTs; `FEE:58-73` accepted forms with exact values (`00,30` → `0.30`, …), rejected `-0`, `-1`, `0,001`, `1,`, `,3`, `1.2.3`, `1e2`, `' '`. V7 killed | ✅ PASS |
| FCAD-10 | `409` / other error / network failure: drawer stays open with typed values, error shown | `FRT:362-372` 409 alert `'Regra de taxa já existe para este comerciante, método de pagamento e parcelas'`, values kept; `FRT:374-388` 500 `'Erro interno ao cadastrar'` and network `'Não foi possível conectar à API. Tente novamente.'` | ✅ PASS |
| FCAD-11 | "Cadastrar" disabled while sending; repeated clicks → one `POST` | `FRT:390-405` `toBeDisabled()`, `createRequests()).toHaveLength(1)` | ✅ PASS |
| FCAD-12 | `Esc` or "Cancelar" closes without an API call | `FRT:407-415` dialog gone, 0 POSTs; `SideDrawer.test.tsx:29-41` | ✅ PASS |

### P2: Alterar taxas de uma regra

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FEDIT-01 | Drawer "Editar regra de taxa", Método and Parcelas read-only, rates **filled with the current values** (comma) | `FRT:468-479` Método `'Crédito'` + `readonly`, Parcelas `'3'` + `readonly`, Percentual `'2,99'` (stored `2.9900`), Taxa fixa `'0,30'`; `FEE:85-90` `toInputDecimal('2.9900')` `'2,99'`, `('0.30')` `'0,30'`. **No stored value has a 3rd or 4th significant decimal.** V1 (`toInputDecimal` keeps only 2 decimals) survived, 247/247. Scratch probe: HEAD `toInputDecimal('2.9999')` = `'2,9999'`, under V1 `'2,99'`. `FeeRuleDrawer.tsx:57` prefill + `:75` re-parse means a save would send `2.99` | ❌ GAP (gap 1, Major: silent rate change on edit) |
| FEDIT-02 | `PUT /api/merchants/{id}/fee-rules/{ruleId}` only `feePercentage` and `fixedFee`, exact JSON numbers | `FRT:481-488` path `/api/merchants/m-1/fee-rules/r-2`, body `toBe('{"feePercentage":3.49,"fixedFee":0.35}')`; `AFT:47-50` `PUT` literal body | ✅ PASS |
| FEDIT-03 | `200` closes the drawer and shows the new values | `FRT:490-499` dialog gone, rows `[['Crédito','3x','3,49%','R$ 0,35'],['Pix','1x','0%','R$ 0,00']]`; `QFT:67-79` | ✅ PASS |
| FEDIT-04 | Invalid rates: block, same messages as FCAD-08/09 | `FRT:501-513` `100,01`, empty, `0,301`, `-1`: exact texts, `aria-invalid`, `puts()).toHaveLength(0)`. Same `parseFeePercent`, so the V3/V4 weakness is counted once under FCAD-08 | ✅ PASS |
| FEDIT-05 | Error / network failure: drawer open with typed values, message shown | `FRT:515-526` `'Erro interno ao alterar'` and network message; `3,49` / `0,35` kept | ✅ PASS |
| FEDIT-06 | "Salvar" disabled while sending | `FRT:528-540` `toBeDisabled()`, `puts()).toHaveLength(1)` | ✅ PASS |

### P2: Desativar regra

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FDES-01 | Confirmation "Desativar a regra <Método> <N>x?" with the refusal warning | `FRT:585-594` `toHaveAccessibleName('Desativar a regra Crédito 3x?')` / `'Desativar a regra Pix 1x?'`, `toHaveAccessibleDescription('Novas transações dessa combinação serão recusadas até existir outra regra.')`, 0 DELETEs | ✅ PASS |
| FDES-02 | Confirm → `DELETE /api/merchants/{id}/fee-rules/{id}` | `FRT:596-603` path `/api/merchants/m-1/fee-rules/r-2`, body `toBeUndefined()`; `AFT:53-56` `DELETE` | ✅ PASS |
| FDES-03 | `204` closes the dialog and removes the rule | `FRT:605-611` alertdialog gone, rows `[['Pix','1x','0%','R$ 0,00']]`; `QFT:81-92` | ✅ PASS |
| FDES-04 | Cancel or `Esc` closes without an API call | `FRT:613-626` dialog gone, 0 DELETEs, rows unchanged; `ConfirmDialog.test.tsx:23-34` | ✅ PASS |
| FDES-05 | Error / network failure: dialog open, message shown | `FRT:628-638` `'Erro interno ao desativar'` / network message, dialog still named | ✅ PASS |

**Status**: ❌ 31/34 ACs fully matched; 3 gaps (FEDIT-01, FLIST-03, FCAD-08); 0 spec-precision gaps.

---

## Edge Cases

- [x] `401` on any action → session ends: `frontend/lib/queries.test.tsx:227-240`; fee-rule hooks share `createQueryClient` (`FRT:113`)
- [x] `404` on edit → message + list reload: `FRT:542-554`
- [x] `404` on deactivate → message + list reload: `FRT:640-652`
- [x] Merchant switch with drawer/dialog open → closed, no API call: `FRT:432-443`, `FRT:556-569`, `FRT:654-661`
- [x] `0` and `0` accepted: `FRT:418` body `"feePercentage":0,"fixedFee":0`
- [x] Percent with 4 decimals (`2,9999`) or `100` accepted: `FRT:419-421` (`2,9999`, `100`, `100,00`), `FEE:33-40`. Closed in this iteration

---

## Discrimination Sensor

Scratch: `git worktree add --detach .../scratchpad/sensor HEAD` (at `094330d`), with `frontend/node_modules` symlinked from the real tree. Each mutation was a single exact-match replace in `frontend/lib/fee.ts` (asserted `count == 1`), tested with `vitest run lib components/fee-rules` (247 tests; baseline 247/247 on the unmutated scratch), then reverted with `git checkout -- frontend/lib/fee.ts`. Survivor impact was confirmed by a scratch-only probe test (deleted afterwards). It passes on HEAD and fails under V1, V2, V3, V4, V5 and V10. The worktree was removed with `git worktree remove --force` and pruned, and `git worktree list` no longer shows it.

| # | File:line | Mutation | Killed? |
| --- | --- | --- | --- |
| S6 | `fee.ts:23` | Iteration-3 survivor: `/[1-9]/.test(fraction ?? '')` → `fraction !== undefined` | ✅ Killed (9 failed: `FEE` `100,0` … `100.0000`, `00100,00`, `'  100,00  '`; `FRT` `aceita percentual 100 com casas zeradas`) |
| S1 | `fee.ts:23` | Iteration-1 survivor M3: removed `(digits.length === 3 && digits > '100') \|\|` | ✅ Killed (9 failed: `FEE` `101`, `101,00`, `199`, `200`, `999,99`, `0101`, `00101`, old list; `FRT` `Percentual "101"`) |
| S4 | `fee.ts:23` | Iteration-2 survivor: `digits.length > 3` → `> 4` | ✅ Killed (4 failed: `FEE` `1000`, `1000,00`, old list; `FRT` `Percentual "1000"`) |
| S5 | `fee.ts:23` | `/[1-9]/` → `/[2-9]/` (accepts `100,01`) | ✅ Killed (6 failed, including `FRT` FCAD-08 and FEDIT-04 `"100,01"`) |
| V1 | `fee.ts:44` | `toInputDecimal` keeps only the first 2 decimals (`${fraction.slice(0, 2)}`): `2.9999` → `2,99` | ❌ **Survived** (247/247) → Fix 1 |
| V2 | `fee.ts:36` | `formatPercent` strips every zero (`replace(/0/g, '')`): `2.0500` → `2,5%` | ❌ **Survived** (247/247) → Fix 2 |
| V3 | `fee.ts:23` | `/[1-9]/` → `/[1-9]$/` (accepts `100,10`, `100,50`) | ❌ **Survived** (247/247) → Fix 3 |
| V4 | `fee.ts:23` | `/[1-9]/` → `/[1-5]/` (accepts `100,9`, `100,06`) | ❌ **Survived** (247/247) → Fix 3 |
| V5 | `fee.ts:29` | `parseFixedFee` without `trim()` (`' 0,30 '` rejected) | ⚠️ Survived, not a gap: the spec does not define whitespace handling. Inconsistent with the percent, whose trim is pinned (`FEE:37`). Optional |
| V6 | `fee.ts:23` | `digits > '100'` → `>= '100'` (rejects 100) | ✅ Killed (13 failed) |
| V7 | `fee.ts:30` | `parseFixedFee` returns the trimmed text with comma → dot, no leading-zero normalization | ✅ Killed (1 failed: `FEE` `aceita "00,30" como 0.30`) |
| V8 | `fee.ts:44` | `toInputDecimal` strips every trailing zero (`0.30` → `0,3`) | ✅ Killed (2 failed: `FEE:87-88`, `FRT` FEDIT-01) |
| V9 | `fee.ts:22` | Bound check reads the raw `whole` instead of the normalized digits | ✅ Killed (3 failed: `0100`, `00100,00`) |
| V10 | `fee.ts:43` | `toInputDecimal` returns only the whole part when the fraction is all zeros (`0.00` → `0`) | ⚠️ Survived, equivalent in value: `0` and `0,00` are the same number and are re-sent unchanged. Not a gap |

Real-tree `git status --porcelain` before the sensor and after cleanup (identical, `diff` empty):

```
?? frontend/AGENTS.md
```

**Sensor depth**: expanded (14 mutations over the four `lib/fee.ts` functions touched by the fix loop and used by FLIST-03, FCAD-04/08/09 and FEDIT-01). The iteration-1 16-mutation run and S2/S3 in iteration 2 cover the backend and the components.
**Result**: 8/14 killed, 6 survived (4 spec-anchored gaps, 2 judged non-gap) - FAIL ❌

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ fix commits are test-only |
| Surgical changes | ✅ `094330d`: 2 test files, +32/-0 |
| No scope creep | ✅ |
| Matches patterns | ✅ `it.each` tables, exact `toBe` values |
| Spec-anchored outcome check | ⚠️ gaps 1-3 (FEDIT-01 precision, FLIST-03 inner zero, FCAD-08 100 + fraction) |
| Per-layer Coverage Expectation | ✅ service unit + integration; lib, hooks and components 1:1 with the ACs |
| Every test maps to a requirement | ✅ new tables are tagged FCAD-08 / FCAD-09 |
| Documented guidelines followed | ✅ `AGENTS.md` (JUnit 5/MockMvc/Testcontainers, `AbstractIntegrationTest`, JaCoCo gate) |

---

## Gate Check

- **Backend**: `cd backend && ./mvnw verify` → exit 0. **547 tests, 0 failures, 0 errors, 0 skipped** (summed from `target/surefire-reports` and `target/failsafe-reports` XML). The JaCoCo 85%/75% check passed inside `verify`.
- **Frontend**: `corepack pnpm test` → exit 0, **481 passed (481)**, 35 files; `corepack pnpm typecheck` → exit 0; `corepack pnpm build` → exit 0 (`/taxas` prerendered).
- **Test count**: backend 547 (unchanged), frontend 419 → 481 (+62: 46 percent-table rows, 15 fixed-fee rows, 1 `FRT` row). Nothing deleted or skipped.

---

## Fix Plans

All fixes are test-only. HEAD behavior is already correct for each input.

### Fix 1 (Major): pin full-precision prefill in the edit drawer (FEDIT-01, V1)

- **Root cause**: `toInputDecimal` keeps 2 decimals and then appends decimals 3-4 without trailing zeros. Tests only use `2.9900` and `0.30`, so dropping decimals 3-4 goes unnoticed. A truncated prefill is re-sent on save (`FeeRuleDrawer.tsx:57`, `:75`).
- **Fix task**: in `FEE:85-90` add `expect(toInputDecimal('2.9999')).toBe('2,9999')` and `expect(toInputDecimal('1.2340')).toBe('1,234')`. Add an `FRT` edit test with a stored `2.9999` rule that asserts `Percentual` `toHaveValue('2,9999')`, or that saving with only the fixed fee changed sends `"feePercentage":2.9999`.
- **Verify**: V1 killed in scratch. V8 stays killed.

### Fix 2 (Minor): pin trailing-zero stripping against an inner zero (FLIST-03, V2)

- **Fix task**: in `FEE:76-83` add `expect(formatPercent('2.0500')).toBe('2,05%')` and `expect(formatPercent('10.0000')).toBe('10%')`. Optionally give one `FRT` fixture a `2.0500` rule.
- **Verify**: V2 killed.

### Fix 3 (Minor): pin `100` + fraction for every non-zero digit and position (FCAD-08, V3/V4)

- **Fix task**: add to the rejected table at `FEE:42-46`: `'100,10'`, `'100,50'`, `'100,9'`, `'100,06'`, `'100,0090'`, or every `100,d` for d = 1..9 plus `100,d0`.
- **Verify**: V3 and V4 killed. S5 and S6 stay killed.

### Optional (not counted): V5

Add `expect(parseFixedFee(' 0,30 ')).toBe('0.30')` to match the pinned percent trim.

---

## Requirement Traceability Update

The Verifier does not edit `spec.md` or `tasks.md`. Recommended statuses:

| Requirement | Current text in spec.md | Recommended |
| --- | --- | --- |
| FREG-01..03 | Verified | ✅ Verified |
| FLIST-01, 02, 04..08 | Verified | ✅ Verified |
| FLIST-03 | Verified | ❌ Needs Fix (V2) |
| FCAD-01..07, 09..12 | Verified | ✅ Verified |
| FCAD-08 | Implementing (lacuna aceita: `100,00` sem teste) | ❌ Needs Fix (`100,00` closed; V3/V4 open) |
| FEDIT-01 | Verified | ❌ Needs Fix (V1) |
| FEDIT-02..06 | Verified | ✅ Verified |
| FDES-01..05 | Verified | ✅ Verified |

`tasks.md:12` "Done (lacuna de teste aceita em FCAD-08…)" is stale. The gap it names is closed, and three test gaps are open.

---

## Summary

**Overall**: ❌ Not ready. Three test gaps remain, and behavior is correct. This is iteration 4, already past the normal bound of 3, so the gaps **go back to the user**.

**Spec-anchored check**: 31/34 ACs fully matched | 3 gaps | 0 spec-precision gaps
**Sensor**: 14 injected, 8 killed, 6 survived (4 counted as gaps)
**Gate**: backend 547 passed, frontend 481 passed, 0 failed

**What works**: all four earlier gaps are closed: iteration 1 FCAD-08 (3-digit) and FREG-03, iteration 2 FCAD-08 (4+ digits) and iteration 3 FCAD-08 (`100,00`). S1, S4, S5 and S6 are killed. Every other AC holds.

**Issues found**: gap 1 FEDIT-01 prefill precision (Major), gap 2 FLIST-03 inner zero (Minor), gap 3 FCAD-08 `100` + fraction digits (Minor).

**validate_state.py**: `python3 -I <skill-dir>/scripts/validate_state.py fee-rules-screen --root <repo>` → exit 1 ("validation.md verdict is FAIL"), as expected for a FAIL report.

**Next steps**: the user decides whether to apply Fixes 1-3 (about 10 test lines, no production change) and re-verify, or to accept the gaps knowingly. Fix 1 is the one with real-use impact: editing any rule stored with 3-4 decimals.
