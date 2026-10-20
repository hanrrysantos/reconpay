# Demonstração por Visitante Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/demo-account/design.md`
**Status**: In Progress

---

## Test Coverage Matrix

> Gerada do código, das diretrizes do projeto e da spec; confirmar antes do Execute. Diretrizes encontradas: `AGENTS.md` (não enfraquecer testes; integração herda `AbstractIntegrationTest`; JaCoCo 85% linha e 75% branch), `frontend/package.json` (`vitest run`, `typecheck`), `.github/workflows/ci.yml`. Lições aplicadas: L-018 (cada valor listado num AC é exercitado: os 4 meios de pagamento, os 5 tipos de divergência, os 3 endpoints de importação, `429` e `503` no frontend), L-036 (na restauração da sessão, mudar o estado antes do reset).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Backend regra pura (`CnpjRules`, `DemoScenario`, `DemoRateLimiter`, `InMemoryCsvFile`, `JwtService`) | unit | Todos os ramos; 1:1 com os ACs que cobrem; fronteiras (2ª/3ª criação, 60 min exatos, `today-60`/`today-1`) | `backend/src/test/java/**/{shared,demo,security}/**/*Test.java` | `cd backend && ./mvnw -q test -Dtest='<Class>' -Djacoco.skip=true` |
| Backend orquestração (`DemoAccountService`, `DemoRunExecutor`) | unit (Mockito) | Cada caminho: sucesso, `429`, `503`, compensação em cada falha, espera `COMPLETED`/`FAILED`/timeout | `backend/src/test/java/**/demo/service/*Test.java` | idem |
| Backend persistência e endpoints (`DemoProvisioner`, `DemoPurger`, `DemoQuotaGuard`, `DemoCleanupJob`, `DemoController`, `UserDetailsServiceImpl`, `MeService`) | integration (`AbstractIntegrationTest`, Testcontainers) | Happy + cada edge + cada erro da spec; dados conferidos pela API ou pelo banco | `backend/src/test/java/**/demo/**/*IntegrationTest.java`, `security/integration/` | idem |
| Backend Swagger | integration | `POST /api/auth/demo` com `201`, `429`, `503` no `ApiDocsIntegrationTest` | `backend/src/test/java/**/openapi/ApiDocsIntegrationTest.java` | `cd backend && ./mvnw -q test -Dtest=ApiDocsIntegrationTest` |
| Migration e properties | none | - (build gate only; exercitadas pelos testes de integração) | - | build gate only |
| Frontend lib (`lib/api.ts`, `lib/auth.tsx`) | unit (vitest) | Método, caminho e retorno de `createDemo`; `sessionStorage` só para demo; restauração com sucesso e com `401`; `signOut` apaga | `frontend/lib/*.test.{ts,tsx}` | `cd frontend && corepack pnpm test` |
| Frontend componentes (`DemoButton`, landing, `AppShell`, `DemoBanner`) | unit (jsdom) | 1:1 com DEMO-07, DEMO-08 AC2/AC5 e DEMO-09 AC3–4; testes atuais da landing ajustados só onde a spec mudou o texto | `frontend/components/**/*.test.tsx` | `cd frontend && corepack pnpm test` |
| Docs | none | - | - | build gate only |

## Gate Check Commands

> Gerados do código; confirmar antes do Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick (backend) | Tasks de backend | `cd backend && ./mvnw -q test -Dtest='<classes da task>' -Djacoco.skip=true` |
| Quick (frontend) | Tasks de frontend | `cd frontend && corepack pnpm test && corepack pnpm typecheck` |
| Full | Fim de cada fase de backend e fim da feature | `cd backend && ./mvnw verify` |
| Build | Fim da fase de frontend e docs | `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build` |

---

## Execution Plan

Phases run sequentially; tasks within a phase run in order.

### Phase 1: Base do backend

```
T1
T2
T3
T1 → T4
T5
T6
```

### Phase 2: Criação da demo

```
T7
T7 → T8
T9
T10
T8 → T11
T9 → T11
T10 → T11
T11 → T12
```

### Phase 3: Proteções e operação

```
T13
T14
T15
T16
T17
```

### Phase 4: Frontend

```
T18 → T19
T19 → T20
T19 → T21
T19 → T22
```

### Phase 5: Docs

```
T23
```

---

## Task Breakdown

### T1: Migration e marca de demo no usuário

