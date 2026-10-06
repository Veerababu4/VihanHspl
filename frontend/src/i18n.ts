import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

import en from './locales/en.json'

// All UI text lives in locales/*.json (DESIGN.md §19.8). Telugu (te.json) is added in Phase 2.
void i18n.use(initReactI18next).init({
  resources: { en: { translation: en } },
  lng: 'en',
  fallbackLng: 'en',
  interpolation: { escapeValue: false },
})

export default i18n
