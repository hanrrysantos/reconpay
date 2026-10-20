# Demonstração por Visitante Validation

**Verdict**: FAIL
**Result**: FAIL

**Date**: 2026-10-20
**Round**: 1 of max 3
**Spec**: `.specs/features/demo-account/spec.md`
**Diff range**: `2500673..7acef5c` (27 commits: `89fb1ae`, `5340cfb`, `eb47021`, `d38061d` docs; `c7d6e55`..`7acef5c` T1–T23)
**Verifier**: independent sub-agent (author ≠ verifier)

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1–T17 (backend) | ✅ Done | `tasks.md` marca Done; commits `c7d6e55`..`2c22a3c` |
| T18–T22 (frontend) | ✅ Done | commits `040c8a7`..`88d4bee` |
| T23 (docs) | ✅ Done | `7acef5c` |

---

## Spec-Anchored Acceptance Criteria

Caminhos abreviados: `B/` = `backend/src/test/java/br/com/hanrry/reconpay/`, `F/` = `frontend/`.

### DEMO-01 / DEMO-02: Abrir uma demonstração

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 `POST /api/auth/demo` anônimo | `201`, `token`, `type`=`Bearer`, `expiresIn` ≤ 86400 | `B/demo/controller/DemoControllerIntegrationTest.java:76-80` - `status().isCreated()`, `$.token` isNotEmpty, `$.type` `"Bearer"`, `$.expiresIn` `lessThanOrEqualTo(86400)` e `greaterThan(0)`; `B/demo/service/DemoAccountServiceTest.java:246` `expiresIn` `isEqualTo(86400L)` com JWT de 7 dias; `:256` `3600L` quando o JWT é menor | ✅ PASS |
| AC2 usuário gravado | `OPERATOR`, ativo, `demo_expires_at` = criação + 24 h, e-mail `@demo.reconpay.invalid` | `B/demo/service/DemoProvisionerIntegrationTest.java:69-74` - `matches("demo-[0-9a-f]{12}@demo\\.reconpay\\.invalid")`, `getRole()` `OPERATOR`, `isActive()` true, `getDemoExpiresAt()` `isEqualTo(now.plus(Duration.ofHours(24)))` (B15 morto) | ✅ PASS |
| AC3 sem e-mail | nenhum e-mail enviado | `B/demo/service/DemoProvisionerIntegrationTest.java:75` - `emailSender.lastEmail()).isNull()` | ✅ PASS |
| AC4 exatamente 1 merchant, CNPJ válido e inédito | 1 acesso; `CnpjRules.isValid`; colisão sorteia outro | `B/demo/service/DemoProvisionerIntegrationTest.java:87` `hasSize(1)`, `:90` `isValid(document)` true; `:122-129` duas tentativas, a segunda difere da primeira e é a gravada; `B/demo/controller/DemoControllerIntegrationTest.java:252` `$.totalElements` 1 pelo token | ✅ PASS |
| AC5 regras de taxa | ativas para `CREDIT_CARD`, `DEBIT_CARD`, `PIX`, `BOLETO` | `B/demo/service/DemoProvisionerIntegrationTest.java:92-94` - `... AND active` `containsExactlyInAnyOrder("CREDIT_CARD","DEBIT_CARD","PIX","BOLETO")` | ✅ PASS |
| AC6 datas | nos 60 dias anteriores ao dia da criação, nenhuma posterior | `B/demo/service/DemoProvisionerIntegrationTest.java:100-107` - min `isAfterOrEqualTo(today.minusDays(60))`, max `isBeforeOrEqualTo(today.minusDays(1))` nas três tabelas (B12 morto); `B/demo/service/DemoScenarioTest.java:67-68` `isBetween(first, last)`, `doesNotContain(TODAY)` | ✅ PASS |
| AC7 conciliações | todas `COMPLETED`, com itens `MATCHED` e `DIVERGENT` | `B/demo/controller/DemoControllerIntegrationTest.java:90-98` - `everyItem(is("COMPLETED"))`, `matchedCount`/`divergentCount` `greaterThan(0)`, filtros `result=MATCHED` e `result=DIVERGENT` com `totalElements > 0` | ✅ PASS |
| AC8 divergências | ao menos uma `OPEN` de cada um dos 5 tipos | `B/demo/controller/DemoControllerIntegrationTest.java:99-103` - para cada tipo, `discrepancies[?(@.type == type && @.status == 'OPEN')]` `not(emptyIterable())` | ✅ PASS |
| AC9 mesmo cenário no mesmo dia | mesmas quantidades e mesmo valor bruto | `B/demo/controller/DemoControllerIntegrationTest.java:114-115` - `transactions` 200 e `secondScenario` `isEqualTo(firstScenario)` (transações, liquidações, extrato, bruto, itens, divergências); `B/demo/service/DemoScenarioTest.java:97-100`, `:117-120` | ✅ PASS |
| AC10 falha no cenário | **`500`** e nada da demo gravado | Rollback: `B/demo/service/DemoProvisionerIntegrationTest.java:140-147` - exceção propagada, `userRepository.count()` e `merchantRepository.count()` inalterados. Compensação: `B/demo/service/DemoAccountServiceTest.java:279-281` - `isSameAs(failure)` e `verify(purger).purge(ACCOUNT.userId())` (B11 morto). **Status `500`: nenhuma evidência.** Nenhum teste chama `POST /api/auth/demo` com falha no cenário, e nenhum teste do repositório afirma `500`/`INTERNAL_SERVER_ERROR` (busca por `isInternalServerError`, `value(500)`, `INTERNAL_SERVER_ERROR` em `backend/src/test`: zero ocorrências) | ❌ GAP (conjunção: metade "responder 500" sem evidência) |
| AC11 isolamento | `403` ao ler ou alterar merchant de outro usuário | `B/demo/controller/DemoControllerIntegrationTest.java:123-137` - `GET /merchants/{id}`, `GET .../transactions`, `POST .../reconciliations` com `status().isForbidden()` e `$.error` `"FORBIDDEN"` | ✅ PASS |

