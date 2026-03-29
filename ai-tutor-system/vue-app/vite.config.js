import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const isProduction = process.env.NODE_ENV === 'production'

export default defineConfig({
  plugins: [
    vue()
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 3000,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: isProduction ? 'http://localhost:5000' : 'http://localhost:5000',
        changeOrigin: true,
        secure: false
      },
      '/ws': {
        target: isProduction ? 'http://localhost:5000' : 'ws://localhost:5000',
        ws: true,
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'highlight': ['highlight.js'],
          'markdown': ['marked']
        }
      }
    }
  }
})
