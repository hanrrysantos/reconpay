# Validação — drawer-focus

**Veredito: PASS**

- Diff: `2559dc8..1fb1861` (branch `fix/drawer-focus`, commit `1fb1861 fix(frontend): keep focus in the drawer after resolving`)
- Verifier independente (autor ≠ verifier), 2026-10-19
- Critério: `.specs/features/dashboard-redesign/spec.md:275` — "WHEN a resolution or reopening succeeds THEN the drawer SHALL keep keyboard focus so J/K keep navigating without another click."

## Evidência

| Item | Evidência | Resultado |
|---|---|---|
| Produção: foco volta ao painel depois do `await` bem-sucedido | `frontend/components/dashboard/ResolveDrawer.tsx:113` (dentro do `try`, após `mutateAsync` em `:110`) | OK |
| Resolver e reabrir passam pelo mesmo `save()` | `frontend/components/dashboard/ResolveDrawer.tsx:107` (`save`), `:127`/`:134` (resolver), `:296` (Reabrir → `save({ status: 'OPEN' })`) | OK |
| Teste do resultado da spec ao resolver: dialog com foco e `j` chama `onNavigate(1)` | `frontend/components/dashboard/ResolveDrawer.test.tsx:221` | OK |
| Teste do resultado da spec ao reabrir: idem | `frontend/components/dashboard/ResolveDrawer.test.tsx:233` | OK |
| Caminho 409/erro: a spec só exige mensagem + recarga (`.specs/features/dashboard-redesign/spec.md:144`); a edge de foco cobre só o sucesso. Teste existente segue verde | `frontend/components/dashboard/ResolveDrawer.test.tsx:187` | OK (sem exigência de foco) |
| Clique removido no teste de página. Observação: o clique saiu do teste "J e K andam… (DIV-07 AC7)", não do "edge de run novo" (`:368`, inalterado) | `frontend/components/discrepancies/DiscrepanciesPage.test.tsx:322` | Mais forte (ver abaixo) |

Sobre o clique removido: sem ele, o teste passa a depender do foco de montagem (`frontend/components/dashboard/ResolveDrawer.tsx:103`). Com o efeito de montagem removido (mutante M7), o teste agora falha; antes o clique na tabela mascarava essa falta. O teste também passa com a produção de `2559dc8`, ou seja, a remoção é independente da correção — fortalece, não enfraquece. Nenhum teste foi apagado, pulado ou relaxado.

## Sensor de discriminação

Worktree descartável em `1fb1861`, só `frontend/components/dashboard/ResolveDrawer.tsx` mutado e restaurado entre mutantes. Comando: `corepack pnpm vitest run components/dashboard/ResolveDrawer.test.tsx components/discrepancies/DiscrepanciesPage.test.tsx` (41 testes; base verde).

| Mutante | Falha injetada | Resultado | Testes que mataram |
|---|---|---|---|
| M1 | Remove `panel.current?.focus()` pós-sucesso (= produção anterior) | Morto | `ResolveDrawer.test.tsx:221`, `:233` |
| M2 | Foca só ao resolver (`status !== 'OPEN'`) | Morto | `ResolveDrawer.test.tsx:233` |
| M3 | Foca só ao reabrir (`status === 'OPEN'`) | Morto | `ResolveDrawer.test.tsx:221` |
| M4 | Foca antes do `await` (e não depois) | **Sobrevive** | — |
| M5 | Foca no `catch` em vez do `try` | Morto | `ResolveDrawer.test.tsx:221`, `:233` |
| M6 | Troca o foco por `blur()` do elemento ativo | Morto | `ResolveDrawer.test.tsx:221`, `:233` |
| M7 | Remove o foco de montagem (`:103`) | Morto | `DiscrepanciesPage.test.tsx:322` (só falha após a remoção do clique) + 3 do `ResolveDrawer.test.tsx` |

Classificação do sobrevivente M4: **equivalente quanto à spec**. No sucesso, o painel já tem o foco quando o botão some, então o estado final é idêntico ao da produção. A diferença aparece só em erro (o foco sai do botão que falhou), caminho sobre o qual a spec não diz nada.

Limpeza: `git worktree remove --force` + `git worktree prune`; `git status --porcelain` na árvore real ficou limpo antes desta validação.

## Gate (árvore real)

| Comando | Resultado |
|---|---|
| `cd frontend && corepack pnpm test` | 65 arquivos, 866 testes, todos verdes |
| `cd frontend && corepack pnpm typecheck` | `next typegen && tsc --noEmit` sem erros |

## Lacunas (fora da spec, sem bloqueio)

1. O foco no caminho de erro/409 não é especificado nem fixado por teste (M4 sobrevive por isso). Se o 409 recarregar um status que remove o botão, o foco pode sair da gaveta. Seria uma nova edge, se o produto quiser.
2. Os testes de foco não cobrem o resultado `ADJUSTED` (com valor). Usa o mesmo `save()` (`frontend/components/dashboard/ResolveDrawer.tsx:134`), então o risco é baixo.

---

## Depois do Verifier (autor)

O gap 3 tinha razão: um edit anterior (`8880ab8`) pôs o clique de contorno em dois testes de `frontend/components/discrepancies/DiscrepanciesPage.test.tsx`. A rodada verificada tirou só o do teste DIV-07. O clique do teste "edge de run novo" (`frontend/components/discrepancies/DiscrepanciesPage.test.tsx:369`) também saiu, no mesmo commit da correção (emendado, local). Esse teste agora passa só com a correção: sem o `panel.current?.focus()` de `frontend/components/dashboard/ResolveDrawer.tsx:113`, ele falha. Os gaps 1 e 2 ficam como estão (fora da spec).
