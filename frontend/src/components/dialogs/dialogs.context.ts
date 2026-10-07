import { createContext } from 'react'
import type { ReactNode } from 'react'

export type DialogSize = 'sm' | 'md' | 'lg'

export type OpenOptions = {
  title: string
  description?: string
  size?: DialogSize
  content: (close: () => void) => ReactNode
}

export type ConfirmOptions = {
  title: string
  description?: string
  confirmText?: string
  destructive?: boolean
}

export type Dialogs = {
  open: (options: OpenOptions) => void
  close: () => void
  confirm: (options: ConfirmOptions) => Promise<boolean>
}

export const DialogsContext = createContext<Dialogs | null>(null)
