# Nova Marca Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: inline (abaixo)
**Status**: In Progress

---

## Design (inline)

- **Arquivos** em `frontend/public/brand/`: `reconpay-mark-on-dark.svg` (R `#FFFFFF`, folha `#C6F135`), `reconpay-mark-on-light.svg` (R `#0B0D0A`, folha `#C6F135`), `reconpay-mark-on-dark.png` (56 px de altura, fundo transparente, para o e-mail exibir a 28 px). O símbolo é o redesenho medido sobre o quadro (`viewBox="40 40 600 625"`, dois `path`). `frontend/public/icon.svg` vira o quadrado `#0B0D0A` arredondado com o símbolo para fundo escuro.
- **Fonte**: `@fontsource-variable/manrope` 5.3.0, carregada em `app/layout.tsx` com `next/font/local` (como as outras), variável `--font-manrope`; no `@theme inline` do `globals.css`, `--font-brand`.
- **Componente** `components/brand/Logo.tsx`: `Logo({ tone, tagline = false, size = 'md' })`. `tone: 'dark'` (fundo escuro: símbolo para fundo escuro, "Recon" branco e "Pay" verde) ou `'light'` (fundo claro: símbolo para fundo claro, "ReconPay" preto). O símbolo é `<img alt="" aria-hidden>`; o nome é texto em `font-brand` 800. `size`: `md` (símbolo 30 px, nome 21 px) e `sm` (26 px e 18 px). Com `tagline`, a frase em versalete espaçado embaixo do nome.
- **Uso**: `AuthPanel` → `tone="dark" tagline`; `VerifyEmail`, `TopNav`, menu da landing → `tone="light"`; rodapé da landing → `tone="light" size="sm" tagline`; prévia → `tone="light" size="sm"`. Os links que envolvem a marca ganham o nome "ReconPay" pelo próprio texto.
- **E-mail**: `VerificationEmailComposer` monta a URL da imagem a partir de `EmailProperties.verificationBaseUrl()` sem barras finais (mesma regra do link) e troca o ✓ e "reconpay" por `<img src=… width height=28 alt="">` + "Recon" / "Pay"; o texto puro diz "ReconPay".

---

## Test Coverage Matrix

> Gerada do código, das diretrizes do projeto e da spec; confirmar antes do Execute. Diretrizes: `AGENTS.md` (não enfraquecer testes; JaCoCo 85%/75%), `frontend/vitest.config.ts`, `.github/workflows/ci.yml`. Lições aplicadas: L-018 (cada lugar listado em BRD-02 AC1–AC3 tem a sua asserção de tom e frase), L-054 candidata (medir larguras logo abaixo dos limites: a marca é conferida a 320 px).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Arquivos e fonte (`public/brand`, `icon.svg`, `layout.tsx`, `globals.css`) | none | - (build gate only); conferido no Chromium: arquivos servidos, ícone a 16 px, Manrope aplicada | - | build gate only |
| Componente `Logo` | unit (jsdom) | Cada tom com o símbolo certo e as cores do nome, com e sem frase, nos dois tamanhos; símbolo decorativo | `frontend/components/brand/*.test.tsx` | `cd frontend && corepack pnpm test` |
| Telas (`AuthPanel`, `VerifyEmail`, `TopNav`, `LandingPage`, `ProductPreview`) | unit (jsdom) | BRD-02 AC1–AC4 em cada lugar: tom, frase, nome do link e destino | `frontend/{components,app}/**/*.test.tsx` | `cd frontend && corepack pnpm test` |
| E-mail (`VerificationEmailComposer`) | unit | BRD-03 AC1–AC3: URL da imagem com e sem barra final, `alt=""`, cores do nome, texto puro | `backend/src/test/java/**/auth/email/*Test.java` | `cd backend && ./mvnw -q test -Dtest=VerificationEmailComposerTest -Djacoco.skip=true` |
| Pen | none | - (conferido por captura) | - | - |
| Documentação | none | - (build gate only) | - | build gate only |

## Gate Check Commands

> Gerados do código; confirmar antes do Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick (frontend) | Tasks de componente e telas | `cd frontend && corepack pnpm test && corepack pnpm typecheck` |
| Quick (backend) | Task do e-mail | `cd backend && ./mvnw -q test -Dtest=VerificationEmailComposerTest -Djacoco.skip=true` |
| Full | Fim da task do e-mail | `cd backend && ./mvnw verify` |
| Build | Arquivos, fim das telas e docs | `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build` |

---

## Execution Plan

Phases run sequentially; tasks within a phase run in order.

### Phase 1: Base

```
T1 → T2
```

### Phase 2: Aplicação

```
T2 → T3
T3 → T4
T1 → T5
```

### Phase 3: Desenho e documentação

```
T4 → T6
T5 → T6
T6 → T7
```

---

## Task Breakdown

### T1: Arquivos da marca e fonte Manrope

**What**: Símbolo para fundo escuro e claro em SVG e o PNG para o e-mail em `public/brand/`; novo `public/icon.svg`; Manrope pelo Fontsource com `--font-brand`.
**Where**: `frontend/public/brand/*`, `frontend/public/icon.svg`, `frontend/app/layout.tsx`, `frontend/app/globals.css`, `frontend/package.json`, `frontend/pnpm-lock.yaml`
**Depends on**: None
**Reuses**: padrão `next/font/local` do `layout.tsx`
**Requirement**: BRD-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `/brand/reconpay-mark-on-dark.svg`, `/brand/reconpay-mark-on-light.svg` e `/brand/reconpay-mark-on-dark.png` respondem 200
- [x] O ícone da aba é o quadrado preto com o símbolo, conferido a 16 e 32 px
- [x] `font-brand` aplica Manrope 800, sem rede no build (o Tailwind só gera a classe quando ela é usada; conferido na T2: `getComputedStyle` dá `manrope` e `document.fonts` tem `manrope` carregada)
- [x] Gate build passa

