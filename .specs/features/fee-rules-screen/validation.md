# Tela de Regras de Taxa Validation

**Date**: 2026-10-15
**Spec**: `.specs/features/fee-rules-screen/spec.md`
**Diff range**: `1b31a86..0add611` (spec/design `22eb692`, `6b4f5a3`; implementation `bd063cf..010c4bc`; `fb9bfef` pins the transaction message; iteration-1 fixes `65040f5`, `b944fe1`; iteration-2 fix `0d58242`; docs `0def19c`; iteration-3 fix `094330d`; iteration-4 report `6563e67`; iteration-4 fix `0add611`), branch `feat/fee-rules-screen`, 20 commits
**Verifier**: independent sub-agent (author ≠ verifier), iteration 5. This is past the normal bound of 3. The user authorized it, with a fix aimed at the whole class of gaps rather than one example at a time.

## Validation: fee-rules-screen - PASS ✅

`0add611` changes tests only (3 files, +109/-1). It adds the iteration-4 examples to `FEE` and an `FRT` edit test for a stored `2.9999` rule. It also adds `REF`, which compares `parseFeePercent`, `parseFixedFee`, `formatPercent` and `toInputDecimal` against an independent BigInt reference over generated inputs. All three iteration-4 gaps are closed. V1, V2, V3 and V4 are killed, and the earlier survivors S1, S4 and S6 stay killed.

I injected 20 new mutants of my own: 15 in `lib/fee.ts` and 5 in the components (C1-C5). 10 of the `lib/fee.ts` ones and 4 of the 5 component ones were killed. Every standard operator mutation was killed: relational, constant, regex character-class range and anchor, slice bound, trim and leading-zero removal. Six mutants survived, and none of them is counted as a gap:

- **N1b and N2 (contrived, behavior-changing).** These add a new condition that no plausible implementation would write. N1b accepts whole numbers from 300 to 899. N2 swaps `[1-9]` for the set `[1569]`, which accepts `100,2`. They survive because the reference generator only samples some digits. Fraction digits are limited to {0, 1, 5, 9}, and no whole number falls between 300 and 899. The backend still answers `400` for these values (`@DecimalMax("100.0000")`). I list this as optional hardening, not a gap. See "Judgement on N1b/N2".
- **N10 (= V5).** The fixed fee is not trimmed. The spec does not define whitespace handling.
- **N12, N13, C4 (equivalent on every value the API sends).** N12 strips leading zeros from a whole part the API never sends. N13 handles a fraction-less value the API never sends. C4 prefills `100,00` as `100`, which is the same value.

File abbreviations:

- `FRT` = `frontend/components/fee-rules/FeeRulesPage.test.tsx`
- `REF` = `frontend/lib/fee.reference.test.ts`
- `FEE` = `frontend/lib/fee.test.ts`
- `FIT` = `backend/src/test/java/br/com/hanrry/reconpay/feerule/integration/FeeRuleIntegrationTest.java`
- `FST` = `backend/src/test/java/br/com/hanrry/reconpay/feerule/service/FeeRuleServiceTest.java`
- `MMT` = `frontend/components/shell/MerchantMenu.test.tsx`
- `AFT` = `frontend/lib/api.feeRules.test.ts`
- `QFT` = `frontend/lib/queries.feeRules.test.tsx`

---

## Gap history

