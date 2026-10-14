import type { DiscrepancyDetail, Period, PeriodSummary, ReconciliationItem, Run } from '@/lib/api'

// Shared test data for the dashboard blocks.

export const september = { fromDate: '2026-09-01', toDate: '2026-09-30' }

export function makeRun(overrides: Partial<Run> = {}): Run {
  return {
    id: 'run-9', merchantId: 'm-1', ...september, status: 'COMPLETED', totalItems: 1284, matchedCount: 1272, divergentCount: 12,
    createdAt: '2026-10-01T09:42:00Z', startedAt: '2026-10-01T09:42:00Z', finishedAt: '2026-10-01T09:43:00Z',
    errorMessage: null, supersededAt: null, processedItems: 1284, ...overrides,
  }
}

export function makePeriod(overrides: Partial<Period> = {}): Period {
  return {
    runId: 'run-9', ...september, totalItems: 1284, matchedCount: 1209, divergentCount: 75, matchRate: '0.9420',
    openAmount: '10389.38', locked: false, lockedAt: null, closeDurationSeconds: null, ...overrides,
  }
}

export function makeSummary(overrides: Partial<PeriodSummary> = {}): PeriodSummary {
  return {
    runId: 'run-9', ...september, grossAmount: '125000.00', expectedNetAmount: '121212.50', feeAmount: '3787.50',
    feeAboveContractAmount: '35.50', bankReceivedAmount: '110823.12',
    openByType: { MISSING_SETTLEMENT: 7, FEE_DIVERGENCE: 5 }, days: [],
    previousRunMatchRate: null, previousWindowGrossAmount: null, feePercent: '3.03', previousWindowGrossChangePercent: null,
    ...overrides,
  }
}

export function makeItem(overrides: Partial<ReconciliationItem> = {}): ReconciliationItem {
  return {
    id: 'item-1', reconciliationRunId: 'run-9', externalReference: 'PED-10021', result: 'DIVERGENT',
    internalTransactionId: 'tx-1', externalSettlementId: 'st-1', transactionAmount: '5000.00', expectedNetAmount: '4870.00',
    settlementAmount: null, settlementNetAmount: null, netDifference: '-4870.00', paymentMethod: 'CREDIT_CARD', installments: 3,
    transactionStatus: 'APPROVED', settlementStatus: null, transactionDate: '2026-09-26', settlementDate: null,
    discrepancies: [{ id: 'd-1', status: 'OPEN', type: 'MISSING_SETTLEMENT', expectedValue: '4870.00', actualValue: null }],
    createdAt: '2026-10-01T09:43:00Z', ...overrides,
  }
}

export function makeDetail(overrides: Partial<DiscrepancyDetail> = {}): DiscrepancyDetail {
  return {
    id: 'd-1', type: 'MISSING_SETTLEMENT', expectedValue: '4870.00', actualValue: null, status: 'OPEN',
    adjustments: [], transitions: [], ...overrides,
  }
}
