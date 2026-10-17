# Tela de Conciliações Validation

**Date**: 2026-10-16
**Spec**: `.specs/features/reconciliations-screen/spec.md`
**Diff range**: `8f27056^..dc39363` (15 commits), branch `feat/reconciliations-screen`. Iteração 1 cobriu `8f27056^..6496843`; o relatório dela entrou em `f1cd4e0`; as fixes são `7a0ad44` (código + teste) e `dc39363` (só teste).
**Verifier**: sub-agente independente (author ≠ verifier), iteração 2 de 3.

## Validation: reconciliations-screen - PASS ✅

O gap de REC-09 AC8 está fechado. Quando o run sai de `PENDING`/`RUNNING`, o detalhe invalida os itens do run e a tabela mostra os itens finais. O teste novo reproduz o backend real (itens só depois do `COMPLETED`, endpoint de itens mais lento) e mata o mutante que tira a invalidação. Os dois comportamentos sem teste da iteração 1 (mensagem do `FAILED` no cabeçalho e `NoMerchant` no detalhe) agora têm teste, e os mutantes deles morrem. Gate: 743 testes, typecheck e build. Backend sem mudança.

Abreviações: as mesmas da iteração 1 (abaixo). `RDT` = `frontend/components/reconciliations/RunDetailPage.test.tsx`.

---

## Iteração 2

### Fixes `7a0ad44` e `dc39363`

- **Código** (`7a0ad44`). `RunDetailPage.tsx:101-107`: `useRef(polling)` guarda o estado anterior; o efeito chama `queryClient.invalidateQueries({ queryKey: queryKeys.runItems(merchantId, runId) })` só quando `wasPolling.current && !polling`. `queries.ts:46-49`: `queryKeys.runItems` aceita `filters` e `page` opcionais; sem `filters`, devolve o prefixo `['runItems', merchantId, runId]`.
- **Chaves completas inalteradas.** O único outro chamador é `useRunItems` (`queries.ts:227`), que sempre passa `filters` e `page`. A chave continua `['runItems', merchantId, runId, result ?? 'ALL', discrepancyType ?? 'ALL', page]`. O `page ?? 0` só vale sem `page`, e `useRunItems` recebe `page: number`. O prefixo casa com todas as páginas e filtros do run, e com nenhum outro run (M3 e M3b morrem).
- **Nada enfraquecido.** O diff dos testes só acrescenta: a fixture `live` (`RDT:44-48, 61-73`) é `null` por padrão (`RDT:84`) e mantém o comportamento antigo para os demais testes; o teste antigo de AC8 (`RDT:234-270`) não mudou; `dc39363` só acrescenta dois `it`. Nenhuma asserção removida ou afrouxada.
- **README.** `frontend/README.md:67` descreve a busca final dos itens. Correto.
- **Backend.** `git diff --name-only 6496843..dc39363` não lista nenhum arquivo de `backend/`. `./mvnw verify` da iteração 1 segue valendo.

### ACs re-checadas

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| REC-09 AC8 polling do detalhe | recarrega run e itens a cada 3 s até sair de `PENDING`/`RUNNING`; o usuário acompanha o run até a conclusão sem recarregar | `RDT:275` itens vazios enquanto o último run entregue está em andamento, run 20 ms, itens 60 ms (`RDT:61-73`); `RDT:284-285` `Status: 'Em andamento'` e "Nenhum item encontrado para os filtros escolhidos."; `RDT:288` `Status: 'Concluída'`; `RDT:290` `within(getByRole('table',{name:'Itens'})).getByText('REF-1')`; `RDT:294-295` nenhuma chamada nova de run ou itens depois de +15 s. Junto com `RDT:234-270` (cadência de 3 s). M1, M2b, M3, M3b e N6 morrem | ✅ PASS |
| Assumption "Run com falha" / REC-03 AC5 no detalhe | `FAILED` mostra a `errorMessage` do run | `RDT:303` `Status: 'Falhou'`; `RDT:304` `within(region).getByText('Falha ao processar o lote')`; `RDT:305` sem "Exportar CSV". M4 morre | ✅ PASS |
| REC-11 AC3 no detalhe | sem empresa: estado vazio, sem buscar o run | `RDT:312` heading "Nenhuma empresa no seu acesso"; `RDT:313` sem região "Conciliação"; `RDT:314-315` 0 chamadas de run e de itens. M5 morre | ✅ PASS |

**Status**: ✅ 39/39 ACs e 5/5 edge cases com evidência que discrimina (38 ACs da iteração 1 sem mudança que as afete, mais REC-09 AC8 re-checada; os dois extras do detalhe agora cobertos).

**Spec-precision gaps (menores, aceitos, sem mudança)**: `itemsProgress` mostra "—" com `totalItems` nulo fora de `RUNNING` (`frontend/lib/reconciliations.ts:43`); Diferença com sinal (`RunItems.tsx:86`). A spec não fixa nenhum dos dois; a escolha segue as convenções da spec e do dashboard. Ver o ponto 2 e 3 da iteração 1.