| Iter. | Gap | Finding | Fix commit | Evidence now | Status |
| --- | --- | --- | --- | --- | --- |
| 1 | FCAD-08 | No test for a 3-digit integer percent above 100; M3 survived | `65040f5` | `FEE:22-23`, `FEE:44`; `FRT:342` `['101']`; `REF:61-65`. S1 killed (10 failures) | ✅ Closed |
| 1 | FREG-03 | No evidence for BOLETO 1x → 201 | `b944fe1` | `FIT:167-184` `@CsvSource` 5 combinations → `isCreated()` | ✅ Closed |
| 2 | FCAD-08 | No test for a 4+-digit integer percent; S4 survived | `0d58242` | `FEE:24-26`, `FEE:45`; `FRT:342` `['1000']`; `REF:61-65`. S4 killed (5 failures) | ✅ Closed |
| 3 | FCAD-08 (+ edge case "é `100`") | `100,00` accepted not pinned; S6 survived | `094330d` | `FEE:36-37`; `FRT:421`; `REF:61-65`. S6 killed (11 failures) | ✅ Closed |
| 4 | FEDIT-01 | Prefill not pinned for 3-4 significant decimals; V1 survived | `0add611` | `FEE:92-95` `toInputDecimal('2.9999')` `'2,9999'`, `('2.9990')` `'2,999'`, `('2.0500')` `'2,05'`; `FRT:490-498` stored `2.9999` → `toHaveValue('2,9999')`, unchanged save → body `'{"feePercentage":2.9999,"fixedFee":0.30}'`; `REF:82-90`. V1 killed (3 failures) | ✅ Closed |
| 4 | FLIST-03 | Inner zero not pinned; V2 survived | `0add611` | `FEE:82-84` `formatPercent('2.0500')` `'2,05%'`, `('10.0000')` `'10%'`, `('0.0500')` `'0,05%'`; `REF:73-80`. V2 killed (2 failures) | ✅ Closed |
| 4 | FCAD-08 | `100` + non-zero decimal pinned only for the digit last and only for the digits 1 and 5; V3/V4 survived | `0add611` | `FEE:43` `'100,10'`, `'100,50'`, `'100,9'`, `'100,06'`, `'100,0090'`; `REF:61-65`. V3 and V4 killed (4 failures each) | ✅ Closed |
| 5 | - | No new gap. N1b and N2 survive only because the generator samples some digits (contrived, optional hardening) | - | - | ➖ Not a gap |

**User decisions.**

- After iteration 3 (FAIL, the last iteration in the bound), the user first accepted the FCAD-08 gap as a known gap. `0def19c` recorded that in `validation.md`, in `STATE.md`, in the FCAD-08 traceability ("Implementing (lacuna aceita…)") and in the tasks.md Status ("Done (lacuna de teste aceita em FCAD-08…)").
- The user then chose to close it (`094330d`) and authorized iteration 4. Iteration 4 failed with three new gaps (V1-V4), which went back to the user.
- The user authorized iteration 5 with a fix that targets the whole class of gaps. `0add611` adds the reference test plus the specific iteration-4 cases.

All fix commits change tests only. `0add611` changes one existing line, `FEE:43`, which keeps the four original inputs and adds five. No assertion was removed or weakened.

**Docs state (reported, not edited).** `spec.md:196` still reads FCAD-08 "Implementing (lacuna aceita: `100,00` sem teste, ver validation.md)". `spec.md:215` reads "33 verified, 1 com lacuna aceita (FCAD-08)". `tasks.md:12` reads "Done (lacuna de teste aceita em FCAD-08, ver validation.md)". All three are stale: the accepted gap is closed and FCAD-08 is now verified. The orchestrator should set FCAD-08 to Verified, the coverage line to "34 verified", and the tasks.md Status to Done.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 backend installments rule | ✅ Done | `FeeRuleService.java:130-135` `ensureInstallmentsAllowed`, used on create and update |
| T2 `SideDrawer` / `ConfirmDialog` shells | ✅ Done | - |
| T3 `lib/fee.ts` | ✅ Done | Iteration-4 test gaps closed by `0add611` |
| T4 API functions | ✅ Done | - |
| T5 hooks | ✅ Done | - |
| T6 list + menu item | ✅ Done | - |
| T7 create drawer | ✅ Done | - |
| T8 edit rates | ✅ Done | `FRT:490-498` pins 4-decimal prefill and an unchanged save |
| T9 deactivate | ✅ Done | - |
| T10 docs | ✅ Done | README + frontend/README |

---

## Reference test review (`REF`, new in `0add611`)

**Independence: yes.** The reference does not reuse any logic or regex from `lib/fee.ts`. It splits each generated input into its whole and fraction parts while building it (`REF:14-21`). It does not parse text. Bounds are checked by exact integer arithmetic: `scaled(whole, fraction, 4) <= 100·10⁴` (`REF:24-37`), not by comparing digit strings. The expected API text is `BigInt(whole).toString()` + `.` + the typed fraction (`REF:32`), not the app's `normalize` regex. `formatPercent` and `toInputDecimal` are checked by properties, not against a re-implementation:

- `formatPercent` (`REF:73-80`): the output must match `^\d+(,\d*[1-9])?%$`, so the separator is a comma and no zero is left at the end, and its scaled value must equal the API value.
- `toInputDecimal` (`REF:82-90`): the prefill must be `\d+,\d{2,4}` with the same value, and it must round-trip through `parseFeePercent` to the same value. This checks FEDIT-01 and the "salvar sem mudar" half of FEDIT-02 together.

**Spec encoding: correct.**

