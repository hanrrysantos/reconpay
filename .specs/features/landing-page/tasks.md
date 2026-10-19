# Landing Page Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: inline (abaixo)
**Status**: In Progress

---

## Design (inline)

- **Rotas**: `app/(app)/page.tsx` vira `app/(app)/painel/page.tsx`. `app/page.tsx` é um componente de servidor que renderiza `components/landing/LandingPage.tsx`; a metadata da landing fica nele.
- **Componentes** em `components/landing/`: `LandingPage` (ordem das seções, `nav`, `main` e `footer`), `ProductPreview` (prévia estática, `role="img"`), uma seção por arquivo (`Problem`, `HowItWorks`, `Discrepancies`, `FinalCta`) e `CountUp` (único componente de cliente).
- **Progresso do scroll**: `lib/scrollProgress.ts` tem funções puras. `coverProgress(top, height, viewport)` dá a fração do intervalo `cover` (0 com o topo do bloco na base da tela, 1 com a base do bloco no topo). `rangeProgress(cover, start, end)` recorta e limita a 0–1. `CHART_RANGE = { start: 0.05, end: 0.45 }` é o mesmo intervalo do CSS (`animation-range: cover 5% cover 45%`), então anel, barra e números andam juntos.
- **Formatação**: `formatCount(value, { decimals, prefix, suffix })` usa `Intl.NumberFormat('pt-BR')` com casas fixas.
- **`CountUp`**: renderiza o valor final no servidor. No navegador, só anima se `CSS.supports('animation-timeline: view()')` e sem `prefers-reduced-motion: reduce`. Escuta `scroll` e `resize` com `requestAnimationFrame`, lê o retângulo do bloco do gráfico (o ancestral `[data-chart]`) e escreve `formatCount(valor × progresso)`. O texto animado é `aria-hidden`; o valor final fica em `sr-only`.
- **CSS** no `globals.css`, tudo dentro de `@supports (animation-timeline: view())` e `@media (prefers-reduced-motion: no-preference)`. Fora disso, nenhuma regra esconde nada.
  - `.reveal`: `opacity 0→1` e `translateY(32px)→0`, com `animation-timeline: view()`, `animation-range: entry 0% cover 30%` e `animation-fill-mode: both`. A posição do scroll define o estado, então descer e subir são simétricos.
  - Escalonamento: `.reveal-stagger > *` desloca o intervalo de cada filho por `--i` (início `entry calc(var(--i) * 8%)`).
  - `.rise`: a prévia sobe de `translateY(80px) scale(.96)` ao lugar no intervalo `cover 0% cover 35%`.
  - `.ring-fill`: `stroke-dashoffset` do arco do circunferência cheia até o valor, com `view-timeline` no bloco `[data-chart]` e `animation-range: cover 5% cover 45%`.
  - `.bar-fill`: `transform: scaleX(0→1)` com `transform-origin: left`, no mesmo intervalo.
  - `.hero-in`: entrada por tempo (`animation-delay` por `--i`), sem timeline de scroll.
- **Anel**: SVG com `circle` de trilho e `circle` de arco (`stroke-dasharray` = circunferência, `stroke-dashoffset` final = circunferência × (1 − 0,942)), girado −90°.

---

## Test Coverage Matrix

> Gerada do código, das diretrizes do projeto e da spec; confirmar antes do Execute. Diretrizes encontradas: `AGENTS.md` (não enfraquecer testes), `frontend/vitest.config.ts` (`*.test.ts` em node, `*.test.tsx` em jsdom), `.github/workflows/ci.yml`. Lições aplicadas: L-018 (cada valor listado em LAND-04 AC6 tem teste do formato final; cada link de LAND-02 AC2–AC4 é conferido), L-036 (não se aplica: não há reset de estado).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Frontend lib (`lib/scrollProgress.ts`) | unit (node) | `coverProgress` nas fronteiras (topo na base, base no topo, meio), `rangeProgress` antes, dentro e depois do intervalo, `formatCount` de cada valor de LAND-04 AC6 e do zero | `frontend/lib/*.test.ts` | `cd frontend && corepack pnpm test` |
| Frontend componentes (`components/landing`) | unit (jsdom) | 1:1 com LAND-02 AC1–AC4, LAND-05 AC1; `CountUp` com e sem suporte, com movimento reduzido, progresso 0, meio e 1, ao rolar para baixo e de volta | `frontend/components/landing/*.test.tsx` | `cd frontend && corepack pnpm test` |
| Frontend rotas (`app/page.tsx`, `app/(app)/painel`, `app/login`, `components/shell/TopNav`) | unit (jsdom) | LAND-01 AC1–AC3: landing em `/` sem chamar a API, painel em `/painel`, login e marca levando a `/painel` | `frontend/{app/**,components/shell}/*.test.tsx` | `cd frontend && corepack pnpm test` |
| CSS das animações e responsividade | none | - (build gate only); conferido no Chromium sem interface: opacidade e deslocamento medidos descendo e subindo, sem rolagem horizontal de 320 a 1920 px, capturas a 390 e 1440 px | - | build gate only |
| Documentação | none | - (build gate only) | - | build gate only |

