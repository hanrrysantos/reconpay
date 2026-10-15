# Tela de Empresas Specification

## Problem Statement

O fluxo do ReconPay começa pela empresa (merchant), mas `/empresas` mostra "em construção" e o menu de empresa e o estado "Nenhuma empresa no seu acesso" levam para lá. Sem tela, quem testa ou opera precisa do Swagger para cadastrar a primeira empresa, e nada depois dela (regras de taxa, transações, imports, conciliação) é alcançável pela interface. Além disso, a API grava o documento como texto livre: `12.345.678/0001-90` e `12345678000190` viram duas empresas diferentes.

## Goals

- [ ] Um usuário recém-cadastrado chega do login a uma empresa selecionada no menu do topo sem sair da interface.
- [ ] Listar, cadastrar, renomear e desativar empresas pela tela `/empresas`, com os mesmos limites de acesso da API.
- [ ] O documento é sempre um CNPJ válido de 14 dígitos, normalizado e checado contra duplicidade no backend, qualquer que seja a formatação enviada.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Regras de taxa | Feature seguinte (AD-012, `/taxas`) |
| Reativar empresa desativada / reutilizar o CNPJ dela | A API não tem reativação; `existsByDocument` considera inativas |
| Alterar o CNPJ de uma empresa existente | `PUT /api/merchants/{id}` só altera o nome |
| Busca no servidor na lista de empresas | `GET /api/merchants` não tem filtro; o menu do topo já busca por nome/CNPJ |
| Conceder acesso de empresa a outro usuário | Tela de admin futura (`PUT /api/users/{id}/merchants`); AD-004 |
| Migrar documentos já gravados com pontuação | Dado legado só é comparado na forma normalizada; nenhum UPDATE em produção |
| CPF ou documento estrangeiro | Decisão do usuário: CNPJ apenas |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Desativar empresa entra nesta feature | Sim, com diálogo de confirmação | Escolha do usuário; a API já faz soft delete | y |
| Formato do documento | CNPJ de 14 dígitos com dígitos verificadores; frontend com máscara envia só dígitos; backend normaliza e valida | Escolha do usuário: a regra não pode depender da tela | y |
| Depois de cadastrar | A nova empresa vira a selecionada no menu e o usuário fica em `/empresas` | Escolha do usuário | y |
| Apresentação dos formulários | Gaveta lateral, como a gaveta de resolução do dashboard (`Esc` fecha) | Escolha do usuário | y |
| Duplicidade com registros legados formatados | O backend compara só os dígitos do documento novo com só os dígitos dos documentos gravados (ativos ou não) | Pega duplicata legada sem reescrever dados de produção | n |
| Documento legado que não é CNPJ válido | Continua listado e exibido como está; não bloqueia edição do nome | Edição não toca no documento | n |
| Fonte da lista | `GET /api/merchants` (paginado, tem `createdAt`), ordenado por nome; o menu do topo continua em `/api/me/merchants` | Mesmo conjunto de empresas para ADMIN (todas ativas) e OPERATOR (grants) | n |
| Tamanho da página | 20 empresas, com navegação anterior/próxima | Padrão `@PageableDefault(size = 20)` da API | n |
| Empresa selecionada desativada | O menu passa para a primeira empresa restante, ou para o estado "sem empresa" | `useSelectedMerchant` já cai em `merchants[0]` | n |
| Mensagem de CNPJ inválido | "CNPJ inválido" (frontend e `400` do backend) | Uma só mensagem nos dois lados | n |
| Edição concorrente do nome | Última escrita vence | A entidade não tem `@Version`; nome não é dado financeiro | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Cadastrar empresa ⭐ MVP

**User Story**: As an OPERATOR or ADMIN, I want to cadastrar uma empresa pela tela so that eu comece o fluxo de conciliação sem usar o Swagger.

**Why P1**: É o primeiro passo do fluxo; sem ele nada mais é testável pela interface.

**Acceptance Criteria**:

1. WHEN o usuário clica em "Cadastrar empresa" em `/empresas` THEN the system SHALL abrir uma gaveta lateral com os campos Nome e CNPJ vazios.
2. WHEN o usuário digita no campo CNPJ THEN the system SHALL exibir o valor com a máscara `00.000.000/0000-00` e aceitar no máximo 14 dígitos.
3. WHEN o usuário envia nome e CNPJ válidos THEN the system SHALL chamar `POST /api/merchants` com `name` sem espaços nas pontas e `document` só com os 14 dígitos.
4. WHEN a API responde `201` THEN the system SHALL fechar a gaveta, mostrar a empresa na lista e no menu do topo e torná-la a empresa selecionada.
5. IF o nome está vazio (após remover espaços) ou tem mais de 150 caracteres THEN the system SHALL bloquear o envio e mostrar a mensagem do campo sem chamar a API.
6. IF o CNPJ não tem 14 dígitos ou os dígitos verificadores não conferem THEN the system SHALL bloquear o envio e mostrar "CNPJ inválido" sem chamar a API.
7. IF a API responde `409` THEN the system SHALL manter a gaveta aberta com os valores digitados e mostrar a mensagem do `StandardError`.
8. IF a API responde outro erro ou a rede falha THEN the system SHALL manter a gaveta aberta com os valores digitados e mostrar a mensagem do erro.
9. WHILE o cadastro está em envio the system SHALL desabilitar o botão de enviar, de modo que cliques repetidos gerem um único `POST`.
10. WHEN o usuário pressiona `Esc` ou "Cancelar" THEN the system SHALL fechar a gaveta sem chamar a API.

**Independent Test**: Com um OPERATOR sem empresas, abrir `/empresas`, cadastrar "Loja Teste" com um CNPJ válido e ver a empresa listada e selecionada no menu do topo.

---

### P1: CNPJ normalizado e validado no backend ⭐ MVP

**User Story**: As a operador, I want que a API recuse CNPJ inválido ou duplicado em qualquer formatação so that a mesma empresa não exista duas vezes por causa de pontuação.

**Why P1**: A regra não pode depender da tela; Swagger e clientes programáticos usam a mesma API.

**Acceptance Criteria**:

1. WHEN `POST /api/merchants` recebe um `document` com pontuação de CNPJ (`.`, `/`, `-`, espaços) THEN the system SHALL gravar só os 14 dígitos.
2. IF o `document` recebido, sem pontuação, não tem 14 dígitos ou falha nos dígitos verificadores THEN the system SHALL responder `400` `VALIDATION_ERROR` com a mensagem "CNPJ inválido".
3. IF o `document` recebido contém caractere diferente de dígito ou da pontuação de CNPJ THEN the system SHALL responder `400` `VALIDATION_ERROR` com a mensagem "CNPJ inválido".
4. IF os dígitos do `document` recebido são iguais aos dígitos do documento de qualquer empresa gravada, ativa ou desativada, inclusive gravada com pontuação antes desta feature, THEN the system SHALL responder `409` `CONFLICT`.
5. The system SHALL rejeitar como inválido um CNPJ com os 14 dígitos iguais (ex.: `00000000000000`, `11111111111111`).

**Independent Test**: Pela API, criar `12.345.678/0001-95`, ver `document` `12345678000195` na resposta, e receber `409` ao criar `12345678000195`.

---

### P1: Listar empresas ⭐ MVP

**User Story**: As an OPERATOR or ADMIN, I want ver as empresas do meu acesso em `/empresas` so that eu saiba quais existem e possa agir sobre elas.

**Why P1**: Sem lista não há onde editar ou desativar, nem confirmação visual do cadastro.

**Acceptance Criteria**:

1. WHEN o usuário abre `/empresas` THEN the system SHALL buscar `GET /api/merchants?page=0&size=20&sort=name,asc` e listar nome, CNPJ formatado e data de cadastro de cada empresa.
2. The system SHALL marcar na lista a empresa selecionada no menu do topo.
3. WHERE a API indica mais de uma página the system SHALL mostrar "Anterior" e "Próxima", com "Anterior" desabilitado na primeira página e "Próxima" desabilitado na última.
4. WHEN o usuário clica no nome de uma empresa da lista THEN the system SHALL torná-la a empresa selecionada no menu do topo.
5. IF a lista volta vazia THEN the system SHALL mostrar "Nenhuma empresa no seu acesso" com o botão "Cadastrar empresa".
6. WHILE a lista carrega the system SHALL mostrar o estado de carregamento no lugar da tabela.
7. IF a busca falha THEN the system SHALL mostrar a mensagem do erro com "Tentar de novo", que refaz a busca.
8. IF um documento gravado não tem 14 dígitos THEN the system SHALL exibi-lo como está, sem máscara.

