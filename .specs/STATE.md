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

### AD-013
- **Decision**: Conta de demonstração é um `OPERATOR` comum marcado por `users.demo_expires_at` (atributo, não papel), criada por `POST /api/auth/demo` com cenário fixo, limitada a 100 ativas e 2 por IP por hora, com cotas de 2 merchants e 10 importações, e apagada por limpeza agendada depois de 24 h.
- **Reason**: A vitrine precisa de demo em um clique sem criar papel novo (AD-001) nem dono de merchant (AD-004); o isolamento sai dos acessos em `user_merchants`.
- **Trade-off**: Limite por IP em memória (instância única, zera ao reiniciar); a limpeza precisa conhecer toda tabela ligada a merchant (teste de catálogo protege).
- **Scope**: Módulo `demo`, `UserDetailsServiceImpl`, `MerchantService.create` e os três `importCsv` (cotas), landing.
- **Date**: 2026-10-20
- **Status**: active

### AD-014
- **Decision**: Exceção ao AD-009: o token de uma sessão de **demo** fica em `sessionStorage` e sobrevive ao recarregar; o token de login comum continua só em memória.
- **Reason**: O visitante não conhece a senha da conta gerada; perder a sessão ao recarregar gastaria uma das 2 demos por hora.
- **Trade-off**: Token de demo legível por script da página (XSS); aceito porque a conta é descartável e expira em 24 h.
- **Scope**: `frontend/lib/auth.tsx`, `AppShell`.
- **Date**: 2026-10-20
- **Status**: active

## Handoff

- **Feature**: users-screen (item 11 da vitrine: tela de usuários do ADMIN)
- **Phase / Task**: T1–T14 commitadas; Verifier PASS na rodada 2 (rodada 1 FAIL por falta de teste dos edge cases 1 e 4, corrigida em `bd2d2a6`); teste manual aprovado pelo usuário em 2026-10-20
- **Completed**: backend (filtros `status`/`demo`, `demoExpiresAt`, trava de autodesativação, trava de grant para demo, `401` para token de conta inativa), `lib/api.ts` e `lib/queries.ts`, menu do avatar, `/usuarios` (lista, criar, desativar, acessos, renomear, reativar) e documentação. Depois do PASS, pedidos do teste manual, sem nova rodada do Verifier: cursor de mãozinha em todo controle clicável (`899b5ae`, regra global em `app/globals.css`) e "Voltar para o início" no login e no cadastro, com a marca do painel levando a `/` (`70b89f2`)
- **Known issues**: os da demo continuam (ver PR #61): `docker-compose.yaml` não repassa `DEMO_*`; `RESEND_API_KEY` vazia dá 500 no cadastro; L-058..L-061 candidatas
- **In-progress** (file:line): none
- **Next step**: PR de `feat/users-screen` aberta e mergeada pelo usuário; depois conferir CI e deploy. Pendente da vitrine: recuperar senha
- **Blockers**: none
- **Uncommitted files**: none
- **Branch**: `feat/users-screen`, enviada para o `origin`
