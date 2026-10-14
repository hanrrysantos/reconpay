# Dashboard Redesign Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/dashboard-redesign/design.md`
**Status**: Approved

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `AGENTS.md` (JUnit 5, Mockito, MockMvc, AssertJ, Testcontainers; não enfraquecer testes), `backend/pom.xml` (JaCoCo 85% linha / 75% ramo no `verify`), `.github/workflows/ci.yml` (`./mvnw -B verify`; `pnpm test`, `pnpm typecheck`, `pnpm build`), `frontend/package.json` (Vitest). Amostras: `reconciliation/service/*Test.java`, `reconciliation/*IntegrationTest.java`, `frontend/lib/api.test.ts`, `frontend/components/dashboard.test.ts`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Backend service / matcher / policy | unit | Todos os ramos; 1:1 com os ACs de DASH-08/09/10; cada borda da spec | `backend/src/test/java/**/service/*Test.java` | `cd backend && ./mvnw -B test -Dtest=ClassName` |
| Backend controller / repository / run state | integration | Rotas e queries da tarefa: sucesso, borda e erro (400, 401, 403, 404, 409). Testcontainers via `AbstractIntegrationTest` | `backend/src/test/java/**/*IntegrationTest.java` | `cd backend && ./mvnw -B verify` |
| Backend OpenAPI | integration | Operação nova no `/v3/api-docs` com exemplo e códigos em português | `backend/src/test/java/**/openapi/*Test.java` | `cd backend && ./mvnw -B verify` |
| Flyway / entity / DTO sem ramo | none | Gate de compilação + contexto sobe no `verify` | — | `cd backend && ./mvnw -B -DskipTests compile` |
| Frontend lib pura (money, period, charts, view state) | unit | Todos os ramos; cada AC numérico e cada edge case da spec | `frontend/lib/*.test.ts`, `frontend/components/**/*.test.ts` | `cd frontend && corepack pnpm test` |
| Frontend API client / hooks | unit | Cada endpoint: URL, método, corpo, parse decimal como string, erro 401/403/404/409/rede; hooks: chave, polling, invalidação | `frontend/lib/*.test.ts(x)` | `cd frontend && corepack pnpm test` |
| Frontend componentes | unit (jsdom + Testing Library) | Cada AC de UI do componente: render com dados, estado vazio/erro, interações (clique, teclado, submit bloqueado) | `frontend/components/**/*.test.tsx` | `cd frontend && corepack pnpm test` |
| Frontend config / tema / dependências | none | Typecheck + build | — | `cd frontend && corepack pnpm typecheck && corepack pnpm build` |
| Documentação | none | — | — | build gate only |

## Gate Check Commands

