import { useTranslation } from 'react-i18next'
import { useParams } from 'react-router'

import { useBranch } from '../../api/public'
import { DoctorCard } from '../../components/DoctorCard'
import { QueryState } from '../../components/QueryState'
import { Seo } from '../../components/Seo'
import { directionsUrl } from '../../lib/format'
import { branchPageLd } from '../../lib/jsonLd'
import { GalleryGrid } from './GalleryPage'

/** Branch page: address, map, phones, timings, doctors, photos (FR-P4). */
export function BranchPage() {
  const { t } = useTranslation()
  const code = useParams().code ?? ''
  const { data, isPending, error } = useBranch(code)

  return (
    <QueryState isPending={isPending} error={error} notFoundText={t('branch.notFound')}>
      {data && (
        <>
          <Seo
            title={data.branch.name}
            description={`${data.branch.name}, ${data.branch.city}. ${data.branch.openingHours ?? ''}`}
            jsonLd={branchPageLd(data.branch)}
          />
          <section className="relative bg-brand-900 text-white">
            {data.branch.imageUrl && (
              <img src={data.branch.imageUrl} alt="" className="absolute inset-0 h-full w-full object-cover opacity-30" />
            )}
            <div className="relative mx-auto max-w-6xl px-4 py-12">
              <h1 className="text-3xl font-bold sm:text-4xl">{data.branch.city}</h1>
              <p className="mt-1 text-white/90">{data.branch.name}</p>
            </div>
          </section>

          <div className="mx-auto grid max-w-6xl gap-8 px-4 py-10 lg:grid-cols-2">
            <section className="rounded-xl border border-slate-200 bg-white p-6">
              <dl className="space-y-3">
                <div>
                  <dt className="text-sm text-slate-500">{t('branch.address')}</dt>
                  <dd>
                    {data.branch.address}, {data.branch.city}
                    {data.branch.pincode && ` - ${data.branch.pincode}`}
                  </dd>
                </div>
                {data.branch.phone && (
                  <div>
                    <dt className="text-sm text-slate-500">{t('home.phone')}</dt>
                    <dd>
                      <a href={`tel:${data.branch.phone}`} className="text-accent-600 underline">
                        {data.branch.phone}
                      </a>
                    </dd>
                  </div>
                )}
                {data.branch.emergencyPhone && (
                  <div>
                    <dt className="text-sm text-emergency-600">{t('home.emergency')}</dt>
                    <dd>
                      <a href={`tel:${data.branch.emergencyPhone}`} className="font-semibold text-emergency-600 underline">
                        {data.branch.emergencyPhone}
                      </a>
                    </dd>
                  </div>
                )}
                {data.branch.email && (
                  <div>
                    <dt className="text-sm text-slate-500">{t('branch.email')}</dt>
                    <dd>
                      <a href={`mailto:${data.branch.email}`} className="text-accent-600 underline">
                        {data.branch.email}
                      </a>
                    </dd>
                  </div>
                )}
                {data.branch.openingHours && (
                  <div>
                    <dt className="text-sm text-slate-500">{t('home.hours')}</dt>
                    <dd>{data.branch.openingHours}</dd>
                  </div>
                )}
              </dl>
              <a
                href={directionsUrl(data.branch.name, data.branch.city)}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-5 inline-block rounded-lg bg-brand-600 px-4 py-2 font-medium text-white hover:bg-brand-700"
              >
                {t('branch.directions')}
              </a>
            </section>

            {data.branch.mapUrl && (
              <iframe
                title={t('branch.mapTitle', { city: data.branch.city })}
                src={data.branch.mapUrl}
                loading="lazy"
                referrerPolicy="no-referrer-when-downgrade"
                className="h-80 w-full rounded-xl border border-slate-200 lg:h-full"
              />
            )}
          </div>

          <section className="mx-auto max-w-6xl px-4 pb-10">
            <h2 className="mb-6 text-2xl font-bold text-slate-900">{t('branch.doctorsHere')}</h2>
            <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {data.doctors.map((d) => (
                <DoctorCard key={d.id} doctor={d} />
              ))}
            </ul>
          </section>

          {data.gallery.length > 0 && (
            <section className="mx-auto max-w-6xl px-4 pb-12">
              <h2 className="mb-6 text-2xl font-bold text-slate-900">{t('branch.photos')}</h2>
              <GalleryGrid images={data.gallery} />
            </section>
          )}
        </>
      )}
    </QueryState>
  )
}
