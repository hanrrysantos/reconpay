'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { DashboardView } from '@/components/dashboard'
import { ApiError, getMerchants, getTransactions, type Merchant, type Transaction } from '@/lib/api'
import { useAuth } from '@/lib/auth'

export default function Home() {
  const router = useRouter()
  const { token, user, signOut } = useAuth()
  const [merchants, setMerchants] = useState<Merchant[]>([])
  const [selectedMerchantId, setSelectedMerchantId] = useState<string | null>(null)
  const [transactionResult, setTransactionResult] = useState<{ merchantId: string; items: Transaction[]; error: string | null } | null>(null)
  const [loadingMerchants, setLoadingMerchants] = useState(false)
  const [merchantError, setMerchantError] = useState<string | null>(null)
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    if (!token) {
      router.replace('/login')
      return
    }
    let active = true
    setLoadingMerchants(true)
    setMerchantError(null)
    getMerchants(token).then((items) => {
      if (!active) return
      setMerchants(items)
      setSelectedMerchantId((current) => items.some((item) => item.id === current) ? current : (items[0]?.id ?? null))
    }).catch((error: unknown) => {
      if (!active) return
      if (error instanceof ApiError && error.status === 401) {
        signOut()
        router.replace('/login')
      } else {
        setMerchantError(error instanceof Error ? error.message : 'Não foi possível carregar as empresas.')
      }
    }).finally(() => { if (active) setLoadingMerchants(false) })
    return () => { active = false }
  }, [token, retry, router, signOut])

  useEffect(() => {
    if (!token || !selectedMerchantId) {
      setTransactionResult(null)
      return
    }
    let active = true
    setTransactionResult(null)
    getTransactions(token, selectedMerchantId).then((items) => {
      if (active) setTransactionResult({ merchantId: selectedMerchantId, items, error: null })
    }).catch((error: unknown) => {
      if (!active) return
      if (error instanceof ApiError && error.status === 401) {
        signOut()
        router.replace('/login')
      } else {
        setTransactionResult({ merchantId: selectedMerchantId, items: [], error: error instanceof Error ? error.message : 'Não foi possível carregar as transações.' })
      }
    })
    return () => { active = false }
  }, [token, selectedMerchantId, retry, router, signOut])

  if (!token || !user) return <p className="grid min-h-screen place-items-center" role="status">Abrindo login...</p>

  const currentTransactions = transactionResult?.merchantId === selectedMerchantId ? transactionResult : null

  return <DashboardView
    user={user}
    merchants={merchants}
    selectedMerchantId={selectedMerchantId}
    onSelectMerchant={setSelectedMerchantId}
    onLogout={() => { signOut(); router.replace('/login') }}
    transactions={currentTransactions?.items ?? []}
    isLoading={loadingMerchants || (selectedMerchantId !== null && currentTransactions === null)}
    error={merchantError ?? currentTransactions?.error ?? null}
    onRetry={() => { setTransactionResult(null); setRetry((current) => current + 1) }}
  />
}
