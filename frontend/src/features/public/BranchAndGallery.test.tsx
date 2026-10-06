import { fireEvent, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { routes } from '../../routes'
import { drHaritha, kodad, layoutApi, pharmacyPhoto, wardPhoto } from '../../test/fixtures'
import { mockApi, renderRoutes } from '../../test/render'

describe('BranchPage', () => {
  it('shows contact details, map, directions, doctors and photos', async () => {
    mockApi({ ...layoutApi, '/public/branches/KDD': { branch: kodad, doctors: [drHaritha], gallery: [wardPhoto] } })
    renderRoutes(routes, '/branches/KDD')

    expect(await screen.findByRole('heading', { level: 1, name: 'Kodad' })).toBeInTheDocument()
    expect(screen.getByTitle('Map of Kodad')).toHaveAttribute('src', kodad.mapUrl)
    expect(screen.getByRole('link', { name: 'Get directions' }).getAttribute('href')).toContain('destination=Vihan')
    expect(screen.getByRole('link', { name: 'Dr. Haritha Kommu' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Open photo: Patient ward' })).toBeInTheDocument()
  })

  it('shows not found for an unknown branch', async () => {
    mockApi({ ...layoutApi, '/public/branches/XYZ': new Response('{}', { status: 404 }) })
    renderRoutes(routes, '/branches/XYZ')
    expect(await screen.findByRole('alert')).toHaveTextContent('Hospital not found.')
  })
})

describe('GalleryPage', () => {
  it('filters by branch and opens photos in a lightbox', async () => {
    mockApi({
      ...layoutApi,
      '/public/gallery': [wardPhoto, pharmacyPhoto],
      '/public/gallery?branch=KDD': [wardPhoto],
    })
    renderRoutes(routes, '/gallery')

    fireEvent.click(await screen.findByRole('button', { name: 'Open photo: Pharmacy' }))
    expect(screen.getByRole('dialog', { name: 'Pharmacy' })).toBeInTheDocument()
    fireEvent.keyDown(window, { key: 'ArrowRight' })
    expect(screen.getByRole('dialog', { name: 'Patient ward' })).toBeInTheDocument()
    fireEvent.keyDown(window, { key: 'Escape' })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Kodad' }))
    expect(await screen.findByRole('button', { name: 'Kodad', pressed: true })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Open photo: Pharmacy' })).not.toBeInTheDocument()
  })
})

describe('PublicLayout on phones', () => {
  it('opens the menu and the emergency numbers', async () => {
    mockApi({ ...layoutApi, '/public/gallery': [] })
    renderRoutes(routes, '/gallery')

    fireEvent.click(await screen.findByRole('button', { name: 'Menu' }))
    expect(screen.getByRole('navigation', { name: 'Mobile' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /Emergency/ }))
    expect(screen.getByRole('link', { name: /Call Kodad/ })).toHaveAttribute('href', 'tel:0000000103')
  })
})
