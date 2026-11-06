import { BedDouble, BookOpen, CalendarDays, CalendarRange, House, LogOut, Menu, Plus, Sparkles, Users, X } from 'lucide-react'
import { useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import type { StaffRole } from '../../types/api'

const operationalRoles: StaffRole[] = ['ADMIN', 'MANAGER', 'RECEPTION']
const navigation = [
  { to: '/', label: 'Accueil', icon: House, end: true, roles: operationalRoles },
  { to: '/chambres', label: 'Chambres', icon: BedDouble, end: true, roles: ['ADMIN', 'MANAGER', 'RECEPTION', 'HOUSEKEEPING'] as StaffRole[] },
  { to: '/housekeeping', label: 'Housekeeping', icon: Sparkles, end: true, roles: ['ADMIN', 'MANAGER', 'RECEPTION', 'HOUSEKEEPING'] as StaffRole[] },
  { to: '/planning', label: 'Planning', icon: CalendarDays, end: true, roles: operationalRoles },
  { to: '/reservations', label: 'Réservations', icon: BookOpen, end: true, roles: operationalRoles },
  { to: '/reservations/nouvelle', label: 'Nouvelle réservation', icon: Plus, end: false, roles: operationalRoles },
  { to: '/utilisateurs', label: 'Utilisateurs', icon: Users, end: true, roles: ['ADMIN'] as StaffRole[] },
]

export function AppLayout() {
  const [open, setOpen] = useState(false)
  const { user, logout } = useAuth()
  const location = useLocation()
  const today = new Intl.DateTimeFormat('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(new Date())

  return (
    <div className="min-h-screen bg-ivory text-forest">
      <button
        type="button"
        className="fixed left-4 top-4 z-50 grid size-11 place-items-center rounded-full bg-forest text-ivory shadow-xl lg:hidden"
        onClick={() => setOpen((value) => !value)}
        aria-label={open ? 'Fermer la navigation' : 'Ouvrir la navigation'}
        aria-expanded={open}
      >
        {open ? <X className="size-5" /> : <Menu className="size-5" />}
      </button>

      {open && <button className="fixed inset-0 z-30 bg-forest/40 backdrop-blur-sm lg:hidden" aria-label="Fermer la navigation" onClick={() => setOpen(false)} />}

      <aside className={`fixed inset-y-0 left-0 z-40 flex w-[280px] flex-col bg-forest px-6 py-7 text-ivory transition-transform duration-300 lg:translate-x-0 ${open ? 'translate-x-0' : '-translate-x-full'}`}>
        <div className="flex items-center gap-3 border-b border-white/10 pb-7">
          <div className="grid size-11 place-items-center rounded-full border border-gold/50 bg-white/5">
            <CalendarRange className="size-5 text-gold" aria-hidden="true" />
          </div>
          <div>
            <p className="font-editorial text-2xl leading-none">Maison Verde</p>
            <p className="mt-1 text-[10px] uppercase tracking-[0.28em] text-ivory/45">Réception privée</p>
          </div>
        </div>

        <nav className="mt-9 space-y-2" aria-label="Navigation principale">
          {navigation.filter((item) => user && item.roles.includes(user.role)).map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              onClick={() => setOpen(false)}
              className={({ isActive }) => `group flex items-center gap-3 rounded-2xl px-4 py-3.5 text-sm transition ${isActive ? 'bg-ivory text-forest shadow-lg' : 'text-ivory/65 hover:bg-white/7 hover:text-ivory'}`}
            >
              <Icon className="size-[18px]" aria-hidden="true" />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="mt-auto rounded-3xl border border-white/10 bg-white/[0.04] p-5">
          <p className="text-[10px] uppercase tracking-[0.24em] text-gold">Session sécurisée</p>
          <p className="mt-3 text-sm font-semibold text-ivory">{user?.firstName} {user?.lastName}</p><p className="mt-1 text-xs text-ivory/45">{user?.role}</p>
          <button type="button" className="mt-4 flex items-center gap-2 text-xs text-ivory/60 transition hover:text-ivory" onClick={() => void logout()}><LogOut className="size-4" /> Se déconnecter</button>
        </div>
      </aside>

      <div className="lg:pl-[280px]">
        <header className="flex min-h-20 items-center justify-between border-b border-forest/8 px-5 pl-20 sm:px-8 sm:pl-20 lg:px-12">
          <div>
            <p className="text-[10px] font-semibold uppercase tracking-[0.24em] text-forest/40">Gestion hôtelière</p>
            <p className="mt-1 text-sm font-medium capitalize text-forest/75">{today}</p>
          </div>
          <div className="flex items-center gap-3">
            <span className="hidden text-right sm:block">
              <span className="block text-sm font-semibold">{user?.firstName} {user?.lastName}</span>
              <span className="block text-xs text-forest/45">{user?.role}</span>
            </span>
            <span className="grid size-10 place-items-center rounded-full bg-olive font-editorial text-lg text-ivory" aria-hidden="true">{user?.firstName?.charAt(0) ?? 'M'}</span>
          </div>
        </header>
        <main key={location.pathname} className="page-enter min-h-[calc(100vh-5rem)] px-5 py-8 sm:px-8 lg:px-12 lg:py-11">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
