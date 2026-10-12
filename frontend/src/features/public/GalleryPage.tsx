import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'

import { useBranches, useGallery, type GalleryImage } from '../../api/public'
import { Seo } from '../../components/Seo'
import { useDialogFocus } from '../../lib/dialogFocus'

/** Hospital photos, filterable by branch (FR-P5). */
export function GalleryPage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const branch = params.get('branch') ?? undefined
  const { data: branches } = useBranches()
  const { data: images, isPending, isError } = useGallery(branch)

  const tabs = [{ code: undefined, label: t('gallery.all') }, ...(branches ?? []).map((b) => ({ code: b.code, label: b.city }))]

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <Seo title={t('seo.galleryTitle')} />
      <h1 className="mb-6 text-3xl font-bold text-slate-900">{t('gallery.title')}</h1>
      <div role="group" aria-label={t('doctors.filterBranch')} className="mb-6 flex flex-wrap gap-2">
        {tabs.map((tab) => (
          <button
            key={tab.code ?? 'all'}
            type="button"
            aria-pressed={branch === tab.code}
            onClick={() => setParams(tab.code ? { branch: tab.code } : {}, { replace: true })}
            className={`rounded-full border px-4 py-1.5 text-sm font-medium ${
              branch === tab.code ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-300 bg-white hover:border-brand-500'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>
      {isPending && <p className="text-slate-500">{t('common.loading')}</p>}
      {isError && <p role="alert" className="text-emergency-600">{t('doctors.error')}</p>}
      {images && (images.length === 0 ? <p className="text-slate-600">{t('gallery.empty')}</p> : <GalleryGrid images={images} />)}
    </div>
  )
}

/** Thumbnail grid; clicking opens the full image in a lightbox. Also used on branch pages. */
export function GalleryGrid({ images }: { images: GalleryImage[] }) {
  const { t } = useTranslation()
  const [openIndex, setOpenIndex] = useState<number | null>(null)

  return (
    <>
      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {images.map((img, i) => (
          <li key={img.id}>
            <button
              type="button"
              onClick={() => setOpenIndex(i)}
              aria-label={t('gallery.open', { title: img.title ?? '' })}
              className="group block w-full overflow-hidden rounded-lg bg-slate-200 text-left"
            >
              <img
                src={img.thumbUrl}
                alt={img.title ?? ''}
                loading="lazy"
                className="aspect-[4/3] w-full object-cover transition group-hover:scale-105"
              />
            </button>
            {img.title && <p className="mt-1 text-sm text-slate-700">{img.title}</p>}
          </li>
        ))}
      </ul>

      {openIndex !== null && (
        <Lightbox images={images} index={openIndex} onIndex={setOpenIndex} onClose={() => setOpenIndex(null)} />
      )}
    </>
  )
}

/** Full-size image; arrow keys step through the images. */
function Lightbox({
  images,
  index,
  onIndex,
  onClose,
}: {
  images: GalleryImage[]
  index: number
  onIndex: (index: number) => void
  onClose: () => void
}) {
  const { t } = useTranslation()
  const ref = useRef<HTMLDivElement>(null)
  const open = images[index]
  useDialogFocus(ref, onClose)

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'ArrowRight') onIndex((index + 1) % images.length)
      if (e.key === 'ArrowLeft') onIndex((index - 1 + images.length) % images.length)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [index, images.length, onIndex])

  return (
    <div
      ref={ref}
      role="dialog"
      aria-modal="true"
      aria-label={open.title ?? t('gallery.title')}
      tabIndex={-1}
      className="fixed inset-0 z-30 flex items-center justify-center bg-black/80 p-4 focus:outline-none"
      onClick={onClose}
    >
      <figure className="max-h-full max-w-5xl" onClick={(e) => e.stopPropagation()}>
        <img src={open.imageUrl} alt={open.title ?? ''} className="max-h-[80vh] w-auto rounded-lg" />
        <figcaption className="mt-2 flex items-center justify-between gap-4 text-white">
          <span>
            {open.title}
            {open.caption && <span className="text-white/70"> — {open.caption}</span>}
          </span>
          <button type="button" onClick={onClose} className="rounded-md bg-white/20 px-3 py-1 hover:bg-white/30">
            {t('gallery.close')}
          </button>
        </figcaption>
      </figure>
    </div>
  )
}
