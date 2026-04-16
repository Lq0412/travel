import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return
          }

          if (id.includes('ant-design-vue') || id.includes('@ant-design/icons-vue')) {
            return 'vendor-antd'
          }

          if (id.includes('vue-router') || id.includes('pinia') || id.includes('/vue/')) {
            return 'vendor-vue'
          }

          if (id.includes('marked')) {
            return 'vendor-markdown'
          }

          return 'vendor'
        },
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    allowedHosts: ['192.168.1.100', 'localhost'],
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        ws: true,
        rewrite: (path) => path.replace(/^\/api/, '/api'),
      },
    },
  },

})
