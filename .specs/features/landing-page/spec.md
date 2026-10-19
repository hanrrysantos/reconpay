# Landing Page Specification

## Problem Statement

O frontend está público em `https://reconpay.vercel.app`, mas quem abre o endereço cai no painel autenticado, que manda direto para o login. Ninguém fica sabendo o que o ReconPay faz. A landing já está desenhada no Pen (`Landing · Desktop` e `Landing · Mobile`) e precisa virar a página inicial, com animações ligadas ao scroll que também voltam quando a pessoa rola para cima.

## Goals

- [ ] Quem abre `/` vê a landing do Pen, no desktop e no celular.
- [ ] As seções entram conforme o scroll e desfazem a entrada quando a pessoa volta.
- [ ] O painel continua funcionando, em outro endereço.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Conta de demonstração e o botão "Ver demonstração" | É o item 9–10 da vitrine; até lá os botões principais levam ao cadastro |
| Barra de navegação fixa ao rolar | Não está no desenho |
| Tema escuro, outros idiomas | Não estão no desenho |
| Imagem de compartilhamento (Open Graph) | Fica para depois; título e descrição já entram |
| Analytics e formulário de contato | Não pedidos |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Endereço do painel | O painel sai de `/` e vai para `/painel`; o login leva a `/painel` | A landing precisa ser o endereço raiz | y |
| Landing para quem já entrou | A landing aparece para todos; "Entrar" leva ao login, que já manda quem tem sessão para `/painel` | Sem mudar o fluxo de sessão | y |
| Botões de demonstração | Viram "Criar conta" (`/register`); o texto do CTA final vira "Crie sua conta e concilie o primeiro mês." | A demonstração ainda não existe (combinado antes) | y |
| Link "Fechamento" do menu | Leva à prévia do painel ("Faltam 12 divergências para fechar setembro") | Não há seção própria de fechamento no desenho | y |
| Tecnologia das animações | CSS ligado ao scroll (`animation-timeline: view()`), sem biblioteca e sem JavaScript; a posição do scroll define o progresso, então voltar desfaz a animação | Funciona no Chrome, Edge e Safari 26; nada a carregar | y |
| Navegador sem suporte (Firefox hoje) | Conteúdo aparece parado e visível, sem animação | Nada pode ficar escondido sem animação | y |
| Prévia do painel | Desenho estático com os números do Pen, não os componentes reais do painel | Os componentes do painel dependem de dados da API | y |
| Gráficos e números | O anel, a barra do passo 2 e os números ligados a eles crescem de zero até o valor do desenho com o mesmo progresso do scroll, e voltam a zero ao subir; os números usam um pequeno script que lê a posição do bloco na tela | Pedido do usuário (2026-10-19); CSS sozinho não formata `R$ 121.212,50` | y |
| Links do rodapé | GitHub `https://github.com/hanrrysantos/reconpay`, Documentação da API `https://reconpay.hanrry.top/swagger-ui/index.html`, Entrar `/login` | Endereços públicos atuais | y |

**Open questions:** none - all resolved or logged above.

### Implicit-requirement sweep

| Dimension | Coverage |
| --- | --- |
| Input validation & bounds | N/A because a página não tem formulário |
| Failure / partial-failure states | LAND-04 AC3 (navegador sem suporte), LAND-05 AC2 (sem JavaScript) |
| Idempotency / retry / duplicate handling | N/A because não há ação que grave |
| Auth / authorization boundaries | LAND-01 AC1–AC3 (landing pública, painel protegido) |
| Concurrency / state transitions | N/A because a página não tem estado |
| Data lifecycle / expiry | N/A because nada é gravado |
| Persistence / external calls | N/A because a landing não chama a API |
| Accessibility | LAND-04 AC4 (movimento reduzido), LAND-05 AC1 |
| Responsiveness | LAND-03 AC1–AC3 |

---

## User Stories

### P1: Landing no endereço raiz ⭐ MVP

**User Story**: As a visitante, I want ver o que o ReconPay faz ao abrir o site so that eu decida criar uma conta.

