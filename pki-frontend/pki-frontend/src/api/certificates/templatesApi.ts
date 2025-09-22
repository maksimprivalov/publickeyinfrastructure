// API для работы с шаблонами сертификатов
import type { AuthAxiosRequestConfig, IApi } from "../data/apiTypes";
import apiClient from "../../interceptor/interceptor";
import type { CertificateTemplate } from "../../models/certificateTemplate";

class TemplatesApi implements IApi {
  private baseUrl = "/api/certificates/templates";

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
