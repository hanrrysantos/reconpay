import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { LandingPage } from './LandingPage'

const GITHUB = 'https://github.com/hanrrysantos/reconpay'
const API_DOCS = 'https://reconpay.hanrry.top/swagger-ui/index.html'

const hrefOf = (link: HTMLElement) => link.getAttribute('href')

describe('LandingPage', () => {
  it('menu tem a marca, as três âncoras, "Entrar" e "Criar conta" (LAND-02 AC2, AC3)', () => {
    render(<LandingPage />)

    const nav = screen.getByRole('navigation', { name: 'Principal' })
    expect(within(nav).getAllByRole('link').map(link => [link.textContent, hrefOf(link)])).toEqual([
      ['reconpay', '/'],
      ['Como funciona', '#como-funciona'],
      ['Divergências', '#divergencias'],
      ['Fechamento', '#fechamento'],
      ['Entrar', '/login'],
      ['Criar conta', '/register'],
    ])
  })

  it('abertura com o selo, o único h1, o texto e os dois botões (LAND-02 AC1, AC2; LAND-05 AC1)', () => {
    render(<LandingPage />)

    expect(screen.getByText('Conciliação financeira para fintechs, gateways e marketplaces')).toBeInTheDocument()
    const headings = screen.getAllByRole('heading', { level: 1 })
    expect(headings).toHaveLength(1)
    expect(headings[0]).toHaveTextContent('O que você vendeu, o que caiu na conta e onde não bate.')
    expect(screen.getByText('O ReconPay cruza suas vendas com a liquidação da adquirente e o extrato do banco, aponta cada divergência e fecha o período com rastro para auditoria.')).toBeInTheDocument()

    const hero = headings[0].closest('section')!
    expect(within(hero).getByRole('link', { name: 'Criar conta' })).toHaveAttribute('href', '/register')
    expect(within(hero).getByRole('link', { name: 'Entrar na minha conta' })).toHaveAttribute('href', '/login')
    expect(screen.queryByText(/Ver demonstração/)).not.toBeInTheDocument()
  })

  it('prévia do painel é uma imagem com nome, alvo de "Fechamento" (LAND-02 AC3; LAND-05 AC1)', () => {
    render(<LandingPage />)

    const preview = screen.getByRole('img', { name: 'Prévia do painel de fechamento do ReconPay' })
    expect(preview).toHaveAttribute('id', 'fechamento')
    expect(preview).toHaveTextContent('Faltam 12 divergências para fechar setembro')
    expect(preview).toHaveTextContent('Fechamento · Setembro de 2026')
    expect(preview).toHaveTextContent('Taxa de match')
    expect(preview).toHaveTextContent('94,2%')
    expect(preview).toHaveTextContent('1.209 de 1.284 itens')
    for (const value of ['R$ 10.389,38', 'R$ 121.212,50', 'R$ 3.787,50']) expect(preview).toHaveTextContent(value)
    expect(preview).toHaveTextContent('Divergências abertas')
    expect(preview).toHaveTextContent('12 abertas')
    for (const row of ['PED-10021', 'Liquidação ausente', 'R$ 145,22', 'PED-10022', 'Taxa divergente', '−R$ 990,10', 'PED-10047', 'Status inconsistente', 'R$ 0,00', 'PED-10063', 'Parcelas divergentes', 'R$ 12,40']) {
      expect(preview).toHaveTextContent(row)
    }
  })

  it('rodapé com a marca, GitHub, Documentação da API, Entrar e o crédito (LAND-02 AC2, AC4)', () => {
    render(<LandingPage />)

    const footer = screen.getByRole('contentinfo')
    const links = within(footer).getAllByRole('link')
    expect(links.map(link => [link.textContent, hrefOf(link)])).toEqual([
      ['reconpay', '/'],
      ['GitHub', GITHUB],
      ['Documentação da API', API_DOCS],
      ['Entrar', '/login'],
    ])
    for (const external of links.slice(1, 3)) {
      expect(external).toHaveAttribute('target', '_blank')
      expect(external).toHaveAttribute('rel', 'noopener noreferrer')
    }
    expect(footer).toHaveTextContent('Feito por Hanrry Santos · 2026')
  })

  it('tem as regiões nav, main e footer (LAND-05 AC1)', () => {
    render(<LandingPage />)

    expect(screen.getByRole('main')).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Principal' })).toBeInTheDocument()
    expect(screen.getByRole('contentinfo')).toBeInTheDocument()
  })
})

const sectionOf = (name: string) => screen.getByRole('heading', { level: 2, name }).closest('section')!

