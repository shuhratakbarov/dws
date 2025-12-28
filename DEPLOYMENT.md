# Deployment Guide

This guide covers deploying the Digital Wallet System to various cloud platforms.

## Table of Contents
- [Deployment Options](#deployment-options-comparison)
- [Heroku Deployment (Eco Dynos)](#option-1-heroku-deployment-with-eco-dynos)
- [Railway Deployment](#option-2-railway-deployment)
- [Docker Deployment](#option-3-docker-deployment)
- [Environment Configuration](#environment-configuration)
- [Production Checklist](#production-checklist)

## Deployment Options Comparison

| Platform | Monthly Cost | Best For | Complexity |
|----------|--------------|----------|------------|
| **Heroku Eco** | ~$5-10 | Learning (with Student Pack) | Easy |
| **Railway** | ~$5-20 | Startups, microservices | Easy |
| **Render** | ~$7-35 | Full control | Medium |
| **Fly.io** | ~$0-20 | Global distribution | Medium |
| **AWS/GCP/Azure** | Varies | Production | Complex |

---

## Option 1: Heroku Deployment with Eco Dynos

### Heroku Eco Plan Explained

**Eco Plan**: $5/month for a **shared pool of 1000 dyno-hours**

This means:
- All your Eco dynos share from the same 1000-hour pool
- Dynos auto-sleep after 30 minutes of inactivity
- Perfect for development/testing (not 24/7 production)

### Cost Calculation for Your 7 Services

| Running Hours | Calculation | Monthly Usage |
|---------------|-------------|---------------|
| 6 hours/day × 30 days | 6 × 7 services × 30 | 1,260 hours ❌ |
| 5 hours/day × 30 days | 5 × 7 services × 30 | 1,050 hours ❌ |
| 4 hours/day × 30 days | 4 × 7 services × 30 | 840 hours ✅ |
| 3 hours/day × 30 days | 3 × 7 services × 30 | 630 hours ✅ |

**Recommendation**: Run services ~4 hours/day to stay under 1000 hours.

### Total Cost with GitHub Student Pack

| Resource | Cost |
|----------|------|
| Eco Dyno Pool (1000 hrs) | $5/month |
| PostgreSQL Mini | $5/month |
| **Total** | **$10/month** ✅ |

With $13/month credits, you have $3/month extra for overage protection!

### Step-by-Step Deployment

#### 1. Install Heroku CLI

```powershell
# Using Chocolatey
choco install heroku-cli

# Or download from https://devcenter.heroku.com/articles/heroku-cli
```

#### 2. Login and Create Apps

```bash
heroku login

# Create all 7 apps (they share the Eco dyno pool)
heroku create dws-api-gateway --region us
heroku create dws-auth --region us
heroku create dws-wallet --region us
heroku create dws-customer --region us
heroku create dws-ledger --region us
heroku create dws-notification --region us
heroku create dws-frontend --region us

# Add PostgreSQL (shared by all backend services)
heroku addons:create heroku-postgresql:mini -a dws-wallet
```

#### 3. Get Database URL and Configure Services

```bash
# Get the DATABASE_URL
heroku config:get DATABASE_URL -a dws-wallet

# Set environment variables for each service
heroku config:set \
  DATABASE_URL=postgres://... \
  JWT_SECRET=your-super-secret-key-at-least-32-chars \
  SPRING_PROFILES_ACTIVE=prod \
  -a dws-auth

heroku config:set \
  DATABASE_URL=postgres://... \
  JWT_SECRET=your-super-secret-key-at-least-32-chars \
  SPRING_PROFILES_ACTIVE=prod \
  LEDGER_SERVICE_URL=https://dws-ledger.herokuapp.com \
  NOTIFICATION_SERVICE_URL=https://dws-notification.herokuapp.com \
  -a dws-wallet

# Set API Gateway routes
heroku config:set \
  JWT_SECRET=your-super-secret-key-at-least-32-chars \
  AUTH_SERVICE_URL=https://dws-auth.herokuapp.com \
  WALLET_SERVICE_URL=https://dws-wallet.herokuapp.com \
  CUSTOMER_SERVICE_URL=https://dws-customer.herokuapp.com \
  LEDGER_SERVICE_URL=https://dws-ledger.herokuapp.com \
  NOTIFICATION_SERVICE_URL=https://dws-notification.herokuapp.com \
  -a dws-api-gateway

# Set frontend API URL
heroku config:set \
  VITE_API_URL=https://dws-api-gateway.herokuapp.com/api/v1 \
  -a dws-frontend
```

#### 4. Deploy Each Service

```bash
# Login to Heroku Container Registry
heroku container:login

# Deploy API Gateway
cd api-gateway
./mvnw clean package -DskipTests
heroku container:push web -a dws-api-gateway
heroku container:release web -a dws-api-gateway

# Deploy Auth Service
cd ../auth-service
./mvnw clean package -DskipTests
heroku container:push web -a dws-auth
heroku container:release web -a dws-auth

# Repeat for other services...

# Deploy Frontend
cd ../frontend
heroku container:push web -a dws-frontend
heroku container:release web -a dws-frontend
```

#### 5. Scale Down When Not Using (Save Hours!)

```bash
# Sleep all dynos at night
heroku ps:scale web=0 -a dws-api-gateway
heroku ps:scale web=0 -a dws-auth
heroku ps:scale web=0 -a dws-wallet
# ... repeat for all services

# Wake up in morning
heroku ps:scale web=1 -a dws-api-gateway
heroku ps:scale web=1 -a dws-auth
heroku ps:scale web=1 -a dws-wallet
# ... repeat for all services
```

---

## Option 2: Railway Deployment

Railway offers better microservices support and automatic GitHub deployments.

### Why Railway?
- $5/month free credits with GitHub Student
- Auto-deploy from GitHub pushes
- Built-in PostgreSQL
- Better cold-start times than Heroku Eco

### Steps

1. Go to [railway.app](https://railway.app)
2. Sign in with GitHub
3. Create new project → Deploy from GitHub repo
4. Railway auto-detects your services from Dockerfiles
5. Add PostgreSQL service
6. Configure environment variables
7. Deploy!

### Railway Configuration

Each service needs a `railway.json` in its directory:

```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "DOCKERFILE"
  },
  "deploy": {
    "healthcheckPath": "/actuator/health",
    "restartPolicyType": "ON_FAILURE"
  }
}
```

---

## Option 3: Docker Deployment

For self-hosted or VPS deployment.

```bash
# Build all images
docker-compose build

# Start with production profile
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

---

## Environment Configuration

### Backend Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `DATABASE_URL` | ✅ | PostgreSQL connection string |
| `JWT_SECRET` | ✅ | JWT signing key (min 32 chars) |
| `SPRING_PROFILES_ACTIVE` | ✅ | `dev`, `test`, or `prod` |
| `AUTH_SERVICE_URL` | Gateway | Auth service URL |
| `WALLET_SERVICE_URL` | Gateway | Wallet service URL |
| `CUSTOMER_SERVICE_URL` | Gateway | Customer service URL |
| `LEDGER_SERVICE_URL` | Wallet | Ledger service URL |
| `NOTIFICATION_SERVICE_URL` | Wallet | Notification service URL |
| `MAIL_HOST` | Notification | SMTP server |
| `MAIL_USERNAME` | Notification | SMTP username |
| `MAIL_PASSWORD` | Notification | SMTP password |

### Frontend Environment Variables

| Variable | Description |
|----------|-------------|
| `VITE_API_URL` | API Gateway URL |
| `VITE_APP_ENV` | `development` or `production` |

---

## Production Checklist

Before deploying to production:

### Security
- [ ] Change all default passwords and secrets
- [ ] Use strong JWT_SECRET (min 256 bits)
- [ ] Enable HTTPS only
- [ ] Set restrictive CORS origins
- [ ] Review rate limiting settings

### Database
- [ ] Configure database backups
- [ ] Set `ddl-auto: validate` (not `update`)
- [ ] Test database migrations

### Monitoring
- [ ] Set up logging (Papertrail, Loggly)
- [ ] Set up uptime monitoring (UptimeRobot, Better Stack)
- [ ] Set up error tracking (Sentry)
- [ ] Enable health check endpoints

### Performance
- [ ] Enable gzip compression
- [ ] Configure connection pool sizes
- [ ] Test under load

### Operations
- [ ] Document deployment process
- [ ] Create rollback procedure
- [ ] Set up alerting

---

## Quick Reference Commands

```bash
# Test locally with Docker
docker-compose up -d

# Run tests
./mvnw clean verify

# Build all services
./mvnw clean package -DskipTests

# Check service health
curl http://localhost:8080/actuator/health

# View logs
docker-compose logs -f wallet-service

# Scale down Heroku (save hours)
for app in dws-api-gateway dws-auth dws-wallet dws-customer dws-ledger dws-notification dws-frontend; do
  heroku ps:scale web=0 -a $app
done
```
