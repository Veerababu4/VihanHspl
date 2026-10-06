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

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...init,
      headers: { Accept: 'application/json', ...(init?.body ? { 'Content-Type': 'application/json' } : {}), ...init?.headers },
    })
  } catch {
    throw new ApiError(0, { code: 'NETWORK_ERROR', message: 'Cannot reach the server. Check your connection.' })
  }
  if (!response.ok) {
    const body = (await response.json().catch(() => ({}))) as Partial<ApiErrorBody>
    throw new ApiError(response.status, body)
  }
  return (await response.json()) as T
}

export const apiGet = <T,>(path: string) => apiRequest<T>(path)
