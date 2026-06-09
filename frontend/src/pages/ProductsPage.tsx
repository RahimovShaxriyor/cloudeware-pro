import { CrudPage } from '../components/ui/CrudPage'
import type { Field } from '../components/ui/FormModal'

const fields: Field[] = [
  { key: 'sku', label: 'SKU', required: true },
  { key: 'name', label: 'Name', required: true },
  { key: 'category', label: 'Category' },
  { key: 'brand', label: 'Brand' },
  { key: 'sizeRange', label: 'Size range' },
  { key: 'color', label: 'Color' },
  { key: 'season', label: 'Season' },
  { key: 'wholesalePrice', label: 'Wholesale price', type: 'number' },
  { key: 'retailPrice', label: 'Retail price', type: 'number' },
  { key: 'minimumStock', label: 'Minimum stock', type: 'number' },
  { key: 'active', label: 'Active', type: 'checkbox' },
  { key: 'description', label: 'Description', type: 'textarea' },
]

export function ProductsPage() {
  return (
    <CrudPage
      title="Products"
      subtitle="Catalog service — product master data"
      path="/products"
      search
      columns={['sku', 'name', 'category', 'brand', 'wholesalePrice', 'retailPrice', 'minimumStock', 'active']}
      fields={fields}
      defaults={{ active: true, wholesalePrice: 0, retailPrice: 0, minimumStock: 10, season: 'All season' }}
      writePermission="products.write"
    />
  )
}
