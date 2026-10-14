# Dashboard Redesign — relatório de validação (iteração 2)

## Validation: dashboard-redesign — PASS ✅

**Date**: 2026-10-14
**Spec**: `.specs/features/dashboard-redesign/spec.md` (emendado em `8eecdb4`: DASH-03 AC1/AC10, DASH-08 AC10, AD-010 restrito a dinheiro)
**Diff range**: `63f4df6..f62d60c` (branch `feat/dashboard-redesign`; `b982519`, o skill vendorizado, fica fora do escopo)
**Verifier**: sub-agente independente (autor ≠ verifier), evidence-or-zero

**Histórico**: na iteração 1 (`63f4df6..3ffaeef`) o veredito foi FAIL. Havia a SPEC_DEVIATION em DASH-03 AC1, o DASH-07 AC3 não era discriminado na página e três mutantes sobreviveram: B7, F3 e F6. As fix tasks T43–T47 (`471baf6`, `e0002e5`, `ee372a7`, `1ad435a`, `f62d60c`) e as decisões do usuário (resolvidas vindas do backend; AD-010 restrito a dinheiro) fecharam os gaps 1 a 5. Esta iteração é a 1 de 3 do ciclo de correção.

**Veredito**: 74 de 75 ACs (o spec agora inclui DASH-08 AC10) têm uma asserção de teste que bate com o spec. DASH-01 AC1 continua sem teste automatizado, mas tem evidência `file:line` por inspeção; é um critério de forma de código, não bloqueante, e já era opcional na iteração 1. Sensor: 28 de 28 mutantes não equivalentes mortos, 1 equivalente. Gates verdes.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1–T42 | ✅ Done | Iteração 1 |
| T43 `resolvedCount` no backend | ✅ Done | `countResolved` (status ≠ OPEN), DTO, serviço, OpenAPI e testes |
| T44 anel mostra "resolvidas" | ✅ Done | `SPEC_DEVIATION` removido |
| T45 testes de página do unlock e do 409 do unlock | ✅ Done | |
| T46 403 só do resumo → forbidden | ✅ Done | |
| T47 desempate com 8 itens em ordem inversa e página de tamanho 1 | ✅ Done | |

`tasks.md`: 47 tasks, 0 itens `- [ ]`.

---

## Gate Check

Rodados numa worktree temporária em `f62d60c`, sem tocar no `.next` do `next dev` nem nas portas 8080 e 3000.

| Gate | Comando | Saída |
| ---- | ------- | ----- |
| Backend (fim de fase) | `cd backend && ./mvnw -B verify` | exit 0 — 489 testes, 0 falhas, 0 erros, 0 skip; "All coverage checks have been met" (JaCoCo) |
| Frontend test | `corepack pnpm test` | exit 0 — 25 arquivos, 239 testes |
| Frontend typecheck | `corepack pnpm typecheck` | exit 0 |
| Frontend build | `corepack pnpm build` | exit 0 |

- **Antes da feature**: backend 419, frontend 10. **Agora**: backend 489 (+70), frontend 239 (+229). Na iteração 1 eram 487 e 236.
- **Removidos**: só `frontend/components/dashboard.test.ts`, junto com o componente v0 `frontend/components/dashboard.tsx`. Nenhum teste foi removido ou enfraquecido nas correções: a asserção `divergentes` de `MatchRingCard.test.tsx` foi substituída pela asserção do spec.
- **Skips**: nenhum.

---

## Spec-Anchored Acceptance Criteria

Caminhos relativos a `frontend/` (TS/TSX) ou `backend/src/test/java/br/com/hanrry/reconpay/` (Java). As linhas marcadas com **(re)** foram reverificadas nesta iteração.

### DASH-01 — Casca, tema e rotas

