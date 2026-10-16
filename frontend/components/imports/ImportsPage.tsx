'use client'

import { useRouter, useSearchParams } from 'next/navigation'
import { NoMerchant } from '@/components/dashboard/states'
import { useSelectedMerchant } from '@/components/shell/MerchantMenu'
import { IMPORT_KINDS, kindFromSlug } from '@/lib/imports'
import { cn } from '@/lib/utils'
import { ImportPanel } from './ImportPanel'

/** `/importacoes`: the three CSV imports of the merchant selected in the top menu, one tab per kind, kept in `?tipo=`. */
export function ImportsPage() {
  const { merchant, merchantId, isLoading: merchantsLoading, error: merchantsError } = useSelectedMerchant()
  const router = useRouter()
  const kind = kindFromSlug(useSearchParams()?.get('tipo') ?? null)
  const info = IMPORT_KINDS.find(item => item.kind === kind)!
  // While the top menu has not loaded (or failed), the page waits, as the other merchant pages do.
  const noMerchant = !merchantsLoading && !merchantsError && merchantId === null

  return (
    <>
      <section className="flex min-w-0 flex-col gap-2">
        <p className="truncate text-[14px] text-muted">{merchant?.name}</p>
        <h1 className="font-display text-[44px] leading-none font-extrabold tracking-[-0.04em]">Importações</h1>
      </section>

      {noMerchant && <NoMerchant />}

      {merchantId !== null && (
        <>
          <div role="tablist" aria-label="Tipo de arquivo" className="flex w-fit gap-1 rounded-full bg-surface p-1">
            {IMPORT_KINDS.map(item => {
              const selected = item.kind === kind
              return (
                <button
                  key={item.kind}
                  type="button"
                  role="tab"
                  id={`import-tab-${item.slug}`}
                  aria-selected={selected}
                  aria-controls={`import-panel-${item.slug}`}
                  onClick={() => router.replace(`/importacoes?tipo=${item.slug}`)}
                  className={cn('h-9 rounded-full px-4 text-[13px] font-semibold', selected ? 'bg-foreground text-white' : 'text-muted hover:text-foreground')}
                >
                  {item.label}
                </button>
              )
            })}
          </div>
          {/* A new key per merchant and kind drops the chosen file, the last outcome and the list page. */}
          <ImportPanel key={`${merchantId}:${kind}`} merchantId={merchantId} info={info} />
        </>
      )}
    </>
  )
}
