import type { ProblemDetail, QuoteRequest, QuoteResponse, SkuResponse } from './types'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function toApiError(response: Response): Promise<ApiError> {
  const problem: ProblemDetail | undefined = await response.json().catch(() => undefined)
  const message = problem?.detail ?? problem?.title ?? response.statusText
  return new ApiError(response.status, message || `HTTP ${response.status}`)
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  })
  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

const json = (method: string, body: unknown): RequestInit => ({
  method,
  body: JSON.stringify(body),
})

export const listQuotes = () => request<QuoteResponse[]>('/api/quotes')

export const getQuote = (id: string) => request<QuoteResponse>(`/api/quotes/${id}`)

export const createQuote = (body: QuoteRequest) =>
  request<QuoteResponse>('/api/quotes', json('POST', body))

export const reviseQuote = (id: string, body: QuoteRequest) =>
  request<QuoteResponse>(`/api/quotes/${id}`, json('PUT', body))

export const saveQuote = (id: string) =>
  request<QuoteResponse>(`/api/quotes/${id}/save`, { method: 'POST' })

export const deleteQuote = (id: string) =>
  request<void>(`/api/quotes/${id}`, { method: 'DELETE' })

export const listSkus = () => request<SkuResponse[]>('/api/skus')

export const messageOf = (error: unknown): string =>
  error instanceof Error ? error.message : String(error)
