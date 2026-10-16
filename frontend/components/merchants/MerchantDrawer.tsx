'use client'

import { useEffect, useRef, useState, type FormEvent } from 'react'
import { SideDrawer } from '@/components/ui/SideDrawer'
import { ApiError, type Merchant, type MerchantRecord } from '@/lib/api'
import { cnpjDigits, formatCnpj, isValidCnpj, maskCnpj } from '@/lib/cnpj'
import { useCreateMerchant, useInvalidateMerchants, useRenameMerchant } from '@/lib/queries'
import { cn } from '@/lib/utils'

export const NAME_LIMIT = 150
export const NAME_REQUIRED_MESSAGE = 'Informe o nome da empresa'
export const NAME_TOO_LONG_MESSAGE = `Use no máximo ${NAME_LIMIT} caracteres`
export const INVALID_CNPJ_MESSAGE = 'CNPJ inválido'

const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível salvar.'
/** 403 or 404: the merchant left the user's access or was deactivated elsewhere, so the lists are stale. */
export const isGone = (error: unknown) => error instanceof ApiError && (error.status === 403 || error.status === 404)

export function nameError(name: string): string | null {
  const value = name.trim()
  if (!value) return NAME_REQUIRED_MESSAGE
  return value.length > NAME_LIMIT ? NAME_TOO_LONG_MESSAGE : null
}

const field = 'h-12 rounded-md bg-surface px-3 text-[14px] outline-none focus:shadow-[inset_0_0_0_2px_var(--color-foreground)] aria-invalid:shadow-[inset_0_0_0_2px_var(--color-danger)]'

/**
 * Side drawer checked here before the API is called. Without `merchant` it registers one (name and masked CNPJ);
 * with `merchant` it renames it, and the CNPJ is read-only.
 */
export function MerchantDrawer({ merchant, onClose, onCreated }: { merchant?: Merchant; onClose: () => void; onCreated?: (merchant: MerchantRecord) => void }) {
  const editing = merchant !== undefined
  const create = useCreateMerchant()
  const rename = useRenameMerchant()
  const reloadLists = useInvalidateMerchants()
  const nameInput = useRef<HTMLInputElement>(null)
  const sending = useRef(false)
  const [name, setName] = useState(merchant?.name ?? '')
  const [cnpj, setCnpj] = useState('')
  const [errors, setErrors] = useState<{ name?: string | null; cnpj?: string | null }>({})
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  useEffect(() => { nameInput.current?.focus() }, [])

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (sending.current) return
    const invalid = { name: nameError(name), cnpj: editing || isValidCnpj(cnpj) ? null : INVALID_CNPJ_MESSAGE }
    setErrors(invalid)
    if (invalid.name || invalid.cnpj) return

    sending.current = true
    setPending(true)
    setError(null)
    try {
      if (editing) await rename.mutateAsync({ merchantId: merchant.id, name: name.trim() })
      else onCreated?.(await create.mutateAsync({ name: name.trim(), document: cnpjDigits(cnpj) }))
      onClose()
    } catch (failure) {
      setError(messageOf(failure))
      if (isGone(failure)) await reloadLists()
    } finally {
      sending.current = false
      setPending(false)
    }
  }

  return (
    <SideDrawer
      title={editing ? 'Editar empresa' : 'Cadastrar empresa'}
      description={editing ? 'Só o nome pode mudar.' : 'Ela passa a ser a empresa selecionada no menu.'}
      formId="merchant-form"
      submitLabel={editing ? 'Salvar' : 'Cadastrar'}
      pending={pending}
      error={error}
      onSubmit={event => void submit(event)}
      onClose={onClose}
    >
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
          readOnly={editing}
          value={editing ? formatCnpj(merchant.document) : cnpj}
          onChange={event => setCnpj(maskCnpj(event.target.value))}
          aria-invalid={Boolean(errors.cnpj)}
          aria-describedby={errors.cnpj ? 'merchant-cnpj-error' : undefined}
          className={cn(field, 'font-mono placeholder:text-muted', editing && 'text-muted')}
        />
        {errors.cnpj && <p id="merchant-cnpj-error" className="text-[12px] text-danger">{errors.cnpj}</p>}
      </div>
    </SideDrawer>
  )
}
