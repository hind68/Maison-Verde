import { Link } from 'react-router-dom'
import { Button } from '../components/ui/Button'

export function NotFoundPage() {
  return <div className="grid min-h-[60vh] place-items-center text-center"><div><p className="font-editorial text-8xl text-gold">404</p><h1 className="mt-4 font-editorial text-4xl">Page introuvable</h1><p className="mt-3 text-sm text-forest/50">Cette adresse ne correspond à aucune page disponible.</p><Link to="/" className="mt-7 inline-block"><Button>Retour à l’accueil</Button></Link></div></div>
}
