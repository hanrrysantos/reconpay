import type { Metadata } from 'next'
import { AuthProvider } from '@/lib/auth'
import './globals.css'

export const metadata: Metadata = {
  title: 'ReconPay · Conciliação financeira',
  description: 'Acompanhe suas transações e conciliações no ReconPay.',
  icons: { icon: '/icon.svg' },
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="pt-BR">
      <body className="antialiased"><AuthProvider>{children}</AuthProvider></body>
    </html>
  )
}
