export interface AvailableRoom {
  id: number
  number: string
  floor: number
  status: string
  roomTypeId: number
  roomType: string
  capacity: number
}

export interface ClientSummary {
  id: number
  firstName: string
  lastName: string
  email: string
  phone: string | null
}

export interface CreateClientRequest {
  firstName: string
  lastName: string
  email: string
  phone?: string
}

export interface RoomCatalogItem {
  id: number
  number: string
  floor: number
  operationalStatus: OperationalStatus
  housekeepingStatus: HousekeepingStatus
  roomTypeId: number
  roomType: string
  capacity: number
}

export type OperationalStatus = 'en_service' | 'hors_service'
export type HousekeepingStatus = 'a_nettoyer' | 'nettoyee' | 'prete'

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CreateReservationRequest {
  clientId: number
  roomId: number
  checkIn: string
  checkOut: string
  adults: number
  children: number
}

export interface UpdateReservationRequest {
  roomId: number
  checkIn: string
  checkOut: string
  adults: number
  children: number
}

export interface Reservation {
  id: number
  checkIn: string
  checkOut: string
  guestCount: number
  status: string
  actualCheckIn: string | null
  actualCheckOut: string | null
  occupants: {
    adults: number
    children: number
  }
  client: {
    id: number
    firstName: string
    lastName: string
    email: string
  }
  room: {
    id: number
    number: string
    floor: number
    roomType: string
    capacity: number
  }
}

export interface RoomCatalogFilters {
  query: string
  status: string
  housekeepingStatus?: string
  floor?: number
  minCapacity: number
  page: number
  size: number
}

export interface ReservationFilters {
  query: string
  status: string
  checkIn?: string
  checkOut?: string
  page: number
  size: number
}

export interface Planning {
  start: string
  end: string
  rooms: PlanningRoom[]
}

export interface PlanningRoom {
  id: number
  number: string
  floor: number
  operationalStatus: OperationalStatus
  housekeepingStatus: HousekeepingStatus
  roomType: string
  capacity: number
  reservations: PlanningStay[]
}

export interface PlanningStay {
  id: number
  checkIn: string
  checkOut: string
  status: string
  guestName: string
  guestCount: number
}

export interface ExtendStayRequest {
  checkOut: string
}

export interface ApiProblemBody {
  timestamp?: string
  status?: number
  code?: string
  message?: string
  errors?: Record<string, string>
}

export type StaffRole = 'ADMIN' | 'MANAGER' | 'RECEPTION' | 'HOUSEKEEPING'

export interface AuthenticatedUser {
  id: number
  login: string
  email: string
  firstName: string
  lastName: string
  role: StaffRole
  mustChangePassword: boolean
}

export interface StaffUser extends AuthenticatedUser {
  active: boolean
}

export interface CreateStaffUserRequest {
  login: string
  email: string
  firstName: string
  lastName: string
  role: StaffRole
  initialPassword: string
}

export interface StaySearch {
  checkIn: string
  checkOut: string
  adults: number
  children: number
}
