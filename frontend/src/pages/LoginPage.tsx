import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Boxes, Sparkles } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'

const demoUsers = [
  ['admin@cloudware.local', 'admin123', 'Admin'],
  ['seller@cloudware.local', 'seller123', 'Seller'],
  ['warehouse@cloudware.local', 'warehouse123', 'Warehouse'],
]

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('admin@cloudware.local')
  const [password, setPassword] = useState('admin123')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(email, password)
      navigate('/')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-page">
      <div className="login-bg" />
      <motion.section
        className="login-card"
        initial={{ opacity: 0, y: 28, scale: 0.96 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ type: 'spring', stiffness: 260, damping: 22 }}
      >
        <motion.div
          className="brand-mark"
          animate={{ rotate: [0, 3, -3, 0] }}
          transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut' }}
        >
          <Boxes size={34} />
        </motion.div>
        <div className="login-badge"><Sparkles size={14} /> Enterprise ERP Platform</div>
        <h1>CloudWare Pro</h1>
        <p>Professional ERP / CRM / WMS for wholesale clothing businesses.</p>
        <form onSubmit={submit} className="stack">
          <label>
            Email
            <input value={email} onChange={e => setEmail(e.target.value)} type="email" required autoComplete="username" />
          </label>
          <label>
            Password
            <input value={password} onChange={e => setPassword(e.target.value)} type="password" required autoComplete="current-password" />
          </label>
          {error && <div className="error">{error}</div>}
          <button className="primary login-btn" disabled={loading}>
            {loading ? 'Signing in...' : 'Sign in'}
          </button>
        </form>
        <div className="demo-users">
          <b>Quick access</b>
          {demoUsers.map(([em, pw, role]) => (
            <button
              key={em}
              type="button"
              className="demo-chip"
              onClick={() => { setEmail(em); setPassword(pw) }}
            >
              <span>{role}</span>
              <small>{em}</small>
            </button>
          ))}
        </div>
      </motion.section>
    </main>
  )
}
