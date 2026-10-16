'use client'

import { MutationCache, QueryCache, QueryClient, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useSelectedMerchant } from '@/components/shell/MerchantMenu'
import {
  ApiError, createFeeRule, createMerchant, createTransaction, deactivateFeeRule, deactivateMerchant, exportRunCsv, getDiscrepancy, getFeeRules,
  getImportBatches, getLatestBankStatementImportTime, getLatestSettlementImport, getMerchantPage, importCsv, getOpenDiscrepancies, getPeriod,
  getPeriodSummary, getRuns, getTransactionPage, lockPeriod, renameMerchant, runReconciliation, unlockPeriod, updateDiscrepancy,
  updateFeeRuleFees, updateTransactionStatus, type DiscrepancyType, type DiscrepancyUpdate, type FeeRuleFees, type FeeRuleInput,
  type ImportKind, type Run, type TransactionFilters, type TransactionInput, type TransactionStatus,
} from './api'
import { useAuth } from './auth'
import type { Window } from './period'

export const RUN_POLL_MS = 3000
const RUNS_SCAN_SIZE = 100

const isUnauthorized = (error: unknown) => error instanceof ApiError && error.status === 401
const isClientError = (error: unknown) => error instanceof ApiError && error.status !== undefined && error.status < 500

/** One client per app: no retry on 4xx, no refetch on focus, and any 401 ends the session. */
export function createQueryClient(onUnauthorized: () => void): QueryClient {
  const onError = (error: unknown) => { if (isUnauthorized(error)) onUnauthorized() }
  return new QueryClient({
    queryCache: new QueryCache({ onError }),
    mutationCache: new MutationCache({ onError }),
    defaultOptions: {
      queries: {
        retry: (failureCount, error) => !isClientError(error) && failureCount < 1,
        refetchOnWindowFocus: false,
      },
      mutations: { retry: false },
    },
  })
}

export const queryKeys = {
  runs: (merchantId: string) => ['runs', merchantId] as const,
  latestRun: (merchantId: string, window: Window) => ['runs', merchantId, window.fromDate, window.toDate] as const,
  period: (merchantId: string, window: Window) => ['period', merchantId, window.fromDate, window.toDate] as const,
  summary: (merchantId: string, window: Window) => ['summary', merchantId, window.fromDate, window.toDate] as const,
  openDiscrepancies: (merchantId: string, runId?: string, type?: DiscrepancyType) =>
    (runId === undefined ? ['openDiscrepancies', merchantId] : ['openDiscrepancies', merchantId, runId, type ?? 'ALL']) as readonly string[],
  discrepancy: (merchantId: string, runId?: string, discrepancyId?: string) =>
    (runId === undefined ? ['discrepancy', merchantId] : ['discrepancy', merchantId, runId, discrepancyId ?? '']) as readonly string[],
  imports: (merchantId: string) => ['imports', merchantId] as const,
  merchantPage: (page?: number) => (page === undefined ? ['merchantPage'] : ['merchantPage', page]) as readonly unknown[],
  /** Prefix of the top menu list, `['merchants', userId]`, owned by `SelectedMerchantProvider`. */
  merchants: ['merchants'] as const,
  feeRules: (merchantId: string) => ['feeRules', merchantId] as const,
  /** Accepted imports listed on `/importacoes`; apart from `imports`, the dashboard's latest import times. */
  importBatches: (merchantId: string, kind: ImportKind, page?: number) =>
    (page === undefined ? ['importBatches', merchantId, kind] : ['importBatches', merchantId, kind, page]) as readonly unknown[],
  /** Without filters, the prefix of every transaction page of the merchant. */
  transactions: (merchantId: string, filters?: TransactionFilters, page?: number) =>
    (filters === undefined
      ? ['transactions', merchantId]
      : ['transactions', merchantId, filters.window.fromDate, filters.window.toDate, filters.status ?? 'ALL', filters.paymentMethod ?? 'ALL', page ?? 0]
    ) as readonly unknown[],
}

const inFlight = (run: Run | null | undefined) => run?.status === 'PENDING' || run?.status === 'RUNNING'

/**
 * Keeps the previous window's data while the next one loads (and while `/periods` answers 409 during a run),
 * but never across merchants: a different merchant starts empty.
 */
function keepSameMerchant<T>(merchantId: string) {
  return (previous: T | undefined, previousQuery?: { queryKey: readonly unknown[] }) =>
    previousQuery?.queryKey[1] === merchantId ? previous : undefined
}

export function useRuns(merchantId: string | null) {
  const { token } = useAuth()
  return useQuery({
    queryKey: queryKeys.runs(merchantId ?? ''),
    queryFn: () => getRuns(token!, merchantId!, { size: RUNS_SCAN_SIZE }),
    enabled: Boolean(token && merchantId),
  })
}

