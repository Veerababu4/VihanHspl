import '@testing-library/jest-dom/vitest'
import '../i18n'

import { cleanup, configure } from '@testing-library/react'
import { afterEach, vi } from 'vitest'

// jsdom has no scrolling; React Router's ScrollRestoration calls it on every navigation.
window.scrollTo = () => {}

// Staff pages are lazy-loaded; in tests each is compiled on first import, which can exceed the default 1 s wait.
configure({ asyncUtilTimeout: 5000 })

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})
