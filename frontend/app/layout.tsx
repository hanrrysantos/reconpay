import type { Metadata } from 'next'
import localFont from 'next/font/local'
import { Providers } from './providers'
import './globals.css'

// Self-hosted from @fontsource (latin subset) so the build never fetches Google Fonts over the network.
const inter = localFont({
  src: '../node_modules/@fontsource-variable/inter/files/inter-latin-wght-normal.woff2',
  weight: '100 900',
  variable: '--font-inter',
  display: 'swap',
})
const interTight = localFont({
  src: '../node_modules/@fontsource-variable/inter-tight/files/inter-tight-latin-wght-normal.woff2',
  weight: '100 900',
  variable: '--font-inter-tight',
  display: 'swap',
})
const manrope = localFont({
  src: '../node_modules/@fontsource-variable/manrope/files/manrope-latin-wght-normal.woff2',
  weight: '200 800',
  variable: '--font-manrope',
  display: 'swap',
})
const plexMono = localFont({
  src: [
    { path: '../node_modules/@fontsource/ibm-plex-mono/files/ibm-plex-mono-latin-400-normal.woff2', weight: '400' },
    { path: '../node_modules/@fontsource/ibm-plex-mono/files/ibm-plex-mono-latin-500-normal.woff2', weight: '500' },
    { path: '../node_modules/@fontsource/ibm-plex-mono/files/ibm-plex-mono-latin-600-normal.woff2', weight: '600' },
  ],
  variable: '--font-ibm-plex-mono',
  display: 'swap',
})

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
    <html lang="pt-BR" className={`${inter.variable} ${interTight.variable} ${manrope.variable} ${plexMono.variable}`}>
      <body className="bg-background font-sans text-foreground antialiased"><Providers>{children}</Providers></body>
    </html>
  )
}
