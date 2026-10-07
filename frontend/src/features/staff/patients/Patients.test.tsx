import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { normalizeAadhaar, type PatientDetail, type VisitRow } from '../../../api/patients'
import type { PatientSummary } from '../../../api/reception'
import type { Me } from '../../../auth/session'
import { routes } from '../../../routes'
import { huzurnagar, kodad, layoutApi } from '../../../test/fixtures'
import { json, mockApi, renderRoutes, requestBody } from '../../../test/render'

const receptionist: Me = { id: 2, username: 'reception.kdd', fullName: 'Asha Receptionist', role: 'RECEPTIONIST', doctorId: null, homeBranchId: 3, mustChangePassword: false }
const doctor: Me = { ...receptionist, id: 9, username: 'doctor012', fullName: 'Dr. Haritha Kommu', role: 'DOCTOR', doctorId: 12, homeBranchId: null }
const session = (user: Me) => ({ accessToken: 't', expiresAt: '2026-10-07T12:00:00Z', user })

const VALID_AADHAAR = '234123412346'   // fictional, checksum-valid
const lakshmi: PatientSummary = {
  id: 21, uhid: 'VMH-26-000021', fullName: 'Lakshmi Reddy', age: 34, dateOfBirth: '1991-07-01', dobEstimated: true, gender: 'F',
  mobile: '9876500001', registeredBranchCode: 'KDD', aadhaarMasked: 'XXXX-XXXX-2346',
}
const detail: PatientDetail = {
  ...lakshmi, altMobile: null, address: '1-2, Main Road', city: 'Kodad', bloodGroup: 'B+', emergencyName: 'Ramu',
  emergencyPhone: '9876500009', registeredBranchCity: 'Kodad', registeredAt: '2026-10-01T09:30:00',
  upcoming: [{ id: 5, bookingRef: 'VMH-261009-7K3Q', date: '2026-10-09', startAt: '2026-10-09T10:20:00', tokenNo: 2, doctorName: 'Dr. Haritha Kommu', branchCity: 'Kodad', status: 'BOOKED' }],
}
const visits: VisitRow[] = [{ id: 1, date: '2026-09-12', branchCity: 'Kodad', doctorName: 'Dr. Haritha Kommu', speciality: 'ENT', type: 'OP', status: 'COMPLETED' }]

const deskApi = { ...layoutApi, '/public/branches': [huzurnagar, kodad], 'POST /auth/refresh': session(receptionist) }

describe('Aadhaar check in the browser', () => {
  it('accepts valid numbers with spaces and refuses a wrong check digit', () => {
    expect(normalizeAadhaar('2341 2341 2346')).toBe(VALID_AADHAAR)
    expect(normalizeAadhaar('234123412345')).toBeNull()
    expect(normalizeAadhaar('134123412346')).toBeNull()
  })
})

