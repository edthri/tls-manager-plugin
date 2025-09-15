import React, { useEffect, useMemo, useRef, useState } from 'react'
import { Box, Typography, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, CircularProgress, Alert, Button } from '@mui/material'
import { fetchCertificates } from '../services/sslService'

export default function SslManagement() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [importing, setImporting] = useState(false)
  const fileInputRef = useRef(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      setLoading(true)
      setError('')
      try {
        const data = await fetchCertificates()
        if (!cancelled) {
          setRows(data)
        }
      } catch (e) {
        if (!cancelled) {
          setError('Failed to load certificates')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [])

  const hasData = useMemo(() => rows && rows.length > 0, [rows])

  const handleImportClick = () => {
    fileInputRef.current?.click()
  }

  const handleFileSelected = (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    setImporting(true)
    // Placeholder: hook API upload here later
    console.debug('Selected certificate file:', file.name, file.type, file.size)
    setTimeout(() => {
      setImporting(false)
      // Clear the input so selecting the same file again triggers onChange
      if (fileInputRef.current) fileInputRef.current.value = ''
    }, 600)
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2, justifyContent: 'flex-end' }}>
        <input ref={fileInputRef} type="file" hidden onChange={handleFileSelected} />
        <Button variant="contained" onClick={handleImportClick} disabled={importing}>
          {importing ? 'Importing…' : 'Import'}
        </Button>
      </Box>

      {loading ? (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <CircularProgress size={20} />
          <Typography variant="body2">Loading certificates…</Typography>
        </Box>
      ) : error ? (
        <Alert severity="error">{error}</Alert>
      ) : (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Alias</TableCell>
                <TableCell>Subject CN</TableCell>
                <TableCell>Issuer CN</TableCell>
                <TableCell>Valid Until</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {hasData ? rows.map((row) => (
                <TableRow key={row.alias} hover>
                  <TableCell>{row.alias}</TableCell>
                  <TableCell>{row.subjectCn}</TableCell>
                  <TableCell>{row.issuerCn}</TableCell>
                  <TableCell>{row.validUntil}</TableCell>
                </TableRow>
              )) : (
                <TableRow>
                  <TableCell colSpan={4}>
                    <Typography variant="body2" color="text.secondary">No certificates found.</Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  )
}
