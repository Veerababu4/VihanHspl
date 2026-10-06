import { useTranslation } from 'react-i18next'
import { Link, NavLink, Outlet } from 'react-router'

import { useAppInfo, useBranches } from '../api/public'

export function PublicLayout() {
  const { t } = useTranslation()
  const { data: appInfo } = useAppInfo()

  return (
    <div className="flex min-h-screen flex-col">
      {appInfo?.sampleData && (
        <div role="status" className="bg-amber-400 px-4 py-1 text-center text-sm font-semibold text-amber-950">
          {t('banner.sampleData')}
        </div>
      )}
      <header className="sticky top-0 z-10 border-b border-slate-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
          <Link to="/" className="flex items-center gap-2" aria-label={t('brand.fullName')}>
            <img src="/favicon.svg" alt="" className="h-9 w-9" />
            <span className="leading-tight">
              <span className="block text-xl font-bold text-brand-700">{t('brand.name')}</span>
              <span className="block text-xs text-slate-500">{t('brand.tagline')}</span>
            </span>
          </Link>
          <nav className="flex items-center gap-1 text-sm font-medium sm:gap-4">
            <NavLink to="/doctors" className="hidden px-2 py-1 hover:text-brand-700 sm:block">
              {t('nav.doctors')}
            </NavLink>
            <NavLink to="/staff/login" className="hidden px-2 py-1 text-slate-500 hover:text-brand-700 sm:block">
              {t('nav.staffLogin')}
            </NavLink>
            <Link to="/book" className="rounded-lg bg-brand-600 px-3 py-2 text-white hover:bg-brand-700">
              {t('nav.book')}
            </Link>
          </nav>
        </div>
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <Footer />
    </div>
  )
}

const currentYear = new Date().getFullYear()

function Footer() {
  const { t } = useTranslation()
  const { data: branches } = useBranches()

  return (
    <footer className="bg-brand-900 text-brand-100">
      <div className="mx-auto max-w-6xl px-4 py-8">
        <p className="mb-3 font-semibold text-white">🚑 {t('footer.emergency')}</p>
        <ul className="grid gap-2 text-sm sm:grid-cols-3">
          {branches?.map((b) => (
            <li key={b.id}>
              {b.city}:{' '}
              {b.emergencyPhone ? (
                <a href={`tel:${b.emergencyPhone}`} className="font-semibold text-white underline">
                  {b.emergencyPhone}
                </a>
              ) : (
                '—'
              )}
            </li>
          ))}
        </ul>
        <p className="mt-6 text-xs text-brand-100/70">{t('footer.rights', { year: currentYear })}</p>
      </div>
    </footer>
  )
}
