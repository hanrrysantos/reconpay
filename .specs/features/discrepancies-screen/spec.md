# Tela de Divergências Specification

## Problem Statement

O dashboard mostra só as 4 maiores divergências abertas do run vigente da janela. Com mais divergências do que isso, ou para revisar o que já foi aceito, ajustado ou baixado, o operador não tem onde ver a lista completa nem filtrar por tipo ou status. `/divergencias` ainda é um placeholder. A API já dá tudo o que a tela precisa: itens do run com filtros de tipo e status de divergência, detalhe com histórico e a mudança de status (resolver e reabrir).

## Goals

- [ ] Ver todas as divergências do run vigente de uma janela, com filtros de tipo e status, em `/divergencias`.
- [ ] Resolver (aceitar, ajustar, baixar) e reabrir qualquer divergência da lista, vendo o histórico dela, sem passar pelo dashboard.
- [ ] Deixar claro quando a janela não permite resolver (sem run, run em andamento, com falha ou período travado).

## Out of Scope

| Feature | Reason |
| --- | --- |
| Divergências de runs substituídos ou de várias janelas ao mesmo tempo | Decisão do usuário: a fonte é o run vigente do período; a API não lista divergências fora de um run |
| Endpoint novo de divergências no backend | Decisão do usuário: só frontend, com os endpoints existentes |
| Resolver várias divergências de uma vez | A API muda uma divergência por vez |
| Disparar conciliação | Já existe no dashboard e em `/conciliacoes` |
| Travar ou destravar período | Tela de Fechamento |
| Exportar CSV | Já existe no detalhe do run em `/conciliacoes/[runId]` |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Fonte da lista | Run vigente (mais recente) da janela escolhida; itens com `discrepancyStatus`/`discrepancyType` da API | Escolha do usuário | y |
| Resolver | Gaveta de resolução do dashboard (`ResolveDrawer`): aceitar, ajustar, baixar | Escolha do usuário | y |
| Histórico | A gaveta mostra transições (quem, quando, nota) e ajustes, como já faz no dashboard | Escolha do usuário | y |
| Reabrir | Divergência aceita, ajustada ou baixada pode voltar para aberta pela gaveta; reabrir uma ajustada anula o lançamento (regra da API) | Escolha do usuário; a API permite (`DiscrepancyResolutionService`) | y |
| Seletor de período | O `PeriodMenu` do dashboard, com marcas de run e travamento; janela inicial = a do run mais recente da empresa, ou o mês atual sem run | Mesmo comportamento do dashboard | n |
| Filtro de status | "Status": Abertas (padrão), Aceitas, Ajustadas, Baixadas, Todas | Abertas é a fila de trabalho; as outras servem para revisar | n |
| Filtro de tipo | "Tipo": Todos (padrão) e os tipos de `TYPE_LABELS` | Parâmetro `discrepancyType` da API | n |
| Uma linha por divergência | Um item com mais de uma divergência que atende aos filtros vira uma linha por divergência | Cada divergência é resolvida separadamente | n |
| Ordem e paginação | Maior diferença absoluta primeiro (`sort=absDifference,desc`), 20 itens por página | Mesma prioridade do dashboard; a API pagina por item | n |
| Colunas | Transação (referência, método e parcelas, data da venda), Tipo, Esperado, Diferença, Status, ação "Abrir" | As colunas da tabela de divergências do dashboard, mais a ação | n |
| Formatos | Esperado com `formatMoney`; Diferença com sinal (`formatMoney(…, { signed: true })`); "—" no lado ausente; data `dd/mm/aaaa` | Mesmos formatos do dashboard e de `/conciliacoes/[runId]` (AD-010) | n |
| Linha da Transação | "<método>[ <parcelas>x] · <dd/mm/aaaa>": parcelas só quando mais de 1 ("Crédito 3x · 03/10/2026", "Pix · 03/10/2026"); "—" no método ou na data ausente | Mesmo critério do dashboard, que omite "1x" (gap de precisão da verificação 1) | n |
| Contador | Sem total de divergências; a paginação mostra só "Página X de Y" | A API conta itens, não divergências; um total de itens rotulado como divergências seria errado | n |
| Navegação na gaveta | J/K e as setas da gaveta andam pelas divergências da página atual; o rodapé mostra "<posição> de <total da página>" | A gaveta já navega por uma lista carregada | n |
| Depois de resolver ou reabrir | A gaveta continua aberta mostrando o novo status; a lista recarrega; uma divergência que sai do filtro some da lista depois de fechar a gaveta | Mesmo comportamento do dashboard | n |
| Sem run na janela | "Nenhuma conciliação nesta janela." com o link "Ir para conciliações" (`/conciliacoes`) | O run é a fonte da lista | n |
| Run na fila ou em andamento | "Conciliação em andamento." e a tela recarrega a cada 3 s até o run terminar; a lista só aparece com o run concluído | Itens só existem com o run concluído | n |
| Run com falha | "A conciliação desta janela falhou." com a mensagem do run e o link "Ir para conciliações" | Não há itens para mostrar | n |
| Período travado | Aviso "Período travado: as divergências ficam só para consulta." e a gaveta abre sem ações de resolver ou reabrir | A API recusa mudanças em período travado; a gaveta já tem o modo `locked` | n |
| Erro ao resolver | Mensagem da API na gaveta; `409` recarrega o estado atual (comportamento existente da gaveta) | Reuso | n |
| Troca de empresa | Fecha a gaveta, volta a janela para a do run mais recente da nova empresa, mantém os filtros e volta para a página 1 | A janela é da empresa; os filtros são escolha do operador | n |
| Sem empresa selecionada | Mesmo estado vazio das outras telas por empresa | Consistência | n |
| Filtros na URL | Não | Mesma escolha de Transações e Conciliações | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Ver as divergências de uma janela ⭐ MVP

