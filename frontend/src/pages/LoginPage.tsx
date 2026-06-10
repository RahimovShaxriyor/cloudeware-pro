import { useState, useCallback, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { Boxes, Sparkles, Eye, EyeOff, Loader2, AlertCircle } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'

type DemoUser = { email: string; password: string; role: string; color: string }

const DEMO_USERS: DemoUser[] = [
  { email: 'admin@cloudware.local', password: 'admin123', role: 'Admin', color: 'from-violet-600 to-indigo-600' },
  { email: 'seller@cloudware.local', password: 'seller123', role: 'Seller', color: 'from-emerald-500 to-teal-600' },
  { email: 'warehouse@cloudware.local', password: 'warehouse123', role: 'Warehouse', color: 'from-amber-500 to-orange-600' },
]

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState(DEMO_USERS[0].email)
  const [password, setPassword] = useState(DEMO_USERS[0].password)
  const [showPass, setShowPass] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const isValid = useMemo(() => /\S+@\S+\.\S+/.test(email) && password.length >= 6, [email, password])

  const submit = useCallback(async (e: React.FormEvent) => {
    e.preventDefault()
    if (!isValid || loading) return
    setError('')
    setLoading(true)
    try {
      await login(email.trim(), password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof Error? err.message : 'Invalid credentials')
    } finally {
      setLoading(false)
    }
  }, [email, password, isValid, loading, login, navigate])

  return (
      <main className="relative min-h-screen overflow-hidden bg-[#0b0f1a] text-white">
        {/* background */}
        <div className="pointer-events-none absolute inset-0">
          <div className="absolute -top-40 -left-40 h-[480px] w-[480px] rounded-full bg-violet-700/30 blur-[120px]" />
          <div className="absolute -bottom-40 -right-40 h-[520px] w-[520px] rounded-full bg-indigo-600/20 blur-[120px]" />
          <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_rgba(255,255,255,0.06),_transparent_60%)]" />
        </div>

        <div className="relative z-10 mx-auto flex min-h-screen max-w-6xl items-center justify-center p-6">
          <motion.section
              initial={{ opacity: 0, y: 24, scale: 0.98 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              transition={{ type: 'spring', stiffness: 220, damping: 24 }}
              className="grid w-full grid-cols-1 overflow-hidden rounded-[28px] border border-white/10 bg-white/5 shadow-2xl backdrop-blur-xl lg:grid-cols-[1.1fr_0.9fr]"
          >
            {/* left brand */}
            <div className="relative hidden flex-col justify-between p-10 lg:flex">
              <div>
                <motion.div
                    animate={{ rotate: [0, 4, -4, 0] }}
                    transition={{ duration: 8, repeat: Infinity, ease: 'easeInOut' }}
                    className="mb-6 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-white/10 ring-1 ring-white/15"
                >
                  <Boxes size={26} />
                </motion.div>
                <h1 className="text-4xl font-semibold tracking-tight">CloudWare Pro</h1>
                <p className="mt-3 max-w-md text-white/70">
                  ERP / CRM / WMS для оптовой одежды. Склады, заказы, платежи — всё в одном окне.
                </p>
              </div>
              <div className="flex items-center gap-2 text-sm text-white/60">
                <Sparkles size={16} className="text-violet-300" />
                Enterprise-grade. Self-hosted.
              </div>
            </div>

            {/* right form */}
            <div className="relative bg-black/20 p-8 sm:p-10">
              <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs text-white/80">
                <Sparkles size={14} /> Secure sign-in
              </div>
              <h2 className="text-2xl font-medium">Welcome back</h2>
              <p className="mt-1 text-sm text-white/60">Use your work email to continue</p>

              <form onSubmit={submit} className="mt-8 space-y-4" noValidate>
                <div>
                  <label className="mb-1.5 block text-sm text-white/80">Email</label>
                  <input
                      value={email}
                      onChange={e => setEmail(e.target.value)}
                      type="email"
                      autoComplete="username"
                      required
                      className="w-full rounded-xl border border-white/10 bg-white/5 px-3.5 py-2.5 outline-none ring-violet-500/0 transition focus:border-violet-400/50 focus:ring-4 focus:ring-violet-500/20"
                      placeholder="you@company.com"
                  />
                </div>

                <div>
                  <label className="mb-1.5 block text-sm text-white/80">Password</label>
                  <div className="relative">
                    <input
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        type={showPass? 'text' : 'password'}
                        autoComplete="current-password"
                        required
                        className="w-full rounded-xl border border-white/10 bg-white/5 px-3.5 py-2.5 pr-10 outline-none ring-violet-500/0 transition focus:border-violet-400/50 focus:ring-4 focus:ring-violet-500/20"
                        placeholder="••••••••"
                    />
                    <button
                        type="button"
                        onClick={() => setShowPass(s =>!s)}
                        className="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg p-1.5 text-white/60 hover:bg-white/10 hover:text-white"
                        aria-label={showPass? 'Hide password' : 'Show password'}
                    >
                      {showPass? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  </div>
                </div>

                <AnimatePresence>
                  {error && (
                      <motion.div
                          initial={{ opacity: 0, y: -6 }}
                          animate={{ opacity: 1, y: 0 }}
                          exit={{ opacity: 0, y: -6 }}
                          className="flex items-center gap-2 rounded-xl border border-red-500/30 bg-red-500/10 px-3 py-2 text-sm text-red-200"
                          role="alert" aria-live="polite"
                      >
                        <AlertCircle size={16} /> {error}
                      </motion.div>
                  )}
                </AnimatePresence>

                <motion.button
                    whileTap={{ scale: 0.98 }}
                    disabled={!isValid || loading}
                    className="relative mt-2 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-violet-600 to-indigo-600 px-4 py-2.5 font-medium shadow-[0_8px_30px_rgba(99,102,241,0.35)] transition hover:from-violet-500 hover:to-indigo-500 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {loading && <Loader2 size={18} className="animate-spin" />}
                  {loading? 'Signing in…' : 'Sign in'}
                </motion.button>
              </form>

              <div className="mt-8">
                <p className="mb-2 text-xs uppercase tracking-wider text-white/50">Quick access</p>
                <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
                  {DEMO_USERS.map(u => (
                      <button
                          key={u.email}
                          type="button"
                          onClick={() => { setEmail(u.email); setPassword(u.password); setError('') }}
                          className="group relative overflow-hidden rounded-xl border border-white/10 bg-white/5 p-3 text-left transition hover:bg-white/10"
                      >
                        <div className={`absolute inset-0 opacity-0 blur-2xl transition group-hover:opacity-30 bg-gradient-to-r ${u.color}`} />
                        <div className="relative">
                          <div className="text-sm font-medium">{u.role}</div>
                          <div className="truncate text-xs text-white/60">{u.email}</div>
                        </div>
                      </button>
                  ))}
                </div>
              </div>
            </div>
          </motion.section>
        </div>
      </main>
  )
}