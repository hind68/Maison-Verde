import { AlertCircle, LoaderCircle, SearchX } from 'lucide-react'
import type { ReactNode } from 'react'

export function LoadingState({ label = 'Chargement en cours…' }: { label?: string }) {
  return (
    <div className="flex min-h-44 items-center justify-center gap-3 rounded-3xl border border-forest/8 bg-white/40 text-sm text-forest/60" role="status">
      <LoaderCircle className="size-5 animate-spin text-gold" aria-hidden="true" />
      {label}
    </div>
  )
}

export function EmptyState({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="grid min-h-48 place-items-center rounded-3xl border border-dashed border-forest/18 bg-white/35 px-6 text-center">
      <div>
        <SearchX className="mx-auto mb-4 size-7 text-gold" aria-hidden="true" />
        <h3 className="font-editorial text-2xl text-forest">{title}</h3>
        <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-forest/55">{children}</p>
      </div>
    </div>
  )
}

export function ErrorNotice({ message, action }: { message: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col gap-4 rounded-2xl border border-red-900/15 bg-red-50/70 p-4 text-sm text-red-950 sm:flex-row sm:items-center sm:justify-between" role="alert">
      <span className="flex items-start gap-3">
        <AlertCircle className="mt-0.5 size-5 shrink-0" aria-hidden="true" />
        {message}
      </span>
      {action}
    </div>
  )
}