**User Story**: As a operador, I want ver todas as divergências do run vigente de uma janela, filtradas por tipo e status, so that eu trabalhe a fila inteira e não só as 4 maiores.

**Why P1**: É a razão da tela.

**Acceptance Criteria**:

1. WHEN o operador abre `/divergencias` com uma empresa selecionada THEN the system SHALL usar a janela do run mais recente da empresa (ou o mês atual, sem run) e mostrar o título "Divergências", o nome da empresa e o menu de período nessa janela.
2. WHEN o run vigente da janela está concluído THEN the system SHALL buscar `GET /reconciliations/<runId>/items?discrepancyStatus=OPEN&page=0&size=20&sort=absDifference,desc` e mostrar uma linha por divergência aberta, com Transação, Tipo, Esperado, Diferença, Status e "Abrir".
3. WHEN o operador escolhe outro status THEN the system SHALL buscar com `discrepancyStatus=<valor>` e voltar para a página 1; "Todas" SHALL remover o parâmetro e listar todas as divergências dos itens divergentes (`result=DIVERGENT`).
4. WHEN o operador escolhe um tipo THEN the system SHALL buscar com `discrepancyType=<tipo>` e voltar para a página 1; "Todos" SHALL remover o parâmetro.
5. The system SHALL mostrar, de cada item, só as divergências que atendem aos filtros de status e tipo escolhidos.
6. WHEN o operador escolhe outra janela no menu THEN the system SHALL usar o run vigente dessa janela e voltar para a página 1, mantendo os filtros.
7. WHILE os itens tiverem mais de uma página the system SHALL mostrar "Anterior", "Próxima" e "Página X de Y", com "Anterior" desabilitado na primeira página e "Próxima" na última.
8. IF não há divergências para os filtros THEN the system SHALL mostrar "Nenhuma divergência para os filtros escolhidos."
9. IF a busca falha THEN the system SHALL mostrar a mensagem do erro e "Tentar novamente".

**Independent Test**: Com o run de outubro da Loja Alfa, abrir `/divergencias`, ver as divergências abertas e filtrar por "Liquidação ausente".

---

### P1: Estado da janela ⭐ MVP

**User Story**: As a operador, I want saber por que a janela não tem lista so that eu vá direto ao passo que falta.

**Why P1**: Sem run concluído não há o que listar; a tela precisa dizer isso.

**Acceptance Criteria**:

1. IF a janela não tem run THEN the system SHALL mostrar "Nenhuma conciliação nesta janela." e o link "Ir para conciliações" para `/conciliacoes`.
2. WHILE o run vigente estiver `PENDING` ou `RUNNING` the system SHALL mostrar "Conciliação em andamento." e recarregar a cada 3 s; WHEN o run concluir THEN the system SHALL mostrar a lista sem recarregar a página.
3. IF o run vigente está `FAILED` THEN the system SHALL mostrar "A conciliação desta janela falhou.", a mensagem do run e o link "Ir para conciliações".
4. WHERE o período da janela está travado the system SHALL mostrar "Período travado: as divergências ficam só para consulta." acima da lista.

