import React from 'react';
import { Certificate } from '../models/certificate';

interface CertificateTableProps {
  certificates: Certificate[];
  onDownload?: (cert: Certificate) => void;
  onRevoke?: (cert: Certificate) => void;
}

const CertificateTable: React.FC<CertificateTableProps> = ({ certificates, onDownload, onRevoke }) => {
  return (
    <table style={{ width: '100%', borderCollapse: 'collapse', background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e7ff' }}>
      <thead style={{ background: '#f1f5f9' }}>
        <tr>
          <th style={{ padding: 8 }}>Серийный номер</th>
          <th style={{ padding: 8 }}>Владелец</th>
          <th style={{ padding: 8 }}>Шаблон</th>
          <th style={{ padding: 8 }}>Действителен с</th>
          <th style={{ padding: 8 }}>Действителен до</th>
          <th style={{ padding: 8 }}>Статус</th>
          <th style={{ padding: 8 }}>Действия</th>
        </tr>
      </thead>
      <tbody>
        {certificates.map(cert => (
          <tr key={cert.id}>
            <td style={{ padding: 8 }}>{cert.serialNumber}</td>
            <td style={{ padding: 8 }}>{cert.issuedTo}</td>
            <td style={{ padding: 8 }}>{cert.templateName}</td>
            <td style={{ padding: 8 }}>{cert.validFrom}</td>
            <td style={{ padding: 8 }}>{cert.validTo}</td>
            <td style={{ padding: 8 }}>{cert.status}</td>
            <td style={{ padding: 8 }}>
              {cert.downloadUrl && (
                <button style={{ marginRight: 8 }} onClick={() => onDownload?.(cert)}>Скачать</button>
              )}
              {cert.status === 'valid' && (
                <button style={{ color: '#f87171' }} onClick={() => onRevoke?.(cert)}>Отозвать</button>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};

export default CertificateTable;
