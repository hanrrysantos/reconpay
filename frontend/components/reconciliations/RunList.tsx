'use client'

import { ChevronLeft, ChevronRight } from 'lucide-react'
import Link from 'next/link'
import { BlockError } from '@/components/dashboard/states'
import type { Run } from '@/lib/api'
import { formatImportedAt } from '@/lib/imports'
import { formatWindow, type Window } from '@/lib/period'
import { useRunPage } from '@/lib/queries'
import { RUN_STATUS_LABELS, itemsProgress } from '@/lib/reconciliations'

const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível carregar as conciliações.'
const count = (value: number | null) => value === null ? '—' : String(value)

function TableSkeleton() {
  return (
    <div role="status" aria-label="Carregando conciliações" aria-busy="true" className="flex animate-pulse flex-col gap-2 rounded-lg bg-surface p-4 [animation-duration:1.4s]">
      {[0, 1, 2].map(row => <div key={row} className="h-12 rounded-sm bg-[#EBEDE6]" />)}
      <span className="sr-only">Carregando…</span>
    </div>
  )
}

function RunStatusCell({ run }: { run: Run }) {
  return (
    <div className="flex flex-col gap-1">
      <span className="flex flex-wrap items-center gap-1.5">
        {RUN_STATUS_LABELS[run.status]}
        {run.supersededAt !== null && (
          <span className="rounded-full bg-surface px-2 py-0.5 text-[11px] font-semibold text-muted">Substituído</span>
        )}
      </span>
      {run.status === 'FAILED' && run.errorMessage && <span className="text-[12px] text-muted">{run.errorMessage}</span>}
    </div>
  )
}

type Props = {
  merchantId: string
  window: Window | null
  page: number
  onPageChange: (page: number) => void
}

const cell = 'px-3 py-2 group-hover:bg-subtle'

/** The merchant's runs, 20 per page, newest first; reloads every 3 s while any run of the page is in progress. */
export function RunList({ merchantId, window, page, onPageChange }: Props) {
  const list = useRunPage(merchantId, window, page)
  const runs = list.data?.content ?? []
  const totalPages = list.data?.totalPages ?? 0
  const empty = list.data !== undefined && runs.length === 0 && page === 0

  if (list.isPending) return <TableSkeleton />
  if (list.isError) return <BlockError message={messageOf(list.error)} action="Tentar novamente" onRetry={() => void list.refetch()} />
  if (empty) return <p className="rounded-lg bg-surface p-7 text-[14px] text-muted">Nenhuma conciliação encontrada.</p>

  return (
    <section className="flex flex-col gap-2 rounded-lg bg-surface p-2">
      <div className="overflow-x-auto rounded-[18px] bg-white p-1.5">
        <table aria-label="Conciliações" className="w-full min-w-[860px] border-separate border-spacing-y-1 text-left">
          <thead className="text-[12px] text-muted">
            <tr>
              <th scope="col" className="px-3 font-normal">Janela</th>
              <th scope="col" className="w-[200px] px-3 font-normal">Status</th>
              <th scope="col" className="w-[100px] px-3 text-right font-normal">Itens</th>
              <th scope="col" className="w-[100px] px-3 text-right font-normal">Conciliados</th>
              <th scope="col" className="w-[100px] px-3 text-right font-normal">Divergentes</th>
              <th scope="col" className="w-[150px] px-3 font-normal">Criado em</th>
              <th scope="col" className="w-[110px] px-3 font-normal"><span className="sr-only">Ação</span></th>
            </tr>
          </thead>
          <tbody>
            {runs.map(run => (
              <tr key={run.id} className="group">
                <td className={`${cell} rounded-l-[14px] text-[14px] font-semibold`}>{formatWindow(run)}</td>
                <td className={`${cell} text-[13px]`}><RunStatusCell run={run} /></td>
                <td className={`${cell} text-right font-mono text-[13px]`}>{itemsProgress(run)}</td>
                <td className={`${cell} text-right font-mono text-[13px]`}>{count(run.matchedCount)}</td>
                <td className={`${cell} text-right font-mono text-[13px]`}>{count(run.divergentCount)}</td>
                <td className={`${cell} font-mono text-[13px] text-muted`}>{formatImportedAt(run.createdAt)}</td>
                <td className={`${cell} rounded-r-[14px] text-right`}>
                  <Link href={`/conciliacoes/${encodeURIComponent(run.id)}`} className="inline-flex h-8 items-center rounded-full bg-surface px-3 text-[12px] font-semibold">
                    Ver itens
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <nav aria-label="Paginação" className="flex items-center justify-end gap-2 px-2 pb-1">
          <span className="mr-auto font-mono text-[12px] text-muted">Página {page + 1} de {totalPages}</span>
          <button type="button" disabled={page === 0} onClick={() => onPageChange(page - 1)} className="flex h-9 items-center gap-1 rounded-full bg-white px-3 text-[13px] font-semibold disabled:text-muted">
            <ChevronLeft aria-hidden="true" className="size-4" />
            Anterior
          </button>
          <button type="button" disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)} className="flex h-9 items-center gap-1 rounded-full bg-white px-3 text-[13px] font-semibold disabled:text-muted">
            Próxima
            <ChevronRight aria-hidden="true" className="size-4" />
          </button>
        </nav>
      )}
    </section>
  )
}
