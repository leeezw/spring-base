import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// 开发态把 /api 代理到后端 Spring Boot 服务（默认 8090 端口）
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true,
      },
    },
  },
});
