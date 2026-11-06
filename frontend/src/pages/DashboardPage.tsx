import { ArrowRight, BedDouble, CalendarDays, CheckCircle2, UsersRound } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { ErrorNotice, LoadingState } from '../components/ui/Feedback'
import { ApiError, hotelApi } from '../lib/api'
import { formatShortDate, toIsoLocal } from '../lib/format'
import type { AvailableRoom, StaySearch } from '../types/api'

export function DashboardPage() {
  const [rooms, setRooms] = useState<AvailableRoom[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const search = useMemo<StaySearch>(() => {
    const checkIn = new Date()
    checkIn.setDate(checkIn.getDate() + 1)
    const checkOut = new Date(checkIn)
    checkOut.setDate(checkOut.getDate() + 1)
    return { checkIn: toIsoLocal(checkIn), checkOut: toIsoLocal(checkOut), adults: 1, children: 0 }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    hotelApi.findAvailableRooms(search, controller.signal)
      .then(setRooms)
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === 'AbortError') return
        setError(reason instanceof ApiError ? reason.message : 'Impossible de charger les disponibilités.')
      })
      .finally(() => setLoading(false))
    return () => controller.abort()
  }, [search])

  return (
    <div className="mx-auto max-w-[1500px]">
      <section className="relative overflow-hidden rounded-[2rem] bg-forest px-7 py-10 text-ivory sm:px-10 lg:px-14 lg:py-14">
        <div className="architectural-grid absolute inset-0 opacity-25" aria-hidden="true" />
        <div className="relative grid gap-10 lg:grid-cols-[1.3fr_.7fr] lg:items-end">
          <div>
            <p className="eyebrow text-gold">Le parcours essentiel</p>
            <h1 className="mt-5 max-w-3xl font-editorial text-5xl leading-[.95] tracking-[-0.035em] sm:text-6xl lg:text-7xl">
              Une réservation, orchestrée avec précision.
            </h1>
            <p className="mt-6 max-w-xl text-sm leading-7 text-ivory/60 sm:text-base">
              Vérifiez les disponibilités, associez un client existant et laissez Oracle confirmer chaque séjour en toute sécurité.
            </p>
          </div>
          <div className="flex lg:justify-end">
            <Link to="/reservations/nouvelle">
              <Button className="!bg-gold !text-forest hover:!bg-[#d3bb8e]">
                Démarrer une réservation <ArrowRight className="size-4" />
              </Button>
            </Link>
          </div>
        </div>
      </section>

      <section className="mt-7 grid gap-6 xl:grid-cols-[.8fr_1.2fr]">
        <article className="rounded-[2rem] border border-forest/8 bg-white/55 p-7 sm:p-9">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="eyebrow">Disponibilité réelle</p>
              <h2 className="mt-3 font-editorial text-3xl">Prochain séjour</h2>
              <p className="mt-2 text-sm text-forest/50">
                {formatShortDate(search.checkIn)} — {formatShortDate(search.checkOut)}, 1 adulte
              </p>
            </div>
            <span className="grid size-12 place-items-center rounded-full bg-olive/10 text-olive">
              <BedDouble className="size-5" />
            </span>
          </div>
          <div className="mt-9">
            {loading ? <LoadingState label="Consultation d’Oracle…" /> : error ? <ErrorNotice message={error} /> : (
              <>
                <div className="flex items-end gap-3">
                  <strong className="font-editorial text-7xl font-normal leading-none">{rooms.length}</strong>
                  <span className="pb-2 text-sm text-forest/50">chambre{rooms.length > 1 ? 's' : ''} compatible{rooms.length > 1 ? 's' : ''}</span>
                </div>
                <div className="mt-7 flex flex-wrap gap-2">
                  {rooms.slice(0, 5).map((room) => (
                    <span key={room.id} className="rounded-full border border-forest/10 bg-white px-3 py-1.5 text-xs font-medium">
                      {room.number} · {room.roomType}
                    </span>
                  ))}
                </div>
              </>
            )}
          </div>
        </article>

        <article className="rounded-[2rem] bg-[#e4dfd1] p-7 sm:p-9">
          <p className="eyebrow">Parcours guidé</p>
          <h2 className="mt-3 font-editorial text-3xl">De la demande à la confirmation</h2>
          <div className="mt-8 grid gap-3 sm:grid-cols-3">
            {[
              { icon: CalendarDays, number: '01', title: 'Séjour', text: 'Dates et occupants validés.' },
              { icon: UsersRound, number: '02', title: 'Attribution', text: 'Chambre et client sélectionnés.' },
              { icon: CheckCircle2, number: '03', title: 'Confirmation', text: 'Réservation créée par Oracle.' },
            ].map(({ icon: Icon, number, title, text }) => (
              <div key={number} className="rounded-3xl border border-forest/8 bg-ivory/70 p-5">
                <div className="flex items-center justify-between">
                  <Icon className="size-5 text-olive" aria-hidden="true" />
                  <span className="font-editorial text-2xl text-forest/25">{number}</span>
                </div>
                <h3 className="mt-8 font-semibold">{title}</h3>
                <p className="mt-2 text-sm leading-6 text-forest/50">{text}</p>
              </div>
            ))}
          </div>
        </article>
      </section>
    </div>
  )
}
