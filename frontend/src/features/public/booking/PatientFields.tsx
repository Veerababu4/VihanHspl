import type { ReactNode } from 'react'
import type { UseFormReturn } from 'react-hook-form'
import { useTranslation } from 'react-i18next'

import { Field, inputClass } from '../../../components/ui'
import type { PatientFormInput, PatientFormOutput } from './patientSchema'

/** Name, age, gender, mobile, reason and consent: the same fields online and at the counter. */
export function PatientFields({
  form,
  consentLabel,
  mobileHint,
}: {
  form: UseFormReturn<PatientFormInput, unknown, PatientFormOutput>
  consentLabel: ReactNode
  mobileHint?: string
}) {
  const { t } = useTranslation()
  const errors = form.formState.errors
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <div className="sm:col-span-2">
        <Field label={t('booking.patientName')} error={errors.patientName?.message}>
          {(a) => <input {...a} {...form.register('patientName')} autoComplete="name" className={inputClass} />}
        </Field>
      </div>
      <Field label={t('booking.age')} error={errors.patientAge?.message}>
        {(a) => <input {...a} {...form.register('patientAge')} inputMode="numeric" className={inputClass} />}
      </Field>
      <fieldset>
        <legend className="mb-1 text-sm font-medium text-slate-700">{t('booking.gender')}</legend>
        <div className="flex gap-4 pt-2">
          {(['M', 'F', 'O'] as const).map((g) => (
            <label key={g} className="flex items-center gap-1.5 text-sm">
              <input type="radio" value={g} {...form.register('patientGender')} className="h-4 w-4" />
              {t(`booking.genders.${g}`)}
            </label>
          ))}
        </div>
        {errors.patientGender && <p className="mt-1 text-sm text-emergency-600">{errors.patientGender.message}</p>}
      </fieldset>
      <div className="sm:col-span-2">
        <Field label={t('booking.mobile')} error={errors.mobile?.message} hint={mobileHint ?? t('booking.mobileHint')}>
          {(a) => <input {...a} {...form.register('mobile')} type="tel" inputMode="tel" autoComplete="tel" className={inputClass} />}
        </Field>
      </div>
      <div className="sm:col-span-2">
        <Field label={t('booking.reason')} error={errors.reason?.message}>
          {(a) => <textarea {...a} {...form.register('reason')} rows={2} maxLength={500} className={inputClass} />}
        </Field>
      </div>
      <div className="sm:col-span-2">
        <label className="flex items-start gap-2 text-sm">
          <input type="checkbox" {...form.register('consent')} className="mt-0.5 h-4 w-4" />
          <span>{consentLabel}</span>
        </label>
        {errors.consent && <p className="mt-1 text-sm text-emergency-600">{errors.consent.message}</p>}
      </div>
    </div>
  )
}
