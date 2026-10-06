import { useState } from 'react'
import { useTranslation } from 'react-i18next'

import {
  useAdminBranches,
  useAdminGallery,
  useDeleteGalleryImage,
  useUpdateGalleryImage,
  useUploadGalleryImage,
  type AdminGalleryImage,
  type GalleryInput,
} from '../../../api/admin'
import { ApiError } from '../../../api/client'
import { Seo } from '../../../components/Seo'
import { Alert, Badge, Button, Field, inputClass, Modal, PageHeader } from '../../../components/ui'

/** Gallery management (FR-AD4). */
export function GalleryAdminPage() {
  const { t } = useTranslation()
  const { data: images, isError } = useAdminGallery()
  const { data: branches } = useAdminBranches()
  const remove = useDeleteGalleryImage()
  const [editing, setEditing] = useState<AdminGalleryImage | 'new' | null>(null)
  const branchName = (id: number | null) => (id ? (branches?.find((b) => b.id === id)?.city ?? '') : t('staff.gallery.network'))

  const onDelete = async (img: AdminGalleryImage) => {
    if (window.confirm(t('staff.gallery.deleteConfirm'))) await remove.mutateAsync(img.id)
  }

  return (
    <>
      <Seo title={t('staff.gallery.title')} />
      <PageHeader title={t('staff.gallery.title')} actions={<Button onClick={() => setEditing('new')}>{t('staff.gallery.upload')}</Button>} />
      {isError && <Alert>{t('staff.common.loadError')}</Alert>}
      {images?.length === 0 && <p className="text-slate-600">{t('staff.gallery.empty')}</p>}
      <ul className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
        {images?.map((img) => (
          <li key={img.id} className="overflow-hidden rounded-xl border border-slate-200 bg-white">
            <img src={img.thumbUrl} alt={img.title ?? ''} className="aspect-[4/3] w-full object-cover" />
            <div className="p-3 text-sm">
              <p className="font-medium">
                {img.title ?? '—'} {!img.active && <Badge>{t('staff.common.inactive')}</Badge>}
              </p>
              <p className="text-slate-500">
                {branchName(img.branchId)} · #{img.sortOrder}
              </p>
              <div className="mt-2 flex gap-3">
                <Button variant="link" onClick={() => setEditing(img)}>
                  {t('staff.common.edit')}
                </Button>
                <Button variant="link" className="text-emergency-600" onClick={() => onDelete(img)}>
                  {t('staff.gallery.delete')}
                </Button>
              </div>
            </div>
          </li>
        ))}
      </ul>
      {editing && <GalleryForm image={editing === 'new' ? null : editing} onClose={() => setEditing(null)} />}
    </>
  )
}

function GalleryForm({ image, onClose }: { image: AdminGalleryImage | null; onClose: () => void }) {
  const { t } = useTranslation()
  const { data: branches } = useAdminBranches()
  const upload = useUploadGalleryImage()
  const update = useUpdateGalleryImage()
  const [file, setFile] = useState<File | null>(null)
  const [values, setValues] = useState({
    branchId: image?.branchId ? String(image.branchId) : '',
    title: image?.title ?? '',
    caption: image?.caption ?? '',
    sortOrder: String(image?.sortOrder ?? 0),
    active: image?.active ?? true,
  })
  const [error, setError] = useState<string | null>(null)
  const busy = upload.isPending || update.isPending

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    const input: GalleryInput = {
      branchId: values.branchId ? Number(values.branchId) : null,
      title: values.title.trim() || null,
      caption: values.caption.trim() || null,
      sortOrder: Number(values.sortOrder) || 0,
      active: values.active,
    }
    try {
      if (image) {
        await update.mutateAsync({ id: image.id, input })
      } else {
        if (!file) {
          setError(t('staff.gallery.file'))
          return
        }
        await upload.mutateAsync({ file, input })
      }
      onClose()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : t('staff.common.loadError'))
    }
  }

  return (
    <Modal title={image ? t('staff.gallery.editTitle') : t('staff.gallery.uploadTitle')} onClose={onClose}>
      <form onSubmit={onSubmit} noValidate className="space-y-4">
        {error && <Alert>{error}</Alert>}
        {image ? (
          <img src={image.thumbUrl} alt="" className="h-32 rounded-lg object-cover" />
        ) : (
          <Field label={t('staff.gallery.file')} hint={t('staff.doctors.photoHint')}>
            {(a) => (
              <input
                {...a}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                className="block text-sm"
              />
            )}
          </Field>
        )}
        <Field label={t('staff.gallery.branch')}>
          {(a) => (
            <select {...a} value={values.branchId} onChange={(e) => setValues({ ...values, branchId: e.target.value })} className={inputClass}>
              <option value="">{t('staff.gallery.network')}</option>
              {branches?.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.city}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label={t('staff.gallery.imageTitle')}>
          {(a) => <input {...a} maxLength={120} value={values.title} onChange={(e) => setValues({ ...values, title: e.target.value })} className={inputClass} />}
        </Field>
        <Field label={t('staff.gallery.caption')}>
          {(a) => <input {...a} maxLength={500} value={values.caption} onChange={(e) => setValues({ ...values, caption: e.target.value })} className={inputClass} />}
        </Field>
        <Field label={t('staff.gallery.sortOrder')}>
          {(a) => (
            <input {...a} type="number" min={0} value={values.sortOrder} onChange={(e) => setValues({ ...values, sortOrder: e.target.value })} className={`${inputClass} max-w-32`} />
          )}
        </Field>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={values.active} onChange={(e) => setValues({ ...values, active: e.target.checked })} className="h-4 w-4" />
          {t('staff.common.active')}
        </label>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            {t('staff.common.cancel')}
          </Button>
          <Button type="submit" disabled={busy}>
            {busy ? t('staff.common.saving') : t('staff.common.save')}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
