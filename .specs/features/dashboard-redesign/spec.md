# Dashboard Redesign Specification

## Problem Statement

O dashboard atual mostra perfil, merchant e cinco transações; os indicadores ficam em "—". O backend já publica taxa de match, valor em aberto, trava, divergências e desfecho, mas o operador não tem tela para ler o fechamento da janela nem agir sobre as divergências. O novo dashboard "Lima" junta leitura do fechamento, resolução de divergências, run e trava numa só página, com dados reais.

## Goals

- [ ] Um OPERATOR com grant, ou um ADMIN, lê o fechamento da janela (taxa, abertas, valor em aberto, trava, série diária, tipos) sem sair de `/`
- [ ] O mesmo usuário resolve uma divergência aberta (aceitar, ajustar, baixar, reabrir) pela gaveta, sem outra página
- [ ] O mesmo usuário roda a conciliação, trava, destrava e exporta a janela pelo dashboard
- [ ] Cada estado da referência visual (carregando, vazio, sem empresa, erro, run em andamento, run com falha, travado) tem tela própria

## Out of Scope

| Feature | Reason |
| --- | --- |
| Páginas de Conciliações, Divergências, Transações, Importações, Fechamento e cadastro de empresa | Só rotas vazias nesta feature |
| Layout mobile dedicado | O design aprovado é desktop; abaixo de 1024px só empilha |
| Notificações (sino) | Não há fonte de dados |
| Status de fechamento por empresa no menu de empresa | Exigiria um resumo por merchant a cada abertura do menu |
| Segmentado "7 dias / Setembro / Trimestre" | A unidade do backend é a janela do run; o menu de período cobre a escolha |
| Refresh de JWT ou sessão persistente | AD-007: sessão em memória; recarregar pede login |
| Mudar regras de conciliação, trava ou desfecho | O comportamento vem das features `period-close` e `discrepancy-resolution` |
| Landing page pública | Fora deste branch |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Fonte dos blocos agregados | Endpoint novo de resumo do período, nesta feature | Escolha do usuário; mantém cálculo de dinheiro no servidor | y |
| Período inicial | Janela do run mais recente do merchant; sem run, mês civil atual no estado vazio | Escolha do usuário | y |
| Gráficos | SVG feito à mão, sem biblioteca | Escolha do usuário | y |
| Páginas do nav | Rotas vazias "em construção" | Escolha do usuário | y |
| Rota do dashboard | `/` | Branch renomeado; não há landing agora | y |
| Janela de um mês | `fromDate` = dia 1, `toDate` = último dia do mês civil | O menu lista meses; `max-window-days` padrão é 366 | n |
| Período personalizado | Dois campos de data; envia a janela exata ao backend | Período é a janela exata do run (period-close) | n |
| Segmentado da referência | Substituído pelo botão de período que abre o menu | Ver Out of Scope | n |
| Sino de notificações | Não renderizado | Ver Out of Scope | n |
| Menu de empresa | Busca, nome, CNPJ, marcação da atual, "Cadastrar empresa"; sem pill de status | Ver Out of Scope | n |
| "Cadastrar empresa" e "Importar extrato" | Navegam para rotas vazias (`/empresas`, `/importacoes`) | As páginas ainda não existem | n |
| "Usar diferença" | Preenche `correctionAmount` com `settlementNetAmount − expectedNetAmount` do item; sem liquidação, com `−expectedNetAmount` | Representa o quanto falta ou sobra em relação ao esperado | n |
| Tabela do dashboard | 4 divergências `OPEN` do run vigente, maior diferença absoluta primeiro; filtros por tipo | Mesma regra do design ("maior diferença primeiro") | n |
| Atalho J/K na gaveta | Navega entre as divergências abertas da lista carregada | Indicado no rodapé da gaveta | n |
| Polling do run | GET do run a cada 3 s enquanto `PENDING` ou `RUNNING`; para ao sair da página | Executor assíncrono sem push | n |
| Progresso do run | Campos novos no run (`processedItems`, `totalItems` estimado); sem eles, anel indeterminado sem número | Hoje o run só expõe status | n |
| Abaixo de 1024px | Blocos empilham em uma coluna; nav rola horizontalmente | Sem design mobile aprovado | n |
| Comparativos | Taxa vs run anterior da mesma janela; vendido vs mês civil anterior quando a janela é um mês civil inteiro, senão vs janela anterior de mesmo tamanho | Revisão do lote 2: a janela de mesmo tamanho nunca coincide com o run do mês anterior | y |
| Total "caiu no banco" | Soma só créditos de items com venda (`transactionDate` não nulo); créditos de liquidação órfã ficam fora | Revisão do lote 2: total igual à soma da série diária | y |
| Horário do último extrato | Endpoint `GET .../bank-statements/imports` (paginado, `createdAt` desc), espelho de `.../external-settlements/imports` | Revisão do lote 3: a linha do extrato não traz `createdAt` e não há listagem de imports | y |
| Erro de rede | Banner global; blocos já carregados permanecem | Estado "Erro" da referência | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Casca, tema e rotas ⭐ MVP

