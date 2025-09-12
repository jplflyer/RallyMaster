# Rally Master - Motorcycle Rally Management System

A multi-platform motorcycle rally management system built with Kotlin Compose Multiplatform and Java Spring Boot.

## Project Structure

```
rally-master/
├── api/                                    # Spring Boot REST API Server
│   ├── src/main/java/com/rallymaster/
│   │   ├── model/                         # JPA Entities
│   │   ├── repository/                    # Spring Data Repositories  
│   │   ├── service/                       # Business Logic
│   │   ├── controller/                    # REST Controllers
│   │   ├── config/                        # Configuration Classes
│   │   └── dto/                          # Data Transfer Objects
│   ├── src/test/java/com/rallymaster/
│   │   ├── contract/                     # Contract Tests
│   │   ├── integration/                  # Integration Tests
│   │   └── unit/                        # Unit Tests
│   └── src/main/resources/
│       └── db/migration/                 # Flyway Database Migrations
│
├── mobile/                               # Kotlin Compose Multiplatform App
│   ├── src/commonMain/kotlin/com/rallymaster/
│   │   ├── ui/                          # Compose UI Components
│   │   ├── data/                        # Data Layer
│   │   ├── domain/                      # Business Logic
│   │   └── util/                        # Utilities
│   ├── src/desktopMain/kotlin/          # Desktop-specific code
│   ├── src/androidMain/kotlin/          # Android-specific code (future)
│   ├── src/iosMain/kotlin/              # iOS-specific code (future)
│   └── src/commonTest/kotlin/           # Shared Tests
│
├── build.gradle.kts                     # Root build configuration
├── settings.gradle.kts                  # Multi-project settings
└── gradle.properties                    # Build properties
```

## Getting Started

### Prerequisites
- JDK 21
- PostgreSQL 15+
- Git

### Initial Setup
1. Clone the repository
2. Run task T002 to initialize Spring Boot dependencies
3. Run task T003 to initialize Kotlin Multiplatform dependencies
4. Set up PostgreSQL database (task T005)
5. Run tests to verify setup

### Technology Stack

**Backend (API)**:
- Java 21 & Kotlin JVM
- Spring Boot 3.2+
- PostgreSQL (standard, lat/long as doubles)
- JWT Authentication
- Testcontainers for testing

**Frontend (Mobile)**:
- Kotlin Compose Multiplatform
- Ktor for HTTP client
- Kotlinx Serialization
- Material 3 Design

### Development Workflow

This project follows **Test-Driven Development (TDD)** with a **Desktop-First Approach**:
1. **Phase 1**: Rally Master Desktop App with local JSON storage (T003-T025)
2. **Phase 2**: REST API Server development (T026-T038) 
3. **Phase 3+**: Integration, rider features, web, and mobile apps

**Important**: All tests must be written and failing before any implementation begins (RED-GREEN-REFACTOR cycle).

### Architecture

The system is designed with offline-first capabilities:
- API server handles shared data and synchronization
- Mobile apps can operate offline with local JSON storage
- Repository pattern abstracts local vs remote data sources
- Real-time sync when connectivity is available

### Build Commands

```bash
# Build all projects
./gradlew build

# Run API server
./gradlew :api:bootRun

# Run desktop app
./gradlew :mobile:run

# Run all tests
./gradlew test

# Format code
./gradlew spotlessApply
```

## Current Status

✅ **T001 COMPLETED**: Project structure and Gradle configuration
✅ **T002 COMPLETED**: Spring Boot initialization with dependencies, security, JPA, and testing setup
✅ **T003 COMPLETED**: Kotlin Compose Desktop project with local JSON storage capabilities
✅ **T004-T005 COMPLETED**: Spotless formatting and file system structure (included in T003)
✅ **TASKS RESTRUCTURED**: Desktop-first approach prioritizing Rally Master app with local JSON storage
⏳ **Next**: T006-T012 - Write failing tests before implementation (TDD approach)

### Verification Commands

```bash
# Verify API project builds and tests pass
./gradlew :api:test

# Verify application starts (will need database for full startup)
./gradlew :api:bootRun
```

See `specs/001-overview-create-an/tasks.md` for complete task breakdown and execution plan.