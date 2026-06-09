# CloudWare Pro — Microservices Architecture

## Overview

CloudWare Pro v2 uses a **7-service microservices architecture** behind an **Nginx API Gateway**, with a **React SPA** frontend and **PostgreSQL 16** database.

## Service map

| Service | Port | API routes | Responsibility |
|---------|------|------------|----------------|
| **identity-service** | 8081 | `/api/auth`, `/api/users`, `/api/roles` | Authentication, users, RBAC |
| **catalog-service** | 8082 | `/api/products` | Products and categories |
| **crm-service** | 8083 | `/api/customers` | Customer master data |
| **wms-service** | 8084 | `/api/warehouses`, `/api/inventory` | Warehouses, stock, movements |
| **order-service** | 8085 | `/api/orders` | Order lifecycle |
| **finance-service** | 8086 | `/api/payments` | Payments and reconciliation |
| **platform-service** | 8087 | `/api/dashboard`, `/api/reports`, `/api/settings`, `/api/activity`, `/api/notifications`, `/api/network`, `/api/health` | Analytics, settings, audit |

## Request flow

```
Browser → Nginx (port 3000) → path-based routing → microservice → PostgreSQL
```

## Code structure

```
backend/
├── cloudware-common/          # Shared controllers, services, JDBC layer
├── services/
│   ├── identity-service/
│   ├── catalog-service/
│   ├── crm-service/
│   ├── wms-service/
│   ├── order-service/
│   ├── finance-service/
│   └── platform-service/
└── pom.xml                    # Maven parent

frontend/
├── src/
│   ├── api/                   # HTTP client
│   ├── auth/                  # Auth context
│   ├── components/            # UI + layout
│   ├── pages/                 # Route pages
│   └── styles/                # Global CSS
└── nginx.conf                 # API gateway routing
```

Each microservice loads only its controllers via `@Microservice("name")` conditional beans.

## Security

- BCrypt password hashing
- Bearer token authentication
- Role-based permissions enforced on sensitive endpoints
- Private Docker network for backend services and database

## Run

```bash
docker compose up --build
```

Open `http://localhost:3000`
