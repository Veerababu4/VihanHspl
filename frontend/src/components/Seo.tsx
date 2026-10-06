import { useTranslation } from 'react-i18next'

interface SeoProps {
  /** Page title; the hospital name is appended unless `fullTitle` is set. */
  title: string
  description?: string
  fullTitle?: boolean
  /** schema.org data (Hospital, Physician…) for search engines. */
  jsonLd?: object
}

/**
 * Per-page title, description and structured data (DESIGN.md §19.8 SEO).
 * React 19 moves <title> and <meta> into the document head by itself.
 */
export function Seo({ title, description, fullTitle, jsonLd }: SeoProps) {
  const { t } = useTranslation()
  const pageTitle = fullTitle ? title : `${title}${t('seo.titleSuffix')}`
  return (
    <>
      <title>{pageTitle}</title>
      {description && <meta name="description" content={description} />}
      <meta property="og:title" content={pageTitle} />
      {description && <meta property="og:description" content={description} />}
      {jsonLd && (
        <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />
      )}
    </>
  )
}
