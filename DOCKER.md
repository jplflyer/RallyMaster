# Docker Setup for RallyMaster

This project includes Docker configuration for running the RallyServer with PostgreSQL.

## Prerequisites

- Docker Desktop installed
- Docker Compose installed (included with Docker Desktop)
- Make (optional but recommended - included on macOS/Linux, available on Windows)

## Quick Start

### Using Make (Recommended)

```bash
# See all available commands
make help

# Build and start everything
make build
make start

# Run tests
make test

# Stop everything
make stop
```

### Using Docker Compose Directly

```bash
# Start the entire stack (PostgreSQL + RallyServer)
docker-compose up -d
```

This will:
1. Start PostgreSQL on port 5432
2. Build and start RallyServer on port 8080
3. Run Flyway migrations automatically
4. Create persistent volumes for database data

### Check status

```bash
docker-compose ps
```

### View logs

```bash
# All services
docker-compose logs -f

# Just the server
docker-compose logs -f rallyserver

# Just the database
docker-compose logs -f postgres
```

### Stop the stack

```bash
docker-compose down
```

### Stop and remove volumes (clean slate)

```bash
docker-compose down -v
```

## Individual Services

### Start only PostgreSQL

```bash
docker-compose up -d postgres
```

Then you can run RallyServer from IntelliJ or command line:

```bash
./gradlew :RallyServer:bootRun
```

### Rebuild after code changes

```bash
docker-compose up -d --build rallyserver
```

## Health Checks

- Server health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/api-docs
- OpenAPI JSON: http://localhost:8080/api-docs/json

## Database Access

Connect to PostgreSQL from your host machine:

- Host: localhost
- Port: 5433 (mapped to avoid conflict with local PostgreSQL on 5432)
- Database: rallymaster
- Username: rallymaster
- Password: rallyhq

## Testing with Docker

### Running integration tests against Docker database

```bash
# Start just PostgreSQL
docker-compose up -d postgres

# Wait for it to be healthy
docker-compose ps

# Run integration tests
./gradlew integrationTest
```

### Clean database for fresh test run

```bash
docker-compose down -v postgres
docker-compose up -d postgres
# Wait a few seconds for initialization
./gradlew integrationTest
```

## Troubleshooting

### Port conflicts

If port 5432 or 8080 is already in use:

1. Edit `docker-compose.yml`
2. Change the port mapping, e.g., `"15432:5432"` for PostgreSQL
3. Update your local `application.yml` or use environment variables

### Database connection issues

Check that PostgreSQL is healthy:

```bash
docker-compose ps
docker-compose logs postgres
```

### Container won't start

View detailed logs:

```bash
docker-compose logs rallyserver
```

Rebuild from scratch:

```bash
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```

## Development Workflow

### Option 1: Full Docker (recommended for testing)

```bash
make build
make start
# Make code changes
make rebuild
```

Or with docker-compose:
```bash
docker-compose up -d
# Make code changes
docker-compose up -d --build rallyserver
```

### Option 2: Database in Docker, Server in IDE (recommended for development)

```bash
make dev
# Run server from IntelliJ IDEA
# Hot reload works normally
```

Or with docker-compose:
```bash
docker-compose up -d postgres
# Run server from IntelliJ IDEA
```

### Option 3: Everything local (existing workflow)

Continue using your existing PostgreSQL installation and IntelliJ.

## Files

- `Dockerfile` - Multi-stage build for RallyServer
- `docker-compose.yml` - Orchestrates PostgreSQL and RallyServer
- `RallyServer/src/main/resources/application-docker.yml` - Docker-specific Spring configuration
