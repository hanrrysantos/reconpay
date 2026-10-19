# Cadastro e Verificação de E-mail Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/register-flow/design.md`
**Status**: Approved

---

## Test Coverage Matrix

> Gerada do código, das diretrizes do projeto e da spec; confirmar antes do Execute. Diretrizes encontradas: `AGENTS.md` (não enfraquecer testes; integração herda `AbstractIntegrationTest`; JaCoCo 85% linha e 75% branch), `frontend/vitest.config.ts`, `.github/workflows/ci.yml`. Lições aplicadas: L-018 (exercitar cada valor listado num AC: cada regra da senha, cada motivo de não reenviar), L-036 (mudar os valores mantidos antes de reenviar: campos do cadastro e e-mail do reenvio).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Backend e-mail (`auth/email`) | unit | Compositor 1:1 com MAIL-01: assunto, cada texto do HTML, prazo 24 e 1, texto puro, escape dos cinco caracteres, link com o caminho novo | `backend/src/test/java/**/auth/email/*Test.java` | `cd backend && ./mvnw -q test -Dtest='<Class>'` |
| Backend serviço (`EmailVerificationService`) | unit | RSD-01 AC3–AC5: cada motivo de não enviar (sem conta, ativa, sem token, já confirmou, dentro de 60 s) e o envio; fronteira de 60 s | `backend/src/test/java/**/auth/service/*Test.java` | `cd backend && ./mvnw -q test -Dtest='<Class>'` |
| Backend endpoint e segurança | integration | RSD-01 AC1–AC2: `202` sem corpo com e sem conta, `400` vazio e inválido, público sem token; fluxo real reenvio → novo token → link antigo recusado | `backend/src/test/java/**/{auth/controller,security/integration}/*Test.java` | `cd backend && ./mvnw -q test -Dtest='<Class>'` |
| Backend Swagger | integration | RSD-01 AC6 no `ApiDocsIntegrationTest` | `backend/src/test/java/**/openapi/ApiDocsIntegrationTest.java` | `cd backend && ./mvnw -q test -Dtest=ApiDocsIntegrationTest` |
| Frontend lib (`lib/password.ts`, `lib/api.ts`) | unit (node) | Cada regra com valor que cumpre e que não cumpre, fronteira 7/8; chamadas novas com método, caminho e corpo | `frontend/lib/*.test.ts` | `cd frontend && corepack pnpm test` |
| Frontend componentes e rotas (`components/auth`, `app/register`, `app/verificar-email`, `app/login`) | unit (jsdom) | 1:1 com CAD-01..04, VER-01..04 e edges; testes do login inalterados e verdes | `frontend/{components/auth,app/**}/*.test.tsx` | `cd frontend && corepack pnpm test` |
| Remoção de `auth.module.css` e docs | none | - (build gate only) | - | build gate only |

## Gate Check Commands

> Gerados do código; confirmar antes do Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick (backend) | Tasks de backend | `cd backend && ./mvnw -q test -Dtest='<classes da task>' -Djacoco.skip=true` |
| Quick (frontend) | Tasks de frontend | `cd frontend && corepack pnpm test && corepack pnpm typecheck` |
| Full | Fim da fase de backend e fim da feature | `cd backend && ./mvnw verify` |
| Build | Fim das fases de frontend, remoção e docs | `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build` |

---

## Execution Plan

Phases run sequentially; tasks within a phase run in order.

### Phase 1: Backend

```
T1 → T2
T2 → T3
```

### Phase 2: Base do frontend

```
T4
T5
```

### Phase 3: Telas

```
T4 → T6
T5 → T6
T4 → T7
T5 → T7
```

### Phase 4: Limpeza e documentação

```
T6 → T8
T7 → T8
```

---

## Task Breakdown

### T1: E-mail novo e link para o app

