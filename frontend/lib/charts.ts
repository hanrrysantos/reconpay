// Pure SVG geometry. Values arrive as decimal strings and become numbers only for pixel scale (AD-010).

const num = (value: number) => String(Number(value.toFixed(1)))

/** `stroke-dasharray` for the match ring: arc length proportional to a 0–1 rate. */
export function ringDash(rate: string, circumference: number): string {
  const clamped = Math.min(1, Math.max(0, Number(rate) || 0))
  return `${num(clamped * circumference)} ${num(circumference)}`
}

function points(values: string[], max: number, w: number, h: number): Array<[number, number]> {
  const step = values.length > 1 ? w / (values.length - 1) : 0
  return values.map((value, index) => [index * step, max > 0 ? h - (Number(value) / max) * h : h])
}

const toPath = (coords: Array<[number, number]>) => coords.map(([x, y], index) => `${index ? 'L' : 'M'} ${num(x)} ${num(y)}`).join(' ')

export function seriesPath(values: string[], max: number, w: number, h: number): string {
  return toPath(points(values, max, w, h))
}

/** Closed band between the expected line and the received line, only where received is below expected. */
export function gapPolygon(expected: string[], received: string[], max: number, w: number, h: number): string {
  const top = points(expected, max, w, h)
  const bottom = points(received, max, w, h).map(([x, y], index): [number, number] => [x, Math.max(y, top[index][1])])
  return `${toPath([...top, ...bottom.reverse()])} Z`
}

export type DonutSegment = { key: string; count: number; dasharray: string; dashoffset: string }

export function donutSegments(counts: Record<string, number>, r: number, gap: number): DonutSegment[] {
  const circumference = 2 * Math.PI * r
  const total = Object.values(counts).reduce((sum, count) => sum + count, 0)
  if (total === 0) return []
  let start = 0
  return Object.entries(counts).filter(([, count]) => count > 0).map(([key, count]) => {
    const share = (count / total) * circumference
    const length = Math.max(0, share - gap)
    const segment = { key, count, dasharray: `${num(length)} ${num(circumference - length)}`, dashoffset: num(-start) }
    start += share
    return segment
  })
}

export type HeatBand = 'full' | 'soft' | 'neutral' | 'warn' | 'bad' | 'empty'

/** Heatmap band for a day: ≥99% full, 97–98 soft, 94–96 neutral, 90–93 warn, <90 bad, no item empty. */
export function heatBand(matched: number, total: number): HeatBand {
  if (total === 0) return 'empty'
  const percent = matched * 100
  if (percent >= 99 * total) return 'full'
  if (percent >= 97 * total) return 'soft'
  if (percent >= 94 * total) return 'neutral'
  if (percent >= 90 * total) return 'warn'
  return 'bad'
}
