# Tela de Importações Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/imports-screen/design.md`
**Status**: In Progress

---

## Test Coverage Matrix

> Gerada do código, das diretrizes do projeto e da spec; confirmar antes do Execute. Diretrizes encontradas: `AGENTS.md` (não enfraquecer, pular ou apagar testes), `frontend/vitest.config.ts` (`*.test.ts` em node, `*.test.tsx` em jsdom), `.github/workflows/ci.yml`. Lições confirmadas: nenhuma. Amostras: `lib/api.feeRules.test.ts`, `lib/queries.feeRules.test.tsx`, `components/fee-rules/FeeRulesPage.test.tsx`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Frontend lib (`lib/api.ts`, `lib/imports.ts`) | unit (node) | URL, método, corpo multipart e cabeçalhos exatos por tipo; `details` lido; limites exatos (5.242.880 e 5.242.881 bytes); slug desconhecido; `details` malformado | `frontend/lib/*.test.ts` | `cd frontend && corepack pnpm test` |
| Frontend hooks (`lib/queries.ts`) | unit (jsdom) | Chave por empresa/tipo/página; invalidação de `importBatches` e `imports` após import aceito; nada invalidado após recusa | `frontend/lib/queries*.test.tsx` | `cd frontend && corepack pnpm test` |
| Frontend componentes (`components/imports/*`) | unit (jsdom) | 1:1 com os ACs de IMP-01..IMP-13 e edge cases: happy path, cada forma de recusa, rede, vazio, erro da lista e isolamento | `frontend/components/imports/*.test.tsx` | `cd frontend && corepack pnpm test` |
| Rota (`app/(app)/importacoes/page.tsx`) | unit (jsdom) | Coberta pelo teste da página que renderiza a rota, como em `FeeRulesPage.test.tsx` | `frontend/components/imports/ImportsPage.test.tsx` | `cd frontend && corepack pnpm test` |
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

### Phase 1: Dados

```
T1 → T2
T1 → T3
```

### Phase 2: Tela

```
T2 → T4
T3 → T4
T2 → T5
T4 → T6
T5 → T6 → T7
```

### Phase 3: Documentação

```
T7 → T8
```

---

## Task Breakdown

### T1: Chamadas de import no cliente da API

**What**: Adicionar `ImportKind`, `ImportBatch` (com `SettlementImport` como alias), `ApiError.details`, `importCsv` e `getImportBatches`, e fazer `send` enviar `Content-Type: application/json` só com corpo `string`.
**Where**: `frontend/lib/api.ts`
**Depends on**: None
**Reuses**: `request`, `send`, `merchantPath`, `Page<T>`
**Requirement**: IMP-03, IMP-06, IMP-07, IMP-08, IMP-10

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `importCsv` faz `POST` em `/api/merchants/m-1/transactions/import`, `/external-settlements/import` e `/bank-statements/import`, com `FormData` contendo `file`, sem `layout` na URL e sem `Content-Type: application/json`
- [x] `getImportBatches` faz `GET .../<tipo>/imports?page=<n>&size=20&sort=createdAt%2Cdesc` para cada tipo
- [x] `ApiError.details` traz o objeto `details` de um `400` com `rowErrors` e de um `409` com `conflictingReferences`; fica `undefined` sem `details` e em resposta sem JSON
- [x] Os `POST`/`PUT` JSON existentes continuam com `Content-Type: application/json` (testes atuais de `lib/api*.test.ts` inalterados e verdes)
- [x] Testes em `frontend/lib/api.imports.test.ts`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add CSV import API calls`

---

### T2: Regras puras da tela de importações

**What**: Criar `IMPORT_KINDS`, `kindFromSlug`, `MAX_CSV_BYTES`, `FILE_TOO_LARGE_MESSAGE`, `isTooLarge`, `importFailure`, `importedRowsMessage` e `formatImportedAt`.
**Where**: `frontend/lib/imports.ts`
**Depends on**: T1
**Reuses**: `ApiError`
**Requirement**: IMP-01, IMP-02, IMP-04, IMP-05, IMP-06, IMP-07, IMP-08, IMP-10

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `IMPORT_KINDS` na ordem Transações, Liquidação, Extrato, com slugs `transacoes`, `liquidacao` e `extrato` e as colunas exatas da spec
- [x] `kindFromSlug` devolve o tipo de cada slug, e `transactions` para `null`, `''` e `xyz`
- [x] `isTooLarge` é `false` em 5.242.880 bytes e `true` em 5.242.881
- [x] `importFailure` lê `rowErrors` e `conflictingReferences` de um `ApiError`; ignora `details` malformado (não lista, item sem `row` numérico, referência não string); para `Error` comum devolve só a mensagem
- [x] `importedRowsMessage` devolve "extrato.csv importado: 3 linha(s)"
- [x] `formatImportedAt('2026-10-16T13:05:00Z', 'America/Sao_Paulo')` devolve `16/10/2026 10:05`
- [x] Testes em `frontend/lib/imports.test.ts`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add import screen helpers`

