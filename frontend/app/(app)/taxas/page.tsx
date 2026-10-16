import { Suspense } from 'react'
import { FeeRulesPage } from '@/components/fee-rules/FeeRulesPage'

export default function TaxasPage() {
  // useSearchParams needs a Suspense boundary for the static build.
  return <Suspense><FeeRulesPage /></Suspense>
}
