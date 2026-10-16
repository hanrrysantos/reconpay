'use client'

import { ChevronLeft, ChevronRight } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { METHOD_LABELS, TYPE_LABELS } from '@/components/dashboard/format'
import { BlockError } from '@/components/dashboard/states'
import type { DiscrepancyType, ReconciliationItem, RunItemFilters } from '@/lib/api'
import { formatMoney } from '@/lib/money'
import { useRunItems } from '@/lib/queries'
import { RESULT_FILTER_LABELS, RESULT_LABELS, discrepancyBadge } from '@/lib/reconciliations'
import { formatTransactionDate } from '@/lib/transactions'

type ItemResult = ReconciliationItem['result']

const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível carregar os itens.'
const MISSING = '—'
const money = (value: string | null) => value === null ? MISSING : formatMoney(value)
const select = 'h-11 rounded-full bg-surface px-4 text-[14px] font-semibold outline-none focus:shadow-[inset_0_0_0_2px_var(--color-foreground)]'
const cell = 'px-3 py-2 group-hover:bg-subtle'

function Filter({ id, label, value, onChange, children }: { id: string; label: string; value: string; onChange: (value: string) => void; children: ReactNode }) {
  return (
    <div className="flex items-center gap-2">
      <label htmlFor={id} className="text-[13px] text-muted">{label}</label>
      <select id={id} value={value} onChange={event => onChange(event.target.value)} className={select}>
        <option value="">Todos</option>
        {children}
      </select>
    </div>
  )
}

function TableSkeleton() {
  return (
    <div role="status" aria-label="Carregando itens" aria-busy="true" className="flex animate-pulse flex-col gap-2 rounded-lg bg-surface p-4 [animation-duration:1.4s]">
      {[0, 1, 2].map(row => <div key={row} className="h-12 rounded-sm bg-[#EBEDE6]" />)}
      <span className="sr-only">Carregando…</span>
    </div>
  )
}

type Props = { merchantId: string; runId: string; polling: boolean }

/** Items of one run, 20 per page, largest absolute difference first, filtered by result and discrepancy type. */
export function RunItems({ merchantId, runId, polling }: Props) {
  const [result, setResult] = useState<ItemResult | ''>('')
  const [discrepancyType, setDiscrepancyType] = useState<DiscrepancyType | ''>('')
  const [page, setPage] = useState(0)
  const filters: RunItemFilters = {
    ...(result ? { result } : {}),
    ...(discrepancyType ? { discrepancyType } : {}),
  }
  const list = useRunItems(merchantId, runId, filters, page, polling)
  const items = list.data?.content ?? []
  const totalPages = list.data?.totalPages ?? 0
  const empty = list.data !== undefined && items.length === 0 && page === 0

  let body: ReactNode
  if (list.isPending) body = <TableSkeleton />
  else if (list.isError) body = <BlockError message={messageOf(list.error)} action="Tentar novamente" onRetry={() => void list.refetch()} />
  else if (empty) body = <p className="rounded-lg bg-surface p-7 text-[14px] text-muted">Nenhum item encontrado para os filtros escolhidos.</p>
  else body = (
    <section className="flex flex-col gap-2 rounded-lg bg-surface p-2">
      <div className="overflow-x-auto rounded-[18px] bg-white p-1.5">
        <table aria-label="Itens" className="w-full min-w-[1040px] border-separate border-spacing-y-1 text-left">
          <thead className="text-[12px] text-muted">
            <tr>
              <th scope="col" className="px-3 font-normal">Referência</th>
              <th scope="col" className="w-[110px] px-3 font-normal">Data da venda</th>
              <th scope="col" className="w-[90px] px-3 font-normal">Método</th>
              <th scope="col" className="w-[130px] px-3 text-right font-normal">Líquido esperado</th>
              <th scope="col" className="w-[130px] px-3 text-right font-normal">Líquido liquidado</th>
              <th scope="col" className="w-[130px] px-3 text-right font-normal">Diferença</th>
              <th scope="col" className="w-[100px] px-3 font-normal">Resultado</th>
              <th scope="col" className="w-[240px] px-3 font-normal">Divergências</th>
            </tr>
          </thead>
          <tbody>
            {items.map(item => (
              <tr key={item.id} className="group">
                <td className={`${cell} rounded-l-[14px] truncate text-[14px] font-semibold`}>{item.externalReference}</td>
                <td className={`${cell} font-mono text-[13px]`}>{item.transactionDate === null ? MISSING : formatTransactionDate(item.transactionDate)}</td>
                <td className={`${cell} text-[13px]`}>{item.paymentMethod === null ? MISSING : METHOD_LABELS[item.paymentMethod]?.label ?? item.paymentMethod}</td>
                <td className={`${cell} text-right font-mono text-[13px]`}>{money(item.expectedNetAmount)}</td>
                <td className={`${cell} text-right font-mono text-[13px]`}>{money(item.settlementNetAmount)}</td>
                <td className={`${cell} text-right font-mono text-[13px]`}>{formatMoney(item.netDifference, { signed: true })}</td>
                <td className={`${cell} text-[13px]`}>{RESULT_LABELS[item.result]}</td>
                <td className={`${cell} rounded-r-[14px]`}>
                  {item.discrepancies.length > 0 && (
                    <ul className="flex flex-wrap gap-1">
                      {item.discrepancies.map(discrepancy => (
                        <li key={discrepancy.id} className="rounded-full bg-surface px-2 py-0.5 text-[11px] font-semibold">{discrepancyBadge(discrepancy)}</li>
                      ))}
                    </ul>
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
          <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)} className="flex h-9 items-center gap-1 rounded-full bg-white px-3 text-[13px] font-semibold disabled:text-muted">
            <ChevronLeft aria-hidden="true" className="size-4" />
            Anterior
          </button>
          <button type="button" disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)} className="flex h-9 items-center gap-1 rounded-full bg-white px-3 text-[13px] font-semibold disabled:text-muted">
            Próxima
            <ChevronRight aria-hidden="true" className="size-4" />
          </button>
        </nav>
      )}
    </section>
  )

  return (
    <>
      <div className="flex flex-wrap items-center gap-3">
        <Filter id="run-item-result-filter" label="Resultado" value={result} onChange={value => { setResult(value as ItemResult | ''); setPage(0) }}>
          {(Object.keys(RESULT_FILTER_LABELS) as ItemResult[]).map(key => <option key={key} value={key}>{RESULT_FILTER_LABELS[key]}</option>)}
        </Filter>
        <Filter id="run-item-type-filter" label="Tipo de divergência" value={discrepancyType} onChange={value => { setDiscrepancyType(value as DiscrepancyType | ''); setPage(0) }}>
          {(Object.keys(TYPE_LABELS) as DiscrepancyType[]).map(key => <option key={key} value={key}>{TYPE_LABELS[key]}</option>)}
        </Filter>
      </div>
      {body}
    </>
  )
}
