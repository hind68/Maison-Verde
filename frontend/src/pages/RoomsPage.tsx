import { BedDouble, CalendarSearch, ChevronLeft, ChevronRight, LoaderCircle, Search, Sparkles, Wrench } from 'lucide-react'
import { type FormEvent, useCallback, useEffect, useState } from 'react'
import { Button } from '../components/ui/Button'
import { EmptyState, ErrorNotice, LoadingState } from '../components/ui/Feedback'
import { Field, Input, Select } from '../components/ui/Field'
import { ApiError, hotelApi } from '../lib/api'
import { toIsoLocal } from '../lib/format'
import type { PagedResponse, RoomCatalogItem, StaySearch } from '../types/api'
import { useAuth } from '../auth/AuthContext'

function initialStay(): StaySearch {
  const arrival = new Date()
  arrival.setDate(arrival.getDate() + 1)
  const departure = new Date(arrival)
  departure.setDate(departure.getDate() + 2)
  return { checkIn: toIsoLocal(arrival), checkOut: toIsoLocal(departure), adults: 1, children: 0 }
}

export function RoomsPage() {
  const { hasRole } = useAuth()
  const canManageOperations = hasRole('ADMIN', 'MANAGER')
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('')
  const [housekeepingStatus, setHousekeepingStatus] = useState('')
  const [minCapacity, setMinCapacity] = useState(1)
  const [page, setPage] = useState(0)
  const [rooms, setRooms] = useState<PagedResponse<RoomCatalogItem> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [stay, setStay] = useState<StaySearch>(initialStay)
  const [checking, setChecking] = useState(false)
  const [availableIds, setAvailableIds] = useState<Set<number> | null>(null)
  const [pendingOperational, setPendingOperational] = useState<RoomCatalogItem | null>(null)
  const [updating, setUpdating] = useState<number | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const load = useCallback((signal?: AbortSignal) => {
    setLoading(true)
    setError(null)
    hotelApi.findRooms({ query, status, housekeepingStatus, minCapacity, page, size: 12 }, signal)
      .then(setRooms)
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === 'AbortError') return
        setError(reason instanceof ApiError ? reason.message : 'Impossible de charger les chambres.')
      })
      .finally(() => setLoading(false))
  }, [housekeepingStatus, minCapacity, page, query, status])

  async function changeOperational(room: RoomCatalogItem) {
    const target = room.operationalStatus === 'en_service' ? 'hors_service' : 'en_service'
    setUpdating(room.id)
    setError(null)
    try {
      await hotelApi.updateOperationalStatus(room.id, target)
      setSuccess(target === 'hors_service' ? `Chambre ${room.number} mise hors service.` : `Chambre ${room.number} remise en service.`)
      setPendingOperational(null)
      load()
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'Le statut opérationnel n’a pas pu être modifié.')
    } finally {
      setUpdating(null)
    }
  }

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal)
    return () => controller.abort()
  }, [load])

  function filter(event: FormEvent) {
    event.preventDefault()
    setPage(0)
    setAvailableIds(null)
    load()
  }

  async function checkAvailability(event: FormEvent) {
    event.preventDefault()
    setChecking(true)
    setError(null)
    try {
      const available = await hotelApi.findAvailableRooms(stay)
      setAvailableIds(new Set(available.map((room) => room.id)))
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'La disponibilité n’a pas pu être vérifiée.')
    } finally {
      setChecking(false)
    }
  }

  return (
    <div className="mx-auto max-w-[1380px]">
      <p className="eyebrow">Inventaire Oracle</p>
      <div className="mt-4 flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
        <div><h1 className="font-editorial text-5xl tracking-[-.035em] sm:text-6xl">Catalogue des chambres</h1><p className="mt-4 max-w-2xl text-sm leading-7 text-forest/55">Le statut opérationnel décrit la chambre aujourd’hui. La disponibilité est contrôlée séparément pour une période.</p></div>
      </div>

      {error && <div className="mt-7"><ErrorNotice message={error} /></div>}
      {success && <div className="mt-7 rounded-2xl border border-olive/20 bg-olive/8 px-5 py-4 text-sm text-olive">{success}</div>}

      <section className="mt-8 rounded-[2rem] bg-forest p-6 text-ivory sm:p-8">
        <div className="flex items-center gap-3"><CalendarSearch className="size-5 text-gold" /><h2 className="font-editorial text-2xl">Vérifier une période</h2></div>
        <form onSubmit={checkAvailability} className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
          <Field label="Arrivée"><Input type="date" required value={stay.checkIn} onChange={(e) => { setStay({ ...stay, checkIn: e.target.value }); setAvailableIds(null) }} /></Field>
          <Field label="Départ"><Input type="date" required min={stay.checkIn} value={stay.checkOut} onChange={(e) => { setStay({ ...stay, checkOut: e.target.value }); setAvailableIds(null) }} /></Field>
          <Field label="Adultes"><Input type="number" min="1" required value={stay.adults} onChange={(e) => { setStay({ ...stay, adults: Number(e.target.value) }); setAvailableIds(null) }} /></Field>
          <Field label="Enfants"><Input type="number" min="0" required value={stay.children} onChange={(e) => { setStay({ ...stay, children: Number(e.target.value) }); setAvailableIds(null) }} /></Field>
          <div className="flex items-end"><Button className="w-full bg-gold !text-forest hover:bg-ivory" disabled={checking}>{checking ? <><LoaderCircle className="size-4 animate-spin" /> Vérification…</> : 'Vérifier'}</Button></div>
        </form>
      </section>

      <form onSubmit={filter} className="mt-6 grid gap-4 rounded-3xl border border-forest/8 bg-white/55 p-5 md:grid-cols-2 xl:grid-cols-[1fr_180px_180px_150px_auto]">
        <Field label="Recherche"><Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Numéro ou type" /></Field>
        <Field label="Statut opérationnel"><Select value={status} onChange={(e) => setStatus(e.target.value)}><option value="">Tous</option><option value="en_service">En service</option><option value="hors_service">Hors service</option></Select></Field>
        <Field label="Propreté"><Select value={housekeepingStatus} onChange={(e) => setHousekeepingStatus(e.target.value)}><option value="">Toutes</option><option value="a_nettoyer">À nettoyer</option><option value="nettoyee">Nettoyée</option><option value="prete">Inspectée / prête</option></Select></Field>
        <Field label="Capacité minimale"><Input type="number" min="1" value={minCapacity} onChange={(e) => setMinCapacity(Number(e.target.value))} /></Field>
        <div className="flex items-end"><Button><Search className="size-4" /> Filtrer</Button></div>
      </form>

      <div className="mt-6">
        {loading ? <LoadingState label="Chargement du catalogue…" /> : !rooms || rooms.content.length === 0 ? <EmptyState title="Aucune chambre">Aucune chambre ne correspond à ces filtres.</EmptyState> : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {rooms.content.map((room) => {
              const dateAvailable = availableIds?.has(room.id)
              return <article key={room.id} className="rounded-[1.7rem] border border-forest/8 bg-white/60 p-6 transition hover:-translate-y-0.5 hover:shadow-xl">
                <div className="flex items-start justify-between gap-3"><span className="grid size-12 place-items-center rounded-full bg-olive/10 text-olive"><BedDouble className="size-5" /></span><div className="flex flex-wrap justify-end gap-2"><OperationalBadge status={room.operationalStatus} /><HousekeepingBadge status={room.housekeepingStatus} /></div></div>
                <p className="mt-7 text-[10px] uppercase tracking-[.2em] text-forest/40">Étage {room.floor}</p>
                <h2 className="mt-1 font-editorial text-3xl">Chambre {room.number}</h2>
                <div className="mt-4 flex justify-between text-sm"><span>{room.roomType}</span><span className="text-forest/50">{room.capacity} pers. max.</span></div>
                <div className="mt-5 border-t border-forest/8 pt-4 text-xs"><span className="text-forest/45">Pour la période : </span>{availableIds === null ? <strong>non vérifiée</strong> : dateAvailable ? <strong className="text-olive">disponible</strong> : <strong className="text-red-800">indisponible</strong>}</div>
                {canManageOperations && <Button type="button" variant="ghost" className="mt-4 w-full" onClick={() => setPendingOperational(room)}><Wrench className="size-4" />{room.operationalStatus === 'en_service' ? 'Mettre hors service' : 'Remettre en service'}</Button>}
              </article>
            })}
          </div>
        )}
      </div>
      {rooms && rooms.totalPages > 1 && <div className="mt-7 flex items-center justify-between text-sm text-forest/55"><span>{rooms.totalElements} chambres</span><div className="flex items-center gap-3"><Button variant="ghost" disabled={page === 0} onClick={() => setPage((v) => v - 1)}><ChevronLeft className="size-4" /> Précédent</Button><span>{page + 1} / {rooms.totalPages}</span><Button variant="ghost" disabled={page + 1 >= rooms.totalPages} onClick={() => setPage((v) => v + 1)}>Suivant <ChevronRight className="size-4" /></Button></div></div>}
      {pendingOperational && <div className="fixed inset-0 z-[80] grid place-items-center bg-forest/55 p-5 backdrop-blur-sm" role="presentation"><div role="dialog" aria-modal="true" aria-labelledby="room-operation-title" className="w-full max-w-md rounded-[2rem] bg-ivory p-7 shadow-2xl"><span className="grid size-11 place-items-center rounded-full bg-gold/20 text-forest"><Wrench className="size-5" /></span><h2 id="room-operation-title" className="mt-5 font-editorial text-3xl">{pendingOperational.operationalStatus === 'en_service' ? 'Mettre hors service ?' : 'Remettre en service ?'}</h2><p className="mt-3 text-sm leading-6 text-forest/60">La chambre {pendingOperational.number} {pendingOperational.operationalStatus === 'en_service' ? 'sera exclue des disponibilités. Toute réservation active ou future bloquera cette action.' : 'redeviendra disponible pour les recherches compatibles.'}</p><div className="mt-7 flex justify-end gap-3"><Button variant="ghost" onClick={() => setPendingOperational(null)}>Fermer</Button><Button disabled={updating === pendingOperational.id} onClick={() => void changeOperational(pendingOperational)}>{updating === pendingOperational.id ? <LoaderCircle className="size-4 animate-spin" /> : <Wrench className="size-4" />}Confirmer</Button></div></div></div>}
    </div>
  )
}

function OperationalBadge({ status }: { status: RoomCatalogItem['operationalStatus'] }) {
  const classes = status === 'en_service' ? 'bg-olive/10 text-olive' : 'bg-amber-100 text-amber-900'
  const label = status === 'en_service' ? 'En service' : 'Hors service'
  return <span className={`rounded-full px-3 py-1 text-[10px] font-semibold uppercase tracking-[.14em] ${classes}`}>{label}</span>
}

function HousekeepingBadge({ status }: { status: RoomCatalogItem['housekeepingStatus'] }) {
  const presentation = status === 'a_nettoyer'
    ? ['À nettoyer', 'bg-red-100 text-red-900']
    : status === 'nettoyee'
      ? ['Nettoyée', 'bg-gold/20 text-forest']
      : ['Prête', 'bg-olive/10 text-olive']
  return <span className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-[10px] font-semibold uppercase tracking-[.12em] ${presentation[1]}`}><Sparkles className="size-3" />{presentation[0]}</span>
}
