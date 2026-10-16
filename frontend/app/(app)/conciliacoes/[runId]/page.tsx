'use client'

import { useParams } from 'next/navigation'
import { RunDetailPage } from '@/components/reconciliations/RunDetailPage'

export default function ConciliacaoPage() {
  const { runId } = useParams<{ runId: string }>()
  return <RunDetailPage runId={runId} />
}
