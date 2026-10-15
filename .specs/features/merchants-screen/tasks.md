# Tela de Empresas Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/merchants-screen/design.md`
**Status**: Approved

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `AGENTS.md` (JUnit 5, Mockito, MockMvc, AssertJ, Testcontainers; integração herda `AbstractIntegrationTest`; JaCoCo 85% linha / 75% branch no `verify`; não enfraquecer testes), `backend/pom.xml` (JaCoCo), `frontend/vitest.config.ts` (`*.test.ts` em node, `*.test.tsx` em jsdom), `.github/workflows/ci.yml`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Backend util / validator (`CnpjRules`, `CnpjValidator`) | unit | Todos os ramos; 1:1 com MDOC-01..03 e MDOC-05 | `backend/src/test/java/**/<Classe>Test.java` | `cd backend && ./mvnw -q test -Dtest=<Classe>Test` |
| Backend service (`MerchantService`) | unit | Todos os ramos de `create`; 1:1 com MDOC-01 e MDOC-04 | `backend/src/test/java/**/merchant/service/MerchantServiceTest.java` | `cd backend && ./mvnw -q test -Dtest=MerchantServiceTest` |
| Backend controller / repository (`POST /api/merchants`, `existsByDocumentDigits`) | integration | Rota: sucesso + `400` + `409` (dígitos, legado formatado, desativada); query contra Postgres real | `backend/src/test/java/**/merchant/integration/*IntegrationTest.java` | `cd backend && ./mvnw verify` |
| Frontend lib (`lib/cnpj.ts`, `lib/api.ts`) | unit (node) | Cada função: casos válidos, inválidos e limites citados nos ACs | `frontend/lib/*.test.ts` | `cd frontend && corepack pnpm test` |
| Frontend hooks (`lib/queries.ts`) | unit (jsdom) | Invalidação e seleção após criar/desativar | `frontend/lib/queries*.test.tsx` | `cd frontend && corepack pnpm test` |
| Frontend componentes (`components/merchants/*`) | unit (jsdom, Testing Library) | 1:1 com os ACs MCAD, MLIST, MEDIT, MDES e edge cases (happy + validação + erro da API) | `frontend/components/merchants/*.test.tsx` | `cd frontend && corepack pnpm test` |
| Docs (README) | none | - (build gate only) | - | build gate only |

## Gate Check Commands

> Generated from codebase - confirm before Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Tasks com testes unitários de frontend | `cd frontend && corepack pnpm test && corepack pnpm typecheck` |
| Full | Tasks de backend (unit + integração + cobertura JaCoCo) | `cd backend && ./mvnw verify` |
| Build | Fim de fase de frontend e task de docs | `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build` |

---

## Execution Plan

Phases run sequentially; tasks within a phase run in order.

### Phase 1: Backend — CNPJ normalizado e validado

```
T1 → T2 → T3 → T4
```

### Phase 2: Frontend — base de dados e CNPJ

```
T5
T6 → T7
```

### Phase 3: Frontend — listar e cadastrar (P1)

```
T7 → T8 → T9
```

### Phase 4: Frontend — renomear, desativar e docs (P2)

```
T9 → T10 → T11 → T12
```

---

## Task Breakdown

### T1: Gerador de CNPJ válido para os testes ✅

**What**: Criar `TestCnpj.random()` (CNPJ de 14 dígitos com dígitos verificadores válidos) e trocar por ele o documento hex aleatório ou o fixo `12345678000199` nos 14 testes de integração que fazem `POST /api/merchants`, sem mudar nenhuma asserção.
**Where**: `backend/src/test/java/br/com/hanrry/reconpay/util/TestCnpj.java`
**Depends on**: None
**Reuses**: `util/IntegrationTestUtils` (mesmo pacote de utilitários)
**Requirement**: MDOC-02 (pré-requisito: os testes existentes precisam enviar CNPJ válido antes da validação existir)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `TestCnpj.random()` gera CNPJ cujo dígito verificador confere (teste unitário do gerador com 1000 amostras)
- [x] Nenhum teste de integração envia documento hex ou `12345678000199` em `POST /api/merchants`
- [x] Gate check passes: `cd backend && ./mvnw verify`
- [x] Test count: 497 + testes do gerador passam (sem remoções)

