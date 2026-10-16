# Tela de Importações Specification

## Problem Statement

A API já importa três arquivos CSV por empresa: transações internas (`POST /transactions/import`), liquidação externa (`POST /external-settlements/import`) e extrato bancário (`POST /bank-statements/import`). Os três imports são tudo ou nada e devolvem os erros por linha. A tela `/importacoes` ainda é um placeholder, então o operador só consegue alimentar a conciliação pelo Swagger. Sem esses dados não há o que conciliar, e o fluxo para logo depois de cadastrar as regras de taxa.

## Goals

- [ ] Com uma empresa selecionada, enviar qualquer um dos três CSVs pela tela `/importacoes` e ver o resultado sem usar o Swagger.
- [ ] Quando o arquivo for recusado, mostrar todos os motivos que a API devolveu (linha e mensagem, ou referências em conflito), para o operador corrigir o arquivo de uma vez.
- [ ] Ver os lotes já importados de cada tipo, do mais recente para o mais antigo.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Tela de Transações (lista, formulário, status) | Decisão do usuário: feature seguinte (AD-011) |
| Layout `ACQUIRER` de liquidação | Decisão do usuário: a tela envia sempre o layout padrão `RECONPAY`; o `ACQUIRER` continua disponível pela API |
| Histórico de tentativas recusadas | A API não persiste arquivo recusado; um lote só existe quando o import é aceito |
| Detalhe de um lote (linhas importadas) | O detalhe da API traz os mesmos campos da listagem; ver as linhas é tarefa das telas de Transações e de Conciliações |
| Modelo de CSV para download | Não pedido; a aba mostra as colunas esperadas em texto |
| Arrastar e soltar arquivo | Não pedido; seletor de arquivo nativo |
| Desfazer / excluir um lote | A API não tem endpoint para isso |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Escopo | Só Importações; Transações é a próxima feature | Escolha do usuário | y |
| Organização | Abas Transações, Liquidação e Extrato; cada aba tem upload, resultado do último envio e lista de lotes daquele tipo | Escolha do usuário | y |
| Layout de liquidação | Sempre `RECONPAY`, sem o parâmetro `layout` | Escolha do usuário | y |
| Aba inicial | Transações, a primeira da ordem do fluxo (transação → liquidação → extrato) | Ordem do domínio | n |
| Aba na URL | A aba ativa fica em `?tipo=transacoes|liquidacao|extrato`; valor ausente ou desconhecido abre Transações | Recarregar a página ou compartilhar o link mantém a aba | n |
| Colunas esperadas exibidas | Transações: `externalReference, amount, paymentMethod, installments, status, transactionDate`; Liquidação: `externalReference, amount, netAmount, paymentMethod, installments, status, settlementDate`; Extrato: `lineReference, externalReference, amount, movementDate` | Cabeçalhos exigidos pelos parsers da API, na mesma ordem | n |
| Limite de tamanho | A tela recusa arquivo acima de 5 MB (5 × 1024 × 1024 bytes) antes de enviar, com o mesmo texto da API: "Arquivo CSV excede o tamanho máximo permitido de 5MB" | Evita subir um arquivo que a API vai recusar com `413` | n |
| Tipo de arquivo | O seletor sugere `.csv` (`accept=".csv,text/csv"`); a tela não valida extensão e deixa o conteúdo para a API | A API valida o arquivo e devolve a mensagem certa | n |
| Quantidade de erros por linha | Mostra todos os `rowErrors`, na ordem em que a API devolve, numa lista com rolagem | O objetivo é corrigir o arquivo de uma vez | n |
| Mensagem de sucesso | "<fileName> importado: <totalRows> linha(s)" usando os campos da resposta | Confirma qual arquivo entrou e quantas linhas | n |
| Lista de lotes | Página de 20, mais recente primeiro (`sort=createdAt,desc`), com "Anterior" e "Próxima" quando houver mais de uma página | Mesmo padrão paginado da tela de Empresas | n |
| Formato da data do lote | `dd/mm/aaaa hh:mm` no fuso do navegador | Só formatação de dado recebido (AD-010) | n |
| O que atualiza após import aceito | A lista de lotes da aba volta para a página 1 e é recarregada; a consulta dos últimos imports do dashboard (`queryKeys.imports`) é invalidada | O dashboard mostra a hora do último import de liquidação e de extrato | n |
| Sem empresa selecionada | Mesmo estado vazio da tela de Regras de taxa, pedindo para selecionar uma empresa no menu | Consistência entre telas por empresa | n |
| Troca de empresa ou de aba | Descarta o arquivo escolhido e o resultado do último envio; a lista passa a ser da empresa/aba atual | O resultado pertence ao par empresa + tipo que o gerou | n |
| Envio concorrente | Enquanto um envio da aba está em andamento, o botão de envio fica desabilitado | Evita enviar o mesmo arquivo duas vezes por duplo clique | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Enviar um CSV e ver o resultado ⭐ MVP

