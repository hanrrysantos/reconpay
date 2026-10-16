'use client'

import { Plus } from 'lucide-react'
import { useState } from 'react'
import { METHOD_LABELS } from '@/components/dashboard/format'
import { BlockError, NoMerchant } from '@/components/dashboard/states'
import { useSelectedMerchant } from '@/components/shell/MerchantMenu'
import { formatPercent } from '@/lib/fee'
import { formatMoney } from '@/lib/money'
import { useFeeRules } from '@/lib/queries'
import { FeeRuleDrawer } from './FeeRuleDrawer'

const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível carregar as regras de taxa.'
const createButton = 'flex h-11 items-center gap-2 rounded-full bg-primary px-5 text-[14px] font-semibold text-foreground'

function TableSkeleton() {
  return (
    <div role="status" aria-label="Carregando regras de taxa" aria-busy="true" className="flex animate-pulse flex-col gap-2 rounded-lg bg-surface p-4 [animation-duration:1.4s]">
      {[0, 1, 2].map(row => <div key={row} className="h-12 rounded-sm bg-[#EBEDE6]" />)}
      <span className="sr-only">Carregando…</span>
    </div>
  )
}

function NoRules({ onCreate }: { onCreate: () => void }) {
  return (
    <section className="flex flex-col items-start gap-5 rounded-lg bg-foreground p-7 text-white lg:w-[420px]">
      <h2 className="font-display text-[30px] leading-tight font-extrabold tracking-[-0.03em]">Nenhuma regra de taxa</h2>
      <p className="text-[13px] text-white/75">Transações só entram com uma regra ativa para o método e as parcelas.</p>
      <button type="button" onClick={onCreate} className={createButton}>
        <Plus aria-hidden="true" className="size-4" />
        Cadastrar regra
      </button>
    </section>
  )
}

/** `/taxas`: the active fee rules of the merchant selected in the top menu, by payment method and installments. */
export function FeeRulesPage() {
  const { merchant, merchantId, isLoading: merchantsLoading, error: merchantsError } = useSelectedMerchant()
  const list = useFeeRules(merchantId)
  const [creating, setCreating] = useState(false)
  // Switching merchants in the menu closes the drawer before it can render for the new merchant.
  const [shownMerchantId, setShownMerchantId] = useState(merchantId)
  if (shownMerchantId !== merchantId) {
    setShownMerchantId(merchantId)
    setCreating(false)
  }
  const openDrawer = () => setCreating(true)
  const rules = list.data ?? []
  const empty = list.data !== undefined && rules.length === 0
  // While the top menu has not loaded (or failed), the page keeps the loading state, as the dashboard does.
  const noMerchant = !merchantsLoading && !merchantsError && merchantId === null

  return (
    <>
      <section className="flex flex-wrap items-end justify-between gap-6">
        <div className="flex min-w-0 flex-col gap-2">
          <p className="truncate text-[14px] text-muted">{merchant?.name}</p>
          <h1 className="font-display text-[44px] leading-none font-extrabold tracking-[-0.04em]">Regras de taxa</h1>
        </div>
        {list.data && !empty && (
          <button type="button" onClick={openDrawer} className={createButton}>
            <Plus aria-hidden="true" className="size-4" />
            Cadastrar regra
          </button>
        )}
      </section>

      {noMerchant && <NoMerchant />}
      {!noMerchant && list.isPending && <TableSkeleton />}
      {list.isError && <BlockError message={messageOf(list.error)} action="Tentar de novo" onRetry={() => void list.refetch()} />}
      {empty && <NoRules onCreate={openDrawer} />}

      {list.data && !empty && (
        <section className="flex flex-col gap-2 rounded-lg bg-surface p-2">
          <div className="overflow-x-auto rounded-[18px] bg-white p-1.5">
            <table aria-label="Regras de taxa" className="w-full min-w-[560px] border-separate border-spacing-y-1 text-left">
              <thead className="text-[12px] text-muted">
                <tr>
                  <th scope="col" className="px-3 font-normal">Método</th>
                  <th scope="col" className="w-[120px] px-3 font-normal">Parcelas</th>
                  <th scope="col" className="w-[140px] px-3 font-normal">Percentual</th>
                  <th scope="col" className="w-[140px] px-3 font-normal">Taxa fixa</th>
                </tr>
              </thead>
              <tbody>
                {rules.map(rule => (
                  <tr key={rule.id} className="group">
                    <td className="rounded-l-[14px] px-3 py-2 text-[14px] font-semibold group-hover:bg-subtle">{METHOD_LABELS[rule.paymentMethod]?.label ?? rule.paymentMethod}</td>
                    <td className="px-3 py-2 font-mono text-[13px] group-hover:bg-subtle">{rule.installments}x</td>
                    <td className="px-3 py-2 font-mono text-[13px] group-hover:bg-subtle">{formatPercent(rule.feePercentage)}</td>
                    <td className="rounded-r-[14px] px-3 py-2 font-mono text-[13px] group-hover:bg-subtle">{formatMoney(rule.fixedFee)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {creating && merchantId !== null && <FeeRuleDrawer merchantId={merchantId} onClose={() => setCreating(false)} />}
    </>
  )
}
