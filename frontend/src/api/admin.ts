import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { Role } from '../auth/session'
import { apiGet, apiSend } from './client'

// IT Admin API (backend /api/admin/**). Types mirror the backend DTOs.

export interface AdminUser {
  id: number
  username: string
  fullName: string
  role: Role
  doctorId: number | null
  doctorName: string | null
  homeBranchId: number | null
  mobile: string | null
  email: string | null
  active: boolean
  mustChangePassword: boolean
  locked: boolean
  lastLoginAt: string | null
}

export interface UserWithPassword {
  user: AdminUser
  temporaryPassword: string
}

export interface UserInput {
  username?: string
  fullName: string
  role: Role
  doctorId: number | null
  homeBranchId: number | null
  mobile: string | null
  email: string | null
  active?: boolean
}

export interface AdminDoctor {
  id: number
  fullName: string
  gender: 'M' | 'F' | 'O' | null
  specialityId: number
  specialityName: string
  qualification: string
  designation: string | null
  registrationNo: string | null
  practiceStartYear: number
  experienceYears: number
  languages: string | null
  bio: string | null
  photoUrl: string | null
  consultationFee: number | null
  active: boolean
  branchIds: number[]
}

export type DoctorInput = Omit<AdminDoctor, 'id' | 'specialityName' | 'experienceYears' | 'photoUrl'>

export interface AdminBranch {
  id: number
  code: string
  name: string
  address: string
  city: string
  district: string | null
  pincode: string | null
  phone: string | null
  emergencyPhone: string | null
  email: string | null
  mapUrl: string | null
  imageUrl: string | null
  openingHours: string | null
  active: boolean
}

export type BranchInput = Omit<AdminBranch, 'id' | 'imageUrl'>

export interface AdminSpeciality {
  id: number
  name: string
  description: string | null
  active: boolean
}

export interface AdminGalleryImage {
  id: number
  branchId: number | null
  title: string | null
  caption: string | null
  imageUrl: string
  thumbUrl: string
  sortOrder: number
  active: boolean
}

export interface GalleryInput {
  branchId: number | null
  title: string | null
  caption: string | null
  sortOrder: number
  active: boolean
}

export interface AuditEntry {
  id: number
  createdAt: string
  userId: number | null
  username: string | null
  action: string
  entityType: string | null
  entityId: string | null
  details: string | null
  ipAddress: string | null
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface AuditFilters {
  userId?: number
  action?: string
  from?: string
  to?: string
  page: number
}

/** After a change, refetch the admin list and the public data that shows it. */
function useInvalidate(...keys: string[][]) {
  const qc = useQueryClient()
  return () => Promise.all(keys.map((queryKey) => qc.invalidateQueries({ queryKey })))
}

// ---- users ----
export const useAdminUsers = () => useQuery({ queryKey: ['admin', 'users'], queryFn: () => apiGet<AdminUser[]>('/admin/users') })

export function useCreateUser() {
  const invalidate = useInvalidate(['admin', 'users'])
  return useMutation({
    mutationFn: (input: UserInput) => apiSend<UserWithPassword>('POST', '/admin/users', input),
    onSuccess: invalidate,
  })
}

export function useUpdateUser() {
  const invalidate = useInvalidate(['admin', 'users'])
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: UserInput }) => apiSend<AdminUser>('PUT', `/admin/users/${id}`, input),
    onSuccess: invalidate,
  })
}

export function useResetPassword() {
  const invalidate = useInvalidate(['admin', 'users'])
  return useMutation({
    mutationFn: (id: number) => apiSend<UserWithPassword>('POST', `/admin/users/${id}/reset-password`),
    onSuccess: invalidate,
  })
}

// ---- doctors ----
/** @param enabled false for non-admins, who may not call /admin (e.g. a doctor on the schedule page) */
export const useAdminDoctors = (enabled = true) =>
  useQuery({ queryKey: ['admin', 'doctors'], queryFn: () => apiGet<AdminDoctor[]>('/admin/doctors'), enabled })

export function useSaveDoctor() {
  const invalidate = useInvalidate(['admin', 'doctors'], ['doctors'], ['doctor'], ['branch'])
  return useMutation({
    mutationFn: ({ id, input }: { id?: number; input: DoctorInput }) =>
      id ? apiSend<AdminDoctor>('PUT', `/admin/doctors/${id}`, input) : apiSend<AdminDoctor>('POST', '/admin/doctors', input),
    onSuccess: invalidate,
  })
}