- FCAD-08: 0 to 100 inclusive, at most 4 decimals.
- FCAD-09: from 0, at most 2 decimals, no upper bound (`REF:39-42`).
- FCAD-04: the exact typed value, comma → dot. The only normalization is dropping leading zeros from the whole part, which a JSON number requires.
- FLIST-03: comma, no trailing zeros.
- FEDIT-01/02: the same value with a comma, and an unchanged save keeps it.

The "at least 2 decimals" prefill rule is stricter than the spec. It is the implementation's documented choice and does not conflict with the spec.

**Breadth: good structurally, but the digits are sampled.**

- Parser inputs: 22 whole parts × (1 + 29 fractions × 2 separators) = 1298. They cover leading zeros, every digit count from 1 to 5, the bound itself (`100`, `0100`, `00100`) and the values on each side of it (`99`, `101`, `109`, `110`), plus fractions of lengths 0 to 5 with the non-zero digit in each position.
- API values: 115 percents from `0.0000` to `100.0000` (`REF:51-58`).
- Not covered by the generator:
  1. Whole parts from 300 to 899.
  2. Non-zero fraction digits other than 1, 5 and 9.
  3. Malformed text: negatives, whitespace, `1,`, `,5`, `1e2`. These stay pinned by the examples in `FEE:42-49` and `FEE:64-73`.
  4. Fixed-fee prefill. `toInputDecimal` is checked over percents only; the fixed fee has the examples `FEE:91` and `FRT:468-479`.

Items 1 and 2 are why N1b and N2 survive.

**AD-010: holds.** BigInt appears only in `REF`. `lib/fee.ts` has no `Number`, `parseFloat`, `BigInt` or arithmetic: it uses only regexes, `split`, `slice`, `replace` and one string comparison (`fee.ts:6-45`). The fee-rules components do no decimal arithmetic. `FeeRuleDrawer.tsx:21` uses `Number()` only on the installments count, and `FeeRulesPage.tsx:98-99` only formats. `lib/api.ts:298` sends both rates as `LosslessNumber` text.

**Does it close the class?** Yes, for every standard mutation operator. In this run the reference tests alone killed V1, V2, V3, V4, S4, S6, N1, N3, N4, N6, N8, N9, N11 and N14, most of them without help from the examples. The class that L-017, L-019, L-020 and L-023 describe is closed: one missing example per path through the text-based bound check. Each of those lessons named a single hand-picked path. The reference now tests all of them together over a generated grid.

---

## Spec-Anchored Acceptance Criteria

`0add611` inserts 10 lines at `FRT:490-499`, so every `FRT` citation after line 489 moved by +10 from iteration 4. The ranges below have been updated. I re-read `fee.ts:1-45`, `FEE:1-103`, `REF:1-91`, `FeeRuleDrawer.tsx:15-100`, `FRT:468-509` and the FRT test index. No backend file changed since iteration 2. Backend citations were confirmed by the green `verify`.

### P1: Método sem parcelamento recusado na API

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FREG-01 | `POST` PIX/BOLETO/DEBIT_CARD with installments > 1 → `400 VALIDATION_ERROR` "Método de pagamento <MÉTODO> não permite parcelamento" | `FIT:147-165` `@ValueSource` PIX, BOLETO, DEBIT_CARD, `installments: 2`: `status().isBadRequest()`, `jsonPath("$.error").value("VALIDATION_ERROR")`, `jsonPath("$.message").value("Método de pagamento " + paymentMethod + " não permite parcelamento")`; `FST:243-259` `hasMessage(...)` + `verify(feeRuleRepository, never()).save(any())` | ✅ PASS |
| FREG-02 | `PUT` producing PIX/BOLETO/DEBIT_CARD with > 1 installment → same `400` + message, rule unchanged | `FIT:186-209` CREDIT_CARD 3x → `{"paymentMethod":"PIX",...}`: `isBadRequest()`, `VALIDATION_ERROR`, message, then `GET` `$.paymentMethod` `CREDIT_CARD`, `$.installments` `3`, `$.feePercentage` `2.99`; `FIT:211-229` PIX 1x → `{"installments":2}` 400 + message, then `$.installments` `1`; `FST:262-318` unchanged + `never().save` | ✅ PASS |
| FREG-03 | CREDIT_CARD with installments ≥ 1, or PIX/BOLETO/DEBIT_CARD with 1 → `201` | `FIT:167-184` `@CsvSource({"CREDIT_CARD, 1", "CREDIT_CARD, 12", "PIX, 1", "BOLETO, 1", "DEBIT_CARD, 1"})`: `status().isCreated()`, `$.paymentMethod`, `$.installments` | ✅ PASS |

