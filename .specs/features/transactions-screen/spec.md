# Tela de Transações Specification

## Problem Statement

As transações internas entram por import CSV (tela de Importações) ou pelo `POST /transactions`, mas `/transacoes` ainda é um placeholder. O operador não tem como ver as transações da empresa, lançar uma venda avulsa ou registrar cancelamento, estorno e chargeback sem o Swagger. Uma mudança de status que não chega ao ReconPay vira divergência "status inconsistente" na conciliação, e o operador não tem onde corrigi-la.

## Goals

- [ ] Com uma empresa selecionada, listar as transações por período, status e método em `/transacoes`.
- [ ] Cadastrar uma transação avulsa por formulário, com as mesmas regras da API e o erro da API visível sem perder o que foi digitado.
- [ ] Mudar o status de uma transação aprovada para cancelada, estornada ou chargeback, com confirmação explícita por ser irreversível.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Import CSV de transações | Já existe na aba Transações de `/importacoes` (AD-011) |
| Editar referência, valor, método, parcelas ou data | A API não tem endpoint para isso |
| Excluir transação | A API não tem endpoint para isso |
| Reverter status (voltar para aprovada) | A API recusa: os status finais não têm saída |
| Busca por referência externa | A API não tem esse filtro |
| Página de detalhe da transação | Decisão do usuário: o status muda pela ação na linha; a linha já mostra todos os campos |
| Mostrar as divergências de uma transação | Pertence às telas de Divergências e Conciliações |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Filtros | Período + status + método, sobre os parâmetros `fromDate`, `toDate`, `status` e `paymentMethod` da API | Escolha do usuário | y |
| Seletor de período | O mesmo menu do dashboard (`PeriodMenu`: meses e "Personalizado…"); período inicial = mês atual | Escolha do usuário; abrir no mês corrente mostra o que está em andamento | y (menu) / n (mês inicial) |
| Meses listados no menu | Do mês atual até 11 meses atrás, sem marcas de run ou de travamento | Na tela de Transações não há run vigente; 12 meses cobrem o ciclo de fechamento | n |
| Intervalo personalizado inválido | Data inicial depois da final mostra "A data inicial deve ser igual ou anterior à final" no menu, sem consultar a API | A listagem da API aceita qualquer intervalo e devolveria lista vazia sem explicar | n |
| Formulário | Gaveta lateral (`SideDrawer`), aberta por "Nova transação" | Escolha do usuário; mesmo padrão de Empresas e Regras de taxa | y |
| Mudança de status | Ação "Alterar status" na linha, só em transação aprovada, com diálogo (Cancelar, Estornar, Chargeback) e aviso de que não pode ser desfeita | Escolha do usuário | y |
| Rótulos de status | Aprovada, Cancelada, Estornada, Chargeback (`APPROVED`, `CANCELLED`, `REFUNDED`, `CHARGEBACK`) | Termos usados pelo operador | n |
| Rótulos de método | Os do dashboard (`METHOD_LABELS`: Crédito, Débito, Pix, Boleto) | Consistência entre telas | n |
| Colunas da lista | Data, Referência, Método, Parcelas, Bruto, Líquido esperado, Status, ação | Todos os campos de negócio que a API devolve | n |
| Formatos | Data `dd/mm/aaaa` lida como texto (sem fuso); parcelas `3x`; dinheiro com `formatMoney` (`R$ 1.234,50`) | Só formatação de dado recebido (AD-010) | n |
| Ordem e paginação | 20 por página, data da transação mais recente primeiro (`sort=transactionDate,desc`), "Anterior"/"Próxima" com mais de uma página | Mesmo padrão de Empresas e Importações | n |
| Filtros e página | Mudar período, status ou método volta para a página 1 | A página antiga pode não existir no novo resultado | n |
| Filtros na URL | Não; ficam no estado da tela e voltam ao padrão ao recarregar | A tela não tem link compartilhável pedido; menos superfície | n |
| Campos do formulário | Referência externa, Valor, Método, Parcelas, Data da transação; Data começa com hoje | Mesmo corpo do `POST /transactions`; venda avulsa é quase sempre do dia | n |
| Validação no cliente | Referência obrigatória após remover espaços, até 100 caracteres; Valor maior que zero, até 17 dígitos inteiros e 2 casas, vírgula ou ponto; Parcelas inteiro a partir de 1; Pix, Boleto e Débito fixam 1 parcela; Data obrigatória e não futura | As mesmas regras da API, para o erro aparecer antes do envio | n |
| Mensagens de validação | Referência: "Informe a referência externa" / "Use até 100 caracteres"; Valor: "Informe um valor maior que zero com até 2 casas decimais"; Parcelas: "Informe um número inteiro de parcelas a partir de 1"; Data: "Informe a data da transação" / "Data da transação não pode ser futura" | Uma mensagem por campo; a de data futura é o texto da API | n |
| Envio do valor | Número JSON montado a partir do texto digitado (`LosslessNumber`), sem passar por `float` | Mesmo cuidado das regras de taxa | n |
| Erros da API no cadastro e no status | A gaveta ou o diálogo continuam abertos com a `message` da API e os campos preenchidos | Mesmo padrão de Empresas e Regras de taxa | n |
| Falta de regra de taxa | Além da mensagem, o erro de cadastro sem regra ativa mostra o link "Cadastrar regra de taxa" para `/taxas?cadastrar=1` | AD-012 previa esse ponto de entrada depois; é o erro mais provável do formulário | n |
| Depois de cadastrar | Fecha a gaveta, mostra "Transação <referência> cadastrada." e recarrega a lista | Confirma o que entrou; a transação pode estar fora do filtro atual | n |
| Depois de mudar o status | Fecha o diálogo e recarrega a lista; a linha passa a mostrar o novo status sem a ação | O status é final | n |
| Troca de empresa | Fecha gaveta e diálogo, descarta o aviso de cadastro, mantém período e filtros e volta para a página 1 | O filtro é escolha do operador, não da empresa | n |
| Sem empresa selecionada | Mesmo estado vazio de Regras de taxa e Importações | Consistência entre telas por empresa | n |
| Edição concorrente | A API usa versão otimista e responde `409`; a tela mostra a mensagem e recarrega a lista | Evita sobrescrever um status que outra pessoa já mudou | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Ver as transações da empresa ⭐ MVP

