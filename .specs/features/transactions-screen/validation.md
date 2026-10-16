# Tela de Transações Validation

**Date**: 2026-10-16
**Spec**: `.specs/features/transactions-screen/spec.md`
**Diff range**: `330d2a9^..1f00b32` (13 commits), branch `feat/transactions-screen`. Iteração 1 cobriu `330d2a9^..ac10fb2`; o relatório dela entrou em `543c091`; a fix de teste é `1f00b32`.
**Verifier**: sub-agente independente (author ≠ verifier), iteração 2 de 3.

## Validation: transactions-screen - PASS ✅

Os dois gaps da iteração 1 estão fechados. M25 e M31 agora morrem. Os 7 mutantes novos também morrem. O gate passa: 656 testes, typecheck e build. O código não mudou desde a iteração 1, então as outras 23 ACs e os 30 mutantes mortos seguem valendo.

Abreviações: as mesmas da iteração 1 (abaixo). `TPT` = `frontend/components/transactions/TransactionsPage.test.tsx`.

---

## Iteração 2

### Fix `1f00b32`

- Toca só `frontend/components/transactions/TransactionsPage.test.tsx` (+15/−4). Nenhum código de produção mudou.
- Nada foi enfraquecido. A linha `getByRole('heading', {level:1, name:'Transações'})` saiu, mas o mesmo `getByRole` roda dentro de `pageHeader()` (`TPT:113`) e lança erro se o título faltar. `getAllByText(...).length > 0` virou `within(pageHeader()).getByText(...)`, mais forte. `toMatchObject` na consulta de `m-2` virou `toEqual` com todos os parâmetros, mais forte.
- O teste de troca acrescenta `TPT:265` (aviso ainda visível antes da troca). Isso mantém a prova de que a troca remove o aviso.

### ACs re-checadas

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| TXN-01 AC1 nome da empresa no cabeçalho | O cabeçalho da página mostra o nome da empresa selecionada | `TPT:113` `pageHeader = () => getByRole('heading',{level:1,name:'Transações'}).parentElement!`; `TPT:124` `within(pageHeader()).getByText('Alfa Pagamentos')).toBeInTheDocument()`; `TPT:281` `within(pageHeader()).getByText('Beta Comércio')` depois da troca. O bloco é o `div` de `TransactionsPage.tsx:75-78`, que não contém o `MerchantSwitcher`. M25 e F2 morrem | ✅ PASS |
| TXN-11 AC1 mantém período e filtros na troca | Período, status e método continuam; página 1 da nova empresa | `TPT:260-264` muda período para Setembro de 2026, status para Aprovada e método para Crédito antes da troca, e confirma `{fromDate:'2026-09-01',status:'APPROVED',paymentMethod:'CREDIT_CARD'}`; `TPT:275-277` consulta de `m-2` `toEqual({fromDate:'2026-09-01',toDate:'2026-09-30',status:'APPROVED',paymentMethod:'CREDIT_CARD',page:'0',size:'20',sort:'transactionDate,desc'})`; `TPT:278` botão `/Setembro de 2026/`; `TPT:279` Status `'APPROVED'`; `TPT:280` Método `'CREDIT_CARD'`. M31, M31a e M31b morrem | ✅ PASS |

**Status**: ✅ 25/25 ACs com evidência que discrimina (23 da iteração 1 sem mudança de código ou teste fora de `TPT`, mais as 2 re-checadas).

### Discrimination Sensor (iteração 2)

Scratch: `git worktree add --detach` em `scratchpad/sensor2` a partir de `1f00b32`, com `frontend/node_modules` por symlink. Cada mutante passou por `tsc --noEmit` (todos compilam) e rodou `vitest run` sobre os mesmos arquivos em escopo (107 testes). Revertido com `git checkout` no scratch.

