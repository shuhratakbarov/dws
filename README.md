# 💰 Digital Wallet System (DWS)

<p align="center">
  <img src="frontend/public/wallet.svg" alt="DWS Logo" width="80" height="80">
</p>

<p align="center">
  <strong>A production-ready microservices-based digital wallet system</strong>
</p>

<p align="center">
  <a href="#"><img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk" alt="Java 21"></a>
  <a href="#"><img src="https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen?style=flat-square&logo=springboot" alt="Spring Boot"></a>
  <a href="#"><img src="https://img.shields.io/badge/React-18-blue?style=flat-square&logo=react" alt="React 18"></a>
  <a href="#"><img src="https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square&logo=postgresql" alt="PostgreSQL"></a>
  <a href="./LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="MIT License"></a>
</p>

<p align="center">
  <a href="#quick-start">Quick Start</a> •
  <a href="#features">Features</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#api-documentation">API Docs</a> •
  <a href="#deployment">Deployment</a>
</p>

---

A full-stack digital wallet application demonstrating **microservices architecture** with Spring Boot and React. Users can create multi-currency wallets, deposit/withdraw funds, transfer between wallets, and view transaction history.

> **📚 Learning Project**: This project was built step-by-step as a learning journey into microservices architecture. It demonstrates real-world patterns used in fintech applications.

## ✨ Key Features

- 🏦 **Multi-currency wallets** - USD, EUR, GBP, UZS support
- 💸 **Secure transactions** - Deposits, withdrawals, and transfers
- 🔐 **JWT authentication** - With automatic token refresh
- 🚦 **API Gateway** - Centralized routing and rate limiting
- 📊 **Transaction history** - Immutable ledger for audit trail
- 📧 **Notifications** - Email alerts for transactions
- 🎨 **Modern UI** - React + Ant Design responsive interface

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         React Frontend (3000)                           │
│   ✅ Ant Design UI   ✅ Dashboard   ✅ Transactions   ✅ Profile       │
└─────────────────────────────────────┬───────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         API Gateway (8080)                               │
│  ✅ JWT Validation  ✅ Routing  ✅ Rate Limiting  ✅ CORS               │
└──┬──────────┬──────────┬──────────┬──────────┬──────────────────────────┘
   │          │          │          │          │
   ▼          ▼          ▼          ▼          ▼
┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐  ┌────────────┐
│ Auth │  │Wallet│  │Cust. │  │Ledger│  │Notification│
│ 8081 │  │ 8082 │  │ 8083 │  │ 8084 │  │    8085    │
└──────┘  └──────┘  └──────┘  └──────┘  └────────────┘
                        │
                        ▼
                ┌──────────────┐
                │  PostgreSQL  │
                │    (5432)    │
                └──────────────┘
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| **Frontend** | 3000 | React + Ant Design web application |
| API Gateway | 8080 | Routes requests, validates JWT, rate limiting |
| Auth Service | 8081 | User registration, login, JWT tokens |
| Wallet Service | 8082 | Wallets, deposits, withdrawals, transfers |
| Customer Service | 8083 | Customer profiles, KYC |
| Ledger Service | 8084 | Immutable transaction history, audit trail |
| Notification Service | 8085 | Email, SMS, Push notifications |
| MailHog (Dev) | 8025 | Email testing UI |

## Quick Start

### Option 1: Docker Compose (Full Stack)

```bash
# Clone and navigate to project
cd DWS

# Create .env file
cp .env.example .env

# Start all services (backend + frontend)
docker-compose up -d

# Open frontend: http://localhost:3000
# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Option 2: Local Development

**Prerequisites:**
- Java 21+
- Node.js 20+
- PostgreSQL 15
- Maven

**Backend Setup:**
```sql
-- Create databases
CREATE DATABASE auth_db;
CREATE DATABASE wallet_db;
CREATE DATABASE customer_db;
CREATE DATABASE ledger_db;
CREATE DATABASE notification_db;
```

**Start backend services:**
```powershell
# Set environment variables
$env:DB_PASSWORD = "your_password"
$env:SPRING_PROFILES_ACTIVE = "dev"

# Terminal 1 - API Gateway
cd api-gateway; .\mvnw.cmd spring-boot:run

# Terminal 2 - Auth Service
cd auth-service; .\mvnw.cmd spring-boot:run

# Terminal 3 - Wallet Service
cd wallet-service; .\mvnw.cmd spring-boot:run

# Terminal 4 - Customer Service
cd customer-service; .\mvnw.cmd spring-boot:run

# Terminal 5 - Ledger Service
cd ledger-service; .\mvnw.cmd spring-boot:run

