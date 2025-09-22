// Типы для запроса на подпись сертификата, соответствующие backend
import { Certificate } from './certificate';
import { User } from './user';

export type CSRStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface CertificateSigningRequest {
  id: number;
  csrContent: string; // CSR в формате PEM
  requestedBy: User;
  selectedCA: Certificate;
  status: CSRStatus;
  createdAt: string; // ISO8601
  processedAt?: string; // ISO8601
  issuedCertificate?: Certificate; // выпущенный сертификат если статус APPROVED
  rejectionReason?: string; // причина отказа если статус REJECTED
}

// DTO для создания нового CSR
export interface CreateCSRRequest {
  csrContent: string;
}
