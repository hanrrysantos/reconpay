'use client'

import { ChevronLeft, ChevronRight, Plus } from 'lucide-react'
import { useState } from 'react'
import type { MerchantRecord } from '@/lib/api'
import { BlockError, NoMerchant } from '@/components/dashboard/states'
import { useSelectedMerchant } from '@/components/shell/MerchantMenu'
import { formatCnpj } from '@/lib/cnpj'
import { useMerchantPage } from '@/lib/queries'
import { cn } from '@/lib/utils'
import { MerchantDrawer } from './MerchantDrawer'

const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível carregar as empresas.'
const createdOn = (iso: string) => new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })

function TableSkeleton() {
  return (
    <div role="status" aria-label="Carregando empresas" aria-busy="true" className="flex animate-pulse flex-col gap-2 rounded-lg bg-surface p-4 [animation-duration:1.4s]">
      {[0, 1, 2].map(row => <div key={row} className="h-12 rounded-sm bg-[#EBEDE6]" />)}
      <span className="sr-only">Carregando…</span>
    </div>
  )
}

/** `/empresas`: the merchants the user can access, 20 per page by name; a click on the name selects it in the top menu. */
export function MerchantsPage() {
  const [page, setPage] = useState(0)
  /** null: closed; 'create': new merchant; a merchant: rename it. */
  const [drawer, setDrawer] = useState<'create' | MerchantRecord | null>(null)
  const list = useMerchantPage(page)
  const { merchantId, select } = useSelectedMerchant()
  const merchants = list.data?.content ?? []
  const totalPages = list.data?.totalPages ?? 0
  const empty = list.data !== undefined && merchants.length === 0 && page === 0

  return (
    <>
      <section className="flex flex-wrap items-end justify-between gap-6">
        <div className="flex min-w-0 flex-col gap-2">
          <p className="text-[14px] text-muted">Empresas do seu acesso</p>
          <h1 className="font-display text-[44px] leading-none font-extrabold tracking-[-0.04em]">Empresas</h1>
        </div>
        {!empty && (
          <button type="button" onClick={() => setDrawer('create')} className="flex h-11 items-center gap-2 rounded-full bg-primary px-5 text-[14px] font-semibold text-foreground">
            <Plus aria-hidden="true" className="size-4" />
            Cadastrar empresa
          </button>
        )}
      </section>

      {list.isPending && <TableSkeleton />}
      {list.isError && <BlockError message={messageOf(list.error)} action="Tentar de novo" onRetry={() => void list.refetch()} />}
      {empty && <NoMerchant onCreate={() => setDrawer('create')} />}

      {list.data && !empty && (
        <section className="flex flex-col gap-2 rounded-lg bg-surface p-2">
          <div className="overflow-x-auto rounded-[18px] bg-white p-1.5">
            <table aria-label="Empresas" className="w-full min-w-[560px] border-separate border-spacing-y-1 text-left">
              <thead className="text-[12px] text-muted">
                <tr>
                  <th scope="col" className="px-3 font-normal">Nome</th>
                  <th scope="col" className="w-[200px] px-3 font-normal">CNPJ</th>
                  <th scope="col" className="w-[120px] px-3 font-normal">Cadastro</th>
                  <th scope="col" className="w-[200px] px-3"><span className="sr-only">Ações</span></th>
                </tr>
              </thead>
              <tbody>
                {merchants.map(merchant => {
                  const selected = merchant.id === merchantId
                  return (
                    <tr key={merchant.id} aria-current={selected || undefined} className="group">
                      <td className={cn('rounded-l-[14px] px-3 py-2 group-hover:bg-subtle', selected && 'bg-surface group-hover:bg-surface')}>
                        <div className="flex items-center gap-2">
                          <button type="button" onClick={() => select(merchant.id)} className="truncate text-left text-[14px] font-semibold hover:underline hover:underline-offset-2">
                            {merchant.name}
                          </button>
                          {selected && <span className="rounded-full bg-primary-soft px-2.5 py-0.5 text-[12px] font-semibold">Selecionada</span>}
                        </div>
                      </td>
                      <td className={cn('px-3 py-2 font-mono text-[13px] group-hover:bg-subtle', selected && 'bg-surface group-hover:bg-surface')}>{formatCnpj(merchant.document)}</td>
                      <td className={cn('px-3 py-2 font-mono text-[13px] text-muted group-hover:bg-subtle', selected && 'bg-surface group-hover:bg-surface')}>{createdOn(merchant.createdAt)}</td>
                      <td className={cn('rounded-r-[14px] px-3 py-2 group-hover:bg-subtle', selected && 'bg-surface group-hover:bg-surface')}>
                        <div className="flex justify-end gap-1">
                          <button type="button" onClick={() => setDrawer(merchant)} className="h-8 rounded-full bg-white px-3 text-[13px] font-semibold shadow-[inset_0_0_0_1px_var(--color-border)]">Editar</button>
                        </div>
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
      )}

      {drawer !== null && <MerchantDrawer merchant={drawer === 'create' ? undefined : drawer} onClose={() => setDrawer(null)} />}
    </>
  )
}
