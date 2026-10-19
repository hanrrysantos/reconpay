import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import RegisterPage from './page'

const api = vi.hoisted(() => ({ register: vi.fn() }))
vi.mock('@/lib/api', () => ({ register: api.register }))

beforeEach(() => {
  api.register.mockReset()
  api.register.mockResolvedValue(undefined)
})

function renderPage() {
  render(<RegisterPage />)
  return userEvent.setup()
}

const name = () => screen.getByLabelText('Nome completo')
const email = () => screen.getByLabelText('E-mail')
const password = () => screen.getByLabelText('Senha')
const submit = () => screen.getByRole('button', { name: /^(Criar conta|Criando conta\.\.\.)$/ })
const rule = (label: string) => within(screen.getByRole('list', { name: 'Regras da senha' })).getByText(label).closest('li')!

async function fill(user: ReturnType<typeof userEvent.setup>, values = { name: 'Ana Souza', email: 'ana@lojacentro.com.br', password: 'Reconpay20' }) {
  await user.type(name(), values.name)
  await user.type(email(), values.email)
  await user.type(password(), values.password)
}

describe('RegisterPage', () => {
  it('abre sem sessão e mostra o painel do cadastro ao lado do formulário "Criar conta" (CAD-01 AC1, VER-04)', () => {
    renderPage()
    const panel = screen.getByRole('complementary')

    expect(within(panel).getByText('reconpay')).toBeInTheDocument()
    expect(within(panel).getByRole('heading', { name: 'Seu primeiro fechamento começa aqui.' })).toBeInTheDocument()
    expect(within(panel).getByText('Crie a conta, cadastre a empresa e importe os arquivos do mês. A conta é ativada pelo link que enviamos por e-mail.')).toBeInTheDocument()
    expect(within(panel).getByText('Conciliação financeira para fintechs, gateways e marketplaces')).toBeInTheDocument()
    const main = screen.getByRole('main')
    expect(within(main).getByRole('heading', { level: 1, name: 'Criar conta' })).toBeInTheDocument()
    expect(within(main).getByText('Você confirma o e-mail antes de entrar.')).toBeInTheDocument()
    expect(panel.compareDocumentPosition(main) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })

  it('abaixo de 1024 px mantém só a marca e o título no painel (CAD-01 AC2)', () => {
    renderPage()
    const panel = screen.getByRole('complementary')

    expect(within(panel).getByText(/^Crie a conta, cadastre/)).toHaveClass('hidden', 'lg:block')
    expect(within(panel).getByText('Conciliação financeira para fintechs, gateways e marketplaces').parentElement).toHaveClass('hidden', 'lg:flex')
    expect(within(panel).getByRole('heading')).not.toHaveClass('hidden')
  })

  it('mostra os campos, o botão e o link para entrar (CAD-02 AC1)', () => {
    renderPage()

    expect(name()).toHaveAttribute('placeholder', 'Seu nome')
    expect(email()).toHaveAttribute('placeholder', 'voce@empresa.com')
    expect(password()).toHaveAttribute('placeholder', 'Crie uma senha')
    expect(submit()).toHaveTextContent('Criar conta')
    expect(screen.getByText('Já tem conta?')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Entrar' })).toHaveAttribute('href', '/login')
  })

  it('marca os campos como obrigatórios, com autocomplete e a regra da senha no navegador (CAD-02 AC2)', () => {
    renderPage()

    expect(name()).toBeRequired()
    expect(name()).toHaveAttribute('autocomplete', 'name')
    expect(email()).toBeRequired()
    expect(email()).toHaveAttribute('type', 'email')
    expect(email()).toHaveAttribute('autocomplete', 'email')
    expect(password()).toBeRequired()
    expect(password()).toHaveAttribute('type', 'password')
    expect(password()).toHaveAttribute('autocomplete', 'new-password')
    expect(password()).toHaveAttribute('minlength', '8')
    expect(password()).toHaveAttribute('pattern', '(?=.*[A-Z])(?=.*[0-9]).{8,}')
  })

  it('mostra e oculta a senha mantendo o que foi digitado (CAD-02 AC3)', async () => {
    const user = renderPage()
    await user.type(password(), 'Reconpay20')

    await user.click(screen.getByRole('button', { name: 'Mostrar senha' }))
    expect(password()).toHaveAttribute('type', 'text')
    expect(password()).toHaveValue('Reconpay20')
    const hide = screen.getByRole('button', { name: 'Ocultar senha' })
    expect(hide).toHaveAttribute('aria-pressed', 'true')

    await user.click(hide)
    expect(password()).toHaveAttribute('type', 'password')
    expect(screen.getByRole('button', { name: 'Mostrar senha' })).toHaveAttribute('aria-pressed', 'false')
    expect(password()).toHaveValue('Reconpay20')
  })

  it('marca cada regra da senha enquanto o usuário digita (CAD-02 AC4)', async () => {
    const user = renderPage()

    expect(rule('8 caracteres')).toHaveTextContent('8 caracteres: pendente')
    expect(rule('Uma letra maiúscula')).toHaveTextContent('Uma letra maiúscula: pendente')
    expect(rule('Um número')).toHaveTextContent('Um número: pendente')
    expect(password()).toHaveAttribute('aria-describedby', screen.getByRole('list', { name: 'Regras da senha' }).id)

    await user.type(password(), 'reconpay20')
    expect(rule('8 caracteres')).toHaveTextContent('8 caracteres: cumprida')
    expect(rule('Uma letra maiúscula')).toHaveTextContent('Uma letra maiúscula: pendente')
    expect(rule('Um número')).toHaveTextContent('Um número: cumprida')

    await user.clear(password())
    await user.type(password(), 'Recon20')
    expect(rule('8 caracteres')).toHaveTextContent('8 caracteres: pendente')
    expect(rule('Uma letra maiúscula')).toHaveTextContent('Uma letra maiúscula: cumprida')

    await user.type(password(), 'X')
    expect(rule('8 caracteres')).toHaveTextContent('8 caracteres: cumprida')
    expect(rule('Uma letra maiúscula')).toHaveTextContent('Uma letra maiúscula: cumprida')
    expect(rule('Um número')).toHaveTextContent('Um número: cumprida')

    await user.clear(password())
    await user.type(password(), 'Reconpay')
    expect(rule('Um número')).toHaveTextContent('Um número: pendente')
  })

  it('envia nome, e-mail e senha digitados (CAD-03 AC1)', async () => {
    const user = renderPage()
    await fill(user)

    await user.click(submit())

    expect(api.register).toHaveBeenCalledTimes(1)
    expect(api.register).toHaveBeenCalledWith({ name: 'Ana Souza', email: 'ana@lojacentro.com.br', password: 'Reconpay20' })
  })

  it('desabilita o botão com "Criando conta..." e não envia de novo (CAD-03 AC2)', async () => {
    let finish: () => void = () => {}
    api.register.mockReturnValue(new Promise<void>(resolve => { finish = resolve }))
    const user = renderPage()
    await fill(user)

    await user.click(submit())
    expect(submit()).toBeDisabled()
    expect(submit()).toHaveTextContent('Criando conta...')
    await user.click(submit())
    screen.getByRole('main').querySelector('form')!.requestSubmit()

    expect(api.register).toHaveBeenCalledTimes(1)
    finish()
    await screen.findByRole('heading', { name: 'Confira seu e-mail' })
  })

  it('troca o formulário pela confirmação com o e-mail usado (CAD-03 AC3)', async () => {
    const user = renderPage()
    await fill(user)

    await user.click(submit())

    const status = await screen.findByRole('status')
    expect(within(status).getByRole('heading', { level: 1, name: 'Confira seu e-mail' })).toBeInTheDocument()
    expect(status).toHaveTextContent('Enviamos um link de verificação para ana@lojacentro.com.br. Abra o e-mail e confirme a conta antes de entrar.')
    expect(within(status).getByRole('link', { name: 'Ir para o login' })).toHaveAttribute('href', '/login')
    expect(status).toHaveTextContent('Não chegou? Confira a caixa de spam.')
    expect(screen.queryByRole('heading', { name: 'Criar conta' })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Senha')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Criar conta' })).not.toBeInTheDocument()
  })

  it('mostra a mensagem da API acima do botão e mantém os campos (CAD-04 AC1)', async () => {
    api.register.mockRejectedValue(new Error('Email já cadastrado: ana@lojacentro.com.br'))
    const user = renderPage()
    await fill(user)

    await user.click(submit())

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Email já cadastrado: ana@lojacentro.com.br')
    expect(alert.compareDocumentPosition(submit()) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(name()).toHaveValue('Ana Souza')
    expect(email()).toHaveValue('ana@lojacentro.com.br')
    expect(password()).toHaveValue('Reconpay20')
    expect(submit()).toBeEnabled()
  })

  it('mostra a mensagem padrão quando a falha não é um Error (CAD-04 AC2)', async () => {
    api.register.mockRejectedValue('falhou')
    const user = renderPage()
    await fill(user)

    await user.click(submit())

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível criar a conta. Tente novamente.')
  })

  it('tira o aviso antes do novo envio e envia os valores corrigidos (CAD-04 AC3)', async () => {
    api.register.mockRejectedValueOnce(new Error('Email já cadastrado: ana@lojacentro.com.br'))
    const user = renderPage()
    await fill(user)
    await user.click(submit())
    await screen.findByRole('alert')

    let finish: () => void = () => {}
    api.register.mockReturnValueOnce(new Promise<void>(resolve => { finish = resolve }))
    await user.clear(name())
    await user.type(name(), 'Ana Lima')
    await user.clear(email())
    await user.type(email(), 'ana.lima@lojacentro.com.br')
    await user.clear(password())
    await user.type(password(), 'Outra2026')
    await user.click(submit())

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(api.register).toHaveBeenLastCalledWith({ name: 'Ana Lima', email: 'ana.lima@lojacentro.com.br', password: 'Outra2026' })
    finish()
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('ana.lima@lojacentro.com.br'))
  })
})
