# Tela de Importações Validation

**Date**: 2026-10-16
**Spec**: `.specs/features/imports-screen/spec.md`
**Diff range**: `464e8c4^..f78905f` (10 commits), branch `feat/imports-screen`. Spec/design/tasks `464e8c4`; código `3476a45`, `6727f83`, `5b2bea1`, `0bd860a`, `97d04b7`, `9caf1eb`, `a022791`; docs `f78905f`.
**Verifier**: sub-agente independente (author ≠ verifier), iteração 1.

## Validation: imports-screen - PASS ✅

Todos os 22 ACs e os 3 edge cases têm evidência `file:line` com o valor da spec. O sensor injetou 16 mutantes de comportamento e os testes mataram os 16. O gate passa: 570 testes, typecheck e build. Não há gap bloqueante. Ficam três observações menores, sem fix task (ver "Observações").

Abreviações:

- `AIT` = `frontend/lib/api.imports.test.ts`
- `IMT` = `frontend/lib/imports.test.ts`
- `QIT` = `frontend/lib/queries.imports.test.tsx`
- `IBT` = `frontend/components/imports/ImportBatches.test.tsx`
- `IRT` = `frontend/components/imports/ImportResult.test.tsx`
- `IPT` = `frontend/components/imports/ImportPanel.test.tsx`
- `IST` = `frontend/components/imports/ImportsPage.test.tsx`

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 chamadas de import no cliente | ✅ Done | `lib/api.ts` `importCsv`, `getImportBatches`, `ApiError.details`, `Content-Type` só para corpo string |
| T2 regras puras | ✅ Done | `lib/imports.ts` |
| T3 hooks | ✅ Done | `lib/queries.ts` `useImportBatches`, `useImportCsv` |
| T4 lista de lotes | ✅ Done | `ImportBatches.tsx` |
| T5 resultado do envio | ✅ Done | `ImportResult.tsx` |
| T6 painel de upload | ✅ Done | `ImportPanel.tsx` |
| T7 página com abas + rota | ✅ Done | `ImportsPage.tsx`, `app/(app)/importacoes/page.tsx` |
| T8 README | ✅ Done | `README.md`, `frontend/README.md` |

**Remoção em `UnderConstruction.test.tsx` (`a022791`): legítima.** A rota `/importacoes` deixou de renderizar o placeholder, então a linha `['/importacoes', 'Importações', ImportacoesPage]` passaria a falhar por comportamento correto. A rota continua coberta: `IST:112-122` renderiza `@/app/(app)/importacoes/page` e verifica título, abas e a consulta de lotes. As outras quatro rotas placeholder seguem no teste. Nenhuma asserção foi enfraquecida.

---

## Spec-Anchored Acceptance Criteria

### P1: Enviar um CSV e ver o resultado

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 abas e `?tipo=` | Abas "Transações", "Liquidação", "Extrato"; `?tipo=` ativa; Transações se ausente/desconhecido | `IST:118-119` `tabs()...toEqual(['Transações','Liquidação','Extrato'])`, `selectedTab()).toBe('Transações')` sem `?tipo=`; `IST:124-133` `extrato`→`Extrato`, `liquidacao`→`Liquidação`, `xyz`→`Transações`; `IMT:38-39` `kindFromSlug(null/''/'xyz'/'EXTRATO')).toBe('transactions')` | ✅ PASS |
| AC2 troca atualiza `?tipo=` | `transacoes`, `liquidacao`, `extrato` | `IST:141` `replace).toHaveBeenLastCalledWith('/importacoes?tipo=liquidacao')`; `IST:146` `...tipo=extrato`; `IST:148` `...tipo=transacoes` | ✅ PASS |
| AC3 colunas esperadas, em ordem | Listas exatas das Assumptions | `IMT:17-21` `columns).toEqual([[...6],[...7],[...4]])`; `IPT:58-64` `getByText('externalReference,amount,paymentMethod,installments,status,transactionDate')` (e as de liquidação e extrato) | ✅ PASS |
| AC4 "Importar" desabilitado sem arquivo | Desabilitado | `IPT:70` `importButton()).toBeDisabled()`; `IPT:73` habilita após escolher | ✅ PASS |
| AC5 `POST` multipart `file` no endpoint do tipo da empresa | `/transactions/import`, `/external-settlements/import`, `/bank-statements/import` | `AIT:36` `url).toBe(\`${merchantBase}/${path}/import\`)`; `AIT:37` `method).toBe('POST')`; `AIT:40` `contentType).toBeNull()`; `AIT:43` `[...form.keys()]).toEqual(['file'])`; `IPT:89` `importCsv).toHaveBeenCalledWith('jwt','m-1',kind,file)` | ✅ PASS |
| AC6 liquidação sem `layout` | URL sem `layout` | `AIT:36` URL exata `.../external-settlements/import` (sem query) | ✅ PASS |
| AC7 "Importando…" desabilitado | Desabilitado, texto "Importando…" | `IPT:99-100` `toBeDisabled()`, `toHaveTextContent('Importando…')`; `IPT:102` segundo clique não envia (`toHaveBeenCalledTimes(1)`) | ✅ PASS |
| AC8 sucesso | "<fileName> importado: <totalRows> linha(s)"; arquivo limpo | `IPT:121` `toHaveTextContent('transacoes-out.csv importado: 42 linha(s)')`; `IPT:122-123` botão desabilitado e `input.value).toBe('')`; `IMT:98` `'extrato.csv importado: 3 linha(s)'`; `IRT:12` | ✅ PASS |
| AC9 > 5 MB | Mensagem exata, sem requisição | `IPT:134` `toHaveTextContent('Arquivo CSV excede o tamanho máximo permitido de 5MB')`; `IPT:135` `importCsv).not.toHaveBeenCalled()`; `IMT:47` `isTooLarge({size:5_242_881})).toBe(true)` | ✅ PASS |

