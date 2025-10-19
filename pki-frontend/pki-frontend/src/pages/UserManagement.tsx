import React, { useState, useEffect } from 'react';
import usersApi, { User, CreateUserRequest, UpdateUserRequest } from '../api/user/usersApi';
import UserForm from '../components/UserForm';

const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [editUser, setEditUser] = useState<User | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const userList = await usersApi.getAllUsers();
      setUsers(userList);
    } catch (err) {
      setError('Error loading users');
      console.error('Error loading users:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = () => {
    setEditUser(undefined);
    setShowForm(true);
  };

  const handleEdit = (user: User) => {
    setEditUser({
      email: user.email,
      name: user.name,
      surname: user.surname,
      organizationName: user.organizationName,
      role: user.role,
      isActive: user.isActive
    } as any);
    setShowForm(true);
  };

  const handleSave = async (userData: CreateUserRequest | UpdateUserRequest) => {
    try {
      if (editUser) {
        // Update existing user
        const updatedUser = await usersApi.updateUser(editUser.id, userData as UpdateUserRequest);
        setUsers(list => list.map(u => 
          u.id === editUser.id ? updatedUser : u
        ));
      } else {
        // Create new user
        const newUser = await usersApi.createUser(userData as CreateUserRequest);
        setUsers(list => [...list, newUser]);
      }
      setShowForm(false);
      setError(null);
    } catch (err) {
      setError('Error saving user');
      console.error('Error saving user:', err);
    }
  };

  const handleToggleActive = async (user: User) => {
    try {
      const updatedUser = await usersApi.toggleUserStatus(user.id);
      setUsers(list => list.map(u => 
        u.id === user.id ? updatedUser : u
      ));
      setError(null);
    } catch (err) {
      setError('Error changing user status');
      console.error('Error changing user status:', err);
    }
  };

  const handleCancel = () => setShowForm(false);

  const getRoleText = (role: string) => {
    switch (role) {
      case 'ADMIN': return 'Administrator';
      case 'CAUSER': return 'CA Operator';
      case 'USER': return 'User';
      default: return role;
    }
  };

  if (loading) {
    return (
      <div style={{ padding: 32, textAlign: 'center' }}>
        <h2>Управление пользователями</h2>
        <div>Загрузка...</div>
      </div>
    );
  }

  return (
    <div>
      {/* Modal для формы */}
      {showForm && (
        <div style={{ 
          position: 'fixed', 
          top: 0, 
          left: 0, 
          width: '100vw', 
          height: '100vh', 
          background: 'rgba(0,0,0,0.5)', 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center', 
          zIndex: 1000,
          backdropFilter: 'blur(4px)'
        }}>
          <UserForm user={editUser} onSubmit={handleSave} onCancel={handleCancel} />
        </div>
      )}

      {/* Заголовок и кнопка добавления */}
      <div style={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        marginBottom: 24 
      }}>
        <h2 style={{ margin: 0, color: '#1f2937' }}>User Management</h2>
        <div>
          <button
            onClick={loadUsers}
            style={{
              padding: '8px 16px',
              backgroundColor: '#10b981',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer',
              marginRight: 8,
              fontSize: 14,
              fontWeight: 500
            }}
          >
            Refresh
          </button>
          <button
            onClick={handleAdd}
            style={{
              padding: '8px 16px',
              backgroundColor: '#2563eb',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer',
              fontSize: 14,
              fontWeight: 600
            }}
          >
            + Add User
          </button>
        </div>
      </div>

      {/* Ошибки */}
      {error && (
        <div style={{
          backgroundColor: '#fee2e2',
          color: '#991b1b',
          padding: 16,
          borderRadius: 6,
          marginBottom: 24,
          border: '1px solid #fecaca'
        }}>
          {error}
        </div>
      )}

      {/* Таблица пользователей */}
      {users.length === 0 ? (
        <div style={{ 
          textAlign: 'center', 
          padding: 40,
          backgroundColor: '#f9fafb',
          borderRadius: 8,
          color: '#6b7280'
        }}>
          <p>Пользователи не найдены</p>
        </div>
      ) : (
        <div style={{ 
          backgroundColor: 'white', 
          borderRadius: 8, 
          boxShadow: '0 2px 8px #e0e7ff',
          overflow: 'hidden'
        }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead style={{ background: '#f1f5f9' }}>
              <tr>
                <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Email</th>
                <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Full Name</th>
                <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Organization</th>
                <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Role</th>
                <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Status</th>
                <th style={{ padding: 16, textAlign: 'left', fontWeight: 600, color: '#374151' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id} style={{ borderBottom: '1px solid #e5e7eb' }}>
                  <td style={{ padding: 16, color: '#1f2937' }}>{user.email}</td>
                  <td style={{ padding: 16, color: '#1f2937' }}>{user.name} {user.surname}</td>
                  <td style={{ padding: 16, color: '#6b7280' }}>{user.organizationName}</td>
                  <td style={{ padding: 16 }}>
                    <span style={{
                      padding: '4px 8px',
                      borderRadius: '4px',
                      fontSize: '12px',
                      fontWeight: 'bold',
                      backgroundColor: user.role === 'ADMIN' ? '#dbeafe' : user.role === 'CAUSER' ? '#f3e8ff' : '#f0f9ff',
                      color: user.role === 'ADMIN' ? '#1e40af' : user.role === 'CAUSER' ? '#7c3aed' : '#0369a1'
                    }}>
                      {getRoleText(user.role)}
                    </span>
                  </td>
                  <td style={{ padding: 16 }}>
                    <span style={{
                      padding: '4px 8px',
                      borderRadius: '4px',
                      fontSize: '12px',
                      fontWeight: 'bold',
                      backgroundColor: user.isActive ? '#dcfce7' : '#fee2e2',
                      color: user.isActive ? '#166534' : '#991b1b'
                    }}>
                      {user.isActive ? 'Active' : 'Blocked'}
                    </span>
                  </td>
                  <td style={{ padding: 16 }}>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <button
                        onClick={() => handleEdit(user)}
                        style={{
                          padding: '6px 12px',
                          fontSize: '12px',
                          backgroundColor: '#3b82f6',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          cursor: 'pointer',
                          fontWeight: 500
                        }}
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleToggleActive(user)}
                        style={{
                          padding: '6px 12px',
                          fontSize: '12px',
                          backgroundColor: user.isActive ? '#ef4444' : '#22c55e',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          cursor: 'pointer',
                          fontWeight: 500
                        }}
                      >
                        {user.isActive ? 'Block' : 'Unblock'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default UserManagement;
