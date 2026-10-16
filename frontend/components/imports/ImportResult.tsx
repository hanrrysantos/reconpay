import { CircleCheck, TriangleAlert } from 'lucide-react'
import type { ImportBatch } from '@/lib/api'
import { importedRowsMessage, type ImportFailure } from '@/lib/imports'

export type ImportOutcome = { batch: ImportBatch } | { failure: ImportFailure }

/** The last upload of a tab: the accepted batch, or every reason the API gave to refuse the file. */
export function ImportResult({ outcome }: { outcome: ImportOutcome }) {
  if ('batch' in outcome) {
    return (
      <p role="status" className="flex items-center gap-2 rounded-lg bg-primary-soft px-5 py-4 text-[14px] font-semibold">
        <CircleCheck aria-hidden="true" className="size-5" />
        {importedRowsMessage(outcome.batch)}
      </p>
    )
  }

  const { message, rowErrors, conflictingReferences } = outcome.failure
  return (
    <section role="alert" className="flex flex-col gap-3 rounded-lg bg-danger-soft px-5 py-4">
      <p className="flex items-center gap-2 text-[14px] font-semibold text-danger-strong">
        <TriangleAlert aria-hidden="true" className="size-5 shrink-0" />
        {message}
      </p>
      {rowErrors.length > 0 && (
        <div className="max-h-[320px] overflow-auto rounded-[14px] bg-white p-1.5">
          <table aria-label="Erros por linha" className="w-full border-separate border-spacing-y-1 text-left">
            <thead className="text-[12px] text-muted">
              <tr>
                <th scope="col" className="w-[80px] px-3 font-normal">Linha</th>
                <th scope="col" className="px-3 font-normal">Erro</th>
              </tr>
            </thead>
            <tbody>
              {rowErrors.map((error, index) => (
                <tr key={index}>
                  <td className="px-3 py-1.5 font-mono text-[13px]">{error.row}</td>
                  <td className="px-3 py-1.5 text-[13px]">{error.message}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {conflictingReferences.length > 0 && (
        <div className="flex flex-col gap-1.5">
          <p className="text-[13px] text-[#8A2A2E]">Referências já importadas</p>
          <ul aria-label="Referências já importadas" className="flex max-h-[200px] flex-wrap gap-1.5 overflow-auto">
            {conflictingReferences.map(reference => (
              <li key={reference} className="rounded-full bg-white px-2.5 py-0.5 font-mono text-[12px]">{reference}</li>
            ))}
          </ul>
        </div>
      )}
    </section>
  )
}
