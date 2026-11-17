import { listQuotes } from '../api/client'
import type { QuoteResponse } from '../api/types'
import { useLoad } from '../api/useLoad'
import { formatYen } from '../format'
import { ContractTypeBadge, StatusBadge } from './Badges'
import { QuoteActions } from './QuoteActions'

interface Props {
  onCreate: () => void
  onOpen: (id: string) => void
  onEdit: (quote: QuoteResponse) => void
}

export function QuoteList({ onCreate, onOpen, onEdit }: Props) {
  const { data, error, loading, reload } = useLoad(listQuotes)

  return (
    <section>
      <div className="toolbar">
        <h2>見積一覧</h2>
        <button type="button" onClick={reload}>
          再読み込み
        </button>
        <button type="button" className="primary" onClick={onCreate}>
          新規作成
        </button>
      </div>
      {loading && <p>読み込み中...</p>}
      {error && <p className="error">{error}</p>}
      {data?.length === 0 && <p>見積はまだありません.</p>}
      {data && data.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>状態</th>
              <th>契約種別</th>
              <th>契約期間</th>
              <th>有効期限</th>
              <th className="num">合計金額 (契約期間)</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {data.map((quote) => (
              <tr key={quote.id}>
                <td>
                  <button type="button" className="link" onClick={() => onOpen(quote.id)}>
                    {quote.id.slice(0, 8)}
                  </button>
                </td>
                <td>
                  <StatusBadge status={quote.status} />
                </td>
                <td>
                  <ContractTypeBadge type={quote.contractType} />
                </td>
                <td>
                  {quote.contractStart} - {quote.contractEnd}
                </td>
                <td>{quote.validUntil}</td>
                <td className="num">{formatYen(quote.totalAmount)}</td>
                <td>
                  <QuoteActions
                    quote={quote}
                    onEdit={onEdit}
                    onSaved={reload}
                    onDeleted={reload}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
