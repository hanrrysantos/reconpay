# Discrepancy Resolution Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/discrepancy-resolution/design.md`
**Status**: In Progress

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `AGENTS.md`, `README.md` (Testes), `pom.xml` (JaCoCo 85% linha / 75% ramo no `verify`), `.github/workflows/ci.yml`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Service / engine / validator | unit | Todos os ramos; 1:1 com os ACs do serviço; cada borda da spec tem teste | `src/test/java/**/service/*Test.java`, `src/test/java/**/validation/*Test.java` | `./mvnw -B test -Dtest=ClassName` |
| Exception handler | unit | 404 e 409 no `StandardError` | `src/test/java/**/*Test.java` | `./mvnw -B test -Dtest=ClassName` |
| Repository | integration | Acha a divergência no merchant e no run; ausência não devolve outra | `src/test/java/**/integration/*Test.java` | `./mvnw -B verify` |
| Controller | integration | GET e PATCH: sucesso, 400, 401, 403, 404, 409 | `src/test/java/**/*IntegrationTest.java` | `./mvnw -B verify` |
| Enum / Flyway / entity / DTO | none | Gate de compilação. JaCoCo já exclui `entity` e `dto` | — | `./mvnw -B -DskipTests compile` |

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

### Phase 1: Foundation

```
T1 → T5
T2 → T3
T2 → T4
T2 → T5
T3 → T5
T4 → T5
```

### Phase 2: Contract

```
T6
T7
```

### Phase 3: Resolution

```
T8 → T9 → T10 → T11
```

### Phase 4: Read model and API

```
T12 → T15
T13 → T15
T14 → T15
```

---

## Task Breakdown

### Phase 1: Foundation

### T1: Add DiscrepancyStatus

**What**: Enum com `OPEN`, `ACCEPTED`, `ADJUSTED` e `WRITTEN_OFF`.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/enums/DiscrepancyStatus.java`
**Depends on**: None
**Reuses**: `DiscrepancyType.java`
**Requirement**: RES-01

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] Os quatro valores existem
- [x] Gate: `./mvnw -B -DskipTests compile`
- [x] Nenhum teste removido

**Tests**: none
**Gate**: build

**Commit**: `feat(reconciliation): add discrepancy status enum`

---

### T2: Add resolution tables

**What**: Migration `V20` com `status` e `version` na divergência, tabelas de lançamento e histórico, check de valor diferente de zero, e índice único parcial de um lançamento ativo. Linhas já gravadas ficam `OPEN`.
**Where**: `src/main/resources/db/migration/V20__discrepancy_resolution.sql`
**Depends on**: None
**Reuses**: `V3__create_feerules_table.sql`, `V17__transaction_optimistic_locking.sql`
**Requirement**: RES-01

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] A migration sobe no Testcontainers no próximo `verify`
- [x] Gate: `./mvnw -B -DskipTests compile`
- [x] Nenhum teste removido

**Tests**: none
**Gate**: build

**Commit**: `feat(reconciliation): add discrepancy resolution tables`

---

### T3: Add adjustment entity

**What**: Entidade do lançamento: valor, ator, instante e `voidedAt`.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/entity/DiscrepancyAdjustmentEntity.java`
**Depends on**: T2
**Reuses**: `ReconciliationDiscrepancyEntity.java`
**Requirement**: RES-03

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] Campos do design mapeados na tabela `discrepancy_adjustments`
- [x] Gate: `./mvnw -B -DskipTests compile`
- [x] Nenhum teste removido

**Tests**: none
**Gate**: build

**Commit**: `feat(reconciliation): add discrepancy adjustment entity`

---

### T4: Add transition entity

**What**: Entidade do histórico: ator, status anterior, status novo, nota e instante.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/entity/DiscrepancyTransitionEntity.java`
**Depends on**: T2
**Reuses**: `DiscrepancyAdjustmentEntity.java`
**Requirement**: RES-18

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] Campos do design mapeados na tabela `discrepancy_transitions`
- [x] Gate: `./mvnw -B -DskipTests compile`
- [x] Nenhum teste removido

**Tests**: none
**Gate**: build

**Commit**: `feat(reconciliation): add discrepancy transition entity`

---

### T5: Store status and version on the discrepancy

**What**: A divergência ganha `status`, `@Version` e as duas coleções lazy.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/entity/ReconciliationDiscrepancyEntity.java`
**Depends on**: T1, T2, T3, T4
**Reuses**: `InternalTransactionEntity.java`
**Requirement**: RES-01, RES-15

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] `status` default `OPEN` e `version` presentes
- [ ] Coleções de ajuste e histórico são lazy
- [ ] Gate: `./mvnw -B -DskipTests compile`
- [ ] Nenhum teste removido

