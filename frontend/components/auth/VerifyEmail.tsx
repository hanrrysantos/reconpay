'use client'

import { ArrowRight, Check, CircleAlert, Link2Off, LoaderCircle, MailCheck, RotateCw, Send } from 'lucide-react'
import Link from 'next/link'
import { useCallback, useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { ApiError, resendVerification, verifyEmail } from '@/lib/api'
import { authAlert, authField, authPrimaryButton } from './AuthPanel'

type State =
  | { kind: 'verifying' }
  | { kind: 'confirmed' }
  | { kind: 'invalid' }
  | { kind: 'error'; message: string }
  | { kind: 'sent'; email: string }

export function VerifyEmail({ token }: { token: string | null }) {
  const [state, setState] = useState<State>(token ? { kind: 'verifying' } : { kind: 'invalid' })
  // Strict Mode runs effects twice; the token is single-use, so it must reach the API only once.
  const requested = useRef<string | null>(null)

  const confirm = useCallback(async (value: string) => {
    setState({ kind: 'verifying' })
    try {
      await verifyEmail(value)
      setState({ kind: 'confirmed' })
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 400) setState({ kind: 'invalid' })
      else setState({ kind: 'error', message: cause instanceof Error ? cause.message : 'Não foi possível confirmar o e-mail. Tente novamente.' })
    }
  }, [])

  useEffect(() => {
    if (!token || requested.current === token) return
    requested.current = token
    void confirm(token)
  }, [confirm, token])

  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-8 bg-surface px-4 py-12 text-foreground">
      <span className="flex items-center gap-2.5 font-display text-[20px] font-extrabold tracking-[-0.04em]">
        <span className="grid size-[30px] place-items-center rounded-sm bg-primary">
          <Check aria-hidden="true" className="size-4" strokeWidth={3} />
        </span>
        reconpay
      </span>
      <main className="w-full max-w-[480px]">
        {state.kind === 'verifying' && (
          <Card mark={<Mark tone="neutral"><LoaderCircle aria-hidden="true" className="size-[26px] animate-spin" /></Mark>}>
            <div role="status" className="flex flex-col gap-3">
              <Title>Confirmando...</Title>
              <Body>Estamos ativando sua conta.</Body>
            </div>
          </Card>
        )}
        {state.kind === 'confirmed' && (
          <Card mark={<Mark tone="primary"><Check aria-hidden="true" className="size-[26px]" strokeWidth={3} /></Mark>}>
            <div className="flex flex-col gap-3">
              <Title>E-mail confirmado</Title>
              <Body>Sua conta está ativa. Entre com o e-mail e a senha que você cadastrou.</Body>
            </div>
            <Link href="/login" className={authPrimaryButton}>
              Ir para o login
              <ArrowRight aria-hidden="true" className="size-[18px]" />
            </Link>
          </Card>
        )}
        {state.kind === 'error' && (
          <Card mark={<Mark tone="danger"><CircleAlert aria-hidden="true" className="size-[26px]" /></Mark>}>
            <Title>Não foi possível confirmar</Title>
            <p role="alert" className={authAlert}>
              <CircleAlert aria-hidden="true" className="size-[18px] shrink-0" />
              {state.message}
            </p>
            <button type="button" onClick={() => token && void confirm(token)} className={authPrimaryButton}>
              Tentar de novo
              <RotateCw aria-hidden="true" className="size-[18px]" />
            </button>
          </Card>
        )}
        {state.kind === 'invalid' && <Resend onSent={email => setState({ kind: 'sent', email })} />}
        {state.kind === 'sent' && (
          <Card mark={<Mark tone="primary"><MailCheck aria-hidden="true" className="size-[26px]" /></Mark>}>
            <div className="flex flex-col gap-3">
              <Title>Confira seu e-mail</Title>
              <Body>
                Se <strong className="font-semibold text-foreground">{state.email}</strong> tiver um cadastro aguardando confirmação, o link novo chega em alguns minutos. Não chegou? Confira a caixa de spam.
              </Body>
            </div>
            <BackToLogin />
          </Card>
        )}
      </main>
    </div>
  )
}

function Resend({ onSent }: { onSent: (email: string) => void }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy) return
    const email = String(new FormData(event.currentTarget).get('email'))
    setError('')
    setBusy(true)
    try {
      await resendVerification(email)
      onSent(email.trim())
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Não foi possível enviar o link. Tente novamente.')
      setBusy(false)
    }
  }

  return (
    <Card mark={<Mark tone="danger"><Link2Off aria-hidden="true" className="size-[26px]" /></Mark>}>
      <div className="flex flex-col gap-3">
        <Title>Este link não vale mais</Title>
        <Body>Ele expirou ou já foi usado. Informe seu e-mail e enviamos um link novo.</Body>
      </div>
      <form className="flex flex-col gap-3.5" onSubmit={submit}>
        <div className="flex flex-col gap-2">
          <label htmlFor="resend-email" className="text-[13px] font-semibold">E-mail</label>
          <input id="resend-email" name="email" type="email" autoComplete="email" placeholder="voce@empresa.com" required className={authField} />
        </div>
        {error && (
          <p role="alert" className={authAlert}>
            <CircleAlert aria-hidden="true" className="size-[18px] shrink-0" />
            {error}
          </p>
        )}
        <button type="submit" disabled={busy} className={authPrimaryButton}>
          {busy ? 'Enviando...' : 'Enviar novo link'}
          <Send aria-hidden="true" className="size-[18px]" />
        </button>
      </form>
      <BackToLogin />
    </Card>
  )
}

function Card({ mark, children }: { mark: ReactNode; children: ReactNode }) {
  return (
    <section className="flex flex-col gap-7 rounded-[28px] bg-background p-7 sm:p-10">
      {mark}
      {children}
    </section>
  )
}

const MARK_TONES = {
  primary: 'bg-primary text-foreground',
  danger: 'bg-danger-soft text-danger-strong',
  neutral: 'bg-surface text-muted',
}

function Mark({ tone, children }: { tone: keyof typeof MARK_TONES; children: ReactNode }) {
  return <span className={`grid size-14 place-items-center rounded-full ${MARK_TONES[tone]}`}>{children}</span>
}

function Title({ children }: { children: ReactNode }) {
  return <h1 className="font-display text-[32px] leading-[1.02] font-extrabold tracking-[-0.04em] sm:text-[36px]">{children}</h1>
}

function Body({ children }: { children: ReactNode }) {
  return <p className="text-[15px] leading-[1.55] text-muted">{children}</p>
}

function BackToLogin() {
  return (
    <p className="flex justify-center gap-1.5 text-[14px] text-muted">
      Já confirmou?
      <Link href="/login" className="font-bold text-foreground underline">Entrar</Link>
    </p>
  )
}
