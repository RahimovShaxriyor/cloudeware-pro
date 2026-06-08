import React, { createContext, useContext, useEffect, useMemo, useState } from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Navigate, NavLink, Route, Routes, useNavigate } from 'react-router-dom'
import {
  Activity, Bell, Boxes, Building2, CheckCircle2, DollarSign, Edit, Eye,
  FileBarChart, LayoutDashboard, LogOut, Package, Plus, RefreshCw, Search, Settings,
  Shield, ShoppingCart, Trash2, Truck, Users, Warehouse, X
} from 'lucide-react'
import './styles.css'

type Row = Record<string, any>
type Field = { key: string; label: string; type?: 'text' | 'number' | 'email' | 'select' | 'checkbox' | 'textarea' | 'date' | 'password' | 'color'; options?: string[]; required?: boolean }
type AuthCtxType = { user: Row | null; login: (email: string, password: string) => Promise<void>; logout: () => Promise<void>; refresh: () => Promise<void> }

const API = '/api'
const AuthContext = createContext<AuthCtxType | null>(null)
const token = () => localStorage.getItem('cw_token')

async function request<T = any>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json', ...(options.headers as Row || {}) }
  const t = token()
  if (t) headers.Authorization = `Bearer ${t}`
  const response = await fetch(`${API}${path}`, { ...options, headers })
  const text = await response.text()
  const data = text ? JSON.parse(text) : null
  if (!response.ok) throw new Error(data?.error || data?.message || text || 'Request failed')
  return data
}

function money(value: any) {
  const n = Number(value || 0)
  return '$' + n.toLocaleString(undefined, { maximumFractionDigits: 2 })
}

function format(value: any) {
  if (value === null || value === undefined || value === '') return '—'
  if (typeof value === 'boolean') return value ? 'Yes' : 'No'
  if (typeof value === 'number') return value.toLocaleString()
  const s = String(value)
  if (s.includes('T') && s.includes(':')) return s.slice(0, 16).replace('T', ' ')
  return s
}

function statusClass(value: any) {
  const v = String(value || '').toLowerCase()
  if (['active', 'paid', 'delivered', 'confirmed', 'true'].includes(v)) return 'good'
  if (['pending', 'partial', 'packing', 'shipped', 'new', 'draft'].includes(v)) return 'warn'
  if (['cancelled', 'failed', 'returned', 'false', 'low'].includes(v)) return 'bad'
  return 'neutral'
}

function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('Auth provider is missing')
  return value
}

function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<Row | null>(null)
  const refresh = async () => {
    if (!token()) return
    try { setUser(await request('/auth/me')) } catch { localStorage.removeItem('cw_token'); setUser(null) }
  }
  useEffect(() => { refresh() }, [])
  const value = useMemo<AuthCtxType>(() => ({
    user,
    async login(email, password) {
      const res = await request('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })
      localStorage.setItem('cw_token', res.token)
      setUser(res.user)
    },
    async logout() {
      try { await request('/auth/logout', { method: 'POST' }) } catch {}
      localStorage.removeItem('cw_token')
      setUser(null)
    },
    refresh
  }), [user])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

function LoginPage() {
  const { login } = useAuth()
  const [email, setEmail] = useState('admin@cloudware.local')
  const [password, setPassword] = useState('admin123')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try { await login(email, password); navigate('/') } catch (err: any) { setError(err.message) } finally { setLoading(false) }
  }
  return <main className="login-page">
    <section className="login-card">
      <div className="brand-mark"><Boxes size={34}/></div>
      <h1>CloudWare Pro</h1>
      <p>Seller ERP / CRM / WMS for clothing wholesale businesses.</p>
      <form onSubmit={submit} className="stack">
        <label>Email<input value={email} onChange={e => setEmail(e.target.value)} type="email" required /></label>
        <label>Password<input value={password} onChange={e => setPassword(e.target.value)} type="password" required /></label>
        {error && <div className="error">{error}</div>}
        <button className="primary" disabled={loading}>{loading ? 'Signing in...' : 'Login'}</button>
      </form>
      <div className="demo-users">
        <b>Default users</b>
        <span>admin@cloudware.local / admin123</span>
        <span>seller@cloudware.local / seller123</span>
        <span>warehouse@cloudware.local / warehouse123</span>
      </div>
    </section>
  </main>
}

