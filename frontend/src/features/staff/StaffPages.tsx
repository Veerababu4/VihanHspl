import { useTranslation } from 'react-i18next'

import { Seo } from '../../components/Seo'
import { PageHeader } from '../../components/ui'
import { ChangePasswordForm } from './ChangePasswordForm'

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
