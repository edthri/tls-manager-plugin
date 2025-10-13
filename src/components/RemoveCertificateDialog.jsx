import React, { useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Button,
  Box,
  Typography,
  Paper,
  Grid,
  Alert,
  Chip,
  Stack
} from '@mui/material'
import { Warning } from '@mui/icons-material'
import { removeCertificate } from '../services/tlsService'

export default function RemoveCertificateDialog({ 
  open, 
  onClose, 
  certificate, 
  onSuccess 
}) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  
  const handleRemove = async () => {
    if (!certificate) return

    setLoading(true)
    setError(null)
    
    try {
      const result = await removeCertificate(certificate.store, certificate.alias)
      if (result.success) {
        onSuccess?.(result.data)
        onClose()
      } else {
        setError(result.error || 'Failed to remove certificate')
      }
    } catch (error) {
      setError(error.message || 'Failed to remove certificate')
    } finally {
      setLoading(false)
    }
  }

  const handleClose = () => {
    setError(null)
    onClose()
  }

  if (!certificate) return null

  // Check if certificate is in use by channels
  const isInUse = certificate.channelsInUse && certificate.channelsInUse.length > 0
  const canRemove = !isInUse

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        <Stack direction="row" spacing={1} alignItems="center">
          <Warning color="error" />
          <Typography variant="h6">Remove Certificate</Typography>
        </Stack>
      </DialogTitle>
      <DialogContent>
        <Box sx={{ pt: 1 }}>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          {/* Certificate Info Display */}
          <Paper variant="outlined" sx={{ p: 2, mb: 3 }}>
            <Typography variant="h6" gutterBottom>Certificate Information</Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">Alias</Typography>
                <Typography variant="body1">{certificate.alias}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">Store</Typography>
                <Typography variant="body1" sx={{ textTransform: 'capitalize' }}>
                  {certificate.store}
                </Typography>
              </Grid>
              <Grid item xs={12}>
                <Typography variant="body2" color="text.secondary">Subject</Typography>
                <Typography variant="body1" sx={{ wordBreak: 'break-all' }}>
                  {certificate.subject}
                </Typography>
              </Grid>
              <Grid item xs={12}>
                <Typography variant="body2" color="text.secondary">Issuer</Typography>
                <Typography variant="body1" sx={{ wordBreak: 'break-all' }}>
                  {certificate.issuer}
                </Typography>
              </Grid>
            </Grid>
          </Paper>

          {/* Channels in Use Warning */}
          {isInUse && (
            <Alert severity="error" sx={{ mb: 2 }}>
              <Typography variant="subtitle2" gutterBottom>
                Cannot remove certificate. It is currently in use by the following channels:
              </Typography>
              <Stack direction="row" spacing={1} sx={{ mt: 1 }} flexWrap="wrap">
                {certificate.channelsInUse.map((channel, index) => (
                  <Chip key={index} label={channel} size="small" color="error" />
                ))}
              </Stack>
              <Typography variant="body2" sx={{ mt: 1 }}>
                Please remove the certificate from these channels first before deletion.
              </Typography>
            </Alert>
          )}

          {/* Warning Message */}
          {canRemove && (
            <Alert severity="warning" sx={{ mb: 2 }}>
              <Typography variant="body2">
                This action cannot be undone. The certificate will be permanently removed from the {certificate.store} store.
              </Typography>
            </Alert>
          )}

          <DialogContentText>
            {canRemove 
              ? 'Are you sure you want to remove this certificate?'
              : 'This certificate cannot be removed because it is currently in use.'
            }
          </DialogContentText>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={loading}>
          Cancel
        </Button>
        <Button 
          onClick={handleRemove}
          variant="contained"
          color="error"
          disabled={loading || !canRemove}
        >
          {loading ? 'Removing...' : 'Remove Certificate'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
