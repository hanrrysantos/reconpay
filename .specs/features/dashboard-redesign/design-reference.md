# Dashboard Redesign — Visual Reference

Fonte: arquivo Paper "Welcome to Paper", página "ReconPay" (fileId `01M4VVBKC5DAKGK0XFFK0P0WA5`). Direção escolhida: **Lima**. Este arquivo transcreve os valores do design para que a implementação não dependa do acesso ao Paper. Em conflito, o Paper vence; corrija este arquivo.

Artboards de referência:

| Artboard | Papel |
| --- | --- |
| `Dashboard — Variação A · Lima` | Estado principal (período aberto, 12 divergências) |
| `Lima · Estado — Carregando` | Skeleton |
| `Lima · Estado — Run em andamento` | Run `PENDING`/`RUNNING` |
| `Lima · Estado — Vazio` | Sem run no período + variante sem empresa |
| `Lima · Estado — Erro` | Erro de rede, run `FAILED`, erro de um bloco |
| `Lima · Estado — Período travado` | Janela travada, somente leitura |
| `Lima · Interação — Resolver divergência` | Gaveta lateral |
| `Lima · Interação — Empresa e período` | Menus abertos |

Os artboards "livro-razão", "Mobile" e "Variação B · Painel âmbar" são alternativas descartadas.

---

## Tokens (Tailwind v4 `@theme`)

```css
@theme {
  --color-background: #FFFFFF;      /* fundo da página */
  --color-surface: #F4F5F1;         /* blocos bento, trilho de pills */
  --color-subtle: #F8F9F5;          /* linha destacada dentro de superfície branca */
  --color-border: #E6E8E1;          /* divisores, gridlines */
  --color-foreground: #0B0D0A;      /* texto, pill ativa, card invertido */
  --color-muted: #6B6F66;           /* texto secundário */
  --color-primary: #C6F135;         /* limão: ação principal; sempre com texto foreground */
  --color-primary-soft: #E2F89A;    /* etiquetas, heatmap 97–98% */
  --color-success: #4F6B00;         /* limão escuro: texto/linha positiva sobre branco */
  --color-danger: #E5484D;          /* divergência, valor faltando */
  --color-danger-soft: #FFE5E6;     /* fundo de etiqueta de divergência */
  --color-danger-strong: #B4232A;   /* texto sobre danger-soft */
  --color-warning: #B7791F;         /* pendente */

  --font-display: "Inter Tight";    /* números e títulos, peso 800 */
  --font-sans: "Inter";             /* interface */
  --font-mono: "IBM Plex Mono";     /* valores, referências, datas, eixos */

  --radius-sm: 10px;
  --radius-md: 16px;
  --radius-lg: 24px;
  --radius-full: 999px;
}
```

Cores auxiliares usadas pontualmente (não viraram token): `#FF7A7E` (número "abertas" sobre o card preto), `#B9BDB2` (cinza do donut, borda pontilhada de anotação), `#EEF0E9`/`#EBEDE6` (skeleton), `#EEF8C9` (skeleton do card limão), `#D3D6CD` (borda tracejada de erro de bloco), `#8A2A2E` (texto secundário do banner de erro). Sobre o card preto: textos em `rgba(255,255,255,0.6–0.75)`, superfícies em `rgba(255,255,255,0.07–0.12)`, trilho do anel `rgba(255,255,255,0.1)`.

Regra de contraste: texto vermelho nunca sobre limão; limão nunca como cor de texto sobre branco (usar `success`).

## Tipografia

| Uso | Fonte | Tamanho / peso / tracking |
| --- | --- | --- |
| Manchete da página | display | 44px / 800 / -0.04em / line-height 1 |
| Valor grande de card (KPI, gráfico) | display | 30–32px / 800 / -0.03em |
| Percentual no anel | display | 34px / 800 / -0.04em |
| Título de bloco | display | 22px / 800 / -0.03em |
| Wordmark `reconpay` | display | 20px / 800 / -0.04em |
| Texto de interface | sans | 13–15px / 400–600 |
| Rótulo pequeno | sans | 12px |
| Valores monetários, referências, datas, eixos | mono | 11–22px / 400–600 |

