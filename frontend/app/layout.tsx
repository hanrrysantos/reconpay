import type { Metadata } from 'next'
import { IBM_Plex_Mono, Inter, Inter_Tight } from 'next/font/google'
import { Providers } from './providers'
import './globals.css'

const inter = Inter({ subsets: ['latin'], variable: '--font-inter', display: 'swap' })
const interTight = Inter_Tight({ subsets: ['latin'], weight: ['600', '700', '800'], variable: '--font-inter-tight', display: 'swap' })
const plexMono = IBM_Plex_Mono({ subsets: ['latin'], weight: ['400', '500', '600'], variable: '--font-ibm-plex-mono', display: 'swap' })

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
    <html lang="pt-BR" className={`${inter.variable} ${interTight.variable} ${plexMono.variable}`}>
      <body className="bg-background font-sans text-foreground antialiased"><Providers>{children}</Providers></body>
    </html>
  )
}
