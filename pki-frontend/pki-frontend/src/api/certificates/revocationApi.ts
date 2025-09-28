// API для отзыва сертификатов
import type { AuthAxiosRequestConfig, IApi } from "../data/apiTypes";
import apiClient from "../../interceptor/interceptor";
import type { RevokedCertificate } from "../../models/revokedCertificate";

class RevocationApi implements IApi {
  private baseUrl = "/api/revocations";

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

  // Отозвать сертификат
  async revokeCertificate(certificateId: number, reason: string): Promise<string> {
    const response = await this.post({
      url: `${this.baseUrl}/${certificateId}/revoke`,
      params: { reason },
      authenticated: true
    });
    return response.data;
  }

  // Получить список отозванных сертификатов
  async getRevokedCertificates(): Promise<RevokedCertificate[]> {
    const response = await this.get({
      url: this.baseUrl,
      authenticated: true
    });
    return response.data;
  }

  // Скачать список отозванных сертификатов (CRL)
  async downloadCRL(issuerId: number): Promise<Blob> {
    const response = await this.get({
      url: `${this.baseUrl}/crl`,
      params: { issuerId },
      responseType: 'blob',
      authenticated: true
    });
    return response.data;
  }
}

const revocationApi = new RevocationApi();
export default revocationApi;
