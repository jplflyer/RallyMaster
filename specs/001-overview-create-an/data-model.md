# Data Model: Motorcycle Rally Management System

## Core Entities

### Rally
**Purpose**: Represents a motorcycle rally event with all configuration and metadata

**Fields**:
- `id: UUID` - Unique identifier
- `name: String` - Rally display name (e.g., "Dog Daze Rally 2025")
- `description: String` - Rally description and rules
- `startDate: LocalDateTime` - Rally start date and time
- `endDate: LocalDateTime` - Rally end date and time
- `location: String` - Base location (e.g., "Brainerd, Minnesota")
- `createdBy: UUID` - Rally Master user ID
- `storageMode: StorageMode` - LOCAL or SERVER
- `status: RallyStatus` - DRAFT, PUBLISHED, ACTIVE, COMPLETED, CANCELLED
- `createdAt: Instant` - Creation timestamp
- `updatedAt: Instant` - Last modification timestamp

**Relationships**:
- One-to-many: BonusPoints
- One-to-many: Combinations  
- One-to-many: RallyRegistrations
- Many-to-one: User (Rally Master)

**Validation Rules**:
- Name must be unique within Rally Master's rallies
- End date must be after start date
- Cannot modify published rallies (status PUBLISHED+)

### BonusPoint
**Purpose**: Individual location that riders can visit to earn points

**Fields**:
- `id: UUID` - Unique identifier
- `rallyId: UUID` - Associated rally
- `name: String` - Display name (e.g., "Historic Water Tower")
- `abbreviation: String` - Short code (e.g., "HWT", max 6 chars)
- `latitude: Double` - GPS latitude (-90.0 to 90.0, standard WGS84)
- `longitude: Double` - GPS longitude (-180.0 to 180.0, standard WGS84)
- `points: Int` - Point value (10-75 typical range)
- `description: String` - Detailed description for riders
- `address: String?` - Optional street address
- `createdAt: Instant` - Creation timestamp

**Relationships**:
- Many-to-one: Rally
- Many-to-many: Combinations (through CombinationBonusPoint)

**Validation Rules**:
- Abbreviation must be unique within rally
- Points must be positive integer
- Latitude must be between -90.0 and 90.0 (decimal degrees)
- Longitude must be between -180.0 and 180.0 (decimal degrees)
- Name must be unique within rally

### Combination
**Purpose**: Group of bonus points that yield additional points when all are completed

**Fields**:
- `id: UUID` - Unique identifier
- `rallyId: UUID` - Associated rally
- `name: String` - Combination name (e.g., "Lakes Loop")
- `points: Int` - Bonus points for completion (200-1000 typical)
- `description: String?` - Optional description
- `isRequired: Boolean` - Whether all bonus points required (vs. minimum count)
- `minimumCount: Int?` - Minimum bonus points needed (if not required=all)
- `createdAt: Instant` - Creation timestamp

**Relationships**:
- Many-to-one: Rally
- Many-to-many: BonusPoints (through CombinationBonusPoint)

**Validation Rules**:
- Must contain at least 2 bonus points
- Points must be positive integer
- If not isRequired, minimumCount must be specified and >= 2

### CombinationBonusPoint
**Purpose**: Junction table linking combinations to their required bonus points

**Fields**:
- `combinationId: UUID` - Combination reference
- `bonusPointId: UUID` - BonusPoint reference
- `order: Int?` - Optional ordering hint for optimal route

**Relationships**:
- Many-to-one: Combination
- Many-to-one: BonusPoint

### User
**Purpose**: System user with role-based access (Rally Master, Rider, Scorer)

**Fields**:
- `id: UUID` - Unique identifier  
- `username: String` - Login username (unique)
- `email: String` - Email address (unique)
- `passwordHash: String` - Hashed password
- `firstName: String` - Given name
- `lastName: String` - Family name
- `roles: Set<UserRole>` - RALLY_MASTER, RIDER, SCORER
- `isActive: Boolean` - Account status
- `createdAt: Instant` - Registration timestamp
- `lastLogin: Instant?` - Last successful login

**Relationships**:
- One-to-many: Rallies (as Rally Master)
- One-to-many: RallyRegistrations (as Rider)
- One-to-many: RiderScores (as Rider)

**Validation Rules**:
- Username 3-30 characters, alphanumeric + underscore
- Valid email format
- Password minimum 8 characters
- Must have at least one role

### RallyRegistration  
**Purpose**: Rider registration for specific rally with assigned rider number

**Fields**:
- `id: UUID` - Unique identifier
- `rallyId: UUID` - Rally reference
- `riderId: UUID` - User reference (with RIDER role)
- `riderNumber: Int` - Assigned sequential number within rally
- `registrationDate: Instant` - When registered
- `status: RegistrationStatus` - REGISTERED, CANCELLED, DNF, FINISHED

**Relationships**:
- Many-to-one: Rally
- Many-to-one: User (Rider)
- One-to-many: RiderScores

**Validation Rules**:
- Rider number unique within rally
- Cannot register for past rallies
- Cannot register twice for same rally

### Route
**Purpose**: Planned sequence of bonus point visits with timing and alternatives

