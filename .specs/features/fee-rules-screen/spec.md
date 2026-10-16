# Tela de Regras de Taxa Specification

## Problem Statement

Uma transação só entra no ReconPay se a empresa tiver regra de taxa ativa para a combinação exata de método de pagamento e parcelas; sem ela, a API responde `409`. Hoje não há tela para cadastrar essas regras, então o fluxo para logo depois de cadastrar a empresa. Além disso, a API aceita regra de PIX, boleto ou débito com mais de uma parcela, que nunca seria usada porque essas transações não podem ser parceladas.

## Goals

- [ ] Com uma empresa selecionada, listar, cadastrar, alterar taxas e desativar as regras dela pela tela `/taxas`, sem Swagger.
- [ ] Nenhuma regra impossível de usar: PIX, boleto e débito só com 1 parcela, na API e na tela.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Cadastrar uma faixa de parcelas de uma vez (ex.: 2x a 12x) | Decisão do usuário: uma regra por envio nesta feature |
| Histórico de regras desativadas / reativar regra | A API lista só ativas; histórico fica para depois |
| Alterar método ou parcelas de uma regra existente | Decisão do usuário: para outra combinação, desativa e cria |
| Simular o líquido de uma venda com a regra | AD-010: o frontend não calcula dinheiro |
| Item "Taxas" na barra superior | AD-012: a tela é aberta pelo menu de empresa |
| Teto de parcelas no cartão de crédito | A API não tem teto; inventar um limite seria regra nova de negócio |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Parcelas em PIX, boleto e débito | API recusa com `400` na criação e na alteração; a tela fixa 1 parcela para esses métodos | Escolha do usuário; mesma regra de `PaymentMethodRules` usada nas transações | y |
| O que a edição altera | Só percentual e taxa fixa; método e parcelas ficam somente leitura | Escolha do usuário | y |
| Faixa de parcelas | Uma regra por envio | Escolha do usuário | y |
| Desativar regra | Sim, com diálogo de confirmação | Escolha do usuário | y |
| Rota e acesso | `/taxas`, sempre da empresa selecionada; aberta pelo item "Regras de taxa da <empresa>" no menu de empresa | AD-012 | y |
| Mensagem de método sem parcelamento na API | "Método de pagamento <MÉTODO> não permite parcelamento" (`400 VALIDATION_ERROR`) | Mesmo texto e exceção que a criação de transação já usa | n |
| Ordem da lista | Por método e depois por parcelas (`sort=paymentMethod,asc&sort=installments,asc`) | Agrupa as parcelas do cartão de crédito | n |
| Paginação | Uma página de até 100 regras, sem navegação | 4 métodos e poucas parcelas por empresa; nenhuma empresa realista passa de 100 | n |
| Formato exibido | Método com o rótulo do dashboard (Crédito, Débito, Pix, Boleto); parcelas como `1x`, `3x`; percentual com vírgula e sem zeros à direita (`2,99%`, `0%`); taxa fixa com `formatMoney` (`R$ 0,30`) | Só formatação de texto recebido, permitido pelo AD-010 | n |
| Entrada de números | Percentual e taxa fixa aceitam vírgula ou ponto como separador decimal e são enviados como número JSON sem passar por `float` | Mesmo cuidado do ajuste de divergência (`LosslessNumber`) | n |
| Edição concorrente | Última escrita vence | A entidade não tem `@Version`; mesma escolha de Empresas | n |
| Mudança de empresa no menu com a tela aberta | A tela passa a mostrar as regras da nova empresa e fecha gaveta/diálogo abertos | A tela é sempre da empresa selecionada | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Método sem parcelamento recusado na API ⭐ MVP

**User Story**: As a operador, I want que a API recuse regra parcelada de PIX, boleto ou débito so that não exista regra que nenhuma transação consegue usar.

**Why P1**: A regra não pode depender da tela; Swagger e clientes programáticos usam a mesma API.

**Acceptance Criteria**:

1. IF `POST /api/merchants/{merchantId}/fee-rules` recebe `paymentMethod` `PIX`, `BOLETO` ou `DEBIT_CARD` com `installments` maior que 1 THEN the system SHALL responder `400` `VALIDATION_ERROR` com a mensagem "Método de pagamento <MÉTODO> não permite parcelamento".
2. IF `PUT /api/merchants/{merchantId}/fee-rules/{id}` resulta numa combinação de `PIX`, `BOLETO` ou `DEBIT_CARD` com mais de 1 parcela THEN the system SHALL responder `400` `VALIDATION_ERROR` com a mesma mensagem e não alterar a regra.
3. WHEN a API recebe `CREDIT_CARD` com qualquer número de parcelas a partir de 1, ou `PIX`, `BOLETO` ou `DEBIT_CARD` com 1 parcela, THEN the system SHALL criar a regra normalmente (`201`).

