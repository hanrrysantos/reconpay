'use client'

import { ArrowLeft, Download } from 'lucide-react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useEffect, useState } from 'react'
import { BlockError, NoMerchant } from '@/components/dashboard/states'
import { useSelectedMerchant } from '@/components/shell/MerchantMenu'
import { ApiError, type Run } from '@/lib/api'
import { formatImportedAt } from '@/lib/imports'
import { formatWindow } from '@/lib/period'
import { useExportRun, useRun } from '@/lib/queries'
import { RUN_STATUS_LABELS, anyInFlight, itemsProgress } from '@/lib/reconciliations'
import { RunItems } from './RunItems'

const LIST_HREF = '/conciliacoes'
const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível carregar a conciliação.'
const count = (value: number | null) => value === null ? '—' : String(value)
const isNotFound = (error: unknown) => error instanceof ApiError && error.status === 404

function BackLink() {
  return (
    <Link href={LIST_HREF} className="flex w-fit items-center gap-1.5 text-[13px] font-semibold text-muted">
      <ArrowLeft aria-hidden="true" className="size-4" />
      Voltar para conciliações
    </Link>
  )
}

function ExportButton({ merchantId, runId }: { merchantId: string; runId: string }) {
  const exportRun = useExportRun(merchantId)
  const [error, setError] = useState<string | null>(null)

  async function download() {
    setError(null)
    try {
      const blob = await exportRun.mutateAsync(runId)
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `reconciliation-${runId}.csv`
      anchor.click()
      URL.revokeObjectURL(url)
    } catch (caught) {
      setError(messageOf(caught))
    }
  }

  return (
    <div className="flex flex-col items-end gap-2">
      <button type="button" onClick={() => void download()} disabled={exportRun.isPending} className="flex h-11 items-center gap-2 rounded-full bg-primary px-5 text-[14px] font-semibold text-foreground disabled:opacity-60">
        <Download aria-hidden="true" className="size-4" />
        {exportRun.isPending ? 'Exportando…' : 'Exportar CSV'}
      </button>
      {error && <p role="alert" className="text-[13px] text-muted">{error}</p>}
    </div>
  )
}

function RunHeader({ run, merchantName }: { run: Run; merchantName?: string }) {
  const totals = [
    { label: 'Status', value: RUN_STATUS_LABELS[run.status] },
    { label: 'Criado em', value: formatImportedAt(run.createdAt) },
    { label: 'Itens', value: itemsProgress(run) },
    { label: 'Conciliados', value: count(run.matchedCount) },
    { label: 'Divergentes', value: count(run.divergentCount) },
  ]
  return (
    <section aria-label="Conciliação" className="flex flex-col gap-4">
      <BackLink />
      <div className="flex flex-wrap items-end justify-between gap-6">
        <div className="flex min-w-0 flex-col gap-2">
          <p className="truncate text-[14px] text-muted">{merchantName}</p>
          <h1 className="flex flex-wrap items-center gap-3 font-display text-[44px] leading-none font-extrabold tracking-[-0.04em]">
            {formatWindow(run)}
            {run.supersededAt !== null && (
              <span className="rounded-full bg-surface px-3 py-1 font-sans text-[12px] font-semibold tracking-normal text-muted">Substituído</span>
            )}
          </h1>
        </div>
        {run.status === 'COMPLETED' && <ExportButton merchantId={run.merchantId} runId={run.id} />}
      </div>
      <dl className="flex flex-wrap gap-x-8 gap-y-3">
        {totals.map(({ label, value }) => (
          <div key={label} className="flex flex-col gap-1">
            <dt className="text-[12px] text-muted">{label}</dt>
            <dd className="font-mono text-[14px] font-semibold">{value}</dd>
          </div>
        ))}
      </dl>
      {run.status === 'FAILED' && run.errorMessage && <p className="text-[13px] text-muted">{run.errorMessage}</p>}
    </section>
  )
}

function RunDetail({ merchantId, merchantName, runId }: { merchantId: string; merchantName?: string; runId: string }) {
  const runQuery = useRun(merchantId, runId)
  const run = runQuery.data
  const polling = run !== undefined && anyInFlight([run])

  if (runQuery.isPending) {
    return <div role="status" aria-label="Carregando conciliação" aria-busy="true" className="h-40 animate-pulse rounded-lg bg-surface" />
  }
  if (runQuery.isError) {
    if (isNotFound(runQuery.error)) {
      return (
        <section className="flex flex-col items-start gap-4 rounded-lg bg-surface p-7">
          <p className="text-[16px] font-semibold">Conciliação não encontrada.</p>
          <BackLink />
        </section>
      )
    }
    return <BlockError message={messageOf(runQuery.error)} action="Tentar novamente" onRetry={() => void runQuery.refetch()} />
  }

  return (
    <>
      <RunHeader run={runQuery.data} merchantName={merchantName} />
      <RunItems merchantId={merchantId} runId={runId} polling={polling} />
    </>
  )
}

/**
 * `/conciliacoes/[runId]`: one run of the merchant selected in the top menu, its items and the CSV export.
 * The run belongs to the merchant loaded first; switching merchants goes back to the list.
 */
export function RunDetailPage({ runId }: { runId: string }) {
  const router = useRouter()
  const { merchant, merchantId, isLoading: merchantsLoading, error: merchantsError } = useSelectedMerchant()
  const [loadedMerchantId, setLoadedMerchantId] = useState<string | null>(merchantId)
  if (loadedMerchantId === null && merchantId !== null) setLoadedMerchantId(merchantId)
  const switched = loadedMerchantId !== null && merchantId !== loadedMerchantId
  useEffect(() => {
    if (switched) router.replace(LIST_HREF)
  }, [switched, router])
  const noMerchant = !merchantsLoading && !merchantsError && merchantId === null

  if (noMerchant && loadedMerchantId === null) return <NoMerchant />
  if (switched || merchantId === null) return null
  return <RunDetail key={runId} merchantId={merchantId} merchantName={merchant?.name} runId={runId} />
}
