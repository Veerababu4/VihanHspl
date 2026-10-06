import { useState } from 'react'
import { useTranslation } from 'react-i18next'

import { ApiError } from '../../../api/client'
import { useAddLeave, useLeaves, useRemoveLeave } from '../../../api/schedule'
import { Alert, Button, Field, inputClass } from '../../../components/ui'
import { shortDate, todayIso } from '../../../lib/dates'
import type { ScheduleTabProps } from './SchedulePage'

/** Doctor leave (FR-S4): adding it blocks slots and lists booked patients to call. */
export function LeaveTab({ doctorId, onChanged }: ScheduleTabProps) {
  const { t } = useTranslation()
  const { data: leaves, isPending } = useLeaves(doctorId)
  const add = useAddLeave(doctorId)
  const remove = useRemoveLeave()
  const [values, setValues] = useState({ fromDate: todayIso(), toDate: todayIso(), reason: '' })
  const [error, setError] = useState<string | null>(null)

  const onAdd = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      const result = await add.mutateAsync({ ...values, reason: values.reason.trim() || null })
      onChanged(result.changes)
      setValues({ fromDate: todayIso(), toDate: todayIso(), reason: '' })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : t('staff.common.loadError'))
    }
  }

  const onRemove = async (id: number) => {
    if (window.confirm(t('staff.schedule.leave.removeConfirm'))) onChanged(await remove.mutateAsync(id))
  }

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <form onSubmit={onAdd} className="space-y-4 rounded-xl border border-slate-200 bg-white p-5">
        <h3 className="font-semibold">{t('staff.schedule.leave.add')}</h3>
        {error && <Alert>{error}</Alert>}
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label={t('staff.schedule.leave.from')}>
            {(a) => <input {...a} type="date" required min={todayIso()} value={values.fromDate}
              onChange={(e) => setValues({ ...values, fromDate: e.target.value, toDate: values.toDate < e.target.value ? e.target.value : values.toDate })}
              className={inputClass} />}
          </Field>
          <Field label={t('staff.schedule.leave.to')}>
            {(a) => <input {...a} type="date" required min={values.fromDate} value={values.toDate}
              onChange={(e) => setValues({ ...values, toDate: e.target.value })} className={inputClass} />}
          </Field>
        </div>
        <Field label={t('staff.schedule.leave.reason')}>
          {(a) => <input {...a} maxLength={300} placeholder={t('staff.schedule.leave.reasonPlaceholder')} value={values.reason}
            onChange={(e) => setValues({ ...values, reason: e.target.value })} className={inputClass} />}
        </Field>
        <Button type="submit" disabled={add.isPending}>
          {add.isPending ? t('staff.common.saving') : t('staff.schedule.leave.add')}
        </Button>
      </form>

      <section className="rounded-xl border border-slate-200 bg-white p-5">
        {isPending && <p className="text-slate-500">{t('staff.common.loading')}</p>}
        {leaves?.length === 0 && <p className="text-slate-600">{t('staff.schedule.leave.empty')}</p>}
        <ul className="divide-y divide-slate-100">
          {leaves?.map((l) => (
            <li key={l.id} className="flex items-center justify-between gap-3 py-2.5 text-sm">
              <span>
                <span className="font-medium">
                  {shortDate(l.fromDate)}
                  {l.toDate !== l.fromDate && ` – ${shortDate(l.toDate)}`}
                </span>
                {l.reason && <span className="text-slate-500"> · {l.reason}</span>}
              </span>
              <Button variant="link" className="text-emergency-600" onClick={() => onRemove(l.id)}>
                {t('staff.schedule.leave.remove')}
              </Button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}