**User Story**: As a operador, I want enviar o CSV de transações, de liquidação ou de extrato da empresa selecionada so that a conciliação tenha os dois lados para cruzar.

**Why P1**: Sem os dados importados não há conciliação; é o motivo da tela.

**Acceptance Criteria**:

1. WHEN o operador abre `/importacoes` com uma empresa selecionada THEN the system SHALL mostrar as abas "Transações", "Liquidação" e "Extrato", com a aba indicada por `?tipo=` ativa e "Transações" quando `tipo` estiver ausente ou desconhecido.
2. WHEN o operador troca de aba THEN the system SHALL atualizar `?tipo=` para `transacoes`, `liquidacao` ou `extrato`.
3. The system SHALL mostrar em cada aba as colunas esperadas do CSV daquele tipo, na ordem listada nas Assumptions.
4. WHILE nenhum arquivo estiver escolhido the system SHALL manter o botão "Importar" desabilitado.
5. WHEN o operador escolhe um arquivo de até 5 MB e clica em "Importar" THEN the system SHALL enviar um `POST` multipart com a parte `file` para o endpoint de import do tipo da aba (`/transactions/import`, `/external-settlements/import` ou `/bank-statements/import`) da empresa selecionada.
6. The system SHALL enviar o import de liquidação sem o parâmetro `layout`.
7. WHILE o envio estiver em andamento the system SHALL manter o botão "Importar" desabilitado com o texto "Importando…".
8. WHEN a API aceita o arquivo THEN the system SHALL mostrar "<fileName> importado: <totalRows> linha(s)" e limpar o arquivo escolhido.
9. IF o arquivo escolhido tem mais de 5 MB THEN the system SHALL mostrar "Arquivo CSV excede o tamanho máximo permitido de 5MB" e não enviar a requisição.

**Independent Test**: Com uma empresa e regras de taxa, enviar um CSV válido de transações e ver a mensagem de sucesso com o nome do arquivo e o total de linhas.

---

### P1: Ver por que o arquivo foi recusado ⭐ MVP

**User Story**: As a operador, I want ver todos os motivos da recusa so that eu corrija o arquivo de uma vez, sem tentativa e erro.

**Why P1**: O import é tudo ou nada; sem os erros por linha o operador não sabe o que corrigir.

**Acceptance Criteria**:

1. IF a API responde `400` com `details.rowErrors` THEN the system SHALL mostrar a `message` da resposta e uma tabela com as colunas "Linha" e "Erro", com uma linha por item de `rowErrors`, na ordem da resposta.
2. IF a API responde `409` com `details.conflictingReferences` THEN the system SHALL mostrar a `message` da resposta e a lista de todas as referências de `conflictingReferences`.
3. IF a API responde com erro sem `rowErrors` nem `conflictingReferences` (período travado, `413`, `403`, cabeçalho inválido) THEN the system SHALL mostrar a `message` da resposta.
4. IF a requisição falha sem resposta da API THEN the system SHALL mostrar "Não foi possível conectar à API. Tente novamente."
5. WHEN um novo envio começa THEN the system SHALL remover o resultado do envio anterior.
6. IF o arquivo foi recusado THEN the system SHALL manter o arquivo escolhido para que o operador possa reenviá-lo depois de corrigir.

**Independent Test**: Enviar um CSV de liquidação com `netAmount` maior que `amount` na linha 3 e ver a tabela com a linha 3 e a mensagem da API.

---

### P1: Ver os lotes importados ⭐ MVP

**User Story**: As a operador, I want ver os lotes já importados de cada tipo so that eu saiba o que já entrou antes de enviar de novo.

**Why P1**: Reenviar um arquivo já importado gera `409`; a lista evita isso e confirma o que o dashboard vai usar.

**Acceptance Criteria**:

