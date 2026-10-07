# Period Close Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/period-close/design.md`
**Status**: Approved

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `AGENTS.md`, `README.md` (Testes), `pom.xml` (JaCoCo 85% linha / 75% ramo no `verify`), `.github/workflows/ci.yml` (`./mvnw -B verify`).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Calculator / guard / service | unit | Todos os ramos; 1:1 com os ACs da camada; cada borda da spec coberta pela tarefa | `src/test/java/**/service/*Test.java` | `./mvnw -B test -Dtest=ClassName` |
| Controller / HTTP / concorrência | integration | Rotas da tarefa: sucesso, borda e erro. Testcontainers | `src/test/java/**/*IntegrationTest.java` | `./mvnw -B verify` |
| Repository | integration | Janela única, merchant ativo e lista de travas | `src/test/java/**/*IntegrationTest.java` | `./mvnw -B verify` |
| Handler de erro | unit | `PeriodConflictException` vira 409 e `PeriodNotFoundException` vira 404 | `src/test/java/**/exception/**/*Test.java` | `./mvnw -B test -Dtest=ClassName` |
| OpenAPI | integration | As três operações entram no `/v3/api-docs` com código, texto em português e exemplo | `src/test/java/**/openapi/*Test.java` | `./mvnw -B verify` |
| Flyway / entity / DTO / exceção sem ramo | none | Gate de compilação | — | `./mvnw -B -DskipTests compile` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Tarefa só com teste unitário | `./mvnw -B test -Dtest=ClassName` |
| Full | Integração, OpenAPI ou repositório | `./mvnw -B verify` |
| Build | Migration, entidade, DTO ou exceção sem ramo | `./mvnw -B -DskipTests compile` |

---

## Execution Plan

Fases em ordem. Dentro da fase, uma tarefa por vez.

### Phase 1: Schema

```
T1 → T3
T2 → T3
```

### Phase 2: Rules

```
T4 → T6
T5 → T6
T4 → T9
T8 → T9
T7
```

### Phase 3: API

```
T10 → T11 → T18
```

### Phase 4: Writers

```
T12
T13
T14
T15
T16
```

### Phase 5: HTTP close

```
T17
```

---

## Task Breakdown

### Phase 1: Schema

### T1: Add the period lock table

**What**: Migration `V22` cria `period_locks` com o único `(merchant_id, from_date, to_date)` e as FKs do design.
**Where**: `src/main/resources/db/migration/V22__create_period_locks.sql`
**Depends on**: None
**Reuses**: `V21__create_bank_statement_tables.sql`
**Requirement**: PER-11

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] A tabela tem `id`, `merchant_id`, `from_date`, `to_date`, `run_id`, `locked_at`
- [x] O único é `(merchant_id, from_date, to_date)`
- [x] Gate: `./mvnw -B -DskipTests compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(reconciliation): add the period lock table`
**Status**: Done

---

### T2: Add the period lock entity

**What**: `PeriodLockEntity` mapeia a tabela. Sem histórico e sem `locked_by`.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/entity/PeriodLockEntity.java`
**Depends on**: None
**Reuses**: `ReconciliationRunEntity.java`
**Requirement**: PER-11

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Campos do design mapeados, com `merchant` e `run` lazy
- [x] Gate: `./mvnw -B -DskipTests compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(reconciliation): add the period lock entity`
**Status**: Done

---

### T3: Add the period lock repository

**What**: Consultas da janela exata e das travas vivas do merchant, provadas no Testcontainers.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/repository/IPeriodLockRepository.java`
**Depends on**: T1, T2
**Reuses**: `IReconciliationRunRepository.java`
**Requirement**: PER-16, PER-41

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `findByMerchant_IdAndFromDateAndToDate` e `findByMerchant_Id` existem
- [x] O teste grava uma trava, relê pela janela e pelo merchant, e a segunda linha da mesma janela estoura o único
- [x] Gate: `./mvnw -B verify`
- [x] Test count: pelo menos 2 testes passam em `PeriodLockRepositoryIntegrationTest`

**Tests**: integration
**Gate**: full

**Commit**: `feat(reconciliation): add the period lock repository`
**Status**: Done

---

### Phase 2: Rules

### T4: Add the period conflict exception

