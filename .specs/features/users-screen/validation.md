# Tela de Usuários Validation

**Verdict**: PASS
**Result**: PASS

**Date**: 2026-10-20
**Round**: 2 of max 3 (rodada 1 FAIL em `057ebde`; fix em `bd2d2a6`)
**Spec**: `.specs/features/users-screen/spec.md`
**Diff range**: `ac73c03..bd2d2a6` (HEAD; código da feature `924b8bc..43e0128`; fix da rodada 1 `057ebde..bd2d2a6`, só testes e spec)
**Verifier**: independent sub-agent (author ≠ verifier)

Caminhos abreviados: `B/` = `backend/src/test/java/br/com/hanrry/reconpay/`, `M/` = `backend/src/main/java/br/com/hanrry/reconpay/`, `F/` = `frontend/`.

Veredito: PASS. Os três gaps da rodada 1 estão fechados com evidência: o Edge Case 1 (redesativar responde `204`) e o Edge Case 4 (demo expirada continua na lista) têm teste de integração que mata os mutantes B7b e B8, o frontend tem o teste da conta já desativada por outra aba, e a spec de USERS-03 AC3 passou a fixar os textos que o teste afirma. O código de produção não mudou desde a rodada 1 (`git diff 057ebde..HEAD` toca só `spec.md`, `UserAdministrationIntegrationTest.java` e `DeactivateUserDialog.test.tsx`); as citações abaixo foram reconferidas em HEAD, com números de linha atualizados onde os testes novos os deslocaram. Gates verdes; 8 de 8 mutações da rodada 2 mortas.

---

## Rodada 2: gaps da rodada 1

| Gap | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| 1. Edge Case 1: desativar conta já inativa | API responde `204` de novo; a lista mostra a conta inativa | API: `B/auth/controller/UserAdministrationIntegrationTest.java:158-169` - laço de duas chamadas `DELETE /api/users/{id}`, `:165` `status().isNoContent()` nas duas, `:168` `isActive()).isFalse()`. Tela: `F/components/users/DeactivateUserDialog.test.tsx:67-77` - conta já `active: false` no fake (outra aba), confirmar → `:73` `status` `toHaveTextContent('Usuário desativado.')`, `:74` linha some de "Ativos", `:76` em "Inativos" `rowNames()).toEqual(['Bruno Operador'])` | ✅ PASS (mata B7, B7b e F9) |
| 2. Edge Case 4: demo expirada ainda não apagada | continua na lista com "Mostrar demonstrações" | `B/auth/controller/UserAdministrationIntegrationTest.java:78-86` - demo ativa com `demoExpiresAt = now - 1h`; `:82-83` `demo=true` → `containsExactly(expiredDemo)`; `:84-85` `demo=false` → `isEmpty()` | ✅ PASS (mata B8) |
| 3. Spec-precision USERS-03 AC3 | "Informe o nome" / "Informe um e-mail válido" (`spec.md:113`) | `F/components/users/UserDrawer.test.tsx:105` `getByText('Informe o nome')`; `:115` `getByText('Informe um e-mail válido')` (três e-mails inválidos); ambos com `posts()` vazio | ✅ PASS |

---

## Task Completion

| Task | Status | Notes |
| --- | --- | --- |
| T1–T5 (backend) | ✅ Done | `924b8bc`, `93f1337`, `e4dc40f`, `42ef102`, `fcc025b` |
| T6–T7 (dados no frontend) | ✅ Done | `d90639f`, `06e3f1f` |
| T8–T13 (telas) | ✅ Done | `e904550`, `888577d`, `65d564c`, `3a7ca7c`, `a896dc9`, `ab3b69b` |
| T14 (docs) | ✅ Done | `43e0128` |
| Fix rodada 1 (gaps 1–3) | ✅ Done | `bd2d2a6` |

---

## Spec-Anchored Acceptance Criteria

