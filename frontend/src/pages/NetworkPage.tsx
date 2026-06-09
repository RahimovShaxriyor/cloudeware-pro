import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Cloud, RefreshCw, Server } from 'lucide-react'
import { request, type Row } from '../api/client'
import { Page } from '../components/ui/Page'
import { Spinner } from '../components/ui/Spinner'
import { Badge } from '../components/ui/Badge'

export function NetworkPage() {
  const [overview, setOverview] = useState<Row | null>(null)
  const [instance, setInstance] = useState<Row | null>(null)

  async function load() {
    const [o, i] = await Promise.all([
      request<Row>('/network/overview'),
      request<Row>('/network/instance'),
    ])
    setOverview(o)
    setInstance(i)
  }

  useEffect(() => { load() }, [])

  if (!overview) return <Spinner />

  const services = (overview.services as Row[]) || []

  return (
    <Page
      title="Cloud Network"
      subtitle="Microservices architecture overview"
      actions={<button type="button" className="ghost" onClick={load}><RefreshCw size={16} />Refresh</button>}
    >
      <div className="network-hero panel">
        <Cloud size={32} />
        <div>
          <h3>{String(overview.architecture)} architecture</h3>
          <p>Gateway: {String(overview.gateway)} · Database: {String(overview.database)}</p>
          <p>Current instance: <Badge value={instance?.instanceId} /> · Host: {String(instance?.hostname)}</p>
        </div>
      </div>
      <div className="service-grid">
        {services.map((svc, i) => (
          <motion.article
            key={String(svc.name)}
            className="service-card"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.08 }}
            whileHover={{ y: -6, boxShadow: '0 20px 50px rgba(0,0,0,0.35)' }}
          >
            <div className="service-icon"><Server size={22} /></div>
            <h4>{String(svc.name)}</h4>
            <p>{String(svc.path)}</p>
            <span className="service-port">Port {String(svc.port)}</span>
          </motion.article>
        ))}
      </div>
      <section className="panel arch-diagram">
        <h3>Request flow</h3>
        <div className="flow-chain">
          <span>Browser</span>
          <em>→</em>
          <span>Nginx Gateway</span>
          <em>→</em>
          <span>Microservice</span>
          <em>→</em>
          <span>PostgreSQL</span>
        </div>
      </section>
    </Page>
  )
}
