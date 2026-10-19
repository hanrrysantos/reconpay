# Cadastro e Verificação de E-mail Specification

## Problem Statement

O cadastro (`/register`) ainda usa o visual antigo. O e-mail de verificação é um HTML simples, e o link dele abre uma página crua servida pela API (`EmailVerificationPages`). Quando o link expira ou já foi usado, a pessoa fica sem saída: a página manda "cadastrar de novo ou pedir um novo e-mail", mas um novo cadastro com o mesmo e-mail responde `409` ("Email já cadastrado") e não existe como pedir outro link. Para a vitrine pública, cadastro, e-mail e confirmação precisam seguir o visual do login e fechar esse buraco.

Desenhos no Pen: "Cadastro · Desktop", "Cadastro · Desktop · Confirmação", "Cadastro · Mobile", "E-mail · Verificação", "Verificação · Confirmado", "Verificação · Link inválido" e "Verificação · Link reenviado".

## Goals

- [ ] `/register` no visual do login, com as regras da senha marcadas enquanto a pessoa digita e a confirmação no lugar do formulário.
- [ ] E-mail de verificação no visual novo, com o prazo real do link e versão em texto puro.
- [ ] O link do e-mail abre uma página do app que confirma a conta e leva ao login.
- [ ] Link expirado ou usado tem saída: pedir um link novo pelo e-mail.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Recuperar e trocar senha | Decisão do usuário: fica para depois |
| Landing, demonstração, tela de usuários | Outros itens da lista da vitrine |
| Reenvio para contas criadas pelo ADMIN | Essas contas não recebem e-mail de verificação; o ADMIN as ativa (`PATCH /api/users/{id}/activation`) |
| Remover `GET /api/auth/verify-email` e as páginas da API | Links já enviados (válidos por até 24 h) e clientes programáticos continuam funcionando; nenhum e-mail novo aponta para ele |
| Contador de tentativas ou bloqueio por IP no reenvio | Um intervalo mínimo por conta basta para não virar disparo de e-mail; limite por IP entra com a demonstração (item 9) |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Desenho | As sete telas do Pen listadas acima | Aprovado pelo usuário ("ta aprovado", "desenha as páginas e escreve a spec única") | y |
| Escopo único | Cadastro, e-mail e páginas de verificação numa feature só | Decisão do usuário | y |
| Onde fica a confirmação | Página do frontend `/verificar-email?token=...`, que chama o `POST /api/auth/verify-email` já existente | A UI fica no app (mesmo visual, testes com Vitest); a API já tem o POST para clientes programáticos | n |
| Link do e-mail | `<verification-base-url>/verificar-email?token=<token>`; `verification-base-url` (`RECONPAY_VERIFICATION_BASE_URL`) passa a ser o endereço do frontend, padrão `http://localhost:3000` | Reaproveita a propriedade existente; em produção vira o domínio da Vercel (item 6 da lista entra aqui) | n |
| Confirmação automática | A página confirma ao abrir, sem clique, e mostra "Confirmando..." enquanto espera | Mesmo comportamento do link atual (GET confirma) | n |
| Reenvio | `POST /api/auth/resend-verification` com `{ "email" }`, sempre `202` sem corpo, exista a conta ou não | Não revela quais e-mails têm cadastro | n |
| Quem recebe o reenvio | Conta existente, inativa, que já recebeu algum e-mail de verificação (auto-cadastro), nunca confirmou nenhum link e cujo último envio foi há 60 s ou mais; nos outros casos nada é enviado | Contas do ADMIN ficam com o ADMIN. Desativar uma conta é `active=false`, o mesmo estado de quem não confirmou: sem a condição "nunca confirmou", uma conta desativada pelo ADMIN poderia se reativar sozinha pelo reenvio. O intervalo evita disparo repetido | n |
| Link anterior | Um reenvio invalida os links pendentes anteriores da conta | `sendVerificationEmail` já apaga os pendentes | y |
| Prazo no e-mail | "O link vale por N horas e só pode ser usado uma vez.", com N de `verification-token-hours` (padrão 24); 1 → "1 hora" | O prazo é configurável; o token é de uso único (`consumedAt`) | n |
| Texto puro | O e-mail leva também a versão `text` (saudação, instrução, link, prazo e aviso para ignorar) | Clientes sem HTML e filtros de spam | n |
| Fonte no e-mail | Inter com alternativa `Helvetica, Arial, sans-serif`; marca com "✓" em texto, sem SVG | Clientes de e-mail não carregam fonte nem SVG de forma confiável | n |
| Regras da senha | Lista "8 caracteres", "Uma letra maiúscula", "Um número" abaixo do campo; cada uma fica marcada quando cumprida; o campo mantém `minLength` e `pattern` atuais, então o navegador bloqueia o envio | Mesma regra da API (`UserRequestDTO`) | n |
| Erro no cadastro | A mensagem da API num aviso `role="alert"` acima do botão (por exemplo "Email já cadastrado: ana@..."); sem mensagem, "Não foi possível criar a conta. Tente novamente." | Comportamento atual, visual do login | y |
| Quebra para o celular | Igual ao login: abaixo de 1024 px, faixa escura no topo; entre 1024 e 1279 px painel de 440 px | Mesmo componente visual | n |
| Painel compartilhado | O painel escuro vira um componente usado por `/login` e `/register`, com título e texto por página | Evita duas cópias do mesmo bloco | n |
| `auth.module.css` | Removido: nenhuma tela usa mais | Fim do visual antigo | n |

