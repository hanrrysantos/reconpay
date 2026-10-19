import { act, render } from '@testing-library/react'
import { renderToString } from 'react-dom/server'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { CountUp } from './CountUp'

const VIEWPORT = 800
let chartTop = VIEWPORT
let frames: FrameRequestCallback[] = []
const flushFrames = () => { const pending = frames; frames = []; pending.forEach(callback => callback(0)) }

function stubBrowser({ supports = true, reducedMotion = false } = {}) {
  vi.stubGlobal('CSS', { supports: (query: string) => supports && query === 'animation-timeline: view()' })
  vi.stubGlobal('matchMedia', (query: string) => ({ matches: reducedMotion && query === '(prefers-reduced-motion: reduce)' }))
  vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => frames.push(callback))
  vi.stubGlobal('cancelAnimationFrame', () => {})
  vi.stubGlobal('innerHeight', VIEWPORT)
  vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (this: HTMLElement) {
    return (this.hasAttribute('data-chart') ? { top: chartTop, height: 300 } : { top: 0, height: 0 }) as DOMRect
  })
}

function renderInChart(ui: React.ReactElement) {
  return render(<div data-chart>{ui}</div>)
}

const shown = (container: HTMLElement) => container.querySelector('[aria-hidden="true"]')!.textContent
const scrollTo = (top: number) => act(() => { chartTop = top; window.dispatchEvent(new Event('scroll')); flushFrames() })

// cover = (800 - top) / 1100; the chart range is cover 5%–45%.
const START = 800 - 0.05 * 1100
const MIDDLE = 800 - 0.25 * 1100
const END = 800 - 0.45 * 1100

beforeEach(() => {
  chartTop = VIEWPORT
  frames = []
})
afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('CountUp (LAND-04 AC6, AC7)', () => {
  it('sem suporte a animação ligada ao scroll mostra o valor final', () => {
    stubBrowser({ supports: false })
    const { container } = renderInChart(<CountUp value={94.2} decimals={1} suffix="%" />)

    expect(shown(container)).toBe('94,2%')
    scrollTo(MIDDLE)
    expect(shown(container)).toBe('94,2%')
  })

  it('com movimento reduzido mostra o valor final', () => {
    stubBrowser({ reducedMotion: true })
    const { container } = renderInChart(<CountUp value={94.2} decimals={1} suffix="%" />)

    expect(shown(container)).toBe('94,2%')
    scrollTo(MIDDLE)
    expect(shown(container)).toBe('94,2%')
  })

  it('cresce de zero ao valor final descendo e volta subindo, no formato brasileiro', () => {
    stubBrowser()
    chartTop = START
    const { container } = renderInChart(<CountUp value={121212.5} decimals={2} prefix="R$ " />)

    expect(shown(container)).toBe('R$ 0,00')
    scrollTo(MIDDLE)
    expect(shown(container)).toBe('R$ 60.606,25')
    scrollTo(END)
    expect(shown(container)).toBe('R$ 121.212,50')
    scrollTo(MIDDLE)
    expect(shown(container)).toBe('R$ 60.606,25')
    scrollTo(VIEWPORT + 100)
    expect(shown(container)).toBe('R$ 0,00')
  })

  it('acompanha o redimensionamento da tela', () => {
    stubBrowser()
    chartTop = MIDDLE
    const { container } = renderInChart(<CountUp value={964} />)
    expect(shown(container)).toBe('482')

    act(() => { chartTop = END; window.dispatchEvent(new Event('resize')); flushFrames() })
    expect(shown(container)).toBe('964')
  })

  it('leitores de tela ouvem só o valor final, mesmo no meio da animação', () => {
    stubBrowser()
    chartTop = MIDDLE
    const { container } = renderInChart(<CountUp value={1209} />)

    expect(shown(container)).toBe('605')
    expect(container.querySelector('.sr-only')).toHaveTextContent(/^1\.209$/)
  })

  it('sem JavaScript (HTML do servidor) já vem com o valor final', () => {
    stubBrowser()
    const html = renderToString(<CountUp value={10389.38} decimals={2} prefix="R$ " />)

    expect(html).toContain('<span aria-hidden="true">R$ 10.389,38</span>')
    expect(html).toContain('<span class="sr-only">R$ 10.389,38</span>')
  })
})
