import { describe, expect, it } from 'vitest'

import { formatFee, formatTime, initials } from './format'

describe('format helpers', () => {
  it('formats 24-hour times as 12-hour', () => {
    expect(formatTime('10:00')).toBe('10:00 AM')
    expect(formatTime('12:30')).toBe('12:30 PM')
    expect(formatTime('17:00')).toBe('5:00 PM')
    expect(formatTime('00:05')).toBe('12:05 AM')
  })

  it('formats fees in rupees', () => {
    expect(formatFee(500)).toBe('₹500')
    expect(formatFee(null)).toBeNull()
  })

  it('takes initials from first and last name, skipping "Dr."', () => {
    expect(initials('Dr. Ravi Teja Gaddam')).toBe('RG')
    expect(initials('Dr Anitha')).toBe('A')
    expect(initials('Swathi Nalla')).toBe('SN')
  })
})
