import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import type { ClinicalNote, History, Queue, VisitDetail } from '../../../api/clinical'
import type { Me } from '../../../auth/session'
import { todayIso } from '../../../lib/dates'
import { routes } from '../../../routes'
import { layoutApi } from '../../../test/fixtures'
import { mockApi, renderRoutes, requestBody } from '../../../test/render'

const doctor: Me = { id: 9, username: 'doctor012', fullName: 'Dr. Haritha Kommu', role: 'DOCTOR', doctorId: 12, homeBranchId: null, mustChangePassword: false }
const receptionist: Me = { ...doctor, id: 2, username: 'reception.kdd', fullName: 'Asha Receptionist', role: 'RECEPTIONIST', doctorId: null, homeBranchId: 3 }
const session = (user: Me) => ({ accessToken: 't', expiresAt: '2026-10-07T12:00:00Z', user })
const today = todayIso()
const SLOW = { timeout: 15000 }   // the first render also loads the lazy staff bundle

const emptyNote: ClinicalNote = {
  bpSystolic: null, bpDiastolic: null, pulse: null, temperatureF: null, spo2: null, weightKg: null, heightCm: null,
  chiefComplaint: null, diagnosis: null, clinicalNotes: null, advice: null, followUpDate: null,
}
const visit: VisitDetail = {
  id: 31, date: today, type: 'OP', status: 'OPEN', canEdit: true,
  patient: { id: 21, uhid: 'VMH-26-000021', fullName: 'Lakshmi Reddy', age: 34, gender: 'F', mobile: '9876500001' },
  allergies: 'Penicillin',
  doctor: { id: 12, fullName: 'Dr. Haritha Kommu', speciality: 'ENT', qualification: 'MBBS, MS (ENT)', registrationNo: 'TSMC-12345' },
  branch: { code: 'KDD', name: 'Vihan Multi Speciality Hospital - Kodad', city: 'Kodad', address: 'Main Road, Kodad', phone: '0000000003' },
  tokenNo: 2, bookingReason: 'Ear pain', clinical: emptyNote, prescription: [],
}
const doctorApi = { ...layoutApi, 'POST /auth/refresh': session(doctor) }

describe('Queue', () => {
  it('lists waiting patients in token order with an allergy flag and a link to the visit', async () => {
    const queue: Queue = {
      date: today,
      waiting: [
        { appointmentId: 7, visitId: 31, tokenNo: 2, startAt: `${today}T10:20:00`, checkedInAt: `${today}T10:05:00`, channel: 'ONLINE', patientId: 21, uhid: 'VMH-26-000021', patientName: 'Lakshmi Reddy', age: 34, gender: 'F', reason: 'Ear pain', branchCity: 'Kodad', visitStatus: 'OPEN', hasAllergies: true },
      ],
      done: [],
    }
    mockApi({ ...doctorApi, [`/clinical/queue?date=${today}`]: queue })
    renderRoutes(routes, '/staff/queue')

    const row = await screen.findByRole('listitem', { name: 'Lakshmi Reddy, token 2' }, SLOW)
    expect(within(row).getByText('Allergies')).toBeInTheDocument()
    expect(within(row).getByRole('link', { name: 'Open' })).toHaveAttribute('href', '/staff/visits/31')
    expect(screen.getByRole('heading', { name: 'Seen (0)' })).toBeInTheDocument()
  })

  it('is not for reception', async () => {
    mockApi({ ...layoutApi, 'POST /auth/refresh': session(receptionist), [`/staff/dashboard/summary?date=${today}&branch=KDD`]: {} })
    renderRoutes(routes, '/staff/queue')
    expect(await screen.findByText('You do not have access to this page.', {}, SLOW)).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'My queue' })).not.toBeInTheDocument()
  })
})

