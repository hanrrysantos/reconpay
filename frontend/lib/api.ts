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

/** A merchant as `/api/merchants` returns it; `document` keeps any punctuation stored before CNPJ normalization. */
export type MerchantRecord = Merchant & {
  active: boolean
  createdAt: string
}

export type PaymentMethod = 'CREDIT_CARD' | 'DEBIT_CARD' | 'PIX' | 'BOLETO'
export type TransactionStatus = 'APPROVED' | 'CANCELLED' | 'REFUNDED' | 'CHARGEBACK'

export type Transaction = {
  id: string
  merchantId: string
  externalReference: string
  amount: string
  expectedNetAmount: string
  paymentMethod: PaymentMethod
  installments: number
  status: TransactionStatus
  transactionDate: string
  createdAt: string
  updatedAt: string
}

export type Page<T> = { content: T[]; totalPages: number; totalElements?: number }

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
  paymentMethod: PaymentMethod | null
  installments: number | null
  transactionStatus: TransactionStatus | null
  settlementStatus: TransactionStatus | null
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

/** An active fee rule; `feePercentage` and `fixedFee` stay as the API text (BigDecimal). */
export type FeeRule = {
  id: string
  merchantId: string
  merchantName: string
  paymentMethod: PaymentMethod
  installments: number
  feePercentage: string
  fixedFee: string
  active: boolean
  createdAt: string
}

/** Decimal fields hold the exact typed text with a dot separator, e.g. `"2.99"`. */
export type FeeRuleFees = { feePercentage: string; fixedFee: string }
export type FeeRuleInput = FeeRuleFees & { paymentMethod: PaymentMethod; installments: number }

/** An accepted CSV import; transactions, settlements and bank statements share this shape. */
export type ImportBatch = { id: string; merchantId: string; fileName: string; totalRows: number; createdAt: string }
export type SettlementImport = ImportBatch
export type ImportKind = 'transactions' | 'settlements' | 'bankStatements'

/** Fields the API serializes as BigDecimal; an integral value (e.g. `0`) is still kept as text. */
const DECIMAL_FIELDS = new Set([
  'amount', 'transactionAmount', 'expectedNetAmount', 'settlementAmount', 'settlementNetAmount', 'settledNetAmount',
  'matchRate', 'openAmount', 'grossAmount', 'feeAmount', 'feeAboveContractAmount', 'bankReceivedAmount',
  'previousRunMatchRate', 'previousWindowGrossAmount', 'bankShortfallAmount', 'feePercent',
  'previousWindowGrossChangePercent', 'netDifference', 'feePercentage', 'fixedFee',
])

export class ApiError extends Error {
  readonly #code: string | undefined

