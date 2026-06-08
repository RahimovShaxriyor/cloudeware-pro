<!--
  CloudWare Pro - ERP/CRM/WMS for Wholesale Clothing
  A beautiful, production-ready README for your GitHub repository
-->

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=java&logoColor=white" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=white" alt="React" />
  <img src="https://img.shields.io/badge/TypeScript-5.x-3178C6?style=for-the-badge&logo=typescript&logoColor=white" alt="TypeScript" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge" alt="License" />
</p>

<h1 align="center">
  ☁️ CloudWare Pro
</h1>

<p align="center">
  <strong>A complete ERP / CRM / WMS platform for wholesale clothing businesses</strong><br />
  Mini ERP with multi-warehouse inventory, order lifecycle, payments, reporting, and role-based access.
</p>

<p align="center">
  <a href="#-quick-start">Quick Start</a> •
  <a href="#-features">Features</a> •
  <a href="#-architecture">Architecture</a> •
  <a href="#-api-endpoints">API Endpoints</a> •
  <a href="#-default-credentials">Credentials</a>
</p>

<hr />

## ✨ Features

<table>
  <tr>
    <td width="50%" valign="top">
      <h3>📦 Inventory & Warehouses</h3>
      <ul>
        <li>Multi-warehouse support</li>
        <li>Stock adjustments & transfers</li>
        <li>Movement history tracking</li>
        <li>Low stock alerts</li>
      </ul>
    </td>
    <td width="50%" valign="top">
      <h3>🛒 Orders & Payments</h3>
      <ul>
        <li>Full order lifecycle (confirm → ship → deliver)</li>
        <li>Uzbekistan payment methods: PAYME, CLICK, UZUM_BANK, CASH, CARD</li>
        <li>Customer balance tracking</li>
        <li>Order history per customer</li>
      </ul>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <h3>📊 Reports & Analytics</h3>
      <ul>
        <li>Sales, revenue & profit reports</li>
        <li>Inventory valuation</li>
        <li>Customer analytics</li>
        <li>CSV export</li>
      </ul>
    </td>
    <td width="50%" valign="top">
      <h3>🔐 Security & Administration</h3>
      <ul>
        <li>Role-based access (RBAC)</li>
        <li>User management</li>
        <li>Activity audit log</li>
        <li>Notification system</li>
      </ul>
    </td>
  </tr>
</table>

## 🏗️ Architecture

```mermaid
flowchart TB
    subgraph "User Browser"
        FE[React SPA\n:3000]
    end

    subgraph "Docker Network"
        NG[Nginx Gateway\n:3000 → /api/*]
        
        subgraph "Backend Pool"
            B1[Spring Boot A\n:8081]
            B2[Spring Boot B\n:8082]
        end
        
        subgraph "Private Network"
            PG[(PostgreSQL 16\n:5432)]
        end
    end

    FE --> NG
    NG -->|/api/| B1
    NG -->|/api/| B2
    B1 --> PG
    B2 --> PG
    
    style NG fill:#4ecdc4,stroke:#333,stroke-width:2px,color:#fff
    style B1 fill:#ff6b6b,stroke:#333,stroke-width:2px,color:#fff
    style B2 fill:#ff6b6b,stroke:#333,stroke-width:2px,color:#fff
    style PG fill:#4d908e,stroke:#333,stroke-width:2px,color:#fff
    style FE fill:#f9c74f,stroke:#333,stroke-width:2px,color:#333