### DEMO-03 / DEMO-04: Limitar a criação

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 teto 100 | `503`, `SERVICE_UNAVAILABLE`, "Demonstrações esgotadas no momento. Tente novamente mais tarde.", sem criar | `B/demo/controller/DemoControllerIntegrationTest.java:162-168` - `isServiceUnavailable()`, `$.status` 503, `$.error`, `$.message` exata, `userRepository.count()` inalterado; `B/demo/service/DemoAccountServiceTest.java:217-223` com 100 → exceção e `verifyNoInteractions(provisioner, ...)`; `:229-236` com 99 → cria (B3 morto) | ✅ PASS |
| AC2 expiradas não contam | 99 ativas + expiradas → cria | `B/demo/controller/DemoControllerIntegrationTest.java:173-178` - 99 ativas e 5 expiradas, `status().isCreated()` (B4 morto) | ✅ PASS |
| AC3 2 por IP por hora | `429`, `TOO_MANY_REQUESTS`, "Limite de demonstrações atingido. Tente novamente em até 1 hora.", sem criar | `B/demo/controller/DemoControllerIntegrationTest.java:143-153` - duas `201`, a terceira `isTooManyRequests()`, `$.status` 429, `$.error`, `$.message` exata, `count()` inalterado; outro IP segue `201` (`:154`); `B/demo/service/DemoRateLimiterTest.java:317-323` (B2 morto) | ✅ PASS |
| AC4 janela de 60 min | volta a aceitar quando uma criação sai da janela | `B/demo/service/DemoRateLimiterTest.java:332-336` - em 59:59 `isFalse()`, em 60:00 `isTrue()` (B1 morto); `:344-349` | ✅ PASS |
| AC5 recusas não contam | `429`, `503`, `500` não consomem a cota | `B/demo/service/DemoAccountServiceTest.java:211` (429), `:224` (503), `:269` (falha no provisionamento), `:282` (falha na conciliação) - `verify(rateLimiter, never()).record(anyString())`; `:236` `verify(rateLimiter).record(IP)` no sucesso (B10 morto); `B/demo/service/DemoRateLimiterTest.java:354-358` | ✅ PASS |
| AC6 `X-Forwarded-For` em `prod` | endereço mais à direita, vindo de proxy interno; XFF forjado não muda o IP | `B/demo/controller/DemoForwardedAddressIntegrationTest.java:45-51` - duas criações com `"1.1.1.1, 9.9.9.9"`, a terceira com `"2.2.2.2, 9.9.9.9"` dá `429` (mesmo IP à direita, primeiro forjado diferente), e `"1.1.1.1, 8.8.8.8"` cria. Ativado por `@TestPropertySource(server.forward-headers-strategy=native)` (`:24`); o profile `prod` define o mesmo valor em `backend/src/main/resources/application-prod.yaml:3` (lido, não carregado em teste) | ✅ PASS (⚠️ a ligação ao profile `prod` é por inspeção do YAML) |
| AC7 proxy não confiável | ignora XFF e usa o IP da conexão | `B/demo/controller/DemoUntrustedProxyIntegrationTest.java:128-133` - `internal-proxies` sem 127.0.0.1; três XFF diferentes, a terceira `429` | ✅ PASS |
| AC8 criação serializada | simultâneas nunca passam de 100 | `B/demo/controller/DemoControllerIntegrationTest.java:184-209` - 99 ativas, duas threads, `containsExactlyInAnyOrder(201, 503)`, `$.error` `SERVICE_UNAVAILABLE`, `countByDemoExpiresAtAfter` `isEqualTo(100L)` (B13 morto) | ✅ PASS |

