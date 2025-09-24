import React, { useMemo, useRef, useState } from 'react'
import { Box, Button, FormControl, FormHelperText, InputLabel, MenuItem, Select, Stack, TextField, Typography } from '@mui/material'

const FORMAT_OPTIONS = [
  { value: 'PEM', label: 'PEM (Base64 with headers)' },
  { value: 'DER', label: 'DER (binary X.509)' },
  { value: 'PKCS12', label: 'PKCS#12 / PFX (.p12 / .pfx)' },
  { value: 'JKS', label: 'JKS / Java KeyStore' },
]

export default function ImportCertificateDialogContent({
  defaultFormat = 'PEM',
  targetStore = 'trusted',
  onCancel,
  onSubmit,
}) {
  const [format, setFormat] = useState(defaultFormat)
  const [pemText, setPemText] = useState('')
  const [file, setFile] = useState(null)
  const [password, setPassword] = useState('')
  const [alias, setAlias] = useState('')
  const [errors, setErrors] = useState({})

  const fileInputRef = useRef(null)

  const fileAccept = useMemo(() => {
    switch (format) {
      case 'DER':
        return '.der,.cer,.crt,application/pkix-cert,application/x-x509-ca-cert'
      case 'PKCS12':
        return '.p12,.pfx,application/x-pkcs12'
      case 'JKS':
        return '.jks,.keystore'
      default:
        return '.pem,.crt,.cer,text/plain,application/x-pem-file'
    }
  }, [format])

  const validate = () => {
    const nextErrors = {}
    if (format === 'PEM') {
      if (!pemText.trim()) nextErrors.pemText = 'PEM content is required.'
      if (pemText && !/-----BEGIN [^-]+-----[\s\S]*-----END [^-]+-----/m.test(pemText)) {
        nextErrors.pemText = 'Expected PEM with BEGIN/END headers.'
      }
    } else {
      if (!file) nextErrors.file = 'Please select a file.'
    }
    if (format === 'PKCS12' || format === 'JKS') {
      if (!password) nextErrors.password = 'Password is required.'
    }
    setErrors(nextErrors)
    return Object.keys(nextErrors).length === 0
  }

  const handleSubmit = () => {
    if (!validate()) return
    const payload = {
      format,
      targetStore,
      source: format === 'PEM' ? 'text' : 'file',
      pemText: format === 'PEM' ? pemText : undefined,
      fileName: file ? file.name : undefined,
      password: format === 'PKCS12' || format === 'JKS' ? password : undefined,
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
        <FormControl size="small">
          <InputLabel id="format-label">Format</InputLabel>
          <Select
            labelId="format-label"
            id="format-select"
            label="Format"
            value={format}
            onChange={(e) => {
              setFormat(e.target.value)
              setErrors({})
              setPemText('')
              setFile(null)
              setPassword('')
              setAlias('')
            }}
          >
            {FORMAT_OPTIONS.map((opt) => (
              <MenuItem key={opt.value} value={opt.value}>{opt.label}</MenuItem>
            ))}
          </Select>
          <FormHelperText>Select the certificate/container format</FormHelperText>
        </FormControl>

        {format === 'PEM' ? (
          <TextField
            label="PEM (paste contents including BEGIN/END)"
            placeholder={"-----BEGIN CERTIFICATE-----\n...base64...\n-----END CERTIFICATE-----"}
            value={pemText}
            onChange={(e) => setPemText(e.target.value)}
            error={Boolean(errors.pemText)}
            helperText={errors.pemText || 'You can paste a certificate chain.'}
            multiline
            minRows={6}
            fullWidth
          />
        ) : (
          <>
            <input
              ref={fileInputRef}
              type="file"
              accept={fileAccept}
              style={{ display: 'none' }}
              onChange={(e) => {
                const f = e.target.files && e.target.files[0]
                setFile(f || null)
                setErrors((prev) => ({ ...prev, file: undefined }))
              }}
            />
            <Stack direction="row" spacing={1} alignItems="center">
              <Button variant="outlined" onClick={() => fileInputRef.current?.click()}>
                {file ? 'Change File' : 'Choose File'}
              </Button>
              <Typography variant="body2" color={errors.file ? 'error' : 'text.secondary'}>
                {file ? file.name : 'No file selected'}
              </Typography>
            </Stack>
            {errors.file && <FormHelperText error>{errors.file}</FormHelperText>}
          </>
        )}

        {(format === 'PKCS12' || format === 'JKS') && (
          <TextField
            type="password"
            label="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={Boolean(errors.password)}
            helperText={errors.password || 'Password for the keystore/container.'}
            fullWidth
          />
        )}

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