### P1: Listar regras da empresa selecionada

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FLIST-01 | Menu footer item "Regras de taxa da <empresa>" closes the menu and opens `/taxas` | `MMT:86-96` link `{ name: 'Regras de taxa da Beta Comércio' }` `toHaveAttribute('href', '/taxas')`, footer order `toEqual([...])`, after click `queryByRole('listbox')).not.toBeInTheDocument()`; `MMT:98-104` no item without a merchant | ✅ PASS |
| FLIST-02 | `GET /api/merchants/{id}/fee-rules?page=0&size=100&sort=paymentMethod,asc&sort=installments,asc`; title "Regras de taxa" + merchant name | `FRT:133-144` path `toBe('/api/merchants/m-1/fee-rules')`, `search.toString()` `toBe('page=0&size=100&sort=paymentMethod%2Casc&sort=installments%2Casc')`, h1 "Regras de taxa", `getByText('Alfa Pagamentos')`; `AFT:30-38` exact URL + `GET` + Bearer | ✅ PASS |
| FLIST-03 | Row: Crédito/Débito/Pix/Boleto, `1x`/`3x`, percent with comma and no trailing zeros (`2,99%`, `0%`), fixed fee `R$ 0,30` | `FRT:146-154` `rowTexts()` `toEqual([['Crédito','3x','2,99%','R$ 0,30'],['Pix','1x','0%','R$ 0,00']])`; `FRT:156-179` Boleto/Débito rows; `FEE:78-84` exact `formatPercent` values, including the inner zeros `'2.0500'` → `'2,05%'` and `'0.0500'` → `'0,05%'`; `REF:73-80` property over 115 API values. V2, N5 killed | ✅ PASS |
| FLIST-04 | Merchant switch shows only the new merchant's rules | `FRT:156-179` while pending no table and no `'Crédito'`, then Beta's rows, last path `/api/merchants/m-2/fee-rules`; `QFT:40-49` `data` `toBeUndefined()` | ✅ PASS |
| FLIST-05 | Empty: "Nenhuma regra de taxa", exact helper text, "Cadastrar regra" | `FRT:181-189` heading, `getByText('Transações só entram com uma regra ativa para o método e as parcelas.')`, button, no table; `FRT:270-276` button opens drawer | ✅ PASS |
| FLIST-06 | No merchant: "Nenhuma empresa no seu acesso" + "Cadastrar empresa" | `FRT:191-198` heading, link `href` `/empresas?cadastrar=1`, `listRequests()).toHaveLength(0)` | ✅ PASS |
| FLIST-07 | Loading "Carregando regras de taxa" instead of the table | `FRT:200-214` `getByRole('status', { name: 'Carregando regras de taxa' })`, no table, then table | ✅ PASS |
| FLIST-08 | Error message + "Tentar de novo" re-fetches | `FRT:216-232` alert text, click "Tentar de novo", table shown, `listRequests()).toHaveLength(2)` | ✅ PASS |

