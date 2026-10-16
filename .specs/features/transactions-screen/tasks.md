# Tela de Transações Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/transactions-screen/design.md`
**Status**: In Progress

---

## Test Coverage Matrix

> Gerada do código, das diretrizes do projeto e da spec; confirmar antes do Execute. Diretrizes encontradas: `AGENTS.md` (não enfraquecer, pular ou apagar testes), `frontend/vitest.config.ts` (`*.test.ts` em node, `*.test.tsx` em jsdom), `.github/workflows/ci.yml`. Lições confirmadas: nenhuma. Amostras: `lib/api.imports.test.ts`, `lib/imports.test.ts`, `lib/queries.imports.test.tsx`, `components/imports/*.test.tsx`, `components/fee-rules/FeeRulesPage.test.tsx`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Frontend ui compartilhada (`PeriodMenu`, `ConfirmDialog`, `SideDrawer`) | unit (jsdom) | Props novas com teste próprio; testes atuais (dashboard, Empresas, Regras de taxa) inalterados e verdes | `frontend/components/{dashboard,ui}/*.test.tsx` | `cd frontend && corepack pnpm test` |
| Frontend lib (`lib/api.ts`, `lib/transactions.ts`) | unit (node) | URL, método e corpo exatos; limites exatos (17/18 dígitos, 2/3 casas, 100/101 caracteres, hoje/amanhã, intervalo igual/invertido); texto exato do erro de regra ausente | `frontend/lib/*.test.ts` | `cd frontend && corepack pnpm test` |
| Frontend hooks (`lib/queries.ts`) | unit (jsdom) | Chave por empresa, filtros e página; invalidação do prefixo da empresa após cadastro e mudança de status | `frontend/lib/queries*.test.tsx` | `cd frontend && corepack pnpm test` |
| Frontend componentes (`components/transactions/*`) e rota | unit (jsdom) | 1:1 com os ACs de TXN-01..TXN-11 e edge cases: happy path, validação, cada erro da API, vazio, erro da lista e isolamento | `frontend/components/transactions/*.test.tsx` | `cd frontend && corepack pnpm test` |
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

### Phase 1: Peças compartilhadas

```
T1
T2
```

### Phase 2: Dados

```
T3 → T4
T3 → T5
```

### Phase 3: Tela

```
T4 → T6
T5 → T6
T2 → T7
T4 → T7
T5 → T7
T2 → T8
T4 → T8
T5 → T8
T1 → T9
T6 → T9
T7 → T9
T8 → T9
```

### Phase 4: Documentação

```
T9 → T10
```

---

## Task Breakdown

### T1: Menu de período sem runs

**What**: Tornar `runs` e `locks` opcionais em `PeriodMenu` e aceitar `months` opcional. Sem `runs`, os itens não mostram marcas e o parágrafo sobre a liquidação some.
**Where**: `frontend/components/dashboard/PeriodMenu.tsx`
**Depends on**: None
**Reuses**: `monthsBack`, `formatWindow`
**Requirement**: TXN-02

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Com `months`, o menu lista exatamente esses meses na ordem recebida
- [x] Sem `runs`, nenhum item mostra "Aberto", "Sem run" ou "Travado", e o texto "Liquidações lidas até" / "O lado da liquidação" não aparece
- [x] `onApply` rejeitado continua mostrando a mensagem no menu
- [x] Testes atuais do dashboard e do `PeriodMenu` inalterados e verdes; testes novos em `frontend/components/dashboard/PeriodMenu.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `refactor(frontend): let the period menu run without reconciliation runs`

---

### T2: Diálogo e gaveta com conteúdo extra

**What**: Adicionar `children` e `confirmDisabled` opcionais ao `ConfirmDialog` e `errorAction` opcional ao `SideDrawer`.
**Where**: `frontend/components/ui/ConfirmDialog.tsx`
**Depends on**: None
**Reuses**: componentes atuais
**Requirement**: TXN-08, TXN-10

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `ConfirmDialog` renderiza `children` entre o aviso e o erro; com `confirmDisabled`, o botão de confirmar fica desabilitado e o clique não chama `onConfirm`
- [x] `SideDrawer` renderiza `errorAction` dentro da caixa de erro, só quando há erro
- [x] Testes atuais de `ConfirmDialog`, `SideDrawer`, Empresas e Regras de taxa inalterados e verdes; testes novos nos `*.test.tsx` dos dois componentes; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): allow extra content in confirm dialog and drawer errors`

