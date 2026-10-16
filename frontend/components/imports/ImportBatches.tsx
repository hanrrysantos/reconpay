'use client'

import { ChevronLeft, ChevronRight } from 'lucide-react'
import { BlockError } from '@/components/dashboard/states'
import type { ImportKind } from '@/lib/api'
import { formatImportedAt } from '@/lib/imports'
import { useImportBatches } from '@/lib/queries'

const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível carregar as importações.'

function TableSkeleton() {
  return (
    <div role="status" aria-label="Carregando importações" aria-busy="true" className="flex animate-pulse flex-col gap-2 rounded-lg bg-surface p-4 [animation-duration:1.4s]">
      {[0, 1, 2].map(row => <div key={row} className="h-12 rounded-sm bg-[#EBEDE6]" />)}
      <span className="sr-only">Carregando…</span>
    </div>
  )
}

type Props = { merchantId: string; kind: ImportKind; noun: string; page: number; onPageChange: (page: number) => void }

/** Accepted imports of one kind, 20 per page, newest first. */
export function ImportBatches({ merchantId, kind, noun, page, onPageChange }: Props) {
  const list = useImportBatches(merchantId, kind, page)
  const batches = list.data?.content ?? []
  const totalPages = list.data?.totalPages ?? 0
  const empty = list.data !== undefined && batches.length === 0 && page === 0

  if (list.isPending) return <TableSkeleton />
  if (list.isError) return <BlockError message={messageOf(list.error)} action="Tentar novamente" onRetry={() => void list.refetch()} />
  if (empty) return <p className="rounded-lg bg-surface p-7 text-[14px] text-muted">Nenhuma importação de {noun} ainda.</p>

  return (
    <section className="flex flex-col gap-2 rounded-lg bg-surface p-2">
      <div className="overflow-x-auto rounded-[18px] bg-white p-1.5">
        <table aria-label="Lotes importados" className="w-full min-w-[480px] border-separate border-spacing-y-1 text-left">
          <thead className="text-[12px] text-muted">
            <tr>
              <th scope="col" className="px-3 font-normal">Arquivo</th>
              <th scope="col" className="w-[120px] px-3 font-normal">Linhas</th>
              <th scope="col" className="w-[180px] px-3 font-normal">Importado em</th>
            </tr>
          </thead>
          <tbody>
            {batches.map(batch => (
              <tr key={batch.id} className="group">
                <td className="truncate rounded-l-[14px] px-3 py-2 text-[14px] font-semibold group-hover:bg-subtle">{batch.fileName}</td>
                <td className="px-3 py-2 font-mono text-[13px] group-hover:bg-subtle">{batch.totalRows}</td>
                <td className="rounded-r-[14px] px-3 py-2 font-mono text-[13px] text-muted group-hover:bg-subtle">{formatImportedAt(batch.createdAt)}</td>
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
