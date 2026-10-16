# Frontend ReconPay

Next.js 16, React 19, TypeScript e Tailwind CSS 4 com o tema "Lima" (tokens em `app/globals.css`). TanStack Query busca e mantém os dados da API. Gráficos são SVG próprio, sem biblioteca. Tudo do aplicativo web fica nesta pasta.

## Rodar localmente

1. Inicie a API ReconPay em `http://localhost:8080` conforme o [README principal](../README.md).
2. Instale Node.js 24 e execute:

```bash
cd frontend
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

Abra `http://localhost:3000`. Para usar outra API, copie `.env.example` para `.env.local`, altere `NEXT_PUBLIC_API_URL` e reinicie o Next. A origem do frontend precisa estar autorizada em `reconpay.cors.allowed-origins` no backend.

## Fluxos disponíveis

- Cadastro e login; após o cadastro, a verificação de email abre o link servido pela API.
- Sessão JWT apenas em memória: atualizar a página pede novo login. Qualquer `401` encerra a sessão e volta ao login. "Sair" fica na barra superior.
- Barra superior com Visão geral, Conciliações, Divergências, Transações, Importações e Fechamento, e o menu de empresa (busca por nome ou CNPJ, "Cadastrar empresa").
- Dashboard em `/`:
  - abre na janela do run mais recente da empresa; sem run, no mês atual com os três passos (liquidação, extrato, conciliar);
  - menu de período com meses ("Sem run", "Aberto", travado) e intervalo personalizado; janela inválida mostra a mensagem da API e mantém a anterior;
  - régua de fechamento Importado → Conciliado → Zerar divergências → Travar período;
  - taxa de conciliação, valor em aberto, bruto, taxas, gráfico esperado × recebido no banco, divergências por tipo e conciliação por dia, todos vindos de `/periods` e `/periods/summary`;
  - tabela das 4 divergências abertas de maior diferença, com filtro por tipo e gaveta de resolução (aceitar, ajustar com valor, baixar, reabrir; `J`/`K` navegam, `Esc` fecha);
  - "Conciliar período" acompanha o run a cada 3 s e recarrega os blocos ao concluir; run com falha mostra a mensagem, o id e "Rodar de novo";
  - travar, destravar e "Exportar relatório" (CSV do run);
  - estados de carregamento, sem empresa, sem acesso, erro de um bloco e erro de rede ("Tentar de novo").
- Empresas em `/empresas`:
  - lista paginada (20 por página, por nome) com nome, CNPJ formatado e data de cadastro; a empresa selecionada aparece marcada e clicar no nome a seleciona no menu do topo;
  - "Cadastrar empresa" abre uma gaveta com Nome e CNPJ com máscara; nome de 1 a 150 caracteres e "CNPJ inválido" são checados antes de chamar a API; a nova empresa passa a ser a selecionada;
  - "Editar" renomeia (o CNPJ fica somente leitura); "Desativar" pede confirmação e avisa que o CNPJ não poderá ser cadastrado de novo; se a desativada era a selecionada, o menu passa para a primeira restante;
  - erros da API mantêm a gaveta ou o diálogo aberto com a mensagem; `403`/`404` também recarregam a lista; `Esc` fecha;
  - estados de carregamento, sem empresa ("Nenhuma empresa no seu acesso") e erro ("Tentar de novo").
- `/conciliacoes`, `/divergencias`, `/transacoes`, `/importacoes` e `/fechamento` mostram a barra e "em construção", sem chamar a API.

## Dependências

- `@tanstack/react-query`: cache, polling do run e invalidação após cada ação.
- `lossless-json`: mantém decimais como texto; dinheiro nunca passa por `number`.
- Testes: Vitest (`*.test.ts` em node, `*.test.tsx` em jsdom), Testing Library (`react`, `user-event`, `jest-dom`).

## Verificação

```bash
corepack pnpm test
corepack pnpm typecheck
corepack pnpm build
```
