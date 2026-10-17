import { ArrowRight, Lock } from 'lucide-react'
import Link from 'next/link'
import type { Run } from '@/lib/api'

const RECONCILIATIONS_HREF = '/conciliacoes'

function ToReconciliations() {
  return (
    <Link href={RECONCILIATIONS_HREF} className="flex w-fit items-center gap-1.5 text-[13px] font-semibold">
      Ir para conciliações
      <ArrowRight aria-hidden="true" className="size-4" />
    </Link>
  )
}

/**
 * Why the window shows no list (no run, run in flight, failed run), or that a completed run is read-only because
 * its period is locked. A completed run of an open period needs no notice.
 */
export function WindowNotice({ run, locked }: { run: Run | null; locked: boolean }) {
  if (run === null) {
    return (
      <section className="flex flex-col items-start gap-3 rounded-lg bg-surface p-7">
        <p className="text-[16px] font-semibold">Nenhuma conciliação nesta janela.</p>
        <ToReconciliations />
      </section>
    )
  }
  if (run.status === 'PENDING' || run.status === 'RUNNING') {
    return <p role="status" className="rounded-lg bg-surface p-7 text-[16px] font-semibold">Conciliação em andamento.</p>
  }
  if (run.status === 'FAILED') {
    return (
      <section className="flex flex-col items-start gap-3 rounded-lg bg-danger-soft p-7">
        <p className="text-[16px] font-semibold text-danger-strong">A conciliação desta janela falhou.</p>
        {run.errorMessage && <p className="text-[13px] text-[#8A2A2E]">{run.errorMessage}</p>}
        <ToReconciliations />
      </section>
    )
  }
  if (locked) {
    return (
      <p role="status" className="flex items-center gap-2 rounded-md bg-surface px-4 py-3 text-[13px]">
        <Lock aria-hidden="true" className="size-4 text-success" />
        Período travado: as divergências ficam só para consulta.
      </p>
    )
  }
  return null
}
