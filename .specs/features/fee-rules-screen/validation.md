# Tela de Regras de Taxa Validation

**Date**: 2026-10-15
**Spec**: `.specs/features/fee-rules-screen/spec.md`
**Diff range**: `1b31a86..0d58242` (spec/design `22eb692`, `6b4f5a3`; implementation `bd063cf..010c4bc`; `fb9bfef` pins the transaction message; iteration-1 fixes `65040f5`, `b944fe1`; iteration-2 fix `0d58242`), branch `feat/fee-rules-screen`, 16 commits, 28 files
**Verifier**: independent sub-agent (author ≠ verifier), iteration 3 of 3 (the last before escalation)

## Validation: fee-rules-screen - FAIL ❌

> **Decisão do usuário (2026-10-15, após a iteração 3):** aceitar a feature com a lacuna de FCAD-08 como pendência conhecida, sem nova correção nem quarta verificação. O veredito do Verifier continua FAIL; `validate_state.py` seguirá apontando esta feature até a lacuna ser fechada. Pendência: testar que `100,00` e `100.0000` são aceitos (mutante S6) e re-verificar.

The iteration-2 gap is closed: `0d58242` pins integers with 4 or more digits (`1000`, `12345`), and S4 and S1 are both killed. I designed six more mutations on `parseFeePercent` / `parseFixedFee`, and one of them survived (S6). The check that rejects `100` followed by decimals was changed to reject `100` with **any** decimals (`/[1-9]/.test(fraction ?? '')` → `fraction !== undefined`), and all 185 tests still passed. Under that mutant, `100,00` and `100,0000` are blocked. Both values equal 100, so FCAD-08 and the "é `100`" edge case say they must be accepted.

This affects the real flow: the edit drawer prefills a 100% rule as `toInputDecimal('100.0000')` = `'100,00'` (`FeeRuleDrawer.tsx:57`, confirmed by a scratch probe). With the mutant applied, saving that rule (for example to change only the fixed fee) would be blocked with the percent message, and no test would notice. Production code is correct today; the gap is a missing test. This was the last allowed iteration, so the gap **escalates to the user**.

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
| 1 | FCAD-08 | 3-digit integer percent above 100 not tested; M3 (drop `digits.length === 3 && digits > '100'`) survived | `65040f5` | `FEE:22-23` `parseFeePercent('101')` / `('999')` `toBeNull()`; `FRT:342` `['101']`. M3 re-run as S1: killed (iterations 2 and 3) | ✅ Closed |
| 1 | FREG-03 | BOLETO 1x → 201 had no evidence | `b944fe1` | `FIT:168` `@CsvSource` with `CREDIT_CARD 1`, `CREDIT_CARD 12`, `PIX 1`, `BOLETO 1`, `DEBIT_CARD 1` → `isCreated()`; S2 killed in iteration 2 | ✅ Closed |
| 2 | FCAD-08 | 4+-digit integer percent not tested; S4 (`digits.length > 3` → `> 4`) survived | `0d58242` | `FEE:24-26` `parseFeePercent('1000')`, `('12345')`, `('0101')` `toBeNull()`; `FEE:14` `parseFeePercent('0100')).toBe('100')`; `FRT:342` gains `['1000']`. S4 killed in iteration 3 (2 failures) | ✅ Closed |
| 3 | FCAD-08 (+ edge case "é `100`", FEDIT-04 via prefill) | `100` with zero decimals (`100,00`) not tested as accepted; S6 survived | - | none | ❌ Open, escalated |

All fix commits add test inputs only (`0d58242`: 2 files, +5/-1). No assertion was removed or weakened, and no production code changed after `010c4bc`.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 backend installments rule | ✅ Done | `FeeRuleService.java:52` (create), `:79` (update, before any setter), `:130-135` |
| T2 `SideDrawer` / `ConfirmDialog` shells | ✅ Done | `MerchantsPage.test.tsx` unchanged in the range, passing |
| T3 `lib/fee.ts` | ⚠️ Done, test gap | `100` with zero decimals not pinned as accepted (gap 1) |
| T4 API functions | ✅ Done | - |
| T5 hooks | ✅ Done | - |
| T6 list + menu item | ✅ Done | - |
| T7 create drawer | ✅ Done | - |
| T8 edit rates | ✅ Done | - |
| T9 deactivate | ✅ Done | - |
| T10 docs | ✅ Done | README + frontend/README |

---

## Spec-Anchored Acceptance Criteria