**Independent Test**: Com o ADMIN do seed e três empresas cadastradas, abrir `/empresas` e ver as três em ordem alfabética, com a selecionada marcada.

---

### P2: Renomear empresa

**User Story**: As an OPERATOR or ADMIN, I want corrigir o nome de uma empresa so that o menu e os relatórios mostrem o nome certo.

**Why P2**: Útil, mas o fluxo de conciliação não depende dele.

**Acceptance Criteria**:

1. WHEN o usuário escolhe "Editar" numa empresa THEN the system SHALL abrir a gaveta com o nome atual editável e o CNPJ formatado somente leitura.
2. WHEN o usuário salva um nome válido THEN the system SHALL chamar `PUT /api/merchants/{id}` com `{ "name": <nome sem espaços nas pontas> }`.
3. WHEN a API responde `200` THEN the system SHALL fechar a gaveta e mostrar o novo nome na lista e no menu do topo.
4. IF o nome está vazio (após remover espaços) ou tem mais de 150 caracteres THEN the system SHALL bloquear o envio sem chamar a API.
5. IF a API responde erro ou a rede falha THEN the system SHALL manter a gaveta aberta com o nome digitado e mostrar a mensagem do erro.
6. WHILE a edição está em envio the system SHALL desabilitar o botão de salvar.

**Independent Test**: Renomear "Loja Teste" para "Loja Teste Matriz" e ver o nome novo na lista e no menu do topo.

---

### P2: Desativar empresa

**User Story**: As an OPERATOR or ADMIN, I want desativar uma empresa que não uso mais so that ela saia da lista e do menu.

**Why P2**: Limpeza; não bloqueia o fluxo.

**Acceptance Criteria**:

1. WHEN o usuário escolhe "Desativar" numa empresa THEN the system SHALL abrir um diálogo de confirmação com o nome da empresa e o aviso de que o CNPJ não poderá ser cadastrado de novo.
2. WHEN o usuário confirma THEN the system SHALL chamar `DELETE /api/merchants/{id}`.
3. WHEN a API responde `204` THEN the system SHALL fechar o diálogo e remover a empresa da lista e do menu do topo.
4. IF a empresa desativada era a selecionada THEN the system SHALL selecionar a primeira empresa restante do menu, ou mostrar o estado "sem empresa" quando não restar nenhuma.
5. WHEN o usuário cancela ou pressiona `Esc` THEN the system SHALL fechar o diálogo sem chamar a API.
6. IF a API responde erro ou a rede falha THEN the system SHALL manter o diálogo aberto e mostrar a mensagem do erro.

**Independent Test**: Desativar a empresa selecionada e ver o menu do topo passar para a próxima empresa.

---

## Edge Cases

- IF a API responde `401` em qualquer ação THEN the system SHALL encerrar a sessão e voltar ao login (comportamento global existente).
- IF a API responde `403` ou `404` ao editar ou desativar (empresa sem acesso ou já desativada em outra aba) THEN the system SHALL mostrar a mensagem do erro e recarregar a lista.
- WHEN o usuário cola um CNPJ formatado no campo THEN the system SHALL aceitar e reformatar com a máscara.
- WHEN o nome tem exatamente 150 caracteres THEN the system SHALL aceitar o envio.

---

## Implicit-Requirement Sweep

