import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import CertificateTable from '../components/CertificateTable';
import { Certificate } from '../models/certificate';
import certificatesApi from '../api/certificates/certificatesApi';
import revocationApi from '../api/certificates/revocationApi';
import { REVOCATION_REASONS } from '../models/revokedCertificate';

const UserPanel: React.FC = () => {
  const navigate = useNavigate();
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
        setCertificates(Array.isArray(certs) ? certs : []);
      } catch (err: any) {
        console.error('Error loading certificates:', err);
        setCertificates([]);
        
        // More specific error handling
        if (err.message === 'No refresh token available' || err.message === 'Token refresh failed') {
          setError('Session expired. Please log in again.');
        } else if (err.response?.status === 401) {
          setError('Authentication required. Please log in again.');
        } else if (err.response?.status === 403) {
          setError('You do not have permission to view certificates.');
        } else if (err.name === 'AxiosError' && !err.response) {
          setError('Unable to connect to server. Please check your connection and try again.');
        } else {
          setError('Error loading certificates. Please try again.');
        }
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
  let reason: string | null = null;

  // Показываем пользователю список допустимых причин
  while (true) {
    reason = prompt(
      'Укажите причину отзыва сертификата:\n' + REVOCATION_REASONS.join('\n')
    );
    if (reason === null) return; // пользователь отменил
    reason = reason.toUpperCase().trim();
    if (REVOCATION_REASONS.includes(reason as typeof REVOCATION_REASONS[number])) break;
    alert('Неверная причина. Выберите одну из перечисленных.');
  }

  try {
    setError(null);
    await revocationApi.revokeCertificate(cert.id, reason);

    alert('Сертификат успешно отозван');
    await loadCertificates()
  } catch (err) {
    setError(`Ошибка при отзыве сертификата: ${err instanceof Error ? err.message : 'Неизвестная ошибка'}`);
    console.error('Error revoking certificate:', err);
  }
};

  return (
    <div style={{ padding: 32 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 style={{ margin: 0, fontSize: 28, fontWeight: 700, color: '#1f2937' }}>My Certificates</h1>
        <button
          onClick={loadCertificates}
          style={{
            padding: '12px 20px',
            backgroundColor: '#10b981',
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            cursor: 'pointer',
            fontSize: 14,
            fontWeight: 500,
            transition: 'background-color 0.2s'
          }}
          onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#059669'}
          onMouseOut={(e) => e.currentTarget.style.backgroundColor = '#10b981'}
        >
          Refresh
        </button>
      </div>

      {error && (
        <div style={{
          padding: 20,
          marginBottom: 24,
          backgroundColor: '#fef2f2',
          border: '1px solid #fecaca',
          borderRadius: 12,
          color: '#dc2626',
          display: 'flex',
          flexDirection: 'column',
          gap: 12
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 18 }}>⚠️</span>
            <strong style={{ fontSize: 16 }}>Error Loading Certificates</strong>
          </div>
          <p style={{ margin: 0, fontSize: 14, lineHeight: 1.5 }}>{error}</p>
          <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
            <button
              onClick={loadCertificates}
              style={{
                padding: '8px 16px',
                backgroundColor: '#dc2626',
                color: 'white',
                border: 'none',
                borderRadius: 6,
                fontSize: 14,
                fontWeight: 500,
                cursor: 'pointer',
                transition: 'background-color 0.2s'
              }}
              onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#b91c1c'}
              onMouseOut={(e) => e.currentTarget.style.backgroundColor = '#dc2626'}
            >
              Retry
            </button>
            {error.includes('Authentication') && (
              <button
                onClick={() => navigate('/auth')}
                style={{
                  padding: '8px 16px',
                  backgroundColor: 'transparent',
                  color: '#dc2626',
                  border: '1px solid #dc2626',
                  borderRadius: 6,
                  fontSize: 14,
                  fontWeight: 500,
                  cursor: 'pointer',
                  transition: 'all 0.2s'
                }}
                onMouseOver={(e) => {
                  e.currentTarget.style.backgroundColor = '#dc2626';
                  e.currentTarget.style.color = 'white';
                }}
                onMouseOut={(e) => {
                  e.currentTarget.style.backgroundColor = 'transparent';
                  e.currentTarget.style.color = '#dc2626';
                }}
              >
                Sign In Again
              </button>
            )}
          </div>
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
