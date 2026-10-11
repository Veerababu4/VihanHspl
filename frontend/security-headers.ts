// Security headers for the website (DESIGN.md §12.4). In production Caddy sends them
// (deploy/Caddyfile, kept identical by security-headers.test.ts); locally `npm run preview`
// sends them, so the built site can be checked against the real policy. The dev server
// doesn't: Vite's hot reload needs inline scripts.

/** What the website may load. Anything else is blocked by the browser. */
export const contentSecurityPolicy = [
  "default-src 'self'",
  // Cloudflare Turnstile ("I am human" check) loads its script and runs in an iframe.
  "script-src 'self' https://challenges.cloudflare.com",
  "style-src 'self'",
  "img-src 'self' data:",
  "font-src 'self'",
  "connect-src 'self'",
  // Turnstile, and the Google Maps embed on branch pages (branch.map_url only accepts google.com/maps).
  'frame-src https://challenges.cloudflare.com https://www.google.com https://google.com',
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'self'",
  "frame-ancestors 'none'",
].join('; ')

export const websiteSecurityHeaders: Record<string, string> = {
  'Content-Security-Policy': contentSecurityPolicy,
  'X-Content-Type-Options': 'nosniff',
  'X-Frame-Options': 'DENY',
  'Referrer-Policy': 'strict-origin-when-cross-origin',
  'Permissions-Policy': 'camera=(), microphone=(), geolocation=(), payment=(), usb=()',
  'Cross-Origin-Opener-Policy': 'same-origin',
}
