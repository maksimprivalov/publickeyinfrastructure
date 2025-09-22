import React, { useState, useEffect } from 'react';
import { Certificate } from '../models/certificate';
import certificatesApi from '../api/certificates/certificatesApi';
import caApi from '../api/certificates/caApi';

const CertificateIssue: React.FC = () => {
  const [availableCAs, setAvailableCAs] = useState<Certificate[]>([]);
  const [selectedCAId, setSelectedCAId] = useState<number | null>(null);
  const [csrContent, setCsrContent] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    loadCAs();
  }, []);

  const loadCAs = async () => {
    try {
      const cas = await caApi.getAllCAs();
      const activeCAs = cas.filter(ca => ca.status === 'ACTIVE');
      setAvailableCAs(activeCAs);

      if (activeCAs.length > 0) {
        setSelectedCAId(activeCAs[0].id);
      }
    } catch (err) {
      setError('Ошибка при загрузке ЦА');
      console.error('Error loading CAs:', err);
    }
  };

  const handleIssueRoot = async () => {
    try {
      setLoading(true);
      setError(null);
      await certificatesApi.issueRootCertificate();
      setSuccess('Корневой сертификат успешно выпущен!');
      loadCAs(); // Обновляем список ЦА
    } catch (err) {
      setError(`Ошибка: ${err instanceof Error ? err.message : 'Неизвестная ошибка'}`);
    } finally {
      setLoading(false);
    }
  };

  const handleIssueEndEntity = async () => {
    if (!selectedCAId) {
      setError('Выберите ЦА');
      return;
    }
    if (!csrContent.trim()) {
      setError('Введите CSR');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      await certificatesApi.issueEndEntityCertificate(selectedCAId, {
        csrContent: csrContent.trim()
      });
      setSuccess('Конечный сертификат успешно выпущен!');
      setCsrContent('');
    } catch (err) {
      setError(`Ошибка: ${err instanceof Error ? err.message : 'Неизвестная ошибка'}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: 32 }}>
      <h1>Выпуск сертификатов</h1>

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

      {success && (
        <div style={{
          backgroundColor: '#d1fae5',
          color: '#065f46',
          padding: 16,
          borderRadius: 6,
          marginBottom: 24,
          border: '1px solid #a7f3d0'
        }}>
          {success}
        </div>
      )}

      {/* Корневой сертификат */}
      <div style={{ 
        backgroundColor: 'white', 
        padding: 24, 
        borderRadius: 8, 
        boxShadow: '0 2px 8px #e0e7ff',
        marginBottom: 24 
      }}>
        <h3>Корневой сертификат ЦА</h3>
        <button
          onClick={handleIssueRoot}
          disabled={loading}
          style={{
            padding: '12px 24px',
            backgroundColor: loading ? '#9ca3af' : '#10b981',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            cursor: loading ? 'not-allowed' : 'pointer'
          }}
        >
          {loading ? 'Создается...' : 'Создать корневой ЦА'}
        </button>
      </div>

      {/* Конечный сертификат */}
      <div style={{ 
        backgroundColor: 'white', 
        padding: 24, 
        borderRadius: 8, 
        boxShadow: '0 2px 8px #e0e7ff'
      }}>
        <h3>Конечный сертификат</h3>
        
        <div style={{ marginBottom: 20 }}>
          <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
            Выберите ЦА:
          </label>
          {availableCAs.length === 0 ? (
            <div style={{ color: '#6b7280' }}>
              Нет активных ЦА. Создайте корневой ЦА сначала.
            </div>
          ) : (
            <select
              value={selectedCAId || ''}
              onChange={(e) => setSelectedCAId(parseInt(e.target.value))}
              style={{
                width: '100%',
                padding: 8,
                border: '1px solid #d1d5db',
                borderRadius: 4
              }}
            >
              {availableCAs.map((ca) => (
                <option key={ca.id} value={ca.id}>
                  {ca.subject} (S/N: {ca.serialNumber})
                </option>
              ))}
            </select>
          )}
        </div>

        <div style={{ marginBottom: 20 }}>
          <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
            CSR (PEM формат):
          </label>
          <textarea
            value={csrContent}
            onChange={(e) => setCsrContent(e.target.value)}
            placeholder="-----BEGIN CERTIFICATE REQUEST-----
MIICXjCCAUYCAQAwGTEXMBUGA1UEAwwOZXhhbXBsZS5jb20...
-----END CERTIFICATE REQUEST-----"
            style={{
              width: '100%',
              minHeight: 200,
              padding: 12,
              border: '1px solid #d1d5db',
              borderRadius: 4,
              fontFamily: 'monospace',
              fontSize: 14,
              resize: 'vertical'
            }}
          />
        </div>

        <button
          onClick={handleIssueEndEntity}
          disabled={loading || !selectedCAId || !csrContent.trim()}
          style={{
            padding: '12px 24px',
            backgroundColor: loading ? '#9ca3af' : '#3b82f6',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            cursor: loading ? 'not-allowed' : 'pointer'
          }}
        >
          {loading ? 'Выпускается...' : 'Выпустить сертификат'}
        </button>
      </div>
    </div>
  );
};

export default CertificateIssue;
