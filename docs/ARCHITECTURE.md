# CloudWare Pro — Architecture

## Overview

CloudWare Pro is a cloud-hosted ERP/CRM/WMS platform for a wholesale clothing company.
The system is containerised using Docker and orchestrated with Docker Compose.

## Network topology

```
Internet
    │  HTTPS 443
    ▼
[ Public Subnet ]
  Nginx Gateway + Load Balancer (round-robin upstream)
    │  HTTP 8080
    ├─────────────────────┐
    ▼                     ▼
[ Private App Subnet ]
  backend-a            backend-b
  Spring Boot          Spring Boot
  (replica)            (replica)
    │                     │
    └──────────┬──────────┘
               ▼
[ Database Subnet ]
  PostgreSQL 15
  (private, not internet-accessible)

[ VPN tunnel ]
  Head office ──────────────────→ cloud VPC
  Warehouses  ──────────────────→ cloud VPC
```

## Assignment evidence mapping

| Criteria | Evidence |
|---|---|
| A.P1, A.M1, A.D1 | Cloud Network page + this document |
| A.P2 | Frontend → Nginx → backend API → PostgreSQL |
| B.P3, B.P4, B.M2 | Remote Spring Boot API, browser React client, Nginx load balancer |
| C.P5, C.P6 | Full ERP/CRM/WMS implementation in cloud containers |
| C.M3, C.D2 | Load test script, two backend replicas |
| D.P7, D.P8, D.M4, D.D3 | Before/after scaling, recommendations page |

## Security controls

- **TLS** — Nginx terminates SSL; backend never exposes port 8080 publicly
- **Firewall** — Docker network; only port 3000 exposed on host
- **Token auth** — Bearer token required for every API call (except login + health)
- **RBAC** — ADMIN, SALES_MANAGER, WAREHOUSE_MANAGER roles enforced in API layer
- **Private subnets** — DB not reachable from internet; backends not directly routable
- **VPN** — Staff connect via site-to-site VPN (simulated by Docker internal DNS)

## Docker Compose services

| Service | Image | Purpose |
|---|---|---|
| frontend | nginx:alpine | Serves React SPA + reverse-proxies /api to load-balanced backends |
| backend-a | cloudware/backend | Spring Boot API instance A |
| backend-b | cloudware/backend | Spring Boot API instance B |
| db | postgres:15 | PostgreSQL database (private network only) |

## Technology choices

| Layer | Technology | Reason |
|---|---|---|
| Frontend | React 18 + TypeScript + Vite | Type safety, fast dev builds, SPA routing |
| Styling | Pure CSS custom properties | No build overhead; full control |
| Backend | Spring Boot 3 + JDBC | Industry standard; minimal dependencies |
| Database | PostgreSQL 15 | Relational integrity; free/open-source |
| Gateway | Nginx | Proven reverse proxy; native round-robin LB |
| Containers | Docker Compose | Reproducible local deployment |

## Scaling strategy

**Horizontal scaling** — Add more backend replicas to the Nginx upstream block.
No code changes required; Nginx automatically distributes traffic.

**Vertical scaling** — Increase container CPU/memory limits in docker-compose.yml.

**Database scaling** — Add a read replica for reporting queries; use connection pooling (PgBouncer).