**Why P1**: Hoje o endereço público só mostra o login.

**Acceptance Criteria**:

1. WHEN alguém abre `/`, com ou sem sessão THEN the system SHALL mostrar a landing, sem chamar a API.
2. WHEN alguém com sessão abre `/painel` THEN the system SHALL mostrar o painel que hoje está em `/`; sem sessão, SHALL levar ao login, como as outras telas autenticadas.
3. WHEN o login dá certo, ou quem já tem sessão abre `/login` THEN the system SHALL levar a `/painel`; a marca e "Visão geral" da barra do painel SHALL levar a `/painel`.

**Independent Test**: Abrir `/` sem sessão mostra a landing; entrar leva a `/painel`.

---

### P1: Conteúdo do desenho ⭐ MVP

**User Story**: As a visitante, I want ler o problema, o fluxo e o resultado so that eu entenda o produto em um minuto.

**Why P1**: É o conteúdo da página.

**Acceptance Criteria**:

1. WHEN a landing abre THEN the system SHALL mostrar, nesta ordem, os textos do Pen `Landing · Desktop`: menu, abertura com a prévia do painel, "O valor que cai na conta quase nunca é o da venda." com as quatro distorções (Taxa, Parcela, Atraso, Estorno), "Três passos por período, do arquivo ao fechamento." com os três passos, "Cada diferença tem dono, decisão e histórico." com as três decisões e o ajuste de exemplo, o convite final e o rodapé.
2. WHEN o visitante usa um botão principal ("Criar conta" no menu, na abertura e no convite final) THEN the system SHALL abrir `/register`; "Entrar" no menu e no rodapé e "Entrar na minha conta" SHALL abrir `/login`.
3. WHEN o visitante usa "Como funciona", "Divergências" ou "Fechamento" no menu THEN the system SHALL rolar até a seção dos passos, a das divergências e a prévia do painel.
4. WHEN o visitante usa "GitHub" ou "Documentação da API" no rodapé THEN the system SHALL abrir os endereços da tabela de decisões.

**Independent Test**: Comparar a página com o Pen e clicar em cada link.

---

### P1: Celular ⭐ MVP

**User Story**: As a visitante no celular, I want ler a página sem rolar para os lados so that a vitrine funcione em qualquer tela.

**Why P1**: Boa parte das visitas vem de link aberto no celular.

**Acceptance Criteria**:

1. WHEN a tela tem menos de 768 px THEN the system SHALL seguir o Pen `Landing · Mobile`: menu só com a marca, "Entrar" e "Criar conta"; botões em coluna e na largura toda; seções em uma coluna; a prévia sem as abas, sem os indicadores ao lado do anel e sem o botão "Resolver" das linhas.
2. WHEN a tela tem de 320 px a 1920 px THEN the system SHALL não ter rolagem horizontal nem texto cortado.
3. WHEN a tela tem 768 px ou mais THEN the system SHALL seguir o Pen `Landing · Desktop`, com o conteúdo centrado em até 1440 px; entre os dois desenhos, o que não cabe entra por largura: as âncoras do menu, as abas e o anel ao lado da tabela a partir de 1024 px, os indicadores a partir de 1280 px e o botão "Resolver" a partir de 1400 px.
4. WHEN a tela tem menos de 380 px THEN the system SHALL tirar "Entrar" do menu (fica "Entrar na minha conta" logo abaixo) e pôr o tipo de cada divergência da prévia numa segunda linha.

**Independent Test**: Abrir a página a 390 px e a 1440 px e comparar com os dois desenhos.

---

### P1: Animações ligadas ao scroll ⭐ MVP

**User Story**: As a visitante, I want que as seções ganhem vida conforme eu rolo, inclusive voltando so that a página pareça viva sem atrapalhar a leitura.

**Why P1**: Pedido explícito do usuário.

**Acceptance Criteria**:

1. WHEN um bloco animado (títulos das seções, cada distorção, cada passo, cada decisão, o ajuste de exemplo e o convite final) entra na tela pelo scroll THEN the system SHALL levá-lo de transparente e deslocado para baixo até a posição final e opaco, com o progresso dado pela posição do scroll; os itens de uma mesma lista SHALL entrar um após o outro.
2. WHEN o visitante rola de volta e o bloco sai pela parte de baixo da tela THEN the system SHALL desfazer a entrada pelo mesmo caminho, até o estado inicial.
3. WHEN o navegador não suporta animação ligada ao scroll THEN the system SHALL mostrar todos os blocos parados, opacos e na posição final.
4. WHEN o sistema pede movimento reduzido (`prefers-reduced-motion: reduce`) THEN the system SHALL mostrar todos os blocos parados, opacos e na posição final.
5. WHEN o anel da taxa de match e a barra "Conciliando setembro" entram na tela pelo scroll THEN the system SHALL construí-los de vazio até o valor do desenho (94,2% e 964 de 1.284), com o progresso dado pela posição do scroll, e desfazê-los ao voltar.
6. WHEN esses gráficos crescem THEN the system SHALL fazer os números ligados a eles crescer de zero até o valor do desenho no mesmo progresso, no formato brasileiro (`94,2%`, `1.209 de 1.284 itens`, `R$ 10.389,38`, `R$ 121.212,50`, `R$ 3.787,50`, `964 de 1.284`, `Conciliados 912`, `Divergentes 52`), assim como as contagens de linhas do passo 1 (`1.284`, `1.271`, `312`); leitores de tela SHALL ouvir só o valor final.
7. WHEN o navegador não suporta animação ligada ao scroll, o sistema pede movimento reduzido ou o JavaScript não roda THEN the system SHALL mostrar os gráficos cheios e os números no valor final.
8. WHEN a página abre THEN the system SHALL animar a abertura (selo, título, texto, botões) uma vez, por tempo; e a prévia do painel SHALL subir conforme o scroll, desfazendo ao voltar.

**Independent Test**: No Chrome, rolar até o fim e voltar: cada bloco entra ao descer e desfaz ao subir; o anel e a barra enchem com os números crescendo e esvaziam ao subir.

---

### P2: Acessibilidade e busca

**User Story**: As a visitante com leitor de tela ou buscador, I want a página bem estruturada so that eu navegue por títulos e ela apareça na busca.

**Why P2**: Vitrine pública.

**Acceptance Criteria**:

1. WHERE a landing é mostrada the system SHALL ter um único `h1` (o título da abertura), um `h2` por seção, as regiões `nav`, `main` e `footer`, e a prévia do painel como uma imagem com descrição ("Prévia do painel de fechamento do ReconPay").
2. WHEN a página é pedida THEN the system SHALL entregar o conteúdo pronto no HTML, legível sem JavaScript, com título "ReconPay · Conciliação financeira" e descrição sobre cruzar vendas, liquidação e extrato.

**Independent Test**: Navegar pelos títulos com o leitor de tela; abrir a página com JavaScript desligado.

---

## Edge Cases

- WHEN o visitante abre a página já no meio (link com `#divergencias` ou recarga) THEN the system SHALL mostrar os blocos já visíveis no estado certo para aquela posição, sem esperar rolar.
- WHEN a pessoa com sessão abre um link antigo para `/` THEN the system SHALL mostrar a landing, e "Entrar" a leva ao painel sem pedir senha.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| LAND-01 | P1: Landing no endereço raiz | Execute | Verified |
| LAND-02 | P1: Conteúdo do desenho | Execute | Verified |
| LAND-03 | P1: Celular | Execute | Verified |
| LAND-04 | P1: Animações ligadas ao scroll | Execute | Verified |
| LAND-05 | P2: Acessibilidade e busca | Execute | Verified |

**ID format:** `LAND-<NN>`; o critério é citado como `LAND-04 AC2`.

**Coverage:** 5 total, 0 mapped to tasks

---

## Success Criteria

- [ ] `https://reconpay.vercel.app` abre a landing no desktop e no celular.
- [ ] Rolar até o fim e voltar anima cada seção nos dois sentidos no Chrome e no Safari.