**Independent Test**: Escolher setembro (sem run) e ver o aviso com o link para Conciliações.

---

### P1: Resolver, reabrir e ver o histórico ⭐ MVP

**User Story**: As a operador, I want abrir uma divergência, ver o histórico e resolvê-la ou reabri-la so that a fila zere sem passar pelo dashboard.

**Why P1**: Sem resolver, a tela seria só leitura.

**Acceptance Criteria**:

1. WHEN o operador clica em "Abrir" numa linha THEN the system SHALL abrir a gaveta de resolução daquela divergência, com os dois lados da transação e o histórico (transições e ajustes).
2. WHERE a divergência está aberta e o período não está travado the system SHALL oferecer Aceitar, Ajustar e Baixar, com as regras atuais da gaveta.
3. WHERE a divergência está aceita, ajustada ou baixada e o período não está travado the system SHALL oferecer "Reabrir", que envia `status` `OPEN`.
4. WHERE o período está travado the system SHALL abrir a gaveta sem ações de resolver ou reabrir.
5. WHEN uma resolução ou reabertura é aceita THEN the system SHALL mostrar o novo status na gaveta e recarregar a lista.
6. WHEN o operador fecha a gaveta depois de resolver THEN the system SHALL mostrar a lista com o status atual, sem a divergência que saiu do filtro.
7. WHILE a gaveta estiver aberta the system SHALL navegar entre as divergências da página atual com J/K e as setas, com o rodapé "<posição> de <total da página>".

**Independent Test**: Aceitar a divergência da AVULSA-1 com uma nota, filtrar "Aceitas", abrir e reabrir.

---

### P2: Empresa isola o estado

**User Story**: As a operador com várias empresas, I want que a tela acompanhe a empresa selecionada so that eu não resolva divergência da empresa errada.

**Why P2**: O isolamento por empresa é regra do produto; o P1 funciona com uma empresa.

**Acceptance Criteria**:

1. WHEN o operador seleciona outra empresa THEN the system SHALL fechar a gaveta, usar a janela do run mais recente da nova empresa, manter os filtros de status e tipo e buscar a página 1.
2. IF nenhuma empresa está selecionada THEN the system SHALL mostrar o mesmo estado vazio das outras telas por empresa, sem filtros nem lista.

**Independent Test**: Com a gaveta aberta na Loja Alfa, trocar para Loja Alfa 2 e ver a gaveta fechar e a janela mudar.

---

## Edge Cases

- WHEN um item tem duas divergências abertas THEN the system SHALL mostrar duas linhas, cada uma com o seu tipo e o seu "Abrir".
- WHEN o filtro é "Aceitas" e um item tem uma divergência aceita e outra aberta THEN the system SHALL mostrar só a aceita.
- WHEN o lado da liquidação está ausente THEN the system SHALL mostrar "—" no que vier dele, mantendo Esperado e Diferença do servidor.
- IF a resposta de erro não é JSON THEN the system SHALL mostrar "A API retornou erro <status>." (comportamento atual do cliente).

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| DIV-01 | P1 Lista: janela inicial, busca, colunas (AC 1, 2) | Execute | Verified |
| DIV-02 | P1 Lista: filtros de status e tipo, uma linha por divergência (AC 3, 4, 5, edges) | Execute | Verified |
| DIV-03 | P1 Lista: troca de janela, paginação, vazio, erro (AC 6–9) | Execute | Verified |
| DIV-04 | P1 Estado: sem run, em andamento, falha (AC 1, 2, 3) | Execute | Verified |
| DIV-05 | P1 Estado: período travado (AC 4) | Execute | Verified |
| DIV-06 | P1 Gaveta: abrir, histórico, resolver, reabrir, travado (AC 1–4) | Execute | Verified |
| DIV-07 | P1 Gaveta: atualização da lista e navegação (AC 5, 6, 7) | Execute | Verified |
| DIV-08 | P2 Isolamento por empresa e sem empresa (AC 1, 2) | Execute | Verified |

**Coverage:** 8 total, 8 mapped to tasks, 0 unmapped ✅

---

## Success Criteria

- [ ] O operador zera as divergências abertas de uma janela sem usar o dashboard.
- [ ] Uma divergência resolvida por engano é reaberta pela mesma tela.
- [ ] Nenhuma ação de resolver aparece em período travado.
