import express from 'express'
import { createProxyMiddleware } from 'http-proxy-middleware'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const app = express()
const PORT = process.env.PORT || 3000

// API target - you can override this with environment variable
const API_TARGET = process.env.API_TARGET || 'https://oie-1.quantis.health/api'

// Serve static files from the dashboard directory
app.use('/dashboard', express.static(path.join(__dirname, 'dashboard'), {
  // Set proper MIME types for JavaScript modules
  setHeaders: (res, filePath) => {
    if (filePath.endsWith('.js')) {
      res.setHeader('Content-Type', 'application/javascript')
    } else if (filePath.endsWith('.mjs')) {
      res.setHeader('Content-Type', 'application/javascript')
    } else if (filePath.endsWith('.css')) {
      res.setHeader('Content-Type', 'text/css')
    }
  }
}))

// Proxy API requests to the backend
app.use('/api', createProxyMiddleware({
  target: API_TARGET,
  changeOrigin: true,
  secure: false, // for local https dev
  logger: console,
  on: {
    proxyReq(proxyReq, req, res) {
      console.log('➡️', req.method, req.url);
    },
    proxyRes(proxyRes, req, res) {
      console.log('⬅️', proxyRes.statusCode);
    },
    error(err, req, res) {
      console.error('❌', err.message);
      res.status(500).json({ error: 'Proxy error' });
    }
  }
}))

// Redirect root to dashboard
app.get('/', (req, res) => {
  res.redirect('/dashboard')
})

// Handle client-side routing - serve index.html for dashboard routes
app.get('/dashboard', (req, res) => {
  res.sendFile(path.join(__dirname, 'dashboard', 'index.html'))
})

// Catch-all route for client-side routing (must be last)
app.use((req, res) => {
  if (req.path.startsWith('/dashboard')) {
    res.sendFile(path.join(__dirname, 'dashboard', 'index.html'))
  } else {
    res.status(404).send('Not Found')
  }
})

app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 Server running on http://0.0.0.0:${PORT}`)
  console.log(`📁 Serving static files from: ${path.join(__dirname, 'dashboard')}`)
  console.log(`🔗 Proxying API requests to: ${API_TARGET}`)
  console.log(`🌐 Access your app at: http://localhost:${PORT}/dashboard`)
})
