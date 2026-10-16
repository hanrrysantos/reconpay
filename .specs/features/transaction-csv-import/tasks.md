# Import CSV de Transações Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/transaction-csv-import/design.md`
**Status**: Approved

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `AGENTS.md` (JUnit 5, Mockito, MockMvc, AssertJ, Testcontainers `postgres:16-alpine`; integração herda `AbstractIntegrationTest`; JaCoCo 85% linha / 75% branch no `verify`; não enfraquecer testes), `backend/pom.xml`, `.github/workflows/ci.yml`. Existing samples: `SettlementCsvParserTest`, `ExternalSettlementServiceTest`, `ExternalSettlementIntegrationTest`. Lições candidatas aplicadas por cautela: L-016 (textos exatos nos ACs, já na spec), L-018 (cada valor permitido testado), L-017/L-019 (limites testados em cada forma).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Domain / util (`ExpectedNetAmountCalculator`) | unit | Fórmula e arredondamento com valores fixos; líquido 0 e negativo lançam | `backend/src/test/java/**/feerule/service/ExpectedNetAmountCalculatorTest.java` | `cd backend && ./mvnw -q test -Dtest=ExpectedNetAmountCalculatorTest` |
| Parser (`TransactionCsvParser`) | unit | Cada erro de TIMP-10 com o texto exato, cabeçalho, sem registros, linhas em branco, numeração, espaços, data de hoje, cada método e status aceitos | `backend/src/test/java/**/transaction/service/TransactionCsvParserTest.java` | `cd backend && ./mvnw -q test -Dtest=TransactionCsvParserTest` |
| Service (`TransactionImportService`) | unit | Todos os ramos: arquivo, erros somados e ordenados, regra ausente, líquido ≤ 0, duplicadas na ordem do arquivo, período, gravação e auditoria, consultas de lote | `backend/src/test/java/**/transaction/service/TransactionImportServiceTest.java` | `cd backend && ./mvnw -q test -Dtest=TransactionImportServiceTest` |
| Controller + repositório + migration | integration | Cada rota: sucesso, `400` (arquivo, cabeçalho, `rowErrors`), `409` (duplicadas, período), `413`, `404`, `403`; nada gravado nos erros; líquido igual ao `POST` | `backend/src/test/java/**/transaction/integration/TransactionImportIntegrationTest.java` | `cd backend && ./mvnw verify` |
| Entity / migration | none (coberto pela integração da task que os usa) | - | - | build gate |
| Docs (README) | none | - (build gate only) | - | build gate only |

## Gate Check Commands

> Generated from codebase - confirm before Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Desenvolvimento de uma task (teste da classe) | `cd backend && ./mvnw -q test -Dtest=<Classe>Test` |
| Full | Fim de cada task de backend (unit + integração + JaCoCo) | `cd backend && ./mvnw verify` |
| Build | Task de docs | `cd backend && ./mvnw verify` |

---

## Execution Plan

### Phase 1: Base

```
T1 → T2 → T3
```

### Phase 2: Import e lote

```
T3 → T4 → T5 → T6
```

---

## Task Breakdown

### T1: ExpectedNetAmountCalculator

