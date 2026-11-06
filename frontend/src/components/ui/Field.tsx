import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react'

interface FieldProps {
  label: string
  hint?: string
  children: ReactNode
}

export function Field({ label, hint, children }: FieldProps) {
  return (
    <label className="grid gap-2 text-sm font-medium text-forest/75">
      <span className="flex items-center justify-between gap-3">
        {label}
        {hint && <small className="font-normal text-forest/45">{hint}</small>}
      </span>
      {children}
    </label>
  )
}

const inputClass =
  'min-h-12 w-full rounded-2xl border border-forest/12 bg-white/70 px-4 text-[15px] text-forest outline-none transition placeholder:text-forest/35 hover:border-forest/25 focus:border-gold focus:bg-white focus:ring-4 focus:ring-gold/10'

export function Input({ className = '', ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={`${inputClass} ${className}`} {...props} />
}

export function Select({ className = '', ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className={`${inputClass} ${className}`} {...props} />
}
