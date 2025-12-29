# Help & Troubleshooting

## Quick Start

### Prerequisites

| Requirement | Version |
|-------------|---------|
| Java | 21+ |
| Node.js | 20+ |
| Docker | 20+ |
| Maven | 3.9+ |

### Running Locally

```bash
# Start all services with Docker
docker-compose --profile dev up -d

# Access points:
# Frontend: http://localhost:3000
# API Gateway: http://localhost:8080
# MailHog: http://localhost:8025
```

---

## Common Issues

### Database Connection Errors

**Problem**: "database does not exist"

**Solution**: Recreate the database volume:
```bash
docker-compose down -v
docker-compose --profile dev up -d
```

### Port Conflicts

**Problem**: "Port already in use"

**Solution**:
```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### CORS Errors

**Problem**: "Access-Control-Allow-Origin" errors

**Solution**: Ensure frontend runs on `http://localhost:3000` or update `FRONTEND_URL` environment variable.

### JWT Expired

**Problem**: API returns 401

**Solution**: The app handles token refresh automatically. If issues persist, clear localStorage and login again.

---

## Development

### Running Individual Services

```powershell
cd auth-service
$env:DB_PASSWORD = "your_password"
$env:SPRING_PROFILES_ACTIVE = "dev"
./mvnw.cmd spring-boot:run
```

### Running Tests

```bash
cd wallet-service
./mvnw.cmd test
```

### API Documentation

Each service has Swagger UI available in development mode:

| Service | URL |
|---------|-----|
| Auth | http://localhost:8081/swagger-ui.html |
| Wallet | http://localhost:8082/swagger-ui.html |
| Customer | http://localhost:8083/swagger-ui.html |
| Ledger | http://localhost:8084/swagger-ui.html |
| Notification | http://localhost:8085/swagger-ui.html |

---

## References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [React Documentation](https://react.dev/)
- [Ant Design Components](https://ant.design/components/overview/)
- [Docker Documentation](https://docs.docker.com/)

