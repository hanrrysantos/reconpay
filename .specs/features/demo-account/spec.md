# Demonstração por Visitante Specification

## Problem Statement

A vitrine pública só deixa testar o ReconPay criando conta, confirmando o e-mail e montando merchant, taxas, CSVs e conciliação do zero. Quem chega pela landing quer ver o produto funcionando em um clique. Itens 9–10 da lista da vitrine: uma conta temporária por visitante, já com dados, isolada das outras e apagada depois de 24 h.

## Goals

- [ ] Um clique em "Ver demonstração" na landing abre o painel logado, com conciliações e divergências já prontas.
- [ ] Cada visitante tem sua própria conta e não vê dados de outro visitante.
- [ ] O banco e a VPS ficam protegidos: no máximo 100 demos ativas, 2 por IP por hora, cotas de escrita por conta.
- [ ] Contas expiradas e seus dados somem sem intervenção manual.

## Out of Scope

| Feature | Reason |
| --- | --- |
| CAPTCHA | Decisão do usuário: só se aparecer abuso real |
| Limite por IP distribuído (Redis etc.) | Instância única; o AGENTS.md proíbe inventar infraestrutura distribuída |
| `limit_req` no nginx do host | Fora do repositório; o limite fica na aplicação |
| Converter demo em conta real | Não pedido; quem quiser conta usa o cadastro |
| Período fechado no cenário | Fecharia as divergências do mês para o visitante; ele pode fechar um período sozinho |
| Esconder demos na listagem de usuários do ADMIN | A tela de usuários é o item 11 |
| Papel novo para visitante | AD-001: só `ADMIN` e `OPERATOR`; a demo é um atributo do usuário |
| Campo `owner_id` em merchant | AD-004 |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Origem dos dados | Cenário fixo no código, gerado pelos serviços do domínio, datas relativas ao dia da criação, CNPJ válido gerado na hora | Decisão do usuário (variação de A) | y |
| Limites de criação | Teto global de 100 demos não expiradas (no banco) + 2 criações por IP por hora (em memória) | Decisão do usuário | y |
| Papel e cotas | `OPERATOR` com no máximo 2 merchants concedidos e 10 importações próprias por conta de demo | Decisão do usuário (opção C) | y |
| Sessão da demo | Token da demo em `sessionStorage`; login comum continua só em memória (AD-009) | Decisão do usuário | y |
| Validade | 24 h a partir da criação; o JWT da demo nunca vale além disso | Plano da vitrine | y |
| Marca de demo | Coluna `demo_expires_at` em `users` (nula para contas comuns) | Atributo, não papel (AD-001) | y |
| Endpoint | `POST /api/auth/demo`, público, sem corpo; responde `201` com o mesmo formato do login (`token`, `type`, `expiresIn`) | Reaproveita o contrato que o frontend já consome | n |
| IP do visitante | Primeiro endereço de `X-Forwarded-For` só quando a requisição vem de proxy confiável (`server.forward-headers-strategy` no profile `prod`); senão o IP da conexão | Sem isso todos os visitantes dividem o IP do nginx, ou o cliente forja o cabeçalho. O envio do cabeçalho pelo nginx da VPS será confirmado antes do deploy | n |
| Janela por IP | Janela deslizante de 60 min; só criações bem-sucedidas contam | Recusas não consomem a cota de quem tentou | n |
| Conta da demo | E-mail `demo-<aleatório>@demo.reconpay.invalid`, nome "Visitante", senha aleatória nunca exposta, `active=true`, nenhum e-mail enviado | `.invalid` é reservado (RFC 2606); ninguém entra pela tela de login | n |
| Criação serializada | Uma criação de demo por vez na instância | Garante o teto de 100 sem corrida e limita a carga na VPS | n |
| Pronto ao responder | O `201` só sai com o cenário gravado e as conciliações do cenário `COMPLETED` | O painel abre cheio, sem tela de espera | n |
| Cotas: o que conta | Merchants concedidos ao usuário (o do cenário conta como 1); importações feitas pelo visitante nos três endpoints de CSV (transações, liquidações, extrato); as importações do cenário não contam | O visitante cria 1 merchant e faz 10 importações | n |
| Recusa por cota | `403` `FORBIDDEN` com mensagem específica | Não é erro de dado (409) nem de sessão (401) | n |
| Códigos novos | `TOO_MANY_REQUESTS` (`429`) e `SERVICE_UNAVAILABLE` (`503`) em `ApiErrorCode` | Formato `StandardError` | n |
| Limpeza | Tarefa agendada a cada 15 min apaga demos expiradas | 15 min de atraso não importa: a conta expirada já não autentica | n |
| O que a limpeza apaga | O usuário de demo e todo merchant em que todos os usuários com acesso são demos expiradas, com todos os dados do merchant | Um merchant real concedido por engano a uma demo nunca é apagado; só o acesso da demo some | n |
| Landing | "Ver demonstração" substitui "Criar conta" no herói e no CTA final; o menu continua com "Entrar" e "Criar conta" | O cadastro real segue acessível | n |
| Aviso de demo no app | Faixa "Conta de demonstração · expira às HH:mm" no topo do app; `GET /api/me` passa a expor `demoExpiresAt` | O visitante sabe que os dados são temporários | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Abrir uma demonstração ⭐ MVP

