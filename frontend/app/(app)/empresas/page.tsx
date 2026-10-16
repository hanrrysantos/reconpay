import { Suspense } from 'react'
import { MerchantsPage } from '@/components/merchants/MerchantsPage'

export default function EmpresasPage() {
  // useSearchParams needs a Suspense boundary for the static build.
  return <Suspense><MerchantsPage /></Suspense>
}