---

### T3: Chamadas de transação no cliente da API

**What**: Adicionar `TransactionFilters`, `TransactionInput`, `getTransactionPage`, `createTransaction` e `updateTransactionStatus`.
**Where**: `frontend/lib/api.ts`
**Depends on**: None
**Reuses**: `request`, `merchantPath`, `LosslessNumber`, `stringify`
**Requirement**: TXN-01, TXN-02, TXN-03, TXN-07, TXN-09

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `getTransactionPage` monta `?fromDate&toDate&page&size=20&sort=transactionDate%2Cdesc` e inclui `status`/`paymentMethod` só quando informados
- [x] `createTransaction` faz `POST` com `amount` como número JSON igual ao texto (`"1234.50"` → `1234.50`, `"0.10"` → `0.10`) e os demais campos
- [x] `updateTransactionStatus` faz `PATCH .../transactions/{id}/status` com `{"status":"REFUNDED"}`
- [x] Valores monetários da resposta continuam texto
- [x] Testes em `frontend/lib/api.transactions.test.ts`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add transaction API calls`

---

### T4: Regras puras da tela de transações

**What**: Criar rótulos de status, `FINAL_STATUSES`, `parseAmount`, `parseReference`, `dateError`, `todayIso`, `recentMonths`, `invalidWindowError`, `isMissingFeeRule`, `formatTransactionDate` e as mensagens.
**Where**: `frontend/lib/transactions.ts`
**Depends on**: T3
**Reuses**: `ApiError`, `monthsBack`
**Requirement**: TXN-01, TXN-02, TXN-06, TXN-08, TXN-10

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `parseAmount`: `"1234,5"` → `"1234.5"`; 17 dígitos inteiros + 2 casas aceito; 18 dígitos ou 3 casas, `"0"`, `"0,00"`, vazio, negativo e texto → `null`
- [x] `parseReference`: espaços das extremidades removidos; vazio → "Informe a referência externa"; 100 caracteres aceito; 101 → "Use até 100 caracteres"
- [x] `dateError`: hoje aceito; amanhã → "Data da transação não pode ser futura"; vazio → "Informe a data da transação"
- [x] `recentMonths(16/10/2026)` devolve outubro de 2026 até novembro de 2025 (12 meses, mais recente primeiro)
- [x] `invalidWindowError`: intervalo igual ou crescente → `null`; invertido → "A data inicial deve ser igual ou anterior à final"
- [x] `isMissingFeeRule` verdadeiro só para `ApiError` com a mensagem exata da spec
- [x] Rótulos Aprovada/Cancelada/Estornada/Chargeback; `formatTransactionDate('2026-10-03')` → `03/10/2026`
- [x] Testes em `frontend/lib/transactions.test.ts`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add transaction screen helpers`

---

### T5: Hooks de transações

**What**: Adicionar `queryKeys.transactions`, `useTransactionPage`, `useCreateTransaction` e `useUpdateTransactionStatus`.
**Where**: `frontend/lib/queries.ts`
**Depends on**: T3
**Reuses**: padrão de `useImportCsv`
**Requirement**: TXN-01, TXN-07, TXN-09

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `useTransactionPage` consulta a empresa, os filtros e a página pedidos
- [x] Após cadastro e após mudança de status aceitos, a lista da empresa já foi recarregada quando a mutação resolve
- [x] Testes em `frontend/lib/queries.transactions.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add transaction queries and mutations`

---

### T6: Lista de transações

