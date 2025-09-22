import React, { useState, useEffect } from 'react';
import CertificateTable from '../components/CertificateTable';
import { Certificate } from '../models/certificate';
import certificatesApi from '../api/certificates/certificatesApi';
import revocationApi from '../api/certificates/revocationApi';

const UserPanel: React.FC = () => {
  const [certificates, setCertificates] = useState<Certificate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Загрузка сертификатов при монтировании компонента
  useEffect(() => {
    loadCertificates();
  }, []);

  const loadCertificates = async () => {
    try {
      setLoading(true);
      setError(null);
      const certs = await certificatesApi.getAllCertificates();
      setCertificates(certs);
    } catch (err) {
      setError('Ошибка при загрузке сертификатов');
      console.error('Error loading certificates:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async (cert: Certificate) => {
    try {
      setError(null);
      const blob = await certificatesApi.downloadCertificate(cert.id);
      
      // Создаем ссылку для скачивания
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `certificate_${cert.serialNumber}.p12`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(`Ошибка при скачивании сертификата: ${err instanceof Error ? err.message : 'Неизвестная ошибка'}`);
      console.error('Error downloading certificate:', err);
    }
  };

  const handleRevoke = async (cert: Certificate) => {
    const reason = prompt('Укажите причину отзыва сертификата:');
    if (!reason) return;

    try {
      setError(null);
      await revocationApi.revokeCertificate(cert.id, reason);
      
      // Обновляем статус сертификата в локальном состоянии
      setCertificates(list => 
        list.map(c => 
          c.id === cert.id 
            ? { ...c, status: 'REVOKED' as const }
            : c
        )
      );

      alert('Сертификат успешно отозван');
    } catch (err) {
      setError(`Ошибка при отзыве сертификата: ${err instanceof Error ? err.message : 'Неизвестная ошибка'}`);
      console.error('Error revoking certificate:', err);
    }
  };

  return (
    <div style={{ padding: 32 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 style={{ margin: 0 }}>Мои сертификаты</h1>
        <button
          onClick={loadCertificates}
          style={{
            padding: '8px 16px',
            backgroundColor: '#10b981',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer'
          }}
        >
          Обновить
        </button>
      </div>

      {error && (
        <div style={{
          backgroundColor: '#fee2e2',
          color: '#991b1b',
          padding: 16,
          borderRadius: 6,
          marginBottom: 24,
          border: '1px solid #fecaca'
        }}>
          {error}
        </div>
      )}

      <CertificateTable 
        certificates={certificates} 
        onDownload={handleDownload} 
        onRevoke={handleRevoke}
        loading={loading}
      />
    </div>
  );
};

export default UserPanel;
