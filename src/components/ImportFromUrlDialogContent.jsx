import React, { useState, useEffect } from 'react'
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
    setSelectedCertificatePem(null)

    try {
      const fetchedCerts = await fetchRemoteCertificates(url)
      if (fetchedCerts.length === 0) {
        setFetchError('No certificates found at the specified URL')
        setLoading(false)
        return
      }
      setCertificates(fetchedCerts)
      // Auto-select first certificate if available
      if (fetchedCerts.length > 0) {
        setSelectedCertificateIndex(0)
        setSelectedCertificatePem(fetchedCerts[0].certificate)
      }
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

  // Update selected certificate PEM when index changes
  useEffect(() => {
    if (selectedCertificateIndex !== null && certificates[selectedCertificateIndex]) {
      setSelectedCertificatePem(certificates[selectedCertificateIndex].certificate)
    }
  }, [selectedCertificateIndex, certificates])

  return (
    <Box sx={{ 
      pt: 0.5,
      maxHeight: '80vh',
      display: 'flex',
      flexDirection: 'column'
    }}>
      {/* URL Input Section */}
      <Box sx={{ mb: 3 }}>
        <Stack direction="row" spacing={2} alignItems="flex-start">
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
          <Button
            variant="contained"
            onClick={handleFetchCertificates}
            disabled={loading || !url.trim() || !!urlError}
            sx={{ minWidth: 150, mt: 1 }}
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
        
        {fetchError && (
          <Alert severity="error" sx={{ mt: 2 }}>{fetchError}</Alert>
        )}
      </Box>

      {/* Certificate List and Import Details - Vertical Layout */}
      {certificates.length > 0 && (
        <Box sx={{ 
          flex: 1,
          overflow: 'auto',
          minHeight: 0,
          display: 'flex',
          flexDirection: 'column',
          gap: 3
        }}>
          {/* Top Section - Certificate List */}
          <Box sx={{ 
            flex: '0 0 auto',
            pb: 3,
            borderBottom: '1px solid',
            borderColor: 'divider'
          }}>
            <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 600 }}>
              Select a certificate to import:
            </Typography>

            <FormControl component="fieldset" sx={{ width: '100%' }}>
              <RadioGroup
                value={selectedCertificateIndex !== null ? selectedCertificateIndex.toString() : ''}
                onChange={(e) => handleCertificateSelect(parseInt(e.target.value, 10))}
                sx={{ display: 'flex', flexDirection: 'row', flexWrap: 'wrap', gap: 2 }}
              >
                {certificates.map((cert, index) => (
                  <Paper
                    key={index}
                    variant="outlined"
                    sx={{
                      p: 2,
                      borderRadius: 2,
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                      minWidth: 200,
                      flex: '1 1 auto',
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
                      value={index.toString()}
                      control={<Radio />}
                      label={
                        <Box sx={{ width: '100%', ml: 1 }}>
                          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                            {cert.alias || `Certificate ${index + 1}`}
                          </Typography>
                          {cert.error && (
                            <Alert severity="warning" sx={{ mt: 1 }}>
                              {cert.subject}
                            </Alert>
                          )}
                        </Box>
                      }
                      sx={{ margin: 0, width: '100%' }}
                    />
                  </Paper>
                ))}
              </RadioGroup>
            </FormControl>
          </Box>

          {/* Bottom Section - Import Certificate Details */}
          <Box sx={{ 
            flex: 1,
            minHeight: 0,
            overflow: 'auto'
          }}>
            {selectedCertificatePem ? (
              <ImportCertificateDialogContent
                key={selectedCertificateIndex}
                targetStore={targetStore}
                currentCertificates={currentCertificates}
                onCancel={onCancel}
                onSuccess={onSuccess}
                initialPemText={selectedCertificatePem}
                readOnlyPem={true}
              />
            ) : (
              <Alert severity="info">
                Select a certificate from the list to view details and import
              </Alert>
            )}
          </Box>
        </Box>
      )}

      {/* Bottom Action Buttons - Only show when certificates are loaded */}
      {certificates.length > 0 && (
        <Stack 
          direction="row" 
          spacing={1} 
          justifyContent="flex-end" 
          sx={{ 
            pt: 2, 
            borderTop: '1px solid', 
            borderColor: 'divider',
            mt: 'auto'
          }}
        >
          <Button onClick={onCancel} disabled={loading}>
            Cancel
          </Button>
        </Stack>
      )}
    </Box>
  )
}

