# Tela de Fechamento Validation

**Date**: 2026-10-17
**Spec**: `.specs/features/closing-screen/spec.md`
**Diff range**: `03b25e9^..e86fdbc` (18 commits), branch `feat/closing-screen`. Correção desta rodada: `080499b..e86fdbc` (`e86fdbc`, só teste).
**Verifier**: sub-agente independente (author ≠ verifier), iteração 5 (rodada extra autorizada pelo usuário depois da iteração 4 reprovada).

## Validation: closing-screen (iteração 5) - PASS ✅

O gap da iteração 4 está fechado. O caso novo `LT:39-48` (`periodWindows` com `{ 2026-10-01, 2026-10-31 }` e `{ 2026-10-16, 2026-10-31 }`, `LT:44` `toEqual` com as 2 janelas, a de 16/10 primeiro) mata W4: com a chave só pela data final, só esse caso falha. Com `LT:24-37` (mesma data inicial, M2 morto na iteração 1), as duas datas entram na identidade da janela. V3, V15, V16, N6, N10 e M27 continuam mortos.

A correção não muda nenhum arquivo de produção e não enfraquece nem remove teste: `git diff --numstat 080499b..e86fdbc` dá só `11 0 frontend/lib/closing.test.ts`, um `it` novo no `describe('periodWindows')`. Gate verde: 861 testes (+1), typecheck e build.

O sensor desta rodada injetou 19 mutantes: W4, V3, V15, V16, N6, N10 e M27 de novo, mais 12 novos (X1-X12). Todos morreram. Nenhum spec-precision gap, nenhum `SPEC_DEVIATION`.

Abreviações: `LT` = `frontend/lib/closing.test.ts`, `RT` = `frontend/components/closing/PeriodRow.test.tsx`, `PT` = `frontend/components/closing/ClosingPage.test.tsx`, `CST` = `frontend/components/dashboard/CloseStepper.test.tsx`, `QT` = `frontend/lib/queries.test.tsx`.

---

## Task Completion

| Task | Status | Notes |
| --- | --- | --- |
| T1 | ✅ Done | `13ccbc7` |
| T2 | ✅ Done | `341c0d8` |
| T3 | ✅ Done | `ef4a69e` |
| T4 | ✅ Done | `c7895bb` |
| T5 | ✅ Done | `5f6c23b` |
| T6 | ✅ Done | `8cb65c2`, `ecb479d` |
| Fix 1 (iteração 1) | ✅ Done | `3289be0`: `PT:301-326` |
| Fix 1 (iteração 2) | ✅ Done | `16f1337`: `RT:199-213` |
| Fix 2 (iteração 2) | ✅ Done | `40d5721`: `PT:217-227` |
| Fix 1 (iteração 3) | ✅ Done | `edda9b5`: `LT:63`, `LT:70`, `LT:75` |
| Fix 1 (iteração 4) | ✅ Done | `e86fdbc`: `LT:39-48` |

---

## Spec-Anchored Acceptance Criteria

Re-derivado no HEAD `e86fdbc`. `RT`, `PT` e os arquivos de produção não mudaram desde `edda9b5`. Em `LT`, as linhas depois de 38 andaram +11: `formatDuration` está em `LT:62-80` (asserção `LT:79`), `formatLockedAt` em `LT:85` e `LT:89`, `periodStatus` em `LT:103` e `LT:107`.

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FEC-01 AC1 título, empresa, tabela, uma linha por janela distinta | `spec.md:80` | `PT:154` `findByRole('table', { name: 'Períodos' })`; `PT:155` `within(pageHeader()).getByText('Alfa Pagamentos')`; `PT:158-163` 4 linhas para 5 runs; `LT:18-21` dedup do mesmo par; `LT:44-47` `toEqual` com 2 janelas de mesma data final; `LT:31-36` 2 janelas de mesma data inicial. W4 e M2 mortos | ✅ PASS |
| FEC-01 AC2 ordem com empate | `spec.md:81` | `LT:31-36` `toEqual([...])` com empate pela data final; `LT:44-47` (16/10 antes de 01/10); `PT:158-163`. X1 morto | ✅ PASS |
| FEC-01 AC3 colunas e nome | `spec.md:82` | `PT:156-157` `toEqual(['Período','Status','Match','Em aberto','Travado em','Duração','Ações'])`; `RT:59` `'Outubro de 2026'`. X9 morto | ✅ PASS |
| FEC-01 edge vários runs | `spec.md:176` | `LT:18-21`; `PT:158` | ✅ PASS |
| FEC-01 edge janela parcial | `spec.md:177` | `RT:83` `toBe('01/10/2026 – 15/10/2026')` | ✅ PASS |
| FEC-02 AC4 `200` aberto | `spec.md:83`, `spec.md:39` | `RT:59` `toEqual(['Outubro de 2026','Aberto','94,2%','R$ 10,50','—','—'])` | ✅ PASS |
| FEC-02 AC4 `200` travado | `spec.md:83` | `RT:70` `[..., 'Travado','100%','R$ 0,00','02/11/2026 08:07','2 h 5 min']`; `LT:85` `'05/09/2026 08:07'`; `LT:89` `'—'`. X3, X4 mortos | ✅ PASS |
| FEC-02 AC4 formato da duração (regra completa) | `spec.md:40` | `LT:79` `toBe(text)` para 0 (`LT:63` "0 s"), 29, 59, 60, 180, 3599, 3600 (`LT:69` "1 h"), 3660 (`LT:70` "1 h 1 min"), 7200, 7500, 86399, 86400, 90000 (`LT:75` "1 d 1 h"), 97200 e nulo (`LT:77` "—"). V3, V15, V16, X2 mortos | ✅ PASS |
| FEC-02 AC5 `409` | `spec.md:84` | `RT:93` `toEqual(['Outubro de 2026', label, '—','—','—','—'])`, `RT:94` `buttons()` = `[]`; releitura depois de um `200`: `RT:211-212`; `LT:107` `toBe('running')`. N6 morto | ✅ PASS |
| FEC-02 AC6 `404` | `spec.md:85` | `RT:93-94` (caso 404); `RT:211-212` releitura 404; `PT:162` | ✅ PASS |
| FEC-02 edge `matchRate` nulo | `spec.md:178` | `RT:77` `toBe('0%')` | ✅ PASS |
| FEC-02 edge duração | `spec.md:179` | `LT:64`, `LT:67`, `LT:71`, `LT:72`, `LT:74`, `LT:76` com `LT:79` `toBe(text)` | ✅ PASS |
| FEC-03 AC1 paginação | `spec.md:99` | `PT:175`, `PT:178-180`, `PT:185-188`, `PT:192-193`; `LT:57` `toBe(10)`. X7 morto | ✅ PASS |
| FEC-03 edge 10 janelas | `spec.md:180` | `PT:203-204` 10 linhas e `navigation` nulo | ✅ PASS |
| FEC-03 AC2 sem runs | `spec.md:100` | `PT:211` "Nenhuma conciliação ainda."; `PT:212` `toHaveAttribute('href', '/conciliacoes')`; `PT:213` sem tabela; borda de uma janela `PT:224-226`. N10, X8 mortos | ✅ PASS |
| FEC-03 AC3 erro dos runs | `spec.md:101` | `PT:234` `toHaveTextContent('Você não tem acesso a este comerciante')`; `PT:237-240` | ✅ PASS |
| FEC-03 AC4 leitura só da página | `spec.md:102` | `PT:181`, `PT:189` | ✅ PASS |
| FEC-03 AC5 erro da linha | `spec.md:103` | `RT:104` `'Não carregou'`; `RT:105` `['Tentar novamente']`; `RT:108-110`; `RT:211-212` (403) | ✅ PASS |
| FEC-03 edge erro não JSON | `spec.md:181` | `frontend/lib/api.imports.test.ts:115` `toBe('A API retornou erro 502.')` | ✅ PASS |
| FEC-04 AC1 botões por status | `spec.md:117` | `RT:61` `['Travar','Exportar CSV']`; `RT:71` `['Destravar','Exportar CSV']` | ✅ PASS |
| FEC-04 AC2 confirmação | `spec.md:118` | `RT:119` `alertdialog` "Travar o período?"; `RT:120` `not.toHaveBeenCalled()`; `RT:121` botão "Travar"; `CST:115-122`. X10, X11, X12 mortos | ✅ PASS |
| FEC-04 AC5 cancelar | `spec.md:121` | `RT:155-156` | ✅ PASS |
| FEC-05 AC3 travar | `spec.md:119` | `RT:123` `toHaveBeenCalledWith('jwt','m-1',october)`; `RT:124`; `PT:273` `toEqual([october])` | ✅ PASS |
| FEC-05 AC4 destravar | `spec.md:120` | `RT:136`, `RT:140`, `RT:141` `[..., 'Aberto','100%','R$ 0,00','—','—']` | ✅ PASS |
| FEC-05 AC6 cache | `spec.md:122` | `RT:169` `toEqual(answer)`; `RT:170` `invalidateQueries` com `queryKeys.period('m-1', october)`; `PT:274-275`; `QT:204`. X5 morto | ✅ PASS |
| FEC-06 AC1 aviso | `spec.md:136` | `PT:255`, `PT:256`, `PT:260`; `RT:181`, `RT:223`, `RT:253`, `RT:242`. X6 morto | ✅ PASS |
| FEC-06 AC2 `409` relê | `spec.md:137` | `RT:182-183`, `RT:195-196`; `RT:211-212`; `RT:224` sem releitura fora do 409 | ✅ PASS |
| FEC-06 AC3 ocupado | `spec.md:138` | `RT:269-272`, `RT:285-288` | ✅ PASS |
| FEC-07 AC1 botão | `spec.md:152` | `RT:61`, `RT:71`, `RT:94`, `RT:212` | ✅ PASS |
| FEC-07 AC2 arquivo | `spec.md:153` | `RT:239` `[{ name: 'reconpay-2026-10-01-2026-10-31.csv', href: 'blob:csv' }]`; `RT:240` `exportRunCsv('jwt','m-1','r-9')`; `RT:241` | ✅ PASS |
| FEC-08 AC1 troca de empresa | `spec.md:167` | Sem cache: `PT:292-298`. Com cache: `PT:322-325`. M27 morto | ✅ PASS |
| FEC-08 AC2 sem empresa | `spec.md:168` | `PT:332` heading "Nenhuma empresa no seu acesso"; `PT:334` sem tabela; `PT:335` nenhuma chamada | ✅ PASS |

**Status**: ✅ 32/32 linhas batem com a spec. Nenhum spec-precision gap.

---

## Discrimination Sensor

Scratch: `git worktree add --detach <scratchpad>/verify-closing5 HEAD` com `frontend/node_modules` em symlink. No HEAD, `vitest run lib/closing.test.ts components/closing components/dashboard` deu 213/213 verdes (17 arquivos). Cada mutante rodou esse comando e depois `git checkout --` só do arquivo de produção mutado. X1-X12 são diferentes de M1-M31, N1-N12, V1-V17 e W1-W12.

