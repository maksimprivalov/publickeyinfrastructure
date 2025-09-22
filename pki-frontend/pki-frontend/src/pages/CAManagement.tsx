import React from 'react';
import { CA } from '../models/ca';

// Пример: список ЦА (заглушка)
const caList: CA[] = [
  { id: '1', name: 'Главный ЦА', status: 'active', createdAt: '', updatedAt: '' },
  { id: '2', name: 'Резервный ЦА', status: 'revoked', createdAt: '', updatedAt: '' },
];

const CAManagement: React.FC = () => {
  return (
    <div>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <th>Название</th>
            <th>Статус</th>
          </tr>
        </thead>
        <tbody>
          {caList.map((ca) => (
            <tr key={ca.id}>
              <td>{ca.name}</td>
              <td>{ca.status === 'active' ? 'Активен' : 'Отозван'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default CAManagement;
