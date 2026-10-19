# Tela de Fechamento Specification

## Problem Statement

O dashboard fecha uma janela por vez: mostra a régua de fechamento, trava, destrava e exporta só a janela escolhida no menu de período. Não existe lugar para ver, de uma vez, quais períodos da empresa já estão travados, quais seguem abertos e quanto ainda falta resolver em cada um. `/fechamento` é o último placeholder do app. A API já dá o necessário: a lista de runs da empresa (de onde saem as janelas), a leitura do período por janela, a trava, a reabertura e o export do run.

## Goals

- [ ] Ver em `/fechamento` todos os períodos com conciliação da empresa selecionada, cada um com status (travado, aberto, em andamento, sem conciliação concluída), taxa de match, valor em aberto, quando travou e quanto levou.
- [ ] Travar, destravar e exportar o CSV de qualquer período pela lista, sem trocar a janela do dashboard.
- [ ] Mostrar por que uma ação falhou (por exemplo, divergência em aberto) sem perder a lista.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Endpoint novo no backend (lista de períodos) | Decisão do usuário: só frontend, com os endpoints existentes |
| Checklist ou régua de uma janela | Decisão do usuário: a tela é o histórico; a régua já existe no dashboard |
| Resolver divergências | Já existe em `/divergencias` e no dashboard |
| Disparar conciliação | Já existe no dashboard e em `/conciliacoes` |
| Filtros de status ou de mês na lista | Não pedido; a lista é curta (uma linha por janela) |
| Atualização automática enquanto um run está em andamento | A linha mostra "Em andamento"; o estado novo aparece ao voltar para a tela ou trocar de página |
| Travar ou destravar vários períodos de uma vez | A API trava uma janela por vez |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Conteúdo da tela | Histórico de períodos: uma linha por janela com run | Escolha do usuário | y |
| Fonte da lista | Só frontend: janelas distintas dos runs da empresa (`GET /reconciliations`, os 100 mais recentes, como o dashboard) e uma leitura `GET /periods?fromDate&toDate` por janela da página | Escolha do usuário; a API não lista períodos | y |
| Janelas antigas | Uma janela cujos runs estão todos fora dos 100 mais recentes não aparece | Mesmo limite do menu de período do dashboard (`RUNS_SCAN_SIZE`) | n |
| Ordem | Data inicial da janela, mais recente primeiro; empate pela data final, mais recente primeiro | Período é lido pelo calendário, não pela data do run | n |
| Paginação | 10 janelas por página, no cliente; "Página X de Y", "Anterior" e "Próxima" só com mais de uma página; "Anterior" desabilitado na primeira e "Próxima" na última | Limita as leituras de período por vez; mesmo controle das outras telas | n |
| Nome da janela | `formatWindow`: "Outubro de 2026" para mês inteiro, senão "01/10/2026 – 15/10/2026" | Mesmo formato do menu de período | n |
| Status da linha | Pela resposta de `GET /periods`: `200` com `locked` → "Travado"; `200` sem `locked` → "Aberto"; `409` → "Em andamento"; `404` → "Sem conciliação concluída" | A leitura do período é a fonte da verdade; `404` cobre janela só com run com falha | n |
| Colunas | Período, Status, Match, Em aberto, Travado em, Duração, Ações | Os campos de `Period` | n |
| Formatos | Match com `formatRate` ("94,2%", nulo vira "0%"); Em aberto com `formatMoney`; Travado em `dd/mm/aaaa hh:mm` (hora local); "—" em campo nulo e nas linhas sem leitura de período | Mesmos formatos do dashboard | n |
| Duração | `closeDurationSeconds`: abaixo de 60 → "N s"; abaixo de 3600 → "N min" (minutos inteiros); abaixo de 86400 → "N h" ou "N h M min" (sem "0 min"); a partir disso → "N d" ou "N d H h" (sem "0 h"); nulo → "—" | Segundos crus não são legíveis; o formato fica definido aqui | n |
| Travar | Botão "Travar" só na linha "Aberto"; pede confirmação "Travar o período?" com "Travar" e "Cancelar", como no dashboard | A API só trava janela com run concluído vigente; travar é reversível, mas trava a importação da janela | n |
| Divergência em aberto | "Travar" fica habilitado em toda linha "Aberto"; a API recusa com `409` "Janela possui divergência em aberto" e a tela mostra a mensagem | A leitura do período não traz a contagem de abertas; ler o resumo de cada janela custaria uma chamada a mais por linha | n |
| Destravar | Botão "Destravar" só na linha "Travado", com confirmação "Destravar o período?" com "Destravar" e "Cancelar" | Mesmo texto do dashboard | n |
| Exportar | Botão "Exportar CSV" nas linhas "Aberto" e "Travado"; baixa `reconpay-<fromDate>-<toDate>.csv` do run da linha (`runId` do período) | Mesmo nome de arquivo do dashboard | n |
| Depois de travar ou destravar | A linha passa a mostrar a resposta da API; a leitura do período dessa janela é invalidada, para o dashboard e `/divergencias` verem o novo estado | Mesmo cache de período (`queryKeys.period`) das outras telas | n |
| Erros de ação | Mensagem da API num aviso acima da tabela (`role="alert"`), que some na próxima ação; `409` relê o período da linha | Mesmo comportamento do dashboard | n |
| Ação em andamento | Enquanto a trava, a reabertura ou o export de uma linha está em andamento, os botões dessa linha ficam desabilitados | Evita dois POSTs pela mesma janela | n |
| Sem runs | "Nenhuma conciliação ainda." com o link "Ir para conciliações" (`/conciliacoes`) | Sem run não há período | n |
| Erro na leitura de uma linha | A linha mostra "Não carregou" e "Tentar novamente", que relê só essa janela; as outras linhas seguem normais | Uma janela com erro não esconde as demais | n |
| Troca de empresa | Volta para a página 1 e limpa o aviso de erro | A lista é da empresa | n |

