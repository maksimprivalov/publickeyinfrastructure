// Типы для сертификата, соответствующие backend
export type CertificateStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED';
export type CertificateType = 'ROOT_CA' | 'INTERMEDIATE_CA' | 'END_ENTITY';

export interface Certificate {
  id: number;
  serialNumber: string; // BigInteger как строка
  subject: string;
  issuer: string;
  publicKey: string;
  encryptedPrivateKey?: string;
  certificateData: string;
  validFrom: string; // ISO8601
  validTo: string;   // ISO8601
  type: CertificateType;
  status: CertificateStatus;
  owner: {
    id: number;
    email: string;
    fullName: string;
  };
  issuerCertificate?: Certificate;
  issuedCertificates?: Certificate[];
  extensions?: string;
  organization: string;
  createdAt: string;
}
