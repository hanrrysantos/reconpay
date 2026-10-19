# Landing Page Validation

**Verdict**: PASS

**Date**: 2026-10-19
**Spec**: `.specs/features/landing-page/spec.md`
**Diff range**: `b7ed449..ce7991d` (a8dab6c … 9fd9a96, plus fix ce7991d; branch `feat/landing-page`)
**Rounds**: 2 (round 1 FAIL on LAND-03 AC4; fix `ce7991d`; round 2 PASS)
**Verifier**: independent sub-agent (author ≠ verifier)

Round 2: fix `ce7991d` closes the only round-1 gap (**LAND-03 AC4**), which I re-measured in the browser (see Round 2 below). Every AC now has evidence, and the sensor killed all 28 code mutants plus 3 in-page CSS mutants.

---

## Task Completion

| Task | Status | Notes |
| --- | --- | --- |
| T1 | ✅ Done | `/painel` route, login and TopNav |
| T2 | ✅ Done | `lib/scrollProgress.ts` |
| T3 | ✅ Done | landing, preview, footer |
| T4 | ✅ Done | sections |
| T5 | ✅ Done | `CountUp` |
| T6 | ✅ Done | CSS; Verifier re-measured the animations (see LAND-04) |
| T7 | ✅ Done | docs |

---

## Spec-Anchored Acceptance Criteria

### LAND-01: Landing at the root

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 `/` shows the landing, with or without a session, without calling the API | landing h1 shown; `fetch` not called | `frontend/app/page.test.tsx:14` - `getByRole('heading',{level:1})).toHaveTextContent('O que você vendeu, …')`; `frontend/app/page.test.tsx:16` - `expect(fetch).not.toHaveBeenCalled()`; `app/page.tsx` sits outside the `(app)` group, so it has no `AppShell` guard | ✅ PASS |
| AC2 `/painel` shows the dashboard with a session; without one it goes to login | dashboard at `/painel`; `/login` without a token | `frontend/app/(app)/painel/page.tsx:6` - `return <DashboardPage />` (a 100% rename of the old `app/(app)/page.tsx`); the build route table lists `○ /painel`; guard: `frontend/components/shell/AppShell.test.tsx:26` - `expect(replace).toHaveBeenCalledWith('/login')` (`(app)/layout.tsx` wraps every route in `AppShell`). Structural evidence only: no test renders the route file itself | ✅ PASS (structural) |
| AC3 login success or an existing session goes to `/painel`; brand and "Visão geral" go to `/painel` | `router.replace('/painel')`; `href="/painel"` | `frontend/app/login/page.test.tsx:105` - `expect(navigation.replace).toHaveBeenCalledWith('/painel')`; `frontend/app/login/page.test.tsx:172` (session) - same; `frontend/components/shell/TopNav.test.tsx:43` - brand `toHaveAttribute('href','/painel')`; `frontend/components/shell/TopNav.test.tsx:12,27` - `['Visão geral','/painel']`; active state `frontend/components/shell/TopNav.test.tsx:35` | ✅ PASS |

### LAND-02: Design content

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 texts in design order | menu → hero + preview → problem (4 distortions) → steps → decisions + example → CTA → footer | `frontend/components/landing/LandingPage.test.tsx:91-96` - h2 list `toEqual([...4 titles in order])`; `:99` - preview precedes the problem section (`compareDocumentPosition`); `:109-117` - the four distortions with their text; `:127` - three steps; `:151-155` - three decisions; `:158-160` - example adjustment texts; `:168` - CTA text; `:73` - footer credit | ✅ PASS |
| AC2 "Criar conta" goes to `/register`; "Entrar" and "Entrar na minha conta" go to `/login` | exact hrefs | menu `frontend/components/landing/LandingPage.test.tsx:15-22` (`['Entrar','/login'],['Criar conta','/register']`); hero `:35-36`; CTA `:169-170`; footer `:67` (`['Entrar','/login']`) | ✅ PASS |
| AC3 anchors scroll to steps, discrepancies and preview | `#como-funciona`, `#divergencias`, `#fechamento` linked to matching ids | hrefs `frontend/components/landing/LandingPage.test.tsx:17-19`; ids `:124` (`id','como-funciona'`), `:148` (`'divergencias'`), `:44` (`'fechamento'`) | ✅ PASS |
| AC4 GitHub and API docs | `https://github.com/hanrrysantos/reconpay`, `https://reconpay.hanrry.top/swagger-ui/index.html` | `frontend/components/landing/LandingPage.test.tsx:63-68` - footer links `toEqual([... ['GitHub', GITHUB], ['Documentação da API', API_DOCS] ...])` with constants at `:5-6` | ✅ PASS |