| # | File | Mutação | Killed? |
| --- | --- | --- | --- |
| W4 | `frontend/lib/closing.ts:18` | janela identificada só pela data final | ✅ Killed (1: `LT:39`) |
| V3 | `frontend/lib/closing.ts:24` | `seconds === null` → `!seconds` | ✅ Killed (1) |
| V15 | `frontend/lib/closing.ts:32` | `hours ?` → `hours > 1 ?` | ✅ Killed (1) |
| V16 | `frontend/lib/closing.ts:29` | `minutes ?` → `minutes > 1 ?` | ✅ Killed (1) |
| N6 | `frontend/components/closing/PeriodRow.tsx:29` | `data = period.data` | ✅ Killed (3) |
| N10 | `frontend/components/closing/ClosingPage.tsx:33` | estado vazio com `<= 1` janela | ✅ Killed (1) |
| M27 | `frontend/components/closing/ClosingPage.tsx:97` | sem `key={merchantId}` | ✅ Killed (1) |
| X1 | `frontend/lib/closing.ts:19` | ordem sem o desempate pela data final | ✅ Killed (1) |
| X2 | `frontend/lib/closing.ts:26` | `< 3600` → `<= 3600` (3600 vira "60 min") | ✅ Killed (1) |
| X3 | `frontend/lib/closing.ts:39` | `formatLockedAt` sem o caso nulo | ✅ Killed (4) |
| X4 | `frontend/components/closing/PeriodRow.tsx:29` | valores só na linha "Aberto" (travada fica sem dados) | ✅ Killed (5) |
| X5 | `frontend/components/closing/PeriodRow.tsx:37` | resposta gravada na chave de outra janela | ✅ Killed (3) |
| X6 | `frontend/components/closing/PeriodRow.tsx:14` | aviso com texto fixo, sem a mensagem da API | ✅ Killed (7) |
| X7 | `frontend/components/closing/ClosingPage.tsx:60` | "Anterior" avança uma página | ✅ Killed (1) |
| X8 | `frontend/components/closing/ClosingPage.tsx:36` | link "Ir para conciliações" para `/conciliacao` | ✅ Killed (1) |
| X9 | `frontend/components/closing/ClosingPage.tsx:16` | colunas Match e Em aberto trocadas | ✅ Killed (1) |
| X10 | `frontend/components/dashboard/CloseStepper.tsx:26` | `Confirm` com `role="dialog"` | ✅ Killed (25) |
| X11 | `frontend/components/dashboard/CloseStepper.tsx:30` | `Confirm`: botão da ação com o texto fixo "Confirmar" | ✅ Killed (22) |
| X12 | `frontend/components/closing/PeriodRow.tsx:85` | confirmação da trava com o título "Destravar o período?" | ✅ Killed (2) |

**Sensor depth**: expandido (a tela trava períodos, caminho de integridade do fechamento).
**Resultado do sensor (iteração 5)**: 19/19 killed, aprovado

Isolamento: `git status --porcelain` do worktree real vazio antes e depois. O symlink foi removido, e `git worktree remove --force` e `git worktree prune` foram feitos; `git worktree list` não mostra mais o scratch.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ |
| Surgical changes | ✅ correção só em teste (11 linhas, um `it`) |
| No scope creep | ✅ |
| Matches patterns | ✅ mesmo formato dos outros casos de `describe('periodWindows')` |
| Spec-anchored outcome check | ✅ `LT:44` assere as 2 janelas exatas e a ordem de `spec.md:81` |
| Per-layer Coverage Expectation | ✅ `periodWindows` com o mesmo par, a mesma data inicial e a mesma data final |
| Every test maps to a spec requirement | ✅ `LT:39` cita FEC-01 AC1, AC2 |
| Documented guidelines followed: `AGENTS.md`, `frontend/AGENTS.md`, `.specs/LESSONS.md` | ✅ |

---

## Edge Cases

- [x] Vários runs por janela: `LT:18`, `PT:158`
- [x] Janela parcial: `RT:83`
- [x] `matchRate` nulo: `RT:77`
- [x] Durações 29/180/7200/7500/86400/97200: `LT:64-79`
- [x] Exatamente 10 janelas: `PT:204`
- [x] Erro não JSON: `frontend/lib/api.imports.test.ts:115`

---

## Gate Check

