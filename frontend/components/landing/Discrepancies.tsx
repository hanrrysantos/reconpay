import { Check } from 'lucide-react'

const OUTCOMES = [
  { label: 'Aceitar', text: 'A diferença está correta e fica registrada.' },
  { label: 'Ajustar', text: 'Lança uma correção com sinal, no valor exato.' },
  { label: 'Baixar', text: 'Encerra sem correção, com a justificativa.' },
]

const COMPARE = [
  { label: 'Bruto', sale: 'R$ 2.400,00', settlement: 'R$ 2.400,00' },
  { label: 'Líquido', sale: 'R$ 2.287,13', settlement: 'R$ 1.297,03', highlight: true },
  { label: 'Método', sale: 'PIX · 1x', settlement: 'PIX · 1x' },
]

const cell = 'w-24 shrink-0 md:w-[130px]'

export function Discrepancies({ shell }: { shell: string }) {
  return (
    <section id="divergencias" className="scroll-mt-6 bg-surface">
      <div className={`${shell} flex flex-col gap-9 py-16 lg:flex-row lg:items-center lg:gap-20 lg:py-28`}>
        <div className="flex flex-1 flex-col gap-5">
          <p className="font-mono text-[13px] text-success">Divergências</p>
          <h2 className="font-display text-[32px] leading-[1.04] font-extrabold tracking-[-0.035em] md:text-[48px] md:tracking-[-0.04em]">
            Cada diferença tem dono, decisão e histórico.
          </h2>
          <p className="text-[16px] leading-[1.6] text-muted md:text-[17px]">
            Abra a divergência, veja os dois lados lado a lado e decida. Toda mudança fica registrada com quem fez, quando e por quê.
          </p>
          <ul className="flex flex-col gap-3.5 pt-3">
            {OUTCOMES.map(outcome => (
              <li key={outcome.label} className="flex items-center gap-3.5 text-[15px]">
                <span className="grid size-6 shrink-0 place-items-center rounded-full bg-primary">
                  <Check aria-hidden="true" className="size-3.5" strokeWidth={3} />
                </span>
                <span className="w-16 shrink-0 font-bold">{outcome.label}</span>
                <span className="text-muted">{outcome.text}</span>
              </li>
            ))}
          </ul>
        </div>

        <div
          role="img"
          aria-label="Exemplo de ajuste da divergência PED-10022"
          className="flex w-full flex-col gap-5 rounded-[28px] bg-background p-5 shadow-[0_24px_60px_#0B0D0A1F] md:p-7 lg:w-[560px] lg:shrink-0"
        >
          <div className="flex items-center justify-between gap-4">
            <div className="flex flex-col gap-1">
              <span className="font-mono text-[13px] text-muted">PED-10022</span>
              <span className="font-display text-[26px] font-extrabold tracking-[-0.03em]">Taxa divergente</span>
            </div>
            <span className="rounded-full bg-danger-soft px-3 py-1.5 text-[12px] font-bold text-danger-strong">Aberta</span>
          </div>
          <div className="flex flex-col rounded-[16px] bg-surface py-1.5 font-mono text-[12px]">
            <div className="flex px-3 py-2.5 font-sans text-muted md:px-4">
              <span className="flex-1" />
              <span className={cell}>Venda</span>
              <span className={cell}>Liquidação</span>
            </div>
            {COMPARE.map(row => (
              <div key={row.label} className={`flex px-3 py-2.5 md:px-4 ${row.highlight ? 'bg-primary-soft font-semibold' : ''}`}>
                <span className="flex-1">{row.label}</span>
                <span className={cell}>{row.sale}</span>
                <span className={cell}>{row.settlement}</span>
              </div>
            ))}
          </div>
          <div className="grid grid-cols-3 gap-2 text-[14px] font-semibold">
            <span className="grid h-11 place-items-center rounded-full bg-surface">Aceitar</span>
            <span className="grid h-11 place-items-center rounded-full bg-foreground text-background">Ajustar</span>
            <span className="grid h-11 place-items-center rounded-full bg-surface">Baixar</span>
          </div>
          <div className="flex h-12 items-center justify-between rounded-full bg-surface px-[18px]">
            <span className="font-mono text-[15px]">−990,10</span>
            <span className="text-[13px] font-semibold">Usar diferença</span>
          </div>
          <span className="grid h-[50px] place-items-center rounded-full bg-primary text-[15px] font-bold">Salvar ajuste</span>
        </div>
      </div>
    </section>
  )
}
