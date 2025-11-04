import { X509, KEYUTIL, KJUR } from 'jsrsasign'
import { convertX509TimeToDate, parseDNString } from './certificateUtils.js'

/**
 * Parse a certificate chain from PEM text (supports multiple certificates)
 * @param {string} certText - PEM certificate text (can contain multiple certificates)
 * @returns {Array} Array of certificate objects with pem and cert properties
 */
export function parseCertificateChain(certText) {
  const certificates = []
  const certRegex = /-----BEGIN CERTIFICATE-----[\s\S]*?-----END CERTIFICATE-----/g
  const matches = certText.match(certRegex)

  if (matches) {
    matches.forEach(certPem => {
      try {
        const cert = new X509()
        cert.readCertPEM(certPem)
        certificates.push({ pem: certPem, cert: cert })
      } catch (e) {
        console.error('Failed to parse certificate:', e)
      }
    })
  }

  return certificates
}

/**
 * Validate a certificate chain for proper ordering and signatures
 * @param {Array} certificates - Array of certificate objects
 * @returns {Object} Validation result with isValid, errors, warnings, and details
 */
export function validateCertificateChain(certificates) {
  const validation = {
    isValid: true,
    errors: [],
    warnings: [],
    details: []
  }

  if (certificates.length === 1) {
    validation.details.push('Single certificate provided - no chain validation needed')
    return validation
  }

  // Check chain order and signatures
  for (let i = 0; i < certificates.length - 1; i++) {
    const cert = certificates[i].cert
    const issuerCert = certificates[i + 1].cert

    validation.details.push(`Checking certificate ${i + 1} against issuer certificate ${i + 2}`)

    // Check if issuer name matches
    const certIssuer = cert.getIssuerString()
    const issuerSubject = issuerCert.getSubjectString()

    if (certIssuer !== issuerSubject) {
      validation.isValid = false
      validation.errors.push(`Certificate ${i + 1} issuer "${certIssuer}" does not match certificate ${i + 2} subject "${issuerSubject}"`)
    } else {
      validation.details.push(`✓ Issuer names match for certificates ${i + 1} and ${i + 2}`)
    }

    // Verify signature
    try {
      const isSignatureValid = cert.verifySignature(issuerCert.pem)  // jsrsasign requires PEM string
      if (isSignatureValid) {
        validation.details.push(`✓ Certificate ${i + 1} signature verified by certificate ${i + 2}`)
      } else {
        validation.isValid = false
        validation.errors.push(`Certificate ${i + 1} signature verification failed against certificate ${i + 2}`)
      }
    } catch (error) {
      validation.isValid = false
      validation.errors.push(`Error verifying certificate ${i + 1} signature: ${error.message}`)
    }

    // Check validity periods
    const certNotBefore = convertX509TimeToDate(cert.getNotBefore())
    const certNotAfter = convertX509TimeToDate(cert.getNotAfter())
    const issuerNotBefore = convertX509TimeToDate(issuerCert.getNotBefore())
    const issuerNotAfter = convertX509TimeToDate(issuerCert.getNotAfter())

    if (certNotBefore < issuerNotBefore) {
      validation.warnings.push(`Certificate ${i + 1} valid from date is before its issuer's valid from date`)
    }
    if (certNotAfter > issuerNotAfter) {
      validation.warnings.push(`Certificate ${i + 1} expires after its issuer certificate ${i + 2}`)
    }
  }

  // Check if root is self-signed
  const rootCert = certificates[certificates.length - 1].cert
  const rootIssuer = rootCert.getIssuerString()
  const rootSubject = rootCert.getSubjectString()

  if (rootIssuer === rootSubject) {
    try {
      const isSelfSigned = rootCert.verifySignature(rootCert.pem)
      if (isSelfSigned) {
        validation.details.push('✓ Root certificate is properly self-signed')
      } else {
        validation.warnings.push('Root certificate appears self-signed but signature verification failed')
      }
    } catch (error) {
      validation.warnings.push(`Error verifying root certificate self-signature: ${error.message}`)
    }
  } else {
    validation.warnings.push('Root certificate is not self-signed - chain may be incomplete')
  }

  // Check certificate purposes and constraints
  certificates.forEach((certObj, index) => {
    const cert = certObj.cert
    let basicConstraints = null
    try {
      basicConstraints = cert.getExtBasicConstraints()
    } catch (e) {
      // Extension doesn't exist or can't be read - continue with null
    }

    if (index === 0) {
      // End entity certificate
      if (basicConstraints && basicConstraints.ca) {  // lowercase 'ca' in jsrsasign
        validation.warnings.push('End entity certificate has CA flag set to true')
      }
    } else {
      // CA certificates
      if (!basicConstraints || !basicConstraints.ca) {  // lowercase 'ca' in jsrsasign
        validation.warnings.push(`Certificate ${index + 1} should be a CA but basicConstraints CA flag is not set`)
      }

      if (basicConstraints && typeof basicConstraints.pathLenConstraint === 'number') {
        const remainingCAs = certificates.length - index - 2 // Exclude self and count remaining CAs
        if (remainingCAs > basicConstraints.pathLenConstraint) {
          validation.errors.push(`Certificate ${index + 1} pathLenConstraint (${basicConstraints.pathLenConstraint}) exceeded by chain depth`)
          validation.isValid = false
        }
      }
    }
  })

  return validation
}

