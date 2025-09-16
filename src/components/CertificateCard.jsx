import React from 'react'
import { Paper, Box, Typography, Stack, Button, Divider } from '@mui/material'
import ShieldOutlinedIcon from '@mui/icons-material/ShieldOutlined'
import StatusPill from './StatusPill'

export default function CertificateCard({ certificate, onViewDetails, onExport }) {
  const {
    name,
    type,
    subject,
    issuer,
    validFrom,
    validTo,
    fingerprintSha1,
  } = certificate

  return (
    <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
      <Stack spacing={2}>
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
          <Typography variant="body2" color="text.primary">{subject}</Typography>
        </Box>

        <Box>
          <Typography variant="subtitle2" sx={{ mb: 0.5 }}>Issuer:</Typography>
          <Typography variant="body2" color="text.primary">{issuer}</Typography>
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

        <Divider />

        <Stack direction="row" spacing={1}>
          <Button variant="contained" color="info" startIcon={<Box component={ShieldOutlinedIcon} />} onClick={() => onViewDetails?.(certificate)}>
            View Details
          </Button>
          <Button variant="outlined" color="secondary" onClick={() => onExport?.(certificate)}>Export</Button>
        </Stack>
      </Stack>
    </Paper>
  )
}


