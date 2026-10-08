# ReconPay

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

Fintechs, gateways e marketplaces vendem todos os dias, mas o valor que cai na conta quase nunca é o bruto da venda. Taxa, parcela, atraso de liquidação e chargeback distorcem o líquido. Quando a conciliação vive em planilha, o erro aparece tarde, sem responsável e sem rastro para auditoria.

O **ReconPay** fecha esse ciclo numa API: cadastra o merchant e as regras de taxa, registra a transação interna com o líquido esperado, importa a liquidação externa e cruza os dois lados automaticamente, com isolamento por merchant e relatório exportável.

O ReconPay executa esse fluxo de ponta a ponta. Cada merchant é uma unidade isolada: o operador só enxerga aqueles a que recebeu acesso.

**Fluxo**

- **Merchant e taxas:** cadastro do negócio e das regras de taxa por forma de pagamento e número de parcelas.
- **Transação interna:** cada venda é registrada com o valor líquido que a empresa deveria receber, já descontando a taxa.
- **Liquidação externa:** o arquivo do adquirente ou gateway entra via CSV, é validado linha a linha e fica ligado a um lote rastreável.
- **Conciliação:** o sistema cruza os dois lados numa janela de datas, em segundo plano, e aponta o que não bate: valor, taxa, status, método, parcelas, venda sem liquidação ou liquidação sem venda.
- **Relatório:** o resultado sai em CSV para auditoria, seguro para abrir em planilha.

O time deixa de caçar desvio na planilha e passa a fechar cada janela com três respostas: o que bateu, o que a empresa vendeu e ainda não recebeu, e o que o adquirente pagou diferente do combinado. Cada execução fica gravada e exportável. Rodar de novo o mesmo período guarda o resultado antigo. O novo não apaga o anterior. O MVP está pronto para uso e já produz os dados para medir taxa de match, valor em aberto e tempo até fechar o período.

---

## Sumário