`0d58242` changes `FRT:342` in place, so `FRT` line numbers are the same as in iteration 2. `FEE` lines after 13 moved by +1 (and by +4 after 23); they are updated below. Re-read in this iteration: `FRT:139-143`, `FRT:150-153`, `FRT:342-350`, `FRT:417-429`, `FRT:470-477`, `FRT:485-486`, `FRT:588-593`, `FIT:147-184`, `FEE:1-65`, `fee.ts:1-45`, `FeeRuleDrawer.tsx:57-91`. Backend files have not changed since iteration 2.

### P1: Método sem parcelamento recusado na API

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FREG-01 | `POST` PIX/BOLETO/DEBIT_CARD with installments > 1 → `400 VALIDATION_ERROR` "Método de pagamento <MÉTODO> não permite parcelamento" | `FIT:147-165` (`@ValueSource` PIX, BOLETO, DEBIT_CARD, installments 2): `status().isBadRequest()`, `jsonPath("$.error").value("VALIDATION_ERROR")`, `jsonPath("$.message").value("Método de pagamento " + paymentMethod + " não permite parcelamento")`; `FST:243-259` `hasMessage(...)` + `verify(feeRuleRepository, never()).save(any())` | ✅ PASS |
| FREG-02 | `PUT` producing PIX/BOLETO/DEBIT_CARD with > 1 installment → same `400` + message, rule unchanged | `FIT:186-209` CREDIT_CARD 3x → `{"paymentMethod":"PIX","feePercentage":1.5000}`: `isBadRequest()`, `VALIDATION_ERROR`, message `"Método de pagamento PIX não permite parcelamento"`, then `GET` asserts `$.paymentMethod` `CREDIT_CARD`, `$.installments` `3`, `$.feePercentage` `2.99`; `FIT:211-229` PIX 1x → `{"installments":2}`: 400 + message, then `GET` `$.installments` `1`; `FST:262-290`, `FST:292-318` entity unchanged + `never().save` | ✅ PASS |
| FREG-03 | CREDIT_CARD with any installments ≥ 1, or PIX/BOLETO/DEBIT_CARD with 1 → `201` | `FIT:167-184` `@CsvSource({"CREDIT_CARD, 1", "CREDIT_CARD, 12", "PIX, 1", "BOLETO, 1", "DEBIT_CARD, 1"})`: `status().isCreated()`, `jsonPath("$.paymentMethod").value(paymentMethod)`, `jsonPath("$.installments").value(installments)` | ✅ PASS |

### P1: Listar regras da empresa selecionada

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FLIST-01 | Menu footer item "Regras de taxa da <empresa>" closes the menu and opens `/taxas` | `MMT:86-96` link `{ name: 'Regras de taxa da Beta Comércio' }` `toHaveAttribute('href', '/taxas')`, footer order `toEqual(['Gerenciar empresas', 'Regras de taxa da Beta Comércio', 'Cadastrar empresa'])`, after click `queryByRole('listbox')).not.toBeInTheDocument()`; `MMT:98-104` no item without a selected merchant | ✅ PASS |
| FLIST-02 | `GET /api/merchants/{id}/fee-rules?page=0&size=100&sort=paymentMethod,asc&sort=installments,asc`; title "Regras de taxa" + merchant name | `FRT:139-143` path `toBe('/api/merchants/m-1/fee-rules')`, `search.toString()` `toBe('page=0&size=100&sort=paymentMethod%2Casc&sort=installments%2Casc')`, `getAll('sort')` `toEqual(['paymentMethod,asc', 'installments,asc'])`, heading level 1 "Regras de taxa", header `getByText('Alfa Pagamentos')`; `AFT:30-38` exact URL + `GET` + `Bearer jwt` | ✅ PASS |
| FLIST-03 | Row shows Crédito/Débito/Pix/Boleto, `1x`/`3x`, `2,99%`, `R$ 0,30` | `FRT:150-153` `rowTexts()` `toEqual([['Crédito','3x','2,99%','R$ 0,30'],['Pix','1x','0%','R$ 0,00']])`; `FRT:172-175` Boleto/Débito rows; `FEE:45-51` `formatPercent` | ✅ PASS |
| FLIST-04 | Merchant switch fetches and shows the new merchant's rules, never the previous ones | `FRT:156-179` while pending, `queryByRole('table')` absent and `queryByText('Crédito')` absent; then Beta's rows; last request path `/api/merchants/m-2/fee-rules`; `QFT:40-49` `result.current.data` `toBeUndefined()` after rerender | ✅ PASS |
| FLIST-05 | Empty: "Nenhuma regra de taxa", "Transações só entram com uma regra ativa para o método e as parcelas.", button "Cadastrar regra" | `FRT:185-188` heading, exact text, button, no table; `FRT:270-276` button opens the drawer | ✅ PASS |
| FLIST-06 | No merchant: "Nenhuma empresa no seu acesso" + "Cadastrar empresa" | `FRT:195-197` heading, link `href` `/empresas?cadastrar=1`, `listRequests()).toHaveLength(0)` | ✅ PASS |
| FLIST-07 | Loading state "Carregando regras de taxa" instead of the table | `FRT:208-213` `getByRole('status', { name: 'Carregando regras de taxa' })`, no table, then table replaces it | ✅ PASS |
| FLIST-08 | Error message + "Tentar de novo" re-fetches | `FRT:223-231` alert `toHaveTextContent('Parâmetro de ordenação inválido')`, "Tentar de novo", table shown, `listRequests()).toHaveLength(2)`, alert gone | ✅ PASS |