function Private({ children }: { children: React.ReactNode }) {
  const { user } = useAuth()
  if (!token()) return <Navigate to="/login" />
  if (!user) return <div className="center"><RefreshCw className="spin"/> Loading session...</div>
  return <>{children}</>
}

const nav = [
  ['/', 'Dashboard', LayoutDashboard], ['/products', 'Products', Package], ['/customers', 'Customers', Building2],
  ['/warehouses', 'Warehouses', Warehouse], ['/inventory', 'Inventory', Boxes], ['/orders', 'Orders', ShoppingCart],
  ['/payments', 'Payments', DollarSign], ['/reports', 'Reports', FileBarChart], ['/settings', 'Settings', Settings],
  ['/users', 'Users', Users], ['/activity', 'Activity', Activity]
] as const

function Layout() {
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  return <div className="app-shell">
    <aside className="sidebar">
      <div className="logo"><Boxes/><div><strong>CloudWare</strong><span>Seller OS</span></div></div>
      <nav>{nav.map(([to, label, Icon]) => <NavLink key={to} to={to} end={to === '/'}><Icon size={18}/>{label}</NavLink>)}</nav>
    </aside>
    <section className="workspace">
      <header className="topbar">
        <div><h2>Wholesale Management System</h2><span>PostgreSQL + Spring Boot + React + Nginx load balancing</span></div>
        <div className="top-actions"><Notifications open={open} setOpen={setOpen}/><div className="user-pill"><Shield size={16}/>{user?.fullName}<small>{user?.role}</small></div><button className="ghost" onClick={logout}><LogOut size={16}/>Logout</button></div>
      </header>
      <Routes>
        <Route path="/" element={<Dashboard/>}/>
        <Route path="/products" element={<ProductsPage/>}/>
        <Route path="/customers" element={<CustomersPage/>}/>
        <Route path="/warehouses" element={<WarehousesPage/>}/>
        <Route path="/inventory" element={<InventoryPage/>}/>
        <Route path="/orders" element={<OrdersPage/>}/>
        <Route path="/payments" element={<PaymentsPage/>}/>
        <Route path="/reports" element={<ReportsPage/>}/>
        <Route path="/settings" element={<SettingsPage/>}/>
        <Route path="/users" element={<UsersPage/>}/>
        <Route path="/activity" element={<ActivityPage/>}/>
      </Routes>
    </section>
  </div>
}

function Notifications({ open, setOpen }: { open: boolean; setOpen: (v: boolean) => void }) {
  const [items, setItems] = useState<Row[]>([])
  const load = () => request<Row[]>('/notifications').then(setItems).catch(() => setItems([]))
  useEffect(() => { load(); const timer = setInterval(load, 30000); return () => clearInterval(timer) }, [])
  const unread = items.filter(i => !i.isRead).length
  async function mark(id: number) { await request(`/notifications/${id}/read`, { method: 'PATCH' }); load() }
  async function all() { await request('/notifications/read-all', { method: 'PATCH' }); load() }
  return <div className="notif-wrap">
    <button className="icon-button" onClick={() => setOpen(!open)}><Bell size={18}/>{unread > 0 && <b>{unread}</b>}</button>
    {open && <div className="notif-panel">
      <div className="panel-head"><strong>Notifications</strong><button className="ghost" onClick={all}>Read all</button></div>
      {items.length === 0 && <Empty text="No notifications"/>}
      {items.map(n => <button key={n.id} className={`notif ${n.isRead ? '' : 'unread'}`} onClick={() => mark(n.id)}>
        <span>{n.title}</span><small>{n.message}</small>
      </button>)}
    </div>}
  </div>
}

function Page({ title, subtitle, actions, children }: { title: string; subtitle?: string; actions?: React.ReactNode; children: React.ReactNode }) {
  return <main className="page"><div className="page-head"><div><h1>{title}</h1>{subtitle && <p>{subtitle}</p>}</div><div className="actions">{actions}</div></div>{children}</main>
}

function Empty({ text }: { text: string }) { return <div className="empty">{text}</div> }
function Spinner() { return <div className="center"><RefreshCw className="spin"/> Loading...</div> }

