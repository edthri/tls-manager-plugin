import React, { useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  Divider,
  Chip,
  Stack,
  Paper,
  Grid,
  IconButton,
} from '@mui/material'
import { Visibility, VisibilityOff } from '@mui/icons-material'
import { formatDate } from '../utils/dateUtils.js'

export default function CertificateDetailsDialog({ open, onClose, certificate }) {
  if (!certificate) return null

  const { parsedCertificate, rawCertificate } = certificate
  const [showPrivateKey, setShowPrivateKey] = useState(false)

  const getStatusColor = (validFrom, validTo) => {
    const now = new Date()
    const validFromDate = new Date(validFrom)
    const validToDate = new Date(validTo)
    
    if (now < validFromDate) return 'warning'
    if (now > validToDate) return 'error'
    
    // Check if expiring within 30 days
    const thirtyDaysFromNow = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000)
    if (validToDate < thirtyDaysFromNow) return 'warning'
    
    return 'success'
  }

  const getStatusText = (validFrom, validTo) => {
    const now = new Date()
    const validFromDate = new Date(validFrom)
    const validToDate = new Date(validTo)
    
    if (now < validFromDate) return 'Not Yet Valid'
    if (now > validToDate) return 'Expired'
    
    // Check if expiring within 30 days
    const thirtyDaysFromNow = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000)
    if (validToDate < thirtyDaysFromNow) return 'Expiring Soon'
    
    return 'Valid'
  }


  const formatExtensions = (extensions) => {
    if (!extensions || extensions.length === 0) return []
    
    return extensions.map(ext => ({
      name: ext.name,
      value: ext.value || ext.critical ? 'Critical' : 'Not Critical',
      critical: ext.critical || false
    }))
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Stack direction="row" spacing={2} alignItems="center">
          <Typography variant="h6">Certificate Details</Typography>
          <Chip
            label={getStatusText(certificate.validFrom, certificate.validTo)}
            color={getStatusColor(certificate.validFrom, certificate.validTo)}
            size="small"
          />
        </Stack>
      </DialogTitle>
      
      <DialogContent>
        <Stack spacing={3}>
          {/* Basic Information */}
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>Basic Information</Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">Alias</Typography>
                <Typography variant="body1">{certificate.alias}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">Type</Typography>
                <Typography variant="body1">{certificate.type}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">Store</Typography>
                <Typography variant="body1" sx={{ textTransform: 'capitalize' }}>
                  {certificate.store}
                </Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">Has Private Key</Typography>
                <Typography variant="body1">
                  {certificate.hasPrivateKey ? 'Yes' : 'No'}
                </Typography>
              </Grid>
            </Grid>
          </Paper>

          {/* Subject Information */}
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>Subject</Typography>
            <Typography variant="body1" sx={{ fontFamily: 'monospace', wordBreak: 'break-all' }}>
              {parsedCertificate?.subjectStr || 'Unknown'}
            </Typography>
          </Paper>

          {/* Issuer Information */}
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>Issuer</Typography>
            <Typography variant="body1" sx={{ fontFamily: 'monospace', wordBreak: 'break-all' }}>
              {parsedCertificate?.issuerStr || 'Unknown'}
            </Typography>
          </Paper>

          {/* Validity Period */}
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>Validity Period</Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">Valid From</Typography>
                <Typography variant="body1">{certificate.validFrom}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">Valid To</Typography>
                <Typography variant="body1">{certificate.validTo}</Typography>
              </Grid>
            </Grid>
          </Paper>

          {/* Fingerprint */}
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>Fingerprint</Typography>
            <Typography variant="body2" color="text.secondary">SHA-1</Typography>
            <Typography variant="body1" sx={{ fontFamily: 'monospace', wordBreak: 'break-all' }}>
              {certificate.fingerprintSha1}
            </Typography>
          </Paper>

          {/* Extensions */}
          {parsedCertificate?.extensions && parsedCertificate.extensions.length > 0 && (
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>Extensions</Typography>
              <Stack spacing={1}>
                {formatExtensions(parsedCertificate.extensions).map((ext, index) => (
                  <Box key={index}>
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Typography variant="body2" sx={{ fontWeight: 'medium' }}>
                        {ext.name}
                      </Typography>
                      {ext.critical && (
                        <Chip label="Critical" color="error" size="small" />
                      )}
                    </Stack>
                    <Typography variant="body2" color="text.secondary" sx={{ ml: 1 }}>
                      {ext.value}
                    </Typography>
                  </Box>
                ))}
              </Stack>
            </Paper>
          )}

          {/* Channels in Use */}
          {certificate.channelsInUse && certificate.channelsInUse.length > 0 && (
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>Channels in Use</Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap">
                {certificate.channelsInUse.map((channel, index) => (
                  <Chip key={index} label={channel} size="small" />
                ))}
              </Stack>
            </Paper>
          )}

          {/* Raw Certificate (Collapsible) */}
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>Raw Certificate (Base64)</Typography>
            <Box
              sx={{
                backgroundColor: 'grey.100',
                p: 1,
                borderRadius: 1,
                maxHeight: 200,
                overflow: 'auto',
                fontFamily: 'monospace',
                fontSize: '0.75rem',
                wordBreak: 'break-all',
              }}
            >
              {rawCertificate}
            </Box>
          </Paper>

          {/* Private Key (if available) */}
          {certificate.hasPrivateKey && certificate.rawPrivateKey && (
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Stack direction="row" spacing={1} alignItems="center" justifyContent="space-between">
                <Typography variant="h6">Private Key (Base64)</Typography>
                <IconButton
                  onClick={() => setShowPrivateKey(!showPrivateKey)}
                  size="small"
                  color="primary"
                  title={showPrivateKey ? 'Hide private key' : 'Show private key'}
                >
                  {showPrivateKey ? <VisibilityOff /> : <Visibility />}
                </IconButton>
              </Stack>
              {showPrivateKey && (
                <Box
                  sx={{
                    backgroundColor: 'grey.100',
                    p: 1,
                    borderRadius: 1,
                    maxHeight: 200,
                    overflow: 'auto',
                    fontFamily: 'monospace',
                    fontSize: '0.75rem',
                    wordBreak: 'break-all',
                    mt: 1,
                  }}
                >
                  {certificate.rawPrivateKey}
                </Box>
              )}
            </Paper>
          )}
        </Stack>
      </DialogContent>
      
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  )
}
