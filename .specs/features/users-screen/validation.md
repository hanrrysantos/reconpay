# Tela de Usuários Validation

**Verdict**: FAIL
**Result**: FAIL

**Date**: 2026-10-20
**Round**: 1 of max 3
**Spec**: `.specs/features/users-screen/spec.md`
**Diff range**: `ac73c03..43e0128` (código da feature: `924b8bc..43e0128`, 14 commits)
**Verifier**: independent sub-agent (author ≠ verifier)

Caminhos abreviados: `B/` = `backend/src/test/java/br/com/hanrry/reconpay/`, `M/` = `backend/src/main/java/br/com/hanrry/reconpay/`, `F/` = `frontend/`.

Veredito: os 6 requisitos (37 critérios) têm evidência no valor que a spec define, os gates passam e 15 das 17 mutações morreram. Falha por duas mutações sobreviventes, ambas em Edge Cases da spec que nenhum teste exercita: desativar de novo uma conta já inativa (Edge Case 1) e listar uma demo já expirada e ainda não apagada (Edge Case 4). O código atual se comporta como a spec pede nos dois casos; falta o teste que o prenda. Os fixes são só de teste.

---

## Task Completion

| Task | Status | Notes |
| --- | --- | --- |
| T1–T5 (backend) | ✅ Done | `924b8bc`, `93f1337`, `e4dc40f`, `42ef102`, `fcc025b` |
| T6–T7 (dados no frontend) | ✅ Done | `d90639f`, `06e3f1f` |
| T8–T13 (telas) | ✅ Done | `e904550`, `888577d`, `65d564c`, `3a7ca7c`, `a896dc9`, `ab3b69b` |
| T14 (docs) | ✅ Done | `43e0128` |

Todos os itens "Done when" de `tasks.md` estão marcados.

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
| AC4 `status` desconhecido | `400` | `B/auth/controller/UserAdministrationIntegrationTest.java:109-114` - `isBadRequest()`, `$.error` `"VALIDATION_ERROR"`; `B/auth/controller/UserControllerIntegrationTest.java:223-229` (serviço `never()` chamado) | ✅ PASS |
| AC5 sem `demo` / `demo=false` | exclui `demo_expires_at` preenchido | `B/auth/controller/UserAdministrationIntegrationTest.java:63-64` (sem `demo`) e `:73-74` (`demo=false`) - `containsExactly(activeRegular)` | ✅ PASS |
| AC6 `demo=true` | inclui demos que atendem ao `status` | `B/auth/controller/UserAdministrationIntegrationTest.java:69-72` - `ALL&demo=true` → os quatro; `ACTIVE&demo=true` → `containsExactly(activeRegular, activeDemo)` | ✅ PASS |
| AC7 `demoExpiresAt` em toda resposta | instante para demo, `null` para comum | `B/auth/controller/UserAdministrationIntegrationTest.java:104-105` - `containsExactly("2099-01-01T12:00:00Z")`; `B/auth/controller/UserControllerIntegrationTest.java:241-242` - `hasKey("demoExpiresAt")`, `value(nullValue())`; `B/auth/mapper/IUserMapperTest.java:47` `isNull()`, `:64` `isEqualTo(demoExpiresAt)` | ✅ PASS |
| AC8 tabela | nome, e-mail, papel, status, criação; ordem por nome; 20 por página | `F/components/users/UsersPage.test.tsx:65` - query `'status=ACTIVE&demo=false&page=0&size=20&sort=name%2Casc'`; `:66` colunas `['Nome','E-mail','Papel','Status','Cadastro','Ações']`; `:68-69` célula `['Bruno Operador','u-bruno@reconpay.dev','Operador','Ativo','01/10/2026']`; ordem na API `B/auth/controller/UserAdministrationIntegrationTest.java:84` `containsExactly(amanda, marcos, zelia)`, `:88` `$.size` `20` | ✅ PASS |
| AC9 filtros | abre em "Ativos" sem demos; "Ativos"/"Inativos"/"Todos"; "Mostrar demonstrações" | `F/components/users/UsersPage.test.tsx:71-74` (`aria-pressed` e checkbox desmarcado); `:77-90` `INACTIVE` e `ALL`; `:92-101` volta a `ACTIVE`; `:103-116` `demo=true` e volta a `false`; `:118-137` trocar filtro na página 2 volta a `page=0` (L-036) | ✅ PASS |
| AC10 selo da demo | "Demo · expira em dd/MM às HH:mm", horário local | `F/components/users/UsersPage.test.tsx:139-151` - TZ `America/Sao_Paulo` (`:11-15`); `textContent` `toBe('Demo · expira em 21/10 às 09:34')` para `2099-10-21T12:34:56Z` e `'20/10 às 23:05'` para `02:05Z` (virada de dia); linha comum sem selo | ✅ PASS |
| AC11 OPERATOR em `/usuarios` | `/painel` sem chamar `/api/users` | `F/components/users/UsersPage.test.tsx:163-170` - `replace` `toHaveBeenCalledWith('/painel')`, `listRequests()).toHaveLength(0)` | ✅ PASS |
| AC12 lista vazia | "Nenhum usuário encontrado." | `F/components/users/UsersPage.test.tsx:153-161` - `findByText('Nenhum usuário encontrado.')`, tabela ausente | ✅ PASS |

