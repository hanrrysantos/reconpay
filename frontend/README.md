# Aplicação web ReconPay

[README principal](../README.md)

## Telas

- `/`: apresentação pública do produto, com o botão "Ver demonstração".
- `/login` e `/register`: entrada e cadastro. No cadastro, as regras da senha ficam marcadas enquanto o usuário digita.
- `/verificar-email`: confirma a conta pelo link do e-mail ou pede um link novo, se o anterior venceu.
- `/painel`: fechamento de uma janela, com taxa de conciliação, valor em aberto, esperado × recebido no banco, divergências por tipo e por dia e as maiores divergências abertas. Daqui também se roda a conciliação, trava o período e exporta o relatório.
- `/empresas`: lista, cadastro, edição e desativação de empresas.
- `/taxas`: regras de taxa da empresa selecionada.
- `/transacoes`: vendas da empresa, com filtros, cadastro avulso e mudança de status.
- `/importacoes`: envio dos CSVs de vendas, liquidação e extrato, com os erros de cada linha e os lotes já importados.
- `/conciliacoes`: histórico de execuções e início de uma nova conciliação.
- `/conciliacoes/[runId]`: itens de uma execução, com filtros e exportação em CSV.
- `/divergencias`: divergências da janela, com filtros e a gaveta de resolução.
- `/fechamento`: situação de cada período, com trava, destrava e exportação.
- `/usuarios`: só para o administrador. Criação e desativação de usuários e escolha das empresas de cada operador.

## Sessão

- No login comum, o token fica só em memória. Recarregar a página pede login de novo.
- Na demonstração, o token fica no `sessionStorage`, então recarregar a página mantém a sessão. "Sair" apaga o token.
- Qualquer resposta `401` da API encerra a sessão e volta para o login, ou para a página inicial na demonstração.

## Organização do código

- `app/`: rotas. As que exigem login ficam em `app/(app)`, que compartilha a barra superior e o menu de empresa.
- `components/`: uma pasta por tela, mais `shell` (barra superior, menus e faixa da demonstração) e `ui` (botão, diálogo de confirmação e gaveta lateral).
- `lib/api.ts`: todas as chamadas à API e o tratamento dos erros.
- `lib/queries.ts`: hooks do TanStack Query, com cache e atualização dos dados após cada ação.
- `lib/`: regras de cada domínio (`money`, `period`, `fee`, `cnpj`, `password`...), sem React.
- Os testes ficam ao lado do arquivo testado, com o mesmo nome e `.test.ts` ou `.test.tsx`.

## Decisões técnicas

- **Dinheiro como texto:** as respostas da API são lidas com `lossless-json`, então valores em dinheiro nunca passam por `number` e não perdem precisão.
- **Acompanhamento das conciliações:** enquanto uma execução está na fila ou em andamento, a tela consulta a API a cada 3 segundos.
- **Gráficos em SVG próprio**, sem biblioteca de gráficos.
- **Animações da landing por scroll** com `animation-timeline`. Em navegadores sem suporte, como o Firefox, ou com movimento reduzido no sistema, tudo aparece parado no estado final.
- **Fontes servidas pelo próprio app**, sem chamada ao Google Fonts no build.

## Como rodar

Pré-requisitos: Node.js 24 e a API rodando em `http://localhost:8080` (veja o [README principal](../README.md)).

```bash
corepack pnpm install
corepack pnpm dev
```

O app abre em `http://localhost:3000`. Para usar outra API, copie `.env.example` para `.env.local` e altere `NEXT_PUBLIC_API_URL`. A origem do app precisa estar liberada no CORS da API (`RECONPAY_CORS_ALLOWED_ORIGINS`).

## Testes

```bash
corepack pnpm test
corepack pnpm typecheck
corepack pnpm build
```

- Vitest com Testing Library. Arquivos `.test.ts` rodam em Node, e `.test.tsx` rodam em jsdom.
- A CI roda os três comandos, além do `pnpm audit`, a cada mudança em `frontend/`.

## Deploy

A Vercel publica o app a cada push na `main`, pela integração com o GitHub.