> Generated from codebase - confirm before Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Tarefa só com teste unitário | Backend: `cd backend && ./mvnw -B test -Dtest=ClassName` · Frontend: `cd frontend && corepack pnpm test` |
| Full | Integração, OpenAPI, repositório ou wiring de página | Backend: `cd backend && ./mvnw -B verify` · Frontend: `cd frontend && corepack pnpm test && corepack pnpm typecheck` |
| Build | Migration, entidade, config, tema, fim de fase | Backend: `cd backend && ./mvnw -B -DskipTests compile` (fim de fase: `./mvnw -B verify`) · Frontend: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build` |

---

## Execution Plan

Phases run in order; tasks within a phase run in order.

### Phase 1: Backend — schema, banco e progresso do run

```
T1 → T2 → T3 → T4 → T5
```

### Phase 2: Backend — items e resumo do período

```
T6 → T7 → T8 → T9 → T10 → T11
```

### Phase 3: Frontend — fundação

```
T12 → T13 → T14 → T15 → T16 → T17 → T18
```

### Phase 4: Frontend — casca e estado de tela

```
T19 → T20 → T21 → T22
```

### Phase 5: Frontend — blocos de indicadores

```
T23 → T24 → T25 → T26 → T27 → T28 → T29 → T30
```

### Phase 6: Frontend — divergências e estados

```
T31 → T32 → T33
```

### Phase 7: Frontend — integração da página

```
T34 → T35
```

---

## Task Breakdown

### T1: Migration V23 e campos nas entidades

**What**: Criar `V23__dashboard_summary_support.sql` (`reconciliation_items.bank_amount NUMERIC(19,2)`, `bank_movement_date DATE`; `reconciliation_runs.bank_tracked BOOLEAN NOT NULL DEFAULT FALSE`, `processed_items INTEGER`) e mapear os campos em `ReconciliationItemEntity` e `ReconciliationRunEntity`.
**Where**: `backend/src/main/resources/db/migration/V23__dashboard_summary_support.sql`
**Depends on**: None
**Reuses**: `V22__create_period_locks.sql` (estilo), entidades existentes
**Requirement**: DASH-08, DASH-10

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Migration aplicada pelo Flyway no contexto de teste (sem `ddl-auto`)
- [x] Campos mapeados com `precision = 19, scale = 2` onde for dinheiro
- [x] Gate passa: `cd backend && ./mvnw -B -DskipTests compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(reconciliation): add bank and progress columns for dashboard summary`

---

### T2: Matcher grava o crédito casado no item

**What**: `BankStatementMatcher` preenche `bankAmount` e `bankMovementDate` do item ao casar por referência e por data+valor; sem casamento ou com casamento ambíguo, ficam nulos.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/reconciliation/service/BankStatementMatcher.java`
**Depends on**: T1
**Reuses**: `BankStatementMatcherTest` (fixtures)
**Requirement**: DASH-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Casamento por referência grava valor e data da linha
- [x] Casamento por data+valor grava valor e data da linha
- [x] Linha órfã, crédito ausente e ambíguo deixam os campos nulos
- [x] Gate passa: `cd backend && ./mvnw -B test -Dtest=BankStatementMatcherTest`; contagem anterior de testes da classe mantida + novos

**Tests**: unit
**Gate**: quick

**Commit**: `feat(reconciliation): record matched bank credit on items`

---

### T3: Escrita de progresso em transação própria

**What**: `ReconciliationRunStateService.recordTotal(runId, total)` e `recordProgress(runId, processed)` em `REQUIRES_NEW` via `UPDATE` JPQL.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/reconciliation/service/ReconciliationRunStateService.java`
**Depends on**: T2
**Reuses**: padrão de transação própria já usado no serviço; `RunStateIntegrationTest`
**Requirement**: DASH-10

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Valores ficam visíveis para outra transação antes do commit do processor
- [x] `processedItems` nunca é gravado acima de `totalItems`
- [x] Gate passa: `cd backend && ./mvnw -B verify`

**Tests**: integration
**Gate**: full

**Commit**: `feat(reconciliation): persist run progress in its own transaction`

---

### T4: Processor publica progresso e marca bankTracked

**What**: `ReconciliationRunProcessor` chama `recordTotal` após engine+matcher, `recordProgress` após cada lote, marca `bankTracked = true` e grava `processedItems = totalItems` no `save` final.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/reconciliation/service/ReconciliationRunProcessor.java`
**Depends on**: T3
**Reuses**: `persistInBatches`
**Requirement**: DASH-10, DASH-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `PENDING` mantém `processedItems` nulo; `COMPLETED` tem `processedItems = totalItems`
- [x] Run concluído tem `bankTracked = true`
- [x] Falha no meio deixa o run `FAILED` sem item persistido (comportamento atual preservado)
- [x] Gate passa: `cd backend && ./mvnw -B verify`

**Tests**: integration
**Gate**: full

**Commit**: `feat(reconciliation): report progress while processing a run`

---

### T5: Run expõe processedItems