| Criterion | Spec-defined outcome | `file:line` + assertion | Status |
| --- | --- | --- | --- |
| AC1 tokens Lima via `@theme`, sem CSS antigo | Tokens de cor/fonte/raio no `@theme`; nenhuma classe v0 | Inspeção: `frontend/app/globals.css:3` (`@theme {`, `--color-primary: #C6F135`), `:25` (`@theme inline`, `--font-display`); `grep sidebar\|metric-card` vazio; build exit 0 | ⚠️ Sem teste automatizado (critério de forma, não bloqueante) |
| AC2 nav com 6 itens e rota ativa | `aria-current="page"` só no atual | `components/shell/TopNav.test.tsx:27` `toEqual(ITEMS)`; `:35`; `:37` | ✅ PASS |
| AC3 rotas vazias sem request | 6 rotas, incluindo `/empresas`; `fetcher` não chamado | `components/shell/UnderConstruction.test.tsx:29`; `:31` `expect(fetcher).not.toHaveBeenCalled()` | ✅ PASS |
| AC4 sem token → `/login` | Redireciona | `components/shell/AppShell.test.tsx:26` `toHaveBeenCalledWith('/login')` | ✅ PASS |
| AC5 menu de empresa | Todas as páginas, nome+CNPJ, busca, selecionada | `components/shell/MerchantMenu.test.tsx:31`, `:32`, `:41-42`, `:51-56`, `:102` | ✅ PASS |
| AC6 trocar empresa recarrega na janela inicial dela | GET `/periods` do m-2 em 2026-08 | `components/dashboard/DashboardPage.test.tsx:150` `toEqual(['/api/merchants/m-2/periods','2026-08-01'])`; `:148` | ✅ PASS |

### DASH-02 — Período e cabeçalho

| Criterion | Spec-defined outcome | `file:line` + assertion | Status |
| --- | --- | --- | --- |
| AC1 janela do run mais recente | 2026-09-01..30 | `components/dashboard/DashboardPage.test.tsx:107` | ✅ PASS |
| AC2 sem run → mês atual + vazio | "Outubro ainda não foi conciliado" | `components/dashboard/DashboardPage.test.tsx:115` | ✅ PASS |
| AC3 menu com "Sem run", "Aberto" e cadeado | Rótulos por mês | `components/dashboard/PeriodMenu.test.tsx:43`; `:47` | ✅ PASS |
| AC4 personalizado com a janela exata | `onApply({2026-09-10, 2026-09-20})` | `components/dashboard/PeriodMenu.test.tsx:72`; `components/dashboard/DashboardPage.test.tsx:168` | ✅ PASS |
| AC5 400 mantém a janela e mostra a mensagem | Alerta da API; janela anterior | `components/dashboard/DashboardPage.test.tsx:167`; `:169` | ✅ PASS |
| AC6 "Faltam N…", N = soma de openByType | 12 | `components/dashboard/PageHeader.test.tsx:20` | ✅ PASS |
| AC7 régua done/current/pending | Estados | `components/dashboard/CloseStepper.test.tsx:32` e cenários seguintes | ✅ PASS |
| AC8 horários dos imports | "Liquidação 05/10, 09:12", "Extrato 05/10, 09:30" | `components/dashboard/CloseStepper.test.tsx:55-56`; `bankstatement/integration/BankStatementIntegrationTest.java:406-407` | ✅ PASS |

### DASH-03 — Indicadores do período

