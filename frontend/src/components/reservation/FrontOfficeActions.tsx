import { CalendarPlus, DoorOpen, LogOut, LoaderCircle, X } from 'lucide-react'
import { useState } from 'react'
import { ApiError, hotelApi } from '../../lib/api'
import type { Reservation } from '../../types/api'
import { Button } from '../ui/Button'
import { ErrorNotice } from '../ui/Feedback'
import { Field, Input } from '../ui/Field'

type Action = 'check-in' | 'check-out' | 'extend'

function nextDay(value: string): string {
  const date = new Date(`${value}T12:00:00`)
  date.setDate(date.getDate() + 1)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

interface Props {
  reservation: Reservation
  onUpdated: (reservation: Reservation) => void
  compact?: boolean
}

export function FrontOfficeActions({ reservation, onUpdated, compact = false }: Props) {
  const [action, setAction] = useState<Action | null>(null)
  const [newCheckOut, setNewCheckOut] = useState(nextDay(reservation.checkOut))
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const canExtend = ['confirmée', 'en_cours'].includes(reservation.status)

  async function confirm() {
    if (!action || loading) return
    setLoading(true)
    setError(null)
    try {
      const updated = action === 'check-in'
        ? await hotelApi.checkInReservation(reservation.id)
        : action === 'check-out'
          ? await hotelApi.checkOutReservation(reservation.id)
          : await hotelApi.extendReservation(reservation.id, { checkOut: newCheckOut })
      setAction(null)
      setNotice(action === 'check-in' ? 'Arrivée enregistrée.' : action === 'check-out' ? 'Départ enregistré.' : 'Séjour prolongé.')
      onUpdated(updated)
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'L’opération Front Office a échoué.')
    } finally {
      setLoading(false)
    }
  }

  const buttonClass = compact ? '!min-h-9 !px-3' : ''
  return <>
    <div className={`flex flex-wrap items-center gap-2 ${compact ? '' : 'w-full sm:w-auto'}`}>
      {reservation.status === 'confirmée' && <Button type="button" className={buttonClass} onClick={() => { setAction('check-in'); setError(null) }}><DoorOpen className="size-4" /> {compact ? 'Arrivée' : 'Enregistrer l’arrivée'}</Button>}
      {reservation.status === 'en_cours' && <Button type="button" className={buttonClass} onClick={() => { setAction('check-out'); setError(null) }}><LogOut className="size-4" /> {compact ? 'Départ' : 'Enregistrer le départ'}</Button>}
      {canExtend && <Button type="button" variant="secondary" className={buttonClass} onClick={() => { setNewCheckOut(nextDay(reservation.checkOut)); setAction('extend'); setError(null) }}><CalendarPlus className="size-4" /> Prolonger</Button>}
      {notice && !compact && <span className="rounded-full bg-olive/10 px-4 py-2 text-xs font-semibold text-olive" role="status">{notice}</span>}
    </div>
    {action && <div className="fixed inset-0 z-[80] grid place-items-center bg-forest/55 p-4 backdrop-blur-sm" role="presentation" onMouseDown={(event) => { if (event.currentTarget === event.target && !loading) setAction(null) }}>
      <section className="w-full max-w-lg rounded-[2rem] bg-ivory p-6 shadow-2xl sm:p-8" role="dialog" aria-modal="true" aria-labelledby={`front-office-${reservation.id}`}>
        <div className="flex items-start justify-between"><span className="grid size-12 place-items-center rounded-full bg-olive/10 text-olive">{action === 'check-in' ? <DoorOpen className="size-5" /> : action === 'check-out' ? <LogOut className="size-5" /> : <CalendarPlus className="size-5" />}</span><button type="button" className="grid size-9 place-items-center rounded-full text-forest/45 hover:bg-forest/5" onClick={() => setAction(null)} disabled={loading} aria-label="Fermer"><X className="size-5" /></button></div>
        <h2 id={`front-office-${reservation.id}`} className="mt-6 font-editorial text-3xl">{action === 'check-in' ? 'Enregistrer l’arrivée' : action === 'check-out' ? 'Enregistrer le départ' : 'Prolonger le séjour'}</h2>
        <p className="mt-3 text-sm leading-6 text-forest/55">{action === 'check-in' ? 'L’heure réelle sera enregistrée et le séjour passera en cours, sans modifier l’état de nettoyage de la chambre.' : action === 'check-out' ? 'L’heure réelle de départ sera conservée et le séjour sera clôturé.' : 'Oracle vérifiera la disponibilité de la chambre avant de prolonger le départ prévu.'}</p>
        {action === 'extend' && <div className="mt-6"><Field label="Nouvelle date de départ"><Input type="date" required min={nextDay(reservation.checkOut)} value={newCheckOut} onChange={(event) => setNewCheckOut(event.target.value)} /></Field></div>}
        {error && <div className="mt-5"><ErrorNotice message={error} /></div>}
        <div className="mt-7 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end"><Button type="button" variant="secondary" onClick={() => setAction(null)} disabled={loading}>Fermer</Button><Button type="button" onClick={confirm} disabled={loading || (action === 'extend' && newCheckOut <= reservation.checkOut)}>{loading ? <><LoaderCircle className="size-4 animate-spin" /> Enregistrement…</> : 'Confirmer'}</Button></div>
      </section>
    </div>}
  </>
}
