import React, { useRef, useState } from 'react'
import { Box, Button, FormHelperText, Stack, TextField, Typography } from '@mui/material'

export default function ImportCertificateDialogContent({
  targetStore = 'trusted',
  onCancel,
  onSubmit,
}) {
  const [pemText, setPemText] = useState('')
  const [file, setFile] = useState(null)
  const [alias, setAlias] = useState('')
  const [errors, setErrors] = useState({})

  const fileInputRef = useRef(null)

  const fileAccept = '.pem,text/plain,application/x-pem-file'

  const validate = () => {
    const nextErrors = {}
    if (!pemText.trim()) nextErrors.pemText = 'PEM content is required.'
    if (pemText && !/-----BEGIN [^-]+-----[\s\S]*-----END [^-]+-----/m.test(pemText)) {
      nextErrors.pemText = 'Expected PEM content with BEGIN/END headers.'
    }
    setErrors(nextErrors)
    return Object.keys(nextErrors).length === 0
  }

  const handleSubmit = () => {
    if (!validate()) return
    const payload = {
      targetStore,
      source: 'text',
      pemText,
      fileName: file ? file.name : undefined,
      alias: alias || undefined,
    }
    // Useful debugging information
    // eslint-disable-next-line no-console
    console.debug('[ImportCertificate] submit', payload)
    if (onSubmit) onSubmit(payload)
  }

  return (
    <Box sx={{ pt: 0.5 }}>
      <Stack spacing={2}>
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
              if (!(name.endsWith('.pem') || f.type === 'text/plain' || f.type === 'application/x-pem-file')) {
                setErrors((prev) => ({ ...prev, file: 'Please select a .pem file.' }))
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
            {file ? 'Change .pem File' : 'Choose .pem File'}
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
          helperText={errors.pemText || 'Paste certificate or chain. Uploading a .pem fills this field.'}
          multiline
          minRows={8}
          fullWidth
        />

        <TextField
          label="Alias (optional)"
          value={alias}
          onChange={(e) => setAlias(e.target.value)}
          helperText="Provide an alias for the entry if applicable"
          fullWidth
        />

        <Stack direction="row" spacing={1} justifyContent="flex-end" sx={{ pt: 1 }}>
          <Button onClick={onCancel}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit}>Import</Button>
        </Stack>
      </Stack>
    </Box>
  )
}


