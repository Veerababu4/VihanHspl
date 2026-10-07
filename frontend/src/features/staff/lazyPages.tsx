import { lazy, Suspense } from 'react'
import { Outlet } from 'react-router'

// The staff portal is loaded only when someone opens /staff, so public visitors on mobile data
// never download it (separate bundles).

export const StaffRoot = lazy(() => import('./StaffLayout').then((m) => ({ default: m.StaffRoot })))
export const StaffLayout = lazy(() => import('./StaffLayout').then((m) => ({ default: m.StaffLayout })))
export const RequireRole = lazy(() => import('./StaffLayout').then((m) => ({ default: m.RequireRole })))
export const LoginPage = lazy(() => import('./LoginPage').then((m) => ({ default: m.LoginPage })))
export const DashboardPage = lazy(() => import('./dashboard/DashboardPage').then((m) => ({ default: m.DashboardPage })))
export const AppointmentsPage = lazy(() => import('./appointments/AppointmentsPage').then((m) => ({ default: m.AppointmentsPage })))
export const PatientsPage = lazy(() => import('./patients/PatientsPage').then((m) => ({ default: m.PatientsPage })))
export const NewPatientPage = lazy(() => import('./patients/PatientsPage').then((m) => ({ default: m.NewPatientPage })))
export const PatientProfilePage = lazy(() => import('./patients/PatientProfilePage').then((m) => ({ default: m.PatientProfilePage })))
export const ReportsPage = lazy(() => import('./reports/ReportsPage').then((m) => ({ default: m.ReportsPage })))
export const ProfilePage = lazy(() => import('./StaffPages').then((m) => ({ default: m.ProfilePage })))
export const UsersPage = lazy(() => import('./admin/UsersPage').then((m) => ({ default: m.UsersPage })))
export const DoctorsAdminPage = lazy(() => import('./admin/DoctorsAdminPage').then((m) => ({ default: m.DoctorsAdminPage })))
export const BranchesAdminPage = lazy(() => import('./admin/BranchesAdminPage').then((m) => ({ default: m.BranchesAdminPage })))
export const GalleryAdminPage = lazy(() => import('./admin/GalleryAdminPage').then((m) => ({ default: m.GalleryAdminPage })))
export const SchedulePage = lazy(() => import('./schedule/SchedulePage').then((m) => ({ default: m.SchedulePage })))
export const CounterBookingPage = lazy(() => import('./booking/CounterBookingPage').then((m) => ({ default: m.CounterBookingPage })))
export const AuditPage = lazy(() => import('./admin/AuditPage').then((m) => ({ default: m.AuditPage })))

/** Loading line while the first staff bundle downloads (before the session provider exists). */
export function StaffSuspense() {
  return (
    <Suspense fallback={<p className="p-8 text-slate-500">Loading…</p>}>
      <Outlet />
    </Suspense>
  )
}
