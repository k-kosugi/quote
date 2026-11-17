import { listSkus } from '../api/client'
import type { SkuResponse } from '../api/types'
import { useLoad } from '../api/useLoad'
import { formatYen } from '../format'

interface Props {
  onSelect: (sku: SkuResponse) => void
  onClose: () => void
}

export function SkuPicker({ onSelect, onClose }: Props) {
  const { data, error, loading } = useLoad(listSkus)

  return (
    <div className="panel">
      <div className="toolbar">
        <h3>SKU カタログ</h3>
        <button type="button" onClick={onClose}>
          閉じる
        </button>
      </div>
      {loading && <p>読み込み中...</p>}
      {error && <p className="error">{error}</p>}
      {data?.length === 0 && <p>SKU がありません.</p>}
      {data && data.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>SKU</th>
              <th>説明</th>
              <th className="num">月額単価</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {data.map((sku) => (
              <tr key={sku.id}>
                <td>{sku.id}</td>
                <td>{sku.description}</td>
                <td className="num">{formatYen(sku.listPrice)}</td>
                <td>
                  <button type="button" onClick={() => onSelect(sku)}>
                    追加
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
