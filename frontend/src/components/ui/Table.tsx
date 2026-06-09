import type { Row } from '../../api/client'
import { money, format, label } from '../../utils/format'
import { Badge } from './Badge'

export function Empty({ text }: { text: string }) {
  return <div className="empty">{text}</div>
}

function renderCell(key: string, value: unknown) {
  const k = key.toLowerCase()
  if (k.includes('price') || k.includes('amount') || k.includes('revenue') || k.includes('total') || k.includes('debt') || k.includes('limit') || k.includes('profit') || k.includes('cost')) {
    return money(value)
  }
  if (['status', 'active', 'isRead', 'lowStock', 'method', 'role'].includes(key)) {
    return <Badge value={value} />
  }
  return format(value)
}

export function Table({ rows, columns, actions }: { rows: Row[]; columns: string[]; actions?: (row: Row) => React.ReactNode }) {
  if (!rows.length) return <Empty text="No records found" />
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map(c => <th key={c}>{label(c)}</th>)}
            {actions && <th>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((r, index) => (
            <tr key={String(r.id ?? index)}>
              {columns.map(c => <td key={c}>{renderCell(c, r[c])}</td>)}
              {actions && <td className="row-actions">{actions(r)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
