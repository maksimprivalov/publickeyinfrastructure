// API для работы с сертификатами
import type { AuthAxiosRequestConfig, IApi } from "../data/apiTypes";
import apiClient from "../../interceptor/interceptor";
import type { Certificate } from "../../models/certificate";
import type { CertificateSigningRequest, CreateCSRRequest } from "../../models/certificateSigningRequest";
import type { CertificateTemplate } from "../../models/certificateTemplate";
import { UserApi } from "../user/userApi";

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
  async issueIntermediateCertificate(csr: CreateCSRRequest): Promise<Certificate> {

    const decodedUser = UserApi.getCurrentUser()

    const response = await this.post({
      url: `${this.baseUrl}/issue/intermediate/${decodedUser?.id}`,
      data: csr,
      authenticated: true
    });
    return response.data;
  }

  // Выпустить конечный сертификат
  async issueEndEntityCertificate(csr: CreateCSRRequest): Promise<Certificate> {

    const decodedUser = UserApi.getCurrentUser()

    const response = await this.post({
      url: `${this.baseUrl}/issue/ee/${decodedUser?.id}`,
      data: csr,
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
}

const certificatesApi = new CertificatesApi();
export default certificatesApi;