### USERS-01: Menu do avatar

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 clique abre o menu | nome, e-mail, selo "Admin"/"Operador", item "Sair" | `F/components/shell/AccountMenu.test.tsx:34-48` - `it.each([admin,'Admin'],[operator,'Operador'])`; `:44` `getByText(account.name)`, `:45` `getByText(account.email)`, `:46` `getByText(badge)`, `:47` `getByRole('menuitem', { name: 'Sair' })` | ✅ PASS |
| AC2 ADMIN vê "Usuários" → `/usuarios` | item com `href="/usuarios"` | `F/components/shell/AccountMenu.test.tsx:54` - `getByRole('menuitem', { name: 'Usuários' })).toHaveAttribute('href', '/usuarios')` | ✅ PASS |
| AC3 OPERATOR não vê "Usuários" | item ausente | `F/components/shell/AccountMenu.test.tsx:63-64` - `queryByRole(... 'Usuários')).not.toBeInTheDocument()`; itens `toEqual(['Sair'])` | ✅ PASS |
| AC4 "Sair" no menu encerra a sessão | mesmo `signOut` do botão | `F/components/shell/AccountMenu.test.tsx:70-72` - `expect(signOut).toHaveBeenCalledOnce()` | ✅ PASS |
| AC5 botão "Sair" ao lado do avatar continua | botão presente e chama `signOut` | `F/components/shell/TopNav.test.tsx:75-87` - `:83` `getByRole('button', { name: 'Sair' })`, `:86` `signOut` `toHaveBeenCalledOnce()` | ✅ PASS |
| AC6 `Esc` ou clique fora fecha | menu some | `F/components/shell/AccountMenu.test.tsx:80-82` (`{Escape}` → `queryByRole('menu')).not.toBeInTheDocument()`, `aria-expanded` `'false'`); `:88-92` (clique dentro mantém; clique fora fecha) | ✅ PASS |

### USERS-02: Listar usuários

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 sem `status` | só ativos | `B/auth/controller/UserAdministrationIntegrationTest.java:63-64` - `containsExactly(activeRegular)` (fixture com ativo, inativo, demo ativa e demo inativa); `B/auth/controller/UserControllerIntegrationTest.java:201-207` - `findAllUsers(eq(UserStatusFilter.ACTIVE), eq(false), any())` | ✅ PASS |
| AC2 `status=INACTIVE` | só inativos | `B/auth/controller/UserAdministrationIntegrationTest.java:65-66` - `containsExactly(inactiveRegular)`; `B/auth/service/UserServiceTest.java:118` (`INACTIVE` → `active=false`) | ✅ PASS |
| AC3 `status=ALL` | ativos e inativos | `B/auth/controller/UserAdministrationIntegrationTest.java:67-68` - `containsExactly(activeRegular, inactiveRegular)`; `B/auth/service/UserServiceTest.java:123` (`ALL` → `null`) | ✅ PASS |
| AC4 `status` desconhecido | `400` | `B/auth/controller/UserAdministrationIntegrationTest.java:120-125` - `isBadRequest()`, `$.error` `"VALIDATION_ERROR"`; `B/auth/controller/UserControllerIntegrationTest.java:223-229` (serviço `never()` chamado) | ✅ PASS |
| AC5 sem `demo` / `demo=false` | exclui `demo_expires_at` preenchido | `B/auth/controller/UserAdministrationIntegrationTest.java:63-64` (sem `demo`) e `:73-74` (`demo=false`) - `containsExactly(activeRegular)` | ✅ PASS |
| AC6 `demo=true` | inclui demos que atendem ao `status` | `B/auth/controller/UserAdministrationIntegrationTest.java:69-72` - `ALL&demo=true` → os quatro; `ACTIVE&demo=true` → `containsExactly(activeRegular, activeDemo)` | ✅ PASS |
| AC7 `demoExpiresAt` em toda resposta | instante para demo, `null` para comum | `B/auth/controller/UserAdministrationIntegrationTest.java:115-116` - `containsExactly("2099-01-01T12:00:00Z")`; `B/auth/controller/UserControllerIntegrationTest.java:241-242` - `hasKey("demoExpiresAt")`, `value(nullValue())`; `B/auth/mapper/IUserMapperTest.java:47` `isNull()`, `:64` `isEqualTo(demoExpiresAt)` | ✅ PASS |
| AC8 tabela | nome, e-mail, papel, status, criação; ordem por nome; 20 por página | `F/components/users/UsersPage.test.tsx:65` - query `'status=ACTIVE&demo=false&page=0&size=20&sort=name%2Casc'`; `:66` colunas `['Nome','E-mail','Papel','Status','Cadastro','Ações']`; `:68-69` célula `['Bruno Operador','u-bruno@reconpay.dev','Operador','Ativo','01/10/2026']`; ordem na API `B/auth/controller/UserAdministrationIntegrationTest.java:95` `containsExactly(amanda, marcos, zelia)`, `:99` `$.size` `20` | ✅ PASS |
| AC9 filtros | abre em "Ativos" sem demos; "Ativos"/"Inativos"/"Todos"; "Mostrar demonstrações" | `F/components/users/UsersPage.test.tsx:71-74` (`aria-pressed` e checkbox desmarcado); `:77-90` `INACTIVE` e `ALL`; `:92-101` volta a `ACTIVE`; `:103-116` `demo=true` e volta a `false`; `:118-137` trocar filtro na página 2 volta a `page=0` (L-036) | ✅ PASS |
| AC10 selo da demo | "Demo · expira em dd/MM às HH:mm", horário local | `F/components/users/UsersPage.test.tsx:139-151` - TZ `America/Sao_Paulo` (`:11-15`); `textContent` `toBe('Demo · expira em 21/10 às 09:34')` para `2099-10-21T12:34:56Z` e `'20/10 às 23:05'` para `02:05Z` (virada de dia); linha comum sem selo | ✅ PASS |
| AC11 OPERATOR em `/usuarios` | `/painel` sem chamar `/api/users` | `F/components/users/UsersPage.test.tsx:163-170` - `replace` `toHaveBeenCalledWith('/painel')`, `listRequests()).toHaveLength(0)` | ✅ PASS |
| AC12 lista vazia | "Nenhum usuário encontrado." | `F/components/users/UsersPage.test.tsx:153-161` - `findByText('Nenhum usuário encontrado.')`, tabela ausente | ✅ PASS |

