import { useState, useRef } from 'react'
import { parseCertificate, pemToBase64, isValidPemCertificate } from '../utils/certificateUtils'
import { verifyCertificate } from '../utils/verificationUtils'

export const useCertificateImport = (targetStore) => {
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

  // Refs
  const fileInputRef = useRef(null)
  const privateKeyFileInputRef = useRef(null)

  const fileAccept = '.pem,.key,text/plain,application/x-pem-file'

  // Get suggested alias from certificate details
  const getSuggestedAlias = (details) => {
    if (!details) return null
    
    // Try to get CN from subject
    const subjectStr = details.subjectStr || ''
    const cnMatch = subjectStr.match(/CN=([^,]+)/)
    if (cnMatch && cnMatch[1]) {
      return cnMatch[1].trim()
    }
    
    // Try to get first DNS name from SAN
    if (details.raw && details.raw.extensions) {
      const sanExtension = details.raw.extensions.find(ext => ext.name === 'subjectAltName')
      if (sanExtension && sanExtension.altNames) {
        const dnsName = sanExtension.altNames.find(altName => altName.type === 2) // DNS type
        if (dnsName && dnsName.value) {
          return dnsName.value.trim()
        }
      }
    }
    
    // Fallback to first part of subject
    const firstPart = subjectStr.split(',')[0]
    if (firstPart && firstPart.includes('=')) {
      return firstPart.split('=')[1]?.trim()
    }
    
    return null
  }

  // Parse certificate details when PEM text changes
  const parseCertificateDetails = (pemText) => {
    if (!pemText.trim() || !isValidPemCertificate(pemText)) {
      setCertificateDetails(null)
      return
    }

    try {
      const details = parseCertificate(pemToBase64(pemText))
      setCertificateDetails(details)
      
      // Auto-complete alias if it's empty
      if (!alias.trim()) {
        const suggestedAlias = getSuggestedAlias(details)
        if (suggestedAlias) {
          setAlias(suggestedAlias)
        }
      }
    } catch (error) {
      console.error('Failed to parse certificate details:', error)
      setCertificateDetails(null)
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
      parseCertificateDetails(text)
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
      setErrors((prev) => ({ ...prev, privateKeyFile: undefined, privateKeyText: undefined }))
    } catch (err) {
      setErrors((prev) => ({ ...prev, privateKeyFile: 'Failed to read file.' }))
    }
  }

  // Handle PEM text change
  const handlePemTextChange = (e) => {
    setPemText(e.target.value)
    parseCertificateDetails(e.target.value)
  }

  // Handle private key text change
  const handlePrivateKeyTextChange = (e) => {
    setPrivateKeyText(e.target.value)
  }

  // Handle alias change
  const handleAliasChange = (e) => {
    setAlias(e.target.value)
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
    validate
  }
}
