import type { RouteObject } from "react-router-dom";
import AuthPage from './pages/AuthPage';
import HomePage from './pages/HomePage';
import AdminPanel from './pages/AdminPanel';
import UserPanel from './pages/UserPanel';
import CAPanel from './pages/CAPanel';
import CertificateTemplates from './pages/CertificateTemplates';
import CertificateDetails from './pages/CertificateDetails';
import CertificateIssue from './pages/CertificateIssue';
import CertificateUpload from './pages/CertificateUpload';
import CertificateSearch from './pages/CertificateSearch';
import CRLDownload from './pages/CRLDownload';
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
    element: <MainLayout><HomePage /></MainLayout>,
  },
  {
    path: '/user',
    element: (
      <ProtectedRoute allowedRoles={['User', 'Admin', 'CAUser']}>
        <MainLayout><UserPanel /></MainLayout>
      </ProtectedRoute>
    ),
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
        <MainLayout><UserManagement /></MainLayout>
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
    path: '/certificates/:id',
    element: (
      <ProtectedRoute allowedRoles={['Admin', 'CAUser', 'User']}>
        <MainLayout><CertificateDetails /></MainLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: '/search',
    element: (
      <ProtectedRoute allowedRoles={['Admin', 'CAUser', 'User']}>
        <MainLayout><CertificateSearch /></MainLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: '/issue',
    element: (
      <ProtectedRoute allowedRoles={['Admin', 'CAUser']}>
        <MainLayout><CertificateIssue /></MainLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: '/upload',
    element: (
      <ProtectedRoute allowedRoles={['Admin', 'CAUser']}>
        <MainLayout><CertificateUpload /></MainLayout>
      </ProtectedRoute>
    ),
  },
  {
    path: '/crl',
    element: (
      <ProtectedRoute allowedRoles={['Admin', 'CAUser']}>
        <MainLayout><CRLDownload /></MainLayout>
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