### P1: Cadastrar regra

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FCAD-01 | Drawer "Cadastrar regra de taxa" with the 4 fields, no method, Parcelas `1` | `FRT:258-268` dialog name, method `toHaveValue('')`, options list, installments `toHaveValue('1')`, rates `''` | ✅ PASS |
| FCAD-02 | Pix/Boleto/Débito: Parcelas `1`, disabled | `FRT:278-285` `it.each` Pix/Boleto/Débito: `toHaveValue('1')`, `toBeDisabled()`; `FEE:99-102` set equality | ✅ PASS |
| FCAD-03 | Crédito enables Parcelas, any integer ≥ 1 | `FRT:287-298` Pix→Crédito `toBeEnabled()`, sends `installments: 12` | ✅ PASS |
| FCAD-04 | `POST` with `paymentMethod`, integer `installments`, decimals as exact JSON numbers (comma → dot) | `FRT:300-308` body `toBe('{"paymentMethod":"CREDIT_CARD","installments":3,"feePercentage":2.99,"fixedFee":0.30}')`; `FRT:417-430` literals `2.9999`, `100`, `100.00`, `0`; `AFT:40-45`; `FEE:5-8`, `FEE:33-40`, `FEE:53-62`; `REF:61-71` exact expected text over 1298 inputs each. N8, N11 killed | ✅ PASS |
| FCAD-05 | `201` closes the drawer and shows the new rule | `FRT:310-320` dialog gone, row `['Crédito','3x','2,99%','R$ 0,30']`; `QFT:53-65` | ✅ PASS |
| FCAD-06 | No method: block, no API call, "Escolha o método de pagamento" | `FRT:322-330` exact text, `aria-invalid` `true`, `createRequests()).toHaveLength(0)` | ✅ PASS |
| FCAD-07 | Installments not an integer ≥ 1: block, exact message | `FRT:332-340` `'0'`, `'1,5'`, `'abc'`, `''`: exact text, `aria-invalid`, 0 POSTs | ✅ PASS |
| FCAD-08 | Percent empty / NaN / < 0 / > 100 / > 4 decimals: block with "Informe um percentual de 0 a 100 com até 4 casas decimais" | `FRT:342-350` exact message, `aria-invalid`, 0 POSTs for `''`, `'abc'`, `'-1'`, `'100,01'`, `'101'`, `'1000'`, `'2,99999'`; `FEE:17-28`, `FEE:42-49` 33 rejected forms including `100,10`, `100,50`, `100,9`, `100,06`, `100,0090`; acceptance at the bound `FEE:33-40`; `REF:61-65` accept/reject equal to the exact BigInt reference over 1298 inputs. S1, S4, S5 (iter. 4), S6, V3, V4, N1, N3, N4, N14 killed. N1b/N2 survive (contrived, see below) | ✅ PASS |
| FCAD-09 | Fixed fee empty / NaN / negative / > 2 decimals: block with "Informe uma taxa fixa a partir de 0 com até 2 casas decimais" | `FRT:352-360` `''`, `'x'`, `'-0,01'`, `'0,301'`: exact text, `aria-invalid`, 0 POSTs; `FEE:58-73`; `REF:67-71`. N9, C3 killed | ✅ PASS |
| FCAD-10 | `409` / other error / network failure: drawer open with typed values, error shown | `FRT:362-372` 409 alert `'Regra de taxa já existe para este comerciante, método de pagamento e parcelas'`, values kept; `FRT:374-388` 500 and network messages | ✅ PASS |
| FCAD-11 | "Cadastrar" disabled while sending; one `POST` | `FRT:390-405` `toBeDisabled()`, `createRequests()).toHaveLength(1)` | ✅ PASS |
| FCAD-12 | `Esc` or "Cancelar" closes without an API call | `FRT:407-415` dialog gone, 0 POSTs; `SideDrawer.test.tsx:29-41` | ✅ PASS |

### P2: Alterar taxas de uma regra

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FEDIT-01 | Drawer "Editar regra de taxa", Método/Parcelas read-only, rates filled with the current values (comma) | `FRT:468-479` Método `'Crédito'` + `readonly`, Parcelas `'3'` + `readonly`, Percentual `'2,99'`, Taxa fixa `'0,30'`; `FRT:490-498` stored `2.9999` → `toHaveValue('2,9999')`; `FEE:89-96` (`2.9999`, `2.9990`, `2.0500`, `100.0000`); `REF:82-90` same value + round-trip over 115 values. V1, N6, N7, C1, C2 killed | ✅ PASS |
| FEDIT-02 | `PUT /api/merchants/{id}/fee-rules/{ruleId}` only `feePercentage` and `fixedFee`, exact JSON numbers | `FRT:481-488` path `/api/merchants/m-1/fee-rules/r-2`, body `toBe('{"feePercentage":3.49,"fixedFee":0.35}')`; `FRT:490-498` unchanged save body `toBe('{"feePercentage":2.9999,"fixedFee":0.30}')`; `AFT:47-50` | ✅ PASS |
| FEDIT-03 | `200` closes the drawer and shows the new values | `FRT:500-509` dialog gone, rows `[['Crédito','3x','3,49%','R$ 0,35'],['Pix','1x','0%','R$ 0,00']]`; `QFT:67-79` | ✅ PASS |
| FEDIT-04 | Invalid rates: block with the FCAD-08/09 messages | `FRT:511-523` `100,01`, empty, `0,301`, `-1`: exact texts, `aria-invalid`, `puts()).toHaveLength(0)`; same parsers as FCAD-08/09 | ✅ PASS |
| FEDIT-05 | Error / network failure: drawer open with typed values, message | `FRT:525-536` `'Erro interno ao alterar'` and network message; `3,49` / `0,35` kept | ✅ PASS |
| FEDIT-06 | "Salvar" disabled while sending | `FRT:538-550` `toBeDisabled()`, `puts()).toHaveLength(1)` | ✅ PASS |