**What**: `PeriodConflictException` para trava, data coberta, run em andamento e divergência aberta.
**Where**: `src/main/java/br/com/hanrry/reconpay/exception/PeriodConflictException.java`
**Depends on**: None
**Reuses**: `DiscrepancyResolutionConflictException.java`
**Requirement**: PER-13

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] A classe é uma `RuntimeException` com mensagem
- [x] Gate: `./mvnw -B -DskipTests compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(exception): add the period conflict exception`
**Status**: Done

---

### T5: Add the period not found exception

**What**: `PeriodNotFoundException` para janela sem run `COMPLETED` vigente.
**Where**: `src/main/java/br/com/hanrry/reconpay/exception/PeriodNotFoundException.java`
**Depends on**: None
**Reuses**: `ReconciliationNotFoundException.java`
**Requirement**: PER-09

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] A classe é uma `RuntimeException` com mensagem
- [x] Gate: `./mvnw -B -DskipTests compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(exception): add the period not found exception`
**Status**: Done

---

### T6: Map period errors to HTTP

**What**: O handler devolve 409 para conflito de período e 404 para período inexistente.
**Where**: `src/main/java/br/com/hanrry/reconpay/exception/handler/GlobalExceptionHandler.java`
**Depends on**: T4, T5
**Reuses**: as listas de `handleConflict` e de not found
**Requirement**: PER-09, PER-13

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `PeriodConflictException` responde HTTP 409 `CONFLICT`
- [x] `PeriodNotFoundException` responde HTTP 404 `NOT_FOUND`
- [x] Gate: `./mvnw -B test -Dtest=PeriodExceptionHandlerTest`
- [x] Test count: pelo menos 2 testes passam em `PeriodExceptionHandlerTest`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(exception): map period errors to http status`
**Status**: Done

---

### T7: Sum open discrepancy amounts

**What**: `OpenAmountCalculator` aplica a tabela de tipos, nulo como zero, e ignora status fora de `OPEN`.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/service/OpenAmountCalculator.java`
**Depends on**: None
**Reuses**: `BankStatementMatcher.formatAmount`
**Requirement**: PER-04

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Cada tipo da tabela do design tem um resultado afirmado, inclusive extrato lido de `actualValue`
- [x] Nulo vira `0.00` antes da diferença. Status fora de `OPEN` não soma. A escala final é 2, half up
- [x] Gate: `./mvnw -B test -Dtest=OpenAmountCalculatorTest`
- [x] Test count: pelo menos 12 testes passam em `OpenAmountCalculatorTest`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(reconciliation): sum open discrepancy amounts`
**Status**: Done

---

### T8: Lock the active merchant row

**What**: `IMerchantRepository` ganha a leitura `PESSIMISTIC_WRITE` do merchant ativo.
**Where**: `src/main/java/br/com/hanrry/reconpay/merchant/repository/IMerchantRepository.java`
**Depends on**: None
**Reuses**: `findByIdAndActiveTrue`
**Requirement**: PER-41

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Merchant ativo é retornado. Merchant inativo ou inexistente volta vazio
- [x] Gate: `./mvnw -B verify`
- [x] Test count: pelo menos 2 testes passam em `MerchantLockRepositoryIntegrationTest`

**Tests**: integration
**Gate**: full

**Commit**: `feat(merchant): lock the active merchant row for period writes`
**Status**: Done

---

### T9: Guard locked windows and dates

**What**: `PeriodGuard` segura o merchant e recusa janela exata ou data coberta, extremos inclusos.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/service/PeriodGuard.java`
**Depends on**: T3, T4, T8
**Reuses**: `IPeriodLockRepository`
**Requirement**: PER-22, PER-31

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Janela com linha de trava lança `PeriodConflictException` e a data igual a `fromDate` ou `toDate` também
- [x] Data fora de toda trava passa. Merchant inativo lança `MerchantNotFoundException`
- [x] As duas checagens chamam o lock pessimista antes de ler `period_locks`
- [x] Gate: `./mvnw -B test -Dtest=PeriodGuardTest`
- [x] Test count: pelo menos 5 testes passam em `PeriodGuardTest`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(reconciliation): guard locked windows and dates`
**Status**: Done

---

### Phase 3: API

### T10: Read and change the period lock

**What**: `PeriodService` lê, trava e reabre, com taxa, valor em aberto e duração do design.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/service/PeriodService.java`
**Depends on**: T3, T5, T7, T9
**Reuses**: `AuditLogger`, `Clock`, `ReconciliationProperties`
**Requirement**: PER-01, PER-02, PER-03, PER-05, PER-06, PER-07, PER-08, PER-10, PER-11, PER-12, PER-14, PER-15, PER-16, PER-17, PER-18, PER-19, PER-20, PER-21, PER-44

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] GET devolve a taxa congelada, o valor em aberto e a duração. Zero itens deixa a taxa nula. `FAILED` não esconde o `COMPLETED` vigente
- [x] Sem run vigente: `PeriodNotFoundException`. `PENDING` ou `RUNNING`: `PeriodConflictException`. `OPEN`: a trava não grava
- [x] Segunda trava conserva `lockedAt`. Reabrir limpa a duração. Travar de novo mede do mesmo `finishedAt`
- [x] Audit só é pedido depois do flush. Falha ao gravar não pede `PERIOD_LOCKED`
- [x] Gate: `./mvnw -B test -Dtest=PeriodServiceTest`
- [x] Test count: pelo menos 14 testes passam em `PeriodServiceTest`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(reconciliation): read and change the period lock`
**Status**: Done

---

### T11: Expose the period endpoints

**What**: GET, lock e unlock em `/api/merchants/{merchantId}/periods`, com o matcher `ADMIN`/`OPERATOR`.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/controller/PeriodController.java`
**Depends on**: T6, T10
**Reuses**: `ReconciliationController.java`, `MerchantAccessAspect`
**Requirement**: PER-01, PER-35, PER-36, PER-37, PER-38, PER-39, PER-40

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] O controller implementa `PeriodControllerApi` e não declara `@Operation` nem mapping
- [x] OPERATOR com grant e ADMIN sem `user_merchants` recebem o mesmo sucesso. Sem token: 401. Sem grant: 403
- [x] Data ausente, invertida ou acima de `maxWindowDays`: 400 `VALIDATION_ERROR`. Merchant inativo: 404
- [x] `SecurityConfig` exige `ADMIN` ou `OPERATOR` em `/api/merchants/*/periods` e `/api/merchants/*/periods/**`
- [x] Gate: `./mvnw -B verify`
- [x] Test count: pelo menos 8 testes passam em `PeriodIntegrationTest`