**What**: `ReconciliationRunResponseDTO` e o mapper ganham `processedItems`; a interface `ReconciliationControllerApi` documenta o campo.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/reconciliation/dto/ReconciliationRunResponseDTO.java`
**Depends on**: T4
**Reuses**: `IReconciliationMapper`
**Requirement**: DASH-10

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `GET .../reconciliations/{runId}` devolve `processedItems` (null em `PENDING`, igual a `totalItems` em `COMPLETED`)
- [x] Gate passa: `cd backend && ./mvnw -B verify`

**Tests**: integration
**Gate**: full

**Commit**: `feat(reconciliation): expose processed items on run response`

---

### T6: Filtros de divergência por EXISTS e discrepancyStatus

**What**: `ReconciliationItemSpecifications.withFilters` recebe `discrepancyStatus`; tipo e status viram subqueries `EXISTS` (sem `join` + `distinct`).
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/reconciliation/repository/ReconciliationItemSpecifications.java`
**Depends on**: None (fase anterior concluída)
**Reuses**: filtros existentes
**Requirement**: DASH-09

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `discrepancyStatus=OPEN` devolve só items com ao menos uma divergência `OPEN`
- [ ] `result`, `discrepancyType` e `discrepancyStatus` combinam
- [ ] Item com duas divergências do mesmo tipo aparece uma vez
- [ ] Gate passa: `cd backend && ./mvnw -B verify`

**Tests**: integration
**Gate**: full

**Commit**: `feat(reconciliation): filter run items by discrepancy status`

---

### T7: Ordem por maior diferença nos items

**What**: `ReconciliationService.findItems` aceita a ordenação `absDifference` e o controller/`*Api` publicam `discrepancyStatus` e a ordenação.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/reconciliation/service/ReconciliationService.java`
**Depends on**: T6
**Reuses**: `Pageable` do endpoint de items
**Requirement**: DASH-09

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `sort=absDifference,desc` ordena por `|coalesce(settlementNet,0) − coalesce(expectedNet,0)|`, desempate por `externalReference`
- [ ] Lado ausente conta como zero
- [ ] `discrepancyStatus` inválido responde 400 `VALIDATION_ERROR`
- [ ] OpenAPI documenta parâmetro e ordenação em português
- [ ] Gate passa: `cd backend && ./mvnw -B verify`

**Tests**: integration
**Gate**: full

**Commit**: `feat(reconciliation): sort run items by absolute difference`

---

### T8: Política de janela compartilhada

**What**: Extrair de `PeriodService` a validação de janela, o run vigente e a recusa de run em andamento para `PeriodWindowPolicy`, usada por `PeriodService`.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/reconciliation/service/PeriodWindowPolicy.java`
**Depends on**: T7
**Reuses**: `PeriodService.ensureWindow`, `currentCompleted`, `rejectInFlight`
**Requirement**: DASH-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Datas invertidas e janela acima de `max-window-days` lançam a exceção que vira 400
- [ ] Sem run vigente lança a exceção que vira 404; run em andamento, a que vira 409
- [ ] `PeriodServiceTest` e `PeriodIntegrationTest` continuam passando sem alteração de asserção
- [ ] Gate passa: `cd backend && ./mvnw -B test -Dtest='PeriodWindowPolicyTest,PeriodServiceTest'`

**Tests**: unit
**Gate**: quick

**Commit**: `refactor(reconciliation): extract period window policy`

---

### T9: Agregações do resumo no repositório

**What**: Consultas JPQL em `IReconciliationItemRepository` (totais do run, série por `transactionDate`, excesso de taxa) e em `IReconciliationDiscrepancyRepository` (abertas por tipo).
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/reconciliation/repository/IReconciliationItemRepository.java`
**Depends on**: T8
**Reuses**: repositórios existentes
**Requirement**: DASH-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Totais e série batem com fixture conhecida, inclusive valores com centavos
- [ ] Liquidação órfã (sem `transactionDate`) fica fora da série e dos totais de venda
- [ ] Abertas por tipo contam só status `OPEN` do run pedido
- [ ] Gate passa: `cd backend && ./mvnw -B verify`

**Tests**: integration
**Gate**: full

**Commit**: `feat(reconciliation): add period summary aggregate queries`

---

### T10: PeriodSummaryService

**What**: Montar `PeriodSummaryResponseDTO`: totais, um dia por data da janela (dias sem item com zeros), `openByType`, banco nulo quando `bankTracked = false`, `previousRunMatchRate` e `previousWindowGrossAmount`.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/reconciliation/service/PeriodSummaryService.java`
**Depends on**: T9
**Reuses**: `PeriodWindowPolicy`, consultas de T9
**Requirement**: DASH-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Cada campo do contrato do design calculado 1:1 com DASH-08 AC1–AC4
- [ ] Dinheiro em `BigDecimal` com escala 2
- [ ] Sem run anterior ou janela anterior, comparativos nulos
- [ ] Gate passa: `cd backend && ./mvnw -B test -Dtest=PeriodSummaryServiceTest`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(reconciliation): build period summary`

---

### T11: Endpoint GET /periods/summary

**What**: `PeriodController.summary` + `PeriodControllerApi` (AD-006) com exemplo e códigos 200/400/401/403/404/409 em português.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/reconciliation/controller/PeriodController.java`
**Depends on**: T10
**Reuses**: `PeriodController`, `MerchantAccessAspect`, `PATH_PERIODS_NESTED`
**Requirement**: DASH-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] OPERATOR com grant e ADMIN recebem 200 com o contrato do design
- [ ] 400 janela inválida, 401 sem token, 403 OPERATOR sem grant, 404 sem run vigente, 409 run em andamento
- [ ] Operação aparece no `/v3/api-docs` com exemplo e textos em português
- [ ] Gate passa: `cd backend && ./mvnw -B verify` (JaCoCo dentro do limite)