### USERS-03: Criar usuário

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 painel | nome, e-mail, senha, papel; `OPERATOR` pré-selecionado | `F/components/users/UserDrawer.test.tsx:48-57` - campos vazios, senha `type="password"`, radio `Operador` `toBeChecked()`, `Admin` `not.toBeChecked()` | ✅ PASS |
| AC2 senha fraca | mensagem exata e nada enviado | `F/components/users/UserDrawer.test.tsx:77-89` - três casos (curta, sem maiúscula, sem número), `getByText('Senha deve ter no mínimo 8 caracteres, uma letra maiúscula e um número')`, `posts()).toHaveLength(0)`; `:91-98` limite exato `Abcdefg1` é enviado | ✅ PASS |
| AC3 nome vazio / e-mail inválido | erro no campo e nada enviado | `F/components/users/UserDrawer.test.tsx:100-108` - `'Informe o nome'`, `aria-invalid`, `posts()` vazio; `:110-118` três e-mails inválidos, `'Informe um e-mail válido'` | ✅ PASS (rodada 2: `spec.md:113` agora fixa "Informe o nome" / "Informe um e-mail válido"; `:105` `getByText('Informe o nome')`, `:115` `getByText('Informe um e-mail válido')`) |
| AC4 `201` | fecha, atualiza a lista, "Usuário criado." | `F/components/users/UserDrawer.test.tsx:59-75` - `status` `'Usuário criado.'`; corpo exato `{ name, email, password, role }` para `OPERATOR` e `ADMIN`; `dialog` ausente; linha nova na tabela; nova chamada de lista | ✅ PASS |
| AC5 `409` | `message` no painel, aberto com os dados | `F/components/users/UserDrawer.test.tsx:120-133` - `alert` `toHaveTextContent('E-mail já cadastrado')`; nome, e-mail, senha e radio `Admin` mantidos; sem `status` | ✅ PASS |
| AC6 envio em andamento | botão desabilitado | `F/components/users/UserDrawer.test.tsx:135-145` - resposta presa, `getByRole('button', { name: 'Criar' })).toBeDisabled()` | ✅ PASS |

