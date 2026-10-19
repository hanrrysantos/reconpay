# Pendências pós-merge Validation

**Date**: 2026-10-18
**Specs**: `.specs/features/merchants-screen/spec.md` (MDES-07), `.specs/features/discrepancies-screen/spec.md` (edge de run novo), `.specs/features/dashboard-redesign/spec.md` (edge de run novo); exemplo do Swagger coberto por `ApiDocsIntegrationTest`.
**Diff range**: `1db4744..22203aa` (3 commits: `3f56e91`, `d53b04e`, `22203aa`), branch `fix/pending-items`.
**Verifier**: sub-agente independente (author ≠ verifier), iteração 1.

## Validation: pending-items (iteração 1) - PASS ✅

As três correções entregam o resultado que a spec define, e cada teste novo falha quando o comportamento é quebrado. O sensor injetou 16 mutantes: 12 morreram, 4 sobreviveram e os 4 são equivalentes no resultado observável (justificativa abaixo). Gate do frontend verde: 864 testes em 65 arquivos, typecheck limpo. O teste de backend novo passa e falha com o CNPJ antigo.

Nenhum teste foi enfraquecido ou removido: o diff de teste só acrescenta `it`/`@Test` e um helper (`collectDocuments`).

Ficam três spec-precision gaps, nenhum bloqueante (ver "Gaps").

Abreviações: `MP` = `frontend/components/merchants/MerchantsPage.tsx`, `MPT` = `frontend/components/merchants/MerchantsPage.test.tsx`, `DP` = `frontend/components/discrepancies/DiscrepanciesPage.tsx`, `DPT` = `frontend/components/discrepancies/DiscrepanciesPage.test.tsx`, `BP` = `frontend/components/dashboard/DashboardPage.tsx`, `BPT` = `frontend/components/dashboard/DashboardPage.test.tsx`, `RD` = `frontend/components/dashboard/ResolveDrawer.tsx`, `ADT` = `backend/src/test/java/br/com/hanrry/reconpay/openapi/ApiDocsIntegrationTest.java`, `MCA` = `backend/src/main/java/br/com/hanrry/reconpay/auth/openapi/MeControllerApi.java`.

---

## Evidência por critério

| Critério | Produção | Teste | Resultado da spec afirmado? | Status |
| --- | --- | --- | --- | --- |
| Exemplo de `document` no Swagger é CNPJ válido | `MCA:80` (`12345678000195`) | `ADT:365-374` percorre `/v3/api-docs` inteiro (inclusive exemplos em string JSON, `ADT:1103-1123`) e valida cada `document` com `CnpjRules.isValid`; `isNotEmpty` impede passar com zero exemplos | Sim: afirma a propriedade do documento publicado, não o literal de um arquivo | ✅ |
| MDES-07: página vazia que não é a primeira mostra a nova última página | `MP:50-53` (`useEffect` que volta para `totalPages - 1`) | `MPT:567-588`: 3 empresas, 1 por página, vai à página 3, desativa Gama; afirma Beta na tela, último pedido com `page=1`, Gama ausente, "Página 2 de 2" e "Próxima" desabilitado | Sim: afirma o que o usuário vê (linha, indicador de página, navegação) | ✅ |
| Divergências: com run novo, a gaveta fica no run em que abriu, só para consulta | `DP:60`, `DP:111` (pin em `onOpen`), `DP:119` (`runId={drawer.runId}`), `DP:124` (lock quando o run mudou) | `DPT:368-382`: abre a gaveta, injeta `r-new`, aceita; afirma status "Aceita", que `r-new` chegou ao cliente, sem "Reabrir", e nenhuma chamada de `/discrepancies/` fora de `r-oct` | Sim: run lido (todas as chamadas no run original) e ausência de ação | ✅ |
| Dashboard: idem | `BP:104`, `BP:270-273` (pin), `BP:284` (`runId={drawer.runId}`), `BP:288` (lock) | `BPT:197-211`: mesmo roteiro com `run-new`; mesmas quatro afirmações | Sim | ✅ |

