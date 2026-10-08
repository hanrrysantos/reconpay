'use client'

import {
  ArrowDownToLine,
  CreditCard,
  FileCheck2,
  FileWarning,
  LayoutDashboard,
  LogOut,
  RefreshCw,
  ShieldCheck,
  Wallet,
  Zap,
} from 'lucide-react'

export type DashboardProps = {
  user: { name: string; email: string; role: string }
  merchants: { id: string; name: string; document: string }[]
  selectedMerchantId: string | null
  onSelectMerchant: (id: string) => void
  onLogout: () => void
  transactions: {
    id: string
    merchantId: string
    externalReference: string
    amount: string
    paymentMethod: string
    status: string
    transactionDate: string
  }[]
  isLoading: boolean
  error: string | null
  onRetry: () => void
}

const pendingMetrics = [
  { label: 'Saldo conciliado', icon: ShieldCheck },
  { label: 'Taxa de conciliação', icon: FileCheck2 },
  { label: 'Em divergência', icon: FileWarning },
  { label: 'Tempo médio', icon: Zap },
]

const paymentMethods: Record<string, string> = {
  CREDIT_CARD: 'Cartão de crédito',
  DEBIT_CARD: 'Cartão de débito',
  PIX: 'Pix',
  BOLETO: 'Boleto',
}

const transactionStatuses: Record<string, { label: string; style: string }> = {
  APPROVED: { label: 'Aprovada', style: 'success' },
  CANCELLED: { label: 'Cancelada', style: 'warning' },
  REFUNDED: { label: 'Estornada', style: 'warning' },
  CHARGEBACK: { label: 'Contestada', style: 'danger' },
}

function initials(value: string) {
  return value.trim().split(/\s+/).slice(0, 2).map((word) => word[0]?.toUpperCase()).join('') || 'RP'
}

export function formatAmount(value: string) {
  const match = /^(-?)(\d+)(?:\.(\d{1,2}))?$/.exec(value)
  if (!match) return `R$ ${value}`
  const [, sign, whole, cents = ''] = match
  return `${sign}R$ ${whole.replace(/\B(?=(\d{3})+(?!\d))/g, '.').replace(/^0+(?=\d)/, '')},${cents.padEnd(2, '0')}`
}

function formatDate(value: string) {
  const date = new Date(`${value}T00:00:00`)
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' }).format(date)
}