### P1: Ver por que o arquivo foi recusado

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 `400` com `rowErrors` | `message` + tabela "Linha"/"Erro", uma linha por item, na ordem | `AIT:85-87` `details).toEqual({rowErrors:[...]})`; `IMT:57-65` ordem `[7, 2]` preservada; `IRT:24-31` `columnheader` `['Linha','Erro']` e linhas `[['9',...],['2',...],['4',...]]`; `IPT:159-160` mensagem e tabela no painel | ✅ PASS |
| AC2 `409` com `conflictingReferences` | `message` + todas as referências | `AIT:97` `details).toEqual({conflictingReferences:['PED-1','PED-2']})`; `IRT:39-41` `listitem` `['PED-1001','PED-1002','PED-1003']`; `IPT:177` `['TX-1','TX-2']` | ✅ PASS |
| AC3 erro sem listas (travado, 413, 403, cabeçalho) | `message` da resposta | `AIT:103-104` mensagem, `details` undefined; `IMT:73-75`; `IRT:49-51` só mensagem, sem tabela/lista; `IPT:186` `'Janela travada de 2026-10-01 a 2026-10-31'` | ✅ PASS |
| AC4 falha sem resposta | "Não foi possível conectar à API. Tente novamente." | `IPT:195` texto exato no alerta; `IMT:78-80`; origem do texto em `send`: `frontend/lib/api.test.ts:88` (pré-existente, mesmo `send`) | ✅ PASS |
| AC5 novo envio limpa o anterior | Resultado anterior removido ao iniciar | `IPT:209-210` com envio pendente: `'Importando…'` e `queryByRole('alert')).toBeNull()` | ✅ PASS |
| AC6 recusa mantém arquivo | Arquivo mantido para reenviar | `IPT:161` `toBeEnabled()`; `IPT:164-165` segundo envio com o mesmo `file` | ✅ PASS |

### P1: Ver os lotes importados

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 listagem | `GET /<tipo>/imports?page=<n>&size=20&sort=createdAt,desc`; colunas "Arquivo", "Linhas", "Importado em" | `AIT:62` `url).toBe(\`.../${path}/imports?page=2&size=20&sort=createdAt%2Cdesc\`)` para os três tipos; `IBT:43` `['Arquivo','Linhas','Importado em']`; `IBT:44-47` linhas na ordem da API | ✅ PASS |
| AC2 formato da data | `dd/mm/aaaa hh:mm` no fuso do navegador | `IMT:104-105` `'16/10/2026 10:05'` (São Paulo), `'02/01/2026 03:04'` (UTC); `IMT:109` dia do fuso perto da meia-noite; `IBT:48` regex `^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$` | ✅ PASS |
| AC3 paginação | "Anterior"/"Próxima" só com > 1 página; desabilitados na primeira/última | `IBT:50` sem `nav` com 1 página; `IBT:70-71` página 0: Anterior desabilitado, Próxima habilitado; `IBT:77-78` última: o inverso; `IBT:61,63` `onPageChange` 0 e 2 | ✅ PASS |
| AC4 vazio | "Nenhuma importação de <transações\|liquidação\|extrato> ainda." | `IBT:81-91` texto exato para os três substantivos; `IMT:25` `noun` `['transações','liquidação','extrato']` | ✅ PASS |
| AC5 erro da listagem | Mensagem + "Tentar novamente" refaz | `IBT:100` `toHaveTextContent('Você não tem acesso a este comerciante')`; `IBT:101-104` clique, tabela aparece, `toHaveBeenCalledTimes(2)` | ✅ PASS |
| AC6 import aceito volta à página 1 com o lote no topo | Página 1, recarregada, novo lote no topo | `IPT:110-112` estava em "Página 2 de 2"; `IPT:124` `'Página 1 de 2'`; `IPT:126` `getAllByRole('row')[1]).toHaveTextContent('transacoes-out.csv')`; `QIT:73` cache da página 0 já refeito com `[newer, older]` | ✅ PASS |
| AC7 invalida últimos imports do dashboard | `queryKeys.imports` recarregada | `QIT:74` `['imports','m-1']...settlementImportedAt).toBe(newer.createdAt)` ao resolver a mutação; `QIT:84-103` recusa (`QIT:102` `isInvalidated).toBe(false)`) não invalida nada | ✅ PASS |

