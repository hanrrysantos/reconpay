# Tela de Regras de Taxa Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/fee-rules-screen/design.md`
**Status**: Approved

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `AGENTS.md` (JUnit 5, Mockito, MockMvc, AssertJ, Testcontainers; integração herda `AbstractIntegrationTest`; JaCoCo 85% linha / 75% branch no `verify`; não enfraquecer testes), `frontend/vitest.config.ts` (`*.test.ts` em node, `*.test.tsx` em jsdom), `.github/workflows/ci.yml`. Lições confirmadas: nenhuma (candidatas L-015/L-016 aplicadas por cautela: uma só mensagem por campo inválido; textos exatos nos ACs).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Backend service (`FeeRuleService`) | unit | Todos os ramos novos de `create` e `update` | `backend/src/test/java/**/feerule/service/FeeRuleServiceTest.java` | `cd backend && ./mvnw -q test -Dtest=FeeRuleServiceTest` |
| Backend controller (`POST`/`PUT` fee-rules) | integration | `400` com texto exato para Pix/Boleto/Débito > 1 parcela (criar e alterar), `201` para combinações válidas, regra inalterada após `400` | `backend/src/test/java/**/feerule/integration/FeeRuleIntegrationTest.java` | `cd backend && ./mvnw verify` |
| Frontend ui (`SideDrawer`, `ConfirmDialog`) | unit (jsdom) | `Esc`, clique fora, foco preso, botão desabilitado em `pending`, erro exibido; testes de Empresas inalterados | `frontend/components/ui/*.test.tsx` | `cd frontend && corepack pnpm test` |
| Frontend lib (`lib/fee.ts`, `lib/api.ts`) | unit (node) | Limites dos ACs (0, 100, 4 e 2 casas, vírgula/ponto, vazio, negativo) e URL/método/corpo/decimais exatos | `frontend/lib/*.test.ts` | `cd frontend && corepack pnpm test` |
| Frontend hooks (`lib/queries.ts`) | unit (jsdom) | Invalidação de `['feeRules', merchantId]` após cada mutação | `frontend/lib/queries*.test.tsx` | `cd frontend && corepack pnpm test` |
| Frontend componentes (`components/fee-rules/*`, item do menu) | unit (jsdom) | 1:1 com FLIST, FCAD, FEDIT, FDES e edge cases (happy + validação + erro da API) | `frontend/components/fee-rules/*.test.tsx`, `frontend/components/shell/MerchantMenu.test.tsx` | `cd frontend && corepack pnpm test` |
| Docs (README) | none | - (build gate only) | - | build gate only |

## Gate Check Commands

> Generated from codebase - confirm before Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Tasks com testes unitários de frontend | `cd frontend && corepack pnpm test && corepack pnpm typecheck` |
| Full | Task de backend (unit + integração + cobertura JaCoCo) | `cd backend && ./mvnw verify` |
| Build | Fim de fase de frontend, refactor da casca e docs | `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build` |

---

## Execution Plan

Phases run sequentially; tasks within a phase run in order.

### Phase 1: Backend — parcelamento indevido

```
T1
```

### Phase 2: Frontend — casca comum e base de dados

```
T2
T3 → T4 → T5
```

### Phase 3: Frontend — listar e cadastrar (P1)

```
T5 → T6 → T7
```

### Phase 4: Frontend — alterar, desativar e docs (P2)

```
T7 → T8 → T9 → T10
```

---

## Task Breakdown

### T1: Recusar regra parcelada de Pix, Boleto e Débito

**What**: `FeeRuleService.create` e `update` validam a combinação (pedido ou valor atual, antes de qualquer `set`) com `PaymentMethodRules.allowsInstallments` e lançam `InvalidInstallmentsForPaymentMethodException("Método de pagamento <MÉTODO> não permite parcelamento")`.
**Where**: `backend/src/main/java/br/com/hanrry/reconpay/feerule/service/FeeRuleService.java`
**Depends on**: None
**Reuses**: `shared/PaymentMethodRules`, mensagem de `TransactionService.validateInstallmentsForPaymentMethod`
**Requirement**: FREG-01, FREG-02, FREG-03

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `POST` de `PIX`, `BOLETO` e `DEBIT_CARD` com 2 parcelas responde `400` `VALIDATION_ERROR` "Método de pagamento <MÉTODO> não permite parcelamento"
- [ ] `PUT` mudando uma regra `CREDIT_CARD` 3x para `PIX` (mantendo 3 parcelas) responde `400` com a mesma mensagem e um `GET` posterior mostra a regra inalterada
- [ ] `PUT` mudando uma regra `PIX` 1x para 2 parcelas responde `400`
- [ ] `POST` de `CREDIT_CARD` 12x e de `PIX` 1x respondem `201`
- [ ] `FeeRuleServiceTest` cobre os ramos novos sem chamar `save` no caso inválido
- [ ] Gate check passes: `cd backend && ./mvnw verify`

**Tests**: integration
**Gate**: full

**Commit**: `feat(feerule): reject installments for single-payment methods`

---

### T2: Casca comum de gaveta e diálogo