| # | File | Mutação | Killed? | Testes que falharam |
| - | ---- | ------- | ------- | ------------------- |
| M25 | `TransactionsPage.tsx:76` | remove `{merchant?.name}` do cabeçalho | ✅ Killed | `TPT` TXN-01, `TPT` troca (TXN-11) |
| M31 | `TransactionsPage.tsx:54` | na troca, `setPeriod(monthWindow(new Date()))` e `setMethod('')` | ✅ Killed | `TPT` troca |
| M31a | `TransactionsPage.tsx:54` | na troca, só `setPeriod(monthWindow(new Date()))` | ✅ Killed | `TPT` troca |
| M31b | `TransactionsPage.tsx:54` | na troca, só `setMethod('')` | ✅ Killed | `TPT` troca |
| F1 | `TransactionsPage.tsx:58` | `filters` sem `paymentMethod` | ✅ Killed | `TPT` método, `TPT` troca |
| F2 | `TransactionsPage.tsx:76` | cabeçalho mostra `merchantId` em vez do nome | ✅ Killed | `TPT` TXN-01, `TPT` troca |
| F3 | `TransactionsPage.tsx:107` | `onChangeStatus` vira no-op (ação não abre o diálogo) | ✅ Killed | `TPT` TXN-09, `TPT` troca |
| F4 | `TransactionsPage.tsx:68` | `onCreated` não fecha a gaveta | ✅ Killed | `TPT` TXN-07, `TPT` troca |
| F5 | `TransactionsPage.tsx:102` | filtro de método sem `setPage(0)` | ✅ Killed | `TPT` método |

**Sensor depth**: ampliado (2 re-execuções + 2 variantes de M31 + 5 novos).
**Result**: 9/9 killed - PASS ✅

**Isolamento**: `git status --porcelain` antes = vazio; depois da remoção do scratch = vazio (igual ao baseline). Scratch removido com `git worktree remove --force` + `git worktree prune`; `git worktree list` não lista mais o scratch. Nenhum `git stash`.

### Gate Check (iteração 2)

