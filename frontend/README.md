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

- Página inicial pública em `/`, sem sessão e sem chamar a API: o que o ReconPay faz, uma prévia estática do painel de fechamento, os três passos e o tratamento de divergências, com "Criar conta" e "Entrar". As seções entram conforme o scroll e desfazem a entrada ao voltar; o anel da taxa de match e a barra do passo 2 enchem junto com os números (`components/landing/CountUp.tsx`, `lib/scrollProgress.ts`). As animações de scroll usam `animation-timeline: view()` (Chrome, Edge, Safari 26); sem suporte, como no Firefox hoje, ou com movimento reduzido no sistema, tudo aparece parado no estado final.
- Cadastro e login no mesmo visual (painel escuro e formulário). No cadastro, as regras da senha (8 caracteres, uma letra maiúscula, um número) ficam marcadas enquanto o usuário digita; depois do envio, a confirmação toma o lugar do formulário.
- O link do e-mail de verificação abre `/verificar-email`, que confirma a conta sozinha e leva ao login. Com o link vencido ou já usado, a página pede o e-mail e envia um link novo. Para isso, `RECONPAY_VERIFICATION_BASE_URL` no backend aponta para este app.
- Sessão JWT apenas em memória: atualizar a página pede novo login. Qualquer `401` encerra a sessão e volta ao login. "Sair" fica na barra superior.
- Barra superior com Visão geral, Conciliações, Divergências, Transações, Importações e Fechamento, e o menu de empresa (busca por nome ou CNPJ, "Regras de taxa da <empresa>" com uma empresa selecionada, "Cadastrar empresa").
- Dashboard em `/painel`, para onde o login leva:
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
- Conciliações em `/conciliacoes`, pelo item "Conciliações" da barra superior:
  - histórico de runs da empresa selecionada, 20 por página, do mais recente ao mais antigo, com Janela ("Outubro de 2026" ou "01/10/2026 – 15/10/2026"), Status (Na fila, Em andamento, Concluída, Falhou), Itens, Conciliados, Divergentes, Criado em e "Ver itens";
  - em andamento, Itens mostra "<processados> de <total>"; run substituído leva o selo "Substituído"; run com falha mostra a mensagem do run; a lista recarrega a cada 3 s enquanto algum run da página estiver na fila ou em andamento;
  - filtro "Janela" com "Todas" e os 12 meses mais recentes; um mês envia `fromDate`/`toDate` exatos e volta para a página 1;
  - "Nova conciliação" abre uma gaveta com "Mês" (no mês atual) ou "Personalizado" e os campos De e Até; datas vazias, intervalo invertido e janela acima de 366 dias são checados antes de chamar a API; no aceite a gaveta fecha, o filtro volta para "Todas" e o run novo aparece no topo como "Na fila"; `409` e `400` mantêm a gaveta aberta com a mensagem;
  - trocar de empresa fecha a gaveta, mantém o filtro e volta para a página 1; estados de carregamento, sem runs ("Nenhuma conciliação encontrada."), sem empresa e erro ("Tentar novamente").
- Detalhe de um run em `/conciliacoes/[runId]`, pelo "Ver itens" da lista:
  - cabeçalho com janela, status, criado em, Itens, Conciliados, Divergentes, o selo "Substituído" e "Voltar para conciliações"; run inexistente ou de outra empresa mostra "Conciliação não encontrada.";
  - itens do run, 20 por página, da maior diferença absoluta para a menor, com Referência, Data da venda, Método, Líquido esperado, Líquido liquidado, Diferença, Resultado (Bateu, Divergente) e Divergências ("<tipo> · <status>"); o lado ausente aparece como "—";
  - filtros "Resultado" (Todos, Bateram, Divergentes) e "Tipo de divergência"; qualquer mudança volta para a página 1; sem itens, "Nenhum item encontrado para os filtros escolhidos.";
  - run na fila ou em andamento recarrega o run e os itens a cada 3 s; ao concluir, busca os itens uma última vez, porque eles só aparecem na API junto com o status final;
  - "Exportar CSV" aparece só em run concluído e baixa `reconciliation-<runId>.csv`; o erro aparece junto do botão;
  - trocar de empresa volta para `/conciliacoes`.
- Divergências em `/divergencias`, pelo item "Divergências" da barra superior:
  - mostra as divergências do run vigente da janela escolhida no menu de período do dashboard (abre na janela do run mais recente, ou no mês atual sem run), uma linha por divergência, da maior diferença para a menor, 20 itens por página;
  - filtros "Status" (Abertas, Aceitas, Ajustadas, Baixadas, Todas) e "Tipo"; mudar filtro ou janela volta para a página 1;
  - janela sem run, run em andamento (a tela recarrega a cada 3 s e mostra a lista ao concluir) ou com falha mostram um aviso com o link para Conciliações; período travado mostra o aviso e deixa a gaveta só para consulta;
  - "Abrir" abre a gaveta do dashboard: os dois lados da transação, o histórico de transições e ajustes, aceitar, ajustar, baixar e reabrir; J/K navegam pelas divergências da página;
  - trocar de empresa fecha a gaveta, vai para a janela do run mais recente da nova empresa e mantém os filtros.
- Fechamento em `/fechamento`, pelo item "Fechamento" da barra superior:
  - uma linha por janela dos 100 runs mais recentes da empresa, da data inicial mais recente para a mais antiga, 10 por página;
  - cada linha lê o seu período: "Travado" ou "Aberto" com taxa de match, valor em aberto, quando travou e quanto levou; "Em andamento" e "Sem conciliação concluída" ficam sem ações; uma leitura com erro mostra "Não carregou" e "Tentar novamente";
  - "Travar" e "Destravar" pedem confirmação e atualizam a linha; a trava recusada (por exemplo, divergência em aberto) mostra a mensagem da API acima da tabela; "Exportar CSV" baixa `reconpay-<início>-<fim>.csv`;
  - empresa sem runs mostra o aviso com o link para Conciliações; trocar de empresa volta para a página 1.

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
