'use client'

import { Check, Lock } from 'lucide-react'
import { Fragment, useState, type ReactNode } from 'react'
import type { Run } from '@/lib/api'
import { cn } from '@/lib/utils'

type StepState = 'done' | 'current' | 'pending'

export type CloseStepperProps = {
  imports: { settlementImportedAt: string | null; bankStatementImportedAt: string | null } | undefined
  /** Latest run of the window; null when it has none. */
  run: Run | null
  /** Open discrepancies of the current run (from the summary); null while unknown. */
  openCount: number | null
  locked: boolean
  onLock: () => void
  onUnlock: () => void
}

const time = (iso: string) =>
  new Date(iso).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })

function Confirm({ title, action, onConfirm, onCancel }: { title: string; action: string; onConfirm: () => void; onCancel: () => void }) {
  return (
    <div role="alertdialog" aria-label={title} className="absolute top-12 right-0 z-30 flex w-64 flex-col gap-3 rounded-[16px] bg-white p-3 shadow-[0_0_0_1px_var(--color-border),0_20px_48px_rgba(11,13,10,0.14)]">
      <p className="text-[13px] font-semibold">{title}</p>
      <div className="flex justify-end gap-2">
        <button type="button" onClick={onCancel} className="h-8 rounded-full bg-surface px-3 text-[13px] font-semibold">Cancelar</button>
        <button type="button" onClick={onConfirm} className="h-8 rounded-full bg-foreground px-3 text-[13px] font-semibold text-white">{action}</button>
      </div>
    </div>
  )
}

export function CloseStepper({ imports, run, openCount, locked, onLock, onUnlock }: CloseStepperProps) {
  const [confirming, setConfirming] = useState<'lock' | 'unlock' | null>(null)
  const completed = run?.status === 'COMPLETED'
  const done = [
    locked || Boolean(imports?.settlementImportedAt && imports?.bankStatementImportedAt),
    locked || completed,
    locked || (completed && openCount === 0),
    locked,
  ]
  const currentIndex = done.indexOf(false)
  const stateOf = (index: number): StepState => done[index] ? 'done' : index === currentIndex ? 'current' : 'pending'
  const canLock = !locked && completed && openCount === 0

  const steps: Array<{ title: string; detail: ReactNode }> = [
    {
      title: 'Importado',
      detail: <>
        {imports?.settlementImportedAt ? `Liquidação ${time(imports.settlementImportedAt)}` : 'Liquidação sem import'}
        {' · '}
        {imports?.bankStatementImportedAt ? `Extrato ${time(imports.bankStatementImportedAt)}` : 'Extrato sem import'}
      </>,
    },
    {
      title: 'Conciliado',
      detail: run === null ? 'Sem run'
        : run.status === 'PENDING' || run.status === 'RUNNING' ? 'Em andamento'
          : run.status === 'FAILED' ? 'Falhou' : `Run de ${time(run.finishedAt ?? run.createdAt)}`,
    },
    {
      title: 'Zerar divergências',
      detail: completed && openCount !== null ? (openCount === 1 ? '1 aberta' : `${openCount} abertas`) : '—',
    },
    {
      title: 'Travar período',
      detail: locked
        ? <>Travado · <button type="button" onClick={() => setConfirming('unlock')} className="font-semibold underline underline-offset-2">Destravar</button></>
        : <button type="button" disabled={!canLock} onClick={() => setConfirming('lock')} className="font-semibold underline underline-offset-2 disabled:no-underline">Travar período</button>,
    },
  ]

  return (
    <ol aria-label="Régua de fechamento" className="flex flex-wrap items-center gap-2">
      {steps.map((step, index) => {
        const state = stateOf(index)
        const lastAndLocked = index === 3 && locked
        return (
          <Fragment key={step.title}>
            {index > 0 && <span aria-hidden="true" className={cn('h-0.5 w-6 shrink-0', done[index - 1] ? 'bg-foreground' : 'bg-border')} />}
            <li
              data-state={state}
              aria-current={state === 'current' ? 'step' : undefined}
              className={cn(
                'relative flex items-center gap-2.5 rounded-full py-2 pr-3.5 pl-2 whitespace-nowrap',
                state === 'done' && (lastAndLocked ? 'bg-primary' : 'bg-surface'),
                state === 'current' && 'bg-primary',
                state === 'pending' && 'shadow-[inset_0_0_0_1px_var(--color-border)]',
              )}
            >
              <span className={cn(
                'grid size-6 shrink-0 place-items-center rounded-full',
                state === 'done' && 'bg-foreground text-primary',
                state === 'current' && 'bg-white font-mono text-[12px] font-semibold',
                state === 'pending' && 'text-muted',
              )}>
                {state === 'done' ? <Check aria-hidden="true" className="size-3.5" strokeWidth={3} />
                  : state === 'current' ? index + 1
                    : <Lock aria-hidden="true" className="size-3.5" />}
              </span>
              <span className="flex flex-col">
                <h3 className="text-[13px] font-semibold">{step.title}</h3>
                <span className={cn('text-[12px]', state === 'current' || lastAndLocked ? 'text-foreground/70' : 'text-muted')}>{step.detail}</span>
              </span>
              {index === 3 && confirming === 'lock' && (
                <Confirm title="Travar o período?" action="Travar" onCancel={() => setConfirming(null)} onConfirm={() => { setConfirming(null); onLock() }} />
              )}
              {index === 3 && confirming === 'unlock' && (
                <Confirm title="Destravar o período?" action="Destravar" onCancel={() => setConfirming(null)} onConfirm={() => { setConfirming(null); onUnlock() }} />
              )}
            </li>
          </Fragment>
        )
      })}
    </ol>
  )
}
