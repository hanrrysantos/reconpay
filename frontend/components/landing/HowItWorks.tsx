import { Download, FileText, Lock } from 'lucide-react'
import type { ReactNode } from 'react'

const FILES = [
  { name: 'vendas-setembro.csv', rows: '1.284 linhas' },
  { name: 'liquidacao-setembro.csv', rows: '1.271 linhas' },
  { name: 'extrato-setembro.csv', rows: '312 linhas' },
]

// 964 of 1.284 items reconciled in the design sample.
const PROGRESS = 964 / 1284

const STEPS: Array<{ title: string; text: string; illustration: ReactNode }> = [
  {
    title: 'Importe os três lados',
    text: 'Vendas, liquidação da adquirente e extrato do banco, em CSV. Um arquivo com erro volta com cada linha apontada.',
    illustration: FILES.map(file => (
      <div key={file.name} className="flex items-center gap-1.5 rounded-[10px] bg-subtle px-2 py-2.5 font-mono text-[11px] md:gap-2.5 md:px-3 md:text-[12px]">
        <FileText aria-hidden="true" className="size-4 shrink-0 text-muted" />
        <span className="min-w-0 flex-1 wrap-break-word">{file.name}</span>
        <span className="shrink-0 text-muted">{file.rows}</span>
      </div>
    )),
  },
  {
    title: 'Concilie a janela',
    text: 'Escolha o período e o ReconPay cruza tudo em segundo plano, item por item, com sete tipos de divergência.',
    illustration: (
      <>
        <div className="flex items-baseline justify-between gap-3 py-1">
          <span className="text-[13px] font-semibold">Conciliando setembro</span>
          <span className="font-mono text-[12px] text-muted">964 de 1.284</span>
        </div>
        <div className="h-2.5 overflow-hidden rounded-full bg-surface">
          <div className="h-full origin-left rounded-full bg-primary" style={{ width: `${PROGRESS * 100}%` }} />
        </div>
        <div className="flex flex-wrap gap-2 pt-2 font-mono text-[12px]">
          <span className="rounded-full bg-subtle px-2.5 py-[5px] text-success">Conciliados 912</span>
          <span className="rounded-full bg-subtle px-2.5 py-[5px] text-danger-strong">Divergentes 52</span>
        </div>
      </>
    ),
  },
  {
    title: 'Trate e trave',
    text: 'Aceite, ajuste ou baixe cada divergência com nota. Sem nada em aberto, trave o período e exporte o CSV.',
    illustration: (
      <>
        <div className="flex items-center gap-3 px-1 py-1.5">
          <span className="grid size-9 shrink-0 place-items-center rounded-full bg-foreground">
            <Lock aria-hidden="true" className="size-4 text-primary" />
          </span>
          <span className="flex min-w-0 flex-col gap-[3px]">
            <span className="text-[14px] font-bold">Setembro travado</span>
            <span className="font-mono text-[12px] text-muted">05/10/2026 18:42 · fechou em 4 d 6 h</span>
          </span>
        </div>
        <div className="flex items-center gap-2 rounded-[10px] bg-subtle px-2.5 py-2.5 font-mono text-[11px] md:px-3 md:text-[12px]">
          <Download aria-hidden="true" className="size-4 shrink-0 text-muted" />
          <span className="min-w-0 wrap-break-word">reconpay-2026-09-01-2026-09-30.csv</span>
        </div>
      </>
    ),
  },
]

export function HowItWorks({ shell }: { shell: string }) {
  return (
    <section id="como-funciona" className={`${shell} flex scroll-mt-6 flex-col gap-8 py-16 md:gap-14 md:py-28`}>
      <div className="flex flex-col gap-3.5">
        <p className="font-mono text-[13px] text-success">Como funciona</p>
        <h2 className="max-w-[720px] font-display text-[32px] leading-[1.04] font-extrabold tracking-[-0.035em] md:text-[48px] md:tracking-[-0.04em]">
          Três passos por período, do arquivo ao fechamento.
        </h2>
      </div>
      <ol className="grid gap-3.5 lg:grid-cols-3 lg:gap-5">
        {STEPS.map((step, index) => (
          <li key={step.title} className="flex flex-col justify-between gap-[22px] rounded-[24px] bg-surface p-[22px] md:gap-7 md:p-7 lg:min-h-[377px]">
            <div className="flex flex-col gap-2.5">
              <span className="font-mono text-[13px] text-muted">{String(index + 1).padStart(2, '0')}</span>
              <h3 className="font-display text-[26px] font-bold tracking-[-0.03em]">{step.title}</h3>
              <p className="text-[15px] leading-[1.55] text-muted">{step.text}</p>
            </div>
            <div className="flex flex-col gap-2 rounded-[16px] bg-background p-3 md:p-4">{step.illustration}</div>
          </li>
        ))}
      </ol>
    </section>
  )
}
