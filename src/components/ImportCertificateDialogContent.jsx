import React, { useState, useEffect } from 'react'
import {
  Box,
  Stack,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions
} from '@mui/material'
import { useCertificateImport } from '../hooks/useCertificateImport'
import CertificateDetailsSection from './CertificateDetailsSection'
import CertificateVerificationSection from './CertificateVerificationSection'
import UserInputsSection from './UserInputsSection'
import MobileCertificateSection from './MobileCertificateSection'
import { updateCertificates } from '../services/tlsService.js'

export default function ImportCertificateDialogContent({
  targetStore = 'trusted',
  onCancel,
  onSubmit,
  onSuccess,
}) {
  const [showConfirmDialog, setShowConfirmDialog] = useState(false)
  
  const {
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
    setLoading,
    setApiError,
    
    // Handlers
    handleVerifyCertificate,
    handleFileUpload,
    handlePrivateKeyFileUpload,
    handlePemTextChange,
    handlePrivateKeyTextChange,
    handleAliasChange,
    validate,
    loadExistingCertificates,
    checkAliasExists
  } = useCertificateImport(targetStore)

  // Load existing certificates on component mount
  useEffect(() => {
    loadExistingCertificates()
  }, [loadExistingCertificates])

  const handleSubmit = async () => {
    if (!validate()) return

    // Check if alias already exists
    const aliasExists = checkAliasExists(alias)
    if (aliasExists) {
      setShowConfirmDialog(true)
      return
    }

    // Proceed with import if no conflict
    await performImport()
  }

  const performImport = async () => {
    setLoading(true)
    setApiError(null)
    try {
      const result = await updateCertificates(targetStore, {
        alias,
        pemText,
        privateKeyText: targetStore === 'private' ? privateKeyText : undefined,
      })
      if (result.success) {
        onSuccess?.(result.data)
        onSubmit?.()
      } else {
        setApiError(result.error || 'Failed to import certificate')
      }
    } catch (error) {
      setApiError(error.message || 'Failed to import certificate')
    } finally {
      setLoading(false)
    }
  }

  const handleConfirmReplace = async () => {
    setShowConfirmDialog(false)
    await performImport()
  }

  const handleCancelReplace = () => {
    setShowConfirmDialog(false)
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
        <UserInputsSection
          alias={alias}
          pemText={pemText}
          privateKeyText={privateKeyText}
          file={file}
          privateKeyFile={privateKeyFile}
          apiError={apiError}
          errors={errors}
          targetStore={targetStore}
          aliasWarning={aliasWarning}
          fileInputRef={fileInputRef}
          privateKeyFileInputRef={privateKeyFileInputRef}
          fileAccept={fileAccept}
          handleAliasChange={handleAliasChange}
          handlePemTextChange={handlePemTextChange}
          handlePrivateKeyTextChange={handlePrivateKeyTextChange}
          handleFileUpload={handleFileUpload}
          handlePrivateKeyFileUpload={handlePrivateKeyFileUpload}
          setApiError={setApiError}
        />
        {/* Right Column - Certificate Details & Verification */}
        <Box sx={{ 
          flex: 1,
          minWidth: 0,
          display: { xs: 'none', md: 'block' } // Hide on mobile, show on desktop
        }}>
          <Stack spacing={2}>
            <CertificateDetailsSection certificateDetails={certificateDetails} />
            <CertificateVerificationSection
              verificationResult={verificationResult}
              isVerifying={isVerifying}
              onVerify={handleVerifyCertificate}
              pemText={pemText}
            />
          </Stack>
        </Box>

       

        {/* Mobile Certificate Details & Verification */}
        <MobileCertificateSection
          certificateDetails={certificateDetails}
          verificationResult={verificationResult}
          isVerifying={isVerifying}
          onVerify={handleVerifyCertificate}
          pemText={pemText}
        />
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
          mt: 'auto'
        }}
      >
        <Button onClick={onCancel} disabled={loading}>
          Cancel
        </Button>
        <Button 
          variant="contained" 
          onClick={handleSubmit} 
          disabled={loading}
        >
          {loading ? 'Importing...' : 'Import Certificate'}
        </Button>
      </Stack>

      {/* Confirmation Dialog for Replacing Existing Certificate */}
      <Dialog
        open={showConfirmDialog}
        onClose={handleCancelReplace}
        aria-labelledby="confirm-dialog-title"
        aria-describedby="confirm-dialog-description"
      >
        <DialogTitle id="confirm-dialog-title">
          Replace Existing Certificate
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="confirm-dialog-description">
            A certificate with the alias "{alias}" already exists. This will replace the existing certificate. Are you sure you want to continue?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCancelReplace} disabled={loading}>
            Cancel
          </Button>
          <Button 
            onClick={handleConfirmReplace} 
            variant="contained" 
            color="warning"
            disabled={loading}
          >
            {loading ? 'Replacing...' : 'Replace Certificate'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
