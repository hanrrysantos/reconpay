# API Docs Context

**Gathered:** 2026-10-07
**Spec:** `.specs/features/api-docs/spec.md`
**Status:** Ready for design

---

## Feature Boundary

Documentação OpenAPI / Swagger UI das rotas `/api/**` que já existem: exemplos para testar, códigos de resposta explicados, tags na ordem do fluxo e intro curta. Sem mudar contrato HTTP, README, coleção externa ou idioma.

---

## Implementation Decisions

### Profundidade

- Cada operação abre com exemplo preenchido para o Try it out.
- Request JSON traz todos os campos obrigatórios com valor que passa na validação.
- Login usa o seed de dev `admin@reconpay.local` / `DevAdmin@2026`.
- Cada status que a operação produz tem uma frase em português.
- Resposta JSON, de sucesso ou de erro, inclui um exemplo. Erro segue `StandardError`, com `status` e `error` iguais ao código documentado.
- 204 não tem corpo. Upload mostra a parte `file` e um CSV mínimo na description. Export de conciliação é `text/csv` com a linha de cabeçalho. O link HTML de verificação é `text/html`.

### Onde a anotação mora

- Uma interface `*Api` por controller, no mesmo módulo, implementada pelo controller.
- Summary, description, exemplos e responses ficam na interface.
- O controller continua só com o mapeamento HTTP e a delegação.
- `AuthControllerApi` já existe e passa a ganhar os exemplos que faltam.

### Agrupamento

- Ordem fixa: Authentication, Session, Users, Merchants, Fee Rules, Transactions, External Settlements, Bank Statements, Reconciliations.
- Cada tag tem description.
- O Swagger UI não reordena em alfabético.
- Dentro da tag, a ordem é a dos métodos no controller.

### Página inicial

- `info.description` diz o que a API faz e o que ADMIN e OPERATOR fazem.
- Sem roteiro numerado de chamadas. O teste fica no exemplo de cada rota.

### Agent's Discretion

- Texto exato das descriptions, desde que em português, específico da operação, e sem lista numerada na intro.
- Nome da interface (`MerchantControllerApi` e equivalentes) e o pacote, desde que haja uma interface por controller.
- UUID de exemplo único `3fa85f64-5717-4562-b3fc-2c963f66afa6` e os valores concretos dos outros exemplos, desde que válidos na Bean Validation.
- Reuso das anotações `ApiValidationErrorResponse`, `ApiUnauthorizedResponse` e `ApiConflictResponse` quando o exemplo servir à operação; exemplo específico quando a mensagem ou o path precisarem ser daquela rota.

### Declined / Undiscussed Gray Areas → Assumptions

Nenhuma. Os quatro pontos pedidos foram decididos.

---

## Specific References

- Interface existente: `auth/openapi/AuthControllerApi`.
- Erros reutilizáveis: `ApiValidationErrorResponse`, `ApiUnauthorizedResponse`, `ApiConflictResponse`.
- Seed de login: README, seção Segurança.
- Ordem operacional já esboçada em `OpenApiConfig`, atualizada com Bank Statements e sem o roteiro numerado.

---

## Deferred Ideas

None - discussion stayed within feature scope