**Tests**: integration
**Gate**: full

**Commit**: `feat(reconciliation): expose the period endpoints`
**Status**: Done

---

### T18: Document the period endpoints

**What**: As três operações entram na tag `Reconciliations` e no catálogo que trava o `*Api`.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/openapi/PeriodControllerApi.java`
**Depends on**: T11
**Reuses**: `ReconciliationControllerApi.java`, `ApiDocumentationInterfacesTest`
**Requirement**: PER-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] GET, lock e unlock têm texto em português, códigos da spec e exemplo de sucesso
- [x] `ApiDocumentationInterfacesTest` inclui `PeriodController` e continua exigindo um `*Api` sem `@Operation` no controller
- [x] Gate: `./mvnw -B verify`
- [x] Test count: os testes de `ApiDocsIntegrationTest` e `ApiDocumentationInterfacesTest` passam, com as três operações novas afirmadas

**Tests**: integration
**Gate**: full

**Commit**: `docs(openapi): document the period endpoints`
**Status**: Done

---

### Phase 4: Writers

### T12: Reject a new run on a locked window

**What**: `ReconciliationService.run` chama `assertWindowOpen` antes do `saveAndFlush`. Outro par de datas segue HTTP 202.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/service/ReconciliationService.java`
**Depends on**: T9
**Reuses**: `PeriodGuard`
**Requirement**: PER-22, PER-23

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] A janela travada lança `PeriodConflictException` e não grava run
- [x] Outro `fromDate` ou `toDate` ainda grava `PENDING`, mesmo com sobreposição de datas
- [x] A checagem de `maxWindowDays` continua antes do guard
- [x] Gate: `./mvnw -B test -Dtest=ReconciliationServiceTest`
- [x] Test count: pelo menos 3 testes passam em `ReconciliationServiceTest`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(reconciliation): reject a run on a locked window`
**Status**: Done

---

### T13: Reject discrepancy changes on a locked window

**What**: O PATCH válido do run vigente da janela travada não grava. A validação atual continua na frente.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/service/DiscrepancyResolutionService.java`
**Depends on**: T9
**Reuses**: `PeriodGuard`, `DiscrepancyResolutionServiceTest`
**Requirement**: PER-28, PER-34

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Transição válida na janela travada lança `PeriodConflictException` e o status permanece
- [x] Payload inválido continua 400, sem consultar a trava
- [x] Run de janela não travada segue as regras de resolução atuais
- [x] Gate: `./mvnw -B test -Dtest=DiscrepancyResolutionServiceTest`
- [x] Test count: a classe passa, com pelo menos 3 testes novos