### Discrimination Sensor (iteração 2)

Scratch: `git worktree add --detach` em `scratchpad/rec-wt2` a partir de `dc39363`, com `frontend/node_modules` por symlink. Baseline do scratch: `tsc --noEmit` exit 0, 251 testes em escopo passam. Cada mutante passou por `tsc --noEmit` (todos compilam) e rodou `vitest run` sobre `components/reconciliations`, `lib/reconciliations.test.ts`, `lib/api.reconciliations.test.ts`, `lib/queries.reconciliations.test.tsx`, `lib/queries.test.tsx` e `components/dashboard` (251 testes). Revertido com `git checkout` no scratch.

| # | File | Mutação | Killed? | Testes que falharam |
| - | ---- | ------- | ------- | ------------------- |
| M1 | `RunDetailPage.tsx:105` | nunca invalida os itens ao concluir | ✅ Killed | `RDT:272` |
| M2a | `RunDetailPage.tsx:105` | invalida em qualquer mudança de `polling` (entrar e sair) | ⚪ Survived, equivalente na spec: mantém a invalidação na saída; só acrescenta uma busca de itens quando o run chega em andamento, que a spec não proíbe nem observa |
| M2b | `RunDetailPage.tsx:105` | invalida ao entrar em andamento, não ao sair | ✅ Killed | `RDT:272` |
| M2c | `RunDetailPage.tsx:104-107` | invalida em todo render de `RunDetail` (efeito sem deps, sem condição) | ⚪ Survived, equivalente na spec: `RunDetail` só renderiza quando o run muda, então inclui a invalidação na saída e não entra em laço (`RDT:295` prova); o efeito extra é busca redundante de itens, sem resultado visível diferente |
| M3 | `RunDetailPage.tsx:105` | invalida `runItems(merchantId, runId + '-x')` | ✅ Killed | `RDT:272` |
| M3b | `queries.ts:48` | prefixo `['runItems', runId, merchantId]` | ✅ Killed | `RDT:272` |
| M4 | `RunDetailPage.tsx:92` | `errorMessage` só em `RUNNING` | ✅ Killed | `RDT:298` |
| M5 | `RunDetailPage.tsx:147` | ramo `NoMerchant` nunca roda no detalhe | ✅ Killed | `RDT:308` |
| N1 | `lib/reconciliations.ts:42` | progresso "<processados> de <processados>" | ✅ Killed | `RLT` REC-03 (2), `RDT:234`, `LRT` (2), `RLT` AC6 |
| N2 | `lib/reconciliations.ts:51` | `anyInFlight` ignora `PENDING` | ✅ Killed | `LRT`, `QRT` AC6, `RLT` AC6 |
| N3 | `lib/api.ts:363` | itens em `absDifference,asc` | ✅ Killed | `ART` (4), `RDT:136` |
| N4 | `NewRunDrawer.tsx:53` | editar data não muda "Mês" para Personalizado | ✅ Killed | `NDT` De e Até |
| N5 | `lib/reconciliations.ts:34` | "Informe as duas datas" só com as duas vazias | ✅ Killed | `LRT`, `NDT` De vazio, `NDT` Até vazio |
| N6 | `lib/queries.ts:219` | `useRun` recarrega sempre, mesmo concluído | ✅ Killed | `QRT` useRun, `RDT:234`, `RDT:272` |

**Sensor depth**: ampliado (8 mutantes dirigidos aos gaps + 6 novos).
**Result**: 12/12 não equivalentes mortos (14 injetados; M2a e M2c equivalentes na spec) - PASS ✅

**Isolamento**: `git status --porcelain` antes = vazio; depois da remoção do scratch = vazio (igual ao baseline). Scratch removido com `git worktree remove --force` + `git worktree prune`; `git worktree list` não lista mais o scratch. Nenhum `git stash`.

### Gate Check (iteração 2)

