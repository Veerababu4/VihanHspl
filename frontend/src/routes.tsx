import type { ReactNode } from 'react'
import { createBrowserRouter, Navigate, type RouteObject } from 'react-router'

import { PublicLayout } from './components/PublicLayout'
import { ComingSoonPage, NotFoundPage } from './components/SimplePages'
import { BranchPage } from './features/public/BranchPage'
import { DoctorProfilePage } from './features/public/DoctorProfilePage'
import { DoctorsPage } from './features/public/DoctorsPage'
import { GalleryPage } from './features/public/GalleryPage'
import { HomePage } from './features/public/HomePage'
import { AboutPage, ContactPage } from './features/public/InfoPages'
import {
  AuditPage,
  BranchesAdminPage,
  DashboardPage,
  DoctorsAdminPage,
  GalleryAdminPage,
  LoginPage,
  ProfilePage,
  RequireRole,
  SchedulePage,
  StaffLayout,
  StaffRoot,
  StaffSuspense,
  UsersPage,
} from './features/staff/lazyPages'

const itAdmin = (element: ReactNode) => <RequireRole roles={['IT_ADMIN']}>{element}</RequireRole>

// Route map: DESIGN.md §6. Placeholders are replaced milestone by milestone.
export const routes: RouteObject[] = [
  {
    element: <PublicLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'doctors', element: <DoctorsPage /> },
      { path: 'doctors/:id', element: <DoctorProfilePage /> },
      { path: 'branches/:code', element: <BranchPage /> },
      { path: 'gallery', element: <GalleryPage /> },
      { path: 'about', element: <AboutPage /> },
      { path: 'contact', element: <ContactPage /> },
      { path: 'book', element: <ComingSoonPage /> }, // M5
      { path: '*', element: <NotFoundPage /> },
    ],
  },
  {
    path: 'staff',
    element: <StaffSuspense />,
    children: [
      {
        element: <StaffRoot />,
        children: [
          { path: 'login', element: <LoginPage /> },
          {
            element: <StaffLayout />,
            children: [
              { index: true, element: <Navigate to="dashboard" replace /> },
              { path: 'dashboard', element: <DashboardPage /> },
              { path: 'profile', element: <ProfilePage /> },
              { path: 'schedule', element: <RequireRole roles={['DOCTOR', 'IT_ADMIN']}><SchedulePage /></RequireRole> },
              { path: 'admin/users', element: itAdmin(<UsersPage />) },
              { path: 'admin/doctors', element: itAdmin(<DoctorsAdminPage />) },
              { path: 'admin/branches', element: itAdmin(<BranchesAdminPage />) },
              { path: 'admin/gallery', element: itAdmin(<GalleryAdminPage />) },
              { path: 'admin/audit', element: itAdmin(<AuditPage />) },
              { path: '*', element: <NotFoundPage /> },
            ],
          },
        ],
      },
    ],
  },
]

export const router = createBrowserRouter(routes)
