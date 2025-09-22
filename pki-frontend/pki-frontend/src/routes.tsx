import type { RouteObject } from "react-router-dom";
import { Test } from "./components/Test";
import AuthPage from './pages/AuthPage';
import AdminPanel from './pages/AdminPanel';
import UserPanel from './pages/UserPanel';
import CAPanel from './pages/CAPanel';
import CertificateTemplates from './pages/CertificateTemplates';
import CertificateDetails from './pages/CertificateDetails';
import CertificateIssue from './pages/CertificateIssue';
import CertificateUpload from './pages/CertificateUpload';
import Revocation from './pages/Revocation';
import UserManagement from './pages/UserManagement';
import CAManagement from './pages/CAManagement';
import Profile from './pages/Profile';
import NotFound from './pages/NotFound';
import MainLayout from './components/layouts/MainLayout';
import ProtectedRoute from './components/ProtectedRoute';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useUserRole } from './context/UserContext';

const AuthRedirect: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const navigate = useNavigate();
  const role = useUserRole();
  useEffect(() => {
    if (localStorage.getItem('access_token') && role) {
      navigate('/', { replace: true });
    }
  }, [navigate, role]);
  return <>{children}</>;
};

const paths: RouteObject[] = [
  {
    path: '/',
    element: <MainLayout><UserPanel /></MainLayout>,
  },
  {
    path: '/admin',
    element: (
      <ProtectedRoute allowedRoles={['Admin']}>
        <MainLayout><AdminPanel /></MainLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: '/users',
    element: (
      <ProtectedRoute allowedRoles={['Admin']}>
        <MainLayout><UserManagement users={[]} /></MainLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: '/ca',
    element: (
      <ProtectedRoute allowedRoles={['Admin']}>
        <MainLayout><CAManagement /></MainLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: '/ca-panel',
    element: (
      <ProtectedRoute allowedRoles={['CAUser']}>
        <MainLayout><CAPanel /></MainLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: '/templates',
    element: (
      <ProtectedRoute allowedRoles={['Admin', 'CAUser']}>
        <MainLayout><CertificateTemplates /></MainLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: '/certificates',
    element: (
      <ProtectedRoute allowedRoles={['CAUser', 'User']}>
        <MainLayout><CertificateDetails /></MainLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: '/issue',
    element: (
      <ProtectedRoute allowedRoles={['CAUser']}>
        <MainLayout><CertificateIssue /></MainLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: '/upload',
    element: (
      <ProtectedRoute allowedRoles={['User']}>
        <MainLayout><CertificateUpload /></MainLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: '/revocation',
    element: (
      <ProtectedRoute allowedRoles={['CAUser']}>
        <MainLayout><Revocation /></MainLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: '/profile',
    element: (
      <ProtectedRoute allowedRoles={['Admin', 'CAUser', 'User']}>
        <MainLayout><Profile /></MainLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: '/auth',
    element: (
      <AuthRedirect>
        <AuthPage />
      </AuthRedirect>
    ),
  },
  {
    path: '*',
    element: <NotFound />,
  },
];

export default paths;
