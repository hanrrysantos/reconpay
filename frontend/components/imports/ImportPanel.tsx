'use client'

import { Upload } from 'lucide-react'
import { useRef, useState, type FormEvent } from 'react'
import { FILE_TOO_LARGE_MESSAGE, importFailure, isTooLarge, type ImportKindInfo } from '@/lib/imports'
import { useImportCsv } from '@/lib/queries'
import { ImportBatches } from './ImportBatches'
import { ImportResult, type ImportOutcome } from './ImportResult'

/**
 * Upload, last outcome and accepted batches of one kind for one merchant. The page mounts it with a key per
 * merchant and kind, so switching either drops the chosen file, the outcome and the list page.
 */
export function ImportPanel({ merchantId, info }: { merchantId: string; info: ImportKindInfo }) {
  const [file, setFile] = useState<File | null>(null)
  const [outcome, setOutcome] = useState<ImportOutcome | null>(null)
  const [page, setPage] = useState(0)
  const input = useRef<HTMLInputElement>(null)
  const upload = useImportCsv(merchantId, info.kind)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (!file) return
    setOutcome(null)
    if (isTooLarge(file)) {
      setOutcome({ failure: { message: FILE_TOO_LARGE_MESSAGE, rowErrors: [], conflictingReferences: [] } })
      return
    }
    upload.mutate(file, {
      onSuccess: batch => {
        setOutcome({ batch })
        setFile(null)
        if (input.current) input.current.value = ''
        setPage(0)
      },
      // The file stays chosen, so it can be sent again once fixed.
      onError: error => setOutcome({ failure: importFailure(error) }),
    })
  }

  return (
    <div role="tabpanel" id={`import-panel-${info.slug}`} aria-labelledby={`import-tab-${info.slug}`} className="flex flex-col gap-4">
      <form onSubmit={submit} className="flex flex-col gap-4 rounded-lg bg-surface p-5">
        <div className="flex flex-col gap-1.5">
          <p className="text-[12px] text-muted">Colunas esperadas, nesta ordem</p>
          <code className="break-words font-mono text-[13px]">{info.columns.join(',')}</code>
        </div>
        <div className="flex flex-wrap items-end gap-3">
          <label className="flex min-w-0 flex-1 flex-col gap-1.5 text-[13px] font-semibold">
            Arquivo CSV
            <input
              ref={input}
              type="file"
              accept=".csv,text/csv"
              onChange={event => setFile(event.target.files?.[0] ?? null)}
              className="rounded-full bg-white px-3 py-2 text-[13px] font-normal shadow-[inset_0_0_0_1px_var(--color-border)] file:mr-3 file:rounded-full file:border-0 file:bg-surface file:px-3 file:py-1 file:font-semibold"
            />
          </label>
          <button
            type="submit"
            disabled={!file || upload.isPending}
            className="flex h-11 items-center gap-2 rounded-full bg-primary px-5 text-[14px] font-semibold text-foreground disabled:opacity-50"
          >
            <Upload aria-hidden="true" className="size-4" />
            {upload.isPending ? 'Importando…' : 'Importar'}
          </button>
        </div>
      </form>

      {outcome && <ImportResult outcome={outcome} />}

      <ImportBatches merchantId={merchantId} kind={info.kind} noun={info.noun} page={page} onPageChange={setPage} />
    </div>
  )
}
