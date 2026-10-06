import { useState } from 'react'
import { useTranslation } from 'react-i18next'

import { ApiError } from '../../../api/client'
import {
  useAddSession,
  useGenerateSlots,
  useSetDayStatus,
  useSetSlotStatus,
  useSlots,
  type Slot,
} from '../../../api/schedule'
import { Alert, Button, Field, inputClass, Modal } from '../../../components/ui'
import { addDays, shortDate, timeOf, todayIso } from '../../../lib/dates'
import type { ScheduleTabProps } from './SchedulePage'

const WINDOW_DAYS = 14

const slotStyle: Record<Slot['status'], string> = {
  OPEN: 'border-emerald-300 bg-emerald-50 text-emerald-900 hover:bg-emerald-100',
  BOOKED: 'border-sky-300 bg-sky-100 text-sky-900 cursor-default',
  BLOCKED: 'border-slate-300 bg-slate-100 text-slate-500 line-through hover:bg-slate-200',
}

/** The next 14 days of slots: block / reopen, block a day, extra sessions, generate (FR-S2, FR-S3). */
export function SlotsTab({ doctorId, branches, onChanged }: ScheduleTabProps) {
  const { t } = useTranslation()
  const today = todayIso()
  const days = Array.from({ length: WINDOW_DAYS }, (_, i) => addDays(today, i))
  const [day, setDay] = useState(days[0])
  const { data: slots, isPending } = useSlots(doctorId, days[0], days[WINDOW_DAYS - 1])
  const setStatus = useSetSlotStatus()
  const setDayStatus = useSetDayStatus(doctorId)
  const generate = useGenerateSlots(doctorId)
  const [error, setError] = useState<string | null>(null)
  const [adding, setAdding] = useState(false)

  const daySlots = slots?.filter((s) => s.date === day) ?? []
  const countFor = (d: string) => slots?.filter((s) => s.date === d && s.status === 'OPEN').length ?? 0
  const branchCity = (id: number) => branches.find((b) => b.id === id)?.city ?? ''

  const run = async (action: () => Promise<unknown>) => {
    setError(null)
    try {
      await action()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : t('staff.common.loadError'))
    }
  }

  const toggle = (s: Slot) => {
    if (s.status === 'BOOKED') return
    void run(() => setStatus.mutateAsync({ id: s.id, status: s.status === 'OPEN' ? 'BLOCKED' : 'OPEN' }))
  }

  return (
    <>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm text-slate-600">{t('staff.schedule.slots.clickHint')}</p>
        <Button
          variant="secondary"
          disabled={generate.isPending}
          onClick={() => run(async () => onChanged(await generate.mutateAsync({ from: today, to: days[WINDOW_DAYS - 1] })))}
        >
          {t('staff.schedule.slots.generate')}
        </Button>
      </div>
      {error && (
        <div className="mb-4">
          <Alert>{error}</Alert>
        </div>
      )}

      <div role="tablist" aria-label={t('staff.schedule.tabs.slots')} className="mb-4 flex gap-2 overflow-x-auto pb-2">
        {days.map((d) => (
          <button
            key={d}
            role="tab"
            type="button"
            aria-selected={d === day}
            onClick={() => setDay(d)}
            className={`shrink-0 rounded-lg border px-3 py-2 text-center text-sm ${
              d === day ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-200 bg-white hover:border-brand-500'
            }`}
          >
            <span className="block font-medium">{shortDate(d)}</span>
            <span className={`block text-xs ${d === day ? 'text-white/80' : 'text-slate-500'}`}>{countFor(d)} open</span>
          </button>
        ))}
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-4">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <h3 className="font-semibold">{shortDate(day)}</h3>
          <div className="flex flex-wrap gap-2">
            <Button variant="secondary" onClick={() => setAdding(true)}>
              {t('staff.schedule.slots.addSession')}
            </Button>
            <Button variant="secondary" disabled={!daySlots.some((s) => s.status === 'OPEN')}
              onClick={() => run(() => setDayStatus.mutateAsync({ date: day, blocked: true }))}>
              {t('staff.schedule.slots.blockDay')}
            </Button>
            <Button variant="secondary" disabled={!daySlots.some((s) => s.status === 'BLOCKED' && s.blockReason === 'MANUAL')}
              onClick={() => run(() => setDayStatus.mutateAsync({ date: day, blocked: false }))}>
              {t('staff.schedule.slots.unblockDay')}
            </Button>
          </div>
        </div>
        {isPending && <p className="text-slate-500">{t('staff.common.loading')}</p>}
        {!isPending && daySlots.length === 0 && <p className="text-slate-600">{t('staff.schedule.slots.none')}</p>}
        {[...new Set(daySlots.map((s) => s.branchId))].map((branchId) => (
          <div key={branchId} className="mb-3">
            <p className="mb-2 text-sm font-medium text-slate-700">{branchCity(branchId)}</p>
            <ul className="flex flex-wrap gap-2">
              {daySlots
                .filter((s) => s.branchId === branchId)
                .map((s) => {
                  const status = s.status === 'BLOCKED' && s.blockReason
                    ? t(`staff.schedule.slots.reason.${s.blockReason}`)
                    : t(`staff.schedule.slots.legend${s.status[0]}${s.status.slice(1).toLowerCase()}`)
                  return (
                    <li key={s.id}>
                      <button
                        type="button"
                        onClick={() => toggle(s)}
                        disabled={s.status === 'BOOKED' || setStatus.isPending}
                        aria-label={t('staff.schedule.slots.slotLabel', { time: timeOf(s.startAt), token: s.tokenNo, status })}
                        title={status}
                        className={`rounded-md border px-2.5 py-1.5 text-xs font-medium ${slotStyle[s.status]}`}
                      >
                        {timeOf(s.startAt)} <span className="opacity-60">#{s.tokenNo}</span>
                      </button>
                    </li>
                  )
                })}
            </ul>
          </div>
        ))}
        <p className="mt-2 flex gap-4 text-xs text-slate-500">
          <span>🟩 {t('staff.schedule.slots.legendOpen')}</span>
          <span>🟦 {t('staff.schedule.slots.legendBooked')}</span>
          <span>⬜ {t('staff.schedule.slots.legendBlocked')}</span>
        </p>
      </div>

      {adding && <ExtraSessionForm doctorId={doctorId} branches={branches} date={day} onClose={() => setAdding(false)} />}
    </>
  )
}

