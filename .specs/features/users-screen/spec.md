# Tela de Usuários Specification

## Problem Statement

A API já administra usuários (`/api/users/**`, só `ADMIN`), mas o frontend não tem tela para isso: criar conta, desativar, reativar e conceder empresas a um operador exige o Swagger. Item 11 da lista da vitrine. A tela também deixa visível a diferença entre os papéis, que hoje só aparece em quais empresas cada um enxerga.

## Goals

- [ ] O ADMIN abre "Usuários" pelo menu do avatar e administra as contas sem usar o Swagger.
- [ ] O ADMIN escolhe, numa lista de caixas de seleção, quais empresas cada operador acessa.
- [ ] Uma conta desativada perde o acesso na hora, inclusive com a sessão já aberta.
- [ ] As até 100 contas de demonstração não poluem a lista.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Trocar o papel de um usuário | Exigiria endpoint novo e a regra do último ADMIN; decisão do usuário |
| Regra "nunca desativar o último ADMIN" | Sem troca de papel, a trava de autodesativação já impede o ADMIN de se trancar fora |
| Recuperar ou trocar senha | Fica para depois; o ADMIN define a senha na criação |
| Busca por nome ou e-mail | Não pedido; a instalação tem poucos usuários |
| Apagar usuário de verdade | O `DELETE` continua sendo desativação (soft delete) |
| Atualizar o nome no avatar ao renomear a si mesmo | O nome novo aparece no próximo login ou ao recarregar a página |
| Papel novo ou dono de merchant | AD-001 e AD-004 |
| Distinguir "cadastro sem confirmação" de "desativado" | Os dois são `active = false`; a tela mostra os dois como "Inativo" |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Escopo | Listar com filtro, criar, renomear, desativar, reativar e editar acessos a empresas | Decisão do usuário | y |
| Entrada da tela | O avatar vira um menu com nome, e-mail, selo do papel, "Usuários" (só ADMIN) e "Sair"; o botão "Sair" ao lado do avatar continua | Decisão do usuário | y |
| Rota | `/usuarios`, dentro do app logado | Segue as rotas em português (`/empresas`, `/taxas`) | n |
| Demos na lista | Escondidas por padrão; "Mostrar demonstrações" as inclui com o selo "Demo · expira em dd/MM às HH:mm" | Decisão do usuário | y |
| Trava de autodesativação | `DELETE /api/users/{id}` do próprio ADMIN responde `409` e a tela esconde "Desativar" na linha dele | Decisão do usuário | y |
| Filtros na API | `GET /api/users` ganha `status` = `ACTIVE` (padrão), `INACTIVE` ou `ALL`, e `demo` = `false` (padrão) ou `true` | Sem os inativos, "Reativar" não tem onde aparecer; `ACTIVE` como padrão mantém quem já chama a API | n |
| Demos fora por padrão também na API | `demo=false` exclui contas com `demo_expires_at` preenchido, mesmo sem filtro na chamada | A API e a tela mostram a mesma lista | n |
| Resposta de usuário | `UserResponseDTO` ganha `demoExpiresAt` (ISO-8601, `null` para conta comum) | A tela precisa do selo | n |
| Ordem da lista | Nome em ordem alfabética, 20 por página | Igual à tela de empresas | n |
| Status mostrado | "Ativo" ou "Inativo"; um cadastro que não confirmou o e-mail aparece como "Inativo" | Os dois casos têm `active = false` | n |
| Reativar cadastro não confirmado | "Reativar" ativa a conta sem confirmar o e-mail, como o `PATCH .../activation` já faz | Comportamento existente, documentado no OpenAPI | n |
| Sessão de conta desativada | Toda requisição com token de conta inativa responde `401` | Hoje o filtro JWT não olha `active`: uma conta desativada continua usando o token por até 24 h | n |
| Acessos de um ADMIN | A linha do ADMIN mostra "Acessa todas as empresas" e não abre o editor | O ADMIN passa por cima dos acessos (`MerchantAccessGuard`) | n |
| Acessos de uma demo | A tela não abre o editor para demo e a API recusa `PUT /api/users/{id}/merchants` de demo com `409` | Conceder empresa real a uma demo expõe dados reais a um visitante anônimo | n |
| Ações numa demo | Só "Desativar"; sem renomear nem editar acessos | A demo é descartável e some sozinha em 24 h | n |
| Empresas no editor | Todas as empresas ativas, em ordem alfabética, marcadas as que o operador já acessa | O ADMIN enxerga todas pela `GET /api/me/merchants` | n |
| Criar usuário | Formulário em painel lateral: nome, e-mail, senha e papel (`OPERATOR` pré-selecionado); a conta nasce ativa, sem e-mail | Contrato existente do `POST /api/users` | n |
| Confirmação antes de desativar | Diálogo de confirmação, como em empresas | Ação que corta o acesso de alguém | n |
| Acesso de OPERATOR à rota | `/usuarios` aberta por OPERATOR redireciona para `/painel` sem chamar `/api/users` | A API já responde `403`; a tela não deve nem tentar | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Menu do avatar ⭐ MVP

