'use client'

import { Plus } from 'lucide-react'
import { useState } from 'react'
import { NoMerchant } from '@/components/dashboard/states'
import { useSelectedMerchant } from '@/components/shell/MerchantMenu'
import { formatWindow, monthWindow, type Window } from '@/lib/period'
import { recentMonths } from '@/lib/transactions'
import { NewRunDrawer } from './NewRunDrawer'
import { RunList } from './RunList'

const select = 'h-11 rounded-full bg-surface px-4 text-[14px] font-semibold outline-none focus:shadow-[inset_0_0_0_2px_var(--color-foreground)]'

/**
 * `/conciliacoes`: the run history of the merchant selected in the top menu, optionally filtered by one month
 * (kept in the page state, not in the URL). New runs start from a drawer.
 */
export function ReconciliationsPage() {
  const { merchant, merchantId, isLoading: merchantsLoading, error: merchantsError } = useSelectedMerchant()
  const [months] = useState(() => recentMonths(new Date()))
  const [window, setWindow] = useState<Window | null>(null)
  const [page, setPage] = useState(0)
  const [drawerOpen, setDrawerOpen] = useState(false)
  // Switching merchants closes the drawer and goes back to page 1; the window filter stays.
  const [shownMerchantId, setShownMerchantId] = useState(merchantId)
  if (shownMerchantId !== merchantId) {
    setShownMerchantId(merchantId)
    setDrawerOpen(false)
    setPage(0)
  }
  // While the top menu has not loaded (or failed), the page waits, as the other merchant pages do.
  const noMerchant = !merchantsLoading && !merchantsError && merchantId === null

  function chooseWindow(fromDate: string) {
    setWindow(fromDate === '' ? null : monthWindow(fromDate))
    setPage(0)
  }

  function onStarted() {
    setDrawerOpen(false)
    setWindow(null)
    setPage(0)
  }

  return (
    <>
      <section className="flex flex-wrap items-end justify-between gap-6">
        <div className="flex min-w-0 flex-col gap-2">
          <p className="truncate text-[14px] text-muted">{merchant?.name}</p>
          <h1 className="font-display text-[44px] leading-none font-extrabold tracking-[-0.04em]">Conciliações</h1>
        </div>
        {merchantId !== null && (
          <button type="button" onClick={() => setDrawerOpen(true)} className="flex h-11 items-center gap-2 rounded-full bg-primary px-5 text-[14px] font-semibold text-foreground">
            <Plus aria-hidden="true" className="size-4" />
            Nova conciliação
          </button>
        )}
      </section>

      {noMerchant && <NoMerchant />}

      {merchantId !== null && (
        <>
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2">
              <label htmlFor="run-window-filter" className="text-[13px] text-muted">Janela</label>
              <select id="run-window-filter" value={window?.fromDate ?? ''} onChange={event => chooseWindow(event.target.value)} className={select}>
                <option value="">Todas</option>
                {months.map(month => <option key={month.fromDate} value={month.fromDate}>{formatWindow(month)}</option>)}
              </select>
            </div>
          </div>

          <RunList key={merchantId} merchantId={merchantId} window={window} page={page} onPageChange={setPage} />

          {drawerOpen && <NewRunDrawer merchantId={merchantId} onClose={() => setDrawerOpen(false)} onStarted={onStarted} />}
        </>
      )}
    </>
  )
}
