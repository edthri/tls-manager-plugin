/**
 * TLS Service - Certificate Management
 * 
 * Currently using internal store for development.
 * 
 * TO SWITCH TO REAL API:
 * 1. Uncomment the api import line below
 * 2. In fetchCertificates(): comment out the "INTERNAL STORE" section and uncomment the "REAL API" section
 * 3. In updateCertificates(): comment out the "INTERNAL STORE" section and uncomment the "REAL API" section
 * 4. Remove or comment out the internal store variables and helper functions at the bottom
 */

import { parseCertificate } from '../utils/certificateUtils.js'
// import { api } from './api.js' // Uncomment when API is ready

// === INTERNAL STORE (remove when switching to real API) ===
// Internal store to simulate API - starts empty
let internalStore = {
  systemCertificates: [],
  certificates: [],
  pairs: []
}

// Load from localStorage if available
const STORAGE_KEY = 'tls-manager-store'
try {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored) {
    internalStore = JSON.parse(stored)
  }
} catch (e) {
  console.warn('Failed to load from localStorage:', e)
}

// Save to localStorage
function saveToStorage() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(internalStore))
  } catch (e) {
    console.warn('Failed to save to localStorage:', e)
  }
}

export async function fetchCertificates() {
  try {
    // === INTERNAL STORE (for development) ===
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 300))
    
    const data = internalStore
    
    // === REAL API (uncomment when API is ready) ===
    // const response = await api.get('/api/tlsmanager/certificates')
    // const data = response.data
    
    const certificates = []
    
    // Map systemCertificates to native store
    if (data.systemCertificates) {
      for (const cert of data.systemCertificates) {
        const parsed = await parseCertificate(cert.certificate)
        certificates.push({
          alias: cert.alias,
          name: parsed.subject?.CN || cert.alias,
          type: parsed.type || 'Unknown',
          subject: parsed.subjectStr || 'Unknown',
          issuer: parsed.issuerStr || 'Unknown',
          validFrom: parsed.validFrom,
          validTo: parsed.validTo,
          fingerprintSha1: parsed.fingerprintSha1,
          hasPrivateKey: false,
          store: 'native',
          rawCertificate: cert.certificate,
          parsedCertificate: parsed,
        })
      }
    }
    
    // Map certificates to trusted store
    if (data.certificates) {
      for (const cert of data.certificates) {
        const parsed = await parseCertificate(cert.certificate)
        certificates.push({
          alias: cert.alias,
          name: parsed.subject?.CN || cert.alias,
          type: parsed.type || 'Unknown',
          subject: parsed.subjectStr || 'Unknown',
          issuer: parsed.issuerStr || 'Unknown',
          validFrom: parsed.validFrom,
          validTo: parsed.validTo,
          fingerprintSha1: parsed.fingerprintSha1,
          hasPrivateKey: false,
          store: 'trusted',
          channelsInUse: cert.channelsInUse || [],
          rawCertificate: cert.certificate,
          parsedCertificate: parsed,
        })
      }
    }
    
    // Map pairs to private store
    if (data.pairs) {
      for (const pair of data.pairs) {
        const parsed = await parseCertificate(pair.certificate)
        certificates.push({
          alias: pair.alias,
          name: parsed.subject?.CN || pair.alias,
          type: parsed.type || 'Unknown',
          subject: parsed.subjectStr || 'Unknown',
          issuer: parsed.issuerStr || 'Unknown',
          validFrom: parsed.validFrom,
          validTo: parsed.validTo,
          fingerprintSha1: parsed.fingerprintSha1,
          hasPrivateKey: true,
          store: 'private',
          channelsInUse: pair.channelsInUse || [],
          rawCertificate: pair.certificate,
          parsedCertificate: parsed,
        })
      }
    }
    
    return certificates
  } catch (error) {
    console.error('Failed to fetch certificates:', error)
    throw new Error('Failed to fetch certificates from server')
  }
}

export async function updateCertificates(certificates, pairs) {
  try {
    // === INTERNAL STORE (for development) ===
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 300))
    
    // Update internal store
    if (certificates && certificates.length > 0) {
      // Add new certificates to the store
      for (const cert of certificates) {
        const existing = internalStore.certificates.findIndex(c => c.alias === cert.alias)
        if (existing >= 0) {
          internalStore.certificates[existing] = cert
        } else {
          internalStore.certificates.push(cert)
        }
      }
    }
    
    if (pairs && pairs.length > 0) {
      // Add new pairs to the store
      for (const pair of pairs) {
        const existing = internalStore.pairs.findIndex(p => p.alias === pair.alias)
        if (existing >= 0) {
          internalStore.pairs[existing] = pair
        } else {
          internalStore.pairs.push(pair)
        }
      }
    }
    
    // Save to localStorage
    saveToStorage()
    
    console.log('[Internal Store] Updated:', internalStore)
    
    return { success: true }
    
    // === REAL API (uncomment when API is ready) ===
    // const payload = {}
    // 
    // if (certificates && certificates.length > 0) {
    //   payload.certificates = certificates.map(cert => ({
    //     alias: cert.alias,
    //     certificate: cert.certificate // Base64-encoded PEM
    //   }))
    // }
    // 
    // if (pairs && pairs.length > 0) {
    //   payload.pairs = pairs.map(pair => ({
    //     alias: pair.alias,
    //     certificate: pair.certificate, // Base64-encoded PEM
    //     privateKey: pair.privateKey // Base64-encoded PEM
    //   }))
    // }
    // 
    // const response = await api.put('/api/tlsmanager/certificates', payload)
    // return response.data
  } catch (error) {
    console.error('Failed to update certificates:', error)
    throw new Error('Failed to update certificates in internal store')
  }
}

// === INTERNAL STORE HELPER FUNCTIONS (remove when switching to real API) ===
// Helper function to clear the internal store (useful for testing)
export function clearInternalStore() {
  internalStore = {
    systemCertificates: [],
    certificates: [],
    pairs: []
  }
  saveToStorage()
  console.log('[Internal Store] Cleared')
}

// Helper function to get current store state (for debugging)
export function getInternalStore() {
  return { ...internalStore }
}