### P1: Cadastrar regra

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FCAD-01 | Drawer "Cadastrar regra de taxa" with Método, Parcelas, Percentual, Taxa fixa; no method; Parcelas `1` | `FRT:261-267` dialog name, method `toHaveValue('')`, options `['Escolha o método','Crédito','Débito','Pix','Boleto']`, installments `toHaveValue('1')`, rates empty | ✅ PASS |
| FCAD-02 | Pix/Boleto/Débito: Parcelas kept at `1`, disabled | `FRT:278-285` `it.each` Pix/Boleto/Débito: `toHaveValue('1')`, `toBeDisabled()`; `FEE:61-64` set equality | ✅ PASS |
| FCAD-03 | Crédito enables Parcelas, any integer ≥ 1 | `FRT:287-298` Pix→Crédito `toBeEnabled()`, sends `installments: 12` | ✅ PASS |
| FCAD-04 | `POST` with `paymentMethod`, integer `installments`, decimals as exact JSON numbers (comma → dot) | `FRT:306-307` path `/api/merchants/m-1/fee-rules`, body `toBe('{"paymentMethod":"CREDIT_CARD","installments":3,"feePercentage":2.99,"fixedFee":0.30}')`; `FRT:427` 2.9999/100/0 literals; `AFT:40-45` literal body + `POST`; `FEE:6-7`, `FEE:33-34`. S10 (fixed fee trailing zeros dropped) killed | ✅ PASS |
| FCAD-05 | `201` closes the drawer and shows the new rule | `FRT:315-319` dialog gone, rows include `['Crédito','3x','2,99%','R$ 0,30']`; `QFT:53-65` | ✅ PASS |
| FCAD-06 | No method: block, no API call, "Escolha o método de pagamento" | `FRT:327-329` exact text, `aria-invalid` `true`, `createRequests()).toHaveLength(0)` | ✅ PASS |
| FCAD-07 | Installments not an integer ≥ 1: block, "Informe um número inteiro de parcelas a partir de 1" | `FRT:332-340` `'0'`, `'1,5'`, `'abc'`, `''`: exact text, `aria-invalid`, 0 POSTs | ✅ PASS |
| FCAD-08 | Percent empty / not a number / < 0 / **> 100** / > 4 decimals: block with "Informe um percentual de 0 a 100 com até 4 casas decimais" (so 0..100, including `100` written with zero decimals, is accepted) | Rejection side: `FRT:342-350` `it.each` `''`, `'abc'`, `'-1'`, `'100,01'`, `'101'`, `'1000'`, `'2,99999'`: `getByText('Informe um percentual de 0 a 100 com até 4 casas decimais')`, `aria-invalid` `true`, `createRequests()).toHaveLength(0)`; `FEE:17-28` (`'101'`, `'999'`, `'1000'`, `'12345'`, `'0101'`, `'100,01'` → `toBeNull()`). S1, S4, S5, S8, S9 killed. Acceptance side: only bare `'100'` is tested (`FEE:12`, `FRT:420`). **`100` with zero decimals (`100,00`, `100,0000`) is not**, and S6 (reject `100` with any fraction) survived, 185/185 | ❌ GAP (gap 1, minor; behavior correct, test not discriminating at the `100` boundary) |
| FCAD-09 | Fixed fee empty / not a number / negative / > 2 decimals: block with "Informe uma taxa fixa a partir de 0 com até 2 casas decimais" | `FRT:352-360` `''`, `'x'`, `'-0,01'`, `'0,301'`: exact text, `aria-invalid`, 0 POSTs; `FEE:37-42`. S7 (3 fixed-fee decimals allowed) killed | ✅ PASS |
| FCAD-10 | `409` / other error / network failure: drawer stays open with typed values, error shown | `FRT:362-372` 409 alert `'Regra de taxa já existe para este comerciante, método de pagamento e parcelas'`, values `PIX`/`1`/`2,99`/`0,30`; `FRT:374-388` 500 `'Erro interno ao cadastrar'` and network `'Não foi possível conectar à API. Tente novamente.'` | ✅ PASS |
| FCAD-11 | "Cadastrar" disabled while sending; repeated clicks → one `POST` | `FRT:396-404` click, click, dblClick: `toBeDisabled()`, `createRequests()).toHaveLength(1)` | ✅ PASS |
| FCAD-12 | `Esc` or "Cancelar" closes without an API call | `FRT:407-415` dialog gone, 0 POSTs; `SideDrawer.test.tsx:29-41` | ✅ PASS |

