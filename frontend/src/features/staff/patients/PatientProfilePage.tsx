import { zodResolver } from '@hookform/resolvers/zod'
import type { TFunction } from 'i18next'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { z } from 'zod'

import { ApiError } from '../../../api/client'
import { usePatient, usePatientVisits, useUpdatePatient, type PatientDetail } from '../../../api/patients'
import { useAuth } from '../../../auth/session'
import { NotFoundPage } from '../../../components/SimplePages'
import { Seo } from '../../../components/Seo'
import { Alert, Badge, Button, Field, inputClass, Modal, PageHeader } from '../../../components/ui'
import { fullDate, shortDate, timeOf, todayIso } from '../../../lib/dates'
import { formatTime } from '../../../lib/format'
import { applyServerErrors } from '../../../lib/forms'
import { normalizeMobile } from '../../public/booking/patientSchema'
import { extrasPayload, extrasShape } from './patientExtras'
import { PatientExtrasFields } from './PatientExtrasFields'

/**
 * Patient profile (FR-PT4): demographics with the Aadhaar masked, upcoming appointments and the
 * visit list. Opening it is audit-logged by the server. Reception / IT Admin can edit (FR-PT5);
 * doctors only look. Nothing clinical is shown here (doctors get the history page in M8).
 */
export function PatientProfilePage() {
  const { t } = useTranslation()
  const id = Number(useParams().id)
  const { user } = useAuth()
  const desk = user?.role === 'RECEPTIONIST' || user?.role === 'IT_ADMIN'
  const patient = usePatient(id)
  const visits = usePatientVisits(id)
  const [editing, setEditing] = useState(false)
  const [saved, setSaved] = useState(false)

  if (patient.error instanceof ApiError && patient.error.status === 404) return <NotFoundPage />
  if (patient.isPending) return <p className="text-slate-500">{t('staff.common.loading')}</p>
  if (patient.error) return <Alert>{patient.error.message}</Alert>
  const p = patient.data
  const none = t('staff.common.none')

  const rows: [string, string | null][] = [
    [t('staff.patients.uhid'), p.uhid],
    [t('staff.patients.age'), t('staff.patients.ageValue', { age: p.age })],
    [t('staff.patients.dob'), `${fullDate(p.dateOfBirth)}${p.dobEstimated ? ` ${t('staff.patients.estimated')}` : ''}`],
    [t('staff.patients.gender'), t(`booking.genders.${p.gender}`)],
    [t('staff.patients.mobile'), p.mobile],
    [t('staff.patients.altMobile'), p.altMobile],
    [t('staff.patients.aadhaar'), p.aadhaarMasked],
    [t('staff.patients.bloodGroup'), p.bloodGroup],
    [t('staff.patients.address'), [p.address, p.city].filter(Boolean).join(', ') || null],
    [t('staff.patients.emergency'), p.emergencyName ? `${p.emergencyName}${p.emergencyPhone ? ` · ${p.emergencyPhone}` : ''}` : null],
    [t('staff.patients.registered'), t('staff.patients.registeredValue', { date: fullDate(p.registeredAt), branch: p.registeredBranchCity ?? none })],
  ]

  return (
    <>
      <Seo title={p.fullName} />
      <Link to="/staff/patients" className="text-sm text-brand-700 hover:underline print:hidden">
        ← {t('staff.patients.back')}
      </Link>
      <PageHeader
        title={p.fullName}
        actions={
          desk ? (
            <Button variant="secondary" onClick={() => { setSaved(false); setEditing(true) }}>
              {t('staff.patients.edit')}
            </Button>
          ) : user?.role === 'DOCTOR' ? (
            <Link to={`/staff/patients/${p.id}/history`} className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
              {t('staff.patients.historyLink')}
            </Link>
          ) : null
        }
      />
      {saved && (
        <div className="mb-4">
          <Alert kind="success">{t('staff.patients.saved')}</Alert>
        </div>
      )}

      <dl className="mb-8 grid gap-x-6 gap-y-3 rounded-xl border border-slate-200 bg-white p-5 sm:grid-cols-2 lg:grid-cols-3">
        {rows.map(([label, value]) => (
          <div key={label}>
            <dt className="text-sm text-slate-500">{label}</dt>
            <dd className="font-medium break-words text-slate-900">{value ?? none}</dd>
          </div>
        ))}
      </dl>

      <section aria-labelledby="upcoming" className="mb-8">
        <h2 id="upcoming" className="mb-2 text-lg font-semibold text-slate-900">{t('staff.patients.upcoming')}</h2>
        {p.upcoming.length === 0 ? (
          <p className="text-slate-600">{t('staff.patients.noUpcoming')}</p>
        ) : (
          <ul className="divide-y divide-slate-200 rounded-xl border border-slate-200 bg-white">
            {p.upcoming.map((a) => (
              <li key={a.id} className="flex flex-wrap items-center gap-x-4 gap-y-1 p-3 text-sm">
                <span className="font-medium">
                  {shortDate(a.date)}
                  {a.startAt && `, ${formatTime(timeOf(a.startAt))}`}
                </span>
                <span>{a.doctorName} · {a.branchCity}</span>
                <span className="font-mono text-xs">{a.bookingRef}</span>
                <Badge>{t(`status.statusLabel.${a.status}`)}</Badge>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section aria-labelledby="visits">
        <h2 id="visits" className="mb-1 text-lg font-semibold text-slate-900">{t('staff.patients.visits')}</h2>
        <p className="mb-2 text-sm text-slate-500">{t('staff.patients.clinicalNote')}</p>
        {visits.data?.length === 0 && <p className="text-slate-600">{t('staff.patients.noVisits')}</p>}
        {visits.data && visits.data.length > 0 && (
          <ul className="divide-y divide-slate-200 rounded-xl border border-slate-200 bg-white">
            {visits.data.map((v) => (
              <li key={v.id} className="flex flex-wrap items-center gap-x-4 gap-y-1 p-3 text-sm">
                <span className="font-medium">{fullDate(v.date)}</span>
                <span>{v.doctorName} · {v.speciality} · {v.branchCity}</span>
                <Badge>{t(`staff.patients.visitTypes.${v.type}`)}</Badge>
                <Badge tone={v.status === 'COMPLETED' ? 'good' : 'warn'}>{t(`staff.patients.visitStatus.${v.status}`)}</Badge>
              </li>
            ))}
          </ul>
        )}
      </section>

      {editing && (
        <EditPatientDialog
          patient={p}
          onClose={() => setEditing(false)}
          onSaved={() => {
            setEditing(false)
            setSaved(true)
          }}
        />
      )}
    </>
  )
}

function editSchema(t: TFunction) {
  return z
    .object({
      fullName: z.string().trim().min(2, t('staff.patient.errors.name')).max(120),
      age: z.string().trim(),
      dateOfBirth: z.string(),
      gender: z.enum(['M', 'F', 'O'], { error: t('staff.patient.errors.gender') }),
      mobile: z.string().refine((v) => normalizeMobile(v) !== null, t('staff.patient.errors.mobile')),
      removeAadhaar: z.boolean(),
      ...extrasShape(t),
    })
    .refine((v) => v.dateOfBirth !== '' || (/^\d{1,3}$/.test(v.age) && Number(v.age) <= 120), {
      path: ['age'],
      message: t('staff.patient.errors.ageOrDob'),
    })
}

type EditInput = z.input<ReturnType<typeof editSchema>>
type EditOutput = z.output<ReturnType<typeof editSchema>>

/** FR-PT5. The stored Aadhaar is never sent to the browser, so the field starts empty = "keep it". */
function EditPatientDialog({ patient: p, onClose, onSaved }: { patient: PatientDetail; onClose: () => void; onSaved: () => void }) {
  const { t } = useTranslation()
  const update = useUpdatePatient(p.id)
  const [error, setError] = useState<string | null>(null)
  const [confirmDuplicate, setConfirmDuplicate] = useState(false)
  const form = useForm<EditInput, unknown, EditOutput>({
    resolver: zodResolver(editSchema(t)),
    defaultValues: {
      fullName: p.fullName,
      // An exact date stays a date; an estimated one is edited as an age.
      age: p.dobEstimated ? String(p.age) : '',
      dateOfBirth: p.dobEstimated ? '' : p.dateOfBirth,
      gender: p.gender,
      mobile: p.mobile,
      removeAadhaar: false,
      aadhaar: '',
      altMobile: p.altMobile ?? '',
      address: p.address ?? '',
      city: p.city ?? '',
      bloodGroup: p.bloodGroup ?? '',
      emergencyName: p.emergencyName ?? '',
      emergencyPhone: p.emergencyPhone ?? '',
    },
  })
  const errors = form.formState.errors

  const onSubmit = form.handleSubmit(async (v) => {
    setError(null)
    try {
      await update.mutateAsync({
        fullName: v.fullName,
        age: v.dateOfBirth ? null : Number(v.age),
        dateOfBirth: v.dateOfBirth || null,
        gender: v.gender,
        mobile: normalizeMobile(v.mobile) ?? v.mobile,
        removeAadhaar: v.removeAadhaar,
        confirmDuplicate,
        ...extrasPayload(v),
      })
      onSaved()
    } catch (e) {
      if (e instanceof ApiError && e.code === 'POSSIBLE_DUPLICATE') {
        setConfirmDuplicate(true)   // the message names the other record; saving again confirms
        setError(e.message)
      } else if (e instanceof ApiError && e.code === 'AADHAAR_EXISTS') {
        form.setError('aadhaar', { message: e.message })
      } else {
        setError(applyServerErrors(e, form.setError))
      }
    }
  })

  return (
    <Modal title={t('staff.patients.editTitle')} onClose={onClose} wide>
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
          </fieldset>
          <Field label={t('staff.patient.mobile')} error={errors.mobile?.message}>
            {(a) => <input {...a} {...form.register('mobile')} inputMode="tel" className={inputClass} />}
          </Field>
        </div>
        <PatientExtrasFields
          register={(n) => form.register(n)}
          errors={errors}
          aadhaarHint={p.aadhaarMasked ? t('staff.patients.aadhaarKeep', { masked: p.aadhaarMasked }) : undefined}
        />
        {p.aadhaarMasked && (
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" {...form.register('removeAadhaar')} className="h-4 w-4" />
            {t('staff.patients.removeAadhaar')}
          </label>
        )}
        <div className="flex gap-3">
          <Button type="submit" disabled={update.isPending}>
            {update.isPending ? t('staff.common.saving') : t('staff.common.save')}
          </Button>
          <Button variant="secondary" onClick={onClose}>
            {t('staff.common.cancel')}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
