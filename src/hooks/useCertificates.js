import { useEffect, useMemo, useState } from 'react'
import { fetchCertificates } from '../services/sslService'

function normalize(text) {
  return (text || '').toLowerCase()
}

export default function useCertificates() {
  const [all, setAll] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadCertificates = async () => {
    setLoading(true)
    setError('')
    try {
      const data = await fetchCertificates()
      setAll(data)
    } catch (e) {
      setError('Failed to load certificates')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let cancelled = false
    async function load() {
      await loadCertificates()
    }
    load()
    return () => { cancelled = true }
  }, [])

  const filterBy = (storeKey, search) => {
    const q = normalize(search)
    return all.filter((c) => c.store === storeKey).filter((c) => {
      if (!q) return true
      return normalize(c.alias).includes(q) || normalize(c.name).includes(q) || normalize(c.subject).includes(q)
    })
  }

  const counts = useMemo(() => ({
    native: all.filter((c) => c.store === 'native').length,
    trusted: all.filter((c) => c.store === 'trusted').length,
    private: all.filter((c) => c.store === 'private').length,
  }), [all])

  return { all, loading, error, counts, filterBy, refetch: loadCertificates }
}


