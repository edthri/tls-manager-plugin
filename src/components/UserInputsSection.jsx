import React from 'react'
import {
  Box,
  Stack,
  TextField,
  Button,
  Typography,
  FormHelperText,
  Alert
} from '@mui/material'

const UserInputsSection = ({
  // State
  alias,
  pemText,
  privateKeyText,
  file,
  privateKeyFile,
  apiError,
  errors,
  targetStore,
  
  // Refs
  fileInputRef,
  privateKeyFileInputRef,
  fileAccept,
  
  // Handlers
  handleAliasChange,
  handlePemTextChange,
  handlePrivateKeyTextChange,
  handleFileUpload,
  handlePrivateKeyFileUpload,
  setApiError
}) => {
  return (
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
          onChange={handleAliasChange}
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
          onChange={handleFileUpload}
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
          onChange={handlePemTextChange}
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
              onChange={handlePrivateKeyFileUpload}
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
              onChange={handlePrivateKeyTextChange}
              error={Boolean(errors.privateKeyText)}
              helperText={errors.privateKeyText || 'Paste private key. Uploading a .pem or .key file fills this field.'}
              multiline
              minRows={4}
              maxRows={6}
              fullWidth
              required
              sx={{ marginTop: '10px !important' }}
            />
          </>
        )}
      </Stack>
    </Box>
  )
}

export default UserInputsSection
