import { useTranslation } from 'react-i18next'

import { shortDate, timeOf } from '../../../lib/dates'

export interface PickerDay {
  date: string
  openSlots: number
}

export interface PickerSlot {
  id: number
  startAt: string
  tokenNo: number
}

/** Date chips (with free counts) then time buttons. Used by booking, reschedule and counter booking. */
export function SlotPicker({
  days,
  date,
  onDate,
  slots,
  slotsLoading,
  slotId,
  onSlot,
}: {
  days: PickerDay[]
  date: string | undefined
  onDate: (date: string) => void
  slots: PickerSlot[] | undefined
  slotsLoading: boolean
  slotId: number | undefined
  onSlot: (slotId: number) => void
}) {
  const { t } = useTranslation()
  if (days.every((d) => d.openSlots === 0)) {
    return <p className="rounded-lg bg-amber-50 p-4 text-amber-900">{t('booking.noDays')}</p>
  }
  return (
    <div className="min-w-0 space-y-5">
      {/* min-w-0: a fieldset is never narrower than its content by default; the date row must scroll instead */}
      <fieldset className="min-w-0">
        <legend className="mb-2 text-sm font-medium text-slate-700">{t('booking.date')}</legend>
        <div className="flex gap-2 overflow-x-auto pb-2">
          {days.map((d) => {
            const selected = d.date === date
            const full = d.openSlots === 0
            return (
              <button
                key={d.date}
                type="button"
                disabled={full}
                aria-pressed={selected}
                onClick={() => onDate(d.date)}
                className={`shrink-0 rounded-lg border px-3 py-2 text-center text-sm ${
                  selected
                    ? 'border-brand-600 bg-brand-600 text-white'
                    : full
                      ? 'cursor-not-allowed border-slate-200 bg-slate-50 text-slate-400'
                      : 'border-slate-300 bg-white hover:border-brand-500'
                }`}
              >
                <span className="block font-medium">{shortDate(d.date)}</span>
                <span className={`block text-xs ${selected ? 'text-white' : ''}`}>
                  {full ? t('booking.none') : t('booking.freeCount', { count: d.openSlots })}
                </span>
              </button>
            )
          })}
        </div>
      </fieldset>

      {date && (
        <fieldset className="min-w-0">
          <legend className="mb-2 text-sm font-medium text-slate-700">{t('booking.time')}</legend>
          {slotsLoading && <p className="text-slate-500">{t('common.loading')}</p>}
          {slots && slots.length === 0 && <p className="text-slate-600">{t('booking.noSlots')}</p>}
          <div className="grid grid-cols-3 gap-2 sm:grid-cols-5 lg:grid-cols-6">
            {slots?.map((s) => (
              <button
                key={s.id}
                type="button"
                aria-pressed={s.id === slotId}
                aria-label={t('booking.slotLabel', { time: timeOf(s.startAt), token: s.tokenNo })}
                onClick={() => onSlot(s.id)}
                className={`rounded-lg border px-2 py-2 text-sm font-medium ${
                  s.id === slotId ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-300 bg-white hover:border-brand-500'
                }`}
              >
                {timeOf(s.startAt)}
                <span className={`block text-xs font-normal ${s.id === slotId ? 'text-white' : 'text-slate-500'}`}>
                  {t('booking.tokenShort', { token: s.tokenNo })}
                </span>
              </button>
            ))}
          </div>
        </fieldset>
      )}
    </div>
  )
}
