# Tela de Conciliações Specification

## Problem Statement

O dashboard mostra só o run vigente de uma janela, com as 4 maiores divergências abertas. Não há como ver o histórico de runs da empresa (inclusive os substituídos e os que falharam), abrir todos os itens de um run, com o que bateu e o que divergiu, nem disparar uma conciliação fora do mês do dashboard. Além disso, `GET /reconciliations/{runId}/export` com um run inexistente devolve erro de servlet em vez de `404`, porque o controller abre a resposta como CSV antes de achar o run.

## Goals

- [ ] Listar todos os runs da empresa selecionada em `/conciliacoes`, com status e progresso atualizados enquanto um run roda.
- [ ] Abrir os itens de um run em `/conciliacoes/[runId]`, com filtros e export CSV.
- [ ] Disparar uma conciliação para qualquer janela de até 366 dias pela tela.
- [ ] Export de run inexistente responde `404` em JSON, no formato `StandardError`.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Resolver divergência (aceitar, ajustar, baixar) | Fica na tela de Divergências, a próxima feature; o dashboard já resolve as do run vigente |
| Excluir ou cancelar um run | A API não tem endpoint para isso |
| Travar ou destravar período | Tela de Fechamento |
| Comparar dois runs | Não pedido |
| Filtro de status da divergência nos itens | Pertence à tela de Divergências; aqui o foco é o resultado do cruzamento |
| Export de mais de um run de uma vez | A API exporta um run por vez |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Lista | Histórico completo da empresa, 20 por página, mais recente primeiro (`sort=createdAt,desc`) | Escolha do usuário | y |
| Filtro de período da lista | Select "Janela" com "Todas" (padrão) e os 12 meses mais recentes, do atual para trás; um mês envia `fromDate` e `toDate` exatos | Escolha do usuário (filtro opcional); a API filtra pela janela exata, e o mês é a janela usada pelo dashboard | y (filtro) / n (meses) |
| Itens de um run | Página própria `/conciliacoes/[runId]` | Escolha do usuário | y |
| Disparar conciliação | Botão "Nova conciliação" na lista | Escolha do usuário | y |
| Export 404 | Backend verifica o run antes de abrir a resposta CSV | Escolha do usuário; causa achada em `ReconciliationController.exportCsv` | y |
| Colunas da lista | Janela, Status, Itens, Conciliados, Divergentes, Criado em, ação "Ver itens" | Campos de negócio do run | n |
| Rótulos de status do run | `PENDING` Na fila, `RUNNING` Em andamento, `COMPLETED` Concluída, `FAILED` Falhou | Termos do operador | n |
| Progresso | Em `RUNNING`, a coluna Itens mostra `<processedItems> de <totalItems>`; em `PENDING`, "—" | O run informa o progresso enquanto processa | n |
| Run substituído | Selo "Substituído" ao lado do status quando `supersededAt` não é nulo | Um run novo da mesma janela substitui o anterior; o operador precisa saber qual vale | n |
| Run com falha | A linha mostra a `errorMessage` do run abaixo do status | É a única explicação que a API dá | n |
| Atualização automática | Enquanto algum run da página estiver `PENDING` ou `RUNNING`, a lista é recarregada a cada 3 s; para quando nenhum estiver | Mesmo intervalo do dashboard (`RUN_POLL_MS`) | n |
| Formatos | Janela com `formatWindow` ("Outubro de 2026" ou "01/10/2026 – 15/10/2026"); "Criado em" `dd/mm/aaaa hh:mm` no fuso do navegador; contagens como número inteiro | Mesma janela do dashboard; formatação de dado recebido (AD-010) | n |
| Formulário de nova conciliação | Gaveta lateral com "Mês" (os 12 meses mais recentes, mais "Personalizado") e os campos De e Até; abre no mês atual | O dashboard concilia por mês; o personalizado cobre janelas quebradas | n |
| Validação da janela no cliente | De e Até obrigatórios ("Informe as duas datas"); De depois de Até ("A data inicial deve ser igual ou anterior à final"); mais de 366 dias ("Janela de conciliação excede o máximo de 366 dias", mesmo texto da API) | As mesmas regras da API, antes do envio | n |
| Depois de disparar | Fecha a gaveta, volta para a página 1 com o filtro "Todas" e o run novo aparece no topo como "Na fila" | O operador vê o run que acabou de pedir | n |
| Erros ao disparar | `409` (run em andamento na janela, período travado) e `400` ficam na gaveta com a `message` da API | Mesmo padrão das outras gavetas | n |
| Itens: ordem e paginação | Maior diferença absoluta primeiro (`sort=absDifference,desc`), 20 por página | É a ordem que o dashboard usa para priorizar | n |
| Itens: filtros | "Resultado" (Todos, Bateram, Divergentes) e "Tipo de divergência" (Todos e os tipos de `TYPE_LABELS`) | Parâmetros `result` e `discrepancyType` da API | n |
| Itens: colunas | Referência, Data da venda, Método, Líquido esperado, Líquido liquidado, Diferença, Resultado, Divergências | Mesmos valores do dashboard, vindos do item (AD-010) | n |
| Itens: lado ausente | Valor ou data ausente aparece como "—" | Liquidação ausente ou órfã não tem um dos lados | n |
| Itens: divergências | Cada divergência como selo com o rótulo do tipo e o rótulo do status (ex.: "Taxa divergente · Aberta") | O item pode ter várias divergências independentes | n |
| Cabeçalho do detalhe | Janela, status, criado em, totais (itens, conciliados, divergentes), selo "Substituído" e link "Voltar para conciliações" | Contexto do run sem voltar à lista | n |
| Detalhe de run em andamento | Recarrega o run e os itens a cada 3 s até sair de `PENDING`/`RUNNING` | Mesmo comportamento da lista | n |
| Export | Botão "Exportar CSV" no detalhe, só com o run `COMPLETED`; baixa `reconciliation-<runId>.csv` | Run sem itens finais não tem o que exportar | n |
| Run de outra empresa ou inexistente | O detalhe mostra "Conciliação não encontrada." com o link de volta | A API responde `404` | n |
| Troca de empresa | Na lista: fecha a gaveta e volta para a página 1, mantendo o filtro. No detalhe: volta para `/conciliacoes` | O run aberto pertence à empresa anterior | n |
| Sem empresa selecionada | Mesmo estado vazio das outras telas por empresa | Consistência | n |
| Mensagem 404 do export | `404` `NOT_FOUND` com "Conciliação não encontrada com id: <runId>", a mesma do `GET /reconciliations/{runId}` | Mesma exceção (`ReconciliationNotFoundException`) | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Export de run inexistente responde 404 ⭐ MVP

