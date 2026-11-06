import { KeyRound, LoaderCircle } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Button } from '../components/ui/Button'
import { ErrorNotice } from '../components/ui/Feedback'
import { Field, Input } from '../components/ui/Field'
import { ApiError, hotelApi } from '../lib/api'

export function ChangePasswordPage() {
  const { clearSession } = useAuth()
  const navigate = useNavigate()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (newPassword !== confirmation) { setError('La confirmation du nouveau mot de passe ne correspond pas.'); return }
    setLoading(true); setError(null)
    try {
      await hotelApi.changePassword(currentPassword, newPassword)
      clearSession()
      navigate('/connexion', { replace: true, state: { message: 'Mot de passe modifié. Reconnectez-vous avec vos nouveaux identifiants.' } })
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'Le mot de passe n’a pas pu être modifié.')
    } finally { setLoading(false) }
  }

  return <div className="mx-auto max-w-xl"><span className="grid size-12 place-items-center rounded-full bg-gold/20"><KeyRound className="size-5" /></span><p className="eyebrow mt-7">Sécurité du compte</p><h1 className="mt-4 font-editorial text-5xl">Changer le mot de passe</h1><p className="mt-4 text-sm leading-7 text-forest/55">Un mot de passe initial doit être remplacé avant d’accéder aux modules opérationnels. Utilisez au moins 12 caractères.</p>{error && <div className="mt-6"><ErrorNotice message={error} /></div>}<form onSubmit={submit} className="mt-8 space-y-5 rounded-[2rem] border border-forest/8 bg-white/55 p-7"><Field label="Mot de passe actuel"><Input type="password" autoComplete="current-password" required value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} /></Field><Field label="Nouveau mot de passe"><Input type="password" minLength={12} maxLength={72} autoComplete="new-password" required value={newPassword} onChange={(event) => setNewPassword(event.target.value)} /></Field><Field label="Confirmer"><Input type="password" minLength={12} maxLength={72} autoComplete="new-password" required value={confirmation} onChange={(event) => setConfirmation(event.target.value)} /></Field><Button className="w-full" disabled={loading}>{loading && <LoaderCircle className="size-4 animate-spin" />}{loading ? 'Modification…' : 'Enregistrer et se reconnecter'}</Button></form></div>
}