**Tests**: unit
**Gate**: quick

**Commit**: `feat(reconciliation): reject discrepancy changes on a locked window`
**Status**: Done

---

### T14: Reject transaction writes inside a locked window

**What**: Criar transação e mudar status recusam a data coberta depois das regras que já existem.
**Where**: `src/main/java/br/com/hanrry/reconpay/transaction/service/TransactionService.java`
**Depends on**: T9
**Reuses**: `PeriodGuard`, `TransactionServiceTest`
**Requirement**: PER-24, PER-25, PER-31, PER-32

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Data coberta, inclusive `fromDate` e `toDate`, lança `PeriodConflictException` e não grava
- [x] Data fora de toda trava segue o sucesso atual
- [x] Duplicidade, taxa ausente, parcelas inválidas e transição inválida continuam com o erro de hoje, sem passar pelo guard
- [x] Gate: `./mvnw -B test -Dtest=TransactionServiceTest`
- [x] Test count: a classe passa, com pelo menos 4 testes novos

**Tests**: unit
**Gate**: quick

**Commit**: `feat(transaction): reject writes inside a locked window`
**Status**: Done

---

### T15: Reject settlement imports inside a locked window

**What**: O lote de liquidação é recusado se alguma `settlementDate` válida cair numa trava. Parse e duplicidade ficam na frente.
**Where**: `src/main/java/br/com/hanrry/reconpay/externalsettlement/service/ExternalSettlementService.java`
**Depends on**: T9
**Reuses**: `PeriodGuard`, `ExternalSettlementServiceTest`
**Requirement**: PER-26, PER-29, PER-30

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Arquivo válido com uma data coberta lança `PeriodConflictException` e não grava lote nem linha
- [x] Arquivo inválido continua 400, mesmo com outra data coberta
- [x] `toDate` mais um dia, fora de toda trava, grava o lote
- [x] Gate: `./mvnw -B test -Dtest=ExternalSettlementServiceTest`
- [x] Test count: a classe passa, com pelo menos 3 testes novos

**Tests**: unit
**Gate**: quick

**Commit**: `feat(externalsettlement): reject imports inside a locked window`
**Status**: Done

---

### T16: Reject bank statement imports inside a locked window

