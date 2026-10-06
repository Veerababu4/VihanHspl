/// <reference types="vitest/config" />
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

const backend = 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // Same-origin /api in dev, like the Vercel rewrite in production (DESIGN.md §8).
    proxy: {
      '/api': backend,
      '/files': backend,
    },
  },
  test: {
    environment: 'jsdom',
    pool: 'threads', // the default 'forks' pool times out starting workers on Windows
    testTimeout: 20000, // multi-step flows with lazy-loaded pages; each wait is still capped at 5 s
    setupFiles: ['./src/test/setup.ts'],
    // Tests never load Cloudflare's widget, whatever a developer has in .env.local.
    env: { VITE_TURNSTILE_SITE_KEY: '' },
    css: false,
  },
})