**Independent Test**: Pela API, `POST` de PIX com 3 parcelas recebe `400` com a mensagem; PIX com 1 parcela recebe `201`.

---

### P1: Listar regras da empresa selecionada ⭐ MVP

**User Story**: As an OPERATOR or ADMIN, I want ver as regras de taxa da empresa selecionada so that eu saiba quais combinações já aceitam transações.

**Why P1**: Base para criar, editar e desativar.

**Acceptance Criteria**:

1. WHEN o usuário abre o menu de empresa com uma empresa selecionada THEN the system SHALL mostrar no rodapé o item "Regras de taxa da <nome da empresa>", que fecha o menu e abre `/taxas`.
2. WHEN o usuário abre `/taxas` THEN the system SHALL buscar `GET /api/merchants/{merchantId}/fee-rules?page=0&size=100&sort=paymentMethod,asc&sort=installments,asc` da empresa selecionada e mostrar o título "Regras de taxa" com o nome da empresa.
3. The system SHALL mostrar em cada linha o método (Crédito, Débito, Pix ou Boleto), as parcelas (`1x`, `3x`), o percentual (`2,99%`) e a taxa fixa (`R$ 0,30`).
4. WHEN o usuário troca a empresa no menu do topo THEN the system SHALL buscar e mostrar as regras da nova empresa, sem exibir regras da anterior.
5. IF a empresa não tem regras THEN the system SHALL mostrar "Nenhuma regra de taxa" com o texto "Transações só entram com uma regra ativa para o método e as parcelas." e o botão "Cadastrar regra".
6. IF o usuário não tem empresa no acesso THEN the system SHALL mostrar o estado "Nenhuma empresa no seu acesso" com "Cadastrar empresa".
7. WHILE a lista carrega the system SHALL mostrar o estado de carregamento ("Carregando regras de taxa") no lugar da tabela.
8. IF a busca falha THEN the system SHALL mostrar a mensagem do erro com "Tentar de novo", que refaz a busca.

**Independent Test**: Com uma empresa com regras de Pix 1x e Crédito 3x, abrir "Regras de taxa da <empresa>" no menu e ver as duas linhas formatadas, Crédito antes de Pix.

---

### P1: Cadastrar regra ⭐ MVP

**User Story**: As an OPERATOR or ADMIN, I want cadastrar uma regra de taxa so that as transações daquele método e parcelas possam entrar.

**Why P1**: Destrava transações e o import CSV que vem depois.

**Acceptance Criteria**:

1. WHEN o usuário clica em "Cadastrar regra" THEN the system SHALL abrir uma gaveta lateral "Cadastrar regra de taxa" com os campos Método, Parcelas, Percentual e Taxa fixa, sem método escolhido e com Parcelas `1`.
2. WHILE o método escolhido é Pix, Boleto ou Débito the system SHALL manter Parcelas em `1`, desabilitado.
3. WHEN o usuário escolhe Crédito THEN the system SHALL habilitar Parcelas para qualquer inteiro a partir de 1.
4. WHEN o usuário envia campos válidos THEN the system SHALL chamar `POST /api/merchants/{merchantId}/fee-rules` com `paymentMethod`, `installments` (inteiro), `feePercentage` e `fixedFee` como números JSON com o valor exato digitado (vírgula convertida em ponto).
5. WHEN a API responde `201` THEN the system SHALL fechar a gaveta e mostrar a nova regra na lista.
6. IF nenhum método foi escolhido THEN the system SHALL bloquear o envio sem chamar a API e mostrar "Escolha o método de pagamento".
7. IF Parcelas não é um inteiro a partir de 1 THEN the system SHALL bloquear o envio sem chamar a API e mostrar "Informe um número inteiro de parcelas a partir de 1".
8. IF Percentual está vazio, não é número, é menor que 0, maior que 100 ou tem mais de 4 casas decimais THEN the system SHALL bloquear o envio sem chamar a API e mostrar "Informe um percentual de 0 a 100 com até 4 casas decimais".
9. IF Taxa fixa está vazia, não é número, é negativa ou tem mais de 2 casas decimais THEN the system SHALL bloquear o envio sem chamar a API e mostrar "Informe uma taxa fixa a partir de 0 com até 2 casas decimais".
10. IF a API responde `409` (já existe regra ativa para o método e as parcelas) ou outro erro, ou a rede falha, THEN the system SHALL manter a gaveta aberta com os valores digitados e mostrar a mensagem do erro.
11. WHILE o cadastro está em envio the system SHALL desabilitar o botão "Cadastrar", de modo que cliques repetidos gerem um único `POST`.
12. WHEN o usuário pressiona `Esc` ou "Cancelar" THEN the system SHALL fechar a gaveta sem chamar a API.