### DEMO-05: Cotas da conta de demo

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 2 merchants | 3º `POST /api/merchants` → `403`, `FORBIDDEN`, "A demonstração permite no máximo 2 empresas.", sem criar | `B/demo/service/DemoQuotaGuardIntegrationTest.java:212-221` - 2º `isCreated()`, 3º `isForbidden()`, `$.status` 403, `$.error`, `$.message` exata, `totalElements` 2 (B6 morto) | ✅ PASS |
| AC2 10 importações | 11ª em cada endpoint → `403`, `FORBIDDEN`, "A demonstração permite no máximo 10 importações.", sem importar | `B/demo/service/DemoQuotaGuardIntegrationTest.java:238-252` - 10 `isCreated()` nos três endpoints; depois, em transações, liquidações e extrato, `isForbidden()`, `$.status` 403, `$.error`, `$.message` exata e lotes inalterados; `demo_import_count` 10 (B7 morto) | ✅ PASS |
| AC3 cenário não conta | as 3 importações do cenário fora da cota | mesmo teste: as 10 aceitas vêm depois das 3 do cenário (`:224`, `:233-240`) | ✅ PASS |
| AC4 contas comuns sem cota | operador comum cria 3 merchants e faz 11 importações | `B/demo/service/DemoQuotaGuardIntegrationTest.java:260-276` - `isCreated()` em todos (B8 morto) | ✅ PASS |

### DEMO-06: Expiração e limpeza

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 expirada → `401` | `401` | `B/security/integration/DemoExpiryIntegrationTest.java:529-540` - expirada e expirando no instante atual `isUnauthorized()`, `$.error` `UNAUTHORIZED`; `:547-549` 1 s depois ainda `isOk()` (B5 morto) | ✅ PASS |
| AC2 apaga usuários expirados | usuário de demo expirado some | `B/demo/service/DemoCleanupJobIntegrationTest.java:368-371` - `existsById(demo)` false; `B/demo/service/DemoPurgerIntegrationTest.java:131-134` `findExpired` contém expirada e "expira agora" | ✅ PASS |
| AC3 apaga merchants só de demos com todos os dados | transações, importações, liquidações, extratos, taxas, conciliações, divergências e períodos | `B/demo/service/DemoPurgerIntegrationTest.java:89-95` - antes todas as 15 tabelas `isPositive()`, depois `isZero()`; `:138-161` toda tabela que alcança `merchants` sem cascade está em `purgedTables()`; `B/demo/service/DemoCleanupJobIntegrationTest.java:372` `rowsOf(merchantId)` zero | ✅ PASS (ver gap 2 sobre demos ativas) |
| AC4 merchant compartilhado com conta comum | mantém merchant e dados; remove só o acesso da demo | `B/demo/service/DemoPurgerIntegrationTest.java:111-120` - `purgedMerchants` zero, `accessHolders` `containsExactly(regular)`, demais contagens iguais, demo apagado (B9 morto) | ✅ PASS |
| AC5 não apaga demo não expirada | ativa fica | `B/demo/service/DemoCleanupJobIntegrationTest.java:373` - `existsById(active)` true; `B/demo/service/DemoPurgerIntegrationTest.java:134` `doesNotContain(active, regular)` | ✅ PASS |
| AC6 a cada 15 min | 900000 ms | `B/demo/service/DemoCleanupJobIntegrationTest.java:412` - `fixedDelayString` `"${reconpay.demo.cleanup-delay-ms:900000}"`; `backend/src/main/resources/application.yaml` `cleanup-delay-ms: ${DEMO_CLEANUP_DELAY_MS:900000}` (o profile de teste usa 86400000 em `backend/src/test/resources/application-test.yaml:15` para não disparar durante a suíte) | ✅ PASS |
| AC7 log | quantos usuários e merchants apagou | `B/demo/service/DemoCleanupJobIntegrationTest.java:374-378` - `INFO` `isEqualTo("Limpeza de demonstrações concluída \| usuarios=1 merchants=1")`; `:399-403` `usuarios=1 merchants=0` com uma falha (B14 morto) | ✅ PASS |

