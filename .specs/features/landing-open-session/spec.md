# Sessão Aberta na Landing e no Login Specification

## Problem Statement

Em produção (2026-10-21), o usuário abriu uma demonstração, voltou para a landing sem clicar em "Sair" e clicou em "Ver demonstração" de novo. A landing não sabe que há uma sessão aberta, então tentou criar uma segunda conta e o limite por IP recusou. Em seguida, "Entrar" levou direto ao painel da demonstração, porque o login redireciona quem já está logado (LOG-05) sem explicar. O visitante perde a própria demo para o limite e não entende para onde foi.

## Goals

- [ ] Com uma sessão aberta, a landing leva de volta ao painel em vez de criar outra conta.
- [ ] O login com sessão aberta diz que já há uma sessão e deixa escolher entre voltar ao painel e entrar com outra conta.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Backend e limite por IP | O limite funcionou; o problema é a landing chamar a criação sem precisar |
| Tela de cadastro (`/register`) | Não tem redirecionamento de sessão hoje; fica igual |
| Botão "Sair" do painel | Já apaga a sessão e vai para o login |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Fluxo | A tabela "fluxo correto" da conversa de 2026-10-21 | Decisão do usuário ("sim") | y |
| "Sessão aberta" | `useAuth().user` não nulo (demo restaurada do `sessionStorage` ou login na mesma aba) | É o que o painel usa | y |
| Enquanto a sessão é restaurada | A landing e o login mostram o estado sem sessão | Evita esconder os botões em quem não tem sessão; a troca acontece assim que a sessão volta | n |
| Texto na landing com sessão | Botão principal "Voltar ao painel"; "Entrar" e "Entrar na minha conta" viram "Voltar ao painel" no menu e no rodapé e "Sair e entrar com outra conta" nos botões secundários | Não repete dois "Voltar ao painel" lado a lado | n |
| Login com sessão | Aviso "Você já está em uma sessão" com o e-mail, "Ir para o painel" e "Sair e entrar com outra conta" no lugar do formulário | Substitui LOG-05 AC1 (redirecionar sem explicar) | n |
| Depois de um login bem-sucedido | Continua indo para `/painel` sem mostrar o aviso | O aviso é para quem chega já logado | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Voltar à sessão pela landing ⭐ MVP

**User Story**: Como visitante com uma demonstração aberta, quero voltar a ela pela landing sem gastar o limite de demonstrações.

**Why P1**: É o erro visto em produção.

**Acceptance Criteria**:

1. WHILE há sessão aberta THEN o botão "Ver demonstração" SHALL aparecer como "Voltar ao painel" e levar a `/painel` sem chamar `POST /api/auth/demo`.
2. WHILE há sessão aberta THEN os links "Entrar" do menu e do rodapé SHALL aparecer como "Voltar ao painel" e apontar para `/painel`.
3. WHILE há sessão aberta THEN os botões "Entrar na minha conta" da abertura e do convite final SHALL aparecer como "Sair e entrar com outra conta" e, ao clicar, encerrar a sessão e ir para `/login`.
4. WHILE não há sessão THEN a landing SHALL continuar como hoje ("Ver demonstração", "Entrar", "Entrar na minha conta").

**Independent Test**: Com um token de demo válido no `sessionStorage`, abrir `/`: os botões dizem "Voltar ao painel" e clicar não cria conta.

---

### P1: Login com sessão aberta ⭐ MVP

**User Story**: Como usuário que já está logado, quero entender que já estou em uma sessão quando abro o login, em vez de ser jogado no painel.

**Why P1**: Foi o redirecionamento sem explicação que confundiu em produção.

**Acceptance Criteria**:

1. WHEN o login abre com sessão aberta THEN a tela SHALL mostrar "Você já está em uma sessão", o e-mail da conta e as ações "Ir para o painel" (link para `/painel`) e "Sair e entrar com outra conta", sem redirecionar sozinha.
2. WHEN o usuário clica em "Sair e entrar com outra conta" THEN o sistema SHALL encerrar a sessão e mostrar o formulário de login.
3. WHEN o login é enviado com sucesso THEN o sistema SHALL ir para `/painel` sem mostrar o aviso.
4. WHILE não há sessão THEN o login SHALL mostrar o formulário como hoje.

**Independent Test**: Com a sessão da demo aberta, abrir `/login`: aparece o aviso; "Sair e entrar com outra conta" mostra o formulário e apaga o token.

---

## Edge Cases

- WHEN a sessão é de uma conta comum (não demo) THEN a landing e o login SHALL se comportar igual (o aviso e os botões valem para qualquer sessão).
- WHEN o token da demo expirou THEN a restauração já apaga a sessão (comportamento atual), e a landing SHALL mostrar o estado sem sessão.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| SESS-01 | P1: Voltar à sessão pela landing | Execute | Pending |
| SESS-02 | P1: Login com sessão aberta (substitui LOG-05 AC1) | Execute | Pending |

**Coverage:** 2 total, 0 mapped to tasks, 2 unmapped ⚠️

---

## Success Criteria

- [ ] Repetindo o caso de produção, a landing leva de volta à demo sem bater no limite por IP.
