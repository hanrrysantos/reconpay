# Backend Access Alignment Design

**Spec**: `.specs/features/backend-access-alignment/spec.md`
**Status**: Draft (awaiting approval)

---

## Architecture Overview

Evolução incremental do monólito: mesmos módulos `auth`, `security`, `merchant`, controllers existentes. Mudanças concentradas em `SecurityConfig`, guard/aspect, serviços de merchant e auth, nova entidade de token de verificação, endpoints `/api/me*`.

```mermaid
flowchart TB
  subgraph auth [auth]
    Reg[Register]
    Verify[Verify Email]
    Login[Login]
    Me[/api/me]
  end
  subgraph sec [security]
    SC[SecurityConfig]
    Guard[MerchantAccessGuard]
    Aspect[MerchantAccessAspect]
  end
  subgraph merch [merchant]
    MS[MerchantService.create]
    AG[Auto-grant]
  end
  Reg --> TokenTable[(email_verification_tokens)]
  Verify --> TokenTable
  Login --> JWT[JwtService]
  MS --> AG --> UM[(user_merchants)]
  SC --> Guard
  Aspect --> Guard
```

---

## Code Reuse Analysis

### Existing Components to Leverage

| Component | Location | How to Use |
| --------- | -------- | ---------- |
| `MerchantAccessGuard` | `security/MerchantAccessGuard.java` | Manter bypass ADMIN; OPERATOR via grants |
| `MerchantAccessAspect` | `security/MerchantAccessAspect.java` | Continua em `{merchantId}` path vars |
| `UserMerchantAccessService` | `auth/service/UserMerchantAccessService.java` | Extrair `grantIfAbsent(userId, merchantId)` |
| `SecurityConfig` | `config/SecurityConfig.java` | Refinar matchers por subpath e role |
| `StandardError` | `exception/standardexceptionerror/` | Erros de verify e 403 |
| `AuditLogger` | `observability/AuditLogger.java` | EVENTS: MERCHANT_CREATED, USER_EMAIL_VERIFIED, USER_MERCHANT_GRANTED |
| Seeds | `db/seed/R__seed_local_users.sql` | Role OPERATOR + usuários de teste |

### Integration Points

| System | Integration Method |
| ------ | ------------------ |
| PostgreSQL | Flyway migration: rename role value if needed, `email_verification_tokens` |
| Mail | **Resend** REST API (`ResendEmailSender`); interface `EmailSender` para testes com fake |
| OpenAPI | Annotations em novos controllers/métodos |

---

## Components

### EmailVerificationService

- **Purpose**: Criar token, enviar e-mail, consumir token com idempotência.
- **Location**: `auth/service/EmailVerificationService.java`
- **Interfaces**: `issueFor(UserEntity)`, `verify(String rawToken)`
- **Dependencies**: Repository token, `EmailSender` (Resend em prod/dev com key; fake em test), SHA-256 para hash do token
- **Reuses**: Padrão transacional de `UserService`

### MeController (ou AuthController extension)

- **Purpose**: `GET /api/me`, `GET /api/me/merchants`
- **Location**: `auth/controller/MeController.java`
- **Dependencies**: `UserService`, `MerchantService` ou query join grants
- **Reuses**: Paginação Spring existente em merchants

### SecurityConfig refactor

- **Purpose**: Matriz RBAC-01…07
- **Location**: `config/SecurityConfig.java`
- **Pattern**: Matchers específicos antes do catch-all `/api/merchants/**`

Exemplo de ordem (conceitual):

1. `/api/auth/**` público onde já é
2. `/api/me/**` authenticated
3. `/api/users/**` ADMIN
4. `/api/merchants/*/transactions|external-settlements|reconciliations|fee-rules/**` OPERATOR+ADMIN
5. `/api/merchants` POST/GET/PUT/DELETE — OPERATOR+ADMIN com regras de listagem no service

### MerchantService + auto-grant

- **Purpose**: GRANT-01 transacional
- **Location**: `merchant/service/MerchantService.java`
- **Dependencies**: `SecurityContext` → `CustomUserDetails.getId()`, `UserMerchantAccessService.grantIfAbsent`

### CorsConfig (P2)

- **Purpose**: CORS-01
- **Location**: `config/CorsConfig.java` + `ReconPayProperties` ou `application.yaml`

---

## Data Models

### email_verification_tokens (new table)

| Column | Type | Notes |
| ------ | ---- | ----- |
| id | UUID PK | |
| user_id | UUID FK users | |
| token_hash | VARCHAR | never store raw token |
| expires_at | TIMESTAMPTZ | |
| consumed_at | TIMESTAMPTZ nullable | |

### users.role

- Enum Java `OPERATOR` substituindo `FINANCIAL_ANALYST`; migration SQL `UPDATE users SET role = 'OPERATOR' WHERE role = 'FINANCIAL_ANALYST'` se persistido como string.

---

## Error Handling Strategy

| Error Scenario | Handling | User Impact |
| -------------- | -------- | ----------- |
| Login inactive | 401 + message verify email | Clear next step |
| Bad verify token | 400 VALIDATION_ERROR | Link inválido/expirado |
| Operator no grant | 403 FORBIDDEN | Sem acesso ao merchant |

---

## Risks & Concerns

| Concern | Location | Impact | Mitigation |
| ------- | -------- | ------ | ---------- |
| Catch-all `/api/merchants/**` ADMIN-only | `SecurityConfig.java:79` | Blocker OPERATOR | Refactor matchers (T2) |
| 31+ integration tests assume ANALYST role name | `src/test/**` | CI fail | T1 rename + bulk test update |
| Shared DB integration tests | vários `*IntegrationTest` | Flaky grants | Unique documents per test; assert auto-grant |
| Mail in CI | GitHub Actions | Flaky send | Test profile: capture link via mock `MailSender` bean |

---

## Tech Decisions

| Decision | Choice | Rationale |
| -------- | ------ | --------- |
| Token storage | Hash only in DB | Leak of DB ≠ usable link |
| Verify endpoint | `POST /api/auth/verify-email` body `{ "token": "..." }` | Evita token em access logs de GET |
| Deprecate activation | Remove public doc of PATCH for self-service; keep for ADMIN bootstrap? | Optional: ADMIN still activates users created inactive via POST /api/users — out of AUTH scope if POST creates active users |
| List merchants for OPERATOR | Filter in service, not only security | RBAC-04 |
