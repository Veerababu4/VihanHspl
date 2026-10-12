import { useEffect, useRef, type RefObject } from 'react'

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'

/**
 * Keyboard handling for a modal dialog (WCAG 2.1.2, 2.4.3): focus moves to the first form field
 * (or the dialog itself), Tab and Shift+Tab wrap around inside it, Escape closes it, and focus
 * goes back to where it was. Call it from a component that is mounted only while the dialog is open.
 */
export function useDialogFocus(ref: RefObject<HTMLElement | null>, onClose: () => void): void {
  // Pages often pass an inline arrow; a ref keeps re-renders from re-running the effect,
  // which would pull focus out of the dialog and back to the first field.
  const onCloseRef = useRef(onClose)
  useEffect(() => {
    onCloseRef.current = onClose
  })

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    const previouslyFocused = document.activeElement as HTMLElement | null
    ;(dialog.querySelector<HTMLElement>('input:not([disabled]), select:not([disabled]), textarea:not([disabled])') ?? dialog).focus()

    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onCloseRef.current()
        return
      }
      if (e.key !== 'Tab') return
      const items = [...dialog.querySelectorAll<HTMLElement>(FOCUSABLE)].filter((el) => el.checkVisibility?.() !== false)
      if (items.length === 0) {
        e.preventDefault()
        dialog.focus()
        return
      }
      const active = document.activeElement
      const outside = active === dialog || !dialog.contains(active)
      if (e.shiftKey && (outside || active === items[0])) {
        e.preventDefault()
        items[items.length - 1].focus()
      } else if (!e.shiftKey && (outside || active === items[items.length - 1])) {
        e.preventDefault()
        items[0].focus()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => {
      window.removeEventListener('keydown', onKey)
      previouslyFocused?.focus()
    }
  }, [ref])
}