### USERS-03: Criar usuário

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 painel | nome, e-mail, senha, papel; `OPERATOR` pré-selecionado | `F/components/users/UserDrawer.test.tsx:48-57` - campos vazios, senha `type="password"`, radio `Operador` `toBeChecked()`, `Admin` `not.toBeChecked()` | ✅ PASS |
| AC2 senha fraca | mensagem exata e nada enviado | `F/components/users/UserDrawer.test.tsx:77-89` - três casos (curta, sem maiúscula, sem número), `getByText('Senha deve ter no mínimo 8 caracteres, uma letra maiúscula e um número')`, `posts()).toHaveLength(0)`; `:91-98` limite exato `Abcdefg1` é enviado | ✅ PASS |
| AC3 nome vazio / e-mail inválido | erro no campo e nada enviado | `F/components/users/UserDrawer.test.tsx:100-108` - `'Informe o nome'`, `aria-invalid`, `posts()` vazio; `:110-118` três e-mails inválidos, `'Informe um e-mail válido'` | ⚠️ Spec-precision gap (a spec não fixa o texto; o texto do nome está no `design.md:140`, o do e-mail não) |
| AC4 `201` | fecha, atualiza a lista, "Usuário criado." | `F/components/users/UserDrawer.test.tsx:59-75` - `status` `'Usuário criado.'`; corpo exato `{ name, email, password, role }` para `OPERATOR` e `ADMIN`; `dialog` ausente; linha nova na tabela; nova chamada de lista | ✅ PASS |
| AC5 `409` | `message` no painel, aberto com os dados | `F/components/users/UserDrawer.test.tsx:120-133` - `alert` `toHaveTextContent('E-mail já cadastrado')`; nome, e-mail, senha e radio `Admin` mantidos; sem `status` | ✅ PASS |
| AC6 envio em andamento | botão desabilitado | `F/components/users/UserDrawer.test.tsx:135-145` - resposta presa, `getByRole('button', { name: 'Criar' })).toBeDisabled()` | ✅ PASS |

### USERS-04: Desativar e reativar

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 confirmação antes do `DELETE` | diálogo; nada chamado antes | `F/components/users/DeactivateUserDialog.test.tsx:43-50` - `alertdialog` `'Desativar Bruno Operador?'`, `deletes()).toHaveLength(0)`; `:67-76` cancelar não chama a API | ✅ PASS |
| AC2 confirmar | desativa, atualiza lista, "Usuário desativado." | `F/components/users/DeactivateUserDialog.test.tsx:52-65` - OPERATOR, ADMIN e demo: `status` `'Usuário desativado.'`, `DELETE` `toEqual([`/api/users/${id}`])`, linha some | ✅ PASS |
| AC3 autodesativação | `409`, `CONFLICT`, "Você não pode desativar a própria conta.", conta intacta | `B/auth/controller/UserAdministrationIntegrationTest.java:118-131` - `isConflict()`, `$.error` `"CONFLICT"`, `$.message` exata, depois `GET` `$.active` `true`; `B/auth/service/UserServiceTest.java:253-265` - `hasMessage(...)`, `never()` `findById`/`save`/`deleteAllPendingByUserId` | ✅ PASS |
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
| AC5 `PUT` em demo | `409`, `CONFLICT`, mensagem exata, acessos intactos | `B/auth/controller/UserAdministrationIntegrationTest.java:147-168` - `isConflict()`, `$.error`, `$.message` `"Contas de demonstração não recebem acesso a outras empresas."`, depois `merchantIds` `[ownMerchant]`; `B/auth/service/UserMerchantAccessServiceTest.java:76-91` (`never()` `deleteAllByUserId`/`saveAll`) | ✅ PASS |
| AC6 linha de demo | só "Desativar" | `F/components/users/UsersPage.test.tsx:206-210` - `rowActions('Visitante Demo')).toEqual(['Desativar'])` | ✅ PASS |
| AC7 sem empresa ativa | "Nenhuma empresa cadastrada." | `F/components/users/AccessDrawer.test.tsx:89-96` - `findByText('Nenhuma empresa cadastrada.')`, zero checkboxes | ✅ PASS |