### USERS-04: Desativar e reativar

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 confirmação antes do `DELETE` | diálogo; nada chamado antes | `F/components/users/DeactivateUserDialog.test.tsx:43-50` - `alertdialog` `'Desativar Bruno Operador?'`, `deletes()).toHaveLength(0)`; `:79-88` cancelar não chama a API | ✅ PASS |
| AC2 confirmar | desativa, atualiza lista, "Usuário desativado." | `F/components/users/DeactivateUserDialog.test.tsx:52-65` - OPERATOR, ADMIN e demo: `status` `'Usuário desativado.'`, `DELETE` `toEqual([`/api/users/${id}`])`, linha some | ✅ PASS |
| AC3 autodesativação | `409`, `CONFLICT`, "Você não pode desativar a própria conta.", conta intacta | `B/auth/controller/UserAdministrationIntegrationTest.java:129-141` - `isConflict()`, `$.error` `"CONFLICT"`, `$.message` exata, depois `GET` `$.active` `true`; `B/auth/service/UserServiceTest.java:253-265` - `hasMessage(...)`, `never()` `findById`/`save`/`deleteAllPendingByUserId` | ✅ PASS |
| AC4 sem "Desativar" na própria linha | ausente | `F/components/users/UsersPage.test.tsx:200-204` - `rowActions('Ana Admin')).toEqual(['Renomear'])` | ✅ PASS |
| AC5 token de conta inativa | `401` | `B/security/integration/DeactivatedAccountSessionIntegrationTest.java:61-75` - mesmo token `200` antes, depois `GET /api/me` e `GET /api/merchants` `isUnauthorized()`, `$.error` `"UNAUTHORIZED"` | ✅ PASS |
| AC6 "Reativar" | `PATCH .../activation`, atualiza, "Usuário reativado." | `F/components/users/UsersPage.test.tsx:219-234` - `status` `'Usuário reativado.'`, `PATCH` `toEqual(['/api/users/u-davi/activation'])`, sem `alertdialog`, linha sai de "Inativos" | ✅ PASS |
| AC7 reativada volta a logar | login com a senha funciona | `B/security/integration/DeactivatedAccountSessionIntegrationTest.java:78-90` - `PATCH` `isOk()`, token antigo `GET /api/me` `$.email`, `login()` com `status().isOk()` (`:110-117`) | ✅ PASS |

### USERS-05: Acessos a empresas

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 painel de acessos | empresas ativas por nome, marcadas as atuais | `F/components/users/AccessDrawer.test.tsx:43-54` - rótulos em ordem alfabética, `checked` `toEqual([false, true, false])`, `/api/me/merchants` com `sort=name,asc`; botão "Acessos" só no OPERATOR comum `F/components/users/UsersPage.test.tsx:189-192` | ✅ PASS |
| AC2 salvar | `PUT` com exatamente as marcadas, "Acessos atualizados." | `F/components/users/AccessDrawer.test.tsx:56-68` - `puts().map(call => call.body)).toEqual([{ merchantIds: ['m-1', 'm-3'] }])`, `status` `'Acessos atualizados.'`; `:70-77` sem mexer reenvia `['m-2']` | ✅ PASS |
| AC3 nenhuma marcada | lista vazia; operador sem acesso | `F/components/users/AccessDrawer.test.tsx:79-87` - `toEqual([{ merchantIds: [] }])`; API: `B/security/integration/MerchantIsolationIntegrationTest.java:73-86` - `$.merchantIds` `isEmpty()` e depois `403` na empresa antes concedida | ✅ PASS |
| AC4 linha de ADMIN | "Acessa todas as empresas", sem "Acessos" | `F/components/users/UsersPage.test.tsx:194-198` - texto presente, `rowActions` `toEqual(['Renomear', 'Desativar'])` | ✅ PASS |
| AC5 `PUT` em demo | `409`, `CONFLICT`, mensagem exata, acessos intactos | `B/auth/controller/UserAdministrationIntegrationTest.java:172-193` - `isConflict()`, `$.error`, `$.message` `"Contas de demonstração não recebem acesso a outras empresas."`, depois `merchantIds` `[ownMerchant]`; `B/auth/service/UserMerchantAccessServiceTest.java:76-91` (`never()` `deleteAllByUserId`/`saveAll`) | ✅ PASS |
| AC6 linha de demo | só "Desativar" | `F/components/users/UsersPage.test.tsx:206-210` - `rowActions('Visitante Demo')).toEqual(['Desativar'])` | ✅ PASS |
| AC7 sem empresa ativa | "Nenhuma empresa cadastrada." | `F/components/users/AccessDrawer.test.tsx:89-96` - `findByText('Nenhuma empresa cadastrada.')`, zero checkboxes | ✅ PASS |