**Tests**: none
**Gate**: build

**Commit**: `feat(reconciliation): store status and version on discrepancy`

---

### Phase 2: Contract

### T6: Map resolution errors

**What**: `DiscrepancyNotFoundException` entra no handler de 404. `DiscrepancyResolutionConflictException` entra no handler de 409, separado da transição de transação que responde 400.
**Where**: `src/main/java/br/com/hanrry/reconpay/exception/handler/GlobalExceptionHandler.java`
**Depends on**: None
**Reuses**: `ReconciliationNotFoundException`, `handleConflict`
**Requirement**: RES-10, RES-14

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] Teste unitário vê HTTP 404 `NOT_FOUND` e HTTP 409 `CONFLICT` no `StandardError`
- [x] Gate: `./mvnw -B test -Dtest=DiscrepancyResolutionErrorTest`
- [x] Pelo menos 2 testes novos passam, e nenhum teste antigo é removido

**Tests**: unit
**Gate**: quick

**Commit**: `feat(reconciliation): map discrepancy resolution errors`

---

### T7: Validate status requests

**What**: DTO com `status`, `note` e `correctionAmount`, e validador que rejeita nota acima de 500, valor ausente ou zero ou fora de 17 inteiros e 2 decimais em `ADJUSTED`, e valor presente nos outros alvos.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/validation/DiscrepancyStatusRequestValidator.java`
**Depends on**: T1
**Reuses**: `@Digits` de `CreateTransactionRequestDTO`
**Requirement**: RES-04, RES-05, RES-06

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [x] Os três rejeites da spec falham a validação
- [x] Valor negativo diferente de zero em `ADJUSTED` passa
- [x] Gate: `./mvnw -B test -Dtest=DiscrepancyStatusRequestValidatorTest`
- [x] Pelo menos 4 testes novos passam, e nenhum teste antigo é removido

**Tests**: unit
**Gate**: quick

**Commit**: `feat(reconciliation): validate discrepancy status requests`

---

### Phase 3: Resolution

### T8: Load a discrepancy by merchant and run

**What**: Consulta por id da divergência, id do run e id do merchant. Outro merchant ou outro run não devolve a linha.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/repository/IReconciliationDiscrepancyRepository.java`
**Depends on**: T5
**Reuses**: `IReconciliationItemRepository.java`, `AbstractIntegrationTest`
**Requirement**: RES-14

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] Integração acha a divergência do run daquele merchant
- [ ] A mesma divergência com outro merchant ou outro run vem vazia
- [ ] Gate: `./mvnw -B verify`
- [ ] Pelo menos 2 testes novos passam, e nenhum teste antigo é removido

**Tests**: integration
**Gate**: full

**Commit**: `feat(reconciliation): load discrepancy by merchant and run`

---

### T9: Close an open discrepancy

**What**: De `OPEN`, grava `ACCEPTED`, `WRITTEN_OFF` ou `ADJUSTED` com lançamento, histórico e auditoria depois do commit. ADMIN sem linha em `user_merchants` usa o mesmo método. Transação e snapshot não são alterados.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/service/DiscrepancyResolutionService.java`
**Depends on**: T6, T7, T8
**Reuses**: `AuditLogger`, `TransactionService.updateStatus`
**Requirement**: RES-02, RES-03, RES-17, RES-22

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] Testes unitários cobrem Aceita, Baixada e Ajustada
- [ ] Ajustada persiste um lançamento ativo com o valor informado
- [ ] Auditoria é registrada dentro da transação, para sair só depois do commit
- [ ] Nenhum save de transação interna é chamado
- [ ] Gate: `./mvnw -B test -Dtest=DiscrepancyResolutionServiceTest`
- [ ] Pelo menos 4 testes novos passam, e nenhum teste antigo é removido

**Tests**: unit
**Gate**: quick

**Commit**: `feat(reconciliation): close an open discrepancy`

---

### T10: Reopen a resolved discrepancy

**What**: `ACCEPTED` e `WRITTEN_OFF` voltam para `OPEN`. `ADJUSTED` volta para `OPEN` e anula o lançamento ativo. Mesmo status, ou salto entre terminais, responde conflito e não grava.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/service/DiscrepancyResolutionService.java`
**Depends on**: T9
**Reuses**: `DiscrepancyResolutionConflictException`
**Requirement**: RES-08, RES-09, RES-10

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] Reabrir Ajustada preenche `voidedAt` e não deixa outro lançamento ativo
- [ ] Reabrir Aceita não cria lançamento
- [ ] Salto entre terminais e status repetido lançam conflito sem save
- [ ] Gate: `./mvnw -B test -Dtest=DiscrepancyResolutionServiceTest`
- [ ] Pelo menos 3 testes novos passam, e nenhum teste antigo é removido

