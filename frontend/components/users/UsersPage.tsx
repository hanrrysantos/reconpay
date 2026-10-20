'use client'

import { ChevronLeft, ChevronRight, Plus, X } from 'lucide-react'
import { useRouter } from 'next/navigation'
import { useEffect, useState } from 'react'
import { BlockError } from '@/components/dashboard/states'
import { formatDemoExpiry } from '@/components/shell/DemoBanner'
import type { User, UserFilters } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import { useActivateUser, useUserPage } from '@/lib/queries'
import { cn } from '@/lib/utils'
import { AccessDrawer } from './AccessDrawer'
import { DeactivateUserDialog } from './DeactivateUserDialog'
import { UserDrawer } from './UserDrawer'

const messageOf = (error: unknown, fallback: string) => error instanceof Error ? error.message : fallback
const createdOn = (iso: string) => new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })

const STATUS_OPTIONS: Array<[UserFilters['status'], string]> = [['ACTIVE', 'Ativos'], ['INACTIVE', 'Inativos'], ['ALL', 'Todos']]
const ROLE_LABEL = { ADMIN: 'Admin', OPERATOR: 'Operador' } as const

const action = 'h-8 rounded-full bg-white px-3 text-[13px] font-semibold shadow-[inset_0_0_0_1px_var(--color-border)]'
const cell = 'px-3 py-2 group-hover:bg-subtle'

function TableSkeleton() {
  return (
    <div role="status" aria-label="Carregando usuários" aria-busy="true" className="flex animate-pulse flex-col gap-2 rounded-lg bg-surface p-4 [animation-duration:1.4s]">
      {[0, 1, 2].map(row => <div key={row} className="h-12 rounded-sm bg-[#EBEDE6]" />)}
      <span className="sr-only">Carregando…</span>
    </div>
  )
}