**Tests**: integration
**Gate**: full

**Commit**: `feat(reconciliation): expose period summary endpoint`

---

### T12: Dependências e ambiente de teste do frontend

**What**: Adicionar `@tanstack/react-query`, `jsdom`, `@testing-library/react`, `@testing-library/user-event` e `@testing-library/jest-dom`; criar `vitest.config.ts` com ambiente jsdom para `*.test.tsx` e alias `@/`.
**Where**: `frontend/package.json`
**Depends on**: None (fase anterior concluída)
**Reuses**: lockfile pnpm
**Requirement**: DASH-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `pnpm install --frozen-lockfile` funciona com o lockfile atualizado
- [ ] `pnpm audit --audit-level high` sem achado novo
- [ ] Os 10 testes atuais continuam passando
- [ ] Gate passa: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`

**Tests**: none
**Gate**: build

**Commit**: `chore(frontend): add query client and component test tooling`

---

### T13: Tema Lima e fontes

**What**: `globals.css` passa a ter só Tailwind + `@theme` de `design-reference.md`; `app/layout.tsx` carrega Inter, Inter Tight e IBM Plex Mono via `next/font/google`.
**Where**: `frontend/app/globals.css`
**Depends on**: T12
**Reuses**: tokens de `design-reference.md`
**Requirement**: DASH-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Nenhuma classe do CSS antigo (`.sidebar`, `.metric-card`…) permanece
- [ ] Tokens acessíveis como utilitários (`bg-surface`, `text-muted`, `font-display`)
- [ ] Gate passa: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`

**Tests**: none
**Gate**: build

**Commit**: `style(frontend): apply lima theme tokens and fonts`

---

### T14: Formatação e aritmética de dinheiro

**What**: `lib/money.ts` com `formatMoney(value, { currency, signed })` e `subtractDecimal(a, b)` em centavos `BigInt`; testes de `formatAmount` migram para cá.
**Where**: `frontend/lib/money.ts`
**Depends on**: T13
**Reuses**: `formatAmount` de `components/dashboard.tsx`
**Requirement**: DASH-03, DASH-04

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `R$ 10.389,38`, `−4.870,00` (U+2212) e valor sem `R$` formatados como na referência
- [ ] Valor acima de 13 dígitos sem arredondar (edge case)
- [ ] `subtractDecimal('1297.03','2287.13') = '-990.10'`; lado nulo vale zero
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add decimal-safe money helpers`

---

### T15: Janelas de período

**What**: `lib/period.ts` com `monthWindow`, `monthsBack`, `formatWindow` e rótulos pt-BR.
**Where**: `frontend/lib/period.ts`
**Depends on**: T14
**Reuses**: —
**Requirement**: DASH-02

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Mês = dia 1 ao último dia (fevereiro bissexto incluso)
- [ ] `monthsBack` vai do mês atual ao mais antigo com run, sem duplicar
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add period window helpers`

---

### T16: Geometria dos gráficos