**User Story**: Como visitante da landing, quero abrir uma conta de demonstração com um clique para explorar o ReconPay com dados reais sem me cadastrar.

**Why P1**: É o objetivo dos itens 9–10.

**Acceptance Criteria**:

1. WHEN `POST /api/auth/demo` é chamado sem autenticação THEN o sistema SHALL responder `201` com `token`, `type` = `Bearer` e `expiresIn` ≤ 86400.
2. WHEN uma demo é criada THEN o sistema SHALL gravar um usuário `OPERATOR`, ativo, com `demo_expires_at` = instante da criação + 24 h e e-mail terminado em `@demo.reconpay.invalid`.
3. WHEN uma demo é criada THEN o sistema SHALL NOT enviar e-mail.
4. WHEN uma demo é criada THEN o sistema SHALL conceder ao usuário exatamente 1 merchant, com CNPJ válido e diferente de todos os merchants existentes.
5. WHEN o `201` é respondido THEN o merchant da demo SHALL ter regra de taxa ativa para `CREDIT_CARD`, `DEBIT_CARD`, `PIX` e `BOLETO`.
6. WHEN o `201` é respondido THEN o merchant da demo SHALL ter transações, liquidações e lançamentos de extrato com datas nos 60 dias anteriores ao dia da criação e nenhuma data posterior a ele.
7. WHEN o `201` é respondido THEN toda conciliação do merchant da demo SHALL estar `COMPLETED`, com itens `MATCHED` e `DIVERGENT`.
8. WHEN o `201` é respondido THEN o merchant da demo SHALL ter ao menos uma divergência `OPEN` de cada tipo: `MISSING_SETTLEMENT`, `ORPHAN_SETTLEMENT`, `INCORRECT_AMOUNT`, `FEE_DIVERGENCE` e `MISSING_BANK_CREDIT`.
9. The system SHALL gerar o mesmo cenário em toda demo criada no mesmo dia: mesma quantidade de transações, liquidações, lançamentos e divergências, e mesmo valor bruto total.
10. IF a geração do cenário falha em qualquer passo THEN o sistema SHALL responder `500` e SHALL NOT deixar usuário, merchant ou dado da demo gravado.
11. The system SHALL impedir que o token de uma demo leia ou altere merchant de outro usuário (`403`, como hoje para `OPERATOR`).

**Independent Test**: Chamar `POST /api/auth/demo`, usar o token em `GET /api/merchants`, no resumo do painel e nas divergências, e ver o merchant da demo com conciliações e divergências.

---

### P1: Limitar a criação

**User Story**: Como dono da instalação, quero limitar quantas demos existem e quantas cada IP cria para que um script não esgote o banco nem a VPS.

**Why P1**: Sem limite, o endpoint público é uma porta aberta para encher o banco.

**Acceptance Criteria**:

1. IF já existem 100 demos não expiradas THEN o sistema SHALL responder `503` com código `SERVICE_UNAVAILABLE` e mensagem "Demonstrações esgotadas no momento. Tente novamente mais tarde." e SHALL NOT criar a demo.
2. The system SHALL NOT contar demos expiradas, mesmo que ainda não apagadas, no teto de 100.
3. IF o mesmo IP já criou 2 demos nos últimos 60 minutos THEN o sistema SHALL responder `429` com código `TOO_MANY_REQUESTS` e mensagem "Limite de demonstrações atingido. Tente novamente em até 1 hora." e SHALL NOT criar a demo.
4. WHEN uma criação do mesmo IP sai da janela de 60 minutos THEN o sistema SHALL voltar a aceitar uma criação daquele IP.
5. The system SHALL NOT contar no limite do IP as tentativas recusadas (`429`, `503`, `500`).
6. WHERE o profile é `prod` the system SHALL usar como IP do visitante o primeiro endereço de `X-Forwarded-For` enviado pelo proxy confiável.
7. IF a requisição não vem de proxy confiável THEN o sistema SHALL ignorar `X-Forwarded-For` e usar o IP da conexão.
8. The system SHALL processar uma criação de demo por vez, de modo que criações simultâneas nunca passem de 100 demos não expiradas.