**User Story**: As a cliente da API, I want um `404` em JSON quando o run do export não existe so that o erro seja tratado como nos outros endpoints.

**Why P1**: A tela de detalhe chama o export; um erro de servlet quebra o tratamento padrão do cliente.

**Acceptance Criteria**:

1. IF `GET /api/merchants/{merchantId}/reconciliations/{runId}/export` recebe um `runId` que não existe THEN the system SHALL responder `404` com corpo `StandardError` em JSON, `error` `NOT_FOUND` e `message` "Conciliação não encontrada com id: <runId>".
2. IF o `runId` pertence a outra empresa THEN the system SHALL responder o mesmo `404`.
3. WHEN o run existe na empresa THEN the system SHALL continuar respondendo `200` `text/csv` com `Content-Disposition` `attachment; filename="reconciliation-<runId>.csv"` e o mesmo conteúdo de hoje.

**Independent Test**: `GET .../reconciliations/<uuid aleatório>/export` responde `404` com JSON; um run existente continua baixando o CSV.

---

### P1: Ver o histórico de runs ⭐ MVP

**User Story**: As a operador, I want ver todos os runs da empresa so that eu saiba o que já foi conciliado, o que falhou e o que está rodando.

**Why P1**: É a entrada da tela.

**Acceptance Criteria**:

1. WHEN o operador abre `/conciliacoes` com uma empresa selecionada THEN the system SHALL buscar `GET /reconciliations?page=0&size=20&sort=createdAt,desc` e mostrar o título "Conciliações" e o nome da empresa.
2. The system SHALL mostrar cada run com Janela, Status, Itens, Conciliados, Divergentes, Criado em e a ação "Ver itens", nos formatos das Assumptions.
3. WHILE um run estiver `RUNNING` the system SHALL mostrar na coluna Itens "<processedItems> de <totalItems>".
4. WHERE o run tem `supersededAt` the system SHALL mostrar o selo "Substituído".
5. WHERE o run está `FAILED` the system SHALL mostrar a `errorMessage` do run.
6. WHILE algum run da página estiver `PENDING` ou `RUNNING` the system SHALL recarregar a lista a cada 3 s; WHEN nenhum estiver THEN the system SHALL parar de recarregar.
7. WHEN o operador escolhe um mês no filtro "Janela" THEN the system SHALL buscar com `fromDate` e `toDate` daquele mês e voltar para a página 1; "Todas" SHALL remover os dois parâmetros.
8. WHILE a lista tiver mais de uma página the system SHALL mostrar "Anterior" e "Próxima", desabilitando "Anterior" na primeira página e "Próxima" na última.
9. IF a busca não tem runs THEN the system SHALL mostrar "Nenhuma conciliação encontrada."
10. IF a busca falha THEN the system SHALL mostrar a mensagem do erro e o botão "Tentar novamente", que refaz a busca.
11. WHEN o operador clica em "Ver itens" THEN the system SHALL abrir `/conciliacoes/<runId>`.

