import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'

import {
  useAdminBranches,
  useAdminSpecialities,
  useBranchImage,
  useSaveBranch,
  useSaveSpeciality,
  type AdminBranch,
  type AdminSpeciality,
} from '../../../api/admin'
import { ApiError } from '../../../api/client'
import { Seo } from '../../../components/Seo'
import { Alert, Badge, Button, Field, inputClass, Modal, PageHeader } from '../../../components/ui'
import { applyServerErrors } from '../../../lib/forms'

/** Branches and specialities (FR-AD3). Holidays are added with scheduling in M4. */
export function BranchesAdminPage() {
  const { t } = useTranslation()
  const { data: branches, isError } = useAdminBranches()
  const { data: specialities } = useAdminSpecialities()
  const [branch, setBranch] = useState<AdminBranch | 'new' | null>(null)
  const [speciality, setSpeciality] = useState<AdminSpeciality | 'new' | null>(null)

  return (
    <>
      <Seo title={t('staff.branches.title')} />
      <PageHeader title={t('staff.branches.title')} />
      {isError && <Alert>{t('staff.common.loadError')}</Alert>}

      <section className="mb-10">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-semibold">{t('staff.branches.branchesTitle')}</h2>
          <Button variant="secondary" onClick={() => setBranch('new')}>
            {t('staff.branches.addBranch')}
          </Button>
        </div>
        <ul className="grid gap-4 md:grid-cols-3">
          {branches?.map((b) => (
            <li key={b.id} className="overflow-hidden rounded-xl border border-slate-200 bg-white">
              {b.imageUrl && <img src={b.imageUrl} alt="" className="h-28 w-full object-cover" />}
              <div className="p-4">
                <p className="font-semibold">
                  {b.city} <span className="font-mono text-xs text-slate-500">{b.code}</span>{' '}
                  {!b.active && <Badge>{t('staff.common.inactive')}</Badge>}
                </p>
                <p className="text-sm text-slate-600">{b.address}</p>
                <p className="text-sm text-slate-600">
                  {b.phone} · {b.emergencyPhone}
                </p>
                <Button variant="link" className="mt-2" onClick={() => setBranch(b)} aria-label={`${t('staff.common.edit')} ${b.city}`}>
                  {t('staff.common.edit')}
                </Button>
              </div>
            </li>
          ))}
        </ul>
      </section>

      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-semibold">{t('staff.branches.specialitiesTitle')}</h2>
          <Button variant="secondary" onClick={() => setSpeciality('new')}>
            {t('staff.branches.addSpeciality')}
          </Button>
        </div>
        <ul className="divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white">
          {specialities?.map((s) => (
            <li key={s.id} className="flex items-center justify-between gap-4 px-4 py-2.5">
              <div>
                <p className="font-medium">
                  {s.name} {!s.active && <Badge>{t('staff.common.inactive')}</Badge>}
                </p>
                {s.description && <p className="text-sm text-slate-500">{s.description}</p>}
              </div>
              <Button variant="link" onClick={() => setSpeciality(s)} aria-label={`${t('staff.common.edit')} ${s.name}`}>
                {t('staff.common.edit')}
              </Button>
            </li>
          ))}
        </ul>
      </section>

      {branch && <BranchForm branch={branch === 'new' ? null : branch} onClose={() => setBranch(null)} />}
      {speciality && <SpecialityForm speciality={speciality === 'new' ? null : speciality} onClose={() => setSpeciality(null)} />}
    </>
  )
}

const optionalPhone = z.string().trim().regex(/^([0-9+\- ]{6,15})?$/, 'Invalid phone number')
const branchSchema = z.object({
  code: z.string().trim().regex(/^[A-Z]{2,10}$/, '2-10 capital letters'),
  name: z.string().trim().min(1, 'Required').max(120),
  address: z.string().trim().min(1, 'Required').max(400),
  city: z.string().trim().min(1, 'Required').max(60),
  district: z.string().trim().max(60),
  pincode: z.string().trim().regex(/^(\d{6})?$/, 'Must be 6 digits'),
  phone: optionalPhone,
  emergencyPhone: optionalPhone,
  email: z.union([z.literal(''), z.string().trim().email()]),
  mapUrl: z.string().trim().regex(/^(https:\/\/(www\.)?google\.com\/maps\S*)?$/, 'Must be a Google Maps embed link'),
  openingHours: z.string().trim().max(200),
  active: z.boolean(),
})
type BranchValues = z.infer<typeof branchSchema>

