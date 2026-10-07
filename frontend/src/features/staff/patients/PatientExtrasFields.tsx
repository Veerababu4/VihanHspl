import type { UseFormRegisterReturn } from 'react-hook-form'
import { useTranslation } from 'react-i18next'

import { BLOOD_GROUPS } from '../../../api/patients'
import { Field, inputClass } from '../../../components/ui'
import type { ExtrasForm } from './patientExtras'

export function PatientExtrasFields({
  register,
  errors,
  aadhaarHint,
}: {
  register: (name: keyof ExtrasForm) => UseFormRegisterReturn
  errors: Partial<Record<keyof ExtrasForm, { message?: string }>>
  aadhaarHint?: string
}) {
  const { t } = useTranslation()
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <div className="sm:col-span-2">
        <Field label={t('staff.patient.aadhaar')} error={errors.aadhaar?.message} hint={aadhaarHint ?? t('staff.patient.aadhaarHint')}>
          {(a) => <input {...a} {...register('aadhaar')} inputMode="numeric" autoComplete="off" className={inputClass} />}
        </Field>
      </div>
      <Field label={t('staff.patient.altMobile')} error={errors.altMobile?.message}>
        {(a) => <input {...a} {...register('altMobile')} inputMode="tel" className={inputClass} />}
      </Field>
      <Field label={t('staff.patient.bloodGroup')} error={errors.bloodGroup?.message}>
        {(a) => (
          <select {...a} {...register('bloodGroup')} className={inputClass}>
            <option value="" />
            {BLOOD_GROUPS.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        )}
      </Field>
      <div className="sm:col-span-2">
        <Field label={t('staff.patient.address')} error={errors.address?.message}>
          {(a) => <input {...a} {...register('address')} maxLength={400} className={inputClass} />}
        </Field>
      </div>
      <Field label={t('staff.patient.city')} error={errors.city?.message}>
        {(a) => <input {...a} {...register('city')} maxLength={60} className={inputClass} />}
      </Field>
      <div className="hidden sm:block" />
      <Field label={t('staff.patient.emergencyName')} error={errors.emergencyName?.message}>
        {(a) => <input {...a} {...register('emergencyName')} maxLength={120} className={inputClass} />}
      </Field>
      <Field label={t('staff.patient.emergencyPhone')} error={errors.emergencyPhone?.message}>
        {(a) => <input {...a} {...register('emergencyPhone')} inputMode="tel" className={inputClass} />}
      </Field>
    </div>
  )
}
