# Tela de Divergências Validation

**Date**: 2026-10-16
**Spec**: `.specs/features/discrepancies-screen/spec.md`
**Diff range**: `baa9d12^..79fea18` (19 commits), branch `feat/discrepancies-screen`. Iteração 4 cobriu `baa9d12^..cadc990`; o relatório dela entrou em `08cb72c`; as fixes são `e481c6e` e `79fea18` (só teste e spec).
**Verifier**: sub-agente independente (author ≠ verifier), iteração 5 (extra, autorizada pelo usuário depois da iteração 4 reprovada).

## Validation: discrepancies-screen - PASS ✅

Os dois gaps da iteração 4 estão fechados. F7 morre: a página volta o Tipo para "Todos" e a busca sai sem `discrepancyType`, com 3 linhas. F1 e F2 morrem: a spec agora fixa as bordas da paginação (`spec.md:74`) e `DLT` testa as duas pontas. As fixes só mexem em teste e spec. As 22 ACs têm evidência que discrimina. Os 14 mutantes morrem (5 re-execuções e 9 novos). Gate verde: 796 testes, typecheck e build. Backend sem mudança.

Abreviações: as mesmas da iteração 1 (abaixo).

---

## Iteração 5

### Fixes `e481c6e` e `79fea18`

- **Só teste e spec.** `git diff --numstat 08cb72c..79fea18`: `spec.md` +1/-1, `DPT` +6/-1, `DLT` +13/-0. Nenhum arquivo de produção.
- **Nada enfraquecido.** A única linha removida em `DPT` é o título do `it` (`DPT:164`), que ganhou "e \"Todos\"". As asserções antigas do teste ficaram iguais. Em `spec.md` a linha de AC7 só ganhou a cláusula das bordas. Nenhum teste removido.
- **Backend.** `git diff --name-only baa9d12^..79fea18 -- backend` vazio.

### Gaps da iteração 4 re-checados

| Gap | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| DIV-01 AC4 "Todos" pela página (e AC5) | `spec.md:71`: "Todos" SHALL remover o parâmetro e voltar para a página 1; `spec.md:72`: só as divergências que atendem aos filtros, ou seja, todos os tipos | `DPT:187` vai para a página 2; `DPT:188` Tipo "Todos"; `DPT:189` `toEqual({ page:'0', size:'20', sort:'absDifference,desc', … })` com `result` = `'DIVERGENT'` e sem `discrepancyType` (objeto exato: sem o parâmetro e na página 1); `DPT:190` `rows()` com 3 linhas (todos os tipos dos dois itens). F7 morre | ✅ PASS |
| DIV-03 AC7 bordas (era spec-precision) | `spec.md:74`: "Anterior" desabilitado na primeira página e "Próxima" na última | `DLT:118-129` `it.each`: página 0 → `DLT:126` "Página 1 de 3", `DLT:127` "Anterior" `toBeDisabled()`, `DLT:128` "Próxima" `toBeEnabled()`; página 2 → "Página 3 de 3", "Próxima" `toBeDisabled()`, "Anterior" `toBeEnabled()`. F1 e F2 morrem. A spec agora fixa o resultado, então o gap de precisão está fechado | ✅ PASS |

### Spec-Anchored Acceptance Criteria (todas as ACs, linhas atuais)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| DIV-01 AC1 janela inicial | run mais recente; título, empresa e menu; mês atual sem run | `DPT:157` `within(pageHeader()).getByText('Alfa Pagamentos')`; `DPT:158` `/Outubro de 2026/`; sem run: `DPT:233` `/Novembro de 2026/`, `DPT:235` `/periods` `toEqual({ fromDate:'2026-11-01', toDate:'2026-11-30' })` | ✅ PASS |
| DIV-01 AC2 busca e colunas | `discrepancyStatus=OPEN&page=0&size=20&sort=absDifference,desc`; uma linha por divergência aberta; 6 colunas | `DPT:159` path `r-oct/items`; `DPT:160` `toEqual({ page:'0', size:'20', sort:'absDifference,desc', discrepancyStatus:'OPEN' })`; `DPT:161`; `DLT:55` cabeçalhos; `DLT:56-61` | ✅ PASS |
| DIV-01 AC3 status | `discrepancyStatus=<valor>`, página 1; "Todas" sem o parâmetro e com `result=DIVERGENT` | `DPT:174` `toMatchObject({ discrepancyStatus:'ACCEPTED', page:'0' })` depois da página 2; `DPT:179` `toEqual` exato com `result` = `'DIVERGENT'`; `DPT:180` 3 linhas; `LDT:32` | ✅ PASS |
| DIV-01 AC4 tipo | `discrepancyType=<tipo>`, página 1; "Todos" remove | `DPT:184` `toMatchObject({ discrepancyType:'MISSING_SETTLEMENT', page:'0' })`; `DPT:189` "Todos" com `toEqual` exato sem o tipo; `ART:74-77` URL | ✅ PASS |
| DIV-01 AC5 só as divergências que atendem | filtro de status e tipo dentro do item | `LDT:51-54`; `DLT:84-91`; `DPT:175`, `DPT:185`, `DPT:190` | ✅ PASS |
| DIV-03 AC6 troca de janela | run vigente da nova janela, página 1, filtros mantidos | `DPT:220` path `r-aug/items`; `DPT:221` `toEqual` exato com `page:'0'`, `result` = `'DIVERGENT'`, `discrepancyType:'STATUS_MISMATCH'`; `DPT:222-224` | ✅ PASS |
| DIV-03 AC7 paginação | "Anterior", "Próxima", "Página X de Y"; bordas desabilitadas | `DLT:110` `'Página 2 de 3'`; `DLT:112-115` `onPageChange` 0 e 2; `DLT:118-129` bordas | ✅ PASS |
| DIV-03 AC8 vazio | "Nenhuma divergência para os filtros escolhidos." | `DLT:134-135` | ✅ PASS |
| DIV-03 AC9 erro | mensagem e "Tentar novamente" | `DLT:145-148` | ✅ PASS |
| DIV-04 AC1 sem run | texto e link `/conciliacoes` | `WNT:15-16`; `DPT:201-204` | ✅ PASS |
| DIV-04 AC2 em andamento | "Conciliação em andamento."; recarrega; lista ao concluir | `WNT:19-23` (PENDING e RUNNING); `DPT:263-271` | ✅ PASS |
| DIV-04 AC3 falha | texto, mensagem do run, link | `WNT:26-29`; `DPT:242-244` | ✅ PASS |
| DIV-05 AC4 travado | "Período travado: as divergências ficam só para consulta." | `WNT:34`; `WNT:37-44` só com run concluído; `DPT:361` | ✅ PASS |
| DIV-06 AC1 abrir | gaveta daquela divergência, dois lados, histórico | `DPT:299-305` segunda linha abre `IMP-T-002`, "2 de 2", sem `AVULSA-1`; `DLT:93-103`; histórico `RDT:71-81` | ✅ PASS |
| DIV-06 AC2 aceitar/ajustar/baixar | regras atuais da gaveta | `DPT:287` PATCH `…/r-oct/discrepancies/d-1`; `DPT:288` `{ status:'ACCEPTED', note:'conferido' }` | ✅ PASS |
| DIV-06 AC3 reabrir | `status` `OPEN` | `DPT:352` path `d-3`; `DPT:353` `toEqual({ status:'OPEN' })` | ✅ PASS |
| DIV-06 AC4 travado sem ações | sem Aceitar/Ajustar/Baixar/Reabrir | `DPT:364-365` | ✅ PASS |
| DIV-07 AC5 novo status e recarga | status na gaveta; lista recarrega | `DPT:289` `drawer-status` `'Aceita'`; `QDT:60` | ✅ PASS |
| DIV-07 AC6 fechar | lista sem a divergência que saiu do filtro | `DPT:291-292` `['IMP-T-002|Status inconsistente|Aberta']` | ✅ PASS |
| DIV-07 AC7 J/K e rodapé | J/K andam pela página; "<posição> de <total da página>" | `DPT:328-338` J/K; `DPT:318` "3 de 3 · J / K navega" (3 linhas de 2 itens); `RDT:233-235` | ✅ PASS |
| DIV-08 AC1 troca de empresa | fecha a gaveta, janela da nova empresa, filtros mantidos, página 1 | `DPT:380-389`; com cache: `DPT:409-412` | ✅ PASS |
| DIV-08 AC2 sem empresa | estado vazio, sem filtros nem lista | `DPT:419-423` | ✅ PASS |

