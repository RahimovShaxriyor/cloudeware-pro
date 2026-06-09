import { useEffect, useState } from 'react'
import { Eye, Plus, Trash2 } from 'lucide-react'
import { request, type Row } from '../api/client'
import { Page } from '../components/ui/Page'
import { Table } from '../components/ui/Table'
import { Modal } from '../components/ui/Modal'
import { FormModal } from '../components/ui/FormModal'
import { Badge } from '../components/ui/Badge'
import { money } from '../utils/format'

const statuses = ['DRAFT', 'NEW', 'CONFIRMED', 'PACKING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED']

export function OrdersPage() {
  const [rows, setRows] = useState<Row[]>([])
  const [customers, setCustomers] = useState<Row[]>([])
  const [products, setProducts] = useState<Row[]>([])
  const [detail, setDetail] = useState<Row | null>(null)
  const [create, setCreate] = useState(false)
  const [addItem, setAddItem] = useState(false)
  const [filter, setFilter] = useState('')

  async function load() {
    const [o, c, p] = await Promise.all([
      request<Row[]>(filter ? `/orders/status/${filter}` : '/orders'),
      request<Row[]>('/customers'),
      request<Row[]>('/products'),
    ])
    setRows(o)
    setCustomers(c)
    setProducts(p)
  }

  useEffect(() => { load() }, [filter])

  async function open(row: Row) {
    setDetail(await request<Row>(`/orders/${row.id}`))
  }

  async function action(name: string) {
    if (!detail) return
    setDetail(await request<Row>(`/orders/${detail.id}/${name}`, { method: 'POST' }))
    await load()
  }

  const customerOptions = customers.map(c => ({ value: String(c.id), label: String(c.companyName) }))
  const productOptions = products.map(p => ({ value: String(p.id), label: `${p.name} (${p.sku})` }))

  return (
    <Page
      title="Orders"
      subtitle="Order service — full lifecycle management"
      actions={
        <>
          <select value={filter} onChange={e => setFilter(e.target.value)}>
            <option value="">All statuses</option>
            {statuses.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
          <button type="button" className="primary" onClick={() => setCreate(true)}><Plus size={16} />New order</button>
        </>
      }
    >
      <Table
        rows={rows}
        columns={['orderNumber', 'customerName', 'status', 'priority', 'deliveryCity', 'subtotal', 'tax', 'totalAmount']}
        actions={row => <button type="button" className="ghost" onClick={() => open(row)}><Eye size={15} />Open</button>}
      />
      {create && (
        <FormModal
          title="Create order"
          fields={[
            { key: 'customerId', label: 'Customer', type: 'select', options: customerOptions },
            { key: 'priority', label: 'Priority', type: 'select', options: ['NORMAL', 'HIGH', 'URGENT'].map(v => ({ value: v, label: v })) },
            { key: 'deliveryCity', label: 'Delivery city' },
            { key: 'deliveryAddress', label: 'Delivery address' },
            { key: 'discount', label: 'Discount', type: 'number' },
            { key: 'deliveryFee', label: 'Delivery fee', type: 'number' },
            { key: 'notes', label: 'Notes', type: 'textarea' },
          ]}
          initial={{ customerId: customerOptions[0]?.value, priority: 'NORMAL', deliveryCity: 'Tashkent', discount: 0, deliveryFee: 0 }}
          onClose={() => setCreate(false)}
          onSubmit={async d => {
            const created = await request<Row>('/orders', { method: 'POST', body: JSON.stringify(d) })
            setCreate(false)
            setDetail(created)
            await load()
          }}
        />
      )}
      {detail && (
        <Modal title={`Order ${detail.orderNumber}`} onClose={() => setDetail(null)}>
          <div className="stack modal-body">
            <div className="detail-grid">
              <span>Status <Badge value={detail.status} /></span>
              <span>Customer <b>{String(detail.customerName)}</b></span>
              <span>Total <b>{money(detail.totalAmount)}</b></span>
            </div>
            <div className="button-row">
              <button type="button" className="primary" onClick={() => setAddItem(true)}>Add item</button>
              <button type="button" className="ghost" onClick={() => action('confirm')}>Confirm</button>
              <button type="button" className="ghost" onClick={() => action('ship')}>Ship</button>
              <button type="button" className="ghost" onClick={() => action('deliver')}>Deliver</button>
              <button type="button" className="danger" onClick={() => action('cancel')}>Cancel</button>
            </div>
            <Table
              rows={(detail.items as Row[]) || []}
              columns={['productName', 'sku', 'quantity', 'unitPrice', 'totalPrice']}
              actions={item => (
                <button
                  type="button"
                  className="danger"
                  onClick={async () => {
                    setDetail(await request<Row>(`/orders/${detail.id}/items/${item.id}`, { method: 'DELETE' }))
                    await load()
                  }}
                >
                  <Trash2 size={14} />
                </button>
              )}
            />
          </div>
        </Modal>
      )}
      {addItem && detail && (
        <FormModal
          title="Add order item"
          fields={[
            { key: 'productId', label: 'Product', type: 'select', options: productOptions },
            { key: 'quantity', label: 'Quantity', type: 'number' },
          ]}
          initial={{ productId: productOptions[0]?.value, quantity: 10 }}
          onClose={() => setAddItem(false)}
          onSubmit={async d => {
            setDetail(await request<Row>(`/orders/${detail.id}/items`, { method: 'POST', body: JSON.stringify(d) }))
            setAddItem(false)
            await load()
          }}
        />
      )}
    </Page>
  )
}
