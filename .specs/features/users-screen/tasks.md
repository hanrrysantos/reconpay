# Tela de Usuários Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/users-screen/design.md`
**Status**: Approved

---

## Test Coverage Matrix

> Gerada do código, das diretrizes do projeto e da spec; confirmar antes do Execute. Diretrizes encontradas: `AGENTS.md` (não enfraquecer testes; integração herda `AbstractIntegrationTest`; JaCoCo 85% linha e 75% branch), `frontend/package.json` (`vitest run`, `typecheck`), `.github/workflows/ci.yml`. Lições aplicadas: L-018 (cada valor listado num AC é exercitado: `ACTIVE`, `INACTIVE`, `ALL`; `demo` `true` e `false`; os quatro tipos de linha da tabela de ações; "Ativos", "Inativos" e "Todos" na tela), L-036 (ao trocar de filtro, a página sai de 0 antes, para provar que volta a 0).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Backend serviço (`UserService`, `UserMerchantAccessService`) | unit (Mockito) | Cada ramo novo: trava de autodesativação, trava de demo, mapeamento de `status` para o repositório; a recusa não grava nada | `backend/src/test/java/**/auth/service/*Test.java` | `cd backend && ./mvnw -q test -Dtest='<Class>' -Djacoco.skip=true` |
| Backend endpoints, repositório e filtro JWT | integration (`AbstractIntegrationTest`, Testcontainers) | Happy + cada edge + cada erro da spec, conferidos pela API: os 3 `status`, `demo` nos 2 valores, `400`, os dois `409`, `401` com token anterior à desativação, login depois de reativar | `backend/src/test/java/**/auth/controller/*IntegrationTest.java`, `security/integration/` | idem |
| Backend mapper | unit | `demoExpiresAt` preenchido e nulo | `backend/src/test/java/**/auth/mapper/IUserMapperTest.java` | idem |
| Backend Swagger | integration | `status` e `demo` documentados na listagem; `409` no `DELETE` e no `PUT .../merchants` | `backend/src/test/java/**/openapi/ApiDocsIntegrationTest.java` | `cd backend && ./mvnw -q test -Dtest=ApiDocsIntegrationTest` |
| Frontend lib (`lib/api.ts`, `lib/queries.ts`) | unit (vitest) | Método, caminho, query string e corpo de cada função; cada mutação invalida a listagem | `frontend/lib/api.users.test.ts`, `frontend/lib/queries.users.test.tsx` | `cd frontend && corepack pnpm test` |
| Frontend componentes (`AccountMenu`, `TopNav`, `UsersPage`, `UserDrawer`, `AccessDrawer`, `DeactivateUserDialog`) | unit (jsdom) | 1:1 com os ACs de USERS-01 e USERS-02 AC8–12 a USERS-06; textos exatos da spec; erros `409`/`404` mantêm o painel aberto | `frontend/components/{shell,users}/*.test.tsx` | `cd frontend && corepack pnpm test` |
| Rota fina (`app/(app)/usuarios/page.tsx`) | none | - (renderiza `UsersPage`; build gate) | - | build gate only |
| Docs | none | - | - | build gate only |

## Gate Check Commands

> Gerados do código; confirmar antes do Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick (backend) | Tasks de backend | `cd backend && ./mvnw -q test -Dtest='<classes da task>' -Djacoco.skip=true` |
| Quick (frontend) | Tasks de frontend | `cd frontend && corepack pnpm test && corepack pnpm typecheck` |
| Full | Fim da fase de backend e fim da feature | `cd backend && ./mvnw verify` |
| Build | Fim das fases de frontend e docs | `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build` |

---

## Execution Plan

Phases run sequentially; tasks within a phase run in order.

### Phase 1: Backend

```
T1
T1 → T2
T3
T3 → T4
T5
```

### Phase 2: Dados no frontend

```
T6
T6 → T7
```

### Phase 3: Telas

```
T8
T9
T9 → T10
T9 → T11
T9 → T12
T10 → T13
```