### LAND-03: Mobile (CSS only; evidence is browser measurement)

Measured by the Verifier in headless Chromium 148 against `next dev` on port 3100. Scripts are in `/home/hanrry/.claude/jobs/3748ece6/tmp/verifier/` (`vis.mjs`, `overflow.mjs`, `overflow2.mjs`, `wrap.mjs`, `nav379.mjs`, `rows375.png`).

| Criterion | Spec-defined outcome | Evidence (browser) | Result |
| --- | --- | --- | --- |
| AC1 < 768 px | menu shows only the brand, "Entrar" and "Criar conta"; hero buttons stacked; one column; preview without tabs, KPIs or "Resolver" | `node vis.mjs`, 320–767: `anchors:false, tabs:false, kpis:false, resolver:false, chartDir:"column", heroBtns:"column"`. Classes: `frontend/components/landing/LandingPage.tsx:35` (`hidden … lg:flex`), `:60` (`flex-col … md:flex-row`), `frontend/components/landing/ProductPreview.tsx:41,88,107` | ✅ PASS |
| AC2 320–1920 px: no horizontal scroll, no clipped text | `scrollWidth == clientWidth`, no element past the right edge | `node overflow.mjs` for 320/360/390/414/600/767/768/900/1024/1280/1440/1920 and `overflow2.mjs` for 340/370/375/379: all `ok`, no wide elements | ✅ PASS |
| AC3 ≥ 768 px: content capped at 1440 px; anchors, tabs and ring beside the table from 1024; KPIs from 1280; "Resolver" from 1400 | breakpoints as listed | `vis.mjs`: at 1023 `anchors:false, tabs:false, chartDir:column`; at 1024 `true, true, row`; at 1279 `kpis:false`, 1280 `true`; at 1399 `resolver:false`, 1400 `true`; at 1920 `contentW:1440` | ✅ PASS |
| AC4 < 380 px: no "Entrar" in the menu; each divergence type on a second line | below 380 px, every row's type sits under the ref and amount, and the menu has no "Entrar" | ✅ **Round 2 PASS** (see Round 2 re-verification). Round 1 (superseded) failed: (a) `node nav379.mjs` → `379 ["reconpay@137","Entrar@242","Criar conta@363"]`: at 379 px "Entrar" is still visible. `max-[379px]` in Tailwind 4 compiles to `@media not all and (min-width:379px)`, i.e. `< 379px`, not `< 380px` (`frontend/components/landing/LandingPage.tsx:41`, `frontend/components/landing/ProductPreview.tsx:103,105`). (b) `node wrap.mjs`: at 375 px, PED-10021, PED-10022 and PED-10047 have the type on the same line as the ref (top 1097/1150/1204 vs ref 1101/1154/1207), placed after the amount; only PED-10063 wraps. At 379 px no row wraps. Screenshot `rows375.png` shows the mixed layout. `order-last` + `flex-wrap` only wraps when the row overflows; nothing forces a second line. No test covers it (CSS, matrix "none"; T6 evidence covers only 390/1440) | ✅ PASS (browser, round 2) |

### LAND-04: Scroll-linked animations

