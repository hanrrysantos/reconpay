'use client'

import { ArrowRight, ChevronRight } from 'lucide-react'
import Link from 'next/link'
import type { Discrepancy, DiscrepancyType, ReconciliationItem } from '@/lib/api'
import { formatMoney } from '@/lib/money'
import { cn } from '@/lib/utils'
import { METHOD_LABELS, STATUS_LABELS, TYPE_LABELS, shortDate } from './format'

export const TABLE_ROWS = 4

export type OpenDiscrepancyTableProps = {
  /** Open items of the current run, largest absolute difference first; undefined while loading. */
  items: ReconciliationItem[] | undefined
  /** Total open items for the active filter (page `totalElements`). */
  total: number | null
  /** Types offered as filters (the window's open types). */
  types: DiscrepancyType[]
  typeFilter: DiscrepancyType | null
  onFilter: (type: DiscrepancyType | null) => void
  onResolve: (item: ReconciliationItem, discrepancy: Discrepancy) => void
  /** Locked window: rows open the drawer read-only ("Detalhes"). */
  locked: boolean
}

/** The discrepancy a row stands for: the open one of the filtered type, else the first open one. */
export function rowDiscrepancy(item: ReconciliationItem, type: DiscrepancyType | null): Discrepancy | undefined {
  const open = item.discrepancies.filter(discrepancy => discrepancy.status === 'OPEN')
  return open.find(discrepancy => discrepancy.type === type) ?? open[0] ?? item.discrepancies[0]
}

function methodLine(item: ReconciliationItem): string {
  const method = item.paymentMethod ? METHOD_LABELS[item.paymentMethod]?.label ?? item.paymentMethod : 'Sem venda'
  const installments = item.installments && item.installments > 1 ? ` ${item.installments}x` : ''
  const date = item.transactionDate ?? item.settlementDate
  return `${method}${installments}${date ? ` · ${shortDate(date)}` : ''}`
}

const money = (value: string | null) => value === null ? '—' : formatMoney(value, { currency: false })

