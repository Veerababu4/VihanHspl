import './i18n'
import './index.css'

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router'
import { z } from 'zod'

import { router } from './routes'

// The site's Content-Security-Policy forbids eval (security-headers.ts); without this, Zod
// probes it with new Function() and the browser reports a violation.
z.config({ jitless: true })

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
)
