import type { DrawerEntry } from '@/components/dashboard/ResolveDrawer'
import type { DiscrepancyStatus, DiscrepancyType, ReconciliationItem, RunItemFilters } from './api'

export type StatusFilter = DiscrepancyStatus | 'ALL'

/** Options of the "Status" filter on `/divergencias`; open first, the operator's work queue. */
export const STATUS_FILTERS: readonly { value: StatusFilter; label: string }[] = [
  { value: 'OPEN', label: 'Abertas' },
  { value: 'ACCEPTED', label: 'Aceitas' },
  { value: 'ADJUSTED', label: 'Ajustadas' },
  { value: 'WRITTEN_OFF', label: 'Baixadas' },
  { value: 'ALL', label: 'Todas' },
]

/** The API has no "any status" filter: "Todas" asks for the divergent items, which carry every discrepancy. */
export function itemFilters(status: StatusFilter, type: DiscrepancyType | null): RunItemFilters {
  const base: RunItemFilters = status === 'ALL' ? { result: 'DIVERGENT' } : { discrepancyStatus: status }
  return type === null ? base : { ...base, discrepancyType: type }
}

/** One row per discrepancy that matches both filters, in the order of the items and of their discrepancies. */
export function rowsOf(items: ReconciliationItem[], status: StatusFilter, type: DiscrepancyType | null): DrawerEntry[] {
  return items.flatMap(item => item.discrepancies
    .filter(discrepancy => (status === 'ALL' || discrepancy.status === status) && (type === null || discrepancy.type === type))
    .map(discrepancy => ({ item, discrepancyId: discrepancy.id })))
}
