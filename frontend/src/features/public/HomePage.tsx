import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { useBranches, type Branch } from '../../api/public'

export function HomePage() {
  const { t } = useTranslation()
  const { data: branches, isPending, isError } = useBranches()

  return (
    <>
      <section className="bg-gradient-to-br from-brand-700 to-accent-600 text-white">
        <div className="mx-auto max-w-6xl px-4 py-14 sm:py-20">
          <h1 className="max-w-2xl text-3xl font-bold sm:text-5xl">{t('home.heroTitle')}</h1>
          <p className="mt-4 max-w-2xl text-lg text-white/90">{t('home.heroText')}</p>
          <Link
            to="/book"
            className="mt-8 inline-block rounded-lg bg-white px-5 py-3 font-semibold text-brand-700 shadow hover:bg-brand-50"
          >
            {t('home.bookCta')}
          </Link>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-12">
        <h2 className="mb-6 text-2xl font-bold text-slate-900">{t('home.branchesTitle')}</h2>
        {isPending && <p className="text-slate-500">{t('home.loadingBranches')}</p>}
        {isError && (
          <p role="alert" className="text-emergency-600">
            {t('home.branchesError')}
          </p>
        )}
        {branches && (
          <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {branches.map((b) => (
              <BranchCard key={b.id} branch={b} />
            ))}
          </ul>
        )}
      </section>
    </>
  )
}

function BranchCard({ branch }: { branch: Branch }) {
  const { t } = useTranslation()
  return (
    <li className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <h3 className="text-lg font-semibold text-brand-700">{branch.city}</h3>
      <p className="text-sm text-slate-500">{branch.district} district</p>
      <p className="mt-3 text-sm">{branch.address}</p>
      <dl className="mt-3 space-y-1 text-sm">
        {branch.phone && (
          <div>
            <dt className="inline font-medium">{t('home.phone')}: </dt>
            <dd className="inline">
              <a href={`tel:${branch.phone}`} className="text-accent-600 underline">
                {branch.phone}
              </a>
            </dd>
          </div>
        )}
        {branch.emergencyPhone && (
          <div>
            <dt className="inline font-medium text-emergency-600">{t('home.emergency')}: </dt>
            <dd className="inline">
              <a href={`tel:${branch.emergencyPhone}`} className="font-semibold text-emergency-600 underline">
                {branch.emergencyPhone}
              </a>
            </dd>
          </div>
        )}
        {branch.openingHours && (
          <div>
            <dt className="inline font-medium">{t('home.hours')}: </dt>
            <dd className="inline">{branch.openingHours}</dd>
          </div>
        )}
      </dl>
    </li>
  )
}
