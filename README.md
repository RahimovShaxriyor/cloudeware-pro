# CloudWare Pro

CloudWare Pro is a working seller management system for a small or medium wholesale clothing business. The project is built as a mini ERP / CRM / WMS platform where a seller can manage products, customers, warehouses, inventory, orders, payments, reports, users, roles, notifications and business settings.

The frontend is not mock-only. It calls the backend through `/api`, and the backend saves data in PostgreSQL. The Docker architecture keeps the original cloud networking setup: PostgreSQL in a private network, two Spring Boot backend instances, and an Nginx web gateway that serves React and proxies API requests.

## Stack

- Backend: Java 17, Spring Boot, Spring JDBC
- Database: PostgreSQL 16
- Frontend: React, TypeScript, Vite
- Icons: lucide-react
- Gateway: Nginx
- Runtime: Docker Compose

## Main modules

- Authentication and profile management
- Dashboard with summary cards, backend instance indicator and sales chart
- Products and product categories
- Customers, balances and customer order history
- Warehouses and warehouse inventory
- Inventory, stock adjustment, stock transfer and movement history
- Orders with items and lifecycle actions: confirm, cancel, ship and deliver
- Payments with Uzbekistan-friendly methods: CASH, CARD, BANK_TRANSFER, PAYME, CLICK, UZUM_BANK
- Reports: sales, revenue, inventory, customers, orders and profit
- Settings: company, store, tax, currency, notifications, orders, inventory, security and theme
- Users, roles and permission display
- Activity log and notification panel

## How to run

```bash
docker compose up --build
```

Open the frontend:

```text
http://localhost:3000
```

API is available through the same web gateway:

```text
http://localhost:3000/api/health
```

If you already had an old database volume and want a clean seed database:

```bash
docker compose down -v
docker compose up --build
```

## Default login credentials

| Role | Email | Password |
|---|---|---|
| ADMIN | admin@cloudware.local | admin123 |
| SELLER | seller@cloudware.local | seller123 |
| WAREHOUSE_MANAGER | warehouse@cloudware.local | warehouse123 |
| ACCOUNTANT | accountant@cloudware.local | accountant123 |
| VIEWER | viewer@cloudware.local | viewer123 |

## Frontend pages

- `/login` — login page
- `/` — dashboard
- `/products` — products CRUD
- `/customers` — customers CRUD
- `/warehouses` — warehouses CRUD
- `/inventory` — stock table, adjustment, transfer and movement history
- `/orders` — order list, order details, add/remove items and lifecycle actions
- `/payments` — payments CRUD
- `/reports` — reports with filters and CSV export
- `/settings` — real backend settings tabs
- `/users` — user management
- `/activity` — audit log

## API endpoints

### Health and API info

- `GET /api/health`
- `GET /api/openapi`

### Auth

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `PUT /api/auth/profile`
- `PUT /api/auth/change-password`

### Dashboard

- `GET /api/dashboard/summary`
- `GET /api/dashboard/sales-chart`
- `GET /api/dashboard/low-stock`
- `GET /api/dashboard/recent-orders`
- `GET /api/dashboard/top-products`
- `GET /api/dashboard/latest-activity`

### Products and categories

