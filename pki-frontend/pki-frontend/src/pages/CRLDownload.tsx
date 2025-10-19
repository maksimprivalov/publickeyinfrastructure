import React, { useState, useEffect } from 'react';
import certificatesApi from '../api/certificates/certificatesApi';
import revocationApi from '../api/certificates/revocationApi';
import type { Certificate } from '../models/certificate';
import type { RevokedCertificate } from '../models/revokedCertificate';

const CRLDownload: React.FC = () => {
  const [certificates, setCertificates] = useState<Certificate[]>([]);
  const [revokedCertificates, setRevokedCertificates] = useState<RevokedCertificate[]>([]);
  const [loading, setLoading] = useState(true);
  const [downloadingCRL, setDownloadingCRL] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // Load all certificates (CA certificates that can issue CRLs)
      const [allCerts, revokedCerts] = await Promise.all([
        certificatesApi.getAllCertificates(),
        revocationApi.getRevokedCertificates()
      ]);
      
      // Filter to only CA certificates (ROOT and INTERMEDIATE)
      const caCertificates = allCerts.filter(cert => 
        cert.type === 'ROOT' || cert.type === 'INTERMEDIATE'
      );
      
      setCertificates(caCertificates);
      setRevokedCertificates(revokedCerts);
    } catch (err) {
      setError('Error loading certificates and revocation data');
      console.error('Error loading data:', err);
    } finally {
      setLoading(false);
    }
  };

  const downloadCRL = async (issuerId: number, issuerSubject: string) => {
    try {
      setDownloadingCRL(issuerId);
      
      const blob = await revocationApi.downloadCRL(issuerId);
      
      // Create download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `crl_${issuerId}.crl`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      console.log(`CRL downloaded for issuer: ${issuerSubject}`);
    } catch (err) {
      setError(`Error downloading CRL for issuer ${issuerId}`);
      console.error('CRL download error:', err);
    } finally {
      setDownloadingCRL(null);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'VALID': return '#10b981';
      case 'EXPIRED': return '#f59e0b';
      case 'REVOKED': return '#ef4444';
      default: return '#6b7280';
    }
  };

  const getTypeColor = (type: string) => {
    switch (type) {
      case 'ROOT': return '#8b5cf6';
      case 'INTERMEDIATE': return '#3b82f6';
      default: return '#6b7280';
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getRevokedCountForIssuer = (issuerId: number) => {
    return revokedCertificates.filter(revoked => 
      revoked.certificate.issuerId === issuerId
    ).length;
  };

  if (loading) {
    return (
      <div style={{ padding: 32, textAlign: 'center' }}>
        <h2>Certificate Revocation Lists (CRL)</h2>
        <div>Loading...</div>
      </div>
    );
  }

  return (
    <div>
      <div style={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        marginBottom: 24 
      }}>
        <h2 style={{ margin: 0, color: '#1f2937' }}>Certificate Revocation Lists (CRL)</h2>
        <button
          onClick={loadData}
          style={{
            padding: '8px 16px',
            backgroundColor: '#10b981',
            color: 'white',
            border: 'none',
            borderRadius: 6,
            cursor: 'pointer',
            fontSize: 14,
            fontWeight: 500
          }}
        >
          Refresh
        </button>
      </div>

      {/* Info Box */}
      <div style={{
        backgroundColor: '#eff6ff',
        border: '1px solid #bfdbfe',
        borderRadius: 8,
        padding: 16,
        marginBottom: 24
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          marginBottom: 8
        }}>
          <span style={{ fontSize: 20 }}>ℹ️</span>
          <h3 style={{ margin: 0, color: '#1e40af', fontSize: 16 }}>About Certificate Revocation Lists</h3>
        </div>
        <p style={{ margin: 0, color: '#1e40af', fontSize: 14, lineHeight: 1.5 }}>
          Certificate Revocation Lists (CRLs) contain the serial numbers of certificates that have been revoked 
          before their expiration date. Each CA maintains its own CRL. Download and distribute these files to 
          ensure applications can verify certificate validity status.
        </p>
      </div>

      {/* Error Display */}
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

      {/* Revocation Statistics */}
      <div style={{
        backgroundColor: 'white',
        padding: 24,
        borderRadius: 8,
        boxShadow: '0 2px 8px #e0e7ff',
        marginBottom: 24
      }}>
        <h3 style={{ margin: 0, marginBottom: 16, color: '#374151' }}>Revocation Statistics</h3>
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: 16
        }}>
          <div style={{
            backgroundColor: '#f0f9ff',
            padding: 16,
            borderRadius: 8,
            textAlign: 'center',
            border: '1px solid #e0f2fe'
          }}>
            <div style={{ fontSize: 24, fontWeight: 'bold', color: '#0369a1', marginBottom: 4 }}>
              {certificates.length}
            </div>
            <div style={{ fontSize: 14, color: '#6b7280' }}>Total CA Certificates</div>
          </div>
          
          <div style={{
            backgroundColor: '#fef2f2',
            padding: 16,
            borderRadius: 8,
            textAlign: 'center',
            border: '1px solid #fecaca'
          }}>
            <div style={{ fontSize: 24, fontWeight: 'bold', color: '#dc2626', marginBottom: 4 }}>
              {revokedCertificates.length}
            </div>
            <div style={{ fontSize: 14, color: '#6b7280' }}>Total Revoked Certificates</div>
          </div>
          
          <div style={{
            backgroundColor: '#f0fdf4',
            padding: 16,
            borderRadius: 8,
            textAlign: 'center',
            border: '1px solid #bbf7d0'
          }}>
            <div style={{ fontSize: 24, fontWeight: 'bold', color: '#16a34a', marginBottom: 4 }}>
              {certificates.filter(cert => cert.status === 'VALID').length}
            </div>
            <div style={{ fontSize: 14, color: '#6b7280' }}>Active CA Certificates</div>
          </div>
        </div>
      </div>

      {/* CA Certificates and CRL Downloads */}
      {certificates.length === 0 ? (
        <div style={{ 
          textAlign: 'center', 
          padding: 40,
          backgroundColor: '#f9fafb',
          borderRadius: 8,
          color: '#6b7280'
        }}>
          <p>No CA certificates found</p>
        </div>
      ) : (
        <div style={{ 
          backgroundColor: 'white', 
          borderRadius: 8, 
          boxShadow: '0 2px 8px #e0e7ff',
          overflow: 'hidden'
        }}>
          <div style={{
            padding: 16,
            borderBottom: '1px solid #e5e7eb',
            backgroundColor: '#f9fafb'
          }}>
            <h3 style={{ margin: 0, color: '#374151' }}>CA Certificates & CRL Downloads</h3>
          </div>

          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead style={{ backgroundColor: '#f1f5f9' }}>
                <tr>
                  <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Serial Number</th>
                  <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Subject</th>
                  <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Type</th>
                  <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Status</th>
                  <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Revoked Certs</th>
                  <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Valid Until</th>
                  <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {certificates.map((cert) => (
                  <tr key={cert.id} style={{ borderBottom: '1px solid #e5e7eb' }}>
                    <td style={{ padding: 16, fontFamily: 'monospace', fontSize: 12 }}>
                      {cert.serialNumber}
                    </td>
                    <td style={{ padding: 16, color: '#1f2937' }}>
                      <div style={{ maxWidth: 250, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {cert.subject}
                      </div>
                    </td>
                    <td style={{ padding: 16 }}>
                      <span style={{
                        padding: '4px 8px',
                        borderRadius: 4,
                        fontSize: 12,
                        fontWeight: 'bold',
                        backgroundColor: getTypeColor(cert.type) + '20',
                        color: getTypeColor(cert.type)
                      }}>
                        {cert.type}
                      </span>
                    </td>
                    <td style={{ padding: 16 }}>
                      <span style={{
                        padding: '4px 8px',
                        borderRadius: 4,
                        fontSize: 12,
                        fontWeight: 'bold',
                        backgroundColor: getStatusColor(cert.status) + '20',
                        color: getStatusColor(cert.status)
                      }}>
                        {cert.status}
                      </span>
                    </td>
                    <td style={{ padding: 16, textAlign: 'center' }}>
                      <span style={{
                        padding: '4px 8px',
                        borderRadius: 4,
                        fontSize: 12,
                        fontWeight: 'bold',
                        backgroundColor: '#fee2e2',
                        color: '#991b1b'
                      }}>
                        {getRevokedCountForIssuer(cert.id)}
                      </span>
                    </td>
                    <td style={{ padding: 16, color: '#6b7280', fontSize: 14 }}>
                      {formatDate(cert.validTo)}
                    </td>
                    <td style={{ padding: 16 }}>
                      <button
                        onClick={() => downloadCRL(cert.id, cert.subject)}
                        disabled={downloadingCRL === cert.id}
                        style={{
                          padding: '6px 12px',
                          fontSize: 12,
                          backgroundColor: downloadingCRL === cert.id ? '#d1d5db' : '#3b82f6',
                          color: 'white',
                          border: 'none',
                          borderRadius: 4,
                          cursor: downloadingCRL === cert.id ? 'not-allowed' : 'pointer',
                          fontWeight: 500,
                          opacity: downloadingCRL === cert.id ? 0.6 : 1
                        }}
                      >
                        {downloadingCRL === cert.id ? 'Downloading...' : 'Download CRL'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default CRLDownload;