### P2: Empresa e aba isolam o estado

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 troca de empresa | Descarta arquivo e resultado; lotes da nova empresa | `IST:175-180` `beta-transacoes.csv` visível, `alfa-transacoes.csv` ausente, `alert` null, `Importar` desabilitado, consulta `/api/merchants/m-2/transactions/imports` | ✅ PASS |
| AC2 troca de aba | Descarta arquivo e resultado | `IST:158-159` e `IST:163-164` `alert` null e `Importar` desabilitado após cada troca | ✅ PASS |
| AC3 sem empresa | Mesmo estado vazio de Regras de taxa, sem abas | `IST:187-189` heading `'Nenhuma empresa no seu acesso'`, `tablist` null, nenhuma consulta de lotes. Mesmo componente `NoMerchant`: `ImportsPage.tsx:26` e `FeeRulesPage.tsx:87` | ✅ PASS |

**Status**: ✅ Todos os 22 ACs cobertos com o valor da spec. Nenhum spec-precision gap bloqueante.

---

## Edge Cases

- [x] Resposta de erro não JSON → "A API retornou erro <status>.": `AIT:115` `message).toBe('A API retornou erro 502.')`, `details` undefined. O painel mostra `error.message` (`IMT:78-80`, `IPT:195`).
- [x] Exatamente 5 MB (5.242.880 bytes) é enviado: `IMT:45-46` `MAX_CSV_BYTES).toBe(5_242_880)`, `isTooLarge({size:5_242_880})).toBe(false)`; `IPT:138-146` `importCsv).toHaveBeenCalledWith(..., file)`.
- [x] `403` mostra a `message`: mesmo caminho do AC3 da recusa (`AIT:103-104`, `IRT:49-51`). A lista com `403` também é coberta em `IBT:95-100`.

---

## Discrimination Sensor

Scratch: `git worktree add --detach` em `scratchpad/sensor` a partir de `f78905f`, `node_modules` por symlink. Cada mutante rodou `vitest run` sobre os 7 arquivos de teste em escopo (76 testes) e foi revertido com `git checkout` no scratch.

| # | File | Mutação | Killed? | Testes que falharam |
| - | ---- | ------- | ------- | ------------------- |
| M1 | `lib/api.ts` `send` | `typeof init.body === 'string'` → `init.body` (força JSON no multipart) | ✅ Killed | `AIT` importCsv ×3 |
| M2 | `lib/imports.ts:39` | `size > MAX` → `size >= MAX` | ✅ Killed | `IMT` isTooLarge, `IPT` 5 MB exato |
| M3 | `lib/imports.ts:32` | default de `kindFromSlug` → `'settlements'` | ✅ Killed | 8 (`IMT`, `IST`) |
| M4 | `lib/queries.ts` `useImportCsv` | remove invalidação de `queryKeys.imports` | ✅ Killed | `QIT` IMP-12 |
| M5 | `ImportsPage.tsx:50` | `key` → só `kind` (empresa não reseta) | ✅ Killed | `IST` troca de empresa |
| M6 | `ImportsPage.tsx:50` | `key` → só `merchantId` (aba não reseta) | ✅ Killed | `IST` troca de aba |
| M7 | `ImportPanel.tsx` onSuccess | remove `setPage(0)` | ✅ Killed | `IPT` IMP-04/IMP-12 |
| M8 | `lib/imports.ts:56` | remove `every(isRowError)` | ✅ Killed | `IMT` details malformado ×2 |
| M9 | `lib/api.ts` `IMPORT_PATHS` | `settlements` → path `settlements` | ✅ Killed | 5 (`AIT`, `IST`) |
| M10 | `ImportPanel.tsx` submit | remove `setOutcome(null)` | ✅ Killed | `IPT` IMP-09 |
| M11 | `ImportPanel.tsx` onSuccess | remove `setFile(null)` | ✅ Killed | `IPT` IMP-04 |
| M12 | `ImportBatches.tsx:63` | `page >= totalPages - 1` → `>` | ✅ Killed | `IBT` paginação |
| M13 | `lib/api.ts` `getImportBatches` | `size=20` → `size=10` | ✅ Killed | `AIT` ×3 |
| M14 | `lib/api.ts` `send` | aceita `details` array | ✅ Killed | `AIT` details não objeto |
| M15 | `ImportPanel.tsx` botão | `disabled={!file}` (ignora `isPending`) | ✅ Killed | `IPT` Importando… |
| M16 | `lib/imports.ts:57` | remove checagem de referência string | ✅ Killed | `IMT` details malformado |

