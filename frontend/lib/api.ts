import { LosslessNumber, parse, stringify } from 'lossless-json'
import type { Window } from './period'

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

export type Page<T> = { content: T[]; totalPages: number; totalElements?: number }
type TransactionWire = Omit<Transaction, 'amount' | 'expectedNetAmount'> & {
  amount: string | number
  expectedNetAmount: string | number
}

export type DiscrepancyType =
  | 'MISSING_SETTLEMENT' | 'ORPHAN_SETTLEMENT' | 'INCORRECT_AMOUNT' | 'FEE_DIVERGENCE' | 'STATUS_MISMATCH'
  | 'PAYMENT_METHOD_MISMATCH' | 'INSTALLMENTS_MISMATCH' | 'BANK_AMOUNT_MISMATCH' | 'MISSING_BANK_CREDIT'
  | 'ORPHAN_BANK_CREDIT' | 'AMBIGUOUS_BANK_MATCH'
export type DiscrepancyStatus = 'OPEN' | 'ACCEPTED' | 'ADJUSTED' | 'WRITTEN_OFF'
export type RunStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'

export type Run = {
  id: string
  merchantId: string
  fromDate: string
  toDate: string
  status: RunStatus
  totalItems: number | null
  matchedCount: number | null
  divergentCount: number | null
  createdAt: string
  startedAt: string | null
  finishedAt: string | null
  errorMessage: string | null
  supersededAt: string | null
  processedItems: number | null
}

export type Period = {
  runId: string
  fromDate: string
  toDate: string
  totalItems: number
  matchedCount: number
  divergentCount: number
  matchRate: string | null
  openAmount: string
  locked: boolean
  lockedAt: string | null
  closeDurationSeconds: number | null
}

export type PeriodSummaryDay = {
  date: string
  expectedNetAmount: string
  settledNetAmount: string
  bankReceivedAmount: string | null
  /** Expected net − bank received, from the server; negative when the bank received more, null without bank data. */
  bankShortfallAmount: string | null
  itemCount: number
  matchedCount: number
}

export type PeriodSummary = {
  runId: string
  fromDate: string
  toDate: string
  grossAmount: string
  expectedNetAmount: string
  feeAmount: string
  feeAboveContractAmount: string
  bankReceivedAmount: string | null
  openByType: Partial<Record<DiscrepancyType, number>>
  /** Discrepancies of the run already ACCEPTED, ADJUSTED or WRITTEN_OFF. */
  resolvedCount: number
  days: PeriodSummaryDay[]
  previousRunMatchRate: string | null
  previousWindowGrossAmount: string | null
  /** Fee ÷ gross in percentage points, two decimals; null when gross is zero. */
  feePercent: string | null
  /** Gross change over the previous window in percentage points, one decimal; null without a comparable base. */
  previousWindowGrossChangePercent: string | null
}

export type Discrepancy = {
  id: string
  status: DiscrepancyStatus
  type: DiscrepancyType
  expectedValue: string | null
  actualValue: string | null
}

export type ReconciliationItem = {
  id: string
  reconciliationRunId: string
  externalReference: string
  result: 'MATCHED' | 'DIVERGENT'
  internalTransactionId: string | null
  externalSettlementId: string | null
  transactionAmount: string | null
  expectedNetAmount: string | null
  settlementAmount: string | null
  settlementNetAmount: string | null
  /** settlementNetAmount − expectedNetAmount with a missing side as zero, from the server. */
  netDifference: string
  paymentMethod: string | null
  installments: number | null
  transactionStatus: string | null
  settlementStatus: string | null
  transactionDate: string | null
  settlementDate: string | null
  discrepancies: Discrepancy[]
  createdAt: string
}

export type DiscrepancyDetail = {
  id: string
  type: DiscrepancyType
  expectedValue: string | null
  actualValue: string | null
  status: DiscrepancyStatus
  adjustments: Array<{ amount: string; voided: boolean }>
  transitions: Array<{
    actorUserId: string
    fromStatus: DiscrepancyStatus
    toStatus: DiscrepancyStatus
    note: string | null
    createdAt: string
  }>
}

export type DiscrepancyUpdate = { status: DiscrepancyStatus; note?: string; correctionAmount?: string }

export type SettlementImport = { id: string; merchantId: string; fileName: string; totalRows: number; createdAt: string }

/** Fields the API serializes as BigDecimal; an integral value (e.g. `0`) is still kept as text. */
const DECIMAL_FIELDS = new Set([
  'amount', 'transactionAmount', 'expectedNetAmount', 'settlementAmount', 'settlementNetAmount', 'settledNetAmount',
  'matchRate', 'openAmount', 'grossAmount', 'feeAmount', 'feeAboveContractAmount', 'bankReceivedAmount',
  'previousRunMatchRate', 'previousWindowGrossAmount', 'bankShortfallAmount', 'feePercent',
  'previousWindowGrossChangePercent', 'netDifference',
])

export class ApiError extends Error {
  readonly #code: string | undefined

  /** `code` is the StandardError `error` field (ApiErrorCode), e.g. `CONFLICT`. */
  constructor(message: string, public status?: number, code?: string) {
    super(message)
    this.name = 'ApiError'
    this.#code = code
  }

  get code(): string | undefined {
    return this.#code
  }
}

