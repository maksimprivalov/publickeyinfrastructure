import React, { useState } from 'react';
import userApi from '../api/user/userApi';

const Profile: React.FC = () => {
  const [userInfo] = useState({
    email: 'user@example.com',
    name: 'John',
    surname: 'Doe',
    organization: 'Example Organization',
    role: 'USER'
  });

  const handleLogout = async () => {
    try {
      // Call logout API to invalidate refresh token on server
      await userApi.post({
        url: '/api/users/logout',
        authenticated: true
      });
    } catch (error) {
      // Log error but still proceed with logout
      console.warn('Logout API call failed:', error);
    } finally {
      // Always clear tokens and redirect, even if API call fails
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      
      // Dispatch custom event to notify hooks about token removal
      window.dispatchEvent(new CustomEvent('tokenChanged'));
      
      window.location.href = '/auth';
    }
  };

  const getRoleText = (role: string) => {
    switch (role) {
      case 'ADMIN': return 'Administrator';
      case 'CAUSER': return 'CA Operator';
      case 'USER': return 'User';
      default: return role;
    }
  };

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: 32 }}>
      <div style={{ marginBottom: 32 }}>
        <h1 style={{ 
          fontSize: 28, 
          fontWeight: 700, 
          color: '#1f2937', 
          margin: 0, 
          marginBottom: 8 
        }}>
          User Profile
        </h1>
        <p style={{ 
          fontSize: 16, 
          color: '#6b7280', 
          margin: 0 
        }}>
          Manage your account information and settings
        </p>
      </div>

      <div style={{
        backgroundColor: 'white',
        borderRadius: 12,
        padding: 32,
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        border: '1px solid #e5e7eb',
        marginBottom: 24
      }}>
        <h2 style={{
          fontSize: 20,
          fontWeight: 600,
          color: '#1f2937',
          marginTop: 0,
          marginBottom: 24
        }}>
          Account Information
        </h2>

        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: '1fr 1fr', 
          gap: 24,
          marginBottom: 32
        }}>
          <div>
            <label style={{
              display: 'block',
              fontSize: 14,
              fontWeight: 600,
              color: '#374151',
              marginBottom: 8
            }}>
              Email Address
            </label>
            <div style={{
              padding: 12,
              backgroundColor: '#f9fafb',
              borderRadius: 8,
              border: '1px solid #e5e7eb',
              color: '#6b7280'
            }}>
              {userInfo.email}
            </div>
          </div>

          <div>
            <label style={{
              display: 'block',
              fontSize: 14,
              fontWeight: 600,
              color: '#374151',
              marginBottom: 8
            }}>
              Role
            </label>
            <div style={{
              padding: 12,
              backgroundColor: '#f9fafb',
              borderRadius: 8,
              border: '1px solid #e5e7eb',
              color: '#6b7280'
            }}>
              <span style={{
                padding: '4px 8px',
                borderRadius: '4px',
                fontSize: '12px',
                fontWeight: 'bold',
                backgroundColor: '#dbeafe',
                color: '#1e40af'
              }}>
                {getRoleText(userInfo.role)}
              </span>
            </div>
          </div>

          <div>
            <label style={{
              display: 'block',
              fontSize: 14,
              fontWeight: 600,
              color: '#374151',
              marginBottom: 8
            }}>
              First Name
            </label>
            <div style={{
              padding: 12,
              backgroundColor: '#f9fafb',
              borderRadius: 8,
              border: '1px solid #e5e7eb',
              color: '#6b7280'
            }}>
              {userInfo.name}
            </div>
          </div>

          <div>
            <label style={{
              display: 'block',
              fontSize: 14,
              fontWeight: 600,
              color: '#374151',
              marginBottom: 8
            }}>
              Last Name
            </label>
            <div style={{
              padding: 12,
              backgroundColor: '#f9fafb',
              borderRadius: 8,
              border: '1px solid #e5e7eb',
              color: '#6b7280'
            }}>
              {userInfo.surname}
            </div>
          </div>

          <div style={{ gridColumn: '1 / -1' }}>
            <label style={{
              display: 'block',
              fontSize: 14,
              fontWeight: 600,
              color: '#374151',
              marginBottom: 8
            }}>
              Organization
            </label>
            <div style={{
              padding: 12,
              backgroundColor: '#f9fafb',
              borderRadius: 8,
              border: '1px solid #e5e7eb',
              color: '#6b7280'
            }}>
              {userInfo.organization}
            </div>
          </div>
        </div>

        <div style={{
          borderTop: '1px solid #e5e7eb',
          paddingTop: 24
        }}>
          <h3 style={{
            fontSize: 18,
            fontWeight: 600,
            color: '#1f2937',
            marginTop: 0,
            marginBottom: 16
          }}>
            Account Actions
          </h3>

          <div style={{ display: 'flex', gap: 12 }}>
            <button
              onClick={handleLogout}
              style={{
                padding: '12px 24px',
                backgroundColor: '#ef4444',
                color: 'white',
                border: 'none',
                borderRadius: '8px',
                fontSize: 14,
                fontWeight: 600,
                cursor: 'pointer',
                transition: 'all 0.2s ease',
                boxShadow: '0 2px 8px rgba(239, 68, 68, 0.2)'
              }}
              onMouseOver={(e) => {
                e.currentTarget.style.backgroundColor = '#dc2626';
                e.currentTarget.style.transform = 'translateY(-1px)';
                e.currentTarget.style.boxShadow = '0 4px 12px rgba(239, 68, 68, 0.3)';
              }}
              onMouseOut={(e) => {
                e.currentTarget.style.backgroundColor = '#ef4444';
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = '0 2px 8px rgba(239, 68, 68, 0.2)';
              }}
            >
              🚪 Sign Out
            </button>
          </div>
        </div>
      </div>

      <div style={{
        backgroundColor: '#fef3c7',
        borderRadius: 8,
        padding: 16,
        border: '1px solid #f59e0b'
      }}>
        <p style={{
          fontSize: 14,
          color: '#92400e',
          margin: 0,
          fontWeight: 500
        }}>
          💡 <strong>Note:</strong> To update your profile information, please contact your system administrator.
        </p>
      </div>
    </div>
  );
};

export default Profile;