**What**: `lib/charts.ts` com `ringDash`, `seriesPath`, `gapPolygon`, `donutSegments` e `heatBand`.
**Where**: `frontend/lib/charts.ts`
**Depends on**: T15
**Reuses**: medidas de `design-reference.md`
**Requirement**: DASH-03

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `ringDash('0.9420', 439.8)` = `414.3 439.8`; taxa 0 e 1 nos extremos
- [ ] `gapPolygon` fecha o polígono entre as duas séries
- [ ] `heatBand` cobre as cinco faixas e `empty` para dia sem item
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add svg chart geometry helpers`

---

### T17: Cliente da API

**What**: `lib/api.ts` ganha tipos e funções para runs, período, resumo, items abertos, divergência (GET/PATCH), lock/unlock, imports e export (blob), com `ApiError` carregando `status` e `code`.
**Where**: `frontend/lib/api.ts`
**Depends on**: T16
**Reuses**: `request`, `lossless-json`
**Requirement**: DASH-02, DASH-04, DASH-05, DASH-07, DASH-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Cada função monta URL, método e corpo esperados (PATCH: `correctionAmount` como string decimal, nota em branco omitida)
- [ ] Decimais chegam como string
- [ ] Erros 401/403/404/409 e rede viram `ApiError` com status
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add dashboard api client`

---

### T18: Hooks de dados e providers

**What**: `lib/queries.ts` com as queries e mutações do design (chaves por empresa+janela, polling de 3 s, `keepPreviousData`, invalidação) e `app/providers.tsx` com `QueryClientProvider` e tratamento global de 401.
**Where**: `frontend/lib/queries.ts`
**Depends on**: T17
**Reuses**: cliente de T17, `useAuth`
**Requirement**: DASH-02, DASH-04, DASH-05, DASH-06, DASH-07

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Run `RUNNING` é refeito a cada 3 s e para em `COMPLETED`/`FAILED`
- [ ] Mutação bem-sucedida invalida período, resumo, abertas, detalhe e runs da mesma chave
- [ ] Troca de empresa não entrega resposta antiga ao bloco atual
- [ ] 401 chama `signOut`
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add dashboard query hooks`

---

### T19: Layout autenticado e TopNav

**What**: `app/(app)/layout.tsx` com guarda de sessão e `components/shell/TopNav.tsx` com os seis itens e ativo por rota.
**Where**: `frontend/components/shell/TopNav.tsx`
**Depends on**: None (fase anterior concluída)
**Reuses**: `useAuth`, providers de T18
**Requirement**: DASH-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Sem token, qualquer rota do grupo redireciona para `/login`
- [ ] Item da rota atual marcado como ativo (`aria-current="page"`)
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add authenticated shell with top navigation`

---

### T20: Menu de empresa e seleção

**What**: `MerchantMenu` (busca por nome ou CNPJ, marca a atual, "Cadastrar empresa" → `/empresas`) e contexto `useSelectedMerchant`.
**Where**: `frontend/components/shell/MerchantMenu.tsx`
**Depends on**: T19
**Reuses**: `getMerchants` (todas as páginas)
**Requirement**: DASH-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Lista todas as empresas de todas as páginas
- [ ] Busca filtra por nome e por dígitos do CNPJ
- [ ] Selecionar outra empresa atualiza o contexto
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add merchant menu`

---

### T21: Rotas vazias

**What**: `UnderConstruction` e as páginas `/conciliacoes`, `/divergencias`, `/transacoes`, `/importacoes`, `/fechamento`, `/empresas`.
**Where**: `frontend/components/shell/UnderConstruction.tsx`
**Depends on**: T20
**Reuses**: layout de T19
**Requirement**: DASH-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Cada rota renderiza a casca com "em construção" e não dispara request
- [ ] Gate passa: `cd frontend && corepack pnpm test && corepack pnpm typecheck`

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend): add placeholder routes for upcoming pages`

---

### T22: deriveDashboardView