**User Story**: As an authenticated user, I want the new shell with the Lima theme and the product navigation so that the dashboard and the future pages share one layout.

**Why P1**: Todas as outras histórias renderizam dentro desta casca.

**Acceptance Criteria**:

1. The frontend SHALL apply the tokens of `design-reference.md` (cores, fontes, raios) through the Tailwind `@theme`, and SHALL NOT keep the hand-written dashboard CSS.
2. The frontend SHALL render the top nav with the items Visão geral, Conciliações, Divergências, Transações, Importações and Fechamento, marking the current route as active.
3. WHEN the user opens `/conciliacoes`, `/divergencias`, `/transacoes`, `/importacoes`, `/fechamento` or `/empresas` THEN the frontend SHALL render the shell with a "em construção" message and no data request.
4. IF there is no session token THEN the frontend SHALL redirect any route to `/login`.
5. WHEN the user opens the merchant menu THEN the frontend SHALL list every merchant returned by `/api/me/merchants` (all pages) with name and CNPJ, filter them by the search text over name or CNPJ, and mark the selected one.
6. WHEN the user selects another merchant THEN the frontend SHALL reload the dashboard for that merchant and its initial period.

**Independent Test**: Log in, navigate every nav item, open the merchant menu, search, switch merchant.

---

### P1: Período e cabeçalho de fechamento ⭐ MVP

**User Story**: As an operator, I want to pick the window and see its closing state so that I know what is left before locking.

**Why P1**: Todos os blocos dependem da janela selecionada.

**Acceptance Criteria**:

1. WHEN the dashboard loads a merchant that has runs THEN the frontend SHALL select the window (`fromDate`, `toDate`) of the most recent run.
2. WHEN the dashboard loads a merchant without runs THEN the frontend SHALL select the current calendar month and show the empty state.
3. WHEN the user opens the period menu THEN the frontend SHALL list the current month and the previous months back to the oldest run, each labeled "Sem run", "Aberto" or with a lock icon when locked.
4. WHEN the user applies a custom range THEN the frontend SHALL request the period for that exact `fromDate` and `toDate`.
5. IF the backend answers 400 for the chosen window THEN the frontend SHALL keep the previous window and show the API message next to the date fields.
6. WHILE the window has a current run with open discrepancies the frontend SHALL show the headline "Faltam N divergências para fechar <mês>" with N = the sum of the summary's open count per type.
7. The frontend SHALL show the closing stepper Importado → Conciliado → Zerar divergências → Travar período, marking each step done, current or pending from imports, run status, open count and `locked`.
8. The frontend SHALL show in the Importado step the time of the latest settlement import and of the latest bank statement import.

**Independent Test**: Open a merchant with a September run and 12 open items; see the headline, the stepper with "Zerar divergências" current, and switch to August (locked).

---