## Gate Check Commands

> Gerados do código; confirmar antes do Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Cada task | `cd frontend && corepack pnpm test && corepack pnpm typecheck` |
| Build | Tasks de rota, CSS e docs, e fim da feature | `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build` |

---

## Execution Plan

Phases run sequentially; tasks within a phase run in order.

### Phase 1: Base

```
T1
T2
```

### Phase 2: Página

```
T1 → T3
T3 → T4
```

### Phase 3: Movimento

```
T2 → T5
T4 → T5
T5 → T6
```

### Phase 4: Documentação

```
T6 → T7
```

---

## Task Breakdown

### T1: Painel em `/painel`

**What**: Mover `app/(app)/page.tsx` para `app/(app)/painel/page.tsx`; o login (sucesso e sessão existente) leva a `/painel`; "Visão geral" e a marca da `TopNav` levam a `/painel`.
**Where**: `frontend/app/(app)/painel/page.tsx`, `frontend/app/login/page.tsx`, `frontend/components/shell/TopNav.tsx` e seus testes
**Depends on**: None
**Reuses**: `DashboardPage`, `AppShell`
**Requirement**: LAND-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Login com sucesso e com sessão existente chamam `router.replace('/painel')`
- [x] "Visão geral" e a marca apontam para `/painel` e "Visão geral" fica ativa em `/painel`
- [x] Gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `refactor(frontend): move the dashboard to /painel`

---

### T2: Progresso do scroll e formato dos números

**What**: Criar `lib/scrollProgress.ts` com `coverProgress`, `rangeProgress`, `CHART_RANGE` e `formatCount`.
**Where**: `frontend/lib/scrollProgress.ts`, `frontend/lib/scrollProgress.test.ts`
**Depends on**: None
**Reuses**: `Intl.NumberFormat('pt-BR')`
**Requirement**: LAND-04

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `coverProgress` dá 0 com o topo na base da tela, 1 com a base no topo e 0,5 no meio, limitado a 0–1 fora disso
- [ ] `rangeProgress` dá 0 antes de `start`, 1 depois de `end` e a fração no meio
- [ ] `formatCount` dá `94,2%`, `1.209`, `R$ 10.389,38`, `R$ 121.212,50`, `R$ 3.787,50`, `964`, `912`, `52`, `1.284`, `1.271`, `312` no valor final e `0,0%` / `R$ 0,00` / `0` no zero
- [ ] Gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add scroll progress and count formatting helpers`

---

### T3: Landing em `/` com menu, abertura, prévia e rodapé

**What**: `app/page.tsx` com a metadata e `LandingPage`; menu (marca, âncoras, "Entrar", "Criar conta"), abertura (selo, `h1`, texto, botões), `ProductPreview` estática (`id="fechamento"`, `role="img"`), rodapé. Desktop e celular conforme o Pen.
**Where**: `frontend/app/page.tsx`, `frontend/app/page.test.tsx`, `frontend/components/landing/{LandingPage,ProductPreview}.tsx` e testes
**Depends on**: T1
**Reuses**: tokens do `globals.css`, ícones `lucide-react`
**Requirement**: LAND-01, LAND-02, LAND-03, LAND-05

**Tools**:

- MCP: pencil (conferir o Pen)
- Skill: NONE

**Done when**:

- [ ] `/` renderiza a landing sem chamar a API e sem `AppShell`
- [ ] Textos do menu, da abertura, da prévia e do rodapé iguais ao Pen, com as trocas de "Ver demonstração" da spec
- [ ] Links: "Criar conta" → `/register`; "Entrar" e "Entrar na minha conta" → `/login`; âncoras `#como-funciona`, `#divergencias`, `#fechamento`; GitHub e Documentação da API nos endereços da spec
- [ ] Um `h1`; regiões `nav`, `main`, `footer`; prévia com `role="img"` e o nome da spec
- [ ] Metadata com o título e a descrição da spec
- [ ] Capturas a 390 e 1440 px conferidas com o Pen
- [ ] Gate build passa

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): add the landing page with hero, preview and footer`

---

### T4: Seções do problema, passos, divergências e convite final

**What**: `Problem` (quatro distorções), `HowItWorks` (`id="como-funciona"`, três passos com as ilustrações), `Discrepancies` (`id="divergencias"`, três decisões e o ajuste de exemplo), `FinalCta`; cada uma com `h2`, desktop e celular conforme o Pen.
**Where**: `frontend/components/landing/{Problem,HowItWorks,Discrepancies,FinalCta}.tsx` e testes de `LandingPage`
**Depends on**: T3
**Reuses**: `LandingPage`
**Requirement**: LAND-02, LAND-03, LAND-05

**Tools**:

- MCP: pencil (conferir o Pen)
- Skill: NONE

**Done when**:

- [ ] As seções aparecem na ordem de LAND-02 AC1, com os textos do Pen e um `h2` cada
- [ ] Convite final com "Crie sua conta e concilie o primeiro mês.", "Criar conta" → `/register` e "Entrar na minha conta" → `/login`
- [ ] Sem rolagem horizontal de 320 a 1920 px; capturas a 390 e 1440 px conferidas com o Pen
- [ ] Gate build passa

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): add the landing problem, steps, discrepancies and CTA`

