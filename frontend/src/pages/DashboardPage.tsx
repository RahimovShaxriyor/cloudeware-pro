import { useCallback, useEffect, useMemo, useState } from 'react'
import { motion, Variants } from 'framer-motion'
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Boxes, CheckCircle2, DollarSign, Package, RefreshCw, ShoppingCart, Users, AlertTriangle } from 'lucide-react'
import { request } from '../api/client'
import { Page } from '../components/ui/Page'
import { Table } from '../components/ui/Table'
import { Spinner } from '../components/ui/Spinner'
import { money } from '../utils/format'

// --- Types (под твой API) ---
type Summary = {
  revenue: number
  activeOrders: number
  customers: number
  products: number
  lowStock: number
  instanceId: string
}
type Order = { orderNumber: string; customerName: string; status: string; totalAmount: number }
type LowStock = { productName: string; sku: string; warehouseName: string; availableQuantity: number; minimumStock: number }
type Activity = { module: string; action: string; description: string; createdAt: string }
type ChartPoint = { date: string; revenue: number }

// --- Animations ---
const containerVariants: Variants = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.06 } }
}
const itemVariants: Variants = {
  hidden: { opacity: 0, y: 12 },
  show: { opacity: 1, y: 0 }
}

export function DashboardPage() {
  const [summary, setSummary] = useState<Summary | null>(null)
  const [orders, setOrders] = useState<Order[]>([])
  const [low, setLow] = useState<LowStock[]>([])
  const [activity, setActivity] = useState<Activity[]>([])
  const [chart, setChart] = useState<ChartPoint[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const [s, o, l, a, c] = await Promise.all([
        request<Summary>('/dashboard/summary'),
        request<Order[]>('/dashboard/recent-orders'),
        request<LowStock[]>('/dashboard/low-stock'),
        request<Activity[]>('/dashboard/latest-activity'),
        request<ChartPoint[]>('/dashboard/sales-chart'),
      ])
      setSummary(s)
      setOrders(o)
      setLow(l)
      setActivity(a)
      setChart([...c].reverse())
    } catch (e: unknown) {
      const msg = e instanceof Error? e.message : 'Failed to load dashboard'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const cards = useMemo(() => {
    if (!summary) return []
    return [
      { label: 'Revenue', value: money(summary.revenue), icon: DollarSign, color: '#38bdf8' },
      { label: 'Active orders', value: summary.activeOrders.toLocaleString(), icon: ShoppingCart, color: '#a78bfa' },
      { label: 'Customers', value: summary.customers.toLocaleString(), icon: Users, color: '#34d399' },
      { label: 'Products', value: summary.products.toLocaleString(), icon: Package, color: '#fbbf24' },
      { label: 'Low stock', value: summary.lowStock.toLocaleString(), icon: Boxes, color: '#fb7185' },
      { label: 'Instance', value: summary.instanceId, icon: CheckCircle2, color: '#22d3ee' },
    ]
  }, [summary])

  // Форматируем данные под твой Table (он ждет string[])
  const ordersRows = useMemo(() => orders.map(o => ({
    ...o,
    totalAmount: money(o.totalAmount),
  })), [orders])

  const activityRows = useMemo(() => activity.map(a => ({
    ...a,
    createdAt: new Date(a.createdAt).toLocaleString(),
  })), [activity])

  if (loading &&!summary) {
    return <div className="page-center"><Spinner /></div>
  }

  if (error) {
    return (
        <div className="page-center error">
          <AlertTriangle size={20} /> {error}
        </div>
    )
  }

  return (
      <Page
          title="Dashboard"
          subtitle="Real-time business overview"
          actions={
            <button type="button" className="btn ghost" onClick={load} disabled={loading} aria-label="Refresh">
              <RefreshCw size={16} className={loading? 'spin' : ''} />
              <span>Refresh</span>
            </button>
          }
      >
        {/* KPI */}
        <motion.div className="cards grid-6" variants={containerVariants} initial="hidden" animate="show">
          {cards.map((card) => (
              <motion.article key={card.label} className="card stat-card" variants={itemVariants} whileHover={{ y: -4 }}>
                <div className="stat-icon" style={{ background: `${card.color}22`, color: card.color }}>
                  <card.icon size={20} />
                </div>
                <span>{card.label}</span>
                <strong>{card.value}</strong>
              </motion.article>
          ))}
        </motion.div>

        <div className="grid two">
          <section className="panel chart-panel">
            <h3>Sales trend</h3>
            <div className="chart-wrap">
              <ResponsiveContainer width="100%" height={260}>
                <AreaChart data={chart}>
                  <defs>
                    <linearGradient id="revenueGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#38bdf8" stopOpacity={0.45} />
                      <stop offset="100%" stopColor="#38bdf8" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
                  <XAxis dataKey="date" tick={{ fill: '#93a4bf', fontSize: 12 }} tickFormatter={(v: string) => String(v).slice(5)} />
                  <YAxis tick={{ fill: '#93a4bf', fontSize: 12 }} />
                  <Tooltip contentStyle={{ background: '#11182d', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 12 }} />
                  <Area type="monotone" dataKey="revenue" stroke="#38bdf8" fill="url(#revenueGrad)" strokeWidth={2} />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </section>

          <section className="panel">
            <h3>Recent orders</h3>
            <Table rows={ordersRows} columns={['orderNumber', 'customerName', 'status', 'totalAmount']} />
          </section>
        </div>

        <div className="grid two">
          <section className="panel">
            <h3>Low stock alerts</h3>
            <Table rows={low} columns={['productName', 'sku', 'warehouseName', 'availableQuantity', 'minimumStock']} />
          </section>
          <section className="panel">
            <h3>Latest activity</h3>
            <Table rows={activityRows} columns={['module', 'action', 'description', 'createdAt']} />
          </section>
        </div>
      </Page>
  )
}