### P2: Desativar regra

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FDES-01 | Confirmation "Desativar a regra <Método> <N>x?" with the refusal warning | `FRT:595-604` `toHaveAccessibleName('Desativar a regra Crédito 3x?')` / `'Desativar a regra Pix 1x?'`, `toHaveAccessibleDescription('Novas transações dessa combinação serão recusadas até existir outra regra.')`, 0 DELETEs | ✅ PASS |
| FDES-02 | Confirm → `DELETE /api/merchants/{id}/fee-rules/{id}` | `FRT:606-613` path `/api/merchants/m-1/fee-rules/r-2`, body `toBeUndefined()`; `AFT:53-56` | ✅ PASS |
| FDES-03 | `204` closes the dialog and removes the rule | `FRT:615-621` alertdialog gone, rows `[['Pix','1x','0%','R$ 0,00']]`; `QFT:81-92` | ✅ PASS |
| FDES-04 | Cancel or `Esc` closes without an API call | `FRT:623-636` dialog gone, 0 DELETEs, rows unchanged; `ConfirmDialog.test.tsx:23-34` | ✅ PASS |
| FDES-05 | Error / network failure: dialog open, message shown | `FRT:638-648` `'Erro interno ao desativar'` / network message, dialog still named | ✅ PASS |

**Status**: ✅ 34/34 ACs matched the spec outcome; 0 gaps; 0 spec-precision gaps.

---

## Edge Cases

- [x] `401` on any action → session ends: `frontend/lib/queries.test.tsx:227-240`; the fee-rule hooks share `createQueryClient` (`FRT:113`)
- [x] `404` on edit → message + list reload: `FRT:552-564`
- [x] `404` on deactivate → message + list reload: `FRT:650-662`
- [x] Merchant switch with the drawer or dialog open → closed, no API call: `FRT:432-443`, `FRT:566-579`, `FRT:664-671`
- [x] `0` and `0` accepted: `FRT:417-430` body `"feePercentage":0,"fixedFee":0`
- [x] Percent with 4 decimals (`2,9999`) or `100` accepted: `FRT:417-430`, `FEE:33-40`, `REF:61-65`

---

## Discrimination Sensor

Scratch: `git worktree add --detach .../scratchpad/sensor HEAD` (at `0add611`), with `frontend/node_modules` symlinked from the real tree. Each mutation was a single exact-match replace (asserted `count == 1`), tested with `vitest run lib components/fee-rules` (257 tests; baseline 257/257 on the unmutated scratch), then reverted with `git checkout -- <file>`. After the run, I removed the symlink, ran `git worktree remove --force` and `git worktree prune`, and confirmed that `git worktree list` no longer shows the scratch worktree.

### Earlier survivors (all must be killed)

| # | File:line | Mutation | Killed? |
| --- | --- | --- | --- |
| V1 | `fee.ts:44` | `toInputDecimal` keeps only 2 decimals | ✅ Killed (3: `FEE` toInputDecimal, `REF` toInputDecimal, `FRT` 4-decimal edit) |
| V2 | `fee.ts:36` | `formatPercent` strips every `0` | ✅ Killed (2: `FEE` formatPercent, `REF` formatPercent) |
| V3 | `fee.ts:23` | `/[1-9]/` → `/[1-9]$/` | ✅ Killed (4: `100,10`, `100,50`, `100,0090`, `REF` parseFeePercent) |
| V4 | `fee.ts:23` | `/[1-9]/` → `/[1-5]/` | ✅ Killed (4: `100,9`, `100,06`, `100,0090`, `REF`) |
| S1 | `fee.ts:23` | removed `(digits.length === 3 && digits > '100') \|\|` | ✅ Killed (10) |
| S4 | `fee.ts:23` | `digits.length > 3` → `> 4` | ✅ Killed (5, including `REF` and `FRT` `"1000"`) |
| S6 | `fee.ts:23` | `/[1-9]/.test(fraction ?? '')` → `fraction !== undefined` | ✅ Killed (11, including `REF` parseFeePercent and toInputDecimal round-trip) |

### New mutants (iteration 5)

