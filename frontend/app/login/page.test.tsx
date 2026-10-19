import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LoginPage from './page'

const navigation = vi.hoisted(() => ({ replace: vi.fn() }))
vi.mock('next/navigation', () => ({ useRouter: () => ({ replace: navigation.replace }) }))
const auth = vi.hoisted(() => ({ signIn: vi.fn(), user: null as { name: string } | null }))
vi.mock('@/lib/auth', () => ({ useAuth: () => ({ signIn: auth.signIn, user: auth.user }) }))

beforeEach(() => {
  navigation.replace.mockReset()
  auth.signIn.mockReset()
  auth.signIn.mockResolvedValue(undefined)
  auth.user = null
})

function renderPage() {
  render(<LoginPage />)
  return userEvent.setup()
}

const email = () => screen.getByLabelText('E-mail')
const password = () => screen.getByLabelText('Senha')
const submit = () => screen.getByRole('button', { name: /^(Entrar|Entrando\.\.\.)$/ })

async function fill(user: ReturnType<typeof userEvent.setup>, typedEmail = 'ana@lojacentro.com.br', typedPassword = 'Senha@2026') {
  await user.type(email(), typedEmail)
  await user.type(password(), typedPassword)
}

describe('LoginPage', () => {
  it('mostra o painel com a marca, o título, o texto e o rodapé, e o formulário "Entrar" (LOG-01 AC1)', () => {
    renderPage()
    const panel = screen.getByRole('complementary')
    expect(within(panel).getByText('Pay').parentElement).toHaveTextContent(/^ReconPay$/)
    expect(within(panel).getByText('Conciliação financeira com clareza')).toBeInTheDocument()
    expect(within(panel).getByRole('heading', { name: 'Do vendido ao que caiu na conta, sem planilha.' })).toBeInTheDocument()
    expect(within(panel).getByText('Cruze vendas, liquidações e extrato numa janela, trate cada divergência e trave o período com rastro para auditoria.')).toBeInTheDocument()
    expect(within(panel).getByText('Conciliação financeira para fintechs, gateways e marketplaces')).toBeInTheDocument()
    const main = screen.getByRole('main')
    expect(within(main).getByRole('heading', { level: 1, name: 'Entrar' })).toBeInTheDocument()
    expect(within(main).getByText('Use o e-mail e a senha da sua conta ReconPay.')).toBeInTheDocument()
  })

  it('abaixo de 1024 px o texto e o rodapé do painel ficam ocultos e a marca e o título ficam (LOG-01 AC2)', () => {
    renderPage()
    const panel = screen.getByRole('complementary')
    expect(within(panel).getByText(/^Cruze vendas/)).toHaveClass('hidden', 'lg:block')
    expect(within(panel).getByText('Conciliação financeira para fintechs, gateways e marketplaces').parentElement).toHaveClass('hidden', 'lg:flex')
    expect(within(panel).getByText('Pay').parentElement).not.toHaveClass('hidden')
    expect(within(panel).getByRole('heading')).not.toHaveClass('hidden')
  })

  it('o painel vem antes do formulário: em cima no celular e à esquerda a partir de 1024 px (LOG-01 AC1, AC2)', () => {
    renderPage()
    const panel = screen.getByRole('complementary')
    const main = screen.getByRole('main')
    expect(panel.compareDocumentPosition(main) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(panel.parentElement).toBe(main.parentElement)
    expect(panel.parentElement).toHaveClass('flex', 'flex-col', 'lg:flex-row')
    expect(panel.parentElement).not.toHaveClass('flex-col-reverse')
  })

  it('campos com rótulos, placeholders, tipos e preenchimento automático (LOG-02 AC1, AC2)', () => {
    renderPage()
    expect(email()).toHaveAttribute('placeholder', 'voce@empresa.com')
    expect(email()).toHaveAttribute('type', 'email')
    expect(email()).toHaveAttribute('autocomplete', 'email')
    expect(email()).toBeRequired()
    expect(password()).toHaveAttribute('placeholder', 'Sua senha')
    expect(password()).toHaveAttribute('type', 'password')
    expect(password()).toHaveAttribute('autocomplete', 'current-password')
    expect(password()).toBeRequired()
    expect(submit()).toHaveTextContent('Entrar')
  })

  it('"Mostrar senha" mostra o texto e vira "Ocultar senha"; ocultar volta a esconder, sem perder o que foi digitado (LOG-02 AC3, edge)', async () => {
    const user = renderPage()
    await user.type(password(), 'Senha@2026')

    await user.click(screen.getByRole('button', { name: 'Mostrar senha' }))
    expect(password()).toHaveAttribute('type', 'text')
    expect(password()).toHaveValue('Senha@2026')
    const hide = screen.getByRole('button', { name: 'Ocultar senha' })
    expect(hide).toHaveAttribute('aria-pressed', 'true')

    await user.click(hide)
    expect(password()).toHaveAttribute('type', 'password')
    expect(password()).toHaveValue('Senha@2026')
    expect(screen.getByRole('button', { name: 'Mostrar senha' })).toHaveAttribute('aria-pressed', 'false')
  })

  it('"Criar conta" leva a /register (LOG-02 AC4)', () => {
    renderPage()
    expect(screen.getByText('Ainda não tem conta?')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Criar conta' })).toHaveAttribute('href', '/register')
  })

  it('enviar chama signIn com o e-mail e a senha e, ao concluir, vai para /painel (LOG-03 AC1, AC3)', async () => {
    const user = renderPage()
    await fill(user)
    await user.click(submit())

    expect(auth.signIn).toHaveBeenCalledWith('ana@lojacentro.com.br', 'Senha@2026')
    await waitFor(() => expect(navigation.replace).toHaveBeenCalledWith('/painel'))
  })

  it('durante o envio o botão fica desabilitado com "Entrando..." e outro envio não chama signIn de novo (LOG-03 AC2)', async () => {
    let finish!: () => void
    auth.signIn.mockReturnValue(new Promise<void>(resolve => { finish = resolve }))
    const user = renderPage()
    await fill(user)
    await user.click(submit())

    expect(submit()).toBeDisabled()
    expect(submit()).toHaveTextContent('Entrando...')
    await user.type(password(), '{Enter}')
    expect(auth.signIn).toHaveBeenCalledTimes(1)
    expect(navigation.replace).not.toHaveBeenCalled()

    finish()
    await waitFor(() => expect(navigation.replace).toHaveBeenCalledWith('/painel'))
    expect(submit()).toBeEnabled()
    expect(submit()).toHaveTextContent('Entrar')
  })

  it('falha com mensagem mostra o aviso acima do botão e mantém e-mail e senha (LOG-04 AC1)', async () => {
    auth.signIn.mockRejectedValue(new Error('Credenciais inválidas'))
    const user = renderPage()
    await fill(user)
    await user.click(submit())

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Credenciais inválidas')
    expect(alert.compareDocumentPosition(submit()) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(email()).toHaveValue('ana@lojacentro.com.br')
    expect(password()).toHaveValue('Senha@2026')
    expect(navigation.replace).not.toHaveBeenCalled()
  })

  it('falha sem mensagem mostra "Não foi possível entrar. Tente novamente." (LOG-04 AC2)', async () => {
    auth.signIn.mockRejectedValue('falhou')
    const user = renderPage()
    await fill(user)
    await user.click(submit())

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível entrar. Tente novamente.')
  })

  it('o novo envio tira o aviso antes de chamar signIn e manda o e-mail mantido com a senha corrigida (LOG-04 AC3, edge)', async () => {
    auth.signIn.mockRejectedValueOnce(new Error('Credenciais inválidas'))
    const user = renderPage()
    await fill(user, 'ana@lojacentro.com.br', 'errada')
    await user.click(submit())
    await screen.findByRole('alert')

    let finish!: () => void
    auth.signIn.mockReturnValueOnce(new Promise<void>(resolve => { finish = resolve }))
    await user.clear(password())
    await user.type(password(), 'Senha@2026')
    await user.click(submit())

    expect(screen.queryByRole('alert')).toBeNull()
    expect(auth.signIn).toHaveBeenLastCalledWith('ana@lojacentro.com.br', 'Senha@2026')
    finish()
    await waitFor(() => expect(navigation.replace).toHaveBeenCalledWith('/painel'))
  })

  it('com sessão ativa vai para /painel sem enviar nada (LOG-05 AC1)', () => {
    auth.user = { name: 'Ana Souza' }
    renderPage()
    expect(navigation.replace).toHaveBeenCalledWith('/painel')
    expect(auth.signIn).not.toHaveBeenCalled()
  })

  it('sem sessão não sai da tela (LOG-05 AC1)', () => {
    renderPage()
    expect(navigation.replace).not.toHaveBeenCalled()
  })
})