**What**: Criar `VerificationEmailComposer` (assunto, HTML e texto); `ResendEmailSender` passa a enviar `html` e `text` do compositor; `LoggingEmailSender` registra o link; o link usa `/verificar-email`; o padrão de `verification-base-url` vira `http://localhost:3000`.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/auth/email/*`, `backend/src/main/java/br/com/hanrry/reconpay/auth/config/EmailProperties.java`, `backend/src/main/resources/application.yaml`
**Depends on**: None
**Reuses**: `EmailVerificationLinkFactory`, escape do `ResendEmailSender`
**Requirement**: MAIL-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] O compositor devolve o assunto e um HTML com cada texto de MAIL-01 AC1, o link no botão e por extenso
- [x] Prazo "24 horas" no padrão e "1 hora" com `verificationTokenHours=1`
- [x] Texto puro com saudação, instrução, link, prazo e aviso para ignorar
- [x] Nome com `<`, `>`, `&`, `"` e `'` sai escapado no HTML
- [x] Link `http://localhost:3000/verificar-email?token=...` no padrão e com barra final na base
- [x] Testes do compositor e da fábrica de link; gate quick (backend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(auth): send the redesigned verification email`

---

### T2: Reenvio do e-mail de verificação

**What**: `EmailVerificationService.resend` com as regras da spec e `Clock` injetado; consultas novas no repositório; `ResendVerificationRequestDTO`; `POST /api/auth/resend-verification` com `202`; rota pública no `SecurityConfig`.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/auth/{service,repository,dto,controller}/*`, `backend/src/main/java/br/com/hanrry/reconpay/config/SecurityConfig.java`
**Depends on**: T1
**Reuses**: `sendVerificationEmail`, `CapturingEmailSender`, `AbstractIntegrationTest`
**Requirement**: RSD-01 (AC1–AC5)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Envia para conta inativa, com token, sem token consumido e último envio há 60 s ou mais
- [x] Não envia: sem conta, ativa, sem token, com token consumido (desativada depois de ativa), último envio há 59 s
- [x] O reenvio invalida o link anterior (integração: link antigo recusado, novo aceito)
- [x] `202` sem corpo com e sem conta; `400` `VALIDATION_ERROR` para vazio e inválido; sem token de acesso
- [x] E-mail com espaços nas pontas é encontrado
- [x] Gate quick (backend) passa

**Tests**: unit, integration
**Gate**: quick

**Commit**: `feat(auth): let unverified users request a new verification email`

---

### T3: Swagger do reenvio

**What**: Documentar o endpoint em `AuthControllerApi` (tag "Authentication", exemplos `202` e `400`, sem segurança) e cobrir no `ApiDocsIntegrationTest`.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/auth/openapi/AuthControllerApi.java`, `backend/src/test/java/br/com/hanrry/reconpay/openapi/ApiDocsIntegrationTest.java`
**Depends on**: T2
**Reuses**: padrão de `publicAuthDocumentsCodesExamplesAndOpenSecurity`
**Requirement**: RSD-01 (AC6)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] A operação aparece com resumo e descrição em português, `202` e `400` com exemplo, sem exigir token
- [ ] Gate full (`./mvnw verify`) passa

**Tests**: integration
**Gate**: full

**Commit**: `docs(auth): document the resend verification endpoint`

---

### T4: Chamadas e regras da senha no frontend

**What**: `verifyEmail(token)` e `resendVerification(email)` em `lib/api.ts`; `lib/password.ts` com `PASSWORD_RULES` e `PASSWORD_PATTERN`.
**Where**: `frontend/lib/api.ts`, `frontend/lib/password.ts`
**Depends on**: None
**Reuses**: `request`, `register`
**Requirement**: CAD-02 (AC4), VER-01 (AC1), VER-03 (AC1)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `verifyEmail` faz `POST /api/auth/verify-email` com `{ token }`; `resendVerification` faz `POST /api/auth/resend-verification` com o e-mail sem espaços nas pontas
- [ ] Cada regra com um valor que cumpre e um que não cumpre; "8 caracteres" com 7 e 8
- [ ] Testes em `frontend/lib/*.test.ts`; gate quick (frontend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add verification calls and password rules`

---

### T5: Painel compartilhado do login

**What**: Extrair o painel escuro e as classes de campo e botão do login para `components/auth/AuthPanel.tsx`; o login passa a usá-lo sem mudança visível.
**Where**: `frontend/components/auth/AuthPanel.tsx`, `frontend/app/login/page.tsx`
**Depends on**: None
**Reuses**: markup atual do login
**Requirement**: CAD-01 (AC3)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `AuthPanel` recebe título e texto; marca e rodapé fixos; classes responsivas iguais às do login
- [ ] Os 13 testes do login passam sem alteração
- [ ] Teste próprio do painel (título e texto por prop, texto e rodapé ocultos abaixo de 1024 px); gate quick (frontend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `refactor(frontend): share the auth panel between auth screens`

---

### T6: Tela de cadastro

**What**: Reescrever `/register` com o painel, o formulário, as regras da senha, o envio, o erro e a confirmação.
**Where**: `frontend/app/register/page.tsx`
**Depends on**: T4, T5
**Reuses**: `AuthPanel`, `PASSWORD_RULES`, `register`
**Requirement**: CAD-01, CAD-02, CAD-03, CAD-04, VER-04 (`/register`)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Testes 1:1 com cada AC de CAD-01..04 e com `/register` público
- [ ] Gate quick (frontend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): redesign the register screen`

---

### T7: Página de verificação

**What**: Rota pública `/verificar-email` com confirmação automática, estados confirmado, inválido e erro, e o reenvio.
**Where**: `frontend/app/verificar-email/page.tsx`, `frontend/components/auth/VerifyEmail.tsx`
**Depends on**: T4, T5
**Reuses**: `verifyEmail`, `resendVerification`, classes de `AuthPanel.tsx`
**Requirement**: VER-01, VER-02, VER-03, VER-04 (`/verificar-email`)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Testes 1:1 com cada AC de VER-01..03, rota pública e os edges (uma chamada em modo estrito, e-mail do reenvio sem espaços)
- [ ] Gate build passa (`Suspense` em volta de `useSearchParams`)

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): add the email verification page`

---

### T8: Limpeza e documentação

**What**: Remover `app/auth.module.css`; atualizar `README.md` (reenvio, `RECONPAY_VERIFICATION_BASE_URL` aponta para o frontend) e `frontend/README.md` (cadastro, verificação e reenvio).
**Where**: `frontend/app/auth.module.css`, `README.md`, `frontend/README.md`
**Depends on**: T6, T7
**Reuses**: None
**Requirement**: MAIL-01 (AC5), VER-04

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Nenhuma referência a `auth.module.css`
- [ ] READMEs descrevem o fluxo novo e a variável
- [ ] Gate build passa

**Tests**: none
**Gate**: build

**Commit**: `docs: describe the new register and verification flow`

---

## Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: E-mail novo | 1 compositor + ajuste de 2 senders e 1 fábrica | ✅ Granular |
| T2: Reenvio | 1 método de serviço + 1 endpoint | ✅ Granular |
| T3: Swagger | 1 operação documentada | ✅ Granular |
| T4: Chamadas e regras | 2 funções + 1 módulo pequeno | ✅ Granular |
| T5: Painel | 1 componente extraído | ✅ Granular |
| T6: Cadastro | 1 página | ✅ Granular |
| T7: Verificação | 1 página + 1 componente | ✅ Granular |
| T8: Limpeza | 1 remoção + docs | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | Phase 1 | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | None | Phase 2 | ✅ Match |
| T5 | None | Phase 2 | ✅ Match |
| T6 | T4, T5 | T4 → T6, T5 → T6 | ✅ Match |
| T7 | T4, T5 | T4 → T7, T5 → T7 | ✅ Match |
| T8 | T6, T7 | T6 → T8, T7 → T8 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | backend e-mail | unit | unit | ✅ OK |
| T2 | backend serviço, endpoint, segurança | unit, integration | unit, integration | ✅ OK |
| T3 | backend Swagger | integration | integration | ✅ OK |
| T4 | frontend lib | unit | unit | ✅ OK |
| T5 | frontend componente | unit | unit | ✅ OK |
| T6 | frontend rota | unit | unit | ✅ OK |
| T7 | frontend rota + componente | unit | unit | ✅ OK |
| T8 | remoção e docs | none | none | ✅ OK |