describe('Visit screen', () => {
  it('warns about allergies, records vitals and a prescription, and saves them together', async () => {
    const spy = mockApi({
      ...doctorApi,
      '/clinical/visits/31': visit,
      'PUT /clinical/visits/31': { ...visit, clinical: { ...emptyNote, pulse: 78, diagnosis: 'Otitis media' }, prescription: [{ medicineName: 'Amoxicillin', dosage: null, frequency: null, duration: null, instructions: null }] },
    })
    renderRoutes(routes, '/staff/visits/31')

    expect(await screen.findByRole('alert', {}, SLOW)).toHaveTextContent('Allergies: Penicillin')
    fireEvent.change(screen.getByLabelText('Pulse (/min)'), { target: { value: '780' } })
    fireEvent.change(screen.getByLabelText('Weight (kg)'), { target: { value: '60' } })
    fireEvent.change(screen.getByLabelText('Height (cm)'), { target: { value: '160' } })
    expect(await screen.findByText('BMI 23.4')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Diagnosis'), { target: { value: 'Otitis media' } })
    fireEvent.click(screen.getByRole('button', { name: 'Add medicine' }))
    fireEvent.change(screen.getByLabelText('Medicine'), { target: { value: 'Amoxicillin' } })
    fireEvent.change(screen.getByLabelText('Frequency'), { target: { value: '1-0-1' } })

    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(await screen.findByText('Between 20 and 250')).toBeInTheDocument()
    expect(requestBody(spy, 'PUT /clinical/visits/31')).toBeUndefined()

    fireEvent.change(screen.getByLabelText('Pulse (/min)'), { target: { value: '78' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(await screen.findByText('Saved.')).toBeInTheDocument()
    expect(requestBody(spy, 'PUT /clinical/visits/31')).toMatchObject({
      pulse: 78, weightKg: 60, heightCm: 160, bpSystolic: null, diagnosis: 'Otitis media', chiefComplaint: null, followUpDate: null,
      prescription: [{ medicineName: 'Amoxicillin', dosage: null, frequency: '1-0-1', duration: null, instructions: null }],
    })
  })

  it('saves and then completes after confirmation', async () => {
    const completed = { ...visit, status: 'COMPLETED' as const, canEdit: false }
    const spy = mockApi({
      ...doctorApi,
      '/clinical/visits/31': visit,
      'PUT /clinical/visits/31': visit,
      'POST /clinical/visits/31/complete': completed,
    })
    renderRoutes(routes, '/staff/visits/31')

    fireEvent.click(await screen.findByRole('button', { name: 'Complete visit' }, SLOW))
    const dialog = screen.getByRole('dialog', { name: 'Complete visit' })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Complete visit' }))

    expect(await screen.findByText('This visit is completed.')).toBeInTheDocument()
    expect(requestBody(spy, 'PUT /clinical/visits/31')).toBeDefined()
    await waitFor(() => expect(spy.mock.calls.some(([url, init]) => String(url) === '/api/clinical/visits/31/complete' && init?.method === 'POST')).toBe(true))
    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument()
    expect(screen.getByLabelText('Diagnosis')).toBeDisabled()
  })

  it('is read-only for another doctor', async () => {
    mockApi({ ...doctorApi, '/clinical/visits/31': { ...visit, canEdit: false, doctor: { ...visit.doctor, fullName: 'Dr. Ravi Teja' } } })
    renderRoutes(routes, '/staff/visits/31')
    expect(await screen.findByText('Recorded by Dr. Ravi Teja. Only the treating doctor can change it.', {}, SLOW)).toBeInTheDocument()
    expect(screen.getByLabelText('Diagnosis')).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Add medicine' })).not.toBeInTheDocument()
  })
})

describe('Prescription print', () => {
  it('shows the letterhead, the doctor, the medicines and the follow-up date', async () => {
    mockApi({
      ...doctorApi,
      '/clinical/visits/31': {
        ...visit,
        clinical: { ...emptyNote, bpSystolic: 120, bpDiastolic: 80, diagnosis: 'Otitis media', advice: 'Keep the ear dry', followUpDate: '2026-10-14' },
        prescription: [{ medicineName: 'Amoxicillin', dosage: '500 mg', frequency: '1-0-1', duration: '5 days', instructions: 'after food' }],
      },
    })
    renderRoutes(routes, '/staff/visits/31/print')

    expect(await screen.findByText('Vihan Multi Speciality Hospital', {}, SLOW)).toBeInTheDocument()
    expect(screen.getByText('Reg. No. TSMC-12345')).toBeInTheDocument()
    expect(screen.getByText('BP 120/80')).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'Amoxicillin' })).toBeInTheDocument()
    expect(screen.getByText('Review on 14 Oct 2026')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Print' })).toBeInTheDocument()
  })
})

describe('Health history', () => {
  it('shows the medical profile and past visits, and edits the profile', async () => {
    const history: History = {
      patient: visit.patient,
      medicalProfile: { allergies: 'Penicillin', chronicConditions: null, pastSurgeries: null, currentMedications: null, familyHistory: null, updatedAt: null },
      visits: [{
        id: 30, date: '2026-09-12', type: 'OP', status: 'COMPLETED', doctorName: 'Dr. Ravi Teja', speciality: 'General Medicine', branchCity: 'Kodad',
        clinical: { ...emptyNote, diagnosis: 'Viral fever', pulse: 88 },
        prescription: [{ medicineName: 'Paracetamol', dosage: '650 mg', frequency: '1-1-1', duration: '3 days', instructions: null }],
      }],
    }
    const spy = mockApi({ ...doctorApi, '/clinical/patients/21/history': history, 'PUT /clinical/patients/21/medical-profile': history.medicalProfile })
    renderRoutes(routes, '/staff/patients/21/history')

    expect(await screen.findByText('Viral fever', {}, SLOW)).toBeInTheDocument()
    expect(screen.getByText('Pulse 88')).toBeInTheDocument()
    expect(screen.getByText('Paracetamol')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Open visit' })).toHaveAttribute('href', '/staff/visits/30')

    fireEvent.click(screen.getByRole('button', { name: 'Edit medical profile' }))
    const dialog = screen.getByRole('dialog', { name: 'Edit medical profile' })
    fireEvent.change(within(dialog).getByLabelText('Chronic conditions'), { target: { value: 'Asthma' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }))
    expect(await screen.findByText('Medical profile saved.')).toBeInTheDocument()
    expect(requestBody(spy, 'PUT /clinical/patients/21/medical-profile')).toEqual({
      allergies: 'Penicillin', chronicConditions: 'Asthma', pastSurgeries: null, currentMedications: null, familyHistory: null,
    })
  })
})