**What**: Criar `TransactionList`, com a tabela de 7 colunas, a ação "Alterar status" só em aprovada, a paginação, o estado vazio e o erro com "Tentar novamente".
**Where**: `frontend/components/transactions/TransactionList.tsx`
**Depends on**: T4, T5
**Reuses**: marcação de `ImportBatches`, `formatMoney`, `METHOD_LABELS`, `BlockError`
**Requirement**: TXN-01, TXN-04, TXN-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Linhas com `03/10/2026`, referência, `Crédito`, `3x`, `R$ 300,00`, `R$ 290,73`, `Aprovada`
- [x] "Alterar status" só nas linhas aprovadas; o clique entrega a transação ao `onChangeStatus`
- [x] Paginação como em Importações
- [x] Vazio: "Nenhuma transação encontrada para os filtros escolhidos."; erro com "Tentar novamente"
- [x] Testes em `frontend/components/transactions/TransactionList.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): list transactions of the selected merchant`

---

### T7: Gaveta de nova transação

**What**: Criar `TransactionDrawer`, com os campos, a validação no cliente, o envio, os erros da API e o link "Cadastrar regra de taxa" quando falta a regra.
**Where**: `frontend/components/transactions/TransactionDrawer.tsx`
**Depends on**: T2, T4, T5
**Reuses**: padrão de `FeeRuleDrawer`, `SideDrawer`, `CREATE_FEE_RULE_HREF`
**Requirement**: TXN-05, TXN-06, TXN-07, TXN-10

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Gaveta abre com Data = hoje, Parcelas = 1, Método em "Escolha o método"
- [ ] Pix, Boleto e Débito fixam Parcelas em 1 e desabilitam o campo; trocar Crédito 3x → Pix → Crédito deixa 1
- [ ] Cada regra de validação mostra a mensagem do campo e nenhuma requisição sai; os limites dos edge cases são cobertos
- [ ] Envio com referência sem espaços e valor como número exato; botão "Cadastrando…" desabilitado durante o envio
- [ ] Sucesso chama `onCreated` com a transação
- [ ] `400`, `409` de referência repetida e `409` de período travado mantêm a gaveta aberta com a mensagem e os campos
- [ ] Erro de regra ausente mostra o link para `/taxas?cadastrar=1`; outros erros não mostram
- [ ] Testes em `frontend/components/transactions/TransactionDrawer.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): register transactions from a drawer`

---

### T8: Diálogo de mudança de status

**What**: Criar `StatusDialog`, com as opções Cancelada, Estornada e Chargeback, o aviso de mudança irreversível, o envio e os erros da API.
**Where**: `frontend/components/transactions/StatusDialog.tsx`
**Depends on**: T2, T4, T5
**Reuses**: `ConfirmDialog`
**Requirement**: TXN-08, TXN-09

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Diálogo cita a referência e mostra "A mudança não pode ser desfeita."
- [ ] "Confirmar" desabilitado sem opção escolhida
- [ ] Cada opção envia o status certo (`CANCELLED`, `REFUNDED`, `CHARGEBACK`); "Confirmando…" desabilitado durante o envio
- [ ] Sucesso fecha o diálogo
- [ ] `400` de transição inválida e `409` de período travado mantêm o diálogo aberto com a mensagem e recarregam a lista
- [ ] Testes em `frontend/components/transactions/StatusDialog.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): change transaction status with confirmation`

---

### T9: Página de transações