### Phase 4: Docs

```
T14
```

---

## Task Breakdown

### T1: `demoExpiresAt` na resposta de usuário

**What**: `UserResponseDTO` ganha `Instant demoExpiresAt` no fim do record, com `@Schema`; o MapStruct mapeia pelo nome.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/auth/dto/UserResponseDTO.java`
**Depends on**: None
**Reuses**: `IUserMapper`
**Requirement**: USERS-02 (AC7)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `IUserMapperTest`: usuário de demo → `demoExpiresAt` igual ao da entidade; conta comum → `null`
- [x] `UserControllerIntegrationTest`: `GET /api/users/{id}` de conta comum traz `demoExpiresAt: null`
- [x] Gate quick (backend) passa com `IUserMapperTest,UserControllerIntegrationTest`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(auth): expose the demo expiry on user responses`

---

### T2: Listagem com `status` e `demo`

**What**: `UserStatusFilter` (`ACTIVE`, `INACTIVE`, `ALL`); `IUserRepository.findAllFiltered(Boolean active, boolean includeDemo, Pageable)` com `@Query`; `UserService.findAllUsers(status, demo, pageable)`; `@RequestParam(defaultValue = "ACTIVE") UserStatusFilter status` e `@RequestParam(defaultValue = "false") boolean demo` no controller e na interface `UserControllerApi`, com descrição e exemplo atualizados. Remove `findAllByActiveTrue` se ficar sem uso.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/auth/service/UserService.java`
**Depends on**: T1
**Reuses**: `@PageableDefault(size = 20)`, `MethodArgumentTypeMismatchException` → `400`
**Requirement**: USERS-02 (AC1–6)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Integração com um ativo comum, um inativo comum, uma demo ativa e uma demo inativa: sem parâmetros → só o ativo comum; `INACTIVE` → só o inativo comum; `ALL` → os dois comuns; `ALL&demo=true` → os quatro; `ACTIVE&demo=true` → o ativo comum e a demo ativa
- [x] `status=FOO` → `400`
- [x] Ordenação `sort=name,asc` e `size=20` continuam valendo
- [x] `UserServiceTest` cobre o mapeamento de cada `UserStatusFilter` para `active` (`true`, `false`, `null`)
- [x] `ApiDocsIntegrationTest`: `status` e `demo` aparecem como parâmetros de `GET /api/users`
- [x] Gate quick (backend) passa com `UserServiceTest,UserControllerIntegrationTest,ApiDocsIntegrationTest`

**Tests**: integration
**Gate**: quick

**Commit**: `feat(auth): filter the user list by status and demo accounts`

---

### T3: Trava de autodesativação

**What**: `UserAccountConflictException` na lista de `handleConflict`; `UserService.deleteById` lança "Você não pode desativar a própria conta." quando o `id` é o do principal, antes de qualquer leitura ou gravação; `409` documentado no `DELETE` da `UserControllerApi`.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/auth/service/UserService.java`
**Depends on**: None
**Reuses**: `AuthenticatedUserAccessor.requirePrincipal()`, `GlobalExceptionHandler.handleConflict`
**Requirement**: USERS-04 (AC3)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Integração: ADMIN chama `DELETE /api/users/{próprio id}` → `409`, `error` = `CONFLICT`, `message` exata; a conta continua ativa e o ADMIN continua autenticado
- [x] Integração: ADMIN desativa outro usuário → `204` (caminho atual intacto)
- [x] `UserServiceTest`: recusa não chama `save` nem apaga tokens de verificação
- [x] `ApiDocsIntegrationTest`: `409` no `DELETE /api/users/{id}`
- [x] Gate quick (backend) passa com `UserServiceTest,UserControllerIntegrationTest,ApiDocsIntegrationTest`

**Tests**: integration
**Gate**: quick

**Commit**: `feat(auth): refuse an admin deactivating their own account`

---

### T4: Demo não recebe empresas pelo ADMIN

