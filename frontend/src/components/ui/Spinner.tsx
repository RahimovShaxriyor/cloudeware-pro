import { RefreshCw } from 'lucide-react'

export function Spinner({ text = 'Loading...' }: { text?: string }) {
  return (
    <div className="center">
      <RefreshCw className="spin" size={28} />
      <span>{text}</span>
    </div>
  )
}
