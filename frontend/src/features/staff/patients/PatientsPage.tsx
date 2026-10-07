import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'

import { searchPatients, type SearchBy } from '../../../api/patients'
import { useBranches } from '../../../api/public'
import type { PatientSummary } from '../../../api/reception'
import { useAuth } from '../../../auth/session'
import { Seo } from '../../../components/Seo'
import { Alert, Badge, Button, Field, inputClass, PageHeader } from '../../../components/ui'
import { RegisterPatientForm } from '../appointments/RegisterPatientForm'

const BY: SearchBy[] = ['name', 'mobile', 'uhid', 'aadhaar']

/** Patient search (FR-PT3) by name, mobile, UHID or Aadhaar; reception can register from here. */
export function PatientsPage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const desk = user?.role === 'RECEPTIONIST' || user?.role === 'IT_ADMIN'
  const [by, setBy] = useState<SearchBy>('name')
  const [text, setText] = useState('')
  const [results, setResults] = useState<PatientSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [searching, setSearching] = useState(false)

  const search = async () => {
    if (!text.trim()) return
    setError(null)
    setSearching(true)
    try {
      setResults(await searchPatients(by, text.trim()))
    } catch (e) {
      setResults(null)
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setSearching(false)
    }
  }

  return (
    <>
      <Seo title={t('staff.patients.title')} />
      <PageHeader
        title={t('staff.patients.title')}
        actions={
          desk && (
            <Link to="/staff/patients/new" className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">
              {t('staff.patients.register')}
            </Link>
          )
        }
      />
      <form
        role="search"
        className="mb-4 flex flex-wrap items-end gap-3"
        onSubmit={(e) => {
          e.preventDefault()
          void search()
        }}
      >
        <Field label={t('staff.patients.searchBy')}>
          {(f) => (
            <select {...f} value={by} onChange={(e) => { setBy(e.target.value as SearchBy); setResults(null) }} className={inputClass}>
              {BY.map((b) => (
                <option key={b} value={b}>
                  {t(`staff.patients.by.${b}`)}
                </option>
              ))}
            </select>
          )}
        </Field>
        <div className="min-w-0 flex-1 basis-60">
          <Field label={t('staff.patients.text')} hint={by === 'aadhaar' ? t('staff.patients.aadhaarNote') : undefined}>
            {(f) => (
              <input
                {...f}
                type="search"
                value={text}
                onChange={(e) => setText(e.target.value)}
                inputMode={by === 'name' || by === 'uhid' ? 'text' : 'numeric'}
                autoComplete="off"
                className={inputClass}
              />
            )}
          </Field>
        </div>
        <Button type="submit" disabled={searching}>
          {t('staff.patients.search')}
        </Button>
      </form>

      {error && <Alert>{error}</Alert>}
      {results && (
        <>
          <p className="mb-2 text-sm text-slate-600" aria-live="polite">
            {t('staff.patients.results', { count: results.length })}
          </p>
          {results.length === 0 ? (
            <p className="rounded-xl border border-slate-200 bg-white p-6 text-slate-600">{t('staff.patients.none')}</p>
          ) : (
            <ul className="divide-y divide-slate-200 rounded-xl border border-slate-200 bg-white">
              {results.map((p) => (
                <li key={p.id}>
                  <Link to={`/staff/patients/${p.id}`} className="flex flex-wrap items-center gap-x-4 gap-y-1 p-4 hover:bg-slate-50">
                    <span className="font-semibold text-brand-700">{p.fullName}</span>
                    <span className="font-mono text-sm text-slate-700">{p.uhid}</span>
                    <span className="text-sm text-slate-600">
                      {p.age} y · {t(`booking.genders.${p.gender}`)} · {p.mobile}
                    </span>
                    {p.aadhaarMasked && <Badge>{p.aadhaarMasked}</Badge>}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </>
  )
}

/** Full registration (FR-PT1), then the new patient's profile. */
export function NewPatientPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { user } = useAuth()
  const { data: branches } = useBranches()
  const homeBranch = branches?.find((b) => b.id === user?.homeBranchId)?.code
  return (
    <div className="max-w-2xl">
      <Seo title={t('staff.patients.newTitle')} />
      <Link to="/staff/patients" className="text-sm text-brand-700 hover:underline">
        ← {t('staff.patients.back')}
      </Link>
      <PageHeader title={t('staff.patients.newTitle')} />
      <div className="rounded-xl border border-slate-200 bg-white p-5">
        <RegisterPatientForm
          key={homeBranch ?? ''}   // starts again once the home branch is known
          prefill={{ branch: homeBranch ?? null }}
          showDetails
          onRegistered={(p) => navigate(`/staff/patients/${p.id}`)}
        />
      </div>
    </div>
  )
}