**Independent Test**: Cadastrar Crédito 3x com `2,99` e `0,30`, ver a linha `Crédito · 3x · 2,99% · R$ 0,30`; tentar de novo a mesma combinação e ver a mensagem do `409` com a gaveta aberta.

---

### P2: Alterar taxas de uma regra

**User Story**: As an OPERATOR or ADMIN, I want corrigir o percentual ou a taxa fixa de uma regra so that transações novas usem o contrato atual.

**Why P2**: Correção; o fluxo de teste não depende dele.

**Acceptance Criteria**:

1. WHEN o usuário escolhe "Editar" numa regra THEN the system SHALL abrir a gaveta "Editar regra de taxa" com Método e Parcelas somente leitura e Percentual e Taxa fixa preenchidos com os valores atuais (vírgula como separador).
2. WHEN o usuário salva valores válidos THEN the system SHALL chamar `PUT /api/merchants/{merchantId}/fee-rules/{id}` só com `feePercentage` e `fixedFee`, como números JSON com o valor exato digitado.
3. WHEN a API responde `200` THEN the system SHALL fechar a gaveta e mostrar os novos valores na lista.
4. IF Percentual ou Taxa fixa são inválidos THEN the system SHALL bloquear o envio sem chamar a API e mostrar as mesmas mensagens de FCAD-08 e FCAD-09.
5. IF a API responde erro ou a rede falha THEN the system SHALL manter a gaveta aberta com os valores digitados e mostrar a mensagem do erro.
6. WHILE a alteração está em envio the system SHALL desabilitar o botão "Salvar".

**Independent Test**: Alterar Crédito 3x para `3,49` e ver `3,49%` na lista.

---

### P2: Desativar regra

**User Story**: As an OPERATOR or ADMIN, I want desativar uma regra que não vale mais so that transações novas daquela combinação não usem uma taxa errada.

**Why P2**: Limpeza e troca de combinação; não bloqueia o fluxo.

**Acceptance Criteria**:

1. WHEN o usuário escolhe "Desativar" numa regra THEN the system SHALL abrir um diálogo de confirmação "Desativar a regra <Método> <N>x?" avisando que novas transações dessa combinação serão recusadas até existir outra regra.
2. WHEN o usuário confirma THEN the system SHALL chamar `DELETE /api/merchants/{merchantId}/fee-rules/{id}`.
3. WHEN a API responde `204` THEN the system SHALL fechar o diálogo e remover a regra da lista.
4. WHEN o usuário cancela ou pressiona `Esc` THEN the system SHALL fechar o diálogo sem chamar a API.
5. IF a API responde erro ou a rede falha THEN the system SHALL manter o diálogo aberto e mostrar a mensagem do erro.

**Independent Test**: Desativar Pix 1x e ver a lista sem ela; cadastrar Pix 1x de novo com sucesso.

---

## Edge Cases

- IF a API responde `401` em qualquer ação THEN the system SHALL encerrar a sessão e voltar ao login (comportamento global existente).
- IF a API responde `404` ao editar ou desativar (regra já desativada em outra aba) THEN the system SHALL mostrar a mensagem do erro e recarregar a lista.
- WHEN o usuário troca de empresa no menu com a gaveta ou o diálogo aberto THEN the system SHALL fechá-los sem chamar a API.
- WHEN o usuário digita `0` no percentual e `0` na taxa fixa THEN the system SHALL aceitar o envio (regra sem taxa).
- WHEN o percentual tem exatamente 4 casas (`2,9999`) ou é `100` THEN the system SHALL aceitar o envio.

---

## Implicit-Requirement Sweep