**Open questions:** none - all resolved or logged above.

### Implicit-requirement sweep

| Dimension | Coverage |
| --- | --- |
| Input validation & bounds | N/A because a tela não recebe entrada livre; as janelas vêm dos runs |
| Failure / partial-failure states | FEC-03 (lista), FEC-02 AC5 (linha), FEC-06 (ações) |
| Idempotency / retry / duplicate handling | FEC-06 AC3 (botões desabilitados durante a ação); segunda trava recusada pela API com `409` (FEC-06 AC2) |
| Auth boundaries & rate limits | `403` da API vira a mensagem do erro (FEC-03 AC3, FEC-06 AC1); papéis e acesso já são da API |
| Concurrency / ordering | FEC-06 AC2: `409` por mudança feita em outro lugar relê a linha |
| Data lifecycle / expiry | N/A because a tela não cria nem apaga dados além da trava, que é reversível |
| Observability | N/A because o frontend não tem telemetria; a API já audita `PERIOD_LOCKED` e `PERIOD_UNLOCKED` |
| External-dependency failure | FEC-03 AC3 e FEC-02 AC5 (API indisponível) |
| State-transition integrity | FEC-04 e FEC-05: travar só "Aberto", destravar só "Travado"; nenhuma ação em "Em andamento" ou "Sem conciliação concluída" |

---

## User Stories

### P1: Histórico de períodos ⭐ MVP

**User Story**: As a operador, I want ver todos os períodos conciliados da empresa com o status de fechamento so that eu saiba o que já está travado e o que ainda falta fechar.

**Why P1**: É o conteúdo da tela.

**Acceptance Criteria**:

