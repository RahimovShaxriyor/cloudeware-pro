import { CrudPage } from '../components/ui/CrudPage'
import type { Field } from '../components/ui/FormModal'

const fields: Field[] = [
  { key: 'name', label: 'Name', required: true },
  { key: 'code', label: 'Code', required: true },
  { key: 'city', label: 'City' },
  { key: 'address', label: 'Address' },
  { key: 'capacityUnits', label: 'Capacity units', type: 'number' },
  { key: 'active', label: 'Active', type: 'checkbox' },
]

export function WarehousesPage() {
  return (
    <CrudPage
      title="Warehouses"
      subtitle="WMS service — multi-warehouse locations"
      path="/warehouses"
      columns={['name', 'code', 'city', 'capacityUnits', 'active']}
      fields={fields}
      defaults={{ active: true, capacityUnits: 1000 }}
      writePermission="inventory.write"
    />
  )
}