**Status**: ✅ 22/22 ACs com evidência que discrimina. Sem spec-precision gap aberto.

### Edge Cases

- [x] Item com duas divergências abertas vira duas linhas: `LDT:43-45`; `DLT:56-61`.
- [x] Filtro Aceitas mostra só a aceita: `LDT:47-49`; `DLT:84-91`; `DPT:175`.
- [x] Lado ausente com "—", Esperado e Diferença do servidor: `DLT:59`.
- [x] Erro sem JSON: `frontend/lib/api.imports.test.ts:115` `'A API retornou erro 502.'` (cliente existente).

### Discrimination Sensor

Scratch: `git worktree add --detach` em `scratchpad/verify5` a partir de `79fea18`, com `frontend/node_modules` por symlink. Baseline do scratch: `next typegen` + `tsc --noEmit` exit 0; 227 testes em escopo passam (`components/discrepancies`, `components/dashboard`, `lib/discrepancies.test.ts`, `lib/queries.discrepancies.test.tsx`, `lib/api.reconciliations.test.ts`, `lib/queries.test.tsx`). Cada mutante passou por `tsc --noEmit` (todos compilam) e rodou `vitest run` nesse escopo. Depois de cada mutante, só o arquivo de produção mutado foi restaurado (`git checkout -- <arquivo>`).

| # | File | Mutação | Killed? | Testes que falharam |
| - | ---- | ------- | ------- | ------------------- |
| F7 | `DiscrepanciesPage.tsx:91` | "Todos" vira tipo `''` em vez de `null` | ✅ Killed | `DPT:164` (passo "Todos") |
| F1 | `DiscrepancyList.tsx:116` | "Anterior" nunca desabilitado | ✅ Killed | `DLT:118` página 0 |
| F2 | `DiscrepancyList.tsx:120` | "Próxima" habilitado na última página (`>=` → `>`) | ✅ Killed | `DLT:118` página 2 |
| Q3 | `DiscrepanciesPage.tsx:119` | `total` = itens distintos | ✅ Killed | `DPT:308` |
| N3 | `DiscrepanciesPage.tsx:108` | `onOpen` abre sempre a linha 0 | ✅ Killed | `DPT:295`, `DPT:308` |
| G1 | `WindowNotice.tsx:5` | link vai para `/conciliacao` | ✅ Killed | `WNT:13`, `WNT:25`, `DPT:193` |
| G2 | `lib/discrepancies.ts:18` | com "Todas", o tipo é descartado | ✅ Killed | `LDT:35`, `DPT:164`, `DPT:207` |
| G3 | `ResolveDrawer.tsx:280` | `footerNoun` nulo volta a mostrar "abertas" | ✅ Killed | `RDT:233`, `DPT:274`, `DPT:295`, `DPT:308` |
| G4 | `api.ts:364` | `discrepancyStatus=OPEN` some da URL | ✅ Killed | `ART:80` (OPEN), `DPT:153` |
| G5 | `queries.ts:51` | chave de `runItems` sem o tipo | ✅ Killed | `DPT:207` |
| G6 | `DiscrepancyList.tsx:94` | coluna Status sempre "Aberta" | ✅ Killed | `DLT:84`, `DPT:164`, `DPT:341`, `DPT:368` |
| G7 | `DiscrepanciesPage.tsx:107` | `onPageChange` sempre volta para a página 1 | ✅ Killed | `DPT:164`, `DPT:207`, `DPT:392` |
| G8 | `lib/discrepancies.ts:24` | `&&` → `\|\|` entre status e tipo em `rowsOf` | ✅ Killed | 11 testes (`LDT`, `DLT`, `DPT`) |
| G9 | `WindowNotice.tsx:29` | `PENDING` deixa de contar como em andamento (re-execução de F4) | ✅ Killed | `WNT:19` PENDING |

