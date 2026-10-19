/** Fraction of the `cover` view-timeline range: 0 when the block's top meets the viewport bottom, 1 when its bottom leaves the top. */
export function coverProgress(top: number, height: number, viewport: number): number {
  return clamp((viewport - top) / (viewport + height))
}

/** Maps a cover fraction onto a sub-range, like CSS `animation-range: cover <start> cover <end>`. */
export function rangeProgress(cover: number, start: number, end: number): number {
  return clamp((cover - start) / (end - start))
}

/** Must match `animation-range: cover 5% cover 45%` on `.ring-fill` and `.bar-fill` in globals.css. */
export const CHART_RANGE = { start: 0.05, end: 0.45 } as const

export type CountFormat = { decimals?: number; prefix?: string; suffix?: string }

export function formatCount(value: number, { decimals = 0, prefix = '', suffix = '' }: CountFormat = {}): string {
  const number = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: decimals, maximumFractionDigits: decimals }).format(value)
  return `${prefix}${number}${suffix}`
}

const clamp = (value: number) => Math.min(1, Math.max(0, value))