| Criterion | Spec-defined outcome | `file:line` + assertion | Status |
| --- | --- | --- | --- |
| **(re)** AC1 anel ∝ `matchRate`; conciliadas (`matchedCount`), resolvidas (`resolvedCount`) e abertas | Arco 414.3/439.8; resolvidas = `resolvedCount` do resumo (63 no fixture); sem "divergentes" | `components/dashboard/MatchRingCard.test.tsx:13` `'414.3 439.8'`; `:16` `count('resolvidas')...'63'`; `:18` `queryByText('divergentes')` ausente; sem resumo, `:43` `'—'` (mutantes N4 e N5 mortos) | ✅ PASS |
| AC2 "Valor em aberto" sem float | `R$ 10.389,38`; >13 dígitos | `components/dashboard/KpiCards.test.tsx:17`; `:23` | ✅ PASS |
| AC3 bruto, `feePercent`, excesso | Valores do resumo | `components/dashboard/KpiCards.test.tsx:32`; `:38` | ✅ PASS |
| AC4 séries + faixa, 1 ponto/dia | 30 pontos; polígono fechado | `components/dashboard/CashflowChart.test.tsx:25`; `:28`; `lib/charts.test.ts:29` | ✅ PASS |
| AC5 hover: esperado + `bankShortfallAmount` / "Nada faltou" | Valores do servidor | `components/dashboard/CashflowChart.test.tsx:46`; `:55`; `:64` | ✅ PASS |
| AC6 donut por tipo | Legenda com contagens | `components/dashboard/TypeDonut.test.tsx:13` | ✅ PASS |
| AC7 heatmap em 5 faixas | Faixas da referência | `lib/charts.test.ts:55-64`; `components/dashboard/DailyHeatmap.test.tsx:27` | ✅ PASS |
| AC8 comparativos só com valor do resumo | Sem valor, sem rótulo | `components/dashboard/KpiCards.test.tsx:48`; `:53`; `components/dashboard/MatchRingCard.test.tsx:29` | ✅ PASS |
| AC9 erro por bloco | Erro local e "Recarregar" | `components/dashboard/DashboardPage.test.tsx:309`; `:315` | ✅ PASS |
| **(re)** AC10 dinheiro e % de dinheiro como recebidos; taxas de contagem podem ser derivadas | `netDifference`, `bankShortfallAmount`, `feePercent` e `previousWindowGrossChangePercent` vêm do servidor; o delta em pp (`format.ts:38`) e o % diário (`DailyHeatmap.tsx:26`) agora são permitidos | `components/dashboard/OpenDiscrepancyTable.test.tsx:48` `'−12,34'` (do servidor); `components/dashboard/CashflowChart.test.tsx:55` `'Faltaram R$ 321,45'`; `components/dashboard/KpiCards.test.tsx:38` `'9,99% do bruto'`. Texto emendado conferido: `spec.md:121` e `.specs/STATE.md:78` (AD-010 "A regra cobre só dinheiro") | ✅ PASS |

### DASH-04 — Divergências abertas e gaveta

| Criterion | Spec-defined outcome | `file:line` + assertion | Status |
| --- | --- | --- | --- |
| AC1 até 4 OPEN por \|dif.\| com `netDifference` | Query e células | `lib/api.dashboard.test.ts:99`; `components/dashboard/OpenDiscrepancyTable.test.tsx:43` | ✅ PASS |
| AC2 filtro por tipo | Só o tipo | `components/dashboard/OpenDiscrepancyTable.test.tsx:66`; `lib/api.dashboard.test.ts:106` | ✅ PASS |
| AC3 dois lados, campo destacado, histórico do mais novo | `data-divergent`; ordem | `components/dashboard/ResolveDrawer.test.tsx:66`; `:84` | ✅ PASS |
| AC4 Aceitar/Baixar sem `correctionAmount`; nota em branco ausente | Corpos exatos | `components/dashboard/ResolveDrawer.test.tsx:97`; `:107`; `lib/api.dashboard.test.ts:137` | ✅ PASS |
| AC5 Ajustar com decimal com sinal | `-1000.00` literal | `components/dashboard/ResolveDrawer.test.tsx:118` | ✅ PASS |
| AC6 zero ou vazio bloqueia | Mensagem; 0 PATCH | `components/dashboard/ResolveDrawer.test.tsx:140-141` | ✅ PASS |
| AC7 nota > 500 bloqueia | `text-danger`, botão desabilitado | `components/dashboard/ResolveDrawer.test.tsx:151-152` | ✅ PASS |
| AC8 Reabrir → OPEN | `{status:'OPEN'}` | `components/dashboard/ResolveDrawer.test.tsx:172` | ✅ PASS |
| AC9 sucesso atualiza tudo | Manchete, valor, tabela, régua e gaveta | `components/dashboard/DashboardPage.test.tsx:183`, `:185`, `:186`, `:187`, `:188`; `lib/queries.test.tsx:208` | ✅ PASS |
| AC10 409 → mensagem + recarrega | Texto exato; 2 GETs | `components/dashboard/ResolveDrawer.test.tsx:194`; `:196` | ✅ PASS |
| AC11 travado: só leitura | Sem formulário | `components/dashboard/ResolveDrawer.test.tsx:201-203` | ✅ PASS |
| AC12 J/K | `onNavigate(±1)` dentro dos limites | `components/dashboard/ResolveDrawer.test.tsx:209-218` | ✅ PASS |

### DASH-05 — Run de conciliação

