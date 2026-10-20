# Redirecionar E-mails em Dev Validation

**Verdict**: PASS (rodada 2)

**Date**: 2026-10-19
**Spec**: `.specs/features/dev-email-redirect/spec.md`
**Diff range**: rodada 1 `1c86d83..0c50e3a`; rodada 2 `0c50e3a..1c5bd3c` (fix). Rodada 1: commits `1af99e9` spec, `efde52d` implementação, `0c50e3a` README; branch `feat/dev-email-redirect`)
**Rounds**: 2
**Verifier**: sub-agente independente (autor ≠ verificador)

**Rodada 2 (seção no fim)**: o fix `1c5bd3c` fecha RED-02 AC1. Num contexto Spring real, com a variável no ambiente do processo, o envio vai para o endereço do cadastro sem profile, em `test` e em `prod`, e para o destino só em `dev`. O sensor matou 23 de 23 mutantes, M16 incluído. RED-01 e RED-02 estão Verified.

## Rodada 1

RED-01 está correto e bem testado: o sensor matou 15 de 16 mutantes. RED-02 AC1 falha: a variável `RECONPAY_EMAIL_REDIRECT_TO`, exportada como variável de ambiente do processo, liga o redirecionamento nos profiles `prod` e `test` e também sem profile. O relaxed binding do Spring liga o nome da variável à propriedade `reconpay.email.redirect-to` sem passar pelo `application-dev.yaml`. O `EmailRedirectProfileTest` não pega isso porque simula o ambiente com um `MapPropertySource` comum, que não tem esse mapeamento.

---

## Task Completion

Feature média, sem `tasks.md`. A implementação está em um commit (`efde52d`) e a documentação em outro (`0c50e3a`).

---

## Spec-Anchored Acceptance Criteria

### RED-01: Redirecionar no dev

| Critério | Resultado definido na spec | `file:line` + asserção | Result |
| --- | --- | --- | --- |
| AC1 com `redirect-to`, manda para esse endereço e só para ele | `to` = `[destino]`, um elemento | `backend/src/test/java/br/com/hanrry/reconpay/auth/email/ResendEmailSenderTest.java:59` - `jsonPath("$.to.length()").value(1)`; `:60` - `jsonPath("$['to'][0]").value(expectedTo)`; caso em `:72-75` (`expectedTo` = `dev@example.com`). Código: `backend/src/main/java/br/com/hanrry/reconpay/auth/email/ResendEmailSender.java:36,41` | ✅ PASS |
| AC2 mantém HTML e texto do cadastro e prefixa o assunto com `[dev → <cadastro>] ` | HTML e texto iguais aos montados para o cadastro; assunto `[dev → ana@lojacentro.com.br] Confirme seu e-mail — ReconPay` | `ResendEmailSenderTest.java:55` (monta o esperado com `toEmail`), `:62-63` - `jsonPath("$.html").value(expected.html())`, `jsonPath("$.text").value(expected.text())`; `:61` + `:74` - assunto exato com o prefixo. Corpo cita o cadastro e o link e não cita o destino: `:83-86`. Código: `ResendEmailSender.java:34,37` | ✅ PASS |
| AC3 ausente, vazio ou só espaços: endereço do cadastro e assunto de hoje | `to` = cadastro; assunto `Confirme seu e-mail — ReconPay` | `ResendEmailSenderTest.java:89-94` - `@NullSource` + `""` + `"   "`, `expectedTo` = `ana@lojacentro.com.br`, assunto sem prefixo. Normalização: `backend/src/main/java/br/com/hanrry/reconpay/auth/config/EmailProperties.java:24`; espaços em volta: `ResendEmailSenderTest.java:96-100` | ✅ PASS |
| AC4 log com o endereço do cadastro e o de destino | os dois endereços no log | `ResendEmailSenderTest.java:113` - `assertThat(output).contains("ana@lojacentro.com.br").contains("redirected to dev@example.com")`. Código: `ResendEmailSender.java:57-58` | ✅ PASS |

### RED-02: Só no profile dev

