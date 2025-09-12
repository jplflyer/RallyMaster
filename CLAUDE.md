# RallyMaster Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-09-11

## Active Technologies
- Kotlin 1.9+, Java 21 (Spring Boot) (001-overview-create-an)
- Kotlin Compose Multiplatform, Spring Boot 3.2+, PostgreSQL driver, OpenStreetMap libraries (001-overview-create-an)
- JUnit 5 (Kotlin), Spring Boot Test, Testcontainers for integration tests (001-overview-create-an)
- PostgreSQL database (standard, no PostGIS), local JSON files for offline data (001-overview-create-an)

## Project Structure
```
api/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

mobile/
├── src/
│   ├── commonMain/
│   ├── androidMain/
│   ├── iosMain/
│   └── desktopMain/
└── tests/
```

## Commands
# Rally management CLI tools
rally-cli --help
navigation-cli --help  
scoring-cli --help

## Code Style
Kotlin: Follow standard conventions with Compose Multiplatform patterns
Java: Standard Spring Boot conventions with REST API design

## Performance Targets
- <2s map rendering, 60fps navigation, <500ms API response
- <100MB map cache per region, cross-platform UI consistency
- 1000 concurrent riders, 100 rallies/year, 50 screens across all apps

## Testing Requirements (NON-NEGOTIABLE)
- RED-GREEN-Refactor cycle enforced (contract tests first, then implementation)
- Order: Contract→Integration→E2E→Unit strictly followed
- Real dependencies used (PostgreSQL via Testcontainers, not H2)
- Integration tests for: API contracts, data sync, cross-platform UI

## Architecture Requirements
- Every feature as library (rally-core, navigation-core, scoring-core libraries)
- CLI per library with --help/--version/--format
- Repository pattern justified for offline requirements
- Offline-first design with eventual consistency

## Recent Changes
- 001-overview-create-an: Added motorcycle rally management system with multi-platform support

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->