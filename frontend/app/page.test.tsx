import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import Home, { metadata } from './page'

afterEach(() => vi.unstubAllGlobals())

describe('página inicial', () => {
  it('mostra a landing sem chamar a API nem pedir sessão (LAND-01 AC1)', () => {
    const fetch = vi.fn()
    vi.stubGlobal('fetch', fetch)

    render(<Home />)

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('O que você vendeu, o que caiu na conta e onde não bate.')
    expect(screen.queryByText('Abrindo login...')).not.toBeInTheDocument()
    expect(fetch).not.toHaveBeenCalled()
  })

  it('tem o título e a descrição da página (LAND-05 AC2)', () => {
    expect(metadata.title).toBe('ReconPay · Conciliação financeira')
    expect(metadata.description).toBe('O ReconPay cruza suas vendas com a liquidação da adquirente e o extrato do banco, aponta cada divergência e fecha o período com rastro para auditoria.')
  })
})
