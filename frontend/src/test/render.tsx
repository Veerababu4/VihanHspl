import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter, RouterProvider, type RouteObject } from 'react-router'
import { vi } from 'vitest'

/** Renders routes with a fresh query cache (no retries) at the given URL. */
export function renderRoutes(routes: RouteObject[], url = '/') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(routes, { initialEntries: [url] })
  const view = render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return { ...view, router }
}

type MockValue = unknown | Response | ((init: RequestInit | undefined) => unknown)

/** JSON response helper for mocks. */
export const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

/**
 * Stubs fetch. Keys are API paths without /api, optionally prefixed by a method
 * ("POST /auth/login"). Values are a JSON body, a Response, or a function returning either
 * (called on every request, so it can change answers between calls; may be async).
 */
export function mockApi(responses: Record<string, MockValue>) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const path = String(input).replace(/^\/api/, '')
    const method = (init?.method ?? 'GET').toUpperCase()
    let value = `${method} ${path}` in responses ? responses[`${method} ${path}`] : responses[path]
    if (typeof value === 'function') value = await (value as (i: RequestInit | undefined) => unknown)(init)
    if (value instanceof Response) return value
    if (value === undefined) return json({ code: 'NOT_FOUND', message: `not mocked: ${method} ${path}` }, 404)
    return json(value)
  })
}

/** Calls to a path, optionally "METHOD /path" (GET is the default method). */
function callsTo(spy: ReturnType<typeof mockApi>, key: string) {
  const [method, path] = key.includes(' ') ? key.split(' ') : [undefined, key]
  return spy.mock.calls.filter(
    ([url, init]) => String(url) === `/api${path}` && (!method || (init?.method ?? 'GET').toUpperCase() === method),
  )
}

/** Reads the JSON body of the n-th call, e.g. requestBody(spy, 'POST /admin/users'). */
export function requestBody(spy: ReturnType<typeof mockApi>, key: string, nth = 0) {
  const call = callsTo(spy, key)[nth]
  return call ? JSON.parse(String(call[1]?.body)) : undefined
}

/** The Authorization header sent on the n-th call. */
export function authHeader(spy: ReturnType<typeof mockApi>, key: string, nth = 0) {
  const call = callsTo(spy, key)[nth]
  return (call?.[1]?.headers as Record<string, string> | undefined)?.Authorization
}
