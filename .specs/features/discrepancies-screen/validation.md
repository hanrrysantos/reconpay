# Tela de Divergências Validation

**Date**: 2026-10-16
**Spec**: `.specs/features/discrepancies-screen/spec.md`
**Diff range**: `baa9d12^..71174f1` (10 commits), branch `feat/discrepancies-screen`. Spec `baa9d12`, plano `8a24fe1`, código `2689579`..`71bdfb0`, docs `71174f1`.
**Verifier**: sub-agente independente (author ≠ verifier), iteração 1 de 3.

## Validation: discrepancies-screen - FAIL ❌

O código atende a spec nos pontos que verifiquei. Os testes não. Quatro comportamentos pedidos pela spec não têm teste que discrimine: J/K na página (DIV-07 AC7), troca de janela volta para a página 1 e mantém os filtros (DIV-03 AC6), troca de empresa volta para a página 1 e reseta a janela também com dados em cache (DIV-08 AC1), e janela inicial no mês atual sem run (DIV-01 AC1). Oito mutantes não equivalentes sobrevivem. Gate verde: 785 testes, typecheck e build. Backend sem mudança.

Abreviações: `DPT` = `frontend/components/discrepancies/DiscrepanciesPage.test.tsx`, `DLT` = `frontend/components/discrepancies/DiscrepancyList.test.tsx`, `WNT` = `frontend/components/discrepancies/WindowNotice.test.tsx`, `LDT` = `frontend/lib/discrepancies.test.ts`, `QDT` = `frontend/lib/queries.discrepancies.test.tsx`, `ART` = `frontend/lib/api.reconciliations.test.ts`, `RDT` = `frontend/components/dashboard/ResolveDrawer.test.tsx`.

---

## Task Completion

| Task | Status | Notes |
| --- | --- | --- |
| T1 Rodapé da gaveta | ✅ Done | `2689579` |
| T2 Filtro na API | ✅ Done | `71e4453` |
| T3 Chave e invalidação | ✅ Done | `f799295` |
| T4 Regras puras | ✅ Done | `85d731b` |
| T5 Lista | ✅ Done | `65c17f2` |
| T6 Aviso | ✅ Done | `9dbfc07` |
| T7 Página e rota | ⚠️ Done com gaps de teste | `71bdfb0`; ver gaps 1-4 |
| T8 README | ✅ Done | `71174f1` |

---

## Pontos pedidos pelo orquestrador

1. **Compatibilidade das peças compartilhadas.** ✅ O padrão de `footerNoun` é `'abertas'` (`ResolveDrawer.tsx:78`). O teste antigo `RDT:228-231` ("2 de 12 abertas · J / K navega") não mudou e mata M8. O diff só acrescenta `RDT:233-237`. Nenhum teste do dashboard ou de Conciliações mudou (`git diff --stat`: só `ResolveDrawer.test.tsx` +6 e `UnderConstruction.test.tsx` -2, este planejado). A chave completa de `runItems` só ganhou um segmento; os prefixos `['runItems', m, r]` e `['runItems', m]` continuam prefixos (`queries.ts:46-51`). Os 278 testes de dashboard, Conciliações e queries passam com a invalidação nova.
2. **`rowsOf` e paginação por item.** ✅ Uma linha por divergência que atende status e tipo (`lib/discrepancies.ts:22-26`). "Todas" vira `{ result: 'DIVERGENT' }` (`lib/discrepancies.ts:17`). M1-M4 morrem.
3. **Lista da gaveta congelada.** ✅ `drawer.list` é guardada no clique (`DiscrepanciesPage.tsx:57,108`) e não se recalcula. M7 (gaveta com as linhas vivas) morre em `DPT:252`. Rodapé "1 de 2 · J / K navega" em `DPT:243`; M8b morre. **J/K na página não tem teste**: M20 sobrevive (gap 1).
4. **Travado.** ⚠️ `locked = period.locked || anyInFlight(run)` (`DiscrepanciesPage.tsx:62`). A parte do período está provada (`DPT:273-283`). A parte do run em andamento não tem teste (M9 sobrevive), mas a spec não pede isso: a lista só existe com run concluído. `WindowNotice` só mostra o aviso de travado depois dos ramos sem run, andamento e falha (`WindowNotice.tsx:41`). Isso também não tem teste (M10 sobrevive). Os dois são menores (gap 5).
5. **Run ao vivo (L-037).** ✅ A fixture serve itens só depois de entregar `COMPLETED` (`DPT:74,77`). `DPT:225-227` mostra o aviso sem tabela; `DPT:229-230` mostra as linhas finais; `DPT:232-234` não busca mais itens depois de +15 s. M24 (lista montada com run em andamento) morre. L-037 evitou a falha da feature anterior.
6. **Troca de empresa (L-036).** ⚠️ Os filtros saem do padrão antes da troca (`DPT:289-291`) e M12a/M12b morrem: L-036 funcionou para os filtros. A gaveta fecha (`DPT:297`) e a janela muda (`DPT:302`). Mas a página nunca sai de 0 antes da troca, e a empresa nova não tem `runs` em cache, então o carregamento desmonta a janela de qualquer forma. M13 (sem `key={merchantId}`) e M14 (página no pai) sobrevivem (gap 3).
7. **Helper `rows()` da página.** ✅ O split em `DPT:144` corta a célula Transação no início da linha de método e compara só a referência. Não esconde erro de referência. A linha de método e data é afirmada por inteiro em `DLT:57-59`: `'REF-1Crédito 3x · 03/10/2026'` e `'REF-2— · —'`. M18a e M18b morrem. Esperado `'—'` e Diferença `'+R$ 48,50'` do servidor no lado ausente estão em `DLT:59`.

