# Cadastro e Verificação de E-mail Design

**Spec**: `.specs/features/register-flow/spec.md`
**Status**: Approved

---

## Architecture Overview

Duas metades independentes que se encontram no link do e-mail.

```mermaid
graph LR
  R[/register] -- POST /api/auth/register --> AS[AuthService.register]
  AS --> EVS[EmailVerificationService.sendVerificationEmail]
  EVS --> ES[EmailSender]
  ES --> C[VerificationEmailComposer]
  C --> L[EmailVerificationLinkFactory<br/>base/verificar-email?token=]
  L -. e-mail .-> V[/verificar-email]
  V -- POST /api/auth/verify-email --> AS
  V -- POST /api/auth/resend-verification --> RS[EmailVerificationService.resend]
  RS --> EVS
```

- **Backend**: o texto do e-mail sai do `ResendEmailSender` e vai para um compositor puro (`VerificationEmailComposer`), testável sem rede. O link passa a apontar para o frontend. O reenvio é um método novo no `EmailVerificationService`, exposto por um endpoint público que sempre responde `202`.
- **Frontend**: o painel escuro do login vira `AuthPanel`, usado pelo login e pelo cadastro. `/register` é reescrito. `/verificar-email` é uma rota pública nova que confirma ao abrir e oferece o reenvio.

## Code Reuse Analysis

### Existing Components to Leverage

| Component | Location | How to Use |
| --- | --- | --- |
| `EmailVerificationService.sendVerificationEmail` | `backend/.../auth/service/EmailVerificationService.java` | O reenvio chama o mesmo método: apaga os pendentes, cria token e envia |
| `EmailVerificationLinkFactory` | `backend/.../auth/email/` | Só troca o caminho para `/verificar-email` |
| `EmailProperties` | `backend/.../auth/config/EmailProperties.java` | `verificationBaseUrl` (padrão novo `http://localhost:3000`) e `verificationTokenHours` para o prazo |
| `CapturingEmailSender` | `backend/src/test/.../auth/email/` | Integração do reenvio: conferir se houve ou não e-mail |
| `POST /api/auth/verify-email` | `AuthController` | A página do frontend chama este endpoint; já responde `204`/`400` |
| Painel e formulário do login | `frontend/app/login/page.tsx` | Painel vira `AuthPanel`; classes de campo e botão vão para o mesmo módulo |
| `request` / `ApiError` | `frontend/lib/api.ts` | `verifyEmail` e `resendVerification` seguem o padrão de `register` |

### Integration Points

| System | Integration Method |
| --- | --- |
| Resend | `ResendEmailSender` passa a mandar `html` e `text` do compositor |
| Spring Security | `POST /api/auth/resend-verification` entra no `permitAll` |
| Swagger | `AuthControllerApi` ganha o endpoint com exemplos; `ApiDocsIntegrationTest` cobre |
| Next.js | `/verificar-email` fica fora do grupo `(app)`, então não passa pelo `AppShell` (que manda para `/login`) |

## Components

### `VerificationEmailComposer` (backend)

- **Purpose**: montar assunto, HTML e texto puro do e-mail de verificação.
- **Location**: `backend/src/main/java/br/com/hanrry/reconpay/auth/email/VerificationEmailComposer.java`
- **Interfaces**: `VerificationEmail compose(String toEmail, String recipientName, String rawToken)`; `record VerificationEmail(String subject, String html, String text)`.
- **Dependencies**: `EmailVerificationLinkFactory`, `EmailProperties`.
- **Reuses**: o escape HTML do `ResendEmailSender` (estendido para `"` e `'`).
- **Notes**: HTML em tabelas com estilos inline, largura 560, fonte `Inter, Helvetica, Arial, sans-serif`, marca com "✓" em texto. Prazo: `1 hora` ou `N horas`.

### `EmailVerificationService.resend` (backend)