  /** `code` is the StandardError `error` field (ApiErrorCode), e.g. `CONFLICT`; `details` its `details` object, when present. */
  constructor(message: string, public status?: number, code?: string, readonly details?: Record<string, unknown>) {
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
        // A FormData body gets its multipart Content-Type, with the boundary, from fetch itself.
        ...(typeof init.body === 'string' ? { 'Content-Type': 'application/json' } : {}),
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
  let details: Record<string, unknown> | undefined
  try {
    const error = JSON.parse(body) as { message?: string; error?: string; details?: unknown }
    if (error.message) message = error.message
    if (error.error) code = error.error
    if (typeof error.details === 'object' && error.details !== null && !Array.isArray(error.details)) {
      details = error.details as Record<string, unknown>
    }
  } catch { /* A API também pode retornar uma resposta sem JSON. */ }
  throw new ApiError(message, response.status, code, details)
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

export async function verifyEmail(token: string): Promise<void> {
  await request('/api/auth/verify-email', { method: 'POST', body: JSON.stringify({ token }) })
}

export async function resendVerification(email: string): Promise<void> {
  await request('/api/auth/resend-verification', { method: 'POST', body: JSON.stringify({ email: email.trim() }) })
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

export function getMerchantPage(token: string, page: number): Promise<Page<MerchantRecord>> {
  return request(`/api/merchants?page=${page}&size=20&sort=name%2Casc`, {}, token)
}

export function createMerchant(token: string, input: { name: string; document: string }): Promise<MerchantRecord> {
  return request('/api/merchants', { method: 'POST', body: JSON.stringify(input) }, token)
}

export function renameMerchant(token: string, merchantId: string, name: string): Promise<MerchantRecord> {
  return request(merchantPath(merchantId), { method: 'PUT', body: JSON.stringify({ name }) }, token)
}

export async function deactivateMerchant(token: string, merchantId: string): Promise<void> {
  await request(merchantPath(merchantId), { method: 'DELETE' }, token)
}

export async function getTransactions(token: string, merchantId: string): Promise<Transaction[]> {
  const result = await request<Page<Transaction>>(
    `/api/merchants/${encodeURIComponent(merchantId)}/transactions?size=5&sort=transactionDate%2Cdesc`,
    {},
    token,
  )
  return result.content
}

const feeRulesPath = (merchantId: string) => `${merchantPath(merchantId)}/fee-rules`
// Sent as JSON number literals built from the text, so the values never pass through a float.
const feesBody = ({ feePercentage, fixedFee }: FeeRuleFees) => ({ feePercentage: new LosslessNumber(feePercentage), fixedFee: new LosslessNumber(fixedFee) })

/** The merchant's active rules (one page of up to 100), by payment method and then installments. */
export async function getFeeRules(token: string, merchantId: string): Promise<FeeRule[]> {
  const result = await request<Page<FeeRule>>(
    `${feeRulesPath(merchantId)}?page=0&size=100&sort=paymentMethod%2Casc&sort=installments%2Casc`,
    {},
    token,
  )
  return result.content
}

export function createFeeRule(token: string, merchantId: string, { paymentMethod, installments, ...fees }: FeeRuleInput): Promise<FeeRule> {
  const body = stringify({ paymentMethod, installments, ...feesBody(fees) })
  return request(feeRulesPath(merchantId), { method: 'POST', body }, token)
}

/** Changes only the rates; payment method and installments are not sent. */
export function updateFeeRuleFees(token: string, merchantId: string, ruleId: string, fees: FeeRuleFees): Promise<FeeRule> {
  return request(`${feeRulesPath(merchantId)}/${encodeURIComponent(ruleId)}`, { method: 'PUT', body: stringify(feesBody(fees)) }, token)
}

export async function deactivateFeeRule(token: string, merchantId: string, ruleId: string): Promise<void> {
  await request(`${feeRulesPath(merchantId)}/${encodeURIComponent(ruleId)}`, { method: 'DELETE' }, token)
}

const merchantPath = (merchantId: string) => `/api/merchants/${encodeURIComponent(merchantId)}`
const runPath = (merchantId: string, runId: string) => `${merchantPath(merchantId)}/reconciliations/${encodeURIComponent(runId)}`
const windowQuery = ({ fromDate, toDate }: Window) => `fromDate=${encodeURIComponent(fromDate)}&toDate=${encodeURIComponent(toDate)}`
const windowBody = ({ fromDate, toDate }: Window) => JSON.stringify({ fromDate, toDate })

/** Runs newest first; with `window`, only the runs of that exact window. */
export function getRuns(
  token: string,
  merchantId: string,
  { page = 0, size = 20, direction = 'desc', window }: { page?: number; size?: number; direction?: 'asc' | 'desc'; window?: Window } = {},
): Promise<Page<Run>> {
  const windowFilter = window ? `&${windowQuery(window)}` : ''
  return request(`${merchantPath(merchantId)}/reconciliations?page=${page}&size=${size}&sort=createdAt%2C${direction}${windowFilter}`, {}, token)
}

export function getRun(token: string, merchantId: string, runId: string): Promise<Run> {
  return request(runPath(merchantId, runId), {}, token)
}

export type RunItemFilters = { result?: ReconciliationItem['result']; discrepancyType?: DiscrepancyType; discrepancyStatus?: DiscrepancyStatus }

/** Items of a run, 20 per page, largest absolute difference first; result, type and discrepancy status only when given. */
export function getRunItems(
  token: string,
  merchantId: string,
  runId: string,
  { result, discrepancyType, discrepancyStatus }: RunItemFilters,
  page: number,
): Promise<Page<ReconciliationItem>> {
  const resultFilter = result ? `&result=${result}` : ''
  const typeFilter = discrepancyType ? `&discrepancyType=${discrepancyType}` : ''
  // An item is listed when at least one of its discrepancies has this status.
  const statusFilter = discrepancyStatus ? `&discrepancyStatus=${discrepancyStatus}` : ''
  return request(`${runPath(merchantId, runId)}/items?page=${page}&size=20&sort=absDifference%2Cdesc${resultFilter}${typeFilter}${statusFilter}`, {}, token)
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

export type TransactionFilters = { window: Window; status?: TransactionStatus; paymentMethod?: PaymentMethod }

/** `amount` holds the exact typed text with a dot separator, e.g. `"1234.50"`; `transactionDate` is `yyyy-mm-dd`. */
export type TransactionInput = {
  externalReference: string
  amount: string
  paymentMethod: PaymentMethod
  installments: number
  transactionDate: string
}

const transactionsPath = (merchantId: string) => `${merchantPath(merchantId)}/transactions`

/** Transactions of the window, 20 per page, newest transaction date first; status and method only when given. */
export function getTransactionPage(token: string, merchantId: string, { window, status, paymentMethod }: TransactionFilters, page: number): Promise<Page<Transaction>> {
  const statusFilter = status ? `&status=${status}` : ''
  const methodFilter = paymentMethod ? `&paymentMethod=${paymentMethod}` : ''
  return request(
    `${transactionsPath(merchantId)}?${windowQuery(window)}${statusFilter}${methodFilter}&page=${page}&size=20&sort=transactionDate%2Cdesc`,
    {},
    token,
  )
}

export function createTransaction(
  token: string,
  merchantId: string,
  { externalReference, amount, paymentMethod, installments, transactionDate }: TransactionInput,
): Promise<Transaction> {
  // Sent as a JSON number literal built from the text, so the value never passes through a float.
  const body = stringify({ externalReference, amount: new LosslessNumber(amount), paymentMethod, installments, transactionDate })
  return request(transactionsPath(merchantId), { method: 'POST', body }, token)
}

export function updateTransactionStatus(token: string, merchantId: string, transactionId: string, status: TransactionStatus): Promise<Transaction> {
  return request(`${transactionsPath(merchantId)}/${encodeURIComponent(transactionId)}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }, token)
}

const IMPORT_PATHS: Record<ImportKind, string> = {
  transactions: 'transactions',
  settlements: 'external-settlements',
  bankStatements: 'bank-statements',
}
const importPath = (merchantId: string, kind: ImportKind) => `${merchantPath(merchantId)}/${IMPORT_PATHS[kind]}`

/** All or nothing: the API accepts every row or refuses the file with every error in `details`. Settlements use the default layout. */
export function importCsv(token: string, merchantId: string, kind: ImportKind, file: File): Promise<ImportBatch> {
  const body = new FormData()
  body.append('file', file)
  return request(`${importPath(merchantId, kind)}/import`, { method: 'POST', body }, token)
}

/** Accepted imports of one kind, 20 per page, newest first. */
export function getImportBatches(token: string, merchantId: string, kind: ImportKind, page: number): Promise<Page<ImportBatch>> {
  return request(`${importPath(merchantId, kind)}/imports?page=${page}&size=20&sort=createdAt%2Cdesc`, {}, token)
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