**Tests**: none
**Gate**: build

**Commit**: `feat(frontend): add the new brand mark files and Manrope`

---

### T2: Componente da marca

**What**: `components/brand/Logo.tsx` com `tone`, `tagline` e `size`.
**Where**: `frontend/components/brand/Logo.tsx`, `frontend/components/brand/Logo.test.tsx`
**Depends on**: T1
**Reuses**: arquivos de `public/brand`
**Requirement**: BRD-02

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `tone="dark"`: símbolo para fundo escuro, "Recon" branco e "Pay" verde-limão
- [x] `tone="light"`: símbolo para fundo claro e "ReconPay" todo preto
- [x] Frase só com `tagline`; tamanhos `md` e `sm`
- [x] Símbolo com `alt=""` e fora da leitura de tela; o texto lido é "ReconPay"
- [x] Gate quick (frontend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add the brand logo component`

---

### T3: Marca no login, cadastro e confirmação

**What**: `AuthPanel` usa `Logo tone="dark" tagline`; `VerifyEmail` usa `Logo tone="light"`; testes do login, cadastro, painel e confirmação passam a esperar "ReconPay".
**Where**: `frontend/components/auth/{AuthPanel,VerifyEmail}.tsx` e testes, `frontend/app/{login,register}/page.test.tsx`
**Depends on**: T2
**Reuses**: `Logo`
**Requirement**: BRD-02

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Painel escuro com o tom escuro e a frase, no celular e no desktop
- [x] Confirmação de e-mail com o tom claro, sem a frase
- [x] Gate quick (frontend) passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): use the new brand on the auth screens`

---

### T4: Marca no painel e na landing

**What**: `TopNav`, menu e rodapé da landing e prévia usam `Logo`; links da marca chamados "ReconPay" com o destino de hoje.
**Where**: `frontend/components/shell/TopNav.tsx`, `frontend/components/landing/{LandingPage,ProductPreview}.tsx` e testes
**Depends on**: T3
**Reuses**: `Logo`
**Requirement**: BRD-02

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Barra do painel, menu da landing e prévia com o tom claro e sem frase
- [ ] Rodapé da landing com o tom claro e a frase
- [ ] Links "ReconPay" → `/painel` (barra) e `/` (landing)
- [ ] Sem rolagem horizontal de 320 a 1920 px; capturas a 390 e 1440 px conferidas
- [ ] Gate build passa

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): use the new brand on the dashboard and landing`

---

### T5: Marca no e-mail de verificação

**What**: `VerificationEmailComposer` com a imagem do símbolo vinda do frontend, "Recon" branco e "Pay" verde; texto puro com "ReconPay".
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/auth/email/VerificationEmailComposer.java`, `VerificationEmailComposerTest.java`
**Depends on**: T1
**Reuses**: `EmailProperties.verificationBaseUrl()`, regra de barra final do `EmailVerificationLinkFactory`
**Requirement**: BRD-03

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] HTML com `<img src="http://localhost:3000/brand/reconpay-mark-on-dark.png"` no padrão e sem barra dupla com base terminada em `/`
- [ ] Imagem com `alt=""` e altura 28; "Recon" branco e "Pay" `#C6F135`
- [ ] Sem ✓ nem "reconpay" minúsculo; texto puro com "ReconPay"
- [ ] Gate full passa

**Tests**: unit
**Gate**: full

**Commit**: `feat(auth): show the new brand in the verification email`

---

### T6: Marca nos desenhos

**What**: Trocar a marca nas telas do Pen (landing de desktop e de celular, login, cadastro, confirmação) pelas regras de BRD-02.
**Where**: arquivo do Pen
**Depends on**: T4, T5
**Reuses**: SVG do símbolo
**Requirement**: BRD-04

**Tools**:

- MCP: pencil
- Skill: NONE

**Done when**:

- [ ] Nenhuma tela do Pen mostra o ✓ ou "reconpay" como marca
- [ ] Capturas das telas trocadas conferidas

**Tests**: none
**Gate**: build

**Commit**: `docs(specs): record the brand update in the designs`

---

### T7: Documentação

**What**: `frontend/README.md` com os arquivos da marca, o componente `Logo` e as regras de uso (tom por fundo, frase, "Pay" em verde só no escuro).
**Where**: `frontend/README.md`
**Depends on**: T6
**Reuses**: —
**Requirement**: BRD-01, BRD-02

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Arquivos e regras de uso documentados
- [ ] Gate build passa

**Tests**: none
**Gate**: build

**Commit**: `docs: describe the brand files and usage rules`

---

## Parallel Execution Map

Sequential only.

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | Phase 1 | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | T1 | T1 → T5 | ✅ Match |
| T6 | T4, T5 | T4 → T6, T5 → T6 | ✅ Match |
| T7 | T6 | T6 → T7 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | arquivos e fonte | none | none | ✅ OK |
| T2 | componente `Logo` | unit | unit | ✅ OK |
| T3 | telas | unit | unit | ✅ OK |
| T4 | telas | unit | unit | ✅ OK |
| T5 | e-mail | unit | unit | ✅ OK |
| T6 | Pen | none | none | ✅ OK |
| T7 | documentação | none | none | ✅ OK |