/**
 * Validate if a private key matches a certificate
 * @param {Object} certObj - Certificate object with cert property (X509 object)
 * @param {string} keyPem - PEM private key string
 * @returns {Object} Validation result with isValid and message
 */
export function validatePrivateKey(certObj, keyPem) {
  try {
    // Parse private key using KEYUTIL (handles all formats automatically)
    const privateKey = KEYUTIL.getKey(keyPem)
    if (!privateKey) {
      return { isValid: false, message: 'Failed to parse private key' }
    }

    // Get public key from certificate
    const certPubKeyPem = certObj.cert.getPublicKeyPEM()
    
    // Extract public key from private key
    const pubKeyFromPrivate = KEYUTIL.getPublicKeyFromPrivateKey(privateKey)
    const pubKeyPemFromPrivate = KEYUTIL.getPEMFromPublicKey(pubKeyFromPrivate)

    // Compare public keys (PEM format comparison)
    if (certPubKeyPem === pubKeyPemFromPrivate) {
      return { isValid: true, message: 'Private key matches the certificate!' }
    }

    // Alternative: Compare hex representations
    const certPubKeyHex = KEYUTIL.getHexFromPublicKey(KEYUTIL.getKey(certPubKeyPem))
    const privatePubKeyHex = KEYUTIL.getHexFromPublicKey(pubKeyFromPrivate)

    if (certPubKeyHex === privatePubKeyHex) {
      return { isValid: true, message: 'Private key matches the certificate!' }
    }

    // Last resort: Try signature verification
    try {
      const testData = 'test-data-for-validation'
      const sig = new KJUR.crypto.Signature({ alg: 'SHA256withRSA' })
      sig.init(privateKey)
      sig.updateString(testData)
      const signature = sig.sign()

      const certPubKey = KEYUTIL.getKey(certPubKeyPem)
      const verifier = new KJUR.crypto.Signature({ alg: 'SHA256withRSA' })
      verifier.init(certPubKey)
      verifier.updateString(testData)
      const isValid = verifier.verify(signature)

      if (isValid) {
        return { isValid: true, message: 'Private key matches the certificate!' }
      } else {
        return { isValid: false, message: 'Private key does not match the certificate' }
      }
    } catch (signError) {
      // If signature verification fails (e.g., EC keys need different algorithm)
      // Fall back to fingerprint comparison
      const certKeyFingerprint = getPublicKeyFingerprint(KEYUTIL.getKey(certPubKeyPem))
      const privateKeyFingerprint = getPrivateKeyFingerprint(privateKey)

      if (certKeyFingerprint === privateKeyFingerprint) {
        return { isValid: true, message: 'Private key matches the certificate!' }
      } else {
        return { isValid: false, message: 'Private key does not match the certificate' }
      }
    }

  } catch (error) {
    return { isValid: false, message: `Error validating private key: ${error.message}` }
  }
}

