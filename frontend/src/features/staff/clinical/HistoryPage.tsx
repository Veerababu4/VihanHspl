import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'

import { ApiError } from '../../../api/client'
import { useHistory, useSaveMedicalProfile, type MedicalProfile } from '../../../api/clinical'
import { NotFoundPage } from '../../../components/SimplePages'
import { Seo } from '../../../components/Seo'
import { Alert, Badge, Button, Field, inputClass, Modal, PageHeader } from '../../../components/ui'
import { fullDate } from '../../../lib/dates'
import { vitalsLine } from './vitals'

const PROFILE_FIELDS = ['allergies', 'chronicConditions', 'pastSurgeries', 'currentMedications', 'familyHistory'] as const

/**
 * Health history (FR-C4), doctors only: the medical profile and every visit with full clinical
 * details, across all doctors and branches (DESIGN.md §17 #11). Opening it is audit-logged.
 */
export function HistoryPage() {
  const { t } = useTranslation()
  const patientId = Number(useParams().id)
  const history = useHistory(patientId)
  const [editing, setEditing] = useState(false)
  const [saved, setSaved] = useState(false)

  if (history.error instanceof ApiError && history.error.status === 404) return <NotFoundPage />
  if (history.isPending) return <p className="text-slate-500">{t('staff.common.loading')}</p>
  if (history.error) return <Alert>{history.error.message}</Alert>
  const { patient: p, medicalProfile: m, visits } = history.data

  return (
    <>
      <Seo title={`${t('staff.history.title')} · ${p.fullName}`} />
      <Link to={`/staff/patients/${p.id}`} className="text-sm text-brand-700 hover:underline">
        ← {p.fullName}
      </Link>
      <PageHeader title={t('staff.history.title')} />
      <p className="-mt-4 mb-5 text-slate-600">
        {p.fullName} · {p.age} y · {t(`booking.genders.${p.gender}`)} · {p.uhid}
      </p>
      {saved && <div className="mb-4"><Alert kind="success">{t('staff.history.profileSaved')}</Alert></div>}

      <section aria-labelledby="profile" className="mb-8 rounded-xl border border-slate-200 bg-white p-5">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <h2 id="profile" className="text-lg font-semibold text-slate-900">{t('staff.history.profile')}</h2>
          <Button variant="secondary" onClick={() => { setSaved(false); setEditing(true) }}>
            {t('staff.history.editProfile')}
          </Button>
        </div>
        <dl className="grid gap-3 sm:grid-cols-2">
          {PROFILE_FIELDS.map((k) => (
            <div key={k} className={k === 'allergies' && m.allergies ? 'rounded-lg bg-red-50 p-2' : ''}>
              <dt className="text-sm text-slate-500">{t(`staff.history.${k}`)}</dt>
              <dd className={`whitespace-pre-line ${m[k] ? 'font-medium text-slate-900' : 'text-slate-500'} ${k === 'allergies' && m.allergies ? 'text-red-800' : ''}`}>
                {m[k] ?? t('staff.history.none')}
              </dd>
            </div>
          ))}
        </dl>
      </section>

      <section aria-labelledby="visits">
        <h2 id="visits" className="mb-3 text-lg font-semibold text-slate-900">{t('staff.history.visits', { count: visits.length })}</h2>
        {visits.length === 0 && <p className="text-slate-600">{t('staff.history.noVisits')}</p>}
        <ol className="space-y-3">
          {visits.map((v) => {
            const vitals = vitalsLine(v.clinical)
            return (
              <li key={v.id} className="rounded-xl border border-slate-200 bg-white p-4">
                <div className="mb-2 flex flex-wrap items-center gap-x-3 gap-y-1">
                  <span className="font-semibold">{fullDate(v.date)}</span>
                  <span className="text-sm text-slate-600">
                    {v.doctorName} · {v.speciality} · {v.branchCity}
                  </span>
                  {v.status === 'OPEN' && <Badge tone="warn">{t('staff.history.open')}</Badge>}
                  <Link to={`/staff/visits/${v.id}`} className="ml-auto text-sm font-medium text-brand-700 hover:underline">
                    {t('staff.history.openVisit')}
                  </Link>
                </div>
                <p className="font-medium text-slate-900">{v.clinical.diagnosis ?? <span className="text-slate-500">{t('staff.history.noDiagnosis')}</span>}</p>
                {v.clinical.chiefComplaint && <p className="text-sm text-slate-700">{v.clinical.chiefComplaint}</p>}
                {vitals && <p className="text-sm text-slate-600">{vitals}</p>}
                {v.clinical.clinicalNotes && <p className="mt-1 text-sm whitespace-pre-line text-slate-700">{v.clinical.clinicalNotes}</p>}
                {v.prescription.length > 0 && (
                  <ul className="mt-2 list-inside list-disc text-sm text-slate-800">
                    {v.prescription.map((r, i) => (
                      <li key={i}>
                        <span className="font-medium">{r.medicineName}</span>
                        {[r.dosage, r.frequency, r.duration, r.instructions].filter(Boolean).map((x) => ` · ${x}`).join('')}
                      </li>
                    ))}
                  </ul>
                )}
                {v.clinical.advice && <p className="mt-1 text-sm text-slate-700">{t('staff.rx.advice')}: {v.clinical.advice}</p>}
              </li>
            )
          })}
        </ol>
      </section>

      {editing && (
        <ProfileDialog
          patientId={p.id}
          profile={m}
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

function ProfileDialog({ patientId, profile, onClose, onSaved }: { patientId: number; profile: MedicalProfile; onClose: () => void; onSaved: () => void }) {
  const { t } = useTranslation()
  const save = useSaveMedicalProfile(patientId)
  const [values, setValues] = useState(() => Object.fromEntries(PROFILE_FIELDS.map((k) => [k, profile[k] ?? ''])) as Record<(typeof PROFILE_FIELDS)[number], string>)
  const [error, setError] = useState<string | null>(null)

  const submit = async () => {
    setError(null)
    try {
      await save.mutateAsync(Object.fromEntries(PROFILE_FIELDS.map((k) => [k, values[k].trim() || null])) as Omit<MedicalProfile, 'updatedAt'>)
      onSaved()
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    }
  }

  return (
    <Modal title={t('staff.history.editProfile')} onClose={onClose} wide>
      <form
        className="space-y-4"
        onSubmit={(e) => {
          e.preventDefault()
          void submit()
        }}
      >
        {error && <Alert>{error}</Alert>}
        {PROFILE_FIELDS.map((k) => (
          <Field key={k} label={t(`staff.history.${k}`)}>
            {(a) => <textarea {...a} rows={2} maxLength={1000} value={values[k]} onChange={(e) => setValues({ ...values, [k]: e.target.value })} className={inputClass} />}
          </Field>
        ))}
        <div className="flex gap-3">
          <Button type="submit" disabled={save.isPending}>{save.isPending ? t('staff.common.saving') : t('staff.common.save')}</Button>
          <Button variant="secondary" onClick={onClose}>{t('staff.common.cancel')}</Button>
        </div>
      </form>
    </Modal>
  )
}
