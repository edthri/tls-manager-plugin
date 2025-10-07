import React, { useRef, useState } from 'react'
import { 
  Box, 
  Button, 
  FormHelperText, 
  Stack, 
  TextField, 
  Typography, 
  Alert,
  CircularProgress,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  List,
  ListItem,
  ListItemText,
  Chip,
  Divider
} from '@mui/material'
import { ExpandMore, CheckCircle, Error, Warning, Security } from '@mui/icons-material'
import { pemToBase64, privateKeyPemToBase64, isValidPemCertificate, isValidPemPrivateKey, parseCertificate } from '../utils/certificateUtils.js'
import { updateCertificates } from '../services/tlsService.js'
import { verifyCertificate } from '../utils/verificationUtils.js'

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
  const [verificationResult, setVerificationResult] = useState(null)
  const [isVerifying, setIsVerifying] = useState(false)
  const [certificateDetails, setCertificateDetails] = useState(null)

  const fileInputRef = useRef(null)
  const privateKeyFileInputRef = useRef(null)

  const fileAccept = '.pem,.key,text/plain,application/x-pem-file'

  // Parse certificate details when PEM text changes
  const parseCertificateDetails = (pemText) => {
    if (!pemText.trim() || !isValidPemCertificate(pemText)) {
      setCertificateDetails(null)
      return
    }

    try {
      const details = parseCertificate(pemToBase64(pemText))
      setCertificateDetails(details)
    } catch (error) {
      console.error('Failed to parse certificate details:', error)
      setCertificateDetails(null)
    }
  }

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

  const handleVerifyCertificate = async () => {
    if (!pemText.trim()) {
      setErrors({ pemText: 'PEM content is required for verification.' })
      return
    }

    setIsVerifying(true)
    setVerificationResult(null)
    
    try {
      // For private store, include private key if available
      const privateKeyPem = targetStore === 'private' && privateKeyText.trim() 
        ? privateKeyText 
        : null
      
      const result = verifyCertificate(pemText, privateKeyPem)
      setVerificationResult(result)
    } catch (error) {
      setVerificationResult({
        success: false,
        error: `Verification failed: ${error.message}`
      })
    } finally {
      setIsVerifying(false)
    }
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
        minHeight: 0,
        display: 'flex',
        gap: 3
      }}>
        

        {/* Left Column - User Inputs */}
        <Box sx={{ 
          flex: 1,
          minWidth: 0
        }}>
          <Stack spacing={2}>
            {apiError && (
              <Alert severity="error" onClose={() => setApiError(null)}>
                {apiError}
              </Alert>
            )}

             <TextField
               label="Alias"
               value={alias}
               onChange={(e) => setAlias(e.target.value)}
               error={Boolean(errors.alias)}
               helperText={errors.alias || "Provide a unique alias for this certificate"}
               fullWidth
               required
               sx={{ marginTop: '10px !important' }}
             />

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
                  parseCertificateDetails(text)
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
               onChange={(e) => {
                 setPemText(e.target.value)
                 parseCertificateDetails(e.target.value)
               }}
               error={Boolean(errors.pemText)}
               helperText={errors.pemText || 'Paste certificate or chain. Uploading a .pem or .key file fills this field.'}
               multiline
               minRows={4}
               maxRows={6}
               fullWidth
               sx={{ marginTop: '10px' }}
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
                   sx={{ marginTop: '10px' }}
                 />
              </>
            )}
          </Stack>
        </Box>
        {/* Right Column - Certificate Details & Verification */}
        <Box sx={{ 
          flex: 1,
          minWidth: 0,
          display: { xs: 'none', md: 'block' } // Hide on mobile, show on desktop
        }}>
          <Stack spacing={2}>
            {/* Certificate Details Section */}
            {certificateDetails && (
              <Box>
                <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                  <Security color="primary" />
                  Certificate Details
                </Typography>
                
                <Box sx={{ 
                  border: '1px solid', 
                  borderColor: 'divider', 
                  borderRadius: 1, 
                  p: 2,
                  backgroundColor: 'grey.50'
                }}>
                  <Stack spacing={2}>
                    <Box>
                      <Typography variant="subtitle2" color="text.secondary">Subject</Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace', wordBreak: 'break-all' }}>
                        {certificateDetails.subjectStr || 'Unknown'}
                      </Typography>
                    </Box>
                    
                    <Box>
                      <Typography variant="subtitle2" color="text.secondary">Issuer</Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace', wordBreak: 'break-all' }}>
                        {certificateDetails.issuerStr || 'Unknown'}
                      </Typography>
                    </Box>
                    
                    <Box>
                      <Typography variant="subtitle2" color="text.secondary">Type</Typography>
                      <Chip label={certificateDetails.type || 'Unknown'} size="small" color="primary" />
                    </Box>
                    
                    <Box>
                      <Typography variant="subtitle2" color="text.secondary">Serial Number</Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                        {certificateDetails.serialNumber || 'Unknown'}
                      </Typography>
                    </Box>
                    
                    <Box>
                      <Typography variant="subtitle2" color="text.secondary">Validity Period</Typography>
                      <Typography variant="body2">
                        From: {certificateDetails.validFrom || 'Unknown'}
                      </Typography>
                      <Typography variant="body2">
                        To: {certificateDetails.validTo || 'Unknown'}
                      </Typography>
                    </Box>
                    
                    <Box>
                      <Typography variant="subtitle2" color="text.secondary">SHA-1 Fingerprint</Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.75rem', wordBreak: 'break-all' }}>
                        {certificateDetails.fingerprintSha1 || 'Unknown'}
                      </Typography>
                    </Box>
                  </Stack>
                </Box>
              </Box>
            )}

            {/* Certificate Verification Section */}
            <Box>
              <Stack direction="row" spacing={2} alignItems="center" justifyContent="space-between" sx={{ mb: 2 }}>
                <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Security color="primary" />
                  Certificate Verification
                </Typography>
                <Button
                  variant="outlined"
                  onClick={handleVerifyCertificate}
                  disabled={isVerifying || !pemText.trim()}
                  startIcon={isVerifying ? <CircularProgress size={16} /> : <Security />}
                  size="small"
                >
                  {isVerifying ? 'Verifying...' : 'Verify'}
                </Button>
              </Stack>

              {verificationResult && (
                <Box>
                  {verificationResult.success ? (
                    <Alert severity="success" sx={{ mb: 2 }}>
                      Certificate verification completed successfully!
                    </Alert>
                  ) : (
                    <Alert severity="error" sx={{ mb: 2 }}>
                      {verificationResult.error}
                    </Alert>
                  )}

                  {verificationResult.success && (
                    <Stack spacing={2}>
                      {/* Chain Validation Results */}
                      {verificationResult.chainValidation && (
                        <Accordion>
                          <AccordionSummary expandIcon={<ExpandMore />}>
                            <Stack direction="row" spacing={1} alignItems="center">
                              {verificationResult.chainValidation.isValid ? (
                                <CheckCircle color="success" />
                              ) : (
                                <Error color="error" />
                              )}
                              <Typography variant="h6">
                                Chain Validation {verificationResult.chainValidation.isValid ? 'Passed' : 'Failed'}
                              </Typography>
                            </Stack>
                          </AccordionSummary>
                          <AccordionDetails>
                            {verificationResult.chainValidation.errors.length > 0 && (
                              <Box sx={{ mb: 2 }}>
                                <Typography variant="subtitle2" color="error" gutterBottom>
                                  Errors:
                                </Typography>
                                <List dense>
                                  {verificationResult.chainValidation.errors.map((error, index) => (
                                    <ListItem key={index}>
                                      <ListItemText primary={error} />
                                    </ListItem>
                                  ))}
                                </List>
                              </Box>
                            )}
                            {verificationResult.chainValidation.warnings.length > 0 && (
                              <Box sx={{ mb: 2 }}>
                                <Typography variant="subtitle2" color="warning.main" gutterBottom>
                                  Warnings:
                                </Typography>
                                <List dense>
                                  {verificationResult.chainValidation.warnings.map((warning, index) => (
                                    <ListItem key={index}>
                                      <ListItemText primary={warning} />
                                    </ListItem>
                                  ))}
                                </List>
                              </Box>
                            )}
                            {verificationResult.chainValidation.details.length > 0 && (
                              <Box>
                                <Typography variant="subtitle2" gutterBottom>
                                  Details:
                                </Typography>
                                <List dense>
                                  {verificationResult.chainValidation.details.map((detail, index) => (
                                    <ListItem key={index}>
                                      <ListItemText primary={detail} />
                                    </ListItem>
                                  ))}
                                </List>
                              </Box>
                            )}
                          </AccordionDetails>
                        </Accordion>
                      )}

                      {/* Private Key Validation */}
                      {verificationResult.keyValidation && (
                        <Accordion>
                          <AccordionSummary expandIcon={<ExpandMore />}>
                            <Stack direction="row" spacing={1} alignItems="center">
                              {verificationResult.keyValidation.isValid ? (
                                <CheckCircle color="success" />
                              ) : (
                                <Error color="error" />
                              )}
                              <Typography variant="h6">
                                Private Key Validation {verificationResult.keyValidation.isValid ? 'Passed' : 'Failed'}
                              </Typography>
                            </Stack>
                          </AccordionSummary>
                          <AccordionDetails>
                            <Alert 
                              severity={verificationResult.keyValidation.isValid ? 'success' : 'error'}
                            >
                              {verificationResult.keyValidation.message}
                            </Alert>
                          </AccordionDetails>
                        </Accordion>
                      )}

                      {/* Certificate Chain Details */}
                      {verificationResult.chainDetails && verificationResult.chainDetails.length > 1 && (
                        <Accordion>
                          <AccordionSummary expandIcon={<ExpandMore />}>
                            <Typography variant="h6">
                              Certificate Chain ({verificationResult.chainDetails.length} certificates)
                            </Typography>
                          </AccordionSummary>
                          <AccordionDetails>
                            <List>
                              {verificationResult.chainDetails.map((cert, index) => (
                                <ListItem key={index} sx={{ flexDirection: 'column', alignItems: 'flex-start' }}>
                                  <Typography variant="subtitle2" gutterBottom>
                                    {cert.type} (Certificate #{cert.index})
                                  </Typography>
                                  <Typography variant="body2" color="text.secondary">
                                    Subject: {cert.subject}
                                  </Typography>
                                  <Typography variant="body2" color="text.secondary">
                                    Issuer: {cert.issuer}
                                  </Typography>
                                  <Typography variant="body2" color="text.secondary">
                                    Valid: {cert.validFrom} - {cert.validTo}
                                  </Typography>
                                </ListItem>
                              ))}
                            </List>
                          </AccordionDetails>
                        </Accordion>
                      )}
                    </Stack>
                  )}
                </Box>
              )}
            </Box>
          </Stack>
        </Box>

        {/* Mobile Certificate Details & Verification */}
        <Box sx={{ 
          display: { xs: 'block', md: 'none' }, // Show on mobile, hide on desktop
          mt: 2
        }}>
          <Stack spacing={2}>
            {/* Certificate Details Section */}
            {certificateDetails && (
              <Box>
                <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                  <Security color="primary" />
                  Certificate Details
                </Typography>
                
                <Box sx={{ 
                  border: '1px solid', 
                  borderColor: 'divider', 
                  borderRadius: 1, 
                  p: 2,
                  backgroundColor: 'grey.50'
                }}>
                  <Stack spacing={2}>
                    <Box>
                      <Typography variant="subtitle2" color="text.secondary">Subject</Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace', wordBreak: 'break-all' }}>
                        {certificateDetails.subjectStr || 'Unknown'}
                      </Typography>
                    </Box>
                    
                    <Box>
                      <Typography variant="subtitle2" color="text.secondary">Issuer</Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace', wordBreak: 'break-all' }}>
                        {certificateDetails.issuerStr || 'Unknown'}
                      </Typography>
                    </Box>
                    
                    <Box>
                      <Typography variant="subtitle2" color="text.secondary">Type</Typography>
                      <Chip label={certificateDetails.type || 'Unknown'} size="small" color="primary" />
                    </Box>
                    
                    <Box>
                      <Typography variant="subtitle2" color="text.secondary">Serial Number</Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                        {certificateDetails.serialNumber || 'Unknown'}
                      </Typography>
                    </Box>
                    
                    <Box>
                      <Typography variant="subtitle2" color="text.secondary">Validity Period</Typography>
                      <Typography variant="body2">
                        From: {certificateDetails.validFrom || 'Unknown'}
                      </Typography>
                      <Typography variant="body2">
                        To: {certificateDetails.validTo || 'Unknown'}
                      </Typography>
                    </Box>
                    
                    <Box>
                      <Typography variant="subtitle2" color="text.secondary">SHA-1 Fingerprint</Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.75rem', wordBreak: 'break-all' }}>
                        {certificateDetails.fingerprintSha1 || 'Unknown'}
                      </Typography>
                    </Box>
                  </Stack>
                </Box>
              </Box>
            )}

            {/* Certificate Verification Section */}
            <Box>
              <Stack direction="row" spacing={2} alignItems="center" justifyContent="space-between" sx={{ mb: 2 }}>
                <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Security color="primary" />
                  Certificate Verification
                </Typography>
                <Button
                  variant="outlined"
                  onClick={handleVerifyCertificate}
                  disabled={isVerifying || !pemText.trim()}
                  startIcon={isVerifying ? <CircularProgress size={16} /> : <Security />}
                  size="small"
                >
                  {isVerifying ? 'Verifying...' : 'Verify'}
                </Button>
              </Stack>

              {verificationResult && (
                <Box>
                  {verificationResult.success ? (
                    <Alert severity="success" sx={{ mb: 2 }}>
                      Certificate verification completed successfully!
                    </Alert>
                  ) : (
                    <Alert severity="error" sx={{ mb: 2 }}>
                      {verificationResult.error}
                    </Alert>
                  )}

                  {verificationResult.success && (
                    <Stack spacing={2}>
                      {/* Chain Validation Results */}
                      {verificationResult.chainValidation && (
                        <Accordion>
                          <AccordionSummary expandIcon={<ExpandMore />}>
                            <Stack direction="row" spacing={1} alignItems="center">
                              {verificationResult.chainValidation.isValid ? (
                                <CheckCircle color="success" />
                              ) : (
                                <Error color="error" />
                              )}
                              <Typography variant="h6">
                                Chain Validation {verificationResult.chainValidation.isValid ? 'Passed' : 'Failed'}
                              </Typography>
                            </Stack>
                          </AccordionSummary>
                          <AccordionDetails>
                            {verificationResult.chainValidation.errors.length > 0 && (
                              <Box sx={{ mb: 2 }}>
                                <Typography variant="subtitle2" color="error" gutterBottom>
                                  Errors:
                                </Typography>
                                <List dense>
                                  {verificationResult.chainValidation.errors.map((error, index) => (
                                    <ListItem key={index}>
                                      <ListItemText primary={error} />
                                    </ListItem>
                                  ))}
                                </List>
                              </Box>
                            )}
                            {verificationResult.chainValidation.warnings.length > 0 && (
                              <Box sx={{ mb: 2 }}>
                                <Typography variant="subtitle2" color="warning.main" gutterBottom>
                                  Warnings:
                                </Typography>
                                <List dense>
                                  {verificationResult.chainValidation.warnings.map((warning, index) => (
                                    <ListItem key={index}>
                                      <ListItemText primary={warning} />
                                    </ListItem>
                                  ))}
                                </List>
                              </Box>
                            )}
                            {verificationResult.chainValidation.details.length > 0 && (
                              <Box>
                                <Typography variant="subtitle2" gutterBottom>
                                  Details:
                                </Typography>
                                <List dense>
                                  {verificationResult.chainValidation.details.map((detail, index) => (
                                    <ListItem key={index}>
                                      <ListItemText primary={detail} />
                                    </ListItem>
                                  ))}
                                </List>
                              </Box>
                            )}
                          </AccordionDetails>
                        </Accordion>
                      )}

                      {/* Private Key Validation */}
                      {verificationResult.keyValidation && (
                        <Accordion>
                          <AccordionSummary expandIcon={<ExpandMore />}>
                            <Stack direction="row" spacing={1} alignItems="center">
                              {verificationResult.keyValidation.isValid ? (
                                <CheckCircle color="success" />
                              ) : (
                                <Error color="error" />
                              )}
                              <Typography variant="h6">
                                Private Key Validation {verificationResult.keyValidation.isValid ? 'Passed' : 'Failed'}
                              </Typography>
                            </Stack>
                          </AccordionSummary>
                          <AccordionDetails>
                            <Alert 
                              severity={verificationResult.keyValidation.isValid ? 'success' : 'error'}
                            >
                              {verificationResult.keyValidation.message}
                            </Alert>
                          </AccordionDetails>
                        </Accordion>
                      )}

                      {/* Certificate Chain Details */}
                      {verificationResult.chainDetails && verificationResult.chainDetails.length > 1 && (
                        <Accordion>
                          <AccordionSummary expandIcon={<ExpandMore />}>
                            <Typography variant="h6">
                              Certificate Chain ({verificationResult.chainDetails.length} certificates)
                            </Typography>
                          </AccordionSummary>
                          <AccordionDetails>
                            <List>
                              {verificationResult.chainDetails.map((cert, index) => (
                                <ListItem key={index} sx={{ flexDirection: 'column', alignItems: 'flex-start' }}>
                                  <Typography variant="subtitle2" gutterBottom>
                                    {cert.type} (Certificate #{cert.index})
                                  </Typography>
                                  <Typography variant="body2" color="text.secondary">
                                    Subject: {cert.subject}
                                  </Typography>
                                  <Typography variant="body2" color="text.secondary">
                                    Issuer: {cert.issuer}
                                  </Typography>
                                  <Typography variant="body2" color="text.secondary">
                                    Valid: {cert.validFrom} - {cert.validTo}
                                  </Typography>
                                </ListItem>
                              ))}
                            </List>
                          </AccordionDetails>
                        </Accordion>
                      )}
                    </Stack>
                  )}
                </Box>
              )}
            </Box>
          </Stack>
        </Box>
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


