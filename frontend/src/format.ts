import type { ContractType, QuoteStatus } from './api/types'

const yen = new Intl.NumberFormat('ja-JP', { style: 'currency', currency: 'JPY' })

export const formatYen = (value: number) => yen.format(value)

export const CONTRACT_TYPE_LABELS: Record<ContractType, string> = {
  NEW: '新規',
  CANCELLATION: '解約',
  EARLY_RENEWAL: '早期更新',
}

export const STATUS_LABELS: Record<QuoteStatus, string> = {
  DRAFT: '仮保存',
  SAVED: '保存済み',
}
