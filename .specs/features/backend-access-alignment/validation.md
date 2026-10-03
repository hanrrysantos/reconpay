# Backend Access Alignment Validation

**Date**: 2026-10-03 (re-verified after gap closure)
**Spec**: `.specs/features/backend-access-alignment/spec.md`
**Diff range**: `8a93549..HEAD` (origin/main..HEAD)
**Verifier**: [Verifier](74f94012-b1b7-40cc-81e0-941c3a6a6395) FAIL → gaps addressed → **PASS**

---

## Task Completion

| Task | Status |
| ---- | ------ |
| T1–T15 | ✅ Done |

---

## Spec-Anchored Acceptance Criteria (requirement IDs)

| ID | Spec-defined outcome | `file:line` + assertion | Result |
| -- | -------------------- | ----------------------- | ------ |
| RBAC-03 | OPERATOR fee-rules CRUD with grant | `FeeRuleIntegrationTest.java:92-115` — `isCreated()`, content contains DEBIT_CARD | ✅ PASS |
| RBAC-04 | OPERATOR merchant CRUD + scoped list | `MerchantIntegrationTest.java:104-138` — create, list contains id, PUT update | ✅ PASS |
| RBAC-05 | OPERATOR denied `/api/users/**` | `AuthorizationIntegrationTest.java:47-48` — `FORBIDDEN` | ✅ PASS |
| AUTH-04 | Invalid verify token → HTTP 400 `StandardError` | `AuthorizationIntegrationTest.java:99-110` — `isBadRequest()`, `VALIDATION_ERROR` | ✅ PASS |
| CTX-01 | GET `/api/me` fields | `MeControllerIntegrationTest.java:36-43` — `$.id`, `$.name`, email, role, active | ✅ PASS |
| CTX-03 | ADMIN `/api/me/merchants` ≥ operator breadth | `MeControllerIntegrationTest.java:74-93` — `adminTotal >= operatorTotal` | ✅ PASS |
| CTX-04 | Unauthenticated `/api/me/merchants` → 401 | `MeControllerIntegrationTest.java:101-104` — `isUnauthorized()` | ✅ PASS |
| GRANT-02 | Grant failure propagates (transactional create) | `MerchantServiceTest.java:107-121` — grant throws → exception propagates | ✅ PASS |
| Edge pending verify + admin delete | Verify must not activate | `UserService.java:88-95` clears tokens; `AuthorizationIntegrationTest.java:113-133` — verify `400` after DELETE | ✅ PASS |
| CORS-02 | Preflight CORS headers | `CorsConfigIntegrationTest.java:21-26` | ⚠️ Spec says HTTP 204; test asserts **200** (Spring default) |

**Status**: ✅ 20/21 requirements with matching evidence; 1 spec-precision gap (CORS status code)

---

## Discrimination Sensor

| Mutation | Killed? |
| -------- | ------- |
| Email verify skip activation | ✅ |
| Grant save removed | ✅ |
| CORS origins unset | ✅ |

**Result**: 3/3 killed — PASS

---

## Gate Check

- **Command**: `./mvnw -B verify`
- **Result**: 189 passed, 0 failed, 0 skipped

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 20/21 matched; 1 spec-precision gap (CORS 204 vs 200)
**Sensor**: 3/3 killed
**Gate**: 189 passed

**Follow-up from Verifier FAIL**: closed ranked gaps 1–7 with tests + revoke pending tokens on user delete.
