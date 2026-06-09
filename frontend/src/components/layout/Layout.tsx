import { useState } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { LogOut, Shield } from 'lucide-react'
import { token } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { Spinner } from '../ui/Spinner'
import { Modal } from '../ui/Modal'
import { FormModal } from '../ui/FormModal'
import { Sidebar } from './Sidebar'
import { Notifications } from './Notifications'
import { DashboardPage } from '../../pages/DashboardPage'
import { ProductsPage } from '../../pages/ProductsPage'
import { CustomersPage } from '../../pages/CustomersPage'
import { WarehousesPage } from '../../pages/WarehousesPage'
import { InventoryPage } from '../../pages/InventoryPage'
import { OrdersPage } from '../../pages/OrdersPage'
import { PaymentsPage } from '../../pages/PaymentsPage'
import { ReportsPage } from '../../pages/ReportsPage'
import { SettingsPage } from '../../pages/SettingsPage'
import { UsersPage } from '../../pages/UsersPage'
import { ActivityPage } from '../../pages/ActivityPage'
import { NetworkPage } from '../../pages/NetworkPage'
import { request } from '../../api/client'

function Private({ children }: { children: React.ReactNode }) {
  const { user } = useAuth()
  if (!token()) return <Navigate to="/login" replace />
  if (!user) return <Spinner text="Loading session..." />
  return <>{children}</>
}

export function AppLayout() {
  const { user, logout, refresh } = useAuth()
  const [notifOpen, setNotifOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)

  return (
    <Private>
      <div className="app-shell">
        <Sidebar user={user} />
        <section className="workspace">
          <header className="topbar">
            <div>
              <h2>Wholesale Management System</h2>
              <span>Microservices · PostgreSQL · Spring Boot · React</span>
            </div>
            <div className="top-actions">
              <Notifications open={notifOpen} setOpen={setNotifOpen} />
              <button type="button" className="user-pill" onClick={() => setProfileOpen(true)}>
                <Shield size={16} />
                {String(user?.fullName)}
                <small>{String(user?.role)}</small>
              </button>
              <button type="button" className="ghost" onClick={logout}>
                <LogOut size={16} />Logout
              </button>
            </div>
          </header>
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/products" element={<ProductsPage />} />
            <Route path="/customers" element={<CustomersPage />} />
            <Route path="/warehouses" element={<WarehousesPage />} />
            <Route path="/inventory" element={<InventoryPage />} />
            <Route path="/orders" element={<OrdersPage />} />
            <Route path="/payments" element={<PaymentsPage />} />
            <Route path="/reports" element={<ReportsPage />} />
            <Route path="/network" element={<NetworkPage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="/users" element={<UsersPage />} />
            <Route path="/activity" element={<ActivityPage />} />
          </Routes>
        </section>
      </div>
      {profileOpen && (
        <FormModal
          title="Profile"
          fields={[
            { key: 'fullName', label: 'Full name', required: true },
            { key: 'phone', label: 'Phone' },
          ]}
          initial={{ fullName: user?.fullName, phone: user?.phone }}
          onClose={() => setProfileOpen(false)}
          onSubmit={async data => {
            await request('/auth/profile', { method: 'PUT', body: JSON.stringify(data) })
            await refresh()
          }}
        />
      )}
    </Private>
  )
}