/** Latest run of the exact window, polled every 3 s while it is PENDING or RUNNING. */
export function useLatestRun(merchantId: string | null, window: Window) {
  const { token } = useAuth()
  return useQuery({
    queryKey: queryKeys.latestRun(merchantId ?? '', window),
    queryFn: async () => (await getRuns(token!, merchantId!, { size: 1, window })).content[0] ?? null,
    enabled: Boolean(token && merchantId),
    refetchInterval: query => inFlight(query.state.data) ? RUN_POLL_MS : false,
  })
}

export function usePeriod(merchantId: string | null, window: Window) {
  const { token } = useAuth()
  return useQuery({
    queryKey: queryKeys.period(merchantId ?? '', window),
    queryFn: () => getPeriod(token!, merchantId!, window),
    enabled: Boolean(token && merchantId),
    placeholderData: keepSameMerchant(merchantId ?? ''),
  })
}

export function usePeriodSummary(merchantId: string | null, window: Window) {
  const { token } = useAuth()
  return useQuery({
    queryKey: queryKeys.summary(merchantId ?? '', window),
    queryFn: () => getPeriodSummary(token!, merchantId!, window),
    enabled: Boolean(token && merchantId),
    placeholderData: keepSameMerchant(merchantId ?? ''),
  })
}

export function useOpenDiscrepancies(merchantId: string | null, runId: string | null, type?: DiscrepancyType) {
  const { token } = useAuth()
  return useQuery({
    queryKey: queryKeys.openDiscrepancies(merchantId ?? '', runId ?? '', type),
    queryFn: () => getOpenDiscrepancies(token!, merchantId!, runId!, type),
    enabled: Boolean(token && merchantId && runId),
  })
}

export function useDiscrepancy(merchantId: string | null, runId: string | null, discrepancyId: string | null) {
  const { token } = useAuth()
  return useQuery({
    queryKey: queryKeys.discrepancy(merchantId ?? '', runId ?? '', discrepancyId ?? ''),
    queryFn: () => getDiscrepancy(token!, merchantId!, runId!, discrepancyId!),
    enabled: Boolean(token && merchantId && runId && discrepancyId),
  })
}

export function useLatestImports(merchantId: string | null) {
  const { token } = useAuth()
  return useQuery({
    queryKey: queryKeys.imports(merchantId ?? ''),
    queryFn: async () => {
      const [settlement, bankStatementImportedAt] = await Promise.all([
        getLatestSettlementImport(token!, merchantId!),
        getLatestBankStatementImportTime(token!, merchantId!),
      ])
      return { settlementImportedAt: settlement?.createdAt ?? null, bankStatementImportedAt }
    },
    enabled: Boolean(token && merchantId),
  })
}

/** After a successful change, every block of the same merchant and window reloads from the API. */
function useInvalidateWindow(merchantId: string, window: Window) {
  const queryClient = useQueryClient()
  return () => Promise.all([
    queryClient.invalidateQueries({ queryKey: queryKeys.period(merchantId, window) }),
    queryClient.invalidateQueries({ queryKey: queryKeys.summary(merchantId, window) }),
    queryClient.invalidateQueries({ queryKey: queryKeys.openDiscrepancies(merchantId) }),
    queryClient.invalidateQueries({ queryKey: queryKeys.discrepancy(merchantId) }),
    queryClient.invalidateQueries({ queryKey: queryKeys.runs(merchantId) }),
  ])
}

export function useResolveDiscrepancy(merchantId: string, window: Window) {
  const { token } = useAuth()
  const invalidate = useInvalidateWindow(merchantId, window)
  return useMutation({
    mutationFn: ({ runId, discrepancyId, update }: { runId: string; discrepancyId: string; update: DiscrepancyUpdate }) =>
      updateDiscrepancy(token!, merchantId, runId, discrepancyId, update),
    onSuccess: invalidate,
  })
}

export function useRunReconciliation(merchantId: string, window: Window) {
  const { token } = useAuth()
  const invalidate = useInvalidateWindow(merchantId, window)
  return useMutation({ mutationFn: () => runReconciliation(token!, merchantId, window), onSuccess: invalidate })
}

export function useLockPeriod(merchantId: string, window: Window) {
  const { token } = useAuth()
  const invalidate = useInvalidateWindow(merchantId, window)
  return useMutation({ mutationFn: () => lockPeriod(token!, merchantId, window), onSuccess: invalidate })
}

export function useUnlockPeriod(merchantId: string, window: Window) {
  const { token } = useAuth()
  const invalidate = useInvalidateWindow(merchantId, window)
  return useMutation({ mutationFn: () => unlockPeriod(token!, merchantId, window), onSuccess: invalidate })
}

export function useExportRun(merchantId: string) {
  const { token } = useAuth()
  return useMutation({ mutationFn: (runId: string) => exportRunCsv(token!, merchantId, runId) })
}

export function useMerchantPage(page: number) {
  const { token } = useAuth()
  return useQuery({
    queryKey: queryKeys.merchantPage(page),
    queryFn: () => getMerchantPage(token!, page),
    enabled: Boolean(token),
  })
}