### USERS-06: Renomear usuário

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 "Renomear" | painel com o nome atual | `F/components/users/UserDrawer.test.tsx:166-173` - `toHaveValue('Bruno Operador')`, sem e-mail/senha/papel | ✅ PASS |
| AC2 vazio ou > 150 | erro no campo, nada enviado | `F/components/users/UserDrawer.test.tsx:175-186` - `'Informe o nome'` e `'Use no máximo 150 caracteres'`, `aria-invalid`, `renames()` vazio; `:188-194` 150 caracteres é enviado | ✅ PASS (texto do limite reutiliza `MerchantDrawer`, `design.md:140`) |
| AC3 `200` | atualiza lista, "Usuário renomeado." | `F/components/users/UserDrawer.test.tsx:196-209` - `status` `'Usuário renomeado.'`, `PUT` `[`/api/users/${id}`, { name: 'Zeca Renomeado' }]`, nome novo na tabela | ✅ PASS |
| AC4 conta inativa | sem "Renomear" | `F/components/users/UsersPage.test.tsx:212-216` - inativo comum e demo inativa `toEqual(['Reativar'])` | ✅ PASS |

**Status**: ❌ 37/37 critérios com evidência; 1 spec-precision gap (USERS-03 AC3); 2 Edge Cases sem teste (abaixo).

---

## Edge Cases

| Edge case | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| 1. Desativar conta já desativada por outra aba | `DELETE` responde `204` de novo; lista mostra a conta inativa | nenhum teste desativa uma conta já inativa (backend ou frontend). O código atende (`M/auth/service/UserService.java:104` usa `findById`, sem filtro de `active`), mas o mutante B7b sobrevive | ❌ GAP |
| 2. Empresa marcada desativada antes de salvar | `404`; `message` no painel, que continua aberto | `F/components/users/AccessDrawer.test.tsx:98-111` - `alert` `toHaveTextContent('Empresa não encontrada')`, checkboxes mantidas, sem `status`; API: `B/security/integration/MerchantIsolationIntegrationTest.java:107-113` `isNotFound()` (empresa inexistente; `requireMerchant` usa `findByIdAndActiveTrue`) | ✅ PASS |
| 3. Última página vazia após desativar em "Ativos" | volta à página anterior | `F/components/users/DeactivateUserDialog.test.tsx:88-99` - 21 ativos, página 2, desativa `Pessoa 20` → `rowNames()).toHaveLength(20)`, `[0]` `'Pessoa 00'`; também `F/components/users/UsersPage.test.tsx:236-250` (reativar em "Inativos") | ✅ PASS |
| 4. Demo expirada ainda não apagada | continua na lista com "Mostrar demonstrações" | nenhum teste lista uma demo com `demo_expires_at` no passado: as fixtures usam `now + 24h` ou `2099` (`B/auth/controller/UserAdministrationIntegrationTest.java:60-61`, `F/components/users/UsersPage.test.tsx:30-31`). O código atende (a query não olha a data), mas o mutante B8 sobrevive | ❌ GAP |

---

## Discrimination Sensor

Worktree descartável `git worktree add --detach <scratchpad>/wt HEAD`, `node_modules` por symlink do tree real; uma mutação por vez com `perl -0pi`, `git checkout -- <arquivo>` entre elas. Backend: `./mvnw -q test -Dtest=<classes> -Djacoco.skip=true`; frontend: `corepack pnpm exec vitest run <arquivos>`. Toda morte conferida no log como falha de asserção (nenhum erro de compilação).

