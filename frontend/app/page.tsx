import type { Metadata } from 'next'
import { LandingPage } from '@/components/landing/LandingPage'

export const metadata: Metadata = {
  title: 'ReconPay · Conciliação financeira',
  description: 'O ReconPay cruza suas vendas com a liquidação da adquirente e o extrato do banco, aponta cada divergência e fecha o período com rastro para auditoria.',
}

export default function Home() {
  return <LandingPage />
}
