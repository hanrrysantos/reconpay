const DISTORTIONS = [
  { title: 'Taxa', text: 'A adquirente cobra diferente da regra combinada.' },
  { title: 'Parcela', text: 'A venda em 3x liquida como 1x, ou o contrário.' },
  { title: 'Atraso', text: 'A venda do dia 30 só liquida no mês seguinte.' },
  { title: 'Estorno', text: 'Cancelamento e chargeback que o sistema não viu.' },
]

export function Problem({ shell }: { shell: string }) {
  return (
    <section className="bg-foreground text-background">
      <div className={`${shell} flex flex-col gap-8 py-16 md:gap-14 md:py-28`}>
        <div className="flex flex-col gap-4 lg:flex-row lg:justify-between lg:gap-20">
          <h2 className="max-w-[620px] font-display text-[34px] leading-[1.02] font-extrabold tracking-[-0.035em] md:text-[52px] md:tracking-[-0.04em]">
            O valor que cai na conta quase nunca é o da venda.
          </h2>
          <p className="max-w-[440px] text-[16px] leading-[1.6] text-white/65 md:text-[17px]">
            Taxa, parcela, atraso e estorno mudam o líquido no caminho. Na planilha, a diferença aparece semanas depois, sem dono e sem histórico.
          </p>
        </div>
        <ol className="grid gap-px overflow-hidden rounded-[22px] bg-white/10 md:grid-cols-2 lg:grid-cols-4">
          {DISTORTIONS.map((item, index) => (
            <li key={item.title} className="flex flex-col gap-3.5 bg-foreground px-5 py-[22px] md:px-7 md:pt-7 md:pb-8">
              <span className="font-mono text-[13px] text-primary">{String(index + 1).padStart(2, '0')}</span>
              <h3 className="font-display text-[24px] font-bold tracking-[-0.025em]">{item.title}</h3>
              <p className="text-[15px] leading-normal text-white/60">{item.text}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  )
}