AC1–AC5 and the CSS half of AC7/AC8 rely on browser measurement by design. The Verifier re-ran the sweep: `node anim.mjs 1440 900 9340` → "viewport 1440x900, 32 posições, 18 blocos animados … blocos assimétricos: 0 … movimento reduzido: fora do estado final = 0". Every `.reveal` / `.reveal-stagger > *` block went from opacity 0→1 and y 32→0, `rise` from y 46.6→0 and scale 0.977→1, `ring-fill` dash from 428.8→24.9, and `bar-fill` scaleX from 0→1. All were symmetric between scrolling down and scrolling back up. This matches the T6 evidence in `tasks.md` (which also covers 390×844), so that evidence is accepted.

| Criterion | Spec-defined outcome | Evidence | Result |
| --- | --- | --- | --- |
| AC1 blocks go from transparent and shifted down to final as the scroll progresses; list items stagger | opacity 0→1, y 32→0, staggered | sweep above (18 blocks); stagger range `frontend/app/globals.css` `.reveal-stagger > *` `animation-range: entry calc(var(--i)*8%) cover calc(30% + var(--i)*8%)`; `#divergencias` probe (`node hash.mjs`) shows the 3rd decision at opacity 0.85 while the 1st and 2nd are at 1.00 | ✅ PASS (browser) |
| AC2 scrolling back undoes the entry | same values going up | sweep: `blocos assimétricos: 0` | ✅ PASS (browser) |
| AC3 no scroll-timeline support: everything static and visible | no hiding rules outside `@supports` | Static review: every scroll-linked rule is inside `@supports (animation-timeline: view())` in `frontend/app/globals.css`. Outside it only `.hero-in` (time-based) and the `@keyframes` exist. Chromium cannot emulate missing support | ✅ PASS (static) |
| AC4 reduced motion: everything static, opaque, in place | 0 blocks off their final state | sweep with `prefers-reduced-motion: reduce` emulated: `fora do estado final = 0` | ✅ PASS (browser) |
| AC5 ring and bar build from empty to 94.2% and 964/1.284 with scroll, and undo on the way back | dashoffset from full length to length×(1−0.942); scaleX 0→1 | sweep: `ring-fill dash 428.8→24.9` (2π·68.25 = 428.8; ×0.058 = 24.9), `bar-fill scaleX 0→1`, symmetric; bar width `frontend/components/landing/HowItWorks.tsx:12,36` (964/1284) | ✅ PASS (browser) |
| AC6 numbers grow with the same progress, in pt-BR format; screen readers hear only the final value | each listed value; `sr-only` final | format of every listed value: `frontend/lib/scrollProgress.test.ts:51` - `expect(formatCount(value, options)).toBe(expected)` over `94,2%`, `1.209`, `R$ 10.389,38`, `R$ 121.212,50`, `R$ 3.787,50`, `964`, `912`, `52`, `1.284`, `1.271`, `312` (L-018 satisfied); zero `:55-57`; progress: `frontend/components/landing/CountUp.test.tsx:67,69,71,73,75` (`R$ 0,00` → `R$ 60.606,25` → `R$ 121.212,50` → back → `R$ 0,00`); range parity with CSS `frontend/lib/scrollProgress.test.ts:33` - `toEqual({ start: 0.05, end: 0.45 })`; screen reader `frontend/components/landing/CountUp.test.tsx:94` - `.sr-only toHaveTextContent(/^1\.209$/)` while the visible value is `605`; texts in the page `frontend/components/landing/LandingPage.test.tsx:49-50,131-137`. Browser: `node counts.mjs` shows all 11 numbers going from 0 to their final values and back in step with the charts (e.g. y=150 → `19,5%`, `R$ 25.030,43`; y=600 → final) | ✅ PASS |
| AC7 no support, reduced motion or no JS: charts full, numbers final | final values | `frontend/components/landing/CountUp.test.tsx:48,50` (no support) and `:57,59` (reduced motion) - `toBe('94,2%')` even after scrolling; no JS `:101-102` - `renderToString` contains the final values; the prerendered `.next/server/app/index.html` contains `R$ 121.212,50`, `94,2%`, `1.209`; the static ring offset is the final value (`frontend/components/landing/ProductPreview.tsx:76`) | ✅ PASS |
| AC8 hero animates once on load (time-based); preview rises with scroll and undoes on the way back | `.hero-in` time-based; `.rise` scroll-linked | `.hero-in > *` uses `animation: landing-reveal 700ms … both` with `--i` delays and no timeline (`frontend/app/globals.css`); `.rise` symmetric in the sweep (y 46.6→0) | ✅ PASS (browser/static) |