### P1: Indicadores do período ⭐ MVP

**User Story**: As an operator, I want the period indicators and charts so that I see where money is missing.

**Why P1**: É a leitura do fechamento, objetivo principal da tela.

**Acceptance Criteria**:

1. The frontend SHALL draw the match ring with the arc proportional to `matchRate` and show matched, resolved and open counts from the period and summary responses.
2. The frontend SHALL show "Valor em aberto" equal to `openAmount`, formatted as `R$ 1.234,56`, without passing the value through a binary floating-point number.
3. The frontend SHALL show the gross sold total, the fee total with its percentage of gross, and the fee amount above contract, from the summary.
4. The frontend SHALL draw the daily chart with the expected net line, the bank-received area and the missing band between them, one point per day of the window.
5. WHEN the user hovers a day on the chart THEN the frontend SHALL show that day's expected amount and the missing amount.
6. The frontend SHALL draw the donut and legend with the count of open discrepancies per type.
7. The frontend SHALL color each day of the heatmap by that day's match percentage using the five bands of `design-reference.md`.
8. The frontend SHALL show the comparison labels (taxa vs run anterior, vendido vs janela anterior) only when the summary provides the previous value.
9. IF a block's request fails THEN the frontend SHALL show that block's error with a reload action and keep the other blocks.

**Independent Test**: Load a window with known fixtures and check each number, the ring arc, the missing band on day 27, and a heatmap day under 90% in red.

---

### P1: Divergências abertas e gaveta de resolução ⭐ MVP

**User Story**: As an operator, I want to resolve open discrepancies from the dashboard so that the period can be locked.

**Why P1**: Zerar divergências é o passo que destrava o fechamento.

**Acceptance Criteria**:

1. The frontend SHALL list up to 4 `OPEN` discrepancies of the current run ordered by largest absolute difference, with reference, method, date, type, expected amount, difference and status.
2. WHEN the user picks a type filter THEN the frontend SHALL list only open discrepancies of that type.
3. WHEN the user clicks "Resolver" THEN the frontend SHALL open the drawer with both sides of the snapshot, the divergent field highlighted, and the discrepancy history newest first.
4. WHEN the user submits Aceitar or Baixar THEN the frontend SHALL send PATCH with `ACCEPTED` or `WRITTEN_OFF`, the note (blank sent as absent) and no `correctionAmount`.
5. WHEN the user submits Ajustar THEN the frontend SHALL send PATCH with `ADJUSTED` and the signed `correctionAmount` as a decimal string.
6. IF Ajustar has an empty or zero amount THEN the frontend SHALL block the submit and show "Informe um valor diferente de zero".
7. IF the note exceeds 500 characters THEN the frontend SHALL block the submit and show the counter in the danger color.
8. WHEN the user clicks "Reabrir" on a resolved discrepancy THEN the frontend SHALL send PATCH with `OPEN`.
9. WHEN a PATCH succeeds THEN the frontend SHALL update the drawer, the table, the open count, the open amount and the stepper from fresh data.
10. IF a PATCH answers 409 THEN the frontend SHALL show "Esta divergência mudou. Recarregamos o estado atual." and reload the discrepancy.
11. WHILE the period is locked the frontend SHALL hide the outcome form and show the drawer read-only.
12. WHEN the user presses J or K with the drawer open THEN the frontend SHALL move to the next or previous discrepancy of the loaded list.

**Independent Test**: Open a discrepancy, adjust it with −1.000,00 and a note, see the counters drop; reopen it and see the adjustment marked as voided in the history.

---

### P1: Run de conciliação ⭐ MVP

**User Story**: As an operator, I want to run the reconciliation for the window and follow it so that the dashboard reflects new imports.

**Why P1**: Sem run não há período a fechar.

**Acceptance Criteria**:

