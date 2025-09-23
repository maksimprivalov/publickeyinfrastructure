import React from 'react';
import Navigation from '../Navigation';

const MainLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <div style={{ minHeight: '100vh', minWidth: '100vw', background: '#f1f5f9' }}>
    <Navigation />
    <main style={{ width: '100%', padding: '32px 24px' }}>
      {children}
    </main>
  </div>
);

export default MainLayout;