### P2: Alterar taxas de uma regra

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FEDIT-01 | Drawer "Editar regra de taxa", Método and Parcelas read-only, rates filled with comma | `FRT:470-477` Método `'Crédito'` + `readonly`, Parcelas `'3'` + `readonly`, Percentual `'2,99'`, Taxa fixa `'0,30'`, both editable | ✅ PASS |
| FEDIT-02 | `PUT /api/merchants/{id}/fee-rules/{ruleId}` with only `feePercentage` and `fixedFee`, exact JSON numbers | `FRT:485-486` path `/api/merchants/m-1/fee-rules/r-2`, body `toBe('{"feePercentage":3.49,"fixedFee":0.35}')`; `AFT:47-50` `'{"feePercentage":3.49,"fixedFee":0.30}'`, `PUT` | ✅ PASS |
| FEDIT-03 | `200` closes the drawer and shows the new values | `FRT:493-497` dialog gone, row `['Crédito','3x','3,49%','R$ 0,35']`; `QFT:67-79` | ✅ PASS |
| FEDIT-04 | Invalid rates: block, same messages as FCAD-08/09 | `FRT:500-512` `100,01`, empty percent, `0,301`, `-1`: exact texts, `aria-invalid`, `puts()).toHaveLength(0)` (S5, S7 killed here too). It shares `parseFeePercent`, so gap 1 means an unchanged 100% rule (prefilled `'100,00'`) has no test proving it can be saved | ✅ PASS (AC text; the 100% prefill risk is counted under gap 1) |
| FEDIT-05 | Error / network failure: drawer open with typed values, message shown | `FRT:514-525` `'Erro interno ao alterar'` and network message; values `3,49` / `0,35` kept | ✅ PASS |
| FEDIT-06 | "Salvar" disabled while sending | `FRT:533-538` `toBeDisabled()`, extra click, `puts()).toHaveLength(1)` | ✅ PASS |

### P2: Desativar regra

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FDES-01 | Confirmation "Desativar a regra <Método> <N>x?" with the refusal warning | `FRT:584-593` `toHaveAccessibleName('Desativar a regra Crédito 3x?')` / `'Desativar a regra Pix 1x?'`, `toHaveAccessibleDescription('Novas transações dessa combinação serão recusadas até existir outra regra.')`, 0 DELETEs | ✅ PASS |
| FDES-02 | Confirm → `DELETE /api/merchants/{id}/fee-rules/{id}` | `FRT:599-601` path `/api/merchants/m-1/fee-rules/r-2`, body `undefined`; `AFT:53-56` `DELETE` | ✅ PASS |
| FDES-03 | `204` closes the dialog and removes the rule | `FRT:608-609` alertdialog gone, rows `[['Pix','1x','0%','R$ 0,00']]`; `QFT:81-92` | ✅ PASS |
| FDES-04 | Cancel or `Esc` closes without an API call | `FRT:612-625` dialog gone, 0 DELETEs, rows unchanged; `ConfirmDialog.test.tsx:23-34` | ✅ PASS |
| FDES-05 | Error / network failure: dialog stays open, message shown | `FRT:627-637` `'Erro interno ao desativar'` / network message, dialog still named `'Desativar a regra Crédito 3x?'` | ✅ PASS |

