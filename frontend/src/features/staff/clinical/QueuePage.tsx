import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { useQueue, type QueueEntry } from '../../../api/clinical'
import { Seo } from '../../../components/Seo'
import { Alert, Badge, Field, inputClass, PageHeader } from '../../../components/ui'
import { timeOf, todayIso } from '../../../lib/dates'
import { formatTime } from '../../../lib/format'

/** The doctor's own checked-in patients for a day, waiting ones in token order (FR-C1). Refreshes every minute. */
export function QueuePage() {
  const { t } = useTranslation()
  const [date, setDate] = useState(todayIso)
  const queue = useQueue(date)

  return (
    <>
      <Seo title={t('staff.queue.title')} />
      <PageHeader title={t('staff.queue.title')} />
      <div className="mb-5 max-w-xs">
        <Field label={t('staff.queue.date')}>
          {(f) => <input {...f} type="date" value={date} onChange={(e) => setDate(e.target.value || todayIso())} className={inputClass} />}
        </Field>
      </div>
      {queue.isPending && <p className="text-slate-500">{t('staff.common.loading')}</p>}
      {queue.error && <Alert>{queue.error.message}</Alert>}
      {queue.data && (
        <div className="space-y-8">
          <section aria-labelledby="waiting">
            <h2 id="waiting" className="mb-2 text-lg font-semibold text-slate-900">
              {t('staff.queue.waiting', { count: queue.data.waiting.length })}
            </h2>
            {queue.data.waiting.length === 0 ? (
              <p className="rounded-xl border border-slate-200 bg-white p-6 text-slate-600">{t('staff.queue.empty')}</p>
            ) : (
              <ul className="space-y-2">
                {queue.data.waiting.map((e) => <QueueRow key={e.appointmentId} entry={e} action={t('staff.queue.open')} />)}
              </ul>
            )}
          </section>
          <section aria-labelledby="done">
            <h2 id="done" className="mb-2 text-lg font-semibold text-slate-900">
              {t('staff.queue.done', { count: queue.data.done.length })}
            </h2>
            {queue.data.done.length === 0 ? (
              <p className="text-slate-600">{t('staff.queue.noneDone')}</p>
            ) : (
              <ul className="space-y-2">
                {queue.data.done.map((e) => <QueueRow key={e.appointmentId} entry={e} action={t('staff.queue.view')} />)}
              </ul>
            )}
          </section>
        </div>
      )}
    </>
  )
}

function QueueRow({ entry: e, action }: { entry: QueueEntry; action: string }) {
  const { t } = useTranslation()
  return (
    <li className="flex flex-wrap items-center gap-x-5 gap-y-2 rounded-xl border border-slate-200 bg-white p-4" aria-label={`${e.patientName}, token ${e.tokenNo ?? '—'}`}>
      <span className="w-12 text-2xl font-bold text-brand-700">{e.tokenNo ?? '—'}</span>
      <div className="min-w-0 flex-1 basis-56">
        <p className="font-semibold text-slate-900">
          {e.patientName}{' '}
          <span className="text-sm font-normal text-slate-600">
            {e.age != null && `${e.age} y`}
            {e.gender && ` · ${t(`booking.genders.${e.gender}`)}`}
            {e.uhid && ` · ${e.uhid}`}
          </span>
        </p>
        <p className="text-sm text-slate-600">
          {e.startAt ? formatTime(timeOf(e.startAt)) : t('staff.queue.walkIn')}
          {e.checkedInAt && ` · ${t('staff.queue.checkedIn', { time: formatTime(timeOf(e.checkedInAt)) })}`}
          {e.branchCity && ` · ${e.branchCity}`}
          {e.reason && ` · ${e.reason}`}
        </p>
        {e.hasAllergies && (
          <div className="mt-1">
            <Badge tone="bad">{t('staff.queue.allergies')}</Badge>
          </div>
        )}
      </div>
      {e.visitId && (
        <Link to={`/staff/visits/${e.visitId}`} className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
          {action}
        </Link>
      )}
    </li>
  )
}
