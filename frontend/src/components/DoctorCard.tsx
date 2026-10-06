import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import type { DoctorSummary } from '../api/public'
import { DoctorAvatar } from './DoctorAvatar'

/**
 * Doctor directory card (FR-P2): photo, name, speciality, qualification, experience, branches.
 * @param branchCode on a branch page: pre-select that hospital when booking
 */
export function DoctorCard({ doctor, branchCode }: { doctor: DoctorSummary; branchCode?: string }) {
  const { t } = useTranslation()
  return (
    <li className="flex flex-col rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex gap-4">
        <DoctorAvatar name={doctor.fullName} photoUrl={doctor.photoUrl} />
        <div className="min-w-0">
          <h3 className="font-semibold text-slate-900">
            <Link to={`/doctors/${doctor.id}`} className="hover:text-brand-700 hover:underline">
              {doctor.fullName}
            </Link>
          </h3>
          <p className="text-sm font-medium text-brand-700">{doctor.speciality.name}</p>
          <p className="text-sm text-slate-600">{doctor.qualification}</p>
          <p className="text-sm text-slate-500">{t('doctors.experience', { count: doctor.experienceYears })}</p>
        </div>
      </div>
      <p className="mt-3 text-sm">
        <span className="text-slate-500">{t('doctors.available')}: </span>
        {doctor.branches.map((b) => b.city).join(', ')}
      </p>
      <div className="mt-4 flex gap-2 pt-1 text-sm font-medium">
        <Link
          to={`/doctors/${doctor.id}`}
          className="flex-1 rounded-lg border border-brand-600 px-3 py-2 text-center text-brand-700 hover:bg-brand-50"
        >
          {t('doctors.viewProfile')}
        </Link>
        <Link
          to={`/book?doctor=${doctor.id}${branchCode ? `&branch=${branchCode}` : ''}`}
          className="flex-1 rounded-lg bg-brand-600 px-3 py-2 text-center text-white hover:bg-brand-700"
        >
          {t('doctors.book')}
        </Link>
      </div>
    </li>
  )
}
