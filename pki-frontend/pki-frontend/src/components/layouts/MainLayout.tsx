import React from 'react';
import Navigation from '../Navigation';

const MainLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <div style={{ minHeight: '100vh', background: '#f1f5f9' }}>
    <Navigation />
    <main style={{ maxWidth: 1200, margin: '0 auto', padding: '32px 16px' }}>
      {children}
    </main>
  </div>
);

export default MainLayout;
