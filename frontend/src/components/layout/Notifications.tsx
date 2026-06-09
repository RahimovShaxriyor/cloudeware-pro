import { useEffect, useState } from 'react'
import { Bell } from 'lucide-react'
import { request, type Row } from '../../api/client'
import { Empty } from '../ui/Table'

export function Notifications({ open, setOpen }: { open: boolean; setOpen: (v: boolean) => void }) {
  const [items, setItems] = useState<Row[]>([])

  const load = () => request<Row[]>('/notifications').then(setItems).catch(() => setItems([]))

  useEffect(() => {
    load()
    const timer = setInterval(load, 30000)
    return () => clearInterval(timer)
  }, [])

  const unread = items.filter(i => !i.isRead).length

  async function mark(id: number) {
    await request(`/notifications/${id}/read`, { method: 'PATCH' })
    load()
  }

  async function markAll() {
    await request('/notifications/read-all', { method: 'PATCH' })
    load()
  }

  return (
    <div className="notif-wrap">
      <button type="button" className="icon-button" onClick={() => setOpen(!open)} aria-label="Notifications">
        <Bell size={18} />
        {unread > 0 && <b>{unread}</b>}
      </button>
      {open && (
        <div className="notif-panel">
          <div className="panel-head">
            <strong>Notifications</strong>
            <button type="button" className="ghost" onClick={markAll}>Read all</button>
          </div>
          {items.length === 0 && <Empty text="No notifications" />}
          {items.map(n => (
            <button
              key={String(n.id)}
              type="button"
              className={`notif ${n.isRead ? '' : 'unread'}`}
              onClick={() => mark(Number(n.id))}
            >
              <span>{String(n.title)}</span>
              <small>{String(n.message)}</small>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