### USERS-06: Renomear usuário

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 "Renomear" | painel com o nome atual | `F/components/users/UserDrawer.test.tsx:166-173` - `toHaveValue('Bruno Operador')`, sem e-mail/senha/papel | ✅ PASS |
| AC2 vazio ou > 150 | erro no campo, nada enviado | `F/components/users/UserDrawer.test.tsx:175-186` - `'Informe o nome'` e `'Use no máximo 150 caracteres'`, `aria-invalid`, `renames()` vazio; `:188-194` 150 caracteres é enviado | ✅ PASS (texto do limite reutiliza `MerchantDrawer`, `design.md:140`) |
| AC3 `200` | atualiza lista, "Usuário renomeado." | `F/components/users/UserDrawer.test.tsx:196-209` - `status` `'Usuário renomeado.'`, `PUT` `[`/api/users/${id}`, { name: 'Zeca Renomeado' }]`, nome novo na tabela | ✅ PASS |
| AC4 conta inativa | sem "Renomear" | `F/components/users/UsersPage.test.tsx:212-216` - inativo comum e demo inativa `toEqual(['Reativar'])` | ✅ PASS |

**Status**: ✅ 37/37 critérios com evidência no valor da spec; nenhum spec-precision gap; 4/4 Edge Cases cobertos (abaixo).

---

## Edge Cases

| Edge case | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| 1. Desativar conta já desativada por outra aba | `DELETE` responde `204` de novo; lista mostra a conta inativa | `B/auth/controller/UserAdministrationIntegrationTest.java:165` `status().isNoContent()` (duas vezes), `:168` `isActive()).isFalse()`; `F/components/users/DeactivateUserDialog.test.tsx:73-76` "Usuário desativado." e conta em "Inativos" | ✅ PASS |
| 2. Empresa marcada desativada antes de salvar | `404`; `message` no painel, que continua aberto | `F/components/users/AccessDrawer.test.tsx:98-111` - `alert` `toHaveTextContent('Empresa não encontrada')`, checkboxes mantidas, sem `status`; API: `B/security/integration/MerchantIsolationIntegrationTest.java:107-113` `isNotFound()` | ✅ PASS |
| 3. Última página vazia após desativar em "Ativos" | volta à página anterior | `F/components/users/DeactivateUserDialog.test.tsx:100-111` - `rowNames()).toHaveLength(20)`, `[0]` `'Pessoa 00'`; `F/components/users/UsersPage.test.tsx:236-250` | ✅ PASS |
| 4. Demo expirada ainda não apagada | continua na lista com "Mostrar demonstrações" | `B/auth/controller/UserAdministrationIntegrationTest.java:83` `containsExactly(expiredDemo)` com `demo=true`, `:85` `isEmpty()` com `demo=false` | ✅ PASS |

---

## Discrimination Sensor

Rodada 2. Worktree descartável `git worktree add --detach <scratchpad>/wt HEAD` (`bd2d2a6`), `frontend/node_modules` por symlink; uma mutação por vez com `perl -0pi`, `git checkout -- <arquivo>` entre elas. Backend: `./mvnw -q test -Dtest=UserServiceTest,UserAdministrationIntegrationTest,UserControllerIntegrationTest,DeactivatedAccountSessionIntegrationTest,UserMerchantAccessServiceTest,MerchantIsolationIntegrationTest -Djacoco.skip=true`; frontend: `corepack pnpm exec vitest run components/users`. Toda morte conferida no log como falha de asserção.

| # | Arquivo | Mutação | Resultado |
| --- | --- | --- | --- |
| B7b | `M/auth/service/UserService.java:104-106` | lança `UserNotFoundException` se a conta já está inativa (sobrevivente da rodada 1) | ✅ Killed - `UserAdministrationIntegrationTest.java:165` `Status expected:<204> but was:<404>` |
| B7 | `M/auth/service/UserService.java:104` | `findById` → `getActiveUserOrThrow` (lookup só de ativos) | ✅ Killed - `UserAdministrationIntegrationTest.java:165` 404 em vez de 204 (agora por asserção do Edge Case 1, não só pelo stub de `UserServiceTest:232`) |
| B8 | `M/auth/repository/IUserRepository.java:27` | `and (user.demoExpiresAt is null or user.demoExpiresAt > CURRENT_TIMESTAMP)` na listagem (sobrevivente da rodada 1) | ✅ Killed - `UserAdministrationIntegrationTest.java:83` `containsExactly(expiredDemo)` falha |
| B9 (novo) | `M/auth/service/UserService.java:59` | `activate` grava `setActive(false)` | ✅ Killed - `DeactivatedAccountSessionIntegrationTest.reactivationShouldRestoreTheOldTokenAndThePasswordLogin:87` 401 em vez de 200 |
| B10 (novo) | `M/auth/service/UserMerchantAccessService.java:53` | remove `accessRepository.deleteAllByUserId(userId)` (substituição vira soma) | ✅ Killed - `MerchantIsolationIntegrationTest.revokingAccessShouldRemoveVisibility:85` 200 em vez de 403 |
| F8 (novo) | `F/lib/queries.ts:356` | `useDeactivateUser` sem `onSuccess: invalidate` (lista não atualiza) | ✅ Killed - 5 testes de `DeactivateUserDialog.test.tsx`, inclusive o do Edge Case 1 |
| F9 (análogo de B7b) | `F/components/users/fakeUsersApi.ts:69` | o fake da API responde `404` ao `DELETE` de conta já inativa | ✅ Killed - só `DeactivateUserDialog.test.tsx` "desativar quem outra aba já desativou…" falha |

