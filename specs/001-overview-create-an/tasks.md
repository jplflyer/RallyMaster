# Tasks: Motorcycle Rally Management System - Desktop First Approach

**Input**: Design documents from `/specs/001-overview-create-an/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/, quickstart.md

## New Priority Order
```
1. Rally Master Desktop App (Local JSON storage)
2. REST API Server (PostgreSQL backend)  
3. Rally Master Desktop App + REST integration (Scoring support)
4. Rally Rider Desktop App (Route planning)
5. Web Interface (Simple UI)
6. Mobile Apps (iOS/Android)
```

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → Tech stack: Kotlin Compose Multiplatform (Desktop focus) + Java Spring Boot API
   → Structure: Desktop app first, then API server, then mobile
2. Load design documents:
   → data-model.md: 9 entities (Rally, BonusPoint, Combination, User, etc.)
   → Local JSON storage patterns for offline-first approach
   → API contracts for later server integration
3. Generate tasks by priority:
   → Phase 1: Desktop Rally Master app with local JSON
   → Phase 2: REST API server development
   → Phase 3: Desktop app + server integration
   → Phase 4: Rally Rider desktop functionality
   → Phase 5: Web interface
   → Phase 6: Mobile apps
4. Applied task rules:
   → TDD enforced: Tests before implementation
   → Desktop-first: Working local app before server complexity
   → Incremental: Each phase builds working software
5. Generated 55 numbered tasks across 6 phases
6. Focus: Get Rally Masters creating rallies quickly with local storage
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Exact file paths included for clarity

## Path Conventions
Based on new desktop-first approach:
- **Desktop App**: `mobile/src/commonMain/kotlin/`, `mobile/src/desktopMain/kotlin/`
- **API Server**: `api/src/main/java/`, `api/src/test/java/` (developed later)

## PHASE 1: Rally Master Desktop App (Local JSON Storage)
**Goal**: Working desktop app for Rally Masters to create and manage rallies locally

### Setup & Foundation
- [x] T001 Create project structure with api/ and mobile/ directories, configure Gradle build files
- [x] T002 Initialize Java Spring Boot project in api/ with PostgreSQL, Security, JPA, Testcontainers dependencies  
- [x] T003 Initialize Kotlin Compose Desktop project with local JSON storage capabilities
- [x] T004 Configure Spotless code formatting for Kotlin (included in T003)
- [x] T005 Set up file system structure for local JSON rally storage (included in T003)

### Tests First (TDD) ⚠️ MUST COMPLETE BEFORE IMPLEMENTATION
**CRITICAL: Desktop app tests MUST be written and MUST FAIL before implementation**

- [ ] T006 [P] Unit test Rally data model with JSON serialization in `mobile/src/commonTest/kotlin/com/rallymaster/model/RallyTest.kt`
- [ ] T007 [P] Unit test BonusPoint model with coordinate validation in `mobile/src/commonTest/kotlin/com/rallymaster/model/BonusPointTest.kt`
- [ ] T008 [P] Unit test Combination model in `mobile/src/commonTest/kotlin/com/rallymaster/model/CombinationTest.kt`
- [ ] T009 [P] Unit test JSON file storage service in `mobile/src/commonTest/kotlin/com/rallymaster/data/JsonStorageServiceTest.kt`
- [ ] T010 [P] Integration test "Create New Rally" workflow in `mobile/src/desktopTest/kotlin/com/rallymaster/ui/CreateRallyWorkflowTest.kt`
- [ ] T011 [P] Integration test "Add Bonus Points" workflow in `mobile/src/desktopTest/kotlin/com/rallymaster/ui/BonusPointWorkflowTest.kt`
- [ ] T012 [P] Integration test "Create Combinations" workflow in `mobile/src/desktopTest/kotlin/com/rallymaster/ui/CombinationWorkflowTest.kt`

### Core Desktop Implementation (ONLY after tests are failing)

#### Data Models [P] - Local JSON-friendly entities
- [x] T013 [P] Rally data class with JSON serialization in `mobile/src/commonMain/kotlin/com/rallymaster/model/Rally.kt`
- [x] T014 [P] BonusPoint data class in `mobile/src/commonMain/kotlin/com/rallymaster/model/BonusPoint.kt`
- [x] T015 [P] Combination data class in `mobile/src/commonMain/kotlin/com/rallymaster/model/Combination.kt`
- [x] T016 [P] RallyStatus and other enums + User, RallyRegistration, Route models added

#### Local Storage Layer [P]
- [ ] T017 [P] JsonStorageService for file I/O in `mobile/src/commonMain/kotlin/com/rallymaster/data/JsonStorageService.kt`
- [ ] T018 [P] RallyRepository with local JSON backend in `mobile/src/commonMain/kotlin/com/rallymaster/data/RallyRepository.kt`
- [ ] T019 [P] FileManager for rally file organization in `mobile/src/commonMain/kotlin/com/rallymaster/data/FileManager.kt`

#### UI Layer - Compose Desktop screens
- [x] T020 MainScreen with rally list in `mobile/src/commonMain/kotlin/com/rallymaster/ui/MainScreen.kt`
- [ ] T021 CreateRallyScreen with form in `mobile/src/commonMain/kotlin/com/rallymaster/ui/CreateRallyScreen.kt`
- [ ] T022 EditRallyScreen in `mobile/src/commonMain/kotlin/com/rallymaster/ui/EditRallyScreen.kt`
- [ ] T023 BonusPointsScreen with CRUD operations in `mobile/src/commonMain/kotlin/com/rallymaster/ui/BonusPointsScreen.kt`
- [ ] T024 CombinationsScreen in `mobile/src/commonMain/kotlin/com/rallymaster/ui/CombinationsScreen.kt`
- [ ] T025 Navigation and app structure in `mobile/src/desktopMain/kotlin/com/rallymaster/Main.kt`

## PHASE 2: REST API Server (PostgreSQL Backend)
**Goal**: Server to support shared rallies and scoring

### API Server Setup
- [ ] T026 [P] Set up PostgreSQL database (standard, lat/long as doubles)
- [ ] T027 [P] Configure application.yml for standard PostgreSQL
- [ ] T028 [P] Create database migration scripts for all entities

### API Tests First (TDD)
- [ ] T029 [P] Contract test rally management endpoints in `api/src/test/java/com/rallymaster/contract/RallyContractTest.java`
- [ ] T030 [P] Contract test bonus point endpoints in `api/src/test/java/com/rallymaster/contract/BonusPointContractTest.java`
- [ ] T031 [P] Contract test combination endpoints in `api/src/test/java/com/rallymaster/contract/CombinationContractTest.java`
- [ ] T032 [P] Integration test rally creation workflow in `api/src/test/java/com/rallymaster/integration/RallyWorkflowTest.java`

### API Implementation
- [ ] T033 [P] Rally JPA entity in `api/src/main/java/com/rallymaster/model/Rally.java`
- [ ] T034 [P] BonusPoint JPA entity in `api/src/main/java/com/rallymaster/model/BonusPoint.java`
- [ ] T035 [P] Combination JPA entity in `api/src/main/java/com/rallymaster/model/Combination.java`
- [ ] T036 RallyController REST endpoints in `api/src/main/java/com/rallymaster/controller/RallyController.java`
- [ ] T037 BonusPointController in `api/src/main/java/com/rallymaster/controller/BonusPointController.java`
- [ ] T038 CombinationController in `api/src/main/java/com/rallymaster/controller/CombinationController.java`

## PHASE 3: Desktop + Server Integration (Scoring Support)
**Goal**: Rally Master app can publish to server and support scoring workflow

- [ ] T039 HTTP client in desktop app for server communication in `mobile/src/commonMain/kotlin/com/rallymaster/data/ApiClient.kt`
- [ ] T040 Rally publishing feature - upload local rally to server
- [ ] T041 Scorer workflow screens for reviewing rider submissions  
- [ ] T042 Score calculation and leaderboard generation
- [ ] T043 Server sync for collaborative scoring between multiple scorers

## PHASE 4: Rally Rider Desktop Features
**Goal**: Same desktop app extended with rider functionality

- [ ] T044 Rally discovery and registration screens
- [ ] T045 Route planning UI with map integration
- [ ] T046 Route optimization algorithms  
- [ ] T047 Export functionality (GPX, KML, CSV formats)
- [ ] T048 Offline route storage and management

## PHASE 5: Simple Web Interface  
**Goal**: Basic web UI for rally discovery and registration

- [ ] T049 Simple HTML/CSS/JS web interface
- [ ] T050 Rally browsing and registration forms
- [ ] T051 Basic scorer web interface for remote scoring

## PHASE 6: Mobile Apps (iOS/Android)
**Goal**: Native mobile apps for rally navigation

- [ ] T052 Configure iOS/Android targets in Kotlin Multiplatform
- [ ] T053 Mobile-specific navigation UI
- [ ] T054 GPS integration and offline maps
- [ ] T055 Real-time navigation and timing features

## Dependencies

**Critical Path (TDD Enforcement)**:
- Phase 1 Tests (T006-T012) MUST complete and FAIL before implementation (T013-T025)
- Phase 2 Tests (T029-T032) MUST complete and FAIL before API implementation (T033-T038)

**Phase Dependencies**:
- Phase 1 → Working desktop app with local JSON
- Phase 2 → API server development (can be parallel to Phase 1 polish)
- Phase 3 → Requires both Phase 1 and Phase 2 complete
- Phase 4 → Extends Phase 1 desktop app
- Phase 5 → Requires Phase 2 API server
- Phase 6 → Requires Phases 2, 3, 4 for full functionality

## Parallel Execution Examples

### Phase 1: Launch all data model tests simultaneously
```bash
# All model tests can run in parallel (different test files)
Task: "Unit test Rally data model with JSON serialization in mobile/src/commonTest/kotlin/com/rallymaster/model/RallyTest.kt"
Task: "Unit test BonusPoint model with coordinate validation in mobile/src/commonTest/kotlin/com/rallymaster/model/BonusPointTest.kt"  
Task: "Unit test Combination model in mobile/src/commonTest/kotlin/com/rallymaster/model/CombinationTest.kt"
Task: "Unit test JSON file storage service in mobile/src/commonTest/kotlin/com/rallymaster/data/JsonStorageServiceTest.kt"
```

### Phase 1: Launch all data model implementations simultaneously (after tests fail)
```bash
# All data classes can be created in parallel (separate files)
Task: "Rally data class with JSON serialization in mobile/src/commonMain/kotlin/com/rallymaster/model/Rally.kt"
Task: "BonusPoint data class in mobile/src/commonMain/kotlin/com/rallymaster/model/BonusPoint.kt"
Task: "Combination data class in mobile/src/commonMain/kotlin/com/rallymaster/model/Combination.kt"
Task: "RallyStatus and other enums in mobile/src/commonMain/kotlin/com/rallymaster/model/Enums.kt"
```

## Notes
- **Desktop-first approach** gets working Rally Master app faster
- **Local JSON storage** eliminates server/database complexity initially  
- **TDD enforced** but with simpler desktop UI tests vs complex API integration tests
- **Incremental delivery** - each phase produces working software
- **Phase 1 focus** - Rally Masters can create complete rallies locally and export data files

## Task Generation Rules Applied
✅ **Desktop Priority**: Rally Master app with local storage comes first  
✅ **Incremental Phases**: Each phase delivers working functionality
✅ **TDD Enforced**: Tests written and failing before implementation in each phase
✅ **Parallel Optimization**: Independent files marked [P] for simultaneous work
✅ **Practical Focus**: Local JSON storage eliminates database setup complexity

## Validation Checklist
✅ Desktop Rally Master app can be built and used without server
✅ Each phase has clear deliverable working software
✅ TDD cycle maintained with simpler, more focused tests  
✅ Parallel tasks use different files with no dependencies
✅ Each task specifies exact file path
✅ Local storage approach eliminates PostGIS and server setup complexity