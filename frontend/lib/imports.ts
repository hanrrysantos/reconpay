import { ApiError, type ImportBatch, type ImportKind } from './api'

export type ImportKindInfo = {
  kind: ImportKind
  /** Value of `?tipo=` on `/importacoes`. */
  slug: 'transacoes' | 'liquidacao' | 'extrato'
  label: string
  /** Completes "Nenhuma importação de <noun> ainda." */
  noun: string
  /** CSV header the API expects, in order. */
  columns: readonly string[]
}

/** The three CSV imports, in the order of the flow: transaction, then settlement, then bank statement. */
export const IMPORT_KINDS: readonly ImportKindInfo[] = [
  {
    kind: 'transactions', slug: 'transacoes', label: 'Transações', noun: 'transações',
    columns: ['externalReference', 'amount', 'paymentMethod', 'installments', 'status', 'transactionDate'],
  },
  {
    kind: 'settlements', slug: 'liquidacao', label: 'Liquidação', noun: 'liquidação',
    columns: ['externalReference', 'amount', 'netAmount', 'paymentMethod', 'installments', 'status', 'settlementDate'],
  },
  {
    kind: 'bankStatements', slug: 'extrato', label: 'Extrato', noun: 'extrato',
    columns: ['lineReference', 'externalReference', 'amount', 'movementDate'],
  },
]

/** A missing or unknown `?tipo=` opens the transactions tab. */
export function kindFromSlug(slug: string | null): ImportKind {
  return IMPORT_KINDS.find(info => info.slug === slug)?.kind ?? 'transactions'
}

/** The API's multipart limit (5MB). */
export const MAX_CSV_BYTES = 5 * 1024 * 1024
export const FILE_TOO_LARGE_MESSAGE = 'Arquivo CSV excede o tamanho máximo permitido de 5MB'

export const isTooLarge = (file: { size: number }) => file.size > MAX_CSV_BYTES

export type RowError = { row: number; message: string }
export type ImportFailure = { message: string; rowErrors: RowError[]; conflictingReferences: string[] }

const isRowError = (value: unknown): value is RowError =>
  typeof value === 'object' && value !== null
  && typeof (value as RowError).row === 'number' && typeof (value as RowError).message === 'string'

/** Why the API refused the file; a `details` list in an unexpected shape is dropped, keeping the message. */
export function importFailure(error: unknown): ImportFailure {
  const message = error instanceof Error ? error.message : 'Não foi possível importar o arquivo.'
  const details = error instanceof ApiError ? error.details : undefined
  const rowErrors = details?.rowErrors
  const references = details?.conflictingReferences
  return {
    message,
    rowErrors: Array.isArray(rowErrors) && rowErrors.every(isRowError) ? rowErrors : [],
    conflictingReferences: Array.isArray(references) && references.every(ref => typeof ref === 'string') ? references : [],
  }
}

export const importedRowsMessage = (batch: ImportBatch) => `${batch.fileName} importado: ${batch.totalRows} linha(s)`

/** `dd/mm/aaaa hh:mm` in the browser's time zone; `timeZone` pins it in tests. */
export function formatImportedAt(iso: string, timeZone?: string): string {
  const parts = new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hourCycle: 'h23', timeZone,
  }).formatToParts(new Date(iso))
  const part = (type: Intl.DateTimeFormatPartTypes) => parts.find(p => p.type === type)?.value ?? ''
  return `${part('day')}/${part('month')}/${part('year')} ${part('hour')}:${part('minute')}`
}
