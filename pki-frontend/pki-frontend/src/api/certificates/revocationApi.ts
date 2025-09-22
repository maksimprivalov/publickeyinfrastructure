// API для отзыва сертификатов
import type { AuthAxiosRequestConfig, IApi } from "../data/apiTypes";
import apiClient from "../../interceptor/interceptor";
import type { RevokedCertificate } from "../../models/revokedCertificate";

class RevocationApi implements IApi {
  private baseUrl = "/api/revocations";

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
