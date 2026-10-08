# Frontend ReconPay

Next.js 16, React 19, TypeScript, Tailwind CSS e o estilo shadcn do protótipo v0. Tudo do aplicativo web fica nesta pasta.

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
- Sessão JWT apenas em memória: atualizar a página pede novo login.
- Perfil, lista de merchants acessíveis e cinco transações recentes do merchant escolhido.
- Indicadores, gráfico e saúde das fontes aparecem sem valores até existir um contrato de API para esses dados.

## Verificação

```bash
corepack pnpm test
corepack pnpm typecheck
corepack pnpm build
```
