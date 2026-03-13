import axios from 'axios';
const request = axios.create({
  // 使用相对路径，通过 Vite 代理转发到后端
  // 代理配置在 vite.config.js 中：/api -> http://localhost:8080
  baseURL: '/api',
  timeout: 10000
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('uc_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('uc_token');
      localStorage.removeItem('uc_user');
      localStorage.removeItem('uc_remember');
      window.location.href = '/login';
    }
    // 统一错误处理，返回错误信息
    const errorMessage = error.response?.data?.message || error.message || '请求失败';
    return Promise.reject(new Error(errorMessage));
  }
);

export default request;