**Open questions:** none - all resolved or logged above.

### Implicit-requirement sweep

| Dimension | Coverage |
| --- | --- |
| Input validation & bounds | CAD-02 (obrigatórios, regra da senha no navegador e na API); RSD-01 AC2 (e-mail vazio ou inválido responde `400`) |
| Failure / partial-failure states | CAD-04 (erro da API e rede); VER-02 (link inválido); VER-03 AC3 (falha no reenvio); MAIL-01 AC4 (falha do provedor não muda o comportamento atual) |
| Idempotency / retry / duplicate handling | CAD-03 AC2 (um envio por vez); RSD-01 AC4 (intervalo de 60 s); VER-01 AC3 (link usado de novo cai em "não vale mais") |
| Auth / authorization boundaries | RSD-01 AC3 (só auto-cadastro que nunca confirmou; conta desativada não se reativa); RSD-01 AC1 (resposta igual exista ou não a conta); VER-04 (rotas públicas) |
| Concurrency / state transitions | Conta inativa → ativa só pelo token (VER-01); reenvio troca o token pendente (RSD-01 AC5) |
| Data lifecycle / expiry | MAIL-01 AC2 (prazo real); VER-02 (expirado) |
| Persistence / external calls | MAIL-01 (Resend); RSD-01 (novo token) |
| Accessibility | CAD-02 AC3 (botão de senha com nome), CAD-02 AC4 (regras com estado legível), CAD-04 AC1 (`role="alert"`), VER-01 AC1 (`role="status"`) |
| Responsiveness | CAD-01 AC2 |

---

## User Stories

### P1: Visual do cadastro ⭐ MVP

**User Story**: As a visitante, I want um cadastro no mesmo visual do login so that a vitrine pareça um produto só.

**Why P1**: Motivo da feature.

**Acceptance Criteria**:

1. WHEN `/register` abre em 1024 px ou mais THEN the system SHALL mostrar o painel escuro com a marca, o título "Seu primeiro fechamento começa aqui.", o texto "Crie a conta, cadastre a empresa e importe os arquivos do mês. A conta é ativada pelo link que enviamos por e-mail." e o rodapé do login, ao lado do formulário com o título "Criar conta" e o subtítulo "Você confirma o e-mail antes de entrar.".
2. WHEN `/register` abre abaixo de 1024 px THEN the system SHALL mostrar a faixa escura com a marca e o título acima do formulário, sem o texto e o rodapé do painel.
3. WHEN `/login` abre THEN the system SHALL continuar com o título e o texto atuais no painel.

**Independent Test**: Abrir `/register` no desktop e no celular e comparar com o Pen.

---

### P1: Formulário de cadastro

**User Story**: As a visitante, I want saber na hora se minha senha serve so that eu não descubra só depois de enviar.

**Why P1**: Entrada do produto.

**Acceptance Criteria**:

1. WHEN a tela abre THEN the system SHALL mostrar os campos "Nome completo" (placeholder "Seu nome"), "E-mail" ("voce@empresa.com") e "Senha" ("Crie uma senha"), o botão "Criar conta" e "Já tem conta?" com o link "Entrar" para `/login`.
2. WHERE os campos aparecem the system SHALL marcar os três como obrigatórios, com `autocomplete` `name`, `email` e `new-password`, o e-mail com `type="email"` e a senha com `minLength` 8 e o padrão de uma maiúscula e um número.
3. WHEN o usuário aciona "Mostrar senha" THEN the system SHALL mostrar a senha como texto e trocar o botão para "Ocultar senha" (`aria-pressed="true"`), mantendo o que foi digitado.
4. WHILE o usuário digita a senha the system SHALL marcar como cumprida cada regra atendida, "8 caracteres" (8 ou mais), "Uma letra maiúscula" (A–Z) e "Um número" (0–9), e como pendente as demais, com o estado legível por leitor de tela.

