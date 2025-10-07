import React from 'react'
import {
  Box,
  Typography,
  Stack,
  Chip
} from '@mui/material'
import { Security } from '@mui/icons-material'

const CertificateDetailsSection = ({ certificateDetails }) => {
  if (!certificateDetails) return null

  return (
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
  )
}

export default CertificateDetailsSection
