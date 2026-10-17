# Tela de Fechamento Design

**Spec**: `.specs/features/closing-screen/spec.md`
**Status**: Approved

---

## Architecture Overview

Só frontend. A página lê os runs da empresa (`useRuns`, os 100 mais recentes), tira deles as janelas distintas em ordem de calendário, pagina 10 por vez no cliente e monta uma linha por janela. Cada linha é um componente próprio que lê o seu período (`usePeriod`, mesma chave de cache do dashboard e de `/divergencias`) e tem as suas mutações (trava, reabertura, export). Os erros das ações sobem para a página, que mostra um aviso único acima da tabela.

```mermaid
graph TD
    R[app/(app)/fechamento/page.tsx] --> P[ClosingPage]
    P --> SM[useSelectedMerchant]
    P --> RU[useRuns]
    P --> W[periodWindows]
    P -->|key merchantId| CW[ClosingWindows: página + aviso]
    CW --> ROW[PeriodRow x 10]
    ROW --> PE[usePeriod]
    ROW --> LK[useLockPeriod / useUnlockPeriod]
    ROW --> EX[useExportRun]
    ROW --> CF[Confirm]
    LK --> INV[invalidateWindow: period, runs, summary...]
```

---

## Code Reuse Analysis

### Existing Components to Leverage

| Component | Location | How to Use |
| --- | --- | --- |
| `useRuns` | `frontend/lib/queries.ts:85` | Fonte das janelas (100 runs mais recentes) |
| `usePeriod` | `frontend/lib/queries.ts:107` | Leitura do período da linha; chave `queryKeys.period` compartilhada |
| `useLockPeriod`, `useUnlockPeriod` | `frontend/lib/queries.ts:193-203` | Trava e reabertura; já invalidam a janela (`invalidateWindow`), o que cobre FEC-05 AC6 |
| `useExportRun` | `frontend/lib/queries.ts:248` | CSV do run da linha |
| `Confirm` (interno) | `frontend/components/dashboard/CloseStepper.tsx:24` | Passa a ser exportado e reusado na linha, com os mesmos textos |
| `formatWindow` | `frontend/lib/period.ts:38` | Nome da janela |
| `formatRate`, `formatMoney` | `frontend/components/dashboard/format.ts` | Match e Em aberto |
| `BlockError`, `NoMerchant` | `frontend/components/dashboard/states.tsx` | Erro da lista e sem empresa |
| `useSelectedMerchant` | `frontend/components/shell/MerchantMenu.tsx` | Empresa selecionada |
| Download do CSV | `frontend/components/dashboard/DashboardPage.tsx:160-174` | Mesmo padrão `createObjectURL` + `<a download>` e mesmo nome de arquivo |
| Paginação | `frontend/components/discrepancies/DiscrepancyList.tsx:113-124` | Mesmo markup de "Página X de Y", "Anterior", "Próxima" |

### Integration Points

| System | Integration Method |
| --- | --- |
| `GET /reconciliations?size=100` | `useRuns`; sem endpoint novo |
| `GET /periods?fromDate&toDate` | Uma leitura por linha da página exibida |
| `POST /periods/lock`, `POST /periods/unlock` | Hooks existentes |
| `GET /reconciliations/{runId}/export` | `useExportRun` com o `runId` do período |

---

## Components

### `lib/closing.ts`

- **Purpose**: Regras puras da tela.
- **Location**: `frontend/lib/closing.ts`
- **Interfaces**:
  - `CLOSING_PAGE_SIZE = 10`
  - `periodWindows(runs: readonly Pick<Run, 'fromDate' | 'toDate'>[]): Window[]` - janelas distintas, data inicial desc, empate pela final desc
  - `formatDuration(seconds: number | null): string` - "29 s", "3 min", "2 h", "2 h 5 min", "1 d", "1 d 3 h", "—"
  - `formatLockedAt(iso: string | null): string` - `dd/mm/aaaa hh:mm` local, "—" no nulo
  - `periodStatus(query): 'locked' | 'open' | 'running' | 'none' | 'error' | 'loading'` - pela resposta (`200`, `409`, `404`, outro erro)
  - `PERIOD_STATUS_LABELS` - "Travado", "Aberto", "Em andamento", "Sem conciliação concluída"
- **Dependencies**: `ApiError` (status)
- **Reuses**: nada novo

### `PeriodRow`

- **Purpose**: Uma linha da tabela: lê o período, mostra as colunas e faz as ações.
- **Location**: `frontend/components/closing/PeriodRow.tsx`
- **Interfaces**:
  - `PeriodRow({ merchantId, window, onError }: { merchantId: string; window: Window; onError: (message: string | null) => void })`