**Tests**: unit
**Gate**: full

**Commit**: `test(merchant): generate valid CNPJs for merchant fixtures`

---

### T2: CnpjRules ✅

**What**: Util estático com `digits` (remove `.`, `/`, `-`, espaço) e `isValid` (14 dígitos, não todos iguais, verificadores corretos, nenhum outro caractere).
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/shared/CnpjRules.java`
**Depends on**: T1
**Reuses**: estilo de `shared/PaymentMethodRules.java`
**Requirement**: MDOC-01, MDOC-02, MDOC-03, MDOC-05

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `digits("12.345.678/0001-95")` = `12345678000195`
- [x] `isValid` verdadeiro para `12345678000195` e `12.345.678/0001-95`; falso para 13/15 dígitos, verificador errado, `11111111111111`, `00000000000000`, letra no meio
- [x] Gate check passes: `cd backend && ./mvnw -q test -Dtest=CnpjRulesTest`

**Tests**: unit
**Gate**: full

**Commit**: `feat(merchant): add CNPJ digits and check-digit rules`

---

### T3: @ValidCnpj no cadastro ✅

**What**: Anotação `@ValidCnpj` + `CnpjValidator` (branco/`null` válidos para ela) aplicada em `MerchantRequestDTO.document`, com mensagem "CNPJ inválido"; exemplos do OpenAPI trocados para `12345678000195`.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/merchant/validation/`
**Depends on**: T2
**Reuses**: padrão de `reconciliation/validation/ValidDiscrepancyStatusRequest`
**Requirement**: MDOC-02, MDOC-03, MDOC-05

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `POST /api/merchants` com 13 dígitos, verificador errado, `11111111111111` ou `12A45678000195` responde `400` `VALIDATION_ERROR` "CNPJ inválido"
- [x] `POST` com documento vazio responde só a mensagem do `@NotBlank`
- [x] Unit test do `CnpjValidator` (branco, `null`, válido, inválido)
- [x] Gate check passes: `cd backend && ./mvnw verify`

**Tests**: integration
**Gate**: full

**Commit**: `feat(merchant): reject invalid CNPJ on merchant creation`

---

### T4: Gravar dígitos e checar duplicidade por dígitos ✅

**What**: `IMerchantRepository.existsByDocumentDigits` (JPQL com `regexp_replace`, ativas e desativadas) e `MerchantService.create` gravando `CnpjRules.digits` e usando a nova checagem.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/merchant/service/MerchantService.java`
**Depends on**: T3
**Reuses**: `MerchantAlreadyExistsException`, `AbstractIntegrationTest`
**Requirement**: MDOC-01, MDOC-04

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `POST` com `12.345.678/0001-95` responde `201` com `document` `12345678000195`
- [x] `POST` com os mesmos dígitos (sem pontuação) responde `409`
- [x] Empresa legada gravada direto no repositório como `12.345.678/0001-95` faz `POST 12345678000195` responder `409`
- [x] Empresa desativada bloqueia o mesmo CNPJ com `409`
- [x] `MerchantServiceTest` cobre normalização e duplicidade com o método novo
- [x] Gate check passes: `cd backend && ./mvnw verify`

**Tests**: integration
**Gate**: full

**Commit**: `feat(merchant): store CNPJ digits and detect duplicates on digits`

---

### T5: lib/cnpj.ts ✅

**What**: `cnpjDigits`, `maskCnpj`, `formatCnpj` (movida do menu) e `isValidCnpj`; `MerchantMenu.tsx` passa a importar `formatCnpj` daqui.
**Where**: `frontend/lib/cnpj.ts`
**Depends on**: None
**Reuses**: `formatCnpj`/`digits` de `components/shell/MerchantMenu.tsx`
**Requirement**: MCAD-02, MCAD-06, MLIST-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `maskCnpj` progressivo (`12` → `12`, `12345` → `12.345`, 15 dígitos colados → corta em 14, colado formatado → reformata)
- [x] `isValidCnpj` com os mesmos casos do T2
- [x] `formatCnpj` deixa documento sem 14 dígitos como está
- [x] Testes existentes do `MerchantMenu` continuam passando
- [x] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add CNPJ mask and validation helpers`

