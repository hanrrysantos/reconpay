# Tela de Fechamento Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/closing-screen/design.md`
**Status**: Done

---

## Test Coverage Matrix

> Gerada do código, das diretrizes do projeto e da spec; confirmar antes do Execute. Diretrizes encontradas: `AGENTS.md` (não enfraquecer testes), `frontend/vitest.config.ts`, `.github/workflows/ci.yml`. Lições aplicadas: L-018 (exercitar cada valor listado num AC), L-036 (mudar cada valor mantido antes de um reset), L-035 (texto procurado dentro da região da própria página), L-040 (testar pela página o callback que ela liga num componente reusado), L-042 (fixture com várias linhas para contagens e posições), L-044 (bordas da paginação).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Frontend lib (`lib/closing.ts`) | unit (node) | Cada regra 1:1 com a spec: janelas distintas e ordem com empate; todos os valores de duração dos edge cases; cada status da resposta | `frontend/lib/*.test.ts` | `cd frontend && corepack pnpm test` |
| Frontend ui existente (`CloseStepper`) | unit (jsdom) | Testes atuais da régua e do dashboard inalterados e verdes; teste próprio do `Confirm` exportado | `frontend/components/dashboard/*.test.tsx` | `cd frontend && corepack pnpm test` |
| Frontend componentes e rota (`components/closing/*`) | unit (jsdom) | 1:1 com FEC-01..FEC-08 e edge cases: cada status, cada ação com confirmação e cancelamento, cada erro, botões desabilitados, paginação com bordas, isolamento por empresa, cache compartilhado | `frontend/components/closing/*.test.tsx` | `cd frontend && corepack pnpm test` |
| Remoção de código morto (`UnderConstruction`) | none | - (build gate only; sem rota que o use) | - | build gate only |
| Docs (README) | none | - (build gate only) | - | build gate only |

## Gate Check Commands

> Gerados do código; confirmar antes do Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Tasks com testes unitários de frontend | `cd frontend && corepack pnpm test && corepack pnpm typecheck` |
| Full | Sem backend nesta feature; igual ao Build | `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build` |
| Build | Fim de fase, remoção de código e docs | `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build` |

---

## Execution Plan

Phases run sequentially; tasks within a phase run in order.

### Phase 1: Base

```
T1
T2
```

### Phase 2: Tela

```
T1 → T3
T2 → T3
T3 → T4
```

### Phase 3: Limpeza e documentação

```
T4 → T5
T5 → T6
```

---

## Task Breakdown

### T1: Regras puras do fechamento