- **Gate command**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`
- **Resultado**: 861 passed, 0 failed, 0 skipped (65 arquivos); typecheck exit 0; build exit 0 (`/fechamento` estática)
- **Test count before feature**: 796
- **Test count after feature**: 861
- **Delta**: +65 (+1 nesta rodada, `LT:39`)
- **Skipped tests**: nenhum
- **Failures**: nenhuma

---

## Fix Plans

Nenhum.

---

## Requirement Traceability Update

`spec.md:189-198` ainda está em "Design / Pending" e "0 mapped to tasks". O doc está desatualizado e o Verifier não o editou. Proposta:

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| FEC-01 | Pending | ✅ Verified |
| FEC-02 | Pending | ✅ Verified |
| FEC-03 | Pending | ✅ Verified |
| FEC-04 | Pending | ✅ Verified |
| FEC-05 | Pending | ✅ Verified |
| FEC-06 | Pending | ✅ Verified |
| FEC-07 | Pending | ✅ Verified |
| FEC-08 | Pending | ✅ Verified |

Outros docs: `tasks.md` com **Status** "In Progress" e sem os fixes das iterações 1-4 na tabela de tasks.

---

## Summary

**Overall (iteração 5)**: ✅ Ready

**Spec-anchored check**: 32/32 linhas batem com a spec; 0 spec-precision gaps
**Sensor**: 19/19 mutations killed
**Gate**: 861 passed

**What works**: os gaps das iterações 1 a 4 estão fechados (M27, N6, N10, V3, V15, V16, W4). A identidade da janela é o par de datas, e a regra completa da duração está presa. Os quatro status, as ações com confirmação, o ocupado, o export, a paginação, o aviso, o cache, sem empresa, sem runs e a troca de empresa também estão presos.

**Issues found**: nenhum.

**Next steps**: atualizar a rastreabilidade de `spec.md`, o status de `tasks.md` e `.specs/STATE.md` (fora do escopo do Verifier). Push e PR ficam com o usuário.

---

# Histórico

## Veredito da iteração 4: reprovada

**Date**: 2026-10-17
**Spec**: `.specs/features/closing-screen/spec.md`
**Diff range**: `03b25e9^..edda9b5` (16 commits), branch `feat/closing-screen`. Correção desta rodada: `0649bac..edda9b5` (`edda9b5`, só teste).
**Verifier**: sub-agente independente (author ≠ verifier), iteração 4 (rodada extra autorizada pelo usuário depois da iteração 3 reprovada; histórico, o veredito vigente é o da iteração 5, no topo).

O gap da iteração 3 está fechado. As 3 linhas novas do `it.each` de `formatDuration` (`LT:52` `[0, '0 s']`, `LT:59` `[3660, '1 h 1 min']`, `LT:64` `[90000, '1 d 1 h']`) matam V3, V15 e V16, um caso cada. N6, N10 e M27 continuam mortos. A correção não muda nenhum arquivo de produção e não enfraquece nem remove teste: `git diff --numstat 0649bac..edda9b5` dá só `3 0 frontend/lib/closing.test.ts`, três linhas novas no `it.each`. Gate verde: 860 testes, typecheck e build.

O sensor desta rodada injetou 18 mutantes: V3, V15, V16, N6, N10 e M27 de novo, mais 12 novos (W1-W12). Um sobreviveu:

- **W4** (`frontend/lib/closing.ts:18`, chave do `Map` `` `${fromDate}/${toDate}` `` → `` `${toDate}` ``): duas janelas distintas com a mesma data final viram uma linha só. Exemplo: "01/10/2026 – 31/10/2026" e "16/10/2026 – 31/10/2026". A spec pede "uma linha por janela distinta dos runs da empresa" (`spec.md:80`, FEC-01 AC1). O caso é alcançável: a API aceita qualquer janela com `fromDate <= toDate` (`backend/src/main/java/br/com/hanrry/reconpay/reconciliation/dto/RunReconciliationRequestDTO.java:26`), e `/conciliacoes` dispara "qualquer janela de até 366 dias" (`README.md:256`). Com o mutante, uma das janelas some da lista e não pode ser travada nem exportada por ali. O M2 da iteração 1 (dedup só por `fromDate`) morreu porque `LT:24-37` tem duas janelas com a mesma data inicial. Nenhum teste tem duas janelas com a mesma data final.

Uma sonda no scratch confirmou o gap. Ela acrescentou a `LT` um caso com `periodWindows([{ '2026-10-01', '2026-10-31' }, { '2026-10-16', '2026-10-31' }])` e esperou as 2 janelas, a de 16/10 primeiro. A sonda passa no HEAD (31/31) e falha com W4. O scratch foi apagado depois.

O gap é só de teste, sem bug de produção: o código do HEAD usa o par de datas como chave.

Abreviações: `LT` = `frontend/lib/closing.test.ts`, `RT` = `frontend/components/closing/PeriodRow.test.tsx`, `PT` = `frontend/components/closing/ClosingPage.test.tsx`, `CST` = `frontend/components/dashboard/CloseStepper.test.tsx`, `QT` = `frontend/lib/queries.test.tsx`.

---

### Task Completion

| Task | Status | Notes |
| --- | --- | --- |
| T1 | ✅ Done | `13ccbc7` |
| T2 | ✅ Done | `341c0d8` |
| T3 | ✅ Done | `ef4a69e` |
| T4 | ✅ Done | `c7895bb` |
| T5 | ✅ Done | `5f6c23b` |
| T6 | ✅ Done | `8cb65c2`, `ecb479d` |
| Fix 1 (iteração 1) | ✅ Done | `3289be0`: `PT:301-326` |
| Fix 1 (iteração 2) | ✅ Done | `16f1337`: `RT:199-213` |
| Fix 2 (iteração 2) | ✅ Done | `40d5721`: `PT:217-227` |
| Fix 1 (iteração 3) | ✅ Done | `edda9b5`: `LT:52`, `LT:59`, `LT:64` |

---

### Spec-Anchored Acceptance Criteria

Re-derivado no HEAD `edda9b5`. `RT` e `PT` não mudaram desde `0649bac`. Em `LT`, as linhas depois de 51 andaram: a asserção de `formatDuration` está em `LT:68`, `formatLockedAt` em `LT:74` e `LT:78`, `periodStatus` em `LT:92` e `LT:96`.

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FEC-01 AC1 título, empresa, tabela, uma linha por janela distinta | `spec.md:80` | `PT:154` `findByRole('table', { name: 'Períodos' })`; `PT:155` `within(pageHeader()).getByText('Alfa Pagamentos')`; `PT:158-163` 4 linhas para 5 runs; `LT:18-21` dedup do mesmo par. **Sem caso com duas janelas de mesma data final**: W4 sobrevive | ❌ GAP |
| FEC-01 AC2 ordem com empate | `spec.md:81` | `LT:31-36` `toEqual([...])` com empate pela data final; `PT:158-163` | ✅ PASS |
| FEC-01 AC3 colunas e nome | `spec.md:82` | `PT:156-157` `toEqual(['Período','Status','Match','Em aberto','Travado em','Duração','Ações'])`; `RT:59` `'Outubro de 2026'` | ✅ PASS |
| FEC-01 edge vários runs | `spec.md:176` | `LT:18-21`; `PT:158` | ✅ PASS |
| FEC-01 edge janela parcial | `spec.md:177` | `RT:83` `toBe('01/10/2026 – 15/10/2026')` | ✅ PASS |
| FEC-02 AC4 `200` aberto | `spec.md:83`, `spec.md:39` | `RT:59` `toEqual(['Outubro de 2026','Aberto','94,2%','R$ 10,50','—','—'])` | ✅ PASS |
| FEC-02 AC4 `200` travado | `spec.md:83` | `RT:70` `[..., 'Travado','100%','R$ 0,00','02/11/2026 08:07','2 h 5 min']`; `LT:74` `'05/09/2026 08:07'`; `LT:78` `'—'` | ✅ PASS |
| FEC-02 AC4 formato da duração (regra completa) | `spec.md:40` | `LT:68` `toBe(text)` para 0 (`LT:52` "0 s"), 29, 59, 60, 180, 3599, 3600, 3660 (`LT:59` "1 h 1 min"), 7200, 7500, 86399, 86400, 90000 (`LT:64` "1 d 1 h"), 97200 e nulo (`LT:66` "—"). V3, V15, V16 mortos | ✅ PASS |
| FEC-02 AC5 `409` | `spec.md:84` | `RT:93` `toEqual(['Outubro de 2026', label, '—','—','—','—'])`, `RT:94` `buttons()` = `[]`; releitura depois de um `200`: `RT:211-212`. N6 morto | ✅ PASS |
| FEC-02 AC6 `404` | `spec.md:85` | `RT:93-94` (caso 404); `RT:211-212` releitura 404; `PT:162` | ✅ PASS |
| FEC-02 edge `matchRate` nulo | `spec.md:178` | `RT:77` `toBe('0%')` | ✅ PASS |
| FEC-02 edge duração | `spec.md:179` | `LT:53`, `LT:56`, `LT:60`, `LT:61`, `LT:63`, `LT:65` com `LT:68` `toBe(text)` | ✅ PASS |
| FEC-03 AC1 paginação | `spec.md:99` | `PT:175`, `PT:178-180`, `PT:185-188`; `LT:46` `toBe(10)` | ✅ PASS |
| FEC-03 edge 10 janelas | `spec.md:180` | `PT:203-204` 10 linhas e `navigation` nulo | ✅ PASS |
| FEC-03 AC2 sem runs | `spec.md:100` | `PT:211-214`; borda de uma janela `PT:224-226`. N10 morto | ✅ PASS |
| FEC-03 AC3 erro dos runs | `spec.md:101` | `PT:234` `toHaveTextContent('Você não tem acesso a este comerciante')`; `PT:237-240` | ✅ PASS |
| FEC-03 AC4 leitura só da página | `spec.md:102` | `PT:181`, `PT:189` | ✅ PASS |
| FEC-03 AC5 erro da linha | `spec.md:103` | `RT:104` `'Não carregou'`; `RT:105` `['Tentar novamente']`; `RT:108-110`; `RT:211-212` (403) | ✅ PASS |
| FEC-03 edge erro não JSON | `spec.md:181` | `frontend/lib/api.imports.test.ts:115` `toBe('A API retornou erro 502.')` | ✅ PASS |
| FEC-04 AC1 botões por status | `spec.md:117` | `RT:61` `['Travar','Exportar CSV']`; `RT:71` `['Destravar','Exportar CSV']` | ✅ PASS |
| FEC-04 AC2 confirmação | `spec.md:118` | `RT:119` `alertdialog` "Travar o período?"; `RT:120` `not.toHaveBeenCalled()`; `CST:115-122` | ✅ PASS |
| FEC-04 AC5 cancelar | `spec.md:121` | `RT:155-156` | ✅ PASS |
| FEC-05 AC3 travar | `spec.md:119` | `RT:123` `toHaveBeenCalledWith('jwt','m-1',october)`; `RT:124`; `PT:273` `toEqual([october])` | ✅ PASS |
| FEC-05 AC4 destravar | `spec.md:120` | `RT:136`, `RT:140`, `RT:141` `[..., 'Aberto','100%','R$ 0,00','—','—']` | ✅ PASS |
| FEC-05 AC6 cache | `spec.md:122` | `RT:169` `toEqual(answer)`; `RT:170` `invalidateQueries` com `queryKeys.period('m-1', october)`; `PT:274-275`; `QT:204` | ✅ PASS |
| FEC-06 AC1 aviso | `spec.md:136` | `PT:255`, `PT:256`, `PT:260`; `RT:181`, `RT:223`, `RT:253`, `RT:242` | ✅ PASS |
| FEC-06 AC2 `409` relê | `spec.md:137` | `RT:182-183`, `RT:195-196`; `RT:211-212`; `RT:224` sem releitura fora do 409 | ✅ PASS |
| FEC-06 AC3 ocupado | `spec.md:138` | `RT:269-272`, `RT:285-288` | ✅ PASS |
| FEC-07 AC1 botão | `spec.md:152` | `RT:61`, `RT:71`, `RT:94`, `RT:212` | ✅ PASS |
| FEC-07 AC2 arquivo | `spec.md:153` | `RT:239` `[{ name: 'reconpay-2026-10-01-2026-10-31.csv', href: 'blob:csv' }]`; `RT:240` `exportRunCsv('jwt','m-1','r-9')`; `RT:241` | ✅ PASS |
| FEC-08 AC1 troca de empresa | `spec.md:167` | Sem cache: `PT:292-298`. Com cache: `PT:322-325`. M27 morto | ✅ PASS |
| FEC-08 AC2 sem empresa | `spec.md:168` | `PT:332` heading "Nenhuma empresa no seu acesso"; `PT:334` sem tabela; `PT:335` nenhuma chamada | ✅ PASS |

**Status**: ❌ 1 gap (FEC-01 AC1, janelas distintas com a mesma data final), só de teste. Nenhum spec-precision gap: `spec.md:80` pede uma linha por janela distinta, e a janela é o par de datas.

---

### Discrimination Sensor

Scratch: `git worktree add --detach <scratchpad>/verify-closing4b HEAD` com `frontend/node_modules` em symlink. Cada mutante rodou `vitest run lib/closing.test.ts components/closing components/dashboard` (212 testes, verdes no HEAD) e depois `git checkout --` só do arquivo de produção mutado. W1-W12 são diferentes de M1-M31, N1-N12 e V1-V17.

| # | File | Mutação | Killed? |
| --- | --- | --- | --- |
| V3 | `frontend/lib/closing.ts:24` | `seconds === null` → `!seconds` | ✅ Killed (1: `LT:52`) |
| V15 | `frontend/lib/closing.ts:32` | `hours ?` → `hours > 1 ?` | ✅ Killed (1: `LT:64`) |
| V16 | `frontend/lib/closing.ts:29` | `minutes ?` → `minutes > 1 ?` | ✅ Killed (1: `LT:59`) |
| N6 | `frontend/components/closing/PeriodRow.tsx:29` | `data = period.data` | ✅ Killed (3: `RT:199`) |
| N10 | `frontend/components/closing/ClosingPage.tsx:33` | estado vazio com `<= 1` janela | ✅ Killed (1: `PT:217`) |
| M27 | `frontend/components/closing/ClosingPage.tsx:97` | sem `key={merchantId}` | ✅ Killed (1: `PT:301`) |
| W1 | `frontend/lib/closing.ts:29` | horas com `Math.round` | ✅ Killed (1) |
| W2 | `frontend/lib/closing.ts:32` | dias com `Math.ceil` | ✅ Killed (2) |
| W3 | `frontend/lib/closing.ts:41` | `getDate()` → `getDay()` em "Travado em" | ✅ Killed (3) |
| W4 | `frontend/lib/closing.ts:18` | janela identificada só pela data final | ❌ Survived → Fix 1 |
| W5 | `frontend/components/closing/PeriodRow.tsx:36` | "Travar" chama a reabertura e "Destravar" chama a trava | ✅ Killed (12) |
| W6 | `frontend/components/closing/PeriodRow.tsx:39` | trava/reabertura com erro não chama `onError(mensagem)` | ✅ Killed (6) |
| W7 | `frontend/components/closing/PeriodRow.tsx:80` | "Exportar CSV" só na linha "Travado" | ✅ Killed (6) |
| W8 | `frontend/components/closing/ClosingPage.tsx:23` | total de páginas com `Math.floor` | ✅ Killed (3) |
| W9 | `frontend/components/dashboard/CloseStepper.tsx:27` | `Confirm` mostra o nome da ação em vez do título | ✅ Killed (2) |
| W10 | `frontend/components/dashboard/CloseStepper.tsx:30` | `Confirm`: botão da ação chama `onCancel` | ✅ Killed (22) |
| W11 | `frontend/components/closing/ClosingPage.tsx:43` | aviso sem `role="alert"` | ✅ Killed (3) |
| W12 | `frontend/components/closing/PeriodRow.tsx:66` | "Em aberto" sem os centavos | ✅ Killed (1) |

Sonda (só no scratch, apagada): um `it` a mais em `LT` com `{ 2026-10-01, 2026-10-31 }` e `{ 2026-10-16, 2026-10-31 }`, esperando as 2 janelas, a de 16/10 primeiro. No HEAD, 31/31 verdes. Com W4, o caso falha.

**Sensor depth**: expandido (a tela trava períodos, caminho de integridade do fechamento).
**Resultado do sensor (iteração 4)**: 17/18 killed, reprovado

Isolamento: `git status --porcelain` do worktree real vazio antes e depois. O symlink foi removido, e `git worktree remove --force` e `git worktree prune` foram feitos.

---

### Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ |
| Surgical changes | ✅ correção só em teste (3 linhas) |
| No scope creep | ✅ |
| Matches patterns | ✅ |
| Spec-anchored outcome check | ✅ os casos novos asserem o texto exato da regra de `spec.md:40` |
| Per-layer Coverage Expectation | ⚠️ `periodWindows` sem o caso de mesma data final (W4) |
| Every test maps to a spec requirement | ✅ `LT:67` cita FEC-02 AC4 |
| Documented guidelines followed: `AGENTS.md`, `frontend/AGENTS.md`, `.specs/LESSONS.md` | ✅ |

---

### Edge Cases

- [x] Vários runs por janela: `LT:18`, `PT:158`
- [x] Janela parcial: `RT:83`
- [x] `matchRate` nulo: `RT:77`
- [x] Durações 29/180/7200/7500/86400/97200: `LT:53-68`
- [x] Exatamente 10 janelas: `PT:204`
- [x] Erro não JSON: `frontend/lib/api.imports.test.ts:115`

---

### Gate Check

- **Gate command**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`
- **Resultado**: 860 passed, 0 failed, 0 skipped (65 arquivos); typecheck exit 0; build exit 0 (`/fechamento` estática)
- **Test count before feature**: 796
- **Test count after feature**: 860
- **Delta**: +64 (+3 nesta rodada, no `it.each` de `LT:51`)
- **Skipped tests**: nenhum
- **Failures**: nenhuma

---

### Fix Plans

#### Fix 1: `periodWindows` não prende o par de datas como identidade da janela

- **Root cause**: `LT:11-22` repete o mesmo par, e `LT:24-37` tem duas janelas com a mesma data inicial. Nenhum caso tem duas janelas com a mesma data final, então uma chave só com `toDate` passa.
- **Fix task**: em `frontend/lib/closing.test.ts`, no `describe('periodWindows')`, acrescentar um caso com `{ fromDate: '2026-10-01', toDate: '2026-10-31' }` e `{ fromDate: '2026-10-16', toDate: '2026-10-31' }` que espera `toEqual` com as 2 janelas, a de 16/10 primeiro. O caso fecha a classe junto com `LT:24-37`: as duas datas entram na identidade. Sem mudança de produção.
- **Done when**: o caso passa no HEAD e falha com W4 (`` windows.set(`${toDate}`, ...) `` em `closing.ts:18`).
- **Priority**: Minor (FEC-01 AC1; a janela de mesma data final é alcançável, mas o código atual está certo)

---

### Requirement Traceability Update

`spec.md:189-198` ainda está em "Design / Pending" e "0 mapped to tasks". O doc está desatualizado e o Verifier não o editou. Proposta:

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| FEC-01 | Pending | ❌ Needs Fix (Fix 1, só a identidade da janela de AC1) |
| FEC-02 | Pending | ✅ Verified |
| FEC-03 | Pending | ✅ Verified |
| FEC-04 | Pending | ✅ Verified |
| FEC-05 | Pending | ✅ Verified |
| FEC-06 | Pending | ✅ Verified |
| FEC-07 | Pending | ✅ Verified |
| FEC-08 | Pending | ✅ Verified |

Outros docs: `tasks.md` com **Status** "In Progress" e sem o Fix 1 da iteração 3 na tabela de tasks.

---

### Summary

**Overall (iteração 4)**: ❌ Not Ready (1 gap de teste, sem bug de produção)

**Spec-anchored check**: 31/32 linhas batem com a spec, 1 gap; 0 spec-precision gaps
**Sensor**: 17/18 mutations killed
**Gate**: 860 passed

**What works**: o gap da iteração 3 está fechado (V3, V15, V16), e os das iterações 1 e 2 seguem fechados (M27, N6, N10). A regra completa da duração está presa, inclusive 0 s e os restos de 1 min e 1 h. Os quatro status, as ações com confirmação, o ocupado, o export, a paginação, o aviso, o cache, sem empresa, sem runs e a troca de empresa também estão presos.

**Issues found**: Fix 1 (janelas com a mesma data final, W4).

**Next steps**: uma correção de teste de cerca de 9 linhas em `LT`. Outra iteração fica a critério do usuário.

## Veredito da iteração 3: reprovada

**Date**: 2026-10-17
**Spec**: `.specs/features/closing-screen/spec.md`
**Diff range**: `03b25e9^..40d5721` (14 commits), branch `feat/closing-screen`. Correções desta rodada: `f308f38..40d5721` (`16f1337` e `40d5721`, só teste).
**Verifier**: sub-agente independente (author ≠ verifier), iteração 3 (histórico; o veredito vigente é o da iteração 5, no topo).

