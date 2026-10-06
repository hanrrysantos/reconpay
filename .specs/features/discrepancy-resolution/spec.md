# Discrepancy Resolution Specification

## Problem Statement

O run de conciliação aponta a divergência e para aí. O operador não tem como aceitar, ajustar, baixar ou reabrir esse item dentro da API. O líquido da venda e o snapshot do run precisam continuar auditáveis enquanto a decisão fica registrada.

## Goals

- [ ] OPERATOR com grant, e ADMIN, conduzem uma divergência do run vigente até Aceita, Ajustada ou Baixada, e podem reabrir para Aberta
- [ ] Ajustada grava um lançamento de correção anulável, sem alterar a transação, o snapshot, o CSV nem o batimento do run seguinte

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Frontend | Épico seguinte, depois das três fatias de API |
| Layouts de adquirente e extrato bancário | Feature seguinte |
| Indicadores e trava de período | Feature seguinte |
| Owner, convite, refresh de JWT, rate limit e fila externa | Fora desta leva |
| Run seguinte consumir o lançamento de correção | O lançamento não entra no batimento |
| Colunas novas no CSV de conciliação | O export atual permanece o contrato de auditoria |
| Alterar valor, status ou snapshot da transação ao resolver | A decisão não reescreve o passado |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Desfechos | OPEN, ACCEPTED, ADJUSTED, WRITTEN_OFF, com reabertura para OPEN | Quatro estados decididos; em análise não é status | y |
| Transição entre desfechos terminais | Só via OPEN; o mesmo status de novo responde 409 | Evita pular de Aceita para Ajustada | y |
| Efeito no dinheiro | Ajustada grava lançamento ligado à divergência; não muda transação nem snapshot | Líquido antigo permanece auditável | y |
| Reabrir Ajustada | Anula o lançamento ativo e mantém a linha com voidedAt | Histórico da correção não some | y |
| Run que aceita mudança | Só COMPLETED com supersededAt nulo; os demais respondem 409 | Um run vigente por janela | y |
| Quem age | OPERATOR com grant no merchant; ADMIN em qualquer merchant | Mesmo modelo de AD-001 | y |
| Nota | Opcional, no máximo 500 caracteres; em branco vira null | Comentário não é obrigatório | y |
| correctionAmount | Obrigatório só em ADJUSTED; com sinal, não zero, no máximo 17 inteiros e 2 decimais; presente nos outros alvos responde 400 | Diferença pode subir ou descer; teto igual ao dinheiro já aceito na API | y |
| Concorrência | O segundo update simultâneo responde 409 CONFLICT; o cliente não envia version | Lock otimista já usado em transação | y |
| Falha parcial | Status, lançamento e histórico commitam juntos; se um falha, nada muda | Não pode ficar Ajustada sem lançamento | y |
| Leitura | GET e PATCH devolvem a mesma representação, com histórico; a lista de items traz id e status, sem histórico | A página de items não carrega o histórico inteiro | y (agent) |
| Nomes na API | Enums em inglês, como os tipos de divergência atuais | Consistência do contrato | y (agent) |
| Divergências já gravadas | Migram como OPEN, sem histórico | Não há desfecho anterior para inventar | y (agent) |
| Auditoria | Evento DISCREPANCY_STATUS_CHANGED só depois do commit | Mesma regra dos outros eventos de sucesso | y (agent) |
| Dependência externa | N/A | Esta feature não chama serviço externo | y |
| Expiração | N/A | Histórico e lançamento anulado permanecem com a divergência | y |
| Rate limit neste endpoint | N/A | Limite de login ficou fora desta leva | y |

**Open questions:** none

---

## User Stories

### P1: Fechar a divergência no run vigente ⭐ MVP

**User Story**: As an operator, I want to accept, adjust, or write off an open discrepancy on the current run so that the decision is recorded without rewriting the sale.

**Why P1**: Sem desfecho, a divergência continua só um apontamento.

**Acceptance Criteria**:

1. WHEN a reconciliation run stores a new discrepancy THEN the system SHALL set its status to OPEN, with no adjustment and no history entry.
2. WHEN an authenticated OPERATOR with a grant for merchantId submits ACCEPTED or WRITTEN_OFF on PATCH `/api/merchants/{merchantId}/reconciliations/{runId}/discrepancies/{discrepancyId}` for an OPEN discrepancy of a COMPLETED run with null supersededAt, with a note of at most 500 characters and without correctionAmount THEN the system SHALL respond HTTP 200, set that status, append one history entry, and leave the internal transaction and the item snapshot unchanged.
3. WHEN that OPERATOR submits ADJUSTED on the same PATCH for an OPEN discrepancy of that run, with a non-zero correctionAmount of at most 17 integer digits and 2 decimal places and a note of at most 500 characters THEN the system SHALL respond HTTP 200, set status ADJUSTED, store one non-voided adjustment with that amount, the actor user id, and a timestamp, append one history entry, and leave the internal transaction and the item snapshot unchanged.
4. WHEN an authenticated ADMIN with no user_merchants row submits a valid transition on that PATCH THEN the system SHALL apply the same outcome as an OPERATOR with grant.
5. The system SHALL emit audit event DISCREPANCY_STATUS_CHANGED, with the discrepancy id, previous status, and new status, only after the transition commits.
6. The system SHALL compute each run's match results from internal transactions and external settlements alone.

**Independent Test**: Complete a run, PATCH a discrepancy to ACCEPTED and another to ADJUSTED, then read the transaction and the item snapshot unchanged.

---

### P1: Reabrir

**User Story**: As an operator, I want to reopen a closed discrepancy so that a wrong decision can return to open and an adjustment is voided.

**Why P1**: A decisão errada não pode ficar presa, e o lançamento não pode continuar ativo depois da reabertura.

**Acceptance Criteria**:

1. WHEN an authorized actor submits OPEN on that PATCH for a discrepancy in ACCEPTED or WRITTEN_OFF on a current COMPLETED run THEN the system SHALL respond HTTP 200, set status OPEN, append one history entry, and create no adjustment.
2. WHEN an authorized actor submits OPEN on that PATCH for a discrepancy in ADJUSTED on that run THEN the system SHALL respond HTTP 200, set status OPEN, set voidedAt on the active adjustment, leave that adjustment readable, leave no other active adjustment, and append one history entry.
3. IF the target status equals the current status, or both the current status and the target status are in ACCEPTED, ADJUSTED, WRITTEN_OFF THEN the system SHALL respond HTTP 409 with error code CONFLICT and persist no change.

**Independent Test**: Adjust a discrepancy, reopen it, and see status OPEN with the adjustment voided. PATCH ACCEPTED to ADJUSTED and receive 409.

---

### P1: Validar a entrada

**User Story**: As an operator, I want invalid notes and amounts rejected so that a bad payload does not change the discrepancy.

**Why P1**: O valor da correção e o tamanho da nota são parte do contrato.

**Acceptance Criteria**:

1. IF the note has more than 500 characters THEN the system SHALL respond HTTP 400 with error code VALIDATION_ERROR and persist no change.
2. IF the target status is ADJUSTED and correctionAmount is absent, zero, has more than 2 decimal places, or has more than 17 integer digits THEN the system SHALL respond HTTP 400 with error code VALIDATION_ERROR and persist no change.
3. IF the target status is ACCEPTED, WRITTEN_OFF, or OPEN and correctionAmount is present THEN the system SHALL respond HTTP 400 with error code VALIDATION_ERROR and persist no change.
4. WHEN the note is empty or whitespace and the transition is otherwise valid THEN the system SHALL store the history note as null.

**Independent Test**: PATCH ADJUSTED without amount, and ACCEPTED with amount, and see HTTP 400 with the discrepancy still OPEN.

---

### P1: Guardas de acesso, run e concorrência

**User Story**: As an operator, I want changes refused outside my merchant and outside the current completed run so that old runs and other merchants stay intact.

**Why P1**: O snapshot de um run substituído é histórico.

**Acceptance Criteria**:

1. WHILE the run status is not COMPLETED or supersededAt is not null, WHEN a client submits a status change THEN the system SHALL respond HTTP 409 with error code CONFLICT and persist no change.
2. IF the caller is unauthenticated THEN the system SHALL respond HTTP 401 and persist no change.
3. IF an authenticated OPERATOR has no grant for merchantId THEN the system SHALL respond HTTP 403 with error code FORBIDDEN and persist no change.
4. IF no discrepancy exists for that merchantId, runId, and discrepancyId THEN the system SHALL respond HTTP 404 with error code NOT_FOUND.
5. WHILE two status updates for the same discrepancy are processed concurrently THEN the system SHALL persist one transition and respond HTTP 409 with error code CONFLICT to the other.
6. IF persisting the adjustment or the history entry fails THEN the system SHALL leave the previous status unchanged.