**Sensor depth**: ampliado (F7, F1, F2, Q3, N3 re-executados; G1-G8 novos; G9 repete F4).
**Saída**: 14/14 mortos. Nenhum sobrevivente.

**Isolamento**: `git status --porcelain` antes = vazio; depois da remoção do scratch = vazio (comparado com `diff`, antes de editar este relatório). Scratch removido com `git worktree remove --force` + `git worktree prune`; `git worktree list` não lista mais o scratch. Nenhum `git stash`.

### Gate Check

- **Gate command**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`
- **Saída**: 796 passed, 0 failed, 0 skipped (63 arquivos). Typecheck exit 0. Build ok, `/divergencias` estática.
- **Test count**: 794 → 796 (+2, os dois casos de `DLT:118`; o passo "Todos" entrou no `it` de `DPT:164`). Nenhum teste removido ou afrouxado.

### Code Quality

| Principle | Status |
| --- | --- |
| Minimum code / surgical changes | ✅ Fixes só em teste e numa linha da spec |
| Spec-anchored outcome check | ✅ `DPT:189` e `DLT:127-128` afirmam o resultado exato do texto da spec |
| Every test maps to a spec requirement | ✅ Os testes novos citam DIV-01 AC3-5 e DIV-03 AC7 |
| Documented guidelines followed | ✅ `AGENTS.md` (não enfraquecer testes), `frontend/AGENTS.md` |

### Requirement Traceability Update

| Requirement | Iteração 4 | Iteração 5 |
| --- | --- | --- |
| DIV-01 | ❌ Needs Fix (AC4 "Todos", F7) | ✅ Verified |
| DIV-02 | ✅ Verified | ✅ Verified |
| DIV-03 | ✅ Verified (spec-precision menor em AC7) | ✅ Verified |
| DIV-04 | ✅ Verified | ✅ Verified |
| DIV-05 | ✅ Verified | ✅ Verified |
| DIV-06 | ✅ Verified | ✅ Verified |
| DIV-07 | ✅ Verified | ✅ Verified |
| DIV-08 | ✅ Verified | ✅ Verified |

### Lessons

- PASS sem sinal novo: nada registrado.
- L-018/L-043 (exercitar cada valor listado numa AC) foram aplicadas na fix `e481c6e` e mataram F7. L-044 (fixar e testar as bordas da paginação) foi aplicada em `79fea18` e matou F1 e F2. Evitar falha não é sinal; o script não tem comando de "aplicada", então fica só esta nota.

**Docs desatualizados (reportados, não editados)**: `spec.md:147-156` (traceability ainda "Design / Pending", "0 mapped"); `tasks.md:12` (`Status: In Progress`); `.specs/STATE.md:102-108` Handoff (ainda em Specify, "confirmar a spec").

### Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 22/22 ACs; nenhum spec-precision gap aberto.
**Sensor**: 14/14 mortos.
**Gate**: 796 passed, typecheck e build OK.

**Next steps**: atualizar a traceability da spec, `tasks.md` e o Handoff do `STATE.md`; UAT manual em `/divergencias`; push e PR por decisão do usuário.

---

# Histórico: iteração 4

**Diff range da iteração 4**: `baa9d12^..cadc990` (16 commits).

### Veredito da iteração 4: reprovada

O gap da iteração 3 está fechado: Q3 morre. A fix só acrescenta um teste. Mas um mutante novo sobrevive: na página, voltar o Tipo para "Todos" não limpa o filtro (F7). O select entrega `''`, a página deveria virar `null` (`DiscrepanciesPage.tsx:91`). Com F7, a URL sai certa (o cliente ignora `''`, `api.ts:362`), mas `rowsOf` filtra por tipo `''` e a lista fica vazia. Nenhum teste escolhe um tipo e depois "Todos". Isso quebra a evidência de DIV-01 AC4 ("Todos SHALL remover o parâmetro") e AC5. O código está certo; falta o teste. Gate verde: 794 testes, typecheck e build. Backend sem mudança.

Abreviações: as mesmas da iteração 1 (abaixo).

---

## Iteração 4

### Fix `cadc990`

- **Só teste.** `git show --stat cadc990`: `DPT` +14. Nenhum arquivo de produção. 14 inserções, 0 remoções.
- **Nada enfraquecido.** O diff só tem o `it` novo (`DPT:303-315`). Nenhuma linha removida. Fixture sem mudança.
- **Backend.** `git diff --name-only baa9d12^..cadc990 -- backend` vazio.

### Gap da iteração 3 re-checado

| Gap | Spec-defined outcome | `file:line` + assertion | Status |
| --- | --- | --- | --- |
| DIV-07 AC7 total da página, com o edge de duas linhas | `spec.md:113`: rodapé "<posição> de <total da página>"; `spec.md:136`: item com duas divergências vira duas linhas | `DPT:307` Status "Todas"; `DPT:308` `rows()` com 3 linhas (2 itens: `AVULSA-1` e `IMP-T-002` com `d-2` e `d-3`); `DPT:310` clica "Abrir IMP-T-002 · Taxa divergente" (terceira linha); `DPT:313` `getByText('3 de 3 · J / K navega')` (posição 3 e total 3 = linhas, não itens); `DPT:314` gaveta com "Taxa divergente". Q3 morre; N3 também falha aqui | ✅ PASS |

### Gap novo

| Criterion | Spec-defined outcome | `file:line` + assertion | Status |
| --- | --- | --- | --- |
| DIV-01 AC4 "Todos" remove o tipo (e AC5 pela página) | `spec.md:71`: "Todos" SHALL remover o parâmetro; `spec.md:72`: mostrar as divergências que atendem aos filtros, ou seja, todos os tipos | `LDT:31-38` só prova `itemFilters(_, null)`. A conversão `'' → null` da página (`DiscrepanciesPage.tsx:91`) não tem teste: `DPT:183`, `DPT:207` e `DPT:368` escolhem um tipo, nenhum volta para "Todos". F7 sobrevive | ❌ GAP |

As outras 21 ACs seguem com a evidência da iteração 2 e 3 (o arquivo `DPT` só ganhou 14 linhas em 303; números de linha da iteração 3 acima de 302 deslocaram +14).

**Status**: ❌ 21/22 ACs com evidência que discrimina; 1 gap (DIV-01 AC4 "Todos" pela página).

**Spec-precision gap (menor)**: DIV-03 AC7 (`spec.md:74`) pede "Anterior", "Próxima" e "Página X de Y", mas não diz que "Anterior" fica desabilitado na página 1 nem "Próxima" na última. O código desabilita (`DiscrepancyList.tsx:116,120`); F1 e F2 sobrevivem porque `DLT:105-116` só testa a página do meio. Com F2, "Próxima" na última página leva a uma página vazia "Página N+1 de N". Não é gap de AC; vale fixar na spec e testar.

### Discrimination Sensor

Scratch: `git worktree add --detach` em `scratchpad/div-wt4` a partir de `cadc990`, com `frontend/node_modules` por symlink. Baseline do scratch: `next typegen` + `tsc --noEmit` exit 0; 193 testes em escopo passam. Cada mutante passou por `tsc --noEmit` (todos compilam) e rodou `vitest run` sobre `components/discrepancies`, `lib/discrepancies.test.ts`, `lib/queries.discrepancies.test.tsx` e `components/dashboard`. Revertido com `git checkout -- .` no scratch depois de cada mutante.

| # | File | Mutação | Killed? | Testes que falharam |
| - | ---- | ------- | ------- | ------------------- |
| Q3 | `DiscrepanciesPage.tsx:119` | `total` = itens distintos (`new Set(list.map(item.id)).size`) | ✅ Killed | `DPT:303` |
| N3 | `DiscrepanciesPage.tsx:108` | `onOpen` ignora o índice e abre sempre a linha 0 | ✅ Killed | `DPT:290`, `DPT:303` |
| F1 | `DiscrepancyList.tsx:116` | "Anterior" nunca desabilitado | ⚪ Survived, spec-precision (bordas não fixadas) | - |
| F2 | `DiscrepancyList.tsx:120` | "Próxima" habilitado na última página (`>=` → `>`) | ⚪ Survived, spec-precision (bordas não fixadas) | - |
| F3 | `DiscrepancyList.tsx:115` | "Página {page} de" (sem o +1) | ✅ Killed | `DLT:105` |
| F4 | `WindowNotice.tsx:29` | `PENDING` deixa de contar como em andamento | ✅ Killed | `WNT` "run PENDING" (DIV-04 AC2) |
| F5 | `DiscrepanciesPage.tsx:99` | aviso recebe `locked={false}` | ✅ Killed | travado (DIV-05, DIV-06 AC4) |
| F6 | `DiscrepanciesPage.tsx:121` | gaveta recebe `locked={false}` | ✅ Killed | travado (DIV-05, DIV-06 AC4) |
| F7 | `DiscrepanciesPage.tsx:91` | "Todos" vira tipo `''` em vez de `null` | ❌ Survived | - |
| F8 | `DiscrepancyList.tsx:120` | "Próxima" pede `page + 2` | ✅ Killed | `DLT:105` e 3 de `DPT` |

**Por que F7 sobrevive**: nenhum teste volta o Tipo para "Todos". Com uma sonda no scratch (no fim de `DPT:164`, escolher "Todos", afirmar a query `toEqual({ page:'0', size:'20', sort:'absDifference,desc', … })`, com `result` = `'DIVERGENT'` e 3 linhas) o teste passa no código real e falha com F7. A sonda foi descartada com o scratch. É uma falha real: o operador limpa o tipo e vê "Nenhuma divergência para os filtros escolhidos.".

**Sensor depth**: ampliado (Q3 e N3 re-executados + 8 novos).
**Saída**: 7/8 não equivalentes mortos (10 injetados; F1 e F2 contam como spec-precision). 1 sobrevivente.

**Isolamento**: `git status --porcelain` antes = vazio; depois da remoção do scratch = vazio (antes de editar este relatório). Scratch removido com `git worktree remove --force` + `git worktree prune`; `git worktree list` não lista mais o scratch. Nenhum `git stash`.

### Gate Check

- **Gate command**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`
- **Saída**: 794 passed, 0 failed, 0 skipped (63 arquivos). Typecheck exit 0. Build ok, `/divergencias` estática.
- **Test count**: 793 → 794 (+1, `DPT:303`). Nenhum teste removido ou afrouxado.

