import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import UsuariosPage from '@/app/(app)/usuarios/page'
import { createQueryClient } from '@/lib/queries'
import { account, fail, installFakeApi, json, type FakeServer } from './fakeUsersApi'

vi.mock('next/navigation', () => ({ useRouter: () => ({ replace: vi.fn() }) }))
vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-admin', name: 'Ana Admin', email: 'ana@reconpay.dev', role: 'ADMIN' } }) }))

const WEAK = 'Senha deve ter no mínimo 8 caracteres, uma letra maiúscula e um número'

let server: FakeServer

beforeEach(() => {
  server = installFakeApi({ users: [account('u-admin', 'Ana Admin', { role: 'ADMIN' }), account('u-bruno', 'Bruno Operador')] })
})
afterEach(() => vi.unstubAllGlobals())

async function renderPage() {
  render(<QueryClientProvider client={createQueryClient(vi.fn())}><UsuariosPage /></QueryClientProvider>)
  await screen.findByRole('table', { name: 'Usuários' })
  return userEvent.setup()
}

const drawer = () => screen.getByRole('dialog', { name: 'Novo usuário' })
const posts = () => server.calls.filter(call => call.method === 'POST' && call.path === '/api/users')
const listRequests = () => server.calls.filter(call => call.method === 'GET' && call.path === '/api/users')
const rowNames = () => within(screen.getByRole('table', { name: 'Usuários' })).getAllByRole('row').slice(1).map(row => row.querySelector('td span')!.textContent)

async function openCreate() {
  const user = await renderPage()
  await user.click(screen.getByRole('button', { name: 'Novo usuário' }))
  return user
}

async function fill(user: ReturnType<typeof userEvent.setup>, values: { name?: string; email?: string; password?: string }) {
  const panel = drawer()
  if (values.name) await user.type(within(panel).getByLabelText('Nome'), values.name)
  if (values.email) await user.type(within(panel).getByLabelText('E-mail'), values.email)
  if (values.password) await user.type(within(panel).getByLabelText('Senha'), values.password)
}

const valid = { name: 'Carla Nova', email: 'carla@reconpay.dev', password: 'Segura123' }

