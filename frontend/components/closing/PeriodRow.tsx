'use client'

import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { formatRate } from '@/components/dashboard/format'
import { ApiError } from '@/lib/api'
import { PERIOD_STATUS_LABELS, formatDuration, formatLockedAt, periodStatus } from '@/lib/closing'
import { formatMoney } from '@/lib/money'
import { formatWindow, type Window } from '@/lib/period'
import { queryKeys, useExportRun, useLockPeriod, usePeriod, useUnlockPeriod } from '@/lib/queries'

const action = 'h-8 rounded-full bg-surface px-3 text-[13px] font-semibold disabled:text-muted'
const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível concluir a ação.'

/**
 * One window of the closing list: reads its period and locks, unlocks or exports it.
 * Action errors go to `onError` (null clears it before each action); a 409 re-reads the period.
 */
export function PeriodRow({ merchantId, window, onError }: { merchantId: string; window: Window; onError: (message: string | null) => void }) {
  const queryClient = useQueryClient()
  const period = usePeriod(merchantId, window)
  const lock = useLockPeriod(merchantId, window)
  const unlock = useUnlockPeriod(merchantId, window)
  const exportRun = useExportRun(merchantId)
  const [confirming, setConfirming] = useState<'lock' | 'unlock' | null>(null)
  const name = formatWindow(window)
  const status = periodStatus(period)
  const data = status === 'locked' || status === 'open' ? period.data : undefined
  const busy = lock.isPending || unlock.isPending || exportRun.isPending

  async function changeLock(kind: 'lock' | 'unlock') {
    setConfirming(null)
    onError(null)
    try {
      const result = await (kind === 'lock' ? lock : unlock).mutateAsync()
      queryClient.setQueryData(queryKeys.period(merchantId, window), result)
    } catch (error) {
      onError(messageOf(error))
      if (error instanceof ApiError && error.status === 409) await period.refetch()
    }
  }

  async function exportCsv() {
    if (!data) return
    onError(null)
    try {
      const blob = await exportRun.mutateAsync(data.runId)
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `reconpay-${window.fromDate}-${window.toDate}.csv`
      anchor.click()
      URL.revokeObjectURL(url)
    } catch (error) {
      onError(messageOf(error))
    }
  }

  const label = status === 'error' ? 'Não carregou' : status === 'loading' ? 'Carregando' : PERIOD_STATUS_LABELS[status]
  return (
    <tr className="border-t border-border text-[14px]">
      <td className="px-4 py-3 font-semibold">{name}</td>
      <td className="px-4 py-3" aria-busy={status === 'loading' || undefined}>{label}</td>
      <td className="px-4 py-3 font-mono">{data ? formatRate(data.matchRate) : '—'}</td>
      <td className="px-4 py-3 font-mono">{data ? formatMoney(data.openAmount) : '—'}</td>
      <td className="px-4 py-3 font-mono">{data ? formatLockedAt(data.lockedAt) : '—'}</td>
      <td className="px-4 py-3 font-mono">{data ? formatDuration(data.closeDurationSeconds) : '—'}</td>
      <td className="px-4 py-3">
        {/*
          The confirmation takes the place of the buttons: the table scrolls sideways, so a
          floating panel below the last row would be clipped by that scroll area.
        */}
        {confirming ? (
          <InlineConfirm
            title={confirming === 'lock' ? 'Travar o período?' : 'Destravar o período?'}
            action={confirming === 'lock' ? 'Travar' : 'Destravar'}
            onCancel={() => setConfirming(null)}
            onConfirm={() => void changeLock(confirming)}
          />
        ) : (
          <div className="flex justify-end gap-2">
            {status === 'error' && (
              <button type="button" aria-label={`Tentar novamente ${name}`} onClick={() => void period.refetch()} className={action}>Tentar novamente</button>
            )}
            {status === 'open' && (
              <button type="button" aria-label={`Travar ${name}`} disabled={busy} onClick={() => setConfirming('lock')} className={action}>Travar</button>
            )}
            {status === 'locked' && (
              <button type="button" aria-label={`Destravar ${name}`} disabled={busy} onClick={() => setConfirming('unlock')} className={action}>Destravar</button>
            )}
            {data && (
              <button type="button" aria-label={`Exportar CSV de ${name}`} disabled={busy} onClick={() => void exportCsv()} className={action}>Exportar CSV</button>
            )}
          </div>
        )}
      </td>
    </tr>
  )
}

function InlineConfirm({ title, action, onConfirm, onCancel }: { title: string; action: string; onConfirm: () => void; onCancel: () => void }) {
  return (
    <div role="alertdialog" aria-label={title} className="flex items-center justify-end gap-2">
      <p className="text-[13px] font-semibold whitespace-nowrap">{title}</p>
      <button type="button" onClick={onCancel} className="h-8 rounded-full bg-surface px-3 text-[13px] font-semibold">Cancelar</button>
      <button type="button" onClick={onConfirm} className="h-8 rounded-full bg-foreground px-3 text-[13px] font-semibold text-white">{action}</button>
    </div>
  )
}
