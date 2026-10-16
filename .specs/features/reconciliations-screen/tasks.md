# Tela de Conciliações Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/reconciliations-screen/design.md`
**Status**: In Progress

---

## Test Coverage Matrix

> Gerada do código, das diretrizes do projeto e da spec; confirmar antes do Execute. Diretrizes encontradas: `AGENTS.md` (JUnit 5, MockMvc, AssertJ, Testcontainers; integração herda `AbstractIntegrationTest`; JaCoCo 85% linha / 75% branch no `verify`; não enfraquecer testes), `frontend/vitest.config.ts`, `.github/workflows/ci.yml`. Lições: L-035 (texto procurado dentro da região da própria página) e L-036 (mudar cada valor mantido antes de um reset) aplicadas como cautela.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Backend controller (export) | integration | `404` JSON para run inexistente e de outra empresa (status, content type, `error`, `message` exata); CSV atual inalterado | `backend/src/test/java/**/reconciliation/ReconciliationIntegrationTest.java` | `cd backend && ./mvnw verify` |
| Frontend lib (`lib/api.ts`, `lib/reconciliations.ts`) | unit (node) | URL e parâmetros exatos; limites 366/367 dias e janela de 1 dia; rótulos; progresso com `processedItems` nulo | `frontend/lib/*.test.ts` | `cd frontend && corepack pnpm test` |
| Frontend hooks (`lib/queries.ts`) | unit (jsdom) | Chaves por empresa, filtros e página; polling liga com run em andamento e desliga sem; invalidação de runs e da janela do dashboard após o disparo | `frontend/lib/queries*.test.tsx` | `cd frontend && corepack pnpm test` |
| Frontend componentes e rotas (`components/reconciliations/*`) | unit (jsdom) | 1:1 com os ACs de REC-02..REC-11 e edge cases: happy path, filtros, vazio, erro, 404, polling, export e isolamento | `frontend/components/reconciliations/*.test.tsx` | `cd frontend && corepack pnpm test` |
| Docs (README) | none | - (build gate only) | - | build gate only |

## Gate Check Commands

> Gerados do código; confirmar antes do Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Tasks com testes unitários de frontend | `cd frontend && corepack pnpm test && corepack pnpm typecheck` |
| Full | Task de backend (unit + integração + JaCoCo) | `cd backend && ./mvnw verify` |
| Build | Fim de fase de frontend e docs | `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build` |

---

## Execution Plan

Phases run sequentially; tasks within a phase run in order.

### Phase 1: Backend — export 404

```
T1
```

### Phase 2: Dados

```
T2 → T3
T2 → T4
T3 → T4
```

### Phase 3: Lista e disparo

```
T3 → T5
T4 → T5
T3 → T6
T4 → T6
T5 → T7
T6 → T7
```

### Phase 4: Detalhe e docs

```
T3 → T8
T4 → T8
T8 → T9
T7 → T10
T9 → T10
```

---

## Task Breakdown

### T1: Export de run inexistente responde 404

**What**: Validar o run em `ReconciliationController.exportCsv` antes de definir os cabeçalhos CSV e cobrir com testes de integração.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/reconciliation/controller/ReconciliationController.java`
**Depends on**: None
**Reuses**: `ReconciliationService.findRunById`, `ReconciliationIntegrationTest`
**Requirement**: REC-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Run inexistente: `404`, `Content-Type` `application/json`, `error` `NOT_FOUND`, `message` "Conciliação não encontrada com id: <runId>"
- [x] Run de outra empresa: o mesmo `404`
- [x] Os testes atuais de export (CSV, cabeçalho, prefixo `'`) continuam verdes sem alteração
- [x] Gate full passa (`./mvnw verify`, cobertura dentro do limite)

**Tests**: integration
**Gate**: full

**Commit**: `fix(reconciliation): answer 404 when exporting an unknown run`

---

### T2: Chamadas de run e itens no cliente da API