/** Resolves only after the page list and the top menu list have reloaded. */
export function useInvalidateMerchants() {
  const queryClient = useQueryClient()
  return () => Promise.all([
    queryClient.invalidateQueries({ queryKey: queryKeys.merchantPage() }),
    queryClient.invalidateQueries({ queryKey: queryKeys.merchants }),
  ])
}

/** Selects the new merchant once the menu already lists it; before that, the menu would fall back to the first one. */
export function useCreateMerchant() {
  const { token } = useAuth()
  const { select } = useSelectedMerchant()
  const invalidate = useInvalidateMerchants()
  return useMutation({
    mutationFn: (input: { name: string; document: string }) => createMerchant(token!, input),
    onSuccess: async merchant => {
      await invalidate()
      select(merchant.id)
    },
  })
}

export function useRenameMerchant() {
  const { token } = useAuth()
  const invalidate = useInvalidateMerchants()
  return useMutation({
    mutationFn: ({ merchantId, name }: { merchantId: string; name: string }) => renameMerchant(token!, merchantId, name),
    onSuccess: invalidate,
  })
}

/** When the deactivated merchant was selected, the menu falls back to the first remaining one, or to none. */
export function useDeactivateMerchant() {
  const { token } = useAuth()
  const invalidate = useInvalidateMerchants()
  return useMutation({
    mutationFn: (merchantId: string) => deactivateMerchant(token!, merchantId),
    onSuccess: invalidate,
  })
}

/** Active fee rules of one merchant; a different merchant starts empty, never with the previous one's rules. */
export function useFeeRules(merchantId: string | null) {
  const { token } = useAuth()
  return useQuery({
    queryKey: queryKeys.feeRules(merchantId ?? ''),
    queryFn: () => getFeeRules(token!, merchantId!),
    enabled: Boolean(token && merchantId),
  })
}

/** Each fee rule mutation resolves only after the merchant's rule list has reloaded. */
export function useInvalidateFeeRules(merchantId: string) {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: queryKeys.feeRules(merchantId) })
}

export function useCreateFeeRule(merchantId: string) {
  const { token } = useAuth()
  const invalidate = useInvalidateFeeRules(merchantId)
  return useMutation({ mutationFn: (input: FeeRuleInput) => createFeeRule(token!, merchantId, input), onSuccess: invalidate })
}

export function useUpdateFeeRuleFees(merchantId: string) {
  const { token } = useAuth()
  const invalidate = useInvalidateFeeRules(merchantId)
  return useMutation({
    mutationFn: ({ ruleId, fees }: { ruleId: string; fees: FeeRuleFees }) => updateFeeRuleFees(token!, merchantId, ruleId, fees),
    onSuccess: invalidate,
  })
}

export function useDeactivateFeeRule(merchantId: string) {
  const { token } = useAuth()
  const invalidate = useInvalidateFeeRules(merchantId)
  return useMutation({ mutationFn: (ruleId: string) => deactivateFeeRule(token!, merchantId, ruleId), onSuccess: invalidate })
}

/** One page of the accepted imports of a kind, newest first. */
export function useImportBatches(merchantId: string, kind: ImportKind, page: number) {
  const { token } = useAuth()
  return useQuery({
    queryKey: queryKeys.importBatches(merchantId, kind, page),
    queryFn: () => getImportBatches(token!, merchantId, kind, page),
    enabled: Boolean(token),
  })
}

/** Resolves only after the kind's batch list and the dashboard's latest imports have reloaded; a refused file reloads nothing. */
export function useImportCsv(merchantId: string, kind: ImportKind) {
  const { token } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => importCsv(token!, merchantId, kind, file),
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.importBatches(merchantId, kind) }),
      queryClient.invalidateQueries({ queryKey: queryKeys.imports(merchantId) }),
    ]),
  })
}

/** One page of the merchant's transactions for the filters, newest transaction date first. */
export function useTransactionPage(merchantId: string, filters: TransactionFilters, page: number) {
  const { token } = useAuth()
  return useQuery({
    queryKey: queryKeys.transactions(merchantId, filters, page),
    queryFn: () => getTransactionPage(token!, merchantId, filters, page),
    enabled: Boolean(token),
  })
}

/** Resolves only after every transaction page of the merchant has reloaded. */
export function useInvalidateTransactions(merchantId: string) {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: queryKeys.transactions(merchantId) })
}

export function useCreateTransaction(merchantId: string) {
  const { token } = useAuth()
  const invalidate = useInvalidateTransactions(merchantId)
  return useMutation({ mutationFn: (input: TransactionInput) => createTransaction(token!, merchantId, input), onSuccess: invalidate })
}

export function useUpdateTransactionStatus(merchantId: string) {
  const { token } = useAuth()
  const invalidate = useInvalidateTransactions(merchantId)
  return useMutation({
    mutationFn: ({ transactionId, status }: { transactionId: string; status: TransactionStatus }) =>
      updateTransactionStatus(token!, merchantId, transactionId, status),
    onSuccess: invalidate,
  })
}
