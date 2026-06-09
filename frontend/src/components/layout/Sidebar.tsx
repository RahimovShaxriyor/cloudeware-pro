import { NavLink } from 'react-router-dom'
import { motion } from 'framer-motion'
import {
  Activity, Boxes, Building2, Cloud, DollarSign, FileBarChart,
  LayoutDashboard, Package, Settings, ShoppingCart, Users, Warehouse,
} from 'lucide-react'
import type { Row } from '../../api/client'
import { hasPermission } from '../../utils/format'

const navItems = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, perm: null },
  { to: '/products', label: 'Products', icon: Package, perm: 'products.read' },
  { to: '/customers', label: 'Customers', icon: Building2, perm: 'customers.read' },
  { to: '/warehouses', label: 'Warehouses', icon: Warehouse, perm: 'inventory.read' },
  { to: '/inventory', label: 'Inventory', icon: Boxes, perm: 'inventory.read' },
  { to: '/orders', label: 'Orders', icon: ShoppingCart, perm: 'orders.read' },
  { to: '/payments', label: 'Payments', icon: DollarSign, perm: 'orders.read' },
  { to: '/reports', label: 'Reports', icon: FileBarChart, perm: 'reports.read' },
  { to: '/network', label: 'Cloud Network', icon: Cloud, perm: 'reports.read' },
  { to: '/settings', label: 'Settings', icon: Settings, perm: 'settings.write' },
  { to: '/users', label: 'Users', icon: Users, perm: 'users.manage' },
  { to: '/activity', label: 'Activity', icon: Activity, perm: 'reports.read' },
] as const

export function Sidebar({ user }: { user: Row | null }) {
  const visible = navItems.filter(item => !item.perm || hasPermission(user, item.perm))

  return (
    <aside className="sidebar">
      <motion.div
        className="logo"
        initial={{ opacity: 0, x: -12 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ delay: 0.1 }}
      >
        <Boxes size={28} />
        <div>
          <strong>CloudWare</strong>
          <span>Wholesale OS</span>
        </div>
      </motion.div>
      <nav>
        {visible.map((item, i) => (
          <motion.div
            key={item.to}
            initial={{ opacity: 0, x: -16 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.05 * i }}
          >
            <NavLink to={item.to} end={item.to === '/'}>
              <item.icon size={18} />
              {item.label}
            </NavLink>
          </motion.div>
        ))}
      </nav>
    </aside>
  )
}
