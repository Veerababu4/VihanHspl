import { useTranslation } from 'react-i18next'

import { useAuth } from '../../auth/session'
import { Seo } from '../../components/Seo'
import { Alert, PageHeader } from '../../components/ui'
import { ChangePasswordForm } from './ChangePasswordForm'

/** Placeholder until M6 builds the appointment dashboard. */
export function DashboardPage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  return (
    <>
      <Seo title={t('staff.dashboard.title')} />
      <PageHeader title={t('staff.dashboard.welcome', { name: user?.fullName })} />
      <p className="mb-6 text-slate-600">{t('staff.dashboard.role', { role: user ? t(`staff.roles.${user.role}`) : '' })}</p>
      <Alert kind="info">{t('staff.dashboard.comingSoon')}</Alert>
    </>
  )
}

export function ProfilePage() {
  const { t } = useTranslation()
  return (
    <>
      <Seo title={t('staff.password.title')} />
      <PageHeader title={t('staff.password.title')} />
      <ChangePasswordForm />
    </>
  )
}