function Modal({ title, children, onClose }: { title: string; children: React.ReactNode; onClose: () => void }) {
  return <div className="modal-backdrop" onMouseDown={onClose}>
    <div className="modal" onMouseDown={e => e.stopPropagation()}><div className="modal-head"><h3>{title}</h3><button className="icon-button" onClick={onClose}><X size={18}/></button></div>{children}</div>
  </div>
}

function FormModal({ title, fields, initial, onSubmit, onClose }: { title: string; fields: Field[]; initial: Row; onSubmit: (data: Row) => Promise<void>; onClose: () => void }) {
  const [form, setForm] = useState<Row>(initial || {})
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  async function save(e: React.FormEvent) {
    e.preventDefault(); setSaving(true); setError('')
    try { await onSubmit(form); onClose() } catch (err: any) { setError(err.message) } finally { setSaving(false) }
  }
  return <Modal title={title} onClose={onClose}><form onSubmit={save} className="form-grid">
    {fields.map(f => <FieldInput key={f.key} field={f} value={form[f.key]} onChange={v => setForm({ ...form, [f.key]: v })}/>) }
    {error && <div className="error full">{error}</div>}
    <div className="form-actions"><button type="button" className="ghost" onClick={onClose}>Cancel</button><button className="primary" disabled={saving}>{saving ? 'Saving...' : 'Save'}</button></div>
  </form></Modal>
}

function FieldInput({ field, value, onChange }: { field: Field; value: any; onChange: (v: any) => void }) {
  const common = { required: field.required, value: value ?? '', onChange: (e: any) => onChange(field.type === 'number' ? Number(e.target.value) : e.target.value) }
  if (field.type === 'select') return <label>{field.label}<select {...common}>{(field.options || []).map(o => <option key={o} value={o}>{o}</option>)}</select></label>
  if (field.type === 'checkbox') return <label className="check"><input type="checkbox" checked={Boolean(value)} onChange={e => onChange(e.target.checked)}/>{field.label}</label>
  if (field.type === 'textarea') return <label className="full">{field.label}<textarea {...common}/></label>
  return <label>{field.label}<input {...common} type={field.type || 'text'} /></label>
}

function Table({ rows, columns, actions }: { rows: Row[]; columns: string[]; actions?: (row: Row) => React.ReactNode }) {
  if (!rows.length) return <Empty text="No records found" />
  return <div className="table-wrap"><table><thead><tr>{columns.map(c => <th key={c}>{label(c)}</th>)}{actions && <th>Actions</th>}</tr></thead><tbody>
    {rows.map((r, index) => <tr key={r.id ?? index}>{columns.map(c => <td key={c}>{renderCell(c, r[c])}</td>)}{actions && <td className="row-actions">{actions(r)}</td>}</tr>)}
  </tbody></table></div>
}

function label(key: string) { return key.replace(/([A-Z])/g, ' $1').replace(/^./, s => s.toUpperCase()) }
function renderCell(key: string, value: any) {
  if (key.toLowerCase().includes('price') || key.toLowerCase().includes('amount') || key.toLowerCase().includes('revenue') || key.toLowerCase().includes('total') || key.toLowerCase().includes('debt') || key.toLowerCase().includes('limit')) return money(value)
  if (['status', 'active', 'isRead', 'lowStock', 'method', 'role'].includes(key)) return <span className={`badge ${statusClass(value)}`}>{format(value)}</span>
  return format(value)
}

