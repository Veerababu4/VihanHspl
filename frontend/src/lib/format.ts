const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })

/** 500 → "₹500" */
export function formatFee(amount: number | null | undefined): string | null {
  return amount == null ? null : inr.format(amount)
}

/** "17:00" → "5:00 PM" */
export function formatTime(hhmm: string): string {
  const [h, m] = hhmm.split(':').map(Number)
  const suffix = h >= 12 ? 'PM' : 'AM'
  const hour12 = h % 12 === 0 ? 12 : h % 12
  return `${hour12}:${String(m).padStart(2, '0')} ${suffix}`
}

/** "Dr. Ravi Teja Gaddam" → "RG" (first and last name, ignoring the title) */
export function initials(fullName: string): string {
  const parts = fullName.replace(/^dr\.?\s+/i, '').split(/\s+/).filter(Boolean)
  if (parts.length === 0) return '?'
  const first = parts[0][0]
  const last = parts.length > 1 ? parts[parts.length - 1][0] : ''
  return (first + last).toUpperCase()
}

/** Google Maps directions link for a branch; needs no API key. */
export function directionsUrl(name: string, city: string): string {
  return `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(`${name}, ${city}, Telangana`)}`
}
