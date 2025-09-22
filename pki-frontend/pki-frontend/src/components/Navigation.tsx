import React from 'react';
import { Link } from 'react-router-dom';
import { useRole } from '../hooks/useRole';

const Navigation: React.FC = () => {
  const role = useRole();

  return (
    <nav style={{ background: '#2563eb', padding: '12px 32px', color: '#fff', display: 'flex', gap: 24, alignItems: 'center', fontWeight: 500 }}>
      <Link to="/" style={{ color: '#fff', textDecoration: 'none', fontWeight: 700, fontSize: 20 }}>PKI</Link>
      {role === 'admin' && (
        <>
          <Link to="/admin" style={{ color: '#fff', textDecoration: 'none' }}>Админ-панель</Link>
          <Link to="/users" style={{ color: '#fff', textDecoration: 'none' }}>Пользователи</Link>
          <Link to="/ca" style={{ color: '#fff', textDecoration: 'none' }}>Центры сертификации</Link>
          <Link to="/templates" style={{ color: '#fff', textDecoration: 'none' }}>Шаблоны</Link>
        </>
      )}
      {role === 'ca_operator' && (
        <>
          <Link to="/ca-panel" style={{ color: '#fff', textDecoration: 'none' }}>Панель ЦА</Link>
          <Link to="/certificates" style={{ color: '#fff', textDecoration: 'none' }}>Сертификаты</Link>
          <Link to="/revocation" style={{ color: '#fff', textDecoration: 'none' }}>Отзыв</Link>
        </>
      )}
      {role === 'user' && (
        <>
          <Link to="/user" style={{ color: '#fff', textDecoration: 'none' }}>Мои сертификаты</Link>
          <Link to="/upload" style={{ color: '#fff', textDecoration: 'none' }}>Загрузить CSR</Link>
        </>
      )}
      <div style={{ marginLeft: 'auto' }}>
        <Link to="/profile" style={{ color: '#fff', textDecoration: 'none' }}>Профиль</Link>
      </div>
    </nav>
  );
};

export default Navigation;
