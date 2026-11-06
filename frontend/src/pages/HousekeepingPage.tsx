import { CheckCircle2, LoaderCircle, RefreshCw, ShieldCheck, Sparkles } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Button } from '../components/ui/Button'
import { EmptyState, ErrorNotice, LoadingState } from '../components/ui/Feedback'
import { Field, Select } from '../components/ui/Field'
import { ApiError, hotelApi } from '../lib/api'
import type { HousekeepingStatus, RoomCatalogItem } from '../types/api'
import { useAuth } from '../auth/AuthContext'

const labels: Record<HousekeepingStatus, string> = {
  a_nettoyer: 'À nettoyer',
  nettoyee: 'Nettoyée',
  prete: 'Inspectée / prête',
}

export function HousekeepingPage() {
  const { hasRole } = useAuth()
  const canUpdate = hasRole('ADMIN', 'MANAGER', 'HOUSEKEEPING')
  const [rooms, setRooms] = useState<RoomCatalogItem[]>([])
  const [filter, setFilter] = useState<HousekeepingStatus | ''>('')
  const [loading, setLoading] = useState(true)
  const [updating, setUpdating] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const load = useCallback((signal?: AbortSignal) => {
    setLoading(true)
    setError(null)
    hotelApi.findRooms({ query: '', status: '', housekeepingStatus: '', minCapacity: 1, page: 0, size: 100 }, signal)
      .then((response) => setRooms(response.content))
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === 'AbortError') return
        setError(reason instanceof ApiError ? reason.message : 'Impossible de charger l’état des chambres.')
      })
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal)
    return () => controller.abort()
  }, [load])

  const indicators = useMemo(() => ({
    dirty: rooms.filter((room) => room.housekeepingStatus === 'a_nettoyer').length,
    cleaned: rooms.filter((room) => room.housekeepingStatus === 'nettoyee').length,
    ready: rooms.filter((room) => room.housekeepingStatus === 'prete').length,
    out: rooms.filter((room) => room.operationalStatus === 'hors_service').length,
  }), [rooms])
  const visibleRooms = useMemo(
    () => filter ? rooms.filter((room) => room.housekeepingStatus === filter) : rooms,
    [filter, rooms],
  )

  async function advance(room: RoomCatalogItem) {
    const target: HousekeepingStatus | null = room.housekeepingStatus === 'a_nettoyer'
      ? 'nettoyee'
      : room.housekeepingStatus === 'nettoyee' ? 'prete' : null
    if (!target) return
    setUpdating(room.id)
    setError(null)
    setSuccess(null)
    try {
      const updated = await hotelApi.updateHousekeeping(room.id, target)
      setRooms((current) => current.map((item) => item.id === updated.id ? updated : item))
      setSuccess(`Chambre ${updated.number} déclarée ${labels[target].toLowerCase()}.`)
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'La transition de propreté a échoué.')
    } finally {
      setUpdating(null)
    }
  }

  return <div className="mx-auto max-w-[1380px]">
    <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
      <div><p className="eyebrow">Opérations du jour</p><h1 className="mt-4 font-editorial text-5xl tracking-[-.035em] sm:text-6xl">Housekeeping</h1><p className="mt-4 max-w-2xl text-sm leading-7 text-forest/55">Le départ place automatiquement une chambre dans la file de nettoyage. L’inspection finale seule la rend prête pour une nouvelle arrivée.</p></div>
      <Button variant="ghost" onClick={() => load()}><RefreshCw className="size-4" /> Actualiser</Button>
    </div>

    <div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <Indicator label="À nettoyer" value={indicators.dirty} tone="bg-red-900 text-ivory" />
      <Indicator label="Nettoyées" value={indicators.cleaned} tone="bg-gold/25 text-forest" />
      <Indicator label="Prêtes" value={indicators.ready} tone="bg-olive text-ivory" />
      <Indicator label="Hors service" value={indicators.out} tone="bg-forest text-ivory" />
    </div>

    {error && <div className="mt-6"><ErrorNotice message={error} /></div>}
    {success && <div className="mt-6 rounded-2xl border border-olive/20 bg-olive/8 px-5 py-4 text-sm text-olive">{success}</div>}

    <div className="mt-7 max-w-xs"><Field label="État de propreté"><Select value={filter} onChange={(event) => setFilter(event.target.value as HousekeepingStatus | '')}><option value="">Toutes les chambres</option><option value="a_nettoyer">À nettoyer</option><option value="nettoyee">Nettoyées</option><option value="prete">Inspectées / prêtes</option></Select></Field></div>

    <section className="mt-6 overflow-hidden rounded-[2rem] border border-forest/8 bg-white/55">
      {loading ? <LoadingState label="Chargement du suivi housekeeping…" /> : visibleRooms.length === 0 ? <EmptyState title="Aucune chambre">Aucune chambre ne correspond à ce filtre.</EmptyState> : <div className="divide-y divide-forest/7">{visibleRooms.map((room) => {
        const next = room.housekeepingStatus === 'a_nettoyer' ? 'Déclarer nettoyée' : room.housekeepingStatus === 'nettoyee' ? 'Inspecter et rendre prête' : null
        return <article key={room.id} className="grid gap-5 p-6 transition hover:bg-ivory/60 md:grid-cols-[1fr_auto] md:items-center">
          <div className="flex items-center gap-4"><span className="grid size-12 place-items-center rounded-full bg-olive/10 text-olive"><Sparkles className="size-5" /></span><div><div className="flex flex-wrap items-center gap-2"><h2 className="font-editorial text-2xl">Chambre {room.number}</h2><StatusBadge status={room.housekeepingStatus} />{room.operationalStatus === 'hors_service' && <span className="rounded-full bg-amber-100 px-3 py-1 text-[10px] font-semibold uppercase tracking-[.12em] text-amber-900">Hors service</span>}</div><p className="mt-1 text-xs text-forest/45">{room.roomType} · Étage {room.floor} · {room.capacity} personnes</p></div></div>
          {next && canUpdate ? <Button disabled={updating === room.id || room.operationalStatus === 'hors_service'} onClick={() => void advance(room)}>{updating === room.id ? <LoaderCircle className="size-4 animate-spin" /> : room.housekeepingStatus === 'nettoyee' ? <ShieldCheck className="size-4" /> : <CheckCircle2 className="size-4" />}{next}</Button> : <span className="text-xs font-semibold uppercase tracking-[.16em] text-olive">{next ? 'Consultation uniquement' : 'Prête pour l’arrivée'}</span>}
        </article>
      })}</div>}
    </section>
  </div>
}

function Indicator({ label, value, tone }: { label: string; value: number; tone: string }) {
  return <article className={`rounded-[1.7rem] p-6 ${tone}`}><p className="text-[10px] uppercase tracking-[.2em] opacity-60">{label}</p><p className="mt-5 font-editorial text-5xl">{value}</p></article>
}

function StatusBadge({ status }: { status: HousekeepingStatus }) {
  const classes = status === 'a_nettoyer' ? 'bg-red-100 text-red-900' : status === 'nettoyee' ? 'bg-gold/25 text-forest' : 'bg-olive/10 text-olive'
  return <span className={`rounded-full px-3 py-1 text-[10px] font-semibold uppercase tracking-[.12em] ${classes}`}>{labels[status]}</span>
}
