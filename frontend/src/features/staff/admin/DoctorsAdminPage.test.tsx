import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import type { AdminDoctor } from '../../../api/admin'
import type { Me } from '../../../auth/session'
import { routes } from '../../../routes'
import { layoutApi } from '../../../test/fixtures'
import { mockApi, renderRoutes, requestBody } from '../../../test/render'

const SLOW = { timeout: 15000 }   // the first render also loads the lazy staff bundle

const admin: Me = { id: 1, username: 'itadmin', fullName: 'IT Admin', role: 'IT_ADMIN', doctorId: null, homeBranchId: 1, mustChangePassword: false }

const doctor: AdminDoctor = {
  id: 7, fullName: 'Dr. Anitha Gundla', gender: 'F', specialityId: 6, specialityName: 'Neurology',
  qualification: 'MBBS, MD, DM (Neurology)', designation: 'Consultant', registrationNo: 'SAMPLE-007',
  practiceStartYear: 2011, experienceYears: 15, languages: 'Telugu', bio: null, photoUrl: null,
  consultationFee: 700, active: true, branchIds: [1],
}

/** Answers after a short delay, like a real network: the dialog opens before its lists arrive. */
const slow = (body: unknown) => () => new Promise((resolve) => setTimeout(() => resolve(body), 30))

describe('Doctor admin form', () => {
  it('keeps the saved speciality selected even when the list loads after the dialog opens', async () => {
    const spy = mockApi({
      ...layoutApi,
      '/public/specialities': [],
      '/public/doctors': [],
      'POST /auth/refresh': { accessToken: 't', expiresAt: '2026-10-06T12:00:00Z', user: admin },
      '/admin/doctors': [doctor],
      '/admin/specialities': slow([
        { id: 5, name: 'Cardiology', description: null, active: true },
        { id: 6, name: 'Neurology', description: null, active: true },
      ]),
      '/admin/branches': slow([{ id: 1, code: 'MLG', city: 'Miryalaguda' }, { id: 2, code: 'HZN', city: 'Huzurnagar' }]),
      'PUT /admin/doctors/7': doctor,
    })
    renderRoutes(routes, '/staff/admin/doctors')

    fireEvent.click(await screen.findByRole('button', { name: 'Edit Dr. Anitha Gundla' }, SLOW))
    const speciality = await screen.findByLabelText('Speciality')   // form appears once the lists have loaded
    const dialog = screen.getByRole('dialog', { name: 'Edit doctor' })
    expect(speciality).toHaveDisplayValue('Neurology')
    expect(within(dialog).getByRole('checkbox', { name: 'Miryalaguda' })).toBeChecked()

    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }))
    await waitFor(() => expect(requestBody(spy, 'PUT /admin/doctors/7')).toMatchObject({ specialityId: 6, branchIds: [1] }))
  })
})
