import { useState, useRef, useEffect } from 'react'
import { parseCertificate, getSuggestedAlias, isValidPemCertificate } from '../utils/certificateUtils'
import { verifyCertificate } from '../utils/verificationUtils'
import { fetchCertificates } from '../services/tlsService'

export const useCertificateImport = (targetStore, currentCertificates = null) => {
  // State management
  const [alias, setAlias] = useState('')
  const [pemText, setPemText] = useState('')
  const [privateKeyText, setPrivateKeyText] = useState('')
  const [file, setFile] = useState(null)
  const [privateKeyFile, setPrivateKeyFile] = useState(null)
  const [loading, setLoading] = useState(false)
  const [apiError, setApiError] = useState(null)
  const [errors, setErrors] = useState({})
  const [certificateDetails, setCertificateDetails] = useState(null)
  const [verificationResult, setVerificationResult] = useState(null)
  const [isVerifying, setIsVerifying] = useState(false)
  const [existingCertificates, setExistingCertificates] = useState(currentCertificates || [])
  const [aliasWarning, setAliasWarning] = useState(null)

  // Refs
  const fileInputRef = useRef(null)
  const privateKeyFileInputRef = useRef(null)

  const fileAccept = '.pem,.key,text/plain,application/x-pem-file'

  // Load existing certificates to check for alias conflicts
  const loadExistingCertificates = async () => {
    // If currentCertificates were provided, use them instead of fetching
    if (currentCertificates && currentCertificates.length > 0) {
      setExistingCertificates(currentCertificates)
      return
    }
    
    try {
      const certificates = await fetchCertificates()
      setExistingCertificates(certificates)
    } catch (error) {
      console.error('Failed to load existing certificates:', error)
    }
  }
  
  // Update existingCertificates when currentCertificates prop changes
  useEffect(() => {
    if (currentCertificates && currentCertificates.length > 0) {
      setExistingCertificates(currentCertificates)
    }
  }, [currentCertificates])

  // Check if alias already exists within the target store
  const checkAliasExists = (aliasToCheck) => {
    if (!aliasToCheck.trim()) {
      setAliasWarning(null)
      return false
    }

    const exists = existingCertificates.some(cert => 
      cert.store === targetStore &&
      cert.alias.toLowerCase() === aliasToCheck.toLowerCase()
    )
    
    if (exists) {
      setAliasWarning('This alias is already in use in this store')
      return true
    } else {
      setAliasWarning(null)
      return false
    }
  }



  // Parse certificate details when PEM text changes
  const parseCertificateDetails = async (pemText) => {
    if (!pemText.trim() || !isValidPemCertificate(pemText)) {
      setCertificateDetails(null)
      setVerificationResult(null)
      return
    }

    try {
      const details = parseCertificate(pemText)
      setCertificateDetails(details)
      
      // Auto-complete alias if it's empty
      if (!alias.trim()) {
        const suggestedAlias = getSuggestedAlias(details)
        if (suggestedAlias) {
          setAlias(suggestedAlias)
          // Check for conflicts immediately after setting the suggested alias
          checkAliasExists(suggestedAlias)
        }
      } else {
        // If alias is already set, check for conflicts
        checkAliasExists(alias)
      }

      // Auto-verify certificate
      await performAutoVerification(pemText)
    } catch (error) {
      console.error('Failed to parse certificate details:', error)
      setCertificateDetails(null)
      setVerificationResult(null)
    }
  }

  // Perform auto-verification
  const performAutoVerification = async (pemText, privateKeyPem = null) => {
    if (!pemText.trim()) return

    setIsVerifying(true)
    try {
      // Use provided private key or current state
      const keyToUse = privateKeyPem !== null ? privateKeyPem : 
        (targetStore === 'private' && privateKeyText.trim() ? privateKeyText : null)
      
      const result = await verifyCertificate(pemText, keyToUse)
      setVerificationResult(result)
    } catch (error) {
      setVerificationResult({
        success: false,
        error: `Auto-verification failed: ${error.message}`
      })
    } finally {
      setIsVerifying(false)
    }
  }

  // Handle certificate verification
  const handleVerifyCertificate = async () => {
    if (!pemText.trim()) return

    setIsVerifying(true)
    try {
      const result = await verifyCertificate(pemText, privateKeyText || null)
      setVerificationResult(result)
    } catch (error) {
      setVerificationResult({
        success: false,
        error: error.message || 'Verification failed'
      })
    } finally {
      setIsVerifying(false)
    }
  }

  // Validation logic
  const validate = () => {
    const nextErrors = {}
    if (!pemText.trim()) {
      nextErrors.pemText = 'PEM content is required.'
    }
    if (!alias.trim()) {
      nextErrors.alias = 'Alias is required.'
    }
    if (targetStore === 'private' && !privateKeyText.trim()) {
      nextErrors.privateKeyText = 'Private key is required for private store.'
    }
    setErrors(nextErrors)
    return Object.keys(nextErrors).length === 0
  }

  // Handle file upload
  const handleFileUpload = async (e) => {
    try {
      const f = e.target.files && e.target.files[0]
      setFile(f || null)
      if (!f) return
      const name = (f.name || '').toLowerCase()
      if (!(name.endsWith('.pem') || name.endsWith('.key') || f.type === 'text/plain' || f.type === 'application/x-pem-file')) {
        setErrors((prev) => ({ ...prev, file: 'Please select a .pem or .key file.' }))
        return
      }
      const text = await f.text()
      setPemText(text)
      await parseCertificateDetails(text) // This will now auto-verify and check for alias conflicts
      setErrors((prev) => ({ ...prev, file: undefined, pemText: undefined }))
    } catch (err) {
      setErrors((prev) => ({ ...prev, file: 'Failed to read file.' }))
    }
  }

  // Handle private key file upload
  const handlePrivateKeyFileUpload = async (e) => {
    try {
      const f = e.target.files && e.target.files[0]
      setPrivateKeyFile(f || null)
      if (!f) return
      const name = (f.name || '').toLowerCase()
      if (!(name.endsWith('.pem') || name.endsWith('.key') || f.type === 'text/plain' || f.type === 'application/x-pem-file')) {
        setErrors((prev) => ({ ...prev, privateKeyFile: 'Please select a .pem or .key file.' }))
        return
      }
      const text = await f.text()
      setPrivateKeyText(text)
      
      // Auto-verify if certificate is already present
      if (pemText.trim()) {
        await performAutoVerification(pemText, text)
      }
      
      setErrors((prev) => ({ ...prev, privateKeyFile: undefined, privateKeyText: undefined }))
    } catch (err) {
      setErrors((prev) => ({ ...prev, privateKeyFile: 'Failed to read file.' }))
    }
  }

  // Handle PEM text change
  const handlePemTextChange = async (e) => {
    setPemText(e.target.value)
    await parseCertificateDetails(e.target.value) // This will now auto-verify and check for alias conflicts
  }

  // Handle private key text change
  const handlePrivateKeyTextChange = async (e) => {
    const newPrivateKeyText = e.target.value
    setPrivateKeyText(newPrivateKeyText)
    
    // Auto-verify if certificate is already present
    if (pemText.trim()) {
      await performAutoVerification(pemText, newPrivateKeyText)
    }
  }

  // Handle alias change
  const handleAliasChange = (e) => {
    const newAlias = e.target.value
    setAlias(newAlias)
    checkAliasExists(newAlias)
  }

  return {
    // State
    alias,
    pemText,
    privateKeyText,
    file,
    privateKeyFile,
    loading,
    apiError,
    errors,
    certificateDetails,
    verificationResult,
    isVerifying,
    existingCertificates,
    aliasWarning,
    
    // Refs
    fileInputRef,
    privateKeyFileInputRef,
    fileAccept,
    
    // Actions
    setAlias,
    setPemText,
    setPrivateKeyText,
    setFile,
    setPrivateKeyFile,
    setLoading,
    setApiError,
    setErrors,
    setCertificateDetails,
    setVerificationResult,
    setIsVerifying,
    
    // Handlers
    handleVerifyCertificate,
    handleFileUpload,
    handlePrivateKeyFileUpload,
    handlePemTextChange,
    handlePrivateKeyTextChange,
    handleAliasChange,
    parseCertificateDetails,
    validate,
    loadExistingCertificates,
    checkAliasExists,
    performAutoVerification
  }
}
