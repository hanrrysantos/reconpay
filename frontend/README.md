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
- Barra superior com Visão geral, Conciliações, Divergências, Transações, Importações e Fechamento, e o menu de empresa (busca por nome ou CNPJ, "Regras de taxa da <empresa>" com uma empresa selecionada, "Cadastrar empresa").
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
- Regras de taxa em `/taxas`, aberta pelo item "Regras de taxa da <empresa>" do menu de empresa:
  - lista as regras ativas da empresa selecionada (até 100, por método e parcelas) com método, parcelas (`3x`), percentual (`2,99%`) e taxa fixa (`R$ 0,30`); trocar a empresa no menu mostra as regras da nova e fecha gaveta ou diálogo abertos;
  - "Cadastrar regra" abre uma gaveta com Método, Parcelas, Percentual e Taxa fixa; Pix, Boleto e Débito ficam com 1 parcela; parcelas inteiras a partir de 1, percentual de 0 a 100 com até 4 casas e taxa fixa a partir de 0 com até 2 casas são checados antes de chamar a API; vírgula ou ponto como separador, enviados como número JSON exato;
  - "Editar" altera só o percentual e a taxa fixa (método e parcelas ficam somente leitura); "Desativar" pede confirmação e avisa que novas transações dessa combinação serão recusadas até existir outra regra;
  - erros da API (como `409` de regra duplicada) mantêm a gaveta ou o diálogo aberto com a mensagem; `403`/`404` também recarregam a lista; `Esc` fecha;
  - estados de carregamento, sem regras ("Nenhuma regra de taxa"), sem empresa e erro ("Tentar de novo").
- Importações em `/importacoes`, pelo item "Importações" da barra superior:
  - abas Transações, Liquidação e Extrato da empresa selecionada; a aba fica em `?tipo=transacoes|liquidacao|extrato` (sem valor ou valor desconhecido abre Transações);
  - cada aba mostra as colunas esperadas do CSV e envia o arquivo escolhido ao import do tipo; a liquidação vai sempre no layout `RECONPAY`; arquivo acima de 5 MB é recusado antes do envio;
  - o import é tudo ou nada: no aceite aparece "<arquivo> importado: N linha(s)"; na recusa aparecem a mensagem da API, a tabela Linha/Erro de `rowErrors` e as referências de `conflictingReferences`, e o arquivo fica escolhido para reenviar;
  - lotes aceitos do tipo, 20 por página, do mais recente ao mais antigo (arquivo, linhas, data e hora); após um aceite a lista volta à primeira página e o dashboard atualiza a hora do último import;
  - trocar de aba ou de empresa descarta o arquivo e o resultado; estados de carregamento, sem lotes, sem empresa e erro ("Tentar novamente").
- Transações em `/transacoes`, pelo item "Transações" da barra superior:
  - lista as transações da empresa selecionada, 20 por página, da data mais recente para a mais antiga, com Data (`dd/mm/aaaa`), Referência, Método, Parcelas (`3x`), Bruto, Líquido esperado e Status (Aprovada, Cancelada, Estornada, Chargeback);
  - filtros de período (o menu do dashboard com o mês atual e os 11 anteriores, ou um intervalo personalizado; data inicial depois da final mostra "A data inicial deve ser igual ou anterior à final" sem consultar a API), status e método, cada um com "Todos"; abre no mês atual, os filtros não vão para a URL e qualquer mudança volta para a página 1;
  - "Nova transação" abre uma gaveta com Referência externa, Valor, Método, Parcelas e Data da transação (hoje por padrão); Pix, Boleto e Débito ficam com 1 parcela; referência até 100 caracteres, valor maior que zero com até 2 casas, parcelas a partir de 1 e data não futura são checados antes de chamar a API; o valor vai como número JSON exato; no aceite aparece "Transação <referência> cadastrada." e a lista recarrega;
  - "Alterar status" aparece só nas transações aprovadas e pede Cancelada, Estornada ou Chargeback num diálogo que avisa que a mudança não pode ser desfeita;
  - erros da API mantêm a gaveta ou o diálogo aberto com a mensagem; sem regra de taxa ativa, a gaveta mostra o link "Cadastrar regra de taxa" para `/taxas?cadastrar=1`; um erro na mudança de status também recarrega a lista;
  - trocar de empresa fecha gaveta e diálogo, remove o aviso, mantém os filtros e volta para a página 1; estados de carregamento, sem transações ("Nenhuma transação encontrada para os filtros escolhidos."), sem empresa e erro ("Tentar novamente").
- `/conciliacoes`, `/divergencias` e `/fechamento` mostram a barra e "em construção", sem chamar a API.

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
