import Link from 'next/link'
import { DemoButton } from './DemoButton'

export function FinalCta({ shell }: { shell: string }) {
  return (
    <section className={`${shell} pt-2 md:pt-6`}>
      <div className="reveal flex flex-col items-center gap-6 rounded-[28px] bg-foreground px-6 py-14 text-center text-background md:rounded-[32px] md:px-20 md:py-24">
        <h2 className="font-display text-[34px] leading-[1.02] font-extrabold tracking-[-0.035em] md:text-[60px] md:tracking-[-0.04em]">
          Feche o próximo mês<br className="hidden md:inline" /> sem abrir uma planilha.
        </h2>
        <p className="text-[16px] text-white/65 md:text-[17px]">Crie sua conta e concilie o primeiro mês.</p>
        <div className="flex w-full flex-col gap-3 pt-3 md:w-auto md:flex-row">
          <DemoButton className="inline-flex h-[54px] items-center justify-center gap-2 rounded-full bg-primary px-[26px] text-[15px] font-bold text-foreground" />
          <Link href="/login" className="inline-flex h-[54px] items-center justify-center rounded-full bg-white/8 px-[26px] text-[15px] font-semibold">
            Entrar na minha conta
          </Link>
        </div>
      </div>
    </section>
  )
}
