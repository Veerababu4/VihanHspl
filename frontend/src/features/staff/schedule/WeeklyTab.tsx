import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'

import { useDeleteSchedule, useSaveSchedule, useSchedules, type Schedule } from '../../../api/schedule'
import { Alert, Button, Field, inputClass, Modal } from '../../../components/ui'
import { shortDate, todayIso } from '../../../lib/dates'
import { formatTime } from '../../../lib/format'
import { applyServerErrors } from '../../../lib/forms'
import type { ScheduleTabProps } from './SchedulePage'

const DAYS = [1, 2, 3, 4, 5, 6, 7]

/** Weekly timetable, one column per weekday (FR-S1). */
export function WeeklyTab({ doctorId, branches, onChanged }: ScheduleTabProps) {
  const { t } = useTranslation()
  const { data: schedules, isPending } = useSchedules(doctorId)
  const remove = useDeleteSchedule()
  const [editing, setEditing] = useState<Schedule | 'new' | null>(null)

  const onDelete = async (s: Schedule) => {
    if (window.confirm(t('staff.schedule.weekly.deleteConfirm'))) onChanged(await remove.mutateAsync(s.id))
  }

  if (isPending) return <p className="text-slate-500">{t('staff.common.loading')}</p>
  return (
    <>
      <div className="mb-4 flex justify-end">
        <Button onClick={() => setEditing('new')}>{t('staff.schedule.weekly.add')}</Button>
      </div>
      {schedules?.length === 0 && <p className="mb-4 text-slate-600">{t('staff.schedule.weekly.empty')}</p>}
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-7">
        {DAYS.map((day) => (
          <section key={day} aria-label={t(`days.${day}`)} className="rounded-xl border border-slate-200 bg-white p-3">
            <h3 className="mb-2 text-sm font-semibold text-slate-700">{t(`days.${day}`)}</h3>
            <ul className="space-y-2">
              {schedules
                ?.filter((s) => s.dayOfWeek === day)
                .map((s) => (
                  <li key={s.id} className="rounded-lg bg-brand-50 p-2 text-sm">
                    <p className="font-medium text-brand-900">
                      {formatTime(s.startTime)} – {formatTime(s.endTime)}
                    </p>
                    <p className="text-slate-600">
                      {s.branchCity} · {t('staff.schedule.weekly.perSession', { count: s.slotsPerSession })} · {s.slotMinutes}′
                    </p>
                    {(s.effectiveTo || s.effectiveFrom > todayIso()) && (
                      <p className="text-xs text-slate-500">
                        {s.effectiveFrom > todayIso() && t('staff.schedule.weekly.from', { date: shortDate(s.effectiveFrom) })}{' '}
                        {s.effectiveTo && t('staff.schedule.weekly.until', { date: shortDate(s.effectiveTo) })}
                      </p>
                    )}
                    <div className="mt-1 flex gap-3">
                      <Button variant="link" onClick={() => setEditing(s)} aria-label={`${t('staff.common.edit')} ${t(`days.${day}`)} ${s.startTime}`}>
                        {t('staff.common.edit')}
                      </Button>
                      <Button variant="link" className="text-emergency-600" onClick={() => onDelete(s)}>
                        {t('staff.schedule.weekly.delete')}
                      </Button>
                    </div>
                  </li>
                ))}
            </ul>
          </section>
        ))}
      </div>
      {editing && (
        <ScheduleForm
          doctorId={doctorId}
          branches={branches}
          schedule={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onChanged={(s) => {
            setEditing(null)
            onChanged(s)
          }}
        />
      )}
    </>
  )
}

const time = z.string().regex(/^([01]\d|2[0-3]):[0-5]\d$/, 'Use a time like 09:30')
const schema = z
  .object({
    branchId: z.string().min(1, 'Choose a branch'),
    dayOfWeek: z.string(),
    startTime: time,
    endTime: time,
    slotMinutes: z.coerce.number<string>().int().min(5, 'At least 5 minutes').max(120),
    effectiveFrom: z.string().min(1, 'Required'),
    effectiveTo: z.string(),
  })
  .refine((v) => v.endTime > v.startTime, { path: ['endTime'], message: 'Must be after the start time' })
  .refine((v) => !v.effectiveTo || v.effectiveTo >= v.effectiveFrom, { path: ['effectiveTo'], message: 'Must be on or after the from date' })
type FormIn = z.input<typeof schema>
type FormOut = z.output<typeof schema>

function ScheduleForm({
  doctorId,
  branches,
  schedule,
  onClose,
  onChanged,
}: ScheduleTabProps & { schedule: Schedule | null; onClose: () => void }) {
  const { t } = useTranslation()
  const save = useSaveSchedule(doctorId)
  const [message, setMessage] = useState<string | null>(null)
  const form = useForm<FormIn, unknown, FormOut>({
    resolver: zodResolver(schema),
    defaultValues: {
      branchId: String(schedule?.branchId ?? branches[0]?.id ?? ''),
      dayOfWeek: String(schedule?.dayOfWeek ?? 1),
      startTime: schedule?.startTime ?? '10:00',
      endTime: schedule?.endTime ?? '14:00',
      slotMinutes: String(schedule?.slotMinutes ?? 20),
      effectiveFrom: schedule?.effectiveFrom ?? todayIso(),
      effectiveTo: schedule?.effectiveTo ?? '',
    },
  })
  const errors = form.formState.errors

  const onSubmit = form.handleSubmit(async (v) => {
    setMessage(null)
    try {
      const result = await save.mutateAsync({
        id: schedule?.id,
        input: {
          branchId: Number(v.branchId),
          dayOfWeek: Number(v.dayOfWeek),
          startTime: v.startTime,
          endTime: v.endTime,
          slotMinutes: v.slotMinutes,
          effectiveFrom: v.effectiveFrom,
          effectiveTo: v.effectiveTo || null,
        },
      })
      onChanged(result.changes)
    } catch (e) {
      setMessage(applyServerErrors(e, form.setError))
    }
  })

  return (
    <Modal title={schedule ? t('staff.schedule.weekly.editTitle') : t('staff.schedule.weekly.addTitle')} onClose={onClose}>
      <form onSubmit={onSubmit} noValidate className="grid gap-4 sm:grid-cols-2">
        {message && (
          <div className="sm:col-span-2">
            <Alert>{message}</Alert>
          </div>
        )}
        <Field label={t('staff.schedule.weekly.branch')} error={errors.branchId?.message}>
          {(a) => (
            <select {...a} {...form.register('branchId')} className={inputClass}>
              {branches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.city}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label={t('staff.schedule.weekly.day')}>
          {(a) => (
            <select {...a} {...form.register('dayOfWeek')} className={inputClass}>
              {DAYS.map((d) => (
                <option key={d} value={d}>
                  {t(`days.${d}`)}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label={t('staff.schedule.weekly.start')} error={errors.startTime?.message}>
          {(a) => <input {...a} {...form.register('startTime')} type="time" className={inputClass} />}
        </Field>
        <Field label={t('staff.schedule.weekly.end')} error={errors.endTime?.message}>
          {(a) => <input {...a} {...form.register('endTime')} type="time" className={inputClass} />}
        </Field>
        <Field label={t('staff.schedule.weekly.slotMinutes')} error={errors.slotMinutes?.message}>
          {(a) => <input {...a} {...form.register('slotMinutes')} type="number" min={5} max={120} step={5} className={inputClass} />}
        </Field>
        <div />
        <Field label={t('staff.schedule.weekly.effectiveFrom')} error={errors.effectiveFrom?.message}>
          {(a) => <input {...a} {...form.register('effectiveFrom')} type="date" className={inputClass} />}
        </Field>
        <Field label={t('staff.schedule.weekly.effectiveTo')} error={errors.effectiveTo?.message} hint={t('staff.schedule.weekly.effectiveToHint')}>
          {(a) => <input {...a} {...form.register('effectiveTo')} type="date" className={inputClass} />}
        </Field>
        <div className="flex justify-end gap-2 sm:col-span-2">
          <Button variant="secondary" onClick={onClose}>
            {t('staff.common.cancel')}
          </Button>
          <Button type="submit" disabled={form.formState.isSubmitting}>
            {form.formState.isSubmitting ? t('staff.common.saving') : t('staff.common.save')}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
