import { useEffect, useState } from 'react'
import { request, type Row } from '../api/client'
import { Page } from '../components/ui/Page'
import { label } from '../utils/format'
import type { Field } from '../components/ui/FormModal'

const settingsGroups: Record<string, Field[]> = {
  company: ['companyName', 'legalName', 'taxNumber', 'phone', 'email', 'website', 'address', 'city', 'country'].map(k => ({ key: k, label: label(k) })),
  store: ['storeName', 'defaultWarehouseId', 'defaultCustomerSegment', 'workingHours', 'supportPhone', 'supportEmail'].map(k => ({ key: k, label: label(k) })),
  tax: [{ key: 'taxEnabled', label: 'Tax enabled', type: 'checkbox' }, { key: 'taxPercent', label: 'Tax percent', type: 'number' }, { key: 'taxName', label: 'Tax name' }],
  currency: [{ key: 'currencyCode', label: 'Currency code' }, { key: 'currencySymbol', label: 'Currency symbol' }, { key: 'exchangeRate', label: 'Exchange rate', type: 'number' }, { key: 'priceRoundingEnabled', label: 'Price rounding', type: 'checkbox' }],
  notifications: ['emailNotifications', 'lowStockAlerts', 'orderStatusAlerts', 'paymentAlerts', 'dailyReportEnabled'].map(k => ({ key: k, label: label(k), type: 'checkbox' as const })),
  order: [{ key: 'autoGenerateOrderNumber', label: 'Auto generate order number', type: 'checkbox' }, { key: 'orderPrefix', label: 'Order prefix' }, { key: 'defaultOrderStatus', label: 'Default order status', type: 'select', options: ['DRAFT', 'NEW'].map(v => ({ value: v, label: v })) }, { key: 'allowNegativeStock', label: 'Allow negative stock', type: 'checkbox' }, { key: 'reserveStockOnConfirm', label: 'Reserve stock on confirm', type: 'checkbox' }, { key: 'autoMarkPaidAfterDelivery', label: 'Auto mark paid after delivery', type: 'checkbox' }],
  inventory: [{ key: 'lowStockThreshold', label: 'Low stock threshold', type: 'number' }, { key: 'stockMovementRequiredReason', label: 'Movement reason required', type: 'checkbox' }, { key: 'allowWarehouseTransfer', label: 'Allow warehouse transfer', type: 'checkbox' }, { key: 'showOutOfStockProducts', label: 'Show out of stock products', type: 'checkbox' }],
  security: [{ key: 'sessionTimeoutMinutes', label: 'Session timeout minutes', type: 'number' }, { key: 'requireStrongPassword', label: 'Require strong password', type: 'checkbox' }, { key: 'allowMultipleSessions', label: 'Allow multiple sessions', type: 'checkbox' }],
  theme: [{ key: 'themeMode', label: 'Theme mode', type: 'select', options: ['dark', 'light', 'system'].map(v => ({ value: v, label: v })) }, { key: 'sidebarCollapsed', label: 'Sidebar collapsed', type: 'checkbox' }, { key: 'accentColor', label: 'Accent color', type: 'color' }, { key: 'compactMode', label: 'Compact mode', type: 'checkbox' }],
}

export function SettingsPage() {
  const groups = Object.keys(settingsGroups)
  const [group, setGroup] = useState(groups[0])
  const [form, setForm] = useState<Row>({})
  const [message, setMessage] = useState('')

  async function load(g = group) {
    setForm(await request<Row>(`/settings/${g}`))
  }

  useEffect(() => { load(group) }, [group])

  async function save(e: React.FormEvent) {
    e.preventDefault()
    await request(`/settings/${group}`, { method: 'PUT', body: JSON.stringify(form) })
    setMessage('Settings saved successfully')
    setTimeout(() => setMessage(''), 2500)
    await load()
  }

  return (
    <Page title="Settings" subtitle="Platform service — application configuration">
      <div className="tabs">
        {groups.map(g => (
          <button key={g} type="button" className={group === g ? 'active' : ''} onClick={() => setGroup(g)}>{label(g)}</button>
        ))}
      </div>
      <form className="form-grid panel" onSubmit={save}>
        {settingsGroups[group].map(f => (
          <label key={f.key}>
            {f.label}
            {f.type === 'checkbox' ? (
              <input type="checkbox" checked={Boolean(form[f.key])} onChange={e => setForm({ ...form, [f.key]: e.target.checked })} />
            ) : f.type === 'select' ? (
              <select value={String(form[f.key] ?? '')} onChange={e => setForm({ ...form, [f.key]: e.target.value })}>
                {(f.options || []).map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            ) : (
              <input type={f.type || 'text'} value={String(form[f.key] ?? '')} onChange={e => setForm({ ...form, [f.key]: f.type === 'number' ? Number(e.target.value) : e.target.value })} />
            )}
          </label>
        ))}
        {message && <div className="success full">{message}</div>}
        <div className="form-actions">
          <button className="primary">Save {label(group)}</button>
        </div>
      </form>
    </Page>
  )
}
