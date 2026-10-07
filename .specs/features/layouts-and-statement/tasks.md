# Layouts and Statement Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/layouts-and-statement/design.md`
**Status**: In Progress

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `AGENTS.md`, `README.md` (Testes), `pom.xml` (JaCoCo 85% linha / 75% ramo no `verify`), `.github/workflows/ci.yml`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Parser / service / matcher | unit | Todos os ramos; 1:1 com os ACs da camada; cada borda da spec tem teste | `src/test/java/**/service/*Test.java` | `./mvnw -B test -Dtest=ClassName` |
| Controller / run | integration | Rotas da tarefa: sucesso, borda e erro. Testcontainers | `src/test/java/**/*IntegrationTest.java` | `./mvnw -B verify` |
| Repository | integration | Consulta da janela e isolamento por merchant, cobertos no teste do run | `src/test/java/**/*IntegrationTest.java` | `./mvnw -B verify` |
| Enum / Flyway / entity / DTO | none | Gate de compilação. JaCoCo exclui `entity` e `dto` | — | `./mvnw -B -DskipTests compile` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Tarefa só com teste unitário | `./mvnw -B test -Dtest=ClassName` |
| Full | Integração, ou fim de fase com código de serviço | `./mvnw -B verify` |
| Build | Enum, migration, entidade ou DTO sem regra | `./mvnw -B -DskipTests compile` |

---

## Execution Plan

Fases em ordem. Dentro da fase, uma tarefa por vez.

### Phase 1: Layout

```
T1 → T2 → T3 → T4
```

### Phase 2: Statement

```
T5 → T6
T5 → T7
T6 → T9
T7 → T9
T8 → T10
T9 → T10
T10 → T11
```

### Phase 3: Pairing

```
T12 → T13 → T14
```

---

## Task Breakdown

### Phase 1: Layout

### T1: Add SettlementLayout

**What**: Enum `RECONPAY` e `ACQUIRER`, cada um com o cabeçalho fixo do design.
**Where**: `src/main/java/br/com/hanrry/reconpay/externalsettlement/enums/SettlementLayout.java`
**Depends on**: None
**Reuses**: `DiscrepancyType.java`
**Requirement**: LAY-02

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] `RECONPAY` expõe `externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate`
- [x] `ACQUIRER` expõe `nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao`
- [x] Gate: `./mvnw -B -DskipTests compile`
- [x] Nenhum teste removido

**Tests**: none
**Gate**: build

**Commit**: `feat(externalsettlement): add settlement layout enum`

---

### T2: Parse both settlement layouts

**What**: `SettlementCsvParser.parse` recebe a receita, cobra o cabeçalho dela e segue validando a linha pelos mesmos índices.
**Where**: `src/main/java/br/com/hanrry/reconpay/externalsettlement/service/SettlementCsvParser.java`
**Depends on**: T1
**Reuses**: `validateRow` já existente
**Requirement**: LAY-02, LAY-03, LAY-04

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] Cabeçalho `RECONPAY` válido continua produzindo `ParsedSettlementRow`
- [x] Cabeçalho `ACQUIRER` válido mapeia `nsu` para `externalReference` e `valor_liquido` para `netAmount`, na ordem do design
- [x] Cabeçalho trocado responde erro de cabeçalho e não devolve linha
- [x] Linha inválida do `ACQUIRER` usa as mesmas regras de valor, enum, data e duplicidade no arquivo
- [x] Gate: `./mvnw -B test -Dtest=SettlementCsvParserTest`
- [x] Nenhum teste removido

**Tests**: unit
**Gate**: quick

**Commit**: `feat(externalsettlement): parse acquirer settlement layout`

---

### T3: Pass layout through settlement import

