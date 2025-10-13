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

import { parseCertificate, pemToBase64, privateKeyPemToBase64 } from '../utils/certificateUtils.js'
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
const CHANNEL_ASSIGNMENTS_KEY = 'tls-manager-channel-assignments'

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

// === MOCK CHANNEL ASSIGNMENT HELPERS ===
const CHANNEL_POOL = [
  'Channel 1', 'Channel 2', 'Channel 3', 'Channel 4', 
  'Channel 5', 'Channel 6', 'Channel 7', 'Channel 8'
]

// Generate random channels (1-3 channels from pool)
function generateMockChannels() {
  const numChannels = Math.floor(Math.random() * 3) + 1 // 1-3 channels
  const shuffled = [...CHANNEL_POOL].sort(() => 0.5 - Math.random())
  return shuffled.slice(0, numChannels)
}

// Returns true ~35% of the time
function shouldHaveChannels() {
  return Math.random() < 0.35
}

// Get or create channel assignments from localStorage
function getOrCreateChannelAssignments() {
  try {
    const stored = localStorage.getItem(CHANNEL_ASSIGNMENTS_KEY)
    if (stored) {
      return JSON.parse(stored)
    }
  } catch (e) {
    console.warn('Failed to load channel assignments from localStorage:', e)
  }
  
  // Create new assignments if none exist
  return {
    trusted: {},
    private: {}
  }
}

// Save channel assignments to localStorage
function saveChannelAssignments(assignments) {
  try {
    localStorage.setItem(CHANNEL_ASSIGNMENTS_KEY, JSON.stringify(assignments))
  } catch (e) {
    console.warn('Failed to save channel assignments to localStorage:', e)
  }
}

// Get channels for a specific certificate, creating if needed
function getChannelsForCertificate(store, alias, assignments) {
  const storeAssignments = assignments[store] || {}
  
  if (!(alias in storeAssignments)) {
    // Create new assignment
    storeAssignments[alias] = shouldHaveChannels() ? generateMockChannels() : []
    assignments[store] = storeAssignments
    saveChannelAssignments(assignments)
  }
  
  return storeAssignments[alias] || []
}