| Critério | Resultado definido na spec | `file:line` + asserção | Result |
| --- | --- | --- | --- |
| AC1 sem o profile `dev` (`test`, `prod`) e com `RECONPAY_EMAIL_REDIRECT_TO` definida: ignora a variável | `redirectTo()` nulo; e-mail vai para o cadastro | O teste `backend/src/test/java/br/com/hanrry/reconpay/auth/config/EmailRedirectProfileTest.java:42-43` passa, mas o que ele testa não é o que acontece de verdade: `:19,24` põe a variável num `MapPropertySource("env", ...)`, e o Spring só aplica o mapeamento de nomes de variável de ambiente à fonte `systemEnvironment` (`SystemEnvironmentPropertySource`). **Contraprova do Verifier** (probe descartável no worktree de scratch): `SpringApplicationBuilder` com `@EnableConfigurationProperties(EmailProperties.class)`, `WebApplicationType.NONE`, rodado com `RECONPAY_EMAIL_REDIRECT_TO=probe@example.com` no ambiente do processo → `redirectTo()` = `probe@example.com` nos profiles `prod`, `test` e `dev`. Um `Binder` sobre `application.yaml` sem profile e com a variável numa `SystemEnvironmentPropertySource` chamada `systemEnvironment` dá o mesmo resultado, para `RECONPAY_EMAIL_REDIRECT_TO` e para `RECONPAY_EMAIL_REDIRECTTO`. A causa é o mapeamento legado de `SystemEnvironmentPropertyMapper`: `reconpay.email.redirect-to` → `RECONPAY_EMAIL_REDIRECT_TO`. Com isso o comentário em `EmailProperties.java:23` ("application-dev.yaml is the only place that maps RECONPAY_EMAIL_REDIRECT_TO") está errado. O `test` também não entra no teste (`:42` só usa `[]` e `prod`) | ❌ GAP |
| AC2 README documenta a variável como só do `dev`, com endereço genérico | variável listada; "só no profile `dev`"; exemplo genérico | `README.md:124` (`RECONPAY_EMAIL_REDIRECT_TO=` vazio no bloco do `.env`); `README.md:127` ("só no profile `dev`", `voce@exemplo.com`, prefixo do assunto). A frase final "Em `test` e `prod` ela é ignorada" não vale hoje, pelo gap do AC1 | ⚠️ PASS no texto, mas a afirmação é falsa até o AC1 ser corrigido |

**Mitigação atual, que não fecha o AC**: o `docker-compose.yaml:32-38` passa ao container da API uma lista fechada de variáveis e `RECONPAY_EMAIL_REDIRECT_TO` não está nela. A produção atual não redireciona enquanto ninguém incluir a variável ali. Mas a aplicação não garante o AC: basta a variável estar no ambiente do processo (Compose, systemd, `export` antes do `java -jar`, um `./mvnw verify` rodado num shell em que ela foi exportada) para redirecionar fora do `dev`.

**Status**: ❌ RED-02 AC1 com gap. RED-01 com todos os ACs cobertos.

---

## Edge Cases

- [x] Endereço do cadastro igual ao de destino: um destinatário, com prefixo. `ResendEmailSenderTest.java:102-106` (e `:59`, `length()` = 1).

---

## Discrimination Sensor

Scratch isolado: `git worktree add --detach /home/hanrry/.claude/jobs/3748ece6/tmp/verifier-wt2 HEAD`. Os mutantes foram aplicados, testados e revertidos com `git checkout -- .` no worktree por `/home/hanrry/.claude/jobs/3748ece6/tmp/sensor2/run.py`. Comando: `./mvnw -q test -Dtest=ResendEmailSenderTest,EmailRedirectProfileTest -Djacoco.skip=true`. Antes das mutações o comando terminou com exit 0. Depois o worktree foi removido (`git worktree remove --force`). `git status --porcelain` da árvore real ficou vazio antes e depois.

