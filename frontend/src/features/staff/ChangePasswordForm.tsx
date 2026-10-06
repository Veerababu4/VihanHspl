import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'

import { useAuth } from '../../auth/session'
import { Alert, Button, Field, inputClass } from '../../components/ui'
import { applyServerErrors } from '../../lib/forms'

type Form = { currentPassword: string; newPassword: string; confirm: string }

/** Used on "My password" and as the forced first-login screen (FR-A2). */
export function ChangePasswordForm({ onDone }: { onDone?: () => void }) {
  const { t } = useTranslation()
  const { changePassword } = useAuth()
  const [message, setMessage] = useState<{ kind: 'error' | 'success'; text: string } | null>(null)

  // Same rules as the backend (Passwords.checkPolicy), checked early for a faster answer.
  const schema = z
    .object({
      currentPassword: z.string().min(1, t('staff.password.current')),
      newPassword: z
        .string()
        .min(8, t('staff.password.rules'))
        .regex(/[A-Za-z]/, t('staff.password.rules'))
        .regex(/\d/, t('staff.password.rules')),
      confirm: z.string(),
    })
    .refine((v) => v.newPassword === v.confirm, { path: ['confirm'], message: t('staff.password.mismatch') })

  const form = useForm<Form>({ resolver: zodResolver(schema), defaultValues: { currentPassword: '', newPassword: '', confirm: '' } })
  const errors = form.formState.errors

  const onSubmit = form.handleSubmit(async (v) => {
    setMessage(null)
    try {
      await changePassword(v.currentPassword, v.newPassword)
      form.reset()
      setMessage({ kind: 'success', text: t('staff.password.changed') })
      onDone?.()
    } catch (e) {
      const text = applyServerErrors(e, form.setError)
      if (text) setMessage({ kind: 'error', text })
    }
  })

  return (
    <form onSubmit={onSubmit} noValidate className="max-w-md space-y-4">
      {message && <Alert kind={message.kind}>{message.text}</Alert>}
      <Field label={t('staff.password.current')} error={errors.currentPassword?.message}>
        {(a) => <input {...a} {...form.register('currentPassword')} type="password" autoComplete="current-password" className={inputClass} />}
      </Field>
      <Field label={t('staff.password.new')} error={errors.newPassword?.message} hint={t('staff.password.rules')}>
        {(a) => <input {...a} {...form.register('newPassword')} type="password" autoComplete="new-password" className={inputClass} />}
      </Field>
      <Field label={t('staff.password.confirm')} error={errors.confirm?.message}>
        {(a) => <input {...a} {...form.register('confirm')} type="password" autoComplete="new-password" className={inputClass} />}
      </Field>
      <Button type="submit" disabled={form.formState.isSubmitting}>
        {form.formState.isSubmitting ? t('staff.common.saving') : t('staff.password.submit')}
      </Button>
    </form>
  )
}