describe('UserDrawer: criar', () => {
  it('"Novo usuário" abre o painel com nome, e-mail, senha e papel, com Operador marcado (USERS-03 AC1)', async () => {
    await openCreate()

    const panel = drawer()
    expect(within(panel).getByLabelText('Nome')).toHaveValue('')
    expect(within(panel).getByLabelText('E-mail')).toHaveValue('')
    expect(within(panel).getByLabelText('Senha')).toHaveAttribute('type', 'password')
    expect(within(panel).getByRole('radio', { name: 'Operador' })).toBeChecked()
    expect(within(panel).getByRole('radio', { name: 'Admin' })).not.toBeChecked()
  })

  it.each([
    ['Operador', 'OPERATOR'],
    ['Admin', 'ADMIN'],
  ])('com o papel %s envia role=%s, fecha, atualiza a lista e mostra "Usuário criado." (USERS-03 AC4)', async (label, role) => {
    const user = await openCreate()
    await fill(user, valid)
    if (label === 'Admin') await user.click(within(drawer()).getByRole('radio', { name: 'Admin' }))
    const listsBefore = listRequests().length

    await user.click(within(drawer()).getByRole('button', { name: 'Criar' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Usuário criado.')
    expect(posts().map(call => call.body)).toEqual([{ name: 'Carla Nova', email: 'carla@reconpay.dev', password: 'Segura123', role }])
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await waitFor(() => expect(rowNames()).toEqual(['Ana Admin', 'Bruno Operador', 'Carla Nova']))
    expect(listRequests().length).toBeGreaterThan(listsBefore)
  })

  it.each([
    ['menos de 8 caracteres', 'Segur12'],
    ['sem letra maiúscula', 'segura123'],
    ['sem número', 'SeguraSenha'],
  ])('senha %s mostra a mensagem exata e não chama a API (USERS-03 AC2)', async (_case, password) => {
    const user = await openCreate()
    await fill(user, { ...valid, password })
    await user.click(within(drawer()).getByRole('button', { name: 'Criar' }))

    expect(within(drawer()).getByText(WEAK)).toBeInTheDocument()
    expect(within(drawer()).getByLabelText('Senha')).toHaveAttribute('aria-invalid', 'true')
    expect(posts()).toHaveLength(0)
  })

  it('senha com exatamente 8 caracteres, uma maiúscula e um número é aceita (USERS-03 AC2)', async () => {
    const user = await openCreate()
    await fill(user, { ...valid, password: 'Abcdefg1' })
    await user.click(within(drawer()).getByRole('button', { name: 'Criar' }))

    await waitFor(() => expect(posts()).toHaveLength(1))
    expect(screen.queryByText(WEAK)).not.toBeInTheDocument()
  })

  it('nome vazio mostra o erro no campo e não chama a API (USERS-03 AC3)', async () => {
    const user = await openCreate()
    await fill(user, { ...valid, name: '   ' })
    await user.click(within(drawer()).getByRole('button', { name: 'Criar' }))

    expect(within(drawer()).getByText('Informe o nome')).toBeInTheDocument()
    expect(within(drawer()).getByLabelText('Nome')).toHaveAttribute('aria-invalid', 'true')
    expect(posts()).toHaveLength(0)
  })

  it.each([['carla'], ['carla@reconpay'], ['carla @reconpay.dev']])('e-mail inválido (%s) mostra o erro no campo e não chama a API (USERS-03 AC3)', async email => {
    const user = await openCreate()
    await fill(user, { ...valid, email })
    await user.click(within(drawer()).getByRole('button', { name: 'Criar' }))

    expect(within(drawer()).getByText('Informe um e-mail válido')).toBeInTheDocument()
    expect(within(drawer()).getByLabelText('E-mail')).toHaveAttribute('aria-invalid', 'true')
    expect(posts()).toHaveLength(0)
  })

  it('409 mostra a message da API e mantém o painel aberto com os dados digitados (USERS-03 AC5)', async () => {
    server.create = () => fail(409, 'E-mail já cadastrado')
    const user = await openCreate()
    await fill(user, valid)
    await user.click(within(drawer()).getByRole('radio', { name: 'Admin' }))
    await user.click(within(drawer()).getByRole('button', { name: 'Criar' }))

    expect(await within(drawer()).findByRole('alert')).toHaveTextContent('E-mail já cadastrado')
    expect(within(drawer()).getByLabelText('Nome')).toHaveValue('Carla Nova')
    expect(within(drawer()).getByLabelText('E-mail')).toHaveValue('carla@reconpay.dev')
    expect(within(drawer()).getByLabelText('Senha')).toHaveValue('Segura123')
    expect(within(drawer()).getByRole('radio', { name: 'Admin' })).toBeChecked()
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('o botão de enviar fica desabilitado durante o envio (USERS-03 AC6)', async () => {
    let release: (response: Response) => void = () => {}
    server.create = () => () => new Promise<Response>(resolve => { release = resolve })
    const user = await openCreate()
    await fill(user, valid)
    await user.click(within(drawer()).getByRole('button', { name: 'Criar' }))

    await waitFor(() => expect(within(drawer()).getByRole('button', { name: 'Criar' })).toBeDisabled())
    release(json(account('u-new', 'Carla Nova'), 201))
    expect(await screen.findByRole('status')).toHaveTextContent('Usuário criado.')
  })
})

describe('UserDrawer: renomear', () => {
  const renameDrawer = () => screen.getByRole('dialog', { name: 'Renomear usuário' })
  const renames = () => server.calls.filter(call => call.method === 'PUT' && /^\/api\/users\/[^/]+$/.test(call.path))

  async function openRename(name = 'Bruno Operador') {
    const user = await renderPage()
    const row = within(screen.getByRole('table', { name: 'Usuários' })).getAllByRole('row').find(item => item.textContent?.includes(name))!
    await user.click(within(row).getByRole('button', { name: 'Renomear' }))
    return user
  }

  async function typeName(user: ReturnType<typeof userEvent.setup>, name: string) {
    const input = within(renameDrawer()).getByLabelText('Nome')
    await user.clear(input)
    if (name) await user.click(input)
    if (name) await user.paste(name)
  }

  it('abre com o nome atual e sem e-mail, senha e papel (USERS-06 AC1)', async () => {
    await openRename()

    expect(within(renameDrawer()).getByLabelText('Nome')).toHaveValue('Bruno Operador')
    expect(within(renameDrawer()).queryByLabelText('E-mail')).not.toBeInTheDocument()
    expect(within(renameDrawer()).queryByLabelText('Senha')).not.toBeInTheDocument()
    expect(within(renameDrawer()).queryByRole('radio')).not.toBeInTheDocument()
  })

  it.each([
    ['vazio', '', 'Informe o nome'],
    ['com 151 caracteres', 'a'.repeat(151), 'Use no máximo 150 caracteres'],
  ])('nome %s mostra o erro no campo e não chama a API (USERS-06 AC2)', async (_case, name, message) => {
    const user = await openRename()
    await typeName(user, name)
    await user.click(within(renameDrawer()).getByRole('button', { name: 'Salvar' }))

    expect(within(renameDrawer()).getByText(message)).toBeInTheDocument()
    expect(within(renameDrawer()).getByLabelText('Nome')).toHaveAttribute('aria-invalid', 'true')
    expect(renames()).toHaveLength(0)
  })

  it('nome com 150 caracteres é enviado (USERS-06 AC2)', async () => {
    const user = await openRename()
    await typeName(user, 'b'.repeat(150))
    await user.click(within(renameDrawer()).getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(renames().map(call => call.body)).toEqual([{ name: 'b'.repeat(150) }]))
  })

  it.each([
    ['Bruno Operador', 'u-bruno'],
    ['Ana Admin', 'u-admin'],
  ])('200 em %s fecha, atualiza a lista e mostra "Usuário renomeado." (USERS-06 AC3)', async (name, id) => {
    const user = await openRename(name)
    await typeName(user, 'Zeca Renomeado')
    await user.click(within(renameDrawer()).getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Usuário renomeado.')
    expect(renames().map(call => [call.path, call.body])).toEqual([[`/api/users/${id}`, { name: 'Zeca Renomeado' }]])
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await waitFor(() => expect(rowNames()).toContain('Zeca Renomeado'))
    expect(rowNames()).not.toContain(name)
  })

  it('erro da API mantém o painel aberto com a message', async () => {
    server.rename = () => fail(404, 'Usuário não encontrado')
    const user = await openRename()
    await typeName(user, 'Zeca Renomeado')
    await user.click(within(renameDrawer()).getByRole('button', { name: 'Salvar' }))

    expect(await within(renameDrawer()).findByRole('alert')).toHaveTextContent('Usuário não encontrado')
    expect(within(renameDrawer()).getByLabelText('Nome')).toHaveValue('Zeca Renomeado')
  })
})
