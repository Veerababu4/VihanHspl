import '@testing-library/jest-dom/vitest'
import '../i18n'

import { cleanup } from '@testing-library/react'
import { afterEach, vi } from 'vitest'

// jsdom has no scrolling; React Router's ScrollRestoration calls it on every navigation.
window.scrollTo = () => {}

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})