describe('LandingPage: seções', () => {
  it('as seções vêm na ordem do desenho, cada uma com um h2 (LAND-02 AC1; LAND-05 AC1)', () => {
    render(<LandingPage />)

    expect(screen.getAllByRole('heading', { level: 2 }).map(heading => heading.textContent)).toEqual([
      'O valor que cai na conta quase nunca é o da venda.',
      'Três passos por período, do arquivo ao fechamento.',
      'Cada diferença tem dono, decisão e histórico.',
      'Feche o próximo mês sem abrir uma planilha.',
    ])
    const preview = screen.getByRole('img', { name: 'Prévia do painel de fechamento do ReconPay' })
    const problem = sectionOf('O valor que cai na conta quase nunca é o da venda.')
    expect(preview.compareDocumentPosition(problem) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(within(screen.getByRole('main')).getAllByRole('heading', { level: 2 })).toHaveLength(4)
  })

  it('problema com o texto e as quatro distorções (LAND-02 AC1)', () => {
    render(<LandingPage />)

    const problem = sectionOf('O valor que cai na conta quase nunca é o da venda.')
    expect(problem).toHaveTextContent('Taxa, parcela, atraso e estorno mudam o líquido no caminho. Na planilha, a diferença aparece semanas depois, sem dono e sem histórico.')
    const items = within(problem).getAllByRole('listitem')
    expect(items.map(item => [
      within(item).getByRole('heading', { level: 3 }).textContent,
      item.textContent,
    ])).toEqual([
      ['Taxa', '01TaxaA adquirente cobra diferente da regra combinada.'],
      ['Parcela', '02ParcelaA venda em 3x liquida como 1x, ou o contrário.'],
      ['Atraso', '03AtrasoA venda do dia 30 só liquida no mês seguinte.'],
      ['Estorno', '04EstornoCancelamento e chargeback que o sistema não viu.'],
    ])
  })

  it('"Como funciona" tem os três passos com as ilustrações e é o alvo da âncora (LAND-02 AC1, AC3)', () => {
    render(<LandingPage />)

    const how = sectionOf('Três passos por período, do arquivo ao fechamento.')
    expect(how).toHaveAttribute('id', 'como-funciona')
    expect(how).toHaveTextContent('Como funciona')
    const steps = within(how).getAllByRole('listitem')
    expect(steps.map(step => within(step).getByRole('heading', { level: 3 }).textContent)).toEqual(['Importe os três lados', 'Concilie a janela', 'Trate e trave'])

    expect(steps[0]).toHaveTextContent('01')
    expect(steps[0]).toHaveTextContent('Vendas, liquidação da adquirente e extrato do banco, em CSV. Um arquivo com erro volta com cada linha apontada.')
    for (const file of ['vendas-setembro.csv1.284 linhas', 'liquidacao-setembro.csv1.271 linhas', 'extrato-setembro.csv312 linhas']) expect(steps[0]).toHaveTextContent(file)

    expect(steps[1]).toHaveTextContent('02')
    expect(steps[1]).toHaveTextContent('Escolha o período e o ReconPay cruza tudo em segundo plano, item por item, com sete tipos de divergência.')
    for (const text of ['Conciliando setembro', '964 de 1.284', 'Conciliados 912', 'Divergentes 52']) expect(steps[1]).toHaveTextContent(text)

    expect(steps[2]).toHaveTextContent('03')
    expect(steps[2]).toHaveTextContent('Aceite, ajuste ou baixe cada divergência com nota. Sem nada em aberto, trave o período e exporte o CSV.')
    for (const text of ['Setembro travado', '05/10/2026 18:42 · fechou em 4 d 6 h', 'reconpay-2026-09-01-2026-09-30.csv']) expect(steps[2]).toHaveTextContent(text)
  })

  it('"Divergências" tem as três decisões e o ajuste de exemplo e é o alvo da âncora (LAND-02 AC1, AC3)', () => {
    render(<LandingPage />)

    const discrepancies = sectionOf('Cada diferença tem dono, decisão e histórico.')
    expect(discrepancies).toHaveAttribute('id', 'divergencias')
    expect(discrepancies).toHaveTextContent('Divergências')
    expect(discrepancies).toHaveTextContent('Abra a divergência, veja os dois lados lado a lado e decida. Toda mudança fica registrada com quem fez, quando e por quê.')
    expect(within(discrepancies).getAllByRole('listitem').map(item => item.textContent)).toEqual([
      'AceitarA diferença está correta e fica registrada.',
      'AjustarLança uma correção com sinal, no valor exato.',
      'BaixarEncerra sem correção, com a justificativa.',
    ])

    const example = within(discrepancies).getByRole('img', { name: 'Exemplo de ajuste da divergência PED-10022' })
    for (const text of ['PED-10022', 'Taxa divergente', 'Aberta', 'Venda', 'Liquidação', 'BrutoR$ 2.400,00R$ 2.400,00', 'LíquidoR$ 2.287,13R$ 1.297,03', 'MétodoPIX · 1xPIX · 1x', 'Aceitar', 'Ajustar', 'Baixar', '−990,10', 'Usar diferença', 'Salvar ajuste']) {
      expect(example).toHaveTextContent(text)
    }
    expect(within(discrepancies).queryByRole('button')).not.toBeInTheDocument()
  })

  it('convite final com o texto da conta e os dois botões (LAND-02 AC1, AC2)', () => {
    render(<LandingPage />)

    const cta = sectionOf('Feche o próximo mês sem abrir uma planilha.')
    expect(cta).toHaveTextContent('Crie sua conta e concilie o primeiro mês.')
    expect(within(cta).getByRole('link', { name: 'Criar conta' })).toHaveAttribute('href', '/register')
    expect(within(cta).getByRole('link', { name: 'Entrar na minha conta' })).toHaveAttribute('href', '/login')
    expect(cta).not.toHaveTextContent('demonstração')
  })
})