**Independent Test**: Criar 2 demos do mesmo IP e ver a terceira recusada com `429`; preencher 100 demos e ver a seguinte recusada com `503`.

---

### P1: Cotas da conta de demo

**User Story**: Como dono da instalação, quero limitar o que uma conta de demo grava para que um token gratuito não vire escrita ilimitada.

**Why P1**: A conta não exige e-mail verificado e vale 24 h.

**Acceptance Criteria**:

1. WHILE a conta de demo tem 2 merchants concedidos, WHEN ela chama `POST /api/merchants` THEN o sistema SHALL responder `403` com código `FORBIDDEN` e mensagem "A demonstração permite no máximo 2 empresas." e SHALL NOT criar o merchant.
2. WHILE a conta de demo já fez 10 importações próprias, WHEN ela envia um CSV de transações, liquidações ou extrato THEN o sistema SHALL responder `403` com código `FORBIDDEN` e mensagem "A demonstração permite no máximo 10 importações." e SHALL NOT importar.
3. The system SHALL NOT contar as importações feitas pelo cenário na cota de 10.
4. The system SHALL NOT aplicar as cotas de demo a contas comuns.

**Independent Test**: Com um token de demo, criar 1 merchant (aceito) e um segundo (`403`); fazer 10 importações (aceitas) e a 11ª (`403`).

---

### P1: Expiração e limpeza

**User Story**: Como dono da instalação, quero que demos expiradas parem de funcionar e sejam apagadas com os dados delas.

**Why P1**: Sem isso, o teto de 100 só cresce e os dados acumulam.

**Acceptance Criteria**:

1. WHILE `demo_expires_at` já passou, WHEN uma requisição chega com o token dessa demo THEN o sistema SHALL responder `401`.
2. WHEN a limpeza roda THEN o sistema SHALL apagar todo usuário de demo expirado.
3. WHEN a limpeza apaga uma demo THEN o sistema SHALL apagar todo merchant em que todos os usuários com acesso são demos expiradas, com transações, importações, liquidações, extratos, regras de taxa, conciliações, divergências e períodos desse merchant.
4. IF um merchant concedido a uma demo expirada também está concedido a uma conta comum THEN a limpeza SHALL manter o merchant e seus dados e remover só o acesso da demo.
5. The system SHALL NOT apagar demos ainda não expiradas.
6. The system SHALL executar a limpeza a cada 15 minutos.
7. WHEN a limpeza termina THEN o sistema SHALL registrar no log quantos usuários e merchants apagou.

**Independent Test**: Criar uma demo, avançar o relógio 24 h, rodar a limpeza e ver usuário, merchant e dados apagados; ver uma demo não expirada intacta.

---

### P1: "Ver demonstração" na landing

**User Story**: Como visitante, quero um botão na landing que me leve ao painel da demo.

**Why P1**: É a entrada da feature.

**Acceptance Criteria**:

1. The landing SHALL mostrar "Ver demonstração" no herói e no CTA final, no lugar de "Criar conta"; o menu SHALL continuar com "Entrar" (`/login`) e "Criar conta" (`/register`).
2. WHEN o visitante clica em "Ver demonstração" THEN o frontend SHALL chamar `POST /api/auth/demo` uma única vez, mesmo com cliques repetidos.
3. WHILE a criação está em andamento the botão clicado SHALL mostrar "Preparando demonstração..." e ficar desabilitado.
4. WHEN a API responde `201` THEN o frontend SHALL abrir `/painel` autenticado com o token da demo.
5. IF a API responde `429` ou `503` THEN o frontend SHALL mostrar a `message` da resposta junto ao botão e reabilitá-lo.
6. IF a API falha sem `message` (rede ou `500`) THEN o frontend SHALL mostrar "Não foi possível abrir a demonstração. Tente novamente." e reabilitar o botão.

**Independent Test**: Na landing, clicar em "Ver demonstração" e cair em `/painel` com o merchant da demo.

---

### P1: Sessão da demo sobrevive ao recarregar

**User Story**: Como visitante, quero recarregar a página sem perder a demo, porque não sei a senha da conta gerada.

**Why P1**: Sem isso, recarregar gasta uma das 2 demos por hora.

