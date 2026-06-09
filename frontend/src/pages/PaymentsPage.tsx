import { CrudPage } from '../components/ui/CrudPage'
import type { Field } from '../components/ui/FormModal'

const fields: Field[] = [
  { key: 'orderId', label: 'Order ID', type: 'number' },
  { key: 'customerId', label: 'Customer ID', type: 'number' },
  { key: 'amount', label: 'Amount', type: 'number' },
  { key: 'method', label: 'Method', type: 'select', options: ['CASH', 'CARD', 'BANK_TRANSFER', 'PAYME', 'CLICK', 'UZUM_BANK'].map(v => ({ value: v, label: v })) },
  { key: 'status', label: 'Status', type: 'select', options: ['PENDING', 'PAID', 'PARTIAL', 'FAILED', 'REFUNDED'].map(v => ({ value: v, label: v })) },
  { key: 'paymentDate', label: 'Payment date', type: 'date' },
  { key: 'notes', label: 'Notes' },
]

export function PaymentsPage() {
  return (
    <CrudPage
      title="Payments"
      subtitle="Finance service — payment tracking and reconciliation"
      path="/payments"
      columns={['orderNumber', 'customerName', 'amount', 'method', 'status', 'paymentDate']}
      fields={fields}
      defaults={{ method: 'CASH', status: 'PENDING', amount: 0 }}
      writePermission="orders.write"
    />
  )
}
