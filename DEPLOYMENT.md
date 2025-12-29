# Deployment Guide

This guide covers deploying the Digital Wallet System to production using Heroku for backend services, Azure Static Web Apps for the frontend, and Supabase for the database.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│    ┌─────────────────────────────────────────────────────────────────────┐  │
│    │                Azure Static Web Apps                                │  │
│    │                  React Frontend                                     │  │
│    └─────────────────────────────────────────────────────────────────────┘  │
│                                  │                                          │
│                                  ▼                                          │
│    ┌─────────────────────────────────────────────────────────────────────┐  │
│    │                   Heroku (API Gateway)                              │  │
│    │         JWT Validation • Routing • Rate Limiting                    │  │
│    └─────────────────────────────────────────────────────────────────────┘  │
│         │           │           │           │           │                   │
│         ▼           ▼           ▼           ▼           ▼                   │
│    ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐  ┌─────────────┐         │
│    │  Auth  │  │ Wallet │  │Customer│  │ Ledger │  │Notification │         │
│    └────────┘  └────────┘  └────────┘  └────────┘  └─────────────┘         │
│                              │                                              │
│                              ▼                                              │
│    ┌─────────────────────────────────────────────────────────────────────┐  │
│    │                 Supabase PostgreSQL                                 │  │
│    │     Schemas: auth | wallet | customer | ledger | notification       │  │
│    └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Prerequisites

