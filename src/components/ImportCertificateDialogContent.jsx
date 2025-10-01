import React, { useRef, useState } from 'react'
import { Box, Button, FormHelperText, Stack, TextField, Typography, Alert } from '@mui/material'
import { pemToBase64, privateKeyPemToBase64, isValidPemCertificate, isValidPemPrivateKey } from '../utils/certificateUtils.js'
import { updateCertificates } from '../services/tlsService.js'

export default function ImportCertificateDialogContent({
  targetStore = 'trusted',
  onCancel,
  onSubmit,
  onSuccess,
}) {
  const [pemText, setPemText] = useState('')
  const [file, setFile] = useState(null)
  const [alias, setAlias] = useState('')
  const [privateKeyText, setPrivateKeyText] = useState('')
  const [privateKeyFile, setPrivateKeyFile] = useState(null)
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [apiError, setApiError] = useState(null)

  const fileInputRef = useRef(null)
  const privateKeyFileInputRef = useRef(null)

  const fileAccept = '.pem,.key,text/plain,application/x-pem-file'

  const validate = () => {
    const nextErrors = {}
    if (!pemText.trim()) {
      nextErrors.pemText = 'PEM content is required.'
    } else if (!isValidPemCertificate(pemText)) {
      nextErrors.pemText = 'Invalid PEM certificate format. Please ensure it contains valid certificate data.'
    }
    if (!alias.trim()) {
      nextErrors.alias = 'Alias is required.'
    }
    
    // For private store, private key is required
    if (targetStore === 'private') {
      if (!privateKeyText.trim()) {
        nextErrors.privateKeyText = 'Private key is required for private store.'
      } else if (!isValidPemPrivateKey(privateKeyText)) {
        nextErrors.privateKeyText = 'Invalid private key format. Please ensure it contains valid private key data.'
      }
    }
    
    setErrors(nextErrors)
    setApiError(null) // Clear API errors on validation
    return Object.keys(nextErrors).length === 0
  }

  const handleSubmit = async () => {
    if (!validate()) return
    
    setLoading(true)
    setApiError(null)
    
    try {
      // Convert PEM to Base64
      const base64Certificate = pemToBase64(pemText)
      
      // Prepare payload based on target store
      let certificates = null
      let pairs = null
      
      if (targetStore === 'trusted') {
        certificates = [{
          alias: alias,
          certificate: base64Certificate
        }]
      } else if (targetStore === 'private') {
        // For private store, we need both certificate and private key
        const base64PrivateKey = privateKeyPemToBase64(privateKeyText)
        pairs = [{
          alias: alias,
          certificate: base64Certificate,
          privateKey: base64PrivateKey
        }]
      }
      
      // Call the API
      await updateCertificates(certificates, pairs)
      
      // Success - call callbacks
      if (onSuccess) onSuccess()
      if (onSubmit) onSubmit({ success: true, targetStore, alias })
      
    } catch (error) {
      console.error('Failed to import certificate:', error)
      setApiError(error.message || 'Failed to import certificate')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box sx={{ 
      pt: 0.5,
      maxHeight: '80vh',
      display: 'flex',
      flexDirection: 'column'
    }}>
      <Box sx={{ 
        flex: 1,
        overflow: 'auto',
        minHeight: 0
      }}>
        <Stack spacing={2}>
        {apiError && (
          <Alert severity="error" onClose={() => setApiError(null)}>
            {apiError}
          </Alert>
        )}
        <input
          ref={fileInputRef}
          type="file"
          accept={fileAccept}
          style={{ display: 'none' }}
          onChange={async (e) => {
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
              setErrors((prev) => ({ ...prev, file: undefined, pemText: undefined }))
            } catch (err) {
              setErrors((prev) => ({ ...prev, file: 'Failed to read file.' }))
            }
          }}
        />
        <Stack direction="row" spacing={1} alignItems="center">
          <Button variant="outlined" onClick={() => fileInputRef.current?.click()}>
            {file ? 'Change Certificate File' : 'Choose Certificate File'}
          </Button>
          <Typography variant="body2" color={errors.file ? 'error' : 'text.secondary'}>
            {file ? file.name : 'No file selected'}
          </Typography>
        </Stack>
        {errors.file && <FormHelperText error>{errors.file}</FormHelperText>}

        <TextField
          label="PEM (paste contents including BEGIN/END)"
          placeholder={"-----BEGIN CERTIFICATE-----\n...base64...\n-----END CERTIFICATE-----"}
          value={pemText}
          onChange={(e) => setPemText(e.target.value)}
          error={Boolean(errors.pemText)}
          helperText={errors.pemText || 'Paste certificate or chain. Uploading a .pem or .key file fills this field.'}
          multiline
          minRows={4}
          maxRows={6}
          fullWidth
        />

        {targetStore === 'private' && (
          <>
            <input
              ref={privateKeyFileInputRef}
              type="file"
              accept={fileAccept}
              style={{ display: 'none' }}
              onChange={async (e) => {
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
              }}
            />
            <Stack direction="row" spacing={1} alignItems="center">
              <Button variant="outlined" onClick={() => privateKeyFileInputRef.current?.click()}>
                {privateKeyFile ? 'Change Private Key File' : 'Choose Private Key File'}
              </Button>
              <Typography variant="body2" color={errors.privateKeyFile ? 'error' : 'text.secondary'}>
                {privateKeyFile ? privateKeyFile.name : 'No private key file selected'}
              </Typography>
            </Stack>
            {errors.privateKeyFile && <FormHelperText error>{errors.privateKeyFile}</FormHelperText>}

            <TextField
              label="Private Key (paste contents including BEGIN/END)"
              placeholder={"-----BEGIN PRIVATE KEY-----\n...base64...\n-----END PRIVATE KEY-----"}
              value={privateKeyText}
              onChange={(e) => setPrivateKeyText(e.target.value)}
              error={Boolean(errors.privateKeyText)}
              helperText={errors.privateKeyText || 'Paste private key. Uploading a .pem or .key file fills this field.'}
              multiline
              minRows={4}
              maxRows={6}
              fullWidth
              required
            />
          </>
        )}

        <TextField
          label="Alias"
          value={alias}
          onChange={(e) => setAlias(e.target.value)}
          error={Boolean(errors.alias)}
          helperText={errors.alias || "Provide a unique alias for this certificate"}
          fullWidth
          required
        />
        </Stack>
      </Box>
      
      {/* Fixed buttons at bottom */}
      <Stack 
        direction="row" 
        spacing={1} 
        justifyContent="flex-end" 
        sx={{ 
          pt: 2,
          borderTop: '1px solid',
          borderColor: 'divider',
          backgroundColor: 'background.paper',
          flexShrink: 0
        }}
      >
        <Button onClick={onCancel} disabled={loading}>Cancel</Button>
        <Button 
          variant="contained" 
          onClick={handleSubmit}
          disabled={loading}
        >
          {loading ? 'Importing...' : 'Import'}
        </Button>
      </Stack>
    </Box>
  )
}