**What**: Função pura que decide `loading | noMerchant | forbidden | empty | running | failed | open | locked` a partir do estado das queries.
**Where**: `frontend/components/dashboard/view.ts`
**Depends on**: T21
**Reuses**: tipos de T17
**Requirement**: DASH-05, DASH-06, DASH-07

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Cada view tem teste com a entrada mínima que a produz
- [ ] 409 do período com run em andamento resulta em `running` com dados anteriores
- [ ] 404 do período resulta em `empty`
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): derive dashboard view state`

---

### T23: Menu de período

**What**: `PeriodMenu` com atalhos mensais e estado (Sem run / Aberto / travado), intervalo personalizado, nota de atraso de liquidação e erro 400 sob os campos.
**Where**: `frontend/components/dashboard/PeriodMenu.tsx`
**Depends on**: None (fase anterior concluída)
**Reuses**: `lib/period.ts`
**Requirement**: DASH-02

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] DASH-02 AC3, AC4 e AC5 cobertos por teste
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add period menu`

---

### T24: Cabeçalho da página

**What**: `PageHeader` com eyebrow, manchete por view (aberto, travado, rodando, vazio) e ação principal (Conciliar / Conciliando… / Exportar relatório).
**Where**: `frontend/components/dashboard/PageHeader.tsx`
**Depends on**: T23
**Reuses**: `PeriodMenu`
**Requirement**: DASH-02, DASH-05, DASH-07

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Manchete "Faltam N divergências para fechar <mês>" com N da soma de `openByType`
- [ ] Travado: "<Mês> fechado em X%" e "Exportar relatório"
- [ ] Rodando: ação desabilitada "Conciliando…"
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add dashboard page header`

---

### T25: Régua de fechamento

**What**: `CloseStepper` com os quatro passos, horários dos imports, "Travar período" habilitado só com run `COMPLETED`, zero abertas e janela destravada, e "Destravar" com confirmação.
**Where**: `frontend/components/dashboard/CloseStepper.tsx`
**Depends on**: T24
**Reuses**: —
**Requirement**: DASH-02, DASH-07

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] DASH-02 AC7, AC8 e DASH-07 AC1–AC3 cobertos por teste
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add closing stepper`

---

### T26: Card de conciliação

**What**: `MatchRingCard` com anel, contagens, comparativo com run anterior (só quando existe) e variantes rodando (percentual ou indeterminado) e travado.
**Where**: `frontend/components/dashboard/MatchRingCard.tsx`
**Depends on**: T25
**Reuses**: `ringDash`
**Requirement**: DASH-03, DASH-05

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] DASH-03 AC1, AC8 e DASH-05 AC3 cobertos por teste
- [ ] Janela com zero items mostra 0% e "0 de 0"
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add match ring card`

---

### T27: Cards de KPI

**What**: `KpiCards` com valor em aberto (`openAmount`), vendido bruto, taxas e excesso sobre contrato, sparklines dos últimos 7 dias da série.
**Where**: `frontend/components/dashboard/KpiCards.tsx`
**Depends on**: T26
**Reuses**: `formatMoney`, `seriesPath`
**Requirement**: DASH-03

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] DASH-03 AC2, AC3 e AC8 cobertos por teste
- [ ] Nenhum número sem origem na resposta (DASH-06 AC7)
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add kpi cards`

---

### T28: Gráfico esperado × recebido

**What**: `CashflowChart` com eixo Y, linha esperada, área recebida, faixa de falta, eixo X e tooltip por dia no hover; mensagem "Sem vendas nesta janela" sem dados.
**Where**: `frontend/components/dashboard/CashflowChart.tsx`
**Depends on**: T27
**Reuses**: `seriesPath`, `gapPolygon`
**Requirement**: DASH-03

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] DASH-03 AC4 e AC5 cobertos por teste
- [ ] Banco nulo: área e faixa não desenhadas e aviso "Extrato não disponível para este run"
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add cashflow chart`

---

### T29: Donut por tipo

**What**: `TypeDonut` com segmentos, total no centro e legenda com rótulos pt-BR dos tipos.
**Where**: `frontend/components/dashboard/TypeDonut.tsx`
**Depends on**: T28
**Reuses**: `donutSegments`
**Requirement**: DASH-03

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] DASH-03 AC6 coberto por teste
- [ ] Zero abertas mostra anel vazio e "0 abertas"
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add discrepancy type donut`

---

