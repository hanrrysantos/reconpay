# Import CSV de Transações Specification

## Problem Statement

Transações internas só entram no ReconPay uma a uma, por `POST /api/merchants/{merchantId}/transactions`. No domínio, elas chegam em lote do sistema do lojista (ERP, gateway, PDV); testar ou operar com dezenas de vendas exige um laço de chamadas fora do sistema. Liquidação e extrato já têm import CSV com validação por linha e lote rastreável; transação não (AD-011).

## Goals

- [ ] Importar as transações de um período a partir de um CSV, com o mesmo cálculo de líquido esperado e as mesmas regras do cadastro unitário.
- [ ] Um arquivo com qualquer problema não grava nada e devolve todos os erros de uma vez, com a linha de cada um.
- [ ] Cada import gera um lote consultável, base para a tela de Importações.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Tela de Importações e tela de Transações | Features seguintes (AD-011); esta é só backend |
| Importar só as linhas válidas | Decisão do usuário: tudo ou nada |
| Layout alternativo (ex.: cabeçalho em português) | Decisão do usuário: só o layout do ReconPay |
| Filtrar transações por lote (`importId`) e mostrar o lote na transação | Sem consumidor ainda; entra com a tela que precisar |
| Desfazer ou apagar um lote | Não existe para liquidação nem extrato |
| Atualizar transações já existentes pelo CSV | Referência já cadastrada é conflito, não atualização |
| Validar transição de status no import | O arquivo traz o estado atual da venda; transição só vale para `PATCH /status` |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Linhas com erro | Tudo ou nada: qualquer erro recusa o arquivo e nada é gravado | Escolha do usuário; igual a liquidação e extrato | y |
| Coluna de status | Obrigatória, `APPROVED`, `CANCELLED`, `REFUNDED` ou `CHARGEBACK` | Escolha do usuário; a conciliação compara status | y |
| Layout | Só `externalReference,amount,paymentMethod,installments,status,transactionDate` | Escolha do usuário | y |
| Referência já cadastrada no banco | `409 CONFLICT` com `conflictingReferences`, nada gravado | Escolha do usuário; igual à liquidação | y |
| Endpoint | `POST /api/merchants/{merchantId}/transactions/import`, multipart com o campo `file` | Mesmo formato de `/external-settlements/import` e `/bank-statements/import` | n |
| Ordem das verificações | Arquivo e cabeçalho → erros por linha (formato e regra de taxa) → referências já cadastradas → período travado | Erros que o usuário corrige no arquivo vêm juntos; conflito com o banco só depois de o arquivo estar íntegro | n |
| Regra de taxa ausente | Erro da linha (`400` em `rowErrors`), não `409` | É um problema de cada linha, que precisa aparecer junto dos outros | n |
| Textos de erro comuns com a liquidação | Os mesmos textos do `SettlementCsvParser` (referência, valores, método, parcelas, status) | Mesmo usuário lê os dois imports | n |
| Separador e formato | Vírgula entre colunas, ponto decimal (`100.50`), data `yyyy-MM-dd`, UTF-8; espaços nas pontas de cada campo são ignorados; linhas em branco são puladas | Igual à liquidação | n |
| Transações criadas pelo formulário | Continuam sem lote | Lote é só do import | n |
| Listagem de lotes | Paginada, mais recente primeiro (`createdAt,desc`), 20 por página por padrão | Igual a `/external-settlements/imports` | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Importar transações por CSV ⭐ MVP

**User Story**: As an OPERATOR or ADMIN, I want enviar o CSV de vendas de uma empresa so that as transações do período entrem de uma vez, com o líquido esperado calculado pelas regras de taxa.

**Why P1**: Sem isso, a conciliação não tem o lado interno sem chamadas uma a uma.

**Acceptance Criteria**:

1. WHEN `POST /api/merchants/{merchantId}/transactions/import` recebe um CSV válido THEN the system SHALL responder `201` com o lote (`id`, `merchantId`, `fileName`, `totalRows`, `createdAt`), sendo `totalRows` o número de transações gravadas.
2. WHEN o import é aceito THEN the system SHALL gravar cada linha como transação da empresa com `externalReference` sem espaços nas pontas, `amount`, `paymentMethod`, `installments`, `status` e `transactionDate` do arquivo.
3. WHEN o import é aceito THEN the system SHALL calcular o `expectedNetAmount` de cada transação com a regra de taxa ativa do método e das parcelas, com o mesmo resultado do cadastro unitário (`amount − amount × percentual ÷ 100 − taxa fixa`, duas casas, `HALF_UP`).
4. The system SHALL exigir o cabeçalho exato `externalReference,amount,paymentMethod,installments,status,transactionDate`; IF o cabeçalho difere THEN the system SHALL responder `400` `VALIDATION_ERROR` "Cabeçalho CSV inválido. Esperado: externalReference,amount,paymentMethod,installments,status,transactionDate".
5. IF o arquivo enviado está vazio (0 bytes) THEN the system SHALL responder `400` `VALIDATION_ERROR` "Arquivo CSV é obrigatório"; IF a requisição não tem o campo `file` THEN the system SHALL responder `400` `VALIDATION_ERROR` "Requisição inválida. Verifique os campos e parâmetros enviados." (handler existente).
6. IF o nome do arquivo não termina em `.csv` (sem diferenciar maiúsculas) THEN the system SHALL responder `400` `VALIDATION_ERROR` "Arquivo deve ser um CSV (.csv)".
7. IF o arquivo tem só o cabeçalho (ou só linhas em branco depois dele) THEN the system SHALL responder `400` `VALIDATION_ERROR` "CSV não contém registros".
8. IF o arquivo passa de 5 MB THEN the system SHALL responder `413` `VALIDATION_ERROR` "Arquivo CSV excede o tamanho máximo permitido de 5MB".
9. IF alguma linha tem erro THEN the system SHALL responder `400` `VALIDATION_ERROR` "Erro na importação do CSV" com `rowErrors` listando `row` (número da linha no arquivo, cabeçalho = 1) e `message` de todos os erros de todas as linhas, e não gravar nenhuma transação nem lote.
10. The system SHALL reportar, por linha, estes erros com estes textos:
    - número de colunas diferente de 6: "Número de colunas inválido. Esperado: 6";
    - referência vazia: "Referência externa é obrigatória";
    - referência com mais de 100 caracteres: "Referência externa deve ter no máximo 100 caracteres";
    - referência repetida no arquivo (a partir da segunda ocorrência): "Referência externa duplicada no arquivo: <referência>";
    - `amount` que não é número: "amount inválido";
    - `amount` com mais de 2 casas ou mais de 17 dígitos inteiros: "amount deve ter no máximo 17 dígitos inteiros e 2 casas decimais";
    - `amount` zero ou negativo: "amount deve ser maior que zero";
    - método fora de `CREDIT_CARD`, `DEBIT_CARD`, `PIX`, `BOLETO`: "Método de pagamento inválido";
    - parcelas que não são inteiro: "Número de parcelas inválido";
    - parcelas menores que 1: "Número de parcelas deve ser no mínimo 1";
    - `PIX`, `BOLETO` ou `DEBIT_CARD` com mais de 1 parcela: "Método de pagamento <MÉTODO> não permite parcelamento";
    - status fora de `APPROVED`, `CANCELLED`, `REFUNDED`, `CHARGEBACK`: "Status inválido";
    - data fora de `yyyy-MM-dd` ou inexistente: "Data da transação inválida. Formato esperado: yyyy-MM-dd";
    - data futura: "Data da transação não pode ser futura";
    - sem regra de taxa ativa para o método e as parcelas: "Regra de taxa ativa não encontrada para <MÉTODO> em <N>x";
    - líquido esperado zero ou negativo com a regra encontrada: "Valor líquido esperado deve ser maior que zero".
11. IF o arquivo não tem erro de linha e alguma referência já está cadastrada como transação da empresa THEN the system SHALL responder `409` `CONFLICT` "Referências externas já cadastradas para este comerciante: <ref1>, <ref2>" com `conflictingReferences` listando todas elas, e não gravar nada.
12. IF o arquivo não tem erro nem conflito e alguma `transactionDate` cai num período travado da empresa THEN the system SHALL responder `409` `CONFLICT` com a mensagem do período travado já usada pelos outros imports ("Janela travada de <de> a <até>") e não gravar nada.
13. The system SHALL aceitar a mesma referência que já existe como transação de outra empresa.

**Independent Test**: Com uma empresa com regras Pix 1x e Crédito 3x, enviar um CSV de 3 linhas e receber `201` com `totalRows` 3; `GET /transactions` lista as 3 com o líquido esperado de cada regra. Reenviar o mesmo arquivo recebe `409` com as 3 referências.

---

### P1: Lote rastreável ⭐ MVP

**User Story**: As an OPERATOR or ADMIN, I want consultar os imports de transações da empresa so that eu saiba quando e com qual arquivo cada lote entrou.

**Why P1**: A tela de Importações mostra o último import de cada tipo; o dashboard já faz isso para liquidação e extrato.

**Acceptance Criteria**:

