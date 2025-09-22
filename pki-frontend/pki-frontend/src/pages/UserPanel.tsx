import React, { useState } from 'react';
import CertificateTable from '../components/CertificateTable';
import { Certificate } from '../models/certificate';

const mockCertificates: Certificate[] = [
  {
    id: '1',
    subject: 'CN=Иванов Иван',
    issuedTo: 'Иванов Иван',
    issuedBy: 'PKI CA',
    validFrom: '2025-01-01',
    validTo: '2026-01-01',
    status: 'valid',
    serialNumber: '1234567890',
    templateName: 'Стандартный',
    downloadUrl: '/certs/1.crt',
  },
  {
    id: '2',
    subject: 'CN=Петров Петр',
    issuedTo: 'Петров Петр',
    issuedBy: 'PKI CA',
    validFrom: '2024-01-01',
    validTo: '2025-01-01',
    status: 'revoked',
    serialNumber: '9876543210',
    templateName: 'VPN',
  },
];

const UserPanel: React.FC = () => {
  const [certificates, setCertificates] = useState<Certificate[]>(mockCertificates);

  const handleDownload = (cert: Certificate) => {
    // TODO: реализовать скачивание
    alert(`Скачать сертификат: ${cert.serialNumber}`);
  };

  const handleRevoke = (cert: Certificate) => {
    // TODO: реализовать отзыв через API
    setCertificates(list => list.map(c => c.id === cert.id ? { ...c, status: 'revoked' } : c));
  };

  return (
    <div style={{ padding: 32 }}>
      <h1>Мои сертификаты</h1>
      <CertificateTable certificates={certificates} onDownload={handleDownload} onRevoke={handleRevoke} />
    </div>
  );
};

export default UserPanel;
