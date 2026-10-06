import { lazy } from 'react'

// Booking pages load on demand: the form and validation libraries stay out of the home page bundle.

export const BookingPage = lazy(() => import('./BookingPage').then((m) => ({ default: m.BookingPage })))
export const ConfirmationPage = lazy(() => import('./ConfirmationPage').then((m) => ({ default: m.ConfirmationPage })))
export const AppointmentStatusPage = lazy(() => import('./AppointmentStatusPage').then((m) => ({ default: m.AppointmentStatusPage })))