### LAND-05: Accessibility and search

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1 one h1, one h2 per section, nav/main/footer, preview as img with its name | exactly so | `frontend/components/landing/LandingPage.test.tsx:30` - `toHaveLength(1)` h1; `:91-96,100` - 4 h2s inside `main`; `:79-81` - main, navigation "Principal", contentinfo; `:43` - `getByRole('img',{name:'Prévia do painel de fechamento do ReconPay'})` | ✅ PASS |
| AC2 content in server HTML, readable without JS; title and description | title `ReconPay · Conciliação financeira`; description about sales, settlement and statement | `frontend/app/page.test.tsx:20-21` - `metadata.title`/`description` `toBe(...)`; `frontend/components/landing/CountUp.test.tsx:101` (SSR values); the built `.next/server/app/index.html` has `<title>ReconPay · Conciliação financeira</title>`, the description meta, 1 `<h1`, 4 `<h2` and the CTA text (route `○ /` static) | ✅ PASS |

**Status**: ✅ All ACs covered (LAND-03 AC4 after fix `ce7991d`). No spec-precision gaps.

---

## Edge Cases

- [x] Page opened mid-way (`#divergencias`): `node hash.mjs` shows scrollY 2605 and every block above at opacity 1.00 / y 0.0, ring dash 24.9 and bar scaleX 1.00, with no scrolling; blocks still entering show intermediate values that match their position.
- [x] Logged-in user opens `/`: the landing renders with no session dependency (`frontend/app/page.test.tsx:14-16`), and "Entrar" goes to `/login` (`frontend/components/landing/LandingPage.test.tsx:20`), which goes straight to `/painel` when there is a session (`frontend/app/login/page.test.tsx:172`).

---

## Discrimination Sensor

Isolated scratch worktree (`git worktree add --detach …/verifier/wt HEAD`, symlinked `node_modules`), full vitest run per mutant. Real-tree `git status --porcelain` was empty before and after (identical). The worktree was removed.

