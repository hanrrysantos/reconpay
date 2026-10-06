# Discrepancy Resolution Context

**Gathered:** 2026-10-06
**Spec:** `.specs/features/discrepancy-resolution/spec.md`
**Status:** Ready for design

---

## Feature Boundary

O OPERATOR no merchant concedido, e o ADMIN em qualquer merchant, conduzem a divergência de um run vigente e concluído até um desfecho, e podem reabrir. Esta feature não muda importação, motor de batimento, indicadores, CSV nem tela.

---

## Implementation Decisions

### Desfechos

- Status da API: `OPEN`, `ACCEPTED`, `ADJUSTED`, `WRITTEN_OFF`.
- Toda divergência nova nasce `OPEN`.
- De `OPEN` o ator vai para `ACCEPTED`, `ADJUSTED` ou `WRITTEN_OFF`.
- Desses três, a única saída é voltar para `OPEN`.
- Repetir o status atual, ou saltar de um terminal para outro, responde HTTP 409 `CONFLICT`.
- Não existe status "em análise".

### Efeito no dinheiro

- `ADJUSTED` grava um lançamento ligado à divergência: valor informado pelo operador, usuário ator e instante.
- O valor tem sinal, é diferente de zero, e aceita no máximo 17 dígitos inteiros e 2 casas decimais.
- `ACCEPTED`, `WRITTEN_OFF` e a reabertura rejeitam `correctionAmount` com HTTP 400 `VALIDATION_ERROR`.
- A transação interna e o snapshot do item não mudam.
- O run seguinte não lê esse lançamento.

### Reabertura

- Reabrir `ACCEPTED` ou `WRITTEN_OFF` só volta para `OPEN` e acrescenta histórico.
- Reabrir `ADJUSTED` anula o lançamento ativo (`voidedAt` preenchido), mantém a linha legível e não deixa outro lançamento ativo.
- Um novo `ADJUSTED` depois da reabertura cria outro lançamento ativo.

### Run e quem age

- Só um run `COMPLETED` com `supersededAt` nulo aceita PATCH. `PENDING`, `RUNNING`, `FAILED` e run já substituído respondem HTTP 409 `CONFLICT`.
- OPERATOR precisa de grant no merchant. Sem grant: HTTP 403 `FORBIDDEN`.
- ADMIN age sem linha em `user_merchants`.
- Sem autenticação: HTTP 401.
- Divergência fora do merchant e do run: HTTP 404 `NOT_FOUND`.

### Contrato de leitura e nota

- PATCH e GET usam `/api/merchants/{merchantId}/reconciliations/{runId}/discrepancies/{discrepancyId}`.
- HTTP 200 devolve a mesma representação: status, lançamentos (valor e flag de anulado) e histórico em ordem cronológica (ator, status anterior, status novo, nota, instante).
- A lista de items inclui `id` e status de cada divergência, sem o histórico.
- Nota opcional, até 500 caracteres. Vazia ou só espaços fica null.
- Dois updates simultâneos: um persiste, o outro recebe HTTP 409. O cliente não envia versão.
- Status, lançamento e histórico commitam na mesma transação.
- Auditoria `DISCREPANCY_STATUS_CHANGED` só depois do commit.
- O CSV de conciliação não ganha status nem valor de correção.

### Agent's Discretion

- Forma do corpo JSON, desde que os campos sejam `status`, `note` e `correctionAmount`.
- Se o histórico da listagem fica de fora, como já está decidido. O detalhe é que carrega o histórico.

### Declined / Undiscussed Gray Areas → Assumptions

Nenhuma das quatro áreas ficou de fora. Premissas de contrato (nota, teto do valor, concorrência, histórico legível) foram aceitas junto com a criação desta spec.

---

## Specific References

Nenhum produto externo citado. O comportamento encosta no que a API já faz: 409 para conflito, grant por merchant, e snapshot de run que não é reescrito.

---

## Deferred Ideas

- Frontend, depois das fatias de API.
- Layouts de adquirente e extrato bancário.
- Indicadores de fechamento e trava de período.
- Owner, convite, refresh de JWT, rate limit e fila para mais de uma instância.
- Fazer o run seguinte usar o lançamento de correção no batimento.
- Incluir desfecho no CSV.