**What**: O serviço usa `RECONPAY` quando a receita vem nula, e não grava lote nem linha quando o parser ou a duplicidade falham.
**Where**: `src/main/java/br/com/hanrry/reconpay/externalsettlement/service/ExternalSettlementService.java`
**Depends on**: T2
**Reuses**: `importCsv` atual e `AuditLogger`
**Requirement**: LAY-01, LAY-05, LAY-06

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] Receita nula chama o parser com `RECONPAY`
- [x] `ACQUIRER` chama o parser com `ACQUIRER` e grava as liquidações mapeadas
- [x] Erro de linha ou referência já existente não chama `save` do lote
- [x] `SETTLEMENTS_IMPORTED` só é pedido depois do `save`
- [x] Gate: `./mvnw -B test -Dtest=ExternalSettlementServiceTest`
- [x] Nenhum teste removido

**Tests**: unit
**Gate**: quick

**Commit**: `feat(externalsettlement): accept layout on settlement import`

---

### T4: Expose layout on the settlement import

**What**: O POST de liquidação aceita `layout` opcional e cobre o contrato HTTP da receita.
**Where**: `src/main/java/br/com/hanrry/reconpay/externalsettlement/controller/ExternalSettlementController.java`
**Depends on**: T3
**Reuses**: `ExternalSettlementIntegrationTest.java`
**Requirement**: LAY-01, LAY-03, LAY-04, LAY-05, LAY-06, LAY-07

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] Sem `layout`, o CSV atual responde HTTP 201
- [x] `layout=ACQUIRER` com o cabeçalho novo responde HTTP 201 e a liquidação lida tem os campos mapeados
- [x] `layout` desconhecido ou cabeçalho errado responde HTTP 400 e não grava linha
- [x] Linha inválida responde HTTP 400 com `rowErrors` e não grava linha
- [x] Referência já existente responde HTTP 409 e não grava o lote novo
- [x] OPERATOR sem grant recebe HTTP 403. ADMIN sem grant recebe HTTP 201
- [x] Gate: `./mvnw -B verify`
- [x] Nenhum teste removido ou enfraquecido

**Tests**: integration
**Gate**: full

**Commit**: `feat(externalsettlement): expose settlement layout on import`

---

### Phase 2: Statement

### T5: Add bank statement tables

**What**: Migration `V21` com lote, linha, índice único `(merchant_id, line_reference)` e índice `(merchant_id, movement_date)`.
**Where**: `src/main/resources/db/migration/V21__create_bank_statement_tables.sql`
**Depends on**: None
**Reuses**: `V8__create_external_settlements_tables.sql`
**Requirement**: LAY-08

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] `amount > 0`, `total_rows > 0` e `external_reference` nulo são permitidos
- [x] Gate: `./mvnw -B -DskipTests compile`
- [x] Nenhum teste removido

**Tests**: none
**Gate**: build

**Commit**: `feat(bankstatement): add bank statement tables`

---

### T6: Add bank statement import entity

**What**: Entidade do lote: merchant, nome do arquivo, total de linhas e criação.
**Where**: `src/main/java/br/com/hanrry/reconpay/bankstatement/entity/BankStatementImportEntity.java`
**Depends on**: T5
**Reuses**: `SettlementImportEntity.java`
**Requirement**: LAY-08

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] Campos mapeados em `bank_statement_imports`
- [x] Gate: `./mvnw -B -DskipTests compile`
- [x] Nenhum teste removido

**Tests**: none
**Gate**: build

**Commit**: `feat(bankstatement): add bank statement import entity`

---

### T7: Add bank statement line entity

**What**: Entidade da linha: `lineReference`, `externalReference` opcional, valor e data.
**Where**: `src/main/java/br/com/hanrry/reconpay/bankstatement/entity/BankStatementLineEntity.java`
**Depends on**: T5
**Reuses**: `ExternalSettlementEntity.java`
**Requirement**: LAY-08, LAY-09

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] Campos mapeados em `bank_statement_lines`
- [ ] Gate: `./mvnw -B -DskipTests compile`
- [ ] Nenhum teste removido

