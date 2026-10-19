# prod-cors-env Validation

**Verdict**: PASS

**Date**: 2026-10-19
**Spec**: `.specs/features/prod-cors-env/spec.md`
**Diff range**: `650941d..HEAD` (eb566ae spec, 5c5df07 compose + tests, 64daff8 README)
**Verifier**: independent sub-agent (author ≠ verifier)

---

## Task Completion

No `tasks.md` (small scope, declared in spec). Three steps observed in git: spec (eb566ae), compose variable + tests (5c5df07), README (64daff8). No Java main code changed in the diff.

---

## Spec-Anchored Acceptance Criteria

How the environment path is proven: it is a chain of two links, and each link has its own test.
- **Link 1 (env var → property list)**: `CorsPropertiesBindingTest` binds `CorsProperties` through a `Binder` over a `SystemEnvironmentPropertySource` that holds the real variable name `RECONPAY_CORS_ALLOWED_ORIGINS`. That source sits above a lower-priority `MapPropertySource` with the four `application.yaml` list entries (`backend/src/test/java/br/com/hanrry/reconpay/config/CorsPropertiesBindingTest.java:23-29`). This runs Spring Boot's real relaxed binding, including the env-name mapping, the comma split and the trim, and the precedence of an env scalar over an indexed YAML list. `containsExactly` proves the list is replaced, not merged.
- **Link 2 (property list → HTTP preflight)**: `CorsFromEnvironmentIntegrationTest` boots the full context with `reconpay.cors.allowed-origins= https://reconpay.vercel.app , https://app.hanrry.top/` (a comma string with spaces and a trailing slash, the same shape the env var produces) and checks the MockMvc preflights (`backend/src/test/java/br/com/hanrry/reconpay/config/CorsFromEnvironmentIntegrationTest.java:17`).

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| CORS-01 AC1: variable with 1+ origins means each is accepted on `/api/**` | `200` + `Access-Control-Allow-Origin` = origin | `backend/src/test/java/br/com/hanrry/reconpay/config/CorsFromEnvironmentIntegrationTest.java:27-28` - `status().isOk()`, `header().string(ACCESS_CONTROL_ALLOW_ORIGIN, origin)` for both origins on `/api/auth/login`; `:30-32` same on `/api/merchants`; env link: `backend/src/test/java/br/com/hanrry/reconpay/config/CorsPropertiesBindingTest.java:37` - `containsExactly("https://reconpay.vercel.app", "https://app.hanrry.top")` | ✅ PASS |
| CORS-01 AC1: origins not in the variable are refused, including the default `localhost` ones (replace, not merge) | `403`, no `Access-Control-Allow-Origin` | `backend/src/test/java/br/com/hanrry/reconpay/config/CorsFromEnvironmentIntegrationTest.java:39-40` - `status().isForbidden()`, `header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN)` for all four localhost origins; env link: `backend/src/test/java/br/com/hanrry/reconpay/config/CorsPropertiesBindingTest.java:37` - `containsExactly` (YAML entries absent) | ✅ PASS |
| CORS-01 AC2: spaces around and trailing slash are dropped | origin accepted without them | spaces via env: `backend/src/test/java/br/com/hanrry/reconpay/config/CorsPropertiesBindingTest.java:52` - `containsExactly(...)` from `" https://reconpay.vercel.app , https://app.hanrry.top "`; spaces + trailing slash end-to-end: `backend/src/test/java/br/com/hanrry/reconpay/config/CorsFromEnvironmentIntegrationTest.java:17` (input `https://app.hanrry.top/`) + `:28` - `header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.hanrry.top")` | ✅ PASS |
| CORS-01 AC3: without the variable, the four current `localhost` origins stay | four localhost origins | `backend/src/test/java/br/com/hanrry/reconpay/config/CorsPropertiesBindingTest.java:59-60` - `containsExactly("http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:3000", "http://127.0.0.1:3000")`; HTTP: `backend/src/test/java/br/com/hanrry/reconpay/config/CorsConfigIntegrationTest.java:25-26` - `isOk()` + `ACCESS_CONTROL_ALLOW_ORIGIN = "http://localhost:5173"`, `:34-35` - `isForbidden()` for `http://evil.example` | ✅ PASS |
| CORS-01 AC4: Compose passes `RECONPAY_CORS_ALLOWED_ORIGINS` to the container, with the four origins as the default | variable present on `api-reconpay`; default = four origins | `docker-compose.yaml:43` - `RECONPAY_CORS_ALLOWED_ORIGINS: ${RECONPAY_CORS_ALLOWED_ORIGINS:-http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000,http://127.0.0.1:3000}`; checked by rendering (see below) | ✅ PASS (render-only evidence) |
| CORS-02 AC1: README documents the variable, the format, that it replaces the list, and the default | all four items present | `README.md:126` - "lista, separadas por vírgula, ... ela substitui a lista padrão inteira, que tem só `localhost` nas portas 3000 e 5173, e aceita espaços em volta e barra final"; `README.md:123` (example `.env`); `README.md:252` (Compose passes it on) | ✅ PASS |

