import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { AppointmentStatus } from './booking'
import { apiGet, apiSend } from './client'
import type { PatientSummary } from './reception'

// Patients API (backend PatientDtos, DESIGN.md §11.5). Aadhaar is only ever sent in request
// bodies and only ever comes back masked (aadhaarMasked = "XXXX-XXXX-1234").

export type BloodGroup = 'A+' | 'A-' | 'B+' | 'B-' | 'AB+' | 'AB-' | 'O+' | 'O-'
export const BLOOD_GROUPS: BloodGroup[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']

export interface PatientDetail {
  id: number
  uhid: string
  fullName: string
  age: number
  dateOfBirth: string
  dobEstimated: boolean
  gender: 'M' | 'F' | 'O'
  mobile: string
  altMobile: string | null
  aadhaarMasked: string | null
  address: string | null
  city: string | null
  bloodGroup: BloodGroup | null
  emergencyName: string | null
  emergencyPhone: string | null
  registeredBranchCode: string | null
  registeredBranchCity: string | null
  registeredAt: string
  upcoming: {
    id: number
    bookingRef: string
    date: string
    startAt: string | null
    tokenNo: number | null
    doctorName: string | null
    branchCity: string | null
    status: AppointmentStatus
  }[]
}

export interface VisitRow {
  id: number
  date: string
  branchCity: string | null
  doctorName: string | null
  speciality: string | null
  type: 'OP' | 'FOLLOW_UP' | 'EMERGENCY'
  status: 'OPEN' | 'COMPLETED'
}

/** Optional details shared by registration and edit (FR-PT1). */
export interface PatientExtras {
  aadhaar: string | null
  altMobile: string | null
  address: string | null
  city: string | null
  bloodGroup: BloodGroup | null
  emergencyName: string | null
  emergencyPhone: string | null
}

export interface UpdatePatientInput extends PatientExtras {
  fullName: string
  age: number | null
  dateOfBirth: string | null
  gender: 'M' | 'F' | 'O'
  mobile: string
  /** aadhaar empty = keep the stored number; removeAadhaar deletes it */
  removeAadhaar: boolean
  confirmDuplicate: boolean
}

export interface DuplicateCheck {
  aadhaarMatch: PatientSummary | null
  similar: PatientSummary[]
}

export type SearchBy = 'name' | 'mobile' | 'uhid' | 'aadhaar'

export const searchPatients = (by: SearchBy, text: string) =>
  by === 'aadhaar'
    ? apiSend<PatientSummary[]>('POST', '/staff/patients/search-aadhaar', { aadhaar: text })
    : apiGet<PatientSummary[]>(`/staff/patients?${by}=${encodeURIComponent(text)}`)

export const checkDuplicates = (input: { name: string; mobile: string; aadhaar: string | null; excludeId?: number }) =>
  apiSend<DuplicateCheck>('POST', '/staff/patients/duplicates', input)

export const usePatient = (id: number) =>
  useQuery({ queryKey: ['patient', id], queryFn: () => apiGet<PatientDetail>(`/staff/patients/${id}`) })

export const usePatientVisits = (id: number) =>
  useQuery({ queryKey: ['patient-visits', id], queryFn: () => apiGet<VisitRow[]>(`/staff/patients/${id}/visits`) })

export const useUpdatePatient = (id: number) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: UpdatePatientInput) => apiSend<PatientDetail>('PUT', `/staff/patients/${id}`, input),
    onSuccess: (patient) => queryClient.setQueryData(['patient', id], patient),
  })
}

/** Same check as the backend (Aadhaar.java): 12 digits, first 2-9, Verhoeff check digit. */
export function normalizeAadhaar(input: string): string | null {
  const digits = input.replace(/[\s-]/g, '')
  if (!/^[2-9]\d{11}$/.test(digits)) return null
  let c = 0
  for (let i = 0; i < digits.length; i++) {
    c = VERHOEFF_D[c][VERHOEFF_P[i % 8][Number(digits[digits.length - 1 - i])]]
  }
  return c === 0 ? digits : null
}

const VERHOEFF_D = [
  [0, 1, 2, 3, 4, 5, 6, 7, 8, 9], [1, 2, 3, 4, 0, 6, 7, 8, 9, 5], [2, 3, 4, 0, 1, 7, 8, 9, 5, 6],
  [3, 4, 0, 1, 2, 8, 9, 5, 6, 7], [4, 0, 1, 2, 3, 9, 5, 6, 7, 8], [5, 9, 8, 7, 6, 0, 4, 3, 2, 1],
  [6, 5, 9, 8, 7, 1, 0, 4, 3, 2], [7, 6, 5, 9, 8, 2, 1, 0, 4, 3], [8, 7, 6, 5, 9, 3, 2, 1, 0, 4],
  [9, 8, 7, 6, 5, 4, 3, 2, 1, 0],
]
const VERHOEFF_P = [
  [0, 1, 2, 3, 4, 5, 6, 7, 8, 9], [1, 5, 7, 6, 2, 8, 3, 0, 9, 4], [5, 8, 0, 3, 7, 9, 6, 1, 4, 2],
  [8, 9, 1, 6, 0, 4, 3, 5, 2, 7], [9, 4, 5, 3, 1, 2, 6, 8, 7, 0], [4, 2, 8, 6, 5, 7, 3, 9, 0, 1],
  [2, 7, 9, 3, 8, 0, 6, 4, 1, 5], [7, 0, 4, 6, 9, 1, 3, 2, 5, 8],
]
