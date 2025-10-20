import React, { useState, useEffect } from 'react';
import UserManagement from './UserManagement';
import CAManagement from './CAManagement';

const AdminPanel: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'users' | 'ca'>('users');
  const [stats, setStats] = useState({
    totalUsers: 0,
    activeUsers: 0,
    totalCAs: 0,
    activeCAs: 0
  });

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      // TODO: Загрузка реальной статистики через API
      setStats({
        totalUsers: 15,
        activeUsers: 12,
        totalCAs: 3,
        activeCAs: 2
      });
    } catch (error) {
      console.error('Error loading stats:', error);
    }
  };

  const tabStyle = (isActive: boolean) => ({
    padding: '12px 24px',
    backgroundColor: isActive ? '#2563eb' : 'transparent',
    color: isActive ? 'white' : '#6b7280',
    border: 'none',
    borderRadius: '8px 8px 0 0',
    cursor: 'pointer',
    fontSize: 14,
    fontWeight: 600,
    transition: 'all 0.2s ease',
    borderBottom: isActive ? 'none' : '2px solid transparent'
  });

  return (
    <div style={{ padding: 32, maxWidth: 1200, margin: '0 auto' }}>
      {/* Заголовок */}
      <div style={{ marginBottom: 32 }}>
        <h1 style={{ 
          fontSize: 28, 
          fontWeight: 700, 
          color: '#1f2937', 
          margin: 0, 
          marginBottom: 8 
        }}>
          Admin Panel
        </h1>
        <p style={{ 
          fontSize: 16, 
          color: '#6b7280', 
          margin: 0 
        }}>
          User and Certificate Authorities management
        </p>
      </div>

      {/* Statistical cards */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', 
        gap: 20, 
        marginBottom: 32 
      }}>
        <div style={{
          backgroundColor: 'white',
          padding: 24,
          borderRadius: 12,
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
          border: '1px solid #e5e7eb'
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 16
          }}>
            <div style={{
              width: 48,
              height: 48,
              backgroundColor: '#dbeafe',
              borderRadius: 12,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 20
            }}>
              👥
            </div>
            <div>
              <h3 style={{ 
                fontSize: 24, 
                fontWeight: 700, 
                color: '#2563eb', 
                margin: 0 
              }}>
                {stats.totalUsers}
              </h3>
              <p style={{ 
                fontSize: 14, 
                color: '#6b7280', 
                margin: 0 
              }}>
                Total users
              </p>
            </div>
          </div>
        </div>

        <div style={{
          backgroundColor: 'white',
          padding: 24,
          borderRadius: 12,
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
          border: '1px solid #e5e7eb'
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 16
          }}>
            <div style={{
              width: 48,
              height: 48,
              backgroundColor: '#d1fae5',
              borderRadius: 12,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 20
            }}>
              ✅
            </div>
            <div>
              <h3 style={{ 
                fontSize: 24, 
                fontWeight: 700, 
                color: '#059669', 
                margin: 0 
              }}>
                {stats.activeUsers}
              </h3>
              <p style={{ 
                fontSize: 14, 
                color: '#6b7280', 
                margin: 0 
              }}>
                Active users
              </p>
            </div>
          </div>
        </div>

        <div style={{
          backgroundColor: 'white',
          padding: 24,
          borderRadius: 12,
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
          border: '1px solid #e5e7eb'
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 16
          }}>
            <div style={{
              width: 48,
              height: 48,
              backgroundColor: '#fef3c7',
              borderRadius: 12,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 20
            }}>
              🏢
            </div>
            <div>
              <h3 style={{ 
                fontSize: 24, 
                fontWeight: 700, 
                color: '#d97706', 
                margin: 0 
              }}>
                {stats.totalCAs}
              </h3>
              <p style={{ 
                fontSize: 14, 
                color: '#6b7280', 
                margin: 0 
              }}>
                Certificate Authorities
              </p>
            </div>
          </div>
        </div>

        <div style={{
          backgroundColor: 'white',
          padding: 24,
          borderRadius: 12,
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
          border: '1px solid #e5e7eb'
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 16
          }}>
            <div style={{
              width: 48,
              height: 48,
              backgroundColor: '#ecfdf5',
              borderRadius: 12,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 20
            }}>
              🔒
            </div>
            <div>
              <h3 style={{ 
                fontSize: 24, 
                fontWeight: 700, 
                color: '#059669', 
                margin: 0 
              }}>
                {stats.activeCAs}
              </h3>
              <p style={{ 
                fontSize: 14, 
                color: '#6b7280', 
                margin: 0 
              }}>
                Active CAs
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs navigation */}
      <div style={{
        backgroundColor: 'white',
        borderRadius: '12px 12px 0 0',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        border: '1px solid #e5e7eb',
        borderBottom: 'none'
      }}>
        <div style={{ 
          display: 'flex', 
          gap: 4,
          padding: '8px 8px 0 8px'
        }}>
          <button
            onClick={() => setActiveTab('users')}
            style={tabStyle(activeTab === 'users')}
            onMouseOver={(e) => {
              if (activeTab !== 'users') {
                e.currentTarget.style.backgroundColor = '#f3f4f6';
                e.currentTarget.style.color = '#374151';
              }
            }}
            onMouseOut={(e) => {
              if (activeTab !== 'users') {
                e.currentTarget.style.backgroundColor = 'transparent';
                e.currentTarget.style.color = '#6b7280';
              }
            }}
          >
            👥 Users
          </button>
          <button
            onClick={() => setActiveTab('ca')}
            style={tabStyle(activeTab === 'ca')}
            onMouseOver={(e) => {
              if (activeTab !== 'ca') {
                e.currentTarget.style.backgroundColor = '#f3f4f6';
                e.currentTarget.style.color = '#374151';
              }
            }}
            onMouseOut={(e) => {
              if (activeTab !== 'ca') {
                e.currentTarget.style.backgroundColor = 'transparent';
                e.currentTarget.style.color = '#6b7280';
              }
            }}
          >
            🏢 Certificate Authorities
          </button>
        </div>
      </div>

      {/* Content tabs */}
      <div style={{
        backgroundColor: 'white',
        borderRadius: '0 0 12px 12px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        border: '1px solid #e5e7eb',
        padding: 32,
        minHeight: 400
      }}>
        {activeTab === 'users' && <UserManagement />}
        {activeTab === 'ca' && <CAManagement />}
      </div>
    </div>
  );
};

export default AdminPanel;