**What**: Adicionar `getRun`, `RunItemFilters` e `getRunItems`.
**Where**: `frontend/lib/api.ts`
**Depends on**: None
**Reuses**: `request`, `runPath`, `getRuns`
**Requirement**: REC-02, REC-05, REC-08, REC-09

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `getRun` faz `GET .../reconciliations/<runId>`
- [x] `getRunItems` monta `?page=N&size=20&sort=absDifference%2Cdesc` e inclui `result` e `discrepancyType` só quando informados, em ordem fixa
- [x] `getRuns` com `window` e sem `window` gera as URLs da lista (teste novo cobre a lista da tela; testes atuais inalterados)
- [x] Decimais dos itens continuam texto
- [x] Testes em `frontend/lib/api.reconciliations.test.ts`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add run detail and run item API calls`

---

### T3: Regras puras da tela de conciliações

**What**: Criar `RUN_STATUS_LABELS`, `RESULT_LABELS`, `MAX_WINDOW_DAYS`, `windowDays`, `newRunWindowError`, `itemsProgress`, `discrepancyBadge` e `anyInFlight`.
**Where**: `frontend/lib/reconciliations.ts`
**Depends on**: T2
**Reuses**: `TYPE_LABELS`, `STATUS_LABELS`, `INVALID_WINDOW_MESSAGE`
**Requirement**: REC-02, REC-03, REC-04, REC-06, REC-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Rótulos Na fila / Em andamento / Concluída / Falhou; Bateu / Divergente e Bateram / Divergentes
- [x] `newRunWindowError`: vazio → "Informe as duas datas"; invertido → mensagem de intervalo; 366 dias aceito (inclusive um ano bissexto); 367 → "Janela de conciliação excede o máximo de 366 dias"; um dia aceito
- [x] `itemsProgress`: `RUNNING` com `processedItems` nulo → "0 de N"; `RUNNING` 3 de 10 → "3 de 10"; `PENDING` → "—"; `COMPLETED` → total
- [x] `discrepancyBadge` → "Taxa divergente · Aberta"
- [x] `anyInFlight` verdadeiro com `PENDING` ou `RUNNING`, falso com `COMPLETED`/`FAILED`
- [x] Testes em `frontend/lib/reconciliations.test.ts`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add reconciliation screen helpers`

---

### T4: Hooks de conciliação

**What**: Adicionar `queryKeys.runPage`, `queryKeys.run`, `queryKeys.runItems`, `useRunPage`, `useRun`, `useRunItems` e `useStartReconciliation`.
**Where**: `frontend/lib/queries.ts`
**Depends on**: T2, T3
**Reuses**: `RUN_POLL_MS`, `inFlight`, padrão de `useLatestRun` e `useInvalidateWindow`
**Requirement**: REC-04, REC-07, REC-09

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `useRunPage` consulta empresa, janela e página pedidas; recarrega a cada `RUN_POLL_MS` com um run em andamento e para quando todos terminam
- [x] `useRun` recarrega enquanto o run está em andamento
- [x] Após `useStartReconciliation` aceito, a lista de runs e as consultas da janela do dashboard já foram recarregadas quando a mutação resolve; na recusa, nada é invalidado
- [x] Testes em `frontend/lib/queries.reconciliations.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add reconciliation run queries and start mutation`

---

### T5: Lista de runs

**What**: Criar `RunList` com a tabela de 7 colunas, o progresso, os selos de substituído e de falha, o link "Ver itens", a paginação, o estado vazio e o erro.
**Where**: `frontend/components/reconciliations/RunList.tsx`
**Depends on**: T3, T4
**Reuses**: marcação de `TransactionList`, `formatWindow`, `formatImportedAt`, `BlockError`
**Requirement**: REC-02, REC-03, REC-04, REC-05

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Linha concluída com "Outubro de 2026", "Concluída", 3, 2, 1, data e hora, link para `/conciliacoes/<id>`
- [x] `RUNNING` mostra "3 de 10"; `PENDING` "—"; substituído com selo; `FAILED` com a `errorMessage`
- [x] A lista recarrega sozinha enquanto há run em andamento e para depois (tempo controlado no teste)
- [x] Paginação, vazio "Nenhuma conciliação encontrada." e erro com "Tentar novamente"
- [x] Testes em `frontend/components/reconciliations/RunList.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): list reconciliation runs`

