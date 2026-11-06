import { CalendarRange, ChevronLeft, ChevronRight, Pencil, Search, UsersRound } from 'lucide-react'
import { type FormEvent, useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { CancelReservationDialog } from '../components/reservation/CancelReservationDialog'
import { FrontOfficeActions } from '../components/reservation/FrontOfficeActions'
import { Button } from '../components/ui/Button'
import { EmptyState, ErrorNotice, LoadingState } from '../components/ui/Feedback'
import { Field, Input, Select } from '../components/ui/Field'
import { ApiError, hotelApi } from '../lib/api'
import { formatDateTime, formatShortDate, statusLabel } from '../lib/format'
import type { PagedResponse, Reservation } from '../types/api'

export function ReservationsPage() {
  const [draftQuery, setDraftQuery] = useState('')
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('')
  const [checkIn, setCheckIn] = useState('')
  const [checkOut, setCheckOut] = useState('')
  const [page, setPage] = useState(0)
  const [result, setResult] = useState<PagedResponse<Reservation> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback((signal?: AbortSignal) => {
    setLoading(true)
    setError(null)
    hotelApi.findReservations({ query, status, checkIn: checkIn || undefined, checkOut: checkOut || undefined, page, size: 12 }, signal)
      .then(setResult)
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === 'AbortError') return
        setError(reason instanceof ApiError ? reason.message : 'Impossible de charger les réservations.')
      })
      .finally(() => setLoading(false))
  }, [checkIn, checkOut, page, query, status])

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal)
    return () => controller.abort()
  }, [load])

  function filter(event: FormEvent) {
    event.preventDefault()
    setPage(0)
    setQuery(draftQuery.trim())
  }

  return <div className="mx-auto max-w-[1500px]">
    <p className="eyebrow">Suivi des séjours</p>
    <div className="mt-4 flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between"><div><h1 className="font-editorial text-5xl tracking-[-.035em] sm:text-6xl">Réservations</h1><p className="mt-4 max-w-2xl text-sm leading-7 text-forest/55">Liste relue depuis Oracle, avec horaires Front Office et actions adaptées à chaque statut.</p></div><Link to="/reservations/nouvelle"><Button>Nouvelle réservation</Button></Link></div>
    {error && <div className="mt-7"><ErrorNotice message={error} /></div>}
    <form onSubmit={filter} className="mt-8 grid gap-4 rounded-3xl border border-forest/8 bg-white/55 p-5 md:grid-cols-2 xl:grid-cols-[1fr_180px_170px_170px_auto]">
      <Field label="Recherche"><Input value={draftQuery} onChange={(event) => setDraftQuery(event.target.value)} placeholder="Client, e-mail, chambre ou référence" /></Field>
      <Field label="Statut"><Select value={status} onChange={(event) => { setStatus(event.target.value); setPage(0) }}><option value="">Tous</option><option value="confirmée">Confirmée</option><option value="en_attente">En attente</option><option value="en_cours">En cours</option><option value="terminée">Terminée</option><option value="annulée">Annulée</option></Select></Field>
      <Field label="Début de période"><Input type="date" value={checkIn} onChange={(event) => { setCheckIn(event.target.value); setPage(0) }} /></Field>
      <Field label="Fin de période"><Input type="date" min={checkIn || undefined} value={checkOut} onChange={(event) => { setCheckOut(event.target.value); setPage(0) }} /></Field>
      <div className="flex items-end"><Button><Search className="size-4" /> Rechercher</Button></div>
    </form>
    <div className="mt-6">{loading ? <LoadingState label="Chargement des réservations…" /> : !result || result.content.length === 0 ? <EmptyState title="Aucune réservation">Aucun séjour ne correspond à ces critères.</EmptyState> : <ReservationTable reservations={result.content} reload={() => load()} />}</div>
    {result && result.totalPages > 1 && <div className="mt-7 flex items-center justify-between text-sm text-forest/55"><span>{result.totalElements} réservations</span><div className="flex items-center gap-3"><Button variant="ghost" disabled={page === 0} onClick={() => setPage((value) => value - 1)}><ChevronLeft className="size-4" /> Précédent</Button><span>{page + 1} / {result.totalPages}</span><Button variant="ghost" disabled={page + 1 >= result.totalPages} onClick={() => setPage((value) => value + 1)}>Suivant <ChevronRight className="size-4" /></Button></div></div>}
  </div>
}

