import { zodResolver } from '@hookform/resolvers/zod'
import type { TFunction } from 'i18next'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'

import { useBranches } from '../../../api/public'
import { useRegisterPatient, type PatientSummary } from '../../../api/reception'
import { Alert, Button, Field, inputClass } from '../../../components/ui'
import { todayIso } from '../../../lib/dates'
import { applyServerErrors } from '../../../lib/forms'
import { normalizeMobile } from '../../public/booking/patientSchema'

function schema(t: TFunction) {
  return z
    .object({
      fullName: z.string().trim().min(2, t('staff.patient.errors.name')).max(120),
      age: z.string().trim(),
      dateOfBirth: z.string(),
      gender: z.enum(['M', 'F', 'O'], { error: t('staff.patient.errors.gender') }),
      mobile: z.string().refine((v) => normalizeMobile(v) !== null, t('staff.patient.errors.mobile')),
      branch: z.string().min(1, t('staff.patient.errors.branch')),
      consent: z.boolean().refine((v) => v, t('staff.patient.errors.consent')),
    })
    .refine((v) => v.dateOfBirth !== '' || (/^\d{1,3}$/.test(v.age) && Number(v.age) <= 120), {
      path: ['age'],
      message: t('staff.patient.errors.ageOrDob'),
    })
}

type Input = z.input<ReturnType<typeof schema>>
type Output = z.output<ReturnType<typeof schema>>

export interface PatientPrefill {
  fullName?: string
  age?: number | null
  gender?: 'M' | 'F' | 'O' | null
  mobile?: string
  branch?: string | null
}

/**
 * Minimal registration (M6): name, age or date of birth, gender, mobile, branch, consent.
 * At check-in it is pre-filled from the booking (DESIGN.md §19.5).
 */
export function RegisterPatientForm({
  prefill,
  submitLabel,
  onRegistered,
}: {
  prefill: PatientPrefill
  submitLabel?: string
  onRegistered: (p: PatientSummary) => void | Promise<void>
}) {
  const { t } = useTranslation()
  const { data: branches } = useBranches()
  const register = useRegisterPatient()
  const [error, setError] = useState<string | null>(null)
  const form = useForm<Input, unknown, Output>({
    resolver: zodResolver(schema(t)),
    defaultValues: {
      fullName: prefill.fullName ?? '',
      age: prefill.age == null ? '' : String(prefill.age),
      dateOfBirth: '',
      gender: prefill.gender ?? undefined,
      mobile: prefill.mobile ?? '',
      branch: prefill.branch ?? '',
      consent: false,
    },
  })
  const errors = form.formState.errors

  const onSubmit = form.handleSubmit(async (v) => {
    setError(null)
    try {
      const patient = await register.mutateAsync({
        fullName: v.fullName,
        age: v.dateOfBirth ? null : Number(v.age),
        dateOfBirth: v.dateOfBirth || null,
        gender: v.gender,
        mobile: normalizeMobile(v.mobile) ?? v.mobile,
        branch: v.branch,
        consent: v.consent,
      })
      await onRegistered(patient)
    } catch (e) {
      setError(applyServerErrors(e, form.setError))
    }
  })

  return (
    <form onSubmit={onSubmit} noValidate className="space-y-4">
      {error && <Alert>{error}</Alert>}
      <Field label={t('staff.patient.fullName')} error={errors.fullName?.message}>
        {(a) => <input {...a} {...form.register('fullName')} className={inputClass} />}
      </Field>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label={t('staff.patient.age')} error={errors.age?.message}>
          {(a) => <input {...a} {...form.register('age')} inputMode="numeric" className={inputClass} />}
        </Field>
        <Field label={t('staff.patient.dob')} error={errors.dateOfBirth?.message}>
          {(a) => <input {...a} {...form.register('dateOfBirth')} type="date" max={todayIso()} className={inputClass} />}
        </Field>
        <fieldset>
          <legend className="mb-1 text-sm font-medium text-slate-700">{t('staff.patient.gender')}</legend>
          <div className="flex gap-4 pt-2">
            {(['M', 'F', 'O'] as const).map((g) => (
              <label key={g} className="flex items-center gap-1.5 text-sm">
                <input type="radio" value={g} {...form.register('gender')} className="h-4 w-4" />
                {t(`booking.genders.${g}`)}
              </label>
            ))}
          </div>
          {errors.gender && <p className="mt-1 text-sm text-emergency-600">{errors.gender.message}</p>}
        </fieldset>
        <Field label={t('staff.patient.mobile')} error={errors.mobile?.message}>
          {(a) => <input {...a} {...form.register('mobile')} inputMode="tel" className={inputClass} />}
        </Field>
        <Field label={t('staff.patient.branch')} error={errors.branch?.message}>
          {(a) => (
            <select {...a} {...form.register('branch')} className={inputClass}>
              <option value="" />
              {branches?.map((b) => (
                <option key={b.code} value={b.code}>
                  {b.city}
                </option>
              ))}
            </select>
          )}
        </Field>
      </div>
      <label className="flex items-start gap-2 text-sm">
        <input type="checkbox" {...form.register('consent')} className="mt-0.5 h-4 w-4" />
        {t('staff.patient.consent')}
      </label>
      {errors.consent && <p className="text-sm text-emergency-600">{errors.consent.message}</p>}
      <Button type="submit" disabled={register.isPending}>
        {register.isPending ? t('staff.patient.registering') : submitLabel ?? t('staff.patient.register')}
      </Button>
    </form>
  )
}
