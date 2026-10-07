import { zodResolver } from '@hookform/resolvers/zod'
import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'

import { useStaffBook, type AppointmentView } from '../../../api/booking'
import { ApiError } from '../../../api/client'
import { useBranches, useDoctors } from '../../../api/public'
import { useSlots } from '../../../api/schedule'
import { Seo } from '../../../components/Seo'
import { Alert, Button, inputClass, PageHeader } from '../../../components/ui'
import { addDays, nowLocalIso, todayIso } from '../../../lib/dates'
import { applyServerErrors } from '../../../lib/forms'
import { AppointmentCard } from '../../public/booking/AppointmentCard'
import { emptyPatient, normalizeMobile, patientSchema, type PatientFormInput, type PatientFormOutput } from '../../public/booking/patientSchema'
import { PatientFields } from '../../public/booking/PatientFields'
import { SlotPicker } from '../../public/booking/SlotPicker'

const DAYS = 14

/** Counter / phone booking by reception (FR-R3): any open future slot, no captcha. */
export function CounterBookingPage() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const today = todayIso()
  const [branch, setBranch] = useState<string>()
  const [doctorId, setDoctorId] = useState<number>()
  const [date, setDate] = useState<string>()
  const [slotId, setSlotId] = useState<number>()
  const [channel, setChannel] = useState<'COUNTER' | 'PHONE'>('COUNTER')
  const [booked, setBooked] = useState<AppointmentView | null>(null)
  const [error, setError] = useState<string | null>(null)

  const { data: branches } = useBranches()
  const { data: doctors } = useDoctors({ branch })
  const branchId = branches?.find((b) => b.code === branch)?.id
  const { data: allSlots, isFetching } = useSlots(doctorId, today, addDays(today, DAYS - 1))
  const book = useStaffBook()
  const form = useForm<PatientFormInput, unknown, PatientFormOutput>({ resolver: zodResolver(patientSchema(t)), defaultValues: emptyPatient })

  // Open, future slots at the chosen branch, and how many per day. "Now" is taken once when the page
  // opens; the server refuses a slot that has passed since.
  const [now] = useState(nowLocalIso)
  const open = (allSlots ?? []).filter((s) => s.status === 'OPEN' && s.branchId === branchId && s.startAt > now)
  const days = Array.from({ length: DAYS }, (_, i) => addDays(today, i)).map((d) => ({
    date: d,
    openSlots: open.filter((s) => s.date === d).length,
  }))

  const reset = () => {
    setBooked(null)
    setSlotId(undefined)
    form.reset(emptyPatient)
  }

  const onSubmit = form.handleSubmit(async (v) => {
    if (!slotId) return
    setError(null)
    try {
      const result = await book.mutateAsync({
        slotId,
        channel,
        patientName: v.patientName,
        patientAge: v.patientAge,
        patientGender: v.patientGender,
        mobile: normalizeMobile(v.mobile) ?? v.mobile,
        reason: v.reason || null,
        uhid: v.uhid || null,
        consent: v.consent,
      })
      await queryClient.invalidateQueries({ queryKey: ['slots'] })
      setBooked(result)
    } catch (e) {
      if (e instanceof ApiError && e.code === 'SLOT_TAKEN') {
        await queryClient.invalidateQueries({ queryKey: ['slots'] })
        setSlotId(undefined)
      }
      setError(applyServerErrors(e, form.setError))
    }
  })

  if (booked) {
    return (
      <div className="max-w-3xl">
        <Seo title={t('staff.booking.title')} />
        <PageHeader title={t('staff.booking.done')} />
        <AppointmentCard appointment={booked} />
        <div className="mt-5 flex gap-3 print:hidden">
          <Button onClick={() => window.print()}>{t('confirmation.print')}</Button>
          <Button variant="secondary" onClick={reset}>
            {t('staff.booking.another')}
          </Button>
        </div>
      </div>
    )
  }

  return (
    <>
      <Seo title={t('staff.booking.title')} />
      <PageHeader title={t('staff.booking.title')} />
      <div className="space-y-6">
        <div className="grid max-w-2xl gap-4 sm:grid-cols-2">
          <label className="text-sm font-medium">
            {t('booking.branch')}
            <select className={`${inputClass} mt-1`} value={branch ?? ''} onChange={(e) => { setBranch(e.target.value || undefined); setDoctorId(undefined); setDate(undefined); setSlotId(undefined) }}>
              <option value="">{t('booking.chooseBranch')}</option>
              {branches?.map((b) => (
                <option key={b.code} value={b.code}>
                  {b.city}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-medium">
            {t('booking.doctor')}
            <select className={`${inputClass} mt-1`} disabled={!branch} value={doctorId ?? ''} onChange={(e) => { setDoctorId(Number(e.target.value) || undefined); setDate(undefined); setSlotId(undefined) }}>
              <option value="" />
              {doctors?.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.fullName} — {d.speciality.name}
                </option>
              ))}
            </select>
          </label>
        </div>

        {doctorId && branchId && (
          <section className="rounded-xl border border-slate-200 bg-white p-5">
            <SlotPicker
              days={days}
              date={date}
              onDate={(d) => { setDate(d); setSlotId(undefined) }}
              slots={date ? open.filter((s) => s.date === date).map((s) => ({ id: s.id, startAt: s.startAt, tokenNo: s.tokenNo })) : undefined}
              slotsLoading={isFetching}
              slotId={slotId}
              onSlot={setSlotId}
            />
          </section>
        )}

        {slotId && (
          <form onSubmit={onSubmit} noValidate className="max-w-2xl space-y-5 rounded-xl border border-slate-200 bg-white p-5">
            {error && <Alert>{error}</Alert>}
            <fieldset>
              <legend className="mb-1 text-sm font-medium text-slate-700">{t('staff.booking.channel')}</legend>
              <div className="flex gap-4">
                {(['COUNTER', 'PHONE'] as const).map((c) => (
                  <label key={c} className="flex items-center gap-1.5 text-sm">
                    <input type="radio" name="channel" checked={channel === c} onChange={() => setChannel(c)} className="h-4 w-4" />
                    {t(`staff.booking.channels.${c}`)}
                  </label>
                ))}
              </div>
            </fieldset>
            <PatientFields form={form} consentLabel={t('staff.booking.consent')} mobileHint={t('staff.booking.mobileHint')} />
            <Button type="submit" disabled={book.isPending}>
              {book.isPending ? t('booking.booking') : t('booking.confirm')}
            </Button>
          </form>
        )}
      </div>
    </>
  )
}