Os dois gaps da iteração 2 estão fechados. O `it.each` novo `RT:199-213` mata N6 (3 casos falham), e o caso novo `PT:217-227` mata N10. M27 continua morto (`PT:301`). As correções não mudam nenhum arquivo de produção e não enfraquecem nem removem teste: `git diff --numstat f308f38..40d5721` dá +16/-0 em `PeriodRow.test.tsx` e +12/-0 em `ClosingPage.test.tsx`, e nada mais. Gate verde: 857 testes, typecheck e build.

O sensor desta rodada injetou 20 mutantes: N6, N10 e M27 de novo, mais 17 novos (V1-V17). Três sobreviveram, todos em `formatDuration` (`frontend/lib/closing.ts:22-33`), regra definida em `spec.md:40` e cobrada por FEC-02 AC4 (`spec.md:83`):

- **V3** (`closing.ts:24`, `seconds === null` → `!seconds`): `0` segundos vira "—" em vez de "0 s". A spec diz "abaixo de 60 → 'N s'" e "nulo → '—'", e `0` não é nulo. O valor é alcançável: o backend calcula `Duration.between(run.getFinishedAt(), lock.getLockedAt()).toSeconds()` (`backend/src/main/java/br/com/hanrry/reconpay/reconciliation/service/PeriodService.java:137`), e uma trava menos de 1 s depois do fim do run dá `0`. Com o mutante, uma linha "Travado" mostraria "—" em Duração, como uma linha aberta.
- **V15** (`closing.ts:32`, `hours ?` → `hours > 1 ?`): `90000` vira "1 d" em vez de "1 d 1 h".
- **V16** (`closing.ts:29`, `minutes ?` → `minutes > 1 ?`): `3660` vira "1 h" em vez de "1 h 1 min".

`LT:51-63` testa as bordas das faixas e os 6 valores da edge, mas nenhum valor com 0 s, 1 min de resto ou 1 h de resto. Uma sonda no scratch acrescentou `[0, '0 s']`, `[3660, '1 h 1 min']` e `[90000, '1 d 1 h']` ao `it.each`. A sonda passa no HEAD e mata V3, V15 e V16, um caso cada. O scratch foi apagado depois.

Esta é a iteração 3, a última do laço. Os gaps restantes são só de teste, sem bug de produção. A decisão de rodar mais uma correção é do usuário.

Abreviações: `LT` = `frontend/lib/closing.test.ts`, `RT` = `frontend/components/closing/PeriodRow.test.tsx`, `PT` = `frontend/components/closing/ClosingPage.test.tsx`, `CST` = `frontend/components/dashboard/CloseStepper.test.tsx`, `QT` = `frontend/lib/queries.test.tsx`.

---

### Task Completion

| Task | Status | Notes |
| --- | --- | --- |
| T1 | ✅ Done | `13ccbc7` |
| T2 | ✅ Done | `341c0d8` |
| T3 | ✅ Done | `ef4a69e` |
| T4 | ✅ Done | `c7895bb` |
| T5 | ✅ Done | `5f6c23b` |
| T6 | ✅ Done | `8cb65c2`, `ecb479d` |
| Fix 1 (iteração 1) | ✅ Done | `3289be0`: `PT:301-326` |
| Fix 1 (iteração 2) | ✅ Done | `16f1337`: `RT:199-213` |
| Fix 2 (iteração 2) | ✅ Done | `40d5721`: `PT:217-227` |

---

### Spec-Anchored Acceptance Criteria

Re-derivado no HEAD `40d5721`. As linhas de `RT` depois de 198 andaram +16 e as de `PT` depois de 216 andaram +12. As citações abaixo já usam os números novos.

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FEC-01 AC1 título, empresa, tabela | `spec.md:80` | `PT:154` `findByRole('table', { name: 'Períodos' })`; `PT:155` `within(pageHeader()).getByText('Alfa Pagamentos')`; `PT:158-163` 4 linhas para 5 runs | ✅ PASS |
| FEC-01 AC2 ordem com empate | `spec.md:81` | `LT:31-36` `toEqual([...])` com empate pela data final; `PT:158-163` | ✅ PASS |
| FEC-01 AC3 colunas e nome | `spec.md:82` | `PT:156-157` `toEqual(['Período','Status','Match','Em aberto','Travado em','Duração','Ações'])`; `RT:59` `'Outubro de 2026'` | ✅ PASS |
| FEC-01 edge vários runs | `spec.md:176` | `LT:18-21`; `PT:158` | ✅ PASS |
| FEC-01 edge janela parcial | `spec.md:177` | `RT:83` `toBe('01/10/2026 – 15/10/2026')` | ✅ PASS |
| FEC-02 AC4 `200` aberto | `spec.md:83`, `spec.md:39` | `RT:59` `toEqual(['Outubro de 2026','Aberto','94,2%','R$ 10,50','—','—'])` | ✅ PASS |
| FEC-02 AC4 `200` travado | `spec.md:83` | `RT:70` `[..., 'Travado','100%','R$ 0,00','02/11/2026 08:07','2 h 5 min']`; `LT:71` `'05/09/2026 08:07'`; `LT:75` `'—'` | ✅ PASS |
| FEC-02 AC4 formato da duração (regra completa) | `spec.md:40`: "N s" abaixo de 60 (inclusive 0), "N h M min" com M ≥ 1, "N d H h" com H ≥ 1, nulo "—" | `LT:51-66` `toBe(text)` para 29, 59, 60, 180, 3599, 3600, 7200, 7500, 86399, 86400, 97200 e nulo. **Sem 0 s, sem resto de 1 min, sem resto de 1 h**: V3, V15 e V16 sobrevivem | ❌ GAP |
| FEC-02 AC5 `409` | `spec.md:84`: "Em andamento", "—" nos valores, nenhuma ação | Primeira leitura: `RT:93` `toEqual(['Outubro de 2026', label, '—','—','—','—'])`, `RT:94` `buttons()` = `[]`. Releitura depois de um `200`: `RT:211` `cells()` = `['Outubro de 2026','Em andamento','—','—','—','—']`, `RT:212` `buttons()` = `[]`. N6 morto | ✅ PASS |
| FEC-02 AC6 `404` | `spec.md:85` | `RT:93-94` (caso 404); `RT:211-212` releitura 404 com `'Sem conciliação concluída'` e `[]`; `PT:162` | ✅ PASS |
| FEC-02 edge `matchRate` nulo | `spec.md:178` | `RT:77` `toBe('0%')` | ✅ PASS |
| FEC-02 edge duração | `spec.md:179` | `LT:52`, `LT:55`, `LT:58`, `LT:59`, `LT:61`, `LT:62` `toBe(text)` com os 6 valores | ✅ PASS |
| FEC-03 AC1 paginação | `spec.md:99` | `PT:175`, `PT:178-180`, `PT:185-188`; `LT:46` `toBe(10)` | ✅ PASS |
| FEC-03 edge 10 janelas | `spec.md:180` | `PT:203-204` 10 linhas e `navigation` nulo | ✅ PASS |
| FEC-03 AC2 sem runs | `spec.md:100`: aviso e link só sem runs | `PT:211-213` (zero janelas); borda de uma janela: `PT:224` `rowText()` = `['Outubro de 2026|Aberto']`, `PT:225` aviso nulo, `PT:226` paginação nula. N10 morto | ✅ PASS |
| FEC-03 AC3 erro dos runs | `spec.md:101` | `PT:234` `toHaveTextContent('Você não tem acesso a este comerciante')`; `PT:237-240` | ✅ PASS |
| FEC-03 AC4 leitura só da página | `spec.md:102` | `PT:181`, `PT:189` | ✅ PASS |
| FEC-03 AC5 erro da linha | `spec.md:103` | `RT:104` `'Não carregou'`; `RT:105` `['Tentar novamente']`; `RT:108-110` releitura só da janela; releitura 403 depois de um `200`: `RT:211` `'Não carregou'` e "—", `RT:212` `['Tentar novamente']` | ✅ PASS |
| FEC-03 edge erro não JSON | `spec.md:181` | `frontend/lib/api.imports.test.ts:115` `toBe('A API retornou erro 502.')` | ✅ PASS |
| FEC-04 AC1 botões por status | `spec.md:117` | `RT:61` `['Travar','Exportar CSV']`; `RT:71` `['Destravar','Exportar CSV']` | ✅ PASS |
| FEC-04 AC2 confirmação | `spec.md:118` | `RT:119` `alertdialog` "Travar o período?"; `RT:120` `not.toHaveBeenCalled()`; `CST:115-122` | ✅ PASS |
| FEC-04 AC5 cancelar | `spec.md:121` | `RT:155-156` | ✅ PASS |
| FEC-05 AC3 travar | `spec.md:119` | `RT:123` `toHaveBeenCalledWith('jwt','m-1',october)`; `RT:124`; `PT:273` `toEqual([october])` | ✅ PASS |
| FEC-05 AC4 destravar | `spec.md:120` | `RT:136`, `RT:140`, `RT:141` `[..., 'Aberto','100%','R$ 0,00','—','—']` | ✅ PASS |
| FEC-05 AC6 cache | `spec.md:122` | `RT:169` `toEqual(answer)`; `RT:170` `invalidateQueries` com `queryKeys.period('m-1', october)`; `PT:274-275`; `QT:204` | ✅ PASS |
| FEC-06 AC1 aviso | `spec.md:136` | `PT:255`, `PT:256`, `PT:260`; `RT:181`, `RT:223`, `RT:253`, `RT:242` | ✅ PASS |
| FEC-06 AC2 `409` relê | `spec.md:137`: relê e mostra o estado atual | `RT:182-183`, `RT:195-196` (releitura `200`); `RT:211-212` (releitura 409, 404 e 403); `RT:224` sem releitura fora do 409 | ✅ PASS |
| FEC-06 AC3 ocupado | `spec.md:138` | `RT:269-272`, `RT:285-288` | ✅ PASS |
| FEC-07 AC1 botão | `spec.md:152` | `RT:61`, `RT:71`, `RT:94`, `RT:212` | ✅ PASS |
| FEC-07 AC2 arquivo | `spec.md:153` | `RT:239` `[{ name: 'reconpay-2026-10-01-2026-10-31.csv', href: 'blob:csv' }]`; `RT:240` `exportRunCsv('jwt','m-1','r-9')`; `RT:241` | ✅ PASS |
| FEC-08 AC1 troca de empresa | `spec.md:167` | Sem cache: `PT:292-298`. Com cache: `PT:322` sem skeleton, `PT:323` `'Página 1 de 2'`, `PT:324` `'Outubro de 2026|Aberto'`, `PT:325` alerta nulo. M27 morto | ✅ PASS |
| FEC-08 AC2 sem empresa | `spec.md:168` | `PT:332` heading "Nenhuma empresa no seu acesso"; `PT:334` sem tabela; `PT:335` nenhuma chamada | ✅ PASS |

**Status**: ❌ 1 gap (FEC-02 AC4, formato da duração fora dos 6 valores da edge), só de teste. Nenhum spec-precision gap: `spec.md:40` define a regra para todo valor.

---

### Discrimination Sensor

Scratch: `git worktree add --detach <scratchpad>/verify-closing3 HEAD` com `frontend/node_modules` em symlink. Cada mutante rodou `vitest run lib/closing.test.ts components/closing components/dashboard` (209 testes, verdes no HEAD) e depois `git checkout --` só do arquivo de produção mutado. V1-V17 são diferentes de M1-M31 (iteração 1) e N1-N12 (iteração 2). Um mutante de `formatLockedAt` com `getUTCHours` ficou de fora: o resultado depende do fuso da máquina, porque a suíte não fixa `TZ`.

