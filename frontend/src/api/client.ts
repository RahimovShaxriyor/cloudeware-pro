export type Row = Record<string, unknown>

const API = '/api'

export const token = () => localStorage.getItem('cw_token')

export async function request<T = unknown>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options.headers as Record<string, string>) || {}),
  }
  const t = token()
  if (t) headers.Authorization = `Bearer ${t}`
  const response = await fetch(`${API}${path}`, { ...options, headers })
  const text = await response.text()
  const data = text ? JSON.parse(text) : null
  if (!response.ok) throw new Error(data?.error || data?.message || text || 'Request failed')
  return data
}