function ReservationTable({ reservations, reload }: { reservations: Reservation[]; reload: () => void }) {
  return <div className="overflow-hidden rounded-[1.8rem] border border-forest/8 bg-white/60"><div className="overflow-x-auto"><table className="w-full min-w-[1320px] text-left"><thead className="border-b border-forest/8 bg-forest/[.035] text-[10px] uppercase tracking-[.16em] text-forest/45"><tr><th className="px-5 py-4">Réf.</th><th className="px-5 py-4">Client</th><th className="px-5 py-4">Chambre</th><th className="px-5 py-4">Séjour</th><th className="px-5 py-4">Occupants</th><th className="px-5 py-4">Statut / horaire réel</th><th className="px-5 py-4">Actions</th></tr></thead><tbody className="divide-y divide-forest/7">{reservations.map((reservation) => {
    const editable = ['en_attente', 'confirmée'].includes(reservation.status)
    const frontOffice = ['confirmée', 'en_cours'].includes(reservation.status)
    const actualTime = reservation.actualCheckOut ?? reservation.actualCheckIn
    return <tr key={reservation.id} className="transition hover:bg-olive/[.045]">
      <td className="px-5 py-5"><Link className="font-semibold text-olive underline-offset-4 hover:underline" to={`/reservations/${reservation.id}`}>#{reservation.id}</Link></td>
      <td className="px-5 py-5"><strong className="block text-sm">{reservation.client.firstName} {reservation.client.lastName}</strong><span className="mt-1 block text-xs text-forest/45">{reservation.client.email}</span></td>
      <td className="px-5 py-5"><strong className="block text-sm">{reservation.room.number}</strong><span className="mt-1 block text-xs text-forest/45">{reservation.room.roomType}</span></td>
      <td className="px-5 py-5 text-sm"><span className="inline-flex items-center gap-2"><CalendarRange className="size-4 text-gold" />{formatShortDate(reservation.checkIn)} — {formatShortDate(reservation.checkOut)}</span></td>
      <td className="px-5 py-5 text-sm"><span className="inline-flex items-center gap-2"><UsersRound className="size-4 text-gold" />{reservation.guestCount}</span></td>
      <td className="px-5 py-5"><span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusClasses(reservation.status)}`}>{statusLabel(reservation.status)}</span>{actualTime && <span className="mt-2 block text-[11px] text-forest/45">{reservation.actualCheckOut ? 'Départ' : 'Arrivée'} · {formatDateTime(actualTime)}</span>}</td>
      <td className="px-5 py-5"><div className="flex items-center gap-1">{frontOffice && <FrontOfficeActions compact reservation={reservation} onUpdated={reload} />}{editable && <Link to={`/reservations/${reservation.id}/modifier`}><Button variant="ghost" className="!min-h-9 !px-3"><Pencil className="size-3.5" /> Modifier</Button></Link>}{editable && <CancelReservationDialog compact reservationId={reservation.id} onCancelled={reload} />}{!frontOffice && !editable && <span className="text-xs text-forest/35">Lecture seule</span>}</div></td>
    </tr>
  })}</tbody></table></div></div>
}

function statusClasses(status: string): string {
  if (status === 'en_cours') return 'bg-forest text-ivory'
  if (status === 'terminée') return 'bg-slate-200 text-slate-700'
  if (status === 'annulée') return 'bg-red-900/8 text-red-900'
  if (status === 'en_attente') return 'bg-gold/25 text-forest'
  return 'bg-olive/10 text-olive'
}