| # | Arquivo (main) | Mutação | Testes | Resultado |
| --- | --- | --- | --- | --- |
| B1 | `M/security/JwtAuthenticationFilter.java:81-83` | remove o `if (!userDetails.isEnabled()) return;` | `DeactivatedAccountSessionIntegrationTest` | ✅ Killed (`tokenIssuedBeforeDeactivation...:70` 200 em vez de 401) |
| B2 | `M/auth/service/UserService.java:100` | trava de autodesativação vira `false && ...` | `UserServiceTest`, `UserAdministrationIntegrationTest` | ✅ Killed (`adminDeactivatingTheirOwnAccount...:123` 204 em vez de 409; `UserServiceTest:258`) |
| B3 | `M/auth/service/UserMerchantAccessService.java:46` | trava de demo em `replace` vira `if (false)` | `UserMerchantAccessServiceTest`, `UserAdministrationIntegrationTest` | ✅ Killed |
| B4 | `M/auth/repository/IUserRepository.java:27` | `demoExpiresAt is null` → `is not null` | `UserAdministrationIntegrationTest` | ✅ Killed |
| B5 | `M/auth/service/UserService.java:68` | `INACTIVE -> false` → `null` | `UserServiceTest`, `UserAdministrationIntegrationTest` | ✅ Killed (`listingShouldFilterByStatusAndDemoAccounts:66`) |
| B6 | `M/auth/dto/UserResponseDTO.java:31` | `@JsonIgnore` em `demoExpiresAt` | `UserAdministrationIntegrationTest`, `UserControllerIntegrationTest` | ✅ Killed |
| B7 | `M/auth/service/UserService.java:104` | `findById` → `getActiveUserOrThrow` (redesativar responde 404) | `UserServiceTest`, `UserAdministrationIntegrationTest`, `UserControllerIntegrationTest`, `DeactivatedAccountSessionIntegrationTest` | ⚠️ Killed só por stub do Mockito (`deleteByIdShouldSoftDeleteUser:232`), não por asserção do Edge Case 1; ver B7b |
| B7b | `M/auth/service/UserService.java:106` | lança `UserNotFoundException` se a conta já está inativa | mesmas 4 classes | ❌ Survived → Edge Case 1 |
| B8 | `M/auth/repository/IUserRepository.java:27` | esconde demos com `demoExpiresAt <= CURRENT_TIMESTAMP` | `UserAdministrationIntegrationTest`, `UserServiceTest` | ❌ Survived → Edge Case 4 |
| F1 | `F/components/shell/AccountMenu.tsx:56` | `{admin && (` → `{true && (` ("Usuários" para OPERATOR) | `AccountMenu.test.tsx` | ✅ Killed |
| F2 | `F/components/users/UsersPage.tsx:88` | "Desativar" também na linha do ADMIN logado | `components/users` | ✅ Killed |
| F3 | `F/components/users/UsersPage.tsx:63` | troca de filtro não volta à página 0 | `UsersPage.test.tsx` | ✅ Killed (3 testes) |
| F4 | `F/components/users/AccessDrawer.tsx:45` | envia as desmarcadas em vez das marcadas | `AccessDrawer.test.tsx` | ✅ Killed (3 testes) |
| F5 | `F/components/users/UsersPage.tsx:89` | demo ganha Acessos e Renomear | `UsersPage.test.tsx` | ✅ Killed |
| F6 | `F/components/users/UsersPage.tsx:56` | página vazia não volta à anterior | `components/users` | ✅ Killed (2 testes) |
| F7 | `F/components/shell/DemoBanner.tsx:11` | `formatDemoExpiry` muda o texto do selo | `UsersPage.test.tsx` | ✅ Killed (2 testes) |

**Sensor depth**: P0 (auth/sessão): 17 mutações, todos os ramos novos de risco.
**Result**: 15 killed, 2 survived (B7b, B8) - ❌ FAIL