### Fix Plan

#### Fix 1: "Todos" limpa o tipo pela página (DIV-01 AC4, AC5) - Major

- **Root cause**: a conversão `value === '' ? null` (`DiscrepanciesPage.tsx:91`) não é exercitada; os testes só escolhem tipos.
- **Fix task**: no fim de `DPT:164-185`, `user.selectOptions(screen.getByLabelText('Tipo'), 'Todos')`; afirmar `query(lastItems())` `toEqual({ page:'0', size:'20', sort:'absDifference,desc', … })`, com `result` = `'DIVERGENT'` e `rows()` com 3 linhas. Mata F7 (verificado por sonda no scratch). Só teste.

#### Fix 2 (opcional): bordas da paginação (DIV-03 AC7) - Minor

- **Fix task**: fixar em `spec.md:74` que "Anterior" fica desabilitado na página 1 e "Próxima" na última; em `DLT`, com `page: 0` afirmar "Anterior" `toBeDisabled()`, e com `page: 2` de 3 afirmar "Próxima" `toBeDisabled()`. Mata F1 e F2.

### Requirement Traceability Update

| Requirement | Iteração 3 | Iteração 4 |
| --- | --- | --- |
| DIV-01 | ✅ Verified | ❌ Needs Fix (AC4 "Todos", F7) |
| DIV-02 | ✅ Verified | ✅ Verified |
| DIV-03 | ✅ Verified | ✅ Verified (spec-precision menor em AC7) |
| DIV-04 | ✅ Verified | ✅ Verified |
| DIV-05 | ✅ Verified | ✅ Verified |
| DIV-06 | ✅ Verified | ✅ Verified |
| DIV-07 | ❌ Needs Fix (AC7 total, Q3) | ✅ Verified |
| DIV-08 | ✅ Verified | ✅ Verified |