| # | File:line | Mutation | Killed? |
| --- | --- | --- | --- |
| N1 | `fee.ts:23` | `digits > '100'` → `digits > '100' && digits < '300'` (accepts 300-999) | ✅ Killed (3: `999`, `999,99`, `REF`) |
| N1b | `fee.ts:23` | `digits > '100'` → `… && !(digits >= '300' && digits < '900')` (accepts 300-899 only) | ⚠️ Survived (257/257). Contrived, see below |
| N2 | `fee.ts:23` | `/[1-9]/` → `/[1569]/` (accepts `100,2` … `100,8` except 6) | ⚠️ Survived (257/257). Contrived, see below |
| N3 | `fee.ts:23` | Checks only the first 3 decimals (`(fraction ?? '').slice(0, 3)`) | ✅ Killed (2: `100,0001`, `REF`) |
| N4 | `fee.ts:7` | `INPUT(4)` → `INPUT(5)` | ✅ Killed (4, including `FRT` `"2,99999"`) |
| N5 | `fee.ts:36` | `/0+$/` → `/0$/` (strips one zero) | ✅ Killed (12, including `FRT` FLIST-03/04, FCAD-05, FEDIT-03) |
| N6 | `fee.ts:44` | Decimals 3-4: `replace(/0+$/,'')` → `replace(/0/g,'')` (`0.5005` → `0,505`) | ✅ Killed (1: `REF` toInputDecimal; only the reference catches it) |
| N7 | `fee.ts:44` | Drops the 4th decimal (`slice(2, 3)`) | ✅ Killed (4, including `FRT` FEDIT-01 and the 4-decimal edit test) |
| N8 | `fee.ts:30` | `parseFixedFee` strips trailing fraction zeros (`0,30` → `0.3`) | ✅ Killed (10, including `FRT` FCAD-04 body) |
| N9 | `fee.ts:8` | `INPUT(2)` → `INPUT(3)` for the fixed fee | ✅ Killed (5, including `FRT` FCAD-09 and FEDIT-04 `"0,301"`) |
| N10 | `fee.ts:29` | `parseFixedFee` without `trim()` (= V5) | ⚠️ Survived. Not a gap: the spec does not define whitespace handling |
| N11 | `fee.ts:12` | `normalize` strips only one leading zero | ✅ Killed (3: `00100,00`, `REF` ×2) |
| N12 | `fee.ts:37` | `formatPercent` strips leading zeros of the whole part | ⚠️ Survived. Equivalent: the API never sends a leading zero |
| N13 | `fee.ts:43` | `toInputDecimal` on a fraction-less value returns `whole,00` | ⚠️ Survived. Equivalent: the API always sends a scale (`BigDecimal`), and the value is the same |
| N14 | `fee.ts:23` | `/[1-9]/` → `/[1-8]/` | ✅ Killed (3: `100,9`, `100,0090`, `REF`) |
| C1 | `FeeRuleDrawer.tsx:57` | Percent prefill truncated to 2 decimals in the component | ✅ Killed (1: `FRT` 4-decimal edit test) |
| C2 | `FeeRuleDrawer.tsx:58` | Fixed-fee prefill is the raw API text (`0.30`) | ✅ Killed (1: `FRT` FEDIT-01) |
| C3 | `FeeRuleDrawer.tsx:76` | Fixed fee validated with `parseFeePercent` | ✅ Killed (2: `FRT` FCAD-09 and FEDIT-04 `"0,301"`) |
| C4 | `FeeRuleDrawer.tsx:57` | Percent prefill strips trailing zeros (`100,00` → `100`) | ⚠️ Survived. Equivalent in value (like V10 in iteration 4); FEDIT-01 asks for the current value with a comma |
| C5 | `FeeRulesPage.tsx:98` | List shows the percent without its 4th decimal | ✅ Killed (2: `FRT` FCAD-05, FEDIT-03) |

**Judgement on N1b/N2.** These are not operator mutations: replacing a relational operator, a constant or a regex range, or removing an anchor or a clause. Each one inserts a new, arbitrary condition, a gap between 300 and 899 or a non-contiguous digit set, that no plausible implementation of "0 to 100" would contain. Every operator-level fault on the same lines was killed (S1, S4, S6, V3, V4, N1, N3, N14, plus S5/V6/V9 from iteration 4). I therefore do not count them as gaps. They do show that the generator samples digits rather than covering them all.

**Optional hardening (not counted):**

1. Add `'2'`…`'8'` (or `'d'`, `'0d'`, `'00d'`, `'000d'` for d = 1..9) to `REF:11` `FRACTIONS`.
2. Add `'300'`, `'500'`, `'899'` to `REF:10` `WHOLES`.

Together these would kill N1b and N2 at little cost.

Real-tree `git status --porcelain` before the sensor and after cleanup (identical, `diff` empty):

```
?? frontend/AGENTS.md
```

