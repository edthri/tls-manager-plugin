export async function fetchCertificates() {
  await new Promise((resolve) => setTimeout(resolve, 500))
  return [
    {
      alias: 'prod-web-1',
      subjectCn: 'www.example.com',
      issuerCn: 'Let\'s Encrypt Authority X3',
      validUntil: '2026-01-15',
    },
    {
      alias: 'staging-api',
      subjectCn: 'api.staging.example.com',
      issuerCn: 'R3',
      validUntil: '2025-12-01',
    },
    {
      alias: 'internal-vpn',
      subjectCn: 'vpn.corp.example',
      issuerCn: 'Corp Root CA',
      validUntil: '2027-05-30',
    },
  ]
}
