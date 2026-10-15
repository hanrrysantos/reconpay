'use client'

import { Check, X } from 'lucide-react'
import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { cnpjDigits, isValidCnpj, maskCnpj } from '@/lib/cnpj'
import { useCreateMerchant } from '@/lib/queries'
import { cn } from '@/lib/utils'

export const NAME_LIMIT = 150
export const NAME_REQUIRED_MESSAGE = 'Informe o nome da empresa'
export const NAME_TOO_LONG_MESSAGE = `Use no máximo ${NAME_LIMIT} caracteres`
export const INVALID_CNPJ_MESSAGE = 'CNPJ inválido'

const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível salvar.'

export function nameError(name: string): string | null {
  const value = name.trim()
  if (!value) return NAME_REQUIRED_MESSAGE
  return value.length > NAME_LIMIT ? NAME_TOO_LONG_MESSAGE : null
}

const field = 'h-12 rounded-md bg-surface px-3 text-[14px] outline-none focus:shadow-[inset_0_0_0_2px_var(--color-foreground)] aria-invalid:shadow-[inset_0_0_0_2px_var(--color-danger)]'

/** Side drawer to register a merchant: name and masked CNPJ, checked here before the API is called. */
export function MerchantDrawer({ onClose }: { onClose: () => void }) {
  const create = useCreateMerchant()
  const panel = useRef<HTMLDivElement>(null)
  const nameInput = useRef<HTMLInputElement>(null)
  const sending = useRef(false)
  const [name, setName] = useState('')
  const [cnpj, setCnpj] = useState('')
  const [errors, setErrors] = useState<{ name?: string | null; cnpj?: string | null }>({})
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  useEffect(() => { nameInput.current?.focus() }, [])

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (sending.current) return
    const invalid = { name: nameError(name), cnpj: isValidCnpj(cnpj) ? null : INVALID_CNPJ_MESSAGE }
    setErrors(invalid)
    if (invalid.name || invalid.cnpj) return

    sending.current = true
    setPending(true)
    setError(null)
    try {
      await create.mutateAsync({ name: name.trim(), document: cnpjDigits(cnpj) })
      onClose()
    } catch (failure) {
      setError(messageOf(failure))
    } finally {
      sending.current = false
      setPending(false)
    }
  }

  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Escape') { event.preventDefault(); onClose(); return }
    if (event.key !== 'Tab') return
    const focusable = Array.from(panel.current?.querySelectorAll<HTMLElement>('button:not([disabled]), input:not([readonly])') ?? [])
    if (focusable.length === 0) return
    const first = focusable[0]
    const last = focusable[focusable.length - 1]
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus() }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
  }

  return (
    <div className="fixed inset-0 z-40 bg-[rgba(11,13,10,0.42)]" onClick={onClose}>
      <div
        ref={panel}
        role="dialog"
        aria-modal="true"
        aria-label="Cadastrar empresa"
        tabIndex={-1}
        onKeyDown={onKeyDown}
        onClick={event => event.stopPropagation()}
        className="absolute top-4 right-4 bottom-4 flex w-[480px] max-w-[calc(100%-32px)] flex-col rounded-[28px] bg-white shadow-[0_24px_64px_rgba(0,0,0,0.24)] outline-none"
      >
        <header className="flex items-start gap-3 p-6 pb-4">
          <div className="flex flex-col gap-1">
            <h2 className="font-display text-[22px] font-extrabold tracking-[-0.03em]">Cadastrar empresa</h2>
            <p className="text-[13px] text-muted">Ela passa a ser a empresa selecionada no menu.</p>
          </div>
          <button type="button" aria-label="Fechar" onClick={onClose} className="ml-auto grid size-9 shrink-0 place-items-center rounded-full bg-surface">
            <X aria-hidden="true" className="size-4" />
          </button>
        </header>

        <form id="merchant-form" onSubmit={event => void submit(event)} noValidate className="flex flex-1 flex-col gap-5 overflow-y-auto px-6 pb-6">
          <div className="flex flex-col gap-2">
            <label htmlFor="merchant-name" className="text-[13px] font-semibold">Nome</label>
            <input
              ref={nameInput}
              id="merchant-name"
              value={name}
              onChange={event => setName(event.target.value)}
              aria-invalid={Boolean(errors.name)}
              aria-describedby={errors.name ? 'merchant-name-error' : undefined}
              className={field}
            />
            {errors.name && <p id="merchant-name-error" className="text-[12px] text-danger">{errors.name}</p>}
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="merchant-cnpj" className="text-[13px] font-semibold">CNPJ</label>
            <input
              id="merchant-cnpj"
              inputMode="numeric"
              placeholder="00.000.000/0000-00"
              value={cnpj}
              onChange={event => setCnpj(maskCnpj(event.target.value))}
              aria-invalid={Boolean(errors.cnpj)}
              aria-describedby={errors.cnpj ? 'merchant-cnpj-error' : undefined}
              className={cn(field, 'font-mono placeholder:text-muted')}
            />
            {errors.cnpj && <p id="merchant-cnpj-error" className="text-[12px] text-danger">{errors.cnpj}</p>}
          </div>

          {error && <p role="alert" className="rounded-md bg-danger-soft px-4 py-3 text-[13px] text-danger-strong">{error}</p>}
        </form>

        <footer className="flex items-center justify-end gap-2 border-t border-border px-6 py-4">
          <button type="button" onClick={onClose} className="h-10 rounded-full bg-surface px-4 text-[14px] font-semibold">Cancelar</button>
          <button type="submit" form="merchant-form" disabled={pending} className="flex h-10 items-center gap-2 rounded-full bg-primary px-4 text-[14px] font-semibold disabled:bg-surface disabled:text-muted">
            Cadastrar <Check aria-hidden="true" className="size-4" />
          </button>
        </footer>
      </div>
    </div>
  )
}
