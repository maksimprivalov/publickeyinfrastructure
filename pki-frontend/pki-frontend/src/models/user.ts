// Типы для пользователя и ролей
export type UserRole = 'admin' | 'ca_operator' | 'user';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}