Nota: o frontend não distingue conta já inativa (o diálogo só chama `DELETE`); o análogo F9 muda o contrato do fake para provar que o teste novo detecta uma API que deixasse de ser idempotente.

**Sensor depth**: P0 (auth/sessão): 7 mutações nesta rodada, somadas às 17 da rodada 1 (15 mortas lá; as 2 sobreviventes, B7b e B8, mortas agora).
**Result**: 7/7 killed - ✅ PASS

**Isolamento**: `git status --porcelain` do tree real vazio antes e depois (`diff` idêntico ao baseline); worktree removido com `git worktree remove --force` e `git worktree prune`; `frontend/node_modules` real intacto.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ |
| Surgical changes | ✅ (`DemoBanner.tsx` só exporta `formatDemoExpiry` para o selo reutilizar o formato; `fakeUsersApi.ts` é helper de teste) |
| No scope creep | ✅ (erro de reativação em `role="alert"`: a spec é omissa, é o tratamento mínimo de falha) |
| Matches patterns | ✅ (`handleConflict`, `@PageableDefault`, `SideDrawer`, rotas em português) |
| Spec-anchored outcome check | ✅ (USERS-03 AC3 fixado na spec na rodada 2) |
| Per-layer Coverage Expectation met | ✅ (Edge Cases 1 e 4 cobertos em `bd2d2a6`) |
| Every test maps to a spec requirement | ✅ (os 3 testes novos da rodada 2 ligam-se aos Edge Cases 1 e 4; testes extras: erro da lista, erro de renomear e de desativar no diálogo, ADMIN não redirecionado; todos ligados a ACs ou Done-when) |
| Documented guidelines followed: `AGENTS.md`, `tasks.md` (Test Coverage Matrix) | ✅ |

---

## Gate Check

- **Backend**: `cd backend && ./mvnw verify` → exit 0; `Tests run: 764, Failures: 0, Errors: 0, Skipped: 0`; JaCoCo "All coverage checks have been met"; BUILD SUCCESS.
- **Frontend**: `corepack pnpm test` → 86 arquivos, 1075 passed; `corepack pnpm typecheck` ok; `corepack pnpm build` ok (rota `/usuarios` estática).
- **Contagem**: backend 762 → 764 (+2, os dois testes de integração do fix); frontend 1074 → 1075 (+1). Nenhum teste removido ou afrouxado em `057ebde..HEAD` (só inserções nos testes).
- **Skipped**: nenhum.

---

## Ranked Gaps

Nenhum.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| USERS-01 | Implementing | ✅ Verified |
| USERS-02 | Implementing | ✅ Verified |
| USERS-03 | Implementing | ✅ Verified |
| USERS-04 | Implementing | ✅ Verified |
| USERS-05 | Implementing | ✅ Verified |
| USERS-06 | Implementing | ✅ Verified |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 37/37 critérios no valor da spec; 0 spec-precision gaps; 4/4 Edge Cases com evidência
**Sensor**: rodada 2 com 7 injetadas e 7 mortas (B7b e B8 da rodada 1 incluídas)
**Gate**: backend 764 passed; frontend 1075 passed, typecheck e build ok

**What works**: menu do avatar por papel, listagem com `status`/`demo` na API e na tela (demo expirada incluída até a limpeza), selo de demo no horário local, criar, renomear, desativar idempotente com confirmação e trava de autodesativação, `401` imediato para sessão de conta desativada, reativação com login, editor de acessos com corpo exato e recusa `409` para demo.

**Next steps**: nenhum na feature; segue o fluxo de push/PR por pedido do usuário.
