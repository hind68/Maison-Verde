import { ArrowLeft, BedDouble, Check, LoaderCircle, Search } from 'lucide-react'
import { type FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { EmptyState, ErrorNotice, LoadingState } from '../components/ui/Feedback'
import { Field, Input } from '../components/ui/Field'
import { ApiError, hotelApi } from '../lib/api'
import type { AvailableRoom, Reservation, UpdateReservationRequest } from '../types/api'

export function EditReservationPage() {
  const { id } = useParams()
  const reservationId = Number(id)
  const navigate = useNavigate()
  const [reservation, setReservation] = useState<Reservation | null>(null)
  const [form, setForm] = useState<UpdateReservationRequest | null>(null)
  const [rooms, setRooms] = useState<AvailableRoom[]>([])
  const [loading, setLoading] = useState(true)
  const [checking, setChecking] = useState(false)
  const [saving, setSaving] = useState(false)
  const [roomsChecked, setRoomsChecked] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!Number.isInteger(reservationId) || reservationId < 1) {
      setError('Identifiant de réservation invalide.')
      setLoading(false)
      return
    }
    hotelApi.getReservation(reservationId)
      .then((value) => {
        setReservation(value)
        const initial = { roomId: value.room.id, checkIn: value.checkIn, checkOut: value.checkOut, adults: value.occupants.adults, children: value.occupants.children }
        setForm(initial)
        if (!['en_attente', 'confirmée'].includes(value.status)) {
          setError('Cette réservation ne peut plus être modifiée dans son statut actuel.')
          return
        }
        return checkRooms(initial)
      })
      .catch((reason: unknown) => setError(reason instanceof ApiError ? reason.message : 'Impossible de charger la réservation.'))
      .finally(() => setLoading(false))
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reservationId])

  function change(values: Partial<UpdateReservationRequest>) {
    setForm((current) => current ? { ...current, ...values } : current)
    setRoomsChecked(false)
  }

  async function checkRooms(values = form) {
    if (!values) return
    setChecking(true)
    setError(null)
    try {
      const available = await hotelApi.findAvailableRooms(values, undefined, reservationId)
      setRooms(available)
      setRoomsChecked(true)
      if (!available.some((room) => room.id === values.roomId)) {
        setForm((current) => current ? { ...current, roomId: 0 } : current)
      }
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'La disponibilité n’a pas pu être vérifiée.')
    } finally {
      setChecking(false)
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!form || !roomsChecked || form.roomId < 1 || saving) return
    setSaving(true)
    setError(null)
    try {
      await hotelApi.updateReservation(reservationId, form)
      navigate(`/reservations/${reservationId}`)
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'La modification n’a pas pu être enregistrée.')
      if (reason instanceof ApiError && reason.status === 409) setRoomsChecked(false)
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <LoadingState label="Chargement de la réservation…" />
  if (!reservation || !form) return <div className="mx-auto max-w-2xl"><ErrorNotice message={error ?? 'Réservation introuvable.'} /></div>

  return <div className="mx-auto max-w-5xl">
    <Link to={`/reservations/${reservationId}`} className="inline-flex items-center gap-2 text-sm text-forest/55 transition hover:text-forest"><ArrowLeft className="size-4" /> Retour à la fiche</Link>
    <div className="mt-7"><p className="eyebrow">Réservation #{reservationId}</p><h1 className="mt-4 font-editorial text-5xl tracking-[-.035em] sm:text-6xl">Modifier le séjour</h1><p className="mt-4 text-sm text-forest/55">Client conservé : {reservation.client.firstName} {reservation.client.lastName}</p></div>
    {error && <div className="mt-7"><ErrorNotice message={error} action={!roomsChecked ? <Button type="button" variant="secondary" onClick={() => checkRooms()}>Relancer la recherche</Button> : undefined} /></div>}
    <form onSubmit={submit} className="mt-8 rounded-[2rem] border border-forest/8 bg-white/55 p-6 sm:p-9">
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="Date d’arrivée"><Input type="date" required value={form.checkIn} onChange={(e) => change({ checkIn: e.target.value })} /></Field>
        <Field label="Date de départ"><Input type="date" required min={form.checkIn} value={form.checkOut} onChange={(e) => change({ checkOut: e.target.value })} /></Field>
        <Field label="Adultes"><Input type="number" min="1" required value={form.adults} onChange={(e) => change({ adults: Number(e.target.value) })} /></Field>
        <Field label="Enfants"><Input type="number" min="0" required value={form.children} onChange={(e) => change({ children: Number(e.target.value) })} /></Field>
      </div>
      <div className="mt-6 flex justify-end"><Button type="button" variant="secondary" disabled={checking} onClick={() => checkRooms()}>{checking ? <><LoaderCircle className="size-4 animate-spin" /> Vérification…</> : <><Search className="size-4" /> Vérifier les chambres</>}</Button></div>

      <div className="mt-8 border-t border-forest/8 pt-8">
        <p className="eyebrow">Chambre compatible</p>
        {!roomsChecked ? <p className="mt-4 text-sm text-forest/50">Vérifiez la disponibilité après chaque changement de dates ou d’occupants.</p> : rooms.length === 0 ? <div className="mt-5"><EmptyState title="Aucune chambre disponible">Modifiez les dates ou la composition du séjour.</EmptyState></div> : <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{rooms.map((room) => <button type="button" key={room.id} onClick={() => setForm({ ...form, roomId: room.id })} className={`rounded-3xl border p-5 text-left transition ${form.roomId === room.id ? 'border-gold bg-[#f5f0e4] ring-2 ring-gold/20' : 'border-forest/10 bg-white/60 hover:border-forest/25'}`}><div className="flex justify-between"><BedDouble className="size-5 text-olive" />{form.roomId === room.id && <Check className="size-5 text-olive" />}</div><h2 className="mt-5 font-editorial text-2xl">Chambre {room.number}</h2><p className="mt-2 text-xs text-forest/50">{room.roomType} · étage {room.floor} · {room.capacity} pers.</p></button>)}</div>}
      </div>
      <div className="mt-9 flex flex-col-reverse gap-3 sm:flex-row sm:justify-between"><Link to={`/reservations/${reservationId}`}><Button type="button" variant="ghost">Abandonner</Button></Link><Button disabled={saving || !roomsChecked || form.roomId < 1}>{saving ? <><LoaderCircle className="size-4 animate-spin" /> Enregistrement Oracle…</> : 'Enregistrer les modifications'}</Button></div>
    </form>
  </div>
}
