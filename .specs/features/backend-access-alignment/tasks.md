# Backend Access Alignment Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement with skill **tlc-spec-driven** Execute flow: one task → gate → atomic commit → mark done. Verifier runs after last task.

**Design**: `.specs/features/backend-access-alignment/design.md`
**Status**: In Progress

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: README (test strategy), CI `./mvnw -B verify`, JaCoCo gates in `pom.xml`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Service (auth, merchant, access) | unit | Branches 1:1 to AUTH/GRANT/RBAC ACs | `src/test/java/**/service/*Test.java` | `./mvnw -B test -Dtest=...` |
| Security / RBAC | integration | Granted vs denied merchant; role denied routes | `src/test/java/**/integration/*Test.java`, `security/integration/*` | `./mvnw -B verify` |
| Controllers (me, verify) | integration | Happy + error paths per CTX/AUTH | `src/test/java/**/controller/*IntegrationTest.java` | `./mvnw -B verify` |
| Flyway / entity | none | Covered via integration | — | build gate |
| Config (CORS) | unit or WebMvcTest | Preflight smoke | `src/test/java/**/config/*` | `./mvnw -B test` |

## Gate Check Commands

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Unit-only task | `./mvnw -B test -Dtest=ClassName` |
| Full | Integration / security | `./mvnw -B verify` |
| Build | Phase complete | `./mvnw -B verify` (CI equivalent) |

---

## Task Breakdown

### T1: Rename role FINANCIAL_ANALYST → OPERATOR

**What**: Enum, DB migration, seeds, JWT authorities, OpenAPI text, test utilities.
**Where**: `UserRole.java`, Flyway `V18__...`, `R__seed_local_users.sql`, `IntegrationTestUtils`
**Depends on**: None
**Reuses**: Existing enum pattern
**Requirement**: RBAC-01 (precondition)

**Done when**:

- [x] No reference to `FINANCIAL_ANALYST` in main sources (tests updated)
- [x] Migration applied on Testcontainers boot
- [x] Gate: `./mvnw -B verify`

**Tests**: unit + integration (existing suites updated)
**Gate**: full

---

### T2: Adjust SecurityConfig role constants and analyst integration tests

**What**: Replace role strings in `SecurityConfig` and authorization tests for OPERATOR name.
**Where**: `SecurityConfig.java`, `AuthorizationIntegrationTest.java`, related
**Depends on**: T1
**Requirement**: RBAC-01

**Done when**:

- [x] Tests pass with `ROLE_OPERATOR`
- [x] Gate: `./mvnw -B verify`

**Tests**: integration
**Gate**: full

---

### T3: Refine SecurityConfig URL matrix for OPERATOR operational paths

**What**: Split `/api/merchants/**` rules: fee-rules, merchant CRUD, sub-resources per spec RBAC-01…06.
**Where**: `SecurityConfig.java`
**Depends on**: T2
**Requirement**: RBAC-01 … RBAC-06

**Done when**:

- [x] OPERATOR denied `/api/users/**` (403)
- [x] OPERATOR allowed merchant-scoped ops when granted (existing isolation tests extended)
- [x] Gate: `./mvnw -B verify`

**Tests**: integration
**Gate**: full

---

### T4: Merchant list/detail authorization for OPERATOR

**What**: Filter `GET /api/merchants` and `GET /api/merchants/{id}` to accessible merchants only for OPERATOR; ADMIN unchanged.
**Where**: `MerchantService`, `MerchantController` (+ tests)
**Depends on**: T3
**Requirement**: RBAC-04, RBAC-07

**Done when**:

- [x] Operator list contains only granted merchants
- [x] Operator GET by id on foreign merchant → 403 or 404 (choose 403 per guard — document in test)
- [x] Gate: `./mvnw -B verify`

**Tests**: integration
**Gate**: full

---

### T5: grantIfAbsent on UserMerchantAccessService

**What**: Idempotent grant helper + audit log.
**Where**: `UserMerchantAccessService.java`, unit test
**Depends on**: T2
**Requirement**: GRANT-01

**Done when**:

- [x] Double grant no-op
- [x] Gate: `./mvnw -B test -Dtest=UserMerchantAccessServiceTest` (new or extended)

**Tests**: unit
**Gate**: quick

---

### T6: Auto-grant in MerchantService.create

**What**: After save merchant, grant creator within same `@Transactional`.
**Where**: `MerchantService.java`, `MerchantIntegrationTest` / isolation test
**Depends on**: T4, T5
**Requirement**: GRANT-01 … GRANT-03

**Done when**:

- [x] Operator creates merchant and reads transactions without manual PUT grants
- [x] Gate: `./mvnw -B verify`

**Tests**: integration
**Gate**: full

---

### T7: GET /api/me

**What**: DTO + controller + security authenticated.
**Where**: `auth/controller/MeController.java`, DTO, `SecurityConfig`
**Depends on**: T3
**Requirement**: CTX-01, CTX-04

**Done when**:

- [x] Returns current user fields from `CustomUserDetails`
- [x] Gate: `./mvnw -B verify`

**Tests**: integration
**Gate**: full

---

### T8: GET /api/me/merchants

**What**: Paginated merchant summary for operator (grants) vs admin (all active).
**Where**: `MeController`, service method
**Depends on**: T7, T4
**Requirement**: CTX-02, CTX-03

**Done when**:

- [x] Operator sees exactly granted merchants
- [x] Admin page matches active merchants list semantics
- [x] Gate: `./mvnw -B verify`

