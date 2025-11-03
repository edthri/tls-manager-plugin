import forge from 'node-forge'

/**
 * Parse a Base64-encoded PEM certificate and extract relevant information
 * @param {string} base64Pem - Base64-encoded PEM certificate
 * @returns {Object} Parsed certificate information
 */
export function parseCertificate(base64Pem) {
  try {

    const pemString = base64Pem
    
    // Parse the PEM certificate
    const cert = forge.pki.certificateFromPem(pemString)
    
    // Extract subject information
    const subject = cert.subject
    const subjectStr = formatDN(subject)
    
    // Extract issuer information
    const issuer = cert.issuer
    const issuerStr = formatDN(issuer)
    
    // Determine certificate type
    const type = determineCertificateType(cert)
    
    // Format validity dates
    const validFrom = formatDate(cert.validity.notBefore)
    const validTo = formatDate(cert.validity.notAfter)
    
    // Calculate SHA-1 fingerprint
    const fingerprintSha1 = forge.md.sha1.create()
      .update(forge.asn1.toDer(forge.pki.certificateToAsn1(cert)).getBytes())
      .digest()
      .toHex()
      .toUpperCase()
    
    return {
      subject,
      subjectStr,
      issuer,
      issuerStr,
      type,
      validFrom,
      validTo,
      fingerprintSha1,
      serialNumber: cert.serialNumber,
      version: cert.version,
      extensions: cert.extensions,
      raw: cert
    }
  } catch (error) {
    console.error('Failed to parse certificate:', error)
    return {
      subject: null,
      subjectStr: 'Parse Error',
      issuer: null,
      issuerStr: 'Parse Error',
      type: 'Unknown',
      validFrom: 'Unknown',
      validTo: 'Unknown',
      fingerprintSha1: 'Unknown',
      error: error.message
    }
  }
}

/**
 * Format a Distinguished Name (DN) object to string
 * @param {Object} dn - Distinguished Name object from node-forge
 * @returns {string} Formatted DN string
 */
function formatDN(dn) {
  if (!dn) return 'Unknown'
  
  const parts = []
  
  // Common DN attributes in order of preference
  const attributes = ['CN', 'OU', 'O', 'L', 'ST', 'C', 'emailAddress']
  
  for (const attr of attributes) {
    const field = dn.getField(attr)
    if (field) {
      // Extract the value from the field object
      const value = field.value || field
      parts.push(`${attr}=${value}`)
    }
  }
  
  // Add any remaining attributes not in our preferred list
  const allAttrs = dn.attributes || []
  for (const attr of allAttrs) {
    if (!attributes.includes(attr.name)) {
      parts.push(`${attr.name}=${attr.value}`)
    }
  }
  
  return parts.join(', ')
}

/**
 * Determine certificate type based on extensions and usage
 * @param {Object} cert - Certificate object from node-forge
 * @returns {string} Certificate type
 */
function determineCertificateType(cert) {
  if (!cert.extensions) return 'End-entity'
  
  // Check for CA certificate
  const basicConstraints = cert.extensions.find(ext => ext.name === 'basicConstraints')
  if (basicConstraints && basicConstraints.cA) {
    return 'Root CA'
  }
  
  // Check for intermediate CA
  const keyUsage = cert.extensions.find(ext => ext.name === 'keyUsage')
  if (keyUsage && keyUsage.keyCertSign) {
    return 'Intermediate'
  }
  
  // Check for server certificate
  const extKeyUsage = cert.extensions.find(ext => ext.name === 'extKeyUsage')
  if (extKeyUsage && extKeyUsage.serverAuth) {
    return 'Server Certificate'
  }
  
  // Check for client certificate
  if (extKeyUsage && extKeyUsage.clientAuth) {
    return 'Client Certificate'
  }
  
  return 'End-entity'
}

/**
 * Format a date object to YYYY-MM-DD string
 * @param {Date} date - Date object
 * @returns {string} Formatted date string
 */
function formatDate(date) {
  if (!date) return 'Unknown'
  
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  
  return `${year}-${month}-${day}`
}

/**
 * Validate if a string contains valid PEM certificate data
 * @param {string} pemString - PEM certificate string
 * @returns {boolean} True if valid PEM certificate
 */
