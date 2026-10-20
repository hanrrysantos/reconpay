import { vi } from 'vitest'
import type { Merchant, User, UserRole } from '@/lib/api'

/** Test support for the `/usuarios` screen: an in-memory `/api/users` that the tests swap in for `fetch`. */

export type Reply = Response | Error | (() => Response | Promise<Response>)
export type Call = { method: string; path: string; search: URLSearchParams; body: unknown }
type CreateInput = { name: string; email: string; password: string; role: UserRole }

export const json = (body: unknown, status = 200) => Response.json(body, { status })
export const fail = (status: number, message: string) => json({ error: status === 409 ? 'CONFLICT' : 'ERROR', message }, status)
const page = <T,>(content: T[], number: number, totalPages: number) => ({ content, number, totalPages, totalElements: content.length })

export const account = (id: string, name: string, extra: Partial<User> = {}): User => ({
  id, name, email: `${id}@reconpay.dev`, role: 'OPERATOR', active: true, createdAt: '2026-10-01T12:00:00Z', demoExpiresAt: null, ...extra,
})

export type FakeServer = {
  users: User[]
  merchants: Merchant[]
  access: Record<string, string[]>
  /** Overrides: when set, they answer instead of the in-memory behavior. */
  list: ((search: URLSearchParams) => Reply) | null
  create: ((body: CreateInput) => Reply) | null
  rename: ((userId: string, body: { name: string }) => Reply) | null
  deactivate: ((userId: string) => Reply) | null
  activate: ((userId: string) => Reply) | null
  replaceAccess: ((userId: string, body: { merchantIds: string[] }) => Reply) | null
  calls: Call[]
}

const answer = async (reply: Reply) => {
  if (reply instanceof Error) throw reply
  return typeof reply === 'function' ? reply() : reply
}

function listUsers(server: FakeServer, search: URLSearchParams): Response {
  const status = search.get('status')
  const demo = search.get('demo') === 'true'
  const number = Number(search.get('page') ?? 0)
  const size = Number(search.get('size') ?? 20)
  const matching = server.users
    .filter(user => status === 'ALL' || (status === 'INACTIVE' ? !user.active : user.active))
    .filter(user => demo || user.demoExpiresAt === null)
    .sort((a, b) => a.name.localeCompare(b.name))
  return json(page(matching.slice(number * size, (number + 1) * size), number, Math.ceil(matching.length / size)))
}

function update(server: FakeServer, userId: string, change: Partial<User>, status = 200): Response {
  server.users = server.users.map(user => user.id === userId ? { ...user, ...change } : user)
  return status === 204 ? new Response(null, { status }) : json(server.users.find(user => user.id === userId), status)
}

async function route(server: FakeServer, method: string, path: string, search: URLSearchParams, body: unknown): Promise<Response> {
  if (path === '/api/me/merchants') return json(page(server.merchants, 0, 1))
  if (path === '/api/users' && method === 'GET') return answer(server.list?.(search) ?? listUsers(server, search))
  if (path === '/api/users' && method === 'POST') {
    const input = body as CreateInput
    if (server.create) return answer(server.create(input))
    const created = account(`u-${server.users.length + 100}`, input.name, { email: input.email, role: input.role })
    server.users = [...server.users, created]
    return json(created, 201)
  }
  const [, userId, rest] = path.match(/^\/api\/users\/([^/]+)(\/.*)?$/) ?? []
  if (userId && !rest && method === 'PUT') {
    const input = body as { name: string }
    return answer(server.rename?.(userId, input) ?? update(server, userId, { name: input.name }))
  }
  if (userId && !rest && method === 'DELETE') return answer(server.deactivate?.(userId) ?? update(server, userId, { active: false }, 204))
  if (userId && rest === '/activation' && method === 'PATCH') return answer(server.activate?.(userId) ?? update(server, userId, { active: true }))
  if (userId && rest === '/merchants' && method === 'GET') return json({ merchantIds: server.access[userId] ?? [] })
  if (userId && rest === '/merchants' && method === 'PUT') {
    const input = body as { merchantIds: string[] }
    if (server.replaceAccess) return answer(server.replaceAccess(userId, input))
    server.access[userId] = input.merchantIds
    return json({ merchantIds: input.merchantIds })
  }
  throw new Error(`rota sem resposta no teste: ${method} ${path}`)
}

/** Replaces `fetch` with the in-memory API; undo with `vi.unstubAllGlobals()`. */
export function installFakeApi(initial: Partial<FakeServer> = {}): FakeServer {
  const server: FakeServer = {
    users: [], merchants: [], access: {}, list: null, create: null, rename: null, deactivate: null, activate: null, replaceAccess: null, calls: [],
    ...initial,
  }
  vi.stubGlobal('fetch', vi.fn(async (input: string, init: RequestInit = {}) => {
    const url = new URL(input)
    const method = init.method ?? 'GET'
    const body = init.body ? JSON.parse(String(init.body)) : undefined
    server.calls.push({ method, path: url.pathname, search: url.searchParams, body })
    return route(server, method, url.pathname, url.searchParams, body)
  }))
  return server
}