# Terminal 6 - Notification Service
cd notification-service; .\mvnw.cmd spring-boot:run
```

**Start frontend:**
```powershell
cd frontend
npm install
npm run dev
# Open http://localhost:3000
```

## Frontend Features

| Page | Features |
|------|----------|
| **Dashboard** | Wallet cards, balances, quick actions (deposit/withdraw/transfer) |
| **Transactions** | Full transaction history, filters, search, CSV export |
| **Profile** | User profile, KYC status, address management |
| **Settings** | Notifications, security, language, theme preferences |

## Environment Profiles

Each service supports multiple profiles:

| Profile | Usage | Command |
|---------|-------|---------|
| `dev` | Local development | `SPRING_PROFILES_ACTIVE=dev` |
| `test` | Integration testing | `SPRING_PROFILES_ACTIVE=test` |
| `prod` | Production deployment | `SPRING_PROFILES_ACTIVE=prod` |

**Profile differences:**

| Feature | dev | prod |
|---------|-----|------|
| SQL logging | ✅ Enabled | ❌ Disabled |
| Swagger UI | ✅ Enabled | ❌ Disabled |
| DDL auto | update | validate |
| Error details | ✅ Shown | ❌ Hidden |
| CORS | localhost:3000 | Frontend domain only |

## API Documentation

| Service | Swagger UI (dev only) |
|---------|-----|
| Auth Service | http://localhost:8081/swagger-ui.html |
| Wallet Service | http://localhost:8082/swagger-ui.html |
| Customer Service | http://localhost:8083/swagger-ui.html |
| Ledger Service | http://localhost:8084/swagger-ui.html |
| Notification Service | http://localhost:8085/swagger-ui.html |

## API Examples

### 1. Register User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecureP@ss123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecureP@ss123"
  }'
```

### 3. Create Wallet
```bash
curl -X POST http://localhost:8080/api/v1/wallets/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"currency": "USD"}'
```

### 4. Deposit Funds
```bash
curl -X POST http://localhost:8080/api/v1/wallets/{walletId}/deposit \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amountMinorUnits": 10000,
    "idempotencyKey": "unique-key-123",
    "description": "Initial deposit"
  }'
```

## Project Structure

```
DWS/
├── frontend/               # React + Ant Design + TypeScript
│   ├── src/
│   │   ├── components/     # Reusable UI components
│   │   ├── contexts/       # React contexts (Auth)
│   │   ├── pages/          # Page components
│   │   ├── services/       # API service layer
│   │   └── types/          # TypeScript types
│   └── Dockerfile
├── api-gateway/            # Spring Cloud Gateway
├── auth-service/           # Authentication & JWT
├── wallet-service/         # Wallet operations
├── customer-service/       # Customer profiles
├── ledger-service/         # Transaction history
├── notification-service/   # Email/SMS/Push
├── docker-compose.yml      # Docker orchestration
├── .github/workflows/      # CI/CD pipelines
└── DEPLOYMENT.md           # Deployment guide
```

## Technology Stack

### Backend
- **Java 21** - Programming language
- **Spring Boot 3.4** - Application framework
- **Spring Cloud Gateway** - API Gateway
- **Spring Security** - Authentication
- **Spring Data JPA** - Data access
- **PostgreSQL** - Database
- **JWT (jjwt)** - Token-based auth
- **Testcontainers** - Integration testing

### Frontend
- **React 18** - UI framework
- **TypeScript** - Type safety
- **Ant Design 5** - UI component library
- **Vite** - Build tool
- **Axios** - HTTP client
- **React Router 6** - Routing

### DevOps
- **Docker** - Containerization
- **GitHub Actions** - CI/CD
- **Heroku/Railway** - Cloud deployment

## Key Features

- ✅ **Multi-currency wallets** (USD, EUR, GBP, UZS, etc.)
- ✅ **Deposits and withdrawals**
- ✅ **Wallet-to-wallet transfers**
- ✅ **Idempotent transactions** (no duplicate processing)
- ✅ **JWT authentication** with refresh tokens
- ✅ **Centralized API Gateway** with rate limiting
- ✅ **Customer profiles** with KYC status
- ✅ **Immutable ledger** for audit trail
- ✅ **Email notifications** for transactions
- ✅ **React frontend** with modern UI
- ✅ **Environment profiles** (dev/test/prod)
- ✅ **Comprehensive API documentation**

## Deployment

See [DEPLOYMENT.md](./DEPLOYMENT.md) for detailed deployment instructions:
- Heroku (with GitHub Student credits)
- Railway
- Render
- Docker deployment

## License

MIT