1. WHEN o operador abre `/fechamento` com uma empresa selecionada THEN the system SHALL mostrar o título "Fechamento", o nome da empresa e a tabela "Períodos" com uma linha por janela distinta dos runs da empresa.
2. The system SHALL ordenar as linhas pela data inicial da janela, mais recente primeiro, e, no empate, pela data final, mais recente primeiro.
3. The system SHALL mostrar em cada linha as colunas Período, Status, Match, Em aberto, Travado em, Duração e Ações, com o nome da janela no formato de `formatWindow`.
4. WHEN a leitura do período de uma janela responde `200` THEN the system SHALL mostrar o status "Travado" se `locked`, senão "Aberto", com Match, Em aberto, Travado em e Duração nos formatos das Assumptions.
5. WHEN a leitura do período de uma janela responde `409` THEN the system SHALL mostrar o status "Em andamento", "—" nas colunas de valores e nenhuma ação.
6. WHEN a leitura do período de uma janela responde `404` THEN the system SHALL mostrar o status "Sem conciliação concluída", "—" nas colunas de valores e nenhuma ação.

**Independent Test**: Com runs de agosto, setembro (travado) e outubro na Loja Alfa, abrir `/fechamento` e ver outubro "Aberto", setembro "Travado" com data e duração, e agosto na última linha.

---

### P1: Estados da lista e paginação ⭐ MVP

**User Story**: As a operador, I want que a lista diga quando não há período, quando falhou e como andar pelas páginas so that eu não confunda lista vazia com erro.

**Why P1**: Sem isso a tela fica muda em casos comuns.

**Acceptance Criteria**:

1. WHILE as janelas passarem de 10 the system SHALL mostrar 10 linhas por página com "Anterior", "Próxima" e "Página X de Y", com "Anterior" desabilitado na primeira página e "Próxima" na última.
2. IF a empresa não tem runs THEN the system SHALL mostrar "Nenhuma conciliação ainda." com o link "Ir para conciliações" para `/conciliacoes`, sem tabela.
3. IF a busca dos runs falha THEN the system SHALL mostrar a mensagem do erro e "Tentar novamente", que refaz a busca.
4. The system SHALL ler o período só das janelas da página exibida.
5. IF a leitura do período de uma linha falha com status diferente de `404` e `409` THEN the system SHALL mostrar nessa linha "Não carregou" e "Tentar novamente", que relê só essa janela, mantendo as outras linhas.

**Independent Test**: Com 12 janelas, ver 10 na página 1, ir para a página 2 e ver as 2 restantes; numa empresa sem runs, ver o aviso com o link.

---

### P1: Travar e destravar ⭐ MVP

**User Story**: As a operador, I want travar e destravar um período direto da lista so that eu feche o mês sem trocar a janela do dashboard.

**Why P1**: É a ação principal do fechamento.

**Acceptance Criteria**:

1. WHILE a linha está "Aberto" the system SHALL mostrar o botão "Travar", e WHILE está "Travado" the system SHALL mostrar o botão "Destravar".
2. WHEN o operador clica "Travar" THEN the system SHALL pedir confirmação "Travar o período?" com "Travar" e "Cancelar", sem chamar a API antes de confirmar.
3. WHEN o operador confirma a trava THEN the system SHALL chamar `POST /periods/lock` com a janela da linha e mostrar na linha o período devolvido ("Travado", Travado em, Duração e Em aberto).
4. WHEN o operador clica "Destravar" e confirma em "Destravar o período?" THEN the system SHALL chamar `POST /periods/unlock` com a janela da linha e mostrar na linha o período devolvido ("Aberto", "—" em Travado em e Duração).
5. WHEN o operador clica "Cancelar" na confirmação THEN the system SHALL fechar a confirmação sem chamar a API.
6. WHEN a trava ou a reabertura dá certo THEN the system SHALL invalidar a leitura do período dessa janela no cache compartilhado com o dashboard e `/divergencias`.

**Independent Test**: Com outubro sem divergência aberta, travar pela lista e ver a linha "Travado"; destravar e ver "Aberto" de novo.

---

### P1: Erros e proteção das ações ⭐ MVP

**User Story**: As a operador, I want ver por que uma trava falhou so that eu saiba o que resolver antes de fechar.

**Why P1**: A trava falha sempre que há divergência em aberto.

**Acceptance Criteria**:

1. IF a trava, a reabertura ou o export falha THEN the system SHALL mostrar a mensagem do erro da API num aviso acima da tabela, que some quando o operador inicia outra ação.
2. IF a trava ou a reabertura responde `409` THEN the system SHALL reler o período dessa janela e mostrar o estado atual na linha.
3. WHILE a trava, a reabertura ou o export de uma linha está em andamento the system SHALL desabilitar os botões dessa linha.

