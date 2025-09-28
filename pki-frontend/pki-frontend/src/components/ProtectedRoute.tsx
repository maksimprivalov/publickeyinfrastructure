import React from 'react';
import { Navigate } from 'react-router-dom';
import { useUserRole } from '../context/UserContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles: string[];
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, allowedRoles }) => {
  const role = useUserRole();
  const hasToken = !!localStorage.getItem('access_token');

  // If no token, redirect to auth
  if (!hasToken) {
    return <Navigate to="/auth" replace />;
  }

  // If token exists but role is not loaded yet, show loading
  if (!role) {
    return (
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        backgroundColor: '#f1f5f9'
      }}>
        <div style={{
          padding: 32,
          textAlign: 'center',
          backgroundColor: 'white',
          borderRadius: 12,
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
        }}>
          <div style={{
            width: 40,
            height: 40,
            border: '4px solid #e5e7eb',
            borderTopColor: '#2563eb',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite',
            margin: '0 auto 16px'
          }} />
          <p style={{
            fontSize: 16,
            color: '#6b7280',
            margin: 0
          }}>
            Loading...
          </p>
        </div>
      </div>
    );
  }

  // If role is loaded but not allowed for this route
  if (!allowedRoles.includes(role)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;