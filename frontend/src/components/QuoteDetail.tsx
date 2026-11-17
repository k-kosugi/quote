import { useCallback } from 'react'
import { getQuote } from '../api/client'
import type { QuoteResponse } from '../api/types'
import { useLoad } from '../api/useLoad'
import { formatYen } from '../format'
import { ContractTypeBadge, StatusBadge } from './Badges'
import { QuoteActions } from './QuoteActions'

interface Props {
  id: string
  onBack: () => void
  onEdit: (quote: QuoteResponse) => void
}

export function QuoteDetail({ id, onBack, onEdit }: Props) {
  const loader = useCallback(() => getQuote(id), [id])
  const { data: quote, error, loading, reload } = useLoad(loader)

  return (
    <section>
      <div className="toolbar">
        <h2>見積詳細</h2>
        <button type="button" onClick={onBack}>
          一覧へ戻る
        </button>
      </div>
      {loading && <p>読み込み中...</p>}
      {error && <p className="error">{error}</p>}
      {quote && (
        <>
          <dl className="summary">
            <dt>ID</dt>
            <dd>{quote.id}</dd>
            <dt>状態</dt>
            <dd>
              <StatusBadge status={quote.status} />
            </dd>
            <dt>契約種別</dt>
            <dd>
              <ContractTypeBadge type={quote.contractType} />
            </dd>
            <dt>契約期間</dt>
            <dd>
              {quote.contractStart} - {quote.contractEnd}
            </dd>
            <dt>有効期限</dt>
            <dd>{quote.validUntil}</dd>
            <dt>割引率</dt>
            <dd>{quote.discountRate}%</dd>
            <dt>年額合計 (数量込み・1年分)</dt>
            <dd>{formatYen(quote.totalAnnual)}</dd>
            <dt>合計金額 (契約期間の総額)</dt>
            <dd>{formatYen(quote.totalAmount)}</dd>
          </dl>
          <table>
            <thead>
              <tr>
                <th>SKU</th>
                <th className="num">月額単価</th>
                <th className="num">数量</th>
                <th className="num">年額 (数量込み)</th>
              </tr>
            </thead>
            <tbody>
              {quote.skuList.map((line, index) => (
                <tr key={index}>
                  <td>{line.sku}</td>
                  <td className="num">{formatYen(line.listPrice)}</td>
                  <td className="num">{line.amount}</td>
                  <td className="num">{formatYen(line.annual)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <QuoteActions quote={quote} onEdit={onEdit} onSaved={reload} onDeleted={onBack} />
        </>
      )}
    </section>
  )
}