1. WHEN the user clicks "Conciliar período" THEN the frontend SHALL POST the run for the selected window.
2. WHILE the latest run of the window is `PENDING` or `RUNNING` the frontend SHALL disable the action, show the running state, and poll the run every 3 seconds.
3. WHERE the run response provides progress fields the frontend SHALL show the processed percentage; otherwise it SHALL show an indeterminate ring without a number.
4. WHEN the polled run reaches `COMPLETED` THEN the frontend SHALL stop polling and reload period, summary and discrepancies.
5. WHEN the polled run reaches `FAILED` THEN the frontend SHALL stop polling and show the failed-run card with the public message, the run id copy action and "Rodar de novo".
6. IF the POST answers 409 THEN the frontend SHALL show the API message and start polling the existing run of the window.

**Independent Test**: Click "Conciliar período", see the running state, then the refreshed numbers after completion.

---

### P1: Estados da tela ⭐ MVP

**User Story**: As a user, I want clear loading, empty and error states so that I never read invented numbers.

**Why P1**: AD-007 proíbe valores inventados; a referência define cada estado.

**Acceptance Criteria**:

1. WHILE the first data of the dashboard is loading the frontend SHALL show the skeleton of the bento grid with the nav already interactive.
2. IF the user has no accessible merchant THEN the frontend SHALL show "Nenhuma empresa no seu acesso" with the action "Cadastrar empresa".
3. IF the selected window has no run THEN the frontend SHALL show the three-step onboarding (liquidação, extrato, conciliar) with each step's state from imports.
4. IF a request fails with a network error THEN the frontend SHALL show the global banner "Não foi possível conectar à API" with "Tentar de novo" and keep already loaded blocks.
5. IF any request answers 401 THEN the frontend SHALL end the session and redirect to `/login`.
6. IF a request answers 403 THEN the frontend SHALL show "Você não tem acesso a esta empresa" and offer the merchant menu.
7. The frontend SHALL NOT display a numeric value that did not come from an API response.

**Independent Test**: Simulate each condition with mocked responses and compare with the state artboards.

---

### P2: Trava, destrava e exportação

**User Story**: As an operator, I want to lock, unlock and export the window from the dashboard so that the close is recorded and auditable.

**Why P2**: O fechamento pode ser feito por API hoje; a tela agiliza.

**Acceptance Criteria**:

1. WHILE the current run is `COMPLETED` with zero open discrepancies and the window is unlocked the frontend SHALL enable "Travar período".
2. WHEN the user confirms "Travar período" THEN the frontend SHALL POST lock for the window and show the locked state from the response.
3. WHEN the user confirms "Destravar" THEN the frontend SHALL POST unlock for the window and show the open state from the response.
4. IF lock or unlock answers 409 THEN the frontend SHALL show the API message and reload the period.
5. WHILE the window is locked the frontend SHALL show the headline "<Mês> fechado em X%", disable "Conciliar período", hide outcome actions, and show "Exportar relatório" as the primary action.
6. WHEN the user clicks "Exportar relatório" THEN the frontend SHALL download the run CSV from the export endpoint with the session token.

**Independent Test**: Resolve the last open discrepancy, lock, see the locked state, export, unlock.

---

### P1: Resumo do período no backend ⭐ MVP

**User Story**: As the dashboard, I want one summary endpoint for the window so that charts and totals come from the server.

**Why P1**: Os indicadores do P1 dependem deste contrato.

**Acceptance Criteria**:

1. WHEN an OPERATOR with grant or an ADMIN calls `GET /api/merchants/{merchantId}/periods/summary` with `fromDate` and `toDate` THEN the API SHALL answer 200 with, for the current `COMPLETED` run of that exact window: gross total, expected net total, fee total, fee amount above contract, bank-received total, open count per discrepancy type, and one daily entry per date of the window with expected net, settled net, bank received, item count and matched count.
2. The API SHALL compute money values with `BigDecimal` and serialize them with two decimal places.
3. The API SHALL include the previous run match rate of the same window and the gross total of the previous window of the same length, or null when absent.
4. IF the window has no current `COMPLETED` run THEN the API SHALL answer 404 `NOT_FOUND`.
5. IF `fromDate` is after `toDate` or the window exceeds `max-window-days` THEN the API SHALL answer 400 `VALIDATION_ERROR`.
6. IF the caller is an OPERATOR without grant THEN the API SHALL answer 403 `FORBIDDEN`; without authentication, 401.
7. The API SHALL document the operation in the `*Api` interface (AD-006) with example and each response code in Portuguese.