export function useDoctorPhoto() {
  const invalidate = useInvalidate(['admin', 'doctors'], ['doctors'], ['doctor'], ['branch'])
  return useMutation({
    mutationFn: ({ id, file }: { id: number; file: File | null }) => {
      if (!file) return apiSend<AdminDoctor>('DELETE', `/admin/doctors/${id}/photo`)
      const form = new FormData()
      form.append('file', file)
      return apiSend<AdminDoctor>('POST', `/admin/doctors/${id}/photo`, form)
    },
    onSuccess: invalidate,
  })
}

// ---- branches & specialities ----
export const useAdminBranches = () =>
  useQuery({ queryKey: ['admin', 'branches'], queryFn: () => apiGet<AdminBranch[]>('/admin/branches') })

export function useSaveBranch() {
  const invalidate = useInvalidate(['admin', 'branches'], ['branches'], ['branch'])
  return useMutation({
    mutationFn: ({ id, input }: { id?: number; input: BranchInput }) =>
      id ? apiSend<AdminBranch>('PUT', `/admin/branches/${id}`, input) : apiSend<AdminBranch>('POST', '/admin/branches', input),
    onSuccess: invalidate,
  })
}

export function useBranchImage() {
  const invalidate = useInvalidate(['admin', 'branches'], ['branches'], ['branch'])
  return useMutation({
    mutationFn: ({ id, file }: { id: number; file: File }) => {
      const form = new FormData()
      form.append('file', file)
      return apiSend<AdminBranch>('POST', `/admin/branches/${id}/image`, form)
    },
    onSuccess: invalidate,
  })
}

export const useAdminSpecialities = () =>
  useQuery({ queryKey: ['admin', 'specialities'], queryFn: () => apiGet<AdminSpeciality[]>('/admin/specialities') })

export function useSaveSpeciality() {
  const invalidate = useInvalidate(['admin', 'specialities'], ['specialities'])
  return useMutation({
    mutationFn: ({ id, input }: { id?: number; input: Omit<AdminSpeciality, 'id'> }) =>
      id
        ? apiSend<AdminSpeciality>('PUT', `/admin/specialities/${id}`, input)
        : apiSend<AdminSpeciality>('POST', '/admin/specialities', input),
    onSuccess: invalidate,
  })
}

// ---- gallery ----
export const useAdminGallery = () =>
  useQuery({ queryKey: ['admin', 'gallery'], queryFn: () => apiGet<AdminGalleryImage[]>('/admin/gallery') })

export function useUploadGalleryImage() {
  const invalidate = useInvalidate(['admin', 'gallery'], ['gallery'], ['branch'])
  return useMutation({
    mutationFn: ({ file, input }: { file: File; input: GalleryInput }) => {
      const form = new FormData()
      form.append('file', file)
      for (const [key, value] of Object.entries(input)) {
        if (value !== null && value !== undefined && value !== '') form.append(key, String(value))
      }
      return apiSend<AdminGalleryImage>('POST', '/admin/gallery', form)
    },
    onSuccess: invalidate,
  })
}

export function useUpdateGalleryImage() {
  const invalidate = useInvalidate(['admin', 'gallery'], ['gallery'], ['branch'])
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: GalleryInput }) =>
      apiSend<AdminGalleryImage>('PUT', `/admin/gallery/${id}`, input),
    onSuccess: invalidate,
  })
}

export function useDeleteGalleryImage() {
  const invalidate = useInvalidate(['admin', 'gallery'], ['gallery'], ['branch'])
  return useMutation({ mutationFn: (id: number) => apiSend<void>('DELETE', `/admin/gallery/${id}`), onSuccess: invalidate })
}

// ---- audit ----
export function useAuditLog(filters: AuditFilters) {
  const params = new URLSearchParams({ page: String(filters.page), size: '50' })
  if (filters.userId) params.set('userId', String(filters.userId))
  if (filters.action) params.set('action', filters.action)
  if (filters.from) params.set('from', filters.from)
  if (filters.to) params.set('to', filters.to)
  return useQuery({
    queryKey: ['admin', 'audit', filters],
    queryFn: () => apiGet<Page<AuditEntry>>(`/admin/audit-logs?${params}`),
  })
}