describe('Patients (reception)', () => {
  it('searches by name and by Aadhaar, sending Aadhaar only in the request body', async () => {
    const spy = mockApi({
      ...deskApi,
      '/staff/patients?name=lakshmi': [lakshmi],
      'POST /staff/patients/search-aadhaar': [lakshmi],
    })
    renderRoutes(routes, '/staff/patients')

    fireEvent.change(await screen.findByLabelText('Search for', {}, { timeout: 15000 }), { target: { value: 'lakshmi' } })
    fireEvent.click(screen.getByRole('button', { name: 'Search' }))
    const link = await screen.findByRole('link', { name: /Lakshmi Reddy/ })
    expect(link).toHaveAttribute('href', '/staff/patients/21')
    expect(within(link).getByText('XXXX-XXXX-2346')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Search by'), { target: { value: 'aadhaar' } })
    expect(screen.getByText('Aadhaar searches are recorded in the audit log.')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Search for'), { target: { value: '2341 2341 2346' } })
    fireEvent.click(screen.getByRole('button', { name: 'Search' }))
    await waitFor(() => expect(requestBody(spy, 'POST /staff/patients/search-aadhaar')).toEqual({ aadhaar: '2341 2341 2346' }))
    expect(spy.mock.calls.some(([url]) => String(url).includes('2341'))).toBe(false)
  })

  it('registers a patient with Aadhaar and details, then opens the profile', async () => {
    const spy = mockApi({
      ...deskApi,
      'POST /staff/patients': lakshmi,
      '/staff/patients/21': detail,
      '/staff/patients/21/visits': visits,
    })
    renderRoutes(routes, '/staff/patients/new')

    await waitFor(() => expect(screen.getByLabelText('Registered at')).toHaveValue('KDD'), { timeout: 15000 })
    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'Lakshmi Reddy' } })
    fireEvent.change(screen.getByLabelText('Age (years)'), { target: { value: '34' } })
    fireEvent.click(screen.getByLabelText('Female'))
    fireEvent.change(screen.getByLabelText('Mobile number'), { target: { value: '98765 00001' } })
    fireEvent.change(screen.getByLabelText('Aadhaar number (optional)'), { target: { value: '2341 2341 2345' } })
    fireEvent.change(screen.getByLabelText('Blood group'), { target: { value: 'B+' } })
    fireEvent.click(screen.getByLabelText('The patient agreed to the privacy notice'))
    fireEvent.click(screen.getByRole('button', { name: 'Register' }))
    expect(await screen.findByText('Not a valid Aadhaar number. Check the 12 digits.')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Aadhaar number (optional)'), { target: { value: '2341 2341 2346' } })
    fireEvent.click(screen.getByRole('button', { name: 'Register' }))

    expect(await screen.findByRole('heading', { name: 'Lakshmi Reddy' })).toBeInTheDocument()
    expect(requestBody(spy, 'POST /staff/patients')).toMatchObject({
      fullName: 'Lakshmi Reddy', age: 34, gender: 'F', mobile: '9876500001', branch: 'KDD', aadhaar: VALID_AADHAAR,
      bloodGroup: 'B+', city: null, confirmDuplicate: false, consent: true,
    })
  })

  it('shows possible duplicates first and registers only when confirmed', async () => {
    let calls = 0
    const spy = mockApi({
      ...deskApi,
      'POST /staff/patients': () =>
        ++calls === 1
          ? json({ code: 'POSSIBLE_DUPLICATE', message: 'Already registered…' }, 409)
          : { ...lakshmi, id: 22, uhid: 'VMH-26-000022', fullName: 'K. Lakshmi Reddy' },
      'POST /staff/patients/duplicates': { aadhaarMatch: null, similar: [lakshmi] },
      '/staff/patients/22': { ...detail, id: 22, fullName: 'K. Lakshmi Reddy' },
      '/staff/patients/22/visits': [],
    })
    renderRoutes(routes, '/staff/patients/new')

    await waitFor(() => expect(screen.getByLabelText('Registered at')).toHaveValue('KDD'), { timeout: 15000 })
    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'K. Lakshmi Reddy' } })
    fireEvent.change(screen.getByLabelText('Age (years)'), { target: { value: '34' } })
    fireEvent.click(screen.getByLabelText('Female'))
    fireEvent.change(screen.getByLabelText('Mobile number'), { target: { value: '9876500001' } })
    fireEvent.click(screen.getByLabelText('The patient agreed to the privacy notice'))
    fireEvent.click(screen.getByRole('button', { name: 'Register' }))

    const warning = await screen.findByRole('region', { name: 'This may be someone already registered' })
    expect(within(warning).getByRole('link', { name: 'Open record' })).toHaveAttribute('href', '/staff/patients/21')
    fireEvent.click(within(warning).getByRole('button', { name: 'Different person — register anyway' }))

    expect(await screen.findByRole('heading', { name: 'K. Lakshmi Reddy' })).toBeInTheDocument()
    expect(requestBody(spy, 'POST /staff/patients', 0).confirmDuplicate).toBe(false)
    expect(requestBody(spy, 'POST /staff/patients', 1).confirmDuplicate).toBe(true)
  })

  it('shows the profile with masked Aadhaar and edits without resending the number', async () => {
    const spy = mockApi({
      ...deskApi,
      '/staff/patients/21': detail,
      '/staff/patients/21/visits': visits,
      'PUT /staff/patients/21': { ...detail, city: 'Huzurnagar' },
    })
    renderRoutes(routes, '/staff/patients/21')

    expect(await screen.findByRole('heading', { name: 'Lakshmi Reddy' }, { timeout: 15000 })).toBeInTheDocument()
    expect(screen.getByText('XXXX-XXXX-2346')).toBeInTheDocument()
    expect(screen.getByText('1 Jul 1991 (estimated from age)')).toBeInTheDocument()
    expect(screen.getByText('VMH-261009-7K3Q')).toBeInTheDocument()
    expect(await screen.findByText('12 Sept 2026')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Edit details' }))
    const dialog = screen.getByRole('dialog', { name: 'Edit patient details' })
    expect(within(dialog).getByLabelText('Aadhaar number (optional)')).toHaveValue('')
    expect(within(dialog).getByText('Leave empty to keep XXXX-XXXX-2346.')).toBeInTheDocument()
    fireEvent.change(within(dialog).getByLabelText('City / village'), { target: { value: 'Huzurnagar' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Details saved.')).toBeInTheDocument()
    expect(requestBody(spy, 'PUT /staff/patients/21')).toMatchObject({
      fullName: 'Lakshmi Reddy', age: 34, dateOfBirth: null, aadhaar: null, removeAadhaar: false, city: 'Huzurnagar',
    })
  })
})

describe('Patients (doctor)', () => {
  it('lets a doctor look but not register or edit', async () => {
    mockApi({
      ...layoutApi,
      'POST /auth/refresh': session(doctor),
      '/staff/patients/21': detail,
      '/staff/patients/21/visits': visits,
    })
    renderRoutes(routes, '/staff/patients/21')

    expect(await screen.findByRole('heading', { name: 'Lakshmi Reddy' }, { timeout: 15000 })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Edit details' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('link', { name: /Back to patients/ }))
    expect(await screen.findByRole('heading', { name: 'Patients' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Register patient' })).not.toBeInTheDocument()
  })
})
