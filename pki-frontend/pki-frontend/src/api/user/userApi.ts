import type { AuthAxiosRequestConfig, IApi } from "../data/apiTypes";
import apiClient from "../../interceptor/interceptor";

class UserApi implements IApi {
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
}

const userApi = new UserApi();

export default userApi;