**What**: Extrair o cálculo do líquido esperado de `TransactionService` para `ExpectedNetAmountCalculator`, com `TransactionService.create` delegando, sem mudar fórmula, arredondamento nem mensagem.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/feerule/service/ExpectedNetAmountCalculator.java`
**Depends on**: None
**Reuses**: `TransactionService.calculateExpectedNetAmount`
**Requirement**: TIMP-03

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Teste unitário: `100.00` com `2.9900`% e `0.30` → `96.71`; `10.00` com `3.3333`% e `0.00` → `9.67`; arredondamento `HALF_UP` num caso de meio (`.005`); líquido `0.00` e negativo lançam `InvalidTransactionAmountException` "Valor líquido esperado deve ser maior que zero"
- [x] Testes existentes de `TransactionService` e `TransactionIntegrationTest` passam sem alteração
- [x] Gate check passes: `cd backend && ./mvnw verify`

**Tests**: unit
**Gate**: full

**Commit**: `refactor(transaction): extract expected net amount calculator`

---

### T2: Lote no banco

**What**: Migration `V24__create_transaction_imports.sql`, `TransactionImportEntity`, `ITransactionImportRepository`, `TransactionImportResponseDTO`, mapper e campo `importBatch` (nulo) em `InternalTransactionEntity`.
**Where**: `backend/src/main/resources/db/migration/V24__create_transaction_imports.sql`
**Depends on**: T1
**Reuses**: `V21__create_bank_statement_tables.sql`, `BankStatementImportEntity`
**Requirement**: TLOTE-01, TLOTE-05

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Teste de integração do repositório: salvar lote e transação ligada a ele; `findAllByMerchant_Id` e `findByIdAndMerchant_Id` filtram pela empresa
- [ ] Transação criada por `POST /transactions` continua com `import_id` nulo (TLOTE-05)
- [ ] Gate check passes: `cd backend && ./mvnw verify`

**Tests**: integration
**Gate**: full

**Commit**: `feat(transaction): add transaction import batches`

---

### T3: TransactionCsvParser

**What**: Parser do layout `externalReference,amount,paymentMethod,installments,status,transactionDate`, devolvendo linhas tipadas e erros de linha, e lançando `InvalidTransactionImportException` para erros de arquivo.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/transaction/service/TransactionCsvParser.java`
**Depends on**: T2
**Reuses**: `SettlementCsvParser`, `ImportRowErrorDTO`, `PaymentMethodRules`
**Requirement**: TIMP-04, TIMP-07, TIMP-09, TIMP-10

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Um teste por erro de formato de TIMP-10 com o texto exato e o número da linha (cabeçalho = 1)
- [ ] Cabeçalho errado e só cabeçalho com as mensagens de TIMP-04 e TIMP-07
- [ ] Cada método (`CREDIT_CARD` 1x e 12x, `DEBIT_CARD`, `PIX`, `BOLETO` 1x) e cada status aceitos; data de hoje aceita; espaços nas pontas removidos; linhas em branco puladas com numeração real
- [ ] Duplicidade no arquivo marcada da segunda ocorrência em diante, mesmo com erro na primeira
- [ ] Gate check passes: `cd backend && ./mvnw verify`

**Tests**: unit
**Gate**: full

**Commit**: `feat(transaction): parse transaction CSV rows`

---

### T4: TransactionImportService e exceções