**What**: `UserMerchantAccessService.replace` carrega o usuário e lança `UserAccountConflictException("Contas de demonstração não recebem acesso a outras empresas.")` quando `demoExpiresAt != null`, antes de validar merchants; `grantIfAbsent` não muda; `409` documentado no `PUT /api/users/{id}/merchants`.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/auth/service/UserMerchantAccessService.java`
**Depends on**: T3
**Reuses**: `UserAccountConflictException`
**Requirement**: USERS-05 (AC5)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Integração: `PUT /api/users/{demo}/merchants` com uma empresa real → `409`, mensagem exata; os acessos da demo continuam os de antes
- [x] Integração: o mesmo `PUT` para um OPERATOR comum → `200` (caminho atual intacto)
- [x] `UserMerchantAccessServiceTest`: recusa não apaga nem grava acessos; `grantIfAbsent` para demo continua concedendo
- [x] `ApiDocsIntegrationTest`: `409` no `PUT /api/users/{id}/merchants`
- [x] Gate quick (backend) passa com `UserMerchantAccessServiceTest,UserControllerIntegrationTest,ApiDocsIntegrationTest`

**Tests**: integration
**Gate**: quick

**Commit**: `feat(auth): refuse merchant grants for demo accounts`

---

### T5: Token de conta inativa responde 401

**What**: `JwtAuthenticationFilter.authenticate` não autentica quando `!userDetails.isEnabled()`; o comentário do `catch` é reescrito para dizer o que de fato cai ali.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/security/JwtAuthenticationFilter.java`
**Depends on**: None
**Reuses**: `CustomUserDetails.isEnabled()`, entry point `401` existente
**Requirement**: USERS-04 (AC5, AC7)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Integração: OPERATOR loga, ADMIN o desativa, o token antigo em `GET /api/me` e `GET /api/merchants` → `401`
- [x] Integração: ADMIN reativa; o token antigo volta a autenticar e um login novo com a mesma senha → `200`
- [x] Integração: login de conta inativa continua respondendo a mensagem "Confirme seu e-mail para ativar sua conta"
- [x] Gate full: `cd backend && ./mvnw verify` (fim da fase de backend)

**Tests**: integration
**Gate**: full

**Commit**: `fix(security): reject tokens of deactivated accounts`

---

### T6: Funções de usuário em `lib/api.ts`

**What**: Tipos `User`, `UserFilters`; `getUserPage`, `createUser`, `renameUser`, `deactivateUser`, `activateUser`, `getUserAccess`, `replaceUserAccess`, conforme o design.
**Where**: `frontend/lib/api.ts`
**Depends on**: None
**Reuses**: `request`, `ApiError`, `Page<T>`
**Requirement**: USERS-02, USERS-03, USERS-04, USERS-05, USERS-06

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `frontend/lib/api.users.test.ts`: método, caminho, query (`status`, `demo`, `page`, `size=20`, `sort=name,asc`), corpo e `Authorization` de cada função; `getUserAccess` devolve `merchantIds`; um `409` vira `ApiError` com a `message`
- [x] Gate quick (frontend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add user administration API calls`

---

### T7: Hooks de usuário em `lib/queries.ts`

**What**: `queryKeys.userPage`, `queryKeys.userAccess`; `useUserPage(filters, page, enabled)`, `useUserAccess(id)`, `useCreateUser`, `useRenameUser`, `useDeactivateUser`, `useActivateUser`, `useReplaceUserAccess`.
**Where**: `frontend/lib/queries.ts`
**Depends on**: T6
**Reuses**: padrão de `useMerchantPage` e `useInvalidateMerchants`
**Requirement**: USERS-02, USERS-03, USERS-04, USERS-05, USERS-06

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `frontend/lib/queries.users.test.tsx`: `useUserPage` com `enabled = false` não chama a API; filtros e página entram na chave; cada mutação invalida `userPage`; `useReplaceUserAccess` também invalida `userAccess` do usuário
- [ ] Gate build (fim da fase): `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): add user administration query hooks`

---

### T8: Menu do avatar

**What**: `AccountMenu` (botão com as iniciais, `role="menu"` com nome, e-mail, selo "Admin"/"Operador", "Usuários" só para ADMIN e "Sair"); `TopNav` usa o menu no lugar do `<span role="img">` e mantém o botão "Sair" ao lado.
**Where**: `frontend/components/shell/AccountMenu.tsx`
**Depends on**: None
**Reuses**: fechar com `mousedown` fora e `Esc` do `MerchantMenu`; `initials` do `TopNav`
**Requirement**: USERS-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `AccountMenu.test.tsx`: clique abre com nome, e-mail e selo; ADMIN vê "Usuários" com `href="/usuarios"`; OPERATOR não vê; "Sair" chama `signOut`; `Esc` e clique fora fecham
- [ ] `TopNav.test.tsx` ajustado só onde o avatar mudou: o botão "Sair" ao lado continua e chama `signOut`
- [ ] Gate quick (frontend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(shell): open an account menu from the avatar`

