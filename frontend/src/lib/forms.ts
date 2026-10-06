import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'

import { ApiError } from '../api/client'

/**
 * Puts backend field errors (400 VALIDATION_FAILED) onto the matching form fields.
 * @returns a general message to show above the form, or null when every error has a field
 */
export function applyServerErrors<T extends FieldValues>(error: unknown, setError: UseFormSetError<T>): string | null {
  if (!(error instanceof ApiError)) return 'Something went wrong. Please try again.'
  for (const f of error.fieldErrors) {
    setError(f.field as Path<T>, { message: f.message })
  }
  return error.fieldErrors.length ? null : error.message
}