- **Frontend**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build` → 743 passed, 0 failed, 0 skipped. Typecheck exit 0. Build exit 0, `/conciliacoes` estática e `/conciliacoes/[runId]` dinâmica.
- **Backend**: sem arquivo alterado desde a iteração 1 (`git diff --name-only 6496843..dc39363`); vale o `./mvnw verify` da iteração 1 (632 testes, JaCoCo 94% linha, 87% branch).
- **Test count**: frontend 740 → 743 (+3: AC8 com backend em estágios, FAILED no cabeçalho, detalhe sem empresa). Nenhum teste removido.

### Code Quality (iteração 2)

| Principle | Status |
| --- | --- |
| Minimum code | ✅ Um `useRef` e um efeito; a chave ganhou só a forma de prefixo |
| Surgical changes | ✅ 4 arquivos em `7a0ad44`, 1 em `dc39363` |
| Matches patterns | ✅ `queryKeys` com prefixo opcional, como `openDiscrepancies` |
| Spec-anchored outcome check | ✅ |
| Every test maps to a spec requirement | ✅ REC-09 AC8, Assumption "Run com falha", REC-11 AC3 |

### Lessons

Nenhuma lição nova: a iteração 2 é PASS limpo (os sobreviventes são equivalentes e os spec-precision gaps já foram aceitos na iteração 1). L-037 segue candidata; `lessons.md` só promove por recorrência em outra feature. A fix aplicou L-037 (`RDT:61-73` serve os itens conforme o estado do run).

### Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| REC-01 .. REC-08 | ✅ Verified | ✅ Verified |
| REC-09 | ❌ Needs Fix (AC8) | ✅ Verified |
| REC-10, REC-11 | ✅ Verified | ✅ Verified |

**Docs desatualizados (reportados, não editados)**: `spec.md:197-209` ainda marca REC-01..REC-11 como "Design | Pending" e diz "0 mapped to tasks". `tasks.md:12` está com Status "In Progress". O orquestrador deve marcar REC-01..REC-11 como Verified e o Status como Done. `frontend/README.md:67` já está certo.

### Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 39/39 ACs e 5/5 edge cases com o valor da spec; 2 spec-precision gaps menores aceitos
**Sensor**: 12/12 não equivalentes mortos na iteração 2 (24/24 na iteração 1)
**Gate**: frontend 743 passed, typecheck e build OK; backend 632 (sem mudança)

---

# Histórico: iteração 1

**Diff range da iteração 1**: `8f27056^..6496843` (12 commits). Spec `8f27056`, plano `80ce6ff`, código `c937b2b`..`13d34af`, docs `6496843`.

### Veredito da iteração 1: reprovada

O detalhe de um run em andamento termina com o cabeçalho "Concluída" e a tabela de itens vazia. Os itens só reaparecem com um recarregamento manual. O backend grava todos os itens na mesma transação que marca o run `COMPLETED`, então toda busca de itens feita em `RUNNING` volta vazia. Quando o run chega `COMPLETED`, o polling dos itens para, e a última busca de itens costuma ser anterior ao commit. Isso quebra REC-09 AC8 ("recarregar o run e os itens a cada 3 s até sair de `PENDING`/`RUNNING`") e o critério de sucesso "acompanha um run do disparo à conclusão sem recarregar a página". O teste de AC8 não pega o defeito, porque o servidor do teste devolve os mesmos itens em qualquer estado.

O resto está sólido: 42 de 44 critérios (39 ACs + 5 edges) com evidência que bate com a spec, 24 de 25 mutantes mortos (o sobrevivente é equivalente), os dois gates passam.

Abreviações: `RLT` = `frontend/components/reconciliations/RunList.test.tsx`, `RPT` = `frontend/components/reconciliations/ReconciliationsPage.test.tsx`, `NDT` = `frontend/components/reconciliations/NewRunDrawer.test.tsx`, `RIT` = `frontend/components/reconciliations/RunItems.test.tsx`, `RDT` = `frontend/components/reconciliations/RunDetailPage.test.tsx`, `QRT` = `frontend/lib/queries.reconciliations.test.tsx`, `LRT` = `frontend/lib/reconciliations.test.ts`, `ART` = `frontend/lib/api.reconciliations.test.ts`, `BIT` = `backend/src/test/java/br/com/hanrry/reconpay/reconciliation/ReconciliationIntegrationTest.java`.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1..T10 (`tasks.md`) | ✅ Done | Todos os itens "Done when" marcados `[x]`. Um commit por task. `tasks.md:12` ainda diz "In Progress". |

---

## Defeito principal: itens não recarregam depois da conclusão

**Backend.** `ReconciliationRunProcessor.process` (`backend/src/main/java/br/com/hanrry/reconpay/reconciliation/service/ReconciliationRunProcessor.java:68`) roda em uma transação `REQUIRES_NEW`. Os itens são gravados em lotes (`:171-184`), mas só ficam visíveis no commit, junto com `status = COMPLETED` (`:124`). Só o progresso é gravado fora dela (`runStateService.recordProgress`). Então `GET .../items` durante `RUNNING` responde vazio.

**Frontend.** `RunDetailPage.tsx:99` liga `polling` só enquanto o run está em andamento. `useRunItems` (`frontend/lib/queries.ts:221-229`) usa `refetchInterval: polling ? RUN_POLL_MS : false`. Quando a resposta do run chega `COMPLETED`, o intervalo dos itens é desligado. Nada invalida `queryKeys.runItems` e `refetchOnWindowFocus` está desligado (`queries.ts:31`). A tabela fica em "Nenhum item encontrado para os filtros escolhidos." até o usuário recarregar a página ou mexer num filtro.

**Prova (sonda no scratch, sem tocar o código).** Copiei o harness de `RDT` com uma única diferença: os itens só existem depois que o servidor responde o run `COMPLETED` (como o commit real). Com latência simulada no `fetch`:

| Latência run / itens | Resultado depois de 15 s | Último GET de itens |
| --- | --- | --- |
| 0 / 0 ms, 20 / 20 ms, 100 / 100 ms | ✅ tabela com `REF-1` | depois do commit |
| 20 / 60 ms | ❌ "Nenhum item encontrado…" | antes do commit, nenhum depois |
| 20 / 25 ms | ❌ "Nenhum item encontrado…" | antes do commit, nenhum depois |
| 50 / 20 ms | ❌ "Nenhum item encontrado…" | 9 ms antes do GET do run que viu `COMPLETED` |

Cada consulta reinicia seu intervalo quando a resposta chega. Os dois relógios andam separados, e basta o endpoint de itens ser alguns milissegundos mais lento que o do run (o caso comum: paginação, ordenação e divergências) para a última busca ficar antes do commit. O teste `RDT:217-253` não pega isso: `RDT:55` devolve `server.items` em qualquer estado do run, e `RDT:241` só conta chamadas.

---

## Spec-Anchored Acceptance Criteria

### P1 Export 404 (REC-01)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 run inexistente | `404`, JSON, `error` `NOT_FOUND`, `message` "Conciliação não encontrada com id: <runId>" | `BIT:290-295` `status().isNotFound()`, `contentTypeCompatibleWith(APPLICATION_JSON)`, `header().doesNotExist("Content-Disposition")`, `jsonPath("$.error").value("NOT_FOUND")`, `jsonPath("$.message").value("Conciliação não encontrada com id: " + unknownRunId)`. B1 e B2 morrem | ✅ PASS |
| AC2 run de outra empresa | o mesmo `404` | `BIT:304-310`, mesmas asserções com o `runId` de outra empresa | ✅ PASS |
| AC3 run existente | `200` `text/csv`, `Content-Disposition` com `reconciliation-<runId>.csv`, mesmo conteúdo | `BIT:155-164` `status().isOk()`, `header().string("Content-Disposition", containsString("reconciliation-" + runId + ".csv"))`, `assertCsvHeader(csv)`; o teste não mudou no diff | ✅ PASS |

### P1 Histórico de runs (REC-02..REC-05)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 busca e cabeçalho | `GET /reconciliations?page=0&size=20&sort=createdAt,desc`; título "Conciliações" e nome da empresa | `RPT:121-122` path `/api/merchants/m-1/reconciliations` e `query(...)` `toEqual({page:'0',size:'20',sort:'createdAt,desc'})`; `RPT:119` `within(pageHeader()).getByText('Alfa Pagamentos')` (L-035); `ART:99` URL exata | ✅ PASS |
| AC2 colunas e formatos | Janela, Status, Itens, Conciliados, Divergentes, Criado em, "Ver itens"; `formatWindow`; `dd/mm/aaaa hh:mm` | `RLT:58-59` cabeçalhos; `RLT:62` `['Outubro de 2026','Concluída','3','2','1']`; `RLT:63-64` `/^15\/10\/2026 \d{2}:\d{2}$/`; `RLT:76` "01/10/2026 – 15/10/2026"; `LRT:16` rótulos | ✅ PASS |
| AC3 progresso em RUNNING | "<processedItems> de <totalItems>" | `RLT:75` `['Em andamento','3 de 10','—','—']`. F19 morre | ✅ PASS |
| AC4 Substituído | selo quando `supersededAt` | `RLT:77-79` `getByText('Substituído')`, `'ConcluídaSubstituído'`, ausente no run sem `supersededAt` | ✅ PASS |
| AC5 FAILED | mostra `errorMessage` | `RLT:80-81` `'FalhouFalha ao processar o lote'`. F22 morre | ✅ PASS |
| AC6 polling da lista | a cada 3 s com PENDING/RUNNING; para sem | `RLT:117-126` 1 chamada em 2998 ms, 2 em 3002 ms, 3 depois, ainda 3 após +15 s; `QRT:85-98` com `RUN_POLL_MS === 3000`; `QRT:101-109` sem run em andamento não recarrega. F3 e F4 morrem | ✅ PASS |
| AC7 filtro Janela | mês envia `fromDate`/`toDate` e volta à página 1; "Todas" remove | `RPT:132-134` 13 opções, do mês atual a Novembro de 2025; `RPT:135-139` sai da página 2, escolhe Setembro, `toEqual({page:'0',...,fromDate:'2026-09-01',toDate:'2026-09-30'})`; `RPT:143-144` sem `fromDate`/`toDate`. F5 e F6 morrem | ✅ PASS |
| AC8 paginação | "Anterior"/"Próxima"; desabilita nos extremos | `RLT:134-139` página 2 de 3 navega para 0 e 2; `RLT:145-154` Anterior desabilitado na primeira, Próxima na última | ✅ PASS |
| AC9 vazio | "Nenhuma conciliação encontrada." | `RLT:161` `findByText('Nenhuma conciliação encontrada.')` | ✅ PASS |
| AC10 erro | mensagem e "Tentar novamente" refaz | `RLT:171-176` mensagem do 403, clique, tabela volta, `toHaveBeenCalledTimes(2)` | ✅ PASS |
| AC11 Ver itens | abre `/conciliacoes/<runId>` | `RLT:65` `href '/conciliacoes/r-1'`; `RPT:123` ids da lista | ✅ PASS |

### P1 Disparo (REC-06, REC-07)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 abre no mês atual | "Mês" = mês atual; De/Até = 1º e último dia | `NDT:56-63` `'Outubro de 2026'`, `'2026-10-01'`, `'2026-10-31'`, 12 meses + Personalizado | ✅ PASS |
| AC2 mês e Personalizado | mês preenche De/Até; editar data → Personalizado | `NDT:69-72` Fevereiro → `2026-02-01`/`2026-02-28`; `NDT:75-82` De e Até → `'Personalizado'` | ✅ PASS |
| AC3 validação no cliente | mensagens exatas; não chama a API | `NDT:84-98` "Informe as duas datas" (De e Até vazios), "A data inicial deve ser igual ou anterior à final", "Janela de conciliação excede o máximo de 366 dias"; `runReconciliation` `not.toHaveBeenCalled()` | ✅ PASS |
| AC4 envio | `POST /reconciliations` com `{"fromDate","toDate"}` | `RPT:162-164` path e body `'{"fromDate":"2026-10-01","toDate":"2026-10-15"}'`; `NDT:126` | ✅ PASS |
| AC5 Conciliando… | botão desabilitado com "Conciliando…" | `NDT:123-124` `findByRole('button',{name:'Conciliando…'})` `toBeDisabled()` | ✅ PASS |
| AC6 aceito | fecha a gaveta, página 1, "Todas", run novo no topo "Na fila" | `RPT:151-153` filtro e página saem do padrão antes (L-036); `RPT:161` gaveta fecha; `RPT:165` filtro `''`; `RPT:166-168` `r-5` no topo, `['01/10/2026 – 15/10/2026','Na fila','—']`, `page:'0'` sem janela. F7 morre | ✅ PASS |
| AC7 recusa | `409`/`400` mantêm a gaveta com a `message` | `NDT:133-148` três mensagens, gaveta aberta, botão habilitado, `onStarted`/`onClose` não chamados; `QRT:215-229` nada invalidado | ✅ PASS |
| AC8 invalida o dashboard | consultas do dashboard da mesma janela recarregadas | `QRT:192-212` lista, `latestRun`, `period` e `summary` da janela já com `r-depois` quando a mutação resolve; `openDiscrepancies` e `discrepancy` invalidadas; outra janela não. F15 morre | ✅ PASS |

### P1 Itens (REC-08, REC-09)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 cabeçalho do detalhe | `GET /reconciliations/<runId>`; Janela, Status, Criado em, Itens, Conciliados, Divergentes, Substituído, "Voltar para conciliações" | `RDT:123` uma chamada a `/api/merchants/m-1/reconciliations/r-1`; `RDT:124` nome dentro da região (L-035); `RDT:125` `'Outubro de 2026'`; `RDT:126-128` totais exatos; `RDT:129` link `/conciliacoes`; `RDT:130,142` selo | ✅ PASS |
| AC2 colunas e "—" | busca `items?page=0&size=20&sort=absDifference,desc`; 8 colunas; "—" no lado ausente | `RDT:132-133` path e query exatos; `RIT:69-71` cabeçalhos; `RIT:73-76` linhas com `'—'` no liquidado; `RIT:86` órfã com `'—'` em data, método e esperado. F13 morre | ✅ PASS |
| AC3 selo de divergência | "<tipo> · <status>" | `RIT:95` `['Taxa divergente · Aberta','Status inconsistente · Aceita']`; `LRT:84-85`. F14 morre | ✅ PASS |
| AC4 filtro Resultado | `result=MATCHED/DIVERGENT`, página 1; "Todos" remove | `RIT:103` opções; `RIT:108,113` `filtros result=DIVERGENT`/`filtros result=MATCHED` na página 0 depois de sair da 1; `RIT:118-119` `toStrictEqual({})`, página 0; `ART:65-77` URL. F16 e F21 morrem | ✅ PASS |
| AC5 filtro Tipo | `discrepancyType=<tipo>`, página 1; "Todos" remove | `RIT:127` `['Todos', ...Object.values(TYPE_LABELS)]`; `RIT:133` `filtros result=DIVERGENT e discrepancyType=FEE_DIVERGENCE` página 0; `RIT:138-139` `filtros result=DIVERGENT`. F20 morre | ✅ PASS |
| AC6 paginação | como a lista | `RIT:146-155` "Página 1 de 2", Anterior desabilitado, Próxima desabilitado na última | ✅ PASS |
| AC7 vazio | "Nenhum item encontrado para os filtros escolhidos." | `RIT:162-164` mensagem, sem tabela, filtros visíveis | ✅ PASS |
| AC8 polling do detalhe | recarrega run e itens a cada 3 s até sair de `PENDING`/`RUNNING` | `RDT:217-253` conta chamadas e mostra "Concluída"; `RIT:188-213`; `QRT:113-132,155-168`. F18 morre. **Mas** nenhum teste prova que os itens mostrados depois da conclusão são os finais, e a sonda reproduz a tabela vazia (ver Defeito principal) | ❌ GAP |
| AC9 404 | "Conciliação não encontrada." e o link de volta | `RDT:149-153` texto, link `/conciliacoes`, sem cabeçalho e sem tabela, uma chamada; `QRT:134-140` sem retry. F10 morre | ✅ PASS |
| AC10 erro dos itens | mensagem e "Tentar novamente" | `RIT:174-178` mensagem do 403, clique refaz, 2 chamadas | ✅ PASS |

### P1 Export (REC-10)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 só COMPLETED | "Exportar CSV" só com `COMPLETED` | `RDT:169-179` ausente em RUNNING, PENDING e FAILED; `RDT:188` presente em COMPLETED. F8 morre | ✅ PASS |
| AC2 download | `GET .../export` salvo como `reconciliation-<runId>.csv` | `RDT:191` path do export; `RDT:194` `downloads` `toEqual(['reconciliation-r-1.csv'])`; `RDT:196` conteúdo do blob. F9 morre | ✅ PASS |
| AC3 Exportando… | desabilitado com "Exportando…" | `RDT:190` `getByRole('button',{name:'Exportando…'})` `toBeDisabled()` | ✅ PASS |
| AC4 erro | `message` junto do botão | `RDT:211-214` alerta com "Conciliação não encontrada com id: r-1" no mesmo contêiner do botão, sem download | ✅ PASS |

### P2 Empresa (REC-11)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 troca na lista | fecha a gaveta, mantém "Janela", página 1 da nova empresa | `RPT:175-179` filtro em Outubro, página 2 e gaveta aberta antes (L-036); `RPT:183` gaveta fechada; `RPT:184-185` `m-2` com `page:'0'` e a janela de outubro; `RPT:186` filtro mantido; `RPT:187` nome dentro do cabeçalho (L-035). F17 morre | ✅ PASS |
| AC2 troca no detalhe | navega para `/conciliacoes` | `RDT:263-265` `replace('/conciliacoes')` uma vez e nenhuma chamada sob `m-2`; `RDT:134,258` o primeiro carregamento não navega. F11 e F12b morrem | ✅ PASS |
| AC3 sem empresa | estado vazio, sem lista, filtro ou "Nova conciliação" | `RPT:195-199` "Nenhuma empresa no seu acesso", sem filtro, botão, tabela e chamadas | ✅ PASS |

**Status**: ❌ 38/39 ACs com evidência que discrimina; REC-09 AC8 em GAP.

---

## Edge Cases

- [x] 366 dias aceito, 367 recusado com a mensagem de limite: `LRT:48-49`, `NDT:88,101`; ano bissexto `LRT:53-55`. F1 e F2 morrem.
- [x] De igual a Até: `LRT:44`, `NDT:102` envia a janela de um dia.
- [x] RUNNING com `processedItems` nulo → "0 de <total>": `RLT:89` `'0 de 40'`, `LRT:65`. F19 morre.
- [x] Item sem divergências → coluna vazia: `RIT:73` último valor `''`.
- [x] Erro que não é JSON → "A API retornou erro <status>.": `RDT:156-167` com resposta 502 em texto passa pelo cliente real e mostra "A API retornou erro 502." e "Tentar novamente". `RLT:183` só injeta o `ApiError` já pronto.

**Status**: 5/5.

---

## Pontos pedidos pelo orquestrador

1. **Itens depois da conclusão.** Defeito real. Ver a seção acima. É o único gap que reprova.
2. **`itemsProgress` com `totalItems` nulo em COMPLETED/FAILED → "—"** (`frontend/lib/reconciliations.ts:43`, `LRT:78`). A spec não fala desse caso. Um run `FAILED` antes de `recordTotal` tem total nulo; "—" segue a convenção de valor ausente da spec ("Itens: lado ausente"). Aceito. Spec-precision gap menor.
3. **Diferença com sinal** (`RunItems.tsx:86`, `RIT:73-75,86`). O valor vem do servidor, sem aritmética (AD-010). O dashboard já mostra `netDifference` com `signed: true` (`OpenDiscrepancyTable.tsx:104`), e a spec pede "mesmos valores do dashboard". Aceito. A spec não fixa o formato: spec-precision gap menor.
4. **Comportamentos fora da spec.** `errorMessage` do FAILED no cabeçalho do detalhe (`RunDetailPage.tsx:91`): coerente com o Assumption "Run com falha", mas sem teste. Erro não-404 do run → `BlockError` com "Tentar novamente": coberto por `RDT:156-167`. `NoMerchant` no detalhe (`RunDetailPage.tsx:139`): coerente com REC-11 AC3, mas sem teste no detalhe. Mensagens de validação no alerta do `SideDrawer`: é o padrão das outras gavetas e é o que `NDT:95` verifica. Nenhum deles contradiz a spec.
5. **Troca de empresa no detalhe.** Correta. `RunDetailPage.tsx:131-136` só marca `switched` depois que uma empresa carregou. `RDT:265` prova que nenhum GET sai sob `m-2`. O guard `loadedMerchantId !== null` não tem efeito observável (F12 equivalente, ver sensor); F12b, um estado inicial errado de verdade, morre.
6. **L-035 e L-036.** Aplicadas e eficazes. L-035: `RPT:104,119,187` e `RDT:103,124` procuram o nome dentro da região da página. L-036: `RPT:151-153` e `RPT:175-179` tiram filtro e página do padrão antes do reset. Os mutantes F5, F7 e F17 morrem por isso.

---

## Discrimination Sensor

Scratch: `git worktree add --detach` em `scratchpad/rec-wt` a partir de `6496843`, com `frontend/node_modules` por symlink. Cada mutante frontend passou por `tsc --noEmit` (todos compilam) e rodou `vitest run` sobre os 8 arquivos de teste da feature, `lib/queries.test.tsx` e `components/dashboard` (248 testes). Os backend rodaram `./mvnw -o test -Dtest=ReconciliationIntegrationTest`. Revertido com `git checkout` no scratch.

| # | File | Mutação | Killed? |
| - | ---- | ------- | ------- |
| B1 | `ReconciliationController.java:95` | remove o `findRunById` antes do CSV | ✅ Killed (`BIT` 2 testes 404) |
| B2 | `ReconciliationController.java:95` | move o `findRunById` para depois dos cabeçalhos CSV | ✅ Killed (`BIT` 2 testes 404) |
| F1 | `lib/reconciliations.ts:36` | `> 366` → `>= 366` | ✅ Killed |
| F2 | `lib/reconciliations.ts:36` | `> 366` → `> 367` | ✅ Killed |
| F3 | `lib/queries.ts:205` | lista recarrega sempre | ✅ Killed |
| F4 | `lib/queries.ts:205` | lista nunca recarrega | ✅ Killed |
| F5 | `ReconciliationsPage.tsx:36` | filtro Janela sem `setPage(0)` | ✅ Killed |
| F6 | `ReconciliationsPage.tsx:35` | "Todas" envia o mês atual | ✅ Killed |
| F7 | `ReconciliationsPage.tsx:41` | disparo aceito não volta para "Todas" | ✅ Killed |
| F8 | `RunDetailPage.tsx:81` | export aparece fora de `COMPLETED` | ✅ Killed |
| F9 | `RunDetailPage.tsx:41` | arquivo `reconciliation_<id>.csv` | ✅ Killed |
| F10 | `RunDetailPage.tsx:19` | 404 tratado como erro genérico | ✅ Killed |
| F11 | `RunDetailPage.tsx:135` | troca de empresa não navega | ✅ Killed |
| F12 | `RunDetailPage.tsx:133` | tira `loadedMerchantId !== null` de `switched` | ⚪ Survived, equivalente: o único caso em que o guard decide (`loadedMerchantId` nulo e empresa carregada) é o render em que `setLoadedMerchantId` roda, e o React descarta esse render sem rodar o efeito |
| F12b | `RunDetailPage.tsx:131` | estado inicial `'m-anterior'` (navega no primeiro carregamento) | ✅ Killed |
| F13 | `RunItems.tsx:17` | lado ausente vira "R$ 0,00" | ✅ Killed |
| F14 | `lib/reconciliations.ts:47` | selo sempre com status "Aberta" | ✅ Killed |
| F15 | `lib/queries.ts:237` | disparo não invalida nada | ✅ Killed |
| F16 | `RunItems.tsx:122` | filtro Resultado sem `setPage(0)` | ✅ Killed |
| F17 | `ReconciliationsPage.tsx:28` | troca de empresa não fecha a gaveta | ✅ Killed |
| F18 | `RunDetailPage.tsx:99` | `polling = false` | ✅ Killed |
| F19 | `lib/reconciliations.ts:42` | `processedItems` nulo vira o total | ✅ Killed |
| F20 | `RunItems.tsx:125` | filtro Tipo sem `setPage(0)` | ✅ Killed |
| F21 | `lib/api.ts:362` | parâmetro `type` em vez de `discrepancyType` | ✅ Killed |
| F22 | `RunList.tsx:33` | `errorMessage` só em `RUNNING` | ✅ Killed |

Sonda (não é mutante): o código real com itens que só existem depois do commit falha em 3 de 6 latências. Ver Defeito principal.

**Sensor depth**: ampliado (25 mutantes, 2 backend e 23 frontend).
**Sensor**: 24/24 mortos entre os não equivalentes (F12 equivalente); nenhum sobrevivente real. A reprovação vem do gap de AC8.

**Isolamento**: `git status --porcelain` antes = vazio; depois da remoção do scratch = vazio (igual ao baseline). Scratch removido com `git worktree remove --force` + `git worktree prune`; `git worktree list` não lista mais o scratch. Nenhum `git stash`.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ |
| Surgical changes | ✅ `invalidateWindow` extraído sem mudar a lista de chaves; `useInvalidateWindow` chama a mesma função (`queries.ts:154-168`), dashboard inalterado |
| No scope creep | ✅ Extras do ponto 4 são pequenos e coerentes |
| Matches patterns | ✅ Mesmo padrão de `TransactionsPage`, `SideDrawer`, `BlockError`, `NoMerchant` |
| Spec-anchored outcome check | ❌ REC-09 AC8 sem prova dos itens finais |
| Per-layer Coverage Expectation | ✅ Backend integração; lib, hooks e componentes 1:1 com os ACs |
| Every test maps to a spec requirement | ✅ |
| Documented guidelines followed: `AGENTS.md`, `frontend/README.md` | ✅ |

---

## Gate Check

- **Backend**: `cd backend && ./mvnw verify` → exit 0. 632 testes, 0 falhas, 0 erros, 0 skipped. JaCoCo 94% linha, 87% branch.
- **Frontend**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build` → 58 arquivos, 740 passed, 0 failed, 0 skipped. Typecheck exit 0. Build exit 0, `/conciliacoes` estática e `/conciliacoes/[runId]` dinâmica.
- **Test count**: frontend 657 → 740 (+83; −1 por tirar `/conciliacoes` de `UnderConstruction.test.tsx`, previsto em `tasks.md:252`). Backend +2.
- **Skipped**: nenhum.

