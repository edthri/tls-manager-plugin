import { useState, useEffect } from 'react'
import { fetchCertificates } from '../services/tlsService'

export const useAliasEdit = (currentAlias, currentStore) => {
  // State management
  const [newAlias, setNewAlias] = useState('')
  const [existingCertificates, setExistingCertificates] = useState([])
  const [aliasWarning, setAliasWarning] = useState(null)
  const [loading, setLoading] = useState(false)
  const [apiError, setApiError] = useState(null)

  // Load existing certificates to check for alias conflicts
  const loadExistingCertificates = async () => {
    try {
      const certificates = await fetchCertificates()
      setExistingCertificates(certificates)
    } catch (error) {
      console.error('Failed to load existing certificates:', error)
    }
  }

  // Check if alias already exists within the same store (excluding current certificate)
  const checkAliasExists = (aliasToCheck) => {
    if (!aliasToCheck.trim()) {
      setAliasWarning(null)
      return false
    }

    // Only check certificates in the same store, excluding the current certificate
    const exists = existingCertificates.some(cert => 
      cert.store === currentStore &&
      cert.alias.toLowerCase() === aliasToCheck.toLowerCase() && 
      cert.alias.toLowerCase() !== currentAlias.toLowerCase()
    )
    
    if (exists) {
      setAliasWarning('This alias is already in use in this store')
      return true
    } else {
      setAliasWarning(null)
      return false
    }
  }

  // Handle alias change
  const handleAliasChange = (e) => {
    const newValue = e.target.value
    setNewAlias(newValue)
    checkAliasExists(newValue)
  }

  // Validation logic
  const validate = () => {
    if (!newAlias.trim()) {
      setApiError('Alias is required.')
      return false
    }
    if (newAlias.trim() === currentAlias) {
      setApiError('New alias must be different from current alias.')
      return false
    }
    return true
  }

  // Initialize with current alias
  useEffect(() => {
    setNewAlias(currentAlias)
  }, [currentAlias])

  // Load existing certificates on mount
  useEffect(() => {
    loadExistingCertificates()
  }, [])

  return {
    // State
    newAlias,
    aliasWarning,
    loading,
    apiError,
    existingCertificates,
    
    // Actions
    setLoading,
    setApiError,
    
    // Handlers
    handleAliasChange,
    validate,
    checkAliasExists,
    loadExistingCertificates
  }
}
