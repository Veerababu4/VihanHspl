import { AxeBuilder } from '@axe-core/playwright'
import type { Page } from '@playwright/test'

import { expect, login, test } from './fixtures.ts'

// Automated WCAG 2.1 AA checks (axe-core) on every main screen: contrast, labels, names,
// landmarks, ARIA use. Automated checks find roughly half of the problems; the rest is the
// manual keyboard / screen-reader list in docs/UAT-CHECKLIST.md.

async function expectAccessible(page: Page, label: string): Promise<void> {
  await page.waitForLoadState('networkidle')
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .exclude('iframe')   // Google Maps / Cloudflare content isn't ours to fix
    .analyze()
  const problems = results.violations.map(
    (v) => `${v.id} (${v.impact}): ${v.help} — ${v.nodes.slice(0, 3).map((n) => n.target.join(' ')).join(' | ')}`,
  )
  expect.soft(problems, label).toEqual([])
}

test('public pages meet WCAG 2.1 AA (automated checks)', async ({ page }) => {
  for (const path of ['/', '/doctors', '/branches/MLG', '/gallery', '/about', '/contact', '/appointment-status', '/privacy', '/book']) {
    await page.goto(path)
    await expectAccessible(page, path)
  }
  // Booking wizard, later steps
  await page.goto('/book')
  await page.getByRole('radio', { name: /Kodad/ }).check({ force: true })
  await page.getByRole('radio', { name: /Venkata Ramana/ }).check({ force: true })
  await page.getByRole('button', { name: 'Continue' }).click()
  await page.getByRole('button', { name: /\d+ free/ }).first().click()
  await expectAccessible(page, 'booking: date & time')
  await page.getByRole('button', { name: /token \d+/ }).first().click()
  await page.getByRole('button', { name: 'Continue' }).click()
  await page.getByRole('button', { name: 'Confirm booking' }).click()   // empty form: shows the errors
  await expectAccessible(page, 'booking: details with errors')
})

test('staff screens meet WCAG 2.1 AA (automated checks)', async ({ page }) => {
  await page.goto('/staff/login')
  await expectAccessible(page, 'login')
  await login(page, 'itadmin')
  for (const path of ['/staff/dashboard', '/staff/appointments', '/staff/patients', '/staff/patients/new', '/staff/reports',
    '/staff/schedule', '/staff/book', '/staff/profile', '/staff/admin/users', '/staff/admin/doctors', '/staff/admin/branches',
    '/staff/admin/gallery', '/staff/admin/audit']) {
    await page.goto(path)
    await expectAccessible(page, path)
  }
  await page.goto('/staff/admin/users')
  await page.getByRole('button', { name: 'Add user' }).click()
  await expectAccessible(page, 'dialog: new staff user')
})

test('doctor screens meet WCAG 2.1 AA (automated checks)', async ({ page }) => {
  await login(page, 'doctor006')
  await page.goto('/staff/queue')
  await expectAccessible(page, 'queue')
  const open = page.getByRole('link', { name: /^(Open|View)$/ }).first()
  if (await open.count()) {
    await open.click()
    await page.getByRole('heading', { name: 'Vitals' }).waitFor()
    await expectAccessible(page, 'visit')
    await page.getByRole('link', { name: 'Health history' }).click()
    await expectAccessible(page, 'health history')
  }
})
