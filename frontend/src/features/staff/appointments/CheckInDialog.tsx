import { useState } from 'react'
import { useTranslation } from 'react-i18next'

import { useCheckIn, usePatientMatches, type StaffAppointment } from '../../../api/reception'
import { Alert, Badge, Button, Modal } from '../../../components/ui'
import { RegisterPatientForm } from './RegisterPatientForm'

/**
 * Check-in links the booking to a registered patient (FR-R4, DESIGN.md §19.5): pick one of the
 * suggested matches, or register a new patient pre-filled from what the booker typed.
 */
export function CheckInDialog({
  appointment: a,
  onClose,
  onDone,
}: {
  appointment: StaffAppointment
  onClose: () => void
  onDone: (a: StaffAppointment) => void
}) {
  const { t } = useTranslation()
  const matches = usePatientMatches(a.id)
  const checkIn = useCheckIn()
  const [registering, setRegistering] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // Booked with a UHID whose mobile matched (DESIGN.md §19.5): offer that patient first.
  const prelinked = a.patientId != null && a.uhid ? a.patientId : null

  const run = async (patientId: number) => {
    setError(null)
    try {
      onDone(await checkIn.mutateAsync({ id: a.id, patientId }))
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    }
  }

  return (
    <Modal title={t('staff.checkIn.title', { name: a.patientName })} onClose={onClose} wide>
      <div className="space-y-5">
        <p className="text-sm text-slate-600">
          {t('staff.checkIn.booking', {
            name: a.patientName,
            age: a.patientAge ?? '?',
            gender: a.patientGender ? t(`booking.genders.${a.patientGender}`) : '?',
            mobile: a.mobile,
          })}
        </p>
        {error && <Alert>{error}</Alert>}

        {prelinked !== null && !registering && (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-emerald-200 bg-emerald-50 p-3">
            <p className="text-sm font-medium text-emerald-900">{t('staff.checkIn.prelinked', { name: a.patientName, uhid: a.uhid })}</p>
            <Button onClick={() => run(prelinked)} disabled={checkIn.isPending}>
              {checkIn.isPending ? t('staff.checkIn.checkingIn') : t('staff.appointments.checkIn')}
            </Button>
          </div>
        )}

        {!registering && (
          <section aria-labelledby="matches-heading">
            <h3 id="matches-heading" className="mb-2 font-semibold text-slate-900">
              {t('staff.checkIn.matches')}
            </h3>
            {matches.isPending && <p className="text-sm text-slate-500">{t('staff.common.loading')}</p>}
            {matches.error && <Alert>{matches.error.message}</Alert>}
            {matches.data &&
              (matches.data.length === 0 ? (
                  <p className="text-sm text-slate-600">{t('staff.checkIn.noMatches')}</p>
                ) : (
                  <ul className="divide-y divide-slate-200 rounded-lg border border-slate-200">
                    {matches.data.map((m) => (
                      <li key={m.patient.id} className="flex flex-wrap items-center justify-between gap-3 p-3">
                        <div>
                          <p className="font-medium">
                            {m.patient.fullName} <span className="text-sm font-normal text-slate-500">· {m.patient.uhid}</span>
                          </p>
                          <p className="text-sm text-slate-600">
                            {m.patient.age} y · {t(`booking.genders.${m.patient.gender}`)} · {m.patient.mobile}
                          </p>
                          <div className="mt-1 flex flex-wrap gap-1">
                            {(['sameMobile', 'sameName', 'ageClose'] as const)
                              .filter((k) => m[k])
                              .map((k) => (
                                <Badge key={k} tone="good">
                                  {t(`staff.checkIn.why.${k}`)}
                                </Badge>
                              ))}
                          </div>
                        </div>
                        <Button onClick={() => run(m.patient.id)} disabled={checkIn.isPending}>
                          {checkIn.isPending ? t('staff.checkIn.checkingIn') : t('staff.checkIn.pick')}
                        </Button>
                      </li>
                    ))}
                  </ul>
                ))}
            <Button variant="secondary" className="mt-4" onClick={() => setRegistering(true)}>
              {t('staff.checkIn.registerNew')}
            </Button>
          </section>
        )}

        {registering && (
          <section aria-label={t('staff.patient.title')}>
            <RegisterPatientForm
              prefill={{ fullName: a.patientName, age: a.patientAge, gender: a.patientGender, mobile: a.mobile, branch: a.branchCode }}
              submitLabel={t('staff.checkIn.registerNew')}
              onRegistered={(p) => run(p.id)}
              onUseExisting={(p) => run(p.id)}
            />
          </section>
        )}
      </div>
    </Modal>
  )
}
