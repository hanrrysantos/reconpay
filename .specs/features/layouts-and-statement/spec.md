# Layouts and Statement Specification

## Problem Statement

A API só aceita um CSV de liquidação com sete colunas fixas. O operador não consegue mandar o arquivo do adquirente com outros nomes de coluna, nem o extrato do banco. A conciliação cruza a venda com o que o adquirente disse que pagou, e para aí. Ela não responde se o dinheiro caiu na conta.

## Goals

- [ ] OPERATOR com grant, e ADMIN, importam a liquidação pela receita `RECONPAY` ou `ACQUIRER`, e o extrato pela receita fixa do banco
- [ ] O run vigente cruza cada liquidação incluída com no máximo uma linha do extrato e registra a divergência quando o par não existe ou não é único

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Indicadores de fechamento e trava de período | Feature seguinte |
| Frontend | Épico seguinte |
| Receita de colunas cadastrada pelo operador | O catálogo desta feature tem duas receitas fixas |
| OFX, CNAB e um segundo adquirente | Um layout de adquirente prova a troca de cabeçalho |
| Depósito único somando várias liquidações | O par desta feature é um para um |
| Colunas novas no CSV de conciliação | O export atual permanece o contrato de auditoria |
| Mudar as regras de venda contra liquidação | O cruzamento atual continua valendo |
| Owner, convite, refresh de JWT, rate limit e fila externa | Fora desta leva |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Receitas de liquidação | `RECONPAY` e `ACQUIRER`, fixas no código | O operador escolhe a receita no envio e não cadastra coluna | y |
| Receita omitida | `RECONPAY` | O CSV atual continua válido sem parâmetro novo | y (agent) |
| Cabeçalho `ACQUIRER` | `nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao` | Os nomes mudam. Os valores de método e status continuam os enums atuais | y (agent) |
| Cabeçalho do extrato | `lineReference,externalReference,amount,movementDate` | A linha precisa de identidade própria porque o código da venda pode faltar | y (agent) |
| Código da venda vazio | Gravado como null | Campo opcional não vira string vazia | y (agent) |
| Linha inválida | HTTP 400, lote e linhas descartados | Mesma regra do CSV atual, confirmada para os dois arquivos | y |
| Duplicidade no arquivo | HTTP 400 com `rowErrors` | O parser atual já trata repetição no arquivo como erro de linha | y (agent) |
| Duplicidade no merchant | HTTP 409 `CONFLICT`, nada gravado | Referência de liquidação e `lineReference` do extrato são únicas por merchant | y (agent) |
| Concorrência de importação | O primeiro persiste. O segundo recebe HTTP 409 | O segundo envio encontra a referência já gravada | y (agent) |
| Quem importa | OPERATOR com grant. ADMIN sem linha em `user_merchants`. Sem token: HTTP 401. Sem grant: HTTP 403 | AD-001 | y |
| Tamanho e tipo | CSV até 5 MB. Acima disso: HTTP 413. Sem arquivo ou sem sufixo `.csv`: HTTP 400 | O limite atual de upload | y (agent) |
| Data | `uuuu-MM-dd`, não posterior a hoje | A liquidação atual já recusa data futura | y (agent) |
| Dinheiro | Positivo, no máximo 17 inteiros e 2 decimais. No adquirente, líquido não passa do bruto | Mesmo teto já aceito na API | y (agent) |
| Janela do extrato | `movementDate` entre `fromDate` e `toDate` mais `settlement-lag-days` | A mesma janela estendida em que a liquidação já entra no run | y (agent) |
| Tolerância | `abs(amount - netAmount)` menor ou igual a `amount-tolerance` | A comparação de valor que o motor já usa | y (agent) |
| Data no casamento por código | Não precisa coincidir | A regra confirmada usa o código. A data entra só quando o código falta | y (agent) |
| Linha ignorada | Código preenchido, nenhuma liquidação do run com esse código, e a transação existe só fora da janela | O run já ignora liquidação cuja venda está fora da janela | y (agent) |
| Sem extrato importado | Cada liquidação do run fica `MISSING_BANK_CREDIT` | Liquidação sem linha do banco ficou divergente | y |
| Venda sem liquidação | Não recebe divergência de banco | Sem liquidação não há líquido para procurar na conta | y (agent) |
| Divergência nova | Nasce `OPEN` e aceita o PATCH de desfecho já existente | É a mesma divergência do run | y (agent) |
| CSV | As colunas atuais permanecem. O nome novo pode aparecer em `discrepancyTypes` | Essa coluna já lista os tipos | y (agent) |
| Expiração | N/A porque linha e lote permanecem, sem exclusão nesta feature | Não há TTL no produto | y |
| Serviço externo | N/A porque a feature só lê arquivo enviado | Não há chamada externa | y |
| Rate limit | N/A porque o limite ficou fora desta leva | Mesma fronteira da feature anterior | y |
| Auditoria | `SETTLEMENTS_IMPORTED` e `BANK_STATEMENTS_IMPORTED` só depois do commit | Sucesso só é auditado com a gravação confirmada | y (agent) |