Moeda: `R$ 10.389,38` no card; na tabela, sem `R$` (`4.870,00`, `−4.870,00` com sinal de menos U+2212). Nunca passar valor por float (manter `lossless-json`).

## Layout

- Página: largura de referência 1440px, padding `24px 40px 48px`, coluna com `gap: 24px`. Sem sidebar.
- **Top nav** (altura ~40px): wordmark (quadrado 30px limão, raio 10, check preto) · trilho de pills (`surface`, padding 4, raio full) com itens 14px, pill ativa `foreground`/branco · à direita: seletor de empresa (pill `surface`, 40px, botão branco 28px com chevron), sino (círculo 40px `surface`), avatar (40px, `foreground`, iniciais limão).
- **Cabeçalho**: à esquerda eyebrow 14px `muted` + manchete; à direita segmentado (7 dias / Setembro / Trimestre; item ativo branco com sombra `0 1px 2px rgba(0,0,0,.08)`) + botão primário limão 44px "Conciliar período" com ícone de refresh.
- **Régua de fechamento** (linha própria, gap 8): 4 passos em pill (padding `8px 14px 8px 8px`, círculo 24px), ligados por traços 24×2px (`foreground` se concluído, `border` se não).
  - Concluído: fundo `surface`, círculo `foreground` com check limão.
  - Atual: fundo `primary`, círculo branco com número mono.
  - Pendente: contorno `inset 0 0 0 1px border`, ícone de cadeado `muted`.
  - Textos: título 13px/600 + subtítulo 12px, `white-space: nowrap`.
- **Linha 1** (gap 16): card preto 420px + gráfico (flex 1). Raio 24, padding 28.
- **Linha 2** (gap 16): 3 KPIs (flex 1, padding 24) + donut 420px.
- **Linha 3** (gap 16, alinhado ao topo): tabela (flex 1) + heatmap 420px.

## Componentes

### Card de conciliação (preto)
- Cabeçalho: "Taxa de conciliação" 14px + badge limão "+1,8 pp vs ontem" (12px/700, raio full).
- Anel SVG 168px: `r=70`, `stroke-width=16`, trilho `rgba(255,255,255,.1)`, arco limão `stroke-linecap=round`, `dasharray = rate × 439.8`, rotação -90°. Centro: percentual + "conciliado" 12px.
- Coluna à direita: 3 números mono 22px/600 com legenda 12px: bateram, resolvidas, abertas (abertas em `#FF7A7E`).
- Rodapé: faixa `rgba(255,255,255,.07)` raio 16 com texto 13px e botão circular limão 32px (seta ↗).

### Gráfico Esperado × recebido no banco (SVG à mão)
- Bloco `surface`, padding 28, gap 20. Rótulo 14px `muted`, valor display 32px + delta mono 13px `danger`.
- Legenda à direita (12px): tracejado preto "Líquido esperado" · quadrado limão "Caiu no banco" · quadrado vermelho "Faltou".
- Área de plotagem 200px de altura: eixo Y à esquerda (30px, mono 11px, 0/5k/10k/15k/20k) + SVG com `viewBox 0 0 720 200`, `preserveAspectRatio=none`.
  - Gridlines `border` 1px.
  - Área recebida: limão com opacidade 0.45.
  - **Faixa de falta**: polígono entre a linha esperada e a recebida, `danger` com opacidade 0.55.
  - Linha recebida `success` 2px; linha esperada `foreground` 1.5px `dasharray 4 4`.
  - Marcador do dia em hover: linha vertical tracejada `2 3` + tooltip preto (raio 12, sombra `0 8px 24px rgba(0,0,0,.18)`): "27 set · esperado R$ 15.060" 11px + "Faltaram R$ 1.478" mono 13px/600.
- Eixo X: mono 11px, 5 marcas (01, 08, 15, 22, 30 set), com padding-left igual ao eixo Y.

