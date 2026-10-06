import type { Branch, DoctorDetail } from '../api/public'

// schema.org structured data so search engines understand the hospital and its doctors.

const HOSPITAL_NAME = 'Vihan Multi Speciality Hospital'

function branchLd(b: Branch) {
  return {
    '@type': 'Hospital',
    name: b.name,
    telephone: b.phone ?? undefined,
    address: {
      '@type': 'PostalAddress',
      streetAddress: b.address,
      addressLocality: b.city,
      addressRegion: 'Telangana',
      postalCode: b.pincode ?? undefined,
      addressCountry: 'IN',
    },
    openingHours: b.openingHours ?? undefined,
  }
}

export function hospitalNetworkLd(branches: Branch[]) {
  return {
    '@context': 'https://schema.org',
    '@type': 'MedicalOrganization',
    name: HOSPITAL_NAME,
    subOrganization: branches.map(branchLd),
  }
}

export function branchPageLd(b: Branch) {
  return { '@context': 'https://schema.org', ...branchLd(b), parentOrganization: { '@type': 'MedicalOrganization', name: HOSPITAL_NAME } }
}

export function physicianLd(d: DoctorDetail) {
  return {
    '@context': 'https://schema.org',
    '@type': 'Physician',
    name: d.fullName,
    medicalSpecialty: d.speciality.name,
    description: d.qualification,
    image: d.photoUrl ?? undefined,
    hospitalAffiliation: d.branches.map((b) => ({ '@type': 'Hospital', name: `${HOSPITAL_NAME} - ${b.city}` })),
  }
}
