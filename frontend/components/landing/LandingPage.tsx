import { ArrowRight, Check } from 'lucide-react'
import Link from 'next/link'
import type { CSSProperties } from 'react'
import { Discrepancies } from './Discrepancies'
import { FinalCta } from './FinalCta'
import { HowItWorks } from './HowItWorks'
import { Problem } from './Problem'
import { ProductPreview } from './ProductPreview'

const GITHUB = 'https://github.com/hanrrysantos/reconpay'
const API_DOCS = 'https://reconpay.hanrry.top/swagger-ui/index.html'

const shell = 'mx-auto w-full max-w-[1440px] px-4 md:px-10 lg:px-20'
const primaryButton = 'inline-flex h-[52px] items-center justify-center gap-2 rounded-full bg-primary px-6 text-[15px] font-bold text-foreground'
const secondaryButton = 'inline-flex h-[52px] items-center justify-center rounded-full bg-surface px-6 text-[15px] font-semibold text-foreground'

function Brand({ size }: { size: 'nav' | 'footer' }) {
  const nav = size === 'nav'
  return (
    <Link href="/" className={`flex items-center gap-2.5 font-display font-extrabold tracking-[-0.04em] ${nav ? 'text-[20px]' : 'text-[17px]'}`}>
      <span className={`grid place-items-center rounded-[6px] bg-primary ${nav ? 'size-[30px]' : 'size-[26px]'}`}>
        <Check aria-hidden="true" className="size-4" strokeWidth={3} />
      </span>
      reconpay
    </Link>
  )
}

export function LandingPage() {
  return (
    <div className="overflow-x-clip bg-background text-foreground">
      <header className={`${shell} py-4 md:py-6`}>
        <nav aria-label="Principal" className="flex items-center justify-between gap-3 md:gap-4">
          <Brand size="nav" />
          <div className="hidden items-center gap-9 text-[14px] font-medium text-muted lg:flex">
            <a href="#como-funciona">Como funciona</a>
            <a href="#divergencias">Divergências</a>
            <a href="#fechamento">Fechamento</a>
          </div>
          <div className="flex shrink-0 items-center gap-4 text-[14px] font-semibold md:gap-5">
            <Link href="/login" className="max-[379px]:hidden">Entrar</Link>
            <Link href="/register" className="inline-flex h-9 items-center rounded-full bg-foreground px-3.5 whitespace-nowrap min-[380px]:px-[18px] text-background md:h-10">Criar conta</Link>
          </div>
        </nav>
      </header>

      <main>
        <section className={`${shell} flex flex-col items-center gap-9 pt-10 md:gap-14 md:pt-[72px]`}>
          <div className="hero-in flex w-full max-w-[900px] flex-col items-center gap-5 text-center md:gap-6">
            <p style={{ '--i': 0 } as CSSProperties} className="flex items-center gap-2 rounded-full bg-surface px-3.5 py-1.5 text-[12px] font-medium md:text-[13px]">
              <span aria-hidden="true" className="size-1.5 shrink-0 rounded-full bg-foreground" />
              Conciliação financeira para fintechs, gateways e marketplaces
            </p>
            <h1 style={{ '--i': 1 } as CSSProperties} className="font-display text-[clamp(32px,10.5vw,40px)] leading-none font-extrabold tracking-[-0.04em] md:text-[60px] lg:text-[76px] lg:leading-[0.98] lg:tracking-[-0.045em]">
              O que você vendeu,<br /> o que caiu na conta<br /> e onde não bate.
            </h1>
            <p style={{ '--i': 2 } as CSSProperties} className="max-w-[640px] text-[16px] leading-[1.55] text-muted md:text-[18px]">
              O ReconPay cruza suas vendas com a liquidação da adquirente e o extrato do banco, aponta cada divergência e fecha o período com rastro para auditoria.
            </p>
            <div style={{ '--i': 3 } as CSSProperties} className="flex w-full flex-col gap-3 pt-2 md:w-auto md:flex-row">
              <Link href="/register" className={primaryButton}>
                Criar conta
                <ArrowRight aria-hidden="true" className="size-[18px]" />
              </Link>
              <Link href="/login" className={secondaryButton}>Entrar na minha conta</Link>
            </div>
          </div>
          <ProductPreview />
        </section>
        <Problem shell={shell} />
        <HowItWorks shell={shell} />
        <Discrepancies shell={shell} />
        <FinalCta shell={shell} />
      </main>

      <footer className={`${shell} flex flex-col items-start gap-5 pt-8 pb-10 md:flex-row md:items-center md:justify-between md:pt-10 md:pb-12`}>
        <Brand size="footer" />
        <div className="flex flex-wrap gap-4 text-[13px] font-medium text-muted md:gap-7">
          <a href={GITHUB} target="_blank" rel="noopener noreferrer">GitHub</a>
          <a href={API_DOCS} target="_blank" rel="noopener noreferrer">Documentação da API</a>
          <Link href="/login">Entrar</Link>
        </div>
        <p className="text-[13px] text-muted">Feito por Hanrry Santos · 2026</p>
      </footer>
    </div>
  )
}
