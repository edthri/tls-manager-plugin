import React, { useState, useRef, useEffect, useCallback } from 'react'
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
  const [showExpandButton, setShowExpandButton] = useState(false)
  const contentRef = useRef(null)

  // Measure content height
  const measureHeight = useCallback(() => {
    if (contentRef.current) {
      const element = contentRef.current
      if (!expanded) {
        // Check if content is scrollable (scrollHeight > clientHeight)
        // Also check if scrollHeight exceeds threshold as fallback
        const isScrollable = element.scrollHeight > element.clientHeight
        const exceedsThreshold = element.scrollHeight > 200
        setShowExpandButton(isScrollable || exceedsThreshold)
      } else {
        // When expanded, always show button to allow collapsing
        setShowExpandButton(true)
      }
    }
  }, [expanded])

  // Use ResizeObserver to detect when content size changes
  useEffect(() => {
    if (!contentRef.current || !channelsInUse || channelsInUse.length === 0) return

    const element = contentRef.current
    // Initial measurements with multiple delays to catch flexbox layout
    const timer1 = setTimeout(() => measureHeight(), 0)
    const timer2 = setTimeout(() => measureHeight(), 100)
    const timer3 = setTimeout(() => measureHeight(), 500)
    const timer4 = setTimeout(() => measureHeight(), 1000)

    const resizeObserver = new ResizeObserver(() => {
      measureHeight()
    })
    resizeObserver.observe(element)

    return () => {
      resizeObserver.disconnect()
      clearTimeout(timer1)
      clearTimeout(timer2)
      clearTimeout(timer3)
      clearTimeout(timer4)
    }
  }, [measureHeight, channelsInUse])
  
  if (!channelsInUse || channelsInUse.length === 0) {
    return null
  }

  return (
    <Alert severity={severity} sx={{ mb: 2 }}>
      <Typography variant="subtitle2" gutterBottom>
        This certificate is currently in use by the following channels:
      </Typography>
      <Box
        ref={contentRef}
        sx={{
          maxHeight: expanded ? 'none' : '200px',
          overflow: expanded ? 'visible' : 'auto',
          transition: 'max-height 0.3s ease-in-out',
          mt: 1
        }}
      >
        <Stack direction="row" spacing={1} flexWrap="wrap" gap={1}>
          {channelsInUse.map((channel, index) => (
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
      {showExpandButton && (
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