**Fields**:
- `id: UUID` - Unique identifier
- `rallyRegistrationId: UUID` - Associated registration
- `name: String` - Route name (e.g., "Northern Loop Strategy")  
- `plannedBonusPoints: List<UUID>` - Ordered list of bonus point IDs
- `estimatedDuration: Duration` - Total estimated time
- `estimatedDistance: Double` - Total distance in miles/km
- `alternativeRoutes: Map<String, List<UUID>>` - Named alternative sub-routes
- `exportFormats: Set<ExportFormat>` - GPX, KML, CSV, JSON
- `createdAt: Instant` - Creation timestamp
- `isActive: Boolean` - Current active route for rider

**Relationships**:
- Many-to-one: RallyRegistration
- Many-to-many: BonusPoints (through route ordering)

**Validation Rules**:
- Must contain at least one bonus point
- Bonus points must belong to same rally
- Only one active route per registration

### RiderScore
**Purpose**: Individual rider's claimed and approved bonus points for scoring

**Fields**:
- `id: UUID` - Unique identifier
- `rallyRegistrationId: UUID` - Associated registration
- `bonusPointId: UUID` - Claimed bonus point
- `claimedAt: Instant` - When rider claimed the point
- `isApproved: Boolean` - Scorer approval (defaults true)
- `scorerNotes: String?` - Optional scorer comments
- `scoredBy: UUID?` - Scorer user ID
- `scoredAt: Instant?` - When scoring occurred
- `photoUrl: String?` - Optional photo evidence URL

**Relationships**:
- Many-to-one: RallyRegistration
- Many-to-one: BonusPoint
- Many-to-one: User (Scorer)

**Validation Rules**:
- Cannot claim same bonus point twice
- claimedAt must be within rally date range
- Scorer must have SCORER role

### RallyResults
**Purpose**: Calculated final results and leaderboard for completed rally

**Fields**:
- `id: UUID` - Unique identifier
- `rallyId: UUID` - Associated rally
- `riderId: UUID` - Rider reference
- `totalPoints: Int` - Final calculated score
- `bonusPointsEarned: Int` - Count of approved bonus points
- `combinationsCompleted: Int` - Count of completed combinations
- `combinationDetails: Map<UUID, Int>` - Combination ID to points earned
- `finalPosition: Int?` - Leaderboard ranking (1st, 2nd, etc.)
- `calculatedAt: Instant` - When results computed
- `isOfficial: Boolean` - Whether results are final/official

**Relationships**:
- Many-to-one: Rally  
- Many-to-one: User (Rider)

**Validation Rules**:
- Can only be calculated after rally end date
- Total points must equal sum of individual + combination points

## Enumerations

### StorageMode
- `LOCAL` - Rally data stored locally only
- `SERVER` - Rally data stored on server with local backup

### RallyStatus  
- `DRAFT` - Rally being created, not visible to riders
- `PUBLISHED` - Rally published, riders can register
- `ACTIVE` - Rally in progress
- `COMPLETED` - Rally finished, scoring in progress
- `CANCELLED` - Rally cancelled

### UserRole
- `RALLY_MASTER` - Can create and manage rallies
- `RIDER` - Can register for rallies and submit routes  
- `SCORER` - Can score rider submissions

### RegistrationStatus
- `REGISTERED` - Active registration
- `CANCELLED` - Rider cancelled registration
- `DNF` - Did not finish rally
- `FINISHED` - Completed rally

### ExportFormat
- `GPX` - GPS Exchange format for Garmin devices
- `KML` - Google Earth format  
- `KMZ` - Compressed KML
- `CSV` - Comma-separated values for printing
- `JSON` - Machine-readable format

## State Transitions

### Rally Lifecycle
```
DRAFT → PUBLISHED → ACTIVE → COMPLETED
   ↓        ↓         ↓
CANCELLED ← CANCELLED ← CANCELLED
```

### Registration Lifecycle  
```
REGISTERED → FINISHED
     ↓         ↑
   CANCELLED   DNF
```

## Data Relationships Summary

**Key Foreign Key Relationships**:
- Rally → User (Rally Master)  
- BonusPoint → Rally
- Combination → Rally
- RallyRegistration → Rally + User
- Route → RallyRegistration
- RiderScore → RallyRegistration + BonusPoint + User (Scorer)
- RallyResults → Rally + User (Rider)

**Many-to-Many Relationships**:
- Combination ↔ BonusPoint (via CombinationBonusPoint)
- Route ↔ BonusPoint (via ordered list)

## Storage Considerations

**PostgreSQL Schema**:
- Use UUIDs for all primary keys to support distributed/offline scenarios
- Standard B-tree indices on latitude/longitude columns for geographic queries
- Composite indices on rallyId + bonusPointId for performance
- JSON columns for flexible route alternatives and combination details
- Standard PostgreSQL data types (no spatial extensions required)

**Local JSON Structure**:
- Single rally file contains all related entities for offline operation
- Hierarchical structure: Rally → BonusPoints/Combinations → Routes/Scores
- Conflict resolution via timestamp comparison during synchronization