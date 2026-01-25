import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3001,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:3000', // Change localhost to 127.0.0.1 to avoid IPv6 issues
        changeOrigin: true,
        secure: false,
      }
    }
  }
})
