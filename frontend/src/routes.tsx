import { createBrowserRouter } from 'react-router'

import { PublicLayout } from './components/PublicLayout'
import { ComingSoonPage, NotFoundPage } from './components/SimplePages'
import { HomePage } from './features/public/HomePage'

// Route map: DESIGN.md §6. Placeholders are replaced milestone by milestone.
export const router = createBrowserRouter([
  {
    element: <PublicLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'doctors', element: <ComingSoonPage /> },
      { path: 'book', element: <ComingSoonPage /> },
      { path: 'staff/login', element: <ComingSoonPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