---

### T5: Números que crescem com o scroll

**What**: `CountUp` (cliente) e uso nos números do anel, dos indicadores, da barra do passo 2 e das linhas do passo 1; blocos do anel e da barra marcados com `data-chart`.
**Where**: `frontend/components/landing/CountUp.tsx`, `CountUp.test.tsx`, `ProductPreview.tsx`, `HowItWorks.tsx`
**Depends on**: T2, T4
**Reuses**: `coverProgress`, `rangeProgress`, `CHART_RANGE`, `formatCount`
**Requirement**: LAND-04

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Sem suporte a animação ligada ao scroll, com movimento reduzido ou antes de montar: valor final
- [ ] Com suporte: valor de zero, do meio e final conforme a posição do bloco; ao rolar de volta, o valor volta
- [ ] Texto animado `aria-hidden` e valor final em `sr-only`
- [ ] Gate quick passa

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): grow landing numbers with scroll progress`

---

### T6: Animações ligadas ao scroll

**What**: CSS `.reveal`, `.reveal-stagger`, `.rise`, `.ring-fill`, `.bar-fill`, `.hero-in` e aplicação nas seções, com as guardas de suporte e de movimento reduzido.
**Where**: `frontend/app/globals.css`, `frontend/components/landing/*.tsx`
**Depends on**: T5
**Reuses**: `CHART_RANGE` (mesmo intervalo no CSS)
**Requirement**: LAND-04

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] No Chromium sem interface: cada bloco animado sai de opacidade 0 e vai a 1 descendo, e volta a 0 subindo; anel e barra enchem e esvaziam junto com os números
- [ ] Com `prefers-reduced-motion: reduce` emulado e sem suporte: tudo opaco, no lugar, anel e barra cheios
- [ ] Abrir com `#divergencias` mostra a seção no estado certo
- [ ] Gate build passa

**Tests**: none
**Gate**: build

**Commit**: `feat(frontend): animate the landing sections with scroll timelines`

---

### T7: Documentação

**What**: `frontend/README.md` com as rotas `/` (landing) e `/painel`, e como as animações se comportam sem suporte e com movimento reduzido; README raiz menciona a landing.
**Where**: `frontend/README.md`, `README.md`
**Depends on**: T6
**Reuses**: —
**Requirement**: LAND-01, LAND-04

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Rotas e comportamento das animações documentados
- [ ] Gate build passa

**Tests**: none
**Gate**: build

**Commit**: `docs: describe the landing page and the /painel route`

---

## Parallel Execution Map

Sequential only.

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | Phase 1 | ✅ Match |
| T2 | None | Phase 1 | ✅ Match |
| T3 | T1 | T1 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | T2, T4 | T2 → T5, T4 → T5 | ✅ Match |
| T6 | T5 | T5 → T6 | ✅ Match |
| T7 | T6 | T6 → T7 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | frontend rotas | unit | unit | ✅ OK |
| T2 | frontend lib | unit | unit | ✅ OK |
| T3 | frontend rotas + componentes | unit | unit | ✅ OK |
| T4 | frontend componentes | unit | unit | ✅ OK |
| T5 | frontend componentes | unit | unit | ✅ OK |
| T6 | CSS das animações | none | none | ✅ OK |
| T7 | documentação | none | none | ✅ OK |
