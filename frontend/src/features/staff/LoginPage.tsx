import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { z } from 'zod'

import { ApiError } from '../../api/client'
import { useBranches, useDoctors, useSpecialities, type DoctorSummary } from '../../api/public'
import { useAuth } from '../../auth/session'
import { DoctorAvatar } from '../../components/DoctorAvatar'
import { Seo } from '../../components/Seo'
import { Alert, Button, Field, inputClass } from '../../components/ui'

const schema = z.object({
  username: z.string().trim().min(1, 'Enter your username'),
  password: z.string().min(1, 'Enter your password'),
})
type LoginForm = z.infer<typeof schema>

/** Staff login (FR-A1): form plus a hospital summary and doctor carousel. */
export function LoginPage() {
  const { t } = useTranslation()
  const { status, login } = useAuth()
  const navigate = useNavigate()
  const from = (useLocation().state as { from?: string } | null)?.from ?? '/staff/dashboard'
  const [error, setError] = useState<string | null>(null)
  const form = useForm<LoginForm>({ resolver: zodResolver(schema), defaultValues: { username: '', password: '' } })

  if (status === 'authenticated') return <Navigate to={from} replace />

  const onSubmit = form.handleSubmit(async ({ username, password }) => {
    setError(null)
    try {
      await login(username, password)
      navigate(from, { replace: true })
    } catch (e) {
      setError(e instanceof ApiError ? e.message : t('staff.common.loadError'))
      form.setValue('password', '')
    }
  })

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <Seo title={t('staff.login.title')} />
      <Showcase />
      <div className="flex items-center justify-center bg-white px-4 py-12">
        <div className="w-full max-w-sm">
          <Link to="/" className="text-sm text-brand-700 hover:underline">
            {t('staff.login.backToSite')}
          </Link>
          <h1 className="mt-6 text-2xl font-bold text-slate-900">{t('staff.login.title')}</h1>
          <p className="mb-6 text-slate-600">{t('staff.login.subtitle')}</p>
          <form onSubmit={onSubmit} noValidate className="space-y-4">
            {error && <Alert>{error}</Alert>}
            <Field label={t('staff.login.username')} error={form.formState.errors.username?.message}>
              {(a) => <input {...a} {...form.register('username')} autoComplete="username" autoCapitalize="none" className={inputClass} />}
            </Field>
            <Field label={t('staff.login.password')} error={form.formState.errors.password?.message}>
              {(a) => <input {...a} {...form.register('password')} type="password" autoComplete="current-password" className={inputClass} />}
            </Field>
            <Button type="submit" disabled={form.formState.isSubmitting || status === 'checking'} className="w-full py-2.5">
              {form.formState.isSubmitting ? t('staff.login.signingIn') : t('staff.login.submit')}
            </Button>
          </form>
        </div>
      </div>
    </div>
  )
}

const CAROUSEL_MS = 4000

function Showcase() {
  const { t } = useTranslation()
  const { data: branches } = useBranches()
  const { data: specialities } = useSpecialities()
  const { data: doctors } = useDoctors()

  return (
    <aside className="hidden flex-col justify-between bg-gradient-to-br from-brand-700 to-accent-600 p-10 text-white lg:flex">
      <div>
        <p className="text-2xl font-bold">{t('staff.login.showcaseTitle')}</p>
        <p className="mt-2 text-white/85">{t('staff.login.showcaseText')}</p>
        <dl className="mt-8 grid grid-cols-3 gap-4">
          <Stat label={t('staff.login.statHospitals')} value={branches?.length} />
          <Stat label={t('staff.login.statDoctors')} value={doctors?.length} />
          <Stat label={t('staff.login.statSpecialities')} value={specialities?.length} />
        </dl>
        {branches && <p className="mt-6 text-white/85">{branches.map((b) => b.city).join(' · ')}</p>}
      </div>
      {doctors && doctors.length > 0 && <DoctorCarousel doctors={doctors} />}
    </aside>
  )
}

function Stat({ label, value }: { label: string; value?: number }) {
  return (
    <div className="rounded-xl bg-white/10 p-4">
      <dd className="text-3xl font-bold">{value ?? '–'}</dd>
      <dt className="text-sm text-white/80">{label}</dt>
    </div>
  )
}

function DoctorCarousel({ doctors }: { doctors: DoctorSummary[] }) {
  const { t } = useTranslation()
  const [index, setIndex] = useState(0)
  useEffect(() => {
    const timer = setInterval(() => setIndex((i) => (i + 1) % doctors.length), CAROUSEL_MS)
    return () => clearInterval(timer)
  }, [doctors.length])
  const d = doctors[index % doctors.length]
  return (
    <section aria-label={t('staff.login.ourDoctors')} aria-live="polite">
      <p className="mb-3 text-sm font-semibold tracking-wide text-white/80 uppercase">{t('staff.login.ourDoctors')}</p>
      <div className="flex items-center gap-4 rounded-xl bg-white/10 p-4">
        <DoctorAvatar name={d.fullName} photoUrl={d.photoUrl} />
        <div>
          <p className="font-semibold">{d.fullName}</p>
          <p className="text-sm text-white/85">{d.speciality.name}</p>
          <p className="text-sm text-white/70">{d.branches.map((b) => b.city).join(', ')}</p>
        </div>
      </div>
    </section>
  )
}
