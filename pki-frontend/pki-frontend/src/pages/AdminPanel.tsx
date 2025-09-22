import React, { useEffect, useState } from 'react';
import UserManagement from './UserManagement';
import CAManagement from './CAManagement';
import { User } from '../models/user';

// Пример: получение пользователей и ЦА (заглушка)
const fetchUsers = async (): Promise<User[]> => {
  // TODO: заменить на реальный API
  return [
    { id: '1', email: 'admin@pki.local', fullName: 'Админ', role: 'admin', isActive: true, createdAt: '', updatedAt: '' },
    { id: '2', email: 'ca@pki.local', fullName: 'CA Operator', role: 'ca_operator', isActive: true, createdAt: '', updatedAt: '' },
  ];
};

const AdminPanel: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);

  useEffect(() => {
    fetchUsers().then(setUsers);
  }, []);

  return (
    <div style={{ padding: 32 }}>
      <h1>Панель администратора</h1>
      <section style={{ marginBottom: 32 }}>
        <h2>Управление пользователями</h2>
        <UserManagement users={users} />
      </section>
      <section>
        <h2>Управление центрами сертификации</h2>
        <CAManagement />
      </section>
    </div>
  );
};

export default AdminPanel;