- **Behavior**:
  - "Travar" (status `open`) e "Destravar" (status `locked`) abrem o `Confirm`; só a confirmação chama a mutação.
  - Antes de cada ação, `onError(null)`; no erro, `onError(mensagem)`; num `409`, `refetch()` do período.
  - No sucesso, `setQueryData(queryKeys.period(...), resposta)`, como o dashboard, além da invalidação dos hooks.
  - "Exportar CSV" (status `open` e `locked`) baixa `reconpay-<from>-<to>.csv`.
  - Com qualquer mutação da linha em andamento, os botões da linha ficam desabilitados.
  - Status `error`: "Não carregou" e "Tentar novamente" (`refetch`).
- **Accessible names**: botões "Travar <janela>", "Destravar <janela>", "Exportar CSV de <janela>", "Tentar novamente <janela>", com `<janela>` vindo de `formatWindow`.
- **Reuses**: `usePeriod`, `useLockPeriod`, `useUnlockPeriod`, `useExportRun`, `Confirm`, formatadores

### `ClosingPage`

- **Purpose**: A rota `/fechamento`: estados da empresa e da lista, paginação, aviso e tabela.
- **Location**: `frontend/components/closing/ClosingPage.tsx`
- **Interfaces**:
  - `ClosingPage()` - sem empresa → `NoMerchant`; runs carregando → skeleton; erro → `BlockError` com "Tentar novamente"; sem runs → aviso com link
  - `ClosingWindows({ merchantId, merchantName, windows })` (interno, `key={merchantId}`) - página e aviso; trocar de empresa remonta e zera os dois
- **Reuses**: `useSelectedMerchant`, `useRuns`, `periodWindows`, `PeriodRow`

### Rota

- `frontend/app/(app)/fechamento/page.tsx` passa a renderizar `<ClosingPage />`.
- Com isso nenhuma rota usa mais `UnderConstruction`; o componente e o teste dele (que só cobria `/fechamento`) são removidos como código morto, como as entradas removidas das telas anteriores (`34723d9`, `a022791`).

---

## Data Models

Sem modelo novo. Usa `Run`, `Period` e `Window` de `frontend/lib/api.ts` e `frontend/lib/period.ts`.

---

## Error Handling Strategy

| Error Scenario | Handling | User Impact |
| --- | --- | --- |
| Lista de runs falha | `BlockError` com a mensagem e "Tentar novamente" (`runs.refetch`) | Sem tabela até a busca dar certo |
| Período `409` | Status "Em andamento", sem ações | Linha visível, sem botão |
| Período `404` | Status "Sem conciliação concluída", sem ações | Idem |
| Período com outro erro | "Não carregou" + "Tentar novamente" na linha | As outras linhas seguem normais |
| Trava `409` (divergência aberta, já travada) | Aviso com a mensagem da API; relê o período | A linha mostra o estado real |
| Reabertura `409` | Idem | Idem |
| Export falha | Aviso com a mensagem | Nada baixa |
| `401` | Fim da sessão (comportamento atual do `QueryClient`) | Volta para o login |

---

## Risks & Concerns

| Concern | Location (file:line) | Impact | Mitigation |
| --- | --- | --- | --- |
| Uma leitura de período por linha (N chamadas) | `frontend/lib/queries.ts:107` | Até 10 GETs por página | Paginação de 10 no cliente; só as janelas da página leem (FEC-03 AC4, testado) |
| `usePeriod` usa `placeholderData` que mantém dados da janela anterior | `frontend/lib/queries.ts:113` | Uma linha poderia mostrar dados de outra janela se a chave mudasse no mesmo observador | Cada linha é montada com `key` da janela, então a chave nunca muda num observador; teste de troca de página confere os valores da página nova |
| `invalidateWindow` também invalida `runs` | `frontend/lib/queries.ts:165` | A lista de janelas é recarregada depois de travar | Inofensivo: as janelas não mudam com a trava; a página atual é mantida |
| Lógica de download duplicada (dashboard, detalhe do run) | `DashboardPage.tsx:160`, `RunDetailPage.tsx:36` | Terceira cópia | Aceito para não mexer em telas fora do escopo; a linha segue o mesmo padrão |
| `Confirm` é interno da régua | `CloseStepper.tsx:24` | Reuso exige exportar | Exportar sem mudar comportamento; testes da régua inalterados |

---

## Tech Decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Estado da linha | Um componente por linha com os próprios hooks | Hooks de mutação são por janela (`useLockPeriod(merchantId, window)`); evita `useQueries` + mutações dinâmicas |
| Atualizar a linha depois da ação | `setQueryData` com a resposta + invalidação existente | Mesma regra do dashboard; a linha muda sem esperar o refetch |
| Aviso de erro | Na página, um só, acima da tabela | A spec pede um aviso único que some na próxima ação |
