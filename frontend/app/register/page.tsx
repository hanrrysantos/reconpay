'use client'

import { ArrowRight, ShieldCheck } from 'lucide-react'
import Link from 'next/link'
import { useState, type FormEvent } from 'react'
import { register } from '@/lib/api'
import styles from '../auth.module.css'

export default function RegisterPage() {
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [registeredEmail, setRegisteredEmail] = useState('')

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
    <div className={styles.shell}>
      <aside className={styles.intro}>
        <div className={styles.brand}><span className={styles.brandMark}><ShieldCheck aria-hidden="true" /></span>ReconPay</div>
        <div><h1>Comece com uma visão mais clara.</h1><p>Crie sua conta para acompanhar os dados financeiros aos quais sua equipe tem acesso.</p></div>
        <span className={styles.caption}>Conciliação financeira em um só lugar</span>
      </aside>
      <main className={styles.main}>
        <div className={styles.card}>
          <span className={styles.eyebrow}>Sua conta ReconPay</span>
          <h2>Crie sua conta</h2>
          {registeredEmail ? (
            <div className={styles.success} role="status">
              Cadastro recebido. Enviamos um link de verificação para <strong>{registeredEmail}</strong>. Abra o email e confirme sua conta antes de entrar.
              <p><Link className={styles.textLink} href="/login">Ir para o login</Link></p>
            </div>
          ) : (
            <>
              <p className={styles.description}>Depois do cadastro, confirme seu email para ativar a conta.</p>
              <form className={styles.form} onSubmit={submit}>
                <label className={styles.field}>Nome completo<input name="name" type="text" autoComplete="name" placeholder="Seu nome" required /></label>
                <label className={styles.field}>Email<input name="email" type="email" autoComplete="email" placeholder="voce@empresa.com" required /></label>
                <label className={styles.field}>Senha<input name="password" type="password" autoComplete="new-password" placeholder="Crie uma senha" minLength={8} pattern="(?=.*[A-Z])(?=.*[0-9]).{8,}" title="Use pelo menos 8 caracteres, uma letra maiúscula e um número" required /><span className={styles.hint}>Mínimo de 8 caracteres, com uma letra maiúscula e um número.</span></label>
                {error && <p className={styles.error} role="alert">{error}</p>}
                <button className={styles.submit} type="submit" disabled={busy}>{busy ? 'Criando conta...' : 'Criar conta'}<ArrowRight aria-hidden="true" /></button>
              </form>
              <p className={styles.footer}>Já tem conta? <Link href="/login">Entrar</Link></p>
            </>
          )}
        </div>
      </main>
    </div>
  )
}
