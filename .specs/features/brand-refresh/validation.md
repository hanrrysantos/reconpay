# Nova Marca Validation

**Verdict**: PASS

**Date**: 2026-10-19
**Spec**: `.specs/features/brand-refresh/spec.md`
**Diff range**: `fcf818b..HEAD` (HEAD = `9d26ff7`; commits `a8c6383` spec, `cdf7cf7` T1, `beffd6d` T2, `50cfaa3` T3, `a5a2d35` T4, `dd95260` T5, `efa6464` T6, `9d26ff7` T7; branch `feat/brand-refresh`)
**Rounds**: 1
**Verifier**: sub-agente independente (autor ≠ verificador)

Todo critério testável tem asserção no valor que a spec define, nos seis lugares de BRD-02 e no e-mail. O sensor matou 31 de 31 mutantes. BRD-02 AC5 e BRD-01 foram medidos no navegador pelo Verifier. BRD-04 (Pen) fica fora do repositório e está conferido só pelas capturas do autor.

---

## Task Completion

| Task | Status | Notes |
| --- | --- | --- |
| T1 | ✅ Done | `public/brand/*`, `icon.svg`, Manrope em `layout.tsx` e `--font-brand` |
| T2 | ✅ Done | `components/brand/Logo.tsx` |
| T3 | ✅ Done | `AuthPanel` (login e cadastro) e `VerifyEmail` |
| T4 | ✅ Done | `TopNav`, menu e rodapé da landing, `ProductPreview` |
| T5 | ✅ Done | `VerificationEmailComposer` e `EmailVerificationLinkFactory` |
| T6 | ✅ Done | Pen (fora do repositório; capturas do autor) |
| T7 | ✅ Done | `frontend/README.md` |

---

## Spec-Anchored Acceptance Criteria

### BRD-01: Arquivos da marca (gate de build e navegador, sem teste unitário por desenho)

| Critério | Resultado definido na spec | Evidência | Result |
| --- | --- | --- | --- |
| AC1 `/brand/` serve o símbolo escuro e claro em SVG e o PNG escuro do e-mail | três arquivos servidos | `frontend/public/brand/reconpay-mark-on-dark.svg:2-3` (R `#FFFFFF`, folha `#C6F135`), `frontend/public/brand/reconpay-mark-on-light.svg:2-3` (R `#0B0D0A`, folha `#C6F135`), `frontend/public/brand/reconpay-mark-on-dark.png` (PNG 54×56 RGBA, 2× os 27×28 do e-mail). `curl` no `next dev` :3100: os três respondem 200 com `image/svg+xml` / `image/png` | ✅ PASS (estrutural + HTTP) |
| AC2 ícone da aba: quadrado preto arredondado, R branco, folha verde | `rect` preto com raio, R `#FFFFFF`, folha `#C6F135` | `frontend/public/icon.svg:2` - `<rect width="64" height="64" rx="14" fill="#0B0D0A"/>`; `:4` R `#FFFFFF`; `:5` folha `#C6F135`; servido com 200 em `/icon.svg` | ✅ PASS (estrutural) |
| AC3 nome em Manrope 800, carregada com as outras fontes, sem rede no build | `next/font/local` com o arquivo do `@fontsource-variable/manrope`; peso 800 aplicado | `frontend/app/layout.tsx:19-24` (`localFont`, `src` em `node_modules/@fontsource-variable/manrope/...woff2`, `--font-manrope`), `:47` (variável no `<html>`); `frontend/app/globals.css:29` - `--font-brand: var(--font-manrope), ...`; `frontend/package.json:17` (`5.3.0`, versão fixa). Classe no componente: `frontend/components/brand/Logo.test.tsx:32` - `toHaveClass('font-brand', 'font-extrabold')`. Navegador (`ac5.mjs`): `getComputedStyle` do nome dá `manrope` e peso `800` em `/`, `/login`, `/register` e `/verificar-email`. `pnpm build` passa sem rede de fontes | ✅ PASS |

### BRD-02: Marca no app

