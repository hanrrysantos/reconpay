# API ReconPay

[README principal](../README.md)

## Regras de negócio

### Usuários e cadastro

- O e-mail é único. Usuários são desativados, nunca apagados, e um usuário inativo não autentica.
- O cadastro público cria um operador inativo, que é ativado pelo link enviado por e-mail. O link vale por 24 horas e pode ser usado uma vez.
- A senha precisa de pelo menos 8 caracteres, uma letra maiúscula e um número.
- Desativar um usuário encerra a sessão dele na requisição seguinte.

### Empresas

- O CNPJ precisa ter dígitos verificadores válidos. Pontuação é aceita e removida; só os dígitos são gravados.
- Não pode haver duas empresas com o mesmo CNPJ, nem entre as desativadas.
- Quem cadastra uma empresa recebe acesso a ela automaticamente.

### Regras de taxa

- Cada combinação de empresa, forma de pagamento e número de parcelas tem no máximo uma regra ativa.
- A taxa tem uma parte percentual (de 0 a 100, com até 4 casas) e uma parte fixa (a partir de zero).
- Pix, boleto e débito só aceitam 1 parcela.

### Vendas

- A referência externa é única por empresa.
- Toda venda precisa de uma regra de taxa ativa. O líquido esperado é `valor − taxa percentual − taxa fixa` e precisa ser positivo.
- A venda nasce aprovada e pode passar para cancelada, estornada ou chargeback. A mudança não pode ser desfeita.
- Alterações simultâneas na mesma venda são recusadas com `409` (versão otimista).

### Importação por CSV

Os três tipos de importação seguem as mesmas regras: arquivo de até 5 MB, recusado inteiro se qualquer linha for inválida, com a lista de erros por linha na resposta. Cada arquivo aceito gera um lote consultável.

| Tipo | Colunas |
| :--- | :--- |
| Vendas | `externalReference,amount,paymentMethod,installments,status,transactionDate` |
| Liquidação, layout `RECONPAY` | `externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate` |
| Liquidação, layout `ACQUIRER` | `nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao` |
| Extrato bancário | `lineReference,externalReference,amount,movementDate` |

No extrato, `externalReference` é opcional. Sem ela, a linha só pode ser ligada a uma liquidação por data e valor.

### Conciliação

A conciliação compara uma janela de datas de uma empresa. A liquidação e o extrato são lidos com alguns dias a mais depois do fim da janela (5 por padrão), porque uma venda do fim do período é paga no período seguinte.

**1ª fase: venda × liquidação**, pela referência externa.

| Divergência | Quando |
| :--- | :--- |
| `MISSING_SETTLEMENT` | Venda sem liquidação |
| `ORPHAN_SETTLEMENT` | Liquidação sem venda |
| `INCORRECT_AMOUNT` | Valor bruto diferente |
| `FEE_DIVERGENCE` | Líquido pago diferente do líquido esperado pela regra de taxa |
| `STATUS_MISMATCH` | Status diferente |
| `PAYMENT_METHOD_MISMATCH` | Forma de pagamento diferente |
| `INSTALLMENTS_MISMATCH` | Número de parcelas diferente |

**2ª fase: liquidação × extrato.** Cada liquidação é ligada ao crédito do extrato com a mesma referência. As linhas sem referência são ligadas por data e valor, quando só existe um par possível.

| Divergência | Quando |
| :--- | :--- |
| `BANK_AMOUNT_MISMATCH` | Crédito com valor diferente do líquido liquidado |
| `MISSING_BANK_CREDIT` | Liquidação sem crédito no extrato |
| `ORPHAN_BANK_CREDIT` | Crédito sem liquidação |
| `AMBIGUOUS_BANK_MATCH` | Mais de um par possível entre liquidações e créditos; o sistema não escolhe |

- A execução é assíncrona: a requisição responde `202` e a execução passa por `PENDING`, `RUNNING` e termina em `COMPLETED` ou `FAILED`.
- Cada item guarda uma cópia dos dados comparados, então alterar uma venda depois não muda um resultado já gerado.
- Uma nova execução da mesma janela substitui a anterior, que continua gravada.
- A janela tem no máximo 366 dias e 300.000 registros.
- O resultado de uma execução pode ser exportado em CSV. Textos que começam com `=`, `+`, `-` ou `@` recebem um `'` na frente, para não virarem fórmula ao abrir na planilha.

