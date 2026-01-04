.PHONY: help all build build-app build-docker start stop restart logs test test-unit test-integration clean clean-all status

# Default target - build everything
all: build

##@ General

help: ## Display this help message
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m<target>\033[0m\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Build

build: build-app build-docker ## Build application and Docker images

build-app: ## Build the Spring Boot application with Gradle
	@echo "Building Spring Boot application..."
	./gradlew :RallyServer:bootJar -x test
	@echo "✓ Application built successfully"

build-docker: ## Build Docker images (requires build-app first)
	@echo "Building Docker images..."
	docker-compose build
	@echo "✓ Docker images built successfully"

##@ Docker Operations

start: ## Start all services (PostgreSQL + RallyServer)
	@echo "Starting all services..."
	docker-compose up -d
	@echo "✓ Services started"
	@echo "Server: http://localhost:8080"
	@echo "Health: http://localhost:8080/actuator/health"
	@echo "Swagger: http://localhost:8080/api-docs"

stop: ## Stop all services
	@echo "Stopping all services..."
	docker-compose down
	@echo "✓ Services stopped"

restart: stop start ## Restart all services

logs: ## Show logs from all services (Ctrl+C to exit)
	docker-compose logs -f

logs-server: ## Show logs from RallyServer only
	docker-compose logs -f rallyserver

logs-db: ## Show logs from PostgreSQL only
	docker-compose logs -f postgres

status: ## Show status of all containers
	@docker-compose ps

##@ Testing

test: test-unit test-integration ## Run all tests (unit + integration)

test-unit: ## Run unit tests only (*Test.java)
	@echo "Running unit tests..."
	./gradlew test
	@echo "✓ Unit tests complete"

test-integration: ## Run integration tests (*IT.java)
	@echo "Running integration tests..."
	./gradlew integrationTest
	@echo "✓ Integration tests complete"

test-clean: ## Run tests against fresh Docker database
	@echo "Starting fresh database..."
	docker-compose down -v postgres
	docker-compose up -d postgres
	@echo "Waiting for database to be ready..."
	@sleep 5
	@echo "Running integration tests..."
	./gradlew integrationTest

##@ Database

db-start: ## Start only PostgreSQL (for local development)
	@echo "Starting PostgreSQL..."
	docker-compose up -d postgres
	@echo "✓ PostgreSQL started on localhost:5432"

db-stop: ## Stop PostgreSQL
	@echo "Stopping PostgreSQL..."
	docker-compose stop postgres
	@echo "✓ PostgreSQL stopped"

db-logs: ## Show PostgreSQL logs
	docker-compose logs -f postgres

db-connect: ## Connect to PostgreSQL with psql
	docker exec -it rallymaster-postgres psql -U rallymaster -d rallymaster

##@ Cleanup

clean: ## Stop containers and remove volumes (clean slate)
	@echo "Stopping all services and removing volumes..."
	docker-compose down -v
	@echo "✓ All containers and volumes removed"

clean-all: clean ## Remove everything including Docker images and Gradle build artifacts
	@echo "Removing Docker images..."
	docker-compose down -v --rmi all
	@echo "Cleaning Gradle build artifacts..."
	./gradlew clean
	@echo "✓ Complete cleanup done"

##@ Development

dev: ## Start PostgreSQL only (for IntelliJ development)
	@echo "Starting PostgreSQL for local development..."
	docker-compose up -d postgres
	@echo "✓ PostgreSQL running on localhost:5432"
	@echo "Run RallyServer from IntelliJ or: ./gradlew :RallyServer:bootRun"

rebuild: ## Rebuild and restart the server (after code changes)
	@echo "Rebuilding application..."
	./gradlew :RallyServer:bootJar -x test
	@echo "Rebuilding and restarting server container..."
	docker-compose up -d --build rallyserver
	@echo "✓ Server rebuilt and restarted"
