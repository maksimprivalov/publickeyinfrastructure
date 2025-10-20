import React, { useState, useEffect } from 'react';
import { Certificate } from '../models/certificate';
import caApi from '../api/certificates/caApi';

const CAManagement: React.FC = () => {
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
      setCas(allCAs);
    } catch (err) {      setError('Error loading certificate authorities');
      console.error('Error loading CAs:', err);
    } finally {
      setLoading(false);
    }
  };


  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ru-RU');
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'Активен';
      case 'REVOKED': return 'Отозван';
      case 'EXPIRED': return 'Истёк';
      default: return status;
    }
  };

  const getTypeText = (type: string) => {
    switch (type) {
      case 'ROOT_CA': return 'Корневой ЦА';
      case 'INTERMEDIATE_CA': return 'Промежуточный ЦА';
      default: return type;
    }
  };

  if (loading) {
    return (
      <div style={{ padding: 32, textAlign: 'center' }}>
        <h1>Active certificates</h1>
        <div>Loading...</div>
      </div>
    );
  }

  return (
    <div style={{ padding: 32 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 style={{ margin: 0 }}>Certificate management</h1>
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
            Refresh
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
        <div style={{ backgroundColor: 'white', borderRadius: 8, boxShadow: '0 2px 8px #e0e7ff' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead style={{ backgroundColor: '#f1f5f9' }}>
              <tr>
                <th style={{ padding: 12, textAlign: 'left' }}>Type</th>
                <th style={{ padding: 12, textAlign: 'left' }}>Subject</th>
                <th style={{ padding: 12, textAlign: 'left' }}>Serial number</th>
                <th style={{ padding: 12, textAlign: 'left' }}>Valid unitl</th>
                <th style={{ padding: 12, textAlign: 'left' }}>Status</th>
                <th style={{ padding: 12, textAlign: 'left' }}>Organization</th>
              </tr>
            </thead>
            <tbody>
              {cas.map((ca) => (
                <tr key={ca.id} style={{ borderBottom: '1px solid #e5e7eb' }}>
                  <td style={{ padding: 12 }}>{getTypeText(ca.type)}</td>
                  <td style={{ padding: 12 }}>{ca.subject}</td>
                  <td style={{ padding: 12, fontFamily: 'monospace' }}>{ca.serialNumber}</td>
                  <td style={{ padding: 12 }}>{formatDate(ca.validTo)}</td>
                  <td style={{ padding: 12 }}>
                    <span style={{
                      padding: '4px 8px',
                      borderRadius: '4px',
                      fontSize: '12px',
                      fontWeight: 'bold',
                      backgroundColor: ca.status === 'ACTIVE' ? '#dcfce7' : ca.status === 'REVOKED' ? '#fee2e2' : '#fef3c7',
                      color: ca.status === 'ACTIVE' ? '#166534' : ca.status === 'REVOKED' ? '#991b1b' : '#92400e'
                    }}>
                      {getStatusText(ca.status)}
                    </span>
                  </td>
                  <td style={{ padding: 12 }}>{ca.organization}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default CAManagement;
