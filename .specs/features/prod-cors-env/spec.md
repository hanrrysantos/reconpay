# CORS de Produção por Variável de Ambiente Specification

## Problem Statement

O frontend está na Vercel (`https://reconpay.vercel.app`), mas a API de produção só aceita origens `localhost`: a pré-requisição de `https://reconpay.vercel.app` responde `403`, então nenhuma tela consegue chamar a API. A lista vem de `reconpay.cors.allowed-origins` no `application.yaml`, e o `docker-compose.yaml` de produção não repassa nenhuma variável para trocá-la.

## Goals

- [ ] Definir as origens permitidas em produção pela variável `RECONPAY_CORS_ALLOWED_ORIGINS`, sem mudar código.
- [ ] Sem a variável, manter as origens de hoje.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Padrões com curinga (`*.vercel.app`) | Liberaria qualquer projeto da Vercel; as prévias de branch não precisam da API agora |
| Expor cabeçalhos de resposta (`Access-Control-Expose-Headers`) | O frontend não lê nenhum cabeçalho de resposta |
| Credenciais (`Access-Control-Allow-Credentials`) | A sessão é um JWT no header `Authorization`, sem cookie |
| Corrigir `RESEND_API_KEY` vazia | Decisão do usuário: fica para depois |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Nome e formato | `RECONPAY_CORS_ALLOWED_ORIGINS`, origens separadas por vírgula | Nome que o Spring Boot já liga a `reconpay.cors.allowed-origins`; nenhuma mudança em Java | n |
| Sobrescrever ou somar | A variável substitui a lista inteira do `application.yaml` | Em produção não faz sentido aceitar `localhost` | n |
| Espaços e barra final | `https://a.com, https://b.com/` vira `https://a.com` e `https://b.com` | Erro comum ao preencher a variável à mão | n |
| Padrão no Compose | As quatro origens `localhost` atuais | Sem a variável no `.env` do servidor, o comportamento não muda | n |
| Domínio | `https://reconpay.vercel.app` | Endereço que a Vercel gerou; um domínio próprio depois é só trocar o valor | y |

**Open questions:** none - all resolved or logged above.

### Implicit-requirement sweep

| Dimension | Coverage |
| --- | --- |
| Input validation & bounds | CORS-01 AC2 (espaços e barra final) |
| Failure / partial-failure states | CORS-01 AC3 (origem fora da lista recusada) |
| Idempotency / retry / duplicate handling | N/A because é configuração lida na subida |
| Auth / authorization boundaries | CORS-01 AC3 (só origens listadas) |
| Concurrency / state transitions | N/A because não há estado |
| Data lifecycle / expiry | N/A because nada é gravado |
| Persistence / external calls | N/A because não há chamada externa |
| Accessibility | N/A because não há tela |
| Responsiveness | N/A because não há tela |

---

## User Stories

### P1: Origens de produção pelo ambiente ⭐ MVP

**User Story**: As a operador da plataforma, I want definir as origens do CORS no `.env` do servidor so that o frontend da Vercel chame a API sem mudar código.

**Why P1**: Sem isso o frontend publicado não funciona.

**Acceptance Criteria**:

1. WHEN `RECONPAY_CORS_ALLOWED_ORIGINS` tem uma ou mais origens THEN the system SHALL aceitar a pré-requisição de cada uma em `/api/**` (`200` com `Access-Control-Allow-Origin` igual à origem) e recusar as origens que não estão nela, inclusive as `localhost` do padrão (`403` sem `Access-Control-Allow-Origin`).
2. WHEN as origens da variável têm espaços em volta ou barra final THEN the system SHALL aceitar a origem sem eles.
3. WHEN a variável não existe THEN the system SHALL manter as quatro origens `localhost` atuais.
4. WHERE a API roda pelo `docker-compose.yaml` the system SHALL repassar `RECONPAY_CORS_ALLOWED_ORIGINS` ao container, com as quatro origens atuais como padrão.

**Independent Test**: Com a variável no `.env` do servidor, a pré-requisição de `https://reconpay.vercel.app` responde `200`.

---

### P2: Documentação

**User Story**: As a quem roda o projeto, I want saber qual variável configurar so that eu publique o frontend em outro domínio.

**Why P2**: Vitrine pública.

**Acceptance Criteria**:

1. WHERE o README lista as variáveis the system SHALL documentar `RECONPAY_CORS_ALLOWED_ORIGINS`, o formato, que ela substitui a lista e o padrão.

**Independent Test**: Ler o README.

---

## Edge Cases

- WHEN a variável tem uma única origem THEN the system SHALL aceitar só ela.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| CORS-01 | P1: Origens de produção pelo ambiente | Execute | Implemented |
| CORS-02 | P2: Documentação | Execute | Implemented |

**ID format:** `CORS-<NN>`; o critério é citado como `CORS-01 AC2`.

**Coverage:** 2 total, 2 mapped (sem `tasks.md`: escopo pequeno, três passos)

---

## Success Criteria

- [ ] O frontend da Vercel faz login na API de produção.
