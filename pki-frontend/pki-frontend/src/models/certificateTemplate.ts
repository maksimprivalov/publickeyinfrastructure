// Типы для шаблона сертификата, соответствующие backend
import { Certificate } from './certificate';
import { User } from './user';

export interface CertificateTemplate {
  id: number;
  name: string;
  caIssuer: Certificate;
  cnRegex?: string;
  sanRegex?: string;
  maxTtlDays?: number;
  defaultKeyUsage?: string; // JSON string
  defaultExtendedKeyUsage?: string; // JSON string
  owner: User;
  createdAt: string;
}
