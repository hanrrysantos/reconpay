'use client'

import { ChevronLeft, ChevronRight } from 'lucide-react'
import { METHOD_LABELS } from '@/components/dashboard/format'
import { BlockError } from '@/components/dashboard/states'
import type { Transaction, TransactionFilters } from '@/lib/api'
import { formatMoney } from '@/lib/money'
import { useTransactionPage } from '@/lib/queries'
import { TRANSACTION_STATUS_LABELS, formatTransactionDate } from '@/lib/transactions'

const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível carregar as transações.'

function TableSkeleton() {
  return (
    <div role="status" aria-label="Carregando transações" aria-busy="true" className="flex animate-pulse flex-col gap-2 rounded-lg bg-surface p-4 [animation-duration:1.4s]">
      {[0, 1, 2].map(row => <div key={row} className="h-12 rounded-sm bg-[#EBEDE6]" />)}
      <span className="sr-only">Carregando…</span>
    </div>
  )
}

type Props = {
  merchantId: string
  filters: TransactionFilters
  page: number
  onPageChange: (page: number) => void
  onChangeStatus: (transaction: Transaction) => void
}

const cell = 'px-3 py-2 group-hover:bg-subtle'

/** Transactions of the filters, 20 per page, newest transaction date first; only approved ones can change status. */
export function TransactionList({ merchantId, filters, page, onPageChange, onChangeStatus }: Props) {
  const list = useTransactionPage(merchantId, filters, page)
  const transactions = list.data?.content ?? []
  const totalPages = list.data?.totalPages ?? 0
  const empty = list.data !== undefined && transactions.length === 0 && page === 0

  if (list.isPending) return <TableSkeleton />
  if (list.isError) return <BlockError message={messageOf(list.error)} action="Tentar novamente" onRetry={() => void list.refetch()} />
  if (empty) return <p className="rounded-lg bg-surface p-7 text-[14px] text-muted">Nenhuma transação encontrada para os filtros escolhidos.</p>

  return (
    <section className="flex flex-col gap-2 rounded-lg bg-surface p-2">
      <div className="overflow-x-auto rounded-[18px] bg-white p-1.5">
        <table aria-label="Transações" className="w-full min-w-[860px] border-separate border-spacing-y-1 text-left">
          <thead className="text-[12px] text-muted">
            <tr>
              <th scope="col" className="w-[110px] px-3 font-normal">Data</th>
              <th scope="col" className="px-3 font-normal">Referência</th>
              <th scope="col" className="w-[90px] px-3 font-normal">Método</th>
              <th scope="col" className="w-[80px] px-3 font-normal">Parcelas</th>
              <th scope="col" className="w-[130px] px-3 text-right font-normal">Bruto</th>
              <th scope="col" className="w-[140px] px-3 text-right font-normal">Líquido esperado</th>
              <th scope="col" className="w-[110px] px-3 font-normal">Status</th>
              <th scope="col" className="w-[140px] px-3 font-normal"><span className="sr-only">Ação</span></th>
            </tr>
          </thead>
          <tbody>
            {transactions.map(transaction => (
              <tr key={transaction.id} className="group">
                <td className={`${cell} rounded-l-[14px] font-mono text-[13px]`}>{formatTransactionDate(transaction.transactionDate)}</td>
                <td className={`${cell} truncate text-[14px] font-semibold`}>{transaction.externalReference}</td>
                <td className={`${cell} text-[13px]`}>{METHOD_LABELS[transaction.paymentMethod]?.label ?? transaction.paymentMethod}</td>
                <td className={`${cell} font-mono text-[13px]`}>{transaction.installments}x</td>
                <td className={`${cell} text-right font-mono text-[13px]`}>{formatMoney(transaction.amount)}</td>
                <td className={`${cell} text-right font-mono text-[13px]`}>{formatMoney(transaction.expectedNetAmount)}</td>
                <td className={`${cell} text-[13px]`}>{TRANSACTION_STATUS_LABELS[transaction.status]}</td>
                <td className={`${cell} rounded-r-[14px] text-right`}>
                  {transaction.status === 'APPROVED' && (
                    <button type="button" onClick={() => onChangeStatus(transaction)} className="h-8 rounded-full bg-surface px-3 text-[12px] font-semibold">
                      Alterar status
                    </button>
                  )}
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
