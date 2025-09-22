// API для управления центрами сертификации
import type { AuthAxiosRequestConfig, IApi } from "../data/apiTypes";
import apiClient from "../../interceptor/interceptor";
import type { Certificate } from "../../models/certificate";
import type { CreateCSRRequest } from "../../models/certificateSigningRequest";
import certificatesApi from "./certificatesApi";

class CAApi implements IApi {
  
  get(requestConfig?: AuthAxiosRequestConfig) {
    return apiClient.get(requestConfig?.url || '', requestConfig);
  }
  
  post(requestConfig?: AuthAxiosRequestConfig) {
    return apiClient.post(requestConfig?.url || '', requestConfig?.data, requestConfig);
  }
  
  put(requestConfig?: AuthAxiosRequestConfig) {
    return apiClient.put(requestConfig?.url || '', requestConfig?.data, requestConfig);
  }
  
  delete(requestConfig?: AuthAxiosRequestConfig) {
    return apiClient.delete(requestConfig?.url || '', requestConfig);
  }

  // Получить все сертификаты центров сертификации (ROOT_CA и INTERMEDIATE_CA)
  async getAllCAs(): Promise<Certificate[]> {
    const allCertificates = await certificatesApi.getAllCertificates();
    return allCertificates.filter(cert => 
      cert.type === 'ROOT_CA' || cert.type === 'INTERMEDIATE_CA'
    );
  }

  // Получить только корневые ЦА
  async getRootCAs(): Promise<Certificate[]> {
    const allCertificates = await certificatesApi.getAllCertificates();
    return allCertificates.filter(cert => cert.type === 'ROOT_CA');
  }

  // Получить только промежуточные ЦА
  async getIntermediateCAs(): Promise<Certificate[]> {
    const allCertificates = await certificatesApi.getAllCertificates();
    return allCertificates.filter(cert => cert.type === 'INTERMEDIATE_CA');
  }

  // Создать корневой ЦА
  async createRootCA(): Promise<Certificate> {
    return certificatesApi.issueRootCertificate();
  }

  // Создать корневой ЦА с шаблоном
  async createRootCAWithTemplate(templateId: number): Promise<Certificate> {
    return certificatesApi.issueRootWithTemplate(templateId);
  }

  // Создать промежуточный ЦА
  async createIntermediateCA(parentCAId: number, csr: CreateCSRRequest): Promise<Certificate> {
    return certificatesApi.issueIntermediateCertificate(parentCAId, csr);
  }

  // Создать промежуточный ЦА с шаблоном
  async createIntermediateCAWithTemplate(templateId: number, csr: CreateCSRRequest): Promise<Certificate> {
    return certificatesApi.issueIntermediateWithTemplate(templateId, csr);
  }

  // Получить ЦА по ID
  async getCAById(id: number): Promise<Certificate> {
    const ca = await certificatesApi.getCertificateById(id);
    if (ca.type !== 'ROOT_CA' && ca.type !== 'INTERMEDIATE_CA') {
      throw new Error('Certificate is not a CA');
    }
    return ca;
  }

  // Получить сертификаты, выпущенные конкретным ЦА
  async getCertificatesIssuedBy(caId: number): Promise<Certificate[]> {
    const ca = await this.getCAById(caId);
    return ca.issuedCertificates || [];
  }
}

const caApi = new CAApi();
export default caApi;
