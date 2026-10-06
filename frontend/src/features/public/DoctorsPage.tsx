import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'

import { useBranches, useDoctors, useSpecialities } from '../../api/public'
import { DoctorCard } from '../../components/DoctorCard'
import { Seo } from '../../components/Seo'

const SEARCH_DELAY_MS = 300

/** Doctor directory with filters kept in the URL, so filtered lists can be shared (FR-P2). */
export function DoctorsPage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const branch = params.get('branch') ?? ''
  const specialityParam = params.get('speciality')
  const speciality = specialityParam ? Number(specialityParam) : undefined
  const q = params.get('q') ?? ''

  // Typing updates the box immediately; the URL (and the API call) follow after a short pause.
  const [searchText, setSearchText] = useState(q)
  useEffect(() => {
    if (searchText.trim() === q) return
    const timer = setTimeout(() => {
      setParams(
        (prev) => {
          const next = new URLSearchParams(prev)
          if (searchText.trim()) next.set('q', searchText.trim())
          else next.delete('q')
          return next
        },
        { replace: true },
      )
    }, SEARCH_DELAY_MS)
    return () => clearTimeout(timer)
  }, [searchText, q, setParams])

  const { data: branches } = useBranches()
  const { data: specialities } = useSpecialities()
  const { data: doctors, isPending, isError } = useDoctors({ branch: branch || undefined, speciality, q: q || undefined })

  function updateParam(key: string, value: string) {
    setParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        if (value) next.set(key, value)
        else next.delete(key)
        return next
      },
      { replace: true },
    )
  }

  function clearFilters() {
    setSearchText('')
    setParams({}, { replace: true })
  }

  const hasFilters = Boolean(branch || speciality || q)

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <Seo title={t('seo.doctorsTitle')} description={t('seo.doctorsDescription')} />
      <h1 className="mb-6 text-3xl font-bold text-slate-900">{t('doctors.title')}</h1>

      <div className="mb-6 grid gap-3 rounded-xl border border-slate-200 bg-white p-4 sm:grid-cols-3">
        <label className="text-sm font-medium">
          {t('doctors.filterBranch')}
          <select
            value={branch}
            onChange={(e) => updateParam('branch', e.target.value)}
            className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2"
          >
            <option value="">{t('doctors.allBranches')}</option>
            {branches?.map((b) => (
              <option key={b.code} value={b.code}>
                {b.city}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm font-medium">
          {t('doctors.filterSpeciality')}
          <select
            value={speciality ?? ''}
            onChange={(e) => updateParam('speciality', e.target.value)}
            className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2"
          >
            <option value="">{t('doctors.allSpecialities')}</option>
            {specialities?.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm font-medium">
          {t('doctors.search')}
          <input
            type="search"
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2"
          />
        </label>
      </div>

      {isPending && <p className="text-slate-500">{t('doctors.loading')}</p>}
      {isError && (
        <p role="alert" className="text-emergency-600">
          {t('doctors.error')}
        </p>
      )}
      {doctors && (
        <>
          <div className="mb-4 flex items-center justify-between text-sm text-slate-600">
            <p aria-live="polite">{t('doctors.count', { count: doctors.length })}</p>
            {hasFilters && (
              <button type="button" onClick={clearFilters} className="font-medium text-brand-700 hover:underline">
                {t('doctors.clearFilters')}
              </button>
            )}
          </div>
          {doctors.length === 0 ? (
            <p className="rounded-xl bg-white p-8 text-center text-slate-600">{t('doctors.none')}</p>
          ) : (
            <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {doctors.map((d) => (
                <DoctorCard key={d.id} doctor={d} />
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  )
}
