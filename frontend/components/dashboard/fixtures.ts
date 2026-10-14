import type { Period, PeriodSummary, Run } from '@/lib/api'

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
    previousRunMatchRate: null, previousWindowGrossAmount: null, ...overrides,
  }
}
