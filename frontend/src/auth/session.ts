import { createContext, useContext } from 'react'

export type Role = 'DOCTOR' | 'RECEPTIONIST' | 'IT_ADMIN'

export interface Me {
  id: number
  username: string
  fullName: string
  role: Role
  doctorId: number | null
  homeBranchId: number | null
  mustChangePassword: boolean
}

export interface SessionDto {
  accessToken: string
  expiresAt: string
  user: Me
}

export type Status = 'checking' | 'authenticated' | 'anonymous'

export interface AuthState {
  status: Status
  user: Me | null
  /** true after the user pressed "Log out" (vs. a session that expired) */
  loggedOut: boolean
  login: (username: string, password: string) => Promise<Me>
  logout: () => Promise<void>
  changePassword: (currentPassword: string, newPassword: string) => Promise<void>
}

export const AuthContext = createContext<AuthState | null>(null)

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}
