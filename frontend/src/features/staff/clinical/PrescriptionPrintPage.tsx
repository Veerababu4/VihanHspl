import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'

import { useVisit } from '../../../api/clinical'
import { Seo } from '../../../components/Seo'
import { Alert, Button } from '../../../components/ui'
import { fullDate } from '../../../lib/dates'
import { vitalsLine } from './vitals'

/**
 * Print-friendly prescription with the hospital letterhead (FR-C5). The staff menus are hidden
 * when printing (StaffLayout), so the browser's Print / Save as PDF gives a clean A4 page.
 */
export function PrescriptionPrintPage() {
  const { t } = useTranslation()
  const id = Number(useParams().id)
  const visit = useVisit(id)
  if (visit.isPending) return <p className="text-slate-500">{t('staff.common.loading')}</p>
  if (visit.error) return <Alert>{visit.error.message}</Alert>
  const v = visit.data
  const c = v.clinical
  const vitals = vitalsLine(c)

  return (
    <>
      <Seo title={`${t('staff.rx.heading')} · ${v.patient.fullName}`} />
      <div className="mb-4 flex gap-3 print:hidden">
        <Button onClick={() => window.print()}>{t('staff.rx.printNow')}</Button>
        <Link to={`/staff/visits/${v.id}`} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-800 hover:bg-slate-50">
          {t('staff.rx.back')}
        </Link>
      </div>

      <article className="mx-auto max-w-3xl bg-white p-6 text-slate-900 shadow-sm print:max-w-none print:p-0 print:shadow-none sm:p-10">
        <header className="flex flex-wrap items-start justify-between gap-4 border-b-2 border-brand-700 pb-4">
          <div>
            <p className="text-2xl font-bold text-brand-700">{t('brand.fullName')}</p>
            {v.branch && (
              <p className="text-sm text-slate-700">
                {v.branch.name}
                {v.branch.address && <span className="block">{v.branch.address}</span>}
                {v.branch.phone && <span className="block">☎ {v.branch.phone}</span>}
              </p>
            )}
          </div>
          <div className="text-right text-sm">
            <p className="text-base font-semibold">{v.doctor.fullName}</p>
            {v.doctor.qualification && <p>{v.doctor.qualification}</p>}
            <p>{v.doctor.speciality}</p>
            {v.doctor.registrationNo && <p>{t('staff.rx.regNo', { no: v.doctor.registrationNo })}</p>}
          </div>
        </header>

        <dl className="grid grid-cols-2 gap-x-6 gap-y-1 border-b border-slate-200 py-3 text-sm sm:grid-cols-4">
          <div><dt className="text-slate-500">{t('staff.rx.patient')}</dt><dd className="font-semibold">{v.patient.fullName}</dd></div>
          <div><dt className="text-slate-500">{t('staff.rx.uhid')}</dt><dd className="font-semibold">{v.patient.uhid}</dd></div>
          <div><dt className="text-slate-500">&nbsp;</dt><dd>{t('staff.rx.ageGender', { age: v.patient.age, gender: t(`booking.genders.${v.patient.gender}`) })}</dd></div>
          <div><dt className="text-slate-500">{t('staff.rx.date')}</dt><dd className="font-semibold">{fullDate(v.date)}</dd></div>
        </dl>

        <div className="space-y-2 py-4 text-sm">
          {vitals && <p><span className="font-semibold">{t('staff.rx.vitals')}:</span> {vitals}</p>}
          {c.chiefComplaint && <p><span className="font-semibold">{t('staff.rx.complaint')}:</span> {c.chiefComplaint}</p>}
          {c.diagnosis && <p><span className="font-semibold">{t('staff.rx.diagnosis')}:</span> {c.diagnosis}</p>}
        </div>

        <p className="text-3xl font-bold text-brand-700" aria-hidden="true">℞</p>
        <div className="mb-4 overflow-x-auto print:overflow-visible">
        <table className="w-full text-sm">
          <thead className="border-b border-slate-300 text-left">
            <tr>
              <th className="py-1 pr-2">#</th>
              <th className="py-1 pr-2">{t('staff.visit.medicine')}</th>
              <th className="py-1 pr-2">{t('staff.visit.dosage')}</th>
              <th className="py-1 pr-2">{t('staff.visit.frequency')}</th>
              <th className="py-1 pr-2">{t('staff.visit.duration')}</th>
              <th className="py-1">{t('staff.visit.instructions')}</th>
            </tr>
          </thead>
          <tbody>
            {v.prescription.map((r, i) => (
              <tr key={i} className="border-b border-slate-100 align-top">
                <td className="py-1.5 pr-2">{i + 1}</td>
                <td className="py-1.5 pr-2 font-semibold">{r.medicineName}</td>
                <td className="py-1.5 pr-2">{r.dosage}</td>
                <td className="py-1.5 pr-2">{r.frequency}</td>
                <td className="py-1.5 pr-2">{r.duration}</td>
                <td className="py-1.5">{r.instructions}</td>
              </tr>
            ))}
          </tbody>
        </table>
        </div>
        {v.prescription.length === 0 && <p className="mb-4 text-sm text-slate-600">{t('staff.visit.noMedicines')}</p>}

        {c.advice && <p className="mb-2 text-sm"><span className="font-semibold">{t('staff.rx.advice')}:</span> {c.advice}</p>}
        {c.followUpDate && <p className="mb-2 text-sm font-semibold">{t('staff.rx.followUp', { date: fullDate(c.followUpDate) })}</p>}

        <div className="mt-12 flex justify-end">
          <p className="w-56 border-t border-slate-400 pt-1 text-center text-sm">{t('staff.rx.signature')}</p>
        </div>
        <p className="mt-6 border-t border-slate-200 pt-2 text-center text-xs text-slate-500">{t('staff.rx.footer')}</p>
      </article>
    </>
  )
}