function CrudPage({ title, subtitle, path, columns, fields, defaults = {}, search = false }: { title: string; subtitle: string; path: string; columns: string[]; fields: Field[]; defaults?: Row; search?: boolean }) {
  const [rows, setRows] = useState<Row[]>([])
  const [loading, setLoading] = useState(true)
  const [query, setQuery] = useState('')
  const [editing, setEditing] = useState<Row | null>(null)
  const [creating, setCreating] = useState(false)
  const load = async () => { setLoading(true); try { setRows(await request<Row[]>(search && query ? `${path}/search?query=${encodeURIComponent(query)}` : path)) } finally { setLoading(false) } }
  useEffect(() => { load() }, [])
  async function remove(row: Row) { if (!confirm('Delete this record?')) return; await request(`${path}/${row.id}`, { method: 'DELETE' }); load() }
  async function save(data: Row) { await request(editing ? `${path}/${editing.id}` : path, { method: editing ? 'PUT' : 'POST', body: JSON.stringify(data) }); setEditing(null); setCreating(false); load() }
  return <Page title={title} subtitle={subtitle} actions={<><button className="ghost" onClick={load}><RefreshCw size={16}/>Refresh</button><button className="primary" onClick={() => setCreating(true)}><Plus size={16}/>New</button></>}>
    {search && <div className="toolbar"><div className="search"><Search size={16}/><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search..." onKeyDown={e => { if (e.key === 'Enter') load() }}/></div><button className="ghost" onClick={load}>Search</button></div>}
    {loading ? <Spinner/> : <Table rows={rows} columns={columns} actions={row => <><button className="ghost" onClick={() => setEditing(row)}><Edit size={15}/>Edit</button><button className="danger" onClick={() => remove(row)}><Trash2 size={15}/>Delete</button></>}/>} 
    {(creating || editing) && <FormModal title={editing ? `Edit ${title}` : `Create ${title}`} fields={fields} initial={editing || defaults} onSubmit={save} onClose={() => { setCreating(false); setEditing(null) }}/>} 
  </Page>
}

function Dashboard() {
  const [summary, setSummary] = useState<Row | null>(null)
  const [orders, setOrders] = useState<Row[]>([])
  const [low, setLow] = useState<Row[]>([])
  const [activity, setActivity] = useState<Row[]>([])
  const [chart, setChart] = useState<Row[]>([])
  async function load() {
    const [s, o, l, a, c] = await Promise.all([
      request('/dashboard/summary'), request('/dashboard/recent-orders'), request('/dashboard/low-stock'), request('/dashboard/latest-activity'), request('/dashboard/sales-chart')
    ])
    setSummary(s); setOrders(o); setLow(l); setActivity(a); setChart(c)
  }
  useEffect(() => { load() }, [])
  if (!summary) return <Spinner/>
  const cards = [
    ['Revenue', money(summary.revenue), DollarSign], ['Active orders', summary.activeOrders, ShoppingCart], ['Customers', summary.customers, Users], ['Products', summary.products, Package], ['Low stock', summary.lowStock, Boxes], ['Backend', summary.instanceId, CheckCircle2]
  ]
  return <Page title="Dashboard" subtitle="Live business summary from backend API" actions={<button className="ghost" onClick={load}><RefreshCw size={16}/>Refresh</button>}>
    <div className="cards">{cards.map(([label, value, Icon]: any) => <div className="card" key={label}><Icon/><span>{label}</span><strong>{value}</strong></div>)}</div>
    <div className="grid two"><section className="panel"><h3>Sales chart</h3><div className="bars">{chart.slice().reverse().map(c => <div key={c.date}><span style={{ height: Math.max(8, Number(c.revenue || 0) / 50) }}></span><small>{String(c.date).slice(5)}</small></div>)}</div></section><section className="panel"><h3>Recent orders</h3><Table rows={orders} columns={['orderNumber','customerName','status','totalAmount']} /></section></div>
    <div className="grid two"><section className="panel"><h3>Low stock</h3><Table rows={low} columns={['productName','sku','warehouseName','availableQuantity','minimumStock']} /></section><section className="panel"><h3>Latest activity</h3><Table rows={activity} columns={['module','action','description','createdAt']} /></section></div>
  </Page>
}

const productFields: Field[] = [
  { key:'sku', label:'SKU', required:true }, { key:'name', label:'Name', required:true }, { key:'category', label:'Category' }, { key:'brand', label:'Brand' },
  { key:'sizeRange', label:'Size range' }, { key:'color', label:'Color' }, { key:'season', label:'Season' },
  { key:'wholesalePrice', label:'Wholesale price', type:'number' }, { key:'retailPrice', label:'Retail price', type:'number' }, { key:'minimumStock', label:'Minimum stock', type:'number' },
  { key:'active', label:'Active', type:'checkbox' }, { key:'description', label:'Description', type:'textarea' }
]
function ProductsPage() { return <CrudPage title="Products" subtitle="Real product CRUD connected to PostgreSQL" path="/products" search columns={['sku','name','category','brand','wholesalePrice','retailPrice','minimumStock','active']} fields={productFields} defaults={{ active:true, wholesalePrice:0, retailPrice:0, minimumStock:10, season:'All season' }}/> }

const customerFields: Field[] = [
  { key:'companyName', label:'Company name', required:true }, { key:'contactPerson', label:'Contact person' }, { key:'email', label:'Email', type:'email' }, { key:'phone', label:'Phone' }, { key:'city', label:'City' }, { key:'address', label:'Address' }, { key:'segment', label:'Segment' }, { key:'creditLimit', label:'Credit limit', type:'number' }, { key:'currentDebt', label:'Current debt', type:'number' }, { key:'active', label:'Active', type:'checkbox' }
]
function CustomersPage() { return <CrudPage title="Customers" subtitle="CRM customers, balances and order history" path="/customers" search columns={['companyName','contactPerson','email','phone','city','segment','creditLimit','currentDebt','active']} fields={customerFields} defaults={{ active:true, creditLimit:0, currentDebt:0 }}/> }

const warehouseFields: Field[] = [
  { key:'name', label:'Name', required:true }, { key:'code', label:'Code', required:true }, { key:'city', label:'City' }, { key:'address', label:'Address' }, { key:'capacityUnits', label:'Capacity units', type:'number' }, { key:'active', label:'Active', type:'checkbox' }
]
function WarehousesPage() { return <CrudPage title="Warehouses" subtitle="WMS warehouses with capacity and inventory view" path="/warehouses" columns={['name','code','city','capacityUnits','active']} fields={warehouseFields} defaults={{ active:true, capacityUnits:1000 }}/> }

function InventoryPage() {
  const [rows, setRows] = useState<Row[]>([]); const [products, setProducts] = useState<Row[]>([]); const [warehouses, setWarehouses] = useState<Row[]>([]); const [movements, setMovements] = useState<Row[]>([])
  const [adjust, setAdjust] = useState(false); const [transfer, setTransfer] = useState(false); const [loading, setLoading] = useState(true)
  async function load() { setLoading(true); const [i,p,w,m] = await Promise.all([request('/inventory'), request('/products'), request('/warehouses'), request('/inventory/movements')]); setRows(i); setProducts(p); setWarehouses(w); setMovements(m); setLoading(false) }
  useEffect(() => { load() }, [])
  const productOptions = products.map(p => String(p.id)); const whOptions = warehouses.map(w => String(w.id))
  return <Page title="Inventory" subtitle="Stock quantities, reservations, adjustments and warehouse transfers" actions={<><button className="ghost" onClick={load}><RefreshCw size={16}/>Refresh</button><button className="primary" onClick={() => setAdjust(true)}><Plus size={16}/>Adjust</button><button className="primary" onClick={() => setTransfer(true)}><Truck size={16}/>Transfer</button></>}>
    {loading ? <Spinner/> : <><Table rows={rows} columns={['productName','sku','warehouseName','quantity','reservedQuantity','availableQuantity','minimumStock','lowStock']} /><section className="panel"><h3>Movement history</h3><Table rows={movements.slice(0, 20)} columns={['type','productName','warehouseName','fromWarehouseName','toWarehouseName','quantity','reason','createdAt']} /></section></>}
    {adjust && <FormModal title="Adjust stock" fields={[{key:'productId',label:'Product ID',type:'select',options:productOptions},{key:'warehouseId',label:'Warehouse ID',type:'select',options:whOptions},{key:'quantity',label:'Quantity change',type:'number'},{key:'reason',label:'Reason'}]} initial={{ productId: productOptions[0], warehouseId: whOptions[0], quantity: 10, reason:'Manual adjustment' }} onClose={() => setAdjust(false)} onSubmit={async d => { await request('/inventory/adjust',{method:'POST', body:JSON.stringify(d)}); await load() }}/>} 
    {transfer && <FormModal title="Transfer stock" fields={[{key:'productId',label:'Product ID',type:'select',options:productOptions},{key:'fromWarehouseId',label:'From warehouse',type:'select',options:whOptions},{key:'toWarehouseId',label:'To warehouse',type:'select',options:whOptions},{key:'quantity',label:'Quantity',type:'number'},{key:'reason',label:'Reason'}]} initial={{ productId: productOptions[0], fromWarehouseId: whOptions[0], toWarehouseId: whOptions[1], quantity: 5, reason:'Rebalance stock' }} onClose={() => setTransfer(false)} onSubmit={async d => { await request('/inventory/transfer',{method:'POST', body:JSON.stringify(d)}); await load() }}/>} 
  </Page>
}

function OrdersPage() {
  const statuses = ['DRAFT','NEW','CONFIRMED','PACKING','SHIPPED','DELIVERED','CANCELLED','RETURNED']
  const [rows, setRows] = useState<Row[]>([]); const [customers, setCustomers] = useState<Row[]>([]); const [products, setProducts] = useState<Row[]>([]); const [detail, setDetail] = useState<Row | null>(null); const [create, setCreate] = useState(false); const [addItem, setAddItem] = useState(false); const [filter, setFilter] = useState('')
  async function load() { const [o,c,p] = await Promise.all([request<Row[]>(filter ? `/orders/status/${filter}` : '/orders'), request<Row[]>('/customers'), request<Row[]>('/products')]); setRows(o); setCustomers(c); setProducts(p) }
  useEffect(() => { load() }, [filter])
  async function open(row: Row) { setDetail(await request(`/orders/${row.id}`)) }
  async function action(name: string) { if (!detail) return; setDetail(await request(`/orders/${detail.id}/${name}`, { method:'POST' })); await load() }
  const customerOptions = customers.map(c => String(c.id)); const productOptions = products.map(p => String(p.id))
  return <Page title="Orders" subtitle="Order lifecycle: create, add items, confirm, cancel, ship and deliver" actions={<><select value={filter} onChange={e => setFilter(e.target.value)}><option value="">All statuses</option>{statuses.map(s => <option key={s}>{s}</option>)}</select><button className="primary" onClick={() => setCreate(true)}><Plus size={16}/>New order</button></>}>
    <Table rows={rows} columns={['orderNumber','customerName','status','priority','deliveryCity','subtotal','tax','totalAmount']} actions={row => <button className="ghost" onClick={() => open(row)}><Eye size={15}/>Open</button>} />
    {create && <FormModal title="Create order" fields={[{key:'customerId',label:'Customer ID',type:'select',options:customerOptions},{key:'priority',label:'Priority',type:'select',options:['NORMAL','HIGH','URGENT']},{key:'deliveryCity',label:'Delivery city'},{key:'deliveryAddress',label:'Delivery address'},{key:'discount',label:'Discount',type:'number'},{key:'deliveryFee',label:'Delivery fee',type:'number'},{key:'notes',label:'Notes',type:'textarea'}]} initial={{ customerId: customerOptions[0], priority:'NORMAL', deliveryCity:'Tashkent', discount:0, deliveryFee:0 }} onClose={() => setCreate(false)} onSubmit={async d => { const created = await request('/orders',{method:'POST',body:JSON.stringify(d)}); setCreate(false); setDetail(created); await load() }}/>} 
    {detail && <Modal title={`Order ${detail.orderNumber}`} onClose={() => setDetail(null)}><div className="stack"><div className="detail-grid"><span>Status <b className={`badge ${statusClass(detail.status)}`}>{detail.status}</b></span><span>Customer <b>{detail.customerName}</b></span><span>Total <b>{money(detail.totalAmount)}</b></span></div><div className="button-row"><button className="primary" onClick={() => setAddItem(true)}>Add item</button><button className="ghost" onClick={() => action('confirm')}>Confirm</button><button className="ghost" onClick={() => action('ship')}>Ship</button><button className="ghost" onClick={() => action('deliver')}>Deliver</button><button className="danger" onClick={() => action('cancel')}>Cancel</button></div><Table rows={detail.items || []} columns={['productName','sku','quantity','unitPrice','totalPrice']} actions={item => <button className="danger" onClick={async () => { setDetail(await request(`/orders/${detail.id}/items/${item.id}`, { method:'DELETE' })); await load() }}><Trash2 size={14}/></button>} /></div></Modal>}
    {addItem && detail && <FormModal title="Add order item" fields={[{key:'productId',label:'Product ID',type:'select',options:productOptions},{key:'quantity',label:'Quantity',type:'number'}]} initial={{ productId: productOptions[0], quantity: 10 }} onClose={() => setAddItem(false)} onSubmit={async d => { setDetail(await request(`/orders/${detail.id}/items`, { method:'POST', body:JSON.stringify(d) })); setAddItem(false); await load() }}/>} 
  </Page>
}

function PaymentsPage() {
  const fields: Field[] = [{key:'orderId',label:'Order ID',type:'number'}, {key:'customerId',label:'Customer ID',type:'number'}, {key:'amount',label:'Amount',type:'number'}, {key:'method',label:'Method',type:'select',options:['CASH','CARD','BANK_TRANSFER','PAYME','CLICK','UZUM_BANK']}, {key:'status',label:'Status',type:'select',options:['PENDING','PAID','PARTIAL','FAILED','REFUNDED']}, {key:'paymentDate',label:'Payment date',type:'date'}, {key:'notes',label:'Notes'}]
  return <CrudPage title="Payments" subtitle="Payment tracking for order and customer debts" path="/payments" columns={['orderNumber','customerName','amount','method','status','paymentDate']} fields={fields} defaults={{ method:'CASH', status:'PENDING', amount:0 }}/> 
}

function ReportsPage() {
  const [tab, setTab] = useState('sales'); const [rows, setRows] = useState<Row[]>([]); const [filters, setFilters] = useState<Row>({ dateFrom:'', dateTo:'', status:'', warehouseId:'', customerId:'', category:'' })
  const tabs = ['sales','revenue','inventory','customers','orders','profit']
  async function load(next = tab) { const qs = new URLSearchParams(Object.entries(filters).filter(([,v]) => v !== '').map(([k,v]) => [k,String(v)])).toString(); setRows(await request(`/reports/${next}${qs ? '?' + qs : ''}`)) }
  useEffect(() => { load() }, [tab])
  function exportCsv() { const cols = Object.keys(rows[0] || {}); const csv = [cols.join(','), ...rows.map(r => cols.map(c => JSON.stringify(r[c] ?? '')).join(','))].join('\n'); const a = document.createElement('a'); a.href = URL.createObjectURL(new Blob([csv], {type:'text/csv'})); a.download = `${tab}-report.csv`; a.click() }
  return <Page title="Reports" subtitle="Sales, revenue, inventory, customers, orders and profit reports" actions={<><button className="ghost" onClick={() => load()}><RefreshCw size={16}/>Run</button><button className="primary" onClick={exportCsv}>Export CSV</button></>}>
    <div className="tabs">{tabs.map(t => <button key={t} className={tab===t?'active':''} onClick={() => setTab(t)}>{label(t)}</button>)}</div>
    <div className="toolbar wrap"><input type="date" value={filters.dateFrom} onChange={e => setFilters({...filters,dateFrom:e.target.value})}/><input type="date" value={filters.dateTo} onChange={e => setFilters({...filters,dateTo:e.target.value})}/><input placeholder="Status" value={filters.status} onChange={e => setFilters({...filters,status:e.target.value})}/><input placeholder="Warehouse ID" value={filters.warehouseId} onChange={e => setFilters({...filters,warehouseId:e.target.value})}/><input placeholder="Customer ID" value={filters.customerId} onChange={e => setFilters({...filters,customerId:e.target.value})}/><input placeholder="Category" value={filters.category} onChange={e => setFilters({...filters,category:e.target.value})}/></div>
    <Table rows={rows} columns={Object.keys(rows[0] || {})}/>
  </Page>
}

const settingsGroups: Record<string, Field[]> = {
  company: ['companyName','legalName','taxNumber','phone','email','website','address','city','country'].map(k => ({key:k,label:label(k)})),
  store: ['storeName','defaultWarehouseId','defaultCustomerSegment','workingHours','supportPhone','supportEmail'].map(k => ({key:k,label:label(k)})),
  tax: [{key:'taxEnabled',label:'Tax enabled',type:'checkbox'},{key:'taxPercent',label:'Tax percent',type:'number'},{key:'taxName',label:'Tax name'}],
  currency: [{key:'currencyCode',label:'Currency code'},{key:'currencySymbol',label:'Currency symbol'},{key:'exchangeRate',label:'Exchange rate',type:'number'},{key:'priceRoundingEnabled',label:'Price rounding',type:'checkbox'}],
  notifications: ['emailNotifications','lowStockAlerts','orderStatusAlerts','paymentAlerts','dailyReportEnabled'].map(k => ({key:k,label:label(k),type:'checkbox'})),
  order: [{key:'autoGenerateOrderNumber',label:'Auto generate order number',type:'checkbox'},{key:'orderPrefix',label:'Order prefix'},{key:'defaultOrderStatus',label:'Default order status',type:'select',options:['DRAFT','NEW']},{key:'allowNegativeStock',label:'Allow negative stock',type:'checkbox'},{key:'reserveStockOnConfirm',label:'Reserve stock on confirm',type:'checkbox'},{key:'autoMarkPaidAfterDelivery',label:'Auto mark paid after delivery',type:'checkbox'}],
  inventory: [{key:'lowStockThreshold',label:'Low stock threshold',type:'number'},{key:'stockMovementRequiredReason',label:'Movement reason required',type:'checkbox'},{key:'allowWarehouseTransfer',label:'Allow warehouse transfer',type:'checkbox'},{key:'showOutOfStockProducts',label:'Show out of stock products',type:'checkbox'}],
  security: [{key:'sessionTimeoutMinutes',label:'Session timeout minutes',type:'number'},{key:'requireStrongPassword',label:'Require strong password',type:'checkbox'},{key:'allowMultipleSessions',label:'Allow multiple sessions',type:'checkbox'}],
  theme: [{key:'themeMode',label:'Theme mode',type:'select',options:['dark','light','system']},{key:'sidebarCollapsed',label:'Sidebar collapsed',type:'checkbox'},{key:'accentColor',label:'Accent color',type:'color'},{key:'compactMode',label:'Compact mode',type:'checkbox'}]
}

function SettingsPage() {
  const groups = Object.keys(settingsGroups); const [group, setGroup] = useState(groups[0]); const [form, setForm] = useState<Row>({}); const [message, setMessage] = useState('')
  async function load(g = group) { setForm(await request(`/settings/${g}`)) }
  useEffect(() => { load(group) }, [group])
  async function save(e: React.FormEvent) { e.preventDefault(); await request(`/settings/${group}`, { method:'PUT', body:JSON.stringify(form) }); setMessage('Settings saved successfully'); setTimeout(() => setMessage(''), 2500); await load() }
  return <Page title="Settings" subtitle="Every setting is loaded and saved through backend API">
    <div className="tabs">{groups.map(g => <button key={g} className={group===g?'active':''} onClick={() => setGroup(g)}>{label(g)}</button>)}</div>
    <form className="form-grid panel" onSubmit={save}>{settingsGroups[group].map(f => <FieldInput key={f.key} field={f} value={form[f.key]} onChange={v => setForm({...form,[f.key]:v})}/>)}{message && <div className="success full">{message}</div>}<div className="form-actions"><button className="primary">Save {label(group)}</button></div></form>
  </Page>
}

function UsersPage() {
  const fields: Field[] = [{key:'fullName',label:'Full name',required:true},{key:'email',label:'Email',type:'email',required:true},{key:'password',label:'Password',type:'password'},{key:'role',label:'Role',type:'select',options:['ADMIN','SELLER','MANAGER','WAREHOUSE_MANAGER','ACCOUNTANT','VIEWER']},{key:'department',label:'Department'},{key:'phone',label:'Phone'},{key:'active',label:'Active',type:'checkbox'}]
  return <CrudPage title="Users" subtitle="User accounts, roles and status management" path="/users" columns={['fullName','email','role','department','phone','active']} fields={fields} defaults={{ role:'VIEWER', active:true, password:'password123' }}/> 
}

function ActivityPage() {
  const [rows, setRows] = useState<Row[]>([]); const [module, setModule] = useState('')
  async function load() { setRows(await request(module ? `/activity/module/${module}` : '/activity')) }
  useEffect(() => { load() }, [])
  return <Page title="Activity" subtitle="Audit log for important business actions" actions={<button className="ghost" onClick={load}>Filter</button>}>
    <div className="toolbar"><input placeholder="Module filter, e.g. Orders" value={module} onChange={e => setModule(e.target.value)}/></div>
    <Table rows={rows} columns={['userName','module','action','description','createdAt']} />
  </Page>
}

function App() {
  return <BrowserRouter><AuthProvider><Routes><Route path="/login" element={<LoginPage/>}/><Route path="/*" element={<Private><Layout/></Private>}/></Routes></AuthProvider></BrowserRouter>
}

ReactDOM.createRoot(document.getElementById('root')!).render(<App />)