- [Heroku CLI](https://devcenter.heroku.com/articles/heroku-cli)
- [Azure Account](https://portal.azure.com)
- [Supabase Account](https://supabase.com)
- GitHub repository with the project

---

## 1. Database Setup (Supabase)

### Create Schemas

Connect to your Supabase database and create schemas for each service:

```sql
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS wallet;
CREATE SCHEMA IF NOT EXISTS customer;
CREATE SCHEMA IF NOT EXISTS ledger;
CREATE SCHEMA IF NOT EXISTS notification;
```

### Connection Configuration

Supabase provides two connection options:

| Connection Type | Port | Use Case |
|-----------------|------|----------|
| Direct | 5432 | Local development, persistent connections |
| Pooled (PgBouncer) | 6543 | Production, serverless, Heroku |

**Pooled connection string format:**
```
jdbc:postgresql://db.[PROJECT_REF].supabase.co:6543/postgres?pgbouncer=true
```

---

## 2. Backend Deployment (Heroku)

### Create Heroku Apps

```bash
heroku login

heroku create dws-api-gateway --region us
heroku create dws-auth --region us
heroku create dws-wallet --region us
heroku create dws-customer --region us
heroku create dws-ledger --region us
heroku create dws-notification --region us
```

### Set Container Stack

```bash
heroku stack:set container -a dws-api-gateway
heroku stack:set container -a dws-auth
heroku stack:set container -a dws-wallet
heroku stack:set container -a dws-customer
heroku stack:set container -a dws-ledger
heroku stack:set container -a dws-notification
```

### Configure Environment Variables

Generate a JWT secret:
```bash
openssl rand -base64 64
```

**API Gateway:**
```bash
heroku config:set \
  SPRING_PROFILES_ACTIVE=prod \
  JWT_SECRET=<your-jwt-secret> \
  AUTH_SERVICE_URL=https://dws-auth.herokuapp.com \
  WALLET_SERVICE_URL=https://dws-wallet.herokuapp.com \
  CUSTOMER_SERVICE_URL=https://dws-customer.herokuapp.com \
  LEDGER_SERVICE_URL=https://dws-ledger.herokuapp.com \
  NOTIFICATION_SERVICE_URL=https://dws-notification.herokuapp.com \
  FRONTEND_URL=https://<your-app>.azurestaticapps.net \
  -a dws-api-gateway
```

**Auth Service:**
```bash
heroku config:set \
  SPRING_PROFILES_ACTIVE=prod \
  DATABASE_URL="jdbc:postgresql://db.<project-ref>.supabase.co:6543/postgres?pgbouncer=true" \
  DB_USERNAME=postgres \
  DB_PASSWORD=<supabase-password> \
  JWT_SECRET=<your-jwt-secret> \
  -a dws-auth
```

**Wallet Service:**
```bash
heroku config:set \
  SPRING_PROFILES_ACTIVE=prod \
  DATABASE_URL="jdbc:postgresql://db.<project-ref>.supabase.co:6543/postgres?pgbouncer=true" \
  DB_USERNAME=postgres \
  DB_PASSWORD=<supabase-password> \
  LEDGER_SERVICE_URL=https://dws-ledger.herokuapp.com \
  NOTIFICATION_SERVICE_URL=https://dws-notification.herokuapp.com \
  -a dws-wallet
```

**Customer Service:**
```bash
heroku config:set \
  SPRING_PROFILES_ACTIVE=prod \
  DATABASE_URL="jdbc:postgresql://db.<project-ref>.supabase.co:6543/postgres?pgbouncer=true" \
  DB_USERNAME=postgres \
  DB_PASSWORD=<supabase-password> \
  -a dws-customer
```

**Ledger Service:**
```bash
heroku config:set \
  SPRING_PROFILES_ACTIVE=prod \
  DATABASE_URL="jdbc:postgresql://db.<project-ref>.supabase.co:6543/postgres?pgbouncer=true" \
  DB_USERNAME=postgres \
  DB_PASSWORD=<supabase-password> \
  -a dws-ledger
```

**Notification Service:**
```bash
heroku config:set \
  SPRING_PROFILES_ACTIVE=prod \
  DATABASE_URL="jdbc:postgresql://db.<project-ref>.supabase.co:6543/postgres?pgbouncer=true" \
  DB_USERNAME=postgres \
  DB_PASSWORD=<supabase-password> \
  MAIL_HOST=smtp.gmail.com \
  MAIL_PORT=587 \
  MAIL_USERNAME=<your-email> \
  MAIL_PASSWORD=<app-password> \
  -a dws-notification
```

---

## 3. Frontend Deployment (Azure Static Web Apps)

### Create Static Web App

1. Go to [Azure Portal](https://portal.azure.com)
2. Create a new **Static Web App**
3. Connect to your GitHub repository
4. Configure:
   - **App location**: `frontend`
   - **Output location**: `dist`

### Get Deployment Token

1. Navigate to your Static Web App in Azure Portal
2. Go to **Settings** → **Manage deployment token**
3. Copy the token

---

## 4. GitHub Actions Configuration

Add these secrets in **GitHub Repository → Settings → Secrets and variables → Actions**:

| Secret | Description |
|--------|-------------|
| `AZURE_STATIC_WEB_APPS_API_TOKEN` | Azure Static Web Apps deployment token |
| `VITE_API_URL` | `https://dws-api-gateway.herokuapp.com/api/v1` |
| `HEROKU_API_KEY` | Heroku API token (run `heroku auth:token`) |
| `HEROKU_APP_API_GATEWAY` | `dws-api-gateway` |
| `HEROKU_APP_AUTH` | `dws-auth` |
| `HEROKU_APP_WALLET` | `dws-wallet` |
| `HEROKU_APP_CUSTOMER` | `dws-customer` |
| `HEROKU_APP_LEDGER` | `dws-ledger` |
| `HEROKU_APP_NOTIFICATION` | `dws-notification` |

---

## 5. Deploy

Push to the main branch to trigger automatic deployment:

```bash
git add .
git commit -m "Deploy to production"
git push origin main
```

GitHub Actions will:
1. Build and deploy frontend to Azure Static Web Apps
2. Build and deploy each backend service to Heroku

---

## Environment Profiles

| Profile | Usage | Database |
|---------|-------|----------|
| `dev` | Local development | Local PostgreSQL |
| `test` | Integration testing | Testcontainers |
| `prod` | Production | Supabase PostgreSQL |

---

## Connection Pooling Configuration

HikariCP settings optimized for Supabase with PgBouncer:

```yaml
hikari:
  maximum-pool-size: 5      # Keep low for pooled connections
  minimum-idle: 2
  connection-timeout: 30000
  idle-timeout: 300000
  max-lifetime: 600000
```

---

## Troubleshooting

### Database Connection Issues
- Use port `6543` for pooled connections
- Ensure `?pgbouncer=true` is in the connection string
- Verify schema exists: `\dn` in psql

### CORS Errors
- Verify `FRONTEND_URL` in API Gateway matches your Azure domain
- Check for trailing slashes in URLs

### Cold Start Delays
- Heroku Eco dynos sleep after 30 minutes of inactivity
- First request after sleep takes 10-30 seconds to wake

