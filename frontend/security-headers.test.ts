import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

import { contentSecurityPolicy, websiteSecurityHeaders } from './security-headers.ts'

const caddyfile = readFileSync(resolve(process.cwd(), '../deploy/Caddyfile'), 'utf8')   // tests run in frontend/

/** The value of `Name "value"` inside the Caddyfile's website header block. */
function caddyHeader(name: string): string | undefined {
  return new RegExp(`^\\s*${name} "([^"]*)"`, 'm').exec(caddyfile)?.[1]
}

describe('website security headers', () => {
  it('production (Caddy) sends the same headers as npm run preview', () => {
    for (const [name, value] of Object.entries(websiteSecurityHeaders)) {
      const expected = name === 'Content-Security-Policy' ? `${value}; upgrade-insecure-requests` : value
      expect(caddyHeader(name), name).toBe(expected)
    }
  })

  it('allows only what the site needs', () => {
    expect(contentSecurityPolicy).toContain("script-src 'self' https://challenges.cloudflare.com")
    expect(contentSecurityPolicy).toContain("frame-ancestors 'none'")
    expect(contentSecurityPolicy).not.toContain('unsafe-inline')
    expect(contentSecurityPolicy).not.toContain('unsafe-eval')
  })
})