| Criterion | Spec-defined outcome | `file:line` + assertion | Status |
| --- | --- | --- | --- |
| AC1 POST com a janela | Corpo da janela | `components/dashboard/DashboardPage.test.tsx:199`; `lib/api.dashboard.test.ts:46` | ✅ PASS |
| AC2 rodando: desabilitado, polling de 3 s | 1→2→3 GETs em 3 s | `lib/queries.test.tsx:76-83`; `components/dashboard/PageHeader.test.tsx:52` | ✅ PASS |
| AC3 % processado ou indeterminado | `aria-valuenow 40` / ausente | `components/dashboard/MatchRingCard.test.tsx:52`; `:64-66` | ✅ PASS |
| AC4 COMPLETED recarrega e para | Polling para; GETs novos | `lib/queries.test.tsx:87`; `components/dashboard/DashboardPage.test.tsx:209-214` | ✅ PASS |
| AC5 FAILED: card | Mensagem, copiar id, rodar de novo | `components/dashboard/states.test.tsx:91`; `:95-99` | ✅ PASS |
| AC6 409 no POST | Mensagem + polling | `components/dashboard/DashboardPage.test.tsx:225-226` | ✅ PASS |

### DASH-06 — Estados da tela

| Criterion | Spec-defined outcome | `file:line` + assertion | Status |
| --- | --- | --- | --- |
| AC1 skeleton com nav | Sem números | `components/dashboard/states.test.tsx:12`; `components/shell/AppShell.test.tsx:35` | ✅ PASS |
| AC2 sem empresa | Mensagem + `/empresas` | `components/dashboard/DashboardPage.test.tsx:124-125` | ✅ PASS |
| AC3 onboarding de 3 passos | Estados dos imports | `components/dashboard/states.test.tsx:43` | ✅ PASS |
| AC4 erro de rede | Banner; blocos mantidos | `components/dashboard/DashboardPage.test.tsx:321-326` | ✅ PASS |
| AC5 401 | Fim da sessão + `/login` | `app/providers.test.tsx:34`; `lib/queries.test.tsx:227`; `components/shell/AppShell.test.tsx:26` | ✅ PASS |
| **(re)** AC6 qualquer 403 → "Você não tem acesso…" | `forbidden` para runs, período ou resumo | `components/dashboard/DashboardPage.test.tsx:137-138` (runs); `components/dashboard/view.test.ts:54` (período); `:61` `deriveDashboardView(input({ summary: { error: forbidden } }))` → `{ kind: 'forbidden' }` (F3 morto) | ✅ PASS |
| AC7 nenhum número inventado | "—" sem dados | `components/dashboard/KpiCards.test.tsx:139-142`; `components/dashboard/MatchRingCard.test.tsx:43`; `components/dashboard/PageHeader.test.tsx:29` | ✅ PASS |

### DASH-07 — Trava, destrava e exportação

| Criterion | Spec-defined outcome | `file:line` + assertion | Status |
| --- | --- | --- | --- |
| AC1 habilita "Travar" só quando permitido | 4 casos desabilitados | `components/dashboard/CloseStepper.test.tsx:72` | ✅ PASS |
| AC2 lock → estado travado da resposta | POST lock com a janela | `components/dashboard/DashboardPage.test.tsx:237-238` | ✅ PASS |
| **(re)** AC3 "Destravar" → POST unlock e estado aberto da resposta | POST `/periods/unlock` com a janela, sem POST lock, estado aberto | `components/dashboard/DashboardPage.test.tsx:266` `body` = janela; `:267` `requests('POST','/periods/lock')).toHaveLength(0)`; `:268` "Setembro sem divergências abertas"; `:269` "Conciliar período" `toBeEnabled()` (F6 morto) | ✅ PASS |
| **(re)** AC4 409 em lock/unlock → mensagem + recarrega | Ambos os caminhos | lock: `components/dashboard/DashboardPage.test.tsx:252-253`; unlock: `:282` `'O período já está aberto.'`, recarga do período logo depois, `:284` sem POST lock | ✅ PASS |
| AC5 travado | Manchete, desabilitados, "Exportar" | `components/dashboard/PageHeader.test.tsx:41-42`; `components/dashboard/DashboardPage.test.tsx:238` | ✅ PASS |
| AC6 exportar com o token | `Bearer jwt`; nome do arquivo | `components/dashboard/DashboardPage.test.tsx:298`; `:301` | ✅ PASS |

