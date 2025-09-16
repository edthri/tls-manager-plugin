import React from 'react'
import { Box, CircularProgress, Typography, Alert, Grid, Stack } from '@mui/material'
import CertificateCard from './CertificateCard'

export default function CertificateList({ rows, loading, error, emptyText = 'No certificates found.', onViewDetails, onExport }) {
  if (loading) {
    return (
      <Stack direction="row" spacing={1} alignItems="center">
        <CircularProgress size={20} />
        <Typography variant="body2">Loading certificates…</Typography>
      </Stack>
    )
  }
  if (error) return <Alert severity="error">{error}</Alert>
  if (!rows || rows.length === 0) return <Typography variant="body2" color="text.secondary">{emptyText}</Typography>

  return (
    <Grid container spacing={2}>
      {rows.map((row) => (
        <Grid key={row.alias} xs={12} md={6}>
          <CertificateCard certificate={row} onViewDetails={onViewDetails} onExport={onExport} />
        </Grid>
      ))}
    </Grid>
  )
}