1. WHEN uma aba é aberta THEN the system SHALL listar os lotes daquele tipo da empresa selecionada (`GET /<tipo>/imports?page=<n>&size=20&sort=createdAt,desc`) com as colunas "Arquivo", "Linhas" e "Importado em".
2. The system SHALL exibir "Importado em" no formato `dd/mm/aaaa hh:mm` no fuso do navegador.
3. WHILE a lista tiver mais de uma página the system SHALL mostrar "Anterior" e "Próxima", desabilitando "Anterior" na primeira página e "Próxima" na última.
4. IF a empresa não tem lotes daquele tipo THEN the system SHALL mostrar "Nenhuma importação de <transações|liquidação|extrato> ainda."
5. IF a listagem falha THEN the system SHALL mostrar a mensagem do erro e um botão "Tentar novamente" que refaz a consulta.
6. WHEN um import é aceito THEN the system SHALL voltar a lista da aba para a primeira página e recarregá-la, mostrando o novo lote no topo.
7. WHEN um import é aceito THEN the system SHALL invalidar a consulta dos últimos imports usada pelo dashboard.

**Independent Test**: Importar um extrato e ver o lote no topo da aba Extrato com o nome do arquivo, o total de linhas e a data e hora.

---

### P2: Empresa e aba isolam o estado

**User Story**: As a operador com várias empresas, I want que a tela mostre só o que pertence à empresa e à aba atuais so that eu não confunda resultado ou lote de uma empresa com o de outra.

**Why P2**: O isolamento por empresa é regra do produto; o P1 funciona com uma empresa só.

**Acceptance Criteria**:

1. WHEN o operador seleciona outra empresa no menu THEN the system SHALL descartar o arquivo escolhido e o resultado do último envio e mostrar os lotes da nova empresa.
2. WHEN o operador troca de aba THEN the system SHALL descartar o arquivo escolhido e o resultado do último envio.
3. IF nenhuma empresa está selecionada THEN the system SHALL mostrar o mesmo estado vazio da tela de Regras de taxa em vez das abas.

**Independent Test**: Com um erro por linha na tela, trocar de empresa e ver a tabela de erros sumir e a lista de lotes mudar.

---

## Edge Cases

- IF a resposta de erro não é JSON THEN the system SHALL mostrar "A API retornou erro <status>." (comportamento atual do cliente).
- WHEN o arquivo tem exatamente 5 MB (5.242.880 bytes) THEN the system SHALL enviá-lo.
- IF a empresa selecionada perde o acesso e a API responde `403` THEN the system SHALL mostrar a `message` da resposta (coberto pelo critério 3 de "Ver por que o arquivo foi recusado").

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| IMP-01 | P1 Enviar: abas e `?tipo=` (AC 1, 2) | Design | Pending |
| IMP-02 | P1 Enviar: colunas esperadas (AC 3) | Design | Pending |
| IMP-03 | P1 Enviar: envio multipart por tipo, sem `layout` (AC 4, 5, 6, 7) | Design | Pending |
| IMP-04 | P1 Enviar: mensagem de sucesso e limpeza (AC 8) | Design | Pending |
| IMP-05 | P1 Enviar: limite de 5 MB no cliente (AC 9, edge 5 MB exato) | Design | Pending |
| IMP-06 | P1 Recusa: tabela de `rowErrors` (AC 1) | Design | Pending |
| IMP-07 | P1 Recusa: `conflictingReferences` (AC 2) | Design | Pending |
| IMP-08 | P1 Recusa: demais erros e falha de rede (AC 3, 4, edge não JSON) | Design | Pending |
| IMP-09 | P1 Recusa: novo envio limpa o resultado; arquivo mantido (AC 5, 6) | Design | Pending |
| IMP-10 | P1 Lotes: lista paginada e formato (AC 1, 2, 3) | Design | Pending |
| IMP-11 | P1 Lotes: vazio e erro com nova tentativa (AC 4, 5) | Design | Pending |
| IMP-12 | P1 Lotes: atualização após import aceito (AC 6, 7) | Design | Pending |
| IMP-13 | P2 Isolamento: troca de empresa/aba e sem empresa (AC 1, 2, 3) | Design | Pending |

**Coverage:** 13 total, 0 mapped to tasks, 13 unmapped ⚠️

---

## Success Criteria

- [ ] Os três CSVs de exemplo de uma empresa entram pela tela, sem Swagger, e aparecem como lotes nas abas.
- [ ] Um arquivo com erros em várias linhas mostra todas elas num único envio.
- [ ] Depois de importar liquidação e extrato, o dashboard mostra as horas dos novos imports sem recarregar a página.
