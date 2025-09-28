import React from 'react';
import Navigation from '../Navigation';

const MainLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <div style={{ 
    minHeight: '100vh', 
    width: '100%',
    maxWidth: '100%',
    background: '#f1f5f9',
    overflow: 'hidden'
  }}>
    <Navigation />
    <main style={{ 
      width: '100%', 
      maxWidth: '100%',
      padding: '32px 16px',
      boxSizing: 'border-box'
    }}>
      {children}
    </main>
  </div>
);

export default MainLayout;
