import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Boxes, CheckCircle2, DollarSign, Package, RefreshCw, ShoppingCart, Users } from 'lucide-react'
import { request, type Row } from '../api/client'
import { Page } from '../components/ui/Page'
import { Table } from '../components/ui/Table'
import { Spinner } from '../components/ui/Spinner'
import { money } from '../utils/format'

export function DashboardPage() {
  const [summary, setSummary] = useState<Row | null>(null)
  const [orders, setOrders] = useState<Row[]>([])
  const [low, setLow] = useState<Row[]>([])
  const [activity, setActivity] = useState<Row[]>([])
  const [chart, setChart] = useState<Row[]>([])

  async function load() {
    const [s, o, l, a, c] = await Promise.all([
      request<Row>('/dashboard/summary'),
      request<Row[]>('/dashboard/recent-orders'),
      request<Row[]>('/dashboard/low-stock'),
      request<Row[]>('/dashboard/latest-activity'),
      request<Row[]>('/dashboard/sales-chart'),
    ])
    setSummary(s)
    setOrders(o)
    setLow(l)
    setActivity(a)
    setChart([...c].reverse())
  }

  useEffect(() => { load() }, [])

  if (!summary) return <Spinner />

  const cards = [
    { label: 'Revenue', value: money(summary.revenue), icon: DollarSign, color: '#38bdf8' },
    { label: 'Active orders', value: String(summary.activeOrders), icon: ShoppingCart, color: '#a78bfa' },
    { label: 'Customers', value: String(summary.customers), icon: Users, color: '#34d399' },
    { label: 'Products', value: String(summary.products), icon: Package, color: '#fbbf24' },
    { label: 'Low stock', value: String(summary.lowStock), icon: Boxes, color: '#fb7185' },
    { label: 'Service', value: String(summary.instanceId), icon: CheckCircle2, color: '#22d3ee' },
  ]

  return (
    <Page title="Dashboard" subtitle="Real-time business overview from microservices" actions={
      <button type="button" className="ghost" onClick={load}><RefreshCw size={16} />Refresh</button>
    }>
      <div className="cards">
        {cards.map((card, i) => (
          <motion.div
            key={card.label}
            className="card stat-card"
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.06 }}
            whileHover={{ y: -4, transition: { duration: 0.2 } }}
          >
            <div className="stat-icon" style={{ background: `${card.color}22`, color: card.color }}>
              <card.icon size={20} />
            </div>
            <span>{card.label}</span>
            <strong>{card.value}</strong>
          </motion.div>
        ))}
      </div>
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
                <XAxis dataKey="date" tick={{ fill: '#93a4bf', fontSize: 12 }} tickFormatter={v => String(v).slice(5)} />
                <YAxis tick={{ fill: '#93a4bf', fontSize: 12 }} />
                <Tooltip contentStyle={{ background: '#11182d', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 12 }} />
                <Area type="monotone" dataKey="revenue" stroke="#38bdf8" fill="url(#revenueGrad)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </section>
        <section className="panel">
          <h3>Recent orders</h3>
          <Table rows={orders} columns={['orderNumber', 'customerName', 'status', 'totalAmount']} />
        </section>
      </div>
      <div className="grid two">
        <section className="panel">
          <h3>Low stock alerts</h3>
          <Table rows={low} columns={['productName', 'sku', 'warehouseName', 'availableQuantity', 'minimumStock']} />
        </section>
        <section className="panel">
          <h3>Latest activity</h3>
          <Table rows={activity} columns={['module', 'action', 'description', 'createdAt']} />
        </section>
      </div>
    </Page>
  )
}