| Critério | Resultado definido na spec | `file:line` + asserção | Result |
| --- | --- | --- | --- |
| AC1 login, cadastro e painel escuro: símbolo escuro, "Recon" branco, "Pay" verde-limão, frase | src `on-dark.svg`; nome `text-background` (`#FFFFFF`); "Pay" `text-primary` (`#C6F135`); frase presente | `frontend/components/auth/AuthPanel.test.tsx:19` - `toHaveAttribute('src', '/brand/reconpay-mark-on-dark.svg')`; `:20` - `toHaveTextContent(/^ReconPay$/)`; `:21` - `toHaveClass('text-background')`; `:22` - `'Pay' toHaveClass('text-primary')`; `:23` - frase `toBeInTheDocument()`; frase visível abaixo de 1024 px `:33` - `not.toHaveClass('hidden')`. Telas: `frontend/app/login/page.test.tsx:36-37` e `frontend/app/register/page.test.tsx:36-37` - nome `/^ReconPay$/` e frase no painel. Componente: `frontend/components/brand/Logo.test.tsx:14-17`. Tokens: `frontend/app/globals.css:4,10` (`#FFFFFF`, `#C6F135`) | ✅ PASS |
| AC2 confirmação de e-mail, barra do painel, menu da landing, prévia: símbolo claro, "ReconPay" preto, sem frase | src `on-light.svg`; nome `text-foreground` (`#0B0D0A`); "Pay" sem `text-primary`; sem frase | VerifyEmail `frontend/components/auth/VerifyEmail.test.tsx:72-76`; TopNav `frontend/components/shell/TopNav.test.tsx:50,52,53,54`; menu `frontend/components/landing/LandingPage.test.tsx:79,81,82,83`; prévia `frontend/components/landing/LandingPage.test.tsx:100-103`. Cada lugar asserta `src` `on-light.svg`, `toHaveClass('text-foreground')`, `not.toHaveClass('text-primary')` e a ausência da frase (L-018: uma asserção por lugar) | ✅ PASS |
| AC3 rodapé da landing: símbolo claro, preto, com frase | src `on-light.svg`; `text-foreground`; frase | `frontend/components/landing/LandingPage.test.tsx:90` - `src` `on-light.svg`; `:91` - `toHaveClass('text-foreground', 'text-[18px]')`; `:92` - `not.toHaveClass('text-primary')`; `:93` - `toHaveTextContent('Conciliação financeira com clareza')` | ✅ PASS |
| AC4 símbolo decorativo, nome como texto; link da marca chamado "ReconPay" com destino `/painel` e `/` | `alt=""`, fora dos papéis `img`; nome acessível `ReconPay`; hrefs exatos | `frontend/components/brand/Logo.test.tsx:66-68` - `alt ''`, `queryByRole('img')` nulo, texto `/^ReconPay$/`; TopNav `frontend/components/shell/TopNav.test.tsx:43` - `getByRole('link', { name: 'ReconPay' })` `href '/painel'`, `:51` `alt ''`; landing menu `frontend/components/landing/LandingPage.test.tsx:16-17` - `['ReconPay', '/']`, `:78,80`; rodapé `:62-63` - `['ReconPay', '/']`, `:89` - `getByRole('link', { name: 'ReconPay' })` (cálculo real do nome acessível, com `aria-label` no rodapé) | ✅ PASS |
| AC5 320 a 1920 px: nome inteiro, sem rolagem horizontal | `scrollWidth == clientWidth`; nome numa linha, dentro da tela | Navegador (Verifier, Chromium headless contra `next dev` :3100, `/home/hanrry/.claude/jobs/3748ece6/tmp/brd/ac5.mjs`): `/` (3 marcas: menu, prévia, rodapé), `/login`, `/register`, `/verificar-email` em 320/360/390/768/1024/1440/1920 → todas `ok` e `nome-inteiro` (sem corte, sem quebra, dentro da largura). `/painel` não medido pelo Verifier (a sessão vive só em memória); a barra usa o mesmo `<Logo tone="light" />` do menu da landing dentro de `Link className="shrink-0"` (`frontend/components/shell/TopNav.tsx:34-35`), e o autor registrou a conferência na T4 | ✅ PASS (navegador; `/painel` por equivalência) |

### BRD-03: Marca no e-mail

| Critério | Resultado definido na spec | `file:line` + asserção | Result |
| --- | --- | --- | --- |
| AC1 faixa escura com a imagem `<base sem barra final>/brand/reconpay-mark-on-dark.png` (28 px) seguida de "Recon" branco e "Pay" verde-limão | URL exata; `height="28"`; `#FFFFFF` e `#C6F135` | `backend/src/test/java/br/com/hanrry/reconpay/auth/email/VerificationEmailComposerTest.java:44` - `contains("<img src=\"https://app.example.com/brand/reconpay-mark-on-dark.png\" width=\"27\" height=\"28\" alt=\"\"")`; `:45` - `contains("color:#FFFFFF;...\">Recon<span style=\"color:#C6F135;\">Pay</span></span>")`; barra final `:55-56`. Fábrica: `backend/src/test/java/br/com/hanrry/reconpay/auth/email/EmailVerificationLinkFactoryTest.java:31-32` (padrão `http://localhost:3000/brand/reconpay-mark-on-dark.png`) e `:40-41` (base `https://app.example.com//` → sem barra dupla) | ✅ PASS |
| AC2 imagem bloqueada: nome escrito continua, imagem com `alt=""` | `alt=""`; "Recon"+"Pay" como texto | `VerificationEmailComposerTest.java:44` - `alt=\"\"`; `:45` - nome em texto no HTML; `:46-47` - sem `✓` nem `>reconpay<` | ✅ PASS |
| AC3 texto puro escreve "ReconPay" | "ReconPay" e nada em minúsculas | `VerificationEmailComposerTest.java:80` - `contains("ReconPay · Conciliação financeira")`; `:81` - `doesNotContain("reconpay")` | ✅ PASS |

