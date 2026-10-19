# Validação — login-redesign

**Veredito: PASS**

- Diff: `3421a9e..c91ff8b` (branch `feat/login-redesign`, commit `c91ff8b feat(frontend): redesign the login screen`)
- Verifier independente (autor ≠ verifier), 2026-10-19, iteração 1
- Spec: `.specs/features/login-redesign/spec.md` (LOG-01..LOG-05 + 2 edges)
- Arquivos: produção `frontend/app/login/page.tsx`; testes `frontend/app/login/page.test.tsx`

Todos os critérios têm teste que afirma o resultado da spec (textos exatos, atributos, comportamento). O sensor injetou 26 mutantes: 23 morreram, 3 sobreviveram — 1 equivalente (defesa redundante) e 2 de layout que o jsdom não enxerga (`flex-col-reverse` no celular, ausência de `lg:flex-row` no desktop), cobertos por screenshots headless feitos por este verifier. Gate verde. A lógica de envio/erro/redirect é idêntica à de `3421a9e` (só markup e textos mudaram). Nenhum teste foi apagado, pulado ou relaxado (o arquivo de teste é novo).

---

## Evidência por critério

| Critério | Produção | Teste | Resultado da spec afirmado? | Status |
| --- | --- | --- | --- | --- |
| LOG-01 AC1 (`.specs/features/login-redesign/spec.md:66`) painel com marca, título, texto, rodapé + "Entrar" e subtítulo | `frontend/app/login/page.tsx:38-57` (aside), `frontend/app/login/page.tsx:62-63` | `frontend/app/login/page.test.tsx:33-43` — textos exatos dentro de `complementary` e `main`, `h1` "Entrar" | Sim nos textos. "Ao lado" (lado a lado) não é afirmável em jsdom → screenshot 1440 e 1024 (abaixo) | ✅ |
| LOG-01 AC2 (`.specs/features/login-redesign/spec.md:67`) abaixo de 1024 px: faixa com marca e título, sem texto e rodapé | `frontend/app/login/page.tsx:38` (`flex-col` / `lg:flex-row`), `frontend/app/login/page.tsx:49` (`hidden lg:block`), `frontend/app/login/page.tsx:53` (`hidden lg:flex`) | `frontend/app/login/page.test.tsx:45-52` — classes `hidden`+`lg:block`/`lg:flex` no texto e rodapé; marca e título sem `hidden` | Proxy por classe: razoável para "oculto/visível" (o `lg` do Tailwind é 1024 px por padrão e o screenshot 1023 confirma). Não cobre "acima do formulário" (M22 sobrevive) | ✅ (com screenshot) |
| LOG-02 AC1 (`.specs/features/login-redesign/spec.md:81`) rótulos, placeholders, botão | `frontend/app/login/page.tsx:68-69`, `frontend/app/login/page.tsx:72-79`, `frontend/app/login/page.tsx:105` | `frontend/app/login/page.test.tsx:23-25` (`getByLabelText('E-mail'/'Senha')`), `frontend/app/login/page.test.tsx:54-65` | Sim | ✅ |
| LOG-02 AC2 (`.specs/features/login-redesign/spec.md:82`) `required`, `type="email"`, `autocomplete` | `frontend/app/login/page.tsx:69`, `frontend/app/login/page.tsx:78-80` | `frontend/app/login/page.test.tsx:57-63` | Sim (atributos exatos) | ✅ |
| LOG-02 AC3 (`.specs/features/login-redesign/spec.md:83`) mostrar/ocultar, nome acessível, `aria-pressed` | `frontend/app/login/page.tsx:77`, `frontend/app/login/page.tsx:83-91` | `frontend/app/login/page.test.tsx:67-81` — `type` text/password, nomes "Mostrar senha"/"Ocultar senha", `aria-pressed` true/false | Sim | ✅ |
| LOG-02 AC4 (`.specs/features/login-redesign/spec.md:84`) "Criar conta" → `/register` | `frontend/app/login/page.tsx:110-113` | `frontend/app/login/page.test.tsx:83-87` (`href="/register"`) | Sim (href; navegação real é do `next/link`) | ✅ |
| LOG-03 AC1/AC3 (`.specs/features/login-redesign/spec.md:98`, `:100`) `signIn(email, senha)` e vai para `/` | `frontend/app/login/page.tsx:27-28` | `frontend/app/login/page.test.tsx:89-96` | Sim | ✅ |
| LOG-03 AC2 (`.specs/features/login-redesign/spec.md:99`) desabilitado com "Entrando..." e sem outro envio | `frontend/app/login/page.tsx:22`, `frontend/app/login/page.tsx:102`, `frontend/app/login/page.tsx:105` | `frontend/app/login/page.test.tsx:98-115` — `toBeDisabled`, "Entrando...", Enter no campo → `signIn` 1 vez; após concluir volta a "Entrar" habilitado | Sim. A asserção de 1 POST é carregante: com guarda **e** `disabled` removidos, falha em `frontend/app/login/page.test.tsx:107` ("called 2 times") | ✅ |
| LOG-04 AC1 (`.specs/features/login-redesign/spec.md:114`) mensagem em `role="alert"` acima do botão, mantendo valores | `frontend/app/login/page.tsx:30`, `frontend/app/login/page.tsx:94-99` | `frontend/app/login/page.test.tsx:117-129` — texto, `compareDocumentPosition` (alerta antes do botão), valores mantidos, sem redirect | Sim | ✅ |
| LOG-04 AC2 (`.specs/features/login-redesign/spec.md:115`) sem mensagem → fallback | `frontend/app/login/page.tsx:30` | `frontend/app/login/page.test.tsx:131-138` (rejeição com string) | Sim (texto exato) | ✅ |
| LOG-04 AC3 (`.specs/features/login-redesign/spec.md:116`) aviso some antes do novo envio | `frontend/app/login/page.tsx:24` | `frontend/app/login/page.test.tsx:140-157` — com o 2º `signIn` pendente, `alert` já ausente | Sim (afirmado durante o envio pendente, não só no fim) | ✅ |
| LOG-05 AC1 (`.specs/features/login-redesign/spec.md:130`) sessão ativa → `/` | `frontend/app/login/page.tsx:18` | `frontend/app/login/page.test.tsx:159-164`; negativo em `frontend/app/login/page.test.tsx:166-169` | Sim (positivo e negativo) | ✅ |
| Edge mostrar senha mantém o digitado (`.specs/features/login-redesign/spec.md:138`) | `frontend/app/login/page.tsx:74-82` (input não é recriado) | `frontend/app/login/page.test.tsx:73`, `frontend/app/login/page.test.tsx:79` | Sim | ✅ |
| Edge corrige só a senha (`.specs/features/login-redesign/spec.md:139`) | `frontend/app/login/page.tsx:23`, `frontend/app/login/page.tsx:27` | `frontend/app/login/page.test.tsx:140-157` (`toHaveBeenLastCalledWith` e-mail mantido + senha nova) | Sim | ✅ |