**Independent Test**: PATCH a superseded run and receive 409. Call without a token and receive 401. Call as OPERATOR without grant and receive 403.

---

### P1: Ler o desfecho

**User Story**: As an operator, I want to read the current status, the adjustments, and the history so that the recorded decision is visible after the PATCH.

**Why P1**: Gravar o histórico sem devolvê-lo não fecha o fluxo.

**Acceptance Criteria**:

1. WHEN an authorized client GETs `/api/merchants/{merchantId}/reconciliations/{runId}/discrepancies/{discrepancyId}` THEN the system SHALL respond HTTP 200 with the status, every adjustment amount and voided flag, and history in ascending timestamp order, each entry with actor user id, previous status, new status, note, and timestamp.
2. WHEN an authorized client GETs `/api/merchants/{merchantId}/reconciliations/{runId}/items` THEN each discrepancy SHALL include its id and current status and SHALL omit transition history.
3. WHEN a transition returns HTTP 200 THEN the response body SHALL match the GET representation of that discrepancy.
4. The system SHALL omit resolution status and correction amount from the reconciliation CSV export.

**Independent Test**: PATCH a discrepancy, GET it, and see the same status and one history entry. Export the CSV and see no resolution column.

---

## Edge Cases

- Nota acima de 500 caracteres, amount ausente ou zero em ADJUSTED, e amount presente em Aceita, Baixada ou reabertura: HTTP 400, estado intacto (RES-04, RES-05, RES-06).
- Mesmo status, ou salto entre Aceita, Ajustada e Baixada: HTTP 409 (RES-10).
- Run PENDING, RUNNING, FAILED ou COMPLETED já substituído: HTTP 409 (RES-11).
- Divergência de outro merchant ou de outro run: HTTP 404 (RES-14).
- Dois updates simultâneos: um persiste, o outro recebe 409 (RES-15).
- Falha ao gravar lançamento ou histórico: status anterior permanece (RES-16).

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| RES-01 | P1: Fechar | Execute | Verified |
| RES-02 | P1: Fechar | Execute | Verified |
| RES-03 | P1: Fechar | Execute | Verified |
| RES-17 | P1: Fechar | Execute | Verified |
| RES-22 | P1: Fechar | Execute | Verified |
| RES-23 | P1: Fechar | Execute | Verified |
| RES-08 | P1: Reabrir | Execute | Verified |
| RES-09 | P1: Reabrir | Execute | Verified |
| RES-10 | P1: Reabrir | Execute | Verified |
| RES-04 | P1: Validar | Execute | Verified |
| RES-05 | P1: Validar | Execute | Verified |
| RES-06 | P1: Validar | Execute | Verified |
| RES-07 | P1: Validar | Execute | Verified |
| RES-11 | P1: Guardas | Execute | Verified |
| RES-12 | P1: Guardas | Execute | Verified |
| RES-13 | P1: Guardas | Execute | Verified |
| RES-14 | P1: Guardas | Execute | Verified |
| RES-15 | P1: Guardas | Execute | Verified |
| RES-16 | P1: Guardas | Execute | Verified |
| RES-18 | P1: Ler | Execute | Verified |
| RES-19 | P1: Ler | Execute | Verified |
| RES-20 | P1: Ler | Execute | Verified |
| RES-21 | P1: Ler | Execute | Verified |

**Coverage:** 23 total, 23 mapped to tasks, 0 unmapped

---

## Success Criteria

- [ ] OPERATOR com grant aceita, ajusta, baixa e reabre uma divergência do run vigente concluído
- [ ] Ajustada grava o valor informado; reabrir anula esse lançamento e o mantém legível
- [ ] Transação, snapshot do item, CSV e o cálculo do run seguinte permanecem sem o desfecho
- [ ] Payload inválido, transição ilegal, run fora do vigente, falta de grant e o update perdedor não alteram o estado
