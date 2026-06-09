import { format, statusClass } from '../../utils/format'

export function Badge({ value }: { value: unknown }) {
  return <span className={`badge ${statusClass(value)}`}>{format(value)}</span>
}
