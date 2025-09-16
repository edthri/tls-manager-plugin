export async function fetchCertificates() {
  // Simulate network delay
  await new Promise((resolve) => setTimeout(resolve, 400))

  // Mock data spanning all stores
  return [
    // Native Java Certificate Store (read-only)
    {
      alias: 'digicert-global-root',
      name: 'DigiCert Global Root CA',
      type: 'Root CA',
      subject: 'CN=DigiCert Global Root CA, OU=www.digicert.com, O=DigiCert Inc, C=US',
      issuer: 'Self-signed',
      validFrom: '2006-11-10',
      validTo: '2031-11-10',
      fingerprintSha1: 'A8985D3A65E5E5C4B2D7D66D40C6DD2FB19C5436',
      hasPrivateKey: false,
      store: 'native',
    },
    {
      alias: 'isrg-root-x1',
      name: 'ISRG Root X1',
      type: 'Root CA',
      subject: 'CN=ISRG Root X1, O=Internet Security Research Group, C=US',
      issuer: 'Self-signed',
      validFrom: '2015-06-04',
      validTo: '2035-06-04',
      fingerprintSha1: 'CABD2A79A1076A31F21D253635CB039D4329A5E8',
      hasPrivateKey: false,
      store: 'native',
    },

    // Additional Trusted Certificates (user-imported)
    {
      alias: 'corp-intermediate-1',
      name: 'Corp Intermediate CA 1',
      type: 'Intermediate',
      subject: 'CN=Corp Intermediate CA 1, O=Corp Example Ltd, C=US',
      issuer: 'Corp Root CA',
      validFrom: '2024-02-01',
      validTo: '2027-02-01',
      fingerprintSha1: '11223344556677889900AABBCCDDEEFF00112233',
      hasPrivateKey: false,
      store: 'trusted',
    },
    {
      alias: 'partner-public-cert',
      name: 'Partner Public Cert',
      type: 'End-entity',
      subject: 'CN=api.partner.example, O=Partner Inc, C=US',
      issuer: 'R3',
      validFrom: '2024-10-01',
      validTo: '2025-10-01',
      fingerprintSha1: '223344556677889900AABBCCDDEEFF0011223344',
      hasPrivateKey: false,
      store: 'trusted',
    },

    // Private Key Store
    {
      alias: 'prod-web-1',
      name: 'Production Web Cert',
      type: 'End-entity',
      subject: 'CN=www.example.com, O=Example Inc, C=US',
      issuer: "Let's Encrypt Authority X3",
      validFrom: '2024-01-10',
      validTo: '2026-01-15',
      fingerprintSha1: '3344556677889900AABBCCDDEEFF001122334455',
      hasPrivateKey: true,
      store: 'private',
    },
    {
      alias: 'staging-api',
      name: 'Staging API Cert',
      type: 'End-entity',
      subject: 'CN=api.staging.example.com, O=Example Inc, C=US',
      issuer: 'R3',
      validFrom: '2024-12-01',
      validTo: '2025-12-01',
      fingerprintSha1: '44556677889900AABBCCDDEEFF00112233445566',
      hasPrivateKey: true,
      store: 'private',
    },
  ]
}
