import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import fs from 'fs'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    https: {
      key: fs.readFileSync(path.resolve(__dirname, '../../pki-backend/src/main/resources/server.key')),
      cert: fs.readFileSync(path.resolve(__dirname, '../../pki-backend/src/main/resources/server.crt')),
      passphrase: '1234',
    },
    proxy: {
      '/api': {
        target: 'https://localhost:8443',
        changeOrigin: true,
        secure: false, // разрешаем self-signed сертификат
      },
    },
  },
})