---

### T3: Hooks de lotes e de envio

**What**: Adicionar `queryKeys.importBatches`, `useImportBatches` e `useImportCsv`; após import aceito, o hook invalida a lista do tipo e os últimos imports do dashboard.
**Where**: `frontend/lib/queries.ts`
**Depends on**: T1
**Reuses**: `useAuth`, padrão de `useCreateFeeRule`/`useInvalidateFeeRules`
**Requirement**: IMP-10, IMP-12

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `useImportBatches('m-1', 'settlements', 2)` consulta a página 2 da liquidação da empresa `m-1`
- [x] Após `useImportCsv` aceito, `['importBatches', 'm-1', kind]` e `['imports', 'm-1']` são recarregados antes de a mutação resolver
- [x] Após recusa, nada é invalidado
- [x] Testes em `frontend/lib/queries.imports.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add import queries and mutation`

---

### T4: Lista de lotes importados

**What**: Criar `ImportBatches`, com a tabela Arquivo/Linhas/Importado em, a paginação, o estado vazio, o skeleton e o erro com "Tentar novamente".
**Where**: `frontend/components/imports/ImportBatches.tsx`
**Depends on**: T2, T3
**Reuses**: tabela e paginação de `MerchantsPage`, `BlockError`
**Requirement**: IMP-10, IMP-11

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Linhas com nome do arquivo, total e data no formato `dd/mm/aaaa hh:mm`
- [ ] "Anterior"/"Próxima" só com mais de uma página; desabilitados na primeira/última; o clique chama `onPageChange` com a página vizinha
- [ ] Vazio mostra "Nenhuma importação de liquidação ainda." (e o texto de cada tipo)
- [ ] Erro mostra a mensagem e "Tentar novamente" refaz a consulta
- [ ] Testes em `frontend/components/imports/ImportBatches.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): list imported batches`

---

### T5: Resultado do envio

**What**: Criar `ImportResult`, que mostra o sucesso ou a recusa com a tabela de erros por linha e a lista de referências em conflito.
**Where**: `frontend/components/imports/ImportResult.tsx`
**Depends on**: T2
**Reuses**: `importedRowsMessage`
**Requirement**: IMP-04, IMP-06, IMP-07, IMP-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Sucesso: `role="status"` com "<fileName> importado: <totalRows> linha(s)"
- [ ] `rowErrors`: `role="alert"` com a mensagem e a tabela "Linha"/"Erro" com todas as linhas na ordem recebida
- [ ] `conflictingReferences`: mensagem e todas as referências listadas
- [ ] Sem listas: só a mensagem
- [ ] Testes em `frontend/components/imports/ImportResult.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): show CSV import outcome`

---

### T6: Painel de upload de um tipo

**What**: Criar `ImportPanel`, com as colunas esperadas, o seletor de arquivo e o botão "Importar". O painel bloqueia arquivo acima de 5 MB, envia, mostra o resultado e volta a lista para a página 1 após o sucesso.
**Where**: `frontend/components/imports/ImportPanel.tsx`
**Depends on**: T4, T5
**Reuses**: `useImportCsv`, `isTooLarge`, `importFailure`, `IMPORT_KINDS`
**Requirement**: IMP-02, IMP-03, IMP-04, IMP-05, IMP-09, IMP-12

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Colunas esperadas exibidas por tipo
- [ ] "Importar" desabilitado sem arquivo e durante o envio, com "Importando…"
- [ ] Envio vai para o endpoint do tipo com o arquivo escolhido; liquidação sem `layout`
- [ ] Arquivo de 5.242.881 bytes mostra "Arquivo CSV excede o tamanho máximo permitido de 5MB" e nenhuma requisição sai; 5.242.880 bytes é enviado
- [ ] Sucesso limpa o arquivo (botão volta a desabilitado) e a lista volta à página 1 com o novo lote no topo
- [ ] Recusa mantém o arquivo; novo envio remove o resultado anterior antes da resposta
- [ ] Falha de rede mostra "Não foi possível conectar à API. Tente novamente."
- [ ] Testes em `frontend/components/imports/ImportPanel.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): upload CSV imports per type`