1. WHEN um import é aceito THEN the system SHALL registrar o lote com o nome original do arquivo e associar cada transação gravada a ele.
2. WHEN `GET /api/merchants/{merchantId}/transactions/imports` é chamado THEN the system SHALL responder `200` com a página de lotes da empresa, mais recente primeiro por padrão, cada um com `id`, `merchantId`, `fileName`, `totalRows` e `createdAt`.
3. WHEN `GET /api/merchants/{merchantId}/transactions/imports/{importId}` é chamado com um lote da empresa THEN the system SHALL responder `200` com o lote.
4. IF o lote não existe ou é de outra empresa THEN the system SHALL responder `404` `NOT_FOUND` "Importação não encontrada com id: <importId>".
5. The system SHALL manter sem lote as transações criadas por `POST /transactions`.

**Independent Test**: Depois de dois imports, `GET /transactions/imports` devolve os dois lotes, o mais novo primeiro; o `id` de um lote de outra empresa responde `404`.

---

## Edge Cases

- IF a empresa não existe ou está desativada THEN the system SHALL responder `404` "Comerciante não encontrado com id: <merchantId>" no import e nas consultas de lote.
- IF o usuário não tem acesso à empresa THEN the system SHALL responder `403` (guarda existente por empresa).
- IF a requisição não tem token THEN the system SHALL responder `401`.
- WHEN o CSV tem linhas em branco entre registros ou no fim THEN the system SHALL ignorá-las sem contar como erro, mantendo a numeração real das linhas nos erros.
- WHEN um campo tem espaços nas pontas (` PIX `, ` 100.00`) THEN the system SHALL aceitá-lo sem os espaços.
- WHEN a mesma referência aparece duas vezes e a primeira também tem outro erro THEN the system SHALL reportar os dois erros (o da primeira linha e a duplicidade na segunda).
- WHEN `transactionDate` é hoje THEN the system SHALL aceitar a linha.

---

## Implicit-Requirement Sweep

| Dimension | Resolution |
| --- | --- |
| Input validation & bounds | Cabeçalho, colunas, tipos, limites de valor, datas e regras de método/parcelas (TIMP-04..TIMP-10); 5 MB (TIMP-08) |
| Failure / partial-failure states | Tudo ou nada: nenhum lote nem transação em qualquer erro (TIMP-09, TIMP-11, TIMP-12) |
| Idempotency / retry / duplicate handling | Reenviar o mesmo arquivo dá `409` com as referências (TIMP-11); duplicidade dentro do arquivo é erro de linha (TIMP-10) |
| Auth boundaries & rate limits | `401`/`403`/`404` nos edge cases, pela segurança existente; rate limit N/A because a API não tem throttle |
| Concurrency / ordering | Dois imports simultâneos com a mesma referência: o índice único `(merchant_id, external_reference)` recusa o segundo, que vira `409` pelo handler de integridade existente |
| Data lifecycle / expiry | Lote e transações permanentes; desfazer lote fora de escopo |
| Observability | O import registra evento de auditoria `TRANSACTIONS_IMPORTED` com empresa e número de linhas, como `SETTLEMENTS_IMPORTED` |
| External-dependency failure | N/A because o import não chama serviço externo |
| State-transition integrity | Status do arquivo gravado como está (fora de escopo validar transição); período travado bloqueia (TIMP-12) |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| TIMP-01 | P1: Importar transações | Execute | Verified |
| TIMP-02 | P1: Importar transações | Execute | Verified |
| TIMP-03 | P1: Importar transações | Execute | Verified |
| TIMP-04 | P1: Importar transações | Execute | Verified |
| TIMP-05 | P1: Importar transações | Execute | Verified |
| TIMP-06 | P1: Importar transações | Execute | Verified |
| TIMP-07 | P1: Importar transações | Execute | Verified |
| TIMP-08 | P1: Importar transações | Execute | Verified |
| TIMP-09 | P1: Importar transações | Execute | Verified |
| TIMP-10 | P1: Importar transações | Execute | Verified |
| TIMP-11 | P1: Importar transações | Execute | Verified |
| TIMP-12 | P1: Importar transações | Execute | Verified |
| TIMP-13 | P1: Importar transações | Execute | Verified |
| TLOTE-01 | P1: Lote rastreável | Execute | Verified |
| TLOTE-02 | P1: Lote rastreável | Execute | Verified |
| TLOTE-03 | P1: Lote rastreável | Execute | Verified |
| TLOTE-04 | P1: Lote rastreável | Execute | Verified |
| TLOTE-05 | P1: Lote rastreável | Execute | Verified |

**ID format:** `TIMP-NN` (import) e `TLOTE-NN` (lote); o número é a posição do critério na story.

**Coverage:** 18 total, 18 mapped to tasks, 0 unmapped; 18 verified

---

## Success Criteria

- [ ] Um CSV de 100 transações de uma empresa com regras configuradas entra numa chamada, com o mesmo líquido esperado que 100 `POST /transactions` dariam.
- [ ] Nenhum arquivo com erro grava transação ou lote.
- [ ] `./mvnw verify` passa com os testes novos e a cobertura do JaCoCo.
