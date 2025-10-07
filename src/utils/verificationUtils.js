import forge from 'node-forge'

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
        const cert = forge.pki.certificateFromPem(certPem)
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
    const certIssuer = getDistinguishedName(cert.issuer)
    const issuerSubject = getDistinguishedName(issuerCert.subject)

    if (certIssuer !== issuerSubject) {
      validation.isValid = false
      validation.errors.push(`Certificate ${i + 1} issuer "${certIssuer}" does not match certificate ${i + 2} subject "${issuerSubject}"`)
    } else {
      validation.details.push(`✓ Issuer names match for certificates ${i + 1} and ${i + 2}`)
    }

    // Verify signature
    try {
      const isSignatureValid = cert.verify(issuerCert)
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
    if (cert.validity.notBefore < issuerCert.validity.notBefore) {
      validation.warnings.push(`Certificate ${i + 1} valid from date is before its issuer's valid from date`)
    }
    if (cert.validity.notAfter > issuerCert.validity.notAfter) {
      validation.warnings.push(`Certificate ${i + 1} expires after its issuer certificate ${i + 2}`)
    }
  }

  // Check if root is self-signed
  const rootCert = certificates[certificates.length - 1].cert
  const rootIssuer = getDistinguishedName(rootCert.issuer)
  const rootSubject = getDistinguishedName(rootCert.subject)

  if (rootIssuer === rootSubject) {
    try {
      const isSelfSigned = rootCert.verify(rootCert)
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
    const basicConstraints = cert.getExtension('basicConstraints')

    if (index === 0) {
      // End entity certificate
      if (basicConstraints && basicConstraints.cA) {
        validation.warnings.push('End entity certificate has CA flag set to true')
      }
    } else {
      // CA certificates
      if (!basicConstraints || !basicConstraints.cA) {
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
 * @param {Object} certObj - Certificate object with cert property
 * @param {string} keyPem - PEM private key string
 * @returns {Object} Validation result with isValid and message
 */
export function validatePrivateKey(certObj, keyPem) {
  try {
    let privateKey
    
    // Try different key formats
    if (keyPem.includes('BEGIN PRIVATE KEY')) {
      privateKey = forge.pki.privateKeyFromPem(keyPem)
    } else if (keyPem.includes('BEGIN RSA PRIVATE KEY')) {
      privateKey = forge.pki.privateKeyFromPem(keyPem)
    } else if (keyPem.includes('BEGIN EC PRIVATE KEY')) {
      privateKey = forge.pki.privateKeyFromPem(keyPem)
    } else {
      throw new Error('Unsupported private key format')
    }

    // Generate a test signature to verify key matches certificate
    const testData = 'test-data-for-validation'
    const md = forge.md.sha256.create()
    md.update(testData)

    try {
      const signature = privateKey.sign(md)
      const publicKey = certObj.cert.publicKey
      const isValid = publicKey.verify(md.digest().bytes(), signature)

      if (isValid) {
        return { isValid: true, message: 'Private key matches the certificate!' }
      } else {
        return { isValid: false, message: 'Private key does not match the certificate' }
      }
    } catch (signError) {
      // Alternative validation method using key fingerprints
      const certKeyFingerprint = getPublicKeyFingerprint(certObj.cert.publicKey)
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
 * @param {Object} cert - Certificate object from node-forge
 * @returns {string} Status message
 */
export function getCertStatus(cert) {
  const now = new Date()
  if (now < cert.validity.notBefore) {
    return '⏳ Not yet valid'
  } else if (now > cert.validity.notAfter) {
    return '⚠️ Expired'
  } else {
    return '✅ Valid'
  }
}

/**
 * Get certificate fingerprint
 * @param {Object} cert - Certificate object from node-forge
 * @param {string} algorithm - Hash algorithm ('sha1' or 'sha256')
 * @returns {string} Formatted fingerprint
 */
export function getFingerprint(cert, algorithm = 'sha1') {
  const der = forge.asn1.toDer(forge.pki.certificateToAsn1(cert)).getBytes()
  let md

  switch (algorithm) {
    case 'sha1':
      md = forge.md.sha1.create()
      break
    case 'sha256':
      md = forge.md.sha256.create()
      break
    default:
      md = forge.md.sha1.create()
  }

  md.update(der)
  return md.digest().toHex().toUpperCase().replace(/(.{2})/g, '$1:').slice(0, -1)
}

/**
 * Get Subject Alternative Names from certificate
 * @param {Object} cert - Certificate object from node-forge
 * @returns {Array} Array of SAN strings
 */
export function getSANs(cert) {
  const subjectAltName = cert.getExtension('subjectAltName')
  if (subjectAltName) {
    return subjectAltName.altNames.map(altName => {
      switch (altName.type) {
        case 2: return 'DNS: ' + altName.value
        case 7: return 'IP: ' + altName.ip
        case 1: return 'Email: ' + altName.value
        default: return 'Other: ' + altName.value
      }
    })
  }
  return []
}

/**
 * Get key size from public key
 * @param {Object} publicKey - Public key object from node-forge
 * @returns {number|string} Key size in bits or 'Unknown'
 */
export function getKeySize(publicKey) {
  if (publicKey.n) {
    return publicKey.n.bitLength()
  }
  return 'Unknown'
}

/**
 * Get Distinguished Name as string
 * @param {Object} name - Distinguished Name object from node-forge
 * @returns {string} Formatted DN string
 */
function getDistinguishedName(name) {
  return name.attributes.map(attr => `${attr.shortName}=${attr.value}`).join(', ')
}

/**
 * Get public key fingerprint
 * @param {Object} publicKey - Public key object from node-forge
 * @returns {string} SHA-256 fingerprint
 */
function getPublicKeyFingerprint(publicKey) {
  const publicKeyDer = forge.asn1.toDer(forge.pki.publicKeyToAsn1(publicKey)).getBytes()
  const md = forge.md.sha256.create()
  md.update(publicKeyDer)
  return md.digest().toHex()
}

/**
 * Get private key fingerprint
 * @param {Object} privateKey - Private key object from node-forge
 * @returns {string} SHA-256 fingerprint
 */
function getPrivateKeyFingerprint(privateKey) {
  const publicKey = forge.pki.rsa.setPublicKey(privateKey.n, privateKey.e)
  return getPublicKeyFingerprint(publicKey)
}

/**
 * Get subject field value from certificate
 * @param {Object} cert - Certificate object from node-forge
 * @param {string} field - Field name (CN, O, C, etc.)
 * @param {string} type - 'subject' or 'issuer'
 * @returns {string} Field value or 'Not specified'
 */
export function getSubjectField(cert, field, type = 'subject') {
  const subject = type === 'subject' ? cert.subject : cert.issuer
  const attr = subject.attributes.find(attr => attr.shortName === field)
  return attr ? attr.value : 'Not specified'
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
    const certDetails = {
      subject: getSubjectField(primaryCert, 'CN'),
      issuer: getSubjectField(primaryCert, 'CN', 'issuer'),
      serialNumber: primaryCert.serialNumber,
      validFrom: primaryCert.validity.notBefore.toISOString(),
      validTo: primaryCert.validity.notAfter.toISOString(),
      status: getCertStatus(primaryCert),
      signatureAlgorithm: primaryCert.siginfo.algorithmOid,
      publicKeyAlgorithm: primaryCert.publicKey.algorithm || 'RSA',
      keySize: getKeySize(primaryCert.publicKey),
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

    return {
      success: true,
      certificates,
      certDetails,
      chainValidation,
      keyValidation,
      chainDetails: certificates.length > 1 ? certificates.map((certObj, index) => ({
        index: index + 1,
        type: index === 0 ? 'End Entity' : index === certificates.length - 1 ? 'Root CA' : 'Intermediate CA',
        subject: getSubjectField(certObj.cert, 'CN'),
        issuer: getSubjectField(certObj.cert, 'CN', 'issuer'),
        validFrom: certObj.cert.validity.notBefore.toDateString(),
        validTo: certObj.cert.validity.notAfter.toDateString()
      })) : null
    }

  } catch (error) {
    return {
      success: false,
      error: `Error parsing certificate: ${error.message}`
    }
  }
}
