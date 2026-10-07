import { useState } from 'react'
import { useTranslation } from 'react-i18next'

import { useBranches, useDoctors } from '../../../api/public'
import { findPatientsByMobile, useWalkIn, type PatientSummary, type StaffAppointment } from '../../../api/reception'
import { Alert, Button, Field, inputClass, Modal } from '../../../components/ui'
import { normalizeMobile } from '../../public/booking/patientSchema'
import { RegisterPatientForm } from './RegisterPatientForm'

/**
 * A patient seen today without a booking (FR-R5): find them by mobile or register them first
 * (DESIGN.md §19.5), then add them to a doctor's queue. They are checked in at once.
 */
export function WalkInDialog({
  defaultBranch,
  onClose,
  onDone,
}: {
  defaultBranch: string | undefined
  onClose: () => void
  onDone: (a: StaffAppointment) => void
}) {
  const { t } = useTranslation()
  const [mobile, setMobile] = useState('')
  const [found, setFound] = useState<PatientSummary[] | null>(null)
  const [patient, setPatient] = useState<PatientSummary | null>(null)
  const [branch, setBranch] = useState(defaultBranch ?? '')
  const [doctorId, setDoctorId] = useState<number>()
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)
  const { data: branches } = useBranches()
  const { data: doctors } = useDoctors({ branch: branch || undefined })
  const walkIn = useWalkIn()

  const search = async () => {
    setError(null)
    const normalized = normalizeMobile(mobile)
    if (!normalized) {
      setError(t('staff.patient.errors.mobile'))
      return
    }
    try {
      setFound(await findPatientsByMobile(normalized))
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    }
  }

  const add = async () => {
    if (!patient || !doctorId || !branch) return
    setError(null)
    try {
      onDone(await walkIn.mutateAsync({ patientId: patient.id, doctorId, branch, reason: reason.trim() || null }))
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    }
  }

  return (
    <Modal title={t('staff.walkIn.title')} onClose={onClose} wide>
      <div className="space-y-5">
        {error && <Alert>{error}</Alert>}

        {!patient && (
          <>
            <form
              className="flex flex-wrap items-end gap-3"
              onSubmit={(e) => {
                e.preventDefault()
                void search()
              }}
            >
              <div className="min-w-0 flex-1">
                <Field label={t('staff.walkIn.findByMobile')}>
                  {(a) => <input {...a} value={mobile} onChange={(e) => setMobile(e.target.value)} inputMode="tel" className={inputClass} />}
                </Field>
              </div>
              <Button type="submit">{t('staff.walkIn.find')}</Button>
            </form>

            {found && found.length > 0 && (
              <section aria-label={t('staff.walkIn.found')}>
                <h3 className="mb-2 font-semibold text-slate-900">{t('staff.walkIn.found')}</h3>
                <ul className="divide-y divide-slate-200 rounded-lg border border-slate-200">
                  {found.map((p) => (
                    <li key={p.id} className="flex flex-wrap items-center justify-between gap-3 p-3">
                      <span>
                        <span className="font-medium">{p.fullName}</span>{' '}
                        <span className="text-sm text-slate-600">
                          · {p.uhid} · {p.age} y · {t(`booking.genders.${p.gender}`)}
                        </span>
                      </span>
                      <Button variant="secondary" onClick={() => setPatient(p)}>
                        {t('staff.walkIn.choose')}
                      </Button>
                    </li>
                  ))}
                </ul>
              </section>
            )}
            {found && (
              <section aria-label={t('staff.patient.title')} className="border-t border-slate-200 pt-4">
                {found.length === 0 && <p className="mb-3 text-sm text-slate-600">{t('staff.walkIn.notFound')}</p>}
                <h3 className="mb-3 font-semibold text-slate-900">{t('staff.patient.title')}</h3>
                <RegisterPatientForm
                  key={mobile}
                  prefill={{ mobile: normalizeMobile(mobile) ?? mobile, branch: branch || null }}
                  onRegistered={setPatient}
                />
              </section>
            )}
          </>
        )}

        {patient && (
          <div className="space-y-4">
            <p className="flex flex-wrap items-center gap-3 font-medium">
              {t('staff.walkIn.chosen', { name: patient.fullName, uhid: patient.uhid })}
              <Button variant="link" onClick={() => setPatient(null)}>
                {t('staff.walkIn.changePatient')}
              </Button>
            </p>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label={t('staff.walkIn.branch')}>
                {(a) => (
                  <select {...a} value={branch} onChange={(e) => { setBranch(e.target.value); setDoctorId(undefined) }} className={inputClass}>
                    <option value="" />
                    {branches?.map((b) => (
                      <option key={b.code} value={b.code}>
                        {b.city}
                      </option>
                    ))}
                  </select>
                )}
              </Field>
              <Field label={t('staff.walkIn.doctor')}>
                {(a) => (
                  <select {...a} value={doctorId ?? ''} disabled={!branch} onChange={(e) => setDoctorId(Number(e.target.value) || undefined)} className={inputClass}>
                    <option value="" />
                    {doctors?.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.fullName} — {d.speciality.name}
                      </option>
                    ))}
                  </select>
                )}
              </Field>
            </div>
            <Field label={t('staff.walkIn.reason')}>
              {(a) => <input {...a} value={reason} maxLength={500} onChange={(e) => setReason(e.target.value)} className={inputClass} />}
            </Field>
            <Button onClick={add} disabled={!doctorId || !branch || walkIn.isPending}>
              {walkIn.isPending ? t('staff.walkIn.adding') : t('staff.walkIn.add')}
            </Button>
          </div>
        )}
      </div>
    </Modal>
  )
}