**Status**: ❌ 33/34 ACs fully matched. In FCAD-08, the "> 100" rejection is now pinned for every digit count, but acceptance at the inclusive `100` boundary is only pinned for bare `100` (S6 survived). 0 spec-precision gaps.

---

## Edge Cases

- [x] `401` on any action → session ends: `frontend/lib/queries.test.tsx:227-240`; the fee-rule hooks use the same `createQueryClient` (`FRT:113`)
- [x] `404` on edit → message + list reload: `FRT:541-553`
- [x] `404` on deactivate → message + list reload: `FRT:639-651`
- [x] Merchant switch with drawer/dialog open → closed, no API call: `FRT:431-442`, `FRT:555-568`, `FRT:653-660`
- [x] `0` and `0` accepted: `FRT:418` body `"feePercentage":0,"fixedFee":0`
- [~] Percent with 4 decimals (`2,9999`) or `100` accepted: `FRT:419-420`, `FEE:12-13`. `100` with zero decimals is not tested (gap 1)

---

## Discrimination Sensor

Scratch: `git worktree add --detach .../scratchpad/sensor HEAD` (at `0d58242`) with `frontend/node_modules` symlinked from the real tree. Each mutation was a single exact-match replace (asserted `count == 1`), checked with `git diff --stat` (1 file, +1/-1), tested, then reverted with `git checkout -- frontend/lib/fee.ts` in the scratch. The worktree was removed with `git worktree remove --force` and pruned.

Frontend run: `vitest run lib components/fee-rules` (185 tests). No backend mutation this iteration: no backend file changed since iteration 2, where S2/S3 were killed.

| # | File:line | Mutation | Killed? |
| --- | --- | --- | --- |
| S4 | `frontend/lib/fee.ts:23` | Iteration-2 survivor: `digits.length > 3` → `> 4` (accepts `1000`..`9999`) | ✅ Killed (2 failed: `FEE` FCAD-08 case, `FRT` `Percentual "1000" bloqueia o envio`) |
| S1 | `fee.ts:23` | Iteration-1 survivor M3: removed `(digits.length === 3 && digits > '100') \|\|` | ✅ Killed (2 failed: `FEE` FCAD-08, `FRT` `Percentual "101"`) |
| S5 | `fee.ts:23` | `100` + fraction check `/[1-9]/` → `/[2-9]/` (accepts `100,01`) | ✅ Killed (3 failed: `FEE` FCAD-08, `FRT` FCAD-08 `"100,01"`, `FRT` FEDIT-04 `"100,01"`) |
| S6 | `fee.ts:23` | `/[1-9]/.test(fraction ?? '')` → `fraction !== undefined` (rejects `100,00`, `100,0000`, which equal 100) | ❌ **Survived** (185/185 passed) → fix task |
| S7 | `fee.ts:8` | `FIXED_FEE_INPUT = INPUT(2)` → `INPUT(3)` (fixed fee with 3 decimals accepted) | ✅ Killed (3 failed: `FEE` FCAD-09, `FRT` FCAD-09 `"0,301"`, `FRT` FEDIT-04 `"0,301"`) |
| S8 | `fee.ts:7` | `PERCENT_INPUT = INPUT(4)` → `INPUT(5)` | ✅ Killed (2 failed: `FEE` FCAD-08, `FRT` `"2,99999"`) |
| S9 | `fee.ts:12` | leading-zero strip removed (`whole.replace(/^0+(?=\d)/, '')` → `whole`) | ✅ Killed (1 failed: `FEE:14` `parseFeePercent('0100')).toBe('100')`) |
| S10 | `fee.ts:30` | `parseFixedFee` drops trailing fraction zeros (`0,30` → `0.3`) | ✅ Killed (4 failed: `FEE` FCAD-04, `FRT` FCAD-04 body, `FRT` "aceita percentual com 4 casas", "aceita percentual 100") |

**S6 impact probe** (scratch only, file deleted): `expect(toInputDecimal('100.0000')).toBe('100,00')` and `expect(parseFeePercent('100,00')).toBe('100.00')` pass on HEAD; with S6 applied, the second fails. `FeeRuleDrawer.tsx:57` prefills the edit percent with `toInputDecimal(rule.feePercentage)`, so a 100% rule could not be saved under S6.

Real-tree `git status --porcelain` before and after (identical, `diff` empty):

```
 M .specs/LESSONS.md
 M .specs/lessons.json
?? .specs/features/fee-rules-screen/validation.md
?? frontend/AGENTS.md
```

