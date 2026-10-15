# Tela de Empresas Design

**Spec**: `.specs/features/merchants-screen/spec.md`
**Status**: Approved

---

## Architecture Overview

Backend: o CNPJ é validado de forma declarativa no DTO, normalizado no service e checado contra duplicidade por dígitos no repositório (abordagem "Bean Validation + util", escolhida pelo usuário). Frontend: a rota `/empresas` vira uma página com lista paginada, gaveta de cadastro/edição e diálogo de desativação; toda mutação invalida a lista da página e a lista do menu do topo, que continua dona da empresa selecionada.

```mermaid
graph TD
    subgraph Frontend
      P[/empresas · MerchantsPage/] --> L[useMerchantPage]
      P --> D[MerchantDrawer · criar/editar]
      P --> X[DeactivateDialog]
      D --> C1[useCreateMerchant / useRenameMerchant]
      X --> C2[useDeactivateMerchant]
      C1 & C2 -->|invalida| K1[['merchantPage']]
      C1 & C2 -->|invalida| K2[['merchants', userId] · menu do topo]
      C1 -->|select novo id| S[SelectedMerchantProvider]
      D --> V[lib/cnpj · máscara e dígitos verificadores]
    end
    subgraph Backend
      R[POST /api/merchants] --> A["@ValidCnpj no MerchantRequestDTO"]
      A -->|inválido| E400[400 VALIDATION_ERROR 'CNPJ inválido']
      A --> SV[MerchantService.create]
      SV --> U[CnpjRules.digits]
      SV --> Q[existsByDocumentDigits]
      Q -->|existe| E409[409 CONFLICT]
      SV --> DB[(merchants.document = 14 dígitos)]
    end
    L --> GET[GET /api/merchants]
    C1 --> R
```

---

## Code Reuse Analysis

### Existing Components to Leverage

| Component | Location | How to Use |
| --- | --- | --- |
| `@ValidDiscrepancyStatusRequest` + validator | `backend/.../reconciliation/validation/` | Mesmo padrão para `@ValidCnpj` |
| `PaymentMethodRules` | `backend/.../shared/PaymentMethodRules.java` | Mesmo estilo de util estático para `CnpjRules` |
| `GlobalExceptionHandler.handleValidation` | `backend/.../exception/handler/GlobalExceptionHandler.java:131` | Já transforma a mensagem do campo em `400 VALIDATION_ERROR` |
| `MerchantAlreadyExistsException` → `409` | `GlobalExceptionHandler.java:153` | Duplicidade continua pela mesma exceção |
| `request` / `ApiError` / `DECIMAL_FIELDS` | `frontend/lib/api.ts` | Novas funções de merchant seguem o mesmo `request` |
| `createQueryClient` (`401` encerra sessão, sem retry em 4xx) | `frontend/lib/queries.ts` | Hooks novos herdam o comportamento |
| `SelectedMerchantProvider` / `useSelectedMerchant` | `frontend/components/shell/MerchantMenu.tsx:23` | `select(id)` após criar; queda para `merchants[0]` cobre a desativação da selecionada |
| `formatCnpj`, `digits` | `frontend/components/shell/MerchantMenu.tsx:54` | Movidos para `lib/cnpj.ts` e reimportados pelo menu |
| Gaveta da resolução (overlay, `Esc`, foco) | `frontend/components/dashboard/ResolveDrawer.tsx` | Mesma casca visual e teclado na `MerchantDrawer` |
| `Confirm` (`role="alertdialog"`) | `frontend/components/dashboard/CloseStepper.tsx:24` | Mesmo estilo no `DeactivateDialog` |
| `NoMerchant` (estado "Nenhuma empresa no seu acesso") | `frontend/components/dashboard/states.tsx:40` | Texto e visual do estado vazio da lista |

### Integration Points

| System | Integration Method |
| --- | --- |
| `GET /api/merchants` | Lista paginada (`id`, `name`, `document`, `active`, `createdAt`), escopo ADMIN/OPERATOR da API |
| `POST /api/merchants` | Cadastro; o backend grava os dígitos e concede acesso ao criador (AD-003) |
| `PUT /api/merchants/{id}` | Renomear (`UpdateMerchantRequestDTO` só tem `name`) |
| `DELETE /api/merchants/{id}` | Soft delete (`204`) |
| Tabela `merchants` | Nenhuma migration; documentos legados ficam como estão e são comparados por dígitos |