async function send(path: string, init: RequestInit, token: string | undefined, accept: string): Promise<Response> {
  let response: Response
  try {
    response = await fetch(`${apiUrl}${path}`, {
      ...init,
      cache: 'no-store',
      headers: {
        Accept: accept,
        ...(init.body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    })
  } catch {
    throw new ApiError('Não foi possível conectar à API. Tente novamente.')
  }
  if (response.ok) return response

  const body = await response.text()
  let message = `A API retornou erro ${response.status}.`
  let code: string | undefined
  try {
    const error = JSON.parse(body) as { message?: string; error?: string }
    if (error.message) message = error.message
    if (error.error) code = error.error
  } catch { /* A API também pode retornar uma resposta sem JSON. */ }
  throw new ApiError(message, response.status, code)
}

async function request<T>(path: string, init: RequestInit = {}, token?: string): Promise<T> {
  const body = await (await send(path, init, token, 'application/json')).text()
  if (!body) return undefined as T
  // Keep decimal values as strings: converting BigDecimal to JS number can silently round money.
  return parse(
    body,
    (key, value) => DECIMAL_FIELDS.has(key) && typeof value === 'number' ? String(value) : value,
    { parseNumber: value => /^-?\d+$/.test(value) && Number.isSafeInteger(Number(value)) ? Number(value) : value },
  ) as T
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

const merchantPath = (merchantId: string) => `/api/merchants/${encodeURIComponent(merchantId)}`
const runPath = (merchantId: string, runId: string) => `${merchantPath(merchantId)}/reconciliations/${encodeURIComponent(runId)}`
const windowQuery = ({ fromDate, toDate }: Window) => `fromDate=${encodeURIComponent(fromDate)}&toDate=${encodeURIComponent(toDate)}`
const windowBody = ({ fromDate, toDate }: Window) => JSON.stringify({ fromDate, toDate })

export function getRuns(
  token: string,
  merchantId: string,
  { page = 0, size = 20, direction = 'desc' }: { page?: number; size?: number; direction?: 'asc' | 'desc' } = {},
): Promise<Page<Run>> {
  return request(`${merchantPath(merchantId)}/reconciliations?page=${page}&size=${size}&sort=createdAt%2C${direction}`, {}, token)
}

export function getRun(token: string, merchantId: string, runId: string): Promise<Run> {
  return request(runPath(merchantId, runId), {}, token)
}

export function runReconciliation(token: string, merchantId: string, window: Window): Promise<Run> {
  return request(`${merchantPath(merchantId)}/reconciliations`, { method: 'POST', body: windowBody(window) }, token)
}

export function getPeriod(token: string, merchantId: string, window: Window): Promise<Period> {
  return request(`${merchantPath(merchantId)}/periods?${windowQuery(window)}`, {}, token)
}

export function lockPeriod(token: string, merchantId: string, window: Window): Promise<Period> {
  return request(`${merchantPath(merchantId)}/periods/lock`, { method: 'POST', body: windowBody(window) }, token)
}

export function unlockPeriod(token: string, merchantId: string, window: Window): Promise<Period> {
  return request(`${merchantPath(merchantId)}/periods/unlock`, { method: 'POST', body: windowBody(window) }, token)
}

export function getPeriodSummary(token: string, merchantId: string, window: Window): Promise<PeriodSummary> {
  return request(`${merchantPath(merchantId)}/periods/summary?${windowQuery(window)}`, {}, token)
}

/** The four open discrepancies with the largest absolute difference, optionally of one type. */
export function getOpenDiscrepancies(token: string, merchantId: string, runId: string, type?: DiscrepancyType): Promise<Page<ReconciliationItem>> {
  const typeFilter = type ? `&discrepancyType=${type}` : ''
  return request(`${runPath(merchantId, runId)}/items?discrepancyStatus=OPEN${typeFilter}&sort=absDifference%2Cdesc&size=4`, {}, token)
}

export function getDiscrepancy(token: string, merchantId: string, runId: string, discrepancyId: string): Promise<DiscrepancyDetail> {
  return request(`${runPath(merchantId, runId)}/discrepancies/${encodeURIComponent(discrepancyId)}`, {}, token)
}

export function updateDiscrepancy(
  token: string,
  merchantId: string,
  runId: string,
  discrepancyId: string,
  { status, note, correctionAmount }: DiscrepancyUpdate,
): Promise<DiscrepancyDetail> {
  const body: Record<string, unknown> = { status }
  if (note?.trim()) body.note = note
  // Sent as a JSON number literal built from the text, so the value never passes through a float.
  if (correctionAmount !== undefined) body.correctionAmount = new LosslessNumber(correctionAmount)
  return request(
    `${runPath(merchantId, runId)}/discrepancies/${encodeURIComponent(discrepancyId)}`,
    { method: 'PATCH', body: stringify(body) },
    token,
  )
}

export async function getLatestSettlementImport(token: string, merchantId: string): Promise<SettlementImport | null> {
  const page = await request<Page<SettlementImport>>(`${merchantPath(merchantId)}/external-settlements/imports?size=1&sort=createdAt%2Cdesc`, {}, token)
  return page.content[0] ?? null
}

/** Time the latest bank statement import was created; null when the merchant has none. */
export async function getLatestBankStatementImportTime(token: string, merchantId: string): Promise<string | null> {
  const page = await request<Page<{ createdAt: string }>>(`${merchantPath(merchantId)}/bank-statements/imports?size=1&sort=createdAt%2Cdesc`, {}, token)
  return page.content[0]?.createdAt ?? null
}

export async function exportRunCsv(token: string, merchantId: string, runId: string): Promise<Blob> {
  return (await send(`${runPath(merchantId, runId)}/export`, {}, token, 'text/csv')).blob()
}