**User Story**: As a operador, I want ver as transações da empresa selecionada filtradas por período, status e método so that eu confira o que entrou antes de conciliar.

**Why P1**: Sem a lista, o operador não sabe o que o import ou o formulário gravaram.

**Acceptance Criteria**:

1. WHEN o operador abre `/transacoes` com uma empresa selecionada THEN the system SHALL buscar `GET /transactions?fromDate=<1º dia do mês atual>&toDate=<último dia do mês atual>&page=0&size=20&sort=transactionDate,desc` e mostrar o título "Transações" e o nome da empresa.
2. The system SHALL mostrar cada transação com as colunas Data, Referência, Método, Parcelas, Bruto, Líquido esperado e Status, nos formatos das Assumptions.
3. WHEN o operador escolhe um mês ou um intervalo personalizado no menu de período THEN the system SHALL refazer a busca com esse `fromDate` e `toDate` e voltar para a página 1.
4. IF o intervalo personalizado tem data inicial depois da final THEN the system SHALL mostrar "A data inicial deve ser igual ou anterior à final" no menu e não refazer a busca.
5. WHEN o operador escolhe um status no filtro THEN the system SHALL refazer a busca com `status=<valor>` e voltar para a página 1; "Todos" SHALL remover o parâmetro.
6. WHEN o operador escolhe um método no filtro THEN the system SHALL refazer a busca com `paymentMethod=<valor>` e voltar para a página 1; "Todos" SHALL remover o parâmetro.
7. WHILE a lista tiver mais de uma página the system SHALL mostrar "Anterior" e "Próxima", desabilitando "Anterior" na primeira página e "Próxima" na última.
8. IF a busca não tem transações THEN the system SHALL mostrar "Nenhuma transação encontrada para os filtros escolhidos."
9. IF a busca falha THEN the system SHALL mostrar a mensagem do erro e um botão "Tentar novamente" que refaz a busca.

