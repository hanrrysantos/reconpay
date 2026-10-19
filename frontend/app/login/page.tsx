'use client'

import { ArrowRight, Check, CircleAlert, Eye, EyeOff } from 'lucide-react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useEffect, useState, type FormEvent } from 'react'
import { useAuth } from '@/lib/auth'

const field = 'h-12 w-full rounded-full bg-surface px-[18px] text-[15px] outline-none placeholder:text-muted focus:shadow-[inset_0_0_0_2px_var(--color-foreground)]'

export default function LoginPage() {
  const { signIn, user } = useAuth()
  const router = useRouter()
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  useEffect(() => { if (user) router.replace('/') }, [router, user])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy) return
    const form = new FormData(event.currentTarget)
    setError('')
    setBusy(true)
    try {
      await signIn(String(form.get('email')), String(form.get('password')))
      router.replace('/')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Não foi possível entrar. Tente novamente.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex min-h-dvh flex-col bg-background text-foreground lg:flex-row">
      <aside className="flex flex-col gap-7 bg-foreground px-6 pt-7 pb-8 text-background lg:w-[440px] lg:shrink-0 lg:justify-between lg:p-12 xl:w-[640px] xl:p-14">
        <span className="flex items-center gap-2.5 font-display text-[20px] font-extrabold tracking-[-0.04em]">
          <span className="grid size-[30px] place-items-center rounded-sm bg-primary text-foreground">
            <Check aria-hidden="true" className="size-4" strokeWidth={3} />
          </span>
          reconpay
        </span>
        <div className="flex flex-col gap-[18px]">
          <h2 className="max-w-[500px] font-display text-[30px] leading-[1.05] font-extrabold tracking-[-0.04em] lg:text-[40px] lg:leading-[1.02] xl:text-[52px]">
            Do vendido ao que caiu na conta, sem planilha.
          </h2>
          <p className="hidden max-w-[460px] text-[16px] leading-[1.55] text-background/65 lg:block">
            Cruze vendas, liquidações e extrato numa janela, trate cada divergência e trave o período com rastro para auditoria.
          </p>
        </div>
        <div className="hidden items-center gap-2 lg:flex">
          <span aria-hidden="true" className="size-1.5 rounded-full bg-primary" />
          <span className="text-[13px] text-background/45">Conciliação financeira para fintechs, gateways e marketplaces</span>
        </div>
      </aside>

      <main className="flex flex-1 justify-center px-6 py-8 lg:items-center lg:p-14">
        <div className="flex w-full max-w-[400px] flex-col gap-7 lg:gap-8">
          <div className="flex flex-col gap-2.5">
            <h1 className="font-display text-[36px] leading-none font-extrabold tracking-[-0.04em] lg:text-[44px]">Entrar</h1>
            <p className="text-[15px] leading-[1.5] text-muted">Use o e-mail e a senha da sua conta ReconPay.</p>
          </div>

          <form className="flex flex-col gap-[18px]" onSubmit={submit}>
            <div className="flex flex-col gap-2">
              <label htmlFor="login-email" className="text-[13px] font-semibold">E-mail</label>
              <input id="login-email" name="email" type="email" autoComplete="email" placeholder="voce@empresa.com" required className={field} />
            </div>
            <div className="flex flex-col gap-2">
              <label htmlFor="login-password" className="text-[13px] font-semibold">Senha</label>
              <div className="relative">
                <input
                  id="login-password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  placeholder="Sua senha"
                  required
                  className={`${field} pr-12`}
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
            </div>
            {error && (
              <p role="alert" className="flex items-center gap-2.5 rounded-[14px] bg-danger-soft px-4 py-3 text-[14px] font-semibold text-danger-strong">
                <CircleAlert aria-hidden="true" className="size-[18px] shrink-0" />
                {error}
              </p>
            )}
            <button
              type="submit"
              disabled={busy}
              className="mt-[14px] flex h-[52px] items-center justify-center gap-2 rounded-full bg-primary text-[15px] font-bold text-foreground disabled:opacity-60"
            >
              {busy ? 'Entrando...' : 'Entrar'}
              <ArrowRight aria-hidden="true" className="size-[18px]" />
            </button>
          </form>

          <p className="flex justify-center gap-1.5 text-[14px] text-muted">
            Ainda não tem conta?
            <Link href="/register" className="font-bold text-foreground underline">Criar conta</Link>
          </p>
        </div>
      </main>
    </div>
  )
}