**Tests**: integration
**Gate**: full

---

### T9: Flyway email_verification_tokens + entity/repository

**What**: Table + JPA entity.
**Where**: `db/migration/V19__...`, `auth/entity`, `auth/repository`
**Depends on**: T1
**Requirement**: AUTH-01

**Done when**:

- [x] Integration test context loads
- [x] Gate: `./mvnw -B verify`

**Tests**: integration (context)
**Gate**: full

---

### T10: EmailVerificationService + Resend mail sender

**What**: Issue token, hash, send via **Resend** (`EmailSender` + fake in tests).
**Where**: `auth/service/EmailVerificationService.java`, test config
**Depends on**: T9
**Requirement**: AUTH-01, AUTH-03, AUTH-04

**Done when**:

- [x] Unit tests for expiry, consume-once, invalid token
- [x] Gate: quick + unit class

**Tests**: unit
**Gate**: quick

---

### T11: Wire register + POST verify-email + login guard

**What**: Register sends mail; verify activates; login blocked while inactive.
**Where**: `AuthService`, `AuthController`, OpenAPI
**Depends on**: T10
**Requirement**: AUTH-01 … AUTH-05

**Done when**:

- [x] Integration tests cover register → verify → login (`AuthorizationIntegrationTest`)
- [x] Gate: `./mvnw -B verify`

**Tests**: integration
**Gate**: full

---

### T12: Remove or restrict PATCH activation for self-service path

**What**: Document deprecation; ensure admin-created users unchanged; update OpenAPI/README.
**Where**: `UserController`, docs
**Depends on**: T11
**Requirement**: AUTH (closure)

**Done when**:

- [x] OpenAPI register documents verify-email (README in T14)
- [x] Gate: `./mvnw -B verify`

**Tests**: integration (regression admin activate if kept)
**Gate**: full

---

### T13: CORS configuration (P2)

**What**: `CorsConfig` + properties for dev origins.
**Where**: `config/CorsConfig.java`, `application.yaml`
**Depends on**: T3
**Requirement**: CORS-01, CORS-02

**Done when**:

- [ ] WebMvcTest or integration asserts allowed origin header
- [ ] Gate: `./mvnw -B verify`

**Tests**: unit/WebMvcTest
**Gate**: full

---

### T14: Update README security & API tables

**What**: ADMIN vs OPERATOR, verify-email, `/api/me`, auto-grant.
**Where**: `README.md`
**Depends on**: T11, T8, T6
**Requirement**: Success criteria

**Done when**:

- [ ] Docs match implemented behavior
- [ ] Gate: `./mvnw -B verify`

**Tests**: none
**Gate**: build

---

### T15: OpenAPI polish for new endpoints

**What**: springdoc tags, security schemes, examples.
**Where**: controllers / `OpenApiConfig`
**Depends on**: T14, T7, T11
**Requirement**: Success criteria

**Done when**:

- [ ] Swagger shows verify + me endpoints
- [ ] Gate: `./mvnw -B verify`

**Tests**: none
**Gate**: build

---

## Execution Plan

| Phase | Tasks | Focus |
| ----- | ----- | ----- |
| 1 | T1 → T2 | Role `OPERATOR` |
| 2 | T3 → T4 | SecurityConfig + merchant list scope |
| 3 | T5 → T6 | Auto-grant |
| 4 | T7 → T8 | `/api/me` |
| 5 | T9 → T10 → T11 → T12 | E-mail verification |
| 6 | T13 | CORS (P2) |
| 7 | T14 → T15 | Docs / OpenAPI |

Dependency graph (matches `Depends on` in Task Breakdown):

```
T1 -> T2
T2 -> T3
T2 -> T5
T1 -> T9
T3 -> T4
T3 -> T7
T3 -> T13
T4 -> T6
T4 -> T8
T5 -> T6
T7 -> T8
T9 -> T10
T10 -> T11
T11 -> T12
T6 -> T14
T8 -> T14
T11 -> T14
T7 -> T15
T11 -> T15
T14 -> T15
```

---

## Phase Execution Map

**Batching (~7 tasks/worker):** Worker1 Ph1–2 (T1–T4), Worker2 Ph3–4 (T5–T8), Worker3 Ph5 (T9–T12), Worker4 Ph6–7 (T13–T15) — sequential batches after user approves Execute.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1 Rename role | enum + migration + seeds | ✅ |
| T3 Security matrix | one config file + tests | ✅ |
| T11 Auth flow | register+verify+login | ✅ cohesive |
| T14 README | doc only | ✅ |

---

## Diagram-Definition Cross-Check

| Task | Depends On | Diagram | Status |
| ---- | ---------- | ------- | ------ |
| T2 | T1 | T1→T2 | ✅ |
| T4 | T3 | T3→T4 | ✅ |
| T6 | T4,T5 | T5→T6 (T4 implicit) | ✅ |
| T8 | T7,T4 | T7→T8 | ✅ |
| T11 | T10 | T10→T11 | ✅ |
| T14 | T11,T8,T6 | phase 7 | ✅ |

---

## Test Co-location Validation

| Task | Layer | Matrix | Task Tests | Status |
| ---- | ----- | ------ | ---------- | ------ |
| T5 | Service unit | unit | unit | ✅ |
| T6 | Merchant service | unit+integration | integration | ✅ |
| T11 | Auth integration | integration | integration | ✅ |
| T14 | README | none | none | ✅ |