---

### T6: Gaveta de nova conciliação

**What**: Criar `NewRunDrawer` com o select "Mês" e os campos De e Até, a validação, o envio e os erros da API.
**Where**: `frontend/components/reconciliations/NewRunDrawer.tsx`
**Depends on**: T3, T4
**Reuses**: `SideDrawer`, `recentMonths`, `monthWindow`, padrão de `TransactionDrawer`
**Requirement**: REC-06, REC-07

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Abre no mês atual com De/Até do mês; escolher outro mês preenche as datas; editar uma data muda para "Personalizado"
- [x] Vazio, invertido e 367 dias mostram a mensagem e não enviam; 366 dias e um dia são enviados
- [x] Envio com `{"fromDate","toDate"}`; "Conciliando…" desabilitado durante o envio; sucesso chama `onStarted`
- [x] `409` e `400` mantêm a gaveta aberta com a mensagem
- [x] Testes em `frontend/components/reconciliations/NewRunDrawer.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): start reconciliations from a drawer`

---

### T7: Página de conciliações

**What**: Criar `ReconciliationsPage`, com cabeçalho, filtro "Janela", "Nova conciliação", reset após o disparo, isolamento por empresa e estado sem empresa. Trocar o placeholder de `app/(app)/conciliacoes/page.tsx` e tirar `/conciliacoes` de `UnderConstruction.test.tsx`.
**Where**: `frontend/components/reconciliations/ReconciliationsPage.tsx`
**Depends on**: T5, T6
**Reuses**: padrão `shownMerchantId`, servidor em memória de `TransactionsPage.test.tsx`
**Requirement**: REC-02, REC-05, REC-07, REC-11

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] A rota busca a página 1 sem janela e mostra o nome da empresa dentro do cabeçalho da página (L-035)
- [ ] Filtro "Janela" com um mês envia `fromDate`/`toDate` e volta para a página 1; "Todas" remove os dois
- [ ] Disparo aceito fecha a gaveta, volta o filtro para "Todas" e a página para 1, e o run novo aparece no topo como "Na fila"
- [ ] Troca de empresa fecha a gaveta, mantém um filtro de mês já escolhido (L-036) e busca a página 1 da nova empresa
- [ ] Sem empresa: `NoMerchant`, sem filtro, sem lista e sem "Nova conciliação"
- [ ] Testes em `frontend/components/reconciliations/ReconciliationsPage.test.tsx` renderizando a rota; gate build passa

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): wire reconciliations page with run history`

---

### T8: Itens de um run

**What**: Criar `RunItems` com os filtros "Resultado" e "Tipo de divergência", a tabela de 8 colunas, os selos, a paginação, o vazio, o erro e o polling.
**Where**: `frontend/components/reconciliations/RunItems.tsx`
**Depends on**: T3, T4
**Reuses**: `formatMoney`, `formatTransactionDate`, `METHOD_LABELS`, `discrepancyBadge`
**Requirement**: REC-08, REC-09

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Item com os dois lados mostra valores, `Diferença` do servidor e "Bateu"/"Divergente"; liquidação ausente mostra "—" no lado ausente; item sem divergência deixa a coluna vazia
- [ ] Duas divergências no mesmo item aparecem como dois selos com tipo e status
- [ ] "Bateram"/"Divergentes" e um tipo enviam `result`/`discrepancyType` e voltam para a página 1; "Todos" remove
- [ ] Vazio "Nenhum item encontrado para os filtros escolhidos."; erro com "Tentar novamente"
- [ ] Com `polling`, os itens recarregam a cada `RUN_POLL_MS`
- [ ] Testes em `frontend/components/reconciliations/RunItems.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): list the items of a reconciliation run`

---

### T9: Página de detalhe do run e export

**What**: Criar `RunDetailPage`, com cabeçalho do run, link de volta, 404, export CSV e volta para a lista na troca de empresa, e a rota `app/(app)/conciliacoes/[runId]/page.tsx` com `useParams`.
**Where**: `frontend/components/reconciliations/RunDetailPage.tsx`
**Depends on**: T8
**Reuses**: `useRun`, `useExportRun`, download de blob do dashboard
**Requirement**: REC-08, REC-09, REC-10, REC-11

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] A rota lê `runId` da URL e mostra janela, status, criado em, totais, selo "Substituído" e "Voltar para conciliações"
- [ ] `404` mostra "Conciliação não encontrada." com o link de volta
- [ ] "Exportar CSV" só em `COMPLETED`; baixa `reconciliation-<runId>.csv`; "Exportando…" durante o envio; erro exibido junto do botão
- [ ] Run em andamento recarrega até concluir e então mostra "Exportar CSV"
- [ ] Trocar de empresa chama `router.replace('/conciliacoes')`
- [ ] Testes em `frontend/components/reconciliations/RunDetailPage.test.tsx` renderizando a rota; gate build passa

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): show reconciliation run detail with CSV export`

