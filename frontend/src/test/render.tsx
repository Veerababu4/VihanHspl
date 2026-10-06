import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter, RouterProvider, type RouteObject } from 'react-router'
import { vi } from 'vitest'

/** Renders routes with a fresh query cache (no retries) at the given URL. */
export function renderRoutes(routes: RouteObject[], url = '/') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(routes, { initialEntries: [url] })
  return render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

/** Stubs fetch: each key is an /api path, value is the JSON body (or a Response for errors). */
export function mockApi(responses: Record<string, unknown>) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
    const path = String(input).replace(/^\/api/, '')
    const body = responses[path]
    if (body instanceof Response) return body
    if (body === undefined) return new Response(JSON.stringify({ code: 'NOT_FOUND', message: 'not mocked' }), { status: 404 })
    return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
  })
}
