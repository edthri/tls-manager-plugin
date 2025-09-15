import React, { useState } from 'react'
import { AppBar, Toolbar, Drawer, List, ListItemButton, ListItemText, IconButton, Box, ListItemIcon } from '@mui/material'
import MenuIcon from '@mui/icons-material/Menu'
import { Link } from 'react-router-dom'
import logo from '../assets/oie_logo_bottom_text.svg'
import SecurityIcon from '@mui/icons-material/Security';

const drawerWidth = 200

export default function DashboardLayout({ children }) {
  const [mobileOpen, setMobileOpen] = useState(false)

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen)
  }

  const drawer = (
    <div>
      <Toolbar />
      <List>
        <ListItemButton component={Link} to="/ssl">
          <ListItemIcon sx={{ minWidth: 30 }}>
            <SecurityIcon />
          </ListItemIcon>
          <ListItemText sx={{ fontSize: 16 }} primary="SSL management" />
        </ListItemButton>
      </List>
    </div>
  )

  return (
    <Box sx={{ display: 'flex'}}>
      <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1,background: 'linear-gradient(0deg, white 16%, rgb(254, 213, 216) 100%)'  }}>
        <Toolbar sx={{ gap: 2 }}>
          <IconButton color="inherit" edge="start" onClick={handleDrawerToggle} sx={{ mr: 2, display: { sm: 'none' } }}>
            <MenuIcon />
          </IconButton>
          <Box component="img" src={logo} alt="Company logo" sx={{ height: 60 }} />
        </Toolbar>
      </AppBar>

      <Drawer
        variant="permanent"
        sx={{
          display: { xs: 'none', sm: 'block' },
          '& .MuiDrawer-paper': { width: drawerWidth, boxSizing: 'border-box' },
        }}
        open
      >
        {drawer}
      </Drawer>

      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={handleDrawerToggle}
        ModalProps={{ keepMounted: true }}
        sx={{
          display: { xs: 'block', sm: 'none' },
          '& .MuiDrawer-paper': { width: drawerWidth, boxSizing: 'border-box' },
        }}
      >
        {drawer}
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1, p: 3, height: '100vh', ml: { sm: `${drawerWidth}px` } }}>
        <Toolbar />
        {children}
      </Box>
    </Box>
  )
}