- [Regras de negócio](#regras-de-negócio)
- [Como executar](#como-executar)
- [Frontend](#frontend)
- [Stack](#stack)
- [Módulos](#módulos)
- [API](#api)
- [Segurança](#segurança)
- [Testes](#testes)
- [Autor](#autor)

---

## Regras de negócio

### Usuários
- E-mail único; soft delete; usuários inativos não autenticam.
- Auto-cadastro cria `OPERATOR` inativo até a verificação de e-mail (link GET no e-mail, ou `POST /api/auth/verify-email` para clientes programáticos).

### Merchants
- Documento único; soft delete; consultas retornam apenas merchants ativos.

### Fee rules
- Uma regra ativa por combinação `(merchant, paymentMethod, installments)`.
- Índice único parcial no banco permite histórico de regras inativas.
- Taxa percentual entre 0 e 100, com até quatro casas decimais; taxa fixa não negativa.

### Transações internas
- Referência externa única por merchant, com espaços nas extremidades removidos e maiúsculas/minúsculas preservadas.
- Fee rule ativa obrigatória; `expectedNetAmount = amount - taxa percentual - taxa fixa`.
- Valores monetários aceitam até 17 dígitos inteiros e 2 casas decimais, em JSON e CSV. O líquido esperado deve ser positivo.
- PIX, boleto e débito não permitem parcelamento.
- Status inicial `APPROVED`; transições para `CANCELLED`, `REFUNDED` ou `CHARGEBACK` (sem reversão).
- Atualizações concorrentes usam versão otimista: uma alteração obsoleta recebe `409` e deve consultar o estado atual.

### Liquidações externas
- Importação via CSV (máx. 5 MB) com validação linha a linha.
- Colunas esperadas: `externalReference`, `amount`, `netAmount`, `paymentMethod`, `installments`, `status`, `settlementDate`.
- Rejeita duplicidade no arquivo e no banco; `netAmount` não pode ser maior que `amount`.
- Cada importação gera um lote rastreável (`settlement_imports`).

### Conciliação
- Cruzamento por `(merchant, externalReference)` entre transações internas e liquidações externas.
- Janela obrigatória (`fromDate`, `toDate`) aplicada sobre a data da transação, limitada a `max-window-days`.
- O lado da liquidação lê a janela estendida por `settlement-lag-days`, porque uma venda no fim do período liquida no período seguinte. Liquidações cuja transação está fora da janela são ignoradas em vez de reportadas como órfãs.
- Tipos de divergência: liquidação ausente, liquidação órfã, valor bruto incorreto, taxa divergente, status inconsistente, método de pagamento divergente, parcelas divergentes. Todos são avaliados de forma independente.
- Comparação de valores aceita `amount-tolerance` (padrão `0.00`, ou seja, comparação exata).
- Cada item guarda um **snapshot** dos dois lados no momento da execução, então alterar uma transação depois não reescreve o resultado de um run passado.
- Uma janela tem no máximo um run vigente: ao concluir, o run marca o anterior como `supersededAt`.
- A execução é assíncrona: o POST devolve `202 Accepted` com o run em `PENDING` e o `Location` para acompanhar. O dispatcher consulta pendências persistidas, confirma `RUNNING` em uma transação própria antes de submeter o trabalho e devolve a `PENDING` se o executor estiver cheio. O run termina em `COMPLETED` ou `FAILED`, com mensagem pública genérica; detalhes técnicos ficam nos logs correlacionados por `runId`.
- Ao iniciar, execuções interrompidas em `RUNNING` voltam a `PENDING`. Este mecanismo pressupõe **uma instância da aplicação**; múltiplas instâncias exigem leases ou infraestrutura de fila antes de escalar.
- Enquanto houver um run `PENDING` ou `RUNNING` para a mesma janela, uma nova execução é rejeitada com `409`.
- Exportação CSV dos resultados para auditoria, escrita em streaming. Referências iniciadas por `=`, `+`, `-` ou `@` recebem prefixo `'` para impedir fórmulas em planilhas.

Ajustáveis por `reconpay.reconciliation.*` ou pelas variáveis `RECONCILIATION_AMOUNT_TOLERANCE`, `RECONCILIATION_SETTLEMENT_LAG_DAYS`, `RECONCILIATION_MAX_WINDOW_DAYS`, `RECONCILIATION_ASYNC`, `RECONCILIATION_WORKERS` e `RECONCILIATION_QUEUE_CAPACITY`.

Tolerância, atraso e capacidade da fila não podem ser negativos; janela máxima e workers devem ser positivos. Configurações inválidas impedem a inicialização. `RECONCILIATION_ASYNC=false` executa no thread do dispatcher e mantém o POST assíncrono. Logs de auditoria de sucesso são emitidos somente após commit. JSON malformado, enum/data/UUID inválidos e multipart sem `file` retornam `400` no formato `StandardError` (`VALIDATION_ERROR`).

---

## Como executar

**Pré-requisitos:** Java 21, Docker e Docker Compose. O wrapper Maven fica em `backend/mvnw`.

### 1. Clone e configure o ambiente

```bash
git clone https://github.com/hanrrysantos/reconpay.git
cd reconpay
```

Crie um arquivo `.env` na raiz do projeto:

```env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=1234
JWT_SECRET=sua-chave-secreta-com-pelo-menos-32-caracteres
JWT_EXPIRATION=86400
RESEND_API_KEY=
RECONPAY_EMAIL_FROM=ReconPay <noreply@reconpay.local>
RECONPAY_VERIFICATION_BASE_URL=http://localhost:8080
RECONPAY_VERIFICATION_TOKEN_HOURS=24
```

> `JWT_SECRET` é obrigatório fora dos testes, que possuem chave local exclusiva. Na **Opção A** o profile `dev` importa o `.env` da raiz ao rodar em `backend/`; na **Opção B** o Compose o injeta no container. `JWT_EXPIRATION` é expresso em segundos, com padrão `86400`, repassado pelo Compose e retornado exatamente como `expiresIn` no login. Fora de `dev`, configure também `DB_URL`, `DB_USER` e `DB_PASSWORD` (o Compose os fornece). Sem `RESEND_API_KEY`, o envio de e-mail de verificação é apenas logado no console (útil em dev).

### 2. Escolha como subir a aplicação

Há duas formas. Em ambas o PostgreSQL roda na porta **5433** e a API fica em **http://localhost:8080**.

#### Opção A - Desenvolvimento local

Docker apenas para o banco; a API roda na sua máquina no profile `dev`, com os usuários de seed carregados.

```bash
docker compose up -d banco-reconpay
cd backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Ideal para desenvolvimento, debug e execução de testes. E-mail e senha dos seeds estão em Segurança.

#### Opção B - Tudo via Docker

Sobe banco e API em containers no profile `prod`, usando o `.env` automaticamente. Não precisa instalar Java localmente, e **não há usuários de seed** — crie o primeiro ADMIN diretamente no banco.

```bash
docker compose up --build
```

Ideal para validar o projeto rapidamente ou demonstrar o ambiente completo.

### 3. Acesse

| Recurso | URL |
| :--- | :--- |
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| Métricas (autenticado) | http://localhost:8080/actuator/prometheus |

---

## Stack

| Camada | Tecnologias |
| :--- | :--- |
| Backend | Java 21, Spring Boot 3.5, Spring Web, Data JPA, Security, Bean Validation, MapStruct, Lombok |
| Banco | PostgreSQL, Flyway, Hibernate |
| Segurança | JWT (stateless) |
| Observabilidade | Log estruturado (JSON Logstash em `prod`), auditoria, Prometheus, tracing |
| Testes | JUnit 5, Mockito, MockMvc, AssertJ, Testcontainers |
| Infra | Docker, Docker Compose, GitHub Actions |

---

## Módulos

| Módulo | Responsabilidade |
| :--- | :--- |
| `auth` | Cadastro, login, verificação de e-mail, gerenciamento de usuários e grants |
| `security` | JWT, filtros, configuração de segurança e guard de acesso por merchant |
| `merchant` | Cadastro e gerenciamento de merchants |
| `feerule` | Regras de taxa por merchant |
| `transaction` | Transações internas por merchant |
| `externalsettlement` | Importação e consulta de liquidações externas |
| `reconciliation` | Motor de conciliação, divergências e relatórios CSV |
| `exception` | Tratamento global e respostas padronizadas (`StandardError`) |
| `observability` | Correlação de request, auditoria e contexto de log |
| `config` / `shared` | Configurações e utilitários compartilhados |

Estrutura interna de cada módulo:

```text
module/
├── controller/
├── dto/
├── entity/
├── mapper/
├── repository/
└── service/
```

---

## API

O contrato vivo está no Swagger, gerado a partir do código. Com a API no ar:

- UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs

Faça login (`POST /api/auth/login`), copie o token e use **Authorize** no Swagger (`Bearer {token}`). Em `dev`, os usuários seed estão na seção Segurança. Rotas públicas: cadastro, login, verificação de e-mail, Swagger e `/actuator/health`.

A conciliação é assíncrona: o POST devolve `202 Accepted` e o `Location` aponta para o GET da execução.

---

## Segurança

Autenticação JWT, sem sessão no servidor.

| Papel | O que faz |
| :--- | :--- |
| **ADMIN** | Usuários, grants e ativação de contas que ele criou. Enxerga todos os merchants. |
| **OPERATOR** | Fluxo operacional (merchant, taxas, transações, import, conciliação) só nos merchants concedidos. |

Quem cria um merchant recebe acesso a ele automaticamente. O auto-cadastro gera `OPERATOR` inativo até o link de e-mail. Contas criadas pelo ADMIN podem ser ativadas por ele.

Em `dev` e `test` existem usuários seed (nunca em produção):

| Role | E-mail | Senha |
| :--- | :--- | :--- |
| ADMIN | `admin@reconpay.local` | `DevAdmin@2026` |
| OPERATOR | `analyst@reconpay.local` | `DevAnalyst@2026` |

CORS em `dev` libera origens em `reconpay.cors.allowed-origins` (incluindo `http://localhost:3000`, Next.js) para `/api/**`.

---

## Frontend

O aplicativo Next.js fica em [`frontend/`](frontend/README.md). Com a API em `http://localhost:8080`, execute `cd frontend && corepack pnpm install && corepack pnpm dev` e acesse `http://localhost:3000`. O login, cadastro, seleção de merchant e transações recentes usam a API. Os indicadores agregados aguardam os endpoints do dashboard e aparecem sem valores fictícios.

---

## Testes

| Tipo | Ferramentas | Escopo |
| :--- | :--- | :--- |
| **Unitário** | JUnit 5, Mockito, MockMvc | Services, parsers, mappers e controllers |
| **Integração** | Testcontainers (PostgreSQL), MockMvc | Fluxos de ponta a ponta com banco real (precisa de Docker) |

```bash
cd backend && ./mvnw verify
```

Isso roda os testes e falha se a cobertura ficar abaixo de 85% das linhas ou 75% dos ramos. Na GitHub Actions o mesmo `verify` corre em todo push e PR para `main`, junto com scan OWASP (reprova CVSS ≥ 7) e `docker build` da imagem.

---

## Autor

**Hanrry Santos**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/hanrrysantos)
[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/hanrrysantos)

---
