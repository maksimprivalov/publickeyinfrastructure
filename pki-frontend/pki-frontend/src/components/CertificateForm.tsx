import React, { useState, useEffect } from 'react';
import { Certificate, CertificateType } from '../models/certificate';
import caApi from '../api/certificates/caApi';

interface CertificateFormProps {
  onSubmit: (certificateData: {
    type: CertificateType;
    subject: string;
    validDays: number;
    keyUsage?: string;
    extendedKeyUsage?: string;
    sanDomains?: string[];
    issuerId?: number;
  }) => void;
  onCancel: () => void;
}

const CertificateForm: React.FC<CertificateFormProps> = ({ onSubmit, onCancel }) => {
  const [type, setType] = useState<CertificateType>('END_ENTITY');
  const [subject, setSubject] = useState('');
  const [validDays, setValidDays] = useState(365);
  const [keyUsage, setKeyUsage] = useState('digitalSignature, keyEncipherment');
  const [extendedKeyUsage, setExtendedKeyUsage] = useState('clientAuth');
  const [sanDomains, setSanDomains] = useState('');
  const [issuerId, setIssuerId] = useState<number | null>(null);
  const [availableCAs, setAvailableCAs] = useState<Certificate[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadCAs();
  }, []);

  const loadCAs = async () => {
    try {
      const cas = await caApi.getAllCAs();
      const activeCAs = cas.filter(ca => ca.status === 'ACTIVE');
      setAvailableCAs(activeCAs);
      if (activeCAs.length > 0) {
        setIssuerId(activeCAs[0].id);
      }
    } catch (error) {
      console.error('Error loading CAs:', error);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    const domains = sanDomains
      .split(',')
      .map(domain => domain.trim())
      .filter(domain => domain.length > 0);

    onSubmit({
      type,
      subject: subject.trim(),
      validDays,
      keyUsage: keyUsage.trim(),
      extendedKeyUsage: extendedKeyUsage.trim(),
      sanDomains: domains.length > 0 ? domains : undefined,
      issuerId: type !== 'ROOT_CA' ? issuerId || undefined : undefined
    });
  };

  const inputStyle = {
    width: '100%',
    padding: '12px 16px',
    borderRadius: 8,
    border: '1px solid #d1d5db',
    fontSize: 14,
    color: '#1f2937',
    outline: 'none',
    transition: 'border-color 0.2s, box-shadow 0.2s',
    boxSizing: 'border-box' as const
  };

  const labelStyle = {
    fontSize: 14,
    fontWeight: 600,
    color: '#374151',
    marginBottom: 6,
    display: 'block' as const
  };

  return (
    <div style={{
      backgroundColor: '#fff',
      borderRadius: 12,
      padding: 32,
      boxShadow: '0 4px 24px rgba(0,0,0,0.1)',
      border: '1px solid #e5e7eb',
      minWidth: 500,
      maxWidth: 700
    }}>
      <h3 style={{
        fontSize: 20,
        fontWeight: 700,
        color: '#1f2937',
        marginTop: 0,
        marginBottom: 24,
        textAlign: 'center'
      }}>
        Создать сертификат
      </h3>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        {/* Certificate Type */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label style={labelStyle}>Тип сертификата *</label>
          <select
            value={type}
            onChange={(e) => setType(e.target.value as CertificateType)}
            style={{
              ...inputStyle,
              cursor: 'pointer'
            }}
          >
            <option value="ROOT_CA">Root CA</option>
            <option value="INTERMEDIATE_CA">Intermediate CA</option>
            <option value="END_ENTITY">End-entity</option>
          </select>
        </div>

        {/* Issuer CA (only for non-root certificates) */}
        {type !== 'ROOT_CA' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <label style={labelStyle}>Центр сертификации *</label>
            <select
              value={issuerId || ''}
              onChange={(e) => setIssuerId(parseInt(e.target.value))}
              style={{
                ...inputStyle,
                cursor: 'pointer'
              }}
              required
            >
              <option value="">Выберите ЦА</option>
              {availableCAs.map((ca) => (
                <option key={ca.id} value={ca.id}>
                  {ca.subject} (S/N: {ca.serialNumber})
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Subject */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label style={labelStyle}>Субъект (Distinguished Name) *</label>
          <input
            type="text"
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            placeholder="CN=example.com,O=Organization,C=RU"
            required
            style={inputStyle}
          />
          <small style={{ color: '#6b7280', fontSize: 12 }}>
            Формат: CN=common_name,O=organization,C=country
          </small>
        </div>

        {/* Valid Days */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label style={labelStyle}>Срок действия (дни) *</label>
          <input
            type="number"
            value={validDays}
            onChange={(e) => setValidDays(parseInt(e.target.value))}
            min="1"
            max="7300"
            required
            style={inputStyle}
          />
        </div>

        {/* Key Usage */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label style={labelStyle}>Использование ключа</label>
          <input
            type="text"
            value={keyUsage}
            onChange={(e) => setKeyUsage(e.target.value)}
            placeholder="digitalSignature, keyEncipherment"
            style={inputStyle}
          />
          <small style={{ color: '#6b7280', fontSize: 12 }}>
            Разделяйте значения запятыми
          </small>
        </div>

        {/* Extended Key Usage */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label style={labelStyle}>Расширенное использование ключа</label>
          <input
            type="text"
            value={extendedKeyUsage}
            onChange={(e) => setExtendedKeyUsage(e.target.value)}
            placeholder="clientAuth, serverAuth"
            style={inputStyle}
          />
          <small style={{ color: '#6b7280', fontSize: 12 }}>
            Разделяйте значения запятыми
          </small>
        </div>

        {/* SAN Domains */}
        {type === 'END_ENTITY' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <label style={labelStyle}>Альтернативные имена субъекта (SAN)</label>
            <input
              type="text"
              value={sanDomains}
              onChange={(e) => setSanDomains(e.target.value)}
              placeholder="example.com, www.example.com"
              style={inputStyle}
            />
            <small style={{ color: '#6b7280', fontSize: 12 }}>
              Разделяйте домены запятыми
            </small>
          </div>
        )}

        {/* Buttons */}
        <div style={{ 
          display: 'flex', 
          gap: 12, 
          justifyContent: 'flex-end',
          marginTop: 24,
          paddingTop: 20,
          borderTop: '1px solid #e5e7eb'
        }}>
          <button
            type="button"
            onClick={onCancel}
            disabled={loading}
            style={{
              background: '#f3f4f6',
              color: '#374151',
              border: 'none',
              borderRadius: 8,
              padding: '12px 24px',
              fontSize: 14,
              fontWeight: 600,
              cursor: loading ? 'not-allowed' : 'pointer',
              transition: 'all 0.2s ease',
              outline: 'none',
              opacity: loading ? 0.6 : 1
            }}
          >
            Cancel
          </button>
          
          <button
            type="submit"
            disabled={loading || !subject.trim() || (type !== 'ROOT_CA' && !issuerId)}
            style={{
              background: loading ? '#9ca3af' : '#2563eb',
              color: '#fff',
              border: 'none',
              borderRadius: 8,
              padding: '12px 24px',
              fontSize: 14,
              fontWeight: 600,
              cursor: loading ? 'not-allowed' : 'pointer',
              transition: 'all 0.2s ease',
              boxShadow: '0 2px 8px rgba(37, 99, 235, 0.2)',
              outline: 'none'
            }}
          >
            {loading ? 'Создание...' : 'Создать сертификат'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default CertificateForm;
