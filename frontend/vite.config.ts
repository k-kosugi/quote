import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // 具体的なパスを先に書く. SKU カタログは見積とは別のサービスで動く
      '/api/skus': 'http://localhost:8000',
      '/api': 'http://localhost:8080',
    },
  },
})