/** `/usuarios`, ADMIN only: every account by name, 20 per page, filtered by status and demo accounts. */
export function UsersPage() {
  const { user: me } = useAuth()
  const router = useRouter()
  const admin = me?.role === 'ADMIN'
  // The API answers 403 to an OPERATOR; the page leaves before asking.
  useEffect(() => { if (me && !admin) router.replace('/painel') }, [me, admin, router])

  const [filters, setFilters] = useState<UserFilters>({ status: 'ACTIVE', demo: false })
  const [page, setPage] = useState(0)
  const [notice, setNotice] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  /** null: closed; 'create': new account; an account: rename it. */
  const [drawer, setDrawer] = useState<'create' | User | null>(null)
  const [deactivating, setDeactivating] = useState<User | null>(null)
  const [granting, setGranting] = useState<User | null>(null)
  const list = useUserPage(filters, page, admin)
  const activate = useActivateUser()
  const users = list.data?.content ?? []
  const totalPages = list.data?.totalPages ?? 0
  // A page left empty by a deactivation (here or in another tab) steps back to the new last page.
  useEffect(() => {
    if (list.data && list.data.content.length === 0 && page > 0) setPage(Math.max(list.data.totalPages - 1, 0))
  }, [list.data, page])

  if (!admin) return null

  function changeFilters(next: Partial<UserFilters>) {
    setFilters(current => ({ ...current, ...next }))
    setPage(0)
  }

  function done(message: string) {
    setDrawer(null)
    setDeactivating(null)
    setGranting(null)
    setError(null)
    setNotice(message)
  }

  async function reactivate(user: User) {
    try {
      await activate.mutateAsync(user.id)
      done('Usuário reativado.')
    } catch (failure) {
      setNotice(null)
      setError(messageOf(failure, 'Não foi possível reativar.'))
    }
  }

  function actions(user: User) {
    if (!user.active) {
      return <button type="button" disabled={activate.isPending} onClick={() => void reactivate(user)} className={action}>Reativar</button>
    }
    const deactivate = user.id !== me?.id && <button type="button" onClick={() => setDeactivating(user)} className={cn(action, 'text-danger-strong')}>Desativar</button>
    if (user.demoExpiresAt) return deactivate
    return (
      <>
        {user.role === 'ADMIN'
          ? <span className="px-2 text-[13px] text-muted">Acessa todas as empresas</span>
          : <button type="button" onClick={() => setGranting(user)} className={action}>Acessos</button>}
        <button type="button" onClick={() => setDrawer(user)} className={action}>Renomear</button>
        {deactivate}
      </>
    )
  }

  return (
    <>
      <section className="flex flex-wrap items-end justify-between gap-6">
        <div className="flex min-w-0 flex-col gap-2">
          <p className="text-[14px] text-muted">Contas da instalação</p>
          <h1 className="font-display text-[44px] leading-none font-extrabold tracking-[-0.04em]">Usuários</h1>
        </div>
        <button type="button" onClick={() => setDrawer('create')} className="flex h-11 items-center gap-2 rounded-full bg-primary px-5 text-[14px] font-semibold text-foreground">
          <Plus aria-hidden="true" className="size-4" />
          Novo usuário
        </button>
      </section>

      <section className="flex flex-wrap items-center gap-3">
        <div role="group" aria-label="Status" className="flex gap-1 rounded-full bg-surface p-1">
          {STATUS_OPTIONS.map(([status, label]) => (
            <button
              key={status}
              type="button"
              aria-pressed={filters.status === status}
              onClick={() => changeFilters({ status })}
              className={cn('rounded-full px-4 py-2 text-[14px] font-medium', filters.status === status ? 'bg-foreground text-white' : 'text-muted hover:text-foreground')}
            >
              {label}
            </button>
          ))}
        </div>
        <label className="flex items-center gap-2 text-[14px] font-medium">
          <input type="checkbox" checked={filters.demo} onChange={event => changeFilters({ demo: event.target.checked })} className="size-4 accent-foreground" />
          Mostrar demonstrações
        </label>
      </section>

      {notice && (
        <section role="status" className="flex items-center gap-3 rounded-lg bg-primary-soft px-5 py-4 text-[14px]">
          <span>{notice}</span>
          <button type="button" aria-label="Fechar aviso" onClick={() => setNotice(null)} className="ml-auto grid size-9 place-items-center rounded-full bg-white">
            <X aria-hidden="true" className="size-4" />
          </button>
        </section>
      )}
      {error && <p role="alert" className="rounded-md bg-danger-soft px-4 py-3 text-[13px] text-danger-strong">{error}</p>}

      {list.isPending && <TableSkeleton />}
      {list.isError && <BlockError message={messageOf(list.error, 'Não foi possível carregar os usuários.')} action="Tentar de novo" onRetry={() => void list.refetch()} />}
      {list.data && users.length === 0 && page === 0 && <p className="rounded-lg bg-surface p-7 text-[14px] text-muted">Nenhum usuário encontrado.</p>}

      {list.data && users.length > 0 && (
        <section className="flex flex-col gap-2 rounded-lg bg-surface p-2">
          <div className="overflow-x-auto rounded-[18px] bg-white p-1.5">
            <table aria-label="Usuários" className="w-full min-w-[820px] border-separate border-spacing-y-1 text-left">
              <thead className="text-[12px] text-muted">
                <tr>
                  <th scope="col" className="px-3 font-normal">Nome</th>
                  <th scope="col" className="px-3 font-normal">E-mail</th>
                  <th scope="col" className="w-[100px] px-3 font-normal">Papel</th>
                  <th scope="col" className="w-[90px] px-3 font-normal">Status</th>
                  <th scope="col" className="w-[110px] px-3 font-normal">Cadastro</th>
                  <th scope="col" className="w-[320px] px-3"><span className="sr-only">Ações</span></th>
                </tr>
              </thead>
              <tbody>
                {users.map(user => (
                  <tr key={user.id} className="group">
                    <td className={cn(cell, 'rounded-l-[14px]')}>
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-[14px] font-semibold">{user.name}</span>
                        {user.demoExpiresAt && (
                          <span className="rounded-full bg-primary-soft px-2.5 py-0.5 text-[12px] font-semibold">Demo · expira em {formatDemoExpiry(user.demoExpiresAt)}</span>
                        )}
                      </div>
                    </td>
                    <td className={cn(cell, 'text-[13px]')}>{user.email}</td>
                    <td className={cn(cell, 'text-[13px]')}>{ROLE_LABEL[user.role]}</td>
                    <td className={cn(cell, 'text-[13px]', !user.active && 'text-muted')}>{user.active ? 'Ativo' : 'Inativo'}</td>
                    <td className={cn(cell, 'font-mono text-[13px] text-muted')}>{createdOn(user.createdAt)}</td>
                    <td className={cn(cell, 'rounded-r-[14px]')}>
                      <div className="flex items-center justify-end gap-1">{actions(user)}</div>
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
      )}

      {drawer !== null && <UserDrawer user={drawer === 'create' ? undefined : drawer} onClose={() => setDrawer(null)} onDone={done} />}
      {granting !== null && <AccessDrawer user={granting} onClose={() => setGranting(null)} onDone={done} />}
      {deactivating !== null && <DeactivateUserDialog user={deactivating} onClose={() => setDeactivating(null)} onDone={done} />}
    </>
  )
}