---

### T9: Página `/usuarios` com a lista

**What**: `app/(app)/usuarios/page.tsx` e `UsersPage`: redirecionamento de OPERATOR, filtro de status, "Mostrar demonstrações", tabela, selo de demo, paginação, estado vazio, volta de página vazia e a coluna de ações conforme a tabela do design (os botões chamam callbacks que as próximas tasks ligam aos painéis; "Reativar" já chama `useActivateUser`).
**Where**: `frontend/components/users/UsersPage.tsx`
**Depends on**: None
**Reuses**: `MerchantsPage` (skeleton, `BlockError`, paginação, aviso `role="status"`), formato de data do `DemoBanner`
**Requirement**: USERS-02 (AC8–12), USERS-04 (AC4, AC6)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `UsersPage.test.tsx`: colunas e ordem pedidas à API (`sort=name,asc`); abre em "Ativos" sem demos; "Inativos", "Todos" e "Mostrar demonstrações" mudam o pedido; trocar filtro estando na página 2 volta à página 0; selo "Demo · expira em dd/MM às HH:mm"; "Nenhum usuário encontrado."; OPERATOR → `router.replace('/painel')` sem chamada a `/api/users`
- [ ] As quatro linhas da tabela de ações mostram exatamente as ações do design; a linha do ADMIN logado não tem "Desativar"
- [ ] "Reativar" chama `PATCH .../activation` e mostra "Usuário reativado."
- [ ] Gate quick (frontend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(users): list users with status and demo filters`

---

### T10: Painel de criar usuário

**What**: `UserDrawer` no modo criação (nome, e-mail, senha, papel com `OPERATOR` marcado), validação local, `409` no painel, aviso "Usuário criado."; `UsersPage` abre o painel por "Novo usuário".
**Where**: `frontend/components/users/UserDrawer.tsx`
**Depends on**: T9
**Reuses**: `SideDrawer`, `PASSWORD_PATTERN` de `lib/password.ts`, `NAME_LIMIT` de `MerchantDrawer`
**Requirement**: USERS-03

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `UserDrawer.test.tsx`: `OPERATOR` pré-selecionado e `ADMIN` selecionável (os dois enviados no corpo em casos separados); senha fraca mostra a mensagem exata e não chama a API; nome vazio e e-mail inválido idem; `201` fecha, invalida a lista e mostra "Usuário criado."; `409` mostra a `message` e mantém os dados; botão desabilitado durante o envio
- [ ] Gate quick (frontend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(users): create users from a side drawer`

---

### T11: Confirmar desativação

**What**: `DeactivateUserDialog` com o aviso do design; `UsersPage` abre o diálogo por "Desativar" e mostra "Usuário desativado." ao concluir.
**Where**: `frontend/components/users/DeactivateUserDialog.tsx`
**Depends on**: T9
**Reuses**: `ConfirmDialog`, padrão de `components/merchants/DeactivateDialog.tsx`
**Requirement**: USERS-04 (AC1, AC2)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `DeactivateUserDialog.test.tsx`: nada é chamado antes de confirmar; confirmar chama `DELETE /api/users/{id}`, fecha e mostra "Usuário desativado."; cancelar não chama a API; erro da API aparece no diálogo
- [ ] Gate quick (frontend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(users): confirm before deactivating a user`

---

### T12: Painel de acessos a empresas

**What**: `AccessDrawer` com uma caixa por empresa ativa (nome e CNPJ, ordem alfabética), marcadas as atuais; salvar envia os ids marcados; `UsersPage` abre por "Acessos" e mostra "Acessos atualizados."
**Where**: `frontend/components/users/AccessDrawer.tsx`
**Depends on**: T9
**Reuses**: `SideDrawer`, `getMerchants`, `formatCnpj`
**Requirement**: USERS-05 (AC1–3, AC7)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `AccessDrawer.test.tsx`: as atuais vêm marcadas; marcar e desmarcar muda exatamente o corpo enviado; desmarcar tudo envia `[]`; "Nenhuma empresa cadastrada." sem empresas; `404` e `409` mostram a `message` e mantêm o painel aberto
- [ ] Gate quick (frontend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(users): edit a user's merchant access`

---

### T13: Renomear usuário

**What**: `UserDrawer` no modo renomear (só o nome, preenchido); `UsersPage` abre por "Renomear" e mostra "Usuário renomeado."
**Where**: `frontend/components/users/UserDrawer.tsx`
**Depends on**: T10
**Reuses**: o próprio `UserDrawer`
**Requirement**: USERS-06

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `UserDrawer.test.tsx`: abre com o nome atual e sem e-mail, senha e papel; vazio e 151 caracteres mostram erro e não chamam a API; 150 caracteres envia; `200` fecha e mostra "Usuário renomeado."
- [ ] Gate build: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(users): rename a user from the side drawer`

---

### T14: Documentação

**What**: README (tela de usuários e o que separa ADMIN de OPERATOR), rastreabilidade da spec e handoff do `STATE.md`.
**Where**: `README.md`
**Depends on**: None
**Reuses**: seções existentes do README
**Requirement**: USERS-01..06

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] README cita `/usuarios`, os filtros e a queda de sessão ao desativar
- [ ] Gate full: `cd backend && ./mvnw verify` e gate build do frontend passam

**Tests**: none
**Gate**: build

**Commit**: `docs: document the users screen`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1, T2, T3, T4, T5
Phase 2:  T6, T7
Phase 3:  T8, T9, T10, T11, T12, T13
Phase 4:  T14
```

Dependências exatas nos diagramas de cada fase acima. Execution is strictly sequential within a worker.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1, T5–T8, T10–T13 | 1 componente, função ou arquivo | ✅ |
| T2 | Enum + query + serviço + parâmetros de um endpoint (só fazem sentido juntos) | ⚠️ coeso |
| T3, T4 | Uma regra no serviço + a exceção ou o doc do `409` | ⚠️ coeso |
| T9 | Página + rota fina | ⚠️ coeso |
| T14 | Docs | ✅ |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1, T3, T5 | None | — | ✅ |
| T2 | T1 | T1 → T2 | ✅ |
| T4 | T3 | T3 → T4 | ✅ |
| T6 | None | — | ✅ |
| T7 | T6 | T6 → T7 | ✅ |
| T8, T9 | None | — | ✅ |
| T10, T11, T12 | T9 | T9 → T10/T11/T12 | ✅ |
| T13 | T10 | T10 → T13 | ✅ |
| T14 | None | — | ✅ |

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | DTO + mapper | unit | unit | ✅ |
| T2, T3, T4, T5 | Serviço, endpoint, filtro JWT | integration | integration | ✅ |
| T6, T7 | Frontend lib | unit | unit | ✅ |
| T8–T13 | Frontend componentes | unit | unit | ✅ |
| T14 | Docs | none | none | ✅ |