### Lessons

- F7 / DIV-01 AC4: o gap de AC entrou como recorrência de L-018 (`ac_gap`, mesmo texto, outra feature). O mutante sobrevivente entrou como L-043 (`surviving_mutant`, mesmo texto; a chave do script inclui o sinal, então não fundiu).
- F1/F2: nova candidata L-044 `spec_precision_gap` (fixar o estado desabilitado dos controles de paginação nas bordas).
- L-042 foi aplicada na fix e matou Q3. Evitar falha não é sinal.

**Docs desatualizados (reportados, não editados)**: `spec.md:144-155` (traceability Pending, "0 mapped"); `tasks.md:12` (`Status: In Progress`); `.specs/STATE.md` Handoff (ainda em Specify).

### Summary

**Overall**: ❌ Not Ready

**Spec-anchored check**: 21/22 ACs; 1 gap (DIV-01 AC4 "Todos" pela página); 1 spec-precision gap menor (bordas da paginação).
**Sensor**: 7/8 não equivalentes mortos (F7 sobrevive; F1, F2 spec-precision).
**Gate**: 794 passed, typecheck e build OK.

**Next steps**: Fix 1 (um bloco de 3 linhas em `DPT`), Fix 2 opcional; nova iteração só por decisão do usuário.

---

# Histórico: iteração 3

### Veredito da iteração 3: reprovada

O gap da iteração 2 está fechado: N3 morre. A fix só acrescenta um teste. Mas um mutante novo sobrevive: o rodapé da gaveta conta itens em vez de divergências (Q3). A tela mostra uma linha por divergência (edge de `spec.md:136`), e o rodapé deve ser "<posição> de <total da página>" (DIV-07 AC7, `spec.md:113`). Nenhum teste da página abre a gaveta numa página em que um item tem duas divergências visíveis. O código está certo (`DiscrepanciesPage.tsx:119`, `total={drawer.list.length}`); falta o teste. Gate verde: 793 testes, typecheck e build. Backend sem mudança.

## Iteração 3

### Fix `c5c736d`

- **Só teste.** `git show --stat c5c736d`: `DPT` +13. Nenhum arquivo de produção. 13 inserções, 0 remoções.
- **Nada enfraquecido.** `git diff b1e566d c5c736d` só tem o `it` novo (`DPT:290-301`). Nenhuma linha removida. Fixture sem mudança.
- **Backend.** `git diff --name-only baa9d12^..c5c736d -- backend` vazio.

### Gap da iteração 2 re-checado

| Gap | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| DIV-06 AC1 abrir pela página | `spec.md:107`: clicar "Abrir" numa linha abre a gaveta daquela divergência | `DPT:294` clica "Abrir IMP-T-002 · Status inconsistente" (segunda linha); `DPT:297` "2 de 2 · J / K navega"; `DPT:298-299` gaveta com `IMP-T-002` e "Status inconsistente"; `DPT:300` sem `AVULSA-1`. N3 morre | ✅ PASS |

### Gap novo

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| DIV-07 AC7 total da página (com o edge de duas divergências) | `spec.md:113`: rodapé "<posição> de <total da página>"; `spec.md:136`: um item com duas divergências vira duas linhas, logo o total conta divergências | Todos os rodapés afirmados pela página (`DPT:297`, `DPT:313`, e os de J/K) são "x de 2" com dois itens de uma divergência visível cada. `IMP-T-002` (`DPT:108`) tem `d-2` e `d-3`, mas nenhum teste abre a gaveta em "Todas" (3 linhas, 2 itens). `RDT:228-237` só prova que a gaveta mostra o `total` recebido. Q3 sobrevive | ❌ GAP |

As outras 21 ACs seguem com a evidência da iteração 2 (o arquivo `DPT` só ganhou 13 linhas em 290; números de linha da iteração 2 acima de 289 deslocaram +13).

**Status**: ❌ 21/22 ACs com evidência que discrimina; 1 gap (DIV-07 AC7, total da página). Sem spec-precision gap aberto.

### Discrimination Sensor

Scratch: `git worktree add --detach` em `scratchpad/div-wt3` a partir de `c5c736d`, com `frontend/node_modules` por symlink. Baseline do scratch: `next typegen` + `tsc --noEmit` exit 0; 192 testes em escopo passam. Cada mutante passou por `tsc --noEmit` (todos compilam) e rodou `vitest run` sobre `components/discrepancies`, `lib/discrepancies.test.ts`, `lib/queries.discrepancies.test.tsx` e `components/dashboard`. Revertido com `git checkout --` no scratch depois de cada mutante.

