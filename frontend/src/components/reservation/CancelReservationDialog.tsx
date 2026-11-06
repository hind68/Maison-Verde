import { AlertTriangle, LoaderCircle, X } from 'lucide-react'
import { useState } from 'react'
import { ApiError, hotelApi } from '../../lib/api'
import type { Reservation } from '../../types/api'
import { Button } from '../ui/Button'
import { ErrorNotice } from '../ui/Feedback'

interface Props {
  reservationId: number
  onCancelled: (reservation: Reservation) => void
  compact?: boolean
}

export function CancelReservationDialog({ reservationId, onCancelled, compact = false }: Props) {
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function cancel() {
    if (loading) return
    setLoading(true)
    setError(null)
    try {
      const reservation = await hotelApi.cancelReservation(reservationId)
      setOpen(false)
      onCancelled(reservation)
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'L’annulation n’a pas pu être enregistrée.')
    } finally {
      setLoading(false)
    }
  }

  return <>
    <Button type="button" variant="ghost" className={compact ? '!min-h-9 !px-3 text-red-900' : 'text-red-900'} onClick={() => setOpen(true)}>Annuler</Button>
    {open && <div className="fixed inset-0 z-[80] grid place-items-center bg-forest/55 p-4 backdrop-blur-sm" role="presentation" onMouseDown={(event) => { if (event.currentTarget === event.target && !loading) setOpen(false) }}>
      <section className="w-full max-w-lg rounded-[2rem] bg-ivory p-6 shadow-2xl sm:p-8" role="dialog" aria-modal="true" aria-labelledby={`cancel-title-${reservationId}`}>
        <div className="flex items-start justify-between gap-4">
          <span className="grid size-12 place-items-center rounded-full bg-red-900/8 text-red-900"><AlertTriangle className="size-5" /></span>
          <button type="button" className="grid size-9 place-items-center rounded-full text-forest/45 transition hover:bg-forest/5 hover:text-forest" onClick={() => setOpen(false)} disabled={loading} aria-label="Fermer"><X className="size-5" /></button>
        </div>
        <h2 id={`cancel-title-${reservationId}`} className="mt-6 font-editorial text-3xl">Annuler la réservation #{reservationId} ?</h2>
        <p className="mt-3 text-sm leading-6 text-forest/55">La réservation sera conservée dans l’historique. La chambre redeviendra disponible pour cette période.</p>
        {error && <div className="mt-5"><ErrorNotice message={error} /></div>}
        <div className="mt-7 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <Button type="button" variant="secondary" onClick={() => setOpen(false)} disabled={loading}>Conserver</Button>
          <Button type="button" className="bg-red-900 hover:bg-red-950" onClick={cancel} disabled={loading}>{loading ? <><LoaderCircle className="size-4 animate-spin" /> Annulation…</> : 'Confirmer l’annulation'}</Button>
        </div>
      </section>
    </div>}
  </>
}
