import type { QuoteRequest, QuoteResponse } from './api/types'

export interface LineDraft {
  key: string
  sku: string
  // 表示専用. 見積の単価はサーバーがカタログから決めるため, リクエストには含めない
  listPrice: string
  amount: string
}

export interface QuoteDraft {
  contractStart: string
  contractEnd: string
  discountRate: string
  lines: LineDraft[]
}

const MIN_INT32 = -2147483648
const MAX_INT32 = 2147483647

const toIsoDate = (date: Date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

export const newLine = (sku = '', listPrice = '', amount = '1'): LineDraft => ({
  key: crypto.randomUUID(),
  sku,
  listPrice,
  amount,
})

const DEFAULT_CONTRACT_YEARS = 3

export function emptyDraft(): QuoteDraft {
  const start = new Date()
  const end = new Date(start.getFullYear() + DEFAULT_CONTRACT_YEARS, start.getMonth(), start.getDate() - 1)
  return {
    contractStart: toIsoDate(start),
    contractEnd: toIsoDate(end),
    discountRate: '0',
    lines: [],
  }
}

export const draftFromQuote = (quote: QuoteResponse): QuoteDraft => ({
  contractStart: quote.contractStart,
  contractEnd: quote.contractEnd,
  discountRate: String(quote.discountRate),
  lines: quote.skuList.map((line) => newLine(line.sku, String(line.listPrice), String(line.amount))),
})

export type DraftResult = { request: QuoteRequest; errors?: undefined } | { request?: undefined; errors: string[] }

export function toRequest(draft: QuoteDraft): DraftResult {
  const errors: string[] = []

  if (!draft.contractStart) errors.push('契約開始日を入力してください.')
  if (!draft.contractEnd) errors.push('契約終了日を入力してください.')
  if (draft.contractStart && draft.contractEnd && draft.contractEnd < draft.contractStart) {
    errors.push('契約終了日は契約開始日以降にしてください.')
  }

  const discountRate = Number(draft.discountRate)
  if (draft.discountRate.trim() === '' || !(discountRate >= 0 && discountRate <= 100)) {
    errors.push('割引率は0から100の範囲で入力してください.')
  }

  if (draft.lines.length === 0) errors.push('SKU を1件以上追加してください.')

  const skuList = draft.lines.map((line, index) => {
    const label = `明細${index + 1}`
    const amount = Number(line.amount)
    if (!line.sku.trim()) errors.push(`${label}: SKU を入力してください.`)
    if (!Number.isInteger(amount) || amount === 0 || amount < MIN_INT32 || amount > MAX_INT32) {
      errors.push(`${label}: 数量は0以外の整数で入力してください.`)
    }
    return { sku: line.sku.trim(), amount }
  })

  if (errors.length > 0) return { errors }
  return {
    request: {
      contractStart: draft.contractStart,
      contractEnd: draft.contractEnd,
      discountRate,
      skuList,
    },
  }
}
