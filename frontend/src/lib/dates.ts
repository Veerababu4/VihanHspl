// Dates in the staff portal are IST calendar dates ("YYYY-MM-DD"), matching the backend.

/** Today's date as YYYY-MM-DD in the browser's local time (the hospital's staff are in IST). */
export function todayIso(): string {
  return toIso(new Date())
}

/** Local date-time "YYYY-MM-DDTHH:mm:ss", comparable with the backend's slot times. */
export function nowLocalIso(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${toIso(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export function toIso(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function addDays(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number)
  return toIso(new Date(y, m - 1, d + days))
}

/** "2026-10-07" → "Wed 7 Oct" */
export function shortDate(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' })
}

/** "2026-10-07T10:20:00" → "10:20" (backend sends IST local date-times without a zone) */
export function timeOf(isoDateTime: string): string {
  return isoDateTime.slice(11, 16)
}

/** "1993-07-01" → "1 Jul 1993" (birth dates, past visits) */
export function fullDate(iso: string): string {
  const [y, m, d] = iso.slice(0, 10).split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}
