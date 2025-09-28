// API для работы с шаблонами сертификатов
import type { AuthAxiosRequestConfig, IApi } from "../data/apiTypes";
import apiClient from "../../interceptor/interceptor";
import type { CertificateTemplate } from "../../models/certificateTemplate";

class TemplatesApi implements IApi {
  private baseUrl = "/api/certificates/templates";

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

  // Получить все шаблоны
  async getAllTemplates(): Promise<CertificateTemplate[]> {
    const response = await this.get({
      url: this.baseUrl,
      authenticated: true
    });
    return response.data;
  }

  // Создать новый шаблон
  async createTemplate(template: Partial<CertificateTemplate>): Promise<CertificateTemplate> {
    const response = await this.post({
      url: this.baseUrl,
      data: template,
      authenticated: true
    });
    return response.data;
  }

  // Удалить шаблон
  async deleteTemplate(id: number): Promise<string> {
    const response = await this.delete({
      url: `${this.baseUrl}/${id}`,
      authenticated: true
    });
    return response.data;
  }
}

const templatesApi = new TemplatesApi();
export default templatesApi;
