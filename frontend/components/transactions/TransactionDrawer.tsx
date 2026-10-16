'use client'

import Link from 'next/link'
import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { METHOD_LABELS } from '@/components/dashboard/format'
import { INSTALLMENTS_MESSAGE, METHOD_REQUIRED_MESSAGE } from '@/components/fee-rules/FeeRuleDrawer'
import { CREATE_FEE_RULE_HREF } from '@/components/fee-rules/FeeRulesPage'
import { SideDrawer } from '@/components/ui/SideDrawer'
import type { PaymentMethod, Transaction } from '@/lib/api'
import { SINGLE_INSTALLMENT_METHODS } from '@/lib/fee'
import { useCreateTransaction } from '@/lib/queries'
import { AMOUNT_MESSAGE, dateError, isMissingFeeRule, parseAmount, parseReference, todayIso } from '@/lib/transactions'
import { cn } from '@/lib/utils'

const METHODS: PaymentMethod[] = ['CREDIT_CARD', 'DEBIT_CARD', 'PIX', 'BOLETO']
const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível cadastrar.'
const singleInstallment = (method: PaymentMethod | '') => method !== '' && SINGLE_INSTALLMENT_METHODS.includes(method)
/** Installments are a count, not money: a whole number from 1. */
const parseInstallments = (input: string) => /^\d+$/.test(input.trim()) && Number(input) >= 1 ? Number(input) : null

const field = 'h-12 rounded-md bg-surface px-3 text-[14px] outline-none focus:shadow-[inset_0_0_0_2px_var(--color-foreground)] aria-invalid:shadow-[inset_0_0_0_2px_var(--color-danger)]'

type Errors = { reference?: string | null; amount?: string | null; method?: string | null; installments?: string | null; date?: string | null }

function Field({ id, label, error, children }: { id: string; label: string; error?: string | null; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={id} className="text-[13px] font-semibold">{label}</label>
      {children}
      {error && <p id={`${id}-error`} className="text-[12px] text-danger">{error}</p>}
    </div>
  )
}

const invalidProps = (id: string, error?: string | null) => ({
  'aria-invalid': Boolean(error),
  'aria-describedby': error ? `${id}-error` : undefined,
})

type Props = { merchantId: string; onClose: () => void; onCreated: (transaction: Transaction) => void }

/**
 * Side drawer that registers one transaction of `merchantId`, checked here with the API's rules before it is called.
 * Pix, Boleto and Débito keep 1 installment; the amount is sent as the exact typed text (AD-010). When the API finds
 * no active fee rule, the error links to the fee rule form.
 */
export function TransactionDrawer({ merchantId, onClose, onCreated }: Props) {
  const create = useCreateTransaction(merchantId)
  const referenceInput = useRef<HTMLInputElement>(null)
  const sending = useRef(false)
  const [today] = useState(() => todayIso())
  const [reference, setReference] = useState('')
  const [amount, setAmount] = useState('')
  const [method, setMethod] = useState<PaymentMethod | ''>('')
  const [installments, setInstallments] = useState('1')
  const [date, setDate] = useState(today)
  const [errors, setErrors] = useState<Errors>({})
  const [failure, setFailure] = useState<unknown>(null)
  const [pending, setPending] = useState(false)
  const locked = singleInstallment(method)

  useEffect(() => { referenceInput.current?.focus() }, [])

  function chooseMethod(value: PaymentMethod | '') {
    setMethod(value)
    if (singleInstallment(value)) setInstallments('1')
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (sending.current) return
    const parsedReference = parseReference(reference)
    const parsedAmount = parseAmount(amount)
    const count = parseInstallments(installments)
    const invalid: Errors = {
      reference: 'error' in parsedReference ? parsedReference.error : null,
      amount: parsedAmount === null ? AMOUNT_MESSAGE : null,
      method: method === '' ? METHOD_REQUIRED_MESSAGE : null,
      installments: count === null ? INSTALLMENTS_MESSAGE : null,
      date: dateError(date, today),
    }
    setErrors(invalid)
    if ('error' in parsedReference || parsedAmount === null || method === '' || count === null || invalid.date) return

    sending.current = true
    setPending(true)
    setFailure(null)
    try {
      const transaction = await create.mutateAsync({
        externalReference: parsedReference.value,
        amount: parsedAmount,
        paymentMethod: method,
        installments: count,
        transactionDate: date,
      })
      onCreated(transaction)
    } catch (error) {
      setFailure(error)
    } finally {
      sending.current = false
      setPending(false)
    }
  }

  return (
    <SideDrawer
      title="Nova transação"
      description="Uma venda avulsa, com as mesmas regras do import."
      formId="transaction-form"
      submitLabel={pending ? 'Cadastrando…' : 'Cadastrar'}
      pending={pending}
      error={failure === null ? null : messageOf(failure)}
      errorAction={isMissingFeeRule(failure) && <Link href={CREATE_FEE_RULE_HREF} className="font-semibold underline">Cadastrar regra de taxa</Link>}
      onSubmit={event => void submit(event)}
      onClose={onClose}
    >
      <Field id="transaction-reference" label="Referência externa" error={errors.reference}>
        <input
          ref={referenceInput}
          id="transaction-reference"
          value={reference}
          onChange={event => setReference(event.target.value)}
          {...invalidProps('transaction-reference', errors.reference)}
          className={field}
        />
      </Field>

      <Field id="transaction-amount" label="Valor" error={errors.amount}>
        <input
          id="transaction-amount"
          inputMode="decimal"
          placeholder="0,00"
          value={amount}
          onChange={event => setAmount(event.target.value)}
          {...invalidProps('transaction-amount', errors.amount)}
          className={cn(field, 'font-mono placeholder:text-muted')}
        />
      </Field>

      <Field id="transaction-method" label="Método" error={errors.method}>
        <select
          id="transaction-method"
          value={method}
          onChange={event => chooseMethod(event.target.value as PaymentMethod | '')}
          {...invalidProps('transaction-method', errors.method)}
          className={cn(field, method === '' && 'text-muted')}
        >
          <option value="">Escolha o método</option>
          {METHODS.map(item => <option key={item} value={item}>{METHOD_LABELS[item].label}</option>)}
        </select>
      </Field>

      <Field id="transaction-installments" label="Parcelas" error={errors.installments}>
        <input
          id="transaction-installments"
          inputMode="numeric"
          disabled={locked}
          value={installments}
          onChange={event => setInstallments(event.target.value)}
          {...invalidProps('transaction-installments', errors.installments)}
          className={cn(field, 'font-mono', locked && 'text-muted')}
        />
      </Field>

      <Field id="transaction-date" label="Data da transação" error={errors.date}>
        <input
          id="transaction-date"
          type="date"
          max={today}
          value={date}
          onChange={event => setDate(event.target.value)}
          {...invalidProps('transaction-date', errors.date)}
          className={cn(field, 'font-mono')}
        />
      </Field>
    </SideDrawer>
  )
}
