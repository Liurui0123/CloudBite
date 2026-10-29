import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端地址可通过环境变量切换：
//   默认      → 单体版（8080）
//   PROXY_TARGET=http://127.0.0.1:18080 → 微服务网关（18080）
//   PORT=5174 → 第二个实例，用于同时预览微服务版
const target = process.env.PROXY_TARGET || 'http://localhost:8080'
const port = Number(process.env.PORT || 5173)

export default defineConfig({
  plugins: [vue()],
  server: {
    port,
    proxy: {
      '/api': {
        target,
        changeOrigin: true
      },
      '/uploads': {
        target,
        changeOrigin: true
      }
    }
  }
})
