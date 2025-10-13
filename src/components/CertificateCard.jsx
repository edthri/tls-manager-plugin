import React from 'react'
import { Paper, Box, Typography, Stack, Button, Divider } from '@mui/material'
import ShieldOutlinedIcon from '@mui/icons-material/ShieldOutlined'
import StatusPill from './StatusPill'

export default function CertificateCard({ certificate, onViewDetails, onExport, onEditAlias, onRemove, showPrivateKeys = false }) {
  const {
    name,
    type,
    subject,
    issuer,
    validFrom,
    validTo,
    fingerprintSha1,
    hasPrivateKey,
    rawPrivateKey,
  } = certificate

  return (
    <Paper variant="outlined" sx={{ 
      p: 2, 
      borderRadius: 2,
      height: '100%',
      display: 'flex',
      flexDirection: 'column'
    }}>
      <Stack spacing={2} sx={{ flex: 1 }}>
        <Stack direction="row" spacing={2} alignItems="center" justifyContent="space-between">
          <Stack direction="row" spacing={1} alignItems="center">
            <Box sx={{ width: 36, height: 36, borderRadius: 2, backgroundColor: 'action.selected', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <ShieldOutlinedIcon fontSize="small" />
            </Box>
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 600, lineHeight: 1 }}>{name}</Typography>
              <Typography variant="body2" color="text.secondary">{type}</Typography>
            </Box>
          </Stack>
          <StatusPill validFrom={validFrom} validTo={validTo} />
        </Stack>

        <Box>
          <Typography variant="subtitle2" sx={{ mb: 0.5 }}>Subject:</Typography>
          <Typography 
            variant="body2" 
            color="text.primary"
            sx={{ 
              wordBreak: 'break-all',
              overflow: 'hidden',
              display: '-webkit-box',
              WebkitLineClamp: 2,
              WebkitBoxOrient: 'vertical'
            }}
          >
            {subject}
          </Typography>
        </Box>

        <Box>
          <Typography variant="subtitle2" sx={{ mb: 0.5 }}>Issuer:</Typography>
          <Typography 
            variant="body2" 
            color="text.primary"
            sx={{ 
              wordBreak: 'break-all',
              overflow: 'hidden',
              display: '-webkit-box',
              WebkitLineClamp: 2,
              WebkitBoxOrient: 'vertical'
            }}
          >
            {issuer}
          </Typography>
        </Box>

        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <Box>
            <Typography variant="subtitle2" sx={{ mb: 0.5 }}>Valid From:</Typography>
            <Typography variant="body2">{validFrom}</Typography>
          </Box>
          <Box>
            <Typography variant="subtitle2" sx={{ mb: 0.5 }}>Valid To:</Typography>
            <Typography variant="body2">{validTo}</Typography>
          </Box>
        </Stack>

        <Box>
          <Typography variant="subtitle2" sx={{ mb: 0.5 }}>Fingerprint (SHA-1):</Typography>
          <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>{fingerprintSha1}</Typography>
        </Box>

        {/* Private Key Section */}
        {showPrivateKeys && hasPrivateKey && rawPrivateKey && (
          <>
            <Divider />
            <Box>
              <Typography variant="subtitle2" sx={{ mb: 0.5 }}>Private Key (Base64):</Typography>
              <Box
                sx={{
                  backgroundColor: 'grey.100',
                  p: 1,
                  borderRadius: 1,
                  maxHeight: 150,
                  overflow: 'auto',
                  fontFamily: 'monospace',
                  fontSize: '0.75rem',
                  wordBreak: 'break-all',
                }}
              >
                {rawPrivateKey}
              </Box>
            </Box>
          </>
        )}

        <Divider />

        <Stack direction="row" spacing={1} sx={{ mt: 'auto' }}>
          <Button 
            variant="contained" 
            color="info" 
            startIcon={<Box component={ShieldOutlinedIcon} />} 
            onClick={() => onViewDetails?.(certificate)}
            fullWidth
          >
            View Details
          </Button>
          <Button 
            variant="outlined" 
            color="secondary" 
            onClick={() => onExport?.(certificate)}
            fullWidth
          >
            Export
          </Button>
          {/* Edit Alias button - only show for trusted and private stores */}
          {(certificate.store === 'trusted' || certificate.store === 'private') && (
            <Button 
              variant="outlined" 
              color="primary" 
              onClick={() => onEditAlias?.(certificate)}
              fullWidth
            >
              Edit Alias
            </Button>
          )}
          {/* Remove button - only show for trusted and private stores */}
          {(certificate.store === 'trusted' || certificate.store === 'private') && (
            <Button 
              variant="outlined" 
              color="error" 
              onClick={() => onRemove?.(certificate)}
              fullWidth
            >
              Remove
            </Button>
          )}
        </Stack>
      </Stack>
    </Paper>
  )
}


