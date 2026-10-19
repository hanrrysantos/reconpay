# Login Redesign Specification

## Problem Statement

A tela `/login` ainda usa o visual antigo (`app/auth.module.css`), diferente do resto do app, que já segue o visual lima (Inter Tight, superfícies `#F4F5F1`, botões arredondados). Com a vitrine pública, o login é uma das primeiras telas que um visitante vê. O desenho novo está no Pen (telas "Login · Desktop", "Login · Desktop · Erro" e "Login · Mobile"): painel escuro com a marca e uma frase, formulário à direita, e no celular uma faixa escura no topo com o formulário embaixo.

## Goals

- [ ] `/login` com o visual do desenho no desktop e no celular, com os tokens do app.
- [ ] Mostrar e ocultar a senha.
- [ ] Manter o comportamento atual de entrar, errar e já estar logado.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Cadastro (`/register`) e página pós-verificação de e-mail | Itens 2 e 3 da lista; desenho próprio depois deste |
| Landing e "Ver demonstração" | Itens 4, 9 e 10 da lista |
| Recuperar senha ("Esqueci minha senha") | Decisão do usuário: fica para depois; não há endpoint |
| Trocar o texto de erro da API ("Credenciais inválidas") | Decisão do usuário: o desenho usa a mensagem que a API devolve |
| "Lembrar de mim" ou sessão persistida | A sessão vive em memória (`AuthProvider`); refresh token fora de escopo (`AGENTS.md`) |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Desenho | Telas do Pen "Login · Desktop", "Login · Desktop · Erro" e "Login · Mobile", com o painel escuro sem o resumo de match | Aprovado pelo usuário ("login mais leve ok") | y |
| Texto do painel | Marca "reconpay" (quadrado lima com check, como na barra do app), título "Do vendido ao que caiu na conta, sem planilha.", texto "Cruze vendas, liquidações e extrato numa janela, trate cada divergência e trave o período com rastro para auditoria." e rodapé "Conciliação financeira para fintechs, gateways e marketplaces" | Desenho aprovado | y |
| Texto do formulário | Título "Entrar"; subtítulo "Use o e-mail e a senha da sua conta ReconPay."; campos "E-mail" (placeholder "voce@empresa.com") e "Senha" (placeholder "Sua senha"); botão "Entrar"; "Ainda não tem conta?" com o link "Criar conta" para `/register` | Desenho aprovado | y |
| Quebra para o celular | Abaixo de 1024 px (`lg` do Tailwind): faixa escura no topo com a marca e o título, sem o texto e o rodapé do painel; formulário embaixo em coluna única. Entre 1024 e 1279 px o painel tem 440 px e o título 40 px; a partir de 1280 px, 640 px e 52 px como no desenho | Desenho "Login · Mobile"; o painel lateral não cabe em tablet estreito, e com 640 px fixos o formulário ficaria com 272 px em 1024 | n |
| Mostrar senha | Botão com ícone de olho dentro do campo, nome acessível "Mostrar senha" / "Ocultar senha", `aria-pressed`; alterna o campo entre `password` e `text` | Desenho tem o olho; nome acessível porque o botão só tem ícone | n |
| Erro | A mensagem do erro num aviso `role="alert"` acima do botão. Falha de rede já chega com mensagem própria do cliente ("Não foi possível conectar à API. Tente novamente."); só uma falha que não é `Error` mostra "Não foi possível entrar. Tente novamente." | Comportamento atual | y |
| Envio em andamento | Botão desabilitado com "Entrando..."; um clique a mais não gera outro POST | Comportamento atual | n |
| Estilo | Tailwind com os tokens de `globals.css`; `auth.module.css` continua só para `/register` até o item 2 | Mesmo padrão das outras telas | n |

**Open questions:** none - all resolved or logged above.

### Implicit-requirement sweep

| Dimension | Coverage |
| --- | --- |
| Input validation & bounds | LOG-02 AC2 (campos obrigatórios e `type="email"`, validação do navegador); a API valida o resto |
| Failure / partial-failure states | LOG-04 (mensagem da API, falha de rede) |
| Idempotency / retry / duplicate handling | LOG-03 AC2 (um POST por envio) |
| Auth / authorization boundaries | LOG-05 (já logado vai para `/`); a sessão segue no `AuthProvider` sem mudança |
| Concurrency / state transitions | N/A because a tela não tem estado compartilhado além da sessão |
| Data lifecycle / expiry | N/A because nada é gravado pela tela |
| Persistence / external calls | LOG-03 AC1 (`signIn` atual, `POST /api/auth/login`) |
| Accessibility | LOG-02 AC1 (rótulos), LOG-02 AC3 (botão de senha com nome), LOG-04 AC1 (`role="alert"`) |
| Responsiveness | LOG-01 AC2 |

