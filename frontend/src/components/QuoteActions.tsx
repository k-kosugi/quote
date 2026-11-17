import { useState } from 'react'
import { deleteQuote, messageOf, saveQuote } from '../api/client'
import type { QuoteResponse } from '../api/types'

interface Props {
  quote: QuoteResponse
  onEdit: (quote: QuoteResponse) => void
  onSaved: (quote: QuoteResponse) => void
  onDeleted: (id: string) => void
}

export function QuoteActions({ quote, onEdit, onSaved, onDeleted }: Props) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string>()

  const run = async (action: () => Promise<void>) => {
    setBusy(true)
    setError(undefined)
    try {
      await action()
    } catch (e) {
      setError(messageOf(e))
    } finally {
      setBusy(false)
    }
  }

  const handleSave = () => run(async () => onSaved(await saveQuote(quote.id)))

  const handleDelete = () => {
    if (!window.confirm('この見積を削除しますか?')) return
    return run(async () => {
      await deleteQuote(quote.id)
      onDeleted(quote.id)
    })
  }

  return (
    <div className="actions">
      <button type="button" onClick={() => onEdit(quote)} disabled={busy}>
        編集
      </button>
      {quote.status === 'DRAFT' && (
        <button type="button" onClick={handleSave} disabled={busy}>
          保存
        </button>
      )}
      <button type="button" className="danger" onClick={handleDelete} disabled={busy}>
        削除
      </button>
      {error && <span className="error">{error}</span>}
    </div>
  )
}
