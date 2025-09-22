import React from 'react';
import { User } from '../models/user';
import UserForm from '../components/UserForm';

interface UserManagementProps {
  users: User[];
}

const UserManagement: React.FC<UserManagementProps> = ({ users }) => {
  const [showForm, setShowForm] = React.useState(false);
  const [editUser, setEditUser] = React.useState<User | undefined>(undefined);
  const [userList, setUserList] = React.useState(users);

  React.useEffect(() => { setUserList(users); }, [users]);

  const handleAdd = () => {
    setEditUser(undefined);
    setShowForm(true);
  };
  const handleEdit = (user: User) => {
    setEditUser(user);
    setShowForm(true);
  };
  const handleSave = (userData: Omit<User, 'id' | 'createdAt' | 'updatedAt'>) => {
    if (editUser) {
      setUserList(list => list.map(u => u.id === editUser.id ? { ...editUser, ...userData } : u));
    } else {
      setUserList(list => [...list, { ...userData, id: Date.now().toString(), createdAt: '', updatedAt: '' }]);
    }
    setShowForm(false);
  };
  const handleCancel = () => setShowForm(false);

  return (
    <div>
      {showForm && (
        <div style={{ position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh', background: 'rgba(0,0,0,0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ background: '#fff', borderRadius: 8, padding: 32, minWidth: 340, boxShadow: '0 2px 16px #e0e7ff' }}>
            <UserForm user={editUser} onSubmit={handleSave} onCancel={handleCancel} />
          </div>
        </div>
      )}
      <table style={{ width: '100%', borderCollapse: 'collapse', background: '#fff', borderRadius: 8, boxShadow: '0 2px 8px #e0e7ff' }}>
        <thead style={{ background: '#f1f5f9' }}>
          <tr>
            <th style={{ padding: 8, borderBottom: '1px solid #e5e7eb' }}>Email</th>
            <th style={{ padding: 8, borderBottom: '1px solid #e5e7eb' }}>ФИО</th>
            <th style={{ padding: 8, borderBottom: '1px solid #e5e7eb' }}>Роль</th>
            <th style={{ padding: 8, borderBottom: '1px solid #e5e7eb' }}>Статус</th>
            <th style={{ padding: 8, borderBottom: '1px solid #e5e7eb' }}>Действия</th>
          </tr>
        </thead>
        <tbody>
          {userList.map((u) => (
            <tr key={u.id}>
              <td style={{ padding: 8 }}>{u.email}</td>
              <td style={{ padding: 8 }}>{u.fullName}</td>
              <td style={{ padding: 8 }}>{u.role}</td>
              <td style={{ padding: 8 }}>{u.isActive ? 'Активен' : 'Заблокирован'}</td>
              <td style={{ padding: 8 }}>
                <button style={{ marginRight: 8, background: '#2563eb', color: '#fff', border: 'none', borderRadius: 4, padding: '4px 12px', cursor: 'pointer' }} onClick={() => handleEdit(u)}>Редактировать</button>
                <button style={{ background: u.isActive ? '#f87171' : '#22c55e', color: '#fff', border: 'none', borderRadius: 4, padding: '4px 12px', cursor: 'pointer' }}>
                  {u.isActive ? 'Заблокировать' : 'Разблокировать'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <button style={{ marginTop: 16, background: '#22c55e', color: '#fff', border: 'none', borderRadius: 4, padding: '8px 20px', fontWeight: 700, cursor: 'pointer' }} onClick={handleAdd}>
        + Добавить пользователя
      </button>
    </div>
  );
};

export default UserManagement;