- `GET /api/products`
- `GET /api/products/{id}`
- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`
- `GET /api/products/search?query=`
- `GET /api/products/categories`
- `POST /api/products/categories`
- `PUT /api/products/categories/{id}`
- `DELETE /api/products/categories/{id}`

### Customers

- `GET /api/customers`
- `GET /api/customers/{id}`
- `POST /api/customers`
- `PUT /api/customers/{id}`
- `DELETE /api/customers/{id}`
- `GET /api/customers/search?query=`
- `GET /api/customers/{id}/orders`
- `GET /api/customers/{id}/balance`

### Warehouses

- `GET /api/warehouses`
- `GET /api/warehouses/{id}`
- `POST /api/warehouses`
- `PUT /api/warehouses/{id}`
- `DELETE /api/warehouses/{id}`
- `GET /api/warehouses/{id}/inventory`

### Inventory

- `GET /api/inventory`
- `GET /api/inventory/{id}`
- `GET /api/inventory/product/{productId}`
- `GET /api/inventory/warehouse/{warehouseId}`
- `POST /api/inventory/adjust`
- `POST /api/inventory/transfer`
- `GET /api/inventory/low-stock`
- `GET /api/inventory/movements`

### Orders

- `GET /api/orders`
- `GET /api/orders/{id}`
- `POST /api/orders`
- `PUT /api/orders/{id}`
- `PATCH /api/orders/{id}/status`
- `DELETE /api/orders/{id}`
- `GET /api/orders/status/{status}`
- `GET /api/orders/customer/{customerId}`
- `POST /api/orders/{id}/items`
- `DELETE /api/orders/{id}/items/{itemId}`
- `POST /api/orders/{id}/confirm`
- `POST /api/orders/{id}/cancel`
- `POST /api/orders/{id}/ship`
- `POST /api/orders/{id}/deliver`

### Payments

- `GET /api/payments`
- `GET /api/payments/{id}`
- `POST /api/payments`
- `PUT /api/payments/{id}`
- `DELETE /api/payments/{id}`
- `GET /api/payments/order/{orderId}`
- `GET /api/payments/customer/{customerId}`

### Reports

- `GET /api/reports/sales`
- `GET /api/reports/revenue`
- `GET /api/reports/inventory`
- `GET /api/reports/customers`
- `GET /api/reports/orders`
- `GET /api/reports/profit`
- `GET /api/reports/export/sales`
- `GET /api/reports/export/inventory`

Report endpoints support filters such as `dateFrom`, `dateTo`, `status`, `warehouseId`, `customerId` and `category` where relevant.

### Settings

- `GET /api/settings`
- `PUT /api/settings`
- `GET /api/settings/company`
- `PUT /api/settings/company`
- `GET /api/settings/store`
- `PUT /api/settings/store`
- `GET /api/settings/tax`
- `PUT /api/settings/tax`
- `GET /api/settings/currency`
- `PUT /api/settings/currency`
- `GET /api/settings/notifications`
- `PUT /api/settings/notifications`
- `GET /api/settings/order`
- `PUT /api/settings/order`
- `GET /api/settings/inventory`
- `PUT /api/settings/inventory`
- `GET /api/settings/security`
- `PUT /api/settings/security`
- `GET /api/settings/theme`
- `PUT /api/settings/theme`

### Users and roles

- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/users`
- `PUT /api/users/{id}`
- `DELETE /api/users/{id}`
- `PATCH /api/users/{id}/status`
- `GET /api/roles`
- `POST /api/roles`
- `PUT /api/roles/{id}`
- `DELETE /api/roles/{id}`

### Activity and notifications

- `GET /api/activity`
- `GET /api/activity/{id}`
- `GET /api/activity/user/{userId}`
- `GET /api/activity/module/{module}`
- `GET /api/notifications`
- `PATCH /api/notifications/{id}/read`
- `PATCH /api/notifications/read-all`
- `DELETE /api/notifications/{id}`

## Seed data

The application creates schema and inserts realistic demo data on startup:

- 5 users
- 6 roles
- 11 permissions
- 20 clothing products
- 10 wholesale customers
- 3 warehouses
- 30 inventory rows
- 20 orders with different statuses
- 10 payments
- settings for all settings tabs
- activity logs and notifications

## Notes for programmers

- Controllers are separated by module in `backend/src/main/java/com/cloudware/controller`.
- Shared database helper logic is in `DataService`.
- Order lifecycle business logic is in `OrderService`.
- Startup schema and seed data are in `DataSeeder`.
- Auth tokens are stored in PostgreSQL table `auth_tokens`, so both `backend-a` and `backend-b` can validate the same token.
- Nginx proxies `/api/` to the backend pool and serves the React SPA.

## Useful test commands

```bash
# compose validation
docker compose config

# build and run
docker compose up --build

# health check
curl http://localhost:3000/api/health

# login
curl -s -X POST http://localhost:3000/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@cloudware.local","password":"admin123"}'

# after saving token from login
TOKEN="paste-token-here"
curl -H "Authorization: Bearer $TOKEN" http://localhost:3000/api/products
curl -H "Authorization: Bearer $TOKEN" http://localhost:3000/api/settings
curl -H "Authorization: Bearer $TOKEN" http://localhost:3000/api/orders
```

## Limitations

- Passwords are plain text because this is a diploma/demo project. For production, replace this with BCrypt and Spring Security.
- A lightweight `/api/openapi` endpoint is included. Full Swagger UI can be added later with `springdoc-openapi-starter-webmvc-ui` if you want a formal Swagger page.
- The frontend provides CSV export from the report tables. Backend export endpoints currently return JSON data that is ready for CSV/Excel conversion.
# cloudeware-pro
