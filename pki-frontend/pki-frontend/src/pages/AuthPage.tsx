import { useState } from 'react';
import { LoginForm } from '../components/LoginForm';
import { RegisterForm } from '../components/RegisterForm';

const AuthPage = () => {
  const [showLogin, setShowLogin] = useState(true);

  const buttonStyle = (isActive: boolean) => ({
    padding: '12px 32px',
    borderRadius: 12,
    fontWeight: 600,
    fontSize: 16,
    border: 'none',
    background: isActive 
      ? 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)' 
      : 'rgba(255, 255, 255, 0.9)',
    color: isActive ? '#fff' : '#2563eb',
    cursor: 'pointer',
    transition: 'all 0.3s ease',
    boxShadow: isActive 
      ? '0 4px 12px rgba(37, 99, 235, 0.3)' 
      : '0 2px 8px rgba(0, 0, 0, 0.1)',
    backdropFilter: 'blur(10px)',
    border: isActive ? 'none' : '2px solid rgba(37, 99, 235, 0.2)'
  });

  return (
    <div style={{ 
      minHeight: '100vh', 
      minWidth: '100vw', 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'center', 
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      padding: '20px',
      boxSizing: 'border-box'
    }}>
        {/* Background pattern */}
        <div style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundImage: `
            radial-gradient(circle at 20% 20%, rgba(255,255,255,0.1) 1px, transparent 1px),
            radial-gradient(circle at 80% 80%, rgba(255,255,255,0.1) 1px, transparent 1px)
          `,
          backgroundSize: '50px 50px',
          zIndex: 1
        }} />

        <div style={{ 
          width: '100%', 
          maxWidth: 1200,
          display: 'flex', 
          flexDirection: 'column', 
          alignItems: 'center',
          position: 'relative',
          zIndex: 2
        }}>
          {/* Header */}
          <div style={{
            textAlign: 'center',
            marginBottom: 40
          }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 16,
              marginBottom: 16
            }}>
              <div style={{
                width: 56,
                height: 56,
                background: 'rgba(255,255,255,0.2)',
                borderRadius: '16px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 24,
                backdropFilter: 'blur(10px)',
                border: '1px solid rgba(255,255,255,0.3)'
              }}>
                🔐
              </div>
              <h1 style={{
                fontSize: 32,
                fontWeight: 700,
                color: '#fff',
                margin: 0,
                letterSpacing: '-0.025em'
              }}>
                PKI System
              </h1>
            </div>
            <p style={{
              fontSize: 16,
              color: 'rgba(255,255,255,0.8)',
              margin: 0,
              maxWidth: 500,
              lineHeight: 1.6
            }}>
              Public Key Infrastructure Management System
            </p>
          </div>

          {/* Toggle buttons */}
          <div style={{ 
            display: 'flex', 
            gap: 8, 
            marginBottom: 32,
            padding: 6,
            backgroundColor: 'rgba(255, 255, 255, 0.15)',
            borderRadius: 16,
            backdropFilter: 'blur(10px)',
            border: '1px solid rgba(255, 255, 255, 0.2)'
          }}>
            <button
              style={buttonStyle(showLogin)}
              onClick={() => setShowLogin(true)}
              onMouseOver={(e) => {
                if (!showLogin) {
                  e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 1)';
                  e.currentTarget.style.transform = 'translateY(-1px)';
                }
              }}
              onMouseOut={(e) => {
                if (!showLogin) {
                  e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
                  e.currentTarget.style.transform = 'translateY(0)';
                }
              }}
            >
              Sign In
            </button>
            <button
              style={buttonStyle(!showLogin)}
              onClick={() => setShowLogin(false)}
              onMouseOver={(e) => {
                if (showLogin) {
                  e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 1)';
                  e.currentTarget.style.transform = 'translateY(-1px)';
                }
              }}
              onMouseOut={(e) => {
                if (showLogin) {
                  e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
                  e.currentTarget.style.transform = 'translateY(0)';
                }
              }}
            >
              Register
            </button>
          </div>

          {/* Form container */}
          <div style={{ 
            width: '100%', 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center',
            minHeight: 500
          }}>
            <div style={{
              width: '100%',
              maxWidth: showLogin ? 400 : 600,
              transition: 'all 0.3s ease'
            }}>
              {showLogin ? <LoginForm /> : <RegisterForm />}
            </div>
          </div>

          {/* Footer */}
          <div style={{
            marginTop: 40,
            textAlign: 'center',
            color: 'rgba(255,255,255,0.7)',
            fontSize: 14
          }}>
            <p style={{ margin: 0 }}>
              © 2025 PKI System. Securing the digital world.
            </p>
          </div>
        </div>
      </div>
  );
};

export default AuthPage;