**Acceptance Criteria**:

1. WHEN uma demo é aberta THEN o frontend SHALL guardar o token em `sessionStorage`.
2. WHEN a página é recarregada com um token de demo em `sessionStorage` THEN o frontend SHALL restaurar a sessão sem pedir login.
3. The frontend SHALL NOT gravar em `sessionStorage` nem em `localStorage` o token de um login comum.
4. WHEN o visitante sai (logout) THEN o frontend SHALL apagar o token de `sessionStorage`.
5. IF o token restaurado é recusado com `401` THEN o frontend SHALL apagar o token de `sessionStorage` e levar à landing (`/`).

**Independent Test**: Abrir a demo, recarregar `/painel` e continuar logado; entrar com conta comum, recarregar e voltar ao login.

---

### P2: Aviso de conta de demonstração

**User Story**: Como visitante, quero saber que estou numa demo e quando ela expira.

**Why P2**: Evita surpresa quando os dados somem, mas a demo funciona sem isso.

**Acceptance Criteria**:

1. WHEN `GET /api/me` é chamado por uma demo THEN o sistema SHALL incluir `demoExpiresAt` (ISO-8601) na resposta.
2. WHEN `GET /api/me` é chamado por uma conta comum THEN o sistema SHALL responder `demoExpiresAt` = `null`.
3. WHILE a sessão é de demo the app SHALL mostrar no topo "Conta de demonstração · expira às HH:mm", no horário local do navegador.
4. WHILE a sessão é de conta comum the app SHALL NOT mostrar a faixa de demo.

**Independent Test**: Abrir a demo e ver a faixa com o horário; entrar com conta comum e não ver a faixa.

---

## Edge Cases

- IF dois visitantes clicam ao mesmo tempo com 99 demos ativas THEN o sistema SHALL criar uma e recusar a outra com `503` (DEMO-04 AC8).
- IF a API reinicia THEN o limite por IP SHALL recomeçar do zero, e o teto global SHALL continuar valendo porque vem do banco.
- IF a criação do CNPJ gerado colide com um existente THEN o sistema SHALL gerar outro antes de gravar (DEMO-01 AC4).
- WHEN a demo é criada perto da meia-noite THEN as datas do cenário SHALL usar o dia da criação no fuso da aplicação, sem data futura (DEMO-01 AC6).

---

## Implicit-Requirement Sweep

| Dimension | Coverage |
| --- | --- |
| Input validation & bounds | Endpoint sem corpo; cotas em DEMO-05 |
| Failure / partial-failure | DEMO-01 AC10 (rollback), DEMO-07 AC5–6 |
| Idempotency / retry | DEMO-07 AC2 (clique repetido); cada `POST` aceito cria uma demo nova por definição |
| Auth boundaries & rate limits | DEMO-01 AC11, DEMO-03, DEMO-04, DEMO-05 |
| Concurrency / ordering | DEMO-04 AC8 |
| Data lifecycle / expiry | DEMO-06 |
| Observability | DEMO-06 AC7; criação registrada pelo `AuditLogger` existente, sem IP no log |
| External-dependency failure | N/A because a demo não chama serviço externo (nenhum e-mail) |
| State-transition integrity | DEMO-06 AC1 (expirada não autentica) |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| DEMO-01 | P1: Abrir uma demonstração (AC1–4, 10–11) | Design | Pending |
| DEMO-02 | P1: Abrir uma demonstração (cenário, AC5–9) | Design | Pending |
| DEMO-03 | P1: Limitar a criação (IP, AC3–7) | Design | Pending |
| DEMO-04 | P1: Limitar a criação (teto, AC1–2, 8) | Design | Pending |
| DEMO-05 | P1: Cotas da conta de demo | Design | Pending |
| DEMO-06 | P1: Expiração e limpeza | Design | Pending |
| DEMO-07 | P1: "Ver demonstração" na landing | Design | Pending |
| DEMO-08 | P1: Sessão da demo sobrevive ao recarregar | Design | Pending |
| DEMO-09 | P2: Aviso de conta de demonstração | Design | Pending |

**Coverage:** 9 total, 0 mapped to tasks, 9 unmapped ⚠️ (tasks vêm depois do design)

---

## Success Criteria

- [ ] Da landing ao painel com dados em um clique, sem cadastro.
- [ ] Nenhuma demo enxerga dados de outra.
- [ ] O banco nunca passa de 100 demos ativas, e nada de demo expirada sobrevive mais de 15 min além da limpeza seguinte.
