import { useEffect, useState } from 'react'
import { Plus, RefreshCw, Truck } from 'lucide-react'
import { request, type Row } from '../api/client'
import { Page } from '../components/ui/Page'
import { Table } from '../components/ui/Table'
import { Spinner } from '../components/ui/Spinner'
import { FormModal } from '../components/ui/FormModal'

export function InventoryPage() {
  const [rows, setRows] = useState<Row[]>([])
  const [products, setProducts] = useState<Row[]>([])
  const [warehouses, setWarehouses] = useState<Row[]>([])
  const [movements, setMovements] = useState<Row[]>([])
  const [adjust, setAdjust] = useState(false)
  const [transfer, setTransfer] = useState(false)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    const [i, p, w, m] = await Promise.all([
      request<Row[]>('/inventory'),
      request<Row[]>('/products'),
      request<Row[]>('/warehouses'),
      request<Row[]>('/inventory/movements'),
    ])
    setRows(i)
    setProducts(p)
    setWarehouses(w)
    setMovements(m)
    setLoading(false)
  }

  useEffect(() => { load() }, [])

  const productOptions = products.map(p => ({ value: String(p.id), label: `${p.name} (${p.sku})` }))
  const whOptions = warehouses.map(w => ({ value: String(w.id), label: String(w.name) }))

  return (
    <Page
      title="Inventory"
      subtitle="WMS service — stock levels, adjustments and transfers"
      actions={
        <>
          <button type="button" className="ghost" onClick={load}><RefreshCw size={16} />Refresh</button>
          <button type="button" className="primary" onClick={() => setAdjust(true)}><Plus size={16} />Adjust</button>
          <button type="button" className="primary" onClick={() => setTransfer(true)}><Truck size={16} />Transfer</button>
        </>
      }
    >
      {loading ? <Spinner /> : (
        <>
          <Table rows={rows} columns={['productName', 'sku', 'warehouseName', 'quantity', 'reservedQuantity', 'availableQuantity', 'minimumStock', 'lowStock']} />
          <section className="panel">
            <h3>Movement history</h3>
            <Table rows={movements.slice(0, 20)} columns={['type', 'productName', 'warehouseName', 'fromWarehouseName', 'toWarehouseName', 'quantity', 'reason', 'createdAt']} />
          </section>
        </>
      )}
      {adjust && (
        <FormModal
          title="Adjust stock"
          fields={[
            { key: 'productId', label: 'Product', type: 'select', options: productOptions },
            { key: 'warehouseId', label: 'Warehouse', type: 'select', options: whOptions },
            { key: 'quantity', label: 'Quantity change', type: 'number' },
            { key: 'reason', label: 'Reason' },
          ]}
          initial={{ productId: productOptions[0]?.value, warehouseId: whOptions[0]?.value, quantity: 10, reason: 'Manual adjustment' }}
          onClose={() => setAdjust(false)}
          onSubmit={async d => { await request('/inventory/adjust', { method: 'POST', body: JSON.stringify(d) }); await load() }}
        />
      )}
      {transfer && (
        <FormModal
          title="Transfer stock"
          fields={[
            { key: 'productId', label: 'Product', type: 'select', options: productOptions },
            { key: 'fromWarehouseId', label: 'From warehouse', type: 'select', options: whOptions },
            { key: 'toWarehouseId', label: 'To warehouse', type: 'select', options: whOptions },
            { key: 'quantity', label: 'Quantity', type: 'number' },
            { key: 'reason', label: 'Reason' },
          ]}
          initial={{ productId: productOptions[0]?.value, fromWarehouseId: whOptions[0]?.value, toWarehouseId: whOptions[1]?.value, quantity: 5, reason: 'Rebalance stock' }}
          onClose={() => setTransfer(false)}
          onSubmit={async d => { await request('/inventory/transfer', { method: 'POST', body: JSON.stringify(d) }); await load() }}
        />
      )}
    </Page>
  )
}