### DEMO-07: "Ver demonstração" na landing

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 botões | "Ver demonstração" no herói e no CTA final em vez de "Criar conta"; menu com "Entrar" `/login` e "Criar conta" `/register` | `F/components/landing/LandingPage.test.tsx:38-39` herói `getByRole('button', {name: 'Ver demonstração'})` e sem link "Criar conta"; `:202-203` no CTA final; `:15-24` menu `['Entrar', '/login']`, `['Criar conta', '/register']` | ✅ PASS |
| AC2 uma chamada | `POST /api/auth/demo` uma vez com cliques repetidos | `F/components/landing/DemoButton.test.tsx:38-49` - 3 cliques, `demoCalls(fetcher)` `toHaveLength(1)`, método `POST` (F2b morto; F2 equivalente, ver sensor) | ✅ PASS |
| AC3 em andamento | "Preparando demonstração..." e desabilitado | `F/components/landing/DemoButton.test.tsx:42-43` - `toBeDisabled()`, `toHaveTextContent('Preparando demonstração...')` | ✅ PASS |
| AC4 `201` | `/painel` autenticado com o token da demo | `F/components/landing/DemoButton.test.tsx:58-60` - `push` `'/painel'` uma vez, `sessionStorage` `'demo-jwt'`; `F/lib/api.test.ts` `createDemo` carrega `/api/me` com `Bearer demo-jwt` | ✅ PASS |
| AC5 `429`/`503` | `message` da resposta junto ao botão, reabilitado | `F/components/landing/DemoButton.test.tsx:64-76` - `findByRole('alert')` `toHaveTextContent(message)` para 429 e 503, `toBeEnabled()` (F5 morto) | ✅ PASS |
| AC6 rede ou `500` sem `message` | "Não foi possível abrir a demonstração. Tente novamente.", reabilitado | `F/components/landing/DemoButton.test.tsx:79-90` - rede e `500` vazio, alerta exato, `toBeEnabled()` | ✅ PASS |

### DEMO-08: Sessão da demo sobrevive ao recarregar

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 grava em `sessionStorage` | token da demo salvo | `F/lib/auth.test.tsx:163-165` - `sessionStorage.getItem(KEY)` `'demo-jwt'` (F9 morto) | ✅ PASS |
| AC2 restaura ao recarregar | sessão sem pedir login | `F/lib/auth.test.tsx:181-192` - token e usuário restaurados por `/api/me`; `F/components/shell/AppShell.test.tsx:33-39` sem redirecionar durante a restauração (F7 morto) | ✅ PASS |
| AC3 login comum fora do storage | nada em `sessionStorage` nem `localStorage` | `F/lib/auth.test.tsx:175-177` - `sessionStorage.length` 0, `localStorage.length` 0 (F1 morto) | ✅ PASS |
| AC4 logout apaga | token removido | `F/lib/auth.test.tsx:222-225` - `getItem(KEY)` `toBeNull()` (F4 morto) | ✅ PASS |
| AC5 `401` na restauração | apaga o token e leva a `/` | `F/lib/auth.test.tsx:210-213` - `expired` true, `getItem(KEY)` null (F6 morto); `F/components/shell/AppShell.test.tsx:43-49` `replace` `'/'` e não `'/login'` (F3 morto) | ✅ PASS |

