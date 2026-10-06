import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

/** Placeholder for pages built in later milestones (doctors M2, booking M5, staff portal M3). */
export function ComingSoonPage() {
  const { t } = useTranslation()
  return <Message title={t('common.comingSoon')} />
}

export function NotFoundPage() {
  const { t } = useTranslation()
  return <Message title={t('notFound.title')} />
}

function Message({ title }: { title: string }) {
  const { t } = useTranslation()
  return (
    <div className="mx-auto max-w-6xl px-4 py-20 text-center">
      <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
      <Link to="/" className="mt-4 inline-block text-accent-600 underline">
        {t('notFound.back')}
      </Link>
    </div>
  )
}
