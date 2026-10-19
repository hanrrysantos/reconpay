import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Logo } from './Logo'

const TAGLINE = 'Conciliação financeira com clareza'

const mark = (container: HTMLElement) => container.querySelector('img')!
const pay = () => screen.getByText('Pay')

describe('Logo (BRD-02)', () => {
  it('em fundo escuro usa o símbolo para fundo escuro, "Recon" branco e "Pay" verde-limão', () => {
    const { container } = render(<Logo tone="dark" />)

    expect(mark(container)).toHaveAttribute('src', '/brand/reconpay-mark-on-dark.svg')
    expect(pay().parentElement).toHaveTextContent(/^ReconPay$/)
    expect(pay().parentElement).toHaveClass('text-background')
    expect(pay()).toHaveClass('text-primary')
  })

  it('em fundo claro usa o símbolo para fundo claro e "ReconPay" todo preto', () => {
    const { container } = render(<Logo tone="light" />)

    expect(mark(container)).toHaveAttribute('src', '/brand/reconpay-mark-on-light.svg')
    expect(pay().parentElement).toHaveTextContent(/^ReconPay$/)
    expect(pay().parentElement).toHaveClass('text-foreground')
    expect(pay()).not.toHaveClass('text-primary')
  })

  it('o nome usa a fonte da marca em peso 800', () => {
    render(<Logo tone="light" />)

    expect(pay().parentElement).toHaveClass('font-brand', 'font-extrabold')
  })

  it('mostra a frase só quando pedida', () => {
    const { rerender } = render(<Logo tone="dark" />)
    expect(screen.queryByText(TAGLINE)).not.toBeInTheDocument()

    rerender(<Logo tone="dark" tagline />)
    expect(screen.getByText(TAGLINE)).toBeInTheDocument()
  })

  it.each([
    ['md', 'h-[30px]', 'text-[21px]'],
    ['sm', 'h-[26px]', 'text-[18px]'],
  ] as const)('no tamanho %s o símbolo tem %s e o nome %s', (size, markHeight, nameSize) => {
    const { container } = render(<Logo tone="light" size={size} />)

    expect(mark(container)).toHaveClass(markHeight)
    expect(pay().parentElement).toHaveClass(nameSize)
  })

  it.each([
    ['md', 'h-[50px]'],
    ['sm', 'h-[42px]'],
  ] as const)('com a frase, no tamanho %s o símbolo cresce para %s e a frase fica em duas linhas', (size, markHeight) => {
    const { container } = render(<Logo tone="dark" size={size} tagline />)

    expect(mark(container)).toHaveClass(markHeight)
    expect(screen.getByText(/^Conciliação financeira/).querySelector('br')).toBeInTheDocument()
  })

  it('o símbolo é decorativo e o texto lido é "ReconPay" (BRD-02 AC4)', () => {
    const { container } = render(<Logo tone="dark" />)

    expect(mark(container)).toHaveAttribute('alt', '')
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
    expect(container).toHaveTextContent(/^ReconPay$/)
  })
})