**Independent Test**: Com transações em outubro, abrir `/transacoes`, ver a lista do mês e filtrar por "Cancelada" para ver só as canceladas.

---

### P1: Cadastrar uma transação avulsa ⭐ MVP

**User Story**: As a operador, I want cadastrar uma venda avulsa pelo formulário so that a correção ou o caso isolado não dependam de montar um CSV.

**Why P1**: É o segundo caminho de entrada definido no AD-011.

**Acceptance Criteria**:

1. WHEN o operador clica em "Nova transação" THEN the system SHALL abrir a gaveta com Referência externa, Valor, Método, Parcelas e Data da transação, com Data preenchida com hoje e Parcelas com 1.
2. WHILE o método escolhido for Pix, Boleto ou Débito the system SHALL fixar Parcelas em 1 e manter o campo desabilitado.
3. IF algum campo viola as regras de validação das Assumptions THEN the system SHALL mostrar a mensagem daquele campo e não chamar a API.
4. WHEN o formulário é válido e o operador clica em "Cadastrar" THEN the system SHALL enviar `POST /transactions` com `externalReference` sem espaços nas extremidades, `amount` como número JSON igual ao texto digitado (vírgula convertida em ponto), `paymentMethod`, `installments` e `transactionDate` (`aaaa-mm-dd`).
5. WHILE o cadastro estiver em andamento the system SHALL desabilitar o botão com o texto "Cadastrando…".
6. WHEN a API aceita a transação THEN the system SHALL fechar a gaveta, mostrar "Transação <referência> cadastrada." e recarregar a lista.
7. IF a API recusa o cadastro (`400`, `409` de referência repetida, `409` de período travado) THEN the system SHALL manter a gaveta aberta, com os campos preenchidos e a `message` da API.

**Independent Test**: Cadastrar `AVULSA-1`, R$ 100,00, Crédito 3x, com hoje como data, e ver o aviso e a linha na lista do mês.

---

### P1: Mudar o status de uma transação aprovada ⭐ MVP

**User Story**: As a operador, I want marcar uma venda aprovada como cancelada, estornada ou chargeback so that a conciliação compare o status certo com o da liquidação.

**Why P1**: Status desatualizado gera divergência "status inconsistente" que só se resolve na origem.

**Acceptance Criteria**:

1. The system SHALL mostrar a ação "Alterar status" apenas nas linhas com status Aprovada.
2. WHEN o operador clica em "Alterar status" THEN the system SHALL abrir um diálogo com a referência da transação, as opções Cancelar, Estornar e Chargeback e o aviso "A mudança não pode ser desfeita."
3. WHILE nenhuma opção estiver escolhida the system SHALL manter o botão "Confirmar" desabilitado.
4. WHEN o operador confirma THEN the system SHALL enviar `PATCH /transactions/{id}/status` com `status` `CANCELLED`, `REFUNDED` ou `CHARGEBACK`, conforme a opção.
5. WHILE a mudança estiver em andamento the system SHALL desabilitar o botão com o texto "Confirmando…".
6. WHEN a API aceita THEN the system SHALL fechar o diálogo e recarregar a lista, mostrando a linha com o novo status e sem a ação.
7. IF a API recusa (`409` de transição inválida, de período travado ou de versão) THEN the system SHALL manter o diálogo aberto com a `message` da API e recarregar a lista.

**Independent Test**: Numa transação aprovada, escolher Estornar e confirmar; a linha passa a mostrar "Estornada" sem o botão.

---

### P2: Atalho para a regra de taxa que falta

**User Story**: As a operador, I want ir direto ao cadastro de regra de taxa quando o formulário recusar a venda por falta de regra so that eu resolva a causa sem procurar a tela.

