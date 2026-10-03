# Backend Access Alignment Specification

## Problem Statement

O ReconPay concluiu o MVP de API, mas o modelo de acesso não reflete o produto desejado: analista só lê dados operacionais, admin faz tudo incluindo operação, aprovação manual de conta, e operator não descobre merchants concedidos. Precisamos alinhar papéis, verificação de e-mail e escopo por merchant antes do frontend.

## Goals

- [ ] OPERATOR executa o fluxo completo (merchant, taxas, transações, import, conciliação) nos merchants concedidos
- [ ] ADMIN foca governança (usuários, grants); auto-registro ativa via e-mail sem PATCH de admin
- [ ] APIs de contexto (`/api/me`, `/api/me/merchants`) e auto-grant na criação de merchant

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Frontend SPA | Epic separado |
| Owner / convites por merchant | AD-004 — backlog |
| Refresh token / revogação JWT | Roadmap README |
| Multi-tenant / organização | Fora do modelo atual |
| Spring Batch / fila externa | Roadmap README |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Nome do role operacional | `OPERATOR` (rename de `FINANCIAL_ANALYST`) | Clareza no portfolio e na UI futura | y (context) |
| ADMIN opera merchants no dia a dia? | Não necessário; ADMIN retém bypass read/write via guard + rotas de governança | Modelo “admin mínimo” acordado | y |
| Token de e-mail | UUID opaco, hash no banco, TTL configurável (ex. 24h), uso único | Padrão seguro simples | y (agent) |
| E-mail | **Resend** (`RESEND_API_KEY`, remetente configurável); testes com sender fake | AD-005 | y |
| CORS | Origens configuráveis; default localhost comuns para Vite | Prepara frontend sem implementá-lo | y (agent) |
| Rate limit login/register | Não neste feature | README futuro | y |

**Open questions:** none — all resolved or logged above.

---

## User Stories

### P1: Papéis ADMIN vs OPERATOR ⭐ MVP

**User Story**: As a **operator**, I want to configure merchants and run reconciliation on granted merchants so that I don't depend on an admin for daily work.

**Why P1**: Core do realinhamento de produto.

**Acceptance Criteria**:

1. WHEN an authenticated user has role `OPERATOR` and a grant for `merchantId` THEN the system SHALL allow `POST|GET|PATCH` on `/api/merchants/{merchantId}/transactions/**` per existing business rules.
2. WHEN an authenticated user has role `OPERATOR` and a grant for `merchantId` THEN the system SHALL allow `POST|GET` on `/api/merchants/{merchantId}/external-settlements/**` and `POST|GET` on `/api/merchants/{merchantId}/reconciliations/**` per existing rules.
3. WHEN an authenticated user has role `OPERATOR` and a grant for `merchantId` THEN the system SHALL allow `POST|GET|PUT|DELETE` on `/api/merchants/{merchantId}/fee-rules/**`.
4. WHEN an authenticated user has role `OPERATOR` THEN the system SHALL allow `POST|GET|PUT|DELETE` on `/api/merchants` for merchants they created or were granted (list/detail filtered to accessible merchants).
5. WHEN an authenticated user has role `OPERATOR` THEN the system SHALL deny `/api/users/**` with HTTP 403.
6. WHEN an authenticated user has role `ADMIN` THEN the system SHALL allow `/api/users/**` and unrestricted merchant access via existing guard bypass.
7. IF an authenticated `OPERATOR` lacks grant for `merchantId` THEN the system SHALL respond HTTP 403 with `StandardError` error code `FORBIDDEN`.

**Independent Test**: Integration tests with operator token — granted vs denied merchant; admin user management still works.

**Requirements**: RBAC-01, RBAC-02, RBAC-03, RBAC-04, RBAC-05, RBAC-06, RBAC-07

---

### P1: Auto-grant na criação de merchant ⭐ MVP

**User Story**: As an **operator**, I want automatic access to a merchant I create so that I can immediately use operational endpoints.

**Why P1**: Prerequisite for operator-owned onboarding.

**Acceptance Criteria**:

1. WHEN an authenticated user successfully creates a merchant via `POST /api/merchants` THEN the system SHALL insert `(userId, merchantId)` into `user_merchants` if not already present.
2. IF the insert fails after merchant persist THEN the system SHALL roll back the transaction and SHALL NOT leave an orphan merchant without grant for the creator.
3. WHEN the creator is `ADMIN` THEN the system MAY insert the grant (redundant with bypass) without changing admin access semantics.

**Independent Test**: Create merchant as operator (once RBAC allows); subsequent `GET .../transactions` returns 200 without manual `PUT .../merchants`.

**Requirements**: GRANT-01, GRANT-02, GRANT-03

---

### P1: Verificação de e-mail antes do login ⭐ MVP

**User Story**: As a **new registrant**, I want to confirm my email via link so that my account activates without admin approval.

