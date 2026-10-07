# Period Close Specification

## Problem Statement

O run vigente já responde o que bateu e o que divergiu, e o operador já dá desfecho a cada divergência. A janela ainda não tem leitura de fechamento nem um ponto em que aquele resultado para de mudar. Sem isso, taxa de match, valor em aberto e tempo até fechar o período continuam conta de planilha.

## Goals

- [ ] OPERATOR com grant, e ADMIN, leem a taxa de match, o valor em aberto e o tempo até a trava da janela `fromDate`/`toDate`
- [ ] A trava só ocorre com run vigente `COMPLETED` e zero divergências `OPEN`, e passa a recusar outro run dessa janela, mudança de divergência e lançamento com data dentro dela

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Frontend | Épico seguinte |
| Período em mês civil | O período desta feature é a janela exata do run |
| Histórico de travas | Só o estado atual importa. Reabrir apaga `lockedAt` |
| Travar a cauda do `settlement-lag-days` | Data depois de `toDate` continua importável |
| Lista de janelas | O cliente nomeia `fromDate` e `toDate` |
| Mudar o motor de batimento | O run e o snapshot continuam a fonte |
| Colunas novas no CSV de conciliação | O export atual permanece |
| Owner, convite, refresh de JWT, rate limit e fila externa | Fora desta leva |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Identidade do período | `merchantId` + `fromDate` + `toDate` do run | A janela já é a unidade da conciliação | y |
| Alcance da trava | Run novo dessa janela, PATCH de divergência desse run, transação, status de transação e importação com data inclusiva na janela | O que alteraria o fechamento ou o próximo run fica parado | y |
| Condição da trava | Run vigente `COMPLETED`, zero `OPEN`, sem run `PENDING` ou `RUNNING` na mesma janela | Fechado convive com pendência zero | y |
| Quem trava e reabre | OPERATOR com grant e ADMIN, com ou sem linha em `user_merchants` | O mesmo par do resto da conciliação | y |
| Números | Taxa congelada do run, valor em aberto pela tabela de tipos, segundos até a trava | A resolução não reescreve `matchedCount` | y |
| Datas inclusivas | `fromDate` e `toDate` entram na trava | Igual à janela do run | y (agent) |
| Janelas sobrepostas | A data cai na trava se qualquer janela travada do merchant a cobre. Outro par `fromDate`/`toDate` ainda pode conciliar | A trava de run é a janela exata. A trava de data é por cobertura | y |
| Cauda de liquidação | `settlementDate` igual a `toDate` mais um dia não está na janela | A decisão travou a janela, não o atraso | y (agent) |
| Validação antes da trava | Payload inválido responde `400` mesmo se a data também estiver travada | O cliente corrige o arquivo antes de tratar o fechamento | y (agent) |
| Run `FAILED` | Não esconde o `COMPLETED` vigente. Sem `COMPLETED` vigente, a leitura é `404` | Falha não substitui o run anterior | y (agent) |
| Segunda trava ou reabertura | `409 CONFLICT`. `lockedAt` original permanece na segunda trava | Mesma regra dos conflitos de estado | y |
| Reabrir e travar de novo | Nova `lockedAt`. A duração usa o `finishedAt` do run vigente | Não há histórico de travas | y (agent) |
| Valor de correção | Não entra no valor em aberto. Só status `OPEN` entra | Ajustada já saiu de `OPEN` | y (agent) |
| Representação | GET, trava e reabertura devolvem o mesmo corpo | O cliente lê o efeito na mesma forma | y (agent) |
| Auditoria | `PERIOD_LOCKED` e `PERIOD_UNLOCKED` só depois do commit | Sucesso só é auditado com a gravação confirmada | y (agent) |
| Merchant inativo | `404 NOT_FOUND` | Consultas de merchant já exigem ativo | y (agent) |
| Serviço externo | N/A porque a feature não chama serviço externo | Não há integração nova | y |
| Expiração | N/A porque a trava permanece até a reabertura | Não há TTL | y |
| Rate limit | N/A porque o limite ficou fora desta leva | Mesma fronteira das features anteriores | y |
| Métrica nova | N/A porque o audit log cobre quem travou | Não há contador novo | y |

