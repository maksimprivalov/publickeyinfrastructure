// Типы для организации, соответствующие backend
export interface Organization {
  id: number;
  name: string;
  description?: string;
  encryptionKey: string;
  users?: Array<{
    id: number;
    email: string;
    fullName: string;
  }>;
  createdAt: string;
  active: boolean;
}
