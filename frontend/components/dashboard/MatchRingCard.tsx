import type { ReactNode } from 'react'
import type { Period, PeriodSummary, Run } from '@/lib/api'
import { ringDash } from '@/lib/charts'
import { cn } from '@/lib/utils'
import { formatRate, formatRateDelta, openCount } from './format'

const R = 70
const CIRCUMFERENCE = 439.8
const count = (value: number) => value.toLocaleString('pt-BR')

function Ring({ dasharray, label, children, spinning = false }: { dasharray: string; label?: string; children?: ReactNode; spinning?: boolean }) {
  return (
    <div className="relative size-[168px] shrink-0">
      <svg viewBox="0 0 168 168" className={cn('size-full -rotate-90', spinning && 'animate-spin [animation-duration:1.6s]')} aria-hidden="true">
        <circle cx="84" cy="84" r={R} fill="none" stroke="rgba(255,255,255,0.1)" strokeWidth="16" />
        <circle data-arc cx="84" cy="84" r={R} fill="none" stroke="var(--color-primary)" strokeWidth="16" strokeLinecap="round" strokeDasharray={dasharray} />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        {label !== undefined && <span data-testid="ring-value" className="font-display text-[34px] font-extrabold tracking-[-0.04em]">{label}</span>}
        {children}
      </div>
    </div>
  )
}

function Stat({ value, label, className }: { value: string; label: string; className?: string }) {
  return (
    <div className="flex flex-col">
      <span className={cn('font-mono text-[22px] font-semibold', className)}>{value}</span>
      <span className="text-[12px] text-white/60">{label}</span>
    </div>
  )
}

/** Match ring of the window. Running runs show progress instead. */
export function MatchRingCard({ period, summary, run }: { period: Period | undefined; summary: PeriodSummary | undefined; run: Run | null | undefined }) {
  const running = run?.status === 'PENDING' || run?.status === 'RUNNING'

  if (running) {
    const known = run.processedItems !== null && run.totalItems !== null && run.totalItems > 0
    const percent = known ? Math.floor((run.processedItems! * 100) / run.totalItems!) : undefined
    return (
      <section className="flex flex-col gap-6 rounded-lg bg-foreground p-7 text-white lg:w-[420px]">
        <header className="flex items-center justify-between gap-3">
          <h2 className="text-[14px] text-white/75">Taxa de conciliação</h2>
          <span className="rounded-full bg-white/12 px-2.5 py-1 font-mono text-[12px] font-bold">{run.status}</span>
        </header>
        <div role="progressbar" aria-label="Progresso do run" aria-valuemin={0} aria-valuemax={100} aria-valuenow={percent} className="flex items-center gap-7">
          {known
            ? <Ring dasharray={ringDash(String(run.processedItems! / run.totalItems!), CIRCUMFERENCE)} label={`${percent}%`}><span className="text-[12px] text-white/60">processado</span></Ring>
            : <Ring dasharray={`${CIRCUMFERENCE / 4} ${CIRCUMFERENCE}`} spinning><span className="text-[12px] text-white/60">conciliando</span></Ring>}
          <p className="text-[13px] text-white/75">
            {known ? `${count(run.processedItems!)} de ${count(run.totalItems!)} itens processados` : 'Calculando progresso…'}
          </p>
        </div>
        <p className="rounded-md bg-white/7 px-4 py-3 text-[13px] text-white/75">Os blocos abaixo mostram o run anterior</p>
      </section>
    )
  }

  if (!period) return null
  const open = summary ? openCount(summary) : null
  const delta = summary?.previousRunMatchRate != null && period.matchRate !== null
    ? formatRateDelta(period.matchRate, summary.previousRunMatchRate)
    : null

  return (
    <section className="flex flex-col gap-6 rounded-lg bg-foreground p-7 text-white lg:w-[420px]">
      <header className="flex items-center justify-between gap-3">
        <h2 className="text-[14px] text-white/75">Taxa de conciliação</h2>
        {period.locked
          ? <span className="rounded-full bg-primary px-2.5 py-1 text-[12px] font-bold text-foreground">Travado</span>
          : delta && <span className="rounded-full bg-primary px-2.5 py-1 text-[12px] font-bold text-foreground">{delta} vs run anterior</span>}
      </header>
      <div className="flex items-center gap-7">
        <Ring dasharray={ringDash(period.matchRate ?? '0', CIRCUMFERENCE)} label={formatRate(period.matchRate)}>
          <span className="text-[12px] text-white/60">conciliado</span>
        </Ring>
        <div className="flex flex-col gap-3">
          <Stat value={count(period.matchedCount)} label="bateram" />
          <Stat value={summary ? count(summary.resolvedCount) : '—'} label="resolvidas" />
          <Stat value={open === null ? '—' : count(open)} label="abertas" className="text-[#FF7A7E]" />
        </div>
      </div>
      <p className="rounded-md bg-white/7 px-4 py-3 text-[13px] text-white/75">
        {count(period.matchedCount)} de {count(period.totalItems)} itens conciliados
      </p>
    </section>
  )
}