- **Purpose**: reenviar o e-mail só para quem tem direito, em silêncio para os demais.
- **Interfaces**: `void resend(String email)`.
- **Rules**: e-mail com `trim`; usuário por `findByEmail`; envia só se `!active`, se existe algum token da conta, se nenhum token foi consumido e se o último `createdAt` tem 60 s ou mais (`RESEND_COOLDOWN`). Usa um `Clock` injetado para o teste do intervalo.
- **Repository**: `IEmailVerificationTokenRepository` ganha `existsByUserIdAndConsumedAtIsNotNull` e `findTopByUserIdOrderByCreatedAtDesc`.

### Endpoint de reenvio (backend)

- `POST /api/auth/resend-verification`, corpo `ResendVerificationRequestDTO(@NotBlank @Email email)`, resposta `202` sem corpo; `400` `VALIDATION_ERROR` por bean validation.
- Documentado em `AuthControllerApi` com exemplos de `202` e `400`.

### `AuthPanel` e estilos compartilhados (frontend)

- **Location**: `frontend/components/auth/AuthPanel.tsx`
- **Interfaces**: `AuthPanel({ title, lead })`; constantes `authField`, `authPrimaryButton` exportadas do mesmo módulo.
- **Reuses**: o markup atual do login, sem mudança visual.

### Regras da senha (frontend)

- **Location**: `frontend/lib/password.ts`
- **Interfaces**: `PASSWORD_RULES: { label: string; test: (value: string) => boolean }[]` com "8 caracteres", "Uma letra maiúscula", "Um número"; `PASSWORD_PATTERN` igual ao atributo atual.

### `/register` (frontend)

- **Location**: `frontend/app/register/page.tsx`
- Formulário controlado só na senha (para as regras); envio igual ao atual; confirmação substitui o formulário.

### `/verificar-email` (frontend)

- **Location**: `frontend/app/verificar-email/page.tsx` (envolve em `Suspense`, exigido por `useSearchParams` no build) e `frontend/components/auth/VerifyEmail.tsx`.
- Estados: `verifying` → `confirmed` | `invalid` | `error`; no `invalid`, formulário de reenvio → `sent`.
- Uma chamada só por token: `useRef` guarda o token já enviado (modo estrito monta o efeito duas vezes).

## Data Models

Sem migração. O reenvio lê `email_verification_tokens` (`user_id`, `consumed_at`, `created_at`), que já existem.

## Error Handling Strategy

| Error Scenario | Handling | User Impact |
| --- | --- | --- |
| Token recusado (`400`) ou ausente | Estado `invalid` | "Este link não vale mais" e o reenvio |
| Rede ou `5xx` na confirmação | Estado `error` com a mensagem e "Tentar de novo" | Repete a confirmação |
| Reenvio `400` ou rede | Aviso `role="alert"` com a mensagem | E-mail mantido no campo |
| Cadastro `409`/`400` | Aviso `role="alert"` com a mensagem da API | Campos mantidos |
| Falha no Resend | Igual a hoje (exceção sobe) | Sem mudança de comportamento |

## Risks & Concerns

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Conta desativada se reativar pelo reenvio | Alto | Regra "nunca confirmou" (RSD-01 AC3) com teste dedicado |
| Enumeração de e-mails pelo reenvio | Médio | `202` igual para todos os casos, teste compara as respostas |
| Produção com `RECONPAY_VERIFICATION_BASE_URL` apontando para a API | Médio | O link novo cairia num 404 da API; o README e o handoff marcam a troca para o domínio da Vercel antes do deploy |
| `useSearchParams` sem `Suspense` quebra o build | Baixo | Página envolve o componente em `Suspense`; gate de build |

## Tech Decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Onde fica a confirmação | Frontend, chamando o `POST` existente | Mesmo visual e testes do app; a página da API fica só para links antigos |
| Texto do e-mail fora do sender | `VerificationEmailComposer` puro | Hoje o HTML não tem teste; com o compositor, todo o conteúdo é testável sem rede |
| Intervalo do reenvio | 60 s fixos com `Clock` injetado | Suficiente para não virar disparo; configurável não foi pedido |
