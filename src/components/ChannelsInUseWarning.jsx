import React, { useState } from 'react'
import {
  Alert,
  Typography,
  Stack,
  Chip,
  Button,
  Box
} from '@mui/material'
import { ExpandMore, ExpandLess } from '@mui/icons-material'

/**
 * Reusable component for displaying channels in use warning
 * Shows channels in an expandable area when there are many channels
 * @param {Array} channelsInUse - Array of channel names
 * @param {string} severity - Alert severity ('warning' or 'error')
 * @param {string} message - Custom message to display (optional)
 */
export default function ChannelsInUseWarning({ 
  channelsInUse, 
  severity = 'warning',
  message 
}) {
  const [expanded, setExpanded] = useState(false)
  
  if (!channelsInUse || channelsInUse.length === 0) {
    return null
  }

  const hasManyChannels = channelsInUse.length > 10
  const displayChannels = expanded || !hasManyChannels 
    ? channelsInUse 
    : channelsInUse.slice(0, 10)

  return (
    <Alert severity={severity} sx={{ mb: 2 }}>
      <Typography variant="subtitle2" gutterBottom>
        This certificate is currently in use by the following channels:
      </Typography>
      <Box
        sx={{
          maxHeight: expanded ? 'none' : '200px',
          overflow: expanded ? 'visible' : 'auto',
          transition: 'max-height 0.3s ease-in-out',
          mt: 1
        }}
      >
        <Stack direction="row" spacing={1} flexWrap="wrap" gap={1}>
          {displayChannels.map((channel, index) => (
            <Chip 
              key={index} 
              label={channel} 
              size="small" 
              color={severity === 'error' ? 'error' : 'warning'} 
              variant="outlined" 
            />
          ))}
        </Stack>
      </Box>
      {hasManyChannels && (
        <Box sx={{ mt: 1, display: 'flex', justifyContent: 'flex-end' }}>
          <Button
            size="small"
            onClick={() => setExpanded(!expanded)}
            endIcon={expanded ? <ExpandLess /> : <ExpandMore />}
            sx={{ minWidth: 'auto', textTransform: 'none' }}
          >
            {expanded 
              ? `Show Less (${channelsInUse.length} channels)` 
              : `Show All (${channelsInUse.length} channels)`
            }
          </Button>
        </Box>
      )}
      {message && (
        <Typography variant="body2" sx={{ mt: 1 }}>
          {message}
        </Typography>
      )}
    </Alert>
  )
}

