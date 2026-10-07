import type { TFunction } from 'i18next'
import { z } from 'zod'

import { normalizeAadhaar, type PatientExtras } from '../../../api/patients'
import { normalizeMobile } from '../../public/booking/patientSchema'

// Rules and conversion for the optional registration details (FR-PT1), shared by the
// registration and edit forms. The fields themselves are in PatientExtrasFields.tsx.

/** The optional registration details (FR-PT1) as form strings ('' = not given). */
export interface ExtrasForm {
  aadhaar: string
  altMobile: string
  address: string
  city: string
  bloodGroup: string
  emergencyName: string
  emergencyPhone: string
}

export const emptyExtras: ExtrasForm = { aadhaar: '', altMobile: '', address: '', city: '', bloodGroup: '', emergencyName: '', emergencyPhone: '' }

const optionalMobile = (t: TFunction) =>
  z.string().refine((v) => v.trim() === '' || normalizeMobile(v) !== null, t('staff.patient.errors.mobile'))

/** Zod rules for the extras, spread into each form's schema. */
export function extrasShape(t: TFunction) {
  return {
    aadhaar: z.string().refine((v) => v.trim() === '' || normalizeAadhaar(v) !== null, t('staff.patient.errors.aadhaar')),
    altMobile: optionalMobile(t),
    address: z.string().max(400),
    city: z.string().max(60),
    bloodGroup: z.string(),
    emergencyName: z.string().max(120),
    emergencyPhone: optionalMobile(t),
  }
}

/** Form strings → API values: blanks become null, numbers are tidied. */
export function extrasPayload(v: ExtrasForm): PatientExtras {
  const blank = (s: string) => (s.trim() === '' ? null : s.trim())
  const mobile = (s: string) => (s.trim() === '' ? null : normalizeMobile(s))
  return {
    aadhaar: v.aadhaar.trim() === '' ? null : normalizeAadhaar(v.aadhaar),
    altMobile: mobile(v.altMobile),
    address: blank(v.address),
    city: blank(v.city),
    bloodGroup: (blank(v.bloodGroup) as PatientExtras['bloodGroup']) ?? null,
    emergencyName: blank(v.emergencyName),
    emergencyPhone: mobile(v.emergencyPhone),
  }
}