**Open questions:** none

---

## User Stories

### P1: Importar a liquidação com a receita ⭐ MVP

**User Story**: As an operator, I want to send the acquirer file and name its recipe so that the rows become the same settlements the reconciliation already uses.

**Why P1**: Sem essa tradução, o arquivo do adquirente não entra.

**Acceptance Criteria**:

1. WHEN an authenticated OPERATOR with a grant for merchantId submits POST `/api/merchants/{merchantId}/external-settlements/import` with a `.csv` of at most 5MB, without `layout` or with `layout=RECONPAY`, a header equal to `externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate`, and every data row valid and new for that merchant THEN the system SHALL respond HTTP 201, persist one import batch and one settlement per data row, and emit `SETTLEMENTS_IMPORTED` only after that commit.
2. WHEN that OPERATOR submits the same POST with `layout=ACQUIRER` and header `nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao` THEN the system SHALL persist each data row as a settlement whose externalReference, amount, netAmount, paymentMethod, installments, status, and settlementDate come from those columns in that order.
3. IF `layout` is present and is neither `RECONPAY` nor `ACQUIRER`, or the header does not match the chosen layout THEN the system SHALL respond HTTP 400 `VALIDATION_ERROR` and persist neither the batch nor any row.
4. IF any data row breaks the current settlement value rules, repeats an externalReference inside the file, or the file is empty, missing, or not a `.csv` THEN the system SHALL respond HTTP 400 `VALIDATION_ERROR` and persist nothing. A row error includes `rowErrors`.
5. IF an externalReference in the file already exists for that merchant THEN the system SHALL respond HTTP 409 `CONFLICT` and persist nothing.
6. IF the file is larger than 5MB THEN the system SHALL respond HTTP 413 `VALIDATION_ERROR` and persist nothing.
7. The system SHALL reject an unauthenticated import with HTTP 401, reject an OPERATOR without grant with HTTP 403, and accept an ADMIN with no `user_merchants` row with the same HTTP 201 outcome.

**Independent Test**: Import the current CSV without `layout`, then import an `ACQUIRER` file, and read both as settlements of the same merchant.

---

### P1: Importar o extrato

**User Story**: As an operator, I want to upload the bank CSV so that each movement is stored for the reconciliation.

**Why P1**: Sem o extrato gravado, a conciliação não tem o terceiro lado.

**Acceptance Criteria**:

1. WHEN an authenticated OPERATOR with a grant submits POST `/api/merchants/{merchantId}/bank-statements/import` with a `.csv` of at most 5MB, header `lineReference,externalReference,amount,movementDate`, and every data row valid and new for that merchant THEN the system SHALL respond HTTP 201 with the batch id, file name, and row count, persist one line per data row, and emit `BANK_STATEMENTS_IMPORTED` only after that commit.
2. WHEN a data row has a blank externalReference THEN the system SHALL store that field as null and keep lineReference, amount, and movementDate.
3. WHEN that OPERATOR calls GET `/api/merchants/{merchantId}/bank-statements` THEN the system SHALL return only that merchant's lines, each with id, lineReference, externalReference, amount, movementDate, and import id.
4. IF any data row has a blank or over-100-character lineReference, a repeated lineReference in the file, an externalReference longer than 100 characters, an amount that is not positive or exceeds 17 integer digits or 2 decimal places, or a movementDate that is not a strict `uuuu-MM-dd` on or before today THEN the system SHALL respond HTTP 400 `VALIDATION_ERROR` with `rowErrors` and persist nothing.
5. IF a lineReference in the file already exists for that merchant THEN the system SHALL respond HTTP 409 `CONFLICT` and persist nothing.
6. The system SHALL apply the same authentication, grant, ADMIN, file presence, `.csv` suffix, and 5MB responses as the settlement import.

**Independent Test**: Import a two-line statement, one with a sale code and one without, then list the lines for that merchant only.

---

### P1: Cruzar extrato e liquidação

**User Story**: As an operator, I want the reconciliation run to pair each settlement with the bank line that paid it so that a missing or ambiguous deposit stays visible.

**Why P1**: O extrato importado ainda não responde se o dinheiro caiu.

**Acceptance Criteria**:

1. WHEN a completed run includes a settlement and a bank line in the extended window whose externalReference equals that settlement, and `abs(bank amount - settlement netAmount)` is less than or equal to `amount-tolerance` THEN the system SHALL pair that line with that settlement and add no bank discrepancy for the pair.
2. WHEN that reference matches and the amount difference is greater than `amount-tolerance` THEN the system SHALL add `BANK_AMOUNT_MISMATCH` on that item, with expectedValue equal to the settlement netAmount and actualValue equal to the bank amount, and SHALL not also add `MISSING_BANK_CREDIT`.
3. WHEN a bank line has a null externalReference, its movementDate equals one settlement's settlementDate, the amount is within `amount-tolerance` of that settlement netAmount, and no other unpaired line or settlement shares that eligibility THEN the system SHALL pair them and add no bank discrepancy for the pair.
4. IF a settlement included in the run has no paired bank line and is not a `BANK_AMOUNT_MISMATCH` THEN the system SHALL add `MISSING_BANK_CREDIT` when zero lines are eligible, or `AMBIGUOUS_BANK_MATCH` when more than one line is eligible, and SHALL not pair any of those lines.
5. IF a bank line in the extended window has no paired settlement THEN the system SHALL add `ORPHAN_BANK_CREDIT` when zero settlements are eligible, or `AMBIGUOUS_BANK_MATCH` when more than one settlement is eligible.
6. The system SHALL ignore a bank line whose externalReference matches no settlement in the run and matches a transaction only outside the run's transaction window.
7. The system SHALL leave a transaction that has no settlement free of `MISSING_BANK_CREDIT`, `ORPHAN_BANK_CREDIT`, `BANK_AMOUNT_MISMATCH`, and `AMBIGUOUS_BANK_MATCH`.
8. The system SHALL keep computing the existing sale-versus-settlement discrepancy types from internal transactions and external settlements alone.
9. The system SHALL set the item result to `MATCHED` only when that item has no discrepancy, and to `DIVERGENT` when it has any discrepancy, including a bank discrepancy.
10. The system SHALL store each new bank discrepancy as `OPEN`.
11. The system SHALL keep the reconciliation CSV header unchanged.

**Independent Test**: Run a window with one exact pair, one amount mismatch, one settlement without a bank line, and one bank line without a settlement. Read the four outcomes on the completed run.

---

## Edge Cases

- IF two bank lines are eligible for one settlement, or one bank line is eligible for two settlements THEN the system SHALL pair none of them and mark each involved side `AMBIGUOUS_BANK_MATCH`.
- IF two imports submit the same new lineReference or externalReference at the same time THEN the system SHALL persist one and respond HTTP 409 to the other.
- WHEN a merchant has imported no bank line THEN the system SHALL mark every settlement included in the run with `MISSING_BANK_CREDIT`.
- IF a bank line belongs to another merchant THEN the system SHALL not use it as a candidate in this run.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| LAY-01 | P1: Importar a liquidação com a receita | Execute | Verified |
| LAY-02 | P1: Importar a liquidação com a receita | Execute | Verified |
| LAY-03 | P1: Importar a liquidação com a receita | Execute | Verified |
| LAY-04 | P1: Importar a liquidação com a receita | Execute | Verified |
| LAY-05 | P1: Importar a liquidação com a receita | Execute | Verified |
| LAY-06 | P1: Importar a liquidação com a receita | Execute | Verified |
| LAY-07 | P1: Importar a liquidação com a receita | Execute | Verified |
| LAY-08 | P1: Importar o extrato | Execute | Verified |
| LAY-09 | P1: Importar o extrato | Execute | Verified |
| LAY-10 | P1: Importar o extrato | Execute | Verified |
| LAY-11 | P1: Importar o extrato | Execute | Verified |
| LAY-12 | P1: Importar o extrato | Execute | Verified |
| LAY-13 | P1: Importar o extrato | Execute | Verified |
| LAY-14 | P1: Cruzar extrato e liquidação | Execute | Verified |
| LAY-15 | P1: Cruzar extrato e liquidação | Execute | Verified |
| LAY-16 | P1: Cruzar extrato e liquidação | Execute | Verified |
| LAY-17 | P1: Cruzar extrato e liquidação | Execute | Verified |
| LAY-18 | P1: Cruzar extrato e liquidação | Execute | Verified |
| LAY-19 | P1: Cruzar extrato e liquidação | Execute | Verified |
| LAY-20 | P1: Cruzar extrato e liquidação | Execute | Verified |
| LAY-21 | P1: Cruzar extrato e liquidação | Execute | Verified |
| LAY-22 | P1: Cruzar extrato e liquidação | Execute | Verified |
| LAY-23 | P1: Cruzar extrato e liquidação | Execute | Verified |
| LAY-24 | P1: Cruzar extrato e liquidação | Execute | Verified |
| LAY-25 | P1: Cruzar extrato e liquidação | Execute | Verified |
| LAY-26 | P1: Cruzar extrato e liquidação | Execute | Verified |
| LAY-27 | P1: Cruzar extrato e liquidação | Execute | Verified |
| LAY-28 | P1: Cruzar extrato e liquidação | Execute | Verified |

**Coverage:** 28 total, 28 mapped to tasks, 0 unmapped

---

## Success Criteria

- [ ] O CSV atual importa sem `layout` e o arquivo `ACQUIRER` vira liquidação com as mesmas colunas semânticas
- [ ] O extrato importa isolado por merchant, com código da venda opcional
- [ ] Um run concluído mostra par exato, valor divergente, liquidação sem depósito e depósito sem liquidação
- [ ] Arquivo inválido, duplicidade e falta de grant não gravam linha parcial