**Tests**: none
**Gate**: build

**Commit**: `feat(bankstatement): add bank statement line entity`

---

### T8: Parse the bank statement file

**What**: O parser cobra o cabeçalho `lineReference,externalReference,amount,movementDate` e rejeita a linha inválida sem gravar.
**Where**: `src/main/java/br/com/hanrry/reconpay/bankstatement/service/BankStatementCsvParser.java`
**Depends on**: None
**Reuses**: `SettlementCsvParser.java`
**Requirement**: LAY-09, LAY-11

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] Código da venda em branco vira null
- [ ] `lineReference` vazio, longo demais ou repetido no arquivo entra em `rowErrors`
- [ ] Valor não positivo, com mais de 17 inteiros ou mais de 2 decimais, entra em `rowErrors`
- [ ] Data inválida ou futura entra em `rowErrors`
- [ ] Cabeçalho errado ou arquivo sem linha de dado falha sem devolver linha
- [ ] Gate: `./mvnw -B test -Dtest=BankStatementCsvParserTest`
- [ ] Nenhum teste removido

**Tests**: unit
**Gate**: quick

**Commit**: `feat(bankstatement): parse bank statement csv`

---

### T9: Add bank statement repositories

**What**: Repositório da linha, com busca por merchant e data, e por `lineReference`.
**Where**: `src/main/java/br/com/hanrry/reconpay/bankstatement/repository/IBankStatementLineRepository.java`
**Depends on**: T6, T7
**Reuses**: `IExternalSettlementRepository.java`
**Requirement**: LAY-10

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] Uma linha gravada é achada pelo merchant e pelo intervalo de `movementDate`
- [ ] A linha de outro merchant não volta nessa busca
- [ ] `lineReference` já gravada é achada entre as referências do merchant
- [ ] Gate: `./mvnw -B verify`
- [ ] Nenhum teste removido

**Tests**: integration
**Gate**: full

**Commit**: `feat(bankstatement): add bank statement repositories`

---

### T10: Import and list bank statement lines

**What**: O serviço grava lote e linhas na mesma transação, recusa duplicidade e lista só o merchant. O repositório do lote nasce aqui porque só o serviço grava o lote.
**Where**: `src/main/java/br/com/hanrry/reconpay/bankstatement/service/BankStatementService.java`
**Depends on**: T8, T9
**Reuses**: `ExternalSettlementService.java`
**Requirement**: LAY-08, LAY-09, LAY-10, LAY-12

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] Arquivo válido pede `save` do lote e das linhas, e `BANK_STATEMENTS_IMPORTED` depois do save
- [ ] Erro de linha ou `lineReference` já existente não pede `save`
- [ ] A listagem devolve só as linhas do merchant, com id, `lineReference`, `externalReference`, valor, data e id do lote
- [ ] Gate: `./mvnw -B test -Dtest=BankStatementServiceTest`
- [ ] Nenhum teste removido

**Tests**: unit
**Gate**: quick

**Commit**: `feat(bankstatement): import and list bank statement lines`

---

### T11: Expose the bank statement import

**What**: POST e GET do extrato, com o mesmo acesso por merchant da liquidação.
**Where**: `src/main/java/br/com/hanrry/reconpay/bankstatement/controller/BankStatementController.java`
**Depends on**: T10
**Reuses**: `ExternalSettlementController.java`, `AbstractIntegrationTest.java`
**Requirement**: LAY-08, LAY-10, LAY-11, LAY-12, LAY-13, LAY-26

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] POST válido responde HTTP 201 com id do lote, nome do arquivo e quantidade de linhas
- [ ] GET do merchant não devolve linha de outro merchant
- [ ] Linha inválida responde HTTP 400 com `rowErrors` e o GET não mostra essa linha
- [ ] Segunda importação da mesma `lineReference` responde HTTP 409 e a primeira permanece
- [ ] Sem token: HTTP 401. OPERATOR sem grant: HTTP 403. ADMIN sem grant: HTTP 201
- [ ] `AGENTS.md` lista o módulo `bankstatement`
- [ ] Gate: `./mvnw -B verify`
- [ ] Nenhum teste removido ou enfraquecido

