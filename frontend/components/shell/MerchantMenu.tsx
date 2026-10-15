'use client'

import { useQuery } from '@tanstack/react-query'
import { Check, ChevronDown, Plus, Search } from 'lucide-react'
import Link from 'next/link'
import { createContext, useContext, useEffect, useId, useRef, useState, type ReactNode } from 'react'
import { getMerchants, type Merchant } from '@/lib/api'
import { cnpjDigits, formatCnpj } from '@/lib/cnpj'
import { useAuth } from '@/lib/auth'
import { cn } from '@/lib/utils'

type SelectedMerchantValue = {
  merchants: Merchant[]
  merchantId: string | null
  merchant: Merchant | null
  isLoading: boolean
  error: unknown
  select: (merchantId: string) => void
}

const SelectedMerchantContext = createContext<SelectedMerchantValue | null>(null)

/** Loads every page of `/api/me/merchants` and keeps the selected one; the first merchant is the default. */
export function SelectedMerchantProvider({ children }: { children: ReactNode }) {
  const { token, user } = useAuth()
  const query = useQuery({
    queryKey: ['merchants', user?.id ?? ''],
    queryFn: () => getMerchants(token!),
    enabled: Boolean(token),
  })
  const [chosenId, setChosenId] = useState<string | null>(null)
  const merchants = query.data ?? []
  const merchant = merchants.find(item => item.id === chosenId) ?? merchants[0] ?? null

  return (
    <SelectedMerchantContext.Provider value={{
      merchants,
      merchantId: merchant?.id ?? null,
      merchant,
      isLoading: query.isPending && query.fetchStatus !== 'idle',
      error: query.error,
      select: setChosenId,
    }}>
      {children}
    </SelectedMerchantContext.Provider>
  )
}

export function useSelectedMerchant(): SelectedMerchantValue {
  const context = useContext(SelectedMerchantContext)
  if (!context) throw new Error('useSelectedMerchant deve ser usado dentro de SelectedMerchantProvider')
  return context
}

const fold = (value: string) => value.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()

/** A query made only of digits and CNPJ punctuation searches the CNPJ; any other text searches the name. */
function matches(merchant: Merchant, query: string): boolean {
  const text = query.trim()
  if (!text) return true
  if (/^[\d.\-/\s]+$/.test(text)) return cnpjDigits(merchant.document).includes(cnpjDigits(text))
  return fold(merchant.name).includes(fold(text))
}

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase()
}

export function MerchantMenu({ merchants, selectedId, onSelect }: {
  merchants: Merchant[]
  selectedId: string | null
  onSelect: (merchantId: string) => void
}) {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const root = useRef<HTMLDivElement>(null)
  const listId = useId()
  const selected = merchants.find(merchant => merchant.id === selectedId) ?? null
  const visible = merchants.filter(merchant => matches(merchant, query))

  useEffect(() => {
    if (!open) return
    const close = (event: MouseEvent) => { if (!root.current?.contains(event.target as Node)) setOpen(false) }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [open])

  function choose(merchantId: string) {
    onSelect(merchantId)
    setOpen(false)
    setQuery('')
  }

  return (
    <div ref={root} className="relative" onKeyDown={event => { if (event.key === 'Escape') setOpen(false) }}>
      <button
        type="button"
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => setOpen(current => !current)}
        className="flex h-10 items-center gap-2.5 rounded-full bg-surface py-1.5 pr-1.5 pl-4 text-[14px] font-semibold"
      >
        <span className="max-w-48 truncate">{selected?.name ?? 'Nenhuma empresa'}</span>
        <span className="grid size-7 place-items-center rounded-full bg-white"><ChevronDown aria-hidden="true" className="size-4" /></span>
      </button>

      {open && (
        <div className="absolute top-12 right-0 z-30 flex w-[340px] flex-col gap-1 rounded-[20px] bg-white p-2 shadow-[0_0_0_1px_var(--color-border),0_20px_48px_rgba(11,13,10,0.14)]">
          <label className="flex h-10 items-center gap-2 rounded-sm bg-surface px-3 text-[14px]">
            <Search aria-hidden="true" className="size-4 text-muted" />
            <input
              autoFocus
              value={query}
              onChange={event => setQuery(event.target.value)}
              placeholder="Buscar empresa ou CNPJ"
              aria-controls={listId}
              className="w-full bg-transparent outline-none placeholder:text-muted"
            />
          </label>

          <ul id={listId} role="listbox" aria-label="Empresas" className="flex max-h-80 flex-col gap-0.5 overflow-y-auto">
            {visible.map(merchant => {
              const isSelected = merchant.id === selectedId
              return (
                <li
                  key={merchant.id}
                  role="option"
                  aria-selected={isSelected}
                  tabIndex={0}
                  onClick={() => choose(merchant.id)}
                  onKeyDown={event => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); choose(merchant.id) } }}
                  className={cn('flex cursor-pointer items-center gap-3 rounded-[14px] p-2 hover:bg-subtle', isSelected && 'bg-surface hover:bg-surface')}
                >
                  <span className="grid size-8 shrink-0 place-items-center rounded-sm bg-foreground font-display text-[12px] font-extrabold text-primary">
                    {initials(merchant.name)}
                  </span>
                  <span className="flex min-w-0 flex-1 flex-col">
                    <span className="truncate text-[14px] font-semibold">{merchant.name}</span>
                    <span className="font-mono text-[12px] text-muted">{formatCnpj(merchant.document)}</span>
                  </span>
                  {isSelected && <Check aria-hidden="true" className="size-4 shrink-0" />}
                </li>
              )
            })}
          </ul>
          {visible.length === 0 && <p className="p-3 text-[13px] text-muted">Nenhuma empresa encontrada</p>}

          <Link
            href="/empresas"
            onClick={() => setOpen(false)}
            className="flex items-center gap-2 rounded-[14px] border-t border-border p-3 text-[14px] font-semibold hover:bg-subtle"
          >
            <Plus aria-hidden="true" className="size-4" />
            Cadastrar empresa
          </Link>
        </div>
      )}
    </div>
  )
}

/** Merchant menu bound to the selected-merchant context. */
export function MerchantSwitcher() {
  const { merchants, merchantId, select } = useSelectedMerchant()
  return <MerchantMenu merchants={merchants} selectedId={merchantId} onSelect={select} />
}
