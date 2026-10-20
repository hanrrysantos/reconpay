# Nova Marca Specification

## Problem Statement

O ReconPay usa um quadrado verde-limão com ✓ e o nome "reconpay" em minúsculas. O usuário criou uma marca nova: um R sem haste com uma folha verde-limão no lugar dela, o nome "ReconPay" em Manrope com "Pay" em destaque e a frase "Conciliação financeira com clareza". O quadro existe só como imagem (PNG de 1536 px), sem vetores; o símbolo foi redesenhado em SVG a partir das medidas dele.

## Goals

- [ ] A marca nova aparece em todo lugar onde a antiga aparece: aba do navegador, login e cadastro, confirmação de e-mail, barra do painel, landing e e-mail de verificação.
- [ ] Um só componente desenha a marca no frontend, para a próxima troca ser em um lugar.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Trocar as cores do produto ou a fonte do texto | A marca mantém o verde-limão `#C6F135` e o preto `#0B0D0A`; só o nome usa Manrope |
| Imagem de compartilhamento (Open Graph) e ícones de app para instalar (PWA) | Não pedidos |
| Mudar o domínio de envio do e-mail | É o item 8 da vitrine |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Desenho do símbolo | SVG redesenhado sobre o quadro, com as bordas medidas linha a linha (`frontend/public/brand/`) | Não há vetor original | y |
| Fonte do nome | Manrope 800, hospedada pelo `@fontsource-variable/manrope` (sem rede no build), só no nome | Fonte informada pelo usuário; o projeto já hospeda as fontes assim | y |
| Escrita | "ReconPay" em todo lugar, inclusive no texto alternativo e no e-mail | Como no quadro | y |
| "Pay" em verde | Só em fundo escuro (painel do login e cadastro, faixa do e-mail); em fundo claro, "ReconPay" todo em preto | Verde-limão some em tamanho pequeno sobre branco (decisão do usuário) | y |
| Frase "Conciliação financeira com clareza" | Só no painel escuro do login e cadastro e no rodapé da landing | Decisão do usuário | y |
| Símbolo por fundo | Fundo escuro: R branco e folha verde; fundo claro: R preto e folha verde | Como no quadro | y |
| Ícone da aba | Quadrado preto arredondado com o R branco e a folha verde, em SVG | Como o ícone escuro do quadro; lê bem a 16 px | n |
| Imagem no e-mail | PNG do ícone (quadrado preto com o símbolo, 2× o tamanho exibido) servido pelo frontend em `<RECONPAY_VERIFICATION_BASE_URL>/brand/reconpay-app-icon.png`, com `alt=""` porque o nome vem escrito ao lado; o `reconpay-mark-on-dark.png` continua publicado para os e-mails já enviados | Clientes de e-mail não mostram SVG de forma confiável; o fundo próprio mantém o símbolo visível quando o Gmail inverte a faixa no modo escuro (aprovado pelo usuário em 2026-10-19) | y |
| Prévia do painel na landing | Usa a mesma marca da barra do painel | A prévia imita o painel | y |

**Open questions:** none - all resolved or logged above.

### Implicit-requirement sweep

| Dimension | Coverage |
| --- | --- |
| Input validation & bounds | N/A because não há entrada |
| Failure / partial-failure states | BRD-03 AC2 (imagem bloqueada no e-mail) |
| Idempotency / retry / duplicate handling | N/A because não há ação |
| Auth / authorization boundaries | N/A because a marca é pública |
| Concurrency / state transitions | N/A because não há estado |
| Data lifecycle / expiry | N/A because nada é gravado |
| Persistence / external calls | BRD-03 AC1 (imagem servida pelo frontend) |
| Accessibility | BRD-02 AC4 |
| Responsiveness | BRD-02 AC5 |

---

## User Stories

### P1: Arquivos da marca ⭐ MVP

**User Story**: As a quem mantém o projeto, I want o símbolo em arquivos próprios so that a marca seja a mesma em todo lugar.

**Why P1**: Sem os arquivos não há o que aplicar.

**Acceptance Criteria**:

1. WHERE o frontend é publicado the system SHALL servir em `/brand/` o símbolo para fundo escuro e para fundo claro em SVG, e o PNG do símbolo para fundo escuro usado no e-mail.
2. WHEN o navegador mostra o ícone da aba THEN the system SHALL usar o quadrado preto arredondado com o R branco e a folha verde.
3. WHEN o nome aparece THEN the system SHALL usar Manrope 800, carregada junto com as outras fontes do app, sem buscar nada na rede durante o build.

**Independent Test**: Abrir `/brand/` e a aba do navegador.

---

### P1: Marca no app ⭐ MVP

**User Story**: As a visitante ou usuário, I want ver a marca nova em todas as telas so that o produto pareça um só.

**Why P1**: É o pedido.

**Acceptance Criteria**:

1. WHEN o login, o cadastro ou o painel escuro deles aparece THEN the system SHALL mostrar o símbolo para fundo escuro, "Recon" em branco e "Pay" em verde-limão, e a frase "Conciliação financeira com clareza".
2. WHEN a confirmação de e-mail, a barra do painel, o menu da landing ou a prévia do painel aparece THEN the system SHALL mostrar o símbolo para fundo claro e "ReconPay" todo em preto, sem a frase.
3. WHEN o rodapé da landing aparece THEN the system SHALL mostrar o símbolo para fundo claro, "ReconPay" em preto e a frase.
4. WHERE a marca aparece the system SHALL deixar o símbolo fora da leitura de tela (decorativo) e o nome "ReconPay" como texto; na barra do painel e na landing, o link da marca SHALL se chamar "ReconPay" e manter o destino de hoje (`/painel` e `/`).
5. WHEN a tela tem de 320 a 1920 px THEN the system SHALL mostrar a marca sem cortar o nome e sem rolagem horizontal.

**Independent Test**: Abrir `/`, `/login`, `/register`, `/verificar-email` e `/painel` e comparar com o quadro.

---

### P1: Marca no e-mail ⭐ MVP

**User Story**: As a pessoa que se cadastrou, I want reconhecer a marca no e-mail so that eu confie no link.

**Why P1**: O e-mail é o primeiro contato fora do site.

**Acceptance Criteria**:

1. WHEN o e-mail de verificação é montado THEN the system SHALL mostrar na faixa escura do topo a imagem `<RECONPAY_VERIFICATION_BASE_URL sem barra final>/brand/reconpay-app-icon.png` (32 × 32 px: o quadrado preto arredondado com o R branco e a folha verde) seguida de "Recon" em branco e "Pay" em verde-limão. *(Revisado em 2026-10-19: o PNG transparente de 28 px sumia no modo escuro do Gmail no celular, que inverte a faixa mas não as imagens.)*
2. WHEN o cliente de e-mail bloqueia imagens THEN the system SHALL continuar mostrando o nome "ReconPay" escrito, com a imagem marcada como decorativa (`alt=""`).
3. WHEN a versão em texto puro é montada THEN the system SHALL escrever "ReconPay".

**Independent Test**: Gerar o e-mail no log de dev e abrir o HTML.

---

### P2: Desenhos

**User Story**: As a quem desenha as próximas telas, I want o Pen com a marca nova so that o desenho e o código não divirjam.

**Why P2**: Evita voltar à marca antiga num desenho novo.

**Acceptance Criteria**:

1. WHERE o Pen mostra a marca (landing de desktop e de celular, login, cadastro e confirmação) the system SHALL usar a marca nova com as mesmas regras de BRD-02.

**Independent Test**: Comparar as telas do Pen com o quadro.

---

## Edge Cases

- WHEN a fonte Manrope ainda não carregou THEN the system SHALL mostrar o nome na fonte de reserva do sistema, sem esconder o texto (`font-display: swap`).

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| BRD-01 | P1: Arquivos da marca | Execute | Verified |
| BRD-02 | P1: Marca no app | Execute | Verified |
| BRD-03 | P1: Marca no e-mail | Execute | Needs Fix |
| BRD-04 | P2: Desenhos | Execute | Verified |

**ID format:** `BRD-<NN>`; o critério é citado como `BRD-02 AC4`.

**Coverage:** 4 total, 4 mapped to tasks

---

## Success Criteria

- [ ] Nenhuma tela, ícone ou e-mail mostra mais o ✓ ou "reconpay" em minúsculas.
