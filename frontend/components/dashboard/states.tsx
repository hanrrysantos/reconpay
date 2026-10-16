'use client'

import { Check, Copy, Lock, Plus, RefreshCw, TriangleAlert, WifiOff } from 'lucide-react'
import Link from 'next/link'
import { useState, type ReactNode } from 'react'
import type { Run } from '@/lib/api'
import { CREATE_MERCHANT_HREF } from '@/components/shell/MerchantMenu'
import { cn } from '@/lib/utils'

const Placeholder = ({ className }: { className?: string }) => <div className={cn('rounded-sm bg-[#EBEDE6]', className)} />

/** Bento grid placeholders while the first data loads; the shell (nav, merchant menu) stays interactive. */
export function DashboardSkeleton() {
  return (
    <div role="status" aria-label="Carregando painel" aria-busy="true" className="flex animate-pulse flex-col gap-4 [animation-duration:1.4s]">
      <div className="flex flex-col gap-4 lg:flex-row">
        <div data-skeleton-block className="flex h-[320px] flex-col gap-4 rounded-lg bg-foreground p-7 lg:w-[420px]">
          <div className="h-4 w-40 rounded-sm bg-white/8" />
          <div className="size-[168px] rounded-full bg-white/8" />
        </div>
        <div data-skeleton-block className="flex h-[320px] flex-1 flex-col gap-4 rounded-lg bg-surface p-7">
          <Placeholder className="h-4 w-56" />
          <Placeholder className="h-full w-full bg-border" />
        </div>
      </div>
      <div className="flex flex-col gap-4 lg:flex-row">
        <div data-skeleton-block className="h-[180px] flex-1 rounded-lg bg-[#EEF8C9]" />
        <div data-skeleton-block className="h-[180px] flex-1 rounded-lg bg-surface" />
        <div data-skeleton-block className="h-[180px] flex-1 rounded-lg bg-surface" />
        <div data-skeleton-block className="h-[180px] rounded-lg bg-surface lg:w-[420px]" />
      </div>
      <div className="flex flex-col gap-4 lg:flex-row">
        <div data-skeleton-block className="h-[300px] flex-1 rounded-lg bg-surface" />
        <div data-skeleton-block className="h-[300px] rounded-lg bg-surface lg:w-[420px]" />
      </div>
      <span className="sr-only">Carregando…</span>
    </div>
  )
}

/** Without `onCreate`, "Cadastrar empresa" links to `/empresas` with the form open; on that page it opens the form directly. */
export function NoMerchant({ onCreate }: { onCreate?: () => void } = {}) {
  const action = 'flex h-11 items-center gap-2 rounded-full bg-primary px-5 text-[14px] font-semibold text-foreground'
  const label = <><Plus aria-hidden="true" className="size-4" />Cadastrar empresa</>
  return (
    <section className="flex flex-col items-start gap-5 rounded-lg bg-foreground p-7 text-white lg:w-[420px]">
      <h2 className="font-display text-[30px] leading-tight font-extrabold tracking-[-0.03em]">Nenhuma empresa no seu acesso</h2>
      <p className="text-[13px] text-white/75">Peça acesso a um administrador ou cadastre uma empresa.</p>
      {onCreate
        ? <button type="button" onClick={onCreate} className={action}>{label}</button>
        : <Link href={CREATE_MERCHANT_HREF} className={action}>{label}</Link>}
    </section>
  )
}

export function Forbidden({ merchantMenu }: { merchantMenu?: ReactNode }) {
  return (
    <section className="flex flex-col items-start gap-4 rounded-lg bg-surface p-7">
      <Lock aria-hidden="true" className="size-6 text-muted" />
      <h2 className="font-display text-[22px] font-extrabold tracking-[-0.03em]">Você não tem acesso a esta empresa</h2>
      <p className="text-[13px] text-muted">Escolha outra empresa do seu acesso.</p>
      {merchantMenu}
    </section>
  )
}

type Imports = { settlementImportedAt: string | null; bankStatementImportedAt: string | null } | undefined