---

## Sensor de discriminação

Worktree isolado em `22203aa` (`node_modules` do frontend por symlink), um mutante por vez, só o arquivo de produção restaurado entre mutantes. Frontend: `corepack pnpm vitest run <arquivo de teste>`. Backend: `./mvnw -q test -Dtest=ApiDocsIntegrationTest#everyDocumentExampleIsAValidCnpj -Djacoco.skip=true`.

| ID | Arquivo | Mutação | Resultado | Teste que matou |
| --- | --- | --- | --- | --- |
| C1 | `MCA:80` | CNPJ volta para `12345678000199` | Morto | `ADT:366` (falha com `[12345678000199]`) |
| M1 | `MP:52` | sempre volta para a página 0 | Morto | `MPT:567` |
| M2 | `MP:52` | nunca volta | Morto | `MPT:567` |
| M3 | `MP:52` | volta para `totalPages` em vez de `totalPages - 1` | Morto | `MPT:567` |
| M4 | `MP:52` | `page > 0` vira `page >= 0` | Sobreviveu (equivalente) | — |
| M5 | `MP:52` | volta para `page - 1` em vez de `totalPages - 1` | Sobreviveu (equivalente no resultado) | — |
| M6 | `MP:52` | volta mesmo com a página cheia | Morto | `MPT:567` |
| D1 | `DP:119`, `DP:124` | sem pin e sem lock (código pré-fix) | Morto | `DPT:368` |
| D2 | `DP:124` | sem lock (`locked={locked}`) | Morto | `DPT:368` |
| D3 | `DP:119` | lock mantido, detalhe lê o run atual | Morto | `DPT:368` |
| D4 | `DP:115` | gate `drawer && current &&` restaurado | Sobreviveu (equivalente) | — |
| B1 | `BP:284`, `BP:288` | sem pin e sem lock (código pré-fix) | Morto | `BPT:197` |
| B2 | `BP:288` | sem lock | Morto | `BPT:197` |
| B3 | `BP:284` | lock mantido, detalhe lê o run atual | Morto | `BPT:197` |
| B4 | `BP:270` | sem o guard `if (!tableRunId) return` | Sobreviveu (equivalente) | — |
| B5 | `BP:280` | gate `drawer && tableRunId &&` restaurado | Sobreviveu (equivalente) | — |

**Placar**: 16 mutantes, 12 mortos, 4 sobreviventes equivalentes, 0 sobreviventes não equivalentes.

Equivalentes:

- **M4**: com `page === 0` e lista vazia, o servidor devolve `totalPages = 0`, então o efeito chama `setPage(0)` sobre o estado `0` e o React não renderiza de novo. Não há estado alcançável com página 0 vazia e `totalPages > 0`.
- **M5**: `page - 1` repete a volta uma página por vez até achar uma página com itens. O usuário termina na mesma última página que `totalPages - 1` mostra, só com pedidos intermediários a mais quando outra aba remove várias páginas de uma vez. A spec define o resultado ("mostrar a nova última página"), não quantos pedidos levam até ele. Ver gap 2.
- **D4 / B5**: `current` e `tableRunId` só viram `undefined`/`null` quando a consulta do run ou do período fica sem dados. O React Query mantém os dados anteriores durante refetch e em erro, e a gaveta é modal, então a janela não muda com ela aberta. O gate removido não tem caminho alcançável com a gaveta aberta.
- **B4**: `onResolve` só é chamado por uma linha da tabela, e as linhas vêm de `useOpenDiscrepancies(merchantId, tableRunId, …)` (`BP:121`), que não traz linhas sem `tableRunId`. O guard existe para o tipo, não para o comportamento.

Worktree removido (`git worktree remove --force` + `git worktree prune`). `git status --porcelain` da árvore real continuou vazio antes de este arquivo ser escrito.

---

## Gaps (ordenados por importância, nenhum bloqueante)

