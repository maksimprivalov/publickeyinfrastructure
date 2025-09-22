import React, { useState } from 'react';
import { User, UserRole } from '../models/user';

interface UserFormProps {
  user?: User;
  onSubmit: (user: Omit<User, 'id' | 'createdAt' | 'updatedAt'>) => void;
  onCancel: () => void;
}

const roles: { value: UserRole; label: string }[] = [
  { value: 'admin', label: 'Администратор' },
  { value: 'ca_operator', label: 'Оператор ЦА' },
  { value: 'user', label: 'Пользователь' },
];

const UserForm: React.FC<UserFormProps> = ({ user, onSubmit, onCancel }) => {
  const [email, setEmail] = useState(user?.email || '');
  const [fullName, setFullName] = useState(user?.fullName || '');
  const [role, setRole] = useState<UserRole>(user?.role || 'user');
  const [isActive, setIsActive] = useState(user?.isActive ?? true);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({ email, fullName, role, isActive });
  };

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16, minWidth: 320 }}>
      <label>
        Email
        <input type="email" value={email} onChange={e => setEmail(e.target.value)} required style={{ width: '100%' }} />
      </label>
      <label>
        ФИО
        <input type="text" value={fullName} onChange={e => setFullName(e.target.value)} required style={{ width: '100%' }} />
      </label>
      <label>
        Роль
        <select value={role} onChange={e => setRole(e.target.value as UserRole)} style={{ width: '100%' }}>
          {roles.map(r => <option key={r.value} value={r.value}>{r.label}</option>)}
        </select>
      </label>
      <label>
        <input type="checkbox" checked={isActive} onChange={e => setIsActive(e.target.checked)} /> Активен
      </label>
      <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
        <button type="button" onClick={onCancel} style={{ background: '#e5e7eb', border: 'none', borderRadius: 4, padding: '6px 16px', cursor: 'pointer' }}>Отмена</button>
        <button type="submit" style={{ background: '#2563eb', color: '#fff', border: 'none', borderRadius: 4, padding: '6px 16px', fontWeight: 700, cursor: 'pointer' }}>Сохранить</button>
      </div>
    </form>
  );
};

export default UserForm;