**Open questions:** none

---

## User Stories

### P1: Ler a janela ⭐ MVP

**User Story**: As an operator, I want the match rate, the open amount, and the time to lock for one window so that closing that period is a number instead of a spreadsheet.

**Why P1**: Sem a leitura, a trava não tem o que publicar.

**Acceptance Criteria**:

1. WHEN an authenticated OPERATOR with a grant for merchantId GETs `/api/merchants/{merchantId}/periods?fromDate={fromDate}&toDate={toDate}` and that window has a COMPLETED run with null supersededAt and no PENDING or RUNNING run for the same dates THEN the system SHALL respond HTTP 200 with runId, fromDate, toDate, totalItems, matchedCount, divergentCount, matchRate, openAmount, locked, lockedAt, and closeDurationSeconds.
2. WHEN totalItems is greater than zero THEN the system SHALL set matchRate to matchedCount divided by totalItems, rounded half up to 4 decimal places.
3. WHEN totalItems is zero THEN the system SHALL set matchRate to null.
4. The system SHALL set openAmount to the half-up scale-2 sum of each OPEN discrepancy on that run: MISSING_SETTLEMENT adds expectedNetAmount, ORPHAN_SETTLEMENT adds settlementNetAmount, INCORRECT_AMOUNT adds the absolute difference of transactionAmount and settlementAmount, FEE_DIVERGENCE adds the absolute difference of expectedNetAmount and settlementNetAmount, MISSING_BANK_CREDIT adds settlementNetAmount, BANK_AMOUNT_MISMATCH adds the absolute difference of settlementNetAmount and the linked bank line amount, ORPHAN_BANK_CREDIT adds the linked bank line amount, and STATUS_MISMATCH, PAYMENT_METHOD_MISMATCH, INSTALLMENTS_MISMATCH, and AMBIGUOUS_BANK_MATCH add 0.00. A null amount adds 0.00. Any other status adds nothing.
5. WHEN the window is not locked THEN the system SHALL set locked to false, lockedAt to null, and closeDurationSeconds to null.
6. WHEN the window is locked THEN the system SHALL set locked to true, lockedAt to the lock timestamp, and closeDurationSeconds to the whole seconds from that run's finishedAt to lockedAt, truncated toward zero.
7. WHEN a discrepancy on that run changes status THEN the system SHALL keep matchRate equal to the value from matchedCount and totalItems.
8. WHEN the newest run for the window is FAILED, a COMPLETED run for the same dates still has null supersededAt, and no PENDING or RUNNING run exists THEN the system SHALL respond HTTP 200 for that COMPLETED run.
9. IF no COMPLETED run with null supersededAt exists for that window THEN the system SHALL respond HTTP 404 with error code NOT_FOUND.
10. WHILE a PENDING or RUNNING run exists for the same fromDate and toDate, WHEN an authorized client GETs the period THEN the system SHALL respond HTTP 409 with error code CONFLICT.

**Independent Test**: Complete a two-item run with one match and one OPEN fee gap of 2.80, GET the window, and see matchRate 0.5000 and openAmount 2.80. Accept the fee gap, GET again, and see the same matchRate and openAmount 0.00.

---

### P1: Travar a janela

**User Story**: As an operator, I want to lock a clean completed window so that the close time is recorded and the picture stops moving.

**Why P1**: A leitura sem trava não fecha o período.

**Acceptance Criteria**:

