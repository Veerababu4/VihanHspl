import type { TFunction } from 'i18next'
import { z } from 'zod'

/** Same rule as the backend (Mobiles.normalize): accepts +91 / 0 prefixes, spaces and dashes. */
export function normalizeMobile(input: string): string | null {
  let digits = input.replace(/[\s\-().]/g, '')
  if (digits.startsWith('+91')) digits = digits.slice(3)
  else if (digits.length === 12 && digits.startsWith('91')) digits = digits.slice(2)
  else if (digits.length === 11 && digits.startsWith('0')) digits = digits.slice(1)
  return /^[6-9]\d{9}$/.test(digits) ? digits : null
}

/** Patient details on the booking forms (FR-B1, FR-R3). */
export function patientSchema(t: TFunction) {
  return z.object({
    patientName: z.string().trim().min(2, t('booking.errors.name')).max(120),
    patientAge: z.coerce.number<string>({ error: t('booking.errors.age') }).int(t('booking.errors.age')).min(0, t('booking.errors.age')).max(120, t('booking.errors.age')),
    patientGender: z.enum(['M', 'F', 'O'], { error: t('booking.errors.gender') }),
    mobile: z.string().refine((v) => normalizeMobile(v) !== null, t('booking.errors.mobile')),
    reason: z.string().trim().max(500),
    consent: z.boolean().refine((v) => v, t('booking.errors.consent')),
  })
}

export type PatientFormInput = z.input<ReturnType<typeof patientSchema>>
export type PatientFormOutput = z.output<ReturnType<typeof patientSchema>>

/** Empty form; gender has no default so the patient must choose. */
export const emptyPatient: Partial<PatientFormInput> = { patientName: '', patientAge: '', mobile: '', reason: '', consent: false }