| # | Arquivo | Mutação | Killed? |
| --- | --- | --- | --- |
| M1 | `ResendEmailSender.java:36` | destinatário ignora o redirect | ✅ |
| M2 | `ResendEmailSender.java:37` | assunto sem prefixo | ✅ |
| M3 | `ResendEmailSender.java:37` | prefixo `[dev -> ` (seta ASCII) | ✅ |
| M4 | `ResendEmailSender.java:37` | prefixo cita o destino em vez do cadastro | ✅ |
| M5 | `ResendEmailSender.java:37` | prefixo também sem redirect | ✅ |
| M6 | `EmailProperties.java:24` | vazio ou só espaços não vira `null` | ✅ |
| M7 | `EmailProperties.java:24` | sem `strip()` | ✅ |
| M8 | `ResendEmailSender.java:41` | `to` com destino e cadastro | ✅ |
| M9 | `ResendEmailSender.java:34` | corpo montado para o destino | ✅ |
| M10 | `ResendEmailSender.java:58` | log antigo no redirect | ✅ |
| M11 | `ResendEmailSender.java:58` | log sem o endereço do cadastro | ✅ |
| M12 | `ResendEmailSender.java:58` | log sem o endereço de destino | ✅ |
| M13 | `application-dev.yaml:18-21` | mapeamento removido do `dev` | ✅ |
| M14 | `application-dev.yaml:18-21` → `application.yaml:69` | mapeamento movido para o `application.yaml` | ✅ |
| M15 | `application-prod.yaml` | mapeamento copiado também no `prod` | ✅ |
| M16 | `backend/src/test/resources/application-test.yaml` | mapeamento `redirect-to` adicionado ao profile `test` | ❌ Survived (o teste não carrega o profile `test`) |

**Sensor depth**: lightweight+ (16 mutantes de comportamento). **Resultado da rodada 1**: 15/16 killed, reprovado (M16).

Fora do sensor, a contraprova de RED-02 AC1 mostra o código real redirecionando fora do `dev`, sem nenhuma mutação.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code / no scope creep | ✅ (um campo em `EmailProperties`, três linhas e um log no `ResendEmailSender`, `LoggingEmailSender` intocado) |
| Surgical changes | ✅ (os outros testes só ganharam o quinto argumento `null` do record) |
| Matches patterns | ✅ (normalização no construtor compacto, como `from` e `verificationBaseUrl`) |
| Spec-anchored outcome check | ✅ RED-01 (destinatário, assunto, HTML e texto por valor exato); ❌ RED-02 AC1 (o teste não reproduz a origem real da variável) |
| Per-layer coverage expectation | ⚠️ sender 1:1 com RED-01; o binding por profile não cobre `test` nem variável de ambiente real |
| No unclaimed tests | ✅ |
| Guidelines followed | `AGENTS.md` (secrets só via env; nenhum teste enfraquecido; JaCoCo passa) |

---

## Gate Check

Rodado no worktree de scratch em `0c50e3a`, antes das mutações.

- **Backend**: `./mvnw verify` exit 0 → 671 tests, 0 failures, 0 errors, 0 skipped; JaCoCo 96,1% linha / 87,7% branch (mínimos 85% / 75%)
- **Test count before feature** (`1c86d83`, registrado na validação da brand-refresh): 661
- **Test count after feature**: 671
- **Delta**: +10 (`ResendEmailSenderTest` +8, `EmailRedirectProfileTest` +2)
- **Skipped / failures**: nenhum

---

## Fix Plans

### Fix 1: o redirecionamento liga fora do `dev` pela variável de ambiente (RED-02 AC1) - Blocker

