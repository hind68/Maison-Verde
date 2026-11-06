import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { AppLayout } from './components/layout/AppLayout'
import { BookingFlowPage } from './pages/BookingFlowPage'
import { DashboardPage } from './pages/DashboardPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ReservationDetailPage } from './pages/ReservationDetailPage'
import { ReservationsPage } from './pages/ReservationsPage'
import { RoomsPage } from './pages/RoomsPage'
import { EditReservationPage } from './pages/EditReservationPage'
import { PlanningPage } from './pages/PlanningPage'
import { HousekeepingPage } from './pages/HousekeepingPage'
import { RequireAuth, RequireRoles } from './auth/AuthContext'
import { LoginPage } from './pages/LoginPage'
import { ChangePasswordPage } from './pages/ChangePasswordPage'
import { UsersPage } from './pages/UsersPage'

const router = createBrowserRouter([
  { path: '/connexion', element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: '/changer-mot-de-passe', element: <ChangePasswordPage /> },
          { path: '/chambres', element: <RoomsPage /> },
          { path: '/housekeeping', element: <HousekeepingPage /> },
          {
            element: <RequireRoles roles={['ADMIN', 'MANAGER', 'RECEPTION']} />,
            children: [
              { path: '/', element: <DashboardPage /> },
              { path: '/planning', element: <PlanningPage /> },
              { path: '/reservations', element: <ReservationsPage /> },
              { path: '/reservations/nouvelle', element: <BookingFlowPage /> },
              { path: '/reservations/:id/modifier', element: <EditReservationPage /> },
              { path: '/reservations/:id', element: <ReservationDetailPage /> },
            ],
          },
          {
            element: <RequireRoles roles={['ADMIN']} />,
            children: [{ path: '/utilisateurs', element: <UsersPage /> }],
          },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
])

export default function App() {
  return <RouterProvider router={router} />
}
