# Redirecionar E-mails em Dev Specification

## Problem Statement

O `.env` local usa a chave real do Resend, então cada cadastro feito no ambiente local manda um e-mail de verdade para o endereço digitado. Endereços de teste inventados (por exemplo `ana.visual@reconpay.local`, em 2026-10-19) voltam como bounce, e bounces repetidos prejudicam a reputação do domínio `hanrry.top`, o mesmo que a produção usa.

## Goals

- [ ] No profile `dev`, todo e-mail vai para um endereço escolhido pelo desenvolvedor, e cadastros com endereços inventados param de gerar bounce.
- [ ] Fora do profile `dev`, nada muda.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Corrigir `RESEND_API_KEY` vazia ativando o `ResendEmailSender` | Bug conhecido, adiado pelo usuário (handoff, item a) |
| Remover a linha duplicada do `.env` do servidor | O usuário decidiu não mexer agora |
| Redirecionar outros tipos de e-mail | Só existe o de verificação |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Endereço de destino | Variável `RECONPAY_EMAIL_REDIRECT_TO`, lida só no `application-dev.yaml` (`reconpay.email.redirect-to`); o endereço pessoal fica só no `.env` local, que é gitignorado | Secrets e dados pessoais só via env (AGENTS.md); ligar a variável só no `dev` impede que ela vaze para produção | y |
| Conteúdo do e-mail redirecionado | Igual ao original: saudação, e-mail citado no texto e link de confirmação continuam sendo os do cadastro | Assim o desenvolvedor confirma qualquer conta de teste pela própria caixa | y |
| Marca de redirecionamento | Assunto ganha o prefixo `[dev → <endereço do cadastro>] ` | Na caixa de entrada dá para saber de qual cadastro veio cada e-mail | n |
| Log | O log de envio diz o endereço do cadastro e o de destino | Rastrear no console | y |
| Sem chave do Resend | O `LoggingEmailSender` continua só logando; o redirecionamento não muda nada nele | Nada é enviado nesse caso | y |

**Open questions:** none - all resolved or logged above.

### Implicit-requirement sweep

| Dimension | Coverage |
| --- | --- |
| Input validation & bounds | RED-01 AC3 (variável vazia ou só com espaços = sem redirecionamento) |
| Failure / partial-failure states | N/A because a falha do Resend segue o caminho de hoje |
| Idempotency / retry / duplicate handling | N/A because não muda o reenvio |
| Auth / authorization boundaries | RED-02 (só no profile `dev`) |
| Concurrency / state transitions | N/A because não há estado novo |
| Data lifecycle / expiry | N/A because nada é gravado |
| Persistence / external calls | RED-01 AC1 (destinatário na chamada ao Resend) |
| Accessibility | N/A because não há tela |
| Responsiveness | N/A because não há tela |

---

## User Stories

### P1: Redirecionar no dev ⭐ MVP

**User Story**: As a desenvolvedor, I want que os e-mails do ambiente local cheguem na minha caixa so that eu teste cadastros com qualquer endereço sem gerar bounce.

**Why P1**: É o pedido.

**Acceptance Criteria**:

1. WHEN `reconpay.email.redirect-to` tem um endereço e um e-mail de verificação é enviado pelo Resend THEN the system SHALL mandar para esse endereço, e só para ele, em vez do endereço do cadastro.
2. WHEN o e-mail é redirecionado THEN the system SHALL manter o HTML e o texto montados para o cadastro original (saudação, e-mail citado e link) e prefixar o assunto com `[dev → <endereço do cadastro>] `.
3. WHEN `reconpay.email.redirect-to` está ausente, vazio ou só com espaços THEN the system SHALL mandar para o endereço do cadastro, com o assunto de hoje.
4. WHEN o e-mail é redirecionado THEN the system SHALL registrar no log o endereço do cadastro e o de destino.

**Independent Test**: Com a variável no `.env` local, cadastrar `teste@exemplo.invalid` e receber o e-mail na caixa configurada.

---

### P1: Só no profile dev ⭐ MVP

**User Story**: As a dono do produto, I want que produção nunca redirecione so that os usuários reais recebam os próprios e-mails.

**Why P1**: Um redirecionamento ligado em produção quebraria o cadastro de todo mundo.

**Acceptance Criteria**:

1. WHEN a aplicação roda sem o profile `dev` (por exemplo `test` ou `prod`) e `RECONPAY_EMAIL_REDIRECT_TO` está definida THEN the system SHALL ignorar a variável e mandar para o endereço do cadastro.
2. WHERE o README lista as variáveis de ambiente the system SHALL documentar `RECONPAY_EMAIL_REDIRECT_TO` como opção só do `dev`, com um endereço de exemplo genérico.

**Independent Test**: Subir a aplicação no profile `test` com a variável definida e conferir que ela não é lida.

---

## Edge Cases

- WHEN o endereço do cadastro é igual ao de destino THEN the system SHALL mandar uma vez só, com o prefixo no assunto.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| RED-01 | P1: Redirecionar no dev | Execute | Verified |
| RED-02 | P1: Só no profile dev | Execute | Verified |

**ID format:** `RED-<NN>`; o critério é citado como `RED-01 AC2`.

**Coverage:** 2 total, 2 mapped to tasks

---

## Success Criteria

- [ ] Cadastros locais com endereços inventados deixam de gerar bounce no Resend.
