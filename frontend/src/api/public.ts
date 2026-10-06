import { useQuery } from '@tanstack/react-query'

import { apiGet } from './client'

export interface AppInfo {
  name: string
  /** true when the backend serves fictional sample data (local profile) */
  sampleData: boolean
}

export interface Branch {
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
}

export const useAppInfo = () =>
  useQuery({ queryKey: ['app-info'], queryFn: () => apiGet<AppInfo>('/public/app-info'), staleTime: Infinity })

export const useBranches = () =>
  useQuery({ queryKey: ['branches'], queryFn: () => apiGet<Branch[]>('/public/branches'), staleTime: 5 * 60_000 })
