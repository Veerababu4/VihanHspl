import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

import { ApiError } from '../api/client'

/** Loading / error / not-found handling shared by pages that fetch one thing. */
export function QueryState({
  isPending,
  error,
  notFoundText,
  errorText,
  children,
}: {
  isPending: boolean
  error: Error | null
  notFoundText?: string
  errorText?: string
  children: ReactNode
}) {
  const { t } = useTranslation()
  if (isPending) {
    return <p className="mx-auto max-w-6xl px-4 py-12 text-slate-500">{t('common.loading')}</p>
  }
  if (error) {
    const notFound = error instanceof ApiError && error.status === 404
    return (
      <p role="alert" className="mx-auto max-w-6xl px-4 py-12 text-emergency-600">
        {notFound && notFoundText ? notFoundText : (errorText ?? error.message)}
      </p>
    )
  }
  return <>{children}</>
}