---

### T10: README das conciliações

**What**: Descrever `/conciliacoes` e `/conciliacoes/[runId]` no README raiz e em `frontend/README.md` (saindo da lista "em construção"), e registrar o `404` do export no README raiz.
**Where**: `README.md`
**Depends on**: T7, T9
**Reuses**: seções de frontend e de API dos READMEs
**Requirement**: REC-01..REC-11 (documentação)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Os READMEs descrevem histórico, disparo, detalhe, filtros e export; `/conciliacoes` sai da lista "em construção"
- [ ] O README raiz cita o `404` do export para run inexistente
- [ ] Gate build passa

**Tests**: none
**Gate**: build

**Commit**: `docs: describe reconciliations screen`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1
Phase 2:  T2 → T3
          T2 → T4
          T3 → T4
Phase 3:  T3 → T5
          T4 → T5
          T3 → T6
          T4 → T6
          T5 → T7
          T6 → T7
Phase 4:  T3 → T8
          T4 → T8
          T8 → T9
          T7 → T10
          T9 → T10
```

10 tasks → dois batches de fases inteiras: Fases 1–3 (7 tasks) e Fase 4 (3 tasks).

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: Export 404 | 1 método de controller + casos de teste | ✅ Granular |
| T2: Chamadas da API | 2 funções no mesmo arquivo | ✅ Granular |
| T3: Regras puras | 1 módulo de funções puras | ✅ Granular |
| T4: Hooks | 4 hooks + chaves, mesmo arquivo | ✅ Granular |
| T5: Lista de runs | 1 componente | ✅ Granular |
| T6: Gaveta | 1 componente | ✅ Granular |
| T7: Página | 1 componente + troca de 1 linha na rota e 1 entrada do teste de placeholder | ✅ Granular |
| T8: Itens | 1 componente | ✅ Granular |
| T9: Detalhe | 1 componente + rota nova de 1 linha | ✅ Granular |
| T10: README | docs | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | Phase 1 | ✅ Match |
| T2 | None | início da Phase 2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T2, T3 | T2 → T4, T3 → T4 | ✅ Match |
| T5 | T3, T4 | T3 → T5, T4 → T5 | ✅ Match |
| T6 | T3, T4 | T3 → T6, T4 → T6 | ✅ Match |
| T7 | T5, T6 | T5 → T7, T6 → T7 | ✅ Match |
| T8 | T3, T4 | T3 → T8, T4 → T8 | ✅ Match |
| T9 | T8 | T8 → T9 | ✅ Match |
| T10 | T7, T9 | T7 → T10, T9 → T10 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | Backend controller | integration | integration | ✅ OK |
| T2 | lib | unit | unit | ✅ OK |
| T3 | lib | unit | unit | ✅ OK |
| T4 | hooks | unit | unit | ✅ OK |
| T5 | componente | unit | unit | ✅ OK |
| T6 | componente | unit | unit | ✅ OK |
| T7 | componente + rota | unit | unit | ✅ OK |
| T8 | componente | unit | unit | ✅ OK |
| T9 | componente + rota | unit | unit | ✅ OK |
| T10 | docs | none | none | ✅ OK |