**Independent Test**: Com runs de outubro, abrir `/conciliacoes` e ver o run vigente e o substituído, cada um com seus totais.

---

### P1: Disparar uma conciliação ⭐ MVP

**User Story**: As a operador, I want disparar a conciliação de uma janela pela tela so that eu não dependa do mês aberto no dashboard.

**Why P1**: Escolha do usuário; janelas quebradas (quinzena, fechamento parcial) só são possíveis aqui.

**Acceptance Criteria**:

1. WHEN o operador clica em "Nova conciliação" THEN the system SHALL abrir a gaveta com "Mês" no mês atual e De e Até preenchidos com o primeiro e o último dia desse mês.
2. WHEN o operador escolhe um mês THEN the system SHALL preencher De e Até com o primeiro e o último dia do mês; WHEN o operador edita De ou Até THEN the system SHALL mudar "Mês" para "Personalizado".
3. IF De ou Até está vazio, De é depois de Até, ou a janela tem mais de 366 dias THEN the system SHALL mostrar a mensagem das Assumptions e não chamar a API.
4. WHEN a janela é válida e o operador clica em "Conciliar" THEN the system SHALL enviar `POST /reconciliations` com `{"fromDate","toDate"}`.
5. WHILE o envio estiver em andamento the system SHALL desabilitar o botão com o texto "Conciliando…".
6. WHEN a API aceita (`202`) THEN the system SHALL fechar a gaveta, voltar para a página 1 com "Janela" em "Todas" e mostrar o run novo no topo como "Na fila".
7. IF a API recusa (`409` de run em andamento ou período travado, `400`) THEN the system SHALL manter a gaveta aberta com a `message` da API.
8. WHEN a API aceita THEN the system SHALL invalidar as consultas do dashboard da mesma janela, para o dashboard mostrar o run novo.

**Independent Test**: Disparar a janela de 01/10/2026 a 15/10/2026 e ver o run aparecer como "Na fila" e, depois, "Concluída" sem recarregar a página.

---

### P1: Ver os itens de um run ⭐ MVP

**User Story**: As a operador, I want ver o que bateu e o que divergiu num run so that eu entenda o resultado antes de resolver as divergências.

**Why P1**: O dashboard só mostra 4 divergências abertas do run vigente.

**Acceptance Criteria**:

1. WHEN o operador abre `/conciliacoes/<runId>` THEN the system SHALL buscar o run (`GET /reconciliations/<runId>`) e mostrar Janela, Status, Criado em, Itens, Conciliados, Divergentes, o selo "Substituído" quando houver e o link "Voltar para conciliações".
2. The system SHALL buscar os itens (`GET /reconciliations/<runId>/items?page=0&size=20&sort=absDifference,desc`) e mostrar Referência, Data da venda, Método, Líquido esperado, Líquido liquidado, Diferença, Resultado e Divergências, com "—" no lado ausente.
3. The system SHALL mostrar cada divergência do item como "<rótulo do tipo> · <rótulo do status>".
4. WHEN o operador escolhe "Bateram" ou "Divergentes" em "Resultado" THEN the system SHALL buscar com `result=MATCHED` ou `result=DIVERGENT` e voltar para a página 1; "Todos" SHALL remover o parâmetro.
5. WHEN o operador escolhe um tipo em "Tipo de divergência" THEN the system SHALL buscar com `discrepancyType=<tipo>` e voltar para a página 1; "Todos" SHALL remover o parâmetro.
6. WHILE os itens tiverem mais de uma página the system SHALL mostrar a paginação como na lista de runs.
7. IF não há itens para os filtros THEN the system SHALL mostrar "Nenhum item encontrado para os filtros escolhidos."
8. WHILE o run estiver `PENDING` ou `RUNNING` the system SHALL recarregar o run e os itens a cada 3 s.
9. IF a API responde `404` para o run THEN the system SHALL mostrar "Conciliação não encontrada." e o link "Voltar para conciliações".
10. IF a busca dos itens falha THEN the system SHALL mostrar a mensagem do erro e "Tentar novamente".

