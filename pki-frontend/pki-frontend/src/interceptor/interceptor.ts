import axios from "axios";
import { history } from "../services/history";

const apiClient = axios.create({
  baseURL: "http://localhost:8080",
  headers: {
    "Content-Type": "application/json"
  }
});

apiClient.interceptors.request.use(
  (config) => {
    if ((config as any).authenticated) {
      const accessToken = localStorage.getItem('access_token');
      if (accessToken) {
        config.headers = config.headers || {};
        config.headers['Authorization'] = `Bearer ${accessToken}`;
      } 
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (
      error.response &&
      error.response.status === 403 &&
      (originalRequest as any).authenticated &&
      !originalRequest._retry
    ) {
      originalRequest._retry = true;
      try {
        const refreshResponse = await apiClient.post('api/users/refresh');
        const newAccessToken = refreshResponse.data?.accessToken;
        if (newAccessToken) {
          localStorage.setItem('access_token', newAccessToken);
          originalRequest.headers = originalRequest.headers || {};
          originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;
          return apiClient(originalRequest);
        }
      } catch (refreshError: any) {
        // If refresh fails (refresh token expired), redirect to login
        history.navigate("/auth");
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;

