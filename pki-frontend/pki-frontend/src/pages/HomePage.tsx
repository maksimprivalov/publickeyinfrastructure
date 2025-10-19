import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useUserRole } from '../context/UserContext';

const HomePage: React.FC = () => {
  const role = useUserRole();
  const [hasToken, setHasToken] = useState(!!localStorage.getItem('access_token'));
  
  // Listen for token changes to update authentication status
  useEffect(() => {
    const handleTokenChange = () => {
      setHasToken(!!localStorage.getItem('access_token'));
    };

    window.addEventListener('tokenChanged', handleTokenChange);
    window.addEventListener('storage', handleTokenChange);

    return () => {
      window.removeEventListener('tokenChanged', handleTokenChange);
      window.removeEventListener('storage', handleTokenChange);
    };
  }, []);

  // Only show authenticated content if we have both token AND valid role
  const isAuthenticated = hasToken && role;

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '60vh',
      textAlign: 'center',
      padding: '40px 20px'
    }}>
      {/* Header */}
      <div style={{
        marginBottom: 48
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 16,
          marginBottom: 24
        }}>
          <div style={{
            width: 64,
            height: 64,
            background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
            borderRadius: '16px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 32,
            color: '#fff',
            boxShadow: '0 4px 12px rgba(37, 99, 235, 0.3)'
          }}>
            🔐
          </div>
        </div>
        <h1 style={{
          fontSize: 48,
          fontWeight: 700,
          color: '#1f2937',
          margin: 0,
          marginBottom: 16,
          letterSpacing: '-0.025em'
        }}>
          PKI System
        </h1>
        <p style={{
          fontSize: 18,
          color: '#6b7280',
          margin: 0,
          maxWidth: 600,
          lineHeight: 1.6
        }}>
          Public Key Infrastructure Management System for secure certificate management, 
          digital signatures, and cryptographic operations.
        </p>
      </div>

      {/* Actions */}
      <div style={{
        display: 'flex',
        gap: 16,
        flexWrap: 'wrap',
        justifyContent: 'center'
      }}>
        {isAuthenticated ? (
          <>
            <Link
              to="/user"
              style={{
                display: 'inline-block',
                padding: '16px 32px',
                background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
                color: '#fff',
                textDecoration: 'none',
                borderRadius: 12,
                fontWeight: 600,
                fontSize: 16,
                boxShadow: '0 4px 12px rgba(37, 99, 235, 0.3)',
                transition: 'all 0.2s ease'
              }}
              onMouseOver={(e) => {
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = '0 6px 16px rgba(37, 99, 235, 0.4)';
              }}
              onMouseOut={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = '0 4px 12px rgba(37, 99, 235, 0.3)';
              }}
            >
              Go to Dashboard
            </Link>
            <Link
              to="/profile"
              style={{
                display: 'inline-block',
                padding: '16px 32px',
                background: '#fff',
                color: '#2563eb',
                textDecoration: 'none',
                borderRadius: 12,
                fontWeight: 600,
                fontSize: 16,
                border: '2px solid #e5e7eb',
                transition: 'all 0.2s ease'
              }}
              onMouseOver={(e) => {
                e.currentTarget.style.borderColor = '#2563eb';
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.1)';
              }}
              onMouseOut={(e) => {
                e.currentTarget.style.borderColor = '#e5e7eb';
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = 'none';
              }}
            >
              View Profile
            </Link>
          </>
        ) : (
          <>
            <Link
              to="/auth"
              style={{
                display: 'inline-block',
                padding: '16px 32px',
                background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
                color: '#fff',
                textDecoration: 'none',
                borderRadius: 12,
                fontWeight: 600,
                fontSize: 16,
                boxShadow: '0 4px 12px rgba(37, 99, 235, 0.3)',
                transition: 'all 0.2s ease'
              }}
              onMouseOver={(e) => {
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = '0 6px 16px rgba(37, 99, 235, 0.4)';
              }}
              onMouseOut={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = '0 4px 12px rgba(37, 99, 235, 0.3)';
              }}
            >
              Sign In
            </Link>
          </>
        )}
      </div>

      {/* Features */}
      <div style={{
        marginTop: 64,
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
        gap: 24,
        maxWidth: 900,
        width: '100%'
      }}>
        <div style={{
          padding: 24,
          background: '#fff',
          borderRadius: 12,
          border: '1px solid #e5e7eb',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
        }}>
          <div style={{
            width: 48,
            height: 48,
            background: '#fef3c7',
            borderRadius: 12,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 24,
            marginBottom: 16
          }}>
            🛡️
          </div>
          <h3 style={{
            fontSize: 18,
            fontWeight: 600,
            color: '#1f2937',
            margin: '0 0 8px 0'
          }}>
            Secure Certificates
          </h3>
          <p style={{
            fontSize: 14,
            color: '#6b7280',
            margin: 0,
            lineHeight: 1.5
          }}>
            Generate, manage, and distribute digital certificates with enterprise-grade security.
          </p>
        </div>

        <div style={{
          padding: 24,
          background: '#fff',
          borderRadius: 12,
          border: '1px solid #e5e7eb',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
        }}>
          <div style={{
            width: 48,
            height: 48,
            background: '#dbeafe',
            borderRadius: 12,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 24,
            marginBottom: 16
          }}>
            🔑
          </div>
          <h3 style={{
            fontSize: 18,
            fontWeight: 600,
            color: '#1f2937',
            margin: '0 0 8px 0'
          }}>
            Key Management
          </h3>
          <p style={{
            fontSize: 14,
            color: '#6b7280',
            margin: 0,
            lineHeight: 1.5
          }}>
            Advanced cryptographic key generation, storage, and lifecycle management.
          </p>
        </div>

        <div style={{
          padding: 24,
          background: '#fff',
          borderRadius: 12,
          border: '1px solid #e5e7eb',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
        }}>
          <div style={{
            width: 48,
            height: 48,
            background: '#f3e8ff',
            borderRadius: 12,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 24,
            marginBottom: 16
          }}>
            📋
          </div>
          <h3 style={{
            fontSize: 18,
            fontWeight: 600,
            color: '#1f2937',
            margin: '0 0 8px 0'
          }}>
            Certificate Authority
          </h3>
          <p style={{
            fontSize: 14,
            color: '#6b7280',
            margin: 0,
            lineHeight: 1.5
          }}>
            Full CA functionality with root and intermediate certificate management.
          </p>
        </div>
      </div>
    </div>
  );
};

export default HomePage;