**Sensor depth**: lightweight-plus (8 mutations on `parseFeePercent` / `parseFixedFee`, the only code touched by the fix loop; the 16-mutation run in iteration 1 and S2/S3 in iteration 2 cover the rest)
**Result**: 7/8 killed, 1 survived - FAIL ❌

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ fix commits are test-only |
| Surgical changes | ✅ `0d58242`: 2 files, +5/-1 |
| No scope creep | ✅ |
| Matches patterns | ✅ same `it.each` / `expect` lists extended |
| Spec-anchored outcome check | ⚠️ gap 1 (FCAD-08 acceptance at `100` with zero decimals) |
| Per-layer Coverage Expectation | ✅ service unit + integration; lib, hooks and components 1:1 with the ACs |
| Every test maps to a requirement | ✅ new cases are named FCAD-08 |
| Documented guidelines followed | ✅ `AGENTS.md` (JUnit 5/MockMvc/Testcontainers, `AbstractIntegrationTest`, JaCoCo gate) |

---

## Gate Check

- **Backend**: `cd backend && ./mvnw verify` → exit 0. **547 tests, 0 failures, 0 errors, 0 skipped** (summed from `target/surefire-reports` and `target/failsafe-reports` XML). JaCoCo 85%/75% check passed inside `verify`
- **Frontend**: `corepack pnpm test` → exit 0, **419 passed (419)**; `corepack pnpm typecheck` → exit 0; `corepack pnpm build` → exit 0 (`/taxas` prerendered)
- **Test count**: backend 547 (unchanged since iteration 2), frontend 418 → 419 (+1 `it.each` row `'1000'`; `FEE` gained 4 `expect` lines inside existing tests). Nothing deleted, nothing skipped

---

## Fix Plans

### Fix 1 (Minor): pin `100` with zero decimals as accepted (gap 1, FCAD-08, S6 survived)

- **Root cause**: `fee.ts:23` treats `100` with a fraction as above 100 only when the fraction has a non-zero digit. The tests cover `100` (accepted) and `100,01` (rejected), but not `100,00`, so a check that rejects `100` with any fraction goes unnoticed. The edit drawer prefills a 100% rule as `'100,00'`, so this case shows up in normal use.
- **Fix task**: add `expect(parseFeePercent('100,00')).toBe('100.00')` and `expect(parseFeePercent('100.0000')).toBe('100.0000')` to `frontend/lib/fee.test.ts:10-15`. Also add an FRT case: either `['percentual 100,00', '100,00', '0,30', '"feePercentage":100.00,"fixedFee":0.30']` in the accept `it.each` at `FeeRulesPage.test.tsx:417-421`, or an edit test that opens a 100% rule and saves it unchanged. No production change.
- **Verify**: re-apply S6 (`/[1-9]/.test(fraction ?? '')` → `fraction !== undefined`) in scratch; it must be killed. S5 must stay killed.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| FREG-01..03 | ✅ Verified | ✅ Verified |
| FLIST-01..08 | ✅ Verified | ✅ Verified |
| FCAD-01..07, FCAD-09..12 | ✅ Verified | ✅ Verified |
| FCAD-08 | ❌ Needs Fix (4+-digit integer) | ❌ Needs Fix (`100` with zero decimals) |
| FEDIT-01..06 | ✅ Verified | ✅ Verified |
| FDES-01..05 | ✅ Verified | ✅ Verified |

---

## Summary

**Overall**: ❌ Not ready (one minor test gap; behavior is correct). Iteration 3 of 3, so this **escalates to the user**.

**Spec-anchored check**: 33/34 ACs fully matched | 0 spec-precision gaps
**Sensor**: 7/8 mutations killed (S6 survived)
**Gate**: backend 547 passed, frontend 419 passed, 0 failed

**What works**: all three earlier gaps (iteration 1: FCAD-08 3-digit, FREG-03 BOLETO; iteration 2: FCAD-08 4+ digits) are closed with test-only commits. S1, S4, S5, S7, S8, S9 and S10 are killed. All other ACs hold as in iteration 2.

**Issues found**: gap 1 (FCAD-08: `100,00` not pinned as accepted; this affects editing a 100% rule).

**validate_state.py**: `python3 -I <skill-dir>/scripts/validate_state.py fee-rules-screen --root <repo>` → exit 1 ("validation.md verdict is FAIL"), as expected for a FAIL report.

**Next steps**: the user decides whether to apply Fix 1 (a test-only change of about 3 lines) and re-verify outside the loop, or to accept the gap knowingly.