**Independent Test**: Abrir um run de outubro, filtrar "Divergentes" e ver só o item com "Liquidação ausente · Aberta".

---

### P1: Exportar o CSV de um run ⭐ MVP

**User Story**: As a operador, I want baixar o CSV de um run so that eu tenha o relatório para auditoria.

**Why P1**: O export existe na API e só é acessível pelo dashboard, para o run vigente.

**Acceptance Criteria**:

1. WHERE o run está `COMPLETED` the system SHALL mostrar o botão "Exportar CSV" no detalhe.
2. WHEN o operador clica em "Exportar CSV" THEN the system SHALL baixar a resposta de `GET /reconciliations/<runId>/export` como `reconciliation-<runId>.csv`.
3. WHILE o export estiver em andamento the system SHALL desabilitar o botão com o texto "Exportando…".
4. IF o export falha THEN the system SHALL mostrar a `message` do erro junto do botão.

**Independent Test**: No detalhe de um run concluído, clicar em "Exportar CSV" e receber o arquivo.

---

### P2: Empresa isola o estado

**User Story**: As a operador com várias empresas, I want que a tela acompanhe a empresa selecionada so that eu não veja ou dispare runs da empresa errada.

**Why P2**: O isolamento por empresa é regra do produto; o P1 funciona com uma empresa.

**Acceptance Criteria**:

1. WHEN o operador seleciona outra empresa na lista THEN the system SHALL fechar a gaveta aberta, manter o filtro "Janela" e buscar a página 1 da nova empresa.
2. WHEN o operador seleciona outra empresa no detalhe de um run THEN the system SHALL navegar para `/conciliacoes`.
3. IF nenhuma empresa está selecionada THEN the system SHALL mostrar o mesmo estado vazio das outras telas por empresa, sem lista, sem filtro e sem "Nova conciliação".

**Independent Test**: No detalhe de um run da Loja Alfa, trocar para Loja Alfa 2 e cair na lista de runs da Loja Alfa 2.

---

## Edge Cases

- WHEN a janela tem exatamente 366 dias THEN the system SHALL aceitá-la; com 367 dias THEN the system SHALL recusá-la com a mensagem de limite.
- WHEN De é igual a Até THEN the system SHALL aceitar a janela de um dia.
- WHEN o run está `RUNNING` com `processedItems` nulo THEN the system SHALL mostrar "0 de <totalItems>".
- WHEN um item não tem divergências THEN the system SHALL deixar a coluna Divergências vazia.
- IF a resposta de erro não é JSON THEN the system SHALL mostrar "A API retornou erro <status>." (comportamento atual do cliente).

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| REC-01 | P1 Export 404 (AC 1, 2, 3) | Design | Pending |
| REC-02 | P1 Lista: busca, colunas, formatos (AC 1, 2) | Design | Pending |
| REC-03 | P1 Lista: progresso, substituído, falha (AC 3, 4, 5, edge processedItems nulo) | Design | Pending |
| REC-04 | P1 Lista: atualização automática (AC 6) | Design | Pending |
| REC-05 | P1 Lista: filtro de janela, paginação, vazio, erro, navegação (AC 7–11) | Design | Pending |
| REC-06 | P1 Disparo: gaveta, mês e personalizado, validação (AC 1, 2, 3, edges 366/367 e um dia) | Design | Pending |
| REC-07 | P1 Disparo: envio, sucesso, erro e invalidação do dashboard (AC 4–8) | Design | Pending |
| REC-08 | P1 Itens: cabeçalho do run, colunas, divergências (AC 1, 2, 3, edge sem divergência) | Design | Pending |
| REC-09 | P1 Itens: filtros, paginação, vazio, erro, 404, atualização (AC 4–10) | Design | Pending |
| REC-10 | P1 Export: botão, download, erro (AC 1–4) | Design | Pending |
| REC-11 | P2 Isolamento por empresa e sem empresa (AC 1, 2, 3) | Design | Pending |

**Coverage:** 11 total, 0 mapped to tasks, 11 unmapped ⚠️

---

## Success Criteria

- [ ] O operador acompanha um run do disparo à conclusão sem recarregar a página.
- [ ] Qualquer item de qualquer run é visível com o motivo da divergência.
- [ ] Nenhum endpoint de conciliação responde erro de servlet para run inexistente.