export function isValidPemCertificate(pemString) {
  try {
    // Check if it contains certificate markers
    if (!pemString.includes('-----BEGIN CERTIFICATE-----') || 
        !pemString.includes('-----END CERTIFICATE-----')) {
      return false
    }
    
    // Try to parse it
    const cert= forge.pki.certificateFromPem(pemString);
    
    return true
  } catch (error) {
    console.error('Failed to validate PEM certificate:', error)
    return false
  }
}

/**
 * Validate if a string contains valid PEM private key data
 * @param {string} pemString - PEM private key string
 * @returns {boolean} True if valid PEM private key
 */
export function isValidPemPrivateKey(pemString) {
  try {
    // Check if it contains private key markers (support multiple formats)
    const hasPrivateKeyMarkers = (
      (pemString.includes('-----BEGIN PRIVATE KEY-----') && pemString.includes('-----END PRIVATE KEY-----')) ||
      (pemString.includes('-----BEGIN RSA PRIVATE KEY-----') && pemString.includes('-----END RSA PRIVATE KEY-----')) ||
      (pemString.includes('-----BEGIN EC PRIVATE KEY-----') && pemString.includes('-----END EC PRIVATE KEY-----')) ||
      (pemString.includes('-----BEGIN DSA PRIVATE KEY-----') && pemString.includes('-----END DSA PRIVATE KEY-----'))
    )
    
    if (!hasPrivateKeyMarkers) {
      return false
    }
    
    // Try to parse it as a private key
    const privateKey = forge.pki.privateKeyFromPem(pemString)
    
    return true
  } catch (error) {
    console.error('Failed to validate PEM private key:', error)
    return false
  }
}

/**
 * Convert PEM string to Base64-encoded format
 * @param {string} pemString - PEM certificate string
 * @returns {string} Base64-encoded certificate
 */
export function pemToBase64(pemString) {
  try {
    // Remove PEM headers and footers
    const base64Content = pemString
      .replace(/-----BEGIN CERTIFICATE-----/g, '')
      .replace(/-----END CERTIFICATE-----/g, '')
      .replace(/\s/g, '') // Remove whitespace
    
    return base64Content
  } catch (error) {
    console.error('Failed to convert PEM to Base64:', error)
    throw new Error('Invalid PEM format')
  }
}

/**
 * Convert PEM private key string to Base64-encoded format
 * @param {string} pemString - PEM private key string
 * @returns {string} Base64-encoded private key
 */
export function privateKeyPemToBase64(pemString) {
  try {
    // Remove all possible private key headers and footers
    const base64Content = pemString
      .replace(/-----BEGIN PRIVATE KEY-----/g, '')
      .replace(/-----END PRIVATE KEY-----/g, '')
      .replace(/-----BEGIN RSA PRIVATE KEY-----/g, '')
      .replace(/-----END RSA PRIVATE KEY-----/g, '')
      .replace(/-----BEGIN EC PRIVATE KEY-----/g, '')
      .replace(/-----END EC PRIVATE KEY-----/g, '')
      .replace(/-----BEGIN DSA PRIVATE KEY-----/g, '')
      .replace(/-----END DSA PRIVATE KEY-----/g, '')
      .replace(/\s/g, '') // Remove whitespace
    
    return base64Content
  } catch (error) {
    console.error('Failed to convert private key PEM to Base64:', error)
    throw new Error('Invalid private key PEM format')
  }
}

/**
 * Convert Base64-encoded certificate to PEM format
 * @param {string} base64Cert - Base64-encoded certificate
 * @returns {string} PEM certificate string
 */
export function base64ToPem(base64Cert) {
  try {
    // Add PEM headers
    const pemString = `-----BEGIN CERTIFICATE-----\n${base64Cert}\n-----END CERTIFICATE-----`
    return pemString
  } catch (error) {
    console.error('Failed to convert Base64 to PEM:', error)
    throw new Error('Invalid Base64 format')
  }
}

/**
 * Convert Base64-encoded private key to PEM format
 * @param {string} base64Key - Base64-encoded private key
 * @returns {string} PEM private key string
 */
export function base64ToPrivateKeyPem(base64Key) {
  try {
    // Add PEM headers for private key
    const pemString = `-----BEGIN PRIVATE KEY-----\n${base64Key}\n-----END PRIVATE KEY-----`
    return pemString
  } catch (error) {
    console.error('Failed to convert Base64 to private key PEM:', error)
    throw new Error('Invalid Base64 private key format')
  }
}
