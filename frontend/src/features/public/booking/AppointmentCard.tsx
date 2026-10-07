import { useTranslation } from 'react-i18next'

import type { AppointmentView } from '../../../api/booking'
import { Badge } from '../../../components/ui'
import { shortDate, timeOf } from '../../../lib/dates'
import { formatFee, formatTime } from '../../../lib/format'

const statusTone = { BOOKED: 'good', CHECKED_IN: 'good', COMPLETED: 'neutral', CANCELLED: 'bad', NO_SHOW: 'warn' } as const

/** The booking at a glance: reference and token large, then who / when / where (FR-B2). Prints well. */
export function AppointmentCard({ appointment: a }: { appointment: AppointmentView }) {
  const { t } = useTranslation()
  const fee = formatFee(a.doctor.consultationFee)
  return (
    <article className="overflow-hidden rounded-2xl border border-slate-200 bg-white print:border-slate-400">
      <div className="flex flex-wrap items-center justify-between gap-4 bg-brand-50 px-6 py-5">
        <div>
          <p className="text-xs font-semibold tracking-wide text-brand-700 uppercase">{t('confirmation.ref')}</p>
          <p className="font-mono text-2xl font-bold tracking-wider text-slate-900" data-testid="booking-ref">
            {a.bookingRef}
          </p>
        </div>
        {a.tokenNo != null && (
          <div className="text-right">
            <p className="text-xs font-semibold tracking-wide text-brand-700 uppercase">{t('confirmation.token')}</p>
            <p className="text-4xl font-bold text-brand-700" data-testid="token">
              {a.tokenNo}
            </p>
          </div>
        )}
      </div>
      <dl className="grid gap-4 px-6 py-5 sm:grid-cols-2">
        <div>
          <dt className="text-sm text-slate-500">{t('confirmation.when')}</dt>
          <dd className="font-semibold">
            {shortDate(a.date)}, {a.startAt ? formatTime(timeOf(a.startAt)) : t('confirmation.walkIn')}
          </dd>
        </div>
        <div>
          <dt className="text-sm text-slate-500">{t('confirmation.doctor')}</dt>
          <dd className="font-semibold">
            {a.doctor.fullName}
            <span className="block text-sm font-normal text-slate-600">{a.doctor.speciality}</span>
          </dd>
        </div>
        <div>
          <dt className="text-sm text-slate-500">{t('confirmation.where')}</dt>
          <dd>
            <span className="font-semibold">{a.branch.name}</span>
            <span className="block text-sm text-slate-600">
              {a.branch.address}, {a.branch.city}
              {a.branch.phone && ` · ${a.branch.phone}`}
            </span>
          </dd>
        </div>
        <div>
          <dt className="text-sm text-slate-500">{t('confirmation.patient')}</dt>
          <dd>
            <span className="font-semibold">{a.patientName}</span>
            <span className="block text-sm text-slate-600">
              {[a.patientAge != null && `${a.patientAge} y`, a.patientGender && t(`booking.genders.${a.patientGender}`), a.mobileMasked]
                .filter(Boolean)
                .join(' · ')}
            </span>
          </dd>
        </div>
      </dl>
      <div className="space-y-1 border-t border-slate-100 px-6 py-4 text-sm">
        <p>
          <Badge tone={statusTone[a.status]}>{t(`status.statusLabel.${a.status}`)}</Badge>
        </p>
        {a.status === 'BOOKED' && <p className="font-medium text-slate-800">{t('confirmation.arrive')}</p>}
        {a.status === 'BOOKED' && fee && <p className="text-slate-600">{t('confirmation.pay', { fee })}</p>}
        {a.cancelReason && <p className="text-slate-600">{t('status.cancelReasonShown', { reason: a.cancelReason })}</p>}
        {a.uhid && <p className="text-slate-600">{t('confirmation.uhid', { uhid: a.uhid })}</p>}
      </div>
    </article>
  )
}
