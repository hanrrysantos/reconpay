'use client'

import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { METHOD_LABELS } from '@/components/dashboard/format'
import { isGone } from '@/components/merchants/MerchantDrawer'
import { SideDrawer } from '@/components/ui/SideDrawer'
import type { FeeRule, PaymentMethod } from '@/lib/api'
import { SINGLE_INSTALLMENT_METHODS, parseFeePercent, parseFixedFee, toInputDecimal } from '@/lib/fee'
import { useCreateFeeRule, useInvalidateFeeRules, useUpdateFeeRuleFees } from '@/lib/queries'
import { cn } from '@/lib/utils'

export const METHOD_REQUIRED_MESSAGE = 'Escolha o método de pagamento'
export const INSTALLMENTS_MESSAGE = 'Informe um número inteiro de parcelas a partir de 1'
export const PERCENT_MESSAGE = 'Informe um percentual de 0 a 100 com até 4 casas decimais'
export const FIXED_FEE_MESSAGE = 'Informe uma taxa fixa a partir de 0 com até 2 casas decimais'

const METHODS: PaymentMethod[] = ['CREDIT_CARD', 'DEBIT_CARD', 'PIX', 'BOLETO']
const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível salvar.'
const singleInstallment = (method: PaymentMethod | '') => method !== '' && SINGLE_INSTALLMENT_METHODS.includes(method)
/** Installments are a count, not money: a whole number from 1. */
const parseInstallments = (input: string) => /^\d+$/.test(input.trim()) && Number(input) >= 1 ? Number(input) : null

const field = 'h-12 rounded-md bg-surface px-3 text-[14px] outline-none focus:shadow-[inset_0_0_0_2px_var(--color-foreground)] aria-invalid:shadow-[inset_0_0_0_2px_var(--color-danger)]'

type Errors = { method?: string | null; installments?: string | null; percent?: string | null; fixedFee?: string | null }

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

/**
 * Side drawer checked here before the API is called. Without `rule` it registers one fee rule of `merchantId`;
 * Pix, Boleto and Débito keep 1 installment. With `rule` it changes only its rates, and method and installments
 * are read-only. Percent and fixed fee are sent as the exact typed text (AD-010).
 */
export function FeeRuleDrawer({ merchantId, rule, onClose }: { merchantId: string; rule?: FeeRule; onClose: () => void }) {
  const editing = rule !== undefined
  const create = useCreateFeeRule(merchantId)
  const update = useUpdateFeeRuleFees(merchantId)
  const reloadList = useInvalidateFeeRules(merchantId)
  const methodInput = useRef<HTMLSelectElement>(null)
  const percentInput = useRef<HTMLInputElement>(null)
  const sending = useRef(false)
  const [method, setMethod] = useState<PaymentMethod | ''>(rule?.paymentMethod ?? '')
  const [installments, setInstallments] = useState(String(rule?.installments ?? 1))
  const [percent, setPercent] = useState(rule ? toInputDecimal(rule.feePercentage) : '')
  const [fixedFee, setFixedFee] = useState(rule ? toInputDecimal(rule.fixedFee) : '')
  const [errors, setErrors] = useState<Errors>({})
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)
  const locked = singleInstallment(method)

  useEffect(() => { (editing ? percentInput : methodInput).current?.focus() }, [editing])

  function chooseMethod(value: PaymentMethod | '') {
    setMethod(value)
    if (singleInstallment(value)) setInstallments('1')
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (sending.current) return
    const count = parseInstallments(installments)
    const feePercentage = parseFeePercent(percent)
    const fee = parseFixedFee(fixedFee)
    const invalid: Errors = {
      method: method === '' ? METHOD_REQUIRED_MESSAGE : null,
      installments: count === null ? INSTALLMENTS_MESSAGE : null,
      percent: feePercentage === null ? PERCENT_MESSAGE : null,
      fixedFee: fee === null ? FIXED_FEE_MESSAGE : null,
    }
    setErrors(invalid)
    if (method === '' || count === null || feePercentage === null || fee === null) return

    sending.current = true
    setPending(true)
    setError(null)
    try {
      if (editing) await update.mutateAsync({ ruleId: rule.id, fees: { feePercentage, fixedFee: fee } })
      else await create.mutateAsync({ paymentMethod: method, installments: count, feePercentage, fixedFee: fee })
      onClose()
    } catch (failure) {
      setError(messageOf(failure))
      // 404: the rule was deactivated elsewhere (403: the merchant left the user's access), so the list is stale.
      if (isGone(failure)) await reloadList()
    } finally {
      sending.current = false
      setPending(false)
    }
  }

  return (
    <SideDrawer
      title={editing ? 'Editar regra de taxa' : 'Cadastrar regra de taxa'}
      description={editing ? 'Só o percentual e a taxa fixa podem mudar.' : 'Uma regra para cada método e número de parcelas.'}
      formId="fee-rule-form"
      submitLabel={editing ? 'Salvar' : 'Cadastrar'}
      pending={pending}
      error={error}
      onSubmit={event => void submit(event)}
      onClose={onClose}
    >
      <Field id="fee-rule-method" label="Método" error={errors.method}>
        {editing ? (
          <input id="fee-rule-method" readOnly value={METHOD_LABELS[rule.paymentMethod]?.label ?? rule.paymentMethod} className={cn(field, 'text-muted')} />
        ) : (
          <select
            ref={methodInput}
            id="fee-rule-method"
            value={method}
            onChange={event => chooseMethod(event.target.value as PaymentMethod | '')}
            {...invalidProps('fee-rule-method', errors.method)}
            className={cn(field, method === '' && 'text-muted')}
          >
            <option value="">Escolha o método</option>
            {METHODS.map(item => <option key={item} value={item}>{METHOD_LABELS[item].label}</option>)}
          </select>
        )}
      </Field>

      <Field id="fee-rule-installments" label="Parcelas" error={errors.installments}>
        <input
          id="fee-rule-installments"
          inputMode="numeric"
          readOnly={editing}
          disabled={!editing && locked}
          value={installments}
          onChange={event => setInstallments(event.target.value)}
          {...invalidProps('fee-rule-installments', errors.installments)}
          className={cn(field, 'font-mono', (editing || locked) && 'text-muted')}
        />
      </Field>

      <Field id="fee-rule-percent" label="Percentual" error={errors.percent}>
        <input
          ref={percentInput}
          id="fee-rule-percent"
          inputMode="decimal"
          placeholder="0,00"
          value={percent}
          onChange={event => setPercent(event.target.value)}
          {...invalidProps('fee-rule-percent', errors.percent)}
          className={cn(field, 'font-mono placeholder:text-muted')}
        />
      </Field>

      <Field id="fee-rule-fixed-fee" label="Taxa fixa" error={errors.fixedFee}>
        <input
          id="fee-rule-fixed-fee"
          inputMode="decimal"
          placeholder="0,00"
          value={fixedFee}
          onChange={event => setFixedFee(event.target.value)}
          {...invalidProps('fee-rule-fixed-fee', errors.fixedFee)}
          className={cn(field, 'font-mono placeholder:text-muted')}
        />
      </Field>
    </SideDrawer>
  )
}
