import { render, screen } from '@testing-library/react'
import type { ComponentType } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ConciliacoesPage from '@/app/(app)/conciliacoes/page'
import DivergenciasPage from '@/app/(app)/divergencias/page'
import FechamentoPage from '@/app/(app)/fechamento/page'
import ImportacoesPage from '@/app/(app)/importacoes/page'
import TransacoesPage from '@/app/(app)/transacoes/page'

const fetcher = vi.fn()
beforeEach(() => { fetcher.mockReset(); vi.stubGlobal('fetch', fetcher) })
afterEach(() => vi.unstubAllGlobals())

const ROUTES: Array<[string, string, ComponentType]> = [
  ['/conciliacoes', 'Conciliações', ConciliacoesPage],
  ['/divergencias', 'Divergências', DivergenciasPage],
  ['/transacoes', 'Transações', TransacoesPage],
  ['/importacoes', 'Importações', ImportacoesPage],
  ['/fechamento', 'Fechamento', FechamentoPage],
]

describe('rotas vazias', () => {
  it.each(ROUTES)('%s mostra "%s" em construção sem disparar request', async (_route, title, Page) => {
    render(<Page />)

    expect(screen.getByRole('heading', { level: 1, name: title })).toBeInTheDocument()
    expect(screen.getByText(/em construção/)).toBeInTheDocument()
    await new Promise(resolve => setTimeout(resolve, 0))
    expect(fetcher).not.toHaveBeenCalled()
  })
})