### DASH-08 — Resumo do período (backend)

| Criterion | Spec-defined outcome | `file:line` + assertion | Status |
| --- | --- | --- | --- |
| AC1 200 com totais, tipos e dias | Valores da fixture | `reconciliation/PeriodSummaryIntegrationTest.java:118` em diante (`"grossAmount":160.65`…); `:139` admin = operator; `reconciliation/service/PeriodSummaryServiceTest.java:105-120` | ✅ PASS |
| AC2 `BigDecimal`, 2 casas | `156.00`, `0.00` | `reconciliation/PeriodSummaryIntegrationTest.java:118-133`; `reconciliation/service/PeriodSummaryServiceTest.java:157` | ✅ PASS |
| AC3 comparativos ou null | `0.7500`, `80.40`; mês civil anterior | `reconciliation/PeriodSummaryIntegrationTest.java:123`; `reconciliation/service/PeriodSummaryServiceTest.java:182`, `:239`, `:251` | ✅ PASS |
| AC4 sem run vigente → 404 | `NOT_FOUND` | `reconciliation/PeriodSummaryIntegrationTest.java:204` | ✅ PASS |
| AC5 janela inválida → 400 | `VALIDATION_ERROR` | `reconciliation/PeriodSummaryIntegrationTest.java:171` | ✅ PASS |
| AC6 403 sem grant / 401 | `FORBIDDEN` / `UNAUTHORIZED` | `reconciliation/PeriodSummaryIntegrationTest.java:197`; `:188` | ✅ PASS |
| AC7 OpenAPI em português | Operação, exemplo e textos | `openapi/ApiDocsIntegrationTest.java:723`; `:739` "aceitas, ajustadas ou baixadas"; `:756` exemplo `resolvedCount` = 3 | ✅ PASS |
| AC8 `bankShortfallAmount` | `20.00`, `0.00`, `-20.00`; null sem banco | `reconciliation/service/PeriodSummaryServiceTest.java:219`; `:143` | ✅ PASS |
| AC9 `feePercent` e `previousWindowGrossChangePercent` HALF_UP; null com divisor zero | `3.04`; `6.2`/`-6.3`; null | `reconciliation/service/PeriodSummaryServiceTest.java:228`; `:233`; `:205`; `reconciliation/PeriodSummaryIntegrationTest.java:125` | ✅ PASS |
| **(re, novo)** AC10 `resolvedCount` = discrepâncias do run com status ACCEPTED/ADJUSTED/WRITTEN_OFF | 3 de 5 (ACCEPTED, ADJUSTED, WRITTEN_OFF; OPEN fora); só o run pedido | `reconciliation/PeriodSummaryQueriesIntegrationTest.java:124` `countResolved(resolvedRun)).isEqualTo(3)`; `:126` outro run `isEqualTo(1)`; serviço `reconciliation/service/PeriodSummaryServiceTest.java:113` `resolvedCount()).isEqualTo(4)`; endpoint `reconciliation/PeriodSummaryIntegrationTest.java:102` `$.resolvedCount` = 0 (N1, N2 e N3 mortos) | ✅ PASS |

### DASH-09 — Divergências ordenadas (backend)

| Criterion | Spec-defined outcome | `file:line` + assertion | Status |
| --- | --- | --- | --- |
| AC1 `discrepancyStatus=OPEN` | A, C, E | `reconciliation/RunItemsFilterIntegrationTest.java:75-76` | ✅ PASS |
| **(re)** AC2 `absDifference,desc`, lado ausente = 0 (desempate `externalReference` do T7) | R2, R3, R1, R4; empates T1..T8 em qualquer ordem de inserção | `reconciliation/RunItemsSortIntegrationTest.java:86`; asc `:151`; desempate `:133` `contains("T1",…,"T8")` com itens salvos de T8 a T1; `:143` página de tamanho 1 (B7 morto) | ✅ PASS |
| AC3 filtros combináveis | Combinações | `reconciliation/RunItemsFilterIntegrationTest.java:99`, `:105-113`; `reconciliation/RunItemsSortIntegrationTest.java:163` | ✅ PASS |
| AC4 status inválido → 400 | `VALIDATION_ERROR` | `reconciliation/RunItemsSortIntegrationTest.java:183` | ✅ PASS |
| AC5 `netDifference` | `-4.50`, `-50.00`, `30.00`, `0.00` | `reconciliation/RunItemsSortIntegrationTest.java:108-111`; `:87` | ✅ PASS |

