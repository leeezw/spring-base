import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true
      }
    }
  },
  build: {
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        manualChunks: {
          // React core
          'vendor-react': ['react', 'react-dom', 'react-router-dom'],
          // Ant Design
          'vendor-antd': ['antd', '@ant-design/icons'],
          // Charts (largest dep)
          'vendor-charts': ['@ant-design/charts'],
          // Pro components
          'vendor-pro': ['@ant-design/pro-components'],
        }
      }
    }
  }
});
