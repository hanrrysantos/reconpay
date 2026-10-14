# Dashboard Redesign Context

**Gathered:** 2026-10-13
**Spec:** `.specs/features/dashboard-redesign/spec.md`
**Status:** Ready for design

---

## Feature Boundary

O frontend troca o dashboard base pelo dashboard "Lima" desenhado no Paper, ligado a dados reais: período da janela, divergências, desfecho, run, trava e exportação. O que o backend ainda não publica (resumo agregado do período, lista de divergências abertas por diferença e progresso do run) entra nesta feature como tarefas de backend. As outras páginas do nav existem só como rotas vazias.

---

## Implementation Decisions

### Direção visual

- A direção é a variação **Lima** do Paper. Os valores estão em `design-reference.md`.
- O CSS escrito à mão sai; tokens vão para o `@theme` do Tailwind 4.
- Fontes: Inter Tight (display), Inter (interface), IBM Plex Mono (valores).
- Desktop é a referência. Os artboards mobile e as variações "livro-razão" e "âmbar" foram descartados.

### Gráficos

- Anel, área diária, donut, heatmap e sparklines são SVG feito à mão. Nenhuma biblioteca de gráfico entra.

### Dados

- Os blocos agregados leem um endpoint novo de resumo do período, implementado nesta feature. O cálculo de dinheiro fica no servidor.
- O que já existe continua a fonte: período (`/periods`), runs, items, divergências (`GET`/`PATCH`), lock/unlock, export CSV, imports.
- Enquanto um bloco não tiver dado, ele mostra estado honesto, sem número inventado (mantém AD-007).

### Período inicial

- Abre no mês do run mais recente do merchant. Sem run, abre o mês atual no estado vazio.

### Navegação e rotas

- O dashboard fica em `/`.
- Conciliações, Divergências, Transações, Importações e Fechamento existem como rotas vazias "em construção".

### Branch

- `feat/dashboard-redesign` (renomeado de `Hanrry/landing-page`; não havia remoto).

### Agent's Discretion

- Estrutura de componentes, hooks de dados e divisão de arquivos no frontend.
- Forma exata do contrato do resumo, desde que cubra os blocos da referência visual.
- Intervalo de polling do run.

### Declined / Undiscussed Gray Areas → Assumptions

Registradas na tabela de Assumptions da spec: segmentado "7 dias / Setembro / Trimestre", sino de notificações, status por empresa no menu, responsividade, atalho J/K, sinal do "Usar diferença", destino de "Cadastrar empresa" e "Importar extrato".

---

## Specific References

- Paper: arquivo "Welcome to Paper", página "ReconPay", artboards `Dashboard — Variação A · Lima` e `Lima · …` (estados e interações).
- `.specs/features/dashboard-redesign/design-reference.md` transcreve tokens, medidas, textos e mapa de dados.

---

## Deferred Ideas

- Versão mobile dedicada (os artboards mobile existem só na direção descartada).
- Notificações (sino) com fonte real.
- Páginas completas de Conciliações, Divergências, Transações, Importações, Fechamento e cadastro de empresa.
- Landing page pública.
