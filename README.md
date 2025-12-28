# Digital Wallet System (DWS)

[![CI/CD Pipeline](https://github.com/YOUR_USERNAME/DWS/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/DWS/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A production-ready microservices-based digital wallet system built with Spring Boot. Supports multi-currency wallets, secure transactions, and real-time notifications.

> **📚 Learning Project**: This project was built step-by-step as a learning journey into microservices architecture. See [DEPLOYMENT.md](./DEPLOYMENT.md) for deployment options.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Client                                      │
└─────────────────────────────────┬───────────────────────────────────────┘
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
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8080 | Routes requests, validates JWT, rate limiting |
| Auth Service | 8081 | User registration, login, JWT tokens |
| Wallet Service | 8082 | Wallets, deposits, withdrawals, transfers |
| Customer Service | 8083 | Customer profiles, KYC |
| Ledger Service | 8084 | Immutable transaction history, audit trail |
| Notification Service | 8085 | Email, SMS, Push notifications |
| MailHog (Dev) | 8025 | Email testing UI |

## Quick Start

### Option 1: Docker Compose (Recommended)

```bash
# Clone and navigate to project
cd DWS

# Create .env file
cp .env.example .env

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Option 2: Local Development

**Prerequisites:**
- Java 17+
- PostgreSQL 15
- Maven

**Create databases:**
```sql
CREATE DATABASE auth_db;
CREATE DATABASE wallet_db;
CREATE DATABASE customer_db;
```

**Start services (each in a separate terminal):**

```powershell
# Terminal 1 - API Gateway
cd api-gateway
$env:DB_PASSWORD = "your_password"
.\mvnw.cmd spring-boot:run

# Terminal 2 - Auth Service
cd auth-service
$env:DB_PASSWORD = "your_password"
.\mvnw.cmd spring-boot:run

# Terminal 3 - Wallet Service
cd wallet-service
$env:DB_PASSWORD = "your_password"
.\mvnw.cmd spring-boot:run

# Terminal 4 - Customer Service
cd customer-service
$env:DB_PASSWORD = "your_password"
.\mvnw.cmd spring-boot:run
```

## API Documentation (Swagger UI)

| Service | URL |
|---------|-----|
| Auth Service | http://localhost:8081/swagger-ui.html |
| Wallet Service | http://localhost:8082/swagger-ui.html |
| Customer Service | http://localhost:8083/swagger-ui.html |

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

### 3. Create Wallet (requires JWT)
```bash
curl -X POST http://localhost:8080/api/v1/wallets/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{"currency": "USD"}'
```

### 4. Deposit Funds
```bash
curl -X POST http://localhost:8080/api/v1/wallets/{walletId}/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "amountMinorUnits": 10000,
    "idempotencyKey": "unique-key-123",
    "description": "Initial deposit"
  }'
```

## Project Structure

```
DWS/
├── docker-compose.yml      # Docker orchestration
├── init-databases.sql      # DB initialization
├── api-gateway/            # API Gateway service
├── auth-service/           # Authentication service
├── wallet-service/         # Wallet management service
└── customer-service/       # Customer profile service
```

## Technology Stack

- **Java 17** - Programming language
- **Spring Boot 3.4** - Application framework
- **Spring Cloud Gateway** - API Gateway
- **Spring Security** - Authentication
- **Spring Data JPA** - Data access
- **PostgreSQL** - Database
- **JWT (jjwt)** - Token-based auth
- **Docker** - Containerization
- **Testcontainers** - Integration testing
- **OpenAPI/Swagger** - API documentation

## Key Features

- ✅ Multi-currency wallets (USD, EUR, UZS, etc.)
- ✅ Deposits and withdrawals
- ✅ Wallet-to-wallet transfers
- ✅ Idempotent transactions
- ✅ JWT authentication
- ✅ Centralized API Gateway
- ✅ Customer profiles
- ✅ KYC status tracking
- ✅ Balance reconciliation
- ✅ Comprehensive API documentation

## License

MIT

