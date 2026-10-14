import { ArrowUpRight } from 'lucide-react'
import type { ReactNode } from 'react'
import type { Period, PeriodSummary } from '@/lib/api'
import { seriesPath } from '@/lib/charts'
import { formatMoney } from '@/lib/money'
import { MONTH_NAMES, monthWindow } from '@/lib/period'
import { cn } from '@/lib/utils'
import { formatPercent, openCount } from './format'

function Sparkline({ values, className }: { values: number[]; className?: string }) {
  if (values.length < 2) return null
  const max = Math.max(...values)
  return (
    <svg viewBox="0 0 100 28" preserveAspectRatio="none" className={cn('h-7 w-[100px] shrink-0 overflow-visible', className)} aria-hidden="true">
      <path d={seriesPath(values.map(String), max, 100, 28)} fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
    </svg>
  )
}

function Card({ label, value, badge, footer, spark, accent = false }: {
  label: string; value: string; badge?: ReactNode; footer?: ReactNode; spark?: ReactNode; accent?: boolean
}) {
  return (
    <article className={cn('flex min-w-0 flex-1 flex-col gap-4 rounded-lg p-6', accent ? 'bg-primary' : 'bg-surface')}>
      <header className="flex items-center justify-between gap-2">
        <h3 className="text-[14px]">{label}</h3>
        {badge}
      </header>
      <p className="font-display text-[30px] font-extrabold tracking-[-0.03em] break-all">{value}</p>
      <footer className="mt-auto flex items-end justify-between gap-3 text-[12px]">
        <span>{footer}</span>
        {spark}
      </footer>
    </article>
  )
}

const pill = (text: string) => <span className="rounded-full bg-white px-2.5 py-1 text-[12px] font-bold whitespace-nowrap">{text}</span>

/** "vs ago" for a whole calendar month, otherwise the previous window of the same length. */
function previousLabel(period: Period): string {
  const month = monthWindow(period.fromDate)
  if (month.fromDate !== period.fromDate || month.toDate !== period.toDate) return 'vs janela anterior'
  const index = (Number(period.fromDate.slice(5, 7)) + 10) % 12
  return `vs ${MONTH_NAMES[index].slice(0, 3).toLowerCase()}`
}

export function KpiCards({ period, summary }: { period: Period; summary: PeriodSummary | undefined }) {
  const recent = summary?.days.slice(-7) ?? []
  const open = summary ? openCount(summary) : null
  const grossChange = summary?.previousWindowGrossChangePercent
    ? formatPercent(summary.previousWindowGrossChangePercent, { signed: true })
    : null
  const feeShare = summary?.feePercent ? formatPercent(summary.feePercent) : null
  const aboveContract = summary && !/^0+(\.0*)?$/.test(summary.feeAboveContractAmount)

  return (
    <div className="flex min-w-0 flex-1 flex-col gap-4 md:flex-row">
      <Card
        accent
        label="Valor em aberto"
        value={formatMoney(period.openAmount)}
        badge={<span className="grid size-8 place-items-center rounded-full bg-foreground text-primary"><ArrowUpRight aria-hidden="true" className="size-4" /></span>}
        footer={open !== null && (open === 1 ? '1 divergência' : `${open.toLocaleString('pt-BR')} divergências`)}
        spark={<Sparkline values={recent.map(day => day.itemCount - day.matchedCount)} />}
      />
      <Card
        label="Vendido bruto"
        value={summary ? formatMoney(summary.grossAmount) : '—'}
        badge={grossChange && pill(`${grossChange} ${previousLabel(period)}`)}
        footer={<span className="text-muted">{period.totalItems.toLocaleString('pt-BR')} itens na janela</span>}
        spark={<Sparkline values={recent.map(day => Number(day.expectedNetAmount))} className="text-success" />}
      />
      <Card
        label="Taxas cobradas"
        value={summary ? formatMoney(summary.feeAmount) : '—'}
        badge={feeShare && pill(`${feeShare} do bruto`)}
        footer={summary && (
          <span className={aboveContract ? 'font-semibold text-danger' : 'text-muted'}>
            {formatMoney(summary.feeAboveContractAmount, { signed: true })} acima do contrato
          </span>
        )}
      />
    </div>
  )
}