**What**: Criar `ui/SideDrawer` e `ui/ConfirmDialog` a partir da marcação atual de `MerchantDrawer` e `DeactivateDialog`, e fazer os dois componentes de Empresas usarem a casca sem mudar textos, papéis ARIA nem comportamento.
**Where**: `frontend/components/ui/SideDrawer.tsx`
**Depends on**: None
**Reuses**: `components/merchants/MerchantDrawer.tsx`, `components/merchants/DeactivateDialog.tsx`
**Requirement**: FCAD-12, FDES-04 (casca de `Esc`/Cancelar reaproveitada pelas telas novas)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `components/merchants/MerchantsPage.test.tsx` passa sem nenhuma alteração no arquivo
- [ ] Testes próprios de `SideDrawer` (`Esc`, clique fora, `Tab` preso, submit desabilitado em `pending`, erro com `role="alert"`) e de `ConfirmDialog` (`Esc`, confirmar, desabilitado em `pending`, erro)
- [ ] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`

**Tests**: unit
**Gate**: build

**Commit**: `refactor(frontend): share drawer and confirm dialog shells`

---

### T3: lib/fee.ts

**What**: `parseFeePercent`, `parseFixedFee`, `formatPercent`, `toInputDecimal` e `SINGLE_INSTALLMENT_METHODS`.
**Where**: `frontend/lib/fee.ts`
**Depends on**: None
**Reuses**: estilo de `parseAmount` em `ResolveDrawer.tsx`
**Requirement**: FLIST-03, FCAD-04, FCAD-08, FCAD-09

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `parseFeePercent`: `"2,99"`→`"2.99"`, `"0"`→`"0"`, `"100"`→`"100"`, `"2,9999"`→`"2.9999"`; `null` para `""`, `"abc"`, `"-1"`, `"100,01"`, `"2,99999"`
- [ ] `parseFixedFee`: `"0,30"`→`"0.30"`, `"0"`→`"0"`; `null` para `""`, `"-0,01"`, `"0,301"`, `"x"`
- [ ] `formatPercent`: `"2.9900"`→`"2,99%"`, `"0.0000"`→`"0%"`, `"100.0000"`→`"100%"`, `"1.5000"`→`"1,5%"`
- [ ] `toInputDecimal("2.9900")`→`"2,99"`, `toInputDecimal("0.30")`→`"0,30"`
- [ ] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add fee rule parsing and formatting helpers`

---

### T4: Funções de API de regra de taxa

**What**: Tipo `FeeRule`, `feePercentage`/`fixedFee` em `DECIMAL_FIELDS` e `getFeeRules`, `createFeeRule`, `updateFeeRuleFees`, `deactivateFeeRule`.
**Where**: `frontend/lib/api.ts`
**Depends on**: T3
**Reuses**: `request`, `LosslessNumber`, `stringify`
**Requirement**: FLIST-02, FCAD-04, FEDIT-02, FDES-02

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] URL exata de listagem (`size=100&sort=paymentMethod%2Casc&sort=installments%2Casc`)
- [ ] `POST` com corpo `{"paymentMethod":"CREDIT_CARD","installments":3,"feePercentage":2.99,"fixedFee":0.30}` literal (decimais sem passar por `float`)
- [ ] `PUT` só com `feePercentage` e `fixedFee`; `DELETE` sem corpo
- [ ] `feePercentage` `2.9900` recebido continua texto `"2.9900"`
- [ ] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): add fee rule API calls`

---

### T5: Hooks de regra de taxa

**What**: `useFeeRules`, `useCreateFeeRule`, `useUpdateFeeRuleFees`, `useDeactivateFeeRule`, com invalidação aguardada de `['feeRules', merchantId]`.
**Where**: `frontend/lib/queries.ts`
**Depends on**: T4
**Reuses**: padrão dos hooks de merchant
**Requirement**: FLIST-04, FCAD-05, FEDIT-03, FDES-03

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Após cada mutação, a lista da empresa é refeita antes de a mutação resolver
- [ ] Trocar o `merchantId` busca a lista da nova empresa e não devolve dados da anterior
- [ ] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): add fee rule queries and mutations`

---

### T6: Lista em /taxas e item do menu

**What**: `FeeRulesPage` (cabeçalho com nome da empresa, tabela formatada, estados vazio/sem empresa/carregando/erro), rota `app/(app)/taxas/page.tsx` e item "Regras de taxa da <empresa>" no rodapé do `MerchantMenu`.
**Where**: `frontend/components/fee-rules/FeeRulesPage.tsx`
**Depends on**: T5
**Reuses**: estrutura de `MerchantsPage`, `NoMerchant`, `BlockError`, `METHOD_LABELS`, `formatMoney`, `formatPercent`
**Requirement**: FLIST-01, FLIST-02, FLIST-03, FLIST-04, FLIST-05, FLIST-06, FLIST-07, FLIST-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Um teste por AC FLIST-01..08 (fetch falso no estilo de `MerchantsPage.test.tsx`), incluindo troca de empresa no menu
- [ ] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): list fee rules of the selected merchant`

---

### T7: Gaveta de cadastro de regra

**What**: `FeeRuleDrawer` em modo `create` sobre `SideDrawer`: Método, Parcelas (travada em 1 para Pix/Boleto/Débito), Percentual, Taxa fixa, validação com as mensagens exatas, erro da API, envio único.
**Where**: `frontend/components/fee-rules/FeeRuleDrawer.tsx`
**Depends on**: T6
**Reuses**: `ui/SideDrawer`, `lib/fee`, `useCreateFeeRule`
**Requirement**: FCAD-01, FCAD-02, FCAD-03, FCAD-04, FCAD-05, FCAD-06, FCAD-07, FCAD-08, FCAD-09, FCAD-10, FCAD-11, FCAD-12

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Um teste por AC FCAD-01..12 e pelos edge cases de limite (0 e 0; `2,9999`; `100`)
- [ ] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): create fee rules from a drawer`

