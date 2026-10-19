'use client'

import { useSearchParams } from 'next/navigation'
import { Suspense } from 'react'
import { VerifyEmail } from '@/components/auth/VerifyEmail'

function VerifyEmailFromUrl() {
  return <VerifyEmail token={useSearchParams().get('token')} />
}

export default function VerifyEmailPage() {
  return (
    <Suspense fallback={null}>
      <VerifyEmailFromUrl />
    </Suspense>
  )
}