**Tests**: unit
**Gate**: quick

**Commit**: `feat(reconciliation): reopen a resolved discrepancy`

---

### T11: Guard resolution writes

**What**: Run que não está `COMPLETED` ou que tem `supersededAt` responde conflito. Nota em branco vira null. Falha ao gravar lançamento ou histórico desfaz o status.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/service/DiscrepancyResolutionService.java`
**Depends on**: T10
**Reuses**: `ReconciliationRunStatus`
**Requirement**: RES-07, RES-11, RES-16

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] Run pendente, em execução, falho ou substituído não altera a divergência
- [ ] Nota vazia ou só espaços fica null no histórico
- [ ] Exceção no save do lançamento ou do histórico mantém o status anterior
- [ ] Gate: `./mvnw -B test -Dtest=DiscrepancyResolutionServiceTest`
- [ ] Pelo menos 3 testes novos passam, e nenhum teste antigo é removido

**Tests**: unit
**Gate**: quick

**Commit**: `feat(reconciliation): guard discrepancy resolution writes`

---

### Phase 4: Read model and API

### T12: Start new discrepancies as open

**What**: O factory do motor define `OPEN`. O batimento continua só com transação e liquidação.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/service/ReconciliationEngine.java`
**Depends on**: T1, T5
**Reuses**: `ReconciliationEngineTest`
**Requirement**: RES-01, RES-23

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] Toda divergência criada no teste nasce `OPEN`, sem histórico
- [ ] Um caso já coberto de match e um de divergência continuam com o mesmo resultado
- [ ] Gate: `./mvnw -B test -Dtest=ReconciliationEngineTest`
- [ ] Nenhum teste antigo é removido

**Tests**: unit
**Gate**: quick

**Commit**: `feat(reconciliation): start new discrepancies as open`

---

### T13: Expose id and status on item discrepancies

**What**: O DTO da lista ganha `id` e `status`. Não leva histórico nem lançamento.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/dto/DiscrepancyResponseDTO.java`
**Depends on**: T5
**Reuses**: `IReconciliationMapper.toDiscrepancyDTO`
**Requirement**: RES-19

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] `GET .../items` devolve `id` e `status` em cada divergência
- [ ] O JSON do item não traz histórico nem lançamento
- [ ] Gate: `./mvnw -B verify`
- [ ] Pelo menos 1 teste de integração novo passa, e nenhum teste antigo é removido

**Tests**: integration
**Gate**: full

**Commit**: `feat(reconciliation): expose discrepancy id and status on items`

---

### T14: Keep resolution data out of the CSV

**What**: O export não ganha coluna de status nem de valor de correção.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/service/ReconciliationCsvExporter.java`
**Depends on**: None
**Reuses**: `ReconciliationCsvExporterTest`
**Requirement**: RES-21

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] O cabeçalho permanece o atual
- [ ] O teste afirma que o cabeçalho não contém status de desfecho nem valor de correção
- [ ] Gate: `./mvnw -B test -Dtest=ReconciliationCsvExporterTest`
- [ ] Nenhum teste antigo é removido

**Tests**: unit
**Gate**: quick

**Commit**: `test(reconciliation): keep resolution data out of the csv`

---

### T15: Expose resolution endpoints

**What**: GET e PATCH em `/api/merchants/{merchantId}/reconciliations/{runId}/discrepancies/{discrepancyId}`. O 200 dos dois tem o mesmo corpo: status, lançamentos e histórico em ordem cronológica. Cobre 401, 403, 404, 409 de run substituído e 409 de dois PATCH simultâneos.
**Where**: `src/main/java/br/com/hanrry/reconpay/reconciliation/controller/ReconciliationController.java`
**Depends on**: T11, T12, T13, T14
**Reuses**: `MerchantAccessAspect`, `PATH_RECONCILIATIONS`
**Requirement**: RES-12, RES-13, RES-14, RES-15, RES-18, RES-20

