import React, { useState, useEffect } from 'react';
import { RevokedCertificate } from '../models/revokedCertificate';
import revocationApi from '../api/certificates/revocationApi';
import caApi from '../api/certificates/caApi';

const Revocation: React.FC = () => {
  const [revokedCertificates, setRevokedCertificates] = useState<RevokedCertificate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [downloadingCRL, setDownloadingCRL] = useState<number | null>(null);

  useEffect(() => {
    loadRevokedCertificates();
  }, []);

  const loadRevokedCertificates = async () => {
    try {
      setLoading(true);
      setError(null);
      const revoked = await revocationApi.getRevokedCertificates();
      setRevokedCertificates(revoked);
    } catch (err) {
      setError('Ошибка при загрузке отозванных сертификатов');
      console.error('Error loading revoked certificates:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadCRL = async (issuerId: number) => {
    try {
      setDownloadingCRL(issuerId);
      setError(null);
      
      const blob = await revocationApi.downloadCRL(issuerId);
      
      // Создаем ссылку для скачивания CRL
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `crl_${issuerId}.crl`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(`Ошибка при скачивании CRL: ${err instanceof Error ? err.message : 'Неизвестная ошибка'}`);
      console.error('Error downloading CRL:', err);
    } finally {
      setDownloadingCRL(null);
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

  const getReasonText = (reason: string) => {
    const reasonMap: { [key: string]: string } = {
      'UNSPECIFIED': 'Не указана',
      'KEY_COMPROMISE': 'Компрометация ключа',
      'CA_COMPROMISE': 'Компрометация ЦА',
      'AFFILIATION_CHANGED': 'Изменение принадлежности',
      'SUPERSEDED': 'Заменен',
      'CESSATION_OF_OPERATION': 'Прекращение деятельности',
      'CERTIFICATE_HOLD': 'Приостановка сертификата',
      'REMOVE_FROM_CRL': 'Удаление из CRL',
      'PRIVILEGE_WITHDRAWN': 'Отзыв привилегий',
      'AA_COMPROMISE': 'Компрометация агента атрибутов'
    };
    return reasonMap[reason] || reason;
  };

  if (loading) {
    return (
      <div style={{ padding: 32, textAlign: 'center' }}>
        <h1>Отозванные сертификаты</h1>
        <div>Загрузка...</div>
      </div>
    );
  }

  return (
    <div style={{ padding: 32 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 style={{ margin: 0 }}>Отозванные сертификаты</h1>
        <button
          onClick={loadRevokedCertificates}
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

      {/* Секция для скачивания CRL */}
      <div style={{ 
        backgroundColor: 'white', 
        padding: 24, 
        borderRadius: 8, 
        boxShadow: '0 2px 8px #e0e7ff',
        marginBottom: 24 
      }}>
        <h3 style={{ marginTop: 0, marginBottom: 16 }}>Скачать списки отозванных сертификатов (CRL)</h3>
        <p style={{ color: '#6b7280', marginBottom: 16 }}>
          Введите ID центра сертификации для скачивания соответствующего CRL
        </p>
        <CRLDownloadForm onDownload={handleDownloadCRL} downloading={downloadingCRL} />
      </div>

      {revokedCertificates.length === 0 ? (
        <div style={{ 
          textAlign: 'center', 
          padding: 40,
          backgroundColor: '#f9fafb',
          borderRadius: 8,
          color: '#6b7280'
        }}>
          <p>Отозванных сертификатов не найдено</p>
        </div>
      ) : (
        <div style={{ backgroundColor: 'white', borderRadius: 8, boxShadow: '0 2px 8px #e0e7ff' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead style={{ backgroundColor: '#f1f5f9' }}>
              <tr>
                <th style={{ padding: 12, textAlign: 'left' }}>Серийный номер</th>
                <th style={{ padding: 12, textAlign: 'left' }}>Субъект</th>
                <th style={{ padding: 12, textAlign: 'left' }}>Дата отзыва</th>
                <th style={{ padding: 12, textAlign: 'left' }}>Причина</th>
                <th style={{ padding: 12, textAlign: 'left' }}>Отозван пользователем</th>
              </tr>
            </thead>
            <tbody>
              {revokedCertificates.map((revoked) => (
                <tr key={revoked.id} style={{ borderBottom: '1px solid #e5e7eb' }}>
                  <td style={{ padding: 12, fontFamily: 'monospace' }}>
                    {revoked.certificate.serialNumber}
                  </td>
                  <td style={{ padding: 12 }}>{revoked.certificate.subject}</td>
                  <td style={{ padding: 12 }}>{formatDate(revoked.revocationDate)}</td>
                  <td style={{ padding: 12 }}>
                    <span style={{
                      padding: '4px 8px',
                      borderRadius: '4px',
                      fontSize: '12px',
                      fontWeight: 'bold',
                      backgroundColor: '#fee2e2',
                      color: '#991b1b'
                    }}>
                      {getReasonText(revoked.reason)}
                    </span>
                  </td>
                  <td style={{ padding: 12 }}>
                    {revoked.revokedBy.name} {revoked.revokedBy.surname} ({revoked.revokedBy.email})
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

// Компонент для скачивания CRL
const CRLDownloadForm: React.FC<{
  onDownload: (issuerId: number) => void;
  downloading: number | null;
}> = ({ onDownload, downloading }) => {
  const [issuerId, setIssuerId] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const id = parseInt(issuerId);
    if (isNaN(id) || id <= 0) {
      alert('Введите корректный ID центра сертификации');
      return;
    }
    onDownload(id);
  };

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', gap: 8, alignItems: 'end' }}>
      <div>
        <label style={{ display: 'block', marginBottom: 4, fontWeight: 'bold', fontSize: 14 }}>
          ID центра сертификации
        </label>
        <input
          type="number"
          value={issuerId}
          onChange={(e) => setIssuerId(e.target.value)}
          placeholder="Например: 1"
          style={{
            padding: 8,
            border: '1px solid #d1d5db',
            borderRadius: 4,
            width: 200
          }}
          required
        />
      </div>
      <button
        type="submit"
        disabled={downloading !== null}
        style={{
          padding: '8px 16px',
          backgroundColor: downloading !== null ? '#9ca3af' : '#3b82f6',
          color: 'white',
          border: 'none',
          borderRadius: '6px',
          cursor: downloading !== null ? 'not-allowed' : 'pointer'
        }}
      >
        {downloading !== null ? 'Скачивается...' : 'Скачать CRL'}
      </button>
    </form>
  );
};

export default Revocation;