**Independent Test**: Seed a run with known items and bank lines; call the endpoint and compare every total and one daily entry.

---

### P1: Divergências abertas ordenadas no backend ⭐ MVP

**User Story**: As the dashboard, I want open discrepancies ordered by difference so that the table shows the most expensive first.

**Why P1**: A tabela do P1 depende da ordem e do filtro.

**Acceptance Criteria**:

1. WHEN the client calls the run items endpoint with `discrepancyStatus=OPEN` THEN the API SHALL return only items with at least one discrepancy in that status.
2. WHEN the client sorts by `absDifference,desc` THEN the API SHALL order items by the absolute value of `settlementNetAmount − expectedNetAmount`, treating a missing side as zero.
3. The API SHALL keep the existing `result` and `discrepancyType` filters combinable with `discrepancyStatus`.
4. IF `discrepancyStatus` is not a valid status THEN the API SHALL answer 400 `VALIDATION_ERROR`.

**Independent Test**: Seed items with mixed statuses and differences; request page 0 size 4 and check the order.

---

### P2: Progresso do run no backend

**User Story**: As the dashboard, I want run progress so that the running state shows a real percentage.

**Why P2**: O estado de run tem fallback indeterminado.

**Acceptance Criteria**:

1. WHILE a run is `RUNNING` the API SHALL expose `processedItems` and `totalItems` in the run response, with `processedItems` never greater than `totalItems`.
2. WHILE a run is `PENDING` the API SHALL return `processedItems` as null.
3. WHEN the run completes THEN the API SHALL return `processedItems` equal to `totalItems`.

**Independent Test**: Start a run on a large seed and poll; see `processedItems` grow to `totalItems`.

---

## Edge Cases

- IF the window has zero items THEN the frontend SHALL show the ring at 0% with "0 de 0" and the empty chart message "Sem vendas nesta janela".
- IF a daily entry has zero items THEN the heatmap SHALL render that day neutral (white) with no percentage.
- IF the merchant list has more than one page THEN the merchant menu SHALL include every page.
- IF an amount exceeds 13 digits THEN the frontend SHALL format it without overflow or rounding.
- WHEN the selected merchant changes while requests are pending THEN the frontend SHALL discard responses of the previous merchant.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| DASH-01 | P1: Casca, tema e rotas | Tasks | In Tasks |
| DASH-02 | P1: Período e cabeçalho de fechamento | Tasks | In Tasks |
| DASH-03 | P1: Indicadores do período | Tasks | In Tasks |
| DASH-04 | P1: Divergências abertas e gaveta de resolução | Tasks | In Tasks |
| DASH-05 | P1: Run de conciliação | Tasks | In Tasks |
| DASH-06 | P1: Estados da tela | Tasks | In Tasks |
| DASH-07 | P2: Trava, destrava e exportação | Tasks | In Tasks |
| DASH-08 | P1: Resumo do período no backend | Tasks | In Tasks |
| DASH-09 | P1: Divergências abertas ordenadas no backend | Tasks | In Tasks |
| DASH-10 | P2: Progresso do run no backend | Tasks | In Tasks |

**Coverage:** 10 total, 10 mapped to tasks, 0 unmapped

---

## Success Criteria

- [ ] `corepack pnpm test`, `typecheck` e `build` passam no frontend; `./mvnw verify` passa no backend com o gate JaCoCo.
- [ ] Nenhum valor numérico do dashboard vem de constante no código.
- [ ] Cada artboard Lima (principal, 5 estados, 2 interações) tem estado correspondente demonstrável na aplicação.
