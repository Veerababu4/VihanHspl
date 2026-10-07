import { zodResolver } from '@hookform/resolvers/zod'
import type { TFunction } from 'i18next'
import { useState } from 'react'
import { useFieldArray, useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { z } from 'zod'

import { ApiError } from '../../../api/client'
import { useCompleteVisit, useSaveVisit, useVisit, type ClinicalNote, type SaveVisitInput, type VisitDetail } from '../../../api/clinical'
import { NotFoundPage } from '../../../components/SimplePages'
import { Seo } from '../../../components/Seo'
import { Alert, Badge, Button, Field, inputClass, Modal, PageHeader } from '../../../components/ui'
import { fullDate } from '../../../lib/dates'
import { applyServerErrors } from '../../../lib/forms'

/** Vitals with their sanity ranges (same as the backend's SaveVisitRequest). */
const VITALS = [
  { key: 'bpSystolic', min: 50, max: 300 },
  { key: 'bpDiastolic', min: 30, max: 200 },
  { key: 'pulse', min: 20, max: 250 },
  { key: 'temperatureF', min: 90, max: 110 },
  { key: 'spo2', min: 50, max: 100 },
  { key: 'weightKg', min: 0.5, max: 300 },
  { key: 'heightCm', min: 30, max: 250 },
] as const

type VitalKey = (typeof VITALS)[number]['key']

function schema(t: TFunction) {
  const vital = (min: number, max: number) =>
    z.string().trim().refine((v) => v === '' || (Number.isFinite(Number(v)) && Number(v) >= min && Number(v) <= max), t('staff.visit.errors.range', { min, max }))
  return z.object({
    ...(Object.fromEntries(VITALS.map((v) => [v.key, vital(v.min, v.max)])) as Record<VitalKey, ReturnType<typeof vital>>),
    chiefComplaint: z.string().max(1000),
    diagnosis: z.string().max(1000),
    clinicalNotes: z.string().max(10000),
    advice: z.string().max(2000),
    followUpDate: z.string(),
    prescription: z.array(
      z.object({
        medicineName: z.string().trim().min(1, t('staff.visit.errors.medicine')).max(200),
        dosage: z.string().max(60),
        frequency: z.string().max(60),
        duration: z.string().max(60),
        instructions: z.string().max(300),
      }),
    ),
  })
}

type Input = z.input<ReturnType<typeof schema>>
type Output = z.output<ReturnType<typeof schema>>

const str = (v: string | number | null) => (v == null ? '' : String(v))
const blank = (s: string) => (s.trim() === '' ? null : s.trim())
const num = (s: string) => (s.trim() === '' ? null : Number(s))

function toForm(v: VisitDetail): Input {
  const c = v.clinical
  return {
    ...(Object.fromEntries(VITALS.map(({ key }) => [key, str(c[key])])) as Record<VitalKey, string>),
    chiefComplaint: c.chiefComplaint ?? '',
    diagnosis: c.diagnosis ?? '',
    clinicalNotes: c.clinicalNotes ?? '',
    advice: c.advice ?? '',
    followUpDate: c.followUpDate ?? '',
    prescription: v.prescription.map((r) => ({
      medicineName: r.medicineName, dosage: r.dosage ?? '', frequency: r.frequency ?? '', duration: r.duration ?? '', instructions: r.instructions ?? '',
    })),
  }
}

function toInput(v: Output): SaveVisitInput {
  const vitals = Object.fromEntries(VITALS.map(({ key }) => [key, num(v[key])])) as Pick<ClinicalNote, VitalKey>
  return {
    ...vitals,
    chiefComplaint: blank(v.chiefComplaint),
    diagnosis: blank(v.diagnosis),
    clinicalNotes: blank(v.clinicalNotes),
    advice: blank(v.advice),
    followUpDate: v.followUpDate || null,
    prescription: v.prescription.map((r) => ({
      medicineName: r.medicineName.trim(), dosage: blank(r.dosage), frequency: blank(r.frequency), duration: blank(r.duration), instructions: blank(r.instructions),
    })),
  }
}

/**
 * The visit record (FR-C2): vitals, complaint, diagnosis, notes, advice, follow-up and the
 * prescription, saved together. The treating doctor completes it (FR-C3); other doctors and
 * completed visits are read-only. Allergies from the medical profile are shown at the top.
 */
export function VisitPage() {
  const { t } = useTranslation()
  const id = Number(useParams().id)
  const visit = useVisit(id)

  if (visit.error instanceof ApiError && visit.error.status === 404) return <NotFoundPage />
  if (visit.isPending) return <p className="text-slate-500">{t('staff.common.loading')}</p>
  if (visit.error) return <Alert>{visit.error.message}</Alert>
  // A fresh form whenever the visit changes status (e.g. after completing).
  return <VisitForm key={`${visit.data.id}-${visit.data.status}`} visit={visit.data} />
}

function VisitForm({ visit: v }: { visit: VisitDetail }) {
  const { t } = useTranslation()
  const save = useSaveVisit(v.id)
  const complete = useCompleteVisit(v.id)
  const [message, setMessage] = useState<{ kind: 'success' | 'error'; text: string } | null>(null)
  const [confirming, setConfirming] = useState(false)
  const form = useForm<Input, unknown, Output>({ resolver: zodResolver(schema(t)), defaultValues: toForm(v) })
  const rx = useFieldArray({ control: form.control, name: 'prescription' })
  const errors = form.formState.errors
  const [weightText, heightText] = useWatch({ control: form.control, name: ['weightKg', 'heightCm'] })
  const weight = Number(weightText)
  const height = Number(heightText)
  const bmi = weight > 0 && height > 0 ? (weight / (height / 100) ** 2).toFixed(1) : null

  /** Save; with complete=true also complete the visit once the save went through. */
  const submit = (alsoComplete: boolean) =>
    form.handleSubmit(async (values) => {
      setMessage(null)
      try {
        const saved = await save.mutateAsync(toInput(values))
        form.reset(toForm(saved))
        if (alsoComplete) {
          await complete.mutateAsync()
          setMessage({ kind: 'success', text: t('staff.visit.completed') })
        } else {
          setMessage({ kind: 'success', text: t('staff.visit.saved') })
        }
      } catch (e) {
        const text = applyServerErrors(e, form.setError)
        if (text) setMessage({ kind: 'error', text })
      }
    })

  const busy = save.isPending || complete.isPending
  const sexAge = `${v.patient.age} y · ${t(`booking.genders.${v.patient.gender}`)}`

  return (
    <>
      <Seo title={`${t('staff.visit.title')} · ${v.patient.fullName}`} />
      <PageHeader
        title={v.patient.fullName}
        actions={
          <div className="flex flex-wrap gap-2">
            <Link to={`/staff/patients/${v.patient.id}/history`} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-800 hover:bg-slate-50">
              {t('staff.visit.history')}
            </Link>
            <Link to={`/staff/visits/${v.id}/print`} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-800 hover:bg-slate-50">
              {t('staff.visit.print')}
            </Link>
          </div>
        }
      />
      <p className="-mt-4 mb-4 text-slate-600">
        {sexAge} · {v.patient.uhid} · {fullDate(v.date)} · {v.branch?.city}
        {v.tokenNo != null && ` · ${t('staff.visit.token', { token: v.tokenNo })}`}
      </p>

      <div className="mb-4 space-y-3">
        {v.allergies && (
          <div role="alert" className="rounded-lg border border-red-300 bg-red-50 px-4 py-3 font-semibold text-red-800">
            {t('staff.visit.allergies', { text: v.allergies })}
          </div>
        )}
        {v.bookingReason && <p className="text-sm text-slate-700">{t('staff.visit.reason', { text: v.bookingReason })}</p>}
        {!v.canEdit && (
          <Alert kind="info">{v.status === 'COMPLETED' ? t('staff.visit.completedNote') : t('staff.visit.otherDoctor', { doctor: v.doctor.fullName })}</Alert>
        )}
        {message && <Alert kind={message.kind}>{message.text}</Alert>}
      </div>

      <form onSubmit={submit(false)} noValidate>
        <fieldset disabled={!v.canEdit || busy} className="space-y-6">
          <section aria-labelledby="vitals" className="rounded-xl border border-slate-200 bg-white p-5">
            <h2 id="vitals" className="mb-3 font-semibold text-slate-900">
              {t('staff.visit.vitals')} {bmi && <Badge>{t('staff.visit.bmi', { value: bmi })}</Badge>}
            </h2>
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4 lg:grid-cols-7">
              {VITALS.map(({ key }) => (
                <Field key={key} label={t(`staff.visit.${key}`)} error={errors[key]?.message}>
                  {(a) => <input {...a} {...form.register(key)} inputMode="decimal" className={inputClass} />}
                </Field>
              ))}
            </div>
          </section>

          <section className="grid gap-4 rounded-xl border border-slate-200 bg-white p-5">
            {(['chiefComplaint', 'diagnosis'] as const).map((key) => (
              <Field key={key} label={t(`staff.visit.${key}`)} error={errors[key]?.message}>
                {(a) => <input {...a} {...form.register(key)} maxLength={1000} className={inputClass} />}
              </Field>
            ))}
            <Field label={t('staff.visit.clinicalNotes')} error={errors.clinicalNotes?.message}>
              {(a) => <textarea {...a} {...form.register('clinicalNotes')} rows={4} maxLength={10000} className={inputClass} />}
            </Field>
            <Field label={t('staff.visit.advice')} error={errors.advice?.message}>
              {(a) => <textarea {...a} {...form.register('advice')} rows={2} maxLength={2000} className={inputClass} />}
            </Field>
            <div className="max-w-xs">
              <Field label={t('staff.visit.followUpDate')} error={errors.followUpDate?.message}>
                {(a) => <input {...a} {...form.register('followUpDate')} type="date" min={v.date} className={inputClass} />}
              </Field>
            </div>
          </section>

          <section aria-labelledby="rx" className="rounded-xl border border-slate-200 bg-white p-5">
            <h2 id="rx" className="mb-3 font-semibold text-slate-900">{t('staff.visit.prescription')}</h2>
            {rx.fields.length === 0 && <p className="mb-3 text-sm text-slate-600">{t('staff.visit.noMedicines')}</p>}
            <ol className="space-y-3">
              {rx.fields.map((field, i) => (
                <li key={field.id} className="grid gap-2 rounded-lg border border-slate-200 p-3 sm:grid-cols-[2fr_1fr_1fr_1fr_2fr_auto] sm:items-end">
                  {(['medicineName', 'dosage', 'frequency', 'duration', 'instructions'] as const).map((key) => (
                    <Field key={key} label={t(`staff.visit.${key === 'medicineName' ? 'medicine' : key}`)} error={errors.prescription?.[i]?.[key]?.message}>
                      {(a) => <input {...a} {...form.register(`prescription.${i}.${key}`)} className={inputClass} />}
                    </Field>
                  ))}
                  <Button variant="link" onClick={() => rx.remove(i)} aria-label={t('staff.visit.remove', { name: form.getValues(`prescription.${i}.medicineName`) || i + 1 })}>
                    ✕
                  </Button>
                </li>
              ))}
            </ol>
            {v.canEdit && (
              <Button variant="secondary" className="mt-3" onClick={() => rx.append({ medicineName: '', dosage: '', frequency: '', duration: '', instructions: '' })}>
                {t('staff.visit.addMedicine')}
              </Button>
            )}
          </section>

          {v.canEdit && (
            <div className="flex flex-wrap gap-3">
              <Button type="submit">{save.isPending ? t('staff.visit.saving') : t('staff.visit.save')}</Button>
              <Button variant="secondary" onClick={() => setConfirming(true)}>
                {t('staff.visit.complete')}
              </Button>
            </div>
          )}
        </fieldset>
      </form>

      {confirming && (
        <Modal title={t('staff.visit.complete')} onClose={() => setConfirming(false)}>
          <p className="mb-4 text-slate-700">{t('staff.visit.confirmComplete')}</p>
          <div className="flex gap-3">
            <Button
              onClick={() => {
                setConfirming(false)
                void submit(true)()
              }}
            >
              {t('staff.visit.complete')}
            </Button>
            <Button variant="secondary" onClick={() => setConfirming(false)}>
              {t('staff.common.cancel')}
            </Button>
          </div>
        </Modal>
      )}
    </>
  )
}