### DASH-10 — Progresso do run (backend)

| Criterion | Spec-defined outcome | `file:line` + assertion | Status |
| --- | --- | --- | --- |
| AC1 RUNNING com processed ≤ total | `RUNNING/0/501`, `RUNNING/500/501`; limitado a 10 | `reconciliation/RunProgressIntegrationTest.java:89-92`; `reconciliation/RunStateIntegrationTest.java:84-86` | ✅ PASS |
| AC2 PENDING → null | `PENDING/null/0` | `reconciliation/RunProgressIntegrationTest.java:69`; `reconciliation/RunStateIntegrationTest.java:70` | ✅ PASS |
| AC3 COMPLETED → processed = total | `COMPLETED/501/501` | `reconciliation/RunProgressIntegrationTest.java:94-95`; `:108-109` | ✅ PASS |

**Status**: ✅ 74 de 75 ACs têm asserção alinhada ao spec. ⚠️ DASH-01 AC1 tem só evidência por inspeção (não bloqueante). Nenhum spec-precision gap aberto: o DASH-03 AC10 foi resolvido pela emenda de AD-010.

---

## Edge Cases

- [x] Janela com zero itens → 0% e "0 de 0" (`components/dashboard/MatchRingCard.test.tsx:38`) e "Sem vendas nesta janela" (`components/dashboard/CashflowChart.test.tsx:88-93`)
- [x] Dia sem itens → neutro, sem % (`components/dashboard/DailyHeatmap.test.tsx:39-43`)
- [x] Lista de empresas paginada → todas as páginas (`components/shell/MerchantMenu.test.tsx:102-104`)
- [x] Valor com mais de 13 dígitos (`lib/money.test.ts:25-26`, `components/dashboard/KpiCards.test.tsx:23`, `lib/api.dashboard.test.ts:130`)
- [x] Troca de empresa descarta respostas antigas (`lib/queries.test.tsx:125-137`)

---

## Discrimination Sensor

Executado numa worktree temporária em `f62d60c` (`git worktree add … --detach`). Cada mutação foi uma substituição exata com contagem 1, seguida dos testes-alvo e de `git checkout` do arquivo; no fim, `git worktree remove --force`. O `git status --porcelain` da worktree real ficou igual antes e depois (só `validation.md`, `.specs/LESSONS.md` e `.specs/lessons.json`, já pendentes). Nenhum `git stash` foi usado.

### Iteração 2 (sobre `f62d60c`)

| # | File:line | Mutação | Testes | Killed? |
| - | --------- | ------- | ------ | ------- |
| B7 | `backend/.../repository/ReconciliationItemSpecifications.java:94` | remove o desempate `externalReference asc` | `RunItemsSortIntegrationTest` | ✅ Killed (`equalDifferencesFollowExternalReferenceRegardlessOfInsertionOrder`) |
| F3 | `frontend/components/dashboard/view.ts:37` | 403 só de `run`/`period` | `view.test`, `DashboardPage.test` | ✅ Killed (`forbidden quando só o resumo responde 403…`) |
| F6 | `frontend/components/dashboard/DashboardPage.tsx:152` | `changeLock('unlock')` chama `lock` | `DashboardPage.test`, `CloseStepper.test`, `queries.test` | ✅ Killed (2 testes: unlock e 409 do unlock) |
| N1 | `backend/.../repository/IReconciliationDiscrepancyRepository.java:49` | `status <> OPEN` → `status = OPEN` | `PeriodSummaryQueriesIntegrationTest`, `PeriodSummaryIntegrationTest` | ✅ Killed |
| N2 | `backend/.../repository/IReconciliationDiscrepancyRepository.java:48` | `countResolved` sem filtro de run | `PeriodSummaryQueriesIntegrationTest`, `PeriodSummaryIntegrationTest` | ✅ Killed |
| N3 | `backend/.../service/PeriodSummaryService.java:70` | `resolvedCount` fixo em `0L` | `PeriodSummaryServiceTest` | ✅ Killed |
| N4 | `frontend/components/dashboard/MatchRingCard.tsx:81` | "resolvidas" volta a mostrar `period.divergentCount` | `MatchRingCard.test`, `DashboardPage.test` | ✅ Killed |
| N5 | `frontend/components/dashboard/MatchRingCard.tsx:81` | sem resumo, mostra `'0'` em vez de `'—'` | `MatchRingCard.test` | ✅ Killed |

