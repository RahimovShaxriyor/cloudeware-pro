import { useEffect, useState } from 'react'
import { Download, RefreshCw } from 'lucide-react'
import { request, token, type Row } from '../api/client'
import { Page } from '../components/ui/Page'
import { Table } from '../components/ui/Table'
import { label } from '../utils/format'

const tabs = ['sales', 'revenue', 'inventory', 'customers', 'orders', 'profit'] as const

export function ReportsPage() {
  const [tab, setTab] = useState<(typeof tabs)[number]>('sales')
  const [rows, setRows] = useState<Row[]>([])
  const [filters, setFilters] = useState<Row>({ dateFrom: '', dateTo: '', status: '', warehouseId: '', customerId: '', category: '' })

  async function load(next = tab) {
    const qs = new URLSearchParams(
      Object.entries(filters).filter(([, v]) => v !== '').map(([k, v]) => [k, String(v)])
    ).toString()
    setRows(await request<Row[]>(`/reports/${next}${qs ? '?' + qs : ''}`))
  }

  useEffect(() => { load() }, [tab])

  async function exportCsv() {
    const qs = new URLSearchParams(
      Object.entries(filters).filter(([, v]) => v !== '').map(([k, v]) => [k, String(v)])
    ).toString()
    const path = tab === 'inventory' ? '/reports/export/inventory' : '/reports/export/sales'
    const res = await fetch(`/api${path}${qs ? '?' + qs : ''}`, {
      headers: { Authorization: `Bearer ${token()}` },
    })
    const blob = await res.blob()
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `${tab}-report.csv`
    a.click()
  }

  return (
    <Page
      title="Reports"
      subtitle="Platform service — analytics and CSV export"
      actions={
        <>
          <button type="button" className="ghost" onClick={() => load()}><RefreshCw size={16} />Run</button>
          <button type="button" className="primary" onClick={exportCsv}><Download size={16} />Export CSV</button>
        </>
      }
    >
      <div className="tabs">
        {tabs.map(t => (
          <button key={t} type="button" className={tab === t ? 'active' : ''} onClick={() => setTab(t)}>{label(t)}</button>
        ))}
      </div>
      <div className="toolbar wrap">
        <input type="date" value={String(filters.dateFrom || '')} onChange={e => setFilters({ ...filters, dateFrom: e.target.value })} />
        <input type="date" value={String(filters.dateTo || '')} onChange={e => setFilters({ ...filters, dateTo: e.target.value })} />
        <input placeholder="Status" value={String(filters.status || '')} onChange={e => setFilters({ ...filters, status: e.target.value })} />
        <input placeholder="Warehouse ID" value={String(filters.warehouseId || '')} onChange={e => setFilters({ ...filters, warehouseId: e.target.value })} />
        <input placeholder="Customer ID" value={String(filters.customerId || '')} onChange={e => setFilters({ ...filters, customerId: e.target.value })} />
        <input placeholder="Category" value={String(filters.category || '')} onChange={e => setFilters({ ...filters, category: e.target.value })} />
      </div>
      <Table rows={rows} columns={Object.keys(rows[0] || {})} />
    </Page>
  )
}
