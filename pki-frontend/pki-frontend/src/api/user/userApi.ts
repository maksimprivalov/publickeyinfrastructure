import type { AuthAxiosRequestConfig, IApi } from "../data/apiTypes";
import apiClient from "../../interceptor/interceptor";
import { jwtDecode } from "jwt-decode";

// User authentication DTOs
export interface RegistrationRequest {
  email: string;
  password: string;
  name: string;
  surname: string;
  organizationName: string;
  role: 'USER' | 'CAUSER' | 'ADMIN';
}

export interface JwtPayload {
  iss: string;
  sub: string;        // обычно содержит userId
  aud: string;
  iat: number;
  exp: number;
  role: 'USER' | 'CAUSER' | 'ADMIN';
  userId: number;     // поле с ID пользователя
  type: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}

export class UserApi implements IApi {
    private baseUrl = "/api/users";

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

    // === Authentication methods ===

    // Register a new user
    async register(userData: RegistrationRequest): Promise<string> {
        const response = await this.post({
            url: `${this.baseUrl}/register`,
            data: userData,
            authenticated: false
        });
        return response.data;
    }

    // Activate user account
    async activate(token: string): Promise<string> {
        const response = await this.get({
            url: `${this.baseUrl}/activate`,
            params: { token },
            authenticated: false
        });
        return response.data;
    }

    // Login user
    async login(credentials: LoginRequest): Promise<TokenResponse> {
        const response = await this.post({
            url: `${this.baseUrl}/login`,
            data: credentials,
            authenticated: false
        });
        return response.data;
    }

    // Refresh access token
    async refresh(refreshToken: string): Promise<TokenResponse> {
        const response = await this.post({
            url: `${this.baseUrl}/refresh`,
            params: { refreshToken },
            authenticated: false
        });
        return response.data;
    }

    // Logout user
    async logout(): Promise<string> {
        const response = await this.post({
            url: `${this.baseUrl}/logout`,
            authenticated: true
        });
        return response.data;
    }

    static getCurrentUser(): { id: number; email: string } | null {
        try {
            const token = localStorage.getItem("access_token");
            if (!token) return null;

            const decoded: JwtPayload = jwtDecode(token);

            return {
                id: decoded.userId, // fallback на sub, если userId нет
                email: decoded.sub
            };
        } catch (err) {
            console.error("Failed to decode JWT token:", err);
            return null;
        }
    }
}

const userApi = new UserApi();

export default userApi;