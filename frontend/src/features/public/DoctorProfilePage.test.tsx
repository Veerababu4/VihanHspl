import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { routes } from '../../routes'
import { drRamanaDetail, layoutApi } from '../../test/fixtures'
import { mockApi, renderRoutes } from '../../test/render'

describe('DoctorProfilePage', () => {
  it('shows details, fee and weekly timings per branch', async () => {
    mockApi({ ...layoutApi, '/public/doctors/6': drRamanaDetail })
    renderRoutes(routes, '/doctors/6')

    expect(await screen.findByRole('heading', { level: 1, name: 'Dr. Venkata Ramana Pulla' })).toBeInTheDocument()
    expect(screen.getByText('₹800')).toBeInTheDocument()
    expect(screen.getByText('SAMPLE-006')).toBeInTheDocument()

    const kodad = within(screen.getByRole('main')).getByRole('link', { name: 'Kodad' }).closest('div')!
    const wednesday = within(kodad).getByRole('row', { name: /Wednesday/ })
    expect(wednesday).toHaveTextContent('10:00 AM – 2:00 PM')
    expect(wednesday).toHaveTextContent('5:00 PM – 7:00 PM')
    expect(screen.getByRole('link', { name: 'Book appointment' })).toHaveAttribute('href', '/book?doctor=6')
    expect(document.title).toContain('Dr. Venkata Ramana Pulla')
  })

  it('shows a not-found message for an unknown doctor', async () => {
    mockApi({ ...layoutApi, '/public/doctors/999': new Response('{"code":"NOT_FOUND"}', { status: 404 }) })
    renderRoutes(routes, '/doctors/999')

    expect(await screen.findByRole('alert')).toHaveTextContent('Doctor not found.')
  })
})