**Why P1**: Substitui fluxo de ativação manual acordado.

**Acceptance Criteria**:

1. WHEN `POST /api/auth/register` succeeds THEN the system SHALL persist the user with `active=false` and role `OPERATOR` and SHALL send a verification message containing a single-use link.
2. WHILE `active=false` the system SHALL reject login with HTTP 401 and a message indicating email verification is required.
3. WHEN the user submits a valid, non-expired verification token THEN the system SHALL set `active=true` and SHALL invalidate the token.
4. IF the verification token is invalid or expired THEN the system SHALL respond HTTP 400 with `StandardError` and SHALL NOT activate the account.
5. WHEN verification succeeds THEN the system SHALL allow subsequent login to return JWT as today.

**Independent Test**: Register → login fails → verify → login succeeds.

**Requirements**: AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05

---

### P1: Contexto do usuário logado ⭐ MVP

**User Story**: As an **operator**, I want to list merchants I can work with so that a future UI can show a selector without guessing UUIDs.

**Why P1**: Blocker de UX/API identificado na análise.

**Acceptance Criteria**:

1. WHEN an authenticated user calls `GET /api/me` THEN the system SHALL return `id`, `name`, `email`, `role`, `active`.
2. WHEN an authenticated `OPERATOR` calls `GET /api/me/merchants` THEN the system SHALL return a page of accessible active merchants (`id`, `name`, `document`) derived from `user_merchants`.
3. WHEN an authenticated `ADMIN` calls `GET /api/me/merchants` THEN the system SHALL return all active merchants (same fields), paginated.
4. IF the user is not authenticated THEN the system SHALL respond HTTP 401 on `/api/me` and `/api/me/merchants`.

**Independent Test**: MockMvc with operator granted one merchant — list size 1; admin sees all seeded merchants.

**Requirements**: CTX-01, CTX-02, CTX-03, CTX-04

---

### P2: CORS para desenvolvimento frontend

**User Story**: As a **frontend developer**, I want browser calls from dev origin to succeed so that local SPA can call the API.

**Why P2**: Preparação; API ainda sem UI neste repo.

**Acceptance Criteria**:

1. WHERE property `reconpay.cors.allowed-origins` is configured the system SHALL include those origins in CORS for `/api/**`.
2. WHEN `Origin` matches an allowed origin and method is permitted THEN the system SHALL respond to preflight `OPTIONS` with HTTP 204 and appropriate headers.

**Independent Test**: MockMvc or WebMvcTest CORS configuration smoke test.

**Requirements**: CORS-01, CORS-02

---

## Edge Cases

- IF operator registers duplicate email THEN system SHALL keep existing `409` conflict behavior.
- IF admin deactivates user WHILE verification pending THEN verification token SHALL NOT reactivate deleted/inactive-by-admin account (define: soft-deleted user stays inactive; token verify returns 400).
- WHEN admin replaces merchant grants via `PUT /api/users/{id}/merchants` THEN operator access SHALL reflect new set immediately on next request.
- IF concurrent merchant create with same document THEN one succeeds, one `409` — unchanged.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| RBAC-01 | P1 Papéis | Tasks | Pending |
| RBAC-02 | P1 Papéis | Tasks | Pending |
| RBAC-03 | P1 Papéis | Tasks | Pending |
| RBAC-04 | P1 Papéis | Tasks | Pending |
| RBAC-05 | P1 Papéis | Tasks | Pending |
| RBAC-06 | P1 Papéis | Tasks | Pending |
| RBAC-07 | P1 Papéis | Tasks | Pending |
| GRANT-01 | P1 Auto-grant | Tasks | Pending |
| GRANT-02 | P1 Auto-grant | Tasks | Pending |
| GRANT-03 | P1 Auto-grant | Tasks | Pending |
| AUTH-01 | P1 E-mail | Tasks | Pending |
| AUTH-02 | P1 E-mail | Tasks | Pending |
| AUTH-03 | P1 E-mail | Tasks | Pending |
| AUTH-04 | P1 E-mail | Tasks | Pending |
| AUTH-05 | P1 E-mail | Tasks | Pending |
| CTX-01 | P1 Contexto | Tasks | Pending |
| CTX-02 | P1 Contexto | Tasks | Pending |
| CTX-03 | P1 Contexto | Tasks | Pending |
| CTX-04 | P1 Contexto | Tasks | Pending |
| CORS-01 | P2 CORS | Tasks | Pending |
| CORS-02 | P2 CORS | Tasks | Pending |

**Coverage:** 21 total, 0 mapped to tasks commits, 21 pending

---

## Success Criteria

- [ ] `./mvnw -B verify` green with new integration tests for RBAC, grant, auth verify, `/api/me`
- [ ] README security section descreve ADMIN vs OPERATOR e fluxo de e-mail
- [ ] Swagger documenta novos endpoints e remove/depreca activation patch para auto-registro
