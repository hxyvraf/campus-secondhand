import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发态：前端跑在 5173，/api 与 /uploads 代理到后端 8080，避免跨域问题
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    strictPort: false,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500
  }
})