| # | File | Mutation | Killed? |
| --- | --- | --- | --- |
| M1 | `frontend/app/login/page.tsx:27` | success `replace('/')` | ✅ (3 failed) |
| M2 | `frontend/app/login/page.tsx:17` | session `replace('/')` | ✅ (1) |
| M3 | `frontend/components/shell/TopNav.tsx:33` | brand `href="/"` | ✅ (1) |
| M4 | `frontend/components/shell/TopNav.tsx:11` | "Visão geral" `href: '/'` | ✅ (2) |
| M5 | `frontend/lib/scrollProgress.ts:3` | coverProgress ignores height | ✅ (4) |
| M6 | `frontend/lib/scrollProgress.ts:8` | rangeProgress without clamp | ✅ (2) |
| M7 | `frontend/lib/scrollProgress.ts:12` | `CHART_RANGE.end` 0.5 | ✅ (4) |
| M8 | `frontend/lib/scrollProgress.ts:17` | locale `en-US` | ✅ (16) |
| M9 | `frontend/lib/scrollProgress.ts:17` | `maximumFractionDigits: 3` | ✅ (2) |
| M10 | `frontend/components/landing/CountUp.tsx:8` | ignores reduced motion | ✅ (1) |
| M11 | `frontend/components/landing/CountUp.tsx:7` | ignores `CSS.supports` | ✅ (1) |
| M12 | `frontend/components/landing/CountUp.tsx:33` | no `scroll` listener | ✅ (1) |
| M13 | `frontend/components/landing/CountUp.tsx:34` | no `resize` listener | ✅ (1) |
| M14 | `frontend/components/landing/CountUp.tsx:46` | `sr-only` without the final value | ✅ (2) |
| M15 | `frontend/components/landing/CountUp.tsx:45` | animated span not `aria-hidden` | ✅ (6) |
| M16 | `frontend/components/landing/LandingPage.tsx:61` | hero "Criar conta" → `/login` | ✅ (1) |
| M17 | `frontend/components/landing/LandingPage.tsx:79` | GitHub href → API docs | ✅ (1) |
| M18 | `frontend/components/landing/LandingPage.tsx:37` | `#divergencias` → `#problema` | ✅ (1) |
| M19 | `frontend/components/landing/HowItWorks.tsx:70` | id `como-funciona` → `passos` | ✅ (1) |
| M20 | `frontend/components/landing/Discrepancies.tsx:20` | id `divergencias` → `decisoes` | ✅ (1) |
| M21 | `frontend/components/landing/FinalCta.tsx:11` | CTA text → "Veja a demonstração…" | ✅ (1) |
| M22 | `frontend/components/landing/LandingPage.tsx:54` | h1 → h2 (balanced tags; a first unbalanced attempt was discarded as a compile error) | ✅ (3) |
| M23 | `frontend/components/landing/LandingPage.tsx:41` | menu "Entrar" → `/register` | ✅ (1) |
| M24 | `frontend/components/landing/ProductPreview.tsx:30` | preview loses `role="img"` | ✅ (2) |
| M25 | `frontend/components/landing/ProductPreview.tsx:29` | id `fechamento` → `previa` | ✅ (1) |
| M26 | `frontend/components/landing/FinalCta.tsx:13` | CTA "Criar conta" → `/login` | ✅ (1) |
| M27 | `frontend/app/page.tsx:5` | metadata title | ✅ (1) |
| M28 | `frontend/components/landing/LandingPage.tsx:76` | footer → div (balanced) | ✅ (2) |

**Sensor depth**: lightweight+ (28 behavior mutants). **Result**: 28/28 killed. PASS.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code / no scope creep | ✅ (one client component; pure helpers; static preview) |
| Surgical changes | ✅ (TopNav `isActive` special case for `/` removed: it is now dead because no item is `/`, covered by `frontend/components/shell/TopNav.test.tsx:30-38`) |
| Matches patterns | ✅ |
| Spec-anchored outcome check | ✅ except LAND-03 AC4 (not tested; fails in the browser) |
| Per-layer coverage expectation | ✅ lib 1:1; components 1:1 with LAND-02/LAND-05; routes LAND-01. CSS layer "none" by design, browser-measured |
| No unclaimed tests | ✅ (the resize test maps to the T5 design, "escuta scroll e resize") |
| Guidelines followed | `AGENTS.md` (no tests weakened: the login/TopNav edits only retarget `/` → `/painel`), `frontend/vitest.config.ts` |

---

## Gate Check

