import type { AxiosRequestConfig } from "axios";

export interface AuthAxiosRequestConfig extends AxiosRequestConfig {
    authenticated?: boolean;
}

export interface IApi {
    get: (requestConfig?: AuthAxiosRequestConfig) => Promise<any>;
    post: (requestConfig?: AuthAxiosRequestConfig) => Promise<any>;
    put: (requestConfig?: AuthAxiosRequestConfig) => Promise<any>;
    delete: (requestConfig?: AuthAxiosRequestConfig) => Promise<any>;
}