---

## Spec-Anchored Acceptance Criteria

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| DIV-01 AC1 janela inicial | run mais recente; título, empresa e menu nessa janela; **mês atual sem run** | `DPT:156` `within(pageHeader()).getByText('Alfa Pagamentos')`; `DPT:157` botão `/Outubro de 2026/`. Sem teste de empresa sem run; M21 sobrevive | ❌ GAP (mês atual) |
| DIV-01 AC2 busca e colunas | `?discrepancyStatus=OPEN&page=0&size=20&sort=absDifference,desc`; uma linha por divergência aberta; 6 colunas | `DPT:158-159` path e `toEqual({ page:'0', size:'20', sort:'absDifference,desc', discrepancyStatus:'OPEN' })`; `DPT:160`; `DLT:55` cabeçalhos; `DLT:56-61` | ✅ PASS |
| DIV-01 AC3 status | `discrepancyStatus=<valor>`, página 1; "Todas" sem o parâmetro e com `result=DIVERGENT` | `DPT:171-174` `toMatchObject({ discrepancyStatus:'ACCEPTED', page:'0' })` depois de ir para a página 2; `DPT:178` `toEqual` com `result` = `'DIVERGENT'` e sem status; `LDT:32`; M1, M5 morrem | ✅ PASS |
| DIV-01 AC4 tipo | `discrepancyType=<tipo>`, página 1; "Todos" remove | `DPT:181-183` `toMatchObject({ discrepancyType:'MISSING_SETTLEMENT', page:'0' })`; `LDT:37` sem tipo; `ART:80-91` | ✅ PASS |
| DIV-01 AC5 só as divergências que atendem | filtra status e tipo dentro do item | `LDT:47-57`; `DLT:75-82`; `DPT:174,184`; M2, M3 morrem | ✅ PASS |
| DIV-03 AC6 troca de janela | run vigente da nova janela, **página 1, filtros mantidos** | `DPT:187-199` só troca de 0 para uma janela sem run. M6 (não volta para a página 1) e M23 (zera filtros) sobrevivem | ❌ GAP |
| DIV-03 AC7 paginação | "Anterior", "Próxima", "Página X de Y" | `DLT:101` `'Página 2 de 3'`; `DLT:102-106` página 1 e `onPageChange` 0 e 2 | ✅ PASS |
| DIV-03 AC8 vazio | "Nenhuma divergência para os filtros escolhidos." | `DLT:112-113` | ✅ PASS |
| DIV-03 AC9 erro | mensagem + "Tentar novamente" | `DLT:122-126` mensagem da API e nova busca | ✅ PASS |
| DIV-04 AC1 sem run | texto + link `/conciliacoes` | `WNT:15-16`; `DPT:195-198` sem tabela e sem busca de itens | ✅ PASS |
| DIV-04 AC2 em andamento | "Conciliação em andamento."; recarrega a cada 3 s; lista ao concluir sem recarregar | `WNT:19-23`; `DPT:226-234` (itens só depois de `COMPLETED`); M24 morre. A cadência de 3 s é do `useLatestRun` existente e não mudou | ✅ PASS |
| DIV-04 AC3 falha | texto, `errorMessage`, link | `WNT:26-29`; `DPT:205-207`; M11 morre | ✅ PASS |
| DIV-05 AC4 travado | "Período travado: as divergências ficam só para consulta." | `WNT:34`; `DPT:278` | ✅ PASS |
| DIV-06 AC1 abrir | gaveta da divergência, dois lados, histórico | `DLT:88-93` linhas e índice; `DPT:241-243` dialog; histórico e lados: `RDT:71` (gaveta reutilizada, sem mudança) | ✅ PASS |
| DIV-06 AC2 aceitar/ajustar/baixar | regras atuais da gaveta | `DPT:244-251` PATCH `…/r-oct/discrepancies/d-1`, `{ status:'ACCEPTED', note:'conferido' }` | ✅ PASS |
| DIV-06 AC3 reabrir | `status` `OPEN` | `DPT:268-270` path `d-3` e `toEqual({ status:'OPEN' })` | ✅ PASS |
| DIV-06 AC4 travado sem ações | sem Aceitar/Ajustar/Baixar/Reabrir | `DPT:281-282` sem `radio` e sem botões | ✅ PASS |
| DIV-07 AC5 novo status e recarga | status na gaveta; lista recarrega | `DPT:252` `drawer-status` `'Aceita'`; `QDT:60-76` página já recarregada quando a mutação resolve; M15 morre | ✅ PASS |
| DIV-07 AC6 fechar | lista sem a divergência que saiu do filtro | `DPT:254-255` `['IMP-T-002|Status inconsistente|Aberta']`; M7 morre | ✅ PASS |
| DIV-07 AC7 J/K e rodapé | J/K e setas andam pela página; rodapé "<n> de <total da página>" | Rodapé: `DPT:243` `'1 de 2 · J / K navega'`. J/K: só `RDT:206` com `onNavigate` mockado; a página não testa. M20 (`onNavigate` vazio) sobrevive | ❌ GAP (J/K) |
| DIV-08 AC1 troca de empresa | fecha a gaveta, janela da nova empresa, filtros mantidos, **página 1** | `DPT:297` sem dialog; `DPT:298-304` path `r-beta`, query com filtros, `Setembro de 2026`, selects; `DPT:306`. Página não sai de 0 antes; empresa nova sem cache. M13 e M14 sobrevivem | ❌ GAP (página 1, cache) |
| DIV-08 AC2 sem empresa | estado vazio, sem filtros nem lista | `DPT:313-317` heading, sem Status/Tipo, sem tabela, 0 buscas | ✅ PASS |