---

## Fix Plans

### Fix 1: itens não recarregam depois que o run conclui (Major)

- **Root cause**: `useRunItems` só recarrega por intervalo enquanto `polling` é verdadeiro (`frontend/lib/queries.ts:227`). O backend publica os itens no mesmo commit do `COMPLETED`. Quando o run sai de `PENDING`/`RUNNING`, nenhuma busca de itens é garantida depois disso.
- **Fix task**: em `RunDetail` (`frontend/components/reconciliations/RunDetailPage.tsx:96-99`), ao ver o run passar de em andamento para `COMPLETED`/`FAILED`, invalidar `['runItems', merchantId, runId]` com `queryClient.invalidateQueries` (por exemplo num `useEffect` que guarda o status anterior). Ou: `useRunItems` recebe o status do run e inclui um marcador de conclusão na chave. Teste novo em `RDT`: o servidor devolve itens vazios enquanto o run responde `RUNNING` e os itens reais só depois de responder `COMPLETED`, com o endpoint de itens mais lento que o do run (`setTimeout` no `fetch` fake); depois da conclusão, a tabela "Itens" mostra `REF-1`, e não há mais GET após +15 s.
- **Priority**: Major.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| REC-01 .. REC-08 | Design / Pending | ✅ Verified |
| REC-09 | Design / Pending | ❌ Needs Fix (AC8) |
| REC-10, REC-11 | Design / Pending | ✅ Verified |

