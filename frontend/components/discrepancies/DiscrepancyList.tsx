'use client'

import { ChevronLeft, ChevronRight } from 'lucide-react'
import { METHOD_LABELS, STATUS_LABELS, TYPE_LABELS } from '@/components/dashboard/format'
import type { DrawerEntry } from '@/components/dashboard/ResolveDrawer'
import { BlockError } from '@/components/dashboard/states'
import type { DiscrepancyType, ReconciliationItem } from '@/lib/api'
import { itemFilters, rowsOf, type StatusFilter } from '@/lib/discrepancies'
import { formatMoney } from '@/lib/money'
import { useRunItems } from '@/lib/queries'
import { formatTransactionDate } from '@/lib/transactions'
import { cn } from '@/lib/utils'

const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível carregar as divergências.'
const MISSING = '—'
const cell = 'px-3 py-2 group-hover:bg-subtle'

/** "Crédito 3x · 03/10/2026"; a side without a sale shows "—" for what it lacks. */
function saleLine(item: ReconciliationItem): string {
  const method = item.paymentMethod ? METHOD_LABELS[item.paymentMethod]?.label ?? item.paymentMethod : MISSING
  const installments = item.installments && item.installments > 1 ? ` ${item.installments}x` : ''
  const date = item.transactionDate ? formatTransactionDate(item.transactionDate) : MISSING
  return `${method}${installments} · ${date}`
}

function TableSkeleton() {
  return (
    <div role="status" aria-label="Carregando divergências" aria-busy="true" className="flex animate-pulse flex-col gap-2 rounded-lg bg-surface p-4 [animation-duration:1.4s]">
      {[0, 1, 2].map(row => <div key={row} className="h-12 rounded-sm bg-[#EBEDE6]" />)}
      <span className="sr-only">Carregando…</span>
    </div>
  )
}

type Props = {
  merchantId: string
  runId: string
  status: StatusFilter
  type: DiscrepancyType | null
  page: number
  onPageChange: (page: number) => void
  /** The rows of the current page and the position of the clicked one, for the drawer's J/K navigation. */
  onOpen: (rows: DrawerEntry[], index: number) => void
}

/** Discrepancies of one run, one row per discrepancy that matches the filters; the API pages by item. */
export function DiscrepancyList({ merchantId, runId, status, type, page, onPageChange, onOpen }: Props) {
  const list = useRunItems(merchantId, runId, itemFilters(status, type), page, false)
  const rows = rowsOf(list.data?.content ?? [], status, type)
  const totalPages = list.data?.totalPages ?? 0

  if (list.isPending) return <TableSkeleton />
  if (list.isError) return <BlockError message={messageOf(list.error)} action="Tentar novamente" onRetry={() => void list.refetch()} />
  if (rows.length === 0 && page === 0) {
    return <p className="rounded-lg bg-surface p-7 text-[14px] text-muted">Nenhuma divergência para os filtros escolhidos.</p>
  }

  return (
    <section className="flex flex-col gap-2 rounded-lg bg-surface p-2">
      <div className="overflow-x-auto rounded-[18px] bg-white p-1.5">
        <table aria-label="Divergências" className="w-full min-w-[760px] border-separate border-spacing-y-1 text-left">
          <thead className="text-[12px] text-muted">
            <tr>
              <th scope="col" className="px-3 font-normal">Transação</th>
              <th scope="col" className="px-3 font-normal">Tipo</th>
              <th scope="col" className="w-[130px] px-3 text-right font-normal">Esperado</th>
              <th scope="col" className="w-[130px] px-3 text-right font-normal">Diferença</th>
              <th scope="col" className="w-[110px] px-3 font-normal">Status</th>
              <th scope="col" className="w-[96px] px-3"><span className="sr-only">Ação</span></th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, index) => {
              const discrepancy = row.item.discrepancies.find(candidate => candidate.id === row.discrepancyId)!
              return (
                <tr key={`${row.item.id}:${row.discrepancyId}`} className="group">
                  <td className={cn(cell, 'rounded-l-[14px]')}>
                    <span className="flex min-w-0 flex-col">
                      <span className="truncate font-mono text-[13px] font-semibold">{row.item.externalReference}</span>
                      <span className="text-[12px] text-muted">{saleLine(row.item)}</span>
                    </span>
                  </td>
                  <td className={cell}>
                    <span className="inline-flex rounded-full bg-primary-soft px-2.5 py-1 text-[12px] font-semibold whitespace-nowrap">{TYPE_LABELS[discrepancy.type]}</span>
                  </td>
                  <td className={cn(cell, 'text-right font-mono text-[13px]')}>
                    {row.item.expectedNetAmount === null ? MISSING : formatMoney(row.item.expectedNetAmount)}
                  </td>
                  <td className={cn(cell, 'text-right font-mono text-[13px]')}>{formatMoney(row.item.netDifference, { signed: true })}</td>
                  <td className={cell}>
                    <span className={cn(
                      'inline-flex rounded-full px-2.5 py-1 text-[12px] font-semibold',
                      discrepancy.status === 'OPEN' ? 'bg-danger-soft text-danger-strong' : 'bg-primary-soft text-success',
                    )}>{STATUS_LABELS[discrepancy.status]}</span>
                  </td>
                  <td className={cn(cell, 'rounded-r-[14px] text-right')}>
                    <button
                      type="button"
                      onClick={() => onOpen(rows, index)}
                      aria-label={`Abrir ${row.item.externalReference} · ${TYPE_LABELS[discrepancy.type]}`}
                      className="h-8 rounded-full bg-white px-3 text-[13px] font-semibold shadow-[inset_0_0_0_1px_var(--color-border)]"
                    >
                      Abrir
                    </button>
                  </td>
                </tr>
              )
            })}
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
