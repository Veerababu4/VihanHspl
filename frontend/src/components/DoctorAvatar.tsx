import { initials } from '../lib/format'

const sizes = {
  md: 'h-16 w-16 text-lg',
  lg: 'h-28 w-28 text-3xl',
}

/** Doctor photo, or initials when no photo has been uploaded (no stock photos of real people). */
export function DoctorAvatar({
  name,
  photoUrl,
  size = 'md',
}: {
  name: string
  photoUrl: string | null
  size?: keyof typeof sizes
}) {
  if (photoUrl) {
    return <img src={photoUrl} alt={name} className={`${sizes[size]} shrink-0 rounded-full object-cover`} />
  }
  return (
    <span
      aria-hidden="true"
      className={`${sizes[size]} flex shrink-0 items-center justify-center rounded-full bg-brand-100 font-semibold text-brand-700`}
    >
      {initials(name)}
    </span>
  )
}
