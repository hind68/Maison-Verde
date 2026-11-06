import type { ButtonHTMLAttributes, ReactNode } from 'react'

type Variant = 'primary' | 'secondary' | 'ghost'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  children: ReactNode
}

const variants: Record<Variant, string> = {
  primary: 'bg-forest text-ivory hover:bg-olive shadow-[0_12px_30px_rgba(28,40,31,.18)]',
  secondary: 'border border-forest/15 bg-white/60 text-forest hover:bg-white',
  ghost: 'text-forest/70 hover:bg-forest/5 hover:text-forest',
}

export function Button({ variant = 'primary', className = '', children, ...props }: ButtonProps) {
  return (
    <button
      className={`inline-flex min-h-11 items-center justify-center gap-2 rounded-full px-5 py-2.5 text-sm font-semibold tracking-[-0.01em] transition duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-55 ${variants[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}