**Docs desatualizados (reportados, não editados)**: `spec.md:193-209` ainda marca REC-01..REC-11 como "Design | Pending" e diz "0 mapped to tasks". `tasks.md:12` está com Status "In Progress". `frontend/README.md` diz "run na fila ou em andamento recarrega o run e os itens a cada 3 s"; continua certo, mas deve citar o recarregamento final dos itens depois da Fix 1.

---

## Lessons

- L-037 (candidata nova, `ac_gap`, REC-09 AC8): fixtures de polling devem mudar o dado servido conforme o estado do recurso pai, para provar que a tela mostra o dado final depois que o polling para.
- L-035 e L-036 foram aplicadas e preveniram as falhas que descrevem (F5, F7, F17 e o nome no cabeçalho morrem). `lessons.md` só promove por recorrência da falha; prevenção não dispara `add` nem `penalize`. Seguem candidatas.

---

## Summary

**Overall**: ❌ Not Ready

**Spec-anchored check**: 38/39 ACs e 5/5 edges; 1 gap (REC-09 AC8); 2 spec-precision gaps menores (total nulo fora de RUNNING, formato da Diferença)
**Sensor**: 24/24 não equivalentes mortos (25 injetados, F12 equivalente)
**Gate**: backend 632 passed; frontend 740 passed, typecheck e build OK

**What works**: export 404, lista com polling, filtro, paginação e reset, gaveta com validação e invalidação do dashboard, itens com filtros e "—", export CSV, isolamento por empresa.

**Issues found**: detalhe fica sem itens depois da conclusão de um run acompanhado ao vivo (Fix 1).

**Next steps**: implementar Fix 1 com o teste descrito e re-verificar (iteração 2 de 3).