export async function fetchCertificates() {
  try {
    // === INTERNAL STORE (for development) ===
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 300))
    
    const data = internalStore
    
    // Get channel assignments
    const channelAssignments = getOrCreateChannelAssignments()
    
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
        const channelsInUse = getChannelsForCertificate('trusted', cert.alias, channelAssignments)
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
          channelsInUse: channelsInUse,
          rawCertificate: cert.certificate,
          parsedCertificate: parsed,
        })
      }
    }
    
    // Map pairs to private store
    if (data.pairs) {
      for (const pair of data.pairs) {
        const parsed = await parseCertificate(pair.certificate)
        const channelsInUse = getChannelsForCertificate('private', pair.alias, channelAssignments)
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
          channelsInUse: channelsInUse,
          rawCertificate: pair.certificate,
          rawPrivateKey: pair.privateKey, // Include private key in response
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

export async function updateCertificates(targetStore, certificateData) {
  try {
    // === INTERNAL STORE (for development) ===
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 300))
    
    const { alias, pemText, privateKeyText } = certificateData
    
    // Convert PEM to Base64 using utility functions
    const base64Certificate = pemToBase64(pemText)
    
    if (targetStore === 'trusted') {
      // Add to trusted certificates
      const cert = {
        alias,
        certificate: base64Certificate
      }
      
      const existing = internalStore.certificates.findIndex(c => c.alias === alias)
      if (existing >= 0) {
        internalStore.certificates[existing] = cert
      } else {
        internalStore.certificates.push(cert)
        // Initialize new certificate with no channels
        const channelAssignments = getOrCreateChannelAssignments()
        if (!channelAssignments.trusted) {
          channelAssignments.trusted = {}
        }
        channelAssignments.trusted[alias] = []
        saveChannelAssignments(channelAssignments)
      }
    } else if (targetStore === 'private') {
      // Add to private key pairs
      const base64PrivateKey = privateKeyPemToBase64(privateKeyText)
      
      const pair = {
        alias,
        certificate: base64Certificate,
        privateKey: base64PrivateKey
      }
      
      const existing = internalStore.pairs.findIndex(p => p.alias === alias)
      if (existing >= 0) {
        internalStore.pairs[existing] = pair
      } else {
        internalStore.pairs.push(pair)
        // Initialize new certificate with no channels
        const channelAssignments = getOrCreateChannelAssignments()
        if (!channelAssignments.private) {
          channelAssignments.private = {}
        }
        channelAssignments.private[alias] = []
        saveChannelAssignments(channelAssignments)
      }
    }
    
    // Save to localStorage
    saveToStorage()
    
    console.log('[Internal Store] Updated:', internalStore)
    
    return { success: true, data: { alias, targetStore } }
    
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

export async function updateCertificateAlias(store, oldAlias, newAlias) {
  try {
    // === INTERNAL STORE (for development) ===
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 300))
    
    if (store === 'trusted') {
      const certIndex = internalStore.certificates.findIndex(c => c.alias === oldAlias)
      if (certIndex >= 0) {
        internalStore.certificates[certIndex].alias = newAlias
      } else {
        throw new Error('Certificate not found')
      }
    } else if (store === 'private') {
      const pairIndex = internalStore.pairs.findIndex(p => p.alias === oldAlias)
      if (pairIndex >= 0) {
        internalStore.pairs[pairIndex].alias = newAlias
      } else {
        throw new Error('Certificate not found')
      }
    } else {
      throw new Error('Invalid store type')
    }
    
    // Update channel assignments to use new alias
    const channelAssignments = getOrCreateChannelAssignments()
    if (channelAssignments[store] && channelAssignments[store][oldAlias]) {
      channelAssignments[store][newAlias] = channelAssignments[store][oldAlias]
      delete channelAssignments[store][oldAlias]
      saveChannelAssignments(channelAssignments)
    }
    
    // Save to localStorage
    saveToStorage()
    
    console.log('[Internal Store] Updated alias:', { store, oldAlias, newAlias })
    
    return { success: true, data: { store, oldAlias, newAlias } }
    
    // === REAL API (uncomment when API is ready) ===
    // const response = await api.put(`/api/tlsmanager/certificates/${store}/alias`, {
    //   oldAlias,
    //   newAlias
    // })
    // return response.data
  } catch (error) {
    console.error('Failed to update certificate alias:', error)
    throw new Error('Failed to update certificate alias')
  }
}

export async function removeCertificate(store, alias) {
  try {
    // === INTERNAL STORE (for development) ===
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 300))
    
    if (store === 'trusted') {
      const certIndex = internalStore.certificates.findIndex(c => c.alias === alias)
      if (certIndex >= 0) {
        internalStore.certificates.splice(certIndex, 1)
      } else {
        throw new Error('Certificate not found')
      }
    } else if (store === 'private') {
      const pairIndex = internalStore.pairs.findIndex(p => p.alias === alias)
      if (pairIndex >= 0) {
        internalStore.pairs.splice(pairIndex, 1)
      } else {
        throw new Error('Certificate not found')
      }
    } else {
      throw new Error('Invalid store type')
    }
    
    // Clean up channel assignments
    const channelAssignments = getOrCreateChannelAssignments()
    if (channelAssignments[store] && channelAssignments[store][alias]) {
      delete channelAssignments[store][alias]
      saveChannelAssignments(channelAssignments)
    }
    
    // Save to localStorage
    saveToStorage()
    
    console.log('[Internal Store] Removed certificate:', { store, alias })
    
    return { success: true, data: { store, alias } }
    
    // === REAL API (uncomment when API is ready) ===
    // const response = await api.delete(`/api/tlsmanager/certificates/${store}/${alias}`)
    // return response.data
  } catch (error) {
    console.error('Failed to remove certificate:', error)
    throw new Error('Failed to remove certificate')
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
