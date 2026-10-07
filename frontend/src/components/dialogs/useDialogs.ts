import { useContext } from 'react'
import { DialogsContext } from './dialogs.context'

export function useDialogs() {
  const context = useContext(DialogsContext)
  if (!context) {
    throw new Error('useDialogs must be used inside DialogsProvider')
  }
  return context
}
