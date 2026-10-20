import { act, renderHook, waitFor } from '@testing-library/react'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from './api'
import type { User, UserFilters } from './api'
import {
  createQueryClient, useActivateUser, useCreateUser, useDeactivateUser, useRenameUser, useReplaceUserAccess, useUserAccess, useUserPage,
} from './queries'

vi.mock('./auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'admin-1' }, signOut: vi.fn() }) }))
vi.mock('./api', async importOriginal => {
  const original = await importOriginal<typeof import('./api')>()
  return {
    ...original,
    getUserPage: vi.fn(),
    getUserAccess: vi.fn(),
    createUser: vi.fn(),
    renameUser: vi.fn(),
    deactivateUser: vi.fn(),
    activateUser: vi.fn(),
    replaceUserAccess: vi.fn(),
  }
})

const ana: User = {
  id: 'u-1', name: 'Ana', email: 'ana@reconpay.local', role: 'OPERATOR', active: true, createdAt: '2026-10-01T12:00:00Z', demoExpiresAt: null,
}
const bruno: User = { ...ana, id: 'u-2', name: 'Bruno', email: 'bruno@reconpay.local' }
const page = (...users: User[]) => ({ content: users, totalPages: 1, totalElements: users.length })
const active: UserFilters = { status: 'ACTIVE', demo: false }

let client: QueryClient
const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>
const pageCache = () => client.getQueryData(['userPage', 'ACTIVE', false, 0])

/** The list answers with `users` from the next request on, after a real delay, so a mutation must wait for the refetch. */
const later = <T,>(value: T) => () => new Promise<T>(resolve => setTimeout(() => resolve(value), 20))
const serve = (...users: User[]) => vi.mocked(api.getUserPage).mockImplementation(later(page(...users)))

beforeEach(() => {
  client = createQueryClient(vi.fn())
  vi.mocked(api.getUserPage).mockReset()
  vi.mocked(api.getUserAccess).mockReset()
})

afterEach(() => client.clear())

describe('useUserPage', () => {
  it('com enabled = false não chama a API', async () => {
    serve(ana)
    const { result } = renderHook(() => useUserPage(active, 0, false), { wrapper })
    await new Promise(resolve => setTimeout(resolve, 40))
    expect(api.getUserPage).not.toHaveBeenCalled()
    expect(result.current.data).toBeUndefined()
  })

  it('busca com os filtros e a página pedidos e guarda cada combinação na própria chave', async () => {
    serve(ana)
    const { result, rerender } = renderHook(({ filters, n }) => useUserPage(filters, n, true), {
      wrapper, initialProps: { filters: { status: 'INACTIVE', demo: true } as UserFilters, n: 2 },
    })
    await waitFor(() => expect(result.current.data).toEqual(page(ana)))
    expect(api.getUserPage).toHaveBeenCalledWith('jwt', { status: 'INACTIVE', demo: true }, 2)
    expect(client.getQueryData(['userPage', 'INACTIVE', true, 2])).toEqual(page(ana))

    serve(bruno)
    rerender({ filters: { status: 'ALL', demo: false }, n: 0 })
    await waitFor(() => expect(result.current.data).toEqual(page(bruno)))
    expect(api.getUserPage).toHaveBeenLastCalledWith('jwt', { status: 'ALL', demo: false }, 0)
    expect(client.getQueryData(['userPage', 'ALL', false, 0])).toEqual(page(bruno))
    expect(client.getQueryData(['userPage', 'INACTIVE', true, 2])).toEqual(page(ana))
  })
})

describe('useUserAccess', () => {
  it('busca os acessos do usuário', async () => {
    vi.mocked(api.getUserAccess).mockResolvedValue(['m-1'])
    const { result } = renderHook(() => useUserAccess('u-1'), { wrapper })
    await waitFor(() => expect(result.current.data).toEqual(['m-1']))
    expect(api.getUserAccess).toHaveBeenCalledWith('jwt', 'u-1')
  })
})

describe('mutações de usuário', () => {
  async function renderList<T>(mutation: () => T) {
    serve(ana)
    const rendered = renderHook(() => ({ list: useUserPage(active, 0, true), mutation: mutation() }), { wrapper })
    await waitFor(() => expect(rendered.result.current.list.data).toEqual(page(ana)))
    return rendered.result
  }

  it('ao criar, a lista já traz a conta nova quando a mutação resolve', async () => {
    const result = await renderList(useCreateUser)
    vi.mocked(api.createUser).mockResolvedValue(bruno)
    serve(ana, bruno)
    const input = { name: 'Bruno', email: 'bruno@reconpay.local', password: 'Operador1', role: 'OPERATOR' as const }
    await act(() => result.current.mutation.mutateAsync(input))
    expect(api.createUser).toHaveBeenCalledWith('jwt', input)
    expect(pageCache()).toEqual(page(ana, bruno))
  })

  it('ao renomear, refaz a lista', async () => {
    const result = await renderList(useRenameUser)
    const renamed = { ...ana, name: 'Ana Souza' }
    vi.mocked(api.renameUser).mockResolvedValue(renamed)
    serve(renamed)
    await act(() => result.current.mutation.mutateAsync({ userId: 'u-1', name: 'Ana Souza' }))
    expect(api.renameUser).toHaveBeenCalledWith('jwt', 'u-1', 'Ana Souza')
    expect(pageCache()).toEqual(page(renamed))
  })

  it('ao desativar, refaz a lista', async () => {
    const result = await renderList(useDeactivateUser)
    vi.mocked(api.deactivateUser).mockResolvedValue(undefined)
    serve()
    await act(() => result.current.mutation.mutateAsync('u-1'))
    expect(api.deactivateUser).toHaveBeenCalledWith('jwt', 'u-1')
    expect(pageCache()).toEqual(page())
  })

  it('ao reativar, refaz a lista', async () => {
    const result = await renderList(useActivateUser)
    vi.mocked(api.activateUser).mockResolvedValue(bruno)
    serve(ana, bruno)
    await act(() => result.current.mutation.mutateAsync('u-2'))
    expect(api.activateUser).toHaveBeenCalledWith('jwt', 'u-2')
    expect(pageCache()).toEqual(page(ana, bruno))
  })

  it('ao substituir acessos, refaz a lista e os acessos daquele usuário', async () => {
    vi.mocked(api.getUserAccess).mockImplementation(later(['m-1']))
    serve(ana)
    const { result } = renderHook(() => ({
      list: useUserPage(active, 0, true), access: useUserAccess('u-1'), replace: useReplaceUserAccess(),
    }), { wrapper })
    await waitFor(() => expect(result.current.access.data).toEqual(['m-1']))
    await waitFor(() => expect(result.current.list.data).toEqual(page(ana)))
    const listCalls = vi.mocked(api.getUserPage).mock.calls.length

    vi.mocked(api.replaceUserAccess).mockResolvedValue(['m-2'])
    vi.mocked(api.getUserAccess).mockImplementation(later(['m-2']))
    await act(() => result.current.replace.mutateAsync({ userId: 'u-1', merchantIds: ['m-2'] }))

    expect(api.replaceUserAccess).toHaveBeenCalledWith('jwt', 'u-1', ['m-2'])
    expect(client.getQueryData(['userAccess', 'u-1'])).toEqual(['m-2'])
    expect(vi.mocked(api.getUserPage).mock.calls.length).toBe(listCalls + 1)
  })
})
