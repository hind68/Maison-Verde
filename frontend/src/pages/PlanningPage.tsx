import { CalendarDays, ChevronLeft, ChevronRight } from 'lucide-react'
import { type FormEvent, useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { EmptyState, ErrorNotice, LoadingState } from '../components/ui/Feedback'
import { Field, Input } from '../components/ui/Field'
import { ApiError, hotelApi } from '../lib/api'
import { toIsoLocal } from '../lib/format'
import type { Planning, PlanningStay } from '../types/api'

const DAY_WIDTH = 54

function addDays(value: string, days: number): string {
  const date = new Date(`${value}T12:00:00`)
  date.setDate(date.getDate() + days)
  return toIsoLocal(date)
}

function daysBetween(start: string, end: string): number {
  return Math.round((Date.parse(`${end}T00:00:00Z`) - Date.parse(`${start}T00:00:00Z`)) / 86_400_000)
}

function initialPeriod() {
  const start = toIsoLocal(new Date())
  return { start, end: addDays(start, 14) }
}

export function PlanningPage() {
  const [period, setPeriod] = useState(initialPeriod)
  const [draft, setDraft] = useState(initialPeriod)
  const [planning, setPlanning] = useState<Planning | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const dayCount = daysBetween(period.start, period.end)
  const days = useMemo(() => Array.from({ length: Math.max(0, dayCount) }, (_, index) => addDays(period.start, index)), [dayCount, period.start])

  const load = useCallback((signal?: AbortSignal) => {
    setLoading(true)
    setError(null)
    hotelApi.getPlanning(period.start, period.end, signal)
      .then(setPlanning)
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === 'AbortError') return
        setError(reason instanceof ApiError ? reason.message : 'Impossible de charger le planning.')
      })
      .finally(() => setLoading(false))
  }, [period])

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal)
    return () => controller.abort()
  }, [load])

  function applyPeriod(event: FormEvent) {
    event.preventDefault()
    setPeriod(draft)
  }

  function move(direction: -1 | 1) {
    const length = Math.max(1, daysBetween(period.start, period.end))
    const next = { start: addDays(period.start, direction * length), end: addDays(period.end, direction * length) }
    setPeriod(next)
    setDraft(next)
  }

  return <div className="mx-auto max-w-[1500px]">
    <p className="eyebrow">Occupation des chambres</p>
    <div className="mt-4 flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between"><div><h1 className="font-editorial text-5xl tracking-[-.035em] sm:text-6xl">Planning hôtelier</h1><p className="mt-4 max-w-2xl text-sm leading-7 text-forest/55">Chaque colonne représente une nuit. Le départ libère immédiatement la chambre selon la convention [arrivée, départ).</p></div><div className="flex gap-2"><Button type="button" variant="secondary" onClick={() => move(-1)}><ChevronLeft className="size-4" /> Période précédente</Button><Button type="button" variant="secondary" onClick={() => move(1)}>Suivante <ChevronRight className="size-4" /></Button></div></div>
    <form onSubmit={applyPeriod} className="mt-8 flex flex-col gap-4 rounded-3xl border border-forest/8 bg-white/55 p-5 sm:flex-row sm:items-end">
      <Field label="Début"><Input type="date" required value={draft.start} onChange={(e) => setDraft({ ...draft, start: e.target.value })} /></Field>
      <Field label="Fin"><Input type="date" required min={draft.start} value={draft.end} onChange={(e) => setDraft({ ...draft, end: e.target.value })} /></Field>
      <Button><CalendarDays className="size-4" /> Afficher la période</Button>
      <div className="ml-auto flex flex-wrap items-center gap-4 pb-2 text-[11px] text-forest/55"><Legend color="bg-gold" label="En attente" /><Legend color="bg-olive" label="Confirmée" /><Legend color="bg-forest" label="En cours" /><Legend color="bg-slate-500" label="Terminée" /></div>
    </form>
    {error && <div className="mt-6"><ErrorNotice message={error} /></div>}
    <div className="mt-6">{loading ? <LoadingState label="Construction du planning Oracle…" /> : !planning || planning.rooms.length === 0 ? <EmptyState title="Aucune chambre">Le catalogue des chambres est vide.</EmptyState> : <section className="overflow-hidden rounded-[1.8rem] border border-forest/10 bg-white/65"><div className="overflow-x-auto"><div style={{ minWidth: 230 + dayCount * DAY_WIDTH }}>
      <div className="grid border-b border-forest/10 bg-forest text-ivory" style={{ gridTemplateColumns: `230px repeat(${dayCount}, ${DAY_WIDTH}px)` }}><div className="sticky left-0 z-30 flex items-end bg-forest px-5 py-4"><span className="text-[10px] uppercase tracking-[.2em] text-ivory/50">Chambre</span></div>{days.map((day) => <DayHeader key={day} day={day} />)}</div>
      {planning.rooms.map((room) => <div key={room.id} className="grid min-h-[74px] border-b border-forest/7 last:border-b-0" style={{ gridTemplateColumns: `230px ${dayCount * DAY_WIDTH}px` }}><div className="sticky left-0 z-20 flex items-center justify-between border-r border-forest/8 bg-[#f8f5ee] px-5"><div><strong className="font-editorial text-xl">{room.number}</strong><span className="ml-2 text-xs text-forest/45">{room.roomType}</span><p className="mt-1 text-[10px] text-forest/40">Étage {room.floor} · {room.capacity} pers. · {room.housekeepingStatus.replace('_', ' ')}</p></div><span className={`size-2.5 rounded-full ${room.operationalStatus === 'hors_service' ? 'bg-amber-500' : 'bg-olive'}`} title={`Statut : ${room.operationalStatus}`} /></div><div className="relative" style={{ width: dayCount * DAY_WIDTH, backgroundImage: `repeating-linear-gradient(to right, transparent 0, transparent ${DAY_WIDTH - 1}px, rgba(28,40,31,.08) ${DAY_WIDTH - 1}px, rgba(28,40,31,.08) ${DAY_WIDTH}px)` }}>{room.reservations.map((stay) => <StayBar key={stay.id} stay={stay} start={period.start} end={period.end} />)}</div></div>)}
    </div></div></section>}</div>
    <p className="mt-4 text-xs text-forest/40">Point vert : en service · ambre : hors service. L’occupation reste représentée uniquement par les séjours.</p>
  </div>
}

