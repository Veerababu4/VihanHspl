import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { apiGet, apiSend } from './client'

// Doctor-only clinical API (backend ClinicalDtos, DESIGN.md §11.6). Only doctors' screens call it.

export type VisitStatus = 'OPEN' | 'COMPLETED'
export type VisitType = 'OP' | 'FOLLOW_UP' | 'EMERGENCY'

export interface QueueEntry {
  appointmentId: number
  visitId: number | null
  tokenNo: number | null
  startAt: string | null
  checkedInAt: string | null
  channel: 'ONLINE' | 'COUNTER' | 'PHONE' | 'WALK_IN'
  patientId: number | null
  uhid: string | null
  patientName: string
  age: number | null
  gender: 'M' | 'F' | 'O' | null
  reason: string | null
  branchCity: string | null
  visitStatus: VisitStatus | null
  hasAllergies: boolean
}

export interface Queue {
  date: string
  waiting: QueueEntry[]
  done: QueueEntry[]
}

export interface ClinicalNote {
  bpSystolic: number | null
  bpDiastolic: number | null
  pulse: number | null
  temperatureF: number | null
  spo2: number | null
  weightKg: number | null
  heightCm: number | null
  chiefComplaint: string | null
  diagnosis: string | null
  clinicalNotes: string | null
  advice: string | null
  followUpDate: string | null
}

export interface RxItem {
  medicineName: string
  dosage: string | null
  frequency: string | null
  duration: string | null
  instructions: string | null
}

export interface PatientHeader {
  id: number
  uhid: string
  fullName: string
  age: number
  gender: 'M' | 'F' | 'O'
  mobile: string
}

export interface VisitDetail {
  id: number
  date: string
  type: VisitType
  status: VisitStatus
  /** the logged-in doctor treats this visit and it is still open */
  canEdit: boolean
  patient: PatientHeader
  allergies: string | null
  doctor: { id: number; fullName: string; speciality: string; qualification: string | null; registrationNo: string | null }
  branch: { code: string; name: string; city: string; address: string | null; phone: string | null } | null
  tokenNo: number | null
  bookingReason: string | null
  clinical: ClinicalNote
  prescription: RxItem[]
}

export type SaveVisitInput = ClinicalNote & { prescription: RxItem[] }

export interface MedicalProfile {
  allergies: string | null
  chronicConditions: string | null
  pastSurgeries: string | null
  currentMedications: string | null
  familyHistory: string | null
  updatedAt: string | null
}

export interface HistoryVisit {
  id: number
  date: string
  type: VisitType
  status: VisitStatus
  doctorName: string | null
  speciality: string | null
  branchCity: string | null
  clinical: ClinicalNote
  prescription: RxItem[]
}

export interface History {
  patient: PatientHeader
  medicalProfile: MedicalProfile
  visits: HistoryVisit[]
}

export const useQueue = (date: string) =>
  useQuery({ queryKey: ['queue', date], queryFn: () => apiGet<Queue>(`/clinical/queue?date=${date}`), refetchInterval: 60_000 })

export const useVisit = (id: number) =>
  useQuery({ queryKey: ['visit', id], queryFn: () => apiGet<VisitDetail>(`/clinical/visits/${id}`) })

export const useHistory = (patientId: number) =>
  useQuery({ queryKey: ['history', patientId], queryFn: () => apiGet<History>(`/clinical/patients/${patientId}/history`) })

export function useSaveVisit(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: SaveVisitInput) => apiSend<VisitDetail>('PUT', `/clinical/visits/${id}`, input),
    onSuccess: (visit) => queryClient.setQueryData(['visit', id], visit),
  })
}

export function useCompleteVisit(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => apiSend<VisitDetail>('POST', `/clinical/visits/${id}/complete`),
    onSuccess: async (visit) => {
      queryClient.setQueryData(['visit', id], visit)
      await queryClient.invalidateQueries({ queryKey: ['queue'] })
    },
  })
}

export function useSaveMedicalProfile(patientId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: Omit<MedicalProfile, 'updatedAt'>) =>
      apiSend<MedicalProfile>('PUT', `/clinical/patients/${patientId}/medical-profile`, input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['history', patientId] }),
  })
}