function BranchForm({ branch, onClose }: { branch: AdminBranch | null; onClose: () => void }) {
  const { t } = useTranslation()
  const save = useSaveBranch()
  const image = useBranchImage()
  const [message, setMessage] = useState<string | null>(null)
  const [imageUrl, setImageUrl] = useState(branch?.imageUrl ?? null)
  const form = useForm<BranchValues>({
    resolver: zodResolver(branchSchema),
    defaultValues: {
      code: branch?.code ?? '',
      name: branch?.name ?? 'Vihan Multi Speciality Hospital - ',
      address: branch?.address ?? '',
      city: branch?.city ?? '',
      district: branch?.district ?? '',
      pincode: branch?.pincode ?? '',
      phone: branch?.phone ?? '',
      emergencyPhone: branch?.emergencyPhone ?? '',
      email: branch?.email ?? '',
      mapUrl: branch?.mapUrl ?? '',
      openingHours: branch?.openingHours ?? '',
      active: branch?.active ?? true,
    },
  })
  const errors = form.formState.errors
  const blankToNull = (s: string) => (s === '' ? null : s)

  const onSubmit = form.handleSubmit(async (v) => {
    setMessage(null)
    try {
      await save.mutateAsync({
        id: branch?.id,
        input: {
          ...v,
          district: blankToNull(v.district),
          pincode: blankToNull(v.pincode),
          phone: blankToNull(v.phone),
          emergencyPhone: blankToNull(v.emergencyPhone),
          email: blankToNull(v.email),
          mapUrl: blankToNull(v.mapUrl),
          openingHours: blankToNull(v.openingHours),
        },
      })
      onClose()
    } catch (e) {
      setMessage(applyServerErrors(e, form.setError))
    }
  })

  const uploadImage = async (file: File) => {
    if (!branch) return
    setMessage(null)
    try {
      setImageUrl((await image.mutateAsync({ id: branch.id, file })).imageUrl)
    } catch (e) {
      setMessage(e instanceof ApiError ? e.message : t('staff.common.loadError'))
    }
  }

  const text = (name: keyof BranchValues, label: string, hint?: string) => (
    <Field label={label} error={errors[name]?.message} hint={hint}>
      {(a) => <input {...a} {...form.register(name)} className={inputClass} />}
    </Field>
  )

  return (
    <Modal title={branch ? t('staff.branches.editBranch') : t('staff.branches.newBranch')} onClose={onClose} wide>
      {branch && (
        <div className="mb-5 flex items-center gap-4 rounded-lg bg-slate-50 p-3">
          {imageUrl && <img src={imageUrl} alt="" className="h-16 w-24 rounded object-cover" />}
          <label className="cursor-pointer rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium hover:bg-slate-50">
            {image.isPending ? t('staff.common.saving') : t('staff.branches.uploadImage')}
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              className="sr-only"
              onChange={(e) => {
                const file = e.target.files?.[0]
                e.target.value = ''
                if (file) void uploadImage(file)
              }}
            />
          </label>
        </div>
      )}
      <form onSubmit={onSubmit} noValidate className="grid gap-4 sm:grid-cols-2">
        {message && (
          <div className="sm:col-span-2">
            <Alert>{message}</Alert>
          </div>
        )}
        {branch ? (
          <p className="text-sm sm:col-span-2">
            {t('staff.branches.code')}: <span className="font-mono">{branch.code}</span>
          </p>
        ) : (
          text('code', t('staff.branches.code'), t('staff.branches.codeHint'))
        )}
        {text('name', t('staff.branches.name'))}
        {text('address', t('staff.branches.address'))}
        {text('city', t('staff.branches.city'))}
        {text('district', t('staff.branches.district'))}
        {text('pincode', t('staff.branches.pincode'))}
        {text('phone', t('staff.branches.phone'))}
        {text('emergencyPhone', t('staff.branches.emergencyPhone'))}
        {text('email', t('staff.branches.email'))}
        {text('openingHours', t('staff.branches.openingHours'))}
        <div className="sm:col-span-2">{text('mapUrl', t('staff.branches.mapUrl'), t('staff.branches.mapHint'))}</div>
        <label className="flex items-center gap-2 text-sm sm:col-span-2">
          <input type="checkbox" {...form.register('active')} className="h-4 w-4" />
          {t('staff.common.active')}
        </label>
        <div className="flex justify-end gap-2 sm:col-span-2">
          <Button variant="secondary" onClick={onClose}>
            {t('staff.common.cancel')}
          </Button>
          <Button type="submit" disabled={form.formState.isSubmitting}>
            {form.formState.isSubmitting ? t('staff.common.saving') : t('staff.common.save')}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

const specialitySchema = z.object({
  name: z.string().trim().min(1, 'Required').max(80),
  description: z.string().trim().max(1000),
  active: z.boolean(),
})
type SpecialityValues = z.infer<typeof specialitySchema>

function SpecialityForm({ speciality, onClose }: { speciality: AdminSpeciality | null; onClose: () => void }) {
  const { t } = useTranslation()
  const save = useSaveSpeciality()
  const [message, setMessage] = useState<string | null>(null)
  const form = useForm<SpecialityValues>({
    resolver: zodResolver(specialitySchema),
    defaultValues: { name: speciality?.name ?? '', description: speciality?.description ?? '', active: speciality?.active ?? true },
  })
  const onSubmit = form.handleSubmit(async (v) => {
    setMessage(null)
    try {
      await save.mutateAsync({ id: speciality?.id, input: { ...v, description: v.description || null } })
      onClose()
    } catch (e) {
      setMessage(applyServerErrors(e, form.setError))
    }
  })
  return (
    <Modal title={speciality ? t('staff.branches.editSpeciality') : t('staff.branches.newSpeciality')} onClose={onClose}>
      <form onSubmit={onSubmit} noValidate className="space-y-4">
        {message && <Alert>{message}</Alert>}
        <Field label={t('staff.branches.name')} error={form.formState.errors.name?.message}>
          {(a) => <input {...a} {...form.register('name')} className={inputClass} />}
        </Field>
        <Field label={t('staff.branches.description')} error={form.formState.errors.description?.message}>
          {(a) => <textarea {...a} {...form.register('description')} rows={3} className={inputClass} />}
        </Field>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" {...form.register('active')} className="h-4 w-4" />
          {t('staff.common.active')}
        </label>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            {t('staff.common.cancel')}
          </Button>
          <Button type="submit" disabled={form.formState.isSubmitting}>
            {t('staff.common.save')}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