**User Story**: Como usuário logado, quero clicar no meu avatar e ver quem sou, com qual papel entrei e, se for ADMIN, o caminho para a tela de usuários.

**Why P1**: É a entrada da tela.

**Acceptance Criteria**:

1. WHEN o usuário clica no avatar THEN o app SHALL abrir um menu com o nome, o e-mail, o selo do papel ("Admin" ou "Operador") e o item "Sair".
2. WHILE o usuário é `ADMIN` the menu SHALL mostrar o item "Usuários", que leva a `/usuarios`.
3. WHILE o usuário é `OPERATOR` the menu SHALL NOT mostrar o item "Usuários".
4. WHEN o usuário clica em "Sair" no menu THEN o app SHALL encerrar a sessão como o botão "Sair" ao lado do avatar.
5. The app SHALL manter o botão "Sair" ao lado do avatar.
6. WHEN o menu está aberto e o usuário aperta `Esc` ou clica fora dele THEN o app SHALL fechar o menu.

**Independent Test**: Entrar como ADMIN, clicar no avatar e ver "Usuários"; entrar como OPERATOR e não ver o item.

---

### P1: Listar usuários ⭐ MVP

**User Story**: Como ADMIN, quero ver todas as contas da instalação, inclusive as inativas, para saber quem tem acesso.

**Why P1**: Sem a lista, nenhuma ação tem onde ficar.

**Acceptance Criteria**:

1. WHEN o ADMIN chama `GET /api/users` sem `status` THEN a API SHALL devolver só usuários ativos.
2. WHEN o ADMIN chama `GET /api/users?status=INACTIVE` THEN a API SHALL devolver só usuários inativos.
3. WHEN o ADMIN chama `GET /api/users?status=ALL` THEN a API SHALL devolver ativos e inativos.
4. IF `status` não é `ACTIVE`, `INACTIVE` nem `ALL` THEN a API SHALL responder `400`.
5. WHEN o ADMIN chama `GET /api/users` sem `demo` ou com `demo=false` THEN a API SHALL excluir contas com `demo_expires_at` preenchido.
6. WHEN o ADMIN chama `GET /api/users?demo=true` THEN a API SHALL incluir as contas de demo que atendem ao `status`.
7. The API SHALL incluir `demoExpiresAt` em toda resposta de usuário: o instante de expiração para demo e `null` para conta comum.
8. WHEN o ADMIN abre `/usuarios` THEN o app SHALL mostrar uma tabela com nome, e-mail, papel, status e data de criação, em ordem alfabética de nome, 20 por página.
9. The app SHALL abrir `/usuarios` com o filtro "Ativos" e sem demonstrações, e SHALL oferecer os filtros "Ativos", "Inativos" e "Todos" e a opção "Mostrar demonstrações".
10. WHILE "Mostrar demonstrações" está ligada the app SHALL mostrar nas linhas de demo o selo "Demo · expira em dd/MM às HH:mm", no horário local do navegador.
11. IF um `OPERATOR` abre `/usuarios` THEN o app SHALL redirecionar para `/painel` sem chamar `/api/users`.
12. WHEN a lista não tem usuários para o filtro THEN o app SHALL mostrar "Nenhum usuário encontrado."

**Independent Test**: Com um ativo, um inativo e uma demo, alternar os filtros e ver cada conta aparecer só onde deve.

---

### P1: Criar usuário ⭐ MVP

**User Story**: Como ADMIN, quero criar a conta de alguém com papel e senha para que a pessoa entre sem passar pelo cadastro público.

**Why P1**: É a ação principal da tela.

**Acceptance Criteria**:

1. WHEN o ADMIN clica em "Novo usuário" THEN o app SHALL abrir um painel lateral com nome, e-mail, senha e papel, com `OPERATOR` pré-selecionado.
2. IF a senha não tem 8 caracteres, uma letra maiúscula e um número THEN o app SHALL mostrar "Senha deve ter no mínimo 8 caracteres, uma letra maiúscula e um número" e SHALL NOT enviar o formulário.
3. IF o nome está vazio ou o e-mail é inválido THEN o app SHALL mostrar o erro no campo e SHALL NOT enviar o formulário.
4. WHEN a API responde `201` THEN o app SHALL fechar o painel, atualizar a lista e mostrar "Usuário criado."
5. IF a API responde `409` (e-mail já cadastrado) THEN o app SHALL mostrar a `message` da resposta no painel e mantê-lo aberto com os dados digitados.
6. WHILE o envio está em andamento the botão de enviar SHALL ficar desabilitado.

**Independent Test**: Criar um OPERATOR, ver a conta na lista e entrar com o e-mail e a senha escolhidos.

---

### P1: Desativar e reativar ⭐ MVP

**User Story**: Como ADMIN, quero desativar uma conta e cortar o acesso na hora, e reativá-la depois.

**Why P1**: Cortar acesso é o motivo principal de ter governança de usuários.

**Acceptance Criteria**:

1. WHEN o ADMIN clica em "Desativar" numa conta ativa THEN o app SHALL pedir confirmação antes de chamar `DELETE /api/users/{id}`.
2. WHEN o ADMIN confirma THEN o app SHALL desativar a conta, atualizar a lista e mostrar "Usuário desativado."
3. IF o ADMIN chama `DELETE /api/users/{id}` com o próprio id THEN a API SHALL responder `409` com código `CONFLICT` e mensagem "Você não pode desativar a própria conta." e SHALL NOT alterar a conta.
4. The app SHALL NOT mostrar "Desativar" na linha do ADMIN logado.
5. WHILE uma conta está inativa, WHEN chega uma requisição com um token emitido para ela THEN a API SHALL responder `401`.
6. WHEN o ADMIN clica em "Reativar" numa conta inativa THEN o app SHALL chamar `PATCH /api/users/{id}/activation`, atualizar a lista e mostrar "Usuário reativado."
7. WHEN uma conta é reativada THEN o login com a senha dela SHALL voltar a funcionar.

**Independent Test**: Com uma sessão aberta de um OPERATOR, desativá-lo e ver a próxima requisição dele responder `401`; reativá-lo e entrar de novo.

---

### P1: Acessos a empresas ⭐ MVP

**User Story**: Como ADMIN, quero marcar quais empresas um operador acessa para compartilhar uma empresa entre operadores.

**Why P1**: É a única forma de compartilhar empresa (AD-004).

**Acceptance Criteria**:

1. WHEN o ADMIN clica em "Acessos" na linha de um `OPERATOR` comum THEN o app SHALL abrir um painel com todas as empresas ativas em ordem alfabética, marcadas as que o operador já acessa.
2. WHEN o ADMIN salva o painel THEN o app SHALL enviar `PUT /api/users/{id}/merchants` com exatamente as empresas marcadas e mostrar "Acessos atualizados."
3. WHEN o ADMIN salva sem nenhuma empresa marcada THEN o app SHALL enviar a lista vazia, e o operador SHALL ficar sem acesso a empresa alguma.
4. WHILE a linha é de um `ADMIN` the app SHALL mostrar "Acessa todas as empresas" e SHALL NOT oferecer "Acessos".
5. IF `PUT /api/users/{id}/merchants` é chamado para uma conta de demo THEN a API SHALL responder `409` com código `CONFLICT` e mensagem "Contas de demonstração não recebem acesso a outras empresas." e SHALL NOT alterar os acessos.
6. WHILE a linha é de uma demo the app SHALL oferecer só "Desativar".
7. WHEN não existe empresa ativa THEN o painel SHALL mostrar "Nenhuma empresa cadastrada."

**Independent Test**: Conceder a um OPERATOR uma empresa criada por outra pessoa, entrar como esse OPERATOR e vê-la na lista de empresas.

---

### P2: Renomear usuário

**User Story**: Como ADMIN, quero corrigir o nome de uma conta.

**Why P2**: Útil, mas raro; a conta funciona com o nome errado.

**Acceptance Criteria**:

1. WHEN o ADMIN clica em "Renomear" numa conta ativa comum THEN o app SHALL abrir um painel com o nome atual.
2. IF o nome está vazio ou passa de 150 caracteres THEN o app SHALL mostrar o erro no campo e SHALL NOT enviar.
3. WHEN a API responde `200` THEN o app SHALL atualizar a lista e mostrar "Usuário renomeado."
4. WHILE a conta está inativa the app SHALL NOT oferecer "Renomear".

**Independent Test**: Renomear um OPERATOR e ver o nome novo na lista.

---

## Edge Cases

- IF o ADMIN desativa um usuário que outra aba já desativou THEN a API SHALL responder `204` de novo, e a lista SHALL mostrar a conta inativa (desativar é idempotente hoje).
- IF uma empresa marcada no painel de acessos é desativada antes de salvar THEN a API SHALL responder `404`, e o app SHALL mostrar a `message` e manter o painel aberto.
- IF a última página fica vazia depois de desativar com o filtro "Ativos" THEN o app SHALL voltar para a página anterior.
- WHEN uma demo expira mas ainda não foi apagada THEN ela SHALL continuar na lista com "Mostrar demonstrações", até a limpeza.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| USERS-01 | P1: Menu do avatar | Tasks | Pending |
| USERS-02 | P1: Listar usuários | Tasks | Pending |
| USERS-03 | P1: Criar usuário | Tasks | Pending |
| USERS-04 | P1: Desativar e reativar | Tasks | Pending |
| USERS-05 | P1: Acessos a empresas | Tasks | Pending |
| USERS-06 | P2: Renomear usuário | Tasks | Pending |

**Coverage:** 6 total, 0 mapped to tasks, 6 unmapped ⚠️

---

## Success Criteria

- [ ] O ADMIN cria um OPERATOR, concede uma empresa e o operador entra e a vê, sem abrir o Swagger.
- [ ] Desativar um usuário derruba a sessão aberta dele na requisição seguinte.
- [ ] Com 100 demos ativas, a lista padrão mostra só as contas comuns.