**What**: Criar `lib/closing.ts` com `CLOSING_PAGE_SIZE`, `periodWindows`, `formatDuration`, `formatLockedAt`, `periodStatus` e `PERIOD_STATUS_LABELS`.
**Where**: `frontend/lib/closing.ts`
**Depends on**: None
**Reuses**: `ApiError` de `lib/api.ts`
**Requirement**: FEC-01, FEC-02

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `periodWindows` devolve uma janela por par `fromDate`/`toDate`, mesmo com runs repetidos da janela, ordenada pela data inicial desc e, no empate, pela final desc
- [x] `formatDuration` cobre 29, 180, 7200, 7500, 86400, 97200 e nulo com os textos da spec, mais as fronteiras 59/60 e 3599/3600
- [x] `formatLockedAt` devolve `dd/mm/aaaa hh:mm` e "—" no nulo
- [x] `periodStatus` mapeia `200` travado, `200` aberto, `409`, `404`, outro erro e carregando
- [x] Testes em `frontend/lib/closing.test.ts`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add closing screen rules`

---

### T2: Exportar a confirmação da régua

**What**: Exportar `Confirm` de `CloseStepper.tsx` sem mudar o comportamento.
**Where**: `frontend/components/dashboard/CloseStepper.tsx`
**Depends on**: None
**Reuses**: `Confirm` atual
**Requirement**: FEC-04

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `Confirm` exportado com a mesma assinatura
- [x] Testes atuais da régua e do dashboard inalterados e verdes
- [x] Teste novo em `CloseStepper.test.tsx`: título, botão de ação chama `onConfirm`, "Cancelar" chama `onCancel`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `refactor(frontend): export the close confirmation`

---

### T3: Linha do período

**What**: Criar `PeriodRow`, que lê o período da janela, mostra as colunas e faz travar, destravar e exportar.
**Where**: `frontend/components/closing/PeriodRow.tsx`
**Depends on**: T1, T2
**Reuses**: `usePeriod`, `useLockPeriod`, `useUnlockPeriod`, `useExportRun`, `Confirm`, `formatWindow`, `formatRate`, `formatMoney`
**Requirement**: FEC-02, FEC-04, FEC-05, FEC-06, FEC-07

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Cada status da spec ("Travado", "Aberto", "Em andamento", "Sem conciliação concluída", "Não carregou") com as colunas e as ações exatas de cada um; `matchRate` nulo mostra "0%"
- [x] "Travar" e "Destravar" só chamam a API depois de confirmar; "Cancelar" não chama; a linha mostra a resposta da API
- [x] Erro de ação chama `onError` com a mensagem; `onError(null)` antes de cada ação; `409` relê o período
- [x] Botões da linha desabilitados com trava, reabertura ou export em andamento (cada um testado)
- [x] "Exportar CSV" baixa `reconpay-<from>-<to>.csv` do `runId` da linha
- [x] "Tentar novamente" relê só a janela da linha
- [x] Testes em `frontend/components/closing/PeriodRow.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add the closing period row`

---

### T4: Página de Fechamento

**What**: Criar `ClosingPage` (estados, janelas, paginação, aviso, tabela) e ligá-la à rota `/fechamento`.
**Where**: `frontend/components/closing/ClosingPage.tsx`
**Depends on**: T3
**Reuses**: `useSelectedMerchant`, `useRuns`, `periodWindows`, `PeriodRow`, `BlockError`, `NoMerchant`
**Requirement**: FEC-01, FEC-03, FEC-05, FEC-06, FEC-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Pela rota: título "Fechamento", nome da empresa, tabela "Períodos" com as colunas e uma linha por janela na ordem da spec, com fixture de janelas repetidas e com empate de data inicial
- [x] Paginação com 12 janelas: 10 na página 1 e 2 na página 2, "Anterior" desabilitado na primeira e "Próxima" na última; com 10 janelas, sem paginação; só as janelas da página leem o período
- [x] Sem runs: aviso e link; erro dos runs: mensagem e "Tentar novamente" que refaz a busca
- [x] O aviso de erro de uma ação aparece acima da tabela e some na ação seguinte (testado pela página, L-040)
- [x] Depois de travar pela página, a leitura do período da janela no cache é a da resposta e foi invalidada
- [x] Trocar de empresa na página 2 com aviso visível volta para a página 1 e limpa o aviso; sem empresa, estado vazio sem tabela
- [x] `app/(app)/fechamento/page.tsx` renderiza `<ClosingPage />`; a entrada `/fechamento` sai de `UnderConstruction.test.tsx`, e como era a única, o arquivo sai junto (uma suíte vazia falha no Vitest com "no tests"; desvio registrado: a remoção do teste veio de T5 para cá)
- [x] Testes em `frontend/components/closing/ClosingPage.test.tsx`; gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add the closing screen`

---

### T5: Remover o placeholder sem uso

**What**: Apagar `UnderConstruction.tsx`, que não tem mais rota (o teste dele saiu em T4).
**Where**: `frontend/components/shell/UnderConstruction.tsx`
**Depends on**: T4
**Reuses**: precedentes `34723d9`, `a022791`
**Requirement**: FEC-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Nenhum import de `UnderConstruction` no código
- [x] Componente removido; a contagem de testes não muda
- [x] Gate build passa

**Tests**: none
**Gate**: build

**Commit**: `refactor(frontend): drop the unused under construction page`

---

### T6: README

**What**: Descrever `/fechamento` no README da raiz e no do frontend; nenhuma tela fica "em construção".
**Where**: `README.md`
**Depends on**: T5
**Reuses**: textos das telas anteriores
**Requirement**: FEC-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `README.md` (parágrafo do frontend) e `frontend/README.md` descrevem a tela; a linha "`/fechamento` mostra a barra e 'em construção'" sai
- [x] Gate build passa

**Tests**: none
**Gate**: build

**Commit**: `docs: describe the closing screen`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3

Phase 1:  T1
          T2
Phase 2:  T1 → T3
          T2 → T3
          T3 → T4
Phase 3:  T4 → T5
          T5 → T6
```

6 tasks → um batch só; execução inline, sem sub-agentes. Depois da última, o Verifier roda automaticamente.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: Regras puras | 1 módulo | ✅ Granular |
| T2: Exportar `Confirm` | 1 palavra-chave num componente | ✅ Granular |
| T3: Linha | 1 componente | ✅ Granular |
| T4: Página | 1 componente + troca de 1 linha na rota e 1 entrada de placeholder | ✅ Granular |
| T5: Remoção | 1 componente sem uso e o seu teste | ✅ Granular |
| T6: README | docs | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | Phase 1 | ✅ Match |
| T2 | None | Phase 1 | ✅ Match |
| T3 | T1, T2 | T1 → T3, T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | T4 | T4 → T5 | ✅ Match |
| T6 | T5 | T5 → T6 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | lib | unit | unit | ✅ OK |
| T2 | ui existente | unit | unit | ✅ OK |
| T3 | componente | unit | unit | ✅ OK |
| T4 | componente + rota | unit | unit | ✅ OK |
| T5 | remoção de código morto | none | none | ✅ OK |
| T6 | docs | none | none | ✅ OK |