/** Three-step onboarding of a window without run; each step's state comes from the latest imports. */
export function EmptyOnboarding({ imports, onRun }: { imports: Imports; onRun: () => void }) {
  const done = [Boolean(imports?.settlementImportedAt), Boolean(imports?.bankStatementImportedAt), false]
  const current = done.indexOf(false)
  const steps = [
    { title: 'Importar liquidação', detail: 'Arquivo da adquirente com as liquidações da janela.' },
    { title: 'Importar extrato', detail: 'Extrato bancário com os créditos recebidos.' },
    { title: 'Conciliar período', detail: 'Cruza vendas, liquidações e extrato.' },
  ]

  return (
    <section className="flex flex-col gap-6 rounded-lg bg-surface p-7 lg:flex-row lg:items-center">
      <div aria-hidden="true" className="relative h-40 w-56 shrink-0">
        <div className="absolute top-4 left-4 h-32 w-24 -rotate-6 rounded-md bg-white shadow-[0_0_0_1px_var(--color-border)]" />
        <div className="absolute top-2 left-20 h-32 w-24 rotate-3 rounded-md bg-white shadow-[0_0_0_1px_var(--color-border)]" />
        <div className="absolute right-6 bottom-2 grid size-14 place-items-center rounded-full bg-primary"><Check className="size-6" /></div>
      </div>
      <ol aria-label="Primeiros passos" className="flex flex-1 flex-col gap-3">
        {steps.map((step, index) => {
          const state = done[index] ? 'done' : index === current ? 'current' : 'blocked'
          return (
            <li
              key={step.title}
              data-state={state}
              className={cn(
                'flex items-center gap-4 rounded-md bg-white p-4',
                state === 'current' && 'shadow-[inset_0_0_0_2px_var(--color-primary)]',
                state === 'blocked' && 'opacity-60',
              )}
            >
              <span className={cn('grid size-8 shrink-0 place-items-center rounded-full font-mono text-[13px] font-semibold', state === 'done' ? 'bg-foreground text-primary' : 'bg-surface')}>
                {state === 'done' ? <Check aria-hidden="true" className="size-4" /> : state === 'blocked' ? <Lock aria-hidden="true" className="size-4 text-muted" /> : index + 1}
              </span>
              <span className="flex flex-1 flex-col">
                <span className="text-[14px] font-semibold">{step.title}</span>
                <span className="text-[12px] text-muted">{step.detail}</span>
              </span>
              {state === 'current' && index < 2 && (
                <Link href="/importacoes" className="h-9 rounded-full bg-foreground px-4 text-[13px] leading-9 font-semibold text-white">Importar</Link>
              )}
              {state === 'current' && index === 2 && (
                <button type="button" onClick={onRun} className="h-9 rounded-full bg-foreground px-4 text-[13px] font-semibold text-white">Conciliar período</button>
              )}
            </li>
          )
        })}
      </ol>
    </section>
  )
}

/** Failed run: the API's public message, the run id with a copy action and "Rodar de novo". */
export function RunFailedCard({ run, onRetry }: { run: Run; onRetry: () => void }) {
  const [copied, setCopied] = useState(false)

  async function copy() {
    try {
      await navigator.clipboard.writeText(run.id)
      setCopied(true)
    } catch { /* Clipboard may be blocked; the id stays visible to copy by hand. */ }
  }

  return (
    <section className="flex flex-col gap-5 rounded-lg bg-foreground p-7 text-white lg:w-[420px]">
      <span className="self-start rounded-full bg-danger px-2.5 py-1 text-[12px] font-bold">Falhou</span>
      <h2 className="font-display text-[30px] leading-tight font-extrabold tracking-[-0.03em]">A conciliação não terminou</h2>
      <p className="text-[13px] text-white/75">{run.errorMessage ?? 'O run parou antes de terminar. Os números abaixo são do run anterior.'}</p>
      <div className="flex items-center gap-2 rounded-md bg-white/7 px-4 py-3">
        <code className="min-w-0 flex-1 truncate font-mono text-[12px] text-white/75">{run.id}</code>
        <button type="button" onClick={() => void copy()} className="flex items-center gap-1 text-[12px] font-semibold">
          <Copy aria-hidden="true" className="size-3.5" />
          {copied ? 'Copiado' : 'Copiar'}
        </button>
      </div>
      <button type="button" onClick={onRetry} className="flex h-11 items-center justify-center gap-2 self-start rounded-full bg-primary px-5 text-[14px] font-semibold text-foreground">
        <RefreshCw aria-hidden="true" className="size-4" />
        Rodar de novo
      </button>
    </section>
  )
}

export function NetworkBanner({ onRetry }: { onRetry: () => void }) {
  return (
    <div role="alert" className="flex flex-wrap items-center gap-4 rounded-md bg-danger-soft px-5 py-4">
      <WifiOff aria-hidden="true" className="size-5 text-danger-strong" />
      <div className="flex flex-1 flex-col">
        <p className="text-[14px] font-semibold text-danger-strong">Não foi possível conectar à API</p>
        <p className="text-[13px] text-[#8A2A2E]">Os blocos já carregados continuam visíveis.</p>
      </div>
      <button type="button" onClick={onRetry} className="h-9 rounded-full bg-foreground px-4 text-[13px] font-semibold text-white">Tentar de novo</button>
    </div>
  )
}

/** One block failed: the rest of the page stays; the action refetches only this block. */
export function BlockError({ message, action = 'Recarregar', onRetry, className }: { message: string; action?: string; onRetry: () => void; className?: string }) {
  return (
    <section role="alert" className={cn('flex flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-[#D3D6CD] p-7 text-center', className)}>
      <span className="grid size-10 place-items-center rounded-full bg-white"><TriangleAlert aria-hidden="true" className="size-5 text-muted" /></span>
      <p className="text-[14px] font-semibold">{message}</p>
      <button type="button" onClick={onRetry} className="h-9 rounded-full bg-white px-4 text-[13px] font-semibold shadow-[inset_0_0_0_1px_var(--color-border)]">{action}</button>
    </section>
  )
}
