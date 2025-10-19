import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Certificate } from '../models/certificate';
import certificatesApi from '../api/certificates/certificatesApi';
import revocationApi from '../api/certificates/revocationApi';

const CertificateDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const [certificate, setCertificate] = useState<Certificate | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      loadCertificate(parseInt(id));
    } else {
      setLoading(false);
      setError('ID сертификата не указан');
    }
  }, [id]);

  const loadCertificate = async (certId: number) => {
    try {
      setLoading(true);
      const cert = await certificatesApi.getCertificateById(certId);
      setCertificate(cert);
    } catch (err) {
      setError('Ошибка при загрузке сертификата');
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async () => {
    if (!certificate) return;

    try {
      const blob = await certificatesApi.downloadCertificate(certificate.id);
      
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `certificate_${certificate.serialNumber}.p12`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError('Ошибка при скачивании');
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ru-RU', {
      year: 'numeric',
      month: '2-digit', 
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div style={{ padding: 32, textAlign: 'center' }}>
        <h1>Детали сертификата</h1>
        <div>Загрузка...</div>
      </div>
    );
  }

  if (!certificate) {
    return (
      <div style={{ padding: 32 }}>
        <h1>Сертификат не найден</h1>
        <button onClick={() => navigate(-1)}>Назад</button>
      </div>
    );
  }

  return (
    <div style={{ padding: 32 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 24 }}>
        <h1>Детали сертификата</h1>
        <button onClick={() => navigate(-1)}>Назад</button>
      </div>

      {error && (
        <div style={{
          backgroundColor: '#fee2e2',
          color: '#991b1b',
          padding: 16,
          borderRadius: 6,
          marginBottom: 24
        }}>
          {error}
        </div>
      )}

      <div style={{ backgroundColor: 'white', padding: 24, borderRadius: 8 }}>
        <div style={{ marginBottom: 20 }}>
          <h2>{certificate.subject}</h2>
          <div style={{ display: 'flex', gap: 16 }}>
            <span>Статус: {certificate.status}</span>
            <span>Тип: {certificate.type}</span>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
          <div>
            <h3>Основная информация</h3>
            <p><strong>S/N:</strong> {certificate.serialNumber}</p>
            <p><strong>Субъект:</strong> {certificate.subject}</p>
            <p><strong>Издатель:</strong> {certificate.issuer}</p>
            <p><strong>Организация:</strong> {certificate.organization}</p>
          </div>

          <div>
            <h3>Период действия</h3>
            <p><strong>С:</strong> {formatDate(certificate.validFrom)}</p>
            <p><strong>До:</strong> {formatDate(certificate.validTo)}</p>
            <p><strong>Создан:</strong> {formatDate(certificate.createdAt)}</p>
          </div>
        </div>

        <div style={{ marginTop: 20 }}>
          <button
            onClick={handleDownload}
            style={{
              padding: '8px 16px',
              backgroundColor: '#3b82f6',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer'
            }}
          >
            Скачать сертификат
          </button>
        </div>
      </div>
    </div>
  );
};

export default CertificateDetails;
