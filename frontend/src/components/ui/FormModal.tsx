import { useState } from 'react'
import type { Row } from '../../api/client'
import { Modal } from './Modal'

export type Field = {
  key: string
  label: string
  type?: 'text' | 'number' | 'email' | 'select' | 'checkbox' | 'textarea' | 'date' | 'password' | 'color'
  options?: { value: string; label: string }[]
  required?: boolean
}

function FieldInput({ field, value, onChange }: { field: Field; value: unknown; onChange: (v: unknown) => void }) {
  if (field.type === 'select') {
    return (
      <label>
        {field.label}
        <select required={field.required} value={String(value ?? '')} onChange={e => onChange(e.target.value)}>
          {(field.options || []).map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
      </label>
    )
  }
  if (field.type === 'checkbox') {
    return (
      <label className="check">
        <input type="checkbox" checked={Boolean(value)} onChange={e => onChange(e.target.checked)} />
        {field.label}
      </label>
    )
  }
  if (field.type === 'textarea') {
    return (
      <label className="full">
        {field.label}
        <textarea required={field.required} value={String(value ?? '')} onChange={e => onChange(e.target.value)} />
      </label>
    )
  }
  return (
    <label>
      {field.label}
      <input
        required={field.required}
        type={field.type || 'text'}
        value={value === undefined || value === null ? '' : String(value)}
        onChange={e => onChange(field.type === 'number' ? Number(e.target.value) : e.target.value)}
      />
    </label>
  )
}

export function FormModal({
  title, fields, initial, onSubmit, onClose,
}: {
  title: string
  fields: Field[]
  initial: Row
  onSubmit: (data: Row) => Promise<void>
  onClose: () => void
}) {
  const [form, setForm] = useState<Row>(initial || {})
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  async function save(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      await onSubmit(form)
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={title} onClose={onClose}>
      <form onSubmit={save} className="form-grid">
        {fields.map(f => (
          <FieldInput key={f.key} field={f} value={form[f.key]} onChange={v => setForm({ ...form, [f.key]: v })} />
        ))}
        {error && <div className="error full">{error}</div>}
        <div className="form-actions">
          <button type="button" className="ghost" onClick={onClose}>Cancel</button>
          <button className="primary" disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
        </div>
      </form>
    </Modal>
  )
}
