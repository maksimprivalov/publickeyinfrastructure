import React, { useState, useEffect } from 'react';
import certificatesApi from '../api/certificates/certificatesApi';
import type { Certificate } from '../models/certificate';

interface SearchFilters {
  status?: 'VALID' | 'EXPIRED' | 'REVOKED';
  type?: 'ROOT' | 'INTERMEDIATE' | 'END_ENTITY';
  organization?: string;
}

interface SearchResults {
  content: Certificate[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

const CertificateSearch: React.FC = () => {
  const [filters, setFilters] = useState<SearchFilters>({});
  const [results, setResults] = useState<SearchResults | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const handleSearch = async (page: number = 0) => {
    try {
      setLoading(true);
      setError(null);
      
      const searchResults = await certificatesApi.searchCertificates({
        ...filters,
        page,
        size: pageSize
      });
      
      setResults(searchResults);
      setCurrentPage(page);
    } catch (err) {
      setError('Error searching certificates');
      console.error('Search error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (key: keyof SearchFilters, value: string) => {
    setFilters(prev => ({
      ...prev,
      [key]: value || undefined
    }));
  };

  const clearFilters = () => {
    setFilters({});
    setResults(null);
    setCurrentPage(0);
  };

  const downloadCertificate = async (id: number) => {
    try {
      const blob = await certificatesApi.downloadCertificate(id);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `certificate_${id}.p12`;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Download error:', err);
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
      case 'END_ENTITY': return '#06b6d4';
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

  return (
    <div>
      <div style={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        marginBottom: 24 
      }}>
        <h2 style={{ margin: 0, color: '#1f2937' }}>Certificate Search</h2>
      </div>

      {/* Search Filters */}
      <div style={{
        backgroundColor: 'white',
        padding: 24,
        borderRadius: 8,
        boxShadow: '0 2px 8px #e0e7ff',
        marginBottom: 24
      }}>
        <h3 style={{ margin: 0, marginBottom: 16, color: '#374151' }}>Search Filters</h3>
        
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: 16,
          marginBottom: 16
        }}>
          <div>
            <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500, color: '#374151' }}>
              Status
            </label>
            <select
              value={filters.status || ''}
              onChange={(e) => handleFilterChange('status', e.target.value)}
              style={{
                width: '100%',
                padding: '8px 12px',
                border: '1px solid #d1d5db',
                borderRadius: 6,
                fontSize: 14,
                backgroundColor: 'white'
              }}
            >
              <option value="">All Statuses</option>
              <option value="VALID">Valid</option>
              <option value="EXPIRED">Expired</option>
              <option value="REVOKED">Revoked</option>
            </select>
          </div>

          <div>
            <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500, color: '#374151' }}>
              Type
            </label>
            <select
              value={filters.type || ''}
              onChange={(e) => handleFilterChange('type', e.target.value)}
              style={{
                width: '100%',
                padding: '8px 12px',
                border: '1px solid #d1d5db',
                borderRadius: 6,
                fontSize: 14,
                backgroundColor: 'white'
              }}
            >
              <option value="">All Types</option>
              <option value="ROOT">Root</option>
              <option value="INTERMEDIATE">Intermediate</option>
              <option value="END_ENTITY">End Entity</option>
            </select>
          </div>