### DEMO-09: Aviso de conta de demonstração

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 `demoExpiresAt` na demo | ISO-8601 | `B/auth/controller/MeControllerIntegrationTest.java:81` - `$.demoExpiresAt` `"2099-10-21T12:34:56Z"` | ✅ PASS |
| AC2 conta comum | `demoExpiresAt` = `null` | `B/auth/controller/MeControllerIntegrationTest.java:62` - `containsEntry("demoExpiresAt", null)` | ✅ PASS |
| AC3 faixa | "Conta de demonstração · expira às HH:mm" no horário local | `F/components/shell/DemoBanner.test.tsx:263-271` - TZ `America/Sao_Paulo`, texto exato `09:34` e `23:05` (F8 morto); `F/components/shell/AppShell.test.tsx:69-75` faixa antes da navegação | ✅ PASS |
| AC4 sem faixa em conta comum | nada | `F/components/shell/DemoBanner.test.tsx:273-277` `toBeEmptyDOMElement()`; `F/components/shell/AppShell.test.tsx:78-83` | ✅ PASS |

**Status**: ❌ 1 gap (DEMO-01 AC10, status `500`); 1 nota de precisão (DEMO-03 AC6 via YAML).

---

## Edge Cases

- [x] Duas criações simultâneas com 99 ativas → uma `201`, outra `503`: `B/demo/controller/DemoControllerIntegrationTest.java:203-209` (B13 morto).
- [x] Reinício zera o limite por IP; o teto continua: o limite é um `HashMap` em memória (`DemoRateLimiter.java:24`), o teto é `countByDemoExpiresAtAfter` no banco, provado pelos testes que enchem o teto com linhas inseridas por SQL (`DemoControllerIntegrationTest.java:279-286`). ⚠️ Nenhum teste reinicia o contexto; coberto por construção.
- [x] CNPJ colide → outro antes de gravar: `B/demo/service/DemoProvisionerIntegrationTest.java:117-129`.
- [x] Perto da meia-noite, dia da criação no fuso da aplicação, sem data futura: o `Clock` da aplicação é `Clock.systemUTC()` (`TimeConfig.java`) e o provisionador usa `LocalDate.ofInstant(now, ZoneOffset.UTC)`; `B/demo/service/DemoScenarioTest.java:33` fixa 23:59:59 e `:67-68` exige nenhuma data em `TODAY`.

---

## Discrimination Sensor

Worktree descartável `git worktree add <scratchpad>/verify-wt HEAD`; uma mutação por vez, só as classes relevantes, `git checkout` entre mutações.

