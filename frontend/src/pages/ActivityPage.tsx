import { useEffect, useState } from 'react'
import { request, type Row } from '../api/client'
import { Page } from '../components/ui/Page'
import { Table } from '../components/ui/Table'

export function ActivityPage() {
  const [rows, setRows] = useState<Row[]>([])
  const [module, setModule] = useState('')

  async function load() {
    setRows(await request<Row[]>(module ? `/activity/module/${module}` : '/activity'))
  }

  useEffect(() => { load() }, [])

  return (
    <Page title="Activity" subtitle="Platform service — audit trail">
      <div className="toolbar">
        <input placeholder="Module filter, e.g. Orders" value={module} onChange={e => setModule(e.target.value)} />
        <button type="button" className="ghost" onClick={load}>Filter</button>
      </div>
      <Table rows={rows} columns={['userName', 'module', 'action', 'description', 'createdAt']} />
    </Page>
  )
}
