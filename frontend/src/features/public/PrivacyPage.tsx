import { useTranslation } from 'react-i18next'

import { Seo } from '../../components/Seo'

/**
 * Privacy notice linked from the booking consent box (DESIGN.md §12.1, §19.1). The text is a draft
 * in en.json; the hospital's legal / compliance contact must review it before launch.
 */
export function PrivacyPage() {
  const { t } = useTranslation()
  const sections = t('privacy.sections', { returnObjects: true }) as { h: string; p: string }[]
  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <Seo title={t('seo.privacyTitle')} />
      <h1 className="text-3xl font-bold text-slate-900">{t('privacy.title')}</h1>
      <p className="mt-1 mb-6 text-sm text-amber-800">{t('privacy.updated')}</p>
      <p className="mb-6 text-lg text-slate-700">{t('privacy.intro')}</p>
      {sections.map((s) => (
        <section key={s.h} className="mb-5">
          <h2 className="mb-1 text-xl font-semibold text-slate-900">{s.h}</h2>
          <p className="text-slate-700">{s.p}</p>
        </section>
      ))}
    </div>
  )
}
