import type { ContractType, QuoteStatus } from '../api/types'
import { CONTRACT_TYPE_LABELS, STATUS_LABELS } from '../format'

export const StatusBadge = ({ status }: { status: QuoteStatus }) => (
  <span className={`badge status-${status.toLowerCase()}`}>{STATUS_LABELS[status]}</span>
)

export const ContractTypeBadge = ({ type }: { type: ContractType }) => (
  <span className={`badge type-${type.toLowerCase().replace('_', '-')}`}>{CONTRACT_TYPE_LABELS[type]}</span>
)
