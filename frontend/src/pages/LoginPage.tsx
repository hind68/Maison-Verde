import { ArrowRight, KeyRound, LoaderCircle, ShieldCheck } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { PublicOnly, useAuth } from '../auth/AuthContext'
import { Button } from '../components/ui/Button'
import { ErrorNotice } from '../components/ui/Feedback'
import { Field, Input } from '../components/ui/Field'
import { ApiError } from '../lib/api'

export function LoginPage() {
  return <PublicOnly><LoginForm /></PublicOnly>
}

function LoginForm() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const message = (location.state as { message?: string } | null)?.message

  async function submit(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const user = await login(identifier, password)
      const requested = (location.state as { from?: string } | null)?.from
      const destination = user.mustChangePassword
        ? '/changer-mot-de-passe'
        : user.role === 'HOUSEKEEPING' ? '/housekeeping' : requested ?? '/'
      navigate(destination, { replace: true })
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'La connexion a échoué.')
    } finally {
      setLoading(false)
    }
  }

  return <main className="grid min-h-screen bg-ivory text-forest lg:grid-cols-[1.05fr_.95fr]">
    <section className="relative hidden overflow-hidden bg-forest p-14 text-ivory lg:flex lg:flex-col lg:justify-between">
      <div className="absolute inset-0 opacity-20 [background-image:radial-gradient(circle_at_20%_15%,#c6ab78_0,transparent_28%),linear-gradient(135deg,transparent_45%,#384638_46%,transparent_47%)]" />
      <div className="relative"><p className="font-editorial text-3xl">Maison Verde</p><p className="mt-2 text-[10px] uppercase tracking-[.3em] text-gold">Front office privé</p></div>
      <div className="relative max-w-xl"><p className="eyebrow !text-gold">Hospitalité & précision</p><h1 className="mt-5 font-editorial text-7xl leading-[.95] tracking-[-.04em]">L’accueil commence par un espace sûr.</h1><p className="mt-8 max-w-md text-sm leading-7 text-ivory/55">Accès réservé aux équipes autorisées de la réception, de la direction et du housekeeping.</p></div>
      <div className="relative flex items-center gap-3 text-xs text-ivory/45"><ShieldCheck className="size-4 text-gold" /> Session sécurisée et permissions contrôlées</div>
    </section>
    <section className="flex items-center justify-center p-6 sm:p-12">
      <div className="w-full max-w-md">
        <span className="grid size-14 place-items-center rounded-full bg-olive/10 text-olive"><KeyRound className="size-6" /></span>
        <p className="eyebrow mt-8">Espace personnel</p><h2 className="mt-4 font-editorial text-5xl tracking-[-.035em]">Connexion</h2><p className="mt-4 text-sm leading-7 text-forest/55">Utilisez votre identifiant professionnel ou votre adresse e-mail.</p>
        {message && <div className="mt-6 rounded-2xl border border-olive/20 bg-olive/8 px-5 py-4 text-sm text-olive">{message}</div>}
        {error && <div className="mt-6"><ErrorNotice message={error} /></div>}
        <form onSubmit={submit} className="mt-8 space-y-5">
          <Field label="Identifiant ou e-mail"><Input autoComplete="username" required value={identifier} onChange={(event) => setIdentifier(event.target.value)} /></Field>
          <Field label="Mot de passe"><Input type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} /></Field>
          <Button className="w-full" disabled={loading}>{loading ? <LoaderCircle className="size-4 animate-spin" /> : <ArrowRight className="size-4" />}{loading ? 'Connexion…' : 'Se connecter'}</Button>
        </form>
        <p className="mt-8 text-center text-xs leading-6 text-forest/40">Les tentatives répétées sont temporairement bloquées. Contactez un administrateur pour l’ouverture d’un compte.</p>
      </div>
    </section>
  </main>
}
