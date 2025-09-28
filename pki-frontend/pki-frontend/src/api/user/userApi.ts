import type { AuthAxiosRequestConfig, IApi } from "../data/apiTypes";
import apiClient from "../../interceptor/interceptor";

class UserApi implements IApi {
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
}

const userApi = new UserApi();

export default userApi;