**What**: Criar `TransactionsPage`, com cabeçalho, filtros (período, status, método), "Nova transação", aviso de cadastro, isolamento por empresa e estado sem empresa. Trocar o placeholder da rota `app/(app)/transacoes/page.tsx` por ela e tirar `/transacoes` de `UnderConstruction.test.tsx`.
**Where**: `frontend/components/transactions/TransactionsPage.tsx`
**Depends on**: T1, T6, T7, T8
**Reuses**: `PeriodMenu`, `recentMonths`, `invalidWindowError`, padrão `shownMerchantId` de `FeeRulesPage`, servidor em memória de `ImportsPage.test.tsx`
**Requirement**: TXN-01, TXN-02, TXN-03, TXN-07, TXN-09, TXN-11

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] A rota busca o mês atual da empresa selecionada com o título e o nome da empresa
- [ ] Mês, intervalo personalizado, status e método refazem a busca com os parâmetros certos e voltam para a página 1; "Todos" remove o parâmetro
- [ ] Intervalo invertido mostra a mensagem no menu e não busca
- [ ] Cadastro aceito fecha a gaveta, mostra "Transação <referência> cadastrada." e a lista recarrega
- [ ] Mudança de status aceita mostra a linha com o novo status e sem a ação
- [ ] Troca de empresa fecha gaveta e diálogo, remove o aviso, mantém filtros e busca a página 1 da nova empresa
- [ ] Sem empresa: `NoMerchant`, sem filtros nem lista
- [ ] Testes em `frontend/components/transactions/TransactionsPage.test.tsx` renderizando a rota; gate build passa

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): wire transactions page with filters and actions`

---

### T10: README da tela de transações

**What**: Descrever `/transacoes` no README raiz e trocar a linha de placeholder em `frontend/README.md` pela descrição da tela.
**Where**: `README.md`
**Depends on**: T9
**Reuses**: seções de frontend dos READMEs
**Requirement**: TXN-01..TXN-11 (documentação)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Os dois READMEs descrevem lista, filtros, cadastro e mudança de status; `/transacoes` sai da lista "em construção"
- [ ] Gate build passa

**Tests**: none
**Gate**: build

**Commit**: `docs: describe transactions screen`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1
          T2
Phase 2:  T3 → T4
          T3 → T5
Phase 3:  T4 → T6
          T5 → T6
          T2 → T7
          T4 → T7
          T5 → T7
          T2 → T8
          T4 → T8
          T5 → T8
          T1 → T9
          T6 → T9
          T7 → T9
          T8 → T9
Phase 4:  T9 → T10
```

10 tasks → dois batches de fases inteiras (Fases 1–2: 5 tasks; Fases 3–4: 5 tasks).

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: Menu de período | 1 componente, props opcionais | ✅ Granular |
| T2: Diálogo e gaveta | 2 componentes de casca, a mesma mudança (slot opcional) | ⚠️ OK, coeso |
| T3: Chamadas da API | 3 funções no mesmo arquivo | ✅ Granular |
| T4: Regras puras | 1 módulo de funções puras | ✅ Granular |
| T5: Hooks | 3 hooks + chave, mesmo arquivo | ✅ Granular |
| T6: Lista | 1 componente | ✅ Granular |
| T7: Gaveta | 1 componente | ✅ Granular |
| T8: Diálogo de status | 1 componente | ✅ Granular |
| T9: Página | 1 componente + troca de 1 linha na rota e 1 entrada de teste de placeholder | ✅ Granular |
| T10: README | docs | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | Phase 1 sem setas | ✅ Match |
| T2 | None | Phase 1 sem setas | ✅ Match |
| T3 | None | início da Phase 2 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | T3 | T3 → T5 | ✅ Match |
| T6 | T4, T5 | T4 → T6, T5 → T6 | ✅ Match |
| T7 | T2, T4, T5 | T2 → T7, T4 → T7, T5 → T7 | ✅ Match |
| T8 | T2, T4, T5 | T2 → T8, T4 → T8, T5 → T8 | ✅ Match |
| T9 | T1, T6, T7, T8 | T1 → T9, T6 → T9, T7 → T9, T8 → T9 | ✅ Match |
| T10 | T9 | T9 → T10 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | ui compartilhada | unit | unit | ✅ OK |
| T2 | ui compartilhada | unit | unit | ✅ OK |
| T3 | lib | unit | unit | ✅ OK |
| T4 | lib | unit | unit | ✅ OK |
| T5 | hooks | unit | unit | ✅ OK |
| T6 | componente | unit | unit | ✅ OK |
| T7 | componente | unit | unit | ✅ OK |
| T8 | componente | unit | unit | ✅ OK |
| T9 | componente + rota | unit | unit | ✅ OK |
| T10 | docs | none | none | ✅ OK |
