import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite' 

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(), 
  ],
  server: {
    proxy: {
      '/api': {
        // všechna volání začínající na /api přesměrujeme na Spring Boot backend
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})