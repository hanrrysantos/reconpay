'use client'

import { useState } from 'react'
import type { PeriodSummaryDay } from '@/lib/api'
import { gapPolygon, seriesPath } from '@/lib/charts'
import { formatMoney } from '@/lib/money'
import { MONTH_NAMES } from '@/lib/period'

const W = 720
const H = 200

const dayLabel = (iso: string) => `${iso.slice(8, 10)} ${MONTH_NAMES[Number(iso.slice(5, 7)) - 1].slice(0, 3).toLowerCase()}`

/** Round axis top and step (1, 2, 2.5 or 5 × 10ⁿ) so the Y axis has four even intervals. */
function axis(max: number): { top: number; step: number } {
  const raw = max / 4
  const power = 10 ** Math.floor(Math.log10(raw))
  const step = [1, 2, 2.5, 5, 10].map(factor => factor * power).find(candidate => candidate >= raw)!
  return { top: step * 4, step }
}

const tick = (value: number) => value >= 1000 ? `${String(value / 1000).replace('.', ',')}k` : String(value)

export function CashflowChart({ days, bankReceivedAmount, expectedNetAmount }: {
  days: PeriodSummaryDay[]
  /** Window totals from the summary; the bank side is null when the run did not track bank credits. */
  bankReceivedAmount: string | null
  expectedNetAmount: string
}) {
  const [hovered, setHovered] = useState<number | null>(null)
  const empty = days.every(day => day.itemCount === 0)
  const hasBank = bankReceivedAmount !== null && days.every(day => day.bankReceivedAmount !== null)
  const expected = days.map(day => day.expectedNetAmount)
  const received = days.map(day => day.bankReceivedAmount ?? '0')
  const max = Math.max(1, ...expected.map(Number), ...(hasBank ? received.map(Number) : []))
  const { top, step } = axis(max)
  const ticks = [4, 3, 2, 1, 0].map(index => index * step)
  const xStep = days.length > 1 ? W / (days.length - 1) : 0
  const xMarks = days.length ? [...new Set([0, 7, 14, 21, days.length - 1].filter(index => index < days.length))] : []
  const day = hovered === null ? null : days[hovered]
  // The server's per-day shortfall (DASH-03 AC5, AD-010); null when the run has no bank data.
  const missing = day?.bankShortfallAmount ?? null

  return (
    <section className="flex min-w-0 flex-1 flex-col gap-5 rounded-lg bg-surface p-7">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h2 className="text-[14px] text-muted">Esperado × recebido no banco</h2>
          <p className="flex flex-wrap items-baseline gap-3">
            <span className="font-display text-[32px] font-extrabold tracking-[-0.03em]">{bankReceivedAmount === null ? '—' : formatMoney(bankReceivedAmount)}</span>
            <span className="font-mono text-[13px] text-muted">de {formatMoney(expectedNetAmount)} esperado</span>
          </p>
        </div>
        <ul className="flex flex-wrap items-center gap-4 text-[12px]">
          <li className="flex items-center gap-1.5"><span className="h-0 w-4 border-t-[1.5px] border-dashed border-foreground" />Líquido esperado</li>
          <li className="flex items-center gap-1.5"><span className="size-2.5 rounded-[3px] bg-primary" />Caiu no banco</li>
          <li className="flex items-center gap-1.5"><span className="size-2.5 rounded-[3px] bg-danger" />Faltou</li>
        </ul>
      </header>

      {!hasBank && !empty && <p className="text-[13px] text-muted">Extrato não disponível para este run</p>}

      {empty ? (
        <div className="grid h-[200px] place-items-center rounded-md border border-dashed border-border text-[14px] text-muted">Sem vendas nesta janela</div>
      ) : (
        <div className="flex flex-col gap-2">
          <div className="flex h-[200px] gap-2">
            <div className="flex w-[30px] shrink-0 flex-col justify-between font-mono text-[11px] text-muted" aria-hidden="true">
              {ticks.map(value => <span key={value} className="leading-none">{tick(value)}</span>)}
            </div>
            <div className="relative min-w-0 flex-1" onMouseLeave={() => setHovered(null)}>
              <svg viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" className="size-full overflow-visible" role="img" aria-label="Líquido esperado e recebido no banco por dia">
                {ticks.map(value => <line key={value} x1="0" x2={W} y1={H - (value / top) * H} y2={H - (value / top) * H} stroke="var(--color-border)" strokeWidth="1" vectorEffect="non-scaling-stroke" />)}
                {hasBank && <>
                  <path data-series="received-area" d={`${seriesPath(received, top, W, H)} L ${W} ${H} L 0 ${H} Z`} fill="var(--color-primary)" fillOpacity="0.45" />
                  <path data-series="gap" d={gapPolygon(expected, received, top, W, H)} fill="var(--color-danger)" fillOpacity="0.55" />
                  <path data-series="received" d={seriesPath(received, top, W, H)} fill="none" stroke="var(--color-success)" strokeWidth="2" vectorEffect="non-scaling-stroke" />
                </>}
                <path data-series="expected" d={seriesPath(expected, top, W, H)} fill="none" stroke="var(--color-foreground)" strokeWidth="1.5" strokeDasharray="4 4" vectorEffect="non-scaling-stroke" />
                {hovered !== null && <line x1={hovered * xStep} x2={hovered * xStep} y1="0" y2={H} stroke="var(--color-foreground)" strokeDasharray="2 3" vectorEffect="non-scaling-stroke" />}
                {days.map((item, index) => (
                  <rect key={item.date} data-day={item.date} x={index * xStep - xStep / 2} y="0" width={Math.max(xStep, 1)} height={H} fill="transparent" onMouseEnter={() => setHovered(index)} />
                ))}
              </svg>
              {day && (
                <div
                  role="tooltip"
                  className="pointer-events-none absolute top-2 z-10 flex -translate-x-1/2 flex-col gap-0.5 rounded-[12px] bg-foreground px-3 py-2 whitespace-nowrap text-white shadow-[0_8px_24px_rgba(0,0,0,0.18)]"
                  style={{ left: `${days.length > 1 ? (hovered! / (days.length - 1)) * 100 : 50}%` }}
                >
                  <span className="text-[11px] text-white/70">{dayLabel(day.date)} · esperado {formatMoney(day.expectedNetAmount)}</span>
                  {missing !== null && (
                    <span className="font-mono text-[13px] font-semibold">
                      {missing.startsWith('-') || /^0+\.00$/.test(missing) ? 'Nada faltou' : `Faltaram ${formatMoney(missing)}`}
                    </span>
                  )}
                </div>
              )}
            </div>
          </div>
          <div className="flex justify-between pl-[38px] font-mono text-[11px] text-muted">
            {xMarks.map(index => <span key={index}>{dayLabel(days[index].date)}</span>)}
          </div>
        </div>
      )}
    </section>
  )
}
