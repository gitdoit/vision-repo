import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import VueDevTools from 'vite-plugin-vue-devtools'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue(), tailwindcss(), VueDevTools()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    proxy: {
      '/api/v1': {
        target: 'http://localhost:26330',
        changeOrigin: true,
      },
    },
  },
})