| # | File | Mutação | Killed? | Testes que falharam |
| - | ---- | ------- | ------- | ------------------- |
| N3 | `DiscrepanciesPage.tsx:108` | `onOpen` ignora o índice e abre sempre a linha 0 | ✅ Killed | `DPT:290` |
| Q1 | `DiscrepanciesPage.tsx:108` | `onOpen` abre `index + 1` (limitado ao fim da lista) | ✅ Killed | `DPT:303` J/K, teste de aceitar (DIV-06 AC2) |
| Q2 | `DiscrepanciesPage.tsx:116` | gaveta com `runId` = id do primeiro item, não o run vigente | ✅ Killed | aceitar (PATCH `r-oct`), Reabrir (DIV-06 AC3) |
| Q3 | `DiscrepanciesPage.tsx:119` | `total` = itens distintos da lista, não divergências | ❌ Survived | - |
| Q4 | `lib/discrepancies.ts:23-26` | `rowsOf` com a ordem invertida | ✅ Killed | 10 testes (`LDT`, `DLT:50`, `DLT:93`, `DPT`) |
| Q5 | `DiscrepancyList.tsx:99` | "Abrir" entrega a posição espelhada (`rows.length - 1 - index`) | ✅ Killed | `DPT:290`, `DPT:303`, aceitar |
| Q6 | `lib/discrepancies.ts:25` | filtro de tipo ignorado dentro do item | ✅ Killed | `LDT` "o tipo restringe as linhas" (DIV-01 AC5) |
| Q7 | `DiscrepanciesPage.tsx:123` | `onNavigate` corta a lista para a primeira linha | ✅ Killed | `DPT:303` |
| Q8 | `DiscrepancyList.tsx:99` | "Abrir" entrega a posição do item, não da divergência | ✅ Killed | `DLT:93` |

**Por que Q3 sobrevive**: nas páginas que os testes abrem, cada item mostra uma divergência só, então itens = linhas. Com uma sonda no scratch (Status "Todas", abrir "IMP-T-002 · Taxa divergente", afirmar "3 de 3 · J / K navega") o teste passa no código real e falha com Q3. A sonda foi descartada com o scratch. É uma falha real: o operador veria "3 de 2".

**Sensor depth**: ampliado (N3 re-executado + 8 novos).
**Saída**: 8/9 mortos. 1 sobrevivente.

**Isolamento**: `git status --porcelain` antes = vazio; depois da remoção do scratch = vazio (antes de editar este relatório). Scratch removido com `git worktree remove --force` + `git worktree prune`; `git worktree list` não lista mais o scratch. Nenhum `git stash`.

### Gate Check