**What**: `TransactionImportService` (importCsv tudo-ou-nada, findAllImports, findImportById), as quatro exceções novas e os handlers que as mapeiam para `400`/`409`/`404` com os mesmos corpos da liquidação.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/transaction/service/TransactionImportService.java`
**Depends on**: T3
**Reuses**: `ExternalSettlementService`, `ExpectedNetAmountCalculator`, `PeriodGuard`, `AuditLogger`, `IFeeRuleRepository`
**Requirement**: TIMP-01, TIMP-02, TIMP-03, TIMP-05, TIMP-06, TIMP-09, TIMP-10, TIMP-11, TIMP-12, TLOTE-01, TLOTE-02, TLOTE-03, TLOTE-04

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Unit: erros do parser e de regra/líquido somados e ordenados por linha num único `400`; regra ausente com "Regra de taxa ativa não encontrada para <MÉTODO> em <N>x"; regras ativas carregadas numa só consulta
- [ ] Unit: duplicadas → `409` com referências na ordem do arquivo; período travado → `PeriodConflictException`; nada salvo em nenhum erro (`save`/`saveAll` nunca chamados)
- [ ] Unit: sucesso salva o lote com `fileName` e `totalRows`, as transações com os campos do arquivo, o líquido do calculador e o lote, e registra `TRANSACTIONS_IMPORTED`
- [ ] Unit: arquivo vazio e não `.csv` com as mensagens exatas; lote de outra empresa → `TransactionImportNotFoundException`
- [ ] Gate check passes: `cd backend && ./mvnw verify`

**Tests**: unit
**Gate**: full

**Commit**: `feat(transaction): import transactions from CSV all or nothing`

---

### T5: Endpoints de import e lotes

**What**: `POST /transactions/import`, `GET /transactions/imports`, `GET /transactions/imports/{importId}` no `TransactionController`, documentados em `TransactionControllerApi` (AD-006).
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/transaction/controller/TransactionController.java`
**Depends on**: T4
**Reuses**: endpoints de import de `ExternalSettlementControllerApi`
**Requirement**: TIMP-01, TIMP-02, TIMP-03, TIMP-04, TIMP-05, TIMP-06, TIMP-07, TIMP-08, TIMP-09, TIMP-10, TIMP-11, TIMP-12, TIMP-13, TLOTE-02, TLOTE-03, TLOTE-04, TLOTE-05

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Integração: `201` com o lote; `GET /transactions` mostra as linhas com o líquido igual ao de um `POST /transactions` com os mesmos dados
- [ ] Integração: `400` com `rowErrors` exatos num arquivo com erros de formato e de regra em linhas diferentes; nenhum lote nem transação gravados
- [ ] Integração: reenviar o mesmo arquivo → `409` com `conflictingReferences`; período travado → `409` "Janela travada de …"; arquivo > 5 MB → `413`; sem campo `file` → `400` "Requisição inválida. Verifique os campos e parâmetros enviados."
- [ ] Integração: mesma referência em outra empresa aceita (TIMP-13); lotes listados do mais novo para o mais antigo; lote de outra empresa → `404`; OPERATOR sem acesso → `403`
- [ ] `ApiDocsIntegrationTest` passa com os endpoints novos documentados
- [ ] Gate check passes: `cd backend && ./mvnw verify`

**Tests**: integration
**Gate**: full

**Commit**: `feat(transaction): expose transaction CSV import endpoints`

---

### T6: Documentação

**What**: README raiz: regra de negócio "Transações internas" com o import CSV (layout, tudo ou nada, `409` de referências, lote) e a linha dos endpoints.
**Where**: `README.md`
**Depends on**: T5
**Reuses**: seção de liquidações externas
**Requirement**: TIMP-04, TLOTE-02

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] README descreve o comportamento entregue
- [ ] Gate check passes: `cd backend && ./mvnw verify`

**Tests**: none
**Gate**: build

**Commit**: `docs: describe transaction CSV import`

---

## Phase Execution Map

```
Phase 1 → Phase 2

Phase 1:  T1 → T2 → T3
Phase 2:  T4 → T5 → T6
```

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: Calculadora | 1 classe + delegação | ✅ Granular |
| T2: Lote no banco | migration + entidade + repositório + DTO/mapper | ⚠️ OK (um conceito: o lote persistido) |
| T3: Parser | 1 classe | ✅ Granular |
| T4: Service e exceções | 1 service + exceções + handler | ⚠️ OK (exceções sem uso fora do service) |
| T5: Endpoints | 3 endpoints do mesmo recurso | ⚠️ OK (coesos) |
| T6: Docs | 1 README | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | início | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 (T3 na Phase 1) | ✅ Match |
| T5 | T4 | T4 → T5 | ✅ Match |
| T6 | T5 | T5 → T6 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | Domain util | unit | unit | ✅ OK |
| T2 | Entity/migration + repositório | integration (maior) | integration | ✅ OK |
| T3 | Parser | unit | unit | ✅ OK |
| T4 | Service + exceções | unit | unit | ✅ OK |
| T5 | Controller | integration | integration | ✅ OK |
| T6 | Docs | none | none | ✅ OK |