### Iteração 1 (sobre `3ffaeef`, código inalterado desde então)

B1–B6, B8–B10 e F1, F2, F5, F7–F14 foram mortos (resumo, falta, percentuais, `netDifference`, ordenação, filtro, progresso, formatação, polling, payloads da gaveta, trava). F4 é equivalente: `lib/api.ts:315` repete a omissão da nota em branco. B7, F3 e F6 sobreviveram e foram mortos nesta iteração.

**Sensor depth**: expandido (P0: dinheiro e integridade do fechamento), 32 mutações no total, 8 delas nesta iteração.
**Result**: 28 de 28 mutantes não equivalentes mortos (1 equivalente) — PASS ✅

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ As correções somam 1 query, 1 campo de DTO e 1 linha de serviço; no frontend, 1 tipo e 1 `Stat` |
| Surgical changes | ✅ Só arquivos ligados aos gaps (19 arquivos, 274+/20−, quase todos testes e specs) |
| No scope creep | ✅ |
| Matches patterns | ✅ `@Query` JPQL como as vizinhas; OpenAPI em português; fixtures existentes estendidas |
| Spec-anchored outcome check (asserted values match spec) | ✅ O teste do anel afirma `resolvidas` e a ausência de `divergentes`; o unlock afirma endpoint, corpo e estado |
| Per-layer Coverage Expectation met (domain 1:1 ACs; routes happy+edge+error) | ✅ `resolvedCount` coberto na query, no serviço, no endpoint e no OpenAPI; unlock com caminho feliz e 409 |
| Every test maps to a spec requirement — no unclaimed tests | ✅ |
| Documented guidelines followed: `AGENTS.md`, `frontend/README.md`, `.specs/STATE.md` (AD-009, AD-010 emendado) | ✅ |

Observações cosméticas da iteração 1, ainda presentes e fora de qualquer AC: a sparkline de "Vendido bruto" plota o líquido (`frontend/components/dashboard/KpiCards.tsx:72`), e a célula "Diferença" é sempre vermelha (`frontend/components/dashboard/OpenDiscrepancyTable.tsx:103`).

---

## Fix Plans

Nenhum bloqueante. Residual opcional da iteração 1: um teste que leia `frontend/app/globals.css` e afirme os tokens Lima e a ausência das classes v0 (DASH-01 AC1).

---

## Requirement Traceability Update

| Requirement | Previous Status (iter. 1) | New Status |
| --- | --- | --- |
| DASH-01 | ⚠️ Verified | ✅ Verified (AC1 por inspeção) |
| DASH-02 | ✅ Verified | ✅ Verified |
| DASH-03 | ❌ Needs Fix | ✅ Verified |
| DASH-04 | ✅ Verified | ✅ Verified |
| DASH-05 | ✅ Verified | ✅ Verified |
| DASH-06 | ⚠️ Verified | ✅ Verified |
| DASH-07 | ❌ Needs Fix | ✅ Verified |
| DASH-08 | ✅ Verified | ✅ Verified (com AC10 novo) |
| DASH-09 | ✅ Verified | ✅ Verified |
| DASH-10 | ✅ Verified | ✅ Verified |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 74 de 75 ACs com asserção alinhada; DASH-01 AC1 só por inspeção (não bloqueante); 0 spec-precision gaps.
**Sensor**: 28 de 28 não equivalentes mortos (8 nesta iteração, incluindo os três sobreviventes anteriores).
**Gate**: backend 489 testes sem falha + JaCoCo; frontend 239 testes sem falha; typecheck e build com exit 0.

**What works**: a contagem de resolvidas vem do servidor e é discriminada da query até o anel; o unlock e o 409 do unlock são provados na página; o 403 de qualquer query leva ao estado `forbidden`; o desempate da ordenação é provado contra a ordem de inserção.

**Next steps**: commitar este relatório e os arquivos de lições (o coordenador faz isso) e atualizar o status da traceability em `spec.md` para Verified.
