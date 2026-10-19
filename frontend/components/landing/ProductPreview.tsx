import { Check, ChevronDown } from 'lucide-react'

const TABS = ['Visão geral', 'Conciliações', 'Divergências', 'Fechamento']

const KPIS = [
  { label: 'Em aberto', value: 'R$ 10.389,38', tone: 'text-danger-strong' },
  { label: 'Líquido esperado', value: 'R$ 121.212,50', tone: 'text-foreground' },
  { label: 'Taxas', value: 'R$ 3.787,50', tone: 'text-foreground' },
]

const ROWS = [
  { ref: 'PED-10021', type: 'Liquidação ausente', value: 'R$ 145,22' },
  { ref: 'PED-10022', type: 'Taxa divergente', value: '−R$ 990,10' },
  { ref: 'PED-10047', type: 'Status inconsistente', value: 'R$ 0,00' },
  { ref: 'PED-10063', type: 'Parcelas divergentes', value: 'R$ 12,40' },
]

// Ring from the design: 150 px wide, inner radius 82%, so a 13.5 px stroke centred on r = 68.25.
const RING_RADIUS = 68.25
export const RING_LENGTH = 2 * Math.PI * RING_RADIUS
const MATCH_RATE = 0.942

/** Static picture of the closing dashboard; the numbers are the design's sample data, not API data. */
export function ProductPreview() {
  return (
    <div
      id="fechamento"
      role="img"
      aria-label="Prévia do painel de fechamento do ReconPay"
      className="flex w-full scroll-mt-6 flex-col gap-[18px] rounded-t-[24px] border border-b-0 border-border bg-surface px-4 pt-5 md:gap-6 md:rounded-t-[28px] md:px-8 md:pt-7 lg:h-[560px]"
    >
      <div className="flex items-center justify-between gap-4">
        <span className="flex items-center gap-2.5 font-display text-[17px] font-extrabold tracking-[-0.05em]">
          <span className="grid size-[26px] place-items-center rounded-[6px] bg-primary">
            <Check aria-hidden="true" className="size-4" strokeWidth={3} />
          </span>
          reconpay
        </span>
        <span className="hidden items-center gap-1.5 lg:flex">
          {TABS.map((tab, index) => (
            <span key={tab} className={`rounded-full px-3.5 py-2 text-[13px] font-semibold ${index === 0 ? 'bg-foreground text-background' : 'text-muted'}`}>
              {tab}
            </span>
          ))}
        </span>
        <span className="flex items-center gap-2 rounded-full bg-background px-3.5 py-2 text-[13px] font-semibold">
          Loja Centro
          <ChevronDown aria-hidden="true" className="size-3.5 text-muted" />
        </span>
      </div>

      <div className="flex flex-col gap-1.5">
        <p className="text-[13px] text-muted">Fechamento · Setembro de 2026</p>
        <p className="font-display text-[22px] leading-tight font-extrabold tracking-[-0.03em] md:text-[34px] md:tracking-[-0.04em]">
          Faltam 12 divergências para fechar setembro
        </p>
      </div>

      <div className="flex flex-col gap-3 lg:flex-row lg:gap-4">
        <div className="flex h-[220px] flex-col justify-between rounded-[22px] bg-foreground p-6 lg:h-[300px] lg:w-[300px] lg:shrink-0">
          <p className="text-[13px] text-white/60">Taxa de match</p>
          <div className="flex justify-center">
            <div className="relative size-[110px] md:size-[150px]">
              <svg viewBox="0 0 150 150" aria-hidden="true" className="size-full -rotate-90">
                <circle cx="75" cy="75" r={RING_RADIUS} fill="none" strokeWidth="13.5" className="stroke-white/12" />
                <circle
                  cx="75"
                  cy="75"
                  r={RING_RADIUS}
                  fill="none"
                  strokeWidth="13.5"
                  className="stroke-primary"
                  strokeDasharray={RING_LENGTH}
                  strokeDashoffset={RING_LENGTH * (1 - MATCH_RATE)}
                />
              </svg>
              <span className="absolute inset-0 grid place-items-center font-display text-[24px] font-extrabold tracking-[-0.03em] text-background md:text-[30px]">
                94,2%
              </span>
            </div>
          </div>
          <p className="font-mono text-[12px] text-white/60">1.209 de 1.284 itens</p>
        </div>

        <div className="hidden flex-col gap-4 xl:flex xl:w-[300px] xl:shrink-0">
          {KPIS.map(kpi => (
            <div key={kpi.label} className="flex flex-col gap-1.5 rounded-[18px] bg-background px-5 py-4">
              <p className="text-[12px] text-muted">{kpi.label}</p>
              <p className={`font-mono text-[18px] font-semibold ${kpi.tone}`}>{kpi.value}</p>
            </div>
          ))}
        </div>

        <div className="flex min-w-0 flex-1 flex-col gap-1 rounded-t-[22px] bg-background px-2 pt-5 pb-2 lg:h-[300px] lg:rounded-[22px]">
          <div className="flex items-center justify-between gap-3 px-3.5 pb-2 whitespace-nowrap">
            <p className="text-[14px] font-bold">Divergências abertas</p>
            <p className="font-mono text-[12px] text-muted">12 abertas</p>
          </div>
          {ROWS.map(row => (
            <div key={row.ref} className="flex items-center gap-2 px-2.5 py-3 max-[379px]:flex-wrap md:gap-3 md:px-3.5">
              <span className="shrink-0 font-mono text-[12px] font-medium md:text-[13px]">{row.ref}</span>
              <span className="rounded-full bg-surface px-2.5 py-1 text-[12px] font-medium whitespace-nowrap max-[379px]:order-last">{row.type}</span>
              <span className="ml-auto shrink-0 font-mono text-[12px] md:text-[13px]">{row.value}</span>
              <span className="hidden shrink-0 rounded-full bg-foreground px-3 py-1.5 text-[12px] font-semibold text-background min-[1400px]:inline">Resolver</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