- **Gate command**: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`
- **Result**: test 953 passed, 0 failed, 0 skipped (75 files); typecheck exit 0; build exit 0 (routes include `○ /` and `○ /painel`)
- **Test count before feature** (`b7ed449`, measured in the scratch worktree): 915
- **Test count after feature**: 953
- **Delta**: +38
- **Skipped / failures**: none

---

## Round 2 re-verification (fix `ce7991d`)

`ce7991d` changes `max-[379px]` to `max-[380px]` in `frontend/components/landing/LandingPage.tsx:41` and `frontend/components/landing/ProductPreview.tsx:103,107`. It also adds an empty `aria-hidden` spacer (`frontend/components/landing/ProductPreview.tsx:106`, `hidden max-[380px]:order-last max-[380px]:block max-[380px]:basis-full`) before the type pill, which is also `max-[380px]:order-last`. The compiled CSS now has `@media not all and (min-width:380px)`, i.e. `< 380px`.

Verifier measurements (headless Chromium 148, `next dev` on :3100, my own scripts in `/home/hanrry/.claude/jobs/3748ece6/tmp/verifier/`):

- `node r2.mjs` (port 9350): at **320/340/360/375/378/379**, "Entrar" is `display:none`, and every one of the 4 rows has `below=33` (type top 33 px under the ref), `left=0` (type starts under the ref) and `valLine=true` (amount stays on the ref's line, right-aligned). At **380/381/390/414**, "Entrar" is `block` and all rows have `below=-4, left=70` (type inline between ref and amount). No overflow and no element past the right edge at any of these widths.
- Screenshots: `rows375-r2.png` shows all four rows as ref + amount, then the type pill on its own line; `rows380-r2.png` shows the one-line rows of the 390 design.
- No regression: `node overflow3.mjs` 320–1920 all `ok`; `node vis2.mjs` breakpoints unchanged (anchors and tabs from 1024, KPIs from 1280, "Resolver" from 1400, content capped at 1440); at 379 now `entrar:false, typeWrapped:true`.
- In-page CSS sensor (`node sensor2.mjs`, `node s3.mjs`; mutating the live DOM, not the tree):

| # | Mutation | Probe result | Killed? |
| --- | --- | --- | --- |
| S1 | remove the spacer at 375 px | `below` 33,33,33,33 → -4,-4,-4,26 | ✅ |
| S2 | restore the old `max-[379px]:hidden` class on "Entrar" at 379 px | `display` none → block | ✅ |
| S3 | drop `max-[380px]:order-last` from the spacer and the type at 379 px | type still below (33), but the amount leaves the ref's line (`valSameLine:false` vs baseline `true`) | ✅ (by the amount-position metric) |

- Gate re-run: 953 passed (75 files), typecheck exit 0, build exit 0 (`○ /`, `○ /painel`). Real tree untouched (only the uncommitted `.specs` files written by the Verifier).

## Fix Plans (round 1, applied in `ce7991d`)

### Fix 1: LAND-03 AC4 below 380 px

- **Root cause**: in Tailwind 4, `max-[379px]:` compiles to `@media not all and (min-width:379px)` (`< 379px`), so 379 px is left out. Also, `flex-wrap` + `order-last` only wraps a row when it overflows. Between roughly 365 and 379 px the type fits after the amount on the first line, which gives a mixed layout (3 of 4 rows unwrapped at 375 px, a common iPhone width).
- **Fix task**: use `max-[380px]:` (`< 380px`) in `frontend/components/landing/LandingPage.tsx:41` and `frontend/components/landing/ProductPreview.tsx:103,105`, and force the type onto its own line below 380 px (e.g. `max-[380px]:basis-full` with the wrapper sized to the pill, or a column layout for the type).
- **Verify**: re-run `wrap.mjs` at 320/360/375/379 (every type's top > its ref's bottom), `nav379.mjs` at 379 (no "Entrar"), at 380 ("Entrar" present, types inline), and `overflow.mjs` 320–1920.
- **Priority**: Minor (visual, spec SHALL)

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| LAND-01 | Implemented | ✅ Verified |
| LAND-02 | Implemented | ✅ Verified |
| LAND-03 | Implemented | ✅ Verified (after `ce7991d`) |
| LAND-04 | Implemented | ✅ Verified |
| LAND-05 | Implemented | ✅ Verified |

---

## Summary

**Overall**: ✅ Ready (round 2)

**Spec-anchored check**: 22/22 ACs matched the spec outcome; 0 spec-precision gaps
**Sensor**: 28/28 code mutants killed; 3/3 in-page CSS mutants for the fix killed
**Gate**: 953 passed; typecheck and build green

**What works**: routes and redirects, all content, links and anchors, metadata and SSR, responsive breakpoints at 768/1024/1280/1400, no horizontal overflow from 320 to 1920 px, symmetric scroll animations, reduced-motion fallback, and numbers in step with the charts.

**Issues found**: round 1 LAND-03 AC4 (off-by-one breakpoint at 379 px, and the type not forced onto a second line). Fixed in `ce7991d` and re-verified. Lesson L-054 recorded as a candidate.

**Next steps**: none for validation. Optional, non-blocking: LAND-01 AC2 has structural evidence only (no test renders `app/(app)/painel/page.tsx`).
