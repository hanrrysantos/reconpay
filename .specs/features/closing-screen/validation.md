# Tela de Fechamento Validation

**Date**: 2026-10-17
**Spec**: `.specs/features/closing-screen/spec.md`
**Diff range**: `03b25e9^..8cb65c2` (8 commits), branch `feat/closing-screen`. Código em `13ccbc7..8cb65c2`; backend sem diff (`git diff --stat` só lista `frontend/`, `README.md` e `.specs/`).
**Verifier**: sub-agente independente (author ≠ verifier), iteração 1.

## Validation: closing-screen - FAIL ❌

As 8 requisições (FEC-01..FEC-08) têm evidência `file:line` e os valores asseridos batem com a spec. Gate verde: 852 testes (796 antes, +56), typecheck e build. O sensor matou 30 de 31 mutantes. O mutante M27 sobreviveu: tirar `key={merchantId}` de `ClosingWindows` (`frontend/components/closing/ClosingPage.tsx:97`) não quebra nenhum teste. O teste de FEC-08 AC1 troca para uma empresa sem runs em cache, então a página passa pelo skeleton, desmonta `ClosingWindows` e zera página e aviso sem depender da `key`. Com os runs da nova empresa já em cache (por exemplo, Alfa → Beta → Alfa), a página 2 e o aviso continuam na tela. Uma sonda no scratch confirmou: o mesmo cenário com os runs de `m-2` semeados no cache passa no HEAD e falha com M27 ("Unable to find ... Página 1 de 2").

Abreviações: `LT` = `frontend/lib/closing.test.ts`, `RT` = `frontend/components/closing/PeriodRow.test.tsx`, `PT` = `frontend/components/closing/ClosingPage.test.tsx`, `CST` = `frontend/components/dashboard/CloseStepper.test.tsx`, `QT` = `frontend/lib/queries.test.tsx`.

---

## Task Completion

| Task | Status | Notes |
| --- | --- | --- |
| T1 | ✅ Done | `13ccbc7` |
| T2 | ✅ Done | `341c0d8` |
| T3 | ✅ Done | `ef4a69e` |
| T4 | ✅ Done | `c7895bb`; desvio registrado em `tasks.md` (remoção de `UnderConstruction.test.tsx` veio de T5) |
| T5 | ✅ Done | `5f6c23b`; nenhum import de `UnderConstruction` resta |
| T6 | ✅ Done | `8cb65c2` |

---

## Spec-Anchored Acceptance Criteria

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

## Discrimination Sensor

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
**Result**: 30/31 killed - FAIL ❌

Isolamento: `git status --porcelain` do worktree real vazio antes e depois; `git worktree remove --force` + `git worktree prune` feitos.

---

## Code Quality

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

## Edge Cases

- [x] Vários runs por janela: `LT:18`, `PT:158`
- [x] Janela parcial: `RT:83`
- [x] `matchRate` nulo: `RT:77`
- [x] Durações 29/180/7200/7500/86400/97200: `LT:51-66`
- [x] Exatamente 10 janelas: `PT:204`
- [x] Erro não JSON: `frontend/lib/api.imports.test.ts:115` (cliente pré-existente)

---

## Gate Check

- **Gate command**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`
- **Result**: 852 passed, 0 failed, 0 skipped (66 arquivos); typecheck ok; build ok (`/fechamento` estática)
- **Test count before feature**: 796 (medido no scratch em `03b25e9^`)
- **Test count after feature**: 852
- **Delta**: +56 (+57 novos: `LT` 27, `RT` 20, `PT` 9, `CST` 1; -1 removido do placeholder)
- **Skipped tests**: nenhum
- **Failures**: nenhuma

---

## Fix Plans

### Fix 1: FEC-08 AC1 não prende a remontagem por empresa

- **Root cause**: `PT:266-287` troca para Beta sem runs em cache; `useRuns` fica `isPending`, a página mostra o skeleton e desmonta `ClosingWindows`, então página e aviso zeram mesmo sem `key={merchantId}`. Com os runs da empresa nova em cache (voltar para uma empresa já vista), só a `key` zera o estado, e nenhum teste cobre isso.
- **Fix task**: em `frontend/components/closing/ClosingPage.test.tsx`, no teste de FEC-08 AC1 (ou num caso novo ao lado), semear `client.setQueryData(queryKeys.runs('m-2'), { content: monthRuns('m-2', 12), totalPages: 1 })` antes de `renderRoute()` (ou fazer Alfa → Beta → Alfa), e depois da troca asserir "Página 1 de 2", `rowText()[0]` = `'Outubro de 2026|Aberto'` e `queryByRole('alert')` nulo. Sem mudança de produção.
- **Done when**: o caso passa no HEAD e falha com M27 (remover `key={merchantId}` em `ClosingPage.tsx:97`).
- **Priority**: Major

---

## Requirement Traceability Update

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

## Summary

**Overall**: ❌ Not Ready (1 gap de teste, sem bug de produção)

**Spec-anchored check**: 30/31 linhas da tabela batem com a spec; 0 spec-precision gaps
**Sensor**: 30/31 mutations killed
**Gate**: 852 passed

**What works**: regras puras, os quatro status e o erro da linha, ações com confirmação, 409, ocupado, export, paginação com bordas, aviso acima da tabela, cache compartilhado, sem empresa, sem runs.

**Issues found**: Fix 1 (FEC-08 AC1 com runs em cache).

**Next steps**: aplicar Fix 1 e re-verificar (iteração 2).