          <div>
            <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500, color: '#374151' }}>
              Organization
            </label>
            <input
              type="text"
              value={filters.organization || ''}
              onChange={(e) => handleFilterChange('organization', e.target.value)}
              placeholder="Enter organization name"
              style={{
                width: '100%',
                padding: '8px 12px',
                border: '1px solid #d1d5db',
                borderRadius: 6,
                fontSize: 14
              }}
            />
          </div>

          <div>
            <label style={{ display: 'block', marginBottom: 4, fontSize: 14, fontWeight: 500, color: '#374151' }}>
              Page Size
            </label>
            <select
              value={pageSize}
              onChange={(e) => setPageSize(Number(e.target.value))}
              style={{
                width: '100%',
                padding: '8px 12px',
                border: '1px solid #d1d5db',
                borderRadius: 6,
                fontSize: 14,
                backgroundColor: 'white'
              }}
            >
              <option value={5}>5 per page</option>
              <option value={10}>10 per page</option>
              <option value={25}>25 per page</option>
              <option value={50}>50 per page</option>
            </select>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 8 }}>
          <button
            onClick={() => handleSearch(0)}
            disabled={loading}
            style={{
              padding: '8px 16px',
              backgroundColor: '#2563eb',
              color: 'white',
              border: 'none',
              borderRadius: 6,
              cursor: loading ? 'not-allowed' : 'pointer',
              fontSize: 14,
              fontWeight: 500,
              opacity: loading ? 0.6 : 1
            }}
          >
            {loading ? 'Searching...' : 'Search'}
          </button>
          
          <button
            onClick={clearFilters}
            style={{
              padding: '8px 16px',
              backgroundColor: '#6b7280',
              color: 'white',
              border: 'none',
              borderRadius: 6,
              cursor: 'pointer',
              fontSize: 14,
              fontWeight: 500
            }}
          >
            Clear Filters
          </button>
        </div>
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

      {/* Results */}
      {results && (
        <div style={{
          backgroundColor: 'white',
          borderRadius: 8,
          boxShadow: '0 2px 8px #e0e7ff',
          overflow: 'hidden'
        }}>
          {/* Results Info */}
          <div style={{
            padding: 16,
            borderBottom: '1px solid #e5e7eb',
            backgroundColor: '#f9fafb'
          }}>
            <p style={{ margin: 0, fontSize: 14, color: '#6b7280' }}>
              Found {results.totalElements} certificates • Page {results.number + 1} of {results.totalPages}
            </p>
          </div>

          {/* Results Table */}
          {results.content.length === 0 ? (
            <div style={{ 
              textAlign: 'center', 
              padding: 40,
              color: '#6b7280'
            }}>
              <p>No certificates found matching your criteria</p>
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead style={{ backgroundColor: '#f1f5f9' }}>
                  <tr>
                    <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Serial Number</th>
                    <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Subject</th>
                    <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Type</th>
                    <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Status</th>
                    <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Valid Until</th>
                    <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {results.content.map((cert) => (
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
                      <td style={{ padding: 16, color: '#6b7280', fontSize: 14 }}>
                        {formatDate(cert.validTo)}
                      </td>
                      <td style={{ padding: 16 }}>
                        <button
                          onClick={() => downloadCertificate(cert.id)}
                          style={{
                            padding: '6px 12px',
                            fontSize: 12,
                            backgroundColor: '#10b981',
                            color: 'white',
                            border: 'none',
                            borderRadius: 4,
                            cursor: 'pointer',
                            fontWeight: 500
                          }}
                        >
                          Download
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination */}
          {results.totalPages > 1 && (
            <div style={{
              padding: 16,
              borderTop: '1px solid #e5e7eb',
              backgroundColor: '#f9fafb',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center'
            }}>
              <div style={{ fontSize: 14, color: '#6b7280' }}>
                Showing {results.number * results.size + 1} to {Math.min((results.number + 1) * results.size, results.totalElements)} of {results.totalElements} results
              </div>
              
              <div style={{ display: 'flex', gap: 4 }}>
                <button
                  onClick={() => handleSearch(currentPage - 1)}
                  disabled={currentPage === 0 || loading}
                  style={{
                    padding: '6px 12px',
                    fontSize: 12,
                    backgroundColor: currentPage === 0 ? '#e5e7eb' : '#374151',
                    color: currentPage === 0 ? '#9ca3af' : 'white',
                    border: 'none',
                    borderRadius: 4,
                    cursor: currentPage === 0 ? 'not-allowed' : 'pointer'
                  }}
                >
                  Previous
                </button>
                
                <span style={{
                  padding: '6px 12px',
                  fontSize: 12,
                  color: '#374151',
                  display: 'flex',
                  alignItems: 'center'
                }}>
                  Page {currentPage + 1} of {results.totalPages}
                </span>
                
                <button
                  onClick={() => handleSearch(currentPage + 1)}
                  disabled={currentPage >= results.totalPages - 1 || loading}
                  style={{
                    padding: '6px 12px',
                    fontSize: 12,
                    backgroundColor: currentPage >= results.totalPages - 1 ? '#e5e7eb' : '#374151',
                    color: currentPage >= results.totalPages - 1 ? '#9ca3af' : 'white',
                    border: 'none',
                    borderRadius: 4,
                    cursor: currentPage >= results.totalPages - 1 ? 'not-allowed' : 'pointer'
                  }}
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default CertificateSearch;