| Dimension | Resolution |
| --- | --- |
| Input validation & bounds | Nome 1–150 após trim (MCAD-05, MEDIT-04); CNPJ 14 dígitos + verificadores no frontend (MCAD-06) e no backend (MDOC-02, MDOC-03, MDOC-05) |
| Failure / partial-failure states | Gaveta e diálogo mantêm o estado e mostram o erro (MCAD-07, MCAD-08, MEDIT-05, MDES-06); lista com "Tentar de novo" (MLIST-07) |
| Idempotency / retry / duplicate handling | Botão desabilitado durante envio (MCAD-09, MEDIT-06); duplicidade por dígitos no backend (MDOC-04) |
| Auth boundaries & rate limits | Lista segue o escopo da API (ADMIN todas, OPERATOR grants); `401`/`403` nos edge cases; rate limit N/A because a API não tem throttle |
| Concurrency / ordering | Última escrita vence no nome (assumption); remoção concorrente vira `404` tratado nos edge cases |
| Data lifecycle / expiry | Desativar é soft delete e o CNPJ fica reservado (MDES-01 avisa); sem reativação (out of scope) |
| Observability | N/A because `AuditLogger` já registra `MERCHANT_CREATED`, `MERCHANT_UPDATED` e `MERCHANT_DEACTIVATED` |
| External-dependency failure | N/A because a feature não chama serviço externo |
| State-transition integrity | Só ativa → desativada (MDES-02); editar não altera o CNPJ (MEDIT-01) |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| MCAD-01 | P1: Cadastrar empresa | Design | Pending |
| MCAD-02 | P1: Cadastrar empresa | Execute | Implementing |
| MCAD-03 | P1: Cadastrar empresa | Execute | Implementing |
| MCAD-04 | P1: Cadastrar empresa | Execute | Implementing |
| MCAD-05 | P1: Cadastrar empresa | Design | Pending |
| MCAD-06 | P1: Cadastrar empresa | Execute | Implementing |
| MCAD-07 | P1: Cadastrar empresa | Design | Pending |
| MCAD-08 | P1: Cadastrar empresa | Design | Pending |
| MCAD-09 | P1: Cadastrar empresa | Design | Pending |
| MCAD-10 | P1: Cadastrar empresa | Design | Pending |
| MDOC-01 | P1: CNPJ no backend | Execute | Implementing |
| MDOC-02 | P1: CNPJ no backend | Execute | Implementing |
| MDOC-03 | P1: CNPJ no backend | Execute | Implementing |
| MDOC-04 | P1: CNPJ no backend | Execute | Implementing |
| MDOC-05 | P1: CNPJ no backend | Execute | Implementing |
| MLIST-01 | P1: Listar empresas | Execute | Implementing |
| MLIST-02 | P1: Listar empresas | Execute | Implementing |
| MLIST-03 | P1: Listar empresas | Execute | Implementing |
| MLIST-04 | P1: Listar empresas | Execute | Implementing |
| MLIST-05 | P1: Listar empresas | Execute | Implementing |
| MLIST-06 | P1: Listar empresas | Execute | Implementing |
| MLIST-07 | P1: Listar empresas | Execute | Implementing |
| MLIST-08 | P1: Listar empresas | Execute | Implementing |
| MEDIT-01 | P2: Renomear empresa | Design | Pending |
| MEDIT-02 | P2: Renomear empresa | Execute | Implementing |
| MEDIT-03 | P2: Renomear empresa | Execute | Implementing |
| MEDIT-04 | P2: Renomear empresa | Design | Pending |
| MEDIT-05 | P2: Renomear empresa | Design | Pending |
| MEDIT-06 | P2: Renomear empresa | Design | Pending |
| MDES-01 | P2: Desativar empresa | Design | Pending |
| MDES-02 | P2: Desativar empresa | Execute | Implementing |
| MDES-03 | P2: Desativar empresa | Execute | Implementing |
| MDES-04 | P2: Desativar empresa | Execute | Implementing |
| MDES-05 | P2: Desativar empresa | Design | Pending |
| MDES-06 | P2: Desativar empresa | Design | Pending |

**ID format:** `M<STORY>-<NN>` (`MCAD`, `MDOC`, `MLIST`, `MEDIT`, `MDES`); o número é a posição do critério na story.

**Coverage:** 35 total, 0 mapped to tasks, 35 unmapped ⚠️ (tasks ainda não criadas)

---

## Success Criteria

- [ ] Do login à primeira empresa selecionada sem Swagger, em menos de 1 minuto.
- [ ] Nenhuma combinação de pontuação do mesmo CNPJ cria uma segunda empresa.
- [ ] Backend e frontend passam no gate (`./mvnw verify`, `pnpm test`, `typecheck`, `build`) com os testes novos.
