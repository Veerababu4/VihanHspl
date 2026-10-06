import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'

import { useDoctor, type BranchAvailability } from '../../api/public'
import { DoctorAvatar } from '../../components/DoctorAvatar'
import { QueryState } from '../../components/QueryState'
import { Seo } from '../../components/Seo'
import { formatFee, formatTime } from '../../lib/format'
import { physicianLd } from '../../lib/jsonLd'

/** Doctor profile with weekly timings per branch and a Book button (FR-P3). */
export function DoctorProfilePage() {
  const { t } = useTranslation()
  const id = Number(useParams().id)
  const { data: doctor, isPending, error } = useDoctor(id)

  return (
    <QueryState isPending={isPending} error={error} notFoundText={t('doctors.notFound')} errorText={t('doctors.error')}>
      {doctor && (
        <div className="mx-auto max-w-4xl px-4 py-10">
          <Seo
            title={`${doctor.fullName} — ${doctor.speciality.name}`}
            description={`${doctor.fullName}, ${doctor.qualification}. ${doctor.speciality.name} at ${doctor.branches.map((b) => b.city).join(', ')}.`}
            jsonLd={physicianLd(doctor)}
          />
          <section className="flex flex-col gap-6 rounded-2xl border border-slate-200 bg-white p-6 sm:flex-row">
            <DoctorAvatar name={doctor.fullName} photoUrl={doctor.photoUrl} size="lg" />
            <div className="flex-1">
              <h1 className="text-2xl font-bold text-slate-900">{doctor.fullName}</h1>
              <p className="font-medium text-brand-700">
                {doctor.speciality.name}
                {doctor.designation && ` · ${doctor.designation}`}
              </p>
              <p className="text-slate-700">{doctor.qualification}</p>
              <p className="text-slate-500">{t('doctors.experience', { count: doctor.experienceYears })}</p>
              <dl className="mt-3 grid gap-1 text-sm sm:grid-cols-2">
                {doctor.languages && (
                  <div>
                    <dt className="inline text-slate-500">{t('doctors.languages')}: </dt>
                    <dd className="inline">{doctor.languages}</dd>
                  </div>
                )}
                {doctor.registrationNo && (
                  <div>
                    <dt className="inline text-slate-500">{t('doctors.registrationNo')}: </dt>
                    <dd className="inline">{doctor.registrationNo}</dd>
                  </div>
                )}
                {doctor.consultationFee != null && (
                  <div>
                    <dt className="inline text-slate-500">{t('doctors.fee')}: </dt>
                    <dd className="inline font-semibold">
                      {formatFee(doctor.consultationFee)}{' '}
                      <span className="font-normal text-slate-500">({t('doctors.feeNote')})</span>
                    </dd>
                  </div>
                )}
              </dl>
              <Link
                to={`/book?doctor=${doctor.id}`}
                className="mt-5 inline-block rounded-lg bg-brand-600 px-5 py-3 font-semibold text-white hover:bg-brand-700"
              >
                {t('doctors.book')}
              </Link>
            </div>
          </section>

          {doctor.bio && (
            <section className="mt-8">
              <h2 className="mb-2 text-xl font-semibold text-slate-900">{t('doctors.about')}</h2>
              <p className="text-slate-700">{doctor.bio}</p>
            </section>
          )}

          <section className="mt-8">
            <h2 className="mb-4 text-xl font-semibold text-slate-900">{t('doctors.weeklyTimings')}</h2>
            {doctor.availability.length === 0 ? (
              <p className="text-slate-600">{t('doctors.noTimings')}</p>
            ) : (
              <div className="grid gap-4 md:grid-cols-2">
                {doctor.availability.map((a) => (
                  <AvailabilityCard key={a.branchCode} availability={a} />
                ))}
              </div>
            )}
          </section>
        </div>
      )}
    </QueryState>
  )
}

function AvailabilityCard({ availability }: { availability: BranchAvailability }) {
  const { t } = useTranslation()
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5">
      <h3 className="mb-3 font-semibold">
        <Link to={`/branches/${availability.branchCode}`} className="text-brand-700 hover:underline">
          {availability.branchCity}
        </Link>
      </h3>
      <table className="w-full text-sm">
        <tbody>
          {availability.days.map((day) => (
            <tr key={day.dayOfWeek} className="border-t border-slate-100 first:border-0">
              <th scope="row" className="py-1.5 pr-4 text-left font-medium">
                {t(`days.${day.dayOfWeek}`)}
              </th>
              <td className="py-1.5 text-slate-700">
                {day.sessions.map((s) => (
                  <span key={s.start} className="block whitespace-nowrap">
                    {formatTime(s.start)} – {formatTime(s.end)}
                  </span>
                ))}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