export function DashboardView({ user, merchants, selectedMerchantId, onSelectMerchant, onLogout, transactions, isLoading, error, onRetry }: DashboardProps) {
  const firstName = user.name.trim().split(/\s+/)[0] || user.name
  const selectedMerchant = merchants.find((merchant) => merchant.id === selectedMerchantId)
  const visibleTransactions = transactions.filter((transaction) => transaction.merchantId === selectedMerchantId)

  return (
    <div className="dashboard app-shell">
      <aside className="sidebar">
        <a className="brand" href="/" aria-label="ReconPay, visão geral"><div className="brand-mark"><ShieldCheck /></div><span>reconpay</span></a>
        <div className="workspace-switcher">
          <span className="workspace-avatar" aria-hidden="true">{initials(selectedMerchant?.name || 'ReconPay')}</span>
          <label className="workspace-selector">
            <span className="sr-only">Empresa selecionada</span>
            <select value={selectedMerchantId ?? ''} onChange={(event) => onSelectMerchant(event.target.value)} disabled={merchants.length === 0}>
              {merchants.length === 0 && <option value="">Nenhuma empresa</option>}
              {merchants.length > 0 && !selectedMerchantId && <option value="">Selecione uma empresa</option>}
              {merchants.map((merchant) => <option key={merchant.id} value={merchant.id}>{merchant.name}</option>)}
            </select>
            <small>{selectedMerchant?.document || 'Seu workspace'}</small>
          </label>
        </div>

        <nav className="main-nav" aria-label="Navegação principal">
          <p className="nav-label">Workspace</p>
          <a className="nav-item active" href="/" aria-label="Visão geral" aria-current="page"><LayoutDashboard /><span>Visão geral</span></a>
          <span className="nav-item nav-item-disabled" role="link" aria-label="Conciliações, disponível em breve" aria-disabled="true" title="Disponível em uma próxima etapa"><FileCheck2 /><span>Conciliações</span></span>
          <a className="nav-item" href="#transacoes" aria-label="Transações"><ArrowDownToLine /><span>Transações</span></a>
          <span className="nav-item nav-item-disabled" role="link" aria-label="Contas e fontes, disponível em breve" aria-disabled="true" title="Disponível em uma próxima etapa"><Wallet /><span>Contas e fontes</span></span>
          <p className="nav-label nav-label-spaced">Gestão</p>
          <span className="nav-item nav-item-disabled" role="link" aria-label="Regras automáticas, disponível em breve" aria-disabled="true" title="Disponível em uma próxima etapa"><Zap /><span>Regras automáticas</span></span>
          <span className="nav-item nav-item-disabled" role="link" aria-label="Exceções, disponível em breve" aria-disabled="true" title="Disponível em uma próxima etapa"><FileWarning /><span>Exceções</span></span>
        </nav>

        <div className="sidebar-bottom">
          <button className="nav-item" type="button" aria-label="Sair" onClick={onLogout}><LogOut /><span>Sair</span></button>
          <div className="profile"><span className="profile-avatar" aria-hidden="true">{initials(user.name)}</span><div><strong>{user.name}</strong><small>{user.role === 'ADMIN' ? 'Administrador' : 'Operador'}</small></div></div>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar"><div className="breadcrumbs"><span>Workspace</span><span>/</span><strong>Visão geral</strong></div><div className="top-actions"><span className="top-user">{user.email}</span><span className="top-avatar" aria-hidden="true">{initials(user.name)}</span></div></header>

        <div className="content-wrap">
          <section className="page-heading">
            <div><div className="eyebrow"><span className="live-dot" /> Visão geral</div><h1>Olá, {firstName} <span>—</span></h1><p>Acompanhe as transações da empresa selecionada.</p></div>
          </section>

          <section className="metrics-grid" aria-label="Indicadores principais">
            {pendingMetrics.map(({ label, icon: Icon }) => <article className="metric-card reference-metric" key={label}><div className="metric-top"><span>{label}</span><span className="metric-icon blue"><Icon /></span></div><div className="metric-value" aria-label={`${label}: indisponível`}>—</div><div className="metric-footer"><span>Disponível após integração do dashboard</span></div></article>)}
          </section>

          <section className="dashboard-grid">
            <article className="panel chart-panel"><div className="panel-header"><div><h2>Movimentação financeira</h2><p>Entradas e saídas conciliadas no período</p></div></div><div className="cashflow-toolbar"><div><strong>Saldo no período</strong><b>—</b></div></div><div className="chart-legend"><span><i className="legend-dot income" /> Entradas</span><span><i className="legend-dot expense" /> Saídas</span></div><div className="chart cashflow-chart chart-placeholder" role="status"><div className="chart-area"><div className="grid-lines" aria-hidden="true"><i /><i /><i /><i /><i /></div><p>Os dados do gráfico aparecerão quando a API do dashboard estiver disponível.</p></div></div></article>
            <article className="panel health-panel"><div className="panel-header"><div><h2>Saúde das fontes</h2><p>Status das conexões financeiras</p></div></div><div className="health-score"><div className="score-ring score-ring-empty"><div><strong>—</strong></div></div><div><strong>Aguardando dados</strong><p>O status das fontes aparecerá quando essa integração estiver disponível.</p></div></div><div className="source-list source-empty"><CreditCard /><p>Nenhuma fonte exibida</p></div></article>
          </section>

          <section className="panel transactions-panel" id="transacoes" aria-busy={isLoading}>
            <div className="panel-header"><div><h2>Transações recentes</h2><p>Últimas transações da empresa selecionada</p></div></div>
            {error && <div className="dashboard-alert" role="alert"><span>{error}</span><button type="button" onClick={onRetry}><RefreshCw /> Tentar novamente</button></div>}
            {isLoading ? <p className="table-state" role="status">Carregando transações...</p> : !error && !selectedMerchantId ? <p className="table-state">{merchants.length === 0 ? 'Nenhuma empresa disponível para sua conta.' : 'Selecione uma empresa para ver as transações.'}</p> : !error && visibleTransactions.length === 0 ? <p className="table-state">Nenhuma transação encontrada para esta empresa.</p> : !error && <div className="table-wrap"><table><thead><tr><th>Transação</th><th>Data</th><th>Valor</th><th>Status</th></tr></thead><tbody>{visibleTransactions.map((transaction) => {
              const status = transactionStatuses[transaction.status] ?? { label: transaction.status, style: 'warning' }
              return <tr key={transaction.id}><td><div className="transaction-name"><span className="transaction-icon"><CreditCard /></span><div><strong>{paymentMethods[transaction.paymentMethod] || transaction.paymentMethod}</strong><small>{transaction.externalReference}</small></div></div></td><td className="muted-cell">{formatDate(transaction.transactionDate)}</td><td><strong>{formatAmount(transaction.amount)}</strong></td><td><span className={`status-badge ${status.style}`}><span className="status-dot" />{status.label}</span></td></tr>
            })}</tbody></table></div>}
          </section>
          <div className="footer-note"><ShieldCheck /> Dados de transações fornecidos pela API ReconPay</div>
        </div>
      </main>
    </div>
  )
}