**What**: `V25__demo_accounts.sql` (`demo_expires_at`, `demo_import_count`, índice parcial); campos em `UserEntity`; `DemoProperties` (`reconpay.demo.*`) com os padrões do design e o bloco no `application.yaml`; `countByDemoExpiresAtAfter` em `IUserRepository`.
**Where**: `backend/src/main/resources/db/migration/V25__demo_accounts.sql`
**Depends on**: None
**Reuses**: padrão de `ReconciliationProperties`
**Requirement**: DEMO-01, DEMO-04

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Flyway aplica a V25 no Testcontainers (suíte de integração existente verde)
- [x] `DemoProperties` carrega 24h, 100, 2, 2, 10, 900000, 30s sem variáveis de ambiente
- [x] Gate quick (backend) passa com uma classe de integração existente de `auth`

**Tests**: none
**Gate**: quick

**Commit**: `feat(demo): add demo account columns and properties`

---

### T2: Gerar CNPJ válido

**What**: `CnpjRules.generate(RandomGenerator)` devolve 14 dígitos com dígitos verificadores corretos.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/shared/CnpjRules.java`
**Depends on**: None
**Reuses**: `checkDigit`, `isValid`
**Requirement**: DEMO-01 (AC4)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] 1000 CNPJs gerados com sementes distintas passam em `isValid`
- [ ] Mesma semente → mesmo CNPJ; nenhum gerado é de dígitos todos iguais
- [ ] Gate quick (backend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(shared): generate valid CNPJ numbers`

---

### T3: Token com validade própria

**What**: `JwtService.generateToken(String email, Duration ttl)`; `generateToken(email)` passa a delegar com `jwt.expiration`.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/security/JwtService.java`
**Depends on**: None
**Reuses**: `generateToken`
**Requirement**: DEMO-01 (AC1)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `exp - iat` do token é igual ao `ttl` informado
- [ ] `generateToken(email)` mantém `exp - iat` = `jwt.expiration`
- [ ] Gate quick (backend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(security): issue tokens with a caller-defined lifetime`

---

### T4: Demo expirada não autentica

