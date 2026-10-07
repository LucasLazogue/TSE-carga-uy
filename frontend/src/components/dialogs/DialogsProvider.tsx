import { useCallback, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { DialogsContext } from './dialogs.context'
import type { ConfirmOptions, DialogSize, OpenOptions } from './dialogs.context'

type Entry =
  | { id: number; kind: 'content'; options: OpenOptions }
  | { id: number; kind: 'confirm'; options: ConfirmOptions; answer: (ok: boolean) => void }

const sizes: Record<DialogSize, string> = {
  sm: 'sm:max-w-sm',
  md: 'sm:max-w-lg',
  lg: 'sm:max-w-3xl',
}

function DialogsProvider({ children }: { children: ReactNode }) {
  const [stack, setStack] = useState<Entry[]>([])
  const nextId = useRef(0)

  const remove = useCallback((id: number) => {
    setStack((current) => current.filter((e) => e.id !== id))
  }, [])

  const open = useCallback((options: OpenOptions) => {
    const id = nextId.current++
    setStack((current) => [...current, { id, kind: 'content', options }])
  }, [])

  const close = useCallback(() => {
    setStack((current) => current.slice(0, -1))
  }, [])

  const confirm = useCallback(
    (options: ConfirmOptions) =>
      new Promise<boolean>((resolve) => {
        const id = nextId.current++
        const answer = (ok: boolean) => {
          remove(id)
          resolve(ok)
        }
        setStack((current) => [...current, { id, kind: 'confirm', options, answer }])
      }),
    [remove],
  )

  const value = useMemo(() => ({ open, close, confirm }), [open, close, confirm])

  return (
    <DialogsContext.Provider value={value}>
      {children}
      {stack.map((entry) =>
        entry.kind === 'content' ? (
          <Dialog key={entry.id} open onOpenChange={(isOpen) => !isOpen && remove(entry.id)}>
            <DialogContent className={sizes[entry.options.size ?? 'md']}>
              <DialogHeader>
                <DialogTitle>{entry.options.title}</DialogTitle>
                {entry.options.description && <DialogDescription>{entry.options.description}</DialogDescription>}
              </DialogHeader>
              {entry.options.content(() => remove(entry.id))}
            </DialogContent>
          </Dialog>
        ) : (
          <AlertDialog key={entry.id} open onOpenChange={(isOpen) => !isOpen && entry.answer(false)}>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>{entry.options.title}</AlertDialogTitle>
                {entry.options.description && (
                  <AlertDialogDescription>{entry.options.description}</AlertDialogDescription>
                )}
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel onClick={() => entry.answer(false)}>Cancelar</AlertDialogCancel>
                <AlertDialogAction
                  variant={entry.options.destructive ? 'destructive' : 'default'}
                  onClick={() => entry.answer(true)}
                >
                  {entry.options.confirmText ?? 'Confirmar'}
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        ),
      )}
    </DialogsContext.Provider>
  )
}

export default DialogsProvider
