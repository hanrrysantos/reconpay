import type { DiscrepancyType } from '@/lib/api'
import { donutSegments } from '@/lib/charts'
import { TYPE_LABELS } from './format'

const R = 54
const COLORS = ['var(--color-foreground)', 'var(--color-primary)', 'var(--color-success)', '#B9BDB2', 'var(--color-danger)']

export function TypeDonut({ openByType }: { openByType: Partial<Record<DiscrepancyType, number>> }) {
  // Largest first; the sort is stable, so ties keep the API order.
  const entries = Object.entries(openByType)
    .filter((entry): entry is [DiscrepancyType, number] => (entry[1] ?? 0) > 0)
    .sort((a, b) => b[1] - a[1])
  const segments = donutSegments(Object.fromEntries(entries), R, 3)
  const total = entries.reduce((sum, [, count]) => sum + count, 0)

  return (
    <section className="flex flex-col gap-5 rounded-lg bg-surface p-7 lg:w-[420px]">
      <h2 className="font-display text-[22px] font-extrabold tracking-[-0.03em]">Divergências por tipo</h2>
      <div className="flex items-center gap-7">
        <div className="relative size-[132px] shrink-0">
          <svg viewBox="0 0 132 132" className="size-full -rotate-90" aria-hidden="true">
            <circle cx="66" cy="66" r={R} fill="none" stroke="var(--color-border)" strokeWidth="18" />
            {segments.map((segment, index) => (
              <circle
                key={segment.key}
                data-segment={segment.key}
                cx="66" cy="66" r={R} fill="none" strokeWidth="18"
                stroke={COLORS[index % COLORS.length]}
                strokeDasharray={segment.dasharray}
                strokeDashoffset={segment.dashoffset}
              />
            ))}
          </svg>
          <p data-testid="donut-total" className="absolute inset-0 flex flex-col items-center justify-center">
            <span className="font-display text-[28px] leading-none font-extrabold tracking-[-0.03em]">{total.toLocaleString('pt-BR')}</span>{' '}
            <span className="text-[12px] text-muted">abertas</span>
          </p>
        </div>
        <ul aria-label="Tipos de divergência" className="flex min-w-0 flex-1 flex-col gap-[9px] text-[13px]">
          {entries.map(([type, count], index) => (
            <li key={type} className="flex items-center gap-2">
              <span aria-hidden="true" className="size-2.5 shrink-0 rounded-[3px]" style={{ background: COLORS[index % COLORS.length] }} />
              <span className="min-w-0 flex-1 truncate">{TYPE_LABELS[type]}</span>
              <span className="font-mono">{count.toLocaleString('pt-BR')}</span>
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}
