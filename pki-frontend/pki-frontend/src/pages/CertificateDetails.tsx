import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Certificate } from '../models/certificate';
import certificatesApi from '../api/certificates/certificatesApi';

const CertificateDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const [certificate, setCertificate] = useState<Certificate | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      loadCertificate(parseInt(id));
    } else {
      setLoading(false);
      setError('ID сертификата не указан');
    }
  }, [id]);

  const loadCertificate = async (certId: number) => {
    try {
      setLoading(true);
      const cert = await certificatesApi.getCertificateById(certId);
      setCertificate(cert);
    } catch (err) {
      setError('Error loading certificate');
    } finally {
      setLoading(false);
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

  if (loading) {
    return (      <div style={{ padding: 32, textAlign: 'center' }}>
        <h1>Certificate Details</h1>
        <div>Loading...</div>
      </div>
    );
  }

  if (!certificate) {
    return (      <div style={{ padding: 32 }}>
        <h1>Certificate not found</h1>
        <button onClick={() => navigate(-1)}>Back</button>
      </div>
    );
  }

  return (
    <div style={{ padding: 32 }}>      
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 24 }}>
        <h1>Certificate Details</h1>
        <button style={{ backgroundColor: '#3b82f6', color: 'white' }} onClick={() => navigate(-1)}>Back</button>
      </div>

      {error && (
        <div style={{
          backgroundColor: '#fee2e2',
          color: '#991b1b',
          padding: 16,
          borderRadius: 6,
          marginBottom: 24
        }}>
          {error}
        </div>
      )}

      <div style={{ backgroundColor: 'white', padding: 24, borderRadius: 8 }}>
        <div style={{ marginBottom: 20 }}>
          <h2>{certificate.subject}</h2>
          <div style={{ display: 'flex', gap: 16 }}>            <span>Status: {certificate.status}</span>
            <span>Type: {certificate.type}</span>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
          <div>
            <h3>Basic Information</h3>
            <p><strong>S/N:</strong> {certificate.serialNumber}</p>            <p><strong>Subject:</strong> {certificate.subject}</p>
            <p><strong>Issuer:</strong> {certificate.issuer}</p>
            <p><strong>Organization:</strong> {certificate.organization}</p>
          </div>

          <div>            <h3>Validity Period</h3>
            <p><strong>From:</strong> {formatDate(certificate.validFrom)}</p>
            <p><strong>To:</strong> {formatDate(certificate.validTo)}</p>
            <p><strong>Created:</strong> {formatDate(certificate.createdAt)}</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CertificateDetails;
