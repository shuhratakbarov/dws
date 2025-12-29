# Contributing to Digital Wallet System

Thank you for considering contributing to this project! This is a learning project demonstrating microservices architecture, and contributions are welcome.

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](../../issues)
2. If not, create a new issue with:
   - Clear, descriptive title
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details (OS, Java version, Docker version)

### Suggesting Features

1. Open an issue with the `enhancement` label
2. Describe the feature and its use case
3. Discuss before implementing

### Pull Requests

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes
4. Write/update tests
5. Ensure all tests pass: `./mvnw test`
6. Commit with clear messages: `git commit -m 'Add amazing feature'`
7. Push to your fork: `git push origin feature/amazing-feature`
8. Open a Pull Request

## Development Setup

### Prerequisites

- Java 21+
- Node.js 20+
- Docker & Docker Compose
- Maven 3.9+

### Running Locally

```bash
# Clone the repo
git clone https://github.com/shuhratakbarov/dws.git
cd dws

# Copy environment file
cp .env.example .env
# Edit .env with your settings

# Start with Docker Compose (dev profile includes MailHog)
docker-compose --profile dev up -d

# Access the application
# Frontend: http://localhost:3000
# API Gateway: http://localhost:8080
# MailHog (email testing): http://localhost:8025
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run tests for specific service
cd wallet-service
./mvnw test
```

## Code Style

- Follow existing code patterns
- Use meaningful variable/method names
- Add comments for complex logic
- Keep methods focused and small

## Architecture Guidelines

- Each microservice should be independently deployable
- Use DTOs for API communication
- Handle errors gracefully with proper HTTP status codes
- Use idempotency keys for financial operations
- Write integration tests with Testcontainers

## Questions?

Feel free to open an issue for any questions about contributing.