**Isolamento**: `git status --porcelain` do tree real vazio antes e depois; worktree removido com `git worktree remove --force` e `git worktree prune`; `frontend/node_modules` real intacto.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ |
| Surgical changes | ✅ (`DemoBanner.tsx` só exporta `formatDemoExpiry` para o selo reutilizar o formato; `fakeUsersApi.ts` é helper de teste) |
| No scope creep | ✅ (erro de reativação em `role="alert"`: a spec é omissa, é o tratamento mínimo de falha) |
| Matches patterns | ✅ (`handleConflict`, `@PageableDefault`, `SideDrawer`, rotas em português) |
| Spec-anchored outcome check | ⚠️ 1 spec-precision gap (USERS-03 AC3) |
| Per-layer Coverage Expectation met | ❌ Edge Cases 1 e 4 sem teste |
| Every test maps to a spec requirement | ✅ (testes extras: erro da lista, erro de renomear e de desativar no diálogo, ADMIN não redirecionado; todos ligados a ACs ou Done-when) |
| Documented guidelines followed: `AGENTS.md`, `tasks.md` (Test Coverage Matrix) | ✅ |

---

## Gate Check

- **Backend**: `cd backend && ./mvnw verify` → exit 0; `Tests run: 762, Failures: 0, Errors: 0, Skipped: 0`; JaCoCo "All coverage checks have been met"; BUILD SUCCESS.
- **Frontend**: `corepack pnpm test` → 86 arquivos, 1074 passed; `corepack pnpm typecheck` ok; `corepack pnpm build` ok (rota `/usuarios` estática).
- **Contagem de métodos de teste** (`git grep`, `ac73c03` → `HEAD`): backend `@Test` 648 → 668 (+20); frontend `it(` 751 → 817 (+66). Nenhum teste removido; as edições em testes existentes (`AuthServiceTest`, `UserServiceTest`, `UserControllerIntegrationTest`, `ApiDocsIntegrationTest`, `TopNav.test.tsx`) acompanham o novo campo, o novo colaborador e os `409` novos, sem afrouxar asserção.
- **Skipped**: nenhum.

---

## Ranked Gaps

1. **Edge Case 1: redesativar responde `204` sem teste** (Minor). Mutante B7b sobrevive.
   - **Fix task**: em `UserAdministrationIntegrationTest`, desativar a mesma conta duas vezes e afirmar `status().isNoContent()` nas duas e `isActive()` `false` no fim. No frontend (`DeactivateUserDialog.test.tsx`), com a conta já inativa no fake (outra aba), confirmar e afirmar "Usuário desativado." e que a conta aparece em "Inativos".
2. **Edge Case 4: demo expirada e não apagada sem teste** (Minor). Mutante B8 sobrevive.
   - **Fix task**: em `UserAdministrationIntegrationTest.listingShouldFilterByStatusAndDemoAccounts` (ou teste próprio), salvar uma demo ativa com `demoExpiresAt = now - 1h` e afirmar que `demo=true` a lista e `demo=false` não.
3. **Spec-precision gap: USERS-03 AC3** (Nota, sem fix obrigatório). A spec diz "mostrar o erro no campo" sem o texto. O teste fixa "Informe o nome" (que `design.md:140` registra) e "Informe um e-mail válido" (sem registro na spec nem no design). Decidir se a spec passa a citar os dois textos.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| USERS-01 | Implementing | ✅ Verified |
| USERS-02 | Implementing | ✅ Verified |
| USERS-03 | Implementing | ✅ Verified (gap 3 é nota de spec) |
| USERS-04 | Implementing | ❌ Needs Fix (Edge Case 1) |
| USERS-05 | Implementing | ✅ Verified |
| USERS-06 | Implementing | ✅ Verified |

A tabela da spec continua em `Implementing` até a re-verificação (o Edge Case 4 pertence à listagem, mas não muda a evidência dos ACs de USERS-02).

---

## Summary

**Overall**: ❌ Not Ready (2 Edge Cases sem teste)

**Spec-anchored check**: 37/37 critérios com evidência no valor da spec; 1 spec-precision gap (USERS-03 AC3); 2 de 4 Edge Cases sem evidência
**Sensor**: 17 injetadas, 15 mortas, 2 sobreviventes (B7b, B8)
**Gate**: backend 762 passed; frontend 1074 passed, typecheck e build ok

**What works**: menu do avatar por papel, listagem com `status`/`demo` na API e na tela, selo de demo no horário local, criar, renomear, desativar com confirmação e trava de autodesativação, `401` imediato para sessão de conta desativada, reativação com login, editor de acessos com corpo exato e recusa `409` para demo.

**Next steps**: fix tasks 1 e 2 (só testes); decidir a nota 3; re-verificar (rodada 2 de 3).
