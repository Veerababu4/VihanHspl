import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'

import { Button, Modal } from './ui'

function Page({ withField = true, onClose = () => {} }: { withField?: boolean; onClose?: () => void }) {
  const [open, setOpen] = useState(false)
  const [renders, setRenders] = useState(0)
  return (
    <>
      <Button onClick={() => setOpen(true)}>Open</Button>
      <Button>Behind</Button>
      {open && (
        // Inline arrow on purpose: re-renders must not move focus.
        <Modal title="Edit" onClose={() => { onClose(); setOpen(false) }}>
          {withField && <input aria-label="Name" />}
          <Button onClick={() => setRenders(renders + 1)}>Re-render {renders}</Button>
          <Button>Save</Button>
        </Modal>
      )}
    </>
  )
}

describe('Modal', () => {
  it('keeps Tab and Shift+Tab inside the dialog', async () => {
    const user = userEvent.setup()
    render(<Page />)
    await user.click(screen.getByRole('button', { name: 'Open' }))
    expect(screen.getByRole('textbox', { name: 'Name' })).toHaveFocus()

    await user.tab()
    await user.tab()
    expect(screen.getByRole('button', { name: 'Save' })).toHaveFocus()
    await user.tab()   // wraps to the close ✕, the first control in the dialog
    expect(screen.getByRole('button', { name: 'Close' })).toHaveFocus()
    await user.tab({ shift: true })
    expect(screen.getByRole('button', { name: 'Save' })).toHaveFocus()
  })

  it('focuses the dialog when it has no fields, and keeps focus across re-renders', async () => {
    const user = userEvent.setup()
    render(<Page withField={false} />)
    await user.click(screen.getByRole('button', { name: 'Open' }))
    expect(screen.getByRole('dialog', { name: 'Edit' })).toHaveFocus()

    await user.click(screen.getByRole('button', { name: 'Re-render 0' }))
    expect(screen.getByRole('button', { name: 'Re-render 1' })).toHaveFocus()
  })

  it('closes on Escape and returns focus to the opener', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()
    render(<Page onClose={onClose} />)
    await user.click(screen.getByRole('button', { name: 'Open' }))
    await user.keyboard('{Escape}')
    expect(onClose).toHaveBeenCalledOnce()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Open' })).toHaveFocus()
  })
})
