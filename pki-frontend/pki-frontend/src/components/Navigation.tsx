import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useRole } from '../hooks/useRole';
import userApi from '../api/user/userApi';
import { useNavigate } from 'react-router-dom';

const Navigation: React.FC = () => {
  const role = useRole();
  const location = useLocation();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const isAuthenticated = !!localStorage.getItem('access_token');

  const navigate = useNavigate();

  const navLinkStyle = (isActive: boolean) => ({
    color: '#fff',
    textDecoration: 'none',
    padding: '8px 16px',
    borderRadius: '6px',
    fontSize: 14,
    fontWeight: 500,
    transition: 'all 0.2s ease',
    backgroundColor: isActive ? 'rgba(255, 255, 255, 0.1)' : 'transparent',
    '&:hover': {
      backgroundColor: 'rgba(255, 255, 255, 0.15)',
      transform: 'translateY(-1px)'
    }
  });

  const brandStyle = {
    color: '#fff',
    textDecoration: 'none',
    fontWeight: 700,
    fontSize: 24,
    letterSpacing: '-0.025em',
    padding: '8px 0',
    display: 'flex',
    alignItems: 'center',
    gap: 8
  };

  const buttonStyle = {
    padding: '8px 16px',
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    color: '#fff',
    border: '1px solid rgba(255, 255, 255, 0.2)',
    borderRadius: '6px',
    fontSize: 14,
    fontWeight: 500,
    cursor: 'pointer',
    transition: 'all 0.2s ease',
    textDecoration: 'none' as const,
    display: 'inline-block' as const
  };

  const toggleMenu = () => setIsMenuOpen(!isMenuOpen);

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
      
      navigate('/auth');
    }
  };

  const NavLink = ({ to, children }: { to: string; children: React.ReactNode }) => (
    <Link
      to={to}
      style={navLinkStyle(location.pathname === to)}
      onMouseOver={(e) => {
        if (location.pathname !== to) {
          e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.15)';
          e.currentTarget.style.transform = 'translateY(-1px)';
        }
      }}
      onMouseOut={(e) => {
        if (location.pathname !== to) {
          e.currentTarget.style.backgroundColor = 'transparent';
          e.currentTarget.style.transform = 'translateY(0)';
        }
      }}
      onClick={() => setIsMenuOpen(false)}
    >
      {children}
    </Link>
  );

  return (
    <nav style={{
      background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
      padding: '0 16px',
      color: '#fff',
      boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
      borderBottom: '1px solid rgba(255,255,255,0.1)',
      position: 'sticky',
      top: 0,
      zIndex: 100,
      width: '100%',
      maxWidth: '100%',
      boxSizing: 'border-box',
      overflowX: 'hidden'
    }}>
      <div style={{
        maxWidth: 1200,
        margin: '0 auto',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        minHeight: 64
      }}>
        {/* Brand */}
        <Link to="/" style={brandStyle}>
          <span style={{
            width: 32,
            height: 32,
            background: 'rgba(255,255,255,0.2)',
            borderRadius: '8px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 16,
            fontWeight: 'bold'
          }}>
            🔐
          </span>
          PKI System
        </Link>

        {/* Desktop Navigation */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8
        }}>
          {isAuthenticated ? (
            <>
              {role === 'Admin' && (
                <>
                  <NavLink to="/admin">Admin Panel</NavLink>
                  <NavLink to="/users">Users</NavLink>
                  <NavLink to="/ca">Certificate Authorities</NavLink>
                  <NavLink to="/templates">Templates</NavLink>
                </>
              )}
              {role === 'CAUser' && (
                <>
                  <NavLink to="/ca-panel">CA Panel</NavLink>
                  <NavLink to="/certificates">Certificates</NavLink>
                  <NavLink to="/revocation">Revocation</NavLink>
                </>
              )}
              {role === 'User' && (
                <>
                  <NavLink to="/user">My Certificates</NavLink>
                  <NavLink to="/upload">Upload CSR</NavLink>
                </>
              )}

              <div style={{ width: 1, height: 24, background: 'rgba(255,255,255,0.2)', margin: '0 8px' }} />
              <NavLink to="/profile">Profile</NavLink>
              
              <button
                onClick={handleLogout}
                style={buttonStyle}
                onMouseOver={(e) => {
                  e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.2)';
                  e.currentTarget.style.transform = 'translateY(-1px)';
                }}
                onMouseOut={(e) => {
                  e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.1)';
                  e.currentTarget.style.transform = 'translateY(0)';
                }}
              >
                Sign Out
              </button>
            </>
          ) : (
            <Link
              to="/auth"
              style={buttonStyle}
              onMouseOver={(e) => {
                e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.2)';
                e.currentTarget.style.transform = 'translateY(-1px)';
              }}
              onMouseOut={(e) => {
                e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.1)';
                e.currentTarget.style.transform = 'translateY(0)';
              }}
            >
              Sign In
            </Link>
          )}
        </div>

        {/* Mobile Menu Button */}
        <button
          onClick={toggleMenu}
          style={{
            display: 'none',
            background: 'rgba(255,255,255,0.1)',
            border: 'none',
            borderRadius: 6,
            padding: 8,
            color: '#fff',
            cursor: 'pointer',
            fontSize: 18
          }}
          onMouseOver={(e) => {
            e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.2)';
          }}
          onMouseOut={(e) => {
            e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.1)';
          }}
        >
          {isMenuOpen ? '✕' : '☰'}
        </button>
      </div>

      {/* Mobile Menu */}
      {isMenuOpen && (
        <div style={{
          position: 'absolute',
          top: '100%',
          left: 0,
          right: 0,
          background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
          borderTop: '1px solid rgba(255,255,255,0.1)',
          boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
          padding: '16px 24px',
          display: 'flex',
          flexDirection: 'column',
          gap: 8
        }}>
          {isAuthenticated ? (
            <>
              {role === 'Admin' && (
                <>
                  <NavLink to="/admin">Admin Panel</NavLink>
                  <NavLink to="/users">Users</NavLink>
                  <NavLink to="/ca">Certificate Authorities</NavLink>
                  <NavLink to="/templates">Templates</NavLink>
                </>
              )}
              {role === 'CAUser' && (
                <>
                  <NavLink to="/ca-panel">CA Panel</NavLink>
                  <NavLink to="/certificates">Certificates</NavLink>
                  <NavLink to="/revocation">Revocation</NavLink>
                </>
              )}
              {role === 'User' && (
                <>
                  <NavLink to="/user">My Certificates</NavLink>
                  <NavLink to="/upload">Upload CSR</NavLink>
                </>
              )}
              <div style={{ height: 1, background: 'rgba(255,255,255,0.2)', margin: '8px 0' }} />
              <NavLink to="/profile">Profile</NavLink>
              
              <button
                onClick={() => {
                  handleLogout();
                  setIsMenuOpen(false);
                }}
                style={{
                  ...buttonStyle,
                  width: '100%',
                  textAlign: 'left'
                }}
              >
                Sign Out
              </button>
            </>
          ) : (
            <Link
              to="/auth"
              style={{
                ...buttonStyle,
                width: '100%',
                textAlign: 'left'
              }}
              onClick={() => setIsMenuOpen(false)}
            >
              Sign In
            </Link>
          )}
        </div>
      )}
    </nav>
  );
};

export default Navigation;
