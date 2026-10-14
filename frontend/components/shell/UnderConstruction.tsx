import { Construction } from 'lucide-react'

/** Placeholder for a page whose feature is not built yet; it requests no data. */
export function UnderConstruction({ title }: { title: string }) {
  return (
    <section className="flex flex-col items-start gap-4 rounded-lg bg-surface p-7">
      <span className="grid size-10 place-items-center rounded-full bg-white">
        <Construction aria-hidden="true" className="size-5 text-muted" />
      </span>
      <h1 className="font-display text-[44px] leading-none font-extrabold tracking-[-0.04em]">{title}</h1>
      <p className="text-[15px] text-muted">Esta página está em construção.</p>
    </section>
  )
}