/**
 * Get certificate status (valid, expired, not yet valid)
 * @param {Object} cert - Certificate object (X509 from jsrsasign)
 * @returns {string} Status message
 */
export function getCertStatus(cert) {
  const now = new Date()
  const notBefore = convertX509TimeToDate(cert.getNotBefore())
  const notAfter = convertX509TimeToDate(cert.getNotAfter())
  
  if (now < notBefore) {
    return '⏳ Not yet valid'
  } else if (now > notAfter) {
    return '⚠️ Expired'
  } else {
    return '✅ Valid'
  }
}

/**
 * Get certificate fingerprint
 * @param {Object} cert - Certificate object (X509 from jsrsasign)
 * @param {string} algorithm - Hash algorithm ('sha1' or 'sha256')
 * @returns {string} Formatted fingerprint
 */
export function getFingerprint(cert, algorithm = 'sha1') {
  const derHex = cert.hex  // DER-encoded certificate as hex string
  const fingerprint = KJUR.crypto.Util.hashHex(derHex, algorithm)
  return fingerprint.toUpperCase().replace(/(.{2})/g, '$1:').slice(0, -1)
}

/**
 * Get Subject Alternative Names from certificate
 * @param {Object} cert - Certificate object (X509 from jsrsasign)
 * @returns {Array} Array of SAN strings
 */
export function getSANs(cert) {
  const sans = cert.getExtSubjectAltName()
  if (!sans || !Array.isArray(sans)) {
    return []
  }
  
  // jsrsasign returns array of arrays: [[type, value], ...]
  // type: 2=DNS, 7=IP, 1=Email
  return sans.map((sanEntry) => {
    const [type, value] = Array.isArray(sanEntry) ? sanEntry : [sanEntry.type, sanEntry.value]
    switch (type) {
      case 2: return 'DNS: ' + value
      case 7: return 'IP: ' + value
      case 1: return 'Email: ' + value
      default: return 'Other: ' + value
    }
  })
}

/**
 * Get key size from certificate
 * @param {Object} cert - Certificate object (X509 from jsrsasign)
 * @returns {number|string} Key size in bits or 'Unknown'
 */
export function getKeySize(cert) {
  try {
    const pubKeyPem = cert.getPublicKeyPEM()
    const pubKeyObj = KEYUTIL.getKey(pubKeyPem)
    
    if (pubKeyObj) {
      // For RSA
      if (pubKeyObj.n) {
        return pubKeyObj.n.bitLength()
      }
      // For EC
      if (pubKeyObj.curve) {
        // Map curve names to bit sizes
        const curveMap = {
          'secp256r1': 256,
          'secp384r1': 384,
          'secp521r1': 521,
          'secp256k1': 256,
          'prime256v1': 256,
          'P-256': 256,
          'P-384': 384,
          'P-521': 521,
        }
        return curveMap[pubKeyObj.curve] || 'Unknown'
      }
    }
  } catch (error) {
    console.error('Error getting key size:', error)
  }
  return 'Unknown'
}

/**
 * Get Distinguished Name as string
 * @param {string} dnString - Distinguished Name string (from X509.getSubjectString() or getIssuerString())
 * @returns {string} Formatted DN string
 */
function getDistinguishedName(dnString) {
  // Already a string from jsrsasign, just return it
  return dnString || 'Unknown'
}

/**
 * Get public key fingerprint
 * @param {Object} publicKey - Public key object (from KEYUTIL)
 * @returns {string} SHA-256 fingerprint
 */
function getPublicKeyFingerprint(publicKey) {
  const pubKeyHex = KEYUTIL.getHexFromPublicKey(publicKey)
  const fingerprint = KJUR.crypto.Util.hashHex(pubKeyHex, 'sha256')
  return fingerprint
}

/**
 * Get private key fingerprint
 * @param {Object} privateKey - Private key object (from KEYUTIL)
 * @returns {string} SHA-256 fingerprint
 */
function getPrivateKeyFingerprint(privateKey) {
  const publicKey = KEYUTIL.getPublicKeyFromPrivateKey(privateKey)
  return getPublicKeyFingerprint(publicKey)
}

