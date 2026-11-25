import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // El navegador ve frontend y API en el mismo origen (localhost:5173),
    // así la cookie de sesión JSESSIONID funciona sin configurar CORS.
    proxy: { '/api': 'http://localhost:8080' },
  },
})