**Why P2**: O cadastro funciona sem o atalho; ele encurta o caminho do erro mais comum.

**Acceptance Criteria**:

1. IF a API recusa o cadastro com "Regra de taxa ativa não encontrada para este comerciante, método de pagamento e parcelas" THEN the system SHALL mostrar, junto da mensagem, o link "Cadastrar regra de taxa" para `/taxas?cadastrar=1`.
2. IF a API recusa o cadastro por outro motivo THEN the system SHALL não mostrar esse link.

**Independent Test**: Cadastrar Pix numa empresa sem regra Pix e ver o link levar à gaveta de nova regra.

---

### P2: Empresa isola o estado

**User Story**: As a operador com várias empresas, I want que a tela mostre só a empresa atual so that eu não altere a transação de uma empresa achando que é de outra.

**Why P2**: O isolamento por empresa é regra do produto; o P1 funciona com uma empresa só.

**Acceptance Criteria**:

1. WHEN o operador seleciona outra empresa no menu THEN the system SHALL fechar gaveta e diálogo abertos, remover o aviso de cadastro, manter período e filtros e buscar a página 1 da nova empresa.
2. IF nenhuma empresa está selecionada THEN the system SHALL mostrar o mesmo estado vazio de Regras de taxa, sem filtros nem lista.

**Independent Test**: Com o diálogo de status aberto, trocar de empresa e ver o diálogo fechar e a lista mudar.

---

## Edge Cases

- WHEN o valor digitado tem 17 dígitos inteiros e 2 casas THEN the system SHALL aceitá-lo; com 18 dígitos inteiros ou 3 casas THEN the system SHALL recusá-lo com a mensagem de Valor.
- WHEN a Data da transação é hoje THEN the system SHALL aceitá-la; amanhã THEN the system SHALL recusá-la com "Data da transação não pode ser futura".
- WHEN a referência tem 100 caracteres depois de remover os espaços das extremidades THEN the system SHALL aceitá-la; com 101 THEN the system SHALL recusá-la com "Use até 100 caracteres".
- WHEN o operador troca o método de Crédito 3x para Pix e volta para Crédito THEN the system SHALL manter Parcelas em 1 até o operador mudar.
- IF a resposta de erro não é JSON THEN the system SHALL mostrar "A API retornou erro <status>." (comportamento atual do cliente).

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| TXN-01 | P1 Lista: busca inicial do mês atual e colunas (AC 1, 2) | Design | Pending |
| TXN-02 | P1 Lista: período por mês ou intervalo, validação do intervalo (AC 3, 4) | Design | Pending |
| TXN-03 | P1 Lista: filtros de status e método (AC 5, 6) | Design | Pending |
| TXN-04 | P1 Lista: paginação, vazio e erro (AC 7, 8, 9) | Design | Pending |
| TXN-05 | P1 Cadastro: gaveta, campos e parcelas fixas (AC 1, 2) | Design | Pending |
| TXN-06 | P1 Cadastro: validação no cliente e edge cases (AC 3) | Design | Pending |
| TXN-07 | P1 Cadastro: envio, sucesso e erro da API (AC 4, 5, 6, 7) | Design | Pending |
| TXN-08 | P1 Status: ação só em aprovada e diálogo (AC 1, 2, 3) | Design | Pending |
| TXN-09 | P1 Status: envio, sucesso e erro da API (AC 4, 5, 6, 7) | Design | Pending |
| TXN-10 | P2 Atalho para regra de taxa (AC 1, 2) | Design | Pending |
| TXN-11 | P2 Isolamento por empresa e sem empresa (AC 1, 2) | Design | Pending |

**Coverage:** 11 total, 0 mapped to tasks, 11 unmapped ⚠️

---

## Success Criteria

- [ ] O operador lista, cadastra e muda o status de transações de uma empresa sem usar o Swagger.
- [ ] Nenhum cadastro inválido chega à API: os erros de campo aparecem antes do envio.
- [ ] Uma transação estornada pela tela entra no próximo run de conciliação com o status `REFUNDED`.