/**
 * Get subject field value from certificate
 * @param {Object} cert - Certificate object (X509 from jsrsasign)
 * @param {string} field - Field name (CN, O, C, etc.)
 * @param {string} type - 'subject' or 'issuer'
 * @returns {string} Field value or 'Not specified'
 */
export function getSubjectField(cert, field, type = 'subject') {
  const dnString = type === 'subject' ? cert.getSubjectString() : cert.getIssuerString()
  const attrs = parseDNString(dnString)
  return attrs[field] || 'Not specified'
}

/**
 * Comprehensive certificate verification
 * @param {string} certText - PEM certificate text
 * @param {string} keyText - Optional PEM private key text
 * @returns {Object} Complete verification results
 */
export function verifyCertificate(certText, keyText = null) {
  try {
    // Parse certificates
    const certificates = parseCertificateChain(certText)

    if (certificates.length === 0) {
      return {
        success: false,
        error: 'No valid certificates found'
      }
    }

    // Get certificate details
    const primaryCert = certificates[0].cert
    const notBefore = convertX509TimeToDate(primaryCert.getNotBefore())
    const notAfter = convertX509TimeToDate(primaryCert.getNotAfter())
    
    // Get signature algorithm
    const sigAlg = primaryCert.getSignatureAlgorithmName() || 'Unknown'
    
    // Get public key algorithm
    const pubKeyPem = primaryCert.getPublicKeyPEM()
    const pubKeyObj = KEYUTIL.getKey(pubKeyPem)
    let pubKeyAlg = 'RSA'
    if (pubKeyObj) {
      if (pubKeyObj.curve) {
        pubKeyAlg = 'ECDSA'
      } else if (pubKeyObj.alg && pubKeyObj.alg.includes('ECDSA')) {
        pubKeyAlg = 'ECDSA'
      }
    }
    
    const certDetails = {
      subject: getSubjectField(primaryCert, 'CN'),
      issuer: getSubjectField(primaryCert, 'CN', 'issuer'),
      serialNumber: primaryCert.getSerialNumberHex() || primaryCert.getSerialNumber(),
      validFrom: notBefore ? notBefore.toISOString() : 'Unknown',
      validTo: notAfter ? notAfter.toISOString() : 'Unknown',
      status: getCertStatus(primaryCert),
      signatureAlgorithm: sigAlg,
      publicKeyAlgorithm: pubKeyAlg,
      keySize: getKeySize(primaryCert),
      fingerprintSha1: getFingerprint(primaryCert, 'sha1'),
      fingerprintSha256: getFingerprint(primaryCert, 'sha256'),
      sans: getSANs(primaryCert)
    }

    // Validate certificate chain
    const chainValidation = validateCertificateChain(certificates)

    // Validate private key if provided
    let keyValidation = null
    if (keyText) {
      keyValidation = validatePrivateKey(certificates[0], keyText)
    }

    // Determine overall success based on validation results
    const chainValid = chainValidation && chainValidation.isValid
    const keyValid = !keyText || (keyValidation && keyValidation.isValid)
    const overallSuccess = chainValid && keyValid

    return {
      success: overallSuccess,
      certificates,
      certDetails,
      chainValidation,
      keyValidation,
      chainDetails: certificates.length > 1 ? certificates.map((certObj, index) => {
        const certNotBefore = convertX509TimeToDate(certObj.cert.getNotBefore())
        const certNotAfter = convertX509TimeToDate(certObj.cert.getNotAfter())
        return {
          index: index + 1,
          type: index === 0 ? 'End Entity' : index === certificates.length - 1 ? 'Root CA' : 'Intermediate CA',
          subject: getSubjectField(certObj.cert, 'CN'),
          issuer: getSubjectField(certObj.cert, 'CN', 'issuer'),
          validFrom: certNotBefore ? certNotBefore.toDateString() : 'Unknown',
          validTo: certNotAfter ? certNotAfter.toDateString() : 'Unknown'
        }
      }) : null,
      error: !overallSuccess ? 
        (!chainValid ? 'Certificate chain validation failed' : 'Private key validation failed') : 
        null
    }

  } catch (error) {
    return {
      success: false,
      error: `Error parsing certificate: ${error.message}`
    }
  }
}