**Status**: ❌ 18/22 ACs com evidência que discrimina; 4 com gap.

**Spec-precision gaps (menores)**: a spec diz "método e parcelas" sem fixar quando as parcelas aparecem. O código omite `1x` (`DiscrepancyList.tsx:21`), como o dashboard (`OpenDiscrepancyTable.tsx:34`). Sem teste com 1 parcela; M19 sobrevive. A spec também não diz o que fazer com o aviso de travado quando o run não está concluído.

---

## Edge Cases

- [x] Item com duas divergências abertas vira duas linhas: `LDT:44`; `DLT:56-58`; `DLT:61` três "Abrir".
- [x] Filtro Aceitas com uma aceita e uma aberta mostra só a aceita: `LDT:48`; `DLT:81`; `DPT:174`.
- [x] Lado ausente com "—", Esperado e Diferença do servidor: `DLT:59` `['REF-2— · —', 'Liquidação órfã', '—', '+R$ 48,50', 'Aberta']`. Observação: a tabela não tem coluna que venha da liquidação; o teste cobre o lado da transação ausente, que é o que a tabela mostra.
- [x] Erro sem JSON: comportamento do cliente existente, `frontend/lib/api.imports.test.ts:115` `'A API retornou erro 502.'`. Sem mudança nesta feature.