### T30: Heatmap diário

**What**: `DailyHeatmap` com grade segunda→domingo alinhada ao primeiro dia da janela, faixas de cor e legenda.
**Where**: `frontend/components/dashboard/DailyHeatmap.tsx`
**Depends on**: T29
**Reuses**: `heatBand`
**Requirement**: DASH-03

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] DASH-03 AC7 e o edge case de dia sem item cobertos por teste
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add daily match heatmap`

---

### T31: Tabela de divergências abertas

**What**: `OpenDiscrepancyTable` com até 4 linhas, filtros por tipo, botão "Resolver", rodapé "Mostrando N de M" e link para `/divergencias`; modo somente leitura no período travado.
**Where**: `frontend/components/dashboard/OpenDiscrepancyTable.tsx`
**Depends on**: None (fase anterior concluída)
**Reuses**: `formatMoney`
**Requirement**: DASH-04

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] DASH-04 AC1 e AC2 cobertos por teste
- [ ] Status exibidos só entre Aberta, Aceita, Ajustada e Baixada
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add open discrepancy table`

---

### T32: Gaveta de resolução

**What**: `ResolveDrawer` com comparação dos dois lados, desfechos, valor de correção com "Usar diferença", nota com contador, histórico, reabrir, 409, somente leitura no travado e navegação J/K.
**Where**: `frontend/components/dashboard/ResolveDrawer.tsx`
**Depends on**: T31
**Reuses**: `subtractDecimal`, `useResolveDiscrepancy`
**Requirement**: DASH-04

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] DASH-04 AC3–AC12 cobertos por teste (incluindo submit bloqueado e mensagem do 409)
- [ ] Foco preso na gaveta e `Esc` fecha
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add discrepancy resolution drawer`

---

### T33: Componentes de estado

**What**: `states.tsx` com `DashboardSkeleton`, `NoMerchant`, `Forbidden`, `EmptyOnboarding`, `RunFailedCard`, `NetworkBanner` e `BlockError`.
**Where**: `frontend/components/dashboard/states.tsx`
**Depends on**: T32
**Reuses**: textos de `design-reference.md`
**Requirement**: DASH-05, DASH-06

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] DASH-06 AC1–AC4, AC6 e DASH-05 AC5 cobertos por teste
- [ ] `RunFailedCard` copia o run id e dispara "Rodar de novo"
- [ ] Gate passa: `cd frontend && corepack pnpm test`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add dashboard state components`

---

### T34: Página do dashboard

**What**: `app/(app)/page.tsx` + `DashboardPage` ligando janela, queries, view e blocos; ações de run (incl. 409), lock/unlock, export e erros por bloco; remove `components/dashboard.tsx` e `app/page.tsx` antigos.
**Where**: `frontend/components/dashboard/DashboardPage.tsx`
**Depends on**: None (fase anterior concluída)
**Reuses**: todos os blocos, `deriveDashboardView`, hooks
**Requirement**: DASH-02, DASH-03, DASH-04, DASH-05, DASH-06, DASH-07

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Abre na janela do run mais recente; sem run, no mês atual vazio (DASH-02 AC1–AC2)
- [ ] Fluxos com fetch simulado: resolver, rodar (até `COMPLETED`), 409 no run, travar, exportar, erro de um bloco, erro de rede
- [ ] Nenhum teste antigo apagado sem equivalente migrado
- [ ] Gate passa: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): wire redesigned dashboard page`

---

### T35: Documentação

**What**: Atualizar `frontend/README.md` (fluxos e dependências) e a seção de API do `README.md` raiz com `/periods/summary`, filtro/ordem dos items e `processedItems`.
**Where**: `frontend/README.md`
**Depends on**: T34
**Reuses**: estrutura atual dos READMEs
**Requirement**: DASH-01, DASH-08, DASH-09, DASH-10

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Fluxos do frontend descrevem o dashboard novo e as rotas vazias
- [ ] Endpoints novos listados no README raiz
- [ ] Gate passa: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`

**Tests**: none
**Gate**: build

**Commit**: `docs: describe redesigned dashboard and summary endpoint`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7

