import type {
  ApiProblemBody,
  AuthenticatedUser,
  AvailableRoom,
  ClientSummary,
  CreateClientRequest,
  CreateReservationRequest,
  CreateStaffUserRequest,
  ExtendStayRequest,
  PagedResponse,
  Planning,
  Reservation,
  ReservationFilters,
  RoomCatalogFilters,
  RoomCatalogItem,
  StaffRole,
  StaffUser,
  StaySearch,
  UpdateReservationRequest,
} from '../types/api'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api/v1').replace(/\/$/, '')
let csrfToken: string | null = null

async function ensureCsrf(): Promise<string> {
  if (csrfToken) return csrfToken
  const response = await fetch(`${API_BASE_URL}/auth/csrf`, {
    credentials: 'include',
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) throw new ApiError(response.status, 'CSRF_ERROR', 'La session sécurisée n’a pas pu être initialisée.')
  const body = (await response.json()) as { token: string }
  csrfToken = body.token
  return body.token
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly fields: Record<string, string> = {},
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    const method = (init?.method ?? 'GET').toUpperCase()
    const token = ['POST', 'PATCH', 'PUT', 'DELETE'].includes(method) ? await ensureCsrf() : null
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      credentials: 'include',
      headers: {
        Accept: 'application/json',
        ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { 'X-XSRF-TOKEN': token } : {}),
        ...init?.headers,
      },
    })
  } catch (reason) {
    if (reason instanceof DOMException && reason.name === 'AbortError') {
      throw reason
    }
    throw new ApiError(0, 'NETWORK_ERROR', "Impossible de joindre le serveur. Vérifiez que l'API est démarrée.")
  }

  if (!response.ok) {
    let body: ApiProblemBody = {}
    try {
      body = (await response.json()) as ApiProblemBody
    } catch {
      // Le statut HTTP reste la source fiable si le corps n'est pas JSON.
    }
    if (response.status === 401 && path !== '/auth/login') {
      csrfToken = null
      window.dispatchEvent(new Event('auth:unauthorized'))
    }
    throw new ApiError(
      response.status,
      body.code ?? 'HTTP_ERROR',
      body.message ?? messageForStatus(response.status),
      body.errors ?? {},
    )
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

function messageForStatus(status: number): string {
  if (status === 401) return 'Votre session a expiré. Veuillez vous reconnecter.'
  if (status === 403) return 'Vous n’avez pas l’autorisation nécessaire.'
  if (status === 400) return 'Certaines informations sont invalides.'
  if (status === 404) return "L'élément demandé est introuvable."
  if (status === 409) return "La chambre n'est plus disponible pour ces dates."
  return 'Une erreur inattendue est survenue. Réessayez dans un instant.'
}

function params(values: object): string {
  const search = new URLSearchParams()
  Object.entries(values as Record<string, string | number | undefined>).forEach(([key, value]) => {
    if (value !== undefined && value !== '') search.set(key, String(value))
  })
  return search.toString()
}

export const hotelApi = {
  getCurrentUser(signal?: AbortSignal) {
    return request<AuthenticatedUser>('/auth/me', { signal })
  },

  login(identifier: string, password: string) {
    return request<AuthenticatedUser>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ identifier, password }),
    })
  },

  async logout() {
    await request<void>('/auth/logout', { method: 'POST' })
    csrfToken = null
  },

  async changePassword(currentPassword: string, newPassword: string) {
    await request<void>('/auth/change-password', {
      method: 'POST',
      body: JSON.stringify({ currentPassword, newPassword }),
    })
    csrfToken = null
  },

  findUsers(query: string, page: number, size = 20, signal?: AbortSignal) {
    return request<PagedResponse<StaffUser>>(`/users?${params({ query, page, size })}`, { signal })
  },

  createUser(payload: CreateStaffUserRequest) {
    return request<StaffUser>('/users', { method: 'POST', body: JSON.stringify(payload) })
  },

  updateUser(id: number, role: StaffRole, active: boolean) {
    return request<StaffUser>(`/users/${id}`, {
      method: 'PATCH', body: JSON.stringify({ role, active }),
    })
  },

  resetUserPassword(id: number, password: string) {
    return request<StaffUser>(`/users/${id}/password`, {
      method: 'PATCH', body: JSON.stringify({ password }),
    })
  },
  findAvailableRooms(search: StaySearch, signal?: AbortSignal, excludeReservationId?: number) {
    return request<AvailableRoom[]>(`/rooms/availability?${params({
      checkIn: search.checkIn,
      checkOut: search.checkOut,
      adults: search.adults,
      children: search.children,
      excludeReservationId,
    })}`, { signal })
  },

  findClients(query: string, page: number, size = 6, signal?: AbortSignal) {
    return request<PagedResponse<ClientSummary>>(
      `/clients?${params({ query: query.trim(), page, size })}`,
      { signal },
    )
  },

  createClient(payload: CreateClientRequest) {
    return request<ClientSummary>('/clients', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  findRooms(filters: RoomCatalogFilters, signal?: AbortSignal) {
    return request<PagedResponse<RoomCatalogItem>>(`/rooms?${params(filters)}`, { signal })
  },

  updateHousekeeping(id: number, status: RoomCatalogItem['housekeepingStatus']) {
    return request<RoomCatalogItem>(`/rooms/${id}/housekeeping`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    })
  },

  updateOperationalStatus(id: number, status: RoomCatalogItem['operationalStatus']) {
    return request<RoomCatalogItem>(`/rooms/${id}/operational-status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    })
  },

  findReservations(filters: ReservationFilters, signal?: AbortSignal) {
    return request<PagedResponse<Reservation>>(`/reservations?${params(filters)}`, { signal })
  },

  createReservation(payload: CreateReservationRequest) {
    return request<Reservation>('/reservations', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  getReservation(id: number, signal?: AbortSignal) {
    return request<Reservation>(`/reservations/${id}`, { signal })
  },

  updateReservation(id: number, payload: UpdateReservationRequest) {
    return request<Reservation>(`/reservations/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    })
  },

  cancelReservation(id: number) {
    return request<Reservation>(`/reservations/${id}/cancel`, { method: 'POST' })
  },

  getPlanning(start: string, end: string, signal?: AbortSignal) {
    return request<Planning>(`/planning?${params({ start, end })}`, { signal })
  },

  checkInReservation(id: number) {
    return request<Reservation>(`/reservations/${id}/check-in`, { method: 'POST' })
  },

  checkOutReservation(id: number) {
    return request<Reservation>(`/reservations/${id}/check-out`, { method: 'POST' })
  },

  extendReservation(id: number, payload: ExtendStayRequest) {
    return request<Reservation>(`/reservations/${id}/extend`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    })
  },
}
