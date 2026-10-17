import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { Run } from '@/lib/api'
import { WindowNotice } from './WindowNotice'

const run = (fields: Partial<Run> = {}): Run => ({
  id: 'r-1', merchantId: 'm-1', fromDate: '2026-10-01', toDate: '2026-10-31', status: 'COMPLETED', totalItems: 4, matchedCount: 2,
  divergentCount: 2, createdAt: '2026-10-16T21:27:00Z', startedAt: null, finishedAt: null, errorMessage: null, supersededAt: null,
  processedItems: 4, ...fields,
})

describe('WindowNotice', () => {
  it('sem run mostra o aviso e o link para Conciliações (DIV-04 AC1)', () => {
    render(<WindowNotice run={null} locked={false} />)
    expect(screen.getByText('Nenhuma conciliação nesta janela.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ir para conciliações' })).toHaveAttribute('href', '/conciliacoes')
  })

  it.each(['PENDING', 'RUNNING'] as const)('run %s mostra "Conciliação em andamento." sem link (DIV-04 AC2)', status => {
    render(<WindowNotice run={run({ status })} locked={false} />)
    expect(screen.getByRole('status')).toHaveTextContent('Conciliação em andamento.')
    expect(screen.queryByRole('link')).toBeNull()
  })

  it('run com falha mostra o aviso, a mensagem do run e o link (DIV-04 AC3)', () => {
    render(<WindowNotice run={run({ status: 'FAILED', errorMessage: 'Falha ao processar o lote' })} locked={false} />)
    expect(screen.getByText('A conciliação desta janela falhou.')).toBeInTheDocument()
    expect(screen.getByText('Falha ao processar o lote')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ir para conciliações' })).toHaveAttribute('href', '/conciliacoes')
  })

  it('run concluído em período travado mostra o aviso de consulta (DIV-05)', () => {
    render(<WindowNotice run={run()} locked />)
    expect(screen.getByRole('status')).toHaveTextContent('Período travado: as divergências ficam só para consulta.')
  })

  it.each([
    ['FAILED', 'A conciliação desta janela falhou.'],
    ['RUNNING', 'Conciliação em andamento.'],
  ] as const)('run %s em período travado mostra o estado do run, não o aviso de travado (DIV-05)', (status, text) => {
    render(<WindowNotice run={run({ status, errorMessage: status === 'FAILED' ? 'Falha' : null })} locked />)
    expect(screen.getByText(text)).toBeInTheDocument()
    expect(screen.queryByText('Período travado: as divergências ficam só para consulta.')).toBeNull()
  })

  it('run concluído em período aberto não mostra aviso', () => {
    const { container } = render(<WindowNotice run={run()} locked={false} />)
    expect(container).toBeEmptyDOMElement()
  })
})
