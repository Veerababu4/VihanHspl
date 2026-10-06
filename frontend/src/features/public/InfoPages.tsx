import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { useBranches } from '../../api/public'
import { Seo } from '../../components/Seo'
import { directionsUrl } from '../../lib/format'

const FACILITIES = ['emergency', 'opd', 'surgery', 'diagnostics', 'pharmacy', 'booking'] as const

/** About page (FR-P6). Text lives in en.json so the hospital can review it before launch. */
export function AboutPage() {
  const { t } = useTranslation()
  return (
    <div className="mx-auto max-w-4xl px-4 py-10">
      <Seo title={t('seo.aboutTitle')} description={t('about.intro')} />
      <h1 className="mb-4 text-3xl font-bold text-slate-900">{t('about.title')}</h1>
      <p className="text-lg text-slate-700">{t('about.intro')}</p>
      <p className="mt-3 text-slate-700">{t('about.mission')}</p>
      <h2 className="mt-8 mb-4 text-xl font-semibold text-slate-900">{t('about.facilitiesTitle')}</h2>
      <ul className="grid gap-3 sm:grid-cols-2">
        {FACILITIES.map((f) => (
          <li key={f} className="rounded-lg border border-slate-200 bg-white px-4 py-3">
            ✓ {t(`about.facilities.${f}`)}
          </li>
        ))}
      </ul>
    </div>
  )
}

/** Contact page (FR-P6): every branch with phones, email and directions. */
export function ContactPage() {
  const { t } = useTranslation()
  const { data: branches } = useBranches()
  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <Seo title={t('seo.contactTitle')} description={t('contact.intro')} />
      <h1 className="mb-2 text-3xl font-bold text-slate-900">{t('contact.title')}</h1>
      <p className="mb-2 text-slate-700">{t('contact.intro')}</p>
      <p className="mb-8 font-medium text-emergency-600">{t('contact.emergencyNote')}</p>
      <ul className="grid gap-6 md:grid-cols-3">
        {branches?.map((b) => (
          <li key={b.id} className="rounded-xl border border-slate-200 bg-white p-5">
            <h2 className="text-lg font-semibold text-brand-700">
              <Link to={`/branches/${b.code}`} className="hover:underline">
                {b.city}
              </Link>
            </h2>
            <p className="mt-2 text-sm">{b.address}</p>
            <ul className="mt-3 space-y-1 text-sm">
              {b.phone && (
                <li>
                  {t('home.phone')}:{' '}
                  <a href={`tel:${b.phone}`} className="text-accent-600 underline">
                    {b.phone}
                  </a>
                </li>
              )}
              {b.emergencyPhone && (
                <li className="text-emergency-600">
                  {t('home.emergency')}:{' '}
                  <a href={`tel:${b.emergencyPhone}`} className="font-semibold underline">
                    {b.emergencyPhone}
                  </a>
                </li>
              )}
              {b.email && (
                <li>
                  {t('branch.email')}:{' '}
                  <a href={`mailto:${b.email}`} className="text-accent-600 underline">
                    {b.email}
                  </a>
                </li>
              )}
            </ul>
            <a
              href={directionsUrl(b.name, b.city)}
              target="_blank"
              rel="noopener noreferrer"
              className="mt-4 inline-block text-sm font-medium text-brand-700 hover:underline"
            >
              {t('branch.directions')} →
            </a>
          </li>
        ))}
      </ul>
    </div>
  )
}