---

## Components

### CnpjRules (backend)

- **Purpose**: Tirar a pontuação e validar um CNPJ.
- **Location**: `backend/src/main/java/br/com/hanrry/reconpay/shared/CnpjRules.java`
- **Interfaces**:
  - `static String digits(String value)` — remove `.`, `/`, `-` e espaços; não remove outros caracteres.
  - `static boolean isValid(String value)` — `true` só para 14 dígitos (após `digits`), não todos iguais, com os dois dígitos verificadores corretos; `false` para qualquer outro caractere.
- **Dependencies**: nenhuma
- **Reuses**: estilo de `PaymentMethodRules`

### @ValidCnpj + CnpjValidator (backend)

- **Purpose**: Validar o campo `document` do `MerchantRequestDTO` com a mensagem "CNPJ inválido".
- **Location**: `backend/src/main/java/br/com/hanrry/reconpay/merchant/validation/`
- **Interfaces**: `ConstraintValidator<ValidCnpj, String>`; `null` e branco são válidos para este validador (o `@NotBlank` cuida deles, evitando duas mensagens para o mesmo campo).
- **Reuses**: padrão de `ValidDiscrepancyStatusRequest`

### MerchantService.create + IMerchantRepository (backend)

- **Purpose**: Gravar o documento normalizado e recusar duplicata por dígitos.
- **Location**: `merchant/service/MerchantService.java`, `merchant/repository/IMerchantRepository.java`
- **Interfaces**:
  - `boolean existsByDocumentDigits(String digits)` — JPQL `function('regexp_replace', m.document, '[^0-9]', '', 'g') = :digits`, sem filtro de `active`.
  - `create` passa a usar `CnpjRules.digits(request.document())` na checagem, na mensagem do `409` e no valor gravado.
- **Reuses**: `MerchantAlreadyExistsException`; constraint `unique` de `document` continua barrando corrida entre duas criações (vira `409` pelo handler de `DataIntegrityViolationException`).

### lib/cnpj.ts (frontend)

- **Purpose**: Dígitos, máscara e validação de CNPJ no navegador.
- **Interfaces**:
  - `cnpjDigits(value: string): string`
  - `maskCnpj(input: string): string` — máscara progressiva `00.000.000/0000-00`, no máximo 14 dígitos.
  - `formatCnpj(document: string): string` — movida do menu; 14 dígitos formatados, outro valor como está.
  - `isValidCnpj(value: string): boolean` — mesma regra do backend.

### API e hooks de merchant (frontend)

- **Location**: `frontend/lib/api.ts`, `frontend/lib/queries.ts`
- **Interfaces**:
  - `type MerchantRecord = { id; name; document; active: boolean; createdAt: string }`
  - `getMerchantPage(token, page): Promise<Page<MerchantRecord>>` — `GET /api/merchants?page=<n>&size=20&sort=name%2Casc`
  - `createMerchant(token, { name, document }): Promise<MerchantRecord>`
  - `renameMerchant(token, id, name): Promise<MerchantRecord>`
  - `deactivateMerchant(token, id): Promise<void>`
  - `useMerchantPage(page)`, `useCreateMerchant()`, `useRenameMerchant()`, `useDeactivateMerchant()` — mutações aguardam a invalidação de `['merchantPage']` e `['merchants']` antes de resolver; `useCreateMerchant` chama `select(novoId)` depois da invalidação, para o menu já conter a empresa.

### MerchantsPage (frontend)

- **Purpose**: Página `/empresas`: cabeçalho com "Cadastrar empresa", tabela (nome, CNPJ, cadastro, ações Editar/Desativar), marca da selecionada, paginação, estados carregando/vazio/erro.
- **Location**: `frontend/components/merchants/MerchantsPage.tsx`; `frontend/app/(app)/empresas/page.tsx` passa a renderizá-la.

### MerchantDrawer (frontend)

- **Purpose**: Gaveta com modo `create` (Nome, CNPJ com máscara) e modo `edit` (Nome editável, CNPJ somente leitura); valida localmente, mostra erro da API, `Esc`/Cancelar fecham, botão desabilitado durante envio.
- **Location**: `frontend/components/merchants/MerchantDrawer.tsx`

### DeactivateDialog (frontend)