### Verificação visual independente (LOG-01)

`next build` + `next start -p 3917` (porta livre; 8080/8081 não tocadas) e `chrome-headless-shell` em 1440×900, 1024×800, 1023×800 e 390×844:

- 1440 e 1024: painel escuro à esquerda (marca, título, texto, rodapé) ao lado do formulário "Entrar" + subtítulo.
- 1023 e 390: faixa escura no topo só com marca e título; texto e rodapé ausentes; formulário em coluna única abaixo.
- A quebra cai exatamente em 1024 px, como a spec.

Conclusão sobre o proxy de classe: adequado para "texto e rodapé ocultos abaixo de `lg`" (é exatamente o que o Tailwind compila), mas não diz nada sobre ordem (faixa acima) nem sobre lado a lado; esses dois ficaram cobertos só pelos screenshots (meus e do autor), não por teste automatizado.

---

## Sensor de discriminação

Worktree descartável em `c91ff8b` (`frontend/node_modules` por symlink), um mutante por vez, só `frontend/app/login/page.tsx` restaurado entre mutantes. Comando: `corepack pnpm vitest run app/login` (12 testes; base verde).

| ID | Critério | Falha injetada | Resultado | Teste que matou |
| --- | --- | --- | --- | --- |
| M01 | LOG-03 AC2 | remove `if (busy) return` | **Sobrevive** | — |
| M02 | LOG-04 AC2 | fallback "Erro ao entrar." | Morto | `frontend/app/login/page.test.tsx:131` |
| M03 | LOG-04 AC3 | não limpa o erro antes do envio | Morto | `frontend/app/login/page.test.tsx:140` |
| M04 | LOG-02 AC3 | `aria-pressed={false}` fixo | Morto | `frontend/app/login/page.test.tsx:67` |
| M05 | LOG-02 AC3 | `type="password"` fixo | Morto | `frontend/app/login/page.test.tsx:67` |
| M06 | LOG-02 AC4 | `href="/signup"` | Morto | `frontend/app/login/page.test.tsx:83` |
| M07 | LOG-05 | efeito sem redirect | Morto | `frontend/app/login/page.test.tsx:159` |
| M08 | LOG-05 | redirect mesmo sem sessão | Morto | `frontend/app/login/page.test.tsx:166`, `:98`, `:117` |
| M09 | LOG-02 AC2 | `autoComplete="new-password"` | Morto | `frontend/app/login/page.test.tsx:54` |
| M10 | LOG-01 AC2 | texto do painel sem `hidden` | Morto | `frontend/app/login/page.test.tsx:45` |
| M11 | LOG-01 AC2 | rodapé sem `hidden` | Morto | `frontend/app/login/page.test.tsx:45` |
| M12 | LOG-04 AC1 | `<form key={error}>` (valores perdidos no erro) | Morto | `frontend/app/login/page.test.tsx:117`, `:140` |
| M13 | LOG-03 AC3 | sem `router.replace('/')` após `signIn` | Morto | `frontend/app/login/page.test.tsx:89`, `:98`, `:140` |
| M14 | LOG-02 AC3 | `aria-label="Mostrar senha"` fixo | Morto | `frontend/app/login/page.test.tsx:67` |
| M15 | LOG-03 AC2 | sem "Entrando..." | Morto | `frontend/app/login/page.test.tsx:98` |
| M16 | LOG-03 AC2 | `disabled={false}` | Morto | `frontend/app/login/page.test.tsx:98` |
| M17 | LOG-04 AC1 | aviso movido para depois do botão | Morto | `frontend/app/login/page.test.tsx:117` |
| M18 | LOG-02 AC2 | e-mail sem `required` | Morto | `frontend/app/login/page.test.tsx:54` |
| M19 | LOG-02 AC2 | e-mail `type="text"` | Morto | `frontend/app/login/page.test.tsx:54` |
| M20 | LOG-04 AC1 | `role="status"` no aviso | Morto | `frontend/app/login/page.test.tsx:117`, `:131`, `:140` |
| M21 | LOG-03 AC1 | e-mail e senha trocados no `signIn` | Morto | `frontend/app/login/page.test.tsx:89`, `:140` |
| M22 | LOG-01 AC2 | raiz `flex-col-reverse` (faixa abaixo do formulário no celular) | **Sobrevive** | — |
| M23 | LOG-01 AC1 | remove `lg:flex-row` (painel empilhado no desktop) | **Sobrevive** | — |
| M24 | edge mostrar senha | `key={String(showPassword)}` no input (recria e perde o valor) | Morto | `frontend/app/login/page.test.tsx:67` |
| M25 | LOG-03 AC2 | remove `setBusy(false)` do `finally` | Morto | `frontend/app/login/page.test.tsx:98`, `:140` |
| M26 | LOG-01 AC1 | subtítulo trocado | Morto | `frontend/app/login/page.test.tsx:33` |
| M01+M16 | LOG-03 AC2 | sem guarda e sem `disabled` (sonda: asserção `toBeDisabled` removida só no scratch) | Morto | `frontend/app/login/page.test.tsx:107` ("called 2 times") |