**Independent Test**: Digitar `reconpay20` e ver só "Uma letra maiúscula" pendente; digitar `Reconpay20` e ver as três cumpridas.

---

### P1: Enviar o cadastro

**User Story**: As a visitante, I want criar a conta e saber o que fazer depois so that eu chegue ao primeiro login.

**Why P1**: Sem isso não há conta.

**Acceptance Criteria**:

1. WHEN o usuário envia THEN the system SHALL chamar `POST /api/auth/register` com nome, e-mail e senha digitados.
2. WHILE o envio está em andamento the system SHALL mostrar o botão desabilitado com "Criando conta..." e não fazer outro envio.
3. WHEN a API responde `201` THEN the system SHALL trocar o formulário pela confirmação: título "Confira seu e-mail", "Enviamos um link de verificação para <e-mail>. Abra o e-mail e confirme a conta antes de entrar.", o botão "Ir para o login" (`/login`) e "Não chegou? Confira a caixa de spam.".

**Independent Test**: Cadastrar e ver a confirmação com o e-mail usado.

---

### P1: Erro no cadastro

**User Story**: As a visitante, I want saber por que o cadastro falhou so that eu corrija.

**Why P1**: E-mail repetido é comum.

**Acceptance Criteria**:

1. IF o cadastro falha com mensagem THEN the system SHALL mostrar a mensagem num aviso `role="alert"` acima do botão, mantendo os três campos.
2. IF o cadastro falha com algo que não é `Error` THEN the system SHALL mostrar "Não foi possível criar a conta. Tente novamente.".
3. WHEN o usuário envia de novo THEN the system SHALL tirar o aviso antes do novo envio.

**Independent Test**: Cadastrar um e-mail já usado e ver "Email já cadastrado: ...".

---

### P1: E-mail de verificação

**User Story**: As a visitante, I want um e-mail claro e confiável so that eu confirme a conta sem dúvida.

**Why P1**: É o caminho para a primeira entrada.

**Acceptance Criteria**:

1. WHEN o e-mail é enviado THEN the system SHALL usar o assunto "Confirme seu e-mail — ReconPay" e um HTML com a marca, "Olá, <nome>!", o título "Confirme seu e-mail", o texto com o e-mail da conta, o botão "Confirmar e-mail" com o link, o prazo, o endereço do link por extenso e o aviso "Não criou uma conta no ReconPay? Ignore este e-mail: nada será ativado.".
2. WHERE o prazo aparece the system SHALL usar `verification-token-hours`: "O link vale por 24 horas e só pode ser usado uma vez." no padrão, e "1 hora" quando o valor é 1.
3. WHEN o e-mail é enviado THEN the system SHALL enviar também a versão em texto puro com a saudação, a instrução, o link, o prazo e o aviso para ignorar.
4. WHERE nome ou e-mail entram no HTML the system SHALL escapar `<`, `>`, `&`, `"` e `'`.
5. WHERE o link é montado the system SHALL usar `<verification-base-url>/verificar-email?token=<token>`.

**Independent Test**: Cadastrar em `dev` e ler o e-mail registrado no log (`LoggingEmailSender`) ou recebido pelo Resend.

---

### P1: Página de confirmação

**User Story**: As a visitante, I want abrir o link e ver a conta ativada so that eu entre em seguida.

**Why P1**: Fecha o cadastro.

**Acceptance Criteria**:

1. WHEN `/verificar-email?token=<t>` abre THEN the system SHALL mostrar "Confirmando..." (`role="status"`) e chamar `POST /api/auth/verify-email` com o token uma única vez.
2. WHEN a API responde `204` THEN the system SHALL mostrar "E-mail confirmado", "Sua conta está ativa. Entre com o e-mail e a senha que você cadastrou." e o botão "Ir para o login" (`/login`).
3. IF a API recusa o token (`400`) ou a página abre sem token THEN the system SHALL mostrar "Este link não vale mais", "Ele expirou ou já foi usado. Informe seu e-mail e enviamos um link novo.", o formulário de reenvio e "Já confirmou?" com "Entrar".
4. IF a chamada falha por rede ou outro erro THEN the system SHALL mostrar a mensagem do erro com "Tentar de novo", que repete a confirmação.

**Independent Test**: Abrir o link do e-mail e ver "E-mail confirmado"; abrir de novo e ver "Este link não vale mais".