**What**: `UserDetailsServiceImpl` recebe `Clock` e lança `UsernameNotFoundException` quando `demoExpiresAt` não é posterior ao instante atual.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/security/UserDetailsServiceImpl.java`
**Depends on**: T1
**Reuses**: tratamento de `UsernameNotFoundException` no `JwtAuthenticationFilter`
**Requirement**: DEMO-06 (AC1)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Integração: token válido de usuário com `demo_expires_at` no passado → `GET /api/me` `401`
- [ ] Fronteira: `demo_expires_at` igual ao instante do relógio → `401`; 1 s depois do relógio → `200`
- [ ] Conta comum (`demo_expires_at` nulo) segue `200`
- [ ] Gate quick (backend) passa

**Tests**: integration
**Gate**: quick

**Commit**: `feat(security): reject expired demo accounts`

---

### T5: Cenário fixo

**What**: `DemoScenario.build(LocalDate today)`: 4 regras de taxa (`CREDIT_CARD`, `DEBIT_CARD`, `PIX`, `BOLETO`, 1x) e os três CSVs nos cabeçalhos dos parsers, ~200 transações de `today-60` a `today-1`, com linhas fixas que provocam `MISSING_SETTLEMENT`, `ORPHAN_SETTLEMENT`, `INCORRECT_AMOUNT`, `FEE_DIVERGENCE` e `MISSING_BANK_CREDIT`.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/demo/service/DemoScenario.java`
**Depends on**: None
**Reuses**: `TransactionCsvParser.HEADER`, `SettlementLayout.RECONPAY.header()`, cabeçalho do `BankStatementCsvParser`
**Requirement**: DEMO-02 (AC5, AC6, AC9)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Os três CSVs passam pelos parsers reais sem erro de linha (`TransactionCsvParser`, `SettlementCsvParser`, `BankStatementCsvParser`)
- [ ] Toda data fica em [`today-60`, `today-1`]; nenhuma é `today` nem posterior
- [ ] Referências de transação únicas; regras para os 4 meios
- [ ] Mesmo `today` → CSVs idênticos; outro `today` → mesmas contagens e mesmo bruto total
- [ ] Gate quick (backend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(demo): define the fixed demo scenario`

---

### T6: CSV em memória

**What**: `InMemoryCsvFile implements MultipartFile` sobre `byte[]` UTF-8, nome e tipo `text/csv`.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/demo/service/InMemoryCsvFile.java`
**Depends on**: None
**Reuses**: NONE
**Requirement**: DEMO-02

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `getBytes`, `getInputStream`, `getSize`, `isEmpty`, `getOriginalFilename`, `getContentType` devolvem o conteúdo e os metadados
- [ ] `transferTo` grava o conteúdo no arquivo
- [ ] Gate quick (backend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(demo): wrap generated CSV content as an upload`

---

### T7: Apagar uma demo

**What**: `DemoPurger.purge(userId)` e `findExpired(now)`, com DELETE nativo na ordem de FK do design; mantém merchant que tenha acesso de conta comum.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/demo/service/DemoPurger.java`
**Depends on**: None
**Reuses**: `AbstractIntegrationTest`
**Requirement**: DEMO-06 (AC2–AC5), DEMO-01 (AC10)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Com merchant populado em todas as tabelas da lista (inclusive divergência resolvida e período fechado), `purge` deixa zero linhas desse merchant em cada tabela e apaga o usuário
- [ ] Merchant concedido também a conta comum fica, com seus dados; só o acesso da demo some
- [ ] `findExpired` traz só demos com `demo_expires_at` ≤ agora; contas comuns e demos ativas ficam de fora
- [ ] Teste de catálogo: toda tabela com FK para `merchants` ou para tabela da lista está na lista do `DemoPurger`
- [ ] Gate quick (backend) passa

**Tests**: integration
**Gate**: quick

**Commit**: `feat(demo): purge a demo account and its merchants`

---

### T8: Provisionar a demo

**What**: `DemoProvisioner.provision(now)`: usuário, merchant com CNPJ gerado, acesso, 4 regras, 3 importações pelos serviços e run `PENDING` de [`today-60`, `today-1`].
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/demo/service/DemoProvisioner.java`
**Depends on**: T7
**Reuses**: `FeeRuleService.create`, os três `importCsv`, `ReconciliationService.run`, `UserMerchantAccessService.grantIfAbsent`, `CnpjRules.generate`, `DemoScenario`, `InMemoryCsvFile`
**Requirement**: DEMO-01 (AC2–AC4), DEMO-02 (AC5, AC6)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Usuário `OPERATOR`, ativo, `demo_expires_at = now + 24h`, e-mail `@demo.reconpay.invalid`, nome "Visitante"
- [ ] Nenhum e-mail capturado pelo `CapturingEmailSender`
- [ ] Exatamente 1 merchant concedido, CNPJ válido; com um CNPJ já existente forçado na 1ª tentativa, gera outro
- [ ] Regras ativas para os 4 meios; um lote de cada tipo; todas as datas em [`today-60`, `today-1`]
- [ ] Falha forçada no meio (ex.: importação lança) não deixa usuário nem merchant
- [ ] Gate quick (backend) passa

**Tests**: integration
**Gate**: quick

**Commit**: `feat(demo): provision the demo account and scenario`

---

### T9: Concluir a conciliação da demo

**What**: `DemoRunExecutor.complete(runId)`: reserva e processa na thread; se o dispatcher reservou, espera `COMPLETED`, `FAILED` ou o timeout.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/demo/service/DemoRunExecutor.java`
**Depends on**: None
**Reuses**: `ReconciliationRunStateService`, `ReconciliationRunProcessor`
**Requirement**: DEMO-02 (AC7)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Reservou → chama `process`; `process` lança → `markFailed` e relança
- [ ] Não reservou → retorna quando o status vira `COMPLETED`; lança em `FAILED`; lança ao passar `runWaitTimeout`
- [ ] Gate quick (backend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(demo): finish the demo reconciliation within the request`

---

### T10: Limite por IP

**What**: `DemoRateLimiter.isAllowed(ip)` e `record(ip)`, janela deslizante de 60 min com `Clock`.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/demo/service/DemoRateLimiter.java`
**Depends on**: None
**Reuses**: `Clock`
**Requirement**: DEMO-03 (AC3–AC5)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Após 2 `record`, o 3º `isAllowed` é falso; outro IP segue verdadeiro
- [ ] 60 min depois da 1ª criação, volta a permitir; 59 min 59 s depois, não
- [ ] `isAllowed` sem `record` não consome cota
- [ ] Gate quick (backend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(demo): limit demo creation per client address`

---

### T11: Orquestrar a criação

**What**: `DemoAccountService.create(ip)` com lock único, `429`, `503`, provisionamento, conclusão do run, compensação e token; exceções novas e `TOO_MANY_REQUESTS`/`SERVICE_UNAVAILABLE` em `ApiErrorCode` e no `GlobalExceptionHandler`.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/demo/service/DemoAccountService.java`
**Depends on**: T8, T9, T10
**Reuses**: `JwtService.generateToken(email, ttl)`, `AuditLogger`, `GlobalExceptionHandler`
**Requirement**: DEMO-01 (AC1, AC10), DEMO-03 (AC3, AC5), DEMO-04 (AC1, AC2, AC8)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] IP bloqueado → `DemoRateLimitException`, sem provisionar
- [ ] 100 ativas → `DemoUnavailableException`, sem provisionar; 99 → cria
- [ ] Falha no provisionamento ou no run → `purge` da demo (quando já gravada), sem `record`
- [ ] Sucesso → `record(ip)` e token com `ttl = min(jwt.expiration, 24h)`
- [ ] Handler: `429` com `TOO_MANY_REQUESTS` e `503` com `SERVICE_UNAVAILABLE`, mensagens da spec
- [ ] Gate quick (backend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(demo): orchestrate demo account creation`

---

### T12: Endpoint `POST /api/auth/demo`

**What**: `DemoController` com `201`; rota pública no `SecurityConfig`; teste ponta a ponta pela API.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/demo/controller/DemoController.java`
**Depends on**: T11
**Reuses**: `AbstractIntegrationTest`, endpoints existentes de merchants, conciliações e divergências
**Requirement**: DEMO-01 (AC1, AC11), DEMO-02 (AC7–AC9), DEMO-03 (AC3), DEMO-04 (AC1, AC2, AC8)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Sem token → `201` com `token`, `type=Bearer`, `expiresIn ≤ 86400`
- [ ] Com o token: 1 merchant; todo run `COMPLETED` com itens `MATCHED` e `DIVERGENT`; divergência `OPEN` de cada um dos 5 tipos
- [ ] Duas demos no mesmo dia: mesmas contagens e mesmo bruto total
- [ ] Token de uma demo em merchant de outra → `403`
- [ ] 3ª criação do mesmo IP → `429`; com 100 ativas → `503`; demos expiradas não contam no teto
- [ ] Duas criações simultâneas com 99 ativas → um `201` e um `503`
- [ ] Gate full passa

**Tests**: integration
**Gate**: full

**Commit**: `feat(demo): expose demo account creation`

---

### T13: IP real atrás do nginx

**What**: `server.forward-headers-strategy: native` em `application-prod.yaml`; teste com servidor real.
**Where**: `backend/src/main/resources/application-prod.yaml`
**Depends on**: None
**Reuses**: `DemoRateLimiter`, `POST /api/auth/demo`
**Requirement**: DEMO-03 (AC6, AC7)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `RANDOM_PORT` com a estratégia `native`: 2 criações com `X-Forwarded-For: 1.1.1.1, 9.9.9.9` e uma 3ª com `X-Forwarded-For: 2.2.2.2, 9.9.9.9` → `429` (conta o endereço mais à direita)
- [ ] Com `server.tomcat.remoteip.internal-proxies` sem `127.0.0.1`: cabeçalhos diferentes do mesmo cliente → 3ª `429` (cabeçalho ignorado)
- [ ] Gate quick (backend) passa

**Tests**: integration
**Gate**: quick

**Commit**: `feat(config): trust forwarded client addresses from the local proxy`

---

### T14: Cotas da demo

**What**: `DemoQuotaGuard` (`checkMerchantCreation`, `consumeImport`) com lock do usuário; `AuthenticatedUserAccessor.currentPrincipal()`; chamadas no início de `MerchantService.create` e dos três `importCsv`; `DemoQuotaExceededException` → `403`.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/demo/service/DemoQuotaGuard.java`
**Depends on**: None
**Reuses**: `AuthenticatedUserAccessor`, `IUserMerchantAccessRepository`
**Requirement**: DEMO-05

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Demo cria o 2º merchant (`201`); o 3º → `403` com a mensagem da spec
- [ ] 10 importações aceitas cobrindo os 3 endpoints; a 11ª em cada endpoint → `403` com a mensagem da spec
- [ ] As 3 importações do cenário não contam; importação recusada por CSV inválido não conta
- [ ] Conta comum faz 3 merchants e 11 importações sem recusa
- [ ] Gate quick (backend) passa

**Tests**: integration
**Gate**: quick

**Commit**: `feat(demo): cap merchants and imports for demo accounts`

---

### T15: Limpeza agendada

**What**: `DemoCleanupJob.purgeExpired()` com `@Scheduled(fixedDelayString = "${reconpay.demo.cleanup-delay-ms:900000}")`, uma transação por demo, `warn` por falha e `info` com as contagens.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/demo/service/DemoCleanupJob.java`
**Depends on**: None
**Reuses**: `DemoPurger`
**Requirement**: DEMO-06 (AC2, AC5–AC7)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Com relógio 24 h à frente: demo expirada e seus dados somem; demo ativa fica
- [ ] Falha numa demo não impede a seguinte
- [ ] Log `info` com usuários e merchants apagados
- [ ] Anotação lida com o atraso padrão de 900000 ms
- [ ] Gate full passa

**Tests**: integration
**Gate**: full

**Commit**: `feat(demo): purge expired demo accounts on a schedule`

---

### T16: `demoExpiresAt` em `/api/me`

**What**: Campo `demoExpiresAt` no `MeResponseDTO`, preenchido pelo `MeService`.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/auth/dto/MeResponseDTO.java`
**Depends on**: None
**Reuses**: `MeService`
**Requirement**: DEMO-09 (AC1, AC2)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Demo → `demoExpiresAt` ISO-8601 igual ao gravado
- [ ] Conta comum → `demoExpiresAt: null`
- [ ] Gate quick (backend) passa

**Tests**: integration
**Gate**: quick

**Commit**: `feat(auth): expose the demo expiry on the current user`

---

### T17: Swagger da demo

**What**: `DemoControllerApi` com descrição, `201`, `429` e `503`; `ApiDocsIntegrationTest` cobre.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/demo/openapi/DemoControllerApi.java`
**Depends on**: None
**Reuses**: anotações de `AuthControllerApi`
**Requirement**: DEMO-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `/v3/api-docs` traz `POST /api/auth/demo` sem segurança, com `201`, `429`, `503`
- [ ] Gate full passa

**Tests**: integration
**Gate**: full

**Commit**: `docs(demo): document the demo endpoint in OpenAPI`

---

### T18: `createDemo` na API do frontend

**What**: `createDemo()` (`POST /api/auth/demo` e `GET /api/me`) e `demoExpiresAt` no tipo `Me`.
**Where**: `frontend/lib/api.ts`
**Depends on**: None
**Reuses**: `request`, `authenticate`
**Requirement**: DEMO-07 (AC2), DEMO-09

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Chama `POST /api/auth/demo` sem corpo e `/api/me` com o token; devolve `{ token, user }`
- [ ] Erro `429` vira `ApiError` com `status` e `message` da API
- [ ] Gate quick (frontend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add the demo account API call`

---

### T19: Sessão da demo no `AuthProvider`

**What**: `startDemo()`, restauração ao montar, `restoring`, `expired`, `signOut` apagando `sessionStorage` (`reconpay.demoToken`); `signIn` não grava.
**Where**: `frontend/lib/auth.tsx`
**Depends on**: T18
**Reuses**: `signIn`
**Requirement**: DEMO-08 (AC1–AC5)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `startDemo` grava o token em `sessionStorage`; `signIn` não grava em `sessionStorage` nem `localStorage`
- [ ] Montar com token salvo restaura `token` e `user`
- [ ] Restauração com `401` apaga o token e marca `expired`
- [ ] `signOut` apaga o token
- [ ] `sessionStorage` que lança não quebra o provider
- [ ] Gate quick (frontend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): keep the demo session across reloads`

---

### T20: `AppShell` espera a restauração

**What**: Enquanto `restoring`, "Abrindo sessão..."; sem token, `/login`, ou `/` quando `expired`.
**Where**: `frontend/components/shell/AppShell.tsx`
**Depends on**: T19
**Reuses**: redirecionamento atual
**Requirement**: DEMO-08 (AC2, AC5)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Durante a restauração não redireciona
- [ ] Restaurado → mostra o conteúdo; sem sessão → `/login`; demo expirada → `/`
- [ ] Gate quick (frontend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): wait for the demo session before guarding routes`

---

### T21: Botão "Ver demonstração"

**What**: `DemoButton` e uso no herói (`LandingPage`) e no `FinalCta`, no lugar de "Criar conta"; o menu mantém "Entrar" e "Criar conta".
**Where**: `frontend/components/landing/DemoButton.tsx`
**Depends on**: T19
**Reuses**: estilo `primaryButton`, `useAuth`
**Requirement**: DEMO-07

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Herói e CTA final mostram "Ver demonstração"; menu mantém `/login` e `/register`
- [ ] Cliques repetidos → uma chamada; durante a chamada, desabilitado com "Preparando demonstração..."
- [ ] `201` → `/painel`
- [ ] `429` e `503` → `message` da API em `role="alert"`; botão reabilitado
- [ ] Falha de rede ou `500` sem `message` → "Não foi possível abrir a demonstração. Tente novamente."
- [ ] Gate quick (frontend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(landing): open a demo account from the landing page`

---

### T22: Faixa de demonstração

**What**: `DemoBanner` no topo do `AppShell`: "Conta de demonstração · expira às HH:mm" no horário local.
**Where**: `frontend/components/shell/DemoBanner.tsx`
**Depends on**: T19
**Reuses**: tokens de cor do tema
**Requirement**: DEMO-09 (AC3, AC4)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Com `demoExpiresAt`, mostra o horário formatado no fuso local
- [ ] Sem `demoExpiresAt`, não renderiza
- [ ] Gate build passa

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): show the demo account expiry`

---

### T23: Docs

**What**: README (endpoint, limites, cotas, limpeza, variáveis `reconpay.demo.*`), `frontend/README.md` (demo e `sessionStorage`), AGENTS.md (módulo `demo`).
**Where**: `README.md`
**Depends on**: None
**Reuses**: NONE
**Requirement**: DEMO-01..09

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] README descreve `POST /api/auth/demo`, `429`/`503`, cotas e limpeza
- [ ] `frontend/README.md` e AGENTS.md atualizados
- [ ] Gate full passa no fim da feature

**Tests**: none
**Gate**: full

**Commit**: `docs: document the demo account`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5

Phase 1:  T1, T2, T3, T4, T5, T6
Phase 2:  T7, T8, T9, T10, T11, T12
Phase 3:  T13, T14, T15, T16, T17
Phase 4:  T18, T19, T20, T21, T22
Phase 5:  T23
```

Dependências exatas nos diagramas de cada fase acima. Execution is strictly sequential within a worker.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1 | Migration + entidade + properties do mesmo conceito | ⚠️ coeso |
| T2–T10, T13, T15–T22 | 1 componente ou função | ✅ |
| T11 | Serviço + 2 exceções + handler (só fazem sentido juntos) | ⚠️ coeso |
| T12 | Controller + 1 linha no `SecurityConfig` | ✅ |
| T14 | Guard + 4 chamadas de uma linha | ⚠️ coeso |
| T23 | Três docs | ⚠️ coeso |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1, T2, T3, T5, T6 | None | — | ✅ |
| T4 | T1 | T1 → T4 | ✅ |
| T7, T9, T10 | None | — | ✅ |
| T8 | T7 | T7 → T8 | ✅ |
| T11 | T8, T9, T10 | T8/T9/T10 → T11 | ✅ |
| T12 | T11 | T11 → T12 | ✅ |
| T13–T17 | None (fases anteriores) | — | ✅ |
| T18 | None | — | ✅ |
| T19 | T18 | T18 → T19 | ✅ |
| T20, T21, T22 | T19 | T19 → T20/T21/T22 | ✅ |
| T23 | None | — | ✅ |

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | Migration e properties | none | none | ✅ |
| T2, T3, T5, T6, T10 | Regra pura | unit | unit | ✅ |
| T9, T11 | Orquestração | unit | unit | ✅ |
| T4, T7, T8, T12, T14, T15, T16 | Persistência e endpoints | integration | integration | ✅ |
| T13 | Config com efeito em endpoint | integration | integration | ✅ |
| T17 | Swagger | integration | integration | ✅ |
| T18, T19 | Frontend lib | unit | unit | ✅ |
| T20, T21, T22 | Frontend componentes | unit | unit | ✅ |
| T23 | Docs | none | none | ✅ |
