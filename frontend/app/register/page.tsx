'use client'

import { ArrowRight, Check, CircleAlert, Eye, EyeOff, MailCheck } from 'lucide-react'
import Link from 'next/link'
import { useState, type FormEvent } from 'react'
import { AuthPanel, authAlert, authField, authPrimaryButton } from '@/components/auth/AuthPanel'
import { register } from '@/lib/api'
import { PASSWORD_PATTERN, PASSWORD_RULES } from '@/lib/password'

export default function RegisterPage() {
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [registeredEmail, setRegisteredEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy) return
    const form = new FormData(event.currentTarget)
    const email = String(form.get('email'))
    setError('')
    setBusy(true)
    try {
      await register({ name: String(form.get('name')), email, password: String(form.get('password')) })
      setRegisteredEmail(email)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Não foi possível criar a conta. Tente novamente.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex min-h-dvh flex-col bg-background text-foreground lg:flex-row">
      <AuthPanel
        title="Seu primeiro fechamento começa aqui."
        lead="Crie a conta, cadastre a empresa e importe os arquivos do mês. A conta é ativada pelo link que enviamos por e-mail."
      />

      <main className="flex flex-1 justify-center px-6 py-8 lg:items-center lg:p-14">
        <div className="flex w-full max-w-[400px] flex-col gap-7 lg:gap-8">
          {registeredEmail ? (
            <div role="status" className="flex flex-col gap-6">
              <span className="grid size-14 place-items-center rounded-full bg-primary">
                <MailCheck aria-hidden="true" className="size-[26px]" />
              </span>
              <div className="flex flex-col gap-3">
                <h1 className="font-display text-[36px] leading-none font-extrabold tracking-[-0.04em] lg:text-[40px]">Confira seu e-mail</h1>
                <p className="text-[15px] leading-[1.55] text-muted">
                  Enviamos um link de verificação para <strong className="font-semibold text-foreground">{registeredEmail}</strong>. Abra o e-mail e confirme a conta antes de entrar.
                </p>
              </div>
              <Link href="/login" className="flex h-[52px] items-center justify-center gap-2 rounded-full bg-foreground text-[15px] font-bold text-background">
                Ir para o login
                <ArrowRight aria-hidden="true" className="size-[18px]" />
              </Link>
              <p className="text-[13px] text-muted">Não chegou? Confira a caixa de spam.</p>
            </div>
          ) : (
            <>
              <div className="flex flex-col gap-2.5">
                <h1 className="font-display text-[36px] leading-none font-extrabold tracking-[-0.04em] lg:text-[44px]">Criar conta</h1>
                <p className="text-[15px] leading-[1.5] text-muted">Você confirma o e-mail antes de entrar.</p>
              </div>

              <form className="flex flex-col gap-[18px]" onSubmit={submit}>
                <div className="flex flex-col gap-2">
                  <label htmlFor="register-name" className="text-[13px] font-semibold">Nome completo</label>
                  <input id="register-name" name="name" type="text" autoComplete="name" placeholder="Seu nome" required className={authField} />
                </div>
                <div className="flex flex-col gap-2">
                  <label htmlFor="register-email" className="text-[13px] font-semibold">E-mail</label>
                  <input id="register-email" name="email" type="email" autoComplete="email" placeholder="voce@empresa.com" required className={authField} />
                </div>
                <div className="flex flex-col gap-2">
                  <label htmlFor="register-password" className="text-[13px] font-semibold">Senha</label>
                  <div className="relative">
                    <input
                      id="register-password"
                      name="password"
                      type={showPassword ? 'text' : 'password'}
                      autoComplete="new-password"
                      placeholder="Crie uma senha"
                      minLength={8}
                      pattern={PASSWORD_PATTERN}
                      title="Use pelo menos 8 caracteres, uma letra maiúscula e um número"
                      required
                      aria-describedby="register-password-rules"
                      value={password}
                      onChange={event => setPassword(event.target.value)}
                      className={`${authField} pr-12`}
                    />
                    <button
                      type="button"
                      aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}
                      aria-pressed={showPassword}
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute top-1/2 right-3 grid size-8 -translate-y-1/2 place-items-center rounded-full text-muted hover:text-foreground"
                    >
                      {showPassword ? <EyeOff aria-hidden="true" className="size-[18px]" /> : <Eye aria-hidden="true" className="size-[18px]" />}
                    </button>
                  </div>
                  <ul id="register-password-rules" aria-label="Regras da senha" className="mt-1 flex flex-wrap gap-x-4 gap-y-2">
                    {PASSWORD_RULES.map(rule => {
                      const met = rule.test(password)
                      return (
                        <li key={rule.label} className={`flex items-center gap-1.5 text-[13px] ${met ? 'font-semibold text-foreground' : 'text-muted'}`}>
                          <span aria-hidden="true" className={`grid size-4 place-items-center rounded-full ${met ? 'bg-primary text-foreground' : 'bg-surface text-muted'}`}>
                            <Check className="size-2.5" strokeWidth={3.5} />
                          </span>
                          {rule.label}
                          <span className="sr-only">: {met ? 'cumprida' : 'pendente'}</span>
                        </li>
                      )
                    })}
                  </ul>
                </div>
                {error && (
                  <p role="alert" className={authAlert}>
                    <CircleAlert aria-hidden="true" className="size-[18px] shrink-0" />
                    {error}
                  </p>
                )}
                <button type="submit" disabled={busy} className={`mt-[14px] ${authPrimaryButton}`}>
                  {busy ? 'Criando conta...' : 'Criar conta'}
                  <ArrowRight aria-hidden="true" className="size-[18px]" />
                </button>
              </form>

              <p className="flex justify-center gap-1.5 text-[14px] text-muted">
                Já tem conta?
                <Link href="/login" className="font-bold text-foreground underline">Entrar</Link>
              </p>
            </>
          )}
        </div>
      </main>
    </div>
  )
}
