# Tela de Divergências Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/discrepancies-screen/design.md`
**Status**: In Progress

---

## Test Coverage Matrix

> Gerada do código, das diretrizes do projeto e da spec; confirmar antes do Execute. Diretrizes encontradas: `AGENTS.md` (não enfraquecer testes), `frontend/vitest.config.ts`, `.github/workflows/ci.yml`. Lições candidatas aplicadas: L-035 (texto procurado dentro da região da própria página), L-036 (mudar cada valor mantido antes de um reset), L-037 (fixture serve dados filhos conforme o estado do pai; provar os dados finais depois que o polling para).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Frontend ui existente (`ResolveDrawer`) | unit (jsdom) | Prop nova com teste próprio; testes atuais do dashboard e da gaveta inalterados e verdes | `frontend/components/dashboard/*.test.tsx` | `cd frontend && corepack pnpm test` |
| Frontend lib (`lib/api.ts`, `lib/discrepancies.ts`) | unit (node) | URL exata com cada combinação de status/tipo; `rowsOf` com item de várias divergências e filtros cruzados | `frontend/lib/*.test.ts` | `cd frontend && corepack pnpm test` |
| Frontend hooks (`lib/queries.ts`) | unit (jsdom) | Chave com status; resolver invalida a lista de itens; testes atuais de Conciliações inalterados | `frontend/lib/queries*.test.tsx` | `cd frontend && corepack pnpm test` |
| Frontend componentes e rota (`components/discrepancies/*`) | unit (jsdom) | 1:1 com DIV-01..DIV-08 e edge cases: lista, filtros, estados da janela, gaveta (resolver, reabrir, travado), isolamento | `frontend/components/discrepancies/*.test.tsx` | `cd frontend && corepack pnpm test` |
| Docs (README) | none | - (build gate only) | - | build gate only |

## Gate Check Commands

> Gerados do código; confirmar antes do Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Tasks com testes unitários de frontend | `cd frontend && corepack pnpm test && corepack pnpm typecheck` |
| Full | Sem backend nesta feature; igual ao Build | `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build` |
| Build | Fim de fase e docs | `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build` |

---

## Execution Plan

Phases run sequentially; tasks within a phase run in order.

### Phase 1: Base

```
T1
T2 → T3
T2 → T4
```

### Phase 2: Tela

```
T3 → T5
T4 → T5
T5 → T7
T6 → T7
T1 → T7
```

### Phase 3: Documentação

```
T7 → T8
```

---

## Task Breakdown

### T1: Rodapé configurável na gaveta de resolução

**What**: Adicionar `footerNoun` opcional à `ResolveDrawer` (padrão `'abertas'`; `null` omite o substantivo).
**Where**: `frontend/components/dashboard/ResolveDrawer.tsx`
**Depends on**: None
**Reuses**: gaveta atual
**Requirement**: DIV-07

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Sem a prop, o rodapé continua "<n> de <total> abertas · J / K navega"
- [x] Com `footerNoun={null}`, o rodapé fica "<n> de <total> · J / K navega"
- [x] Testes atuais da gaveta e do dashboard inalterados e verdes; teste novo no arquivo de testes da gaveta; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `refactor(frontend): let the resolve drawer footer drop its noun`

---

### T2: Filtro de status da divergência nos itens do run

