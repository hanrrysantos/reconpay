import { Download, RefreshCw } from 'lucide-react'
import type { ReactNode } from 'react'
import { formatWindow, type Window } from '@/lib/period'
import { cn } from '@/lib/utils'
import { capitalize, formatRate, openCount, windowName } from './format'
import type { DashboardView } from './view'

function headline(view: DashboardView, window: Window): string | null {
  const name = windowName(window)
  switch (view.kind) {
    case 'loading': return null
    case 'noMerchant':
    case 'forbidden': return 'Visão geral'
    case 'empty': return `${capitalize(name)} ainda não foi conciliado`
    case 'running': return `Conciliando ${name}…`
    case 'failed': return `A conciliação de ${name} falhou`
    case 'locked': return `${capitalize(name)} fechado em ${formatRate(view.data.period.matchRate)}`
    case 'open': {
      if (!view.data.summary) return `Fechamento de ${name}`
      const open = openCount(view.data.summary)
      if (open === 0) return `${capitalize(name)} sem divergências abertas`
      return open === 1 ? `Falta 1 divergência para fechar ${name}` : `Faltam ${open} divergências para fechar ${name}`
    }
  }
}

export function PageHeader({ view, window, periodMenu, onRun, onExport }: {
  view: DashboardView
  window: Window
  periodMenu?: ReactNode
  onRun: () => void
  onExport: () => void
}) {
  const title = headline(view, window)
  const running = view.kind === 'running'
  const canRun = view.kind === 'open' || view.kind === 'failed'

  return (
    <section className="flex flex-wrap items-end justify-between gap-6">
      <div className="flex min-w-0 flex-col gap-2">
        <p className="text-[14px] text-muted">Fechamento · {formatWindow(window)}</p>
        {title === null
          ? <div aria-hidden="true" className="h-11 w-[520px] max-w-full animate-pulse rounded-sm bg-surface" />
          : <h1 className="font-display text-[44px] leading-none font-extrabold tracking-[-0.04em]">{title}</h1>}
      </div>

      <div className="flex flex-wrap items-center gap-2">
        {periodMenu}
        {running ? (
          <button type="button" disabled className="flex h-11 items-center gap-2 rounded-full bg-surface px-5 text-[14px] font-semibold text-muted">
            <RefreshCw aria-hidden="true" className="size-4 animate-spin" />
            Conciliando…
          </button>
        ) : (
          <button
            type="button"
            disabled={!canRun}
            onClick={onRun}
            className={cn(
              'flex h-11 items-center gap-2 rounded-full px-5 text-[14px] font-semibold',
              canRun ? 'bg-primary text-foreground' : 'bg-surface text-muted',
            )}
          >
            <RefreshCw aria-hidden="true" className="size-4" />
            Conciliar período
          </button>
        )}
        {view.kind === 'locked' && (
          <button type="button" onClick={onExport} className="flex h-11 items-center gap-2 rounded-full bg-foreground px-5 text-[14px] font-semibold text-white">
            <Download aria-hidden="true" className="size-4 text-primary" />
            Exportar relatório
          </button>
        )}
      </div>
    </section>
  )
}
