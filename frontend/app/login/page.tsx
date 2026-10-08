'use client'

import { ArrowRight, ShieldCheck } from 'lucide-react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useEffect, useState, type FormEvent } from 'react'
import { useAuth } from '@/lib/auth'
import styles from '../auth.module.css'

export default function LoginPage() {
  const { signIn, user } = useAuth()
  const router = useRouter()
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

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
    <div className={styles.shell}>
      <aside className={styles.intro}>
        <div className={styles.brand}><span className={styles.brandMark}><ShieldCheck aria-hidden="true" /></span>ReconPay</div>
        <div><h1>Clareza para cada conciliação.</h1><p>Acompanhe merchants, transações e resultados em um espaço organizado para sua operação financeira.</p></div>
        <span className={styles.caption}>Conciliação financeira em um só lugar</span>
      </aside>
      <main className={styles.main}>
        <div className={styles.card}>
          <span className={styles.eyebrow}>Bem-vindo de volta</span>
          <h2>Entre na sua conta</h2>
          <p className={styles.description}>Acesse seus dados com o email e a senha cadastrados.</p>
          <form className={styles.form} onSubmit={submit}>
            <label className={styles.field}>Email<input name="email" type="email" autoComplete="email" placeholder="voce@empresa.com" required /></label>
            <label className={styles.field}>Senha<input name="password" type="password" autoComplete="current-password" placeholder="Sua senha" required /></label>
            {error && <p className={styles.error} role="alert">{error}</p>}
            <button className={styles.submit} type="submit" disabled={busy}>{busy ? 'Entrando...' : 'Entrar'}<ArrowRight aria-hidden="true" /></button>
          </form>
          <p className={styles.footer}>Ainda não tem conta? <Link href="/register">Cadastre-se</Link></p>
        </div>
      </main>
    </div>
  )
}