**Sensor depth**: expanded (P0-style, ≥5 per function). 7 earlier survivors re-applied plus 20 new mutants (15 in `lib/fee.ts`, 5 in the components).
**Result**: 27 injected, 21 killed, 6 survived. 0 spec-anchored gaps: 2 contrived, 3 equivalent on API values, 1 outside the spec. PASS ✅

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ fix commits are test-only; no production change since `010c4bc` |
| Surgical changes | ✅ `0add611`: 3 test files, +109/-1. The -1 line is extended, not weakened |
| No scope creep | ✅ |
| Matches patterns | ✅ `it.each` tables, exact `toBe` values; `REF` uses the same Vitest style and Portuguese test names |
| Spec-anchored outcome check | ✅ 34/34 |
| Per-layer Coverage Expectation | ✅ service unit + integration; lib (examples + reference), hooks and components 1:1 with the ACs |
| Every test maps to a requirement | ✅ `REF` tests are tagged FCAD-04/08/09, FLIST-03, FEDIT-01/02 |
| Documented guidelines followed | ✅ `AGENTS.md` (JUnit 5/MockMvc/Testcontainers, `AbstractIntegrationTest`, JaCoCo gate); AD-010 holds (BigInt only in the test) |

---

## Gate Check

- **Backend**: `cd backend && ./mvnw verify` → exit 0. **547 tests, 0 failures, 0 errors, 0 skipped** (summed from `target/surefire-reports` and `target/failsafe-reports` XML). The JaCoCo 85%/75% check passed inside `verify`.
- **Frontend**: `corepack pnpm test` → exit 0, **491 passed (491)**, 36 files. `corepack pnpm typecheck` → exit 0. `corepack pnpm build` → exit 0 (`/taxas` prerendered).
- **Test count**: backend 547 (unchanged). Frontend 481 → 491 (+10: 5 rejected `FEE` rows, 4 `REF` tests and 1 `FRT` edit test). Nothing deleted or skipped.

---

## Fix Plans

None required. Optional hardening of the `REF` generator for N1b/N2 is described under the sensor.

---

## Requirement Traceability Update

The Verifier does not edit `spec.md` or `tasks.md`. Recommended statuses:

| Requirement | Current text in spec.md | Recommended |
| --- | --- | --- |
| FREG-01..03 | Verified | ✅ Verified |
| FLIST-01..08 | Verified | ✅ Verified |
| FCAD-01..07, 09..12 | Verified | ✅ Verified |
| FCAD-08 | Implementing (lacuna aceita: `100,00` sem teste, ver validation.md) | ✅ Verified (accepted gap closed by `094330d`; V3/V4 closed by `0add611`) |
| FEDIT-01..06 | Verified | ✅ Verified |
| FDES-01..05 | Verified | ✅ Verified |

Also: `spec.md:215` → "34 verified"; `tasks.md:12` → "Done".

---

## Summary

**Overall**: ✅ Ready.

**Spec-anchored check**: 34/34 ACs matched | 0 gaps | 0 spec-precision gaps
**Sensor**: 27 injected, 21 killed, 6 survived (0 counted as gaps)
**Gate**: backend 547 passed, frontend 491 passed, 0 failed

**What works**:

- Every gap from iterations 1 to 4 is closed.
- The BigInt reference test is independent of `lib/fee.ts` and matches the spec. It kills every standard mutation of the four helpers, so the "one missing example per path" class is closed.
- AD-010 holds.

**Issues found**: none counted. Two contrived mutants (N1b, N2) survive because the reference samples digits instead of covering them all. The fix is optional and cheap.

**validate_state.py**: `python3 -I <skill-dir>/scripts/validate_state.py fee-rules-screen --root <repo>` → exit 0.

**Next steps**:

1. The orchestrator updates the stale FCAD-08 traceability (`spec.md:196`, `:215`) and the tasks.md Status (`tasks.md:12`).
2. Optionally, widen `REF:10-11` with the extra wholes and fraction digits listed under the sensor.

---

## Depois do PASS

O reforço opcional sugerido nesta iteração foi aplicado em `b0e999f` (commit `test(frontend): widen the fee reference inputs to every digit`): o gerador de `frontend/lib/fee.reference.test.ts` passou a usar todo dígito inicial de 100 a 999 e todo dígito decimal em cada posição. Os sobreviventes N1b (aceitar 300–899) e N2 (`[1569]`) foram reaplicados localmente e agora falham no teste de referência. Frontend 491 testes, `typecheck` ok.