- **Root cause**: `EmailProperties` é `@ConfigurationProperties(prefix = "reconpay.email")`. O Spring liga `reconpay.email.redirect-to` direto às variáveis `RECONPAY_EMAIL_REDIRECT_TO` (mapeamento legado com `_`) e `RECONPAY_EMAIL_REDIRECTTO`, com qualquer profile. O `application-dev.yaml:21` só fornece mais uma fonte; ele não controla o acesso.
- **Fix task**: fazer o próprio código depender do profile `dev`. Duas opções: (a) ler o redirect fora de `EmailProperties`, num bean `@Profile("dev")` (ou `@ConditionalOnProperty` sobre uma chave que só o `application-dev.yaml` define, com um nome que nenhuma variável de ambiente alcance por relaxed binding); ou (b) ignorar o valor quando `Environment.acceptsProfiles(Profiles.of("dev"))` for falso. Corrigir o comentário em `EmailProperties.java:23` e manter a frase do README verdadeira.
- **Teste**: trocar o `MapPropertySource("env", ...)` de `EmailRedirectProfileTest.java:24` por uma `SystemEnvironmentPropertySource` com o nome `systemEnvironment`, que substitui a fonte padrão. Melhor ainda: subir um contexto real com `SpringApplicationBuilder` ou `ApplicationContextRunner` e a variável nessa fonte. Assertar `redirectTo()` nulo, ou que o envio vai para o cadastro, em `prod`, `test` e sem profile, e o destino em `dev`. Esse teste também mata M16.
- **Priority**: Blocker (o AC é a garantia de que produção nunca redireciona)

### Fix 2: o profile `test` não entra em `EmailRedirectProfileTest` (M16) - Minor

- **Root cause**: `EmailRedirectProfileTest.java:42` itera só `[]` e `["prod"]`, e a spec cita `test` pelo nome.
- **Fix task**: incluir `test` (o `application-test.yaml` está no classpath dos testes). O Fix 1 já resolve isso se o teste novo cobrir os três profiles.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| RED-01 | Implemented | Verified |
| RED-02 | Implemented | Needs Fix |

---

## Summary

**Overall**: ❌ Not Ready

**Spec-anchored check**: 5/6 ACs com o resultado da spec; RED-02 AC1 com gap (contraprova em contexto Spring real)
**Sensor**: 15/16 mutações mortas (M16 sobreviveu)
**Gate**: backend 671 passed, `verify` verde, JaCoCo 96,1% / 87,7%

**O que funciona**: destinatário único, prefixo exato do assunto, HTML e texto do cadastro, normalização de vazio e espaços, log com os dois endereços, mapeamento só no `application-dev.yaml`, README.

**Próximo passo**: Fix 1 (e com ele o Fix 2), depois uma nova rodada do Verifier.

---

## Rodada 2 (fix `1c5bd3c`)

**Verdict**: PASS
**Diff range**: `0c50e3a..1c5bd3c` (`1c5bd3c` fix(auth): honour the email redirect only under the dev profile). O gate também rodou em `6acf9e4`, HEAD da branch durante a rodada. Esse commit mexe só na marca do e-mail e foi verificado em `.specs/features/brand-refresh/validation.md`, revisão 2.

### O que mudou

`ResendEmailSender` recebe o `Environment` e só usa `emailProperties.redirectTo()` quando o profile `dev` está ativo: `backend/src/main/java/br/com/hanrry/reconpay/auth/email/ResendEmailSender.java:35` - `environment.acceptsProfiles(Profiles.of("dev")) ? emailProperties.redirectTo() : null`. O único leitor de `redirectTo()` em `src/main` é essa linha. O comentário de `EmailProperties.java:23` agora diz o que acontece de verdade.

### RED-02 refeito

| Critério | Resultado definido na spec | `file:line` + asserção | Result |
| --- | --- | --- | --- |
| AC1 sem `dev`, com a variável definida: ignora e manda para o cadastro | `to` = cadastro; assunto sem prefixo | `backend/src/test/java/br/com/hanrry/reconpay/auth/email/ResendEmailSenderTest.java:127-133` - `@ValueSource(strings = {"", "test", "prod"})`, redirect ligado em `properties("dev@example.com")`, `expectedTo` = `ana@lojacentro.com.br`, assunto `Confirme seu e-mail — ReconPay` (asserções em `:70-72`, `length()` 1). `dev` junto com outro profile continua redirecionando: `:135-139`. Os testes de RED-01 rodam com `profiles("dev")` (`:61`). YAML por profile: `backend/src/test/java/br/com/hanrry/reconpay/auth/config/EmailRedirectProfileTest.java:45-46` (sem profile, `test`, `prod` → nulo pelo `.env` importado), e o javadoc em `:21-24` explica que a variável de ambiente do SO é barrada no sender. **Contraprova do Verifier refeita** (probe descartável no worktree): `SpringApplicationBuilder` com `EmailProperties`, `ResendEmailSender`, `VerificationEmailComposer`, `EmailVerificationLinkFactory` e um `RestClient.Builder` ligado a `MockRestServiceServer` (nenhuma chamada sai para a rede), rodado com `RECONPAY_EMAIL_REDIRECT_TO=probe@example.com` e uma `RESEND_API_KEY` falsa no ambiente do processo. O `redirectTo()` continua preenchido em todos os profiles (é assim que o Spring liga), mas o POST vai com `to` = `["ana@lojacentro.com.br"]` sem profile, em `test` e em `prod`, e com `["probe@example.com"]` em `dev` | ✅ PASS |
| AC2 README | variável só do `dev`, exemplo genérico, ignorada em `test`/`prod` | `README.md:124`, `README.md:127`. A frase "Em `test` e `prod` ela é ignorada" agora é verdadeira, pela contraprova acima | ✅ PASS |