**Sensor depth**: ampliado (≥ 5 pedidos; 16 aplicados).
**Result**: 16/16 killed - PASS ✅

**Isolamento**: `git status --porcelain` antes = depois = `?? frontend/AGENTS.md`. Scratch removido com `git worktree remove --force` + `git worktree prune`; `git worktree list` não lista mais o scratch. Nenhum `git stash`.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ |
| Surgical changes | ✅ `SettlementImport` vira alias de `ImportBatch`; `Content-Type` só muda para corpo não string |
| No scope creep | ✅ sem `layout`, sem drag-and-drop, sem modelo de CSV |
| Matches patterns | ✅ `NoMerchant`, `BlockError`, paginação igual à de Empresas |
| Spec-anchored outcome check | ✅ |
| Per-layer Coverage Expectation met | ✅ lib 1:1, hooks com invalidação e recusa, componentes por AC, rota via `IST` |
| Every test maps to a spec requirement | ✅ testes sem ID (`AIT:48`, `AIT:107`, `QIT:84`, `IMT` malformado) cobrem os Done-when de T1-T3 |
| Documented guidelines followed: `AGENTS.md`, `frontend/vitest.config.ts` | ✅ |

---

## Gate Check

- **Gate command**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`
- **Result**: 570 passed, 0 failed, 0 skipped (43 arquivos). Typecheck exit 0. Build exit 0, `/importacoes` estática.
- **Test count before feature**: 495 (derivado: 570 − 76 novos + 1 linha removida de `UnderConstruction.test.tsx`)
- **Test count after feature**: 570
- **Delta**: +75
- **Skipped tests**: nenhum
- **Failures**: nenhuma

---

## Observações (não bloqueantes, sem fix task)

1. `accept=".csv,text/csv"` (Assumption "Tipo de arquivo") está em `ImportPanel.tsx:54`, mas nenhum teste o fixa. É só uma dica do seletor; a spec não pede validação no cliente.
2. "Lista com rolagem" dos `rowErrors` (Assumption "Quantidade de erros") é só CSS (`max-h-[320px] overflow-auto`, `ImportResult.tsx:26`). Não tem teste e não é AC.
3. A spec escreve `sort=createdAt,desc`; o cliente envia `sort=createdAt%2Cdesc` (`AIT:62`). É o mesmo valor depois de decodificado e segue o padrão já usado em `getLatestSettlementImport`.

**Docs desatualizados (reportados, não editados)**: a tabela de rastreabilidade em `spec.md` ainda marca IMP-01..IMP-13 como "Design | Pending", e `tasks.md:12` está com Status "In Progress". O orquestrador deve marcar IMP-01..IMP-13 como Verified e o Status como Done.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| IMP-01 .. IMP-13 | Pending | ✅ Verified |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 22/22 ACs e 3/3 edge cases com o valor da spec
**Sensor**: 16/16 mutantes killed
**Gate**: 570 passed, typecheck e build OK

**What works**: envio multipart por tipo sem `layout`, limite de 5 MB com fronteira exata, recusas com `rowErrors` e `conflictingReferences`, lista paginada com volta à página 1 e invalidação do dashboard, isolamento por empresa e aba via `key`.

**Issues found**: nenhum bloqueante.

**Next steps**: atualizar status em `spec.md` e `tasks.md`; push e PR só com pedido do usuário.
