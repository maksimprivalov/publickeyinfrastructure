// Типы для отозванного сертификата и причины отзыва, соответствующие backend
import { Certificate } from './certificate';
import { User } from './user';

export const REVOCATION_REASONS = [
  'UNSPECIFIED',
  'KEY_COMPROMISE',
  'CA_COMPROMISE',
  'AFFILIATION_CHANGED',
  'SUPERSEDED',
  'CESSATION_OF_OPERATION',
  'CERTIFICATE_HOLD',
  'REMOVE_FROM_CRL',
  'PRIVILEGE_WITHDRAWN',
  'AA_COMPROMISE'
] as const;

export interface RevokedCertificate {
  id: number;
  certificate: Certificate;
  revocationDate: string; // ISO8601
  reason: RevocationReason;
  revokedBy: User;
}