| # | File | Mutação | Killed? |
| --- | --- | --- | --- |
| N6 | `frontend/components/closing/PeriodRow.tsx:29` | `data = period.data` (dado antigo em 409/404/erro) | ✅ Killed (3: `RT:199` 409, 404, 403) |
| N10 | `frontend/components/closing/ClosingPage.tsx:33` | estado vazio com `<= 1` janela | ✅ Killed (1: `PT:217`) |
| M27 | `frontend/components/closing/ClosingPage.tsx:97` | sem `key={merchantId}` | ✅ Killed (1: `PT:301`) |
| V1 | `frontend/lib/closing.ts:26` | minutos com `Math.ceil` | ✅ Killed (1) |
| V2 | `frontend/lib/closing.ts:48` | erro que não é `ApiError` vira `'none'` | ✅ Killed (1) |
| V3 | `frontend/lib/closing.ts:24` | `seconds === null` → `!seconds` (0 s vira "—") | ❌ Survived → Fix 1 |
| V4 | `frontend/components/closing/PeriodRow.tsx:56` | export com erro não chama `onError(mensagem)` | ✅ Killed (1) |
| V5 | `frontend/components/closing/PeriodRow.tsx:40` | relê em todo erro (`>= 400`), não só no 409 | ✅ Killed (1) |
| V6 | `frontend/components/closing/PeriodRow.tsx:33` | sem `setConfirming(null)` ao confirmar | ✅ Killed (4) |
| V7 | `frontend/components/closing/PeriodRow.tsx:75` | "Travar" sem `disabled={busy}` | ✅ Killed (2) |
| V8 | `frontend/components/closing/PeriodRow.tsx:71` | "Tentar novamente" também em "Em andamento" | ✅ Killed (2) |
| V9 | `frontend/components/closing/ClosingPage.tsx:59` | "Página X de {windows.length}" | ✅ Killed (3) |
| V10 | `frontend/components/closing/ClosingPage.tsx:53` | `onError` da linha ignorado (sem aviso) | ✅ Killed (3) |
| V11 | `frontend/components/closing/ClosingPage.tsx:24` | fatia começa uma janela depois | ✅ Killed (9) |
| V12 | `frontend/components/closing/ClosingPage.tsx:95` | erro dos runs com texto fixo, sem a mensagem da API | ✅ Killed (1) |
| V13 | `frontend/components/dashboard/CloseStepper.tsx:29` | `Confirm`: "Cancelar" chama `onConfirm` | ✅ Killed (4) |
| V14 | `frontend/components/dashboard/CloseStepper.tsx:26` | `Confirm`: `aria-label={action}` em vez do título | ✅ Killed (5) |
| V15 | `frontend/lib/closing.ts:32` | `hours ?` → `hours > 1 ?` (1 d 1 h vira "1 d") | ❌ Survived → Fix 1 |
| V16 | `frontend/lib/closing.ts:29` | `minutes ?` → `minutes > 1 ?` (1 h 1 min vira "1 h") | ❌ Survived → Fix 1 |
| V17 | `frontend/components/closing/ClosingPage.tsx:97` | sem o nome da empresa no cabeçalho | ✅ Killed (3) |

Sonda (só no scratch, apagada): `LT:51` com mais 3 linhas, `[0, '0 s']`, `[3660, '1 h 1 min']` e `[90000, '1 d 1 h']`. No HEAD, 30/30 verdes. V3, V15 e V16 caem, cada um num caso.

**Sensor depth**: expandido (a tela trava períodos, caminho de integridade do fechamento).
**Resultado do sensor (iteração 3)**: 17/20 killed, reprovado

Isolamento: `git status --porcelain` do worktree real vazio antes e depois. O symlink foi removido, e `git worktree remove --force` e `git worktree prune` foram feitos.

---

### Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ |
| Surgical changes | ✅ correções só em teste |
| No scope creep | ✅ |
| Matches patterns | ✅ |
| Spec-anchored outcome check | ✅ nos casos cobertos; os casos novos asserem as células e os botões exatos |
| Per-layer Coverage Expectation | ⚠️ `formatDuration` sem 0 s, resto de 1 min e resto de 1 h |
| Every test maps to a spec requirement | ✅ `RT:203` cita FEC-02 AC5/AC6, FEC-03 AC5 e FEC-06 AC2; `PT:217` cita FEC-03 AC2 |
| Documented guidelines followed: `AGENTS.md`, `frontend/AGENTS.md`, `.specs/LESSONS.md` | ✅ |

O caso 403 de `RT:202` substitui o 500 sugerido na iteração 2. A troca é válida: o cliente repete um 5xx uma vez, e 403 cai no mesmo ramo `'error'` (`frontend/lib/closing.ts:48`). Testes não enfraquecidos: `git diff f308f38..40d5721` só adiciona linhas.

---

### Edge Cases

- [x] Vários runs por janela: `LT:18`, `PT:158`
- [x] Janela parcial: `RT:83`
- [x] `matchRate` nulo: `RT:77`
- [x] Durações 29/180/7200/7500/86400/97200: `LT:51-66`
- [x] Exatamente 10 janelas: `PT:204`
- [x] Erro não JSON: `frontend/lib/api.imports.test.ts:115`

---

### Gate Check

