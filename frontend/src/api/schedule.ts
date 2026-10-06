import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { apiGet, apiSend } from './client'

// Scheduling API (backend /api/staff/..., DESIGN.md §11.3). Types mirror ScheduleDtos.java.

export interface Schedule {
  id: number
  doctorId: number
  branchId: number
  branchCode: string | null
  branchCity: string | null
  /** ISO: 1 = Monday … 7 = Sunday */
  dayOfWeek: number
  startTime: string
  endTime: string
  slotMinutes: number
  effectiveFrom: string
  effectiveTo: string | null
  slotsPerSession: number
}

export interface ScheduleInput {
  branchId: number
  dayOfWeek: number
  startTime: string
  endTime: string
  slotMinutes: number
  effectiveFrom: string
  effectiveTo: string | null
}

export type SlotStatus = 'OPEN' | 'BOOKED' | 'BLOCKED'
export type BlockReason = 'MANUAL' | 'LEAVE' | 'HOLIDAY' | 'REMOVED'

export interface Slot {
  id: number
  branchId: number
  branchCode: string | null
  date: string
  startAt: string
  endAt: string
  tokenNo: number
  status: SlotStatus
  source: 'TEMPLATE' | 'MANUAL'
  blockReason: BlockReason | null
}

export interface AffectedBooking {
  bookingRef: string
  patientName: string
  mobile: string
  startAt: string | null
  tokenNo: number | null
  branchCity: string | null
}

/** What a change did to the slots; shown after every save. */
export interface ChangeSummary {
  slotsCreated: number
  slotsRemoved: number
  slotsBlocked: number
  slotsReopened: number
  affectedBookings: AffectedBooking[]
}

export interface WithChanges<T> {
  item: T
  changes: ChangeSummary
}

export interface Leave {
  id: number
  doctorId: number
  fromDate: string
  toDate: string
  reason: string | null
}

export interface Holiday {
  id: number
  branchId: number | null
  branchCode: string | null
  date: string
  name: string
}

export interface ManualSessionInput {
  branchId: number
  date: string
  startTime: string
  endTime: string
  slotMinutes: number
}

/** Every change can affect slots, schedules, leave and the public timings, so refresh all of them. */
function useInvalidateSchedule() {
  const qc = useQueryClient()
  return () =>
    Promise.all(
      [['schedules'], ['slots'], ['leaves'], ['holidays'], ['doctor']].map((queryKey) => qc.invalidateQueries({ queryKey })),
    )
}

export const useSchedules = (doctorId: number | undefined) =>
  useQuery({
    queryKey: ['schedules', doctorId],
    queryFn: () => apiGet<Schedule[]>(`/staff/doctors/${doctorId}/schedules`),
    enabled: !!doctorId,
  })

export const useSlots = (doctorId: number | undefined, from: string, to: string) =>
  useQuery({
    queryKey: ['slots', doctorId, from, to],
    queryFn: () => apiGet<Slot[]>(`/staff/doctors/${doctorId}/slots?from=${from}&to=${to}`),
    enabled: !!doctorId,
  })

export const useLeaves = (doctorId: number | undefined) =>
  useQuery({
    queryKey: ['leaves', doctorId],
    queryFn: () => apiGet<Leave[]>(`/staff/doctors/${doctorId}/leaves`),
    enabled: !!doctorId,
  })

export const useHolidays = () => useQuery({ queryKey: ['holidays'], queryFn: () => apiGet<Holiday[]>('/staff/holidays') })

export function useSaveSchedule(doctorId: number) {
  const invalidate = useInvalidateSchedule()
  return useMutation({
    mutationFn: ({ id, input }: { id?: number; input: ScheduleInput }) =>
      id
        ? apiSend<WithChanges<Schedule>>('PUT', `/staff/schedules/${id}`, input)
        : apiSend<WithChanges<Schedule>>('POST', `/staff/doctors/${doctorId}/schedules`, input),
    onSuccess: invalidate,
  })
}

export function useDeleteSchedule() {
  const invalidate = useInvalidateSchedule()
  return useMutation({ mutationFn: (id: number) => apiSend<ChangeSummary>('DELETE', `/staff/schedules/${id}`), onSuccess: invalidate })
}

export function useGenerateSlots(doctorId: number) {
  const invalidate = useInvalidateSchedule()
  return useMutation({
    mutationFn: (range: { from: string; to: string }) =>
      apiSend<ChangeSummary>('POST', `/staff/doctors/${doctorId}/slots/generate`, range),
    onSuccess: invalidate,
  })
}

export function useAddSession(doctorId: number) {
  const invalidate = useInvalidateSchedule()
  return useMutation({
    mutationFn: (input: ManualSessionInput) => apiSend<Slot[]>('POST', `/staff/doctors/${doctorId}/slots`, input),
    onSuccess: invalidate,
  })
}

export function useSetSlotStatus() {
  const invalidate = useInvalidateSchedule()
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: 'OPEN' | 'BLOCKED' }) =>
      apiSend<Slot>('PATCH', `/staff/slots/${id}`, { status }),
    onSuccess: invalidate,
  })
}

export function useSetDayStatus(doctorId: number) {
  const invalidate = useInvalidateSchedule()
  return useMutation({
    mutationFn: (input: { date: string; blocked: boolean }) =>
      apiSend<Slot[]>('POST', `/staff/doctors/${doctorId}/slots/day-status`, input),
    onSuccess: invalidate,
  })
}

export function useAddLeave(doctorId: number) {
  const invalidate = useInvalidateSchedule()
  return useMutation({
    mutationFn: (input: { fromDate: string; toDate: string; reason: string | null }) =>
      apiSend<WithChanges<Leave>>('POST', `/staff/doctors/${doctorId}/leaves`, input),
    onSuccess: invalidate,
  })
}

export function useRemoveLeave() {
  const invalidate = useInvalidateSchedule()
  return useMutation({ mutationFn: (id: number) => apiSend<ChangeSummary>('DELETE', `/staff/leaves/${id}`), onSuccess: invalidate })
}

export function useAddHoliday() {
  const invalidate = useInvalidateSchedule()
  return useMutation({
    mutationFn: (input: { branchId: number | null; date: string; name: string }) =>
      apiSend<WithChanges<Holiday>>('POST', '/staff/holidays', input),
    onSuccess: invalidate,
  })
}

export function useRemoveHoliday() {
  const invalidate = useInvalidateSchedule()
  return useMutation({ mutationFn: (id: number) => apiSend<ChangeSummary>('DELETE', `/staff/holidays/${id}`), onSuccess: invalidate })
}