- **Gate command**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`
- **Result**: 50 arquivos, 656 passed, 0 failed, 0 skipped. Typecheck exit 0. Build exit 0, `/transacoes` estática.
- **Test count**: 656, igual à iteração 1 (a fix só mudou asserções em testes existentes).

### Lessons

L-035 e L-036 seguem como candidatas. `lessons.md` só promove por recorrência em outra feature; a fix não dispara promoção. Nenhuma lição nova: a iteração 2 é PASS limpo.

### Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| TXN-01 | ❌ Needs Fix | ✅ Verified |
| TXN-02 .. TXN-10 | ✅ Verified | ✅ Verified |
| TXN-11 | ❌ Needs Fix | ✅ Verified |

**Docs desatualizados (reportados, não editados)**: `spec.md:169-181` ainda marca TXN-01..TXN-11 como "Design | Pending" e diz "0 mapped to tasks". `tasks.md:12` está com Status "In Progress". O orquestrador deve marcar TXN-01..TXN-11 como Verified e o Status como Done.

### Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 25/25 ACs e 5/5 edge cases com o valor da spec
**Sensor**: 9/9 killed na iteração 2 (30/32 na iteração 1; os 2 sobreviventes agora morrem)
**Gate**: 656 passed, typecheck e build OK

---

# Histórico: iteração 1

### Veredito da iteração 1: reprovada

O gate passa: 656 testes, typecheck e build. 30 dos 32 mutantes morreram. Dois sobreviveram, e os dois apontam para partes de AC sem teste que discrimine:

1. TXN-01 AC1: o nome da empresa no cabeçalho da página. Remover o nome não quebra nenhum teste (M25).
2. TXN-11 AC1: "manter período e filtros" na troca de empresa. O teste só cobre o status. Voltar o período para o mês atual e zerar o método na troca não quebra nenhum teste (M31).

O código atende a spec nos dois pontos. A falha é dos testes. São duas fix tasks pequenas, só de teste (ver "Fix Plans").

Abreviações:

- `ATT` = `frontend/lib/api.transactions.test.ts`
- `TT` = `frontend/lib/transactions.test.ts`
- `QTT` = `frontend/lib/queries.transactions.test.tsx`
- `TLT` = `frontend/components/transactions/TransactionList.test.tsx`
- `TDT` = `frontend/components/transactions/TransactionDrawer.test.tsx`
- `SDT` = `frontend/components/transactions/StatusDialog.test.tsx`
- `TPT` = `frontend/components/transactions/TransactionsPage.test.tsx`
- `PMT` = `frontend/components/dashboard/PeriodMenu.test.tsx`
- `CDT` = `frontend/components/ui/ConfirmDialog.test.tsx`
- `SDRT` = `frontend/components/ui/SideDrawer.test.tsx`

---

### Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 menu de período sem runs | ✅ Done | `PeriodMenu.tsx`: `months?`, `runs?`, `locks?`. Sem `runs`, sem marcas e sem nota de liquidação |
| T2 diálogo e gaveta com conteúdo extra | ✅ Done | `ConfirmDialog` `children` + `confirmDisabled`; `SideDrawer` `errorAction` |
| T3 chamadas da API | ✅ Done | `getTransactionPage`, `createTransaction` (`LosslessNumber`), `updateTransactionStatus` |
| T4 regras puras | ✅ Done | `lib/transactions.ts` |
| T5 hooks | ✅ Done | `queryKeys.transactions`, `useTransactionPage`, `useInvalidateTransactions`, `useCreateTransaction`, `useUpdateTransactionStatus` |
| T6 lista | ✅ Done | `TransactionList.tsx` |
| T7 gaveta | ✅ Done | `TransactionDrawer.tsx` |
| T8 diálogo de status | ✅ Done | `StatusDialog.tsx` |
| T9 página + rota | ✅ Done | `TransactionsPage.tsx`, `app/(app)/transacoes/page.tsx` |
| T10 README | ✅ Done | `README.md`, `frontend/README.md` |

`tasks.md` não tem nenhum item `- [ ]` aberto.

**Remoção em `UnderConstruction.test.tsx` (`52ace80`): legítima.** A rota `/transacoes` deixou de renderizar o placeholder. A linha `['/transacoes', 'Transações', TransacoesPage]` falharia por comportamento correto. A rota continua coberta: `TPT:5` importa `@/app/(app)/transacoes/page` e `TPT:118-130` verifica título, lista e consulta. As outras três rotas placeholder seguem no teste.

**T1/T2 aditivos: confirmado.** `git diff --numstat` mostra `PMT` +55/-0, `CDT` +29/-0 e `SDRT` +24/-1. A única linha removida em `SDRT` é o import, que ganhou `within`. Os testes do dashboard, de Empresas e de Regras de taxa não mudaram (`git diff --quiet` sobre `components/dashboard/*.test.tsx` exceto `PMT`, `components/merchants` e `components/fee-rules`). Nenhuma asserção foi enfraquecida.

---

### Spec-Anchored Acceptance Criteria

### P1: Ver as transações da empresa (TXN-01..TXN-04)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 busca inicial, título e empresa | `GET /transactions?fromDate=<1º do mês>&toDate=<último>&page=0&size=20&sort=transactionDate,desc`; título "Transações"; nome da empresa | `TPT:126-129` `path).toBe('/api/merchants/m-1/transactions')`, `query).toEqual({fromDate:'2026-10-01',toDate:'2026-10-31',page:'0',size:'20',sort:'transactionDate,desc'})`; `ATT:34-35` URL exata; `TPT:122` `heading level 1 'Transações'`. Nome: `TPT:123` `getAllByText('Alfa Pagamentos').length).toBeGreaterThan(0)`. O `MerchantSwitcher` renderizado no teste também mostra esse nome, então a asserção passa sem o nome na página (M25 sobreviveu) | ❌ GAP (nome da empresa) |
| AC2 colunas e formatos | Data, Referência, Método, Parcelas, Bruto, Líquido esperado, Status; `dd/mm/aaaa`, `3x`, `R$ 1.234,50`, rótulos | `TLT:65-66` `columnheader...toEqual(['Data','Referência','Método','Parcelas','Bruto','Líquido esperado','Status'])`; `TLT:68-69` `['03/10/2026','VENDA-001','Crédito','3x','R$ 300,00','R$ 290,73','Aprovada']` e `['01/10/2026','VENDA-002','Pix','1x','R$ 1.234,50','R$ 1.200,07','Estornada']`; `TT:124` os 4 rótulos; `TT:132` `'03/10/2026'` | ✅ PASS |
| AC3 mês ou intervalo refaz a busca e volta à página 1 | Novo `fromDate`/`toDate`, `page=0`; 12 meses (atual a 11 atrás) | `TPT:139-141` 12 meses, `Outubro de 2026` … `Novembro de 2025`; `TPT:145` `toMatchObject({fromDate:'2026-09-01',toDate:'2026-09-30',page:'0'})` vindo da página 2; `TPT:160` `{fromDate:'2026-09-10',toDate:'2026-10-04',page:'0'}`; `TT:91-94`; `PMT:108,127` | ✅ PASS |
| AC4 intervalo invertido | "A data inicial deve ser igual ou anterior à final" no menu, sem busca | `TPT:173-174` `alert` no `dialog 'Período'` com o texto exato; `TPT:175` `listCalls()).toHaveLength(before)`; `TPT:176` menu segue aberto; `TT:100-101` igual aceito; `TT:105`; `PMT:142-144` | ✅ PASS |
| AC5 filtro de status | `status=<valor>`, página 1; "Todos" remove | `TPT:183` opções `['Todos','Aprovada','Cancelada','Estornada','Chargeback']`; `TPT:188` `{status:'CANCELLED',page:'0'}` vindo da página 2; `TPT:192` `search.has('status')).toBe(false)`; `ATT:45,51` | ✅ PASS |
| AC6 filtro de método | `paymentMethod=<valor>`, página 1; "Todos" remove | `TPT:199` `['Todos','Crédito','Débito','Pix','Boleto']`; `TPT:204` `{paymentMethod:'PIX',page:'0'}`; `TPT:208` `has('paymentMethod')).toBe(false)`; `ATT:57` | ✅ PASS |
| AC7 paginação | "Anterior"/"Próxima" só com > 1 página; desabilitados na primeira/última | `TLT:71` sem `nav` com 1 página; `TLT:104-105` página 0: Anterior desabilitado, Próxima habilitado; `TLT:111-112` última: o inverso; `TLT:95,97` `onPageChange` 0 e 2 | ✅ PASS |
| AC8 vazio | "Nenhuma transação encontrada para os filtros escolhidos." | `TLT:119-120` texto exato, sem `table` | ✅ PASS |
| AC9 erro da busca | Mensagem + "Tentar novamente" refaz | `TLT:130` `toHaveTextContent('Você não tem acesso a este comerciante')`; `TLT:133-134` tabela volta, `toHaveBeenCalledTimes(2)` | ✅ PASS |

### P1: Cadastrar uma transação avulsa (TXN-05..TXN-07)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 gaveta e valores iniciais | Campos; Data = hoje; Parcelas = 1; Método "Escolha o método" | `TDT:71` `dialog 'Nova transação'`; `TDT:76` `'Escolha o método'`; `TDT:77` `'1'`; `TDT:79` `toHaveValue('2026-10-16')`; `TDT:80` `max` = hoje | ✅ PASS |
| AC2 Pix, Boleto, Débito fixam 1 | Parcelas 1 e desabilitado | `TDT:91-92` `toHaveValue('1')`, `toBeDisabled()` para `PIX`, `BOLETO`, `DEBIT_CARD` | ✅ PASS |
| AC3 validação no cliente | Mensagem do campo, sem API | `TDT:111-117` vazio: referência, valor e método com os textos exatos, `createTransaction).not.toHaveBeenCalled()`; `TDT:137-139` 11 casos (espaços, 101 caracteres, zero, negativo, 3 casas, 18 dígitos, parcelas 0/fração/vazias, data vazia, amanhã) com texto exato e `aria-invalid`; `TT:11-16` mensagens | ✅ PASS |
| AC4 corpo do `POST` | `externalReference` sem bordas, `amount` número JSON igual ao texto (vírgula → ponto), método, parcelas, data `aaaa-mm-dd` | `TDT:169-171` `{externalReference:'AVULSA-1',amount:'1234.5',...,transactionDate:'2026-10-03'}`; `ATT:76-81` corpo `"amount":1234.50`; `ATT:87` `"amount":0.10`; `TPT:227` corpo completo `"amount":100.00` | ✅ PASS |
| AC5 "Cadastrando…" | Botão desabilitado com esse texto | `TDT:165-166` `findByRole('button',{name:'Cadastrando…'})`, `toBeDisabled()`; `TDT:168` segundo submit não envia | ✅ PASS |
| AC6 sucesso | Fecha gaveta, "Transação <referência> cadastrada.", recarrega | `TPT:224` `notice('Transação AVULSA-1 cadastrada.')`; `TPT:225` gaveta `null`; `TPT:228-229` `AVULSA-1` na lista, `listCalls()).toHaveLength(2)`; `QTT:84` cache recarregado antes de `mutateAsync` resolver; `QTT:85` outra empresa não invalidada | ✅ PASS |
| AC7 recusa da API | Gaveta aberta, campos preenchidos, `message` | `TDT:189-200` para `400`, `409` de referência e `409` de período: `alert` com a mensagem, `dialog` presente, campos com `' AVULSA-1 '`, `'100,00'`, `'CREDIT_CARD'`, `'3'`, hoje; `onCreated`/`onClose` não chamados | ✅ PASS |

### P1: Mudar o status de uma transação aprovada (TXN-08, TXN-09)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 ação só em aprovada | "Alterar status" só com Aprovada | `TLT:79` `toEqual([true,false,false,false])` para Aprovada, Estornada, Cancelada, Chargeback; `TPT:245` some depois da mudança | ✅ PASS |
| AC2 diálogo | Referência, Cancelada/Estornada/Chargeback, "A mudança não pode ser desfeita." | `SDT:53` `toHaveAccessibleDescription('Transação VENDA-001. A mudança não pode ser desfeita.')`; `SDT:55` `['Cancelada','Estornada','Chargeback']`; `SDT:56` nenhuma marcada; `TT:128` | ✅ PASS |
| AC3 Confirmar desabilitado sem opção | Desabilitado | `SDT:63-66` `toBeDisabled()`, clique não envia; `SDT:69` habilita após escolher; `CDT:79,81` | ✅ PASS |
| AC4 `PATCH` com o status da opção | `CANCELLED`, `REFUNDED`, `CHARGEBACK` | `SDT:85` `updateTransactionStatus).toHaveBeenCalledWith('jwt','m-1','t-1',status)` para os 3 pares; `ATT:97` URL, `PATCH`, `{"status":"REFUNDED"}`; `TPT:243` | ✅ PASS |
| AC5 "Confirmando…" | Desabilitado com esse texto | `SDT:86-87` `toHaveTextContent('Confirmando…')`, `toBeDisabled()`; `SDT:89` segundo clique não envia | ✅ PASS |
| AC6 sucesso | Fecha, recarrega, novo status sem ação | `SDT:93` `onClose` 1 vez; `TPT:242-245` diálogo some, célula `'Estornada'`, sem botão; `QTT:104` cache recarregado | ✅ PASS |
| AC7 recusa | Diálogo aberto com a `message` e lista recarregada | `SDT:111-117` para `400` e `409` de período: `alert` com a mensagem, tabela passa a `'Estornada'`, `getTransactionPage` 2 vezes, `onClose` não chamado, opção mantida. `StatusDialog.tsx:31` chama `reloadList()` em qualquer erro, então o `409` de versão segue o mesmo caminho | ✅ PASS |

### P2: Atalho para a regra de taxa (TXN-10)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 sem regra ativa | Link "Cadastrar regra de taxa" para `/taxas?cadastrar=1` | `TDT:210-211` mensagem exata e `link ... toHaveAttribute('href','/taxas?cadastrar=1')`; `TT:111-112`; `SDRT:92-93` | ✅ PASS |
| AC2 outro motivo | Sem link | `TDT:191` `queryByRole('link',...)).toBeNull()` para `400` e os dois `409`; `TT:116-118` outro `409`, `Error` com o mesmo texto, `null` | ✅ PASS |

### P2: Empresa isola o estado (TXN-11)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 troca de empresa | Fecha gaveta e diálogo, remove aviso, **mantém período e filtros**, página 1 da nova empresa | `TPT:266` diálogo `null`; `TPT:267` aviso `null`; `TPT:268` `/api/merchants/m-2/transactions`; `TPT:269` `{fromDate:'2026-10-01',toDate:'2026-10-31',status:'APPROVED',page:'0'}` vindo da página 2; `TPT:270` select `'APPROVED'`; `TPT:282` gaveta `null`. O teste não muda período nem método antes da troca: o período ainda é o padrão e o método está em "Todos". Resetar os dois na troca não quebra nada (M31 sobreviveu) | ❌ GAP (período e método mantidos) |
| AC2 sem empresa | Mesmo estado vazio de Regras de taxa, sem filtros nem lista | `TPT:290` heading `'Nenhuma empresa no seu acesso'`; `TPT:291-296` sem Status, Método, período, "Nova transação", tabela e consulta. Mesmo `NoMerchant`: `TransactionsPage.tsx:87` e `FeeRulesPage.tsx:87` | ✅ PASS |

**Status**: ❌ 23 de 25 ACs com evidência que discrimina. 2 gaps de teste (TXN-01 AC1 nome da empresa, TXN-11 AC1 período e método). Nenhum spec-precision gap bloqueante.

---

### Edge Cases

- [x] 17 dígitos e 2 casas aceitos; 18 dígitos ou 3 casas recusados: `TT:29` `'12345678901234567.89'`; `TT:33-35` `toBeNull()`; `TDT:125-126` mensagem de Valor; `TDT:145-155` `'9'×17 + ',99'` enviado como `'…99.99'`.
- [x] Hoje aceito, amanhã recusado: `TT:68` `null`; `TT:73` `'Data da transação não pode ser futura'`; `TDT:131`; `TDT:145-154` hoje enviado.
- [x] 100 caracteres sem as bordas aceitos, 101 recusados: `TT:59-60`; `TDT:122` `'Use até 100 caracteres'`; `TDT:145,150` `'  R×100  '` enviado como `'R'×100`.
- [x] Crédito 3x → Pix → Crédito mantém 1: `TDT:103-104` `toHaveValue('1')`, `toBeEnabled()`.
- [x] Erro não JSON → "A API retornou erro <status>.": `frontend/lib/api.imports.test.ts:115` `message).toBe('A API retornou erro 502.')`. É o mesmo `send` que as três chamadas novas usam via `request`. A gaveta e o diálogo mostram `error.message` (`TDT:190`, `SDT:111`). Comportamento pré-existente, aceito como coberto.

---

### Notas do autor (conferidas)

| Nota | Conferência |
| --- | --- |
| `StatusDialog` chama `useInvalidateTransactions` em qualquer erro | ✅ `StatusDialog.tsx:31`; `SDT:112-113`; M15 morto |
| radiogroup "Novo status" | ✅ `StatusDialog.tsx:49`; `SDT:55` |
| aviso "Transação <ref>. A mudança não pode ser desfeita." | ✅ `SDT:53` |
| gaveta "Nova transação", data com `max=hoje` | ✅ `TDT:71,80` |
| aviso de sucesso sem botão de fechar | ✅ a spec não pede fechar; não é gap |
| testes da página fixam a data em 2026-10-16 | ✅ `TPT:64-65`, `TDT:24-25` (só `Date` é falsificado) |
| erro não JSON em `api.imports.test.ts:115` | ✅ confirmado |

---

### Discrimination Sensor

Scratch: `git worktree add --detach` em `scratchpad/sensor` a partir de `ac10fb2`, com `frontend/node_modules` por symlink. Cada mutante rodou `vitest run` sobre os 10 arquivos em escopo (107 testes) e foi revertido com `git checkout` no scratch.

| # | File | Mutação | Killed? | Testes que falharam |
| - | ---- | ------- | ------- | ------------------- |
| M1 | `lib/transactions.ts:24` | `MAX_AMOUNT_DIGITS` 17 → 18 | ✅ Killed | `TT` 18 dígitos, `TDT` 18 dígitos |
| M2 | `lib/transactions.ts:23` | `\d{1,2}` → `\d{1,3}` | ✅ Killed | `TT`, `TDT` 3 casas |
| M3 | `lib/transactions.ts:41` | `> 100` → `>= 100` | ✅ Killed | `TT` 100/101, `TDT` limites |
| M4 | `lib/transactions.ts:48` | `date > today` → `>=` | ✅ Killed | 9 (`TT`, `TDT`, `TPT`) |
| M5 | `TransactionDrawer.tsx:66` | não fixa Parcelas em 1 ao trocar o método | ✅ Killed | `TDT` ×4 |
| M6 | `TransactionDrawer.tsx:157` | `disabled={locked}` → `false` | ✅ Killed | `TDT` ×4 |
| M7 | `lib/api.ts` `getTransactionPage` | sempre envia `&status=` ("Todos" mantém o parâmetro) | ✅ Killed | 12 (`ATT`, `TPT`) |
| M8 | `TransactionsPage.tsx:99` | filtro de status sem `setPage(0)` | ✅ Killed | `TPT` status |
| M9 | `TransactionsPage.tsx:102` | filtro de método sem `setPage(0)` | ✅ Killed | `TPT` método |
| M10 | `TransactionsPage.tsx:64` | período sem `setPage(0)` | ✅ Killed | `TPT` mês, intervalo |
| M11 | `lib/transactions.ts:16-17` | rótulos Estornada ↔ Chargeback trocados | ✅ Killed | 10 |
| M12 | `lib/transactions.ts:21` | `REFUNDED` → `CHARGEBACK` nas opções (Estornada envia CHARGEBACK) | ✅ Killed | 6 (`SDT`, `TT`, `TPT`) |
| M13 | `TransactionList.tsx:70` | ação em toda linha que não é Chargeback | ✅ Killed | `TLT`, `TPT` |
| M14 | `lib/transactions.ts:68` | `isMissingFeeRule` aceita qualquer `409` | ✅ Killed | `TT`, `TDT` ×2 |
| M15 | `StatusDialog.tsx:31` | remove `reloadList()` no erro | ✅ Killed | `SDT` ×2 |
| M16 | `TransactionsPage.tsx:51` | troca de empresa não fecha a gaveta | ✅ Killed | `TPT` gaveta |
| M17 | `TransactionsPage.tsx:52` | troca de empresa não fecha o diálogo | ✅ Killed | `TPT` troca |
| M18 | `TransactionsPage.tsx:53` | troca de empresa mantém o aviso | ✅ Killed | `TPT` troca |
| M19 | `TransactionsPage.tsx:54` | troca de empresa não volta à página 1 | ✅ Killed | `TPT` troca |
| M20 | `PeriodMenu.tsx` | marcas de run aparecem sem `runs` | ✅ Killed | `PMT` ×3, `TPT` |
| M21 | `ConfirmDialog.tsx` | ignora `confirmDisabled` | ✅ Killed | `CDT`, `SDT` |
| M22 | `TransactionsPage.tsx:62` | intervalo invertido não lança erro | ✅ Killed | `TPT` intervalo invertido |
| M23 | `lib/queries.ts` `useUpdateTransactionStatus` | remove `onSuccess: invalidate` | ✅ Killed | `QTT`, `TPT` |
| M24 | `lib/queries.ts` `useCreateTransaction` | remove `onSuccess: invalidate` | ✅ Killed | `QTT`, `TPT` |
| M25 | `TransactionsPage.tsx:76` | remove `{merchant?.name}` do cabeçalho | ❌ Survived → Fix 1 | - |
| M26 | `TransactionsPage.tsx:38` | menu com 11 meses em vez de 12 | ✅ Killed | `TPT` mês |
| M27 | `TransactionDrawer.tsx:80` | não valida a data | ✅ Killed | `TDT` ×2 |
| M28 | `TransactionList.tsx:37` | nunca mostra o estado vazio | ✅ Killed | `TLT`, `TPT` |
| M29 | `lib/api.ts` `createTransaction` | `LosslessNumber` → `Number` (perde `.00`) | ✅ Killed | `ATT` ×2, `TPT` |
| M30 | `TransactionsPage.tsx:55` | troca de empresa zera o status | ✅ Killed | `TPT` troca |
| M31 | `TransactionsPage.tsx:55` | troca de empresa volta o período ao mês atual e zera o método | ❌ Survived → Fix 2 | - |
| M32 | `TransactionDrawer.tsx:113` | link da regra em qualquer erro | ✅ Killed | `TDT` ×3 |

**Sensor depth**: ampliado (≥ 8 pedidos; 32 aplicados).
**Resultado da iteração 1**: 30/32 killed, reprovado

**Isolamento**: `git status --porcelain` antes = vazio; depois = só `.specs/features/transactions-screen/validation.md`. Scratch removido com `git worktree remove --force` + `git worktree prune`; `git worktree list` não lista mais o scratch. Nenhum `git stash`.

---

### Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ |
| Surgical changes | ✅ props novas opcionais em `PeriodMenu`, `ConfirmDialog` e `SideDrawer`; chamadores antigos inalterados |
| No scope creep | ✅ sem filtro na URL, sem detalhe, sem edição |
| Matches patterns | ✅ `NoMerchant`, `BlockError`, `SideDrawer`, `ConfirmDialog`, `LosslessNumber` como em Regras de taxa |
| Spec-anchored outcome check | ⚠️ dois trechos de AC sem asserção que discrimine (Fix 1, Fix 2) |
| Per-layer Coverage Expectation met | ⚠️ lib e hooks 1:1; página sem "mantém período e método" |
| Every test maps to a spec requirement | ✅ testes sem ID (`ATT:48,54,60`, `TT:83`) cobrem Done-when de T3/T4 |
| Documented guidelines followed: `AGENTS.md`, `frontend/vitest.config.ts` | ✅ |

---

### Gate Check

- **Gate command**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`
- **Result**: 656 passed, 0 failed, 0 skipped. Typecheck exit 0. Build exit 0, `/transacoes` estática.
- **Test count before feature**: 570
- **Test count after feature**: 656
- **Delta**: +86 (87 novos − 1 linha removida de `UnderConstruction.test.tsx`)
- **Skipped tests**: nenhum
- **Failures**: nenhuma

---

### Fix Plans

### Fix 1: nome da empresa no cabeçalho sem teste que discrimine (TXN-01 AC1)

- **Root cause**: `TPT:123` usa `getAllByText('Alfa Pagamentos').length > 0`. O `MerchantSwitcher` do próprio teste já mostra o nome.
- **Fix task**: em `TPT:118-130`, achar o nome dentro do cabeçalho da página, por exemplo no elemento irmão do `heading` "Transações" (`heading.parentElement`), e afirmar `toHaveTextContent('Alfa Pagamentos')`. Na troca de empresa, afirmar `'Beta Comércio'` no mesmo lugar.
- **Done when**: remover `{merchant?.name}` de `TransactionsPage.tsx:76` quebra o teste.
- **Priority**: Minor

### Fix 2: período e método mantidos na troca de empresa sem teste (TXN-11 AC1)

- **Root cause**: `TPT:248-272` só muda o status antes da troca. Período e método continuam no padrão, que é igual ao reset.
- **Fix task**: no teste de troca, escolher também "Setembro de 2026" no menu de período e "Crédito" no filtro de método antes de trocar para Beta. Afirmar que a consulta de `m-2` traz `fromDate:'2026-09-01'`, `toDate:'2026-09-30'`, `paymentMethod:'CREDIT_CARD'`, `status:'APPROVED'` e `page:'0'`, e que o botão do período mostra "Setembro de 2026" e o select de método fica em `CREDIT_CARD`.
- **Done when**: acrescentar `setPeriod(monthWindow(new Date()))` ou `setMethod('')` no bloco de troca (`TransactionsPage.tsx:49-55`) quebra o teste.
- **Priority**: Minor

---

### Observações (não bloqueantes)

1. A spec escreve `sort=transactionDate,desc`; o cliente envia `sort=transactionDate%2Cdesc` (`ATT:35`). É o mesmo valor depois de decodificado (`TPT:128`) e segue o padrão de Importações.
2. O `409` de versão do `PATCH` não tem caso próprio em `SDT`. Ele segue o mesmo `catch` dos casos `400` e `409` de período (`StatusDialog.tsx:28-31`), então o comportamento é o mesmo.

**Docs desatualizados (reportados, não editados)**: a tabela de rastreabilidade em `spec.md:169-181` ainda marca TXN-01..TXN-11 como "Design | Pending" e diz "0 mapped to tasks". `tasks.md:12` está com Status "In Progress". Depois das fixes e de um novo PASS, o orquestrador deve marcar TXN-01..TXN-11 como Verified e o Status como Done.

---

### Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| TXN-01 | Pending | ❌ Needs Fix (teste do nome da empresa) |
| TXN-02 .. TXN-10 | Pending | ✅ Verified |
| TXN-11 | Pending | ❌ Needs Fix (teste de período e método mantidos) |

---

### Summary

**Overall**: ❌ Not Ready

**Spec-anchored check**: 23/25 ACs e 5/5 edge cases com o valor da spec; 2 gaps de teste
**Sensor**: 30/32 mutantes killed (M25, M31 sobreviveram)
**Gate**: 656 passed, typecheck e build OK

**What works**: busca do mês com filtros e volta à página 1, validação no cliente com limites exatos, valor enviado como número exato, parcelas fixas, recusas com gaveta e diálogo abertos, link da regra só para a mensagem exata, ação só em aprovada, invalidação após cadastro, mudança e erro de status, isolamento por empresa para gaveta, diálogo, aviso, página e status.

**Issues found**: Fix 1 e Fix 2, ambas só de teste.

**Next steps**: aplicar as duas fixes em `TransactionsPage.test.tsx` e rodar o Verifier de novo (iteração 2 de 3).