export function OpenDiscrepancyTable({ items, total, types, typeFilter, onFilter, onResolve, locked }: OpenDiscrepancyTableProps) {
  const rows = (items ?? []).slice(0, TABLE_ROWS)
  const filters: Array<{ type: DiscrepancyType | null; label: string }> = [
    { type: null, label: 'Todas' },
    ...types.map(type => ({ type, label: TYPE_LABELS[type] })),
  ]

  return (
    <section className="flex min-w-0 flex-1 flex-col gap-2 rounded-lg bg-surface p-2">
      <header className="flex flex-wrap items-center justify-between gap-3 px-4 pt-4 pb-2">
        <h2 className="font-display text-[22px] font-extrabold tracking-[-0.03em]">Divergências abertas</h2>
        <div role="group" aria-label="Filtrar por tipo" className="flex flex-wrap gap-1">
          {filters.map(filter => (
            <button
              key={filter.type ?? 'ALL'}
              type="button"
              aria-pressed={typeFilter === filter.type}
              onClick={() => onFilter(filter.type)}
              className={cn('h-8 rounded-full px-3 text-[13px] font-semibold', typeFilter === filter.type ? 'bg-white shadow-[0_1px_2px_rgba(0,0,0,0.08)]' : 'text-muted')}
            >
              {filter.label}
            </button>
          ))}
        </div>
      </header>

      <div className="flex flex-col gap-1 overflow-x-auto rounded-[18px] bg-white p-1.5">
        <table className="w-full min-w-[720px] border-separate border-spacing-y-1 text-left">
          <thead className="text-[12px] text-muted">
            <tr>
              <th scope="col" className="w-[200px] px-3 font-normal">Transação</th>
              <th scope="col" className="px-3 font-normal">Tipo</th>
              <th scope="col" className="w-[100px] px-3 text-right font-normal">Esperado</th>
              <th scope="col" className="w-[100px] px-3 text-right font-normal">Diferença</th>
              <th scope="col" className="w-[100px] px-3 font-normal">Status</th>
              <th scope="col" className="w-[104px] px-3"><span className="sr-only">Ação</span></th>
            </tr>
          </thead>
          <tbody>
            {rows.map(item => {
              const discrepancy = rowDiscrepancy(item, typeFilter)
              const method = item.paymentMethod ? METHOD_LABELS[item.paymentMethod]?.short ?? item.paymentMethod.slice(0, 3) : '—'
              return (
                <tr key={item.id} className="group">
                  <td className="rounded-l-[14px] px-3 py-2 group-hover:bg-subtle group-focus-within:bg-subtle">
                    <div className="flex items-center gap-3">
                      <span aria-hidden="true" className="grid size-[34px] shrink-0 place-items-center rounded-sm bg-surface font-mono text-[11px] font-semibold">{method}</span>
                      <span className="flex min-w-0 flex-col">
                        <span className="truncate font-mono text-[13px] font-semibold">{item.externalReference}</span>
                        <span className="text-[12px] text-muted">{methodLine(item)}</span>
                      </span>
                    </div>
                  </td>
                  <td className="px-3 py-2 group-hover:bg-subtle group-focus-within:bg-subtle">
                    {discrepancy && (
                      <span className={cn(
                        'inline-flex rounded-full px-2.5 py-1 text-[12px] font-semibold whitespace-nowrap',
                        discrepancy.type === 'MISSING_SETTLEMENT' ? 'bg-foreground text-white' : 'bg-primary-soft',
                      )}>{TYPE_LABELS[discrepancy.type]}</span>
                    )}
                  </td>
                  <td className="px-3 py-2 text-right font-mono text-[13px] group-hover:bg-subtle group-focus-within:bg-subtle">{money(item.expectedNetAmount)}</td>
                  <td className="px-3 py-2 text-right font-mono text-[13px] text-danger group-hover:bg-subtle group-focus-within:bg-subtle">
                    {formatMoney(item.netDifference, { currency: false, signed: true })}
                  </td>
                  <td className="px-3 py-2 group-hover:bg-subtle group-focus-within:bg-subtle">
                    {discrepancy && (
                      <span className={cn(
                        'inline-flex rounded-full px-2.5 py-1 text-[12px] font-semibold',
                        discrepancy.status === 'OPEN' ? 'bg-danger-soft text-danger-strong' : 'bg-primary-soft text-success',
                      )}>{STATUS_LABELS[discrepancy.status]}</span>
                    )}
                  </td>
                  <td className="rounded-r-[14px] px-3 py-2 text-right group-hover:bg-subtle group-focus-within:bg-subtle">
                    {discrepancy && (
                      <button
                        type="button"
                        onClick={() => onResolve(item, discrepancy)}
                        aria-label={`${locked ? 'Detalhes' : 'Resolver'} ${item.externalReference}`}
                        className="inline-flex h-8 items-center gap-1 rounded-full bg-white px-3 text-[13px] font-semibold shadow-[inset_0_0_0_1px_var(--color-border)] group-hover:bg-foreground group-hover:text-white group-hover:shadow-none group-focus-within:bg-foreground group-focus-within:text-white"
                      >
                        {locked ? 'Detalhes' : 'Resolver'}
                        <ChevronRight aria-hidden="true" className="size-3.5 group-hover:text-primary" />
                      </button>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
        {items !== undefined && rows.length === 0 && (
          <p className="px-3 py-6 text-center text-[13px] text-muted">Nenhuma divergência aberta</p>
        )}
      </div>

      {total !== null && total > 0 && (
        <footer className="flex flex-wrap items-center justify-between gap-2 px-4 py-3 text-[13px]">
          <span className="text-muted">Mostrando {rows.length} de {total} · maior diferença primeiro</span>
          <Link href="/divergencias" className="inline-flex items-center gap-1 font-semibold">
            Ver todas as {total} <ArrowRight aria-hidden="true" className="size-3.5" />
          </Link>
        </footer>
      )}
    </section>
  )
}
