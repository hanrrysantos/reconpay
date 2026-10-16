import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Merchant } from '@/lib/api'
import { createQueryClient } from '@/lib/queries'
import { MerchantMenu, MerchantSwitcher, SelectedMerchantProvider, useSelectedMerchant } from './MerchantMenu'

vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1', name: 'Ana' } }) }))

afterEach(() => vi.unstubAllGlobals())

const alfa: Merchant = { id: 'm-1', name: 'Alfa Pagamentos', document: '12345678000190' }
const beta: Merchant = { id: 'm-2', name: 'Beta Comércio', document: '98765432000155' }
const gama: Merchant = { id: 'm-3', name: 'Gama Serviços', document: '11222333000144' }

describe('MerchantMenu', () => {
  function open(selectedId = 'm-1', onSelect = vi.fn()) {
    render(<MerchantMenu merchants={[alfa, beta, gama]} selectedId={selectedId} onSelect={onSelect} />)
    return { user: userEvent.setup(), onSelect }
  }

  it('lista cada empresa com nome e CNPJ e marca a selecionada', async () => {
    const { user } = open('m-2')
    await user.click(screen.getByRole('button', { name: /Beta Comércio/ }))

    const options = within(screen.getByRole('listbox')).getAllByRole('option')
    expect(options.map(option => option.textContent)).toEqual([
      expect.stringContaining('Alfa Pagamentos'), expect.stringContaining('Beta Comércio'), expect.stringContaining('Gama Serviços'),
    ])
    expect(options[0]).toHaveTextContent('12.345.678/0001-90')
    expect(options.map(option => option.getAttribute('aria-selected'))).toEqual(['false', 'true', 'false'])
  })

  it('filtra pelo nome sem diferenciar maiúsculas e acentos', async () => {
    const { user } = open()
    await user.click(screen.getByRole('button', { name: /Alfa Pagamentos/ }))
    await user.type(screen.getByPlaceholderText('Buscar empresa ou CNPJ'), 'comercio')

    const options = within(screen.getByRole('listbox')).getAllByRole('option')
    expect(options).toHaveLength(1)
    expect(options[0]).toHaveTextContent('Beta Comércio')
  })

  it('filtra pelos dígitos do CNPJ, com ou sem pontuação', async () => {
    const { user } = open()
    await user.click(screen.getByRole('button', { name: /Alfa Pagamentos/ }))
    const search = screen.getByPlaceholderText('Buscar empresa ou CNPJ')

    await user.type(search, '11.222')
    expect(within(screen.getByRole('listbox')).getAllByRole('option').map(o => o.textContent))
      .toEqual([expect.stringContaining('Gama Serviços')])

    await user.clear(search)
    await user.type(search, '98765432')
    expect(within(screen.getByRole('listbox')).getAllByRole('option').map(o => o.textContent))
      .toEqual([expect.stringContaining('Beta Comércio')])
  })

  it('selecionar outra empresa chama onSelect com o id e fecha o menu', async () => {
    const { user, onSelect } = open()
    await user.click(screen.getByRole('button', { name: /Alfa Pagamentos/ }))
    await user.click(screen.getByRole('option', { name: /Gama Serviços/ }))

    expect(onSelect).toHaveBeenCalledWith('m-3')
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
  })

  it('tem no rodapé "Gerenciar empresas" apontando para /empresas e fecha o menu ao clicar (MLIST-09)', async () => {
    const { user } = open()
    await user.click(screen.getByRole('button', { name: /Alfa Pagamentos/ }))
    const manage = screen.getByRole('link', { name: 'Gerenciar empresas' })

    expect(manage).toHaveAttribute('href', '/empresas')
    await user.click(manage)
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
  })

  it('tem no rodapé "Cadastrar empresa" apontando para /empresas com a gaveta aberta (MCAD-11)', async () => {
    const { user } = open()
    await user.click(screen.getByRole('button', { name: /Alfa Pagamentos/ }))

    expect(screen.getByRole('link', { name: 'Cadastrar empresa' })).toHaveAttribute('href', '/empresas?cadastrar=1')
  })

  it('com empresa selecionada, tem no rodapé "Regras de taxa da <empresa>" entre os dois itens, para /taxas, e fecha o menu (FLIST-01)', async () => {
    const { user } = open('m-2')
    await user.click(screen.getByRole('button', { name: /Beta Comércio/ }))
    const rules = screen.getByRole('link', { name: 'Regras de taxa da Beta Comércio' })

    expect(rules).toHaveAttribute('href', '/taxas')
    const footer = within(rules.parentElement!).getAllByRole('link').map(link => link.textContent)
    expect(footer).toEqual(['Gerenciar empresas', 'Regras de taxa da Beta Comércio', 'Cadastrar empresa'])
    await user.click(rules)
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
  })

  it('sem empresa selecionada, não mostra o item de regras de taxa (FLIST-01)', async () => {
    render(<MerchantMenu merchants={[]} selectedId={null} onSelect={vi.fn()} />)
    await userEvent.setup().click(screen.getByRole('button', { name: /Nenhuma empresa/ }))

    expect(screen.getByRole('link', { name: 'Gerenciar empresas' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Regras de taxa/ })).not.toBeInTheDocument()
  })
})

describe('SelectedMerchantProvider', () => {
  function Selected() {
    const { merchantId, isLoading } = useSelectedMerchant()
    return <p>{isLoading ? 'carregando' : `selecionada: ${merchantId ?? 'nenhuma'}`}</p>
  }

  function renderConnected(pages: Merchant[][]) {
    const fetcher = vi.fn(async (input: string) => {
      const page = Number(new URL(input).searchParams.get('page'))
      return Response.json({ content: pages[page], totalPages: pages.length })
    })
    vi.stubGlobal('fetch', fetcher)
    render(
      <QueryClientProvider client={createQueryClient(vi.fn())}>
        <SelectedMerchantProvider><MerchantSwitcher /><Selected /></SelectedMerchantProvider>
      </QueryClientProvider>,
    )
    return { user: userEvent.setup(), fetcher }
  }

  it('lista as empresas de todas as páginas de /api/me/merchants', async () => {
    const { user, fetcher } = renderConnected([[alfa, beta], [gama]])

    await user.click(await screen.findByRole('button', { name: /Alfa Pagamentos/ }))

    expect(within(screen.getByRole('listbox')).getAllByRole('option')).toHaveLength(3)
    expect(screen.getByRole('option', { name: /Gama Serviços/ })).toBeInTheDocument()
    expect(fetcher).toHaveBeenCalledTimes(2)
  })

  it('seleciona a primeira empresa por padrão e troca ao escolher outra', async () => {
    const { user } = renderConnected([[alfa, beta, gama]])

    expect(await screen.findByText('selecionada: m-1')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Alfa Pagamentos/ }))
    await user.click(screen.getByRole('option', { name: /Beta Comércio/ }))

    await waitFor(() => expect(screen.getByText('selecionada: m-2')).toBeInTheDocument())
    expect(screen.getByRole('button', { name: /Beta Comércio/ })).toBeInTheDocument()
  })

  it('sem empresas no acesso não seleciona nenhuma', async () => {
    renderConnected([[]])

    await waitFor(() => expect(screen.getByText('selecionada: nenhuma')).toBeInTheDocument())
  })
})
