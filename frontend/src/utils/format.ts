import type { Row } from '../api/client'

export function money(value: unknown) {
  const n = Number(value || 0)
  return '$' + n.toLocaleString(undefined, { maximumFractionDigits: 2 })
}

export function format(value: unknown) {
  if (value === null || value === undefined || value === '') return '—'
  if (typeof value === 'boolean') return value ? 'Yes' : 'No'
  if (typeof value === 'number') return value.toLocaleString()
  const s = String(value)
  if (s.includes('T') && s.includes(':')) return s.slice(0, 16).replace('T', ' ')
  return s
}

export function statusClass(value: unknown) {
  const v = String(value || '').toLowerCase()
  if (['active', 'paid', 'delivered', 'confirmed', 'true', 'up'].includes(v)) return 'good'
  if (['pending', 'partial', 'packing', 'shipped', 'new', 'draft'].includes(v)) return 'warn'
  if (['cancelled', 'failed', 'returned', 'false', 'low'].includes(v)) return 'bad'
  return 'neutral'
}

export function label(key: string) {
  return key.replace(/([A-Z])/g, ' $1').replace(/^./, s => s.toUpperCase())
}

export function hasPermission(user: Row | null, permission: string) {
  if (!user) return false
  if (user.role === 'ADMIN') return true
  const perms = user.permissions as string[] | undefined
  return perms?.includes(permission) ?? false
}
