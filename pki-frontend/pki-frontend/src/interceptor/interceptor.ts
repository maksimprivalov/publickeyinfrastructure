import axios from "axios";

const apiClient = axios.create({
  baseURL: "https://localhost:8443",
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
        config.headers['Token'] = `Bearer ${accessToken}`;
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
      (error.response.status === 401 || error.response.status === 403) &&
      (originalRequest as any).authenticated &&
      !originalRequest._retry
    ) {
      originalRequest._retry = true;
      try {
        const refreshToken = localStorage.getItem('refresh_token');
        if (!refreshToken) {
          // No refresh token, clear tokens and let components handle redirect
          localStorage.removeItem('access_token');
          localStorage.removeItem('refresh_token');
          window.dispatchEvent(new CustomEvent('tokenChanged'));
          return Promise.reject(new Error('No refresh token available'));
        }

        const refreshResponse = await apiClient.post(`/api/users/refresh?refreshToken=${refreshToken}`);
        
        const newAccessToken = refreshResponse.data?.accessToken;
        if (newAccessToken) {
          localStorage.setItem('access_token', newAccessToken);
          window.dispatchEvent(new CustomEvent('tokenChanged'));
          originalRequest.headers = originalRequest.headers || {};
          originalRequest.headers['Token'] = `Bearer ${newAccessToken}`;
          return apiClient(originalRequest);
        }
      } catch (refreshError: any) {
        // If refresh fails (refresh token expired), clear tokens and let components handle redirect
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        window.dispatchEvent(new CustomEvent('tokenChanged'));
        return Promise.reject(new Error('Token refresh failed'));
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;