**What**: Adicionar `discrepancyStatus` a `RunItemFilters` e a `getRunItems`.
**Where**: `frontend/lib/api.ts`
**Depends on**: None
**Reuses**: `getRunItems`
**Requirement**: DIV-01, DIV-02

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `discrepancyStatus=OPEN` (e os demais) entra no fim da URL só quando informado; combinado com `discrepancyType` e com `result=DIVERGENT`, a ordem dos parâmetros é fixa e testada
- [x] Os testes atuais de `getRunItems` inalterados e verdes
- [x] Testes novos em `frontend/lib/api.reconciliations.test.ts`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): filter run items by discrepancy status`

---

### T3: Chave de itens com status e recarga após resolver

**What**: Incluir `discrepancyStatus` em `queryKeys.runItems`, aceitar o prefixo só com a empresa e fazer `invalidateWindow` invalidar `['runItems', merchantId]`.
**Where**: `frontend/lib/queries.ts`
**Depends on**: T2
**Reuses**: `invalidateWindow`, `useRunItems`
**Requirement**: DIV-02, DIV-07

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Filtros com status diferente geram chaves diferentes e consultam a API de novo
- [x] Depois de `useResolveDiscrepancy` aceito, uma página de itens da mesma empresa já foi recarregada quando a mutação resolve (padrão `later()`)
- [x] Testes atuais de queries (dashboard, conciliações) inalterados e verdes; testes novos em `frontend/lib/queries.discrepancies.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): reload run items after resolving a discrepancy`

---

### T4: Regras puras da tela de divergências

**What**: Criar `STATUS_FILTERS`, `itemFilters` e `rowsOf`.
**Where**: `frontend/lib/discrepancies.ts`
**Depends on**: T2
**Reuses**: `DrawerEntry`, `RunItemFilters`
**Requirement**: DIV-02

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `STATUS_FILTERS` na ordem Abertas, Aceitas, Ajustadas, Baixadas, Todas
- [x] `itemFilters('ALL', null)` → `{ result: 'DIVERGENT' }`; `itemFilters('ACCEPTED', 'FEE_DIVERGENCE')` → `{ discrepancyStatus: 'ACCEPTED', discrepancyType: 'FEE_DIVERGENCE' }`
- [x] `rowsOf`: item com duas abertas → duas linhas na ordem; filtro Aceitas com uma aceita e uma aberta → só a aceita; filtro de tipo restringe; Todas → todas as divergências
- [x] Testes em `frontend/lib/discrepancies.test.ts`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add discrepancy screen helpers`

---

### T5: Lista de divergências

**What**: Criar `DiscrepancyList`, com a tabela, uma linha por divergência, "Abrir", a paginação "Página X de Y", o estado vazio e o erro.
**Where**: `frontend/components/discrepancies/DiscrepancyList.tsx`
**Depends on**: T3, T4
**Reuses**: marcação de `RunItems`, `OpenDiscrepancyTable` (coluna Transação), `formatMoney`, `TYPE_LABELS`, `STATUS_LABELS`
**Requirement**: DIV-01, DIV-02, DIV-03

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Colunas Transação, Tipo, Esperado, Diferença (com sinal), Status e "Abrir"; "—" no lado ausente
- [x] Item com duas divergências mostra duas linhas; "Abrir" entrega a lista da página e o índice da linha
- [x] Consulta com os filtros recebidos (status/tipo) e a página
- [x] Paginação "Página X de Y"; vazio "Nenhuma divergência para os filtros escolhidos."; erro com "Tentar novamente"
- [x] Testes em `frontend/components/discrepancies/DiscrepancyList.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): list the discrepancies of a run`

---

### T6: Aviso do estado da janela

**What**: Criar `WindowNotice` para janela sem run, run em andamento, run com falha e período travado.
**Where**: `frontend/components/discrepancies/WindowNotice.tsx`
**Depends on**: None
**Reuses**: `Link`
**Requirement**: DIV-04, DIV-05

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Sem run: "Nenhuma conciliação nesta janela." + link "Ir para conciliações" (`/conciliacoes`)
- [ ] `PENDING`/`RUNNING`: "Conciliação em andamento."
- [ ] `FAILED`: "A conciliação desta janela falhou.", a `errorMessage` e o link
- [ ] Concluído e travado: "Período travado: as divergências ficam só para consulta."; concluído e aberto: nada
- [ ] Testes em `frontend/components/discrepancies/WindowNotice.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): explain the reconciliation state of a window`

---

### T7: Página de divergências

**What**: Criar `DiscrepanciesPage` e `DiscrepanciesWindow`, com janela inicial, `PeriodMenu`, filtros, estados, lista, gaveta (resolver, reabrir, travado, J/K) e isolamento por empresa. Trocar o placeholder de `app/(app)/divergencias/page.tsx` e tirar `/divergencias` de `UnderConstruction.test.tsx`.
**Where**: `frontend/components/discrepancies/DiscrepanciesPage.tsx`
**Depends on**: T1, T5, T6
**Reuses**: lógica de janela e `locks` de `DashboardPage`, `ResolveDrawer`, servidor em memória dos testes de Conciliações e do dashboard
**Requirement**: DIV-01..DIV-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] A rota abre na janela do run mais recente (ou no mês atual sem run), com o nome da empresa dentro do cabeçalho da página (L-035), e busca as abertas da página 1
- [ ] Status, tipo e janela refazem a busca e voltam para a página 1; "Todas" usa `result=DIVERGENT`
- [ ] Sem run, run com falha e travado mostram o aviso; run em andamento mostra o aviso, recarrega a cada 3 s e mostra a lista ao concluir, com os itens servidos só depois do status final (L-037)
- [ ] "Abrir" abre a gaveta; aceitar com nota mostra o novo status, e ao fechar a lista não tem mais a linha no filtro Abertas; no filtro Aceitas, "Reabrir" envia `OPEN`
- [ ] Período travado: a gaveta abre sem Aceitar/Ajustar/Baixar/Reabrir
- [ ] Rodapé "<posição> de <total da página> · J / K navega"
- [ ] Troca de empresa com status e tipo fora do padrão (L-036): gaveta fecha, janela vai para o run da nova empresa, filtros mantidos, página 1
- [ ] Sem empresa: `NoMerchant`, sem filtros nem lista
- [ ] Testes em `frontend/components/discrepancies/DiscrepanciesPage.test.tsx` renderizando a rota; gate build passa

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): wire discrepancies page with filters and resolution`

---

### T8: README das divergências

**What**: Descrever `/divergencias` no README raiz e em `frontend/README.md`, tirando a rota da lista "em construção".
**Where**: `README.md`
**Depends on**: T7
**Reuses**: seções de frontend dos READMEs
**Requirement**: DIV-01..DIV-08 (documentação)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Os READMEs descrevem fonte (run vigente), filtros, estados, gaveta (resolver, histórico, reabrir); `/divergencias` sai da lista "em construção"
- [ ] Gate build passa

**Tests**: none
**Gate**: build

**Commit**: `docs: describe discrepancies screen`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3

Phase 1:  T1
          T2 → T3
          T2 → T4
Phase 2:  T3 → T5
          T4 → T5
          T5 → T7
          T6 → T7
          T1 → T7
Phase 3:  T7 → T8
```

8 tasks → um batch só; execução inline, sem sub-agentes.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: Rodapé da gaveta | 1 prop num componente | ✅ Granular |
| T2: Filtro na API | 1 função e 1 tipo | ✅ Granular |
| T3: Chave e invalidação | 1 chave e 1 função, mesmo arquivo | ✅ Granular |
| T4: Regras puras | 1 módulo | ✅ Granular |
| T5: Lista | 1 componente | ✅ Granular |
| T6: Aviso | 1 componente | ✅ Granular |
| T7: Página | 1 arquivo de página + troca de 1 linha na rota e 1 entrada de placeholder | ✅ Granular |
| T8: README | docs | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | Phase 1 | ✅ Match |
| T2 | None | Phase 1 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T2 | T2 → T4 | ✅ Match |
| T5 | T3, T4 | T3 → T5, T4 → T5 | ✅ Match |
| T6 | None | Phase 2 (sem dependência) | ✅ Match |
| T7 | T1, T5, T6 | T1 → T7, T5 → T7, T6 → T7 | ✅ Match |
| T8 | T7 | T7 → T8 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | ui existente | unit | unit | ✅ OK |
| T2 | lib | unit | unit | ✅ OK |
| T3 | hooks | unit | unit | ✅ OK |
| T4 | lib | unit | unit | ✅ OK |
| T5 | componente | unit | unit | ✅ OK |
| T6 | componente | unit | unit | ✅ OK |
| T7 | componente + rota | unit | unit | ✅ OK |
| T8 | docs | none | none | ✅ OK |
