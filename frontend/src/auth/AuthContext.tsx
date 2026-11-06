import { createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { ApiError, hotelApi } from '../lib/api'
import type { AuthenticatedUser, StaffRole } from '../types/api'
import { LoadingState } from '../components/ui/Feedback'

interface AuthContextValue {
  user: AuthenticatedUser | null
  loading: boolean
  login: (identifier: string, password: string) => Promise<AuthenticatedUser>
  logout: () => Promise<void>
  clearSession: () => void
  hasRole: (...roles: StaffRole[]) => boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthenticatedUser | null>(null)
  const [loading, setLoading] = useState(true)

  const clearSession = useCallback(() => setUser(null), [])

  useEffect(() => {
    const controller = new AbortController()
    hotelApi.getCurrentUser(controller.signal)
      .then(setUser)
      .catch((reason: unknown) => {
        if (!(reason instanceof DOMException && reason.name === 'AbortError')) setUser(null)
      })
      .finally(() => setLoading(false))
    const expired = () => setUser(null)
    window.addEventListener('auth:unauthorized', expired)
    return () => {
      controller.abort()
      window.removeEventListener('auth:unauthorized', expired)
    }
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    user,
    loading,
    login: async (identifier, password) => {
      const authenticated = await hotelApi.login(identifier, password)
      setUser(authenticated)
      return authenticated
    },
    logout: async () => {
      try { await hotelApi.logout() } finally { setUser(null) }
    },
    clearSession,
    hasRole: (...roles) => user !== null && roles.includes(user.role),
  }), [clearSession, loading, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth doit être utilisé dans AuthProvider.')
  return context
}

export function RequireAuth() {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <div className="min-h-screen bg-ivory p-10"><LoadingState label="Vérification de la session…" /></div>
  if (!user) return <Navigate to="/connexion" replace state={{ from: location.pathname }} />
  if (user.mustChangePassword && location.pathname !== '/changer-mot-de-passe') {
    return <Navigate to="/changer-mot-de-passe" replace />
  }
  return <Outlet />
}

export function RequireRoles({ roles }: { roles: StaffRole[] }) {
  const { user } = useAuth()
  if (!user || !roles.includes(user.role)) {
    return <Navigate to={user?.role === 'HOUSEKEEPING' ? '/housekeeping' : '/'} replace />
  }
  return <Outlet />
}

export function PublicOnly({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="min-h-screen bg-ivory p-10"><LoadingState label="Vérification de la session…" /></div>
  if (user) return <Navigate to={user.mustChangePassword ? '/changer-mot-de-passe' : user.role === 'HOUSEKEEPING' ? '/housekeeping' : '/'} replace />
  return children
}