**Tests**: integration
**Gate**: full

**Commit**: `feat(bankstatement): expose bank statement import`

---

### Phase 3: Pairing

### T12: Add bank discrepancy types

**What**: O enum ganha `BANK_AMOUNT_MISMATCH`, `MISSING_BANK_CREDIT`, `ORPHAN_BANK_CREDIT` e `AMBIGUOUS_BANK_MATCH`.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/enums/DiscrepancyType.java`
**Depends on**: None
**Reuses**: os sete tipos já existentes
**Requirement**: LAY-15, LAY-17, LAY-18

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] Os quatro nomes existem e cabem em `VARCHAR(50)`
- [ ] Gate: `./mvnw -B -DskipTests compile`
- [ ] Nenhum teste removido

**Tests**: none
**Gate**: build

**Commit**: `feat(reconciliation): add bank discrepancy types`

---

### T13: Pair bank lines with settlements

**What**: `BankStatementMatcher` aplica as duas passagens do design e devolve a lista de itens com a divergência de banco.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/service/BankStatementMatcher.java`
**Depends on**: T7, T12
**Reuses**: `ReconciliationEngine.java`, `ReconciliationItemEntity.addDiscrepancy`
**Requirement**: LAY-14, LAY-15, LAY-16, LAY-17, LAY-18, LAY-19, LAY-20, LAY-21, LAY-22, LAY-23, LAY-25, LAY-27

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] Código e valor dentro da tolerância formam par sem divergência de banco
- [ ] Código com valor fora da tolerância grava `BANK_AMOUNT_MISMATCH` e não grava `MISSING_BANK_CREDIT`
- [ ] Sem código, uma só liquidação com a mesma data e valor dentro da tolerância forma par
- [ ] Zero linhas elegíveis grava `MISSING_BANK_CREDIT`. Mais de uma, ou linha compartilhada, grava `AMBIGUOUS_BANK_MATCH` e não forma par
- [ ] Linha sem par grava `ORPHAN_BANK_CREDIT` ou `AMBIGUOUS_BANK_MATCH` num item cuja referência é `lineReference`
- [ ] Linha cujo código só existe fora da janela não vira item
- [ ] Item sem liquidação não recebe tipo de banco
- [ ] Os tipos de venda contra liquidação permanecem os que o motor já colocou
- [ ] Item sem divergência fica `MATCHED`. Com divergência de banco fica `DIVERGENT`. Status da divergência nova é `OPEN`
- [ ] Sem nenhuma linha, toda liquidação fica `MISSING_BANK_CREDIT`
- [ ] `lineReference` igual à referência de uma venda sem liquidação não cria outro item e não marca essa venda
- [ ] Valores gravados com escala 2 e `toPlainString`
- [ ] Gate: `./mvnw -B test -Dtest=BankStatementMatcherTest`
- [ ] Nenhum teste removido

**Tests**: unit
**Gate**: quick

**Commit**: `feat(reconciliation): pair bank lines with settlements`

---

### T14: Apply bank pairing in the reconciliation run