| # | Arquivo (main) | Mutação | Testes | Resultado |
| --- | --- | --- | --- | --- |
| B1 | `demo/service/DemoRateLimiter.java` | janela: `!isAfter(windowStart)` → `isBefore(windowStart)` (criação de exatamente 60 min continua contando) | `DemoRateLimiterTest` | ✅ Killed |
| B2 | `DemoRateLimiter.java` | `size() < perIpPerHour` → `<=` (3 por IP) | `DemoRateLimiterTest`, `DemoControllerIntegrationTest` | ✅ Killed |
| B3 | `demo/service/DemoAccountService.java` | teto `>= maxActive` → `>` | `DemoAccountServiceTest`, `DemoControllerIntegrationTest` | ✅ Killed |
| B4 | `DemoAccountService.java` | `countByDemoExpiresAtAfter(now)` → `count()` (expiradas e comuns contam) | idem | ✅ Killed |
| B5 | `security/UserDetailsServiceImpl.java` | `!expiresAt.isAfter(now)` → `expiresAt.isBefore(now)` | `DemoExpiryIntegrationTest` | ✅ Killed |
| B6 | `demo/service/DemoQuotaGuard.java` | merchants `>= maxMerchants` → `>` | `DemoQuotaGuardIntegrationTest` | ✅ Killed |
| B7 | `DemoQuotaGuard.java` | importações `>= maxImports` → `>` | idem | ✅ Killed |
| B8 | `DemoQuotaGuard.java` | remove `.filter(CustomUserDetails::isDemo)` (cota em conta comum) | idem | ✅ Killed |
| B9 | `demo/service/DemoPurger.java` | `holder.demo_expires_at IS NULL` → `... AND false` (apaga merchant compartilhado com conta comum) | `DemoPurgerIntegrationTest` | ✅ Killed |
| B10 | `DemoAccountService.java` | `rateLimiter.record(ip)` logo após o `isAllowed` (recusas contam) | `DemoAccountServiceTest` | ✅ Killed |
| B11 | `DemoAccountService.java` | remove `purger.purge(userId)` na compensação | `DemoAccountServiceTest` | ✅ Killed |
| B12 | `demo/service/DemoProvisioner.java` | dia do cenário `+1` (datas futuras) | `DemoProvisionerIntegrationTest` | ✅ Killed |
| B13 | `DemoAccountService.java` | `ReentrantLock` com `lock()`/`unlock()` vazios (sem serialização) | `DemoControllerIntegrationTest` (`twoSimultaneous...`) | ✅ Killed |
| B14 | `demo/service/DemoCleanupJob.java` | remove `users++` (log errado) | `DemoCleanupJobIntegrationTest` | ✅ Killed |
| B15 | `DemoProvisioner.java` | `demo_expires_at` = ttl + 1 s | `DemoProvisionerIntegrationTest` | ✅ Killed |
| F1 | `frontend/lib/auth.tsx` | `signIn` grava o token em `sessionStorage` | `lib/auth.test.tsx` | ✅ Killed |
| F2 | `frontend/components/landing/DemoButton.tsx` | remove a guarda `if (started.current) return` | `DemoButton.test.tsx` | ⚠️ Survived: mutante equivalente. `disabled={pending}` já bloqueia o 2º clique (o React aplica o estado de evento discreto antes do próximo evento) |
| F2b | `DemoButton.tsx` | remove `disabled={pending}` (guarda mantida) | `DemoButton.test.tsx` | ✅ Killed (`toBeDisabled`, `:42`) |
| F2c | `DemoButton.tsx` | remove guarda **e** `disabled` | `DemoButton.test.tsx` | ✅ Killed (`:42`) |
| F3 | `frontend/components/shell/AppShell.tsx` | demo expirada → `'/login'` em vez de `'/'` | `AppShell.test.tsx` | ✅ Killed |
| F4 | `auth.tsx` | `signOut` não apaga o token | `auth.test.tsx` | ✅ Killed |
| F5 | `DemoButton.tsx` | mensagem da API só para `429` (503 vira genérica) | `DemoButton.test.tsx` | ✅ Killed |
| F6 | `auth.tsx` | `401` na restauração não apaga o token | `auth.test.tsx` | ✅ Killed |
| F7 | `auth.tsx` | `restoring` começa `false` | `auth.test.tsx`, `AppShell.test.tsx` | ✅ Killed |
| F8 | `frontend/components/shell/DemoBanner.tsx` | hora em UTC em vez da local | `DemoBanner.test.tsx` | ✅ Killed |
| F9 | `auth.tsx` | `startDemo` não grava em `sessionStorage` | `auth.test.tsx`, `DemoButton.test.tsx` | ✅ Killed |

Nenhum log de mutante tem erro de compilação; cada morte é falha de asserção (`Failures`, `Errors: 0`).

**Sensor depth**: P0 expandido (segurança e abuso): 26 mutações manuais (24 principais e 2 sondas, F2b e F2c), 25 mortas e 1 sobrevivente equivalente (F2).
**Isolation**: `git status --porcelain` da árvore real vazio antes e depois; worktree removido com `git worktree remove --force`.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ |
| Surgical changes | ✅ (toques fora de `demo/`: guarda de cota no início dos três imports e de `MerchantService.create`, `currentPrincipal()` opcional, `HEADER` públicos para o cenário) |
| No scope creep | ✅ |
| Matches patterns | ✅ (`StandardError`, Flyway `V25`, `AbstractIntegrationTest`) |
| Spec-anchored outcome check | ❌ DEMO-01 AC10 sem `500` |
| Per-layer Coverage Expectation | ⚠️ rota `POST /api/auth/demo` sem o caminho de erro `500` |
| Every test maps to a spec requirement | ✅ |
| Documented guidelines followed: `AGENTS.md` | ✅ |

