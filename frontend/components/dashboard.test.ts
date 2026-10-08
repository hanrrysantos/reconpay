import { expect, it } from 'vitest'
import { createElement } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { DashboardView, formatAmount } from './dashboard'

it('formata valores grandes sem arredondar os centavos', () => {
  expect(formatAmount('9007199254740993.27')).toBe('R$ 9.007.199.254.740.993,27')
  expect(formatAmount('0.1')).toBe('R$ 0,10')
})

it('informa quando a conta não tem empresas acessíveis', () => {
  const html = renderToStaticMarkup(createElement(DashboardView, {
    user: { name: 'Ana', email: 'ana@example.com', role: 'OPERATOR' },
    merchants: [],
    selectedMerchantId: null,
    onSelectMerchant: () => {},
    onLogout: () => {},
    transactions: [],
    isLoading: false,
    error: null,
    onRetry: () => {},
  }))
  expect(html).toContain('Nenhuma empresa disponível para sua conta.')
  expect(html).not.toContain('R$')
})

it('não mostra transações de outra empresa após trocar a seleção', () => {
  const html = renderToStaticMarkup(createElement(DashboardView, {
    user: { name: 'Ana', email: 'ana@example.com', role: 'OPERATOR' },
    merchants: [
      { id: 'a', name: 'Loja A', document: '1' },
      { id: 'b', name: 'Loja B', document: '2' },
    ],
    selectedMerchantId: 'b',
    onSelectMerchant: () => {},
    onLogout: () => {},
    transactions: [{ id: 'tx-a', merchantId: 'a', externalReference: 'Pedido da loja A', amount: '120.00', paymentMethod: 'PIX', status: 'APPROVED', transactionDate: '2026-10-08' }],
    isLoading: false,
    error: null,
    onRetry: () => {},
  }))
  expect(html).not.toContain('Pedido da loja A')
  expect(html).toContain('Nenhuma transação encontrada para esta empresa.')
  expect(html).toContain('aria-label="Sair"')
})
