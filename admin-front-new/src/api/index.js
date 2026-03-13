import axios from 'axios';

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
});

// 请求拦截器
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('uc_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器
request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    // 网络错误 / 超时
    if (!error.response) {
      return Promise.reject(new Error('网络连接失败，请检查网络'));
    }

    const { status, data } = error.response;

    // 401 未认证 → 跳登录
    if (status === 401) {
      localStorage.removeItem('uc_token');
      localStorage.removeItem('uc_user');
      localStorage.removeItem('uc_remember');
      window.location.href = '/login';
      return Promise.reject(new Error('登录已过期'));
    }

    // 403 无权限
    if (status === 403) {
      return Promise.reject(new Error('没有操作权限'));
    }

    // 后端返回了标准 Result 格式（code + message），当成正常响应返回
    if (data && data.code !== undefined) {
      return data;
    }

    // 其他错误
    const errorMessage = data?.message || error.message || '请求失败';
    return Promise.reject(new Error(errorMessage));
  }
);

export default request;
