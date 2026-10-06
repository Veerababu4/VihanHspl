import { useTranslation } from 'react-i18next'

import type { ChangeSummary } from '../../../api/schedule'
import { Alert } from '../../../components/ui'
import { shortDate, timeOf } from '../../../lib/dates'

/** "6 slots created · 2 removed", plus the booked patients reception must phone (FR-S4, FR-R7). */
export function ChangeSummaryNotice({ summary }: { summary: ChangeSummary }) {
  const { t } = useTranslation()
  const parts = [
    summary.slotsCreated && t('staff.schedule.summary.created', { count: summary.slotsCreated }),
    summary.slotsRemoved && t('staff.schedule.summary.removed', { count: summary.slotsRemoved }),
    summary.slotsBlocked && t('staff.schedule.summary.blocked', { count: summary.slotsBlocked }),
    summary.slotsReopened && t('staff.schedule.summary.reopened', { count: summary.slotsReopened }),
  ].filter(Boolean)

  return (
    <div className="mb-4 space-y-3">
      <Alert kind="success">{parts.length ? parts.join(' · ') : t('staff.schedule.summary.nothing')}</Alert>
      {summary.affectedBookings.length > 0 && (
        <div role="alert" className="rounded-lg border border-amber-300 bg-amber-50 p-4 text-sm text-amber-950">
          <p className="font-semibold">{t('staff.schedule.summary.callTitle', { count: summary.affectedBookings.length })}</p>
          <p className="mb-2">{t('staff.schedule.summary.callText')}</p>
          <ul className="space-y-1">
            {summary.affectedBookings.map((b) => (
              <li key={b.bookingRef} className="font-mono text-xs">
                {b.startAt && `${shortDate(b.startAt.slice(0, 10))} ${timeOf(b.startAt)}`} · {b.branchCity} · {b.patientName} · {b.mobile} ·{' '}
                {b.bookingRef}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