**Independent Test**: Com outubro com uma divergência aberta, clicar "Travar", confirmar e ver "Janela possui divergência em aberto" com a linha ainda "Aberto".

---

### P1: Exportar CSV ⭐ MVP

**User Story**: As a operador, I want exportar o CSV de um período da lista so that eu entregue o fechamento sem abrir o run.

**Why P1**: O export é a saída do fechamento.

**Acceptance Criteria**:

1. WHILE a linha está "Aberto" ou "Travado" the system SHALL mostrar o botão "Exportar CSV".
2. WHEN o operador clica "Exportar CSV" THEN the system SHALL baixar o CSV de `GET /reconciliations/<runId>/export` do run da linha com o nome `reconpay-<fromDate>-<toDate>.csv`.

**Independent Test**: Exportar setembro e receber `reconpay-2026-09-01-2026-09-30.csv`.

---

### P2: Empresa isola o estado

**User Story**: As a operador com várias empresas, I want que a lista acompanhe a empresa selecionada so that eu não trave o período da empresa errada.

**Why P2**: O isolamento por empresa é regra do produto; o P1 funciona com uma empresa.

**Acceptance Criteria**:

1. WHEN o operador seleciona outra empresa THEN the system SHALL mostrar os períodos da nova empresa na página 1 e limpar o aviso de erro.
2. IF nenhuma empresa está selecionada THEN the system SHALL mostrar o mesmo estado vazio das outras telas por empresa, sem tabela.

**Independent Test**: Na página 2 da Loja Alfa, trocar para Loja Alfa 2 e ver a página 1 da nova empresa.

---

## Edge Cases

- WHEN uma janela tem vários runs (substituídos ou com falha) THEN the system SHALL mostrar uma única linha para ela.
- WHEN a janela não é um mês inteiro THEN the system SHALL mostrar o nome como "01/10/2026 – 15/10/2026".
- WHEN `matchRate` é nulo THEN the system SHALL mostrar "0%" em Match.
- WHEN `closeDurationSeconds` vale 29, 180, 7200, 7500, 86400 ou 97200 THEN the system SHALL mostrar "29 s", "3 min", "2 h", "2 h 5 min", "1 d" e "1 d 3 h".
- WHEN há exatamente 10 janelas THEN the system SHALL mostrar uma página só, sem paginação.
- IF a resposta de erro não é JSON THEN the system SHALL mostrar "A API retornou erro <status>." (comportamento atual do cliente).

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| FEC-01 | P1 Histórico: título, linhas, ordem, colunas (AC 1, 2, 3, edges de janela) | Execute | Verified |
| FEC-02 | P1 Histórico: status e valores por resposta do período (AC 4, 5, 6, edges de formato) | Execute | Verified |
| FEC-03 | P1 Estados: paginação, sem runs, erro da lista, leitura por página, erro da linha (AC 1–5) | Execute | Verified |
| FEC-04 | P1 Travar e destravar: botões e confirmação (AC 1, 2, 5) | Execute | Verified |
| FEC-05 | P1 Travar e destravar: chamadas, linha atualizada, cache (AC 3, 4, 6) | Execute | Verified |
| FEC-06 | P1 Erros e proteção das ações (AC 1, 2, 3) | Execute | Verified |
| FEC-07 | P1 Exportar CSV (AC 1, 2) | Execute | Verified |
| FEC-08 | P2 Isolamento por empresa e sem empresa (AC 1, 2) | Execute | Verified |

**Coverage:** 8 total, 8 mapped to tasks, 0 unmapped ✅

---

## Success Criteria

- [ ] O operador vê o status de fechamento de todos os períodos da empresa numa tela só.
- [ ] Um período é travado, destravado e exportado sem trocar a janela do dashboard.
- [ ] Uma trava recusada mostra o motivo da API e a linha continua com o estado real.
- [ ] Nenhuma tela do app fica "em construção".