---

### T6: Funções de API de merchant ✅

**What**: Tipo `MerchantRecord` e `getMerchantPage`, `createMerchant`, `renameMerchant`, `deactivateMerchant` em `lib/api.ts`.
**Where**: `frontend/lib/api.ts`
**Depends on**: None
**Reuses**: `request`, `send`, `Page`
**Requirement**: MLIST-01, MCAD-03, MEDIT-02, MDES-02

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Testes de URL, método, corpo e `Authorization` de cada função (`page=<n>&size=20&sort=name%2Casc`; `POST` com `{name, document}`; `PUT` com `{name}`; `DELETE` sem corpo)
- [x] Erro `409` vira `ApiError` com a mensagem do `StandardError`
- [x] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add merchant API calls`

---

### T7: Hooks de merchant ✅

**What**: `useMerchantPage`, `useCreateMerchant` (aguarda invalidação e seleciona o id criado), `useRenameMerchant`, `useDeactivateMerchant`; todas as mutações aguardam a invalidação de `['merchantPage']` e `['merchants']`.
**Where**: `frontend/lib/queries.ts`
**Depends on**: T6
**Reuses**: `createQueryClient`, `useSelectedMerchant`
**Requirement**: MCAD-04, MEDIT-03, MDES-03, MDES-04

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Após criar, o menu já contém a empresa e ela é a selecionada
- [x] Após renomear/desativar, as duas listas são refeitas
- [x] Desativar a selecionada faz o menu cair na primeira restante (ou `null` sem nenhuma)
- [x] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add merchant queries and mutations`

---

### T8: Lista de empresas em /empresas ✅

**What**: `MerchantsPage` com tabela (nome, CNPJ, cadastro), marca da selecionada, clique no nome seleciona, paginação Anterior/Próxima, estados carregando/vazio/erro com "Tentar de novo"; `app/(app)/empresas/page.tsx` renderiza a página.
**Where**: `frontend/components/merchants/MerchantsPage.tsx`
**Depends on**: T7
**Reuses**: `NoMerchant`/`BlockError` de `components/dashboard/states.tsx`, `formatCnpj`
**Requirement**: MLIST-01, MLIST-02, MLIST-03, MLIST-04, MLIST-05, MLIST-06, MLIST-07, MLIST-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Um teste por AC MLIST-01..08 (fetch falso no estilo de `DashboardPage.test.tsx`)
- [x] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): list merchants on the merchants page`

---

### T9: Gaveta de cadastro

**What**: `MerchantDrawer` em modo `create` (Nome, CNPJ com máscara, validação local, erro da API, botão desabilitado no envio, `Esc`/Cancelar) aberta por "Cadastrar empresa".
**Where**: `frontend/components/merchants/MerchantDrawer.tsx`
**Depends on**: T8
**Reuses**: casca e teclado de `ResolveDrawer`, `maskCnpj`/`isValidCnpj`, `useCreateMerchant`
**Requirement**: MCAD-01, MCAD-02, MCAD-03, MCAD-04, MCAD-05, MCAD-06, MCAD-07, MCAD-08, MCAD-09, MCAD-10

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Um teste por AC MCAD-01..10, incluindo nome com 150 caracteres aceito e 151 bloqueado, e duplo clique gerando um só `POST`
- [ ] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): create merchants from a drawer`

---

### T10: Renomear empresa