- **Gate command**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`
- **Resultado**: 857 passed, 0 failed, 0 skipped; typecheck exit 0; build exit 0 (`/fechamento` estática)
- **Test count before feature**: 796
- **Test count after feature**: 857
- **Delta**: +61 (+4 nesta rodada: 3 casos em `RT:199` e 1 em `PT:217`)
- **Skipped tests**: nenhum
- **Failures**: nenhuma

---

### Fix Plans

#### Fix 1: `formatDuration` não prende 0 s nem os restos de 1 min e 1 h

- **Root cause**: o `it.each` de `LT:51-63` tem as bordas das faixas e os 6 valores da edge, mas nenhum valor que separe `seconds === null` de `!seconds`, nem `minutes`/`hours` de `> 1`.
- **Fix task**: em `frontend/lib/closing.test.ts`, no `it.each` de `formatDuration`, acrescentar `[0, '0 s']`, `[3660, '1 h 1 min']` e `[90000, '1 d 1 h']`. Sem mudança de produção.
- **Done when**: os 3 casos passam no HEAD e cada um falha com V3 (`!seconds` em `closing.ts:24`), V16 (`minutes > 1` em `closing.ts:29`) e V15 (`hours > 1` em `closing.ts:32`).
- **Priority**: Minor (FEC-02 AC4, formato de Duração; o 0 é alcançável numa trava menos de 1 s depois do fim do run)

---

### Requirement Traceability Update

`spec.md:189-198` ainda está em "Design / Pending" e "0 mapped to tasks". O doc está desatualizado e o Verifier não o editou. Proposta:

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| FEC-01 | Pending | ✅ Verified |
| FEC-02 | Pending | ❌ Needs Fix (Fix 1, só a regra de duração de AC4) |
| FEC-03 | Pending | ✅ Verified |
| FEC-04 | Pending | ✅ Verified |
| FEC-05 | Pending | ✅ Verified |
| FEC-06 | Pending | ✅ Verified |
| FEC-07 | Pending | ✅ Verified |
| FEC-08 | Pending | ✅ Verified |

Outros docs: `tasks.md` com **Status** "In Progress".

---

### Summary

**Overall (iteração 3)**: ❌ Not Ready (1 gap de teste, sem bug de produção; laço de 3 iterações esgotado, decisão do usuário)

**Spec-anchored check**: 31/32 linhas batem com a spec, 1 gap; 0 spec-precision gaps
**Sensor**: 17/20 mutations killed
**Gate**: 857 passed

**What works**: os gaps das iterações 1 e 2 estão fechados (M27, N6, N10). Os quatro status funcionam na primeira leitura e na releitura. Funcionam também as ações com confirmação, o ocupado, o export, a paginação com a borda de 1 e de 10 janelas, o aviso, o cache, sem empresa, sem runs e a troca de empresa com e sem cache.

**Issues found**: Fix 1 (formato da duração com 0 s e restos de 1 min e 1 h: V3, V15, V16).

**Next steps**: escalar ao usuário. A correção é de 3 linhas de teste, e uma iteração 4 fica a critério dele.

---

## Veredito da iteração 2: reprovada

**Date**: 2026-10-17
**Spec**: `.specs/features/closing-screen/spec.md`
**Diff range**: `03b25e9^..ecb479d` (11 commits), branch `feat/closing-screen`. Correções desta rodada: `df7cfe7..ecb479d` (`3289be0` só teste, `ecb479d` só `README.md`).
**Verifier**: sub-agente independente (author ≠ verifier), iteração 2 (histórico; o veredito vigente é o da iteração 5, no topo).

O gap da iteração 1 está fechado: o caso novo `PT:289-314` mata M27. As correções não mudam nenhum arquivo de produção e não enfraquecem nem removem teste (`git diff df7cfe7..ecb479d`: +27 linhas em `ClosingPage.test.tsx`, só um `it` novo; 1 linha em `README.md`). Gate verde: 853 testes, typecheck e build.

O sensor desta rodada injetou 13 mutantes (M27 de novo e 12 novos). 11 morreram e 2 sobreviveram:

- **N6** (`frontend/components/closing/PeriodRow.tsx:29`, `const data = period.data`): a linha que já leu um período e depois relê com `409` mostra "Em andamento" com os valores antigos e o botão "Exportar CSV". A spec pede "—" e nenhuma ação (FEC-02 AC5). O caso acontece em FEC-06 AC2: a trava devolve `409` porque um run começou, e a releitura também devolve `409`. Nenhum teste da linha cobre uma releitura com erro depois de um `200`. `LT:93` cobre só a regra pura `periodStatus`, não as colunas e os botões.
- **N10** (`frontend/components/closing/ClosingPage.tsx:33`, `windows.length === 0` → `<= 1`): uma empresa com uma janela só mostra "Nenhuma conciliação ainda." no lugar da tabela. Nenhum teste da página usa uma empresa com exatamente uma janela.

Uma sonda confirmou os dois. No scratch, foi um caso a mais em `RT` (200, depois trava 409 e releitura 409) e outro em `PT` (`monthRuns('m-1', 1)`). As duas sondas passam no HEAD e falham com N6 e N10. O scratch foi apagado depois.

Abreviações: `LT` = `frontend/lib/closing.test.ts`, `RT` = `frontend/components/closing/PeriodRow.test.tsx`, `PT` = `frontend/components/closing/ClosingPage.test.tsx`, `CST` = `frontend/components/dashboard/CloseStepper.test.tsx`, `QT` = `frontend/lib/queries.test.tsx`.

---

### Task Completion

| Task | Status | Notes |
| --- | --- | --- |
| T1 | ✅ Done | `13ccbc7` |
| T2 | ✅ Done | `341c0d8` |
| T3 | ✅ Done | `ef4a69e` |
| T4 | ✅ Done | `c7895bb` |
| T5 | ✅ Done | `5f6c23b` |
| T6 | ✅ Done | `8cb65c2`, `ecb479d` |
| Fix 1 (iteração 1) | ✅ Done | `3289be0`: `PT:289-314` |

---

### Spec-Anchored Acceptance Criteria

Re-derivado nesta rodada. As linhas citadas de `LT`, `RT`, `CST`, `QT` e `PT` até a linha 288 não mudaram desde `df7cfe7`, e todas foram relidas no HEAD.

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FEC-01 AC1 título, empresa, tabela | `spec.md:80` | `PT:154` `findByRole('table', { name: 'Períodos' })`; `PT:155` `within(pageHeader()).getByText('Alfa Pagamentos')`; `PT:158-163` 4 linhas para 5 runs | ✅ PASS |
| FEC-01 AC2 ordem com empate | `spec.md:81` | `LT:31-36` `toEqual([...])` com empate pela data final; `PT:158-163` | ✅ PASS |
| FEC-01 AC3 colunas e nome | `spec.md:82` | `PT:156-157` `toEqual(['Período','Status','Match','Em aberto','Travado em','Duração','Ações'])`; `RT:59` `'Outubro de 2026'` | ✅ PASS |
| FEC-01 edge vários runs | `spec.md:176` | `LT:18-21`; `PT:158` | ✅ PASS |
| FEC-01 edge janela parcial | `spec.md:177` | `RT:83` `toBe('01/10/2026 – 15/10/2026')` | ✅ PASS |
| FEC-02 AC4 `200` aberto | `spec.md:83`, `spec.md:39` | `RT:59` `toEqual(['Outubro de 2026','Aberto','94,2%','R$ 10,50','—','—'])` | ✅ PASS |
| FEC-02 AC4 `200` travado | `spec.md:83` | `RT:70` `[..., 'Travado','100%','R$ 0,00','02/11/2026 08:07','2 h 5 min']`; `LT:71` `'05/09/2026 08:07'`; `LT:75` `'—'` | ✅ PASS |
| FEC-02 AC5 `409` | `spec.md:84`: "Em andamento", "—" nos valores, nenhuma ação | Primeira leitura: `RT:93` `toEqual(['Outubro de 2026', label, '—','—','—','—'])`, `RT:94` `buttons()` = `[]`. **Releitura `409` depois de um `200`: sem evidência na linha.** `LT:93` cobre só `periodStatus`. N6 sobrevive. | ❌ GAP |
| FEC-02 AC6 `404` | `spec.md:85` | `RT:93-94` (caso 404 do `it.each`); `PT:162`. A mesma lacuna de N6 vale para a releitura `404` depois de um `200` (menos provável: o run concluído some só ao ser substituído por um run com falha) | ⚠️ coberto na primeira leitura (ver Fix 1) |
| FEC-02 edge `matchRate` nulo | `spec.md:178` | `RT:77` `toBe('0%')` | ✅ PASS |
| FEC-02 edge duração | `spec.md:179`, `spec.md:40` | `LT:51-66` `it.each` com 29, 180, 7200, 7500, 86400, 97200, as bordas e nulo, `toBe(text)` | ✅ PASS |
| FEC-03 AC1 paginação | `spec.md:99` | `PT:175`, `PT:178-180`, `PT:185-188`; `LT:46` `toBe(10)` | ✅ PASS |
| FEC-03 edge 10 janelas | `spec.md:180` | `PT:203-204` 10 linhas e `navigation` nulo | ✅ PASS |
| FEC-03 AC2 sem runs | `spec.md:100`: aviso e link **só** sem runs | `PT:211-213` (zero janelas). **Borda de uma janela sem evidência**: nenhuma rota com exatamente 1 janela. N10 sobrevive. | ❌ GAP |
| FEC-03 AC3 erro dos runs | `spec.md:101` | `PT:222` `toHaveTextContent('Você não tem acesso a este comerciante')`; `PT:225-228` | ✅ PASS |
| FEC-03 AC4 leitura só da página | `spec.md:102` | `PT:181`, `PT:189` | ✅ PASS |
| FEC-03 AC5 erro da linha | `spec.md:103` | `RT:104` `'Não carregou'`; `RT:105` `['Tentar novamente']`; `RT:108-110` releitura só da janela | ✅ PASS |
| FEC-03 edge erro não JSON | `spec.md:181` | `frontend/lib/api.imports.test.ts:115` `toBe('A API retornou erro 502.')` | ✅ PASS |
| FEC-04 AC1 botões por status | `spec.md:117` | `RT:61` `['Travar','Exportar CSV']`; `RT:71` `['Destravar','Exportar CSV']` | ✅ PASS |
| FEC-04 AC2 confirmação | `spec.md:118` | `RT:119` `alertdialog` "Travar o período?"; `RT:120` `not.toHaveBeenCalled()`; `CST:115-122` | ✅ PASS |
| FEC-04 AC5 cancelar | `spec.md:121` | `RT:155-156` | ✅ PASS |
| FEC-05 AC3 travar | `spec.md:119` | `RT:123` `toHaveBeenCalledWith('jwt','m-1',october)`; `RT:124`; `PT:261` `toEqual([october])` | ✅ PASS |
| FEC-05 AC4 destravar | `spec.md:120` | `RT:136`, `RT:140`, `RT:141` `[..., 'Aberto','100%','R$ 0,00','—','—']` | ✅ PASS |
| FEC-05 AC6 cache | `spec.md:122` | `RT:169` `toEqual(answer)`; `RT:170` `invalidateQueries` com `queryKeys.period('m-1', october)`; `PT:262-263`; `QT:204` | ✅ PASS |
| FEC-06 AC1 aviso | `spec.md:136` | `PT:243`, `PT:244`, `PT:248`; `RT:181`, `RT:207`, `RT:237`, `RT:226` | ✅ PASS |
| FEC-06 AC2 `409` relê | `spec.md:137`: relê e mostra o estado atual | `RT:182-183`, `RT:195-196` (releitura `200`); `RT:208`. O estado atual `409` na releitura não é asserido (N6) | ⚠️ parcial (ver Fix 1) |
| FEC-06 AC3 ocupado | `spec.md:138` | `RT:253-256`, `RT:269-272` | ✅ PASS |
| FEC-07 AC1 botão | `spec.md:152` | `RT:61`, `RT:71`, `RT:94` | ✅ PASS |
| FEC-07 AC2 arquivo | `spec.md:153` | `RT:223` `[{ name: 'reconpay-2026-10-01-2026-10-31.csv', href: 'blob:csv' }]`; `RT:224` `exportRunCsv('jwt','m-1','r-9')`; `RT:225` | ✅ PASS |
| FEC-08 AC1 troca de empresa | `spec.md:167`: página 1 da nova empresa, aviso limpo | Sem cache: `PT:280-286`. Com cache (gap da iteração 1): `PT:310` sem skeleton, `PT:311` `getByText('Página 1 de 2')`, `PT:312` `toBe('Outubro de 2026|Aberto')`, `PT:313` `queryByRole('alert')` nulo. M27 morto | ✅ PASS |
| FEC-08 AC2 sem empresa | `spec.md:168` | `PT:320` heading "Nenhuma empresa no seu acesso"; `PT:322` sem tabela; `PT:323` nenhuma chamada | ✅ PASS |

**Status**: ❌ 2 gaps (FEC-02 AC5 na releitura e FEC-03 AC2 na borda de uma janela), ambos de teste, nenhum bug de produção. Nenhum spec-precision gap.

---

### Discrimination Sensor

Scratch: `git worktree add --detach <scratchpad>/verify-closing2 HEAD` com `frontend/node_modules` em symlink. Cada mutante rodou `vitest run lib/closing.test.ts components/closing components/dashboard` (205 testes, verdes no HEAD) e depois `git checkout --` só do arquivo de produção mutado. N1-N12 são diferentes dos mutantes M1-M31 da iteração 1.

| # | File | Mutação | Killed? |
| --- | --- | --- | --- |
| M27 | `frontend/components/closing/ClosingPage.tsx:97` | sem `key={merchantId}` | ✅ Killed (1: `PT:289`) |
| N1 | `frontend/lib/closing.ts:26` | minutos com `Math.round` | ✅ Killed (1) |
| N2 | `frontend/lib/closing.ts:28` | minutos sem `% 60` | ✅ Killed (5) |
| N3 | `frontend/lib/closing.ts:31` | horas sem `% 24` | ✅ Killed (2) |
| N4 | `frontend/lib/closing.ts:41` | minutos de "Travado em" sem `pad` | ✅ Killed (3) |
| N5 | `frontend/lib/closing.ts:51` | sem leitura vira `'none'` em vez de `'loading'` | ✅ Killed (1) |
| N6 | `frontend/components/closing/PeriodRow.tsx:29` | `data = period.data` (dado antigo em 409/404/erro) | ❌ Survived → Fix 1 |
| N7 | `frontend/components/closing/PeriodRow.tsx:74` | "Travar" também em "Travado" | ✅ Killed (1) |
| N8 | `frontend/components/closing/PeriodRow.tsx:72` | "Tentar novamente" sem `refetch` | ✅ Killed (1) |
| N9 | `frontend/components/closing/ClosingPage.tsx:59` | "Página {page} de" (sem `+ 1`) | ✅ Killed (3) |
| N10 | `frontend/components/closing/ClosingPage.tsx:33` | estado vazio com `<= 1` janela | ❌ Survived → Fix 2 |
| N11 | `frontend/components/closing/ClosingPage.tsx:95` | "Tentar novamente" dos runs sem `refetch` | ✅ Killed (1) |
| N12 | `frontend/lib/closing.ts:19` | data inicial ascendente | ✅ Killed (6) |

Sondas (só no scratch, apagadas): `RT`, com `getPeriod` 200 e depois 409 e `lockPeriod` 409, espera `cells()` = `['Outubro de 2026','Em andamento','—','—','—','—']` e `buttons()` = `[]`. `PT`, com `monthRuns('m-1', 1)`, espera `rowText()` = `['Outubro de 2026|Aberto']` e nenhum "Nenhuma conciliação ainda.". HEAD: 2/2 verdes. N6 e N10: cada um falha na sua sonda.

**Sensor depth**: expandido (a tela trava períodos, caminho de integridade do fechamento).
**Resultado do sensor (iteração 2)**: 11/13 killed, reprovado

Isolamento: `git status --porcelain` do worktree real vazio antes e depois; symlink removido, `git worktree remove --force` + `git worktree prune` feitos.

---

### Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ |
| Surgical changes | ✅ correções só em teste e README |
| No scope creep | ✅ |
| Matches patterns | ✅ |
| Spec-anchored outcome check | ✅ nos casos cobertos |
| Per-layer Coverage Expectation | ⚠️ N6 (releitura com erro) e N10 (borda de 1 janela) |
| Every test maps to a spec requirement | ✅ `PT:289` cita FEC-08 AC1 |
| Documented guidelines followed: `AGENTS.md`, `frontend/AGENTS.md`, `.specs/LESSONS.md` | ✅ |

Testes não enfraquecidos: `git diff df7cfe7..ecb479d -- '*.test.*'` só adiciona linhas.

---

### Edge Cases

- [x] Vários runs por janela: `LT:18`, `PT:158`
- [x] Janela parcial: `RT:83`
- [x] `matchRate` nulo: `RT:77`
- [x] Durações 29/180/7200/7500/86400/97200: `LT:51-66`
- [x] Exatamente 10 janelas: `PT:204`
- [x] Erro não JSON: `frontend/lib/api.imports.test.ts:115`

---

### Gate Check

- **Gate command**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`
- **Resultado**: 853 passed, 0 failed, 0 skipped; typecheck exit 0; build exit 0 (`/fechamento` estática)
- **Test count before feature**: 796
- **Test count after feature**: 853
- **Delta**: +57 (+1 nesta rodada: `PT:289`)
- **Skipped tests**: nenhum
- **Failures**: nenhuma

---

### Fix Plans

#### Fix 1: a linha não prende o estado de uma releitura com erro depois de um `200`

- **Root cause**: React Query mantém `data` quando a releitura falha. Só `PeriodRow.tsx:29` (`data` só em `locked`/`open`) esconde os valores antigos e o "Exportar CSV". Os testes de `RT` com erro partem de uma linha sem dado, e a releitura de `RT:173-196` devolve `200`.
- **Fix task**: em `frontend/components/closing/PeriodRow.test.tsx`, criar um caso (ou `it.each` com 409 → "Em andamento" e `[]`, 404 → "Sem conciliação concluída" e `[]`, 500 → "Não carregou" e `['Tentar novamente']`): `getPeriod` resolve `period()` uma vez e depois rejeita com o status; `lockPeriod` rejeita com `conflict(...)`; confirmar a trava; asserir `cells()` = `['Outubro de 2026', <label>, '—','—','—','—']` e `buttons()` com o valor da spec. Sem mudança de produção.
- **Done when**: o caso passa no HEAD e falha com N6 (`const data = period.data` em `PeriodRow.tsx:29`).
- **Priority**: Major (FEC-02 AC5 com FEC-06 AC2)

#### Fix 2: a página não prende a borda de uma janela

- **Root cause**: as rotas de `PT` usam 0, 4, 10 ou 12 janelas; nenhuma tem exatamente 1.
- **Fix task**: em `frontend/components/closing/ClosingPage.test.tsx`, criar um caso com `server.runs = monthRuns('m-1', 1)` (sem `locked` e `failedOnly`) que espera `rowText()` = `['Outubro de 2026|Aberto']`, `queryByText('Nenhuma conciliação ainda.')` nulo e `queryByRole('navigation', { name: 'Paginação' })` nulo.
- **Done when**: o caso passa no HEAD e falha com N10 (`windows.length <= 1` em `ClosingPage.tsx:33`).
- **Priority**: Minor (borda de FEC-03 AC2 / FEC-01 AC1)

