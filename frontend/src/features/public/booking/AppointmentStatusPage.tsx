import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'

import {
  useCancelAppointment,
  useLookupAppointment,
  usePublicSlots,
  useRescheduleAppointment,
  useSlotDays,
  type AppointmentView,
} from '../../../api/booking'
import { ApiError } from '../../../api/client'
import { useDoctor } from '../../../api/public'
import { Seo } from '../../../components/Seo'
import { Turnstile } from '../../../components/Turnstile'
import { Alert, Button, Field, inputClass } from '../../../components/ui'
import { shortDate, timeOf } from '../../../lib/dates'
import { formatTime } from '../../../lib/format'
import { AppointmentCard } from './AppointmentCard'
import { SlotPicker } from './SlotPicker'

const errorText = (e: unknown, fallback: string) => (e instanceof ApiError ? e.message : fallback)

/** "Check my booking" (FR-B3) with cancel and reschedule (FR-B3a), using booking reference + mobile. */
export function AppointmentStatusPage() {
  const { t } = useTranslation()
  const [params] = useSearchParams()
  const [ref, setRef] = useState(params.get('ref') ?? '')
  const [mobile, setMobile] = useState('')
  const [appointment, setAppointment] = useState<AppointmentView | null>(null)
  const [message, setMessage] = useState<{ kind: 'error' | 'success'; text: string } | null>(null)
  const [mode, setMode] = useState<'view' | 'cancel' | 'reschedule'>('view')
  const lookup = useLookupAppointment()

  const onLookup = async (e: React.FormEvent) => {
    e.preventDefault()
    setMessage(null)
    try {
      setAppointment(await lookup.mutateAsync({ bookingRef: ref.trim(), mobile: mobile.trim() }))
      setMode('view')
    } catch (err) {
      setMessage({ kind: 'error', text: errorText(err, t('staff.common.loadError')) })
    }
  }

  const updated = (a: AppointmentView, text: string) => {
    setAppointment(a)
    setMode('view')
    setMessage({ kind: 'success', text })
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <Seo title={t('seo.statusTitle')} />
      <h1 className="mb-2 text-3xl font-bold text-slate-900">{t('status.title')}</h1>

      {!appointment && (
        <form onSubmit={onLookup} className="space-y-4 rounded-xl border border-slate-200 bg-white p-5">
          <p className="text-slate-600">{t('status.intro')}</p>
          {message && <Alert kind={message.kind}>{message.text}</Alert>}
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label={t('status.ref')}>
              {(a) => <input {...a} required value={ref} onChange={(e) => setRef(e.target.value)} placeholder={t('status.refPlaceholder')} autoCapitalize="characters" className={`${inputClass} font-mono uppercase`} />}
            </Field>
            <Field label={t('status.mobile')}>
              {(a) => <input {...a} required type="tel" inputMode="tel" value={mobile} onChange={(e) => setMobile(e.target.value)} className={inputClass} />}
            </Field>
          </div>
          <Button type="submit" disabled={lookup.isPending}>
            {lookup.isPending ? t('status.finding') : t('status.find')}
          </Button>
        </form>
      )}

      {appointment && (
        <div className="space-y-5">
          {message && <Alert kind={message.kind}>{message.text}</Alert>}
          {appointment.needsAction && appointment.status === 'BOOKED' && (
            <div role="alert" className="rounded-lg border border-amber-300 bg-amber-50 p-4 text-sm text-amber-950">
              {t('status.needsAction')}
            </div>
          )}
          <AppointmentCard appointment={appointment} />

          {appointment.status === 'BOOKED' && (
            <p className="text-sm text-slate-600">
              {appointment.canCancel && appointment.changeDeadline
                ? t('status.deadline', { time: `${shortDate(appointment.changeDeadline.slice(0, 10))}, ${formatTime(timeOf(appointment.changeDeadline))}` })
                : t('status.tooLate', { phone: appointment.branch.phone ?? '' })}
            </p>
          )}

          {mode === 'view' && (
            <div className="flex flex-wrap gap-3 print:hidden">
              {appointment.canReschedule && <Button onClick={() => setMode('reschedule')}>{t('status.reschedule')}</Button>}
              {appointment.canCancel && (
                <Button variant="secondary" className="text-emergency-600" onClick={() => setMode('cancel')}>
                  {t('status.cancel')}
                </Button>
              )}
              <Button variant="link" onClick={() => { setAppointment(null); setMessage(null) }}>
                {t('status.another')}
              </Button>
            </div>
          )}

          {mode === 'cancel' && (
            <CancelPanel appointment={appointment} mobile={mobile} onDone={(a) => updated(a, t('status.cancelled'))} onClose={() => setMode('view')} />
          )}
          {mode === 'reschedule' && (
            <ReschedulePanel appointment={appointment} mobile={mobile} onDone={(a) => updated(a, t('status.moved'))} onClose={() => setMode('view')} />
          )}
        </div>
      )}
    </div>
  )
}

