/// <reference types="vitest/config" />
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

import { websiteSecurityHeaders } from './security-headers.ts'

const backend = 'http://localhost:8080'
// Same-origin /api and /files, like Caddy in production (DESIGN.md §8, §16). The Host header
// is kept (Vite's shorthand would rewrite it), so the backend sees same-origin requests, not CORS.
const target = { target: backend, changeOrigin: false }
const proxy = { '/api': target, '/files': target }

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy,
  },
  // `npm run build && npm run preview`: the production build with the production headers.
  preview: {
    port: 4173,
    proxy,
    headers: websiteSecurityHeaders,
  },
  test: {
    environment: 'jsdom',
    pool: 'threads', // the default 'forks' pool times out starting workers on Windows
    testTimeout: 20000, // multi-step flows with lazy-loaded pages; each wait is still capped at 5 s
    setupFiles: ['./src/test/setup.ts'],
    // Tests never load Cloudflare's widget, whatever a developer has in .env.local.
    env: { VITE_TURNSTILE_SITE_KEY: '' },
    css: false,
    exclude: ['e2e/**', 'node_modules/**'],   // Playwright runs e2e/ (npm run e2e)
  },
})
