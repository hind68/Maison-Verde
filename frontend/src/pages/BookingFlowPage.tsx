import { ArrowLeft, ArrowRight, BedDouble, Check, ChevronLeft, ChevronRight, LoaderCircle, Search, UserPlus, UserRound, X } from 'lucide-react'
import { type FormEvent, useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { EmptyState, ErrorNotice, LoadingState } from '../components/ui/Feedback'
import { Field, Input, Select } from '../components/ui/Field'
import { ApiError, hotelApi } from '../lib/api'
import { formatDate, nightsBetween, toIsoLocal } from '../lib/format'
import type { AvailableRoom, ClientSummary, CreateClientRequest, PagedResponse, StaySearch } from '../types/api'

type Step = 1 | 2 | 3 | 4

const steps = ['Séjour', 'Chambre', 'Client', 'Récapitulatif']
const emptyClient: CreateClientRequest = { firstName: '', lastName: '', email: '', phone: '' }

function initialSearch(): StaySearch {
  const checkIn = new Date()
  checkIn.setDate(checkIn.getDate() + 1)
  const checkOut = new Date(checkIn)
  checkOut.setDate(checkOut.getDate() + 2)
  return { checkIn: toIsoLocal(checkIn), checkOut: toIsoLocal(checkOut), adults: 1, children: 0 }
}

export function BookingFlowPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState<Step>(1)
  const [search, setSearch] = useState<StaySearch>(initialSearch)
  const [rooms, setRooms] = useState<AvailableRoom[]>([])
  const [selectedRoom, setSelectedRoom] = useState<AvailableRoom | null>(null)
  const [selectedClient, setSelectedClient] = useState<ClientSummary | null>(null)
  const [roomsLoading, setRoomsLoading] = useState(false)
  const [clientsLoading, setClientsLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)
  const [clientInput, setClientInput] = useState('')
  const [clientQuery, setClientQuery] = useState('')
  const [clientPage, setClientPage] = useState(0)
  const [clients, setClients] = useState<PagedResponse<ClientSummary> | null>(null)
  const [showClientForm, setShowClientForm] = useState(false)
  const [newClient, setNewClient] = useState<CreateClientRequest>(emptyClient)
  const [creatingClient, setCreatingClient] = useState(false)
  const [clientCreated, setClientCreated] = useState(false)

  const nights = useMemo(() => nightsBetween(search.checkIn, search.checkOut), [search])

  const loadClients = useCallback((signal?: AbortSignal) => {
    setClientsLoading(true)
    setError(null)
    hotelApi.findClients(clientQuery, clientPage, 6, signal)
      .then(setClients)
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === 'AbortError') return
        setError(reason instanceof ApiError ? reason : new ApiError(0, 'UNKNOWN', 'Impossible de charger les clients.'))
      })
      .finally(() => setClientsLoading(false))
  }, [clientPage, clientQuery])

  useEffect(() => {
    if (step !== 3) return
    const controller = new AbortController()
    loadClients(controller.signal)
    return () => controller.abort()
  }, [loadClients, step])

  async function submitStay(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (nights < 1) {
      setError(new ApiError(400, 'LOCAL_VALIDATION', 'Le départ doit être postérieur à l’arrivée.'))
      return
    }
    if (search.adults < 1 || search.children < 0) {
      setError(new ApiError(400, 'LOCAL_VALIDATION', 'Indiquez au moins un adulte et un nombre d’enfants valide.'))
      return
    }
    setRoomsLoading(true)
    setSelectedRoom(null)
    setSelectedClient(null)
    try {
      const result = await hotelApi.findAvailableRooms(search)
      setRooms(result)
      setStep(2)
    } catch (reason) {
      setError(reason instanceof ApiError ? reason : new ApiError(0, 'UNKNOWN', 'La recherche a échoué.'))
    } finally {
      setRoomsLoading(false)
    }
  }

  function searchClients(event: FormEvent) {
    event.preventDefault()
    setClientPage(0)
    setClientQuery(clientInput.trim())
  }

  async function createClient(event: FormEvent) {
    event.preventDefault()
    if (creatingClient) return
    setCreatingClient(true)
    setError(null)
    setClientCreated(false)
    try {
      const client = await hotelApi.createClient(newClient)
      setSelectedClient(client)
      setClients((current) => current ? { ...current, content: [client, ...current.content.filter((item) => item.id !== client.id)], totalElements: current.totalElements + 1 } : current)
      setNewClient(emptyClient)
      setShowClientForm(false)
      setClientCreated(true)
    } catch (reason) {
      setError(reason instanceof ApiError ? reason : new ApiError(500, 'UNKNOWN', 'Le client n’a pas pu être créé.'))
    } finally {
      setCreatingClient(false)
    }
  }

  async function createReservation() {
    if (!selectedRoom || !selectedClient || submitting) return
    setSubmitting(true)
    setError(null)
    try {
      const reservation = await hotelApi.createReservation({
        clientId: selectedClient.id,
        roomId: selectedRoom.id,
        ...search,
      })
      localStorage.setItem('lastReservationId', String(reservation.id))
      navigate(`/reservations/${reservation.id}`)
    } catch (reason) {
      setError(reason instanceof ApiError ? reason : new ApiError(500, 'UNKNOWN', 'La réservation n’a pas pu être créée.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-[1380px]">
      <div className="grid gap-8 xl:grid-cols-[minmax(0,1fr)_290px]">
        <div>
          <p className="eyebrow">Nouvelle réservation</p>
          <h1 className="mt-4 font-editorial text-5xl tracking-[-0.035em] sm:text-6xl">Composer le séjour</h1>
          <p className="mt-4 max-w-2xl text-sm leading-7 text-forest/55">Chaque étape utilise les disponibilités et les clients présents dans la base Oracle.</p>

          <ol className="mt-9 grid grid-cols-4 gap-2" aria-label="Progression de la réservation">
            {steps.map((label, index) => {
              const number = (index + 1) as Step
              const active = number === step
              const complete = number < step
              return (
                <li key={label} className="min-w-0">
                  <div className={`h-1 rounded-full transition ${number <= step ? 'bg-gold' : 'bg-forest/10'}`} />
                  <div className="mt-3 flex items-center gap-2">
                    <span className={`grid size-6 shrink-0 place-items-center rounded-full text-[11px] font-bold ${active ? 'bg-forest text-ivory' : complete ? 'bg-olive text-ivory' : 'bg-forest/7 text-forest/40'}`}>
                      {complete ? <Check className="size-3.5" /> : number}
                    </span>
                    <span className={`hidden truncate text-xs font-medium sm:block ${active ? 'text-forest' : 'text-forest/40'}`}>{label}</span>
                  </div>
                </li>
              )
            })}
          </ol>

          <section className="mt-8 rounded-[2rem] border border-forest/8 bg-white/55 p-6 sm:p-9">
            {error && (
              <div className="mb-6">
                <ErrorNotice
                  message={error.message}
                  action={error.code === 'RESERVATION_CONFLICT' ? (
                    <Button type="button" variant="secondary" className="shrink-0" onClick={() => { setError(null); setStep(1) }}>
                      Nouvelle recherche
                    </Button>
                  ) : undefined}
                />
              </div>
            )}

            {step === 1 && (
              <form onSubmit={submitStay}>
                <StepHeading number="01" title="Définir le séjour" description="Renseignez les dates et la composition du groupe." />
                <div className="mt-8 grid gap-5 sm:grid-cols-2">
                  <Field label="Date d’arrivée">
                    <Input type="date" required value={search.checkIn} onChange={(event) => setSearch({ ...search, checkIn: event.target.value })} />
                  </Field>
                  <Field label="Date de départ" hint={nights > 0 ? `${nights} nuit${nights > 1 ? 's' : ''}` : undefined}>
                    <Input type="date" required min={search.checkIn} value={search.checkOut} onChange={(event) => setSearch({ ...search, checkOut: event.target.value })} />
                  </Field>
                  <Field label="Adultes">
                    <Select value={search.adults} onChange={(event) => setSearch({ ...search, adults: Number(event.target.value) })}>
                      {[1, 2, 3, 4, 5, 6].map((value) => <option key={value} value={value}>{value}</option>)}
                    </Select>
                  </Field>
                  <Field label="Enfants">
                    <Select value={search.children} onChange={(event) => setSearch({ ...search, children: Number(event.target.value) })}>
                      {[0, 1, 2, 3, 4].map((value) => <option key={value} value={value}>{value}</option>)}
                    </Select>
                  </Field>
                </div>
                <div className="mt-8 flex justify-end">
                  <Button disabled={roomsLoading}>
                    {roomsLoading ? <><LoaderCircle className="size-4 animate-spin" /> Recherche…</> : <>Voir les chambres <ArrowRight className="size-4" /></>}
                  </Button>
                </div>
              </form>
            )}

            {step === 2 && (
              <div>
                <StepHeading number="02" title="Choisir une chambre" description={`${formatDate(search.checkIn)} — ${formatDate(search.checkOut)}`} />
                {roomsLoading ? <div className="mt-8"><LoadingState /></div> : rooms.length === 0 ? (
                  <div className="mt-8">
                    <EmptyState title="Aucune chambre compatible">Modifiez les dates ou le nombre d’occupants pour lancer une nouvelle recherche.</EmptyState>
                  </div>
                ) : (
                  <div className="mt-8 grid gap-4 md:grid-cols-2">
                    {rooms.map((room) => (
                      <button
                        type="button"
                        key={room.id}
                        onClick={() => setSelectedRoom(room)}
                        className={`group rounded-3xl border p-5 text-left transition duration-200 hover:-translate-y-0.5 hover:shadow-xl ${selectedRoom?.id === room.id ? 'border-gold bg-[#f5f0e4] ring-2 ring-gold/20' : 'border-forest/10 bg-white/60 hover:border-forest/20'}`}
                      >
                        <div className="flex items-start justify-between">
                          <span className="grid size-11 place-items-center rounded-full bg-olive/10 text-olive"><BedDouble className="size-5" /></span>
                          {selectedRoom?.id === room.id && <span className="grid size-7 place-items-center rounded-full bg-forest text-ivory"><Check className="size-4" /></span>}
                        </div>
                        <p className="mt-7 text-xs uppercase tracking-[.18em] text-forest/40">Étage {room.floor}</p>
                        <h3 className="mt-1 font-editorial text-3xl">Chambre {room.number}</h3>
                        <div className="mt-4 flex items-center justify-between text-sm">
                          <span className="font-medium">{room.roomType}</span>
                          <span className="text-forest/50">jusqu’à {room.capacity} pers.</span>
                        </div>
                      </button>
                    ))}
                  </div>
                )}
                <StepActions back={() => setStep(1)} next={selectedRoom ? () => setStep(3) : undefined} />
              </div>
            )}

            {step === 3 && (
              <div>
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                  <StepHeading number="03" title="Associer un client" description="Sélectionnez un client existant ou créez sa fiche." />
                  <Button type="button" variant="secondary" onClick={() => { setShowClientForm((value) => !value); setError(null); setClientCreated(false) }}>
                    {showClientForm ? <><X className="size-4" /> Fermer</> : <><UserPlus className="size-4" /> Nouveau client</>}
                  </Button>
                </div>
                {clientCreated && selectedClient && <div className="mt-6 rounded-2xl border border-olive/20 bg-olive/8 p-4 text-sm text-olive" role="status"><strong>{selectedClient.firstName} {selectedClient.lastName}</strong> a été créé et sélectionné.</div>}
                {showClientForm && <form onSubmit={createClient} className="mt-7 rounded-3xl border border-gold/25 bg-[#f5f0e4] p-5 sm:p-6">
                  <p className="eyebrow">Nouvelle fiche client</p>
                  <div className="mt-5 grid gap-4 sm:grid-cols-2">
                    <Field label="Prénom"><Input required maxLength={100} autoComplete="given-name" value={newClient.firstName} onChange={(e) => setNewClient({ ...newClient, firstName: e.target.value })} /></Field>
                    <Field label="Nom"><Input required maxLength={100} autoComplete="family-name" value={newClient.lastName} onChange={(e) => setNewClient({ ...newClient, lastName: e.target.value })} /></Field>
                    <Field label="E-mail"><Input required type="email" maxLength={200} autoComplete="email" value={newClient.email} onChange={(e) => setNewClient({ ...newClient, email: e.target.value })} /></Field>
                    <Field label="Téléphone" hint="Facultatif"><Input type="tel" maxLength={20} autoComplete="tel" value={newClient.phone} onChange={(e) => setNewClient({ ...newClient, phone: e.target.value })} /></Field>
                  </div>
                  <div className="mt-6 flex justify-end"><Button disabled={creatingClient}>{creatingClient ? <><LoaderCircle className="size-4 animate-spin" /> Création…</> : <><UserPlus className="size-4" /> Créer et sélectionner</>}</Button></div>
                </form>}
                <form onSubmit={searchClients} className="mt-8 flex gap-3">
                  <Input value={clientInput} onChange={(event) => setClientInput(event.target.value)} placeholder="Nom, prénom ou e-mail" aria-label="Rechercher un client" />
                  <Button aria-label="Lancer la recherche"><Search className="size-4" /> <span className="hidden sm:inline">Rechercher</span></Button>
                </form>
                <div className="mt-6">
                  {clientsLoading ? <LoadingState label="Recherche des clients…" /> : !clients || clients.content.length === 0 ? (
                    <EmptyState title="Aucun client trouvé">Essayez une recherche plus large ou créez une nouvelle fiche client.</EmptyState>
                  ) : (
                    <div className="space-y-3">
                      {clients.content.map((client) => (
                        <button
                          type="button"
                          key={client.id}
                          onClick={() => setSelectedClient(client)}
                          className={`flex w-full items-center gap-4 rounded-2xl border p-4 text-left transition ${selectedClient?.id === client.id ? 'border-gold bg-[#f5f0e4] ring-2 ring-gold/20' : 'border-forest/8 bg-white/55 hover:border-forest/20'}`}
                        >
                          <span className="grid size-11 shrink-0 place-items-center rounded-full bg-olive text-sm font-semibold text-ivory">{client.firstName.charAt(0)}{client.lastName.charAt(0)}</span>
                          <span className="min-w-0 flex-1">
                            <strong className="block truncate text-sm">{client.firstName} {client.lastName}</strong>
                            <span className="mt-1 block truncate text-xs text-forest/50">{client.email}{client.phone ? ` · ${client.phone}` : ''}</span>
                          </span>
                          {selectedClient?.id === client.id && <Check className="size-5 text-olive" />}
                        </button>
                      ))}
                      {clients.totalPages > 1 && (
                        <div className="flex items-center justify-between pt-3 text-xs text-forest/50">
                          <span>{clients.totalElements} résultat{clients.totalElements > 1 ? 's' : ''}</span>
                          <div className="flex items-center gap-2">
                            <Button type="button" variant="ghost" className="!min-h-9 !px-3" disabled={clientPage === 0} onClick={() => setClientPage((value) => value - 1)}><ChevronLeft className="size-4" /> Précédent</Button>
                            <span>{clients.page + 1} / {clients.totalPages}</span>
                            <Button type="button" variant="ghost" className="!min-h-9 !px-3" disabled={clientPage + 1 >= clients.totalPages} onClick={() => setClientPage((value) => value + 1)}>Suivant <ChevronRight className="size-4" /></Button>
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
                <StepActions back={() => setStep(2)} next={selectedClient ? () => setStep(4) : undefined} />
              </div>
            )}

            {step === 4 && selectedRoom && selectedClient && (
              <div>
                <StepHeading number="04" title="Vérifier et confirmer" description="La confirmation n’apparaîtra qu’après l’enregistrement Oracle." />
                <div className="mt-8 grid gap-4 sm:grid-cols-2">
                  <SummaryCard label="Séjour" value={`${formatDate(search.checkIn)} — ${formatDate(search.checkOut)}`} detail={`${nights} nuit${nights > 1 ? 's' : ''} · ${search.adults} adulte${search.adults > 1 ? 's' : ''}${search.children ? ` · ${search.children} enfant${search.children > 1 ? 's' : ''}` : ''}`} />
                  <SummaryCard label="Chambre" value={`${selectedRoom.number} · ${selectedRoom.roomType}`} detail={`Étage ${selectedRoom.floor} · capacité ${selectedRoom.capacity}`} />
                  <SummaryCard label="Client" value={`${selectedClient.firstName} ${selectedClient.lastName}`} detail={selectedClient.email} />
                  <SummaryCard label="Statut attendu" value="Confirmation immédiate" detail="Sous réserve de disponibilité au moment de l’enregistrement" />
                </div>
                <div className="mt-9 flex flex-col-reverse gap-3 sm:flex-row sm:justify-between">
                  <Button type="button" variant="ghost" onClick={() => setStep(3)}><ArrowLeft className="size-4" /> Modifier le client</Button>
                  <Button type="button" disabled={submitting} onClick={createReservation}>
                    {submitting ? <><LoaderCircle className="size-4 animate-spin" /> Confirmation Oracle…</> : <>Confirmer la réservation <Check className="size-4" /></>}
                  </Button>
                </div>
              </div>
            )}
          </section>
        </div>

        <aside className="hidden xl:block">
          <div className="sticky top-8 rounded-[2rem] bg-forest p-7 text-ivory">
            <p className="eyebrow text-gold">Séjour en cours</p>
            <div className="mt-7 space-y-6">
              <SidebarFact icon={<span className="font-editorial text-3xl">{nights > 0 ? nights : '—'}</span>} label="Nuits" />
              <SidebarFact icon={<UserRound className="size-5" />} label={`${search.adults + search.children} occupant${search.adults + search.children > 1 ? 's' : ''}`} />
              <SidebarFact icon={<BedDouble className="size-5" />} label={selectedRoom ? `Chambre ${selectedRoom.number}` : 'Chambre à choisir'} />
            </div>
            <div className="mt-8 border-t border-white/10 pt-6 text-xs leading-6 text-ivory/45">La disponibilité finale est contrôlée à nouveau lors de la confirmation.</div>
          </div>
        </aside>
      </div>
    </div>
  )
}

function StepHeading({ number, title, description }: { number: string; title: string; description: string }) {
  return (
    <div className="flex items-start gap-4">
      <span className="font-editorial text-4xl text-gold">{number}</span>
      <div><h2 className="font-editorial text-3xl">{title}</h2><p className="mt-2 text-sm text-forest/50">{description}</p></div>
    </div>
  )
}

function StepActions({ back, next }: { back: () => void; next?: () => void }) {
  return (
    <div className="mt-9 flex justify-between gap-3">
      <Button type="button" variant="ghost" onClick={back}><ArrowLeft className="size-4" /> Retour</Button>
      <Button type="button" disabled={!next} onClick={next}>Continuer <ArrowRight className="size-4" /></Button>
    </div>
  )
}

function SummaryCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return <div className="rounded-3xl border border-forest/8 bg-white/60 p-5"><p className="eyebrow">{label}</p><p className="mt-4 font-editorial text-2xl leading-tight">{value}</p><p className="mt-2 text-xs leading-5 text-forest/50">{detail}</p></div>
}

function SidebarFact({ icon, label }: { icon: React.ReactNode; label: string }) {
  return <div className="flex items-center gap-4"><span className="grid size-11 place-items-center rounded-full border border-white/10 bg-white/5 text-gold">{icon}</span><span className="text-sm text-ivory/70">{label}</span></div>
}