---

### Requirement Traceability Update

`spec.md:189-198` ainda está em "Design / Pending" e "0 mapped to tasks". O doc está desatualizado e o Verifier não o editou. Proposta:

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| FEC-01 | Pending | ✅ Verified |
| FEC-02 | Pending | ❌ Needs Fix (Fix 1) |
| FEC-03 | Pending | ❌ Needs Fix (Fix 2) |
| FEC-04 | Pending | ✅ Verified |
| FEC-05 | Pending | ✅ Verified |
| FEC-06 | Pending | ⚠️ Verified com Fix 1 pendente (AC2) |
| FEC-07 | Pending | ✅ Verified |
| FEC-08 | Pending | ✅ Verified |

Outros docs: `tasks.md` com **Status** "In Progress". O `README.md` já lista os quatro status (`ecb479d`).

---

### Summary

**Overall (iteração 2)**: ❌ Not Ready (2 gaps de teste, sem bug de produção)

**Spec-anchored check**: 27/31 linhas batem com a spec, 2 gaps e 2 parciais ligados ao Fix 1; 0 spec-precision gaps
**Sensor**: 11/13 mutations killed
**Gate**: 853 passed

**What works**: o gap da iteração 1 (M27) está fechado. Funcionam também as regras puras, os quatro status na primeira leitura, as ações com confirmação, o ocupado, o export, a paginação, o aviso, o cache, sem empresa, sem runs e a troca de empresa com e sem cache.

**Issues found**: Fix 1 (releitura com erro na linha, N6) e Fix 2 (borda de uma janela, N10).

**Next steps**: aplicar Fix 1 e Fix 2 e re-verificar (iteração 3, a última antes de escalar).

---

## Veredito da iteração 1: reprovada

**Date**: 2026-10-17
**Spec**: `.specs/features/closing-screen/spec.md`
**Diff range**: `03b25e9^..8cb65c2` (8 commits), branch `feat/closing-screen`. Código em `13ccbc7..8cb65c2`; backend sem diff (`git diff --stat` só lista `frontend/`, `README.md` e `.specs/`).
**Verifier**: sub-agente independente (author ≠ verifier), iteração 1 (histórico; o veredito vigente é o da iteração 5, no topo).

As 8 requisições (FEC-01..FEC-08) têm evidência `file:line` e os valores asseridos batem com a spec. Gate verde: 852 testes (796 antes, +56), typecheck e build. O sensor matou 30 de 31 mutantes. O mutante M27 sobreviveu: tirar `key={merchantId}` de `ClosingWindows` (`frontend/components/closing/ClosingPage.tsx:97`) não quebra nenhum teste. O teste de FEC-08 AC1 troca para uma empresa sem runs em cache, então a página passa pelo skeleton, desmonta `ClosingWindows` e zera página e aviso sem depender da `key`. Com os runs da nova empresa já em cache (por exemplo, Alfa → Beta → Alfa), a página 2 e o aviso continuam na tela. Uma sonda no scratch confirmou: o mesmo cenário com os runs de `m-2` semeados no cache passa no HEAD e falha com M27 ("Unable to find ... Página 1 de 2").

Abreviações: `LT` = `frontend/lib/closing.test.ts`, `RT` = `frontend/components/closing/PeriodRow.test.tsx`, `PT` = `frontend/components/closing/ClosingPage.test.tsx`, `CST` = `frontend/components/dashboard/CloseStepper.test.tsx`, `QT` = `frontend/lib/queries.test.tsx`.

---

### Task Completion

| Task | Status | Notes |
| --- | --- | --- |
| T1 | ✅ Done | `13ccbc7` |
| T2 | ✅ Done | `341c0d8` |
| T3 | ✅ Done | `ef4a69e` |
| T4 | ✅ Done | `c7895bb`; desvio registrado em `tasks.md` (remoção de `UnderConstruction.test.tsx` veio de T5) |
| T5 | ✅ Done | `5f6c23b`; nenhum import de `UnderConstruction` resta |
| T6 | ✅ Done | `8cb65c2` |

---

### Spec-Anchored Acceptance Criteria

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| FEC-01 AC1 título, empresa, tabela | `spec.md:80`: "Fechamento", nome da empresa, tabela "Períodos", uma linha por janela distinta | `frontend/components/closing/ClosingPage.test.tsx:154` `table()` = `findByRole('table', { name: 'Períodos' })`; `PT:155` `within(pageHeader()).getByText('Alfa Pagamentos')` (dentro do bloco do h1 "Fechamento", `PT:135`); `PT:158-163` 4 linhas para 5 runs (outubro com 2 runs vira 1 linha) | ✅ PASS |
| FEC-01 AC2 ordem com empate | `spec.md:81`: data inicial desc; empate pela final desc | `frontend/lib/closing.test.ts:31-36` `toEqual([10-01/10-31, 10-01/10-15, 09, 08])`; `PT:158-163` pela rota: "Outubro de 2026" antes de "01/10/2026 – 15/10/2026" | ✅ PASS |
| FEC-01 AC3 colunas e nome | `spec.md:82`: Período, Status, Match, Em aberto, Travado em, Duração, Ações; `formatWindow` | `PT:156-157` `toEqual(['Período','Status','Match','Em aberto','Travado em','Duração','Ações'])`; `RT:59` `cells()[0]` = `'Outubro de 2026'` | ✅ PASS |
| FEC-01 edge vários runs | `spec.md:176`: uma linha por janela | `LT:18-21` 4 runs → 2 janelas; `PT:158` 5 runs de `m-1` → 4 linhas | ✅ PASS |
| FEC-01 edge janela parcial | `spec.md:177`: "01/10/2026 – 15/10/2026" | `frontend/components/closing/PeriodRow.test.tsx:83` `toBe('01/10/2026 – 15/10/2026')` | ✅ PASS |
| FEC-02 AC4 `200` aberto | `spec.md:83` + `spec.md:39`: "Aberto", `formatRate`, `formatMoney`, "—" em nulo | `RT:59` `toEqual(['Outubro de 2026','Aberto','94,2%','R$ 10,50','—','—'])` | ✅ PASS |
| FEC-02 AC4 `200` travado | "Travado", `dd/mm/aaaa hh:mm` local, duração | `RT:70` `toEqual([..., 'Travado','100%','R$ 0,00','02/11/2026 08:07','2 h 5 min'])`; `LT:71` `formatLockedAt('2026-09-05T08:07:30')` = `'05/09/2026 08:07'`; `LT:75` nulo = `'—'` | ✅ PASS |
| FEC-02 AC5 `409` | `spec.md:84`: "Em andamento", "—" nos valores, sem ação | `RT:93` `toEqual(['Outubro de 2026','Em andamento','—','—','—','—'])`; `RT:94` `buttons()` = `[]`; `LT:83`, `LT:93` (erro vence dado antigo) | ✅ PASS |
| FEC-02 AC6 `404` | `spec.md:85`: "Sem conciliação concluída", "—", sem ação | `RT:93-94` (caso 404 do `it.each`); `PT:162` `'Agosto de 2026|Sem conciliação concluída'` | ✅ PASS |
| FEC-02 edge `matchRate` nulo | `spec.md:178`: "0%" | `RT:77` `cells()[2]` = `'0%'` | ✅ PASS |
| FEC-02 edge duração | `spec.md:179`: 29, 180, 7200, 7500, 86400, 97200 → "29 s", "3 min", "2 h", "2 h 5 min", "1 d", "1 d 3 h"; nulo "—" (`spec.md:40`) | `LT:51-66` `it.each` com os 6 valores, mais 59/60, 3599/3600, 86399 e nulo, `toBe(text)` | ✅ PASS |
| FEC-03 AC1 paginação | `spec.md:99`: 10 por página, "Página X de Y", "Anterior" off na primeira, "Próxima" off na última | `PT:175` 10 linhas; `PT:178` "Página 1 de 2"; `PT:179` "Anterior" `toBeDisabled()`; `PT:180` "Próxima" `toBeEnabled()`; `PT:185` página 2 = `['Dezembro de 2025|Aberto','Novembro de 2025|Aberto']`; `PT:186-188` "Página 2 de 2", "Próxima" off, "Anterior" on; `LT:46` `CLOSING_PAGE_SIZE` = 10 | ✅ PASS |
| FEC-03 edge 10 janelas | `spec.md:180`: uma página, sem paginação | `PT:203-204` 10 linhas e `queryByRole('navigation', { name: 'Paginação' })` nulo | ✅ PASS |
| FEC-03 AC2 sem runs | `spec.md:100`: "Nenhuma conciliação ainda.", link "Ir para conciliações" → `/conciliacoes`, sem tabela | `PT:211` texto; `PT:212` `toHaveAttribute('href', '/conciliacoes')`; `PT:213` tabela nula | ✅ PASS |
| FEC-03 AC3 erro dos runs | `spec.md:101`: mensagem e "Tentar novamente" que refaz a busca | `PT:222` `toHaveTextContent('Você não tem acesso a este comerciante')`; `PT:225-228` clique e 4 linhas | ✅ PASS |
| FEC-03 AC4 leitura só da página | `spec.md:102` | `PT:181` `new Set(periodReads())` = as 10 janelas da página 1; `PT:189` todas as 12 só depois de ir para a página 2 | ✅ PASS |
| FEC-03 AC5 erro da linha | `spec.md:103`: "Não carregou", "Tentar novamente" relê só a janela | `RT:104` `[..., 'Não carregou','—','—','—','—']`; `RT:105` `['Tentar novamente']`; `RT:108-110` linha "Aberto", 2 leituras, a 2ª com `['jwt','m-1',october]` | ✅ PASS |
| FEC-03 edge erro não JSON | `spec.md:181`: "A API retornou erro <status>." (cliente atual) | `frontend/lib/api.imports.test.ts:115` `toBe('A API retornou erro 502.')` (pré-existente); a linha e a página mostram `error.message` (`RT:207`, `PT:222`) | ✅ PASS |
| FEC-04 AC1 botões por status | `spec.md:117`: "Travar" em "Aberto", "Destravar" em "Travado" | `RT:61` `['Travar','Exportar CSV']`; `RT:71` `['Destravar','Exportar CSV']` | ✅ PASS |
| FEC-04 AC2 confirmação | `spec.md:118`: "Travar o período?" com "Travar"/"Cancelar", sem API antes | `RT:119` `getByRole('alertdialog', { name: 'Travar o período?' })`; `RT:120` `lockPeriod` `not.toHaveBeenCalled()`; `frontend/components/dashboard/CloseStepper.test.tsx:115-122` | ✅ PASS |
| FEC-04 AC5 cancelar | `spec.md:121` | `RT:155-156` diálogo nulo e mutação `not.toHaveBeenCalled()` (Travar e Destravar) | ✅ PASS |
| FEC-05 AC3 travar | `spec.md:119`: `POST /periods/lock` com a janela; linha com a resposta | `RT:123` `toHaveBeenCalledWith('jwt','m-1',october)`; `RT:124` `[..., 'Travado','100%','R$ 0,00','02/11/2026 08:07','29 s']`; `PT:261` corpo `toEqual([october])` | ✅ PASS |
| FEC-05 AC4 destravar | `spec.md:120`: `POST /periods/unlock`, "Aberto", "—" em Travado em e Duração | `RT:136` diálogo "Destravar o período?"; `RT:140` `unlockPeriod` com a janela; `RT:141` `[..., 'Aberto','100%','R$ 0,00','—','—']` | ✅ PASS |
| FEC-05 AC6 cache | `spec.md:122`: invalida a leitura do período no cache compartilhado | `RT:169` `getQueryData(queryKeys.period(...))` = resposta; `RT:170` `invalidateQueries({ queryKey: queryKeys.period('m-1', october) })`; `PT:262-263` 2ª leitura e cache `locked: true`; destravar pelo hook: `frontend/lib/queries.test.tsx:204` | ✅ PASS |
| FEC-06 AC1 aviso | `spec.md:136`: mensagem da API acima da tabela; some na próxima ação | `PT:243` `'Janela possui divergência em aberto'`; `PT:244` `compareDocumentPosition(...) & DOCUMENT_POSITION_FOLLOWING`; `PT:248` alerta nulo depois do export; `RT:181`, `RT:207`, `RT:237` `toHaveBeenLastCalledWith(mensagem)`; `RT:126`, `RT:226` `onError(null)` | ✅ PASS |
| FEC-06 AC2 `409` relê | `spec.md:137` | `RT:182-183` linha com `'R$ 20,00'` e 2 leituras; `RT:195-196` reabertura; `RT:208` sem releitura fora do 409 | ✅ PASS |
| FEC-06 AC3 ocupado | `spec.md:138`: botões da linha off durante trava, reabertura ou export | `RT:253-256` (trava e reabertura): ação e export `toBeDisabled()`, depois `toBeEnabled()`; `RT:269-272` export: export e "Travar" off, depois on | ✅ PASS |
| FEC-07 AC1 botão | `spec.md:152`: "Exportar CSV" em "Aberto" e "Travado" | `RT:61`, `RT:71`; ausente em 409/404 `RT:94` | ✅ PASS |
| FEC-07 AC2 arquivo | `spec.md:153`: `GET /reconciliations/<runId>/export`, `reconpay-<fromDate>-<toDate>.csv` | `RT:223` `[{ name: 'reconpay-2026-10-01-2026-10-31.csv', href: 'blob:csv' }]`; `RT:224` `exportRunCsv('jwt','m-1','r-9')`; `RT:225` conteúdo do blob | ✅ PASS |
| FEC-08 AC1 troca de empresa | `spec.md:167`: períodos da nova empresa na página 1, aviso limpo | `PT:280-286` "Beta Comércio", "Página 1 de 2", alerta nulo, 10 leituras de `m-2`. **Não discrimina o mecanismo**: o teste só cobre troca para empresa sem runs em cache; M27 sobrevive (ver sensor) | ❌ GAP |
| FEC-08 AC2 sem empresa | `spec.md:168`: estado vazio das outras telas, sem tabela | `PT:293` heading "Nenhuma empresa no seu acesso"; `PT:295` sem tabela; `PT:296` nenhuma chamada de runs/períodos | ✅ PASS |