interface PanelProps {
  appointment: AppointmentView
  mobile: string
  onDone: (a: AppointmentView) => void
  onClose: () => void
}

function CancelPanel({ appointment, mobile, onDone, onClose }: PanelProps) {
  const { t } = useTranslation()
  const cancel = useCancelAppointment()
  const [reason, setReason] = useState('')
  const [token, setToken] = useState<string | null>(null)
  const [resetKey, setResetKey] = useState(0)
  const [error, setError] = useState<string | null>(null)

  const onConfirm = async () => {
    setError(null)
    try {
      onDone(await cancel.mutateAsync({ bookingRef: appointment.bookingRef, mobile, reason: reason.trim() || null, captchaToken: token }))
    } catch (e) {
      setResetKey((n) => n + 1)
      setError(errorText(e, t('staff.common.loadError')))
    }
  }

  return (
    <section className="space-y-4 rounded-xl border border-red-200 bg-white p-5" aria-label={t('status.cancelTitle')}>
      <h2 className="text-lg font-semibold">{t('status.cancelTitle')}</h2>
      {error && <Alert>{error}</Alert>}
      <Field label={t('status.cancelReason')}>
        {(a) => <input {...a} maxLength={300} value={reason} onChange={(e) => setReason(e.target.value)} className={inputClass} />}
      </Field>
      <Turnstile onToken={setToken} resetKey={resetKey} />
      <div className="flex gap-3">
        <Button variant="danger" disabled={cancel.isPending || !token} onClick={onConfirm}>
          {t('status.confirmCancel')}
        </Button>
        <Button variant="secondary" onClick={onClose}>
          {t('status.keep')}
        </Button>
      </div>
    </section>
  )
}

/** Same doctor, any of the doctor's hospitals (FR-B3a). */
function ReschedulePanel({ appointment, mobile, onDone, onClose }: PanelProps) {
  const { t } = useTranslation()
  const { data: doctor } = useDoctor(appointment.doctor.id)
  const [branch, setBranch] = useState(appointment.branch.code)
  const [date, setDate] = useState<string>()
  const [slotId, setSlotId] = useState<number>()
  const { data: days } = useSlotDays(appointment.doctor.id, branch)
  const { data: slots, isFetching } = usePublicSlots(appointment.doctor.id, branch, date)
  const reschedule = useRescheduleAppointment()
  const [token, setToken] = useState<string | null>(null)
  const [resetKey, setResetKey] = useState(0)
  const [error, setError] = useState<string | null>(null)

  const onConfirm = async () => {
    if (!slotId) return
    setError(null)
    try {
      onDone(await reschedule.mutateAsync({ bookingRef: appointment.bookingRef, mobile, newSlotId: slotId, captchaToken: token }))
    } catch (e) {
      setResetKey((n) => n + 1)
      setSlotId(undefined)
      setError(errorText(e, t('staff.common.loadError')))
    }
  }

  return (
    <section className="space-y-4 rounded-xl border border-slate-200 bg-white p-5" aria-label={t('status.reschedule')}>
      <h2 className="text-lg font-semibold">{t('status.rescheduleTitle', { doctor: appointment.doctor.fullName })}</h2>
      <p className="text-sm text-slate-600">{t('status.reschedulesLeft', { count: appointment.reschedulesLeft })}</p>
      {error && <Alert>{error}</Alert>}
      {doctor && doctor.branches.length > 1 && (
        <div className="flex flex-wrap gap-2" role="group" aria-label={t('booking.branch')}>
          {doctor.branches.map((b) => (
            <button
              key={b.code}
              type="button"
              aria-pressed={b.code === branch}
              onClick={() => {
                setBranch(b.code)
                setDate(undefined)
                setSlotId(undefined)
              }}
              className={`rounded-full border px-4 py-1.5 text-sm ${b.code === branch ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-300 bg-white'}`}
            >
              {b.city}
            </button>
          ))}
        </div>
      )}
      {days && (
        <SlotPicker
          days={days}
          date={date}
          onDate={(d) => {
            setDate(d)
            setSlotId(undefined)
          }}
          slots={slots?.map((s) => ({ id: s.slotId, startAt: s.startAt, tokenNo: s.tokenNo }))}
          slotsLoading={isFetching}
          slotId={slotId}
          onSlot={setSlotId}
        />
      )}
      <Turnstile onToken={setToken} resetKey={resetKey} />
      <div className="flex gap-3">
        <Button disabled={!slotId || !token || reschedule.isPending} onClick={onConfirm}>
          {t('status.confirmReschedule')}
        </Button>
        <Button variant="secondary" onClick={onClose}>
          {t('staff.common.cancel')}
        </Button>
      </div>
    </section>
  )
}
