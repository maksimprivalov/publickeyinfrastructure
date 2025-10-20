import React, { useState } from 'react';
import certificatesApi from '../api/certificates/certificatesApi';
import type { Certificate, CertificateStatus, CertificateType } from '../models/certificate';
import CertificateTable from '../components/CertificateTable';

interface SearchResults {
  content: Certificate[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

const CertificateSearch: React.FC = () => {
  const [status, setStatus] = useState<CertificateStatus | ''>('');
  const [type, setType] = useState<CertificateType | ''>('');
  const [organization, setOrganization] = useState<string>('');
  const [results, setResults] = useState<SearchResults | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const handleSearch = async (page: number = 0) => {
    try {
      setLoading(true);
      setError(null);
      
      const searchResults = await certificatesApi.searchCertificates(
        status ? status as CertificateStatus : undefined,
        type ? type as CertificateType : undefined,
        organization || undefined,
        page,
        pageSize
      );
      
      setResults(searchResults);
      setCurrentPage(page);
    } catch (err) {
      setError('Error searching certificates');
      console.error('Search error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setStatus('');
    setType('');
    setOrganization('');
    setCurrentPage(0);
    setResults(null);
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    handleSearch(page);
  };

  const handleDownload = async (certificate: Certificate) => {
    try {
      const blob = await certificatesApi.downloadCertificate(certificate.id);
      
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `certificate_${certificate.serialNumber}.p12`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(`Error downloading certificate: ${err instanceof Error ? err.message : 'Unknown error'}`);
    }
  };

  const renderPagination = () => {
    if (!results) return null;
    
    const totalPages = results.totalPages;
    const pages = [];
    
    for (let i = 0; i < totalPages; i++) {
      pages.push(
        <button 
          key={i}
          onClick={() => handlePageChange(i)}
          style={{
            padding: '8px 12px',
            margin: '0 4px',
            backgroundColor: i === currentPage ? '#3b82f6' : '#f3f4f6',
            color: i === currentPage ? 'white' : '#374151',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          {i + 1}
        </button>
      );
    }
    
    return (
      <div style={{ display: 'flex', justifyContent: 'center', marginTop: 24 }}>
        {pages}
      </div>
    );
  };

  return (
    <div style={{ padding: 32 }}>
      <h1 style={{ marginBottom: 24 }}>Certificate Search</h1>
      
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
      
      <div style={{
        backgroundColor: 'white',
        padding: 24,
        borderRadius: 8,
        boxShadow: '0 2px 8px #e0e7ff',
        marginBottom: 24
      }}>
        <h3 style={{ marginTop: 0, marginBottom: 16 }}>Search Filters</h3>
        
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: 20 }}>
          {/* Status filter */}
          <div>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
              Certificate Status
            </label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as CertificateStatus | '')}
              style={{
                width: '100%',
                padding: 10,
                border: '1px solid #d1d5db',
                borderRadius: 4
              }}
            >
              <option value="">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="REVOKED">Revoked</option>
              <option value="EXPIRED">Expired</option>
            </select>
          </div>
          
          {/* Type filter */}
          <div>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
              Certificate Type
            </label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value as CertificateType | '')}
              style={{
                width: '100%',
                padding: 10,
                border: '1px solid #d1d5db',
                borderRadius: 4
              }}
            >
              <option value="">All Types</option>
              <option value="ROOT_CA">Root CA</option>
              <option value="INTERMEDIATE_CA">Intermediate CA</option>
              <option value="END_ENTITY">End Entity</option>
            </select>
          </div>
          
          {/* Organization filter */}
          <div>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
              Organization
            </label>
            <input
              type="text"
              value={organization}
              onChange={(e) => setOrganization(e.target.value)}
              placeholder="Enter organization name"
              style={{
                width: '100%',
                padding: 10,
                border: '1px solid #d1d5db',
                borderRadius: 4
              }}
            />
          </div>
          
          {/* Page size */}
          <div>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
              Results per page
            </label>
            <select
              value={pageSize}
              onChange={(e) => setPageSize(Number(e.target.value))}
              style={{
                width: '100%',
                padding: 10,
                border: '1px solid #d1d5db',
                borderRadius: 4
              }}
            >
              <option value={5}>5</option>
              <option value={10}>10</option>
              <option value={25}>25</option>
              <option value={50}>50</option>
            </select>
          </div>
        </div>
        
        <div style={{ marginTop: 24, display: 'flex', gap: 16 }}>
          <button
            onClick={() => handleSearch()}
            disabled={loading}
            style={{
              padding: '10px 20px',
              backgroundColor: loading ? '#9ca3af' : '#3b82f6',
              color: 'white',
              border: 'none',
              borderRadius: 6,
              cursor: loading ? 'not-allowed' : 'pointer',
              fontWeight: 'bold'
            }}
          >
            {loading ? 'Searching...' : 'Search Certificates'}
          </button>
          
          <button
            onClick={handleReset}
            style={{
              padding: '10px 20px',
              backgroundColor: '#f3f4f6',
              color: '#374151',
              border: '1px solid #d1d5db',
              borderRadius: 6,
              cursor: 'pointer'
            }}
          >
            Clear Filters
          </button>
        </div>
      </div>
      
      {/* Results */}
      {results && (
        <div>
          <div style={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center',
            marginBottom: 16 
          }}>
            <h3 style={{ margin: 0 }}>Search Results</h3>
            <p>Showing {results.content.length} of {results.totalElements} certificates</p>
          </div>
          
          {results.content.length === 0 ? (
            <div style={{
              textAlign: 'center',
              padding: 32,
              backgroundColor: '#f9fafb',
              borderRadius: 8,
              color: '#6b7280'
            }}>
              <p>No certificates found matching your search criteria</p>
            </div>
          ) : (
            <>
              <CertificateTable 
                certificates={results.content} 
                onDownload={handleDownload}
              />
              {renderPagination()}
            </>
          )}
        </div>
      )}
    </div>
  );
};

export default CertificateSearch;
