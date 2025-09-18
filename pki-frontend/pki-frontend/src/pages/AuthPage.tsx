import { useState } from 'react';
import { LoginForm } from '../components/LoginForm';
import { RegisterForm } from '../components/RegisterForm';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient();

const AuthPage = () => {
  const [showLogin, setShowLogin] = useState(true);

  return (
    <QueryClientProvider client={queryClient}>
      <div style={{ minHeight: '100vh', minWidth: '100vw', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'linear-gradient(135deg, #e0e7ff 0%, #fff7ed 50%, #fbcfe8 100%)' }}>
        <div style={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ width: '100%', height: '500px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            {showLogin ? <LoginForm /> : <RegisterForm />}
          </div>
          <div style={{ display: 'flex', gap: 16, marginTop: 24, width: '100%', justifyContent: 'center' }}>
            <button
              style={{ borderRadius: 26, fontWeight: 700, fontSize: 16, border: showLogin ? '2px solid #2563eb' : '2px solid #2563eb', background: showLogin ? '#2563eb' : '#fff', color: showLogin ? '#fff' : '#2563eb', cursor: 'pointer', transition: 'all 0.3s' }}
              onClick={() => setShowLogin(true)}
            >
              Sign In
            </button>
            <button
              style={{ borderRadius: 26, fontWeight: 700, fontSize: 16, border: !showLogin ? '2px solid #2563eb' : '2px solid #2563eb', background: !showLogin ? '#2563eb' : '#fff', color: !showLogin ? '#fff' : '#2563eb', cursor: 'pointer', transition: 'all 0.3s' }}
              onClick={() => setShowLogin(false)}
            >
              Register
            </button>
          </div>
        </div>
      </div>
    </QueryClientProvider>
  );
};

export default AuthPage;
