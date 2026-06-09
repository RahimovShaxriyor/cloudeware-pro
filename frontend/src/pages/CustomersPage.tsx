import { CrudPage } from '../components/ui/CrudPage'
import type { Field } from '../components/ui/FormModal'

const fields: Field[] = [
  { key: 'companyName', label: 'Company name', required: true },
  { key: 'contactPerson', label: 'Contact person' },
  { key: 'email', label: 'Email', type: 'email' },
  { key: 'phone', label: 'Phone' },
  { key: 'city', label: 'City' },
  { key: 'address', label: 'Address' },
  { key: 'segment', label: 'Segment' },
  { key: 'creditLimit', label: 'Credit limit', type: 'number' },
  { key: 'currentDebt', label: 'Current debt', type: 'number' },
  { key: 'active', label: 'Active', type: 'checkbox' },
]

export function CustomersPage() {
  return (
    <CrudPage
      title="Customers"
      subtitle="CRM service — wholesale customer management"
      path="/customers"
      search
      columns={['companyName', 'contactPerson', 'email', 'phone', 'city', 'segment', 'creditLimit', 'currentDebt', 'active']}
      fields={fields}
      defaults={{ active: true, creditLimit: 0, currentDebt: 0 }}
      writePermission="customers.write"
    />
  )
}
