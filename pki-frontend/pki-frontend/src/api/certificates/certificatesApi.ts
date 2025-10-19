// API для работы с сертификатами
import type { AuthAxiosRequestConfig, IApi } from "../data/apiTypes";
import apiClient from "../../interceptor/interceptor";
import type { Certificate } from "../../models/certificate";
import type { CreateCSRRequest } from "../../models/certificateSigningRequest";
import type { CertificateTemplate } from "../../models/certificateTemplate";
import userApi, { JwtPayload, UserApi } from "../user/userApi";
import { jwtDecode } from "jwt-decode";

class CertificatesApi implements IApi {
  private baseUrl = "/api/certificates";

  get(requestConfig?: AuthAxiosRequestConfig) {
    const { url, ...config } = requestConfig || {};
    return apiClient.get(url || '', config);
  }
  
  post(requestConfig?: AuthAxiosRequestConfig) {
    const { url, data, ...config } = requestConfig || {};
    return apiClient.post(url || '', data, config);
  }
  
  put(requestConfig?: AuthAxiosRequestConfig) {
    const { url, data, ...config } = requestConfig || {};
    return apiClient.put(url || '', data, config);
  }
  
  delete(requestConfig?: AuthAxiosRequestConfig) {
    const { url, ...config } = requestConfig || {};
    return apiClient.delete(url || '', config);
  }

  // === GET methods ===

  // Получить все сертификаты (с учетом роли пользователя)
  async getAllCertificates(): Promise<Certificate[]> {
    const response = await this.get({
      url: this.baseUrl,
      authenticated: true
    });
    return response.data;
  }

  // Получить сертификат по ID
  async getCertificateById(id: number): Promise<Certificate> {
    const response = await this.get({
      url: `${this.baseUrl}/${id}`,
      authenticated: true
    });
    return response.data;
  }

  // === POST methods - выпуск сертификатов ===

  // Выпустить корневой сертификат
  async issueRootCertificate(): Promise<Certificate> {
    const response = await this.post({
      url: `${this.baseUrl}/issue/root`,
      authenticated: true
    });
    return response.data;
  }

  // Выпустить промежуточный сертификат
  async issueIntermediateCertificate(issuerId: number, csr: CreateCSRRequest): Promise<Certificate> {
    const response = await this.post({
      url: `${this.baseUrl}/issue/intermediate/${issuerId}`,
      data: csr,
      authenticated: true
    });
    return response.data;
  }

  // Выпустить конечный сертификат
async issueEndEntityCertificate(csr: { csrContent: string }): Promise<Certificate> {

  const decodedRequestedBy = UserApi.getCurrentUser()

  // const payload = {
  //   csrContent: csr.csrContent,
  //   requestedBy: decodedRequestedBy,
  //   selectedCA: { id: 2 },
  //   status: 'PENDING'
  // };

  const payload = { csr: {...csr, requestedBy: decodedRequestedBy} } 

  const response = await this.post({
    url: `${this.baseUrl}/issue/ee/${decodedRequestedBy?.id}`,
    data: payload,
    authenticated: true
  });
  return response.data;
}

  // === POST methods - выпуск с шаблонами ===

  // Выпустить корневой сертификат с шаблоном
  async issueRootWithTemplate(templateId: number): Promise<Certificate> {
    const response = await this.post({
      url: `${this.baseUrl}/issue/root/template/${templateId}`,
      authenticated: true
    });
    return response.data;
  }

  // Выпустить промежуточный сертификат с шаблоном
  async issueIntermediateWithTemplate(templateId: number, csr: CreateCSRRequest): Promise<Certificate> {
    const response = await this.post({
      url: `${this.baseUrl}/issue/intermediate/template/${templateId}`,
      data: csr,
      authenticated: true
    });
    return response.data;
  }

  // Выпустить конечный сертификат с шаблоном
  async issueEndEntityWithTemplate(templateId: number, csr: CreateCSRRequest): Promise<Certificate> {
    const response = await this.post({
      url: `${this.baseUrl}/issue/ee/template/${templateId}`,
      data: csr,
      authenticated: true
    });
    return response.data;
  }

  // === Template operations ===

  // Создать шаблон
  async createTemplate(template: Partial<CertificateTemplate>): Promise<CertificateTemplate> {
    const response = await this.post({
      url: `${this.baseUrl}/templates`,
      data: template,
      authenticated: true
    });
    return response.data;
  }

  // Получить все шаблоны
  async getTemplates(): Promise<CertificateTemplate[]> {
    const response = await this.get({
      url: `${this.baseUrl}/templates`,
      authenticated: true
    });
    return response.data;
  }

  // Удалить шаблон
  async deleteTemplate(id: number): Promise<string> {
    const response = await this.delete({
      url: `${this.baseUrl}/templates/${id}`,
      authenticated: true
    });
    return response.data;
  }

  // === File operations ===

  // Скачать сертификат в формате PKCS12
  async downloadCertificate(id: number, password: string = 'changeit'): Promise<Blob> {
    const response = await this.get({
      url: `${this.baseUrl}/${id}/download`,
      params: { password },
      responseType: 'blob',
      authenticated: true
    });
    return response.data;
  }

  // Загрузить CSR файл для выпуска сертификата
  async uploadCSR(issuerId: number, csrFile: File): Promise<Certificate> {
    const formData = new FormData();
    formData.append('file', csrFile);

    const response = await this.post({
      url: `${this.baseUrl}/csr/upload/${issuerId}`,
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      authenticated: true
    });
    return response.data;
  }

  // === DELETE methods ===

  // Удалить сертификат
  async deleteCertificate(id: number): Promise<void> {
    await this.delete({
      url: `${this.baseUrl}/${id}`,
      authenticated: true
    });
  }

  // === Search and pagination methods ===

  // Search certificates with filters and pagination
  async searchCertificates(filters?: {
    status?: 'VALID' | 'EXPIRED' | 'REVOKED';
    type?: 'ROOT' | 'INTERMEDIATE' | 'END_ENTITY';
    organization?: string;
    page?: number;
    size?: number;
  }): Promise<{
    content: Certificate[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  }> {
    const params: any = {};
    
    if (filters?.status) params.status = filters.status;
    if (filters?.type) params.type = filters.type;
    if (filters?.organization) params.organization = filters.organization;
    if (filters?.page !== undefined) params.page = filters.page;
    if (filters?.size !== undefined) params.size = filters.size;

    const response = await this.get({
      url: `${this.baseUrl}/search`,
      params,
      authenticated: true
    });
    return response.data;
  }
}

const certificatesApi = new CertificatesApi();
export default certificatesApi;
