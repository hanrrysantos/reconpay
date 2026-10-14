import type { PeriodSummaryDay } from '@/lib/api'
import { heatBand, type HeatBand } from '@/lib/charts'
import type { Window } from '@/lib/period'
import { cn } from '@/lib/utils'

const WEEKDAYS = ['S', 'T', 'Q', 'Q', 'S', 'S', 'D']

const BAND_CLASS: Record<HeatBand, string> = {
  full: 'bg-primary',
  soft: 'bg-primary-soft',
  neutral: 'bg-white',
  warn: 'bg-[#FFD9DA] text-danger-strong',
  bad: 'bg-danger text-white',
  empty: 'bg-white text-muted',
}

/** Monday = 0; the ISO date is read as UTC so the time zone never shifts the weekday. */
function weekdayIndex(iso: string): number {
  const [year, month, day] = iso.split('-').map(Number)
  return (new Date(Date.UTC(year, month - 1, day)).getUTCDay() + 6) % 7
}

function label(day: PeriodSummaryDay): string {
  const date = `${day.date.slice(8, 10)}/${day.date.slice(5, 7)}`
  if (day.itemCount === 0) return `${date}: sem itens`
  return `${date}: ${Math.floor((day.matchedCount * 100) / day.itemCount)}%`
}

export function DailyHeatmap({ days, window }: { days: PeriodSummaryDay[]; window: Window }) {
  const cells: Array<PeriodSummaryDay | null> = [...Array<null>(weekdayIndex(window.fromDate)).fill(null), ...days]
  const rows = Array.from({ length: Math.ceil(cells.length / 7) }, (_, index) => cells.slice(index * 7, index * 7 + 7))

  return (
    <section className="flex flex-col gap-5 rounded-lg bg-surface p-7 lg:w-[420px]">
      <h2 className="font-display text-[22px] font-extrabold tracking-[-0.03em]">Conciliação por dia</h2>
      <div role="grid" aria-label="Conciliação por dia" className="flex flex-col gap-1.5">
        <div role="row" className="grid grid-cols-7 gap-1.5">
          {WEEKDAYS.map((weekday, index) => <span key={index} role="columnheader" className="text-center text-[12px] text-muted">{weekday}</span>)}
        </div>
        {rows.map((row, rowIndex) => (
          <div key={rowIndex} role="row" className="grid grid-cols-7 gap-1.5">
            {row.map((day, index) => {
              if (day === null) return <span key={`blank-${index}`} role="gridcell" className="h-10" />
              const band = heatBand(day.matchedCount, day.itemCount)
              return (
                <span
                  key={day.date}
                  role="gridcell"
                  aria-label={label(day)}
                  title={label(day)}
                  data-band={band}
                  className={cn('grid h-10 place-items-center rounded-sm font-mono text-[12px] font-semibold', BAND_CLASS[band])}
                >
                  {Number(day.date.slice(8, 10))}
                </span>
              )
            })}
          </div>
        ))}
      </div>
      <div className="flex items-center gap-1.5 text-[12px] text-muted">
        <span>{'< 90%'}</span>
        {(['bad', 'warn', 'neutral', 'soft', 'full'] as const).map(band => (
          <span key={band} aria-hidden="true" className={cn('h-2.5 w-4 rounded-[3px]', BAND_CLASS[band], band === 'neutral' && 'shadow-[inset_0_0_0_1px_var(--color-border)]')} />
        ))}
        <span>100%</span>
      </div>
    </section>
  )
}
