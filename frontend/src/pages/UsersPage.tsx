import { ChevronLeft, ChevronRight, KeyRound, LoaderCircle, Plus, Search, Shield, UserCheck, UserX, X } from 'lucide-react'
import { type FormEvent, useCallback, useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { Button } from '../components/ui/Button'
import { EmptyState, ErrorNotice, LoadingState } from '../components/ui/Feedback'
import { Field, Input, Select } from '../components/ui/Field'
import { ApiError, hotelApi } from '../lib/api'
import type { CreateStaffUserRequest, PagedResponse, StaffRole, StaffUser } from '../types/api'

const roles: StaffRole[] = ['ADMIN', 'MANAGER', 'RECEPTION', 'HOUSEKEEPING']
const roleLabels: Record<StaffRole, string> = { ADMIN: 'Administrateur', MANAGER: 'Manager', RECEPTION: 'Réception', HOUSEKEEPING: 'Housekeeping' }
const emptyUser: CreateStaffUserRequest = { login: '', email: '', firstName: '', lastName: '', role: 'RECEPTION', initialPassword: '' }

export function UsersPage() {
  const { user: currentUser } = useAuth()
  const [users, setUsers] = useState<PagedResponse<StaffUser> | null>(null)
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState<number | 'create' | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const [newUser, setNewUser] = useState<CreateStaffUserRequest>(emptyUser)
  const [toggleTarget, setToggleTarget] = useState<StaffUser | null>(null)
  const [passwordTarget, setPasswordTarget] = useState<StaffUser | null>(null)
  const [resetPassword, setResetPassword] = useState('')

  const load = useCallback((signal?: AbortSignal) => {
    setLoading(true); setError(null)
    hotelApi.findUsers(query.trim(), page, 12, signal).then(setUsers).catch((reason: unknown) => {
      if (reason instanceof DOMException && reason.name === 'AbortError') return
      setError(reason instanceof ApiError ? reason.message : 'Impossible de charger les utilisateurs.')
    }).finally(() => setLoading(false))
  }, [page, query])

  useEffect(() => { const controller = new AbortController(); load(controller.signal); return () => controller.abort() }, [load])

  async function create(event: FormEvent) {
    event.preventDefault(); setWorking('create'); setError(null)
    try {
      const created = await hotelApi.createUser(newUser)
      setSuccess(`Compte ${created.login} créé. Un changement de mot de passe sera exigé.`)
      setCreateOpen(false); setNewUser(emptyUser); load()
    } catch (reason) { setError(reason instanceof ApiError ? reason.message : 'La création a échoué.') }
    finally { setWorking(null) }
  }

  async function update(user: StaffUser, role: StaffRole, active: boolean) {
    setWorking(user.id); setError(null)
    try {
      const updated = await hotelApi.updateUser(user.id, role, active)
      setUsers((value) => value ? { ...value, content: value.content.map((item) => item.id === updated.id ? updated : item) } : value)
      setSuccess(`Compte ${updated.login} mis à jour.`); setToggleTarget(null)
    } catch (reason) { setError(reason instanceof ApiError ? reason.message : 'La mise à jour a échoué.') }
    finally { setWorking(null) }
  }

  async function reset(event: FormEvent) {
    event.preventDefault(); if (!passwordTarget) return
    setWorking(passwordTarget.id); setError(null)
    try {
      await hotelApi.resetUserPassword(passwordTarget.id, resetPassword)
      setSuccess(`Mot de passe initial renouvelé pour ${passwordTarget.login}.`)
      setPasswordTarget(null); setResetPassword('')
    } catch (reason) { setError(reason instanceof ApiError ? reason.message : 'Le mot de passe n’a pas pu être réinitialisé.') }
    finally { setWorking(null) }
  }

  return <div className="mx-auto max-w-[1380px]">
    <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between"><div><p className="eyebrow">Administration sécurisée</p><h1 className="mt-4 font-editorial text-5xl tracking-[-.035em] sm:text-6xl">Utilisateurs</h1><p className="mt-4 max-w-2xl text-sm leading-7 text-forest/55">Comptes du personnel, rôles et accès. Aucun mot de passe n’est affiché ou conservé côté navigateur.</p></div><Button onClick={() => setCreateOpen(true)}><Plus className="size-4" /> Nouvel utilisateur</Button></div>
    {error && <div className="mt-6"><ErrorNotice message={error} /></div>}{success && <div className="mt-6 rounded-2xl border border-olive/20 bg-olive/8 px-5 py-4 text-sm text-olive">{success}</div>}
    <form onSubmit={(event) => { event.preventDefault(); setPage(0); load() }} className="mt-8 flex max-w-xl gap-3"><Field label="Recherche"><Input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Nom, login ou e-mail" /></Field><div className="flex items-end"><Button><Search className="size-4" /> Rechercher</Button></div></form>
    <section className="mt-6 overflow-x-auto rounded-[2rem] border border-forest/8 bg-white/55">{loading ? <LoadingState label="Chargement des comptes…" /> : !users || users.content.length === 0 ? <EmptyState title="Aucun utilisateur">Aucun compte ne correspond à la recherche.</EmptyState> : <table className="w-full min-w-[960px] text-left"><thead className="border-b border-forest/8 text-[10px] uppercase tracking-[.16em] text-forest/40"><tr><th className="px-6 py-4">Utilisateur</th><th className="px-5 py-4">Rôle</th><th className="px-5 py-4">État</th><th className="px-5 py-4">Mot de passe</th><th className="px-6 py-4 text-right">Actions</th></tr></thead><tbody>{users.content.map((user) => <tr key={user.id} className="border-b border-forest/7 last:border-0"><td className="px-6 py-5"><strong>{user.firstName} {user.lastName}</strong><span className="mt-1 block text-xs text-forest/45">{user.login} · {user.email}</span></td><td className="px-5 py-5"><Select aria-label={`Rôle de ${user.login}`} value={user.role} disabled={working === user.id || user.id === currentUser?.id} onChange={(event) => void update(user, event.target.value as StaffRole, user.active)}>{roles.map((role) => <option key={role} value={role}>{roleLabels[role]}</option>)}</Select></td><td className="px-5 py-5"><span className={`rounded-full px-3 py-1 text-[10px] font-semibold uppercase tracking-[.12em] ${user.active ? 'bg-olive/10 text-olive' : 'bg-red-100 text-red-900'}`}>{user.active ? 'Actif' : 'Inactif'}</span></td><td className="px-5 py-5 text-xs text-forest/55">{user.mustChangePassword ? 'Changement requis' : 'Défini'}</td><td className="px-6 py-5"><div className="flex justify-end gap-2"><Button variant="ghost" disabled={working === user.id} onClick={() => setPasswordTarget(user)}><KeyRound className="size-4" /> Réinitialiser</Button><Button variant="ghost" disabled={working === user.id || user.id === currentUser?.id} onClick={() => setToggleTarget(user)}>{user.active ? <UserX className="size-4" /> : <UserCheck className="size-4" />}{user.active ? 'Désactiver' : 'Activer'}</Button></div></td></tr>)}</tbody></table>}</section>
    {users && users.totalPages > 1 && <div className="mt-6 flex items-center justify-between text-sm text-forest/55"><span>{users.totalElements} comptes</span><div className="flex items-center gap-3"><Button variant="ghost" disabled={page === 0} onClick={() => setPage((value) => value - 1)}><ChevronLeft className="size-4" /> Précédent</Button><span>{page + 1} / {users.totalPages}</span><Button variant="ghost" disabled={page + 1 >= users.totalPages} onClick={() => setPage((value) => value + 1)}>Suivant <ChevronRight className="size-4" /></Button></div></div>}
    {createOpen && <Modal title="Créer un compte" onClose={() => setCreateOpen(false)}><form onSubmit={create} className="mt-6 grid gap-4 sm:grid-cols-2"><Field label="Prénom"><Input required value={newUser.firstName} onChange={(event) => setNewUser({ ...newUser, firstName: event.target.value })} /></Field><Field label="Nom"><Input required value={newUser.lastName} onChange={(event) => setNewUser({ ...newUser, lastName: event.target.value })} /></Field><Field label="Login"><Input required value={newUser.login} onChange={(event) => setNewUser({ ...newUser, login: event.target.value })} /></Field><Field label="E-mail"><Input type="email" required value={newUser.email} onChange={(event) => setNewUser({ ...newUser, email: event.target.value })} /></Field><Field label="Rôle"><Select value={newUser.role} onChange={(event) => setNewUser({ ...newUser, role: event.target.value as StaffRole })}>{roles.map((role) => <option key={role} value={role}>{roleLabels[role]}</option>)}</Select></Field><Field label="Mot de passe initial"><Input type="password" minLength={12} maxLength={72} required value={newUser.initialPassword} onChange={(event) => setNewUser({ ...newUser, initialPassword: event.target.value })} /></Field><Button className="sm:col-span-2" disabled={working === 'create'}>{working === 'create' && <LoaderCircle className="size-4 animate-spin" />}Créer le compte</Button></form></Modal>}
    {toggleTarget && <Modal title={toggleTarget.active ? 'Désactiver ce compte ?' : 'Activer ce compte ?'} onClose={() => setToggleTarget(null)}><p className="mt-4 text-sm leading-6 text-forest/60">{toggleTarget.active ? `${toggleTarget.firstName} ne pourra plus ouvrir de nouvelle session.` : `${toggleTarget.firstName} pourra de nouveau se connecter.`}</p><div className="mt-7 flex justify-end gap-3"><Button variant="ghost" onClick={() => setToggleTarget(null)}>Fermer</Button><Button disabled={working === toggleTarget.id} onClick={() => void update(toggleTarget, toggleTarget.role, !toggleTarget.active)}>Confirmer</Button></div></Modal>}
    {passwordTarget && <Modal title="Réinitialiser le mot de passe" onClose={() => setPasswordTarget(null)}><form onSubmit={reset} className="mt-5 space-y-5"><p className="text-sm leading-6 text-forest/60">Un changement sera exigé à la prochaine connexion de {passwordTarget.firstName}.</p><Field label="Nouveau mot de passe initial"><Input type="password" minLength={12} maxLength={72} required value={resetPassword} onChange={(event) => setResetPassword(event.target.value)} /></Field><Button className="w-full" disabled={working === passwordTarget.id}>Enregistrer</Button></form></Modal>}
  </div>
}

function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  return <div className="fixed inset-0 z-[90] grid place-items-center bg-forest/55 p-5 backdrop-blur-sm"><div role="dialog" aria-modal="true" aria-label={title} className="relative max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-[2rem] bg-ivory p-7 shadow-2xl"><button type="button" aria-label="Fermer" className="absolute right-5 top-5 grid size-9 place-items-center rounded-full hover:bg-forest/8" onClick={onClose}><X className="size-4" /></button><span className="grid size-11 place-items-center rounded-full bg-gold/20"><Shield className="size-5" /></span><h2 className="mt-5 font-editorial text-3xl">{title}</h2>{children}</div></div>
}
