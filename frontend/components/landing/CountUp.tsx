'use client'

import { useEffect, useRef } from 'react'
import { CHART_RANGE, coverProgress, formatCount, rangeProgress, type CountFormat } from '@/lib/scrollProgress'

const canAnimate = () =>
  typeof CSS !== 'undefined' && typeof CSS.supports === 'function' && CSS.supports('animation-timeline: view()') &&
  typeof matchMedia === 'function' && !matchMedia('(prefers-reduced-motion: reduce)').matches

/**
 * A number that grows from zero with the scroll position of its `[data-chart]` block, in step with the
 * CSS chart animations. The server HTML carries the final value, which is also all screen readers get.
 */
export function CountUp({ value, ...format }: { value: number } & CountFormat) {
  const shown = useRef<HTMLSpanElement>(null)
  const final = formatCount(value, format)

  useEffect(() => {
    const element = shown.current
    if (!element || !canAnimate()) return
    const chart = element.closest<HTMLElement>('[data-chart]') ?? element
    let frame = 0

    const update = () => {
      frame = 0
      const { top, height } = chart.getBoundingClientRect()
      const progress = rangeProgress(coverProgress(top, height, innerHeight), CHART_RANGE.start, CHART_RANGE.end)
      element.textContent = formatCount(value * progress, format)
    }
    const schedule = () => { if (!frame) frame = requestAnimationFrame(update) }

    update()
    addEventListener('scroll', schedule, { passive: true })
    addEventListener('resize', schedule)
    return () => {
      removeEventListener('scroll', schedule)
      removeEventListener('resize', schedule)
      cancelAnimationFrame(frame)
    }
    // format is a fresh object each render; its fields are what matter.
  }, [value, format.decimals, format.prefix, format.suffix])

  return (
    <>
      <span aria-hidden="true" ref={shown}>{final}</span>
      <span className="sr-only">{final}</span>
    </>
  )
}
