import { ArrowLeft, BedDouble, CalendarDays, CheckCircle2, Clock, Hash, LoaderCircle, Pencil, UsersRound } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { CancelReservationDialog } from '../components/reservation/CancelReservationDialog'
import { FrontOfficeActions } from '../components/reservation/FrontOfficeActions'
import { ErrorNotice } from '../components/ui/Feedback'
import { ApiError, hotelApi } from '../lib/api'
import { formatDate, formatDateTime, nightsBetween, statusLabel } from '../lib/format'
import type { Reservation } from '../types/api'

export function ReservationDetailPage() {
  const { id } = useParams()
  const reservationId = Number(id)
  const [reservation, setReservation] = useState<Reservation | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!Number.isInteger(reservationId) || reservationId < 1) {
      setError('Identifiant de réservation invalide.')
      setLoading(false)
      return
    }
    const controller = new AbortController()
    hotelApi.getReservation(reservationId, controller.signal)
      .then(setReservation)
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === 'AbortError') return
        setError(reason instanceof ApiError ? reason.message : 'Impossible de consulter cette réservation.')
      })
      .finally(() => setLoading(false))
    return () => controller.abort()
  }, [reservationId])

  if (loading) {
    return <div className="grid min-h-[60vh] place-items-center"><div className="text-center"><LoaderCircle className="mx-auto size-7 animate-spin text-gold" /><p className="mt-4 text-sm text-forest/55">Consultation de la réservation…</p></div></div>
  }

  if (error || !reservation) {
    return <div className="mx-auto max-w-2xl py-16"><ErrorNotice message={error ?? 'Réservation introuvable.'} /><Link to="/reservations/nouvelle" className="mt-6 inline-block"><Button>Nouvelle réservation</Button></Link></div>
  }

  const nights = nightsBetween(reservation.checkIn, reservation.checkOut)
  const editable = ['en_attente', 'confirmée'].includes(reservation.status)
  return (
    <div className="mx-auto max-w-5xl">
      <Link to="/reservations" className="inline-flex items-center gap-2 text-sm text-forest/55 transition hover:text-forest"><ArrowLeft className="size-4" /> Retour aux réservations</Link>
      <section className="relative mt-7 overflow-hidden rounded-[2.2rem] bg-forest p-7 text-ivory sm:p-11">
        <div className="architectural-grid absolute inset-0 opacity-20" />
        <div className="relative flex flex-col gap-8 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <div className="flex items-center gap-2 text-gold"><CheckCircle2 className="size-5" /><span className="text-xs font-semibold uppercase tracking-[.2em]">Réservation {statusLabel(reservation.status).toLowerCase()}</span></div>
            <h1 className="mt-6 font-editorial text-5xl sm:text-6xl">{reservation.status === 'annulée' ? 'Séjour annulé' : 'Séjour enregistré'}</h1>
            <p className="mt-4 max-w-xl text-sm leading-7 text-ivory/55">Les informations ci-dessous ont été relues depuis Oracle. Cette page reste consultable avec son adresse après actualisation.</p>
          </div>
          <div className="rounded-3xl border border-white/10 bg-white/5 px-5 py-4">
            <p className="text-[10px] uppercase tracking-[.2em] text-ivory/40">Référence</p>
            <p className="mt-2 font-editorial text-3xl text-gold">#{reservation.id}</p>
          </div>
        </div>
      </section>

      <section className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <DetailCard icon={<CalendarDays />} label="Arrivée" value={formatDate(reservation.checkIn)} detail={`${nights} nuit${nights > 1 ? 's' : ''}`} />
        <DetailCard icon={<CalendarDays />} label="Départ" value={formatDate(reservation.checkOut)} detail={statusLabel(reservation.status)} />
        <DetailCard icon={<BedDouble />} label="Chambre" value={`${reservation.room.number} · ${reservation.room.roomType}`} detail={`Étage ${reservation.room.floor}`} />
        <DetailCard icon={<UsersRound />} label="Occupants" value={`${reservation.guestCount} personne${reservation.guestCount > 1 ? 's' : ''}`} detail={`${reservation.occupants.adults} adulte${reservation.occupants.adults > 1 ? 's' : ''} · ${reservation.occupants.children} enfant${reservation.occupants.children > 1 ? 's' : ''}`} />
      </section>

      <section className="mt-6 rounded-[2rem] border border-forest/8 bg-white/55 p-7 sm:p-9">
        <div className="flex items-center gap-4"><span className="grid size-12 place-items-center rounded-full bg-olive/10 text-olive"><Hash className="size-5" /></span><div><p className="eyebrow">Client associé</p><h2 className="mt-1 font-editorial text-3xl">{reservation.client.firstName} {reservation.client.lastName}</h2></div></div>
        <div className="mt-6 border-t border-forest/8 pt-5 text-sm text-forest/55">{reservation.client.email}</div>
      </section>

      {(reservation.actualCheckIn || reservation.actualCheckOut) && <section className="mt-6 grid gap-4 sm:grid-cols-2">
        <DetailCard icon={<Clock />} label="Arrivée réelle" value={reservation.actualCheckIn ? formatDateTime(reservation.actualCheckIn) : 'Non enregistrée'} detail="Horodatage Front Office Oracle" />
        <DetailCard icon={<Clock />} label="Départ réel" value={reservation.actualCheckOut ? formatDateTime(reservation.actualCheckOut) : 'Non enregistré'} detail="Distinct de l’état de nettoyage de la chambre" />
      </section>}

      <div className="mt-7 flex flex-wrap gap-3">
        <FrontOfficeActions reservation={reservation} onUpdated={setReservation} />
        {editable && <Link to={`/reservations/${reservation.id}/modifier`}><Button><Pencil className="size-4" /> Modifier</Button></Link>}
        {editable && <CancelReservationDialog reservationId={reservation.id} onCancelled={setReservation} />}
        <Link to="/reservations/nouvelle"><Button>Créer une autre réservation</Button></Link>
        <Link to="/reservations"><Button variant="secondary">Voir toutes les réservations</Button></Link>
      </div>
    </div>
  )
}

function DetailCard({ icon, label, value, detail }: { icon: React.ReactNode; label: string; value: string; detail: string }) {
  return <article className="rounded-3xl border border-forest/8 bg-white/55 p-5"><span className="text-gold [&>svg]:size-5">{icon}</span><p className="mt-6 text-[10px] font-semibold uppercase tracking-[.18em] text-forest/40">{label}</p><p className="mt-2 font-editorial text-2xl leading-tight">{value}</p><p className="mt-2 text-xs text-forest/45">{detail}</p></article>
}
