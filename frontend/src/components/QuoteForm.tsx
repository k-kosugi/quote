import { useState } from 'react'
import { createQuote, messageOf, reviseQuote, saveQuote } from '../api/client'
import type { QuoteResponse, SkuResponse } from '../api/types'
import { formatYen } from '../format'
import {
  type LineDraft,
  type QuoteDraft,
  draftFromQuote,
  emptyDraft,
  newLine,
  toRequest,
} from '../quoteDraft'
import { SkuPicker } from './SkuPicker'

interface Props {
  initial?: QuoteResponse
  onDone: (quote: QuoteResponse) => void
  onCancel: () => void
}

export function QuoteForm({ initial, onDone, onCancel }: Props) {
  const [draft, setDraft] = useState<QuoteDraft>(() => (initial ? draftFromQuote(initial) : emptyDraft()))
  // 仮保存に成功したが保存に失敗した場合, 再試行時に二重作成しないよう ID を保持する
  const [quoteId, setQuoteId] = useState(initial?.id)
  const [pickerOpen, setPickerOpen] = useState(false)
  const [errors, setErrors] = useState<string[]>([])
  const [busy, setBusy] = useState(false)

  const updateField = <K extends keyof QuoteDraft>(key: K, value: QuoteDraft[K]) =>
    setDraft((current) => ({ ...current, [key]: value }))

  const updateLine = (key: string, patch: Partial<LineDraft>) =>
    updateField(
      'lines',
      draft.lines.map((line) => (line.key === key ? { ...line, ...patch } : line)),
    )

  const removeLine = (key: string) =>
    updateField(
      'lines',
      draft.lines.filter((line) => line.key !== key),
    )

  const addSku = (sku: SkuResponse) =>
    updateField('lines', [...draft.lines, newLine(sku.id, String(sku.listPrice))])

  const submit = async (finalize: boolean) => {
    const result = toRequest(draft)
    if (result.errors) {
      setErrors(result.errors)
      return
    }
    setBusy(true)
    setErrors([])
    try {
      let quote = quoteId ? await reviseQuote(quoteId, result.request) : await createQuote(result.request)
      setQuoteId(quote.id)
      if (finalize) quote = await saveQuote(quote.id)
      onDone(quote)
    } catch (e) {
      setErrors([messageOf(e)])
    } finally {
      setBusy(false)
    }
  }

  return (
    <section>
      <h2>{quoteId ? '見積を編集' : '見積を作成'}</h2>
      <form onSubmit={(e) => e.preventDefault()}>
        <div className="grid">
          <label>
            契約開始日
            <input
              type="date"
              value={draft.contractStart}
              onChange={(e) => updateField('contractStart', e.target.value)}
            />
          </label>
          <label>
            契約終了日
            <input
              type="date"
              value={draft.contractEnd}
              onChange={(e) => updateField('contractEnd', e.target.value)}
            />
          </label>
          <label>
            割引率 (%)
            <input
              type="number"
              min={0}
              max={100}
              step="any"
              value={draft.discountRate}
              onChange={(e) => updateField('discountRate', e.target.value)}
            />
          </label>
        </div>

        <div className="toolbar">
          <h3>SKU 明細</h3>
          <button type="button" onClick={() => setPickerOpen((open) => !open)}>
            SKU を追加
          </button>
        </div>
        {pickerOpen && <SkuPicker onSelect={addSku} onClose={() => setPickerOpen(false)} />}

        {draft.lines.length === 0 ? (
          <p>SKU が追加されていません.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>SKU</th>
                <th className="num">定価 (月額)</th>
                <th>数量 (解約は負数)</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {draft.lines.map((line) => (
                <tr key={line.key}>
                  <td>
                    <strong>{line.sku}</strong>
                  </td>
                  <td className="num">{formatYen(Number(line.listPrice))}</td>
                  <td>
                    <input
                      aria-label="数量"
                      type="number"
                      step={1}
                      value={line.amount}
                      onChange={(e) => updateLine(line.key, { amount: e.target.value })}
                    />
                  </td>
                  <td>
                    <button type="button" className="danger" onClick={() => removeLine(line.key)}>
                      削除
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {errors.length > 0 && (
          <ul className="error">
            {errors.map((message) => (
              <li key={message}>{message}</li>
            ))}
          </ul>
        )}

        <div className="actions">
          <button type="button" onClick={() => submit(false)} disabled={busy}>
            仮保存
          </button>
          <button type="button" className="primary" onClick={() => submit(true)} disabled={busy}>
            保存
          </button>
          <button type="button" onClick={onCancel} disabled={busy}>
            キャンセル
          </button>
        </div>
      </form>
    </section>
  )
}