**What**: O processor carrega as linhas do merchant na janela estendida, chama o matcher antes de persistir e a contagem usa o resultado novo.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/service/ReconciliationRunProcessor.java`
**Depends on**: T9, T13
**Reuses**: `loadSettlementsInScope`, `ReconciliationCsvExporter.java`
**Requirement**: LAY-14, LAY-19, LAY-22, LAY-24, LAY-27, LAY-28

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] Um run com par exato, valor divergente, liquidação sem depósito e depósito sem liquidação mostra os quatro desfechos
- [ ] Linha de outro merchant não entra no run
- [ ] Run sem extrato marca cada liquidação incluída com `MISSING_BANK_CREDIT`
- [ ] O cabeçalho do CSV de conciliação permanece o atual. O tipo novo pode aparecer em `discrepancyTypes`
- [ ] Testes de run que esperavam `MATCHED` sem extrato passam a esperar o desfecho da spec
- [ ] Gate: `./mvnw -B verify`
- [ ] Nenhum teste removido ou enfraquecido

**Tests**: integration
**Gate**: full

**Commit**: `feat(reconciliation): apply bank pairing in the reconciliation run`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3

Phase 1:  T1 → T2 → T3 → T4
Phase 2:  T5 → T6
Phase 2:  T5 → T7
Phase 2:  T6 → T9
Phase 2:  T7 → T9
Phase 2:  T8 → T10
Phase 2:  T9 → T10
Phase 2:  T10 → T11
Phase 3:  T12 → T13 → T14
```

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: Add SettlementLayout | 1 enum | ✅ Granular |
| T2: Parse both settlement layouts | 1 parser | ✅ Granular |
| T3: Pass layout through settlement import | 1 service | ✅ Granular |
| T4: Expose layout on the settlement import | 1 endpoint | ✅ Granular |
| T5: Add bank statement tables | 1 migration | ✅ Granular |
| T6: Add bank statement import entity | 1 entity | ✅ Granular |
| T7: Add bank statement line entity | 1 entity | ✅ Granular |
| T8: Parse the bank statement file | 1 parser | ✅ Granular |
| T9: Add bank statement repositories | 1 repository | ✅ Granular |
| T10: Import and list bank statement lines | 1 service | ✅ Granular |
| T11: Expose the bank statement import | 1 endpoint | ✅ Granular |
| T12: Add bank discrepancy types | 1 enum | ✅ Granular |
| T13: Pair bank lines with settlements | 1 matcher | ✅ Granular |
| T14: Apply bank pairing in the reconciliation run | 1 processor | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | nenhuma seta de entrada | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | None | nenhuma seta de entrada | ✅ Match |
| T6 | T5 | T5 → T6 | ✅ Match |
| T7 | T5 | T5 → T7 | ✅ Match |
| T8 | None | nenhuma seta de entrada | ✅ Match |
| T9 | T6, T7 | T6 → T9 e T7 → T9 | ✅ Match |
| T10 | T8, T9 | T8 → T10 e T9 → T10 | ✅ Match |
| T11 | T10 | T10 → T11 | ✅ Match |
| T12 | None | nenhuma seta de entrada na fase 3 | ✅ Match |
| T13 | T7, T12 | T12 → T13. T7 é fase anterior | ✅ Match |
| T14 | T9, T13 | T13 → T14. T9 é fase anterior | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1: Add SettlementLayout | Enum | none | none | ✅ OK |
| T2: Parse both settlement layouts | Parser | unit | unit | ✅ OK |
| T3: Pass layout through settlement import | Service | unit | unit | ✅ OK |
| T4: Expose layout on the settlement import | Controller | integration | integration | ✅ OK |
| T5: Add bank statement tables | Flyway | none | none | ✅ OK |
| T6: Add bank statement import entity | Entity | none | none | ✅ OK |
| T7: Add bank statement line entity | Entity | none | none | ✅ OK |
| T8: Parse the bank statement file | Parser | unit | unit | ✅ OK |
| T9: Add bank statement repositories | Repository | integration | integration | ✅ OK |
| T10: Import and list bank statement lines | Service | unit | unit | ✅ OK |
| T11: Expose the bank statement import | Controller | integration | integration | ✅ OK |
| T12: Add bank discrepancy types | Enum | none | none | ✅ OK |
| T13: Pair bank lines with settlements | Matcher | unit | unit | ✅ OK |
| T14: Apply bank pairing in the reconciliation run | Run | integration | integration | ✅ OK |