### BRD-04: Desenhos (fora do repositório)

| Critério | Resultado definido na spec | Evidência | Result |
| --- | --- | --- | --- |
| AC1 o Pen usa a marca nova com as regras de BRD-02 | landing desktop/celular, login, cadastro e confirmação | Só as capturas do autor (`tasks.md` T6: 16 marcas em 13 telas, sem ✓ nem "reconpay"). O arquivo `.pen` não está no repositório e o Verifier não o abriu | ✅ PASS (capturas do autor) |

**Status**: ✅ Todos os ACs cobertos. Sem spec-precision gaps.

---

## Edge Cases

- [x] Manrope ainda não carregada: `display: 'swap'` em `frontend/app/layout.tsx:23`, e `--font-brand` cai para `"Manrope", ui-sans-serif, system-ui, sans-serif` (`frontend/app/globals.css:29`). O texto nunca fica oculto.
- [x] Base do e-mail com barras finais: coberto em `EmailVerificationLinkFactoryTest.java:40-41` e `VerificationEmailComposerTest.java:55-56`.
- [x] Sobras da marca antiga: nenhum `reconpay` minúsculo de marca nos componentes. As ocorrências restantes são nomes de arquivo CSV (`DashboardPage.tsx:169`, `PeriodRow.tsx:52`, `HowItWorks.tsx:61`), fora do escopo.

---

## Discrimination Sensor

Scratch isolado: `git worktree add --detach /home/hanrry/.claude/jobs/3748ece6/tmp/verifier-wt HEAD`, `pnpm install --frozen-lockfile --offline`. Cada mutante foi aplicado, testado e revertido por `/home/hanrry/.claude/jobs/3748ece6/tmp/sensor/run.py`. Frontend: `vitest run components app`. Backend: `./mvnw -q test -Dtest=VerificationEmailComposerTest,EmailVerificationLinkFactoryTest -Djacoco.skip=true`. O worktree foi removido. `git status --porcelain` da árvore real ficou vazio antes e depois (idêntico).

| # | Arquivo | Mutação | Killed? |
| --- | --- | --- | --- |
| F1 | `frontend/components/brand/Logo.tsx:19` | cores do nome trocadas entre os tons | ✅ |
| F2 | `frontend/components/brand/Logo.tsx:20` | "Pay" verde-limão também em fundo claro | ✅ |
| F3 | `frontend/components/brand/Logo.tsx:22` | frase nunca aparece | ✅ |
| F4 | `frontend/components/brand/Logo.tsx:17` | símbolo trocado entre os tons | ✅ |
| F5 | `frontend/components/brand/Logo.tsx:17` | `alt=""` → `alt="ReconPay"` | ✅ |
| F6 | `frontend/components/brand/Logo.tsx:19` | peso 800 → 700 | ✅ |
| F7 | `frontend/components/brand/Logo.tsx:24` | frase "com transparência" | ✅ |
| F8 | `frontend/components/brand/Logo.tsx:20` | nome vira "Recon Pay" | ✅ |
| F9 | `frontend/components/landing/LandingPage.tsx:22` | `aria-label` do rodapé "ReconPay início" | ✅ |
| F10 | `frontend/components/landing/LandingPage.tsx:23` | rodapé sem a frase | ✅ |
| F11 | `frontend/components/landing/LandingPage.tsx:23` | menu da landing com a frase | ✅ |
| F12 | `frontend/components/landing/LandingPage.tsx:23` | landing com `tone="dark"` | ✅ |
| F13 | `frontend/components/landing/LandingPage.tsx:22` | destino da marca `/inicio` | ✅ |
| F14 | `frontend/components/auth/AuthPanel.tsx:12` | `tone="light"` | ✅ |
| F15 | `frontend/components/auth/AuthPanel.tsx:12` | sem a frase | ✅ |
| F16 | `frontend/components/auth/VerifyEmail.tsx:41` | `tone="dark"` | ✅ |
| F17 | `frontend/components/auth/VerifyEmail.tsx:41` | com a frase | ✅ |
| F18 | `frontend/components/shell/TopNav.tsx:35` | `tone="dark"` | ✅ |
| F19 | `frontend/components/shell/TopNav.tsx:35` | com a frase | ✅ |
| F20 | `frontend/components/shell/TopNav.tsx:34` | destino da marca `/` | ✅ |
| F21 | `frontend/components/landing/ProductPreview.tsx:35` | `tone="dark"` | ✅ |
| F22 | `frontend/components/landing/ProductPreview.tsx:35` | com a frase | ✅ |
| B1 | `backend/.../auth/email/EmailVerificationLinkFactory.java` (`BRAND_MARK_PATH`) | caminho `on-light.png` | ✅ |
| B2 | `EmailVerificationLinkFactory.java` (`buildBrandMarkUrl`) | base sem remover a barra final | ✅ |
| B3 | `EmailVerificationLinkFactory.java` (`baseUrl`) | regex `/+$` → `/$` (só uma barra) | ✅ |
| B4 | `backend/.../auth/email/VerificationEmailComposer.java:41` | "Pay" `#FFFFFF` | ✅ |
| B5 | `VerificationEmailComposer.java:40` | `alt="ReconPay"` | ✅ |
| B6 | `VerificationEmailComposer.java:41` | "Recon" `#0B0D0A` | ✅ |
| B7 | `VerificationEmailComposer.java:75` | texto puro "reconpay · ..." | ✅ |
| B8 | `VerificationEmailComposer.java:40` | imagem 32×32 | ✅ |
| B9 | `VerificationEmailComposer.java:40` | `src` aponta para o link de verificação | ✅ |