| Dimension | Resolution |
| --- | --- |
| Input validation & bounds | Método obrigatório, parcelas inteiro ≥ 1, percentual 0–100 com 4 casas, taxa fixa ≥ 0 com 2 casas (FCAD-06..09, FEDIT-04); parcelas 1 para Pix/Boleto/Débito na tela (FCAD-02) e na API (FREG-01, FREG-02) |
| Failure / partial-failure states | Gaveta e diálogo mantêm estado e mostram o erro (FCAD-10, FEDIT-05, FDES-05); lista com "Tentar de novo" (FLIST-08) |
| Idempotency / retry / duplicate handling | Botão desabilitado durante envio (FCAD-11, FEDIT-06); duplicidade de regra ativa é `409` da API existente (FCAD-10) |
| Auth boundaries & rate limits | Acesso à empresa garantido pelo `MerchantAccessAspect` existente; `401` e `404` nos edge cases; rate limit N/A because a API não tem throttle |
| Concurrency / ordering | Última escrita vence na edição (assumption); desativação concorrente vira `404` tratado nos edge cases |
| Data lifecycle / expiry | Desativar é soft delete; a mesma combinação pode ser cadastrada de novo (independent test de FDES) |
| Observability | N/A because a feature não adiciona fluxo novo de negócio além de CRUD já existente na API |
| External-dependency failure | N/A because a feature não chama serviço externo |
| State-transition integrity | Regra só vai de ativa a desativada (FDES-02); edição não muda método nem parcelas (FEDIT-01, FEDIT-02) |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| FREG-01 | P1: Método sem parcelamento na API | Execute | Implementing |
| FREG-02 | P1: Método sem parcelamento na API | Execute | Implementing |
| FREG-03 | P1: Método sem parcelamento na API | Execute | Implementing |
| FLIST-01 | P1: Listar regras | Execute | Implementing |
| FLIST-02 | P1: Listar regras | Execute | Implementing |
| FLIST-03 | P1: Listar regras | Execute | Implementing |
| FLIST-04 | P1: Listar regras | Execute | Implementing |
| FLIST-05 | P1: Listar regras | Execute | Implementing |
| FLIST-06 | P1: Listar regras | Execute | Implementing |
| FLIST-07 | P1: Listar regras | Execute | Implementing |
| FLIST-08 | P1: Listar regras | Execute | Implementing |
| FCAD-01 | P1: Cadastrar regra | Execute | Implementing |
| FCAD-02 | P1: Cadastrar regra | Execute | Implementing |
| FCAD-03 | P1: Cadastrar regra | Execute | Implementing |
| FCAD-04 | P1: Cadastrar regra | Execute | Implementing |
| FCAD-05 | P1: Cadastrar regra | Execute | Implementing |
| FCAD-06 | P1: Cadastrar regra | Execute | Implementing |
| FCAD-07 | P1: Cadastrar regra | Execute | Implementing |
| FCAD-08 | P1: Cadastrar regra | Execute | Implementing |
| FCAD-09 | P1: Cadastrar regra | Execute | Implementing |
| FCAD-10 | P1: Cadastrar regra | Execute | Implementing |
| FCAD-11 | P1: Cadastrar regra | Execute | Implementing |
| FCAD-12 | P1: Cadastrar regra | Execute | Implementing |
| FEDIT-01 | P2: Alterar taxas | Execute | Implementing |
| FEDIT-02 | P2: Alterar taxas | Execute | Implementing |
| FEDIT-03 | P2: Alterar taxas | Execute | Implementing |
| FEDIT-04 | P2: Alterar taxas | Execute | Implementing |
| FEDIT-05 | P2: Alterar taxas | Execute | Implementing |
| FEDIT-06 | P2: Alterar taxas | Execute | Implementing |
| FDES-01 | P2: Desativar regra | Design | Pending |
| FDES-02 | P2: Desativar regra | Execute | Implementing |
| FDES-03 | P2: Desativar regra | Execute | Implementing |
| FDES-04 | P2: Desativar regra | Execute | Implementing |
| FDES-05 | P2: Desativar regra | Design | Pending |

**ID format:** `F<STORY>-<NN>` (`FREG`, `FLIST`, `FCAD`, `FEDIT`, `FDES`); o número é a posição do critério na story.

**Coverage:** 34 total, 0 mapped to tasks, 34 unmapped ⚠️ (tasks ainda não criadas)

---

## Success Criteria

- [ ] Da empresa recém-cadastrada a uma regra ativa sem Swagger, em menos de 1 minuto.
- [ ] Nenhuma regra parcelada de Pix, Boleto ou Débito criada pela API ou pela tela.
- [ ] Backend e frontend passam no gate (`./mvnw verify`, `pnpm test`, `typecheck`, `build`) com os testes novos.
