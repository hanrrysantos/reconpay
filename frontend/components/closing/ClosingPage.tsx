'use client'

import { ArrowRight, ChevronLeft, ChevronRight } from 'lucide-react'
import Link from 'next/link'
import { useState } from 'react'
import { BlockError, NoMerchant } from '@/components/dashboard/states'
import { useSelectedMerchant } from '@/components/shell/MerchantMenu'
import { CLOSING_PAGE_SIZE, periodWindows } from '@/lib/closing'
import { windowKey } from '@/components/dashboard/PeriodMenu'
import type { Window } from '@/lib/period'
import { useRuns } from '@/lib/queries'
import { PeriodRow } from './PeriodRow'

const title = <h1 className="font-display text-[44px] leading-none font-extrabold tracking-[-0.04em]">Fechamento</h1>
const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível carregar as conciliações.'
const columns = ['Período', 'Status', 'Match', 'Em aberto', 'Travado em', 'Duração']
const pager = 'flex h-9 items-center gap-1 rounded-full bg-white px-3 text-[13px] font-semibold disabled:text-muted'

/** One merchant: its windows, the page shown and the action notice. Remounted per merchant. */
function ClosingWindows({ merchantId, merchantName, windows }: { merchantId: string; merchantName?: string; windows: Window[] }) {
  const [page, setPage] = useState(0)
  const [notice, setNotice] = useState<string | null>(null)
  const totalPages = Math.ceil(windows.length / CLOSING_PAGE_SIZE)
  const shown = windows.slice(page * CLOSING_PAGE_SIZE, (page + 1) * CLOSING_PAGE_SIZE)

  return (
    <>
      <section className="flex min-w-0 flex-col gap-2">
        <p className="truncate text-[14px] text-muted">{merchantName}</p>
        {title}
      </section>

      {windows.length === 0 ? (
        <section className="flex flex-col items-start gap-3 rounded-lg bg-surface p-7">
          <p className="text-[16px] font-semibold">Nenhuma conciliação ainda.</p>
          <Link href="/conciliacoes" className="flex w-fit items-center gap-1.5 text-[13px] font-semibold">
            Ir para conciliações
            <ArrowRight aria-hidden="true" className="size-4" />
          </Link>
        </section>
      ) : (
        <section className="flex flex-col gap-3">
          {notice && <p role="alert" className="rounded-md bg-danger-soft px-4 py-3 text-[13px] font-semibold text-danger-strong">{notice}</p>}
          <div className="overflow-x-auto rounded-[18px] bg-white p-1.5">
            <table aria-label="Períodos" className="w-full min-w-[860px] text-left">
              <thead className="text-[12px] text-muted">
                <tr>
                  {columns.map(column => <th key={column} scope="col" className="px-4 py-2 font-normal">{column}</th>)}
                  <th scope="col" className="px-4 py-2 text-right font-normal">Ações</th>
                </tr>
              </thead>
              <tbody>
                {shown.map(window => <PeriodRow key={windowKey(window)} merchantId={merchantId} window={window} onError={setNotice} />)}
              </tbody>
            </table>
          </div>
          {totalPages > 1 && (
            <nav aria-label="Paginação" className="flex items-center justify-end gap-2 px-2 pb-1">
              <span className="mr-auto font-mono text-[12px] text-muted">Página {page + 1} de {totalPages}</span>
              <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)} className={pager}>
                <ChevronLeft aria-hidden="true" className="size-4" />
                Anterior
              </button>
              <button type="button" disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)} className={pager}>
                Próxima
                <ChevronRight aria-hidden="true" className="size-4" />
              </button>
            </nav>
          )}
        </section>
      )}
    </>
  )
}

/**
 * `/fechamento`: one row per window of the selected merchant's runs (the 100 latest), newest first,
 * each with its period status and the lock, unlock and export actions.
 */
export function ClosingPage() {
  const { merchant, merchantId, isLoading: merchantsLoading, error: merchantsError } = useSelectedMerchant()
  const runs = useRuns(merchantId)

  if (!merchantsLoading && !merchantsError && merchantId === null) {
    return (
      <>
        <section className="flex min-w-0 flex-col gap-2">{title}</section>
        <NoMerchant />
      </>
    )
  }
  if (merchantId === null || runs.isPending) {
    return <div role="status" aria-label="Carregando períodos" aria-busy="true" className="h-40 animate-pulse rounded-lg bg-surface" />
  }
  if (runs.isError) return <BlockError message={messageOf(runs.error)} action="Tentar novamente" onRetry={() => void runs.refetch()} />

  return <ClosingWindows key={merchantId} merchantId={merchantId} merchantName={merchant?.name} windows={periodWindows(runs.data.content)} />
}
