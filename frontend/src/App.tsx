import { useState } from 'react'
import type { QuoteResponse } from './api/types'
import { QuoteDetail } from './components/QuoteDetail'
import { QuoteForm } from './components/QuoteForm'
import { QuoteList } from './components/QuoteList'

type View =
  | { name: 'list' }
  | { name: 'detail'; id: string }
  | { name: 'form'; quote?: QuoteResponse }

export default function App() {
  const [view, setView] = useState<View>({ name: 'list' })

  const showList = () => setView({ name: 'list' })
  const showDetail = (id: string) => setView({ name: 'detail', id })
  const showForm = (quote?: QuoteResponse) => setView({ name: 'form', quote })

  return (
    <>
      <header className="app-header">
        <div className="app-header-inner">
          <span className="logo">Q</span>
          <h1>見積管理</h1>
        </div>
      </header>
      <main>
        {view.name === 'list' && (
          <QuoteList onCreate={() => showForm()} onOpen={showDetail} onEdit={showForm} />
        )}
        {view.name === 'detail' && <QuoteDetail id={view.id} onBack={showList} onEdit={showForm} />}
        {view.name === 'form' && (
          <QuoteForm
            key={view.quote?.id ?? 'new'}
            initial={view.quote}
            onDone={(quote) => showDetail(quote.id)}
            onCancel={showList}
          />
        )}
      </main>
    </>
  )
}