---

## User Stories

### P1: Visual novo ⭐ MVP

**User Story**: As a visitante, I want ver um login no mesmo visual do app so that a vitrine pareça um produto só.

**Why P1**: É o motivo da feature.

**Acceptance Criteria**:

1. WHEN `/login` abre em 1024 px ou mais THEN the system SHALL mostrar o painel escuro com a marca, o título, o texto e o rodapé do painel ao lado do formulário com o título "Entrar" e o subtítulo.
2. WHEN `/login` abre abaixo de 1024 px THEN the system SHALL mostrar a faixa escura com a marca e o título acima do formulário, sem o texto e o rodapé do painel.

**Independent Test**: Abrir `/login` no desktop e no celular e comparar com as telas do Pen.

---

### P1: Formulário

**User Story**: As a usuário, I want preencher e-mail e senha com rótulos claros e ver a senha que digitei so that eu entre sem errar.

**Why P1**: Entrada principal do app.

**Acceptance Criteria**:

1. WHEN a tela abre THEN the system SHALL mostrar os campos com os rótulos "E-mail" e "Senha", os placeholders "voce@empresa.com" e "Sua senha", e o botão "Entrar".
2. WHERE os campos aparecem the system SHALL marcar os dois como obrigatórios, o de e-mail com `type="email"` e `autocomplete="email"`, e o de senha com `autocomplete="current-password"`.
3. WHEN o usuário aciona "Mostrar senha" THEN the system SHALL mostrar a senha como texto e trocar o botão para "Ocultar senha" (`aria-pressed="true"`); WHEN aciona "Ocultar senha" THEN the system SHALL voltar a esconder a senha.
4. WHEN o usuário aciona "Criar conta" THEN the system SHALL levar a `/register`.

**Independent Test**: Digitar uma senha, mostrar, ocultar e seguir para o cadastro.

---

### P1: Entrar

**User Story**: As a usuário, I want entrar com e-mail e senha so that eu chegue ao dashboard.

**Why P1**: Sem isso não há app.

**Acceptance Criteria**:

1. WHEN o usuário envia o formulário THEN the system SHALL chamar `signIn` com o e-mail e a senha digitados.
2. WHILE o envio está em andamento the system SHALL mostrar o botão desabilitado com "Entrando..." e não fazer outro envio.
3. WHEN `signIn` conclui THEN the system SHALL ir para `/`.

**Independent Test**: Entrar com o usuário seed de `dev` e cair no dashboard.

---

### P1: Erro ao entrar

**User Story**: As a usuário, I want saber por que não entrei so that eu corrija.

**Why P1**: Senha errada é o caso mais comum.

**Acceptance Criteria**:

1. IF `signIn` falha com mensagem THEN the system SHALL mostrar a mensagem num aviso `role="alert"` acima do botão, mantendo o e-mail e a senha digitados.
2. IF `signIn` falha sem mensagem (por exemplo, erro que não é `Error`) THEN the system SHALL mostrar "Não foi possível entrar. Tente novamente.".
3. WHEN o usuário envia de novo THEN the system SHALL tirar o aviso antes do novo envio.

**Independent Test**: Errar a senha, ver "Credenciais inválidas" e entrar na tentativa seguinte.

---

### P2: Já logado

**User Story**: As a usuário logado, I want não ver o login de novo so that eu volte direto ao app.

**Why P2**: Comportamento atual; evita tela inútil.

**Acceptance Criteria**:

1. WHEN `/login` abre com uma sessão ativa THEN the system SHALL ir para `/`.

**Independent Test**: Entrar, abrir `/login` e voltar ao dashboard.

---

## Edge Cases

- WHEN o usuário aciona "Mostrar senha" THEN the system SHALL manter o que já foi digitado.
- WHEN o envio falha e o usuário corrige só a senha THEN the system SHALL enviar o e-mail mantido e a senha nova.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| LOG-01 | P1: Visual novo | Execute | Verified |
| LOG-02 | P1: Formulário | Execute | Verified |
| LOG-03 | P1: Entrar | Execute | Verified |
| LOG-04 | P1: Erro ao entrar | Execute | Verified |
| LOG-05 | P2: Já logado | Execute | Verified |

**ID format:** `LOG-<NN>` por story; o critério é citado como `LOG-02 AC3`.

**Coverage:** 5 total, 5 mapped (sem `tasks.md`: escopo médio, uma tela e um arquivo de teste)

---

## Success Criteria

- [ ] `/login` no desktop e no celular bate com as telas do Pen.
- [ ] Entrar, errar e já estar logado funcionam como antes, com testes.