---

### P1: Pedir um link novo

**User Story**: As a visitante com link vencido, I want pedir outro link so that eu não fique preso fora da conta.

**Why P1**: Hoje não há saída.

**Acceptance Criteria**:

1. WHEN o usuário envia o e-mail no formulário de reenvio THEN the system SHALL chamar `POST /api/auth/resend-verification` e, com `202`, mostrar "Confira seu e-mail" e "Se <e-mail> tiver um cadastro aguardando confirmação, o link novo chega em alguns minutos. Não chegou? Confira a caixa de spam.".
2. WHILE o reenvio está em andamento the system SHALL desabilitar "Enviar novo link" com "Enviando..." e não fazer outro envio.
3. IF o reenvio falha THEN the system SHALL mostrar a mensagem num aviso `role="alert"`, mantendo o e-mail digitado.

**Independent Test**: Pedir um link novo para uma conta não confirmada e receber o e-mail.

---

### P1: Reenvio na API

**User Story**: As a operador da plataforma, I want um reenvio que não exponha cadastros nem vire spam so that a saída seja segura.

**Why P1**: Endpoint público.

**Acceptance Criteria**:

1. WHEN `POST /api/auth/resend-verification` recebe um e-mail válido THEN the system SHALL responder `202` sem corpo, exista ou não a conta.
2. IF o e-mail está vazio ou inválido THEN the system SHALL responder `400` `VALIDATION_ERROR`.
3. WHERE a conta existe, está inativa, já recebeu e-mail de verificação e nunca confirmou nenhum link the system SHALL enviar um e-mail novo; IF a conta não existe, está ativa, nunca recebeu e-mail de verificação ou já confirmou um link antes (conta desativada depois de ativa) THEN the system SHALL não enviar nada.
4. IF o último e-mail de verificação da conta saiu há menos de 60 segundos THEN the system SHALL não enviar outro.
5. WHEN um e-mail novo é enviado THEN the system SHALL invalidar os links pendentes anteriores da conta.
6. WHERE o endpoint é documentado the system SHALL descrevê-lo no Swagger na tag "Authentication", com exemplos de `202` e `400`, como os outros endpoints públicos.

**Independent Test**: `curl` no endpoint com e-mail existente e inexistente: ambos `202`; só o existente recebe e-mail.

---

### P2: Rotas públicas

**User Story**: As a visitante sem sessão, I want abrir o cadastro e o link sem login so that o fluxo funcione antes da primeira entrada.

**Why P2**: Garantia de acesso.

**Acceptance Criteria**:

1. WHEN `/register` ou `/verificar-email` abre sem sessão THEN the system SHALL mostrar a página, sem mandar para `/login`.

**Independent Test**: Abrir as duas rotas numa janela anônima.

---

## Edge Cases

- WHEN o token da URL tem espaços nas pontas THEN the system SHALL enviá-lo como veio (a API já faz `trim`).
- WHEN a página de confirmação é aberta duas vezes seguidas (por exemplo, React em modo estrito) THEN the system SHALL chamar a API uma vez só.
- WHEN o e-mail do reenvio tem espaços nas pontas THEN the system SHALL enviá-lo sem eles.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| CAD-01 | P1: Visual do cadastro | Execute | Implemented |
| CAD-02 | P1: Formulário de cadastro | Execute | Implemented |
| CAD-03 | P1: Enviar o cadastro | Execute | Implemented |
| CAD-04 | P1: Erro no cadastro | Execute | Implemented |
| MAIL-01 | P1: E-mail de verificação | Execute | Implemented |
| VER-01 | P1: Página de confirmação (AC 1, 2) | Execute | Implemented |
| VER-02 | P1: Página de confirmação (AC 3, 4) | Execute | Implemented |
| VER-03 | P1: Pedir um link novo | Execute | Implemented |
| RSD-01 | P1: Reenvio na API | Execute | Implemented |
| VER-04 | P2: Rotas públicas | Execute | Implemented |

**ID format:** `<ÁREA>-<NN>`; o critério é citado como `CAD-02 AC4`. VER-01 cobre os critérios 1 e 2 da story, VER-02 os critérios 3 e 4.

**Coverage:** 10 total, 10 mapped to tasks (T1–T8), 0 unmapped

---

## Success Criteria

- [ ] Cadastro, e-mail e páginas de verificação batem com o Pen.
- [ ] Um link vencido tem saída sem ajuda de ninguém.
- [ ] O reenvio responde igual para e-mail cadastrado e não cadastrado.
