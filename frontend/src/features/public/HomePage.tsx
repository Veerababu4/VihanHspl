import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { useBranches, useDoctors, useSpecialities, type Branch } from '../../api/public'
import { DoctorCard } from '../../components/DoctorCard'
import { Seo } from '../../components/Seo'
import { hospitalNetworkLd } from '../../lib/jsonLd'

const FEATURED_COUNT = 6

export function HomePage() {
  const { t } = useTranslation()
  const { data: branches, isPending, isError } = useBranches()

  return (
    <>
      <Seo
        title={t('seo.homeTitle')}
        fullTitle
        description={t('seo.homeDescription')}
        jsonLd={branches ? hospitalNetworkLd(branches) : undefined}
      />
      <section className="bg-gradient-to-br from-brand-700 to-accent-600 text-white">
        <div className="mx-auto max-w-6xl px-4 py-14 sm:py-20">
          <h1 className="max-w-2xl text-3xl font-bold sm:text-5xl">{t('home.heroTitle')}</h1>
          <p className="mt-4 max-w-2xl text-lg text-white/90">{t('home.heroText')}</p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link
              to="/book"
              className="rounded-lg bg-white px-5 py-3 font-semibold text-brand-700 shadow hover:bg-brand-50"
            >
              {t('home.bookCta')}
            </Link>
            <Link
              to="/doctors"
              className="rounded-lg border border-white/70 px-5 py-3 font-semibold text-white hover:bg-white/10"
            >
              {t('home.findDoctorCta')}
            </Link>
          </div>
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

      <SpecialitiesSection />
      <FeaturedDoctorsSection />
    </>
  )
}

function BranchCard({ branch }: { branch: Branch }) {
  const { t } = useTranslation()
  return (
    <li className="flex flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      {branch.imageUrl && <img src={branch.imageUrl} alt="" className="h-36 w-full object-cover" loading="lazy" />}
      <div className="flex flex-1 flex-col p-5">
        <h3 className="text-lg font-semibold text-brand-700">{branch.city}</h3>
        {branch.district && <p className="text-sm text-slate-500">{t('branch.district', { district: branch.district })}</p>}
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
        <Link
          to={`/branches/${branch.code}`}
          className="mt-4 self-start font-medium text-brand-700 hover:underline"
        >
          {t('home.viewBranch')} →
        </Link>
      </div>
    </li>
  )
}

function SpecialitiesSection() {
  const { t } = useTranslation()
  const { data: specialities } = useSpecialities()
  if (!specialities?.length) return null
  return (
    <section className="bg-white py-12">
      <div className="mx-auto max-w-6xl px-4">
        <h2 className="mb-6 text-2xl font-bold text-slate-900">{t('home.specialitiesTitle')}</h2>
        <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {specialities.map((s) => (
            <li key={s.id}>
              <Link
                to={`/doctors?speciality=${s.id}`}
                title={s.description ?? undefined}
                className="block h-full rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium hover:border-brand-500 hover:bg-brand-50"
              >
                {s.name}
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}

function FeaturedDoctorsSection() {
  const { t } = useTranslation()
  const { data: doctors } = useDoctors()
  if (!doctors?.length) return null
  const featured = [...doctors].sort((a, b) => b.experienceYears - a.experienceYears).slice(0, FEATURED_COUNT)
  return (
    <section className="mx-auto max-w-6xl px-4 py-12">
      <div className="mb-6 flex items-baseline justify-between gap-4">
        <h2 className="text-2xl font-bold text-slate-900">{t('home.featuredDoctorsTitle')}</h2>
        <Link to="/doctors" className="font-medium text-brand-700 hover:underline">
          {t('home.allDoctors')} →
        </Link>
      </div>
      <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {featured.map((d) => (
          <DoctorCard key={d.id} doctor={d} />
        ))}
      </ul>
    </section>
  )
}