1. WHEN an authorized actor POSTs `/api/merchants/{merchantId}/periods/lock` with fromDate and toDate, the window has a COMPLETED run with null supersededAt, every discrepancy on that run is outside OPEN, no PENDING or RUNNING run exists for those dates, and the window is not locked THEN the system SHALL respond HTTP 200 with the period representation, locked true, openAmount 0.00, and closeDurationSeconds measured from that run's finishedAt.
2. WHEN that run has totalItems zero and no discrepancy THEN the system SHALL lock it with matchRate null and openAmount 0.00.
3. IF any discrepancy on the current run is OPEN THEN the system SHALL respond HTTP 409 with error code CONFLICT and leave the window unlocked.
4. IF no COMPLETED run with null supersededAt exists THEN the system SHALL respond HTTP 404 with error code NOT_FOUND and store no lock.
5. WHILE a PENDING or RUNNING run exists for those dates, WHEN an authorized actor POSTs lock THEN the system SHALL respond HTTP 409 with error code CONFLICT and store no lock.
6. IF the window is already locked THEN the system SHALL respond HTTP 409 with error code CONFLICT and keep the original lockedAt.
7. The system SHALL emit audit event PERIOD_LOCKED, with merchantId, fromDate, toDate, and runId, only after the lock commits.

**Independent Test**: Resolve every discrepancy, POST lock, and see HTTP 200 with openAmount 0.00 and a close duration. POST lock again and receive 409 with the same lockedAt.

---

### P1: Reabrir a janela

**User Story**: As an operator, I want to unlock a window so that a wrong close can be corrected and a later lock measures the time again.

**Why P1**: A trava sem reabertura prende um fechamento errado.

**Acceptance Criteria**:

1. WHEN an authorized actor POSTs `/api/merchants/{merchantId}/periods/unlock` for a locked window THEN the system SHALL respond HTTP 200 with the period representation, locked false, lockedAt null, closeDurationSeconds null, and the same runId and matchRate as before the unlock.
2. IF the window is not locked THEN the system SHALL respond HTTP 409 with error code CONFLICT and persist no change.
3. WHEN an authorized actor unlocks a window and locks it again while the same COMPLETED run remains current and has zero OPEN discrepancies THEN the system SHALL set closeDurationSeconds from that run's finishedAt to the new lockedAt.
4. The system SHALL emit audit event PERIOD_UNLOCKED, with merchantId, fromDate, and toDate, only after the unlock commits.

**Independent Test**: Lock a clean window, unlock it, and see locked false. Lock it again and see a new lockedAt.

---

### P1: Segurar a janela travada

**User Story**: As an operator, I want mutations inside a locked window to be refused so that the closed numbers and the next run of those dates stay put.

**Why P1**: Sem a recusa, a trava é só um rótulo.

**Acceptance Criteria**:

1. WHILE a window is locked, WHEN an authorized actor POSTs `/api/merchants/{merchantId}/reconciliations` with that same fromDate and toDate THEN the system SHALL respond HTTP 409 with error code CONFLICT and create no run.
2. WHEN an authorized actor POSTs `/api/merchants/{merchantId}/reconciliations` with a different fromDate or toDate THEN the system SHALL respond HTTP 202 even if the dates overlap a locked window.
3. WHILE transactionDate is inside a locked window of that merchant, fromDate and toDate inclusive, WHEN an authorized actor POSTs a valid transaction THEN the system SHALL respond HTTP 409 with error code CONFLICT and persist no transaction.
4. WHILE a transaction's transactionDate is inside a locked window of that merchant, WHEN an authorized actor PATCHes a valid status transition THEN the system SHALL respond HTTP 409 with error code CONFLICT and leave the status unchanged.
5. WHILE a settlement import is otherwise valid and any data row has settlementDate inside a locked window of that merchant THEN the system SHALL respond HTTP 409 with error code CONFLICT and persist neither the batch nor any row.
6. WHILE a bank-statement import is otherwise valid and any data row has movementDate inside a locked window of that merchant THEN the system SHALL respond HTTP 409 with error code CONFLICT and persist neither the batch nor any row.
7. WHILE a window is locked, WHEN an authorized actor PATCHes a discrepancy of that window's current run THEN the system SHALL respond HTTP 409 with error code CONFLICT and leave the discrepancy status unchanged.
8. IF the transaction, settlement import, or bank-statement import payload is invalid under the current rules THEN the system SHALL respond HTTP 400 with error code VALIDATION_ERROR and persist nothing, including when a date in the payload also falls inside a locked window.
9. WHEN a settlement file is otherwise valid, every settlementDate falls outside every locked window, and one settlementDate is toDate plus one day of a locked window THEN the system SHALL respond HTTP 201 and persist the batch.
10. WHEN transactionDate is fromDate or toDate of a locked window THEN the system SHALL respond HTTP 409 with error code CONFLICT.
11. WHEN a transactionDate falls outside every locked window of that merchant and the payload is valid THEN the system SHALL respond HTTP 201.
12. The system SHALL accept fee-rule and merchant updates for that merchant while one of its windows is locked.
13. WHEN a discrepancy belongs to the current run of a window that is not locked THEN the system SHALL apply the existing resolution rules.

