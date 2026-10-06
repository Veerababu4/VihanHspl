import { useState } from 'react'
import { useTranslation } from 'react-i18next'

import { useBranches } from '../api/public'

/**
 * Sticky "Emergency" button on phones (DESIGN.md §19.8): one tap opens the three branch
 * emergency numbers as tel: links. Hidden on wide screens, where the header/footer show them.
 */
export function EmergencyButton() {
  const { t } = useTranslation()
  const { data: branches } = useBranches()
  const [open, setOpen] = useState(false)
  const withPhone = branches?.filter((b) => b.emergencyPhone) ?? []
  if (withPhone.length === 0) return null

  return (
    <div className="fixed right-4 bottom-4 z-20 flex flex-col items-end gap-2 md:hidden print:hidden">
      {open && (
        <ul
          id="emergency-numbers"
          aria-label={t('emergency.title')}
          className="w-56 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg"
        >
          {withPhone.map((b) => (
            <li key={b.id}>
              <a
                href={`tel:${b.emergencyPhone}`}
                className="block border-b border-slate-100 px-4 py-3 font-medium text-emergency-600 last:border-0"
              >
                {t('emergency.call', { city: b.city })}
                <span className="block text-sm text-slate-600">{b.emergencyPhone}</span>
              </a>
            </li>
          ))}
        </ul>
      )}
      <button
        type="button"
        aria-expanded={open}
        aria-controls="emergency-numbers"
        onClick={() => setOpen((o) => !o)}
        className="rounded-full bg-emergency-600 px-5 py-3 font-semibold text-white shadow-lg"
      >
        🚑 {t('emergency.button')}
      </button>
    </div>
  )
}