### Resolução de divergências

- Toda divergência nasce aberta e pode ser aceita, ajustada com um valor de correção ou baixada. Qualquer uma dessas pode ser reaberta.
- Cada mudança grava autor, data e uma observação opcional de até 500 caracteres.
- Reabrir uma divergência ajustada anula o lançamento de correção, que continua no histórico.
- Só a execução mais recente e concluída da janela aceita mudanças.

### Fechamento de período

- Um período só pode ser travado com uma conciliação concluída, nenhuma em andamento e nenhuma divergência aberta.
- Um período travado não aceita importações, alterações de venda, novas conciliações nem mudanças de divergência.
- O período pode ser destravado.

### Conta de demonstração

- Criada sem cadastro, já com uma empresa, regras de taxa, 60 dias de vendas, liquidações e extrato, e conciliações com divergências abertas.
- Expira em 24 horas e é apagada por uma tarefa que roda a cada 15 minutos.
- Limites: 100 demonstrações ativas ao mesmo tempo, 2 por IP por hora, e por conta 2 empresas e 10 importações.

## API

A documentação completa, gerada a partir do código, fica no Swagger:

- Local: http://localhost:8080/swagger-ui.html
- Produção: https://reconpay.hanrry.top/swagger-ui.html

Para testar pelo Swagger, faça login em `POST /api/auth/login`, copie o token e use o botão **Authorize**.

São públicas as rotas de login, cadastro, verificação e reenvio de e-mail, a criação de demonstração (`POST /api/auth/demo`), o Swagger e o `/actuator/health`. As demais exigem o token JWT.

Convenções:

- Toda rota de dados fica sob `/api/merchants/{merchantId}`.
- Erros seguem o formato `StandardError`, com um código em `error` (`VALIDATION_ERROR`, `NOT_FOUND`, `CONFLICT`...) e detalhes em `details` quando existem.
- Listas são paginadas com `page`, `size` e `sort`. Ordenar por um campo que não existe responde `400`.
- Valores em dinheiro aceitam até 17 dígitos inteiros e 2 casas decimais.

## Segurança

- Autenticação por JWT, sem sessão no servidor.
- O administrador acessa todas as empresas e gerencia os usuários.
- O operador acessa só as empresas liberadas para ele. Toda rota de uma empresa passa pela verificação de acesso antes de chegar ao serviço.

## Organização do código

Pacote raiz `br.com.hanrry.reconpay`, com um módulo por domínio:

- `auth`: cadastro, login, verificação de e-mail e usuários
- `merchant`: empresas
- `feerule`: regras de taxa
- `transaction`: vendas e importação de vendas
- `externalsettlement`: importação e consulta de liquidações
- `bankstatement`: importação e consulta do extrato bancário
- `reconciliation`: conciliação, resolução de divergências, fechamento de período e relatório
- `demo`: conta de demonstração
- `security`: JWT e verificação de acesso por empresa
- `observability`: identificador de requisição e log de auditoria
- `exception`: exceções e tratamento global de erros
- `config`, `shared` e `openapi`: configurações, regras compartilhadas e documentação da API

Dentro de cada módulo, as pastas seguem o mesmo padrão: `controller`, `service`, `repository`, `entity`, `dto`, `mapper`. A documentação do Swagger fica nas interfaces da pasta `openapi`, que os controllers implementam.

## Como rodar

Pré-requisitos: Java 21 e Docker. O Maven vem pelo wrapper `./mvnw`.

Com o `.env` criado na raiz (`cp .env.example .env`), suba o banco e rode a API no profile `dev`:

```bash
docker compose up -d banco-reconpay
cd backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

O profile `dev` lê o `.env` da raiz, conecta no banco com `postgres` / `1234` e cria dois usuários de teste:

- Administrador: `admin@reconpay.local` / `DevAdmin@2026`
- Operador: `analyst@reconpay.local` / `DevAnalyst@2026`

Profiles disponíveis:

- `dev`: desenvolvimento local, com usuários de teste e SQL no log
- `test`: testes automatizados, com usuários de teste
- `prod`: produção e Docker Compose, sem usuários de teste e com logs em JSON

## Configuração

As variáveis ficam no `.env` da raiz. As obrigatórias são:

- `JWT_SECRET`: chave de assinatura do token, com pelo menos 32 caracteres.
- `DB_URL`, `DB_USER` e `DB_PASSWORD`: conexão com o PostgreSQL. No `dev` têm valor padrão, e no Docker Compose são preenchidas a partir de `POSTGRES_USER` e `POSTGRES_PASSWORD`.

As principais opcionais:

- `JWT_EXPIRATION`: validade do token, em segundos (padrão 24 horas).
- `RESEND_API_KEY`: chave do Resend, necessária para o cadastro público, que envia o e-mail de verificação. Sem ela, o cadastro falha ao enviar o e-mail; para entrar no app, use a demonstração ou os usuários de teste.
- `RECONPAY_VERIFICATION_BASE_URL`: endereço do frontend, usado no link do e-mail de verificação.
- `RECONPAY_CORS_ALLOWED_ORIGINS`: origens do frontend que podem chamar a API, separadas por vírgula.
- `RECONPAY_EMAIL_REDIRECT_TO`: só no `dev`, envia todo e-mail para este endereço em vez do endereço cadastrado.

Os limites da conciliação (`RECONCILIATION_*`) e da conta de demonstração (`DEMO_*`) também podem ser ajustados. A lista completa, com os valores padrão, está em [`application.yaml`](src/main/resources/application.yaml).

O `docker-compose.yaml` repassa à API só as variáveis de JWT, banco, e-mail e CORS. As demais, no Docker, ficam com o valor padrão.

## Observabilidade

- Cada requisição recebe um identificador, lido do cabeçalho `X-Request-Id` ou gerado, que aparece em todas as linhas de log junto com o e-mail do usuário.
- Logs em texto no `dev` e `test`, e em JSON no `prod`.
- Ações que alteram dados (cadastros, importações, conciliações, mudanças de divergência, trava de período, acessos de usuários) geram um log de auditoria.
- Métricas em `/actuator/prometheus` e `/actuator/metrics`, que exigem autenticação. Só o `/actuator/health` é público.

## Banco de dados

- PostgreSQL com o schema versionado pelo Flyway, em `src/main/resources/db/migration`.
- O Hibernate só valida o schema (`ddl-auto=validate`). Toda mudança de tabela é uma migration nova, `V<n>__descricao.sql`.
- Os usuários de teste ficam em `src/main/resources/db/seed`, carregados apenas nos profiles `dev` e `test`.

## Testes

```bash
./mvnw verify
```

- Testes unitários com JUnit 5 e Mockito, e de integração com MockMvc e PostgreSQL real via Testcontainers. Os de integração precisam do Docker rodando e herdam de `AbstractIntegrationTest`.
- O `verify` falha com cobertura abaixo de 85% das linhas ou 75% dos branches. O relatório do JaCoCo fica em `target/site/jacoco/index.html`.

## Deploy

- Cada push na `main` que altere `backend/`, o `docker-compose.yaml` ou o workflow de deploy dispara o `.github/workflows/deploy-prod.yml`.
- O workflow entra no servidor por SSH, atualiza o código, reconstrói os containers e espera o `/actuator/health` responder `UP` por até 3 minutos.
- O Nginx do servidor precisa enviar o cabeçalho `X-Forwarded-For`. No profile `prod`, é dele que a API tira o IP usado no limite de demonstrações.

## Limites conhecidos

- **Uma instância só:** a fila de conciliação fica no banco, e uma execução interrompida volta para `PENDING` quando a API reinicia. Rodar mais de uma instância exige controle de concorrência na fila antes.
- **Janela inteira em memória:** cada conciliação carrega a janela toda. O limite de 300.000 registros foi medido para a API com 1 GB. Se faltar memória, a API encerra e o Docker Compose a reinicia.
- **Limite de demonstração por IP em memória:** reiniciar a API zera a contagem.
- **Sem refresh token:** quando o token expira, é preciso fazer login de novo.