**CORS-01 AC4 render evidence** (no automated test covers Compose; no container was started). The env files have only `POSTGRES_USER`/`POSTGRES_PASSWORD`/`JWT_SECRET`, plus the CORS variable in the second one:

```
$ env -u RECONPAY_CORS_ALLOWED_ORIGINS docker compose --env-file <tmp>/noenv.env config | grep CORS
      RECONPAY_CORS_ALLOWED_ORIGINS: http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000,http://127.0.0.1:3000
$ env -u RECONPAY_CORS_ALLOWED_ORIGINS docker compose --env-file <tmp>/withenv.env config | grep CORS
      RECONPAY_CORS_ALLOWED_ORIGINS: https://reconpay.vercel.app
```

**Status**: ✅ All ACs covered (AC4 by rendering the config only, as allowed)

---

## Edge Cases

- [x] A single origin in the variable means only that origin is accepted: `backend/src/test/java/br/com/hanrry/reconpay/config/CorsPropertiesBindingTest.java:44` - `containsExactly("https://reconpay.vercel.app")`. The HTTP refusal of other origins follows from the same `CorsConfig` path proven at `backend/src/test/java/br/com/hanrry/reconpay/config/CorsFromEnvironmentIntegrationTest.java:39-40`.

---

## Discrimination Sensor

Scratch: `git worktree add --detach <jobs-tmp>/cors-sensor HEAD`, removed with `git worktree remove --force`. Real tree `git status --porcelain` was empty before and after (match). Tests run per mutant: `./mvnw -q test -Dtest='Cors*Test'` (8 tests). The unmutated baseline passed.