function DayHeader({ day }: { day: string }) {
  const date = new Date(`${day}T12:00:00`)
  const weekday = new Intl.DateTimeFormat('fr-FR', { weekday: 'short' }).format(date).replace('.', '')
  return <div className="border-l border-white/10 px-1 py-3 text-center"><span className="block text-[9px] uppercase text-ivory/40">{weekday}</span><strong className="mt-1 block font-editorial text-lg text-gold">{date.getDate()}</strong></div>
}

function StayBar({ stay, start, end }: { stay: PlanningStay; start: string; end: string }) {
  const visibleStart = stay.checkIn < start ? start : stay.checkIn
  const visibleEnd = stay.checkOut > end ? end : stay.checkOut
  const left = daysBetween(start, visibleStart) * DAY_WIDTH + 4
  const width = Math.max(20, daysBetween(visibleStart, visibleEnd) * DAY_WIDTH - 8)
  const color = stay.status === 'en_attente' ? 'bg-gold text-forest' : stay.status === 'confirmée' ? 'bg-olive text-ivory' : stay.status === 'en_cours' ? 'bg-forest text-ivory' : 'bg-slate-500 text-white'
  return <Link to={`/reservations/${stay.id}`} className={`absolute top-3 flex h-12 items-center overflow-hidden rounded-xl px-3 shadow-sm transition hover:z-10 hover:-translate-y-0.5 hover:shadow-lg ${color}`} style={{ left, width }} title={`${stay.guestName} · ${stay.guestCount} occupant(s)`}><span className="truncate text-xs font-semibold">#{stay.id} · {stay.guestName}</span></Link>
}

function Legend({ color, label }: { color: string; label: string }) {
  return <span className="inline-flex items-center gap-2"><span className={`size-2.5 rounded-full ${color}`} />{label}</span>
}
