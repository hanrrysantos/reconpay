# ReconPay

API de conciliação financeira (monólito modular, Java 21, Spring Boot 3). Este arquivo é o mapa permanente do agente: o mínimo para não errar o caminho. **Não copie domínio, decisões ou specs para cá.**

## Onde procurar

Carregue só o que a tarefa precisa. Não abra várias features de uma vez.

| Precisa de | Leia |
|---|---|
| Como rodar, API, regras de negócio | `README.md` |
| Decisões de produto/arquitetura (`AD-NNN`) e handoff | `.specs/STATE.md` |
| Feature em andamento | `.specs/features/<nome>/spec.md` e, se existirem, `context.md`, `design.md`, `tasks.md`, `validation.md` |
| Lições confirmadas | `.specs/LESSONS.md` (se existir) |
| CI | `.github/workflows/ci.yml` |

Nova feature ou implementação com spec: skill `tlc-spec-driven`. Resume: comece por `.specs/STATE.md` e reconcilie com git.

## Stack e forma

- Pacote raiz: `br.com.hanrry.reconpay`
- Módulos: `auth`, `security`, `merchant`, `feerule`, `transaction`, `externalsettlement`, `bankstatement`, `reconciliation`, `exception`, `config`, `shared`, `observability`
- Persistência: PostgreSQL + Flyway (não use `ddl-auto` para migrar)
- Erros HTTP no formato `StandardError` (`ApiErrorCode`)
- Logging: YAML (`application.yaml` / `application-prod.yaml`). `dev`/`test` em texto; `prod` em JSON Logstash. Sem `logback-spring.xml`
- Testes: JUnit 5, Mockito, MockMvc, AssertJ, Testcontainers (`postgres:16-alpine`). Integração herda `AbstractIntegrationTest`
- Coverage no `verify`: 85% linha, 75% branch (JaCoCo)

## Convenções

- Commits em Conventional Commits, em inglês, um assunto por commit. Não faça push sem pedido explícito.
- Não enfraqueça, pule ou apague teste para passar o gate.
- Não invente papéis, owner de merchant, refresh token ou fila distribuída — isso está em `.specs/STATE.md` e no roadmap do README.
- Secrets só via env / `.env` (gitignorado). Nunca commitar credenciais.
- Profiles: `dev`, `test`, `prod`. Sem profile, vale o `application.yaml`.
