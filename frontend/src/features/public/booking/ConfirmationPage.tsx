import { useTranslation } from 'react-i18next'
import { Link, useLocation, useParams } from 'react-router'

import type { AppointmentView } from '../../../api/booking'
import { Seo } from '../../../components/Seo'
import { Button } from '../../../components/ui'
import { AppointmentCard } from './AppointmentCard'

/** FR-B2: reference, token, doctor, branch, time and "arrive 15 min early"; printable. */
export function ConfirmationPage() {
  const { t } = useTranslation()
  const ref = useParams().ref ?? ''
  const appointment = (useLocation().state as { appointment?: AppointmentView } | null)?.appointment

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <Seo title={t('confirmation.title')} />
      <h1 className="text-3xl font-bold text-slate-900">✅ {t('confirmation.title')}</h1>
      <p className="mt-1 mb-6 text-slate-600 print:hidden">{t('confirmation.subtitle')}</p>

      {appointment ? (
        <AppointmentCard appointment={appointment} />
      ) : (
        // Page reloaded: the details aren't kept in the browser, so point to the lookup page.
        <p className="rounded-xl border border-slate-200 bg-white p-6">{t('confirmation.missing', { ref })}</p>
      )}

      <div className="mt-6 flex flex-wrap gap-3 print:hidden">
        {appointment && <Button onClick={() => window.print()}>{t('confirmation.print')}</Button>}
        <Link
          to={`/appointment-status?ref=${encodeURIComponent(ref)}`}
          className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium hover:bg-slate-50"
        >
          {t('confirmation.check')}
        </Link>
        <Link to="/" className="px-4 py-2 text-sm font-medium text-brand-700 hover:underline">
          {t('confirmation.home')}
        </Link>
      </div>
    </div>
  )
}
