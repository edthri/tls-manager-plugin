import React, { useMemo } from 'react'
import { Chip } from '@mui/material'

function computeStatus(validFrom, validTo, thresholdDays = 30) {
  const now = new Date()
  const start = validFrom ? new Date(validFrom) : null
  const end = validTo ? new Date(validTo) : null
  if (end && end < now) return { label: 'Expired', color: 'error' }
  if (end) {
    const msLeft = end.getTime() - now.getTime()
    const daysLeft = msLeft / (1000 * 60 * 60 * 24)
    if (daysLeft <= thresholdDays) return { label: 'Expiring soon', color: 'warning' }
  }
  return { label: 'Valid', color: 'success' }
}

export default function StatusPill({ validFrom, validTo, thresholdDays = 30 }) {
  const status = useMemo(() => computeStatus(validFrom, validTo, thresholdDays), [validFrom, validTo, thresholdDays])
  return <Chip label={status.label} color={status.color} size="small" variant="outlined" />
}

export { computeStatus }


