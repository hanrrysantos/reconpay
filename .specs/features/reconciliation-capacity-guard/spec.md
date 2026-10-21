# Proteção de Capacidade da Conciliação Specification

## Problem Statement

O benchmark de volume (réplica com 2 vCPU e 1 GB para a API, como a produção, 2026-10-20) mostrou que a conciliação carrega a janela inteira na memória. Com ~100 mil vendas (~300 mil registros) ela termina em 17 s com 809 MB; com 300 mil vendas e com 1 milhão estoura o heap. Depois do `OutOfMemoryError` a API para de responder, inclusive o `/actuator/health`; com 300 mil vendas o run ainda fica preso em `RUNNING`, porque o erro mata a thread do worker. Qualquer `OPERATOR` consegue disparar isso importando arquivos de 5 MB até passar do limite e conciliando.

## Goals

- [ ] Uma janela grande demais é recusada antes de consumir memória, com uma mensagem que diz o que fazer.
- [ ] Um run grande demais que já está na fila termina `FAILED`, sem derrubar a API.
- [ ] Se a memória ainda assim estourar, a API volta sozinha em vez de ficar travada.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Conciliar em lotes (memória constante) | Feature estrutural separada; esta só protege o limite atual |
| Limite por importação ou por empresa | O que estoura é a janela conciliada, não o arquivo; a importação escalou até 3 milhões de linhas |
| Mudar o frontend | O `NewRunDrawer` e o painel já mostram a `message` do `400`, e as telas de run já mostram o `errorMessage` de um `FAILED` |
| Limite de reinícios do container | `ExitOnOutOfMemoryError` + a recusa na execução já quebram o ciclo de quedas |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Limite | 300.000 registros por janela | Decisão do usuário: maior volume que passou com folga no benchmark (100 mil vendas, 809 MB) | y |
| O que conta | Transações da janela + liquidações da janela estendida por `settlement-lag-days` + linhas de extrato da janela estendida | São as três listas que o worker carrega na memória | y |
| Onde recusa | No `POST` (antes de criar o run) e de novo quando o worker pega o run | A segunda checagem pega dados importados depois do pedido e runs devolvidos a `PENDING` após um reinício | y |
| Configuração | `reconpay.reconciliation.max-window-records`, variável `RECONCILIATION_MAX_WINDOW_RECORDS`, padrão 300000, mínimo 1 | Mesmo padrão das outras propriedades da conciliação | n |
| Status da recusa no `POST` | `400` `VALIDATION_ERROR`, como a janela maior que 366 dias | É erro do pedido, e o frontend já trata | n |
| Mensagem | "A janela tem {total} registros para conciliar; o limite é {limite}. Divida o período em janelas menores." com números em pt-BR (ponto de milhar) | Diz o tamanho, o limite e o que fazer | n |
| Exatamente no limite | 300.000 registros é aceito; 300.001 é recusado | "Limite" é o máximo permitido | n |
| Recusa na execução | O run termina `FAILED` com a mesma mensagem em `errorMessage`, `finishedAt` preenchido, sem itens | Mensagem útil em vez da genérica, e nenhuma lista carregada | n |
| Rede de segurança | `-XX:+ExitOnOutOfMemoryError` no `ENTRYPOINT` do `backend/Dockerfile` | O Compose já reinicia com `restart: unless-stopped` | n |
| Auditoria | A recusa na execução registra `RECONCILIATION_REJECTED` no audit log com o total e o limite | Igual aos outros eventos do run | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Recusar a janela grande demais no pedido ⭐ MVP

**User Story**: Como operador, quero saber na hora que a janela é grande demais e como resolver, em vez de ver a conciliação falhar minutos depois.

**Why P1**: Impede que o pedido chegue a consumir memória.

**Acceptance Criteria**:

1. IF a soma de transações da janela, liquidações da janela estendida e linhas de extrato da janela estendida passa de `max-window-records` THEN `POST /api/merchants/{id}/reconciliations` SHALL responder `400` com código `VALIDATION_ERROR` e a mensagem "A janela tem {total} registros para conciliar; o limite é {limite}. Divida o período em janelas menores.", com os dois números formatados em pt-BR.
2. IF o pedido é recusado pelo limite THEN o sistema SHALL NOT criar run.
3. WHEN a soma é igual a `max-window-records` THEN o sistema SHALL aceitar o pedido com `202`.
4. The system SHALL contar só registros da empresa do pedido.
5. The system SHALL ler o limite de `reconpay.reconciliation.max-window-records`, com padrão 300000.

**Independent Test**: Com o limite baixado para 10 e 11 registros na janela, o `POST` responde `400` com a mensagem e nenhum run aparece; com 10 registros, responde `202`.

---

### P1: Falhar de forma limpa na execução ⭐ MVP

**User Story**: Como dono da instalação, quero que um run grande demais que já está na fila termine `FAILED` sem derrubar a API.

**Why P1**: Fecha o ciclo em que o run volta a `PENDING` depois de um reinício e estoura a memória de novo.

**Acceptance Criteria**:

1. WHEN o worker pega um run cuja janela passa de `max-window-records` THEN o sistema SHALL marcar o run `FAILED`, com `errorMessage` igual à mensagem do pedido e `finishedAt` preenchido.
2. WHEN o run é recusado na execução THEN o sistema SHALL NOT gravar itens nem carregar as transações, liquidações e linhas de extrato da janela.
3. WHEN o run é recusado na execução THEN o sistema SHALL registrar `RECONCILIATION_REJECTED` no audit log com o total e o limite.
4. WHEN um run dentro do limite é processado THEN o sistema SHALL concluir como hoje (`COMPLETED`).

**Independent Test**: Criar um run dentro do limite, importar mais registros na janela até passar dele e deixar o worker pegar o run: ele termina `FAILED` com a mensagem e zero itens.

---

### P1: Reiniciar em vez de travar ⭐ MVP

**User Story**: Como dono da instalação, quero que a API reinicie sozinha se a memória estourar, em vez de ficar travada até alguém intervir.

**Why P1**: Hoje um `OutOfMemoryError` deixa o processo vivo e sem responder; o Docker não reinicia container só por estar "unhealthy".

**Acceptance Criteria**:

1. The `backend/Dockerfile` SHALL iniciar a JVM com `-XX:+ExitOnOutOfMemoryError`, mantendo `-XX:MaxRAMPercentage=75.0`.

**Independent Test**: Na réplica do benchmark, conciliar uma janela acima do limite com o limite desligado (`RECONCILIATION_MAX_WINDOW_RECORDS` alto): o container sai, o Docker o reinicia e o `/actuator/health` volta a `UP`.

---

## Edge Cases

- WHEN a janela não tem registros THEN o sistema SHALL aceitar o pedido (0 ≤ limite), como hoje.
- IF outra empresa tem mais registros que o limite na mesma janela THEN o pedido desta empresa SHALL ser aceito.
- WHEN liquidações e linhas de extrato caem nos `settlement-lag-days` depois do fim da janela THEN elas SHALL entrar na contagem.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| CAP-01 | P1: Recusar a janela grande demais no pedido | Execute | Implemented |
| CAP-02 | P1: Falhar de forma limpa na execução | Execute | Implemented |
| CAP-03 | P1: Reiniciar em vez de travar | Execute | Implemented |

**Coverage:** 3 total, 3 implemented (inline steps, no tasks.md)

---

## Success Criteria

- [ ] Na réplica do benchmark, conciliar 300 mil vendas responde `400` com a mensagem em vez de travar a API.
- [ ] Depois de um estouro forçado, a API volta a `UP` sem intervenção.