**What**: O lote do extrato é recusado se alguma `movementDate` válida cair numa trava. Parse e duplicidade ficam na frente.
**Where**: `src/main/java/br/com/hanrry/reconpay/bankstatement/service/BankStatementService.java`
**Depends on**: T9
**Reuses**: `PeriodGuard`
**Requirement**: PER-27, PER-29

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Arquivo válido com uma data coberta lança `PeriodConflictException` e não grava lote nem linha
- [ ] Arquivo inválido continua 400
- [ ] Datas fora de toda trava gravam o lote
- [ ] Gate: `./mvnw -B test -Dtest=BankStatementServiceTest`
- [ ] Test count: pelo menos 3 testes passam em `BankStatementServiceTest`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(bankstatement): reject imports inside a locked window`

---

### Phase 5: HTTP close

### T17: Prove the close over HTTP

**What**: Um teste de integração percorre o fechamento de ponta a ponta, inclusive a corrida e o que continua livre.
**Where**: `src/test/java/br/com/hanrry/reconpay/reconciliation/PeriodCloseIntegrationTest.java`
**Depends on**: T11, T12, T13, T14, T15, T16
**Reuses**: `AbstractIntegrationTest`, `ReconciliationIntegrationTest`
**Requirement**: PER-23, PER-30, PER-32, PER-33, PER-41, PER-42, PER-43

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Travar `2026-07-01`–`2026-07-15`, criar venda em `2026-07-10` recebe 409, venda em `2026-07-20` recebe 201, liquidação em `2026-07-16` recebe 201
- [ ] Run de outro par de datas recebe 202. Taxa e cadastro de merchant seguem com o sucesso atual
- [ ] Duas travas simultâneas: um 200 e um 409. Duas reaberturas simultâneas: um 200 e um 409
- [ ] Trava e mutação da mesma janela ao mesmo tempo: um grava e o outro recebe 409
- [ ] Gate: `./mvnw -B verify`
- [ ] Test count: pelo menos 6 testes passam em `PeriodCloseIntegrationTest`

**Tests**: integration
**Gate**: full

**Commit**: `test(reconciliation): prove period close over http`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5

Phase 1:  T1 → T3
          T2 → T3
Phase 2:  T4 → T6
          T5 → T6
          T4 → T9
          T8 → T9
          T7
Phase 3:  T10 → T11 → T18
Phase 4:  T12
          T13
          T14
          T15
          T16
Phase 5:  T17
```

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: period lock table | 1 migration | ✅ Granular |
| T2: period lock entity | 1 entity | ✅ Granular |
| T3: period lock repository | 1 repository + seu teste | ✅ Granular |
| T4: conflict exception | 1 class | ✅ Granular |
| T5: not found exception | 1 class | ✅ Granular |
| T6: HTTP mapping | 1 handler | ✅ Granular |
| T7: open amount | 1 calculator | ✅ Granular |
| T8: merchant row lock | 1 repository method | ✅ Granular |
| T9: period guard | 1 service | ✅ Granular |
| T10: period service | 1 service | ✅ Granular |
| T11: period endpoints | 1 controller, com API, DTO e matcher no mesmo contrato | ✅ Granular |
| T12: locked run | 1 service | ✅ Granular |
| T13: locked discrepancy | 1 service | ✅ Granular |
| T14: locked transaction | 1 service | ✅ Granular |
| T15: locked settlement import | 1 service | ✅ Granular |
| T16: locked bank import | 1 service | ✅ Granular |
| T17: HTTP close | 1 integration test | ✅ Granular |
| T18: OpenAPI | 1 interface de documentação | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | nenhum | ✅ Match |
| T2 | None | nenhum | ✅ Match |
| T3 | T1, T2 | T1 → T3, T2 → T3 | ✅ Match |
| T4 | None | T4 → T6, T4 → T9 | ✅ Match |
| T5 | None | T5 → T6 | ✅ Match |
| T6 | T4, T5 | T4 → T6, T5 → T6 | ✅ Match |
| T7 | None | nenhum | ✅ Match |
| T8 | None | T8 → T9 | ✅ Match |
| T9 | T3, T4, T8 | T4 → T9, T8 → T9. T3 é fase anterior | ✅ Match |
| T10 | T3, T5, T7, T9 | T10 → T11. Dependências são fase anterior | ✅ Match |
| T11 | T6, T10 | T10 → T11. T6 é fase anterior | ✅ Match |
| T12 | T9 | nenhum na fase. T9 é fase anterior | ✅ Match |
| T13 | T9 | nenhum na fase. T9 é fase anterior | ✅ Match |
| T14 | T9 | nenhum na fase. T9 é fase anterior | ✅ Match |
| T15 | T9 | nenhum na fase. T9 é fase anterior | ✅ Match |
| T16 | T9 | nenhum na fase. T9 é fase anterior | ✅ Match |
| T17 | T11, T12, T13, T14, T15, T16 | nenhum na fase. Todas são fase anterior | ✅ Match |
| T18 | T11 | T11 → T18 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1: table | Flyway | none | none | ✅ OK |
| T2: entity | Entity | none | none | ✅ OK |
| T3: repository | Repository | integration | integration | ✅ OK |
| T4: conflict exception | Exception sem ramo | none | none | ✅ OK |
| T5: not found exception | Exception sem ramo | none | none | ✅ OK |
| T6: handler | Handler | unit | unit | ✅ OK |
| T7: calculator | Service | unit | unit | ✅ OK |
| T8: merchant lock | Repository | integration | integration | ✅ OK |
| T9: guard | Service | unit | unit | ✅ OK |
| T10: period service | Service | unit | unit | ✅ OK |
| T11: endpoints | Controller | integration | integration | ✅ OK |
| T12: run | Service | unit | unit | ✅ OK |
| T13: discrepancy | Service | unit | unit | ✅ OK |
| T14: transaction | Service | unit | unit | ✅ OK |
| T15: settlement | Service | unit | unit | ✅ OK |
| T16: bank statement | Service | unit | unit | ✅ OK |
| T17: HTTP close | Integration | integration | integration | ✅ OK |
| T18: OpenAPI | OpenAPI | integration | integration | ✅ OK |
