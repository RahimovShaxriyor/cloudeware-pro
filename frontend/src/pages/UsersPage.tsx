import { CrudPage } from '../components/ui/CrudPage'
import type { Field } from '../components/ui/FormModal'

const fields: Field[] = [
  { key: 'fullName', label: 'Full name', required: true },
  { key: 'email', label: 'Email', type: 'email', required: true },
  { key: 'password', label: 'Password', type: 'password' },
  { key: 'role', label: 'Role', type: 'select', options: ['ADMIN', 'SELLER', 'MANAGER', 'WAREHOUSE_MANAGER', 'ACCOUNTANT', 'VIEWER'].map(v => ({ value: v, label: v })) },
  { key: 'department', label: 'Department' },
  { key: 'phone', label: 'Phone' },
  { key: 'active', label: 'Active', type: 'checkbox' },
]

export function UsersPage() {
  return (
    <CrudPage
      title="Users"
      subtitle="Identity service — accounts and role management"
      path="/users"
      columns={['fullName', 'email', 'role', 'department', 'phone', 'active']}
      fields={fields}
      defaults={{ role: 'VIEWER', active: true, password: 'password123' }}
      writePermission="users.manage"
    />
  )
}