| # | File:line | Mutation | Killed? | Killed by |
| --- | --- | --- | --- | --- |
| 1 | `backend/src/main/java/br/com/hanrry/reconpay/config/CorsConfig.java:22` | Ignore properties and use the hard-coded localhost list | ✅ Killed | CorsFromEnvironmentIntegrationTest:27, :39 |
| 2 | `backend/src/main/java/br/com/hanrry/reconpay/config/CorsProperties.java:10` | Always use the defaults (`if (true)`) | ✅ Killed | 3 binding tests + CorsFromEnvironmentIntegrationTest:39 |
| 3 | `backend/src/main/java/br/com/hanrry/reconpay/config/CorsProperties.java:16` | Merge the defaults with the configured origins | ✅ Killed | 4 binding tests + CorsFromEnvironmentIntegrationTest:39 |
| 4 | `backend/src/main/java/br/com/hanrry/reconpay/config/CorsConfig.java:15` | Narrow the path pattern `/api/**` to `/api/auth/**` | ✅ Killed | CorsFromEnvironmentIntegrationTest:31 |
| 5 | `backend/src/main/java/br/com/hanrry/reconpay/config/CorsConfig.java:23` | Drop `POST` from the allowed methods | ✅ Killed | CorsFromEnvironmentIntegrationTest:27, CorsConfigIntegrationTest:25 |
| 6 | `backend/src/main/java/br/com/hanrry/reconpay/config/CorsConfig.java:22` | Merge in the config layer (configured + localhost) | ✅ Killed | CorsFromEnvironmentIntegrationTest:39 |
| 7 | `backend/src/main/java/br/com/hanrry/reconpay/config/CorsConfig.java:22` | Keep only the first configured origin | ✅ Killed | CorsFromEnvironmentIntegrationTest:27 |
| 8 | `docker-compose.yaml:43` | Typo in the variable name (`RECONPAY_CORS_ALLOWED_ORIGIN`) | ⚠️ Render-detected (no automated test) | the render shows key `RECONPAY_CORS_ALLOWED_ORIGIN` |
| 9 | `docker-compose.yaml:43` | Remove the default (`:-}`) | ⚠️ Render-detected (no automated test) | the no-env render shows `RECONPAY_CORS_ALLOWED_ORIGINS: ""`. At runtime an empty value binds to an empty list, and `CorsProperties` falls back to the four defaults, so behavior is unchanged |

**Sensor depth**: lightweight-plus (7 Java behavior mutants + 2 Compose config mutants)
**Result**: 7/7 Java mutants killed by tests; the 2 Compose mutants were detected by the render check only - PASS ✅

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code (one Compose line; no Java main change) | ✅ |
| Surgical changes | ✅ |
| No scope creep (no wildcards, credentials or exposed headers) | ✅ |
| Matches patterns (`${VAR:-default}` like its neighbors; tests extend `AbstractIntegrationTest`) | ✅ |
| Spec-anchored outcome check (`200`/`403`, exact header value, exact list) | ✅ |
| Per-layer coverage (binding unit tests 1:1 with AC1/AC2/AC3/edge; HTTP happy + refusal paths) | ✅ |
| Every test maps to a spec requirement | ✅ |
| Documented guidelines followed: `AGENTS.md` (Testcontainers, `AbstractIntegrationTest`, env-only secrets, no weakened tests) | ✅ |

---

## Gate Check

- **Gate command**: `cd backend && ./mvnw verify`
- **Result**: exit 0; 657 passed, 0 failed, 0 errors, 0 skipped (surefire reports)
- **Coverage (JaCoCo, whole bundle)**: line 96.13%, branch 87.58% (thresholds 85% / 75%)
- **Test count before feature**: 651
- **Test count after feature**: 657
- **Delta**: +6 (`CorsPropertiesBindingTest` 4, `CorsFromEnvironmentIntegrationTest` 2); `CorsConfigIntegrationTest` unchanged
- **Skipped tests**: none

---

## Residual observations (non-blocking)

1. CORS-01 AC4 has no automated test; it was verified only by rendering the config with `docker compose config`. A typo in the Compose variable name would pass CI (sensor #8).
2. The env-var binding test builds the property sources by hand (system env above `application.yaml`) instead of booting the real `Environment`. The order matches Spring Boot's real precedence, and the HTTP link is proven separately, so the chain holds.
3. The `.env` example at `README.md:123` sets `RECONPAY_CORS_ALLOWED_ORIGINS=http://localhost:3000`. Copying it as-is replaces the four defaults with only that one origin. This matches the documented replace semantics, but is worth knowing.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| CORS-01 | Implemented | ✅ Verified |
| CORS-02 | Implemented | ✅ Verified |

(spec.md left untouched by the Verifier; the orchestrator updates it.)

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 6/6 AC rows + 1 edge case matched the spec outcome; 0 spec-precision gaps
**Sensor**: 7/7 Java mutants killed; 2 Compose mutants detected by render only
**Gate**: 657 passed, coverage 96.13% / 87.58%