- **Gate command**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`
- **Saída**: 793 passed, 0 failed, 0 skipped (63 arquivos). Typecheck exit 0. Build ok, `/divergencias` estática.
- **Test count**: 792 → 793 (+1, `DPT:290`). Nenhum teste removido ou afrouxado.

### Fix Plan

#### Fix 1: total do rodapé conta divergências (DIV-07 AC7) - Minor

- **Root cause**: o `total` da gaveta (`DiscrepanciesPage.tsx:119`) só é exercitado pela página com dois itens de uma divergência cada.
- **Fix task**: em `DPT`, escolher Status "Todas", clicar "Abrir IMP-T-002 · Taxa divergente" e afirmar na gaveta "3 de 3 · J / K navega". Mata Q3 (verificado por sonda no scratch). Só teste.
- **Severidade**: Minor. Texto errado no rodapé; a navegação e a resolução seguem certas.

### Requirement Traceability Update

| Requirement | Iteração 2 | Iteração 3 |
| --- | --- | --- |
| DIV-01 | ✅ Verified | ✅ Verified |
| DIV-02 | ✅ Verified | ✅ Verified |
| DIV-03 | ✅ Verified | ✅ Verified |
| DIV-04 | ✅ Verified | ✅ Verified |
| DIV-05 | ✅ Verified | ✅ Verified |
| DIV-06 | ❌ Needs Fix (AC1, N3) | ✅ Verified |
| DIV-07 | ✅ Verified | ❌ Needs Fix (AC7 total, Q3) |
| DIV-08 | ✅ Verified | ✅ Verified |

### Lessons

- Q3: nova lição candidata, escopo `frontend` (uma linha por filho exige fixture com um registro que gera várias linhas ao afirmar contagens e posições).
- L-040 foi aplicada na fix e matou N3. Evitar falha não é sinal.

**Docs desatualizados (reportados, não editados)**: `spec.md:144-155` (traceability Pending, "0 mapped"); `tasks.md:12` (`Status: In Progress`); `.specs/STATE.md:104-108` Handoff (ainda em Specify).

### Summary

**Overall**: ❌ Not Ready

**Spec-anchored check**: 21/22 ACs; 1 gap (DIV-07 AC7, total da página).
**Sensor**: 8/9 mortos (Q3 sobrevive).
**Gate**: 793 passed, typecheck e build OK.

**Next steps**: limite de 3 iterações atingido. Escalar ao usuário: aplicar o Fix 1 (um teste em `DPT`) e rodar uma iteração extra, ou aceitar o gap.

---

# Histórico: iteração 2

### Veredito da iteração 2: reprovada

Os cinco gaps da iteração 1 estão fechados. Os oito sobreviventes (M6, M10, M13, M14, M19, M20, M21, M23) agora morrem. A fix só acrescenta testes. Mas um mutante novo sobrevive: a página ignora a linha clicada e abre sempre a primeira divergência da página (N3). Nenhum teste da página clica "Abrir" numa linha que não seja a primeira e afirma qual divergência a gaveta mostra. Isso quebra a evidência de DIV-06 AC1 ("abrir a gaveta de resolução daquela divergência"). É o mesmo padrão de M20 (L-040): callback ligado pela página, testado só no componente com mock. O código está certo (`DiscrepanciesPage.tsx:108`); falta o teste. Gate verde: 792 testes, typecheck e build. Backend sem mudança.


### Fix `55e34e5`

- **Só testes e spec.** `git show --stat 55e34e5`: `spec.md` +1, `DPT` +74, `DLT` +9, `WNT` +9. Nenhum arquivo de produção. 93 inserções, 0 remoções.
- **Nada enfraquecido.** `git diff 18a53d8 55e34e5` não tem nenhuma linha removida. A fixture ganhou o item `i-8` de `r-aug` (`DPT:109`); nenhum teste antigo lia itens de agosto (o teste de janela sem run vai para setembro, `DPT:188`). Os testes antigos passam sem mudança.
- **Linha nova da spec** (`spec.md:41`, "Linha da Transação"). Bate com o código: parcelas só com `installments > 1` (`DiscrepancyList.tsx:21`), "—" no método e na data ausentes (`DiscrepancyList.tsx:20,22`). Bate com o dashboard no critério de parcelas (`OpenDiscrepancyTable.tsx:34`, mesmo `> 1`). Diferença menor, não é gap: o dashboard omite " · data" quando a data falta (`OpenDiscrepancyTable.tsx:36`); esta tela mostra "—", como a própria linha da spec diz e como `DLT:59` (`'REF-2— · —'`) afirma. A linha só invoca o dashboard para o "1x".
- **Backend.** `git diff --name-only baa9d12^..55e34e5 -- backend` vazio.

### Gaps da iteração 1 re-checados

| Gap | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| 1. DIV-07 AC7 J/K pela página | J/K andam pelas divergências da página; rodapé "<n> de <total da página>" | `DPT:296-297` "1 de 2 · J / K navega" e `AVULSA-1`; `j` → `DPT:300-302` "2 de 2 · J / K navega", `IMP-T-002`, "Status inconsistente"; `k` → `DPT:305-306` "1 de 2" e `AVULSA-1`. M20 morre | ✅ PASS |
| 2. DIV-03 AC6 troca de janela | run vigente da nova janela, página 1, filtros mantidos | Status "Todas", Tipo "Status inconsistente" e página 2 antes da troca (`DPT:206-210`); depois de agosto, `DPT:215` path `r-aug/items`; `DPT:216` `toEqual` com `page` = `'0'`, `size` = `'20'`, `sort` = `'absDifference,desc'`, `result` = `'DIVERGENT'`, `discrepancyType` = `'STATUS_MISMATCH'` (objeto exato); `DPT:217-218` selects `ALL` e `STATUS_MISMATCH`; `DPT:219` linha `AGO-1`. M6 e M23 morrem (L-036 aplicada) | ✅ PASS |
| 3. DIV-08 AC1 página 1 e cache | fecha a gaveta, janela da nova empresa, filtros mantidos, página 1 | Alfa em agosto, página 2, gaveta aberta (`DPT:364-370`); Beta: `DPT:373-374` `r-beta/items` com `page:'0'`; volta a Alfa (com `runs` em cache): `DPT:377-380` `r-oct/items`, `page:'0'`, "Outubro de 2026", sem dialog. M13 e M14 morrem (L-038, L-039 aplicadas) | ✅ PASS |
| 4. DIV-01 AC1 mês atual sem run | sem run, janela do mês atual | data 05/11/2026 (fora do mês das fixtures), Alfa sem runs; `DPT:227` "Nenhuma conciliação nesta janela."; `DPT:228` "Novembro de 2026"; `DPT:230` `/periods` com `{ fromDate:'2026-11-01', toDate:'2026-11-30' }`. M21 morre | ✅ PASS |
| 5a. Aviso de travado só com run concluído | spec: DIV-04 AC2/AC3 mostram andamento/falha; DIV-05 AC4 travado para run concluído | `WNT:37-44`: `FAILED` e `RUNNING` com `locked` mostram o estado do run (`WNT:42`) e não "Período travado…" (`WNT:43`). M10 morre | ✅ PASS |
| 5b. "1x" (spec-precision) | `spec.md:41` fixa "Pix · 03/10/2026" | `DLT:70` `toBe('REF-4Pix · 03/10/2026')`. M19 morre; o gap de precisão está fechado | ✅ PASS |

### Gap novo

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| DIV-06 AC1 abrir (pela página) | `spec.md:107`: clicar "Abrir" numa linha abre a gaveta **daquela divergência** | `DLT:93-102` prova o índice entregue a `onOpen` (mock). Na página, os cliques em linha que não é a primeira (`DPT:330` "Abrir IMP-T-002 · Status inconsistente", índice 1) não afirmam qual divergência a gaveta mostra. Os outros cliques (`DPT:273`, `DPT:294`, `DPT:315`, `DPT:343`, `DPT:369`) são todos na primeira (ou única) linha. N3 sobrevive | ❌ GAP |

As demais 18 ACs da iteração 1 seguem com a mesma evidência (o arquivo só ganhou linhas; números de linha da iteração 1 em `DPT` acima de 199 deslocaram +32 ou +54).

**Status**: ❌ 21/22 ACs com evidência que discrimina; 1 gap (DIV-06 AC1 pela página). Sem spec-precision gap aberto.

### Discrimination Sensor

Scratch: `git worktree add --detach` em `scratchpad/div-wt2` a partir de `55e34e5`, com `frontend/node_modules` por symlink. Baseline do scratch: `next typegen` + `tsc --noEmit` exit 0; 191 testes em escopo passam. Cada mutante passou por `tsc --noEmit` (todos compilam) e rodou `vitest run` sobre `components/discrepancies`, `lib/discrepancies.test.ts`, `lib/queries.discrepancies.test.tsx` e `components/dashboard`. Revertido com `git checkout -- .` no scratch depois de cada mutante.

| # | File | Mutação | Killed? | Testes que falharam |
| - | ---- | ------- | ------- | ------------------- |
| M6 | `DiscrepanciesPage.tsx:68` | janela não volta para a página 1 | ✅ Killed | `DPT:202` |
| M10 | `WindowNotice.tsx:21` | aviso de travado antes dos outros ramos (`locked && run !== null`) | ✅ Killed | `WNT:37` (2 casos) |
| M13 | `DiscrepanciesPage.tsx:159` | sem `key={merchantId}` | ✅ Killed | `DPT:360` |
| M14 | `DiscrepanciesPage.tsx:55,137` | página no pai, passada por props | ✅ Killed | `DPT:360` |
| M19 | `DiscrepancyList.tsx:21` | `installments >= 1`: "1x" aparece | ✅ Killed | `DLT:64` |
| M20 | `DiscrepanciesPage.tsx:123` | `onNavigate={() => {}}` | ✅ Killed | `DPT:290` |
| M21 | `DiscrepanciesPage.tsx:156` | sem run, janela do mês anterior | ✅ Killed | `DPT:222` |
| M23 | `DiscrepanciesPage.tsx:68` | troca de janela zera os filtros | ✅ Killed | `DPT:202` |
| N1 | `DiscrepanciesPage.tsx:155` | janela inicial no run mais antigo (`content.at(-1)`) | ✅ Killed | 11 testes de `DPT` |
| N2 | `DiscrepanciesPage.tsx:62` | `locked` ignora o período travado | ✅ Killed | `DPT:324` |
| N3 | `DiscrepanciesPage.tsx:108` | `onOpen` ignora o índice e abre sempre a linha 0 | ❌ Survived | - |
| N4 | `DiscrepanciesPage.tsx:119` | `total` do rodapé com +1 | ✅ Killed | `DPT:269`, `DPT:290` |

**Por que N3 sobrevive**: o teste de travado (`DPT:330`) clica na segunda linha, mas só afirma que não há ações. Com N3 a gaveta abre AVULSA-1 e o teste passa. É uma falha real: o operador abriria outra divergência e poderia resolvê-la.

**M9 e M26**: seguem fora da spec, como na iteração 1. Nada na fix muda isso.

**Sensor depth**: ampliado (8 re-execuções + 4 novos).
**Saída**: 11/12 mortos. 1 sobrevivente.

**Isolamento**: `git status --porcelain` antes = vazio; depois da remoção do scratch = vazio. Scratch removido com `git worktree remove --force` + `git worktree prune`; `git worktree list` não lista mais o scratch. Nenhum `git stash`.

### Gate Check

- **Gate command**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`
- **Saída**: 792 passed, 0 failed, 0 skipped (63 arquivos). Typecheck exit 0. Build ok, `/divergencias` estática.
- **Test count**: 785 → 792 (+7: `DPT` +4, `DLT` +1, `WNT` +2 casos). Nenhum teste removido ou afrouxado.

