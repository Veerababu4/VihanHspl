import { useState } from 'react'
import { useTranslation } from 'react-i18next'

import { useBranches, useDoctors } from '../../../api/public'
import { useStaffReschedule, type StaffAppointment } from '../../../api/reception'
import { useSlots } from '../../../api/schedule'
import { Alert, Button, Field, inputClass, Modal } from '../../../components/ui'
import { addDays, nowLocalIso, todayIso } from '../../../lib/dates'
import { SlotPicker } from '../../public/booking/SlotPicker'

const DAYS = 14

/**
 * Staff reschedule (FR-R6): any free future time of the same or another doctor, with a reason.
 * No cutoff, and it doesn't use up the patient's own two online changes.
 */
export function RescheduleDialog({
  appointment: a,
  onClose,
  onDone,
}: {
  appointment: StaffAppointment
  onClose: () => void
  onDone: (a: StaffAppointment) => void
}) {
  const { t } = useTranslation()
  const today = todayIso()
  const [now] = useState(nowLocalIso)
  const [branch, setBranch] = useState(a.branchCode ?? '')
  const [doctorId, setDoctorId] = useState<number | undefined>(a.doctorId)
  const [date, setDate] = useState<string>()
  const [slotId, setSlotId] = useState<number>()
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)
  const { data: branches } = useBranches()
  const { data: doctors } = useDoctors({ branch: branch || undefined })
  const branchId = branches?.find((b) => b.code === branch)?.id
  const { data: allSlots, isFetching } = useSlots(doctorId, today, addDays(today, DAYS - 1))
  const reschedule = useStaffReschedule()

  const open = (allSlots ?? []).filter((s) => s.status === 'OPEN' && s.branchId === branchId && s.startAt > now)
  const days = Array.from({ length: DAYS }, (_, i) => addDays(today, i)).map((d) => ({
    date: d,
    openSlots: open.filter((s) => s.date === d).length,
  }))

  const submit = async () => {
    if (!slotId) return
    if (!reason.trim()) {
      setError(t('staff.appointments.cancelReasonRequired'))
      return
    }
    setError(null)
    try {
      onDone(await reschedule.mutateAsync({ id: a.id, newSlotId: slotId, reason: reason.trim() }))
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
      setSlotId(undefined)
    }
  }

  return (
    <Modal title={t('staff.appointments.rescheduleTitle', { name: a.patientName })} onClose={onClose} wide>
      <div className="space-y-5">
        <p className="text-sm text-slate-600">{t('staff.appointments.rescheduleHint')}</p>
        {error && <Alert>{error}</Alert>}
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label={t('staff.appointments.branch')}>
            {(f) => (
              <select {...f} value={branch} onChange={(e) => { setBranch(e.target.value); setDoctorId(undefined); setDate(undefined); setSlotId(undefined) }} className={inputClass}>
                {branches?.map((b) => (
                  <option key={b.code} value={b.code}>
                    {b.city}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label={t('staff.appointments.doctor')}>
            {(f) => (
              <select {...f} value={doctorId ?? ''} onChange={(e) => { setDoctorId(Number(e.target.value) || undefined); setDate(undefined); setSlotId(undefined) }} className={inputClass}>
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
        {doctorId && branchId && (
          <SlotPicker
            days={days}
            date={date}
            onDate={(d) => { setDate(d); setSlotId(undefined) }}
            slots={date ? open.filter((s) => s.date === date).map((s) => ({ id: s.id, startAt: s.startAt, tokenNo: s.tokenNo })) : undefined}
            slotsLoading={isFetching}
            slotId={slotId}
            onSlot={setSlotId}
          />
        )}
        <Field label={t('staff.appointments.rescheduleReason')}>
          {(f) => <input {...f} value={reason} maxLength={300} onChange={(e) => setReason(e.target.value)} className={inputClass} />}
        </Field>
        <div className="flex gap-3">
          <Button onClick={submit} disabled={!slotId || reschedule.isPending}>
            {t('staff.appointments.confirmReschedule')}
          </Button>
          <Button variant="secondary" onClick={onClose}>
            {t('staff.appointments.keep')}
          </Button>
        </div>
      </div>
    </Modal>
  )
}
