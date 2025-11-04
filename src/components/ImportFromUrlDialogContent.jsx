import React, { useState } from 'react'
import {
  Box,
  Stack,
  Button,
  TextField,
  RadioGroup,
  Radio,
  FormControlLabel,
  FormControl,
  Typography,
  Alert,
  CircularProgress,
  Paper,
  Divider
} from '@mui/material'
import { fetchRemoteCertificates } from '../services/tlsService.js'
import ImportCertificateDialogContent from './ImportCertificateDialogContent'
import StatusPill from './StatusPill'

export default function ImportFromUrlDialogContent({
  targetStore = 'trusted',
  currentCertificates = null,
  onCancel,
  onSuccess,
}) {
  const [step, setStep] = useState('url-input') // 'url-input' | 'certificate-selection' | 'import'
  const [url, setUrl] = useState('')
  const [urlError, setUrlError] = useState('')
  const [loading, setLoading] = useState(false)
  const [fetchError, setFetchError] = useState(null)
  const [certificates, setCertificates] = useState([])
  const [selectedCertificateIndex, setSelectedCertificateIndex] = useState(null)
  const [selectedCertificatePem, setSelectedCertificatePem] = useState(null)

  const validateUrl = (urlValue) => {
    if (!urlValue.trim()) {
      setUrlError('URL is required')
      return false
    }
    if (!urlValue.startsWith('https://')) {
      setUrlError('URL must start with https://')
      return false
    }
    try {
      new URL(urlValue)
      setUrlError('')
      return true
    } catch (e) {
      setUrlError('Invalid URL format')
      return false
    }
  }

  const handleUrlChange = (e) => {
    const newUrl = e.target.value
    setUrl(newUrl)
    if (urlError) {
      validateUrl(newUrl)
    }
  }

  const handleFetchCertificates = async () => {
    if (!validateUrl(url)) {
      return
    }

    setLoading(true)
    setFetchError(null)
    setCertificates([])
    setSelectedCertificateIndex(null)

    try {
      const fetchedCerts = await fetchRemoteCertificates(url)
      if (fetchedCerts.length === 0) {
        setFetchError('No certificates found at the specified URL')
        setLoading(false)
        return
      }
      setCertificates(fetchedCerts)
      setStep('certificate-selection')
    } catch (error) {
      setFetchError(error.message || 'Failed to fetch certificates from URL')
    } finally {
      setLoading(false)
    }
  }

  const handleCertificateSelect = (index) => {
    setSelectedCertificateIndex(index)
    const selectedCert = certificates[index]
    setSelectedCertificatePem(selectedCert.certificate)
  }

  const handleContinue = () => {
    if (selectedCertificateIndex === null || selectedCertificatePem === null) {
      return
    }
    setStep('import')
  }

  const handleBack = () => {
    if (step === 'certificate-selection') {
      setStep('url-input')
      setCertificates([])
      setSelectedCertificateIndex(null)
      setSelectedCertificatePem(null)
    } else if (step === 'import') {
      setStep('certificate-selection')
    }
  }

  // Step 1: URL Input
  if (step === 'url-input') {
    return (
      <Box sx={{ pt: 0.5, display: 'flex', flexDirection: 'column', gap: 2 }}>
        <TextField
          label="URL"
          placeholder="https://example.com"
          value={url}
          onChange={handleUrlChange}
          onBlur={() => validateUrl(url)}
          error={!!urlError}
          helperText={urlError || 'Enter a valid HTTPS URL to fetch certificates'}
          fullWidth
          disabled={loading}
          autoFocus
        />
        
        {fetchError && (
          <Alert severity="error">{fetchError}</Alert>
        )}

        <Stack direction="row" spacing={1} justifyContent="flex-end" sx={{ pt: 2 }}>
          <Button onClick={onCancel} disabled={loading}>
            Cancel
          </Button>
          <Button
            variant="contained"
            onClick={handleFetchCertificates}
            disabled={loading || !url.trim() || !!urlError}
          >
            {loading ? (
              <>
                <CircularProgress size={16} sx={{ mr: 1 }} />
                Fetching...
              </>
            ) : (
              'Fetch Certificates'
            )}
          </Button>
        </Stack>
      </Box>
    )
  }

  // Step 2: Certificate Selection
  if (step === 'certificate-selection') {
    return (
      <Box sx={{ pt: 0.5, display: 'flex', flexDirection: 'column', gap: 2 }}>
        <Typography variant="body2" color="text.secondary">
          Select a certificate to import:
        </Typography>

        {certificates.length === 0 ? (
          <Alert severity="info">No certificates found</Alert>
        ) : (
          <FormControl component="fieldset" sx={{ width: '100%' }}>
            <RadioGroup
              value={selectedCertificateIndex}
              onChange={(e) => handleCertificateSelect(parseInt(e.target.value, 10))}
            >
              <Stack spacing={2}>
                {certificates.map((cert, index) => (
                  <Paper
                    key={index}
                    variant="outlined"
                    sx={{
                      p: 2,
                      borderRadius: 2,
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                      '&:hover': {
                        backgroundColor: 'action.hover'
                      },
                      ...(selectedCertificateIndex === index && {
                        borderColor: 'primary.main',
                        borderWidth: 2,
                        backgroundColor: 'action.selected'
                      })
                    }}
                    onClick={() => handleCertificateSelect(index)}
                  >
                    <FormControlLabel
                      value={index}
                      control={<Radio />}
                      label={
                        <Box sx={{ width: '100%', ml: 1 }}>
                          <Stack spacing={1}>
                            <Stack direction="row" spacing={2} alignItems="center" justifyContent="space-between">
                              <Box>
                                <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                                  {cert.alias}
                                </Typography>
                                <Typography variant="body2" color="text.secondary">
                                  {cert.type}
                                </Typography>
                              </Box>
                              <StatusPill validFrom={cert.validFrom} validTo={cert.validTo} />
                            </Stack>
                            <Divider />
                            <Stack spacing={0.5}>
                              <Typography variant="body2">
                                <strong>Subject:</strong> {cert.subject}
                              </Typography>
                              <Typography variant="body2">
                                <strong>Issuer:</strong> {cert.issuer}
                              </Typography>
                              {cert.validFrom !== 'Unknown' && cert.validTo !== 'Unknown' && (
                                <Typography variant="body2">
                                  <strong>Valid:</strong> {cert.validFrom} - {cert.validTo}
                                </Typography>
                              )}
                            </Stack>
                            {cert.error && (
                              <Alert severity="warning" sx={{ mt: 1 }}>
                                {cert.subject}
                              </Alert>
                            )}
                          </Stack>
                        </Box>
                      }
                      sx={{ margin: 0, width: '100%' }}
                    />
                  </Paper>
                ))}
              </Stack>
            </RadioGroup>
          </FormControl>
        )}

        <Stack direction="row" spacing={1} justifyContent="flex-end" sx={{ pt: 2 }}>
          <Button onClick={handleBack} disabled={loading}>
            Back
          </Button>
          <Button onClick={onCancel} disabled={loading}>
            Cancel
          </Button>
          <Button
            variant="contained"
            onClick={handleContinue}
            disabled={loading || selectedCertificateIndex === null}
          >
            Continue
          </Button>
        </Stack>
      </Box>
    )
  }

  // Step 3: Import Dialog (reuse existing component)
  if (step === 'import' && selectedCertificatePem) {
    return (
      <ImportCertificateDialogContent
        targetStore={targetStore}
        currentCertificates={currentCertificates}
        onCancel={onCancel}
        onSuccess={onSuccess}
        initialPemText={selectedCertificatePem}
        readOnlyPem={true}
      />
    )
  }

  return null
}

