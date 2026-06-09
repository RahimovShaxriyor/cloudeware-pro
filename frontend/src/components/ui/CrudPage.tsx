import { useEffect, useState } from 'react'
import { Edit, Plus, RefreshCw, Search, Trash2 } from 'lucide-react'
import { request, type Row } from '../../api/client'
import { Page } from './Page'
import { Table } from './Table'
import { Spinner } from './Spinner'
import { FormModal, type Field } from './FormModal'

export function CrudPage({
  title, subtitle, path, columns, fields, defaults = {}, search = false, writePermission,
}: {
  title: string
  subtitle: string
  path: string
  columns: string[]
  fields: Field[]
  defaults?: Row
  search?: boolean
  writePermission?: string
}) {
  const [rows, setRows] = useState<Row[]>([])
  const [loading, setLoading] = useState(true)
  const [query, setQuery] = useState('')
  const [editing, setEditing] = useState<Row | null>(null)
  const [creating, setCreating] = useState(false)

  const load = async () => {
    setLoading(true)
    try {
      const url = search && query ? `${path}/search?query=${encodeURIComponent(query)}` : path
      setRows(await request<Row[]>(url))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  async function remove(row: Row) {
    if (!confirm('Delete this record?')) return
    await request(`${path}/${row.id}`, { method: 'DELETE' })
    load()
  }

  async function save(data: Row) {
    await request(editing ? `${path}/${editing.id}` : path, {
      method: editing ? 'PUT' : 'POST',
      body: JSON.stringify(data),
    })
    setEditing(null)
    setCreating(false)
    load()
  }

  return (
    <Page
      title={title}
      subtitle={subtitle}
      actions={
        <>
          <button type="button" className="ghost" onClick={load}><RefreshCw size={16} />Refresh</button>
          {writePermission !== 'none' && (
            <button type="button" className="primary" onClick={() => setCreating(true)}><Plus size={16} />New</button>
          )}
        </>
      }
    >
      {search && (
        <div className="toolbar">
          <div className="search">
            <Search size={16} />
            <input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search..." onKeyDown={e => { if (e.key === 'Enter') load() }} />
          </div>
          <button type="button" className="ghost" onClick={load}>Search</button>
        </div>
      )}
      {loading ? <Spinner /> : (
        <Table
          rows={rows}
          columns={columns}
          actions={writePermission !== 'none' ? row => (
            <>
              <button type="button" className="ghost" onClick={() => setEditing(row)}><Edit size={15} />Edit</button>
              <button type="button" className="danger" onClick={() => remove(row)}><Trash2 size={15} />Delete</button>
            </>
          ) : undefined}
        />
      )}
      {(creating || editing) && (
        <FormModal
          title={editing ? `Edit ${title}` : `Create ${title}`}
          fields={fields}
          initial={editing || defaults}
          onSubmit={save}
          onClose={() => { setCreating(false); setEditing(null) }}
        />
      )}
    </Page>
  )
}