---

## Gate Check

- **Backend** `cd backend && ./mvnw verify`: BUILD SUCCESS, 738 testes, 0 falhas, 0 erros, 0 ignorados; "All coverage checks have been met."
- **Frontend** `corepack pnpm test`: 79 arquivos, 991 testes, todos passam; `typecheck` exit 0; `build` exit 0.
- **Contagem de métodos de teste** (`@Test` no backend / `it(` no frontend, por `git grep`): backend 597 → 664 (+67), frontend 730 → 750 (+20). Nenhum teste removido; nas edições em testes existentes (`LandingPage.test.tsx`, `page.test.tsx`, `AppShell.test.tsx`, `*ServiceTest` com o novo colaborador) as asserções trocadas acompanham DEMO-07 AC1 e nenhuma foi afrouxada.

---

## Ranked Gaps

1. **DEMO-01 AC10: status `500` sem evidência** (Major). O rollback e a compensação estão provados, mas nenhum teste faz `POST /api/auth/demo` falhar e verifica `500` com `$.error` `INTERNAL_SERVER_ERROR`. O repositório inteiro não tem asserção de `500`.
   - **Fix task**: em `DemoControllerIntegrationTest` (ou classe nova com `@MockitoSpyBean`), um teste que faz o cenário falhar (ex.: `DemoRunExecutor.complete` lança) e afirma `status().isInternalServerError()`, `$.error` `"INTERNAL_SERVER_ERROR"`, nenhum usuário/merchant novo (`count()` inalterado), e que o mesmo IP ainda cria 2 demos depois (DEMO-03 AC5 pelo `500` real). Um segundo caso com falha no provisionamento fecha a classe.
2. **DEMO-06 AC3: merchant compartilhado com demo ainda ativa** (Minor). A spec apaga o merchant quando "todos os usuários com acesso são demos expiradas"; `DemoPurger.java` (`DEMO_ONLY_MERCHANTS`) só poupa merchants com acesso de conta comum, então um merchant concedido a uma demo expirada e a uma demo ativa sai junto com os dados da ativa. O `design.md` (`DemoPurger`) registra a regra mais larga ("demo_expires_at não nulo") sem `SPEC_DEVIATION`. Só acontece se um ADMIN conceder o merchant de uma demo a outra demo. Decidir: alinhar o código à spec (e testar), ou ajustar a spec à decisão do design.
3. **Nota (sem fix obrigatório)**: DEMO-03 AC6 liga-se ao profile `prod` por inspeção de `application-prod.yaml:3`; o teste usa `@TestPropertySource` com o mesmo valor.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| DEMO-01 | Implementing | ❌ Needs Fix (AC10) |
| DEMO-02 | Implementing | ✅ Verified |
| DEMO-03 | Implementing | ✅ Verified |
| DEMO-04 | Implementing | ✅ Verified |
| DEMO-05 | Implementing | ✅ Verified |
| DEMO-06 | Implementing | ✅ Verified (gap 2 é decisão de spec) |
| DEMO-07 | Implementing | ✅ Verified |
| DEMO-08 | Implementing | ✅ Verified |
| DEMO-09 | Implementing | ✅ Verified |

---

## Summary

**Overall**: ❌ Not Ready (1 gap de AC)

**Spec-anchored check**: 53/54 critérios com evidência no valor da spec; 1 gap (DEMO-01 AC10, `500`); 1 nota de precisão
**Sensor**: 26 injetadas, 25 mortas, 1 sobrevivente equivalente (F2, sem gap de AC)
**Gate**: backend 738 passed; frontend 991 passed, typecheck e build ok

**What works**: criação com cenário completo, teto global serializado, limite por IP com XFF confiável, cotas, expiração, limpeza com log, botão, sessão em `sessionStorage` e faixa.

**Next steps**: fix task do gap 1 (só testes); decidir o gap 2; re-verificar.
