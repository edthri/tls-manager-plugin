import React, { useMemo, useRef, useState } from 'react'
import { Box, Paper, Dialog, DialogTitle, DialogContent, DialogActions, Button } from '@mui/material'
import { useSearchParams } from 'react-router-dom'
import TabsWithCounts from '../components/TabsWithCounts'
import TabPanel from '../components/TabPanel'
import StoreToolbar from '../components/StoreToolbar'
import SearchInput from '../components/SearchInput'
import CertificateList from '../components/CertificateList'
import useCertificates from '../hooks/useCertificates'
import ShieldOutlinedIcon from '@mui/icons-material/ShieldOutlined'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import VpnKeyIcon from '@mui/icons-material/VpnKey'

export default function SslManagement() {
  const { counts, filterBy, loading, error } = useCertificates()
  const [params, setParams] = useSearchParams()
  const tabKeys = ['native', 'trusted', 'private']
  const initialKey = params.get('tab') && tabKeys.includes(params.get('tab')) ? params.get('tab') : 'native'
  const [tabKey, setTabKey] = useState(initialKey)
  const [search, setSearch] = useState('')

  const fileInputRef = useRef(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [dialogTitle, setDialogTitle] = useState('')
  const [dialogContent, setDialogContent] = useState('')

  const openDialog = (title, content) => {
    setDialogTitle(title)
    setDialogContent(content)
    setDialogOpen(true)
  }

  const onTabChange = (_e, newIndex) => {
    const newKey = tabKeys[newIndex]
    setTabKey(newKey)
    setSearch('')
    setParams((prev) => {
      const p = new URLSearchParams(prev)
      p.set('tab', newKey)
      return p
    }, { replace: true })
  }

  const tabIndex = useMemo(() => Math.max(0, tabKeys.indexOf(tabKey)), [tabKey])
  const visibleRows = useMemo(() => filterBy(tabKey, search), [filterBy, tabKey, search])

  const tabs = [
    { key: 'native', label: 'Native Java Certificate Store', count: counts.native, icon: <ShieldOutlinedIcon fontSize="small" /> },
    { key: 'trusted', label: 'Additional Trusted Certificates', count: counts.trusted, icon: <CheckCircleOutlineIcon fontSize="small" /> },
    { key: 'private', label: 'Private Key Store', count: counts.private, icon: <VpnKeyIcon fontSize="small" /> },
  ]

  const toolbarByTab = {
    native: {
      title: 'Native Java Certificate Store',
      warning: 'Read-only system store',
      actions: [],
    },
    trusted: {
      title: 'Additional Trusted Certificates',
      actions: [
        { key: 'import', label: 'Import Certificate', color: 'info', onClick: () => openDialog('Import Certificate', 'Placeholder dialog for importing a certificate.') },
        { key: 'add', label: 'Add New', variant: 'contained', color: 'success', onClick: () => openDialog('Add New Certificate', 'Placeholder dialog for adding a new certificate.') },
      ],
    },
    private: {
      title: 'Private Key Store',
      actions: [
        { key: 'show-keys', label: 'Show Private Keys', color: 'secondary', onClick: () => openDialog('Show Private Keys', 'Placeholder dialog for showing private keys.') },
        { key: 'import-cert', label: 'Import Certificate', color: 'info', onClick: () => openDialog('Import Certificate', 'Placeholder dialog for importing a certificate.') },
        { key: 'import-pkcs12', label: 'Import PKCS#12', color: 'warning', onClick: () => openDialog('Import PKCS#12', 'Placeholder dialog for importing PKCS#12.') },
        { key: 'add-new', label: 'Add New', variant: 'contained', color: 'success', onClick: () => openDialog('Add New Private Key', 'Placeholder dialog for adding a new private key certificate.') },
      ],
    },
  }

  return (
    <Box>
      <Paper variant="outlined" sx={{ p: 2 }}>
        <TabsWithCounts value={tabIndex} onChange={onTabChange} tabs={tabs} />
      </Paper>

      <TabPanel value={tabIndex} index={0} sx={{ mt: 2 }}>
        <StoreToolbar title={toolbarByTab.native.title} warning={toolbarByTab.native.warning} actions={toolbarByTab.native.actions} />
        <SearchInput value={search} onChange={setSearch} />
        <Box sx={{ mt: 2 }}>
          <CertificateList rows={visibleRows} loading={loading} error={error} />
        </Box>
      </TabPanel>

      <TabPanel value={tabIndex} index={1} sx={{ mt: 2 }}>
        <StoreToolbar title={toolbarByTab.trusted.title} actions={toolbarByTab.trusted.actions} />
        <SearchInput value={search} onChange={setSearch} />
        <Box sx={{ mt: 2 }}>
          <CertificateList rows={visibleRows} loading={loading} error={error} />
        </Box>
      </TabPanel>

      <TabPanel value={tabIndex} index={2} sx={{ mt: 2 }}>
        <StoreToolbar title={toolbarByTab.private.title} actions={toolbarByTab.private.actions} />
        <SearchInput value={search} onChange={setSearch} />
        <Box sx={{ mt: 2 }}>
          <CertificateList rows={visibleRows} loading={loading} error={error} />
        </Box>
      </TabPanel>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)}>
        <DialogTitle>{dialogTitle}</DialogTitle>
        <DialogContent>{dialogContent}</DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
