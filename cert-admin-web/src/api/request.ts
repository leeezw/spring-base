import axios from 'axios';
import { message } from 'antd';
import type { ResultWrapper } from '../types';

const request = axios.create({
  baseURL: '',
  timeout: 300000, // 签发/部署可能较久
});

export const TOKEN_KEY = 'cert_admin_token';

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    const body = response.data as ResultWrapper<unknown>;
    if (body && typeof body.code === 'number') {
      if (body.code === 200) {
        return body.data as never;
      }
      // 鉴权失败跳登录
      if (body.code === 401 || body.code === 6001 || body.code === 6002 || body.code === 6003) {
        localStorage.removeItem(TOKEN_KEY);
        if (location.hash !== '#/login') {
          location.hash = '#/login';
        }
      }
      message.error(body.message || '请求失败');
      return Promise.reject(new Error(body.message || '请求失败'));
    }
    return response.data as never;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      location.hash = '#/login';
    }
    message.error(error.response?.data?.message || error.message || '网络错误');
    return Promise.reject(error);
  }
);

export default request;
