import React, { useState, useEffect } from 'react';
import { Certificate } from '../models/certificate';
import caApi from '../api/certificates/caApi';
import CertificateTable from '../components/CertificateTable';
import certificatesApi from '../api/certificates/certificatesApi';
import revocationApi from '../api/certificates/revocationApi';
import { REVOCATION_REASONS } from '../models/revokedCertificate';

const CAPanel: React.FC = () => {
  const [cas, setCas] = useState<Certificate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadCAs();
  }, []);

  const loadCAs = async () => {
    try {
      setLoading(true);
      setError(null);
      const allCAs = await caApi.getAllCAs();
      setCas(Array.isArray(allCAs) ? allCAs : []);
    } catch (err) {
      setCas([]);
      setError('Ошибка при загрузке центров сертификации');
      console.error('Error loading CAs:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async (cert: Certificate) => {
    try {
      setError(null);
      const blob = await certificatesApi.downloadCertificate(cert.id);
      
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `ca_certificate_${cert.serialNumber}.p12`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(`Ошибка при скачивании сертификата ЦА: ${err instanceof Error ? err.message : 'Неизвестная ошибка'}`);
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

    // Обновляем статус сертификата в локальном состоянии
    setCas(list =>
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
        <h1 style={{ margin: 0 }}>Панель центров сертификации</h1>
        <div>
          <button
            onClick={loadCAs}
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

      <div style={{ marginBottom: 24 }}>
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', 
          gap: 16 
        }}>
          <div style={{ 
            backgroundColor: 'white', 
            padding: 20, 
            borderRadius: 8, 
            boxShadow: '0 2px 8px #e0e7ff',
            textAlign: 'center'
          }}>
            <h3 style={{ margin: '0 0 8px 0', color: '#059669' }}>
              {cas.filter(ca => ca.status === 'ACTIVE').length}
            </h3>
            <p style={{ margin: 0, color: '#6b7280' }}>Активных ЦА</p>
          </div>
          
          <div style={{ 
            backgroundColor: 'white', 
            padding: 20, 
            borderRadius: 8, 
            boxShadow: '0 2px 8px #e0e7ff',
            textAlign: 'center'
          }}>
            <h3 style={{ margin: '0 0 8px 0', color: '#dc2626' }}>
              {cas.filter(ca => ca.status === 'REVOKED').length}
            </h3>
            <p style={{ margin: 0, color: '#6b7280' }}>Отозванных ЦА</p>
          </div>
          
          <div style={{ 
            backgroundColor: 'white', 
            padding: 20, 
            borderRadius: 8, 
            boxShadow: '0 2px 8px #e0e7ff',
            textAlign: 'center'
          }}>
            <h3 style={{ margin: '0 0 8px 0', color: '#3b82f6' }}>
              {cas.filter(ca => ca.type === 'ROOT_CA').length}
            </h3>
            <p style={{ margin: 0, color: '#6b7280' }}>Корневых ЦА</p>
          </div>
          
          <div style={{ 
            backgroundColor: 'white', 
            padding: 20, 
            borderRadius: 8, 
            boxShadow: '0 2px 8px #e0e7ff',
            textAlign: 'center'
          }}>
            <h3 style={{ margin: '0 0 8px 0', color: '#7c3aed' }}>
              {cas.filter(ca => ca.type === 'INTERMEDIATE_CA').length}
            </h3>
            <p style={{ margin: 0, color: '#6b7280' }}>Промежуточных ЦА</p>
          </div>
        </div>
      </div>

      {cas.length === 0 ? (
        <div style={{ 
          textAlign: 'center', 
          padding: 40,
          backgroundColor: '#f9fafb',
          borderRadius: 8,
          color: '#6b7280'
        }}>
          <p>Центры сертификации не найдены</p>
          <p>Создайте корневой ЦА для начала работы</p>
        </div>
      ) : (
        <CertificateTable 
          certificates={cas} 
          onDownload={handleDownload} 
          onRevoke={handleRevoke}
          loading={loading}
        />
      )}
    </div>
  );
};

export default CAPanel;