1. **Texto da gaveta no estado "run novo"**. O lock reaproveita o `locked` da gaveta, que mostra "Período travado · somente leitura" (`RD:200`). Com o período aberto e só o run trocado, a frase está errada: o período não está travado. As duas specs dizem "só para consulta" / "no resolve or reopen action", mas não definem o texto, e os testes não afirmam a frase. Sugestão: definir o texto na spec (por exemplo "Um run mais novo substituiu este · somente leitura") e afirmá-lo em `DPT:368` e `BPT:197`.
2. **MDES-07 "em outra aba" com mais de uma página removida**. `MPT:567` cobre só a perda de uma página por uma desativação feita aqui. O caso de outra aba esvaziar várias páginas não tem teste. Por isso M5 sobrevive, mesmo convergindo para o mesmo resultado. Um caso com `totalPages` caindo de 4 para 2 enquanto a página 4 está aberta, afirmando um único pedido de volta (`page=1`), fecharia isso.
3. **Ação de resolver em item ainda aberto no run antigo**. Os testes afirmam a ausência de "Reabrir" depois de aceitar, mas não navegam (J/K) para outro item ainda `OPEN` do run antigo para afirmar que "Aceitar"/"Salvar ajuste"/"Baixar" também somem. D2 e B2 já morrem pelo "Reabrir", então o lock está coberto. O que falta é afirmar o lado "resolve" da frase da spec.

---

## Gate

| Comando | Onde | Resultado |
| --- | --- | --- |
| `corepack pnpm test` | `frontend/` (árvore real) | ✅ 65 arquivos, 864 testes passando |
| `corepack pnpm typecheck` | `frontend/` (árvore real) | ✅ `next typegen && tsc --noEmit` sem erros |
| `./mvnw -q test -Dtest=ApiDocsIntegrationTest#everyDocumentExampleIsAValidCnpj -Djacoco.skip=true` | `backend/` (worktree isolado em `22203aa`) | ✅ 1 teste, 0 falhas (com C1: 1 falha) |
| `./mvnw verify` | `backend/` | Não rodado aqui (roda à parte, por instrução do orquestrador) |

---

## Depois do Verifier (autor, não Verifier)

Gaps 1 e 3 fechados em commit próprio, depois desta iteração:

- **Gap 1**: a gaveta ganhou `lockedNotice` (`frontend/components/dashboard/ResolveDrawer.tsx`, `STALE_RUN_NOTICE`). Com um run novo, as duas páginas mostram "Há uma conciliação mais nova nesta janela · somente leitura" em vez de "Período travado · somente leitura". As duas specs agora citam esse texto. `DPT` e `BPT` afirmam o aviso novo e a ausência do antigo; tirar o `lockedNotice` de qualquer página derruba o teste da página.
- **Gap 3**: `DPT` passa com J para a divergência ainda aberta do run antigo e afirma que não há rádios nem Aceitar, Salvar ajuste ou Baixar.
- **Gap 2** fica como está: o mutante `page - 1` chega à mesma página final, só com pedidos a mais.

Observação fora da spec: depois de resolver, o rádio some e o foco sai da gaveta, então J/K só voltam a funcionar depois de clicar dentro dela. Isso já acontecia antes destas correções.

### Evidência com caminho completo

- `backend/src/main/java/br/com/hanrry/reconpay/auth/openapi/MeControllerApi.java:80`, `backend/src/test/java/br/com/hanrry/reconpay/openapi/ApiDocsIntegrationTest.java:366`
- `frontend/components/merchants/MerchantsPage.tsx:52`, `frontend/components/merchants/MerchantsPage.test.tsx:567`
- `frontend/components/discrepancies/DiscrepanciesPage.tsx:119`, `frontend/components/discrepancies/DiscrepanciesPage.test.tsx:369`
- `frontend/components/dashboard/DashboardPage.tsx:284`, `frontend/components/dashboard/DashboardPage.test.tsx:197`, `frontend/components/dashboard/ResolveDrawer.tsx:205`
