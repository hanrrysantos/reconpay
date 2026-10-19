import { Check } from 'lucide-react'

export const authField = 'h-12 w-full rounded-full bg-surface px-[18px] text-[15px] outline-none placeholder:text-muted focus:shadow-[inset_0_0_0_2px_var(--color-foreground)]'

export const authPrimaryButton = 'flex h-[52px] items-center justify-center gap-2 rounded-full bg-primary text-[15px] font-bold text-foreground disabled:opacity-60'

export const authAlert = 'flex items-center gap-2.5 rounded-[14px] bg-danger-soft px-4 py-3 text-[14px] font-semibold text-danger-strong'

export function AuthPanel({ title, lead }: { title: string; lead: string }) {
  return (
    <aside className="flex flex-col gap-7 bg-foreground px-6 pt-7 pb-8 text-background lg:w-[440px] lg:shrink-0 lg:justify-between lg:p-12 xl:w-[640px] xl:p-14">
      <span className="flex items-center gap-2.5 font-display text-[20px] font-extrabold tracking-[-0.04em]">
        <span className="grid size-[30px] place-items-center rounded-sm bg-primary text-foreground">
          <Check aria-hidden="true" className="size-4" strokeWidth={3} />
        </span>
        reconpay
      </span>
      <div className="flex flex-col gap-[18px]">
        <h2 className="max-w-[500px] font-display text-[30px] leading-[1.05] font-extrabold tracking-[-0.04em] lg:text-[40px] lg:leading-[1.02] xl:text-[52px]">
          {title}
        </h2>
        <p className="hidden max-w-[460px] text-[16px] leading-[1.55] text-background/65 lg:block">{lead}</p>
      </div>
      <div className="hidden items-center gap-2 lg:flex">
        <span aria-hidden="true" className="size-1.5 rounded-full bg-primary" />
        <span className="text-[13px] text-background/45">Conciliação financeira para fintechs, gateways e marketplaces</span>
      </div>
    </aside>
  )
}
