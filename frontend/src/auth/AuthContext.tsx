import React, { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { request, token, type Row } from '../api/client'

type AuthCtx = {
  user: Row | null
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  refresh: () => Promise<void>
}

const AuthContext = createContext<AuthCtx | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<Row | null>(null)

  const refresh = async () => {
    if (!token()) return
    try {
      setUser(await request<Row>('/auth/me'))
    } catch {
      localStorage.removeItem('cw_token')
      setUser(null)
    }
  }

  useEffect(() => { refresh() }, [])

  const value = useMemo<AuthCtx>(() => ({
    user,
    async login(email, password) {
      const res = await request<{ token: string; user: Row }>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      })
      localStorage.setItem('cw_token', res.token)
      setUser(res.user)
    },
    async logout() {
      try { await request('/auth/logout', { method: 'POST' }) } catch { /* ignore */ }
      localStorage.removeItem('cw_token')
      setUser(null)
    },
    refresh,
  }), [user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('AuthProvider is missing')
  return ctx
}
