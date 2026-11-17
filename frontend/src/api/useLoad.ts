import { useCallback, useEffect, useState } from 'react'
import { messageOf } from './client'

interface LoadState<T> {
  data?: T
  error?: string
  loading: boolean
}

// loader は参照が安定している関数を渡すこと
export function useLoad<T>(loader: () => Promise<T>) {
  const [state, setState] = useState<LoadState<T>>({ loading: true })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let cancelled = false
    loader()
      .then((data) => !cancelled && setState({ data, loading: false }))
      .catch((error) => !cancelled && setState({ error: messageOf(error), loading: false }))
    return () => {
      cancelled = true
    }
  }, [loader, version])

  const reload = useCallback(() => {
    setState({ loading: true })
    setVersion((v) => v + 1)
  }, [])

  return { ...state, reload }
}