**Independent Test**: Lock 2026-07-01 through 2026-07-15. Create a transaction on 2026-07-10 and receive 409. Create one on 2026-07-20 and receive 201. Import a settlement dated 2026-07-16 and receive 201.

---

### P1: Acesso, validação e concorrência

**User Story**: As an operator, I want the period endpoints to use the same access and conflict rules as reconciliation so that another merchant or a double lock cannot publish a close.

**Why P1**: A trava no merchant errado fecha o período de outra pessoa.

**Acceptance Criteria**:

1. IF the caller is unauthenticated THEN the system SHALL respond HTTP 401 on GET, lock, and unlock, and persist no lock.
2. IF an authenticated OPERATOR has no grant for merchantId THEN the system SHALL respond HTTP 403 with error code FORBIDDEN and persist no lock.
3. WHEN an authenticated ADMIN with no user_merchants row calls GET, lock, or unlock on a window that satisfies the rules THEN the system SHALL apply the same success outcome as an OPERATOR with grant.
4. IF merchantId is not an active merchant THEN the system SHALL respond HTTP 404 with error code NOT_FOUND.
5. IF fromDate or toDate is absent, or fromDate is after toDate THEN the system SHALL respond HTTP 400 with error code VALIDATION_ERROR on GET, lock, and unlock.
6. IF the inclusive day count from fromDate to toDate exceeds maxWindowDays THEN the system SHALL respond HTTP 400 with error code VALIDATION_ERROR.
7. WHILE two lock requests for the same window are processed concurrently THEN the system SHALL persist one lock, respond HTTP 200 to one request, and respond HTTP 409 with error code CONFLICT to the other.
8. WHILE two unlock requests for the same locked window are processed concurrently THEN the system SHALL leave one unlocked state, respond HTTP 200 to one request, and respond HTTP 409 with error code CONFLICT to the other.
9. WHILE a lock request and a mutation blocked by that lock are processed concurrently THEN the system SHALL commit exactly one of them and respond HTTP 409 with error code CONFLICT to the other.
10. IF persisting the lock fails THEN the system SHALL leave the window unlocked and emit no PERIOD_LOCKED event.

**Independent Test**: Call lock without a token and receive 401. Call as OPERATOR without grant and receive 403. Submit two concurrent locks and see one 200 and one 409.

---

## Edge Cases

