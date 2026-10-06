import { useState } from 'react'
import { useTranslation } from 'react-i18next'

import { useAdminBranches } from '../../../api/admin'
import { ApiError } from '../../../api/client'
import { useAddHoliday, useHolidays, useRemoveHoliday, type ChangeSummary } from '../../../api/schedule'
import { Alert, Button, Field, inputClass } from '../../../components/ui'
import { shortDate, todayIso } from '../../../lib/dates'
import { ChangeSummaryNotice } from '../schedule/ChangeSummaryNotice'

/** Branch holidays (FR-S5), on the Branches & specialities page. */
export function HolidaysSection() {
  const { t } = useTranslation()
  const { data: holidays } = useHolidays()
  const { data: branches } = useAdminBranches()
  const add = useAddHoliday()
  const remove = useRemoveHoliday()
  const [values, setValues] = useState({ branchId: '', date: todayIso(), name: '' })
  const [error, setError] = useState<string | null>(null)
  const [summary, setSummary] = useState<ChangeSummary | null>(null)

  const onAdd = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      const result = await add.mutateAsync({ branchId: values.branchId ? Number(values.branchId) : null, date: values.date, name: values.name.trim() })
      setSummary(result.changes)
      setValues({ ...values, name: '' })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : t('staff.common.loadError'))
    }
  }

  const onRemove = async (id: number) => {
    if (window.confirm(t('staff.schedule.holidays.removeConfirm'))) setSummary(await remove.mutateAsync(id))
  }

  return (
    <section className="mt-10">
      <h2 className="mb-3 text-lg font-semibold">{t('staff.schedule.holidays.title')}</h2>
      {summary && <ChangeSummaryNotice summary={summary} />}
      <div className="grid gap-6 lg:grid-cols-2">
        <form onSubmit={onAdd} className="space-y-4 rounded-xl border border-slate-200 bg-white p-5">
          {error && <Alert>{error}</Alert>}
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label={t('staff.schedule.holidays.branch')}>
              {(a) => (
                <select {...a} value={values.branchId} onChange={(e) => setValues({ ...values, branchId: e.target.value })} className={inputClass}>
                  <option value="">{t('staff.schedule.holidays.allBranches')}</option>
                  {branches?.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.city}
                    </option>
                  ))}
                </select>
              )}
            </Field>
            <Field label={t('staff.schedule.holidays.date')}>
              {(a) => <input {...a} type="date" required min={todayIso()} value={values.date} onChange={(e) => setValues({ ...values, date: e.target.value })} className={inputClass} />}
            </Field>
          </div>
          <Field label={t('staff.schedule.holidays.name')}>
            {(a) => <input {...a} required maxLength={120} value={values.name} onChange={(e) => setValues({ ...values, name: e.target.value })} className={inputClass} />}
          </Field>
          <Button type="submit" disabled={add.isPending}>
            {add.isPending ? t('staff.common.saving') : t('staff.schedule.holidays.add')}
          </Button>
        </form>
        <div className="rounded-xl border border-slate-200 bg-white p-5">
          {holidays?.length === 0 && <p className="text-slate-600">{t('staff.schedule.holidays.empty')}</p>}
          <ul className="divide-y divide-slate-100">
            {holidays?.map((h) => (
              <li key={h.id} className="flex items-center justify-between gap-3 py-2.5 text-sm">
                <span>
                  <span className="font-medium">{shortDate(h.date)}</span> · {h.name} ·{' '}
                  <span className="text-slate-500">
                    {h.branchId ? branches?.find((b) => b.id === h.branchId)?.city : t('staff.schedule.holidays.allBranches')}
                  </span>
                </span>
                <Button variant="link" className="text-emergency-600" onClick={() => onRemove(h.id)}>
                  {t('staff.schedule.holidays.remove')}
                </Button>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  )
}
