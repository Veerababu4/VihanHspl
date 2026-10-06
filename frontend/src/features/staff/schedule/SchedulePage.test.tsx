import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import type { ChangeSummary, Schedule, Slot } from '../../../api/schedule'
import type { Me } from '../../../auth/session'
import { addDays, todayIso } from '../../../lib/dates'
import { routes } from '../../../routes'
import { drRamanaDetail, huzurnagar, kodad } from '../../../test/fixtures'
import { mockApi, renderRoutes, requestBody } from '../../../test/render'

const doctorUser: Me = { id: 5, username: 'doctor006', fullName: 'Dr. Venkata Ramana Pulla', role: 'DOCTOR', doctorId: 6, homeBranchId: null, mustChangePassword: false }
const session = { accessToken: 't', expiresAt: '2026-10-06T12:00:00Z', user: doctorUser }
const today = todayIso()
const tomorrow = addDays(today, 1)

const wednesdayAtKodad: Schedule = {
  id: 11, doctorId: 6, branchId: 3, branchCode: 'KDD', branchCity: 'Kodad', dayOfWeek: 3, startTime: '10:00', endTime: '14:00',
  slotMinutes: 20, effectiveFrom: '2026-01-01', effectiveTo: null, slotsPerSession: 12,
}
const slot = (id: number, time: string, status: Slot['status'], blockReason: Slot['blockReason'] = null): Slot => ({
  id, branchId: 3, branchCode: 'KDD', date: tomorrow, startAt: `${tomorrow}T${time}:00`, endAt: `${tomorrow}T${time}:00`,
  tokenNo: id, status, source: 'TEMPLATE', blockReason,
})
const noChange: ChangeSummary = { slotsCreated: 0, slotsRemoved: 0, slotsBlocked: 0, slotsReopened: 0, affectedBookings: [] }

const baseApi = {
  '/public/app-info': { name: 'Vihan', sampleData: false },
  '/public/branches': [huzurnagar, kodad],
  'POST /auth/refresh': session,
  '/public/doctors/6': drRamanaDetail,   // works at HZN and KDD
  '/staff/doctors/6/schedules': [wednesdayAtKodad],
  '/staff/doctors/6/leaves': [],
}

describe('Schedule page (doctor)', () => {
  it('shows the weekly timetable and adds a session for one of the doctor’s branches', async () => {
    const spy = mockApi({
      ...baseApi,
      'POST /staff/doctors/6/schedules': { item: { ...wednesdayAtKodad, id: 12 }, changes: { ...noChange, slotsCreated: 12 } },
    })
    renderRoutes(routes, '/staff/schedule')

    const wednesday = await screen.findByRole('region', { name: 'Wednesday' })
    expect(await within(wednesday).findByText('10:00 AM – 2:00 PM')).toBeInTheDocument()
    expect(within(wednesday).getByText(/Kodad · 12 slots/)).toBeInTheDocument()
    expect(screen.queryByLabelText('Doctor')).not.toBeInTheDocument()   // a doctor sees only their own

    fireEvent.click(screen.getByRole('button', { name: 'Add session' }))
    const dialog = screen.getByRole('dialog', { name: 'New weekly session' })
    expect(within(dialog).getAllByRole('option', { name: /Huzurnagar|Kodad/ })).toHaveLength(2)
    fireEvent.change(within(dialog).getByLabelText('Branch'), { target: { value: '2' } })
    fireEvent.change(within(dialog).getByLabelText('Day'), { target: { value: '5' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('12 slots created')).toBeInTheDocument()
    expect(requestBody(spy, 'POST /staff/doctors/6/schedules')).toMatchObject({
      branchId: 2, dayOfWeek: 5, startTime: '10:00', endTime: '14:00', slotMinutes: 20, effectiveTo: null,
    })
  })

  it('blocks an open slot with one click and refuses to touch booked ones', async () => {
    const spy = mockApi({
      ...baseApi,
      [`/staff/doctors/6/slots?from=${today}&to=${addDays(today, 13)}`]: [slot(1, '10:00', 'BOOKED'), slot(2, '10:20', 'OPEN'), slot(3, '10:40', 'BLOCKED', 'LEAVE')],
      'PATCH /staff/slots/2': slot(2, '10:20', 'BLOCKED', 'MANUAL'),
    })
    renderRoutes(routes, '/staff/schedule?tab=slots')

    fireEvent.click(await screen.findByRole('tab', { name: /^Tomorrow|1 open/ }))
    const booked = await screen.findByRole('button', { name: '10:00, token 1, Booked' })
    expect(booked).toBeDisabled()
    expect(screen.getByRole('button', { name: '10:40, token 3, leave' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '10:20, token 2, Open' }))
    await waitFor(() => expect(requestBody(spy, 'PATCH /staff/slots/2')).toEqual({ status: 'BLOCKED' }))
  })

  it('lists the booked patients to call after adding leave', async () => {
    mockApi({
      ...baseApi,
      'POST /staff/doctors/6/leaves': {
        item: { id: 1, doctorId: 6, fromDate: tomorrow, toDate: tomorrow, reason: null },
        changes: {
          ...noChange,
          slotsBlocked: 11,
          affectedBookings: [{ bookingRef: 'VMH-261007-AB2C', patientName: 'Ravi Kumar', mobile: '98XXXXXX10', startAt: `${tomorrow}T10:00:00`, tokenNo: 1, branchCity: 'Kodad' }],
        },
      },
    })
    renderRoutes(routes, '/staff/schedule?tab=leave')

    fireEvent.click(await screen.findByRole('button', { name: 'Add leave' }))
    expect(await screen.findByText('11 blocked')).toBeInTheDocument()
    const warning = screen.getByRole('alert')
    expect(warning).toHaveTextContent('Booked patients to call (1)')
    expect(warning).toHaveTextContent('Ravi Kumar · 98XXXXXX10 · VMH-261007-AB2C')
  })
})
