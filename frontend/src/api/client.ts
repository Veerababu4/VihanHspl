const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'

/** Matches the backend's single error shape (DESIGN.md §11). */
export interface ApiErrorBody {
  timestamp: string
  status: number
  code: string
  message: string
  fieldErrors: { field: string; message: string }[]
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: ApiErrorBody['fieldErrors']

  constructor(status: number, body: Partial<ApiErrorBody>) {
    super(body.message ?? 'Something went wrong. Please try again.')
    this.status = status
    this.code = body.code ?? 'UNKNOWN'
    this.fieldErrors = body.fieldErrors ?? []
  }
}

/**
 * Hooks the auth layer plugs in (see auth/AuthContext). Kept here so the client has no React
 * dependency: the access token lives in memory only, never in localStorage (DESIGN.md §12.2).
 */
interface AuthHooks {
  getAccessToken: () => string | null
  /** Gets a new access token using the refresh cookie; null if the session is over. */
  refresh: () => Promise<string | null>
}

let authHooks: AuthHooks | null = null

export function setAuthHooks(hooks: AuthHooks | null) {
  authHooks = hooks
}

// Paths where a 401 means "wrong password / session over", not "access token expired".
const NO_RETRY = ['/auth/login', '/auth/refresh', '/auth/logout']

export async function apiRequest<T>(path: string, init: RequestInit = {}, retried = false): Promise<T> {
  const token = authHooks?.getAccessToken()
  const isForm = init.body instanceof FormData
  let response: Response
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...init,
      credentials: 'same-origin',
      headers: {
        Accept: 'application/json',
        ...(init.body && !isForm ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...init.headers,
      },
    })
  } catch {
    throw new ApiError(0, { code: 'NETWORK_ERROR', message: 'Cannot reach the server. Check your connection.' })
  }

  // Access token expired (30 min): refresh once and retry.
  if (response.status === 401 && token && !retried && authHooks && !NO_RETRY.includes(path)) {
    const fresh = await authHooks.refresh()
    if (fresh) return apiRequest<T>(path, init, true)
  }

  if (!response.ok) {
    const body = (await response.json().catch(() => ({}))) as Partial<ApiErrorBody>
    throw new ApiError(response.status, body)
  }
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export const apiGet = <T,>(path: string) => apiRequest<T>(path)

export const apiSend = <T,>(method: 'POST' | 'PUT' | 'PATCH' | 'DELETE', path: string, body?: unknown) =>
  apiRequest<T>(path, {
    method,
    body: body === undefined ? undefined : body instanceof FormData ? body : JSON.stringify(body),
  })
