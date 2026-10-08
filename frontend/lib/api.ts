import { parse } from 'lossless-json'

const apiUrl = (process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080').replace(/\/$/, '')

export type Me = {
  id: string
  name: string
  email: string
  role: 'ADMIN' | 'OPERATOR'
  active: boolean
}

export type Merchant = {
  id: string
  name: string
  document: string
}

export type Transaction = {
  id: string
  merchantId: string
  externalReference: string
  amount: string
  expectedNetAmount: string
  paymentMethod: string
  installments: number
  status: string
  transactionDate: string
  createdAt: string
  updatedAt: string
}

type Page<T> = { content: T[]; totalPages: number }
type TransactionWire = Omit<Transaction, 'amount' | 'expectedNetAmount'> & {
  amount: string | number
  expectedNetAmount: string | number
}

export class ApiError extends Error {
  constructor(message: string, public status?: number) {
    super(message)
    this.name = 'ApiError'
  }
}

async function request<T>(path: string, init: RequestInit = {}, token?: string): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${apiUrl}${path}`, {
      ...init,
      cache: 'no-store',
      headers: {
        Accept: 'application/json',
        ...(init.body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    })
  } catch {
    throw new ApiError('Não foi possível conectar à API. Tente novamente.')
  }

  const body = await response.text()
  if (!response.ok) {
    let message = `A API retornou erro ${response.status}.`
    try {
      const error = JSON.parse(body) as { message?: string }
      if (error.message) message = error.message
    } catch { /* A API também pode retornar uma resposta sem JSON. */ }
    throw new ApiError(message, response.status)
  }

  if (!body) return undefined as T
  // Keep decimal values as strings: converting BigDecimal to JS number can silently round money.
  return parse(body, undefined, {
    parseNumber: value => /^-?\d+$/.test(value) && Number.isSafeInteger(Number(value)) ? Number(value) : value,
  }) as T
}

export async function authenticate(email: string, password: string): Promise<{ token: string; user: Me }> {
  const { token } = await request<{ token: string }>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
  const user = await request<Me>('/api/me', {}, token)
  return { token, user }
}

export async function register(input: { name: string; email: string; password: string }): Promise<void> {
  await request('/api/auth/register', { method: 'POST', body: JSON.stringify(input) })
}

export async function getMerchants(token: string): Promise<Merchant[]> {
  const merchants: Merchant[] = []
  let page = 0
  let totalPages: number
  do {
    const result = await request<Page<Merchant>>(`/api/me/merchants?page=${page}&size=100&sort=name%2Casc`, {}, token)
    merchants.push(...result.content)
    totalPages = result.totalPages
    page++
  } while (page < totalPages)
  return merchants
}

export async function getTransactions(token: string, merchantId: string): Promise<Transaction[]> {
  const result = await request<Page<TransactionWire>>(
    `/api/merchants/${encodeURIComponent(merchantId)}/transactions?size=5&sort=transactionDate%2Cdesc`,
    {},
    token,
  )
  return result.content.map(transaction => ({
    ...transaction,
    amount: String(transaction.amount),
    expectedNetAmount: String(transaction.expectedNetAmount),
  }))
}