### Fix Plan

#### Fix 1: abrir a divergência clicada pela página (DIV-06 AC1) - Major

- **Root cause**: a ligação `onOpen={(list, index) => setDrawer({ list, index })}` (`DiscrepanciesPage.tsx:108`) só tem teste no componente, com mock (`DLT:93`).
- **Fix task**: em `DPT`, clicar "Abrir IMP-T-002 · Status inconsistente" (segunda linha) e afirmar na gaveta "2 de 2 · J / K navega" e o texto `IMP-T-002` / "Status inconsistente" (não `AVULSA-1`). Pode entrar no teste de travado (`DPT:330-334`) ou em um `it` novo. Mata N3. Só teste.

### Requirement Traceability Update

| Requirement | Iteração 1 | Iteração 2 |
| --- | --- | --- |
| DIV-01 | ❌ Needs Fix (AC1) | ✅ Verified |
| DIV-02 | ✅ Verified | ✅ Verified |
| DIV-03 | ❌ Needs Fix (AC6) | ✅ Verified |
| DIV-04 | ✅ Verified | ✅ Verified |
| DIV-05 | ✅ Verified | ✅ Verified |
| DIV-06 | ✅ Verified | ❌ Needs Fix (AC1 pela página, N3) |
| DIV-07 | ❌ Needs Fix (AC7) | ✅ Verified |
| DIV-08 | ❌ Needs Fix (AC1) | ✅ Verified |

### Lessons

- N3 é o mesmo padrão de L-040 (callback ligado pela página sem teste pela página). Registrado como nova evidência em L-040, com o mesmo texto. A recorrência conta features distintas, então L-040 segue `candidate`.
- L-036, L-038 e L-039 foram aplicadas na fix e mataram M6, M23, M13 e M14. L-041 fechou o gap do "1x" na spec. Evitar falha não é sinal; nada registrado para elas.

**Docs desatualizados (reportados, não editados)**: `spec.md:144-156` (traceability Pending, "0 mapped"); `tasks.md:12` (`Status: In Progress`); `.specs/STATE.md` Handoff (ainda em Specify).

### Summary

**Overall**: ❌ Not Ready

**Spec-anchored check**: 21/22 ACs; 1 gap (DIV-06 AC1 pela página).
**Sensor**: 11/12 mortos (N3 sobrevive).
**Gate**: 792 passed, typecheck e build OK.

**Next steps**: Fix 1 (só teste em `DPT`); depois, iteração 3.

---

# Histórico: iteração 1

**Diff range da iteração 1**: `baa9d12^..71174f1` (10 commits). Spec `baa9d12`, plano `8a24fe1`, código `2689579`..`71bdfb0`, docs `71174f1`.

### Veredito da iteração 1: reprovada

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
2. **`rowsOf` e paginação por item.** ✅ Uma linha por divergência que atende status e tipo (`lib/discrepancies.ts:22-26`). "Todas" vira `result` = `'DIVERGENT'` (`lib/discrepancies.ts:17`). M1-M4 morrem.
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
**Saída**: 20/28 não equivalentes mortos (30 injetados; M9 e M26 fora da spec). 8 sobreviventes.

**Isolamento**: `git status --porcelain` antes = vazio; depois da remoção do scratch = vazio. Scratch removido com `git worktree remove --force` + `git worktree prune`; `git worktree list` não lista mais o scratch. Nenhum `git stash`.

---

## Gate Check

- **Gate command**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`
- **Saída**: 785 passed, 0 failed, 0 skipped. Typecheck exit 0. Build ok, `/divergencias` estática.
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
- **Fix task**: em `DPT`, mudar Status para "Todas" e Tipo para "Status inconsistente", ir para a página 2, acrescentar no teste um item divergente de `r-aug`, trocar para agosto, e afirmar a última busca `/r-aug/items` com `page:'0'`, `result` = `'DIVERGENT'`, `discrepancyType:'STATUS_MISMATCH'` e os selects nos mesmos valores. Mata M6 e M23 (L-036).

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
