'use client'

import { useEffect, useRef, useState, type FormEvent } from 'react'
import { NAME_LIMIT, NAME_TOO_LONG_MESSAGE } from '@/components/merchants/MerchantDrawer'
import { SideDrawer } from '@/components/ui/SideDrawer'
import type { UserRole } from '@/lib/api'
import { PASSWORD_PATTERN } from '@/lib/password'
import { useCreateUser } from '@/lib/queries'

export const NAME_REQUIRED_MESSAGE = 'Informe o nome'
export const INVALID_EMAIL_MESSAGE = 'Informe um e-mail válido'
/** Same text the API answers for a weak password. */
export const WEAK_PASSWORD_MESSAGE = 'Senha deve ter no mínimo 8 caracteres, uma letra maiúscula e um número'

const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível salvar.'
const strongPassword = new RegExp(`^${PASSWORD_PATTERN}$`)

export function userNameError(name: string): string | null {
  const value = name.trim()
  if (!value) return NAME_REQUIRED_MESSAGE
  return value.length > NAME_LIMIT ? NAME_TOO_LONG_MESSAGE : null
}

const field = 'h-12 rounded-md bg-surface px-3 text-[14px] outline-none focus:shadow-[inset_0_0_0_2px_var(--color-foreground)] aria-invalid:shadow-[inset_0_0_0_2px_var(--color-danger)]'
const ROLES: Array<[UserRole, string]> = [['OPERATOR', 'Operador'], ['ADMIN', 'Admin']]

type Errors = { name?: string | null; email?: string | null; password?: string | null }

/** Side drawer that creates an account, checked here before the API is called; the account is born active. */
export function UserDrawer({ onClose, onDone }: { onClose: () => void; onDone: (message: string) => void }) {
  const create = useCreateUser()
  const nameInput = useRef<HTMLInputElement>(null)
  const sending = useRef(false)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<UserRole>('OPERATOR')
  const [errors, setErrors] = useState<Errors>({})
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  useEffect(() => { nameInput.current?.focus() }, [])

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (sending.current) return
    const invalid: Errors = {
      name: userNameError(name),
      email: /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()) ? null : INVALID_EMAIL_MESSAGE,
      password: strongPassword.test(password) ? null : WEAK_PASSWORD_MESSAGE,
    }
    setErrors(invalid)
    if (invalid.name || invalid.email || invalid.password) return

    sending.current = true
    setPending(true)
    setError(null)
    try {
      await create.mutateAsync({ name: name.trim(), email: email.trim(), password, role })
      onDone('Usuário criado.')
    } catch (failure) {
      setError(messageOf(failure))
    } finally {
      sending.current = false
      setPending(false)
    }
  }

  const input = (id: keyof Errors, label: string, value: string, onChange: (value: string) => void, type = 'text') => (
    <div className="flex flex-col gap-2">
      <label htmlFor={`user-${id}`} className="text-[13px] font-semibold">{label}</label>
      <input
        ref={id === 'name' ? nameInput : undefined}
        id={`user-${id}`}
        type={type}
        value={value}
        onChange={event => onChange(event.target.value)}
        aria-invalid={Boolean(errors[id])}
        aria-describedby={errors[id] ? `user-${id}-error` : undefined}
        className={field}
      />
      {errors[id] && <p id={`user-${id}-error`} className="text-[12px] text-danger">{errors[id]}</p>}
    </div>
  )

  return (
    <SideDrawer
      title="Novo usuário"
      description="A conta nasce ativa, sem confirmação por e-mail."
      formId="user-form"
      submitLabel="Criar"
      pending={pending}
      error={error}
      onSubmit={event => void submit(event)}
      onClose={onClose}
    >
      {input('name', 'Nome', name, setName)}
      {input('email', 'E-mail', email, setEmail, 'email')}
      {input('password', 'Senha', password, setPassword, 'password')}
      <fieldset className="flex flex-col gap-2">
        <legend className="mb-2 text-[13px] font-semibold">Papel</legend>
        {ROLES.map(([value, label]) => (
          <label key={value} className="flex items-center gap-2 text-[14px]">
            <input type="radio" name="user-role" value={value} checked={role === value} onChange={() => setRole(value)} className="size-4 accent-foreground" />
            {label}
          </label>
        ))}
      </fieldset>
    </SideDrawer>
  )
}
