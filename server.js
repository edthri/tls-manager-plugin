import express from 'express'
import { createProxyMiddleware } from 'http-proxy-middleware'

const target = 'https://oie-test.quantis.health'

const app = express()

app.use('/api', createProxyMiddleware({
  target,
  changeOrigin: true,
  xfwd: true,
  // Strip the Domain attribute so the cookie becomes host-only for your proxy host
  cookieDomainRewrite: '',
  // Optional: ensure SameSite/ Secure as needed
  onProxyRes(proxyRes) {
    const cookies = proxyRes.headers['set-cookie']
    if (cookies) {
      proxyRes.headers['set-cookie'] = cookies.map(c => {
        let v = c
        // Force SameSite=None for cross-site usage (only if served over HTTPS)
        if (!/; *SameSite=/i.test(v)) v += '; SameSite=None'
        // Add Secure if serving the proxy over HTTPS (required for SameSite=None)
        if (!/; *Secure/i.test(v)) v += '; Secure'
        return v
      })
    }
  },
}))

app.listen(3000, () => console.log('Proxy on http://localhost:3000'))