**Status**: ❌ 1 gap (FEC-08 AC1, mutante sobrevivente). Nenhum spec-precision gap: a spec fixa textos, formatos, ordem, tamanho de página, bordas e valores de duração.

---

### Discrimination Sensor

Scratch: `git worktree add --detach <scratchpad>/verify-closing HEAD` com `frontend/node_modules` em symlink. Cada mutante rodou `vitest run lib/closing.test.ts components/closing components/dashboard` (204 testes, verdes no HEAD) e depois `git checkout --` só do arquivo de produção mutado.

| # | File | Mutação | Killed? |
| --- | --- | --- | --- |
| M1 | `frontend/lib/closing.ts:19` | empate pela data final ascendente | ✅ Killed (2) |
| M2 | `frontend/lib/closing.ts:18` | dedup só por `fromDate` | ✅ Killed (3) |
| M3 | `frontend/lib/closing.ts:25` | `< 60` → `<= 60` | ✅ Killed (1) |
| M4 | `frontend/lib/closing.ts:27` | `< 86400` → `<= 86400` | ✅ Killed (1) |
| M5 | `frontend/lib/closing.ts:32` | mostra "0 h" | ✅ Killed (1) |
| M6 | `frontend/lib/closing.ts:46` | dado antigo vence o erro (`error && !data`) | ✅ Killed (1) |
| M7 | `frontend/lib/closing.ts:48` | 409 e 404 trocados | ✅ Killed (6) |
| M8 | `frontend/lib/closing.ts:50` | locked/open invertidos | ✅ Killed (21) |
| M9 | `frontend/lib/closing.ts:41` | mês off-by-one em `formatLockedAt` | ✅ Killed (3) |
| M10 | `frontend/lib/closing.ts:4` | `CLOSING_PAGE_SIZE = 11` | ✅ Killed (3) |
| M11 | `frontend/components/closing/PeriodRow.tsx:40` | sem `refetch` no 409 | ✅ Killed (2) |
| M12 | `frontend/components/closing/PeriodRow.tsx:34` | sem `onError(null)` antes de travar/destravar | ✅ Killed (1) |
| M13 | `frontend/components/closing/PeriodRow.tsx:46` | sem `onError(null)` antes do export | ✅ Killed (2) |
| M14 | `frontend/components/closing/PeriodRow.tsx:30` | `busy` sem `unlock.isPending` | ✅ Killed (1) |
| M15 | `frontend/components/closing/PeriodRow.tsx:30` | `busy` sem `exportRun.isPending` | ✅ Killed (1) |
| M16 | `frontend/components/closing/PeriodRow.tsx:75` | "Travar" sem confirmação | ✅ Killed (9) |
| M17 | `frontend/components/closing/PeriodRow.tsx:78` | "Destravar" sem confirmação | ✅ Killed (4) |
| M18 | `frontend/components/closing/PeriodRow.tsx:52` | nome do arquivo com datas trocadas | ✅ Killed (1) |
| M19 | `frontend/components/closing/PeriodRow.tsx:48` | export com id errado | ✅ Killed (1) |
| M20 | `frontend/components/closing/PeriodRow.tsx:37` | sem `setQueryData` da resposta | ✅ Killed (3) |
| M21 | `frontend/components/closing/PeriodRow.tsx:88` | "Cancelar" do Destravar chama a API | ✅ Killed (1) |
| M22 | `frontend/components/closing/PeriodRow.tsx:71` | "Tentar novamente" também no 404 | ✅ Killed (1) |
| M23 | `frontend/components/closing/ClosingPage.tsx:24` | fatia com uma linha a mais | ✅ Killed (2) |
| M24 | `frontend/components/closing/ClosingPage.tsx:64` | "Próxima" habilitado na última | ✅ Killed (1) |
| M25 | `frontend/components/closing/ClosingPage.tsx:60` | "Anterior" habilitado na primeira | ✅ Killed (1) |
| M26 | `frontend/components/closing/ClosingPage.tsx:57` | paginação com página única (`>= 1`) | ✅ Killed (2) |
| M27 | `frontend/components/closing/ClosingPage.tsx:97` | sem `key={merchantId}` (página e aviso não zeram) | ❌ Survived → Fix 1 |
| M28 | `frontend/components/closing/ClosingPage.tsx:53` | `onError` ignora `null` (aviso não some) | ✅ Killed (1) |
| M29 | `frontend/components/closing/ClosingPage.tsx:43` | aviso abaixo da tabela | ✅ Killed (1) |
| M30 | `frontend/components/closing/ClosingPage.tsx:97` | sem `periodWindows` (sem dedup e ordem) | ✅ Killed (2) |
| M31 | `frontend/components/closing/ClosingPage.tsx:84` | sem empresa vira skeleton | ✅ Killed (1) |

Sonda (só no scratch, apagada depois): cópia de `PT` com um caso extra que semeia `queryKeys.runs('m-2')` no cache antes de trocar da página 2 de Alfa (com aviso) para Beta. HEAD: 10/10 verdes. Com M27: o caso extra falha em "Página 1 de 2".

**Sensor depth**: expandido (31 mutantes; a tela trava períodos, caminho de integridade do fechamento).
**Resultado do sensor (iteração 1)**: 30/31 killed, reprovado

Isolamento: `git status --porcelain` do worktree real vazio antes e depois; `git worktree remove --force` + `git worktree prune` feitos.

---

### Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ |
| Surgical changes | ✅ `CloseStepper.tsx` só ganhou `export` |
| No scope creep | ✅ |
| Matches patterns | ✅ estados de empresa iguais a `DiscrepanciesPage.tsx:138-150`; download igual ao dashboard |
| Spec-anchored outcome check | ✅ |
| Per-layer Coverage Expectation | ⚠️ FEC-08 AC1 sem caso de cache quente (M27) |
| Every test maps to a spec requirement | ✅ cada `it` cita FEC/edge |
| Documented guidelines followed: `AGENTS.md`, `frontend/AGENTS.md`, `.specs/LESSONS.md` | ✅ |

Testes não enfraquecidos: `CST` só ganhou o import de `Confirm` e um `describe` novo; nenhum teste existente mudou. O teste removido (`frontend/components/shell/UnderConstruction.test.tsx`, 1 caso) cobria só o placeholder de `/fechamento` ("em construção", sem request); o comportamento novo da rota está em `PT:151`. Desvio registrado em `tasks.md` T4.

---

### Edge Cases

- [x] Vários runs por janela: `LT:18`, `PT:158`
- [x] Janela parcial: `RT:83`
- [x] `matchRate` nulo: `RT:77`
- [x] Durações 29/180/7200/7500/86400/97200: `LT:51-66`
- [x] Exatamente 10 janelas: `PT:204`
- [x] Erro não JSON: `frontend/lib/api.imports.test.ts:115` (cliente pré-existente)

---

### Gate Check

- **Gate command**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`
- **Resultado**: 852 passed, 0 failed, 0 skipped (66 arquivos); typecheck ok; build ok (`/fechamento` estática)
- **Test count before feature**: 796 (medido no scratch em `03b25e9^`)
- **Test count after feature**: 852
- **Delta**: +56 (+57 novos: `LT` 27, `RT` 20, `PT` 9, `CST` 1; -1 removido do placeholder)
- **Skipped tests**: nenhum
- **Failures**: nenhuma

---

### Fix Plans

#### Fix 1: FEC-08 AC1 não prende a remontagem por empresa

- **Root cause**: `PT:266-287` troca para Beta sem runs em cache; `useRuns` fica `isPending`, a página mostra o skeleton e desmonta `ClosingWindows`, então página e aviso zeram mesmo sem `key={merchantId}`. Com os runs da empresa nova em cache (voltar para uma empresa já vista), só a `key` zera o estado, e nenhum teste cobre isso.
- **Fix task**: em `frontend/components/closing/ClosingPage.test.tsx`, no teste de FEC-08 AC1 (ou num caso novo ao lado), semear `client.setQueryData(queryKeys.runs('m-2'), { content: monthRuns('m-2', 12), totalPages: 1 })` antes de `renderRoute()` (ou fazer Alfa → Beta → Alfa), e depois da troca asserir "Página 1 de 2", `rowText()[0]` = `'Outubro de 2026|Aberto'` e `queryByRole('alert')` nulo. Sem mudança de produção.
- **Done when**: o caso passa no HEAD e falha com M27 (remover `key={merchantId}` em `ClosingPage.tsx:97`).
- **Priority**: Major

---

### Requirement Traceability Update

`spec.md` ainda mostra todas as linhas "Design / Pending" e "0 mapped to tasks" (`spec.md:189-198`): doc desatualizado, não editado pelo Verifier. Proposta:

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| FEC-01 | Pending | ✅ Verified |
| FEC-02 | Pending | ✅ Verified |
| FEC-03 | Pending | ✅ Verified |
| FEC-04 | Pending | ✅ Verified |
| FEC-05 | Pending | ✅ Verified |
| FEC-06 | Pending | ✅ Verified |
| FEC-07 | Pending | ✅ Verified |
| FEC-08 | Pending | ❌ Needs Fix (Fix 1) |

Outros docs: `tasks.md` com **Status** "In Progress"; `README.md` lista os status "(travado, aberto, em andamento)" sem "Sem conciliação concluída" (o `frontend/README.md` cita os quatro).

---

### Summary

**Overall (iteração 1)**: ❌ Not Ready (1 gap de teste, sem bug de produção)

**Spec-anchored check**: 30/31 linhas da tabela batem com a spec; 0 spec-precision gaps
**Sensor**: 30/31 mutations killed
**Gate**: 852 passed

**What works**: regras puras, os quatro status e o erro da linha, ações com confirmação, 409, ocupado, export, paginação com bordas, aviso acima da tabela, cache compartilhado, sem empresa, sem runs.

**Issues found**: Fix 1 (FEC-08 AC1 com runs em cache).

**Next steps**: aplicar Fix 1 e re-verificar (iteração 2).
