import React, { useState, useEffect } from 'react';
import { Certificate } from '../models/certificate';
import caApi from '../api/certificates/caApi';

interface TemplateFormProps {
  onSubmit: (templateData: {
    name: string;
    caIssuerId: number;
    cnRegex?: string;
    sanRegex?: string;
    maxTtlDays?: number;
    defaultKeyUsage?: string;
    defaultExtendedKeyUsage?: string;
  }) => void;
  onCancel: () => void;
}

const TemplateForm: React.FC<TemplateFormProps> = ({ onSubmit, onCancel }) => {
  const [name, setName] = useState('');
  const [caIssuerId, setCaIssuerId] = useState<number | null>(null);
  const [cnRegex, setCnRegex] = useState('');
  const [sanRegex, setSanRegex] = useState('');
  const [maxTtlDays, setMaxTtlDays] = useState<number | ''>('');
  const [defaultKeyUsage, setDefaultKeyUsage] = useState('digitalSignature, keyEncipherment');
  const [defaultExtendedKeyUsage, setDefaultExtendedKeyUsage] = useState('clientAuth');
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
        setCaIssuerId(activeCAs[0].id);
      }
    } catch (error) {
      console.error('Error loading CAs:', error);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!caIssuerId) return;

    setLoading(true);

    onSubmit({
      name: name.trim(),
      caIssuerId,
      cnRegex: cnRegex.trim() || undefined,
      sanRegex: sanRegex.trim() || undefined,
      maxTtlDays: maxTtlDays ? Number(maxTtlDays) : undefined,
      defaultKeyUsage: defaultKeyUsage.trim() || undefined,
      defaultExtendedKeyUsage: defaultExtendedKeyUsage.trim() || undefined,
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

  const predefinedTemplates = [
    {
      name: 'Стандартный пользователь',
      cnRegex: '^[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9]$',
      sanRegex: '^[a-zA-Z0-9][a-zA-Z0-9\\-\\.]*[a-zA-Z0-9]$',
      maxTtlDays: 365,
      keyUsage: 'digitalSignature, keyEncipherment',
      extKeyUsage: 'clientAuth'
    },
    {
      name: 'Веб-сервер',
      cnRegex: '^[a-zA-Z0-9][a-zA-Z0-9\\-\\.]*[a-zA-Z0-9]$',
      sanRegex: '^[a-zA-Z0-9][a-zA-Z0-9\\-\\.]*[a-zA-Z0-9]$',
      maxTtlDays: 730,
      keyUsage: 'digitalSignature, keyEncipherment',
      extKeyUsage: 'serverAuth'
    },
    {
      name: 'VPN-клиент',
      cnRegex: '^[a-zA-Z0-9][a-zA-Z0-9\\-_]*[a-zA-Z0-9]$',
      sanRegex: '',
      maxTtlDays: 365,
      keyUsage: 'digitalSignature',
      extKeyUsage: 'clientAuth'
    }
  ];

  const applyTemplate = (template: typeof predefinedTemplates[0]) => {
    setName(template.name);
    setCnRegex(template.cnRegex);
    setSanRegex(template.sanRegex);
    setMaxTtlDays(template.maxTtlDays);
    setDefaultKeyUsage(template.keyUsage);
    setDefaultExtendedKeyUsage(template.extKeyUsage);
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
        Создать шаблон сертификата
      </h3>

      {/* Quick Templates */}
      <div style={{ marginBottom: 24 }}>
        <label style={labelStyle}>Быстрые шаблоны</label>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {predefinedTemplates.map((template) => (
            <button
              key={template.name}
              type="button"
              onClick={() => applyTemplate(template)}
              style={{
                padding: '8px 16px',
                fontSize: 12,
                backgroundColor: '#f3f4f6',
                color: '#374151',
                border: '1px solid #d1d5db',
                borderRadius: 6,
                cursor: 'pointer',
                transition: 'all 0.2s ease'
              }}
              onMouseOver={(e) => {
                e.currentTarget.style.backgroundColor = '#e5e7eb';
              }}
              onMouseOut={(e) => {
                e.currentTarget.style.backgroundColor = '#f3f4f6';
              }}
            >
              {template.name}
            </button>
          ))}
        </div>
      </div>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        {/* Template Name */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label style={labelStyle}>Название шаблона *</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Введите название шаблона"
            required
            style={inputStyle}
          />
        </div>

        {/* CA Issuer */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label style={labelStyle}>Центр сертификации *</label>
          <select
            value={caIssuerId || ''}
            onChange={(e) => setCaIssuerId(parseInt(e.target.value))}
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

        {/* Max TTL Days */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label style={labelStyle}>Максимальный срок действия (дни)</label>
          <input
            type="number"
            value={maxTtlDays}
            onChange={(e) => setMaxTtlDays(e.target.value ? parseInt(e.target.value) : '')}
            placeholder="365"
            min="1"
            max="7300"
            style={inputStyle}
          />
          <small style={{ color: '#6b7280', fontSize: 12 }}>
            Оставьте пустым для отсутствия ограничений
          </small>
        </div>

        {/* CN Regex */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label style={labelStyle}>Регулярное выражение для CN</label>
          <input
            type="text"
            value={cnRegex}
            onChange={(e) => setCnRegex(e.target.value)}
            placeholder="^[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9]$"
            style={inputStyle}
          />
          <small style={{ color: '#6b7280', fontSize: 12 }}>
            Паттерн для валидации Common Name
          </small>
        </div>

        {/* SAN Regex */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label style={labelStyle}>Регулярное выражение для SAN</label>
          <input
            type="text"
            value={sanRegex}
            onChange={(e) => setSanRegex(e.target.value)}
            placeholder="^[a-zA-Z0-9][a-zA-Z0-9\-\.]*[a-zA-Z0-9]$"
            style={inputStyle}
          />
          <small style={{ color: '#6b7280', fontSize: 12 }}>
            Паттерн для валидации Subject Alternative Names
          </small>
        </div>

        {/* Default Key Usage */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label style={labelStyle}>Использование ключа по умолчанию</label>
          <input
            type="text"
            value={defaultKeyUsage}
            onChange={(e) => setDefaultKeyUsage(e.target.value)}
            placeholder="digitalSignature, keyEncipherment"
            style={inputStyle}
          />
          <small style={{ color: '#6b7280', fontSize: 12 }}>
            Разделяйте значения запятыми
          </small>
        </div>

        {/* Default Extended Key Usage */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label style={labelStyle}>Расширенное использование ключа по умолчанию</label>
          <input
            type="text"
            value={defaultExtendedKeyUsage}
            onChange={(e) => setDefaultExtendedKeyUsage(e.target.value)}
            placeholder="clientAuth, serverAuth"
            style={inputStyle}
          />
          <small style={{ color: '#6b7280', fontSize: 12 }}>
            Разделяйте значения запятыми
          </small>
        </div>

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
            Отмена
          </button>
          
          <button
            type="submit"
            disabled={loading || !name.trim() || !caIssuerId}
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
            {loading ? 'Создание...' : 'Создать шаблон'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default TemplateForm;
