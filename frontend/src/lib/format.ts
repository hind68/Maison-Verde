const frenchDate = new Intl.DateTimeFormat('fr-FR', {
  day: '2-digit',
  month: 'long',
  year: 'numeric',
})

export function formatDate(value: string): string {
  return frenchDate.format(new Date(`${value}T12:00:00`))
}

export function formatShortDate(value: string): string {
  return new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: 'short' }).format(
    new Date(`${value}T12:00:00`),
  )
}

export function nightsBetween(checkIn: string, checkOut: string): number {
  const start = new Date(`${checkIn}T00:00:00`).getTime()
  const end = new Date(`${checkOut}T00:00:00`).getTime()
  return Math.round((end - start) / 86_400_000)
}

export function toIsoLocal(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function statusLabel(status: string): string {
  const labels: Record<string, string> = {
    confirmée: 'Confirmée',
    en_attente: 'En attente',
    en_cours: 'En cours',
    terminée: 'Terminée',
    annulée: 'Annulée',
    disponible: 'Disponible',
  }
  return labels[status] ?? status
}

export function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(new Date(value))
}
