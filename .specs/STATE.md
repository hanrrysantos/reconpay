# STATE

## Decisions

### AD-001
- **Decision**: Dois papéis globais — `ADMIN` (governança mínima da instalação) e `OPERATOR` (fluxo completo de conciliação no escopo concedido).
- **Reason**: Projeto de estudo alinhado ao domínio; operador configura merchant/taxas e executa transações, import e conciliação; admin não aprova conta manualmente.
- **Trade-off**: Separação de funções mais fraca que um modelo enterprise com três papéis; admin ainda pode ser superusuário técnico se mantiver bypass no guard.
- **Scope**: Auth, SecurityConfig, guards, documentação, seeds de teste.
- **Date**: 2026-10-02
- **Status**: active

### AD-002
- **Decision**: Ativação de conta por link de e-mail **antes** do primeiro login bem-sucedido (substitui `PATCH /api/users/{id}/activation` para auto-registro).
- **Reason**: Fluxo familiar de verificação de e-mail; remove dependência de admin para aprovar cadastro público.
- **Trade-off**: Exige infra de e-mail (Mailhog/SMTP), tokens com expiração e novos testes de integração.
- **Scope**: Módulo `auth`, migrations, OpenAPI, testes.
- **Date**: 2026-10-02
- **Status**: active

### AD-003
- **Decision**: Auto-grant — ao criar merchant, o usuário autenticado criador recebe grant em `user_merchants` automaticamente.
- **Reason**: Evita 403 imediato após `POST /api/merchants` para OPERATOR; owner explícito fica fora de escopo.
- **Trade-off**: Admin também recebe linha redundante em `user_merchants` (inofensivo enquanto admin bypassa o guard).
- **Scope**: `merchant` service, `user_merchants`, testes de isolamento.
- **Date**: 2026-10-02
- **Status**: active

### AD-004
- **Decision**: Conceito de merchant **owner** (convites, delegação por merchant) fica **fora de escopo** até reavaliação explícita.
- **Reason**: Complexidade desnecessária para MVP de estudo com poucos usuários.
- **Trade-off**: Compartilhar merchant entre operators continua via `PUT /api/users/{id}/merchants` (ADMIN).
- **Scope**: Backlog futuro; não implementar campos `owner_id`.
- **Date**: 2026-10-02
- **Status**: active

### AD-005
- **Decision**: E-mails transacionais (verificação de conta e futuros) via **Resend** (API HTTP).
- **Reason**: Escolha explícita do autor; simplifica produção vs SMTP self-managed.
- **Trade-off**: Dependência de serviço externo; testes usam mock/fake sender, dev pode usar API key ou stub.
- **Scope**: Módulo `auth` / notificações; variáveis `RESEND_API_KEY`, remetente configurável.
- **Date**: 2026-10-02
- **Status**: active

### AD-006
- **Decision**: A documentação OpenAPI de cada controller fica numa interface `*Api` que ele implementa. O método do controller não carrega `@Operation`.
- **Reason**: O volume de exemplo e de código de resposta enterraria o mapping HTTP. O login já usava essa forma.
- **Trade-off**: Dois tipos por recurso. Um `openapi.yaml` manual ou a anotação no método ficam fora do padrão.
- **Scope**: Controllers em `/api/**` e features futuras que publiquem endpoint.
- **Date**: 2026-10-07
- **Status**: active

### AD-007
- **Decision**: Frontend in `frontend/` with Next.js, React, TypeScript, Tailwind and the v0 dashboard layout; the browser sends the memory-only JWT directly to the Spring API.
- **Reason**: Reuse the approved visual reference while making login, merchant selection and recent transactions functional without inventing financial metrics.
- **Trade-off**: Reload requires login again; aggregated metrics remain unavailable until their API contract exists.
- **Scope**: Frontend base.
- **Date**: 2026-10-08
- **Status**: superseded by AD-009

### AD-008
- **Decision**: O projeto Maven, seu wrapper, código Java e Dockerfile ficam em `backend/`; `frontend/` é seu diretório irmão. Compose, `.env`, CI e documentação geral permanecem na raiz.
- **Reason**: Separar os dois aplicativos sem duplicar segredos ou alterar os comandos do Compose na raiz.
- **Trade-off**: Comandos Maven passam a rodar de `backend/`; o profile `dev` lê `../.env` a partir desse diretório.
- **Scope**: Layout do repositório, build, desenvolvimento local e CI.
- **Date**: 2026-10-08
- **Status**: active

### AD-009
- **Decision**: O frontend segue a direção visual "Lima" (`.specs/features/dashboard-redesign/design-reference.md`), com tokens no `@theme` do Tailwind 4, fontes Inter Tight / Inter / IBM Plex Mono e gráficos em SVG próprio. TanStack Query é a camada de dados. O navegador continua enviando o JWT, mantido só em memória, direto para a API Spring, e nenhum número aparece sem vir da API.
- **Reason**: O layout v0 virou dívida de CSS escrito à mão; o dashboard novo precisa de polling, invalidação após mutação e chave por empresa/janela.
- **Trade-off**: Dependência nova (TanStack Query) e testes com jsdom; recarregar a página continua pedindo login.
- **Scope**: Todo o frontend em `frontend/`.
- **Date**: 2026-10-13
- **Status**: active