---

### T8: Alterar taxas

**What**: Modo `edit` da `FeeRuleDrawer` (Método e Parcelas somente leitura, Percentual e Taxa fixa preenchidos com vírgula), aberto por "Editar"; `404` mostra a mensagem e recarrega a lista; troca de empresa fecha a gaveta.
**Where**: `frontend/components/fee-rules/FeeRuleDrawer.tsx`
**Depends on**: T7
**Reuses**: `useUpdateFeeRuleFees`, `toInputDecimal`
**Requirement**: FEDIT-01, FEDIT-02, FEDIT-03, FEDIT-04, FEDIT-05, FEDIT-06

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Um teste por AC FEDIT-01..06, o edge case `404` e o edge case de troca de empresa com a gaveta aberta
- [ ] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend): change fee rule rates`

---

### T9: Desativar regra

**What**: `DeactivateRuleDialog` sobre `ConfirmDialog`, aberto por "Desativar"; `404` recarrega a lista.
**Where**: `frontend/components/fee-rules/DeactivateRuleDialog.tsx`
**Depends on**: T8
**Reuses**: `ui/ConfirmDialog`, `useDeactivateFeeRule`, `METHOD_LABELS`
**Requirement**: FDES-01, FDES-02, FDES-03, FDES-04, FDES-05

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Um teste por AC FDES-01..05 e o edge case `404`
- [ ] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(frontend): deactivate fee rules with confirmation`

---

### T10: Documentação

**What**: README raiz (regra de Fee rules: Pix/Boleto/Débito só com 1 parcela) e `frontend/README.md` (fluxo de `/taxas` e item do menu de empresa).
**Where**: `README.md`
**Depends on**: T9
**Reuses**: seções existentes
**Requirement**: FREG-01, FLIST-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Os dois READMEs descrevem o comportamento entregue
- [ ] Gate check passes: `cd frontend && corepack pnpm test && corepack pnpm typecheck && corepack pnpm build`

**Tests**: none
**Gate**: build

**Commit**: `docs: describe fee rules screen and installment rule`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1
Phase 2:  T2 ; T3 → T4 → T5
Phase 3:  T6 → T7
Phase 4:  T8 → T9 → T10
```

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: Parcelamento indevido | 1 service (2 métodos) | ✅ Granular |
| T2: Casca comum | 2 componentes de casca + uso em 2 componentes de Empresas | ⚠️ OK (refactor coeso, protegido pelos testes de Empresas) |
| T3: lib/fee.ts | 1 módulo | ✅ Granular |
| T4: API de regra | 4 funções no mesmo arquivo | ⚠️ OK (coesas) |
| T5: Hooks de regra | 4 hooks no mesmo arquivo | ⚠️ OK (coesos) |
| T6: Lista e item do menu | 1 componente + rota + 1 item de menu | ⚠️ OK (entrada e destino da mesma navegação) |
| T7: Gaveta de cadastro | 1 componente | ✅ Granular |
| T8: Alterar taxas | 1 modo do componente | ✅ Granular |
| T9: Desativar | 1 componente | ✅ Granular |
| T10: Docs | 2 READMEs | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | T1 isolado | ✅ Match |
| T2 | None | T2 isolado | ✅ Match |
| T3 | None | início de T3 → T4 → T5 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | T4 | T4 → T5 | ✅ Match |
| T6 | T5 | T5 → T6 (T5 na Phase 2) | ✅ Match |
| T7 | T6 | T6 → T7 | ✅ Match |
| T8 | T7 | T7 → T8 (T7 na Phase 3) | ✅ Match |
| T9 | T8 | T8 → T9 | ✅ Match |
| T10 | T9 | T9 → T10 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | Backend service + contrato do controller | integration (maior) | integration | ✅ OK |
| T2 | Frontend ui + componentes de Empresas | unit | unit | ✅ OK |
| T3 | Frontend lib | unit | unit | ✅ OK |
| T4 | Frontend lib | unit | unit | ✅ OK |
| T5 | Frontend hooks | unit | unit | ✅ OK |
| T6 | Frontend componente + menu | unit | unit | ✅ OK |
| T7 | Frontend componente | unit | unit | ✅ OK |
| T8 | Frontend componente | unit | unit | ✅ OK |
| T9 | Frontend componente | unit | unit | ✅ OK |
| T10 | Docs | none | none | ✅ OK |