---

## Discrimination Sensor

Scratch: `git worktree add --detach` em `scratchpad/div-wt` a partir de `71174f1`, com `frontend/node_modules` por symlink. Baseline do scratch: `tsc --noEmit` exit 0; 278 testes em escopo passam. Cada mutante passou por `tsc --noEmit` (todos compilam) e rodou `vitest run` sobre `components/discrepancies`, `lib/discrepancies.test.ts`, `lib/queries.discrepancies.test.tsx`, `lib/api.reconciliations.test.ts`, `components/dashboard`, `components/reconciliations`, `lib/queries.test.tsx` e `lib/queries.reconciliations.test.tsx`. Revertido com `git checkout` no scratch.

| # | File | Mutação | Killed? | Testes que falharam |
| - | ---- | ------- | ------- | ------------------- |
| M1 | `lib/discrepancies.ts:17` | "Todas" também manda `discrepancyStatus=OPEN` | ✅ Killed | `LDT` (2), `DLT` ALL, `DPT:163` |
| M2 | `lib/discrepancies.ts:24` | `rowsOf` ignora o tipo | ✅ Killed | `LDT:51` |
| M3 | `lib/discrepancies.ts:24` | `rowsOf` ignora o status | ✅ Killed | 9 (`DPT`, `DLT`, `LDT`) |
| M4 | `lib/discrepancies.ts:25` | uma linha por item | ✅ Killed | 5 (`LDT`, `DLT`, `DPT:163`) |
| M5 | `DiscrepanciesPage.tsx:74` | filtro não volta para a página 1 | ✅ Killed | `DPT:163` |
| M6 | `DiscrepanciesPage.tsx:68` | janela não volta para a página 1 | ❌ Survived | - |
| M7 | `DiscrepanciesPage.tsx:117-119` | gaveta usa as linhas vivas da lista | ✅ Killed | `DPT:237` |
| M8 | `ResolveDrawer.tsx:78` | padrão de `footerNoun` vira `null` | ✅ Killed | `RDT:228` |
| M8b | `DiscrepanciesPage.tsx:120` | página não passa `footerNoun={null}` | ✅ Killed | `DPT:237` |
| M9 | `DiscrepanciesPage.tsx:62` | `locked` ignora run em andamento | ⚪ Survived, fora da spec (design defensivo; lista só existe com run concluído) | - |
| M10 | `WindowNotice.tsx:21` | aviso de travado antes dos outros ramos | ❌ Survived (menor) | - |
| M11 | `WindowNotice.tsx:36` | `FAILED` sem `errorMessage` | ✅ Killed | `WNT:25`, `DPT:201` |
| M12a | `DiscrepanciesPage.tsx:138` | troca de empresa volta o status para Abertas | ✅ Killed | `DPT:285` |
| M12b | `DiscrepanciesPage.tsx:138` | troca de empresa volta o tipo para Todos | ✅ Killed | `DPT:285` |
| M13 | `DiscrepanciesPage.tsx:159` | sem `key={merchantId}` | ❌ Survived | - |
| M14 | `DiscrepanciesPage.tsx:55` | página no pai: troca de empresa mantém a página | ❌ Survived | - |
| M15 | `queries.ts:167` | `invalidateWindow` sem `runItems` | ✅ Killed | `QDT:60`, `DPT:237` |
| M16 | `api.ts:364` | parâmetro vira `status` | ✅ Killed | 8 (`ART`, `DPT`) |
| M16b | `api.ts:365` | status antes do tipo na URL | ✅ Killed | `ART:86` |
| M17 | `DiscrepancyList.tsx:89` | Diferença sem sinal | ✅ Killed | `DLT:50` |
| M18a | `DiscrepancyList.tsx:20` | método ausente sem "—" | ✅ Killed | `DLT:50` |
| M18b | `DiscrepancyList.tsx:22` | data ausente sem "—" | ✅ Killed | `DLT:50` |
| M19 | `DiscrepancyList.tsx:21` | parcela `1x` aparece | ❌ Survived (spec-precision) | - |
| M20 | `DiscrepanciesPage.tsx:123` | `onNavigate` vazio: J/K não andam | ❌ Survived | - |
| M21 | `DiscrepanciesPage.tsx:156` | janela inicial sem run = mês anterior | ❌ Survived | - |
| M22 | `queries.ts:51` | chave sem o status | ✅ Killed | `QDT:35`, `QDT:44` |
| M23 | `DiscrepanciesPage.tsx:68` | troca de janela zera os filtros | ❌ Survived | - |
| M24 | `DiscrepanciesPage.tsx:100` | lista monta com run em andamento | ✅ Killed | `DPT:210` |
| M25 | `DiscrepancyList.tsx:87` | Esperado com sinal | ✅ Killed | `DLT:50` |
| M26 | `DiscrepanciesPage.tsx:69` | troca de janela não fecha a gaveta | ⚪ Survived, fora da spec (gaveta modal cobre o menu; a spec não pede) | - |

