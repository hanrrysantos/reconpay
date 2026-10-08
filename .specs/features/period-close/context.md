# Period Close Context

**Gathered:** 2026-10-07
**Spec:** `.specs/features/period-close/spec.md`
**Status:** Ready for design

---

## Feature Boundary

O período é a janela exata do run (`merchantId` + `fromDate` + `toDate`). A API publica a taxa de match, o valor em aberto e o tempo até a trava, e trava essa janela quando o run vigente está `COMPLETED` e não há divergência `OPEN`. Frontend, mês civil e histórico de travas ficam de fora.

---

## Implementation Decisions

### O que é o período

- A unidade é a janela do run, não o mês civil e não um fechamento com datas próprias.
- A leitura e a trava usam o run vigente: `COMPLETED` com `supersededAt` nulo.
- Um run `FAILED` não substitui esse vigente.

### O que a trava impede

- Um POST de conciliação com o mesmo `fromDate` e `toDate`.
- O PATCH de divergência do run vigente dessa janela.
- Criar transação, mudar status de transação e importar liquidação ou extrato quando a data do lançamento cai em qualquer janela travada do merchant, `fromDate` e `toDate` inclusivos.
- Outro par de datas ainda pode conciliar, mesmo com sobreposição.
- Data depois de `toDate`, inclusive a cauda do atraso de liquidação, continua importável.
- Taxa e cadastro de merchant continuam livres.
- Arquivo ou corpo inválido responde `400` antes da regra da trava.

### Quando a trava vale

- Só com run vigente `COMPLETED`, zero divergências `OPEN` (venda e banco) e nenhum run `PENDING` ou `RUNNING` na mesma janela.
- OPERATOR com grant e ADMIN travam e reabrem.
- Recusa de trava, de reabertura indevida e de mutação travada responde `409 CONFLICT`.
- Travar de novo, ou reabrir o que já está aberto, responde `409`.
- Reabrir limpa `lockedAt`. Travar de novo grava outro instante e mede a duração a partir do `finishedAt` do run vigente.

### Quais números

- `matchRate` = `matchedCount / totalItems`, 4 casas, half up. Zero itens: null. Resolver divergência não muda a taxa.
- `openAmount` soma cada divergência `OPEN`, 2 casas, half up, pela tabela da spec. Status fora de `OPEN` não entra. Valor de correção não entra.
- `closeDurationSeconds` são os segundos inteiros de `finishedAt` até `lockedAt`, truncados em direção a zero. Sem trava, null.
- GET, trava e reabertura devolvem o mesmo corpo.
- Sem run `COMPLETED` vigente: `404`. Com `PENDING` ou `RUNNING` na janela: `409`.

### Agent's Discretion

- Ordem dos campos no JSON.
- Onde a trava fica persistida, desde que o contrato acima se mantenha.

### Declined / Undiscussed Gray Areas → Assumptions

Nenhuma. As quatro áreas foram decididas e estão na spec.

---

## Specific References

- Taxa congelada no run, valor em aberto pela tabela de tipos, duração em segundos.
- Trava de data por cobertura: uma data dentro de qualquer janela travada do merchant fica bloqueada.

---

## Deferred Ideas

- Frontend.
- Período em mês civil.
- Histórico de quem travou e quando, além do audit log.
- Travar a cauda do `settlement-lag-days`.