Phase 1:  T1 → T2 → T3 → T4 → T5
Phase 2:  T6 → T7 → T8 → T9 → T10 → T11
Phase 3:  T12 → T13 → T14 → T15 → T16 → T17 → T18
Phase 4:  T19 → T20 → T21 → T22
Phase 5:  T23 → T24 → T25 → T26 → T27 → T28 → T29 → T30
Phase 6:  T31 → T32 → T33
Phase 7:  T34 → T35
```

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1 | 1 migration + mapeamento dos campos que ela cria | ⚠️ Coeso (schema e entidade inseparáveis) |
| T2–T4, T6, T8–T10 | 1 classe cada | ✅ Granular |
| T5 | DTO + mapper do mesmo campo | ⚠️ Coeso |
| T7 | Serviço + parâmetro do endpoint que o expõe | ⚠️ Coeso (sem o endpoint não há como testar) |
| T11 | Endpoint + interface `*Api` (AD-006) | ⚠️ Coeso |
| T12 | Manifesto de dependências + config do runner | ⚠️ Coeso |
| T13 | Tema + carga das fontes que ele referencia | ⚠️ Coeso |
| T14–T17, T20, T22–T32 | 1 módulo/componente cada | ✅ Granular |
| T18 | Hooks + provider que os hospeda | ⚠️ Coeso |
| T19 | Layout do grupo + nav | ⚠️ Coeso |
| T21 | 1 componente + 6 páginas de uma linha que o usam | ⚠️ Coeso |
| T33 | Componentes de estado num único arquivo | ⚠️ Coeso |
| T34 | Página + composição + remoção dos arquivos antigos | ⚠️ Coeso (wiring) |
| T35 | 2 READMEs | ⚠️ Coeso |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | início da Phase 1 | ✅ Match |
| T2–T5 | anterior da fase | cadeia T1→T5 | ✅ Match |
| T6 | None (ordem de fase) | início da Phase 2 | ✅ Match |
| T7–T11 | anterior da fase | cadeia T6→T11 | ✅ Match |
| T12 | None (ordem de fase) | início da Phase 3 | ✅ Match |
| T13–T18 | anterior da fase | cadeia T12→T18 | ✅ Match |
| T19 | None (ordem de fase) | início da Phase 4 | ✅ Match |
| T20–T22 | anterior da fase | cadeia T19→T22 | ✅ Match |
| T23 | None (ordem de fase) | início da Phase 5 | ✅ Match |
| T24–T30 | anterior da fase | cadeia T23→T30 | ✅ Match |
| T31 | None (ordem de fase) | início da Phase 6 | ✅ Match |
| T32–T33 | anterior da fase | cadeia T31→T33 | ✅ Match |
| T34 | None (ordem de fase) | início da Phase 7 | ✅ Match |
| T35 | T34 | T34→T35 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | Flyway / entity | none | none | ✅ OK |
| T2 | Backend matcher | unit | unit | ✅ OK |
| T3 | Run state (transação) | integration | integration | ✅ OK |
| T4 | Processor (fluxo com banco) | integration | integration | ✅ OK |
| T5 | DTO exposto por controller | integration | integration | ✅ OK |
| T6 | Repository specification | integration | integration | ✅ OK |
| T7 | Service + controller + OpenAPI | integration | integration | ✅ OK |
| T8 | Backend policy | unit | unit | ✅ OK |
| T9 | Repository | integration | integration | ✅ OK |
| T10 | Backend service | unit | unit | ✅ OK |
| T11 | Controller + OpenAPI | integration | integration | ✅ OK |
| T12 | Config / dependências | none | none | ✅ OK |
| T13 | Tema | none | none | ✅ OK |
| T14–T16 | Lib pura | unit | unit | ✅ OK |
| T17 | API client | unit | unit | ✅ OK |
| T18 | Hooks | unit | unit | ✅ OK |
| T19–T21 | Componentes | unit | unit | ✅ OK |
| T22 | Lib pura (view state) | unit | unit | ✅ OK |
| T23–T33 | Componentes | unit | unit | ✅ OK |
| T34 | Componente de página | unit | unit | ✅ OK |
| T35 | Documentação | none | none | ✅ OK |