---

### T7: Página de importações com abas

**What**: Criar `ImportsPage` com cabeçalho, abas sincronizadas com `?tipo=`, estado sem empresa e painel montado com `key` por empresa e tipo. Trocar o placeholder da rota `app/(app)/importacoes/page.tsx` por ela.
**Where**: `frontend/components/imports/ImportsPage.tsx`
**Depends on**: T6
**Reuses**: `useSelectedMerchant`, `NoMerchant`, `kindFromSlug`, servidor em memória de `FeeRulesPage.test.tsx`
**Requirement**: IMP-01, IMP-13

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Abas "Transações", "Liquidação" e "Extrato"; `?tipo=extrato` abre Extrato; ausente ou `xyz` abre Transações
- [ ] Clicar numa aba chama `router.replace('/importacoes?tipo=<slug>')`
- [ ] Trocar de empresa no menu ou de aba remove arquivo e resultado e lista os lotes da empresa/aba atual
- [ ] Sem empresa: estado `NoMerchant`, sem abas
- [ ] A rota `/importacoes` renderiza a página (não mais `UnderConstruction`)
- [ ] Testes em `frontend/components/imports/ImportsPage.test.tsx` renderizando a rota; gate build passa

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): wire imports page with type tabs`

---

### T8: README da tela de importações

**What**: Descrever a tela `/importacoes` no README: abas, colunas, limite de 5 MB, erros por linha e lotes.
**Where**: `README.md`
**Depends on**: T7
**Reuses**: seções existentes de frontend no README
**Requirement**: IMP-01..IMP-13 (documentação)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] README cita a tela, os três tipos e o layout `RECONPAY` fixo na tela
- [ ] Gate build passa

**Tests**: none
**Gate**: build

**Commit**: `docs: describe imports screen`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3

Phase 1:  T1 → T2
          T1 → T3
Phase 2:  T2 → T4
          T3 → T4
          T2 → T5
          T4 → T6
          T5 → T6 → T7
Phase 3:  T7 → T8
```

8 tasks → um batch só; execução inline, sem sub-agentes.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: Chamadas de import | 1 arquivo (`api.ts`), 2 funções + ajuste de `send`/`ApiError` coesos | ✅ Granular |
| T2: Regras puras | 1 módulo de funções puras | ✅ Granular |
| T3: Hooks | 2 hooks + chave, mesmo arquivo | ✅ Granular |
| T4: Lista de lotes | 1 componente | ✅ Granular |
| T5: Resultado | 1 componente | ✅ Granular |
| T6: Painel | 1 componente | ✅ Granular |
| T7: Página | 1 componente + troca de 1 linha na rota | ✅ Granular |
| T8: README | 1 arquivo de docs | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | início da Phase 1 | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T1 | T1 → T3 | ✅ Match |
| T4 | T2, T3 | T2 → T4, T3 → T4 | ✅ Match |
| T5 | T2 | T2 → T5 | ✅ Match |
| T6 | T4, T5 | T4 → T6, T5 → T6 | ✅ Match |
| T7 | T6 | T6 → T7 | ✅ Match |
| T8 | T7 | T7 → T8 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | Frontend lib (`api.ts`) | unit | unit | ✅ OK |
| T2 | Frontend lib (`imports.ts`) | unit | unit | ✅ OK |
| T3 | Frontend hooks | unit | unit | ✅ OK |
| T4 | Frontend componente | unit | unit | ✅ OK |
| T5 | Frontend componente | unit | unit | ✅ OK |
| T6 | Frontend componente | unit | unit | ✅ OK |
| T7 | Frontend componente + rota | unit | unit | ✅ OK |
| T8 | Docs | none | none | ✅ OK |