### KPIs
- Primeiro card em `primary` (valor em aberto) com botão circular preto 32px (seta limão). Demais em `surface` com pill branca de comparação (12px/700).
- Rótulo 14px · valor display 30px · rodapé: legenda 12px + sparkline 100×28 (stroke 2).
- Textos: "Valor em aberto / R$ 10.389,38 / 12 divergências · 7 dias"; "Vendido bruto / +6,2% vs ago / 1.284 transações · 7 dias"; "Taxas cobradas / 3,03% do bruto / +R$ 35,50 vs contrato" (vermelho).

### Donut por tipo
- SVG 132px, `r=54`, `stroke-width=18`, circunferência 339.3, segmentos com `dashoffset` acumulado e ~3px de folga. Centro: número display 28px + "abertas".
- Legenda (13px, gap 9): quadrado 10px + nome + contagem mono. Cores por tipo, na ordem: `foreground`, `primary`, `success`, `#B9BDB2`, `danger`.

### Tabela de divergências abertas
- Bloco `surface` padding 8; cabeçalho com título + filtros em pill (ativo branco).
- Lista branca raio 18, padding 6, gap 4. Cabeçalho 12px `muted`.
- Colunas fixas: transação 200px (ícone 34px raio 10 com sigla do método + ref mono 13px/600 + "Crédito 3x · 26 set" 12px), tipo flex (pill: `foreground`/branco para ausente, `primary-soft` para os demais), esperado 100px, diferença 100px (mono, `danger`), status 100px (pill `danger-soft`/`danger-strong` "Aberta"), ação 104px.
- Ação: botão pill 32px "Resolver ›". Linha destacada (hover/foco): fundo `subtle`, botão preto com chevron limão; demais: botão branco com contorno `border`.
- Rodapé: "Mostrando 4 de 12 · maior diferença primeiro" 13px `muted` + "Ver todas as 12 →".
- Status válidos: Aberta, Aceita, Ajustada, Baixada. **Não existe "Em análise".**

### Heatmap de conciliação por dia
- Grade 7 colunas (S T Q Q S S D, segunda primeiro), células 40px de altura, raio 10, gap 6, número do dia mono 12px/600.
- Escala: ≥99% `primary` · 97–98% `primary-soft` · 94–96% branco · 90–93% `#FFD9DA` com texto `danger-strong` · <90% `danger` com texto branco. Legenda "< 90%" → "100%" com quadrados 16×10.

## Estados

| Estado | Regras visuais |
| --- | --- |
| Carregando | Mesma grade bento com blocos placeholder (`surface`; dentro `border`/`#EBEDE6`); o card preto mantém o fundo com placeholders `rgba(255,255,255,.08)`; o card limão vira `#EEF8C9`. Pulse de opacidade 0.6↔1 em 1.4s. Nav e seletor de empresa já interativos. |
| Run em andamento | Manchete "Conciliando setembro…"; botão primário desabilitado (`surface`, texto `muted`, "Conciliando…"); passo "Conciliado" vira atual; card preto mostra badge "RUNNING" e "Os blocos abaixo mostram o run anterior"; demais blocos com `opacity .45`. Progresso numérico depende de API (ver Backend). |
| Vazio (sem run) | Manchete "Outubro ainda não foi conciliado"; botão primário desabilitado; bloco `surface` com ilustração (duas folhas + selo limão) e lista de 3 passos (feito / atual com contorno limão 2px e botão preto / bloqueado com `opacity .6`). |
| Sem empresa | Card preto: "Nenhuma empresa no seu acesso" + botão limão "Cadastrar empresa". |
| Erro de rede | Banner `danger-soft` raio 16: ícone, "Não foi possível conectar à API" 14px/600 `danger-strong`, subtítulo, botão preto "Tentar de novo". Blocos já carregados continuam visíveis. |
| Run FAILED | Card preto: badge vermelho "Falhou", título display 30px, texto genérico, run id mono com "Copiar", botão limão "Rodar de novo". |
| Erro de um bloco | Área com borda tracejada `#D3D6CD`, ícone em círculo branco, "O gráfico não carregou", botão "Recarregar gráfico". |
| 401 | Sem tela: volta ao login (comportamento atual). |
| Período travado | Manchete "Setembro fechado em 94,2%"; régua toda concluída, último passo limão "Travado · Destravar"; botão primário vira preto "Exportar relatório"; abertas 0; valor em aberto R$ 0,00; donut mostra resoluções (Ajustadas, Aceitas, Baixadas); tabela "Resoluções de setembro" com status em `primary-soft`/`success` e botão "Detalhes". |

