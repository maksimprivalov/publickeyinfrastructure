import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useUserRole, useUserId } from '../context/UserContext';
import userApi from '../api/user/userApi';

const UserMenu: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const navigate = useNavigate();
  const role = useUserRole();
  const userId = useUserId();
  const menuRef = useRef<HTMLDivElement>(null);

  // Get user email from token
  const getUserEmail = () => {
    const token = localStorage.getItem('access_token');
    if (token) {
      try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(
          atob(base64)
            .split('')
            .map(function (c) {
              return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            })
            .join('')
        );
        const decoded = JSON.parse(jsonPayload);
        return decoded.sub; // subject field usually contains email
      } catch {
        return 'Unknown User';
      }
    }
    return 'Unknown User';
  };

  // Close menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleLogout = async () => {
    try {
      setLoggingOut(true);
      
      // Call logout API
      await userApi.logout();
    } catch (error) {
      console.error('Logout error:', error);
      // Continue with logout even if API call fails
    } finally {
      // Clear local storage
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      
      // Dispatch custom event for token change
      window.dispatchEvent(new Event('tokenChanged'));
      
      // Navigate to auth page
      navigate('/auth');
      setLoggingOut(false);
    }
  };

  const getRoleDisplayName = (role: string) => {
    switch (role.toLowerCase()) {
      case 'admin': return 'Administrator';
      case 'causer': return 'CA Operator';
      case 'user': return 'User';
      default: return role;
    }
  };

  const getRoleColor = (role: string) => {
    switch (role.toLowerCase()) {
      case 'admin': return '#dc2626';
      case 'causer': return '#7c3aed';
      case 'user': return '#059669';
      default: return '#6b7280';
    }
  };

  if (!role) {
    return null; // Don't show menu if not logged in
  }

  return (
    <div ref={menuRef} style={{ position: 'relative' }}>
      {/* User Avatar/Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          padding: '8px 12px',
          backgroundColor: isOpen ? '#f3f4f6' : 'transparent',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          transition: 'all 0.2s',
          fontSize: 14,
          color: '#374151'
        }}
        onMouseOver={(e) => {
          if (!isOpen) {
            e.currentTarget.style.backgroundColor = '#f9fafb';
          }
        }}
        onMouseOut={(e) => {
          if (!isOpen) {
            e.currentTarget.style.backgroundColor = 'transparent';
          }
        }}
      >
        {/* User Avatar */}
        <div style={{
          width: 32,
          height: 32,
          borderRadius: '50%',
          backgroundColor: getRoleColor(role),
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'white',
          fontSize: 12,
          fontWeight: 'bold'
        }}>
          {getUserEmail().charAt(0).toUpperCase()}
        </div>

        {/* User Info */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}>
          <div style={{ fontSize: 14, fontWeight: 500, color: '#1f2937' }}>
            {getUserEmail().split('@')[0]}
          </div>
          <div style={{ fontSize: 12, color: '#6b7280' }}>
            {getRoleDisplayName(role)}
          </div>
        </div>

        {/* Dropdown Arrow */}
        <svg 
          width="16" 
          height="16" 
          fill="currentColor" 
          style={{ 
            transform: isOpen ? 'rotate(180deg)' : 'rotate(0deg)',
            transition: 'transform 0.2s'
          }}
        >
          <path d="M8 12l-4-4h8l-4 4z"/>
        </svg>
      </button>

      {/* Dropdown Menu */}
      {isOpen && (
        <div style={{
          position: 'absolute',
          top: '100%',
          right: 0,
          marginTop: 4,
          backgroundColor: 'white',
          borderRadius: 8,
          boxShadow: '0 10px 25px rgba(0, 0, 0, 0.1)',
          border: '1px solid #e5e7eb',
          minWidth: 200,
          zIndex: 1000,
          animation: 'fadeIn 0.2s ease-out'
        }}>
          {/* User Info Header */}
          <div style={{
            padding: 16,
            borderBottom: '1px solid #e5e7eb',
            backgroundColor: '#f9fafb'
          }}>
            <div style={{ fontSize: 14, fontWeight: 600, color: '#1f2937', marginBottom: 4 }}>
              {getUserEmail()}
            </div>
            <div style={{
              display: 'inline-block',
              padding: '2px 8px',
              backgroundColor: getRoleColor(role) + '20',
              color: getRoleColor(role),
              borderRadius: 12,
              fontSize: 12,
              fontWeight: 500
            }}>
              {getRoleDisplayName(role)}
            </div>
          </div>

          {/* Menu Items */}
          <div style={{ padding: 8 }}>
            <button
              onClick={() => {
                navigate('/profile');
                setIsOpen(false);
              }}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                width: '100%',
                padding: '8px 12px',
                backgroundColor: 'transparent',
                border: 'none',
                borderRadius: 6,
                cursor: 'pointer',
                fontSize: 14,
                color: '#374151',
                textAlign: 'left',
                transition: 'background-color 0.2s'
              }}
              onMouseOver={(e) => {
                e.currentTarget.style.backgroundColor = '#f3f4f6';
              }}
              onMouseOut={(e) => {
                e.currentTarget.style.backgroundColor = 'transparent';
              }}
            >
              <span style={{ fontSize: 16 }}>👤</span>
              My Profile
            </button>

            <div style={{ height: 1, backgroundColor: '#e5e7eb', margin: '4px 0' }} />

            <button
              onClick={handleLogout}
              disabled={loggingOut}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                width: '100%',
                padding: '8px 12px',
                backgroundColor: 'transparent',
                border: 'none',
                borderRadius: 6,
                cursor: loggingOut ? 'not-allowed' : 'pointer',
                fontSize: 14,
                color: loggingOut ? '#9ca3af' : '#dc2626',
                textAlign: 'left',
                transition: 'background-color 0.2s',
                opacity: loggingOut ? 0.6 : 1
              }}
              onMouseOver={(e) => {
                if (!loggingOut) {
                  e.currentTarget.style.backgroundColor = '#fef2f2';
                }
              }}
              onMouseOut={(e) => {
                if (!loggingOut) {
                  e.currentTarget.style.backgroundColor = 'transparent';
                }
              }}
            >
              <span style={{ fontSize: 16 }}>🚪</span>
              {loggingOut ? 'Logging out...' : 'Logout'}
            </button>
          </div>
        </div>
      )}

      {/* CSS for fade in animation */}
      <style>{`
        @keyframes fadeIn {
          from {
            opacity: 0;
            transform: translateY(-10px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
      `}</style>
    </div>
  );
};

export default UserMenu;