**Tools**:

- MCP: NONE
- Skill: tlc-spec-driven

**Done when**:

- [ ] PATCH Aceita e GET devolvem o mesmo status e uma linha de histórico
- [ ] Sem token: 401. OPERATOR sem grant: 403. Id de outro run: 404
- [ ] Run substituído: 409 sem mudar o status
- [ ] Dois PATCH concorrentes: um persiste e o outro recebe 409
- [ ] Gate: `./mvnw -B verify`
- [ ] Pelo menos 6 testes novos passam, e nenhum teste antigo é removido

**Tests**: integration
**Gate**: full

**Commit**: `feat(reconciliation): expose discrepancy resolution endpoints`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1 → T5
          T2 → T3 → T5
          T2 → T4 → T5
Phase 2:  T6
          T7
Phase 3:  T8 → T9 → T10 → T11
Phase 4:  T12 → T15
          T13 → T15
          T14 → T15
```

Execução estritamente sequencial. 15 tarefas. Na execução, as fases 1 e 2 formam um lote, e as fases 3 e 4 formam o seguinte. O lote só começa se você aceitar subagentes. Sem isso, as tarefas rodam nesta janela, uma por vez.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: DiscrepancyStatus | 1 enum | ✅ Granular |
| T2: Migration V20 | 1 arquivo SQL | ✅ Granular |
| T3: Adjustment entity | 1 entidade | ✅ Granular |
| T4: Transition entity | 1 entidade | ✅ Granular |
| T5: Status e version na divergência | 1 entidade | ✅ Granular |
| T6: Erros 404 e 409 | 1 handler | ✅ Granular |
| T7: Validador do pedido | 1 validador | ✅ Granular |
| T8: Consulta por merchant e run | 1 repositório | ✅ Granular |
| T9: Fechar divergência | 1 serviço | ✅ Granular |
| T10: Reabrir | o mesmo serviço, um comportamento | ✅ Granular |
| T11: Guardas de escrita | o mesmo serviço, um comportamento | ✅ Granular |
| T12: Motor marca OPEN | 1 método | ✅ Granular |
| T13: id e status na lista | 1 DTO | ✅ Granular |
| T14: CSV sem desfecho | 1 exportador | ✅ Granular |
| T15: GET e PATCH | 1 controller | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | sem seta de entrada | ✅ Match |
| T2 | None | sem seta de entrada | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T2 | T2 → T4 | ✅ Match |
| T5 | T1, T2, T3, T4 | T1 → T5, T2 → T5, T3 → T5, T4 → T5 | ✅ Match |
| T6 | None | sem seta de entrada na fase | ✅ Match |
| T7 | T1 | T1 é da fase anterior | ✅ Match |
| T8 | T5 | T5 é da fase anterior | ✅ Match |
| T9 | T6, T7, T8 | T8 → T9; T6 e T7 são da fase anterior | ✅ Match |
| T10 | T9 | T9 → T10 | ✅ Match |
| T11 | T10 | T10 → T11 | ✅ Match |
| T12 | T1, T5 | fases anteriores | ✅ Match |
| T13 | T5 | fase anterior | ✅ Match |
| T14 | None | sem seta de entrada | ✅ Match |
| T15 | T11, T12, T13, T14 | T12 → T15, T13 → T15, T14 → T15; T11 é da fase anterior | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1 | Enum | none | none | ✅ OK |
| T2 | Flyway | none | none | ✅ OK |
| T3 | Entity | none | none | ✅ OK |
| T4 | Entity | none | none | ✅ OK |
| T5 | Entity | none | none | ✅ OK |
| T6 | Exception handler | unit | unit | ✅ OK |
| T7 | Validator | unit | unit | ✅ OK |
| T8 | Repository | integration | integration | ✅ OK |
| T9 | Service | unit | unit | ✅ OK |
| T10 | Service | unit | unit | ✅ OK |
| T11 | Service | unit | unit | ✅ OK |
| T12 | Engine | unit | unit | ✅ OK |
| T13 | DTO de lista no endpoint de items | integration | integration | ✅ OK |
| T14 | CSV exporter | unit | unit | ✅ OK |
| T15 | Controller | integration | integration | ✅ OK |