function ExtraSessionForm({ doctorId, branches, date, onClose }: Omit<ScheduleTabProps, 'onChanged'> & { date: string; onClose: () => void }) {
  const { t } = useTranslation()
  const add = useAddSession(doctorId)
  const [values, setValues] = useState({ branchId: String(branches[0].id), startTime: '17:00', endTime: '19:00', slotMinutes: '20' })
  const [error, setError] = useState<string | null>(null)

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      await add.mutateAsync({ branchId: Number(values.branchId), date, startTime: values.startTime, endTime: values.endTime, slotMinutes: Number(values.slotMinutes) })
      onClose()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : t('staff.common.loadError'))
    }
  }

  return (
    <Modal title={t('staff.schedule.slots.addTitle', { date: shortDate(date) })} onClose={onClose}>
      <form onSubmit={onSubmit} className="grid gap-4 sm:grid-cols-2">
        {error && (
          <div className="sm:col-span-2">
            <Alert>{error}</Alert>
          </div>
        )}
        <Field label={t('staff.schedule.weekly.branch')}>
          {(a) => (
            <select {...a} value={values.branchId} onChange={(e) => setValues({ ...values, branchId: e.target.value })} className={inputClass}>
              {branches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.city}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label={t('staff.schedule.weekly.slotMinutes')}>
          {(a) => <input {...a} type="number" min={5} max={120} step={5} value={values.slotMinutes} onChange={(e) => setValues({ ...values, slotMinutes: e.target.value })} className={inputClass} />}
        </Field>
        <Field label={t('staff.schedule.weekly.start')}>
          {(a) => <input {...a} type="time" required value={values.startTime} onChange={(e) => setValues({ ...values, startTime: e.target.value })} className={inputClass} />}
        </Field>
        <Field label={t('staff.schedule.weekly.end')}>
          {(a) => <input {...a} type="time" required value={values.endTime} onChange={(e) => setValues({ ...values, endTime: e.target.value })} className={inputClass} />}
        </Field>
        <div className="flex justify-end gap-2 sm:col-span-2">
          <Button variant="secondary" onClick={onClose}>
            {t('staff.common.cancel')}
          </Button>
          <Button type="submit" disabled={add.isPending}>
            {add.isPending ? t('staff.common.saving') : t('staff.common.save')}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
