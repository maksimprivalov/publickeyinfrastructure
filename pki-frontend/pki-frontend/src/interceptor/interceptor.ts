import axios from "axios";

// Create an axios instance
const apiClient = axios.create();

// Add a request interceptor
apiClient.interceptors.request.use(
  (config) => {
    // Check for custom 'authenticated' property
    if ((config as any).authenticated) {
      // Retrieve access token (replace with your actual token retrieval logic)
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

export default apiClient;

