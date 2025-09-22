import React, { useState, useEffect } from 'react';
import { Certificate } from '../models/certificate';
import certificatesApi from '../api/certificates/certificatesApi';
import caApi from '../api/certificates/caApi';

const CertificateUpload: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [selectedCAId, setSelectedCAId] = useState<number | null>(null);
  const [availableCAs, setAvailableCAs] = useState<Certificate[]>([]);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [issuedCertificate, setIssuedCertificate] = useState<Certificate | null>(null);

  useEffect(() => {
    loadAvailableCAs();
  }, []);

  const loadAvailableCAs = async () => {
    try {
      const cas = await caApi.getAllCAs();
      // Фильтруем только активные ЦА
      const activeCAs = cas.filter(ca => ca.status === 'ACTIVE');
      setAvailableCAs(activeCAs);
      
      if (activeCAs.length > 0) {
        setSelectedCAId(activeCAs[0].id);
      }
    } catch (err) {
      setError('Ошибка при загрузке списка центров сертификации');
      console.error('Error loading CAs:', err);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      // Проверяем тип файла
      if (!file.name.toLowerCase().endsWith('.csr') && !file.name.toLowerCase().endsWith('.pem')) {
        setError('Пожалуйста, выберите файл с расширением .csr или .pem');
        return;
      }
      setSelectedFile(file);
      setError(null);
      setSuccess(null);
      setIssuedCertificate(null);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Пожалуйста, выберите файл CSR для загрузки');
      return;
    }

    if (!selectedCAId) {
      setError('Пожалуйста, выберите центр сертификации');
      return;
    }

    try {
      setUploading(true);
      setError(null);
      setSuccess(null);

      const certificate = await certificatesApi.uploadCSR(selectedCAId, selectedFile);
      
      setSuccess('CSR успешно обработан и сертификат выпущен!');
      setIssuedCertificate(certificate);
      setSelectedFile(null);
      
      // Сбрасываем input файла
      const fileInput = document.getElementById('csr-file-input') as HTMLInputElement;
      if (fileInput) {
        fileInput.value = '';
      }
    } catch (err) {
      setError(`Ошибка при загрузке CSR: ${err instanceof Error ? err.message : 'Неизвестная ошибка'}`);
      console.error('Error uploading CSR:', err);
    } finally {
      setUploading(false);
    }
  };

  const handleDownloadIssuedCertificate = async () => {
    if (!issuedCertificate) return;

    try {
      const blob = await certificatesApi.downloadCertificate(issuedCertificate.id);
      
      // Создаем ссылку для скачивания
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `certificate_${issuedCertificate.serialNumber}.p12`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(`Ошибка при скачивании сертификата: ${err instanceof Error ? err.message : 'Неизвестная ошибка'}`);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ru-RU');
  };

  return (
    <div style={{ padding: 32 }}>
      <h1 style={{ marginBottom: 24 }}>Загрузка запроса на подпись сертификата (CSR)</h1>

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

      <div style={{ 
        backgroundColor: 'white', 
        padding: 24, 
        borderRadius: 8, 
        boxShadow: '0 2px 8px #e0e7ff',
        marginBottom: 24 
      }}>
        <h3 style={{ marginTop: 0, marginBottom: 16 }}>Выберите файл CSR и центр сертификации</h3>

        <div style={{ marginBottom: 20 }}>
          <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
            Центр сертификации
          </label>
          {availableCAs.length === 0 ? (
            <div style={{ 
              padding: 12, 
              backgroundColor: '#fef3c7', 
              color: '#92400e', 
              borderRadius: 4,
              border: '1px solid #f59e0b'
            }}>
              Активные центры сертификации не найдены. Создайте ЦА перед загрузкой CSR.
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
            Файл CSR (.csr, .pem)
          </label>
          <input
            id="csr-file-input"
            type="file"
            accept=".csr,.pem"
            onChange={handleFileSelect}
            style={{
              width: '100%',
              padding: 8,
              border: '1px solid #d1d5db',
              borderRadius: 4
            }}
          />
          {selectedFile && (
            <div style={{ 
              marginTop: 8, 
              padding: 8, 
              backgroundColor: '#f3f4f6', 
              borderRadius: 4,
              fontSize: 14 
            }}>
              <strong>Выбранный файл:</strong> {selectedFile.name} ({Math.round(selectedFile.size / 1024)} KB)
            </div>
          )}
        </div>

        <button
          onClick={handleUpload}
          disabled={uploading || !selectedFile || availableCAs.length === 0}
          style={{
            padding: '12px 24px',
            backgroundColor: (uploading || !selectedFile || availableCAs.length === 0) ? '#9ca3af' : '#10b981',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            cursor: (uploading || !selectedFile || availableCAs.length === 0) ? 'not-allowed' : 'pointer',
            fontSize: 16,
            fontWeight: 'bold'
          }}
        >
          {uploading ? 'Загружается...' : 'Загрузить CSR'}
        </button>
      </div>

      {/* Информация о выпущенном сертификате */}
      {issuedCertificate && (
        <div style={{ 
          backgroundColor: 'white', 
          padding: 24, 
          borderRadius: 8, 
          boxShadow: '0 2px 8px #e0e7ff'
        }}>
          <h3 style={{ marginTop: 0, marginBottom: 16, color: '#059669' }}>
            ✅ Сертификат успешно выпущен
          </h3>
          
          <div style={{ 
            display: 'grid', 
            gridTemplateColumns: '1fr 1fr', 
            gap: 16,
            marginBottom: 20 
          }}>
            <div>
              <strong>Серийный номер:</strong><br />
              <span style={{ fontFamily: 'monospace' }}>{issuedCertificate.serialNumber}</span>
            </div>
            <div>
              <strong>Тип:</strong><br />
              {issuedCertificate.type === 'END_ENTITY' ? 'Конечный сертификат' : issuedCertificate.type}
            </div>
            <div>
              <strong>Субъект:</strong><br />
              {issuedCertificate.subject}
            </div>
            <div>
              <strong>Статус:</strong><br />
              <span style={{
                padding: '2px 6px',
                borderRadius: '4px',
                fontSize: '12px',
                fontWeight: 'bold',
                backgroundColor: '#dcfce7',
                color: '#166534'
              }}>
                Активен
              </span>
            </div>
            <div>
              <strong>Действителен с:</strong><br />
              {formatDate(issuedCertificate.validFrom)}
            </div>
            <div>
              <strong>Действителен до:</strong><br />
              {formatDate(issuedCertificate.validTo)}
            </div>
          </div>

          <button
            onClick={handleDownloadIssuedCertificate}
            style={{
              padding: '8px 16px',
              backgroundColor: '#3b82f6',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer'
            }}
          >
            📥 Скачать сертификат (PKCS#12)
          </button>
        </div>
      )}

      {/* Инструкции */}
      <div style={{ 
        backgroundColor: '#f9fafb', 
        padding: 20, 
        borderRadius: 8,
        marginTop: 24
      }}>
        <h4 style={{ marginTop: 0 }}>📋 Инструкции:</h4>
        <ul style={{ paddingLeft: 20, color: '#6b7280' }}>
          <li>Выберите активный центр сертификации из списка</li>
          <li>Загрузите файл CSR в формате .csr или .pem</li>
          <li>Нажмите "Загрузить CSR" для обработки запроса</li>
          <li>После успешного выпуска сертификата вы сможете его скачать</li>
          <li>Скачанный сертификат будет в формате PKCS#12 (.p12) с паролем "changeit"</li>
        </ul>
      </div>
    </div>
  );
};

export default CertificateUpload;
