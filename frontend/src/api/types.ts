export type ContractType = 'NEW' | 'CANCELLATION' | 'EARLY_RENEWAL'
export type QuoteStatus = 'DRAFT' | 'SAVED'

export interface LineRequest {
  sku: string
  amount: number
}

export interface QuoteRequest {
  contractStart: string
  contractEnd: string
  skuList: LineRequest[]
  discountRate: number
}

export interface LineResponse extends LineRequest {
  listPrice: number
  annual: number
}

export interface QuoteResponse {
  id: string
  validUntil: string
  contractStart: string
  contractEnd: string
  contractType: ContractType
  status: QuoteStatus
  skuList: LineResponse[]
  discountRate: number
  totalAnnual: number
  totalAmount: number
}

export interface SkuResponse {
  id: string
  listPrice: number
  description: string
}

export interface ProblemDetail {
  title?: string
  status?: number
  detail?: string
}
