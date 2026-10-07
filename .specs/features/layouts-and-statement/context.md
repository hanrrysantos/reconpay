# Layouts and Statement Context

**Gathered:** 2026-10-07
**Spec:** `.specs/features/layouts-and-statement/spec.md`
**Status:** Ready for design

---

## Feature Boundary

O operador importa o arquivo do adquirente e o extrato do banco. Os dois entram na conciliação, com o mesmo isolamento por merchant. Indicadores de período e o frontend ficam de fora.

---

## Implementation Decisions

### Receitas de liquidação

- O CSV atual continua válido.
- Existem duas receitas fixas no código: `RECONPAY` e `ACQUIRER`.
- No mesmo envio vão o arquivo e o nome da receita.
- Sem o nome, a receita é `RECONPAY`.
- Se o cabeçalho não bater com a receita, a API recusa o arquivo e não grava nada.
- O operador não cadastra receita nova.

### Extrato

- O extrato é outro envio, com uma receita fixa só dele.
- Cada linha guarda referência da venda (opcional), valor, data e uma referência própria da linha.
- A conciliação cruza o extrato com a liquidação: é o dinheiro que caiu na conta.

### Arquivo inválido

- Um erro em qualquer linha descarta o arquivo inteiro.
- Nada daquele envio fica gravado.
- A mesma regra vale para a liquidação e para o extrato.

### Casamento

- Se a linha do banco traz o código da venda, ela liga na liquidação desse código.
- Se o código bate e o valor não, a linha fica divergente.
- Se a linha não traz o código, ela liga na liquidação do mesmo valor líquido na mesma data.
- Se nenhuma liquidação servir, ou se mais de uma servir, ninguém é escolhido. Os dois lados ficam divergentes.
- Uma linha do banco liga em no máximo uma liquidação.
- Uma liquidação sem linha do banco também fica divergente.
- Depósito que soma várias vendas fica fora desta feature.

### Agent's Discretion

- Nomes das colunas das receitas `ACQUIRER` e do extrato.
- Nome do parâmetro `layout` e o valor padrão `RECONPAY` quando ele falta.
- Tipos novos de divergência em inglês, no mesmo enum dos tipos atuais.
- Janela do extrato igual à janela estendida da liquidação, e comparação de valor com `amount-tolerance`.

### Declined / Undiscussed Gray Areas → Assumptions

Nenhuma das decisões de produto ficou em aberto. Os detalhes de contrato que não foram perguntados estão na tabela de premissas da spec.

---

## Specific References

Nenhum produto externo citado. O exemplo usado na conversa foi coluna `NSU` e `valor_liquido` no lugar dos nomes do CSV atual.

---

## Deferred Ideas

- Depósito único somando várias vendas.
- Receita montada pelo operador.
- OFX, CNAB e outros adquirentes além de `ACQUIRER`.
- Indicadores de fechamento e trava de período.
- Frontend.
- Owner, convite, refresh de JWT, rate limit e fila para mais de uma instância.
