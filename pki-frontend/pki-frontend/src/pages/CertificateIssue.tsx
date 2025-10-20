import React, { useState, useEffect } from 'react';
import { Certificate } from '../models/certificate';
import certificatesApi from '../api/certificates/certificatesApi';
import caApi from '../api/certificates/caApi';
import { generateCSR } from '../services/CSRService';

interface CSRFormContentI {
  commonName: string;
  organization: string;
  email?: string | null;
}

const CertificateIssue: React.FC = () => {
  const [availableCAs, setAvailableCAs] = useState<Certificate[]>([]);
  const [selectedCAId, setSelectedCAId] = useState<number | null>(null);
  const [CSRFormContent, setCSRFromContent] = useState<CSRFormContentI>({
    commonName: '',
    organization: '',
    email: null,
  });
  const [csrContent, setCsrContent] = useState('');
  const [certificateType, setCertificateType] = useState<'intermediate' | 'end-entity'>('end-entity');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    loadCAs();
  }, []);
  const loadCAs = async () => {
    try {
      const cas = await caApi.getAllCAs();
      const activeCAs = cas.filter((ca) => ca.status === 'ACTIVE');
      setAvailableCAs(activeCAs);
      if (activeCAs.length > 0) setSelectedCAId(activeCAs[0].id);
    } catch (err) {
      setError('Error loading CAs');
      console.error('Error loading CAs:', err);
    }
  };

  /** ✅ Генерация CSR **/
  const handleGenerateCSR = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setLoading(true);
      setError(null);      if (!CSRFormContent.commonName || !CSRFormContent.organization) {
        setError('Please enter CN and organization.');
        return;
      }

      const { pem } = await generateCSR({
        commonName: CSRFormContent.commonName,
        organization: CSRFormContent.organization,
        email: CSRFormContent.email || '',
      });

      setCsrContent(pem);      setSuccess('CSR successfully generated!');
    } catch (err: any) {
      setError(`Error generating CSR: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  /** ✅ Выпуск сертификата **/  const handleIssueCertificate = async () => {
    if (!selectedCAId) {
      setError('Please select a CA');
      return;
    }
    if (!csrContent.trim()) {
      setError('Generate CSR first');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      setSuccess(null);

      if (certificateType === 'intermediate') {
        await certificatesApi.issueIntermediateCertificate({
          csrContent: csrContent.trim(),
          selectedCAId,
        });        setSuccess('Intermediate certificate successfully issued!');
        loadCAs();
      } else {
        await certificatesApi.issueEndEntityCertificate({
          csrContent: csrContent.trim(),
          selectedCAId,
        });
        setSuccess('End entity certificate successfully issued!');
      }

      setCsrContent('');
    } catch (err) {      setError(`Error: ${err instanceof Error ? err.message : 'Unknown error'}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: 32 }}>
      <h1>Certificate Issuance</h1>

      {/* Ошибки */}
      {error && (
        <div
          style={{
            backgroundColor: '#fee2e2',
            color: '#991b1b',
            padding: 16,
            borderRadius: 6,
            marginBottom: 24,
            border: '1px solid #fecaca',
          }}
        >
          {error}
        </div>
      )}

      {/* Успех */}
      {success && (
        <div
          style={{
            backgroundColor: '#d1fae5',
            color: '#065f46',
            padding: 16,
            borderRadius: 6,
            marginBottom: 24,
            border: '1px solid #a7f3d0',
          }}
        >
          {success}
        </div>
      )}

      {/* Форма выпуска */}
      <div
        style={{
          backgroundColor: 'white',
          padding: 24,
          borderRadius: 8,
          boxShadow: '0 2px 8px #e0e7ff',
        }}
      >
        <h3>Issue Certificate</h3>

        {/* Тип сертификата */}
        <div style={{ marginBottom: 20 }}>
          <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
            Choose certificate type:
          </label>
          <div style={{ display: 'flex', gap: 16 }}>
            <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
              <input
                type="radio"
                value="end-entity"
                checked={certificateType === 'end-entity'}
                onChange={(e) => setCertificateType(e.target.value as 'end-entity')}
                style={{ marginRight: 8 }}
              />
              <span>End-Entity Certificate</span>
            </label>

            <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
              <input
                type="radio"
                value="intermediate"
                checked={certificateType === 'intermediate'}
                onChange={(e) => setCertificateType(e.target.value as 'intermediate')}
                style={{ marginRight: 8 }}
              />
              <span>Intermediate Certificate</span>
            </label>
          </div>
        </div>

        {/* Список ЦА */}
        <div style={{ marginBottom: 20 }}>
          <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
            Selected issuer certificate:
          </label>          {availableCAs.length === 0 ? (
            <div style={{ color: '#6b7280' }}>No active CAs. Create a root CA first.</div>
          ) : (
            <select
              value={selectedCAId || ''}
              onChange={(e) => setSelectedCAId(parseInt(e.target.value))}
              style={{
                width: '100%',
                padding: 8,
                border: '1px solid #d1d5db',
                borderRadius: 4,
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

        {/* ✅ Форма ввода CSR */}
        <form onSubmit={handleGenerateCSR}>
          <div style={{ marginBottom: 20 }}>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
              Enter common name (CN)
            </label>
            <input
              value={CSRFormContent.commonName}
              onChange={(e) =>
                setCSRFromContent((prev) => ({ ...prev, commonName: e.target.value }))
              }
              style={{
                width: '100%',
                padding: 12,
                border: '1px solid #d1d5db',
                borderRadius: 4,
                fontFamily: 'monospace',
                fontSize: 14,
              }}
              name="C"
            />
          </div>

          <div style={{ marginBottom: 20 }}>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
              Enter organization (O)
            </label>
            <input
              value={CSRFormContent.organization}
              onChange={(e) =>
                setCSRFromContent((prev) => ({ ...prev, organization: e.target.value }))
              }
              style={{
                width: '100%',
                padding: 12,
                border: '1px solid #d1d5db',
                borderRadius: 4,
                fontFamily: 'monospace',
                fontSize: 14,
              }}
              name="O"
            />
          </div>

          {certificateType === 'end-entity' && (
            <div style={{ marginBottom: 20 }}>
              <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
                Enter email (E)
              </label>
              <input
                value={CSRFormContent.email || ''}
                onChange={(e) =>
                  setCSRFromContent((prev) => ({ ...prev, email: e.target.value }))
                }
                style={{
                  width: '100%',
                  padding: 12,
                  border: '1px solid #d1d5db',
                  borderRadius: 4,
                  fontFamily: 'monospace',
                  fontSize: 14,
                }}
                name="E"
              />
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            style={{
              padding: '12px 24px',
              backgroundColor: '#10b981',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              fontSize: 16,
              fontWeight: 600,
              cursor: loading ? 'not-allowed' : 'pointer',
            }}
          >
            {loading ? 'Generating...' : 'Generate CSR'}
          </button>
        </form>

        {/* ✅ Поле CSR */}
        <div style={{ marginTop: 20 }}>
          <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
            CSR (PEM format)
          </label>
          <textarea
            readOnly
            value={csrContent}
            placeholder="-----BEGIN CERTIFICATE REQUEST-----..."
            style={{
              width: '100%',
              minHeight: 200,
              padding: 12,
              border: '1px solid #d1d5db',
              borderRadius: 4,
              fontFamily: 'monospace',
              fontSize: 14,
              resize: 'vertical',
            }}
          />
        </div>

        {/* ✅ Кнопка выпуска */}
        <button
          onClick={handleIssueCertificate}
          disabled={loading || !selectedCAId || !csrContent.trim()}
          style={{
            marginTop: 20,
            padding: '12px 24px',
            backgroundColor:
              loading || !csrContent.trim()
                ? '#9ca3af'
                : certificateType === 'intermediate'
                ? '#10b981'
                : '#3b82f6',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            cursor:
              loading || !selectedCAId || !csrContent.trim() ? 'not-allowed' : 'pointer',
            fontSize: 16,
            fontWeight: 600,
          }}
        >          {loading
            ? 'Issuing...'
            : `Issue ${
                certificateType === 'intermediate' ? 'intermediate' : 'end-entity'
              } certificate`}
        </button>
      </div>
    </div>
  );
};

export default CertificateIssue;
