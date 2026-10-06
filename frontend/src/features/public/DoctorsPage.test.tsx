import { act, fireEvent, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { routes } from '../../routes'
import { cardiology, drHaritha, drRamana, ent, layoutApi } from '../../test/fixtures'
import { mockApi, renderRoutes } from '../../test/render'

const api = {
  ...layoutApi,
  '/public/specialities': [cardiology, ent],
  '/public/doctors': [drHaritha, drRamana],
  '/public/doctors?branch=KDD': [drHaritha, drRamana],
  '/public/doctors?branch=KDD&speciality=6': [drRamana],
  '/public/doctors?q=haritha': [drHaritha],
  '/public/doctors?q=nobody': [],
}

const requestedPaths = (spy: ReturnType<typeof mockApi>) => spy.mock.calls.map(([url]) => String(url))

describe('DoctorsPage', () => {
  afterEach(() => vi.useRealTimers())

  it('lists doctors with speciality, experience and branches', async () => {
    mockApi(api)
    renderRoutes(routes, '/doctors')

    const card = (await screen.findByRole('link', { name: 'Dr. Haritha Kommu' })).closest('li')!
    expect(within(card).getByText('ENT')).toBeInTheDocument()
    expect(within(card).getByText('1 year experience')).toBeInTheDocument()
    expect(within(card).getByRole('link', { name: 'Book appointment' })).toHaveAttribute('href', '/book?doctor=12')
    expect(screen.getByText('2 doctors')).toBeInTheDocument()
  })

  it('reads filters from the URL and sends them to the API', async () => {
    const spy = mockApi(api)
    renderRoutes(routes, '/doctors?branch=KDD&speciality=6')

    expect(await screen.findByText('1 doctor')).toBeInTheDocument()
    expect(requestedPaths(spy)).toContain('/api/public/doctors?branch=KDD&speciality=6')
    expect(screen.getByLabelText('Hospital')).toHaveValue('KDD')
  })

  it('searches after the user stops typing', async () => {
    const spy = mockApi(api)
    renderRoutes(routes, '/doctors')
    await screen.findByText('2 doctors')

    vi.useFakeTimers({ shouldAdvanceTime: true })
    fireEvent.change(screen.getByLabelText('Search by name or speciality'), { target: { value: 'haritha' } })
    expect(requestedPaths(spy)).not.toContain('/api/public/doctors?q=haritha')   // not on every keystroke
    await act(() => vi.advanceTimersByTimeAsync(400))

    expect(await screen.findByText('1 doctor')).toBeInTheDocument()
    expect(requestedPaths(spy)).toContain('/api/public/doctors?q=haritha')
  })

  it('shows an empty state with a way to clear filters', async () => {
    mockApi(api)
    renderRoutes(routes, '/doctors?q=nobody')

    expect(await screen.findByText('No doctors match your search.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Clear filters' }))
    expect(await screen.findByText('2 doctors')).toBeInTheDocument()
  })
})
