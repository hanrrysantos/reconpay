import { cn } from '@/lib/utils'

// With the tagline the mark spans the name and the two tagline lines, as on the brand board.
const SIZES = {
  md: { mark: 'h-[30px]', markWithTagline: 'h-[50px]', name: 'text-[21px]' },
  sm: { mark: 'h-[26px]', markWithTagline: 'h-[42px]', name: 'text-[18px]' },
}

/**
 * ReconPay brand: the R mark and the Manrope name. `tone` is the background it sits on; "Pay" is lime
 * only on dark backgrounds, where it stays legible at small sizes.
 */
export function Logo({ tone, tagline = false, size = 'md' }: { tone: 'dark' | 'light'; tagline?: boolean; size?: keyof typeof SIZES }) {
  const dark = tone === 'dark'
  return (
    <span className="flex items-center gap-2.5">
      <img src={`/brand/reconpay-mark-on-${tone}.svg`} alt="" className={cn('w-auto shrink-0', tagline ? SIZES[size].markWithTagline : SIZES[size].mark)} />
      <span className="flex flex-col gap-1.5">
        <span className={cn('font-brand leading-none font-extrabold tracking-[-0.03em]', SIZES[size].name, dark ? 'text-background' : 'text-foreground')}>
          Recon<span className={cn(dark && 'text-primary')}>Pay</span>
        </span>
        {tagline && (
          <span className={cn('text-[10px] leading-snug font-medium tracking-[0.2em] uppercase', dark ? 'text-background/55' : 'text-muted')}>
            Conciliação financeira<br /> com clareza
          </span>
        )}
      </span>
    </span>
  )
}