Classificação dos sobreviventes:

- **M01 — equivalente.** Com o botão `disabled`, o navegador (e o user-event) não faz submissão implícita, então a guarda nunca é exercida; além disso ela lê `busy` do closure e não barraria dois submits no mesmo tick. É defesa redundante. A sonda M01+M16 mostra que o resultado "um POST só" é afirmado e falha quando as duas defesas somem.
- **M22 / M23 — gap real, limitação do jsdom.** "Acima do formulário" e "ao lado do formulário" dependem de CSS de layout que o jsdom não calcula. Cobertos pelos screenshots, não por teste. Não bloqueia: a spec trata o visual como "comparar com as telas do Pen" no Independent Test.

Limpeza: `git worktree remove --force` + `git worktree prune`; `git status --porcelain` na árvore real ficou limpo antes de escrever esta validação.

---

## Gate (árvore real, `frontend/`)

| Comando | Resultado |
| --- | --- |
| `corepack pnpm test` | 66 arquivos, 878 testes, todos verdes |
| `corepack pnpm typecheck` | `next typegen && tsc --noEmit` sem erros |
| `corepack pnpm build` | build OK; `/login` prerenderizada como estática |

---

## Gaps (por prioridade, nenhum bloqueante)

1. **Layout sem teste automatizado (M22, M23).** Ordem faixa/formulário no celular e lado a lado no desktop só têm evidência por screenshot. Opções: asserção de classes da raiz (`flex-col` sem `-reverse`, `lg:flex-row`) e de ordem DOM `aside` antes de `main`, ou um teste visual/e2e se o projeto adotar um.
2. **Fallback de LOG-04 AC2 quase inalcançável na prática.** A tabela de suposições diz "sem mensagem (rede)", mas `signIn` usa `authenticate` de `frontend/lib/api.ts`, cujo `ApiError` sempre tem mensagem (rede → "Não foi possível conectar à API. Tente novamente.", `frontend/lib/api.ts:223`). Além disso, um `Error` com `message` vazia não mostra aviso nenhum (`frontend/app/login/page.tsx:30` + `:94`). Comportamento herdado de `3421a9e`; vale alinhar o texto da spec ("sem mensagem" = não-`Error`).
3. **Guarda `if (busy) return` redundante** (`frontend/app/login/page.tsx:22`, M01). Não tem efeito observável; inofensiva, herdada.
4. **Observação visual em 1024 px:** com o painel fixo em 640 px, o formulário fica com ~272 px de largura e o subtítulo quebra em duas linhas. Dentro da spec (que pede painel a partir de 1024 px), mas pode não bater com o Pen nessa largura.

---

## Depois do Verifier (autor)

- **Gap 1 fechado:** teste novo em `frontend/app/login/page.test.tsx:54` afirma que o painel (`aside`) vem antes do formulário (`main`) no mesmo pai, com `flex-col` (sem `-reverse`) e `lg:flex-row`. Os mutantes M22 (`flex-col-reverse`) e M23 (sem `lg:flex-row`) agora falham nesse teste.
- **Gap 2 fechado na spec:** a linha "Erro" das premissas diz que falha de rede chega com a mensagem do cliente e que só uma falha que não é `Error` mostra "Não foi possível entrar. Tente novamente.".
- **Gap 4 fechado:** entre 1024 e 1279 px o painel tem 440 px e o título 40 px (`frontend/app/login/page.tsx:38`); a partir de 1280 px, 640 px e 52 px como no desenho. Captura headless em 1024 px: formulário com os 400 px cheios.
- **Gap 3** fica: a guarda `if (busy) return` é redundante com o botão desabilitado e já existia antes.