- **Purpose**: `alertdialog` com o nome da empresa, aviso de CNPJ reservado, Confirmar/Cancelar, `Esc` fecha, erro da API exibido.
- **Location**: `frontend/components/merchants/DeactivateDialog.tsx`

---

## Data Models

```typescript
// GET /api/merchants (MerchantResponseDTO)
type MerchantRecord = {
  id: string
  name: string
  document: string   // 14 dígitos para cadastros novos; legado pode ter pontuação
  active: boolean
  createdAt: string  // ISO instant
}
```

Nenhuma mudança de schema no banco.

---

## Error Handling Strategy

| Error Scenario | Handling | User Impact |
| --- | --- | --- |
| CNPJ inválido no backend | `@ValidCnpj` → `400 VALIDATION_ERROR` | Mensagem "CNPJ inválido" (não chega a acontecer pela tela, que valida antes) |
| CNPJ duplicado (inclusive legado formatado ou empresa desativada) | `existsByDocumentDigits` → `409` | Gaveta aberta com a mensagem da API |
| Corrida de duas criações iguais | Constraint `unique` → `409` existente | Mesma mensagem de conflito |
| Erro de rede / `5xx` | `ApiError` com mensagem | Gaveta ou diálogo aberto com a mensagem |
| `403`/`404` ao editar ou desativar | Mensagem e invalidação da lista | Lista recarregada sem a empresa |
| `401` | `createQueryClient` encerra a sessão | Volta ao login |

---

## Risks & Concerns

| Concern | Location (file:line) | Impact | Mitigation |
| --- | --- | --- | --- |
| 14 testes de integração criam empresas pela API com documento hex aleatório ou fixo inválido (`12345678000199`) | `backend/src/test/java/**` (ex.: `auth/controller/MeControllerIntegrationTest.java:48`) | Passam a receber `400` e quebram | Task própria antes da validação: gerador `TestCnpj.random()` em `util/` e troca do dado de entrada; nenhuma asserção muda |
| `MerchantServiceTest` simula `existsByDocument` | `backend/src/test/java/br/com/hanrry/reconpay/merchant/service/MerchantServiceTest.java` | Mock deixa de valer com o método novo | Atualizado na task do service, junto com os casos novos |
| Exemplos do OpenAPI usam `12345678000199`, que não é CNPJ válido | `merchant/openapi/MerchantControllerApi.java:51` | Swagger sugere um corpo que a API recusa | Trocar o exemplo por um CNPJ válido na task do validador |
| `existsByDocumentDigits` faz varredura com `regexp_replace` | `IMerchantRepository` | Lento com muitos merchants | Tabela pequena (empresas de um lojista/estudo); índice funcional fica como follow-up se crescer |
| `existsByDocument` antigo ignora documentos legados formatados | `merchant/repository/IMerchantRepository.java:19` | Duplicata passaria | Substituído por `existsByDocumentDigits` |
| `PUT` com nome branco devolve `200` sem mudar nada | `merchant/service/MerchantService.java:90` | Comportamento silencioso | A tela bloqueia nome vazio antes (MEDIT-04); mudança no backend fica fora de escopo |
| `scripts/seed-demo.py` (não versionado) gera documentos que podem não ser CNPJ | worktree `landing-page` | Seed passa a falhar com `400` | Avisar o usuário; ajuste fora do repositório |
| Casca de gaveta duplicada entre `ResolveDrawer` e `MerchantDrawer` | `frontend/components/dashboard/ResolveDrawer.tsx` | Duas cópias do mesmo estilo | Aceito nesta feature; extrair `ui/drawer` quando a terceira gaveta surgir |

---

## Tech Decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Nomes no backend | `CnpjRules` (util) e `@ValidCnpj` (anotação) | Evita dois tipos `Cnpj`; segue `PaymentMethodRules` e `ValidDiscrepancyStatusRequest` |
| Validador com branco | `@ValidCnpj` aceita `null`/branco | `@NotBlank` dá a única mensagem do campo vazio |
| Pontuação aceita no backend | `.`, `/`, `-`, espaço; qualquer outro caractere é inválido | MDOC-03 |
| Seleção após criar | `select` depois de aguardar a invalidação do menu | Sem isso, `useSelectedMerchant` cai em `merchants[0]` até o refetch |
| Lista da página × menu | Queries separadas (`['merchantPage', page]` e `['merchants', userId]`) | A página precisa de `createdAt` e paginação; o menu carrega todas as páginas |
