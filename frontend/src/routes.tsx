import { createBrowserRouter, type RouteObject } from 'react-router'

import { PublicLayout } from './components/PublicLayout'
import { ComingSoonPage, NotFoundPage } from './components/SimplePages'
import { BranchPage } from './features/public/BranchPage'
import { DoctorProfilePage } from './features/public/DoctorProfilePage'
import { DoctorsPage } from './features/public/DoctorsPage'
import { GalleryPage } from './features/public/GalleryPage'
import { HomePage } from './features/public/HomePage'
import { AboutPage, ContactPage } from './features/public/InfoPages'

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
      { path: 'book', element: <ComingSoonPage /> },          // M5
      { path: 'staff/login', element: <ComingSoonPage /> },   // M3
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]

export const router = createBrowserRouter(routes)
