import { zodResolver } from '@hookform/resolvers/zod'
import type { TFunction } from 'i18next'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { z } from 'zod'

import { ApiError } from '../../../api/client'
import { checkDuplicates } from '../../../api/patients'
import { useBranches } from '../../../api/public'
import { useRegisterPatient, type PatientSummary } from '../../../api/reception'
import { Alert, Button, Field, inputClass } from '../../../components/ui'
import { todayIso } from '../../../lib/dates'
import { applyServerErrors } from '../../../lib/forms'
import { normalizeMobile } from '../../public/booking/patientSchema'
import { emptyExtras, extrasPayload, extrasShape } from '../patients/patientExtras'
import { PatientExtrasFields } from '../patients/PatientExtrasFields'

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
      ...extrasShape(t),
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
 * Patient registration (FR-PT1). The short form (name, age or date of birth, gender, mobile,
 * branch, consent) is all check-in and walk-ins need; Aadhaar and contact details are optional.
 * Duplicates (FR-PT2): the same Aadhaar is refused by the server; the same mobile with a similar
 * name shows the existing records first, to use one of them or confirm it's a different person.
 */
export function RegisterPatientForm({
  prefill,
  submitLabel,
  showDetails = false,
  onRegistered,
  onUseExisting,
}: {
  prefill: PatientPrefill
  submitLabel?: string
  /** open the optional details at once (the full registration page) */
  showDetails?: boolean
  onRegistered: (p: PatientSummary) => void | Promise<void>
  /** offered next to each possible duplicate; without it the duplicate's record can be opened */
  onUseExisting?: (p: PatientSummary) => void | Promise<void>
}) {
  const { t } = useTranslation()
  const { data: branches } = useBranches()
  const register = useRegisterPatient()
  const [error, setError] = useState<string | null>(null)
  const [duplicates, setDuplicates] = useState<PatientSummary[] | null>(null)
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
      ...emptyExtras,
    },
  })
  const errors = form.formState.errors

  // confirmDuplicate: the user saw the possible duplicates and said this is a different person.
  const submit = (confirmDuplicate: boolean) => form.handleSubmit(async (v) => {
    setError(null)
    const mobile = normalizeMobile(v.mobile) ?? v.mobile
    try {
      const patient = await register.mutateAsync({
        fullName: v.fullName,
        age: v.dateOfBirth ? null : Number(v.age),
        dateOfBirth: v.dateOfBirth || null,
        gender: v.gender,
        mobile,
        branch: v.branch,
        consent: v.consent,
        ...extrasPayload(v),
        confirmDuplicate,
      })
      setDuplicates(null)
      await onRegistered(patient)
    } catch (e) {
      if (e instanceof ApiError && e.code === 'POSSIBLE_DUPLICATE') {
        const found = await checkDuplicates({ name: v.fullName, mobile, aadhaar: null }).catch(() => null)
        setDuplicates(found?.similar ?? [])
        if (!found?.similar.length) setError(e.message)
      } else if (e instanceof ApiError && e.code === 'AADHAAR_EXISTS') {
        form.setError('aadhaar', { message: e.message })
      } else {
        setError(applyServerErrors(e, form.setError))
      }
    }
  })

  const onSubmit = submit(false)
  const registerAnyway = () => void submit(true)()

  return (
    <form onSubmit={onSubmit} noValidate className="space-y-4">
      {error && <Alert>{error}</Alert>}
      {duplicates && duplicates.length > 0 && (
        <section aria-label={t('staff.patient.duplicateTitle')} className="rounded-lg border border-amber-300 bg-amber-50 p-4">
          <h3 className="font-semibold text-amber-950">{t('staff.patient.duplicateTitle')}</h3>
          <p className="mb-2 text-sm text-amber-900">{t('staff.patient.duplicateIntro')}</p>
          <ul className="mb-3 space-y-2">
            {duplicates.map((p) => (
              <li key={p.id} className="flex flex-wrap items-center justify-between gap-2 rounded border border-amber-200 bg-white p-2 text-sm">
                <span>
                  <span className="font-medium">{p.fullName}</span> · {p.uhid} · {p.age} y · {t(`booking.genders.${p.gender}`)}
                </span>
                {onUseExisting ? (
                  <Button variant="secondary" onClick={() => onUseExisting(p)}>
                    {t('staff.patient.useExisting')}
                  </Button>
                ) : (
                  <Link to={`/staff/patients/${p.id}`} className="font-medium text-brand-700 hover:underline">
                    {t('staff.patient.openRecord')}
                  </Link>
                )}
              </li>
            ))}
          </ul>
          <Button variant="secondary" onClick={registerAnyway} disabled={register.isPending}>
            {t('staff.patient.registerAnyway')}
          </Button>
        </section>
      )}
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
      <details open={showDetails || (Object.keys(emptyExtras) as (keyof typeof emptyExtras)[]).some((k) => errors[k])} className="rounded-lg border border-slate-200 p-3 [&[open]>summary]:mb-3">
        <summary className="cursor-pointer text-sm font-medium text-slate-800">{t('staff.patient.details')}</summary>
        <PatientExtrasFields register={(n) => form.register(n)} errors={errors} />
      </details>
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