- Janela sem run `COMPLETED` vigente: HTTP 404 (PER-09, PER-14).
- Run `PENDING` ou `RUNNING` na mesma janela: GET e lock respondem HTTP 409 (PER-10, PER-15).
- Run `FAILED` com um `COMPLETED` anterior ainda vigente: GET devolve esse `COMPLETED` (PER-08).
- `totalItems` zero: matchRate null, trava permitida, openAmount 0.00 (PER-03, PER-12).
- Divergência `OPEN`: lock responde HTTP 409 e a janela segue aberta (PER-13).
- Segunda trava: HTTP 409 e o `lockedAt` original permanece (PER-16).
- Reabertura sem trava: HTTP 409 (PER-19).
- Data igual a `fromDate` ou `toDate`: a mutação recebe HTTP 409 (PER-31).
- `settlementDate` no dia seguinte a `toDate`: a importação válida recebe HTTP 201 (PER-30).
- Arquivo inválido com uma data também travada: HTTP 400 e nada gravado (PER-29).
- Duas travas simultâneas: uma persiste, a outra recebe HTTP 409 (PER-41).
- Duas reaberturas simultâneas: uma persiste, a outra recebe HTTP 409 (PER-42).
- Falha ao gravar a trava: a janela segue aberta e não há audit (PER-44).

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| PER-01 | P1: Ler | Phase 3 | Verified |
| PER-02 | P1: Ler | Phase 3 | Verified |
| PER-03 | P1: Ler | Phase 3 | Verified |
| PER-04 | P1: Ler | Phase 2 | Implementing |
| PER-05 | P1: Ler | Phase 3 | Verified |
| PER-06 | P1: Ler | Phase 3 | Verified |
| PER-07 | P1: Ler | Phase 3 | Verified |
| PER-08 | P1: Ler | Phase 3 | Verified |
| PER-09 | P1: Ler | Phase 2 | Implementing |
| PER-10 | P1: Ler | Phase 3 | Verified |
| PER-11 | P1: Travar | Phase 3 | Verified |
| PER-12 | P1: Travar | Phase 3 | Verified |
| PER-13 | P1: Travar | Phase 2 | Implementing |
| PER-14 | P1: Travar | Phase 3 | Verified |
| PER-15 | P1: Travar | Phase 3 | Verified |
| PER-16 | P1: Travar | Phase 3 | Verified |
| PER-17 | P1: Travar | Phase 3 | Verified |
| PER-18 | P1: Reabrir | Phase 3 | Verified |
| PER-19 | P1: Reabrir | Phase 3 | Verified |
| PER-20 | P1: Reabrir | Phase 3 | Verified |
| PER-21 | P1: Reabrir | Phase 3 | Verified |
| PER-22 | P1: Segurar | Phase 4 | Verified |
| PER-23 | P1: Segurar | Phase 4 | Verified |
| PER-24 | P1: Segurar | Phase 4 | Verified |
| PER-25 | P1: Segurar | Phase 4 | Verified |
| PER-26 | P1: Segurar | Phase 4 | Verified |
| PER-27 | P1: Segurar | - | Pending |
| PER-28 | P1: Segurar | Phase 4 | Verified |
| PER-29 | P1: Segurar | Phase 4 | Verified |
| PER-30 | P1: Segurar | Phase 4 | Verified |
| PER-31 | P1: Segurar | Phase 4 | Verified |
| PER-32 | P1: Segurar | Phase 4 | Verified |
| PER-33 | P1: Segurar | - | Pending |
| PER-34 | P1: Segurar | Phase 4 | Verified |
| PER-35 | P1: Acesso | Phase 3 | Verified |
| PER-36 | P1: Acesso | Phase 3 | Verified |
| PER-37 | P1: Acesso | Phase 3 | Verified |
| PER-38 | P1: Acesso | Phase 3 | Verified |
| PER-39 | P1: Acesso | Phase 3 | Verified |
| PER-40 | P1: Acesso | Phase 3 | Verified |
| PER-41 | P1: Acesso | Phase 1 | Implementing |
| PER-42 | P1: Acesso | - | Pending |
| PER-43 | P1: Acesso | - | Pending |
| PER-44 | P1: Acesso | Phase 3 | Verified |

**Coverage:** 44 total, 8 mapped to tasks, 36 unmapped

---

## Success Criteria

- [ ] GET da janela vigente devolve taxa, valor em aberto e duração da trava com os arredondamentos acima
- [ ] Trava com `OPEN` ou sem run vigente não grava estado
- [ ] Com a janela travada, run novo dessa janela, divergência desse run e data inclusiva recebem 409 e não gravam
- [ ] Data fora de toda janela travada, inclusive o dia seguinte a `toDate`, segue a regra atual