### AD-010
- **Decision**: Valores de dinheiro exibidos no frontend vêm prontos do backend: somas, séries, diferenças e percentuais sobre dinheiro (ex.: `GET /periods/summary`, `netDifference` nos items). O frontend só formata decimais recebidos como string e compara sinal; não soma, subtrai nem divide valores. A regra cobre só dinheiro: taxas de contagens (ex.: % conciliado de um dia, variação em pontos percentuais entre duas taxas da API) podem ser derivadas no frontend.
- **Reason**: `BigDecimal` no servidor é a única fonte de verdade de dinheiro; calcular no navegador duplicaria regras de arredondamento e arriscaria divergir do CSV de auditoria.
- **Trade-off**: Cada número derivado novo exige campo no contrato do backend.
- **Scope**: Frontend e endpoints de leitura usados por ele.
- **Date**: 2026-10-14 (revisado duas vezes no dia: sem aritmética local de dinheiro; escopo limitado a dinheiro)
- **Status**: active

### AD-011
- **Decision**: Transações internas entram por dois caminhos que convivem: formulário (uma transação, sobre o `POST /transactions` existente) e import CSV em lote, com validação por linha e lote rastreável, no mesmo padrão do import de liquidação.
- **Reason**: No domínio, as transações vêm em lote do sistema do lojista; o formulário cobre a transação avulsa, a correção e o caso de teste isolado.
- **Trade-off**: O import CSV é uma feature nova de backend (endpoint, parser, lote, OpenAPI, testes); cada linha exige regra de taxa ativa e vira erro por linha quando não há.
- **Scope**: Módulo `transaction`, tela de Transações (formulário e status) e tela de Importações (três arquivos: transações, liquidação, extrato).
- **Date**: 2026-10-15
- **Status**: active

### AD-012
- **Decision**: Regras de taxa ficam em rota própria (`/taxas`), sempre da empresa selecionada, acessada pelo menu de empresa ("Regras de taxa da <empresa>"), não pela barra superior nem como aba de `/empresas/[id]`.
- **Reason**: A barra superior é do fluxo operacional diário; regra de taxa é configuração da empresa, alterada raramente. Rota própria não depende da página de detalhe de Empresas, então as duas telas podem ser feitas em paralelo.
- **Trade-off**: A tela fica a um clique a mais; outros pontos de entrada (link na lista de `/empresas`, aviso no `409` por falta de regra ativa) entram depois.
- **Scope**: Frontend: `/taxas`, `MerchantMenu`.
- **Note** (2026-10-15): o mesmo princípio vale para Empresas: `/empresas` fica fora da barra superior e é aberta por "Gerenciar empresas" e "Cadastrar empresa" no menu de empresa.
- **Date**: 2026-10-15
- **Status**: active

## Handoff

- **Feature**: register-flow (cadastro no visual novo, e-mail de verificação redesenhado, página `/verificar-email` e `POST /api/auth/resend-verification`) — **mergeada** (PR #56, 2026-10-19, `86cda11`; CI e deploy de produção verdes; em produção, o reenvio responde e aparece no Swagger)
- **Phase / Task**: Verifier PASS na 3ª rodada (`.specs/features/register-flow/validation.md`, diff `48b1ad4..00e5d7f`); 651 testes no backend, 915 no frontend
- **Completed**: itens 1 (login, #55), 2 (cadastro) e 3 (página pós-verificação) da lista da vitrine. Lista restante: (4) landing (desktop no Pen; celular depois), (5) CORS de produção por env, (6) `RECONPAY_VERIFICATION_BASE_URL` em produção apontando para o frontend, (7) projeto na Vercel, (8) remetente do Resend, (9–10) demonstração própria por visitante, (11) tela de usuários do ADMIN. Recuperar senha fica para depois
- **Known issues**: (a) até a Vercel e o item 6, os links dos e-mails de cadastro em produção não abrem; quem ficar preso pede um link novo pelo reenvio depois; (b) bug anterior à feature: `RESEND_API_KEY` vazia ainda ativa o `ResendEmailSender` (500 no cadastro) e `RESEND_API_KEY=false` deixa a API sem `EmailSender`; o modo "só log" do README não funciona; (c) o `.env` local tem `RECONPAY_VERIFICATION_BASE_URL=http://localhost:8080`, que deveria ser `http://localhost:3000`; (d) opcional: `text-balance` no título do `AuthPanel` para equilibrar as quebras como no Pen
- **In-progress** (file:line): none
- **Next step**: item 5, origens de CORS de produção por variável de ambiente; depois Vercel (item 7) e as variáveis do servidor (itens 5 e 6)
- **Blockers**: none
- **Uncommitted files**: none
- **Branch**: `feat/prod-cors-env`, local, sem push, criada do `main` em `86cda11`.