**What**: Modo `edit` da `MerchantDrawer` (nome editável, CNPJ formatado somente leitura) aberto pela ação "Editar" da lista; `403`/`404` mostram a mensagem e recarregam a lista.
**Where**: `frontend/components/merchants/MerchantDrawer.tsx`
**Depends on**: T9
**Reuses**: `useRenameMerchant`
**Requirement**: MEDIT-01, MEDIT-02, MEDIT-03, MEDIT-04, MEDIT-05, MEDIT-06

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Um teste por AC MEDIT-01..06 e o edge case `404` ao editar
- [ ] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): rename merchants`

---

### T11: Desativar empresa

**What**: `DeactivateDialog` (`alertdialog` com nome e aviso de CNPJ reservado, Confirmar/Cancelar, `Esc`, erro da API) aberto pela ação "Desativar" da lista.
**Where**: `frontend/components/merchants/DeactivateDialog.tsx`
**Depends on**: T10
**Reuses**: estilo de `Confirm` em `CloseStepper.tsx`, `useDeactivateMerchant`
**Requirement**: MDES-01, MDES-02, MDES-03, MDES-04, MDES-05, MDES-06

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Um teste por AC MDES-01..06 e o edge case `403` ao desativar
- [ ] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): deactivate merchants with confirmation`

---

### T12: Documentação

**What**: README raiz (regra de Merchants: CNPJ de 14 dígitos normalizado, duplicidade por dígitos) e `frontend/README.md` (fluxo de `/empresas` fora da lista "em construção").
**Where**: `README.md`
**Depends on**: T11
**Reuses**: seções existentes
**Requirement**: MDOC-01, MLIST-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Os dois READMEs descrevem o comportamento entregue
- [ ] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`

**Tests**: none
**Gate**: build

**Commit**: `docs: describe merchants screen and CNPJ rules`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1 → T2 → T3 → T4
Phase 2:  T5 ; T6 → T7
Phase 3:  T8 → T9
Phase 4:  T10 → T11 → T12
```

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: Gerador de CNPJ de teste | 1 util + troca de dado de entrada nos fixtures | ⚠️ OK (mudança mecânica e coesa) |
| T2: CnpjRules | 1 util | ✅ Granular |
| T3: @ValidCnpj | 1 anotação + validador + uso no DTO | ⚠️ OK (coesos) |
| T4: Dígitos e duplicidade | 1 query + 1 método de service | ⚠️ OK (coesos) |
| T5: lib/cnpj.ts | 1 módulo | ✅ Granular |
| T6: API de merchant | 4 funções no mesmo arquivo | ⚠️ OK (coesas) |
| T7: Hooks de merchant | 4 hooks no mesmo arquivo | ⚠️ OK (coesos) |
| T8: Lista | 1 componente + rota | ✅ Granular |
| T9: Gaveta de cadastro | 1 componente | ✅ Granular |
| T10: Renomear | 1 modo do componente | ✅ Granular |
| T11: Desativar | 1 componente | ✅ Granular |
| T12: Docs | 2 READMEs | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | início da Phase 1 | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | None | T5 isolado | ✅ Match |
| T6 | None | início de T6 → T7 | ✅ Match |
| T7 | T6 | T6 → T7 | ✅ Match |
| T8 | T7 | T7 → T8 (T7 na Phase 2) | ✅ Match |
| T9 | T8 | T8 → T9 | ✅ Match |
| T10 | T9 | T9 → T10 (T9 na Phase 3) | ✅ Match |
| T11 | T10 | T10 → T11 | ✅ Match |
| T12 | T11 | T11 → T12 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | Test util (+ fixtures) | unit | unit | ✅ OK |
| T2 | Backend util | unit | unit | ✅ OK |
| T3 | Backend validator + controller contract | integration (maior) | integration | ✅ OK |
| T4 | Backend service + repository | integration (maior) | integration | ✅ OK |
| T5 | Frontend lib | unit | unit | ✅ OK |
| T6 | Frontend lib | unit | unit | ✅ OK |
| T7 | Frontend hooks | unit | unit | ✅ OK |
| T8 | Frontend componente | unit | unit | ✅ OK |
| T9 | Frontend componente | unit | unit | ✅ OK |
| T10 | Frontend componente | unit | unit | ✅ OK |
| T11 | Frontend componente | unit | unit | ✅ OK |
| T12 | Docs | none | none | ✅ OK |
