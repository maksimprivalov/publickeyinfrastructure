import React from 'react';
import { Certificate } from '../models/certificate';

interface CertificateTableProps {
  certificates: Certificate[];
  onDownload?: (cert: Certificate) => void;
  onRevoke?: (cert: Certificate) => void;
  loading?: boolean;
}

const CertificateTable: React.FC<CertificateTableProps> = ({ certificates, onDownload, onRevoke, loading = false }) => {
  
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ru-RU');
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'Active';
      case 'REVOKED': return 'Revoked';
      case 'EXPIRED': return 'Expired';
      default: return status;
    }
  };

  const getTypeText = (type: string) => {
    switch (type) {
      case 'ROOT_CA': return 'Корневой ЦА';
      case 'INTERMEDIATE_CA': return 'Промежуточный ЦА';
      case 'END_ENTITY': return 'Конечный сертификат';
      default: return type;
    }
  };

  if (loading) {
    return (
      <div style={{ padding: 20, textAlign: 'center' }}>
        Downloading certificates...
      </div>
    );
  }

  if (certificates.length === 0) {
    return (
      <div style={{ padding: 20, textAlign: 'center', color: '#6b7280' }}>
        Сертификаты не найдены
      </div>
    );
  }

  return (
    <table style={{ width: '100%', borderCollapse: 'collapse', background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e7ff' }}>
      <thead style={{ background: '#f1f5f9' }}>
        <tr>
          <th style={{ padding: 12, textAlign: 'left' }}>Serial number</th>
          <th style={{ padding: 12, textAlign: 'left' }}>Subject</th>
          <th style={{ padding: 12, textAlign: 'left' }}>Type</th>
          <th style={{ padding: 12, textAlign: 'left' }}>Valid from</th>
          <th style={{ padding: 12, textAlign: 'left' }}>Valid until</th>
          <th style={{ padding: 12, textAlign: 'left' }}>Status</th>
          <th style={{ padding: 12, textAlign: 'left' }}>Actions</th>
        </tr>
      </thead>
      <tbody>
        {Array.isArray(certificates) ? certificates.map(cert => (
          <tr key={cert.id} style={{ borderBottom: '1px solid #e5e7eb' }}>
            <td style={{ padding: 12, fontFamily: 'monospace' }}>{cert.serialNumber}</td>
            <td style={{ padding: 12 }}>{cert.subject}</td>
            <td style={{ padding: 12 }}>{getTypeText(cert.type)}</td>
            <td style={{ padding: 12 }}>{formatDate(cert.validFrom)}</td>
            <td style={{ padding: 12 }}>{formatDate(cert.validTo)}</td>
            <td style={{ padding: 12 }}>
              <span style={{
                padding: '4px 8px',
                borderRadius: '4px',
                fontSize: '12px',
                fontWeight: 'bold',
                backgroundColor: cert.status === 'ACTIVE' ? '#dcfce7' : cert.status === 'REVOKED' ? '#fee2e2' : '#fef3c7',
                color: cert.status === 'ACTIVE' ? '#166534' : cert.status === 'REVOKED' ? '#991b1b' : '#92400e'
              }}>
                {getStatusText(cert.status)}
              </span>
            </td>
            <td style={{ padding: 12 }}>
              <button 
                style={{ 
                  marginRight: 8, 
                  padding: '4px 12px', 
                  fontSize: '12px',
                  backgroundColor: '#3b82f6',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }} 
                onClick={() => onDownload?.(cert)}
              >
                Download
              </button>
              {cert.status === 'ACTIVE' && (
                <button 
                  style={{ 
                    padding: '4px 12px',
                    fontSize: '12px',
                    backgroundColor: '#ef4444',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer'
                  }} 
                  onClick={() => onRevoke?.(cert)}                >
                  Revoke
                </button>
              )}
            </td>
          </tr>
        )) : (
          <tr>
            <td colSpan={6} style={{ padding: 20, textAlign: 'center', color: '#6b7280' }}>
              {certificates ? 'Invalid data format' : 'No certificates data'}
            </td>
          </tr>
        )}
      </tbody>
    </table>
  );
};

export default CertificateTable;