**Sensor depth**: lightweight+ (31 mutantes de comportamento; cada tom e frase de cada um dos seis lugares mutado em separado). **Result**: 31/31 killed. PASS.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code / no scope creep | ✅ (um componente `Logo` com dois tons e dois tamanhos; o e-mail ganhou um método na fábrica) |
| Surgical changes | ✅ (cada lugar troca o bloco antigo por `<Logo>`; os imports `Check` não usados saíram) |
| Matches patterns | ✅ (fonte via `next/font/local` + `@fontsource`, como Inter e Plex Mono; URL do e-mail pela mesma base do link) |
| Spec-anchored outcome check | ✅ (cores por token com valor fixo em `globals.css`; URL, `alt` e cores do e-mail por string exata) |
| Per-layer coverage expectation | ✅ `Logo` 1:1; telas com tom, frase, nome do link e destino em cada lugar; e-mail AC1–AC3. Arquivos e fonte: build + navegador, como manda a matriz |
| No unclaimed tests | ✅ (os testes de tamanho `md`/`sm` cobrem o critério "nos dois tamanhos" da matriz) |
| Guidelines followed | `AGENTS.md` (nenhum teste enfraquecido: as asserções `'reconpay'` antigas viraram asserções mais estritas sobre `ReconPay`, tom e frase; JaCoCo passa), `frontend/vitest.config.ts` |

---

## Gate Check

Rodado no worktree de scratch em `9d26ff7`, antes das mutações.

- **Frontend**: `corepack pnpm test` → 968 passed, 0 failed, 0 skipped (76 files); `corepack pnpm typecheck` exit 0; `corepack pnpm build` exit 0
- **Backend**: `./mvnw verify` exit 0 → 661 tests, 0 failures, 0 errors, 0 skipped; JaCoCo 96,2% linha / 87,7% branch (mínimos 85% / 75%)
- **Test count before feature** (`fcf818b`, pelo diff): frontend 953, backend 657
- **Test count after feature**: frontend 968, backend 661
- **Delta**: +15 frontend (`Logo` 9, `AuthPanel` 1, `VerifyEmail` 1, `LandingPage` 3, `TopNav` 1), +4 backend
- **Skipped / failures**: nenhum

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| BRD-01 | Implemented | Verified |
| BRD-02 | Implemented | Verified |
| BRD-03 | Implemented | Verified |
| BRD-04 | Implemented | Verified (capturas do autor) |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 12/12 ACs com o resultado da spec (BRD-01 por arquivo, HTTP e navegador; BRD-04 pelas capturas do autor)
**Sensor**: 31/31 mutações mortas
**Gate**: frontend 968 passed, backend 661 passed, build e `verify` verdes

**Resíduos (não bloqueiam)**:
1. BRD-04 depende só das capturas do autor; o `.pen` fica fora do repositório.
2. BRD-02 AC5 em `/painel` não foi medido pelo Verifier no navegador. Vale por equivalência com o menu da landing, medido de 320 a 1920 px.