**Por que M13 sobrevive**: a empresa nova não tem `runs` em cache. `useRuns` fica pendente e a página troca a janela pelo esqueleto (`DiscrepanciesPage.tsx:151`), o que desmonta a janela com ou sem `key`. Voltar para uma empresa já visitada (`runs` em cache) não passa pelo esqueleto; aí só a `key` fecha a gaveta e reseta a janela e a página.

**Sensor depth**: ampliado (30 mutantes).
**Result**: 20/28 não equivalentes mortos (30 injetados; M9 e M26 fora da spec). 8 sobreviventes - FAIL ❌

**Isolamento**: `git status --porcelain` antes = vazio; depois da remoção do scratch = vazio. Scratch removido com `git worktree remove --force` + `git worktree prune`; `git worktree list` não lista mais o scratch. Nenhum `git stash`.

---

## Gate Check

- **Gate command**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`
- **Result**: 785 passed, 0 failed, 0 skipped. Typecheck exit 0. Build ok, `/divergencias` estática.
- **Test count**: 743 → 785 (+42). Removido 1 caso planejado (`/divergencias` em `UnderConstruction.test.tsx`), porque a rota deixou de ser placeholder. Nenhuma asserção afrouxada.
- **Backend**: `git diff --name-only baa9d12^..71174f1 -- backend` vazio. Sem `./mvnw verify` nesta feature.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ Três peças existentes com extensões opcionais; um módulo puro, dois componentes, uma página |
| Surgical changes | ✅ Só os arquivos do plano |
| No scope creep | ✅ `locked` com run em andamento é defensivo e barato; não acrescenta UI |
| Matches patterns | ✅ Servidor em memória e troca de empresa como em Conciliações e Transações; `queryKeys` por prefixo |
| Spec-anchored outcome check | ❌ 4 ACs sem asserção que discrimine |
| Per-layer Coverage Expectation | ⚠️ lib e hooks 1:1; página sem J/K, troca de janela completa e mês atual |
| Every test maps to a spec requirement | ✅ Todos os `it` citam DIV-xx ou edge |
| Documented guidelines followed | ✅ `AGENTS.md` (não enfraquecer testes), `frontend/vitest.config.ts` |

Risco fora da spec (não é gap): se um run novo começar na mesma janela com a gaveta aberta, a gaveta passa a usar `current.id` do run novo com as entradas do run antigo (`DiscrepanciesPage.tsx:116`). `locked` bloqueia ações enquanto ele roda; depois que conclui, um PATCH iria para o run novo. O dashboard tem a mesma forma. Vale uma nota para a tela de Fechamento ou para um ajuste futuro.

---

## Fix Plans

### Fix 1: J/K na página (DIV-07 AC7) - Major

- **Root cause**: só `RDT:206` testa J/K, com `onNavigate` mockado; a ligação `onNavigate={index => setDrawer({ ...drawer, index })}` (`DiscrepanciesPage.tsx:123`) não tem teste.
- **Fix task**: em `DPT`, abrir "AVULSA-1 · Liquidação ausente", apertar `j` e ver "2 de 2 · J / K navega" e o tipo "Status inconsistente" na gaveta; apertar `k` e voltar para "1 de 2". Mata M20.

### Fix 2: troca de janela (DIV-03 AC6) - Major

- **Root cause**: `DPT:187` vai de outubro (página 0, filtros padrão) para setembro (sem run). Não prova página 1 nem filtros mantidos.
- **Fix task**: em `DPT`, mudar Status para "Todas" e Tipo para "Status inconsistente", ir para a página 2, acrescentar no teste um item divergente de `r-aug`, trocar para agosto, e afirmar a última busca `/r-aug/items` com `page:'0'`, `result:'DIVERGENT'`, `discrepancyType:'STATUS_MISMATCH'` e os selects nos mesmos valores. Mata M6 e M23 (L-036).

### Fix 3: troca de empresa com página e cache (DIV-08 AC1) - Major

- **Root cause**: a página não sai de 0 antes da troca, e a empresa nova nunca tem `runs` em cache.
- **Fix task**: em `DPT`, ir para a página 2 na Alfa, trocar para Beta, voltar para Alfa com a gaveta aberta em outra janela (agosto), e afirmar: gaveta fechada, janela "Outubro de 2026", última busca com `page:'0'`. Mata M13 e M14.

### Fix 4: janela inicial sem run (DIV-01 AC1) - Minor

- **Fix task**: em `DPT`, `server.runs = []` (ou uma empresa sem run) com a data falsa de 16/10/2026; afirmar botão "Outubro de 2026" e o aviso "Nenhuma conciliação nesta janela.". Para matar M21 com outubro, usar uma data de sistema em outro mês que o das fixtures (por exemplo, 2026-11-05 → "Novembro de 2026").

### Fix 5: aviso de travado só com run concluído - Minor

- **Fix task**: em `WNT`, `run({ status: 'FAILED', errorMessage: 'x' })` com `locked` mostra a falha e não mostra "Período travado…". Mata M10. Opcional: teste com 1 parcela sem `1x` (M19), se o usuário fixar o formato.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| DIV-01 | Pending | ❌ Needs Fix (AC1 mês atual) |
| DIV-02 | Pending | ✅ Verified |
| DIV-03 | Pending | ❌ Needs Fix (AC6) |
| DIV-04 | Pending | ✅ Verified |
| DIV-05 | Pending | ✅ Verified |
| DIV-06 | Pending | ✅ Verified |
| DIV-07 | Pending | ❌ Needs Fix (AC7 J/K) |
| DIV-08 | Pending | ❌ Needs Fix (AC1 página e cache) |

---

## Lessons

- L-036 recorreu e foi promovida para `confirmed` (recorrência 2): M23 é estado que precisa sobreviver a um reset (filtros na troca de janela) sem teste com os valores fora do padrão.
- L-038 (candidata): para provar que um valor reseta (página), tirá-lo do padrão antes do reset (M6, M14).
- L-039 (candidata): testar reset por `key` trocando para um alvo com dados em cache (M13).
- L-040 (candidata): callback de componente reutilizado ligado pela página precisa de teste pela página (M20).
- L-041 (candidata, spec_precision_gap): fixar o texto exato de células compostas, inclusive quando partes somem (M19).
- L-035 e L-037 evitaram falhas (nome da empresa em `pageHeader`; M24 morre). Evitar falha não é sinal; nada registrado para elas.

**Docs desatualizados (reportados, não editados)**: `spec.md:144-155` (traceability Pending, "0 mapped"); `tasks.md:12` (`Status: In Progress`); `.specs/STATE.md:104-111` (Handoff ainda em Specify, spec "aguardando revisão").

---

## Summary

**Overall**: ❌ Not Ready

**Spec-anchored check**: 18/22 ACs; 4 gaps (DIV-01 AC1, DIV-03 AC6, DIV-07 AC7, DIV-08 AC1); 1 spec-precision gap menor (parcelas).
**Sensor**: 20/28 não equivalentes mortos.
**Gate**: 785 passed.

**What works**: filtros e URL exata, uma linha por divergência, formatos, lista congelada da gaveta, resolver e reabrir, travado, run ao vivo com itens finais, isolamento dos filtros na troca de empresa, compatibilidade com dashboard e Conciliações.

**Next steps**: Fix 1-4 (só testes em `DPT`; o código já se comporta como a spec pede), Fix 5 opcional; depois, re-verificar.