RED-01 continua com a mesma evidência da rodada 1. Os números de linha de `ResendEmailSenderTest` mudaram: destino `:83-86`, corpo `:89-98`, ausente/vazio/espaços `:100-105`, strip `:107-111`, mesmo endereço `:113-117`, log `:119-125`. O sensor confirma (M1-M15 mortos).

### Discrimination Sensor (rodada 2)

Scratch: `git worktree add --detach /home/hanrry/.claude/jobs/3748ece6/tmp/verifier-wt2 HEAD` (em `1c5bd3c`), `/home/hanrry/.claude/jobs/3748ece6/tmp/sensor2/run2.py`, mesmo comando de teste da rodada 1 (exit 0 antes das mutações). Worktree removido. `git status --porcelain` da árvore real ficou vazio antes e depois.

| # | Arquivo | Mutação | Killed? |
| --- | --- | --- | --- |
| M1-M15 | `ResendEmailSender.java`, `EmailProperties.java`, YAMLs | os mesmos 15 da rodada 1 | ✅ 15/15 |
| M16 | `backend/src/test/resources/application-test.yaml` | `redirect-to` mapeado no profile `test` | ✅ (sobreviveu na rodada 1) |
| G1 | `ResendEmailSender.java:35` | guarda de profile removida | ✅ |
| G2 | `ResendEmailSender.java:35` | `Profiles.of("prod")` | ✅ |
| G3 | `ResendEmailSender.java:35` | guarda negada | ✅ |
| G4 | `ResendEmailSender.java:35` | `Profiles.of("!prod")` | ✅ |
| G5 | `ResendEmailSender.java:35` | `Profiles.of("default \| dev")` | ✅ |
| G6 | `ResendEmailSender.java:35` | `Profiles.of("test \| dev")` | ✅ |
| G7 | `ResendEmailSender.java:35` | exige `dev` como único profile | ✅ |

**Result**: 23/23 killed. PASS.

### Gate (rodada 2)

- `./mvnw verify` em `1c5bd3c`: exit 0, 675 tests, 0 failures, 0 skipped; JaCoCo 96,1% linha / 87,8% branch. Em `6acf9e4`: exit 0, 675 tests, coverage atendida.
- **Delta**: 671 → 675 (+4: `outsideTheDevProfileABoundRedirectIsIgnored` ×3, `devAlongsideOtherProfilesStillRedirects`). Nenhum teste removido ou enfraquecido. `EmailRedirectProfileTest` ganhou o caso `test`.

### Resíduo (não bloqueia)

`EmailProperties.redirectTo()` continua preenchido fora do `dev` quando a variável existe no ambiente. Só o `ResendEmailSender` lê o campo, e ele aplica a guarda. Um leitor novo desse campo teria que repetir a checagem de profile.

### Lições

L-055 e L-056 (rodada 1) continuam candidatas. O fix seguiu as duas, e o skill só penaliza lição confirmada que falhou ao ser aplicada, então não há o que penalizar nem registrar nesta rodada.

### Requirement Traceability Update (rodada 2)

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| RED-01 | Verified | Verified |
| RED-02 | Needs Fix | Verified |
