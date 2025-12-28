# Deployment Guide

This guide covers deploying the Digital Wallet System to various cloud platforms.

## Deployment Options Comparison

| Platform | Monthly Cost | Best For | Complexity |
|----------|--------------|----------|------------|
| **Heroku** | ~$35-50 | Learning, demos | Easy |
| **Railway** | ~$5-20 | Startups, microservices | Easy |
| **Render** | ~$7-35 | Full control | Medium |
| **Fly.io** | ~$0-20 | Global distribution | Medium |
| **AWS/GCP/Azure** | Varies | Production | Complex |

---

## Option 1: Heroku Deployment

### Understanding Heroku Costs with GitHub Student Pack

With **$13/month for 24 months** ($312 total), you need to be strategic:

| Configuration | Services | Cost/Month | Notes |
|---------------|----------|------------|-------|
| Minimal | 2 Eco dynos + Postgres Mini | ~$10 | Combine services |
| Basic | 3 Basic dynos + Postgres Mini | ~$26 | Separate core services |
| Full | 6 Basic dynos + Postgres | ~$47 | All microservices |

### Recommended: Combined Services Approach

Combine services to stay within budget:

```
┌─────────────────────────────────────────────────────┐
│  dws-gateway-auth (API Gateway + Auth)    - $7/mo  │
├─────────────────────────────────────────────────────┤
│  dws-core (Wallet + Customer + Ledger)    - $7/mo  │
├─────────────────────────────────────────────────────┤
│  PostgreSQL Mini                          - $5/mo  │
└─────────────────────────────────────────────────────┘
Total: ~$19/month (within budget!)
```

### Step-by-Step Heroku Deployment

#### 1. Install Heroku CLI
```bash
# Windows (using Chocolatey)
choco install heroku-cli

# Or download from https://devcenter.heroku.com/articles/heroku-cli
```

#### 2. Login and Create Apps
```bash
heroku login

# Create apps
heroku create dws-gateway --region us
heroku create dws-core --region us

# Add PostgreSQL
heroku addons:create heroku-postgresql:mini -a dws-core
```

#### 3. Configure Environment Variables
```bash
# For dws-gateway
heroku config:set JWT_SECRET=your-production-secret-key-here -a dws-gateway
heroku config:set CORE_SERVICE_URL=https://dws-core.herokuapp.com -a dws-gateway

# For dws-core
heroku config:set JWT_SECRET=your-production-secret-key-here -a dws-core
heroku config:set SPRING_PROFILES_ACTIVE=production -a dws-core
```

#### 4. Deploy Using Container Registry
```bash
# Login to Heroku Container Registry
heroku container:login

# Build and push (from each service directory)
cd api-gateway
heroku container:push web -a dws-gateway
heroku container:release web -a dws-gateway
```

---

## Option 2: Railway Deployment (Recommended for Learning)

Railway offers better free tier and easier microservices deployment.

### Why Railway?
- $5/month free credits with GitHub Student
- Native Docker support
- Built-in PostgreSQL
- Automatic deployments from GitHub
- Better suited for microservices

### Steps:
1. Go to [railway.app](https://railway.app)
2. Sign in with GitHub
3. Create new project → Deploy from GitHub repo
4. Add PostgreSQL service
5. Configure environment variables
6. Deploy!

### Railway Configuration (railway.json)
```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "DOCKERFILE"
  },
  "deploy": {
    "startCommand": "java -jar app.jar",
    "healthcheckPath": "/actuator/health"
  }
}
```

---

## Option 3: Render Deployment

Render offers a generous free tier with auto-sleep.

### Render Blueprint (render.yaml)
```yaml
services:
  - type: web
    name: dws-api-gateway
    env: docker
    dockerfilePath: ./api-gateway/Dockerfile
    healthCheckPath: /actuator/health
    envVars:
      - key: JWT_SECRET
        generateValue: true
      - key: SPRING_PROFILES_ACTIVE
        value: production

databases:
  - name: dws-postgres
    plan: free
```

---

## Option 4: Fly.io Deployment

Fly.io is great for global distribution and has a generous free tier.

### fly.toml Example
```toml
app = "dws-api-gateway"
primary_region = "iad"

[build]
  dockerfile = "Dockerfile"

[http_service]
  internal_port = 8080
  force_https = true
  auto_stop_machines = true
  auto_start_machines = true
  min_machines_running = 0

[checks]
  [checks.health]
    port = 8080
    type = "http"
    interval = "30s"
    timeout = "5s"
    path = "/actuator/health"
```

### Deploy Commands
```bash
# Install Fly CLI
curl -L https://fly.io/install.sh | sh

# Login
fly auth login

# Deploy
fly deploy

# Scale down to save costs
fly scale count 1
```

---

## Production Checklist

Before deploying to production:

- [ ] Change all default passwords and secrets
- [ ] Enable HTTPS only
- [ ] Set up proper logging (consider Papertrail, Loggly)
- [ ] Configure database backups
- [ ] Set up monitoring (UptimeRobot, Better Stack)
- [ ] Review rate limiting settings
- [ ] Test all endpoints
- [ ] Set up error tracking (Sentry)

---

## Environment Variables Reference

| Variable | Service | Description |
|----------|---------|-------------|
| `JWT_SECRET` | All | JWT signing key (min 32 chars) |
| `DATABASE_URL` | Core services | PostgreSQL connection string |
| `SPRING_PROFILES_ACTIVE` | All | Set to `production` |
| `MAIL_HOST` | Notification | SMTP server |
| `MAIL_USERNAME` | Notification | SMTP username |
| `MAIL_PASSWORD` | Notification | SMTP password |

---

## Quick Start Commands

```bash
# Test locally with Docker
docker-compose up -d

# Run tests
./mvnw clean verify

# Build all services
./mvnw clean package -DskipTests

# Check service health
curl http://localhost:8080/actuator/health
```

