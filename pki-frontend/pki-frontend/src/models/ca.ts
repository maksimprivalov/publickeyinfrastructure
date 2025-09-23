// Типы для центра сертификации
export interface CA {
  id: string;
  name: string;
  status: 'active' | 'revoked';
  createdAt: string;
  updatedAt: string;
}