## Interações

### Gaveta "Resolver divergência"
- Overlay `rgba(11,13,10,.42)`. Gaveta 560px à direita, margem 16, raio 28, sombra `0 24px 64px rgba(0,0,0,.24)`.
- Cabeçalho: pills do tipo e do status, botão fechar (círculo 36px `surface`), ref mono 22px/600, "Pix · venda de 24 set · snapshot do run de hoje 09:42".
- Comparação (bloco `surface` raio 18): colunas Campo 128px / Venda interna / Adquirente; linhas Valor bruto, Líquido, Método, Parcelas, Status, Data; o campo divergente fica em linha branca com contorno `danger` 1.5px e valor do adquirente em `danger`.
- "Desfecho": 3 opções em cartão (Aceitar / Ajustar / Baixar) com descrição 12px; selecionada em `foreground`.
- Ajustar: campo "Valor da correção" (48px, contorno 2px `foreground`, prefixo R$, valor mono, pill "Usar diferença"), ajuda "Com sinal e diferente de zero. Não altera a venda, o snapshot nem o próximo run."
- Nota opcional com contador `n/500` (mono 12px), área `surface` raio 14.
- Histórico: linha do tempo (ponto 10px + trilho 2px `border`), mais recente primeiro; reabertura mostra "lançamento de −1.000,00 anulado".
- Rodapé fixo: "3 de 12 abertas · J / K navega" + "Cancelar" (`surface`) + primário limão "Salvar ajuste ✓". Em divergência resolvida, a ação vira "Reabrir".

### Menu de empresa
- Painel 340px, raio 20, sombra `0 0 0 1px border, 0 20px 48px rgba(11,13,10,.14)`, padding 8.
- Busca "Buscar empresa ou CNPJ"; itens com avatar 32px, nome 14px/600, CNPJ mono 12px, pill de estado (abertas em `danger-soft`, "Travado" em `primary-soft`, "Sem run" em `surface`), check na atual (linha `surface`). Rodapé "Cadastrar empresa".

### Menu de período
- Painel 520px: coluna de atalhos 200px (mês + estado: "Sem run", pill limão "Aberto", cadeado `success` para travado; "Personalizado…") + formulário De/Até (mono, 40px, raio 12) + nota `surface`: "Liquidações lidas até 05/10/2026 — A janela usa a data da venda. O lado da liquidação estende 5 dias…" + Cancelar / Aplicar (preto).

## Rotas

Nav: Visão geral (`/`), Conciliações, Divergências, Transações, Importações, Fechamento. As cinco últimas existem como páginas vazias ("em construção") até suas features.

## Mapa de dados

| Bloco | Fonte |
| --- | --- |
| Anel, abertas, valor em aberto, trava, régua | `GET /api/merchants/{id}/periods` (`matchRate`, `matchedCount`, `divergentCount`, `openAmount`, `locked`, `lockedAt`) |
| Último run, andamento, falha | runs de `/reconciliations` (`status`, `startedAt`, `finishedAt`, `errorMessage`) |
| Tabela, gaveta | items do run + `GET`/`PATCH .../discrepancies/{id}` |
| Importado (régua) | listas de importação de liquidação e extrato |
| Gráfico diário, donut por tipo, heatmap, "caiu no banco", comparativo vs mês anterior, taxas cobradas | **API nova** (resumo do período) |
| Progresso do run (%, itens, ETA) | **API nova** |

Enquanto a API nova não existir, o bloco correspondente mostra estado "em breve" honesto, sem número inventado.
