# Rally Master - Quickstart Guide

## Overview
This guide validates the core user scenarios for the Rally Master motorcycle rally management system through integration test scenarios.

## Prerequisites
- PostgreSQL database running
- Rally Master API server started on localhost:8080
- Mobile/desktop client application installed

## Test Scenario 1: Rally Master Creates Rally

**Objective**: Validate FR-001, FR-002, FR-003 - Rally creation with bonus points and combinations

**Steps**:
1. **Register Rally Master Account**
   ```bash
   POST /api/v1/auth/register
   {
     "username": "testmaster",
     "email": "master@test.com", 
     "password": "password123",
     "firstName": "Test",
     "lastName": "Master",
     "roles": ["RALLY_MASTER"]
   }
   ```
   Expected: 201 Created with user object

2. **Login as Rally Master**
   ```bash
   POST /api/v1/auth/login
   {
     "username": "testmaster",
     "password": "password123"
   }
   ```
   Expected: 200 OK with JWT token

3. **Create New Rally**
   ```bash
   POST /api/v1/rallies
   Authorization: Bearer {token}
   {
     "name": "Test Rally 2025",
     "description": "A test rally for validation",
     "startDate": "2025-06-01T08:00:00Z",
     "endDate": "2025-06-01T18:00:00Z",
     "location": "Test City, Test State",
     "storageMode": "SERVER"
   }
   ```
   Expected: 201 Created with rally object, status=DRAFT

4. **Add Bonus Points**
   ```bash
   POST /api/v1/rallies/{rallyId}/bonus-points
   Authorization: Bearer {token}
   {
     "name": "Test Point 1",
     "abbreviation": "TP1",
     "latitude": 46.7844,
     "longitude": -94.1417,
     "points": 25,
     "description": "First test bonus point"
   }
   ```
   Repeat for 5 different bonus points
   Expected: 201 Created for each point

5. **Create Combination**
   ```bash
   POST /api/v1/rallies/{rallyId}/combinations
   Authorization: Bearer {token}
   {
     "name": "Test Combo",
     "points": 100,
     "description": "Test combination of 3 points",
     "isRequired": true,
     "bonusPointIds": ["{point1Id}", "{point2Id}", "{point3Id}"]
   }
   ```
   Expected: 201 Created with combination object

6. **Publish Rally**
   ```bash
   PUT /api/v1/rallies/{rallyId}
   Authorization: Bearer {token}
   {
     ...rallyData,
     "status": "PUBLISHED"
   }
   ```
   Expected: 200 OK, rally status updated to PUBLISHED

**Success Criteria**: Rally appears in public rally list, contains 5 bonus points and 1 combination

---

## Test Scenario 2: Rally Rider Registration and Route Planning

**Objective**: Validate FR-006, FR-007, FR-008, FR-009, FR-010, FR-014 - Rider workflow

**Steps**:
1. **Register Rider Account**
   ```bash
   POST /api/v1/auth/register
   {
     "username": "testrider",
     "email": "rider@test.com",
     "password": "password123", 
     "firstName": "Test",
     "lastName": "Rider",
     "roles": ["RIDER"]
   }
   ```
   Expected: 201 Created

2. **Login as Rider**
   ```bash
   POST /api/v1/auth/login
   {
     "username": "testrider",
     "password": "password123"
   }
   ```
   Expected: 200 OK with JWT token

3. **Discover Available Rallies**
   ```bash
   GET /api/v1/rallies?status=PUBLISHED
   Authorization: Bearer {token}
   ```
   Expected: 200 OK with array containing test rally

4. **Register for Rally**
   ```bash
   POST /api/v1/rallies/{rallyId}/register
   Authorization: Bearer {token}
   ```
   Expected: 201 Created with registration object including riderNumber

5. **Get Rally Bonus Points**
   ```bash
   GET /api/v1/rallies/{rallyId}/bonus-points
   Authorization: Bearer {token}
   ```
   Expected: 200 OK with array of 5 bonus points

6. **Get Rally Combinations**
   ```bash
   GET /api/v1/rallies/{rallyId}/combinations
   Authorization: Bearer {token}
   ```
   Expected: 200 OK with array containing test combination

7. **Create Optimized Route**
   ```bash
   POST /api/v1/registrations/{registrationId}/routes
   Authorization: Bearer {token}
   {
     "name": "My Rally Route",
     "plannedBonusPoints": ["{point1Id}", "{point2Id}", "{point3Id}"],
     "estimatedDuration": "PT6H30M",
     "estimatedDistance": 150.5
   }
   ```
   Expected: 201 Created with route object

8. **Export Route as GPX**
   ```bash
   GET /api/v1/routes/{routeId}/export?format=GPX
   Authorization: Bearer {token}
   ```
   Expected: 200 OK with GPX file content

**Success Criteria**: Rider successfully registered with unique number, route created and exportable

---

## Test Scenario 3: Mobile Navigation and Scoring

**Objective**: Validate FR-015, FR-016, FR-019, FR-020, FR-021, FR-022 - Navigation and scoring workflow

**Steps**:
1. **Mobile App Login** (Integration test simulation)
   - Login with rider credentials
   - Download rally data for offline use
   - Verify offline map tiles downloaded for rally area

2. **Start Navigation**
   - Load active route
   - Verify "expected time to finish" calculation displays
   - Verify "fastest time to finish" calculation displays
   - Check offline navigation works without network

3. **Visit Bonus Points**
   ```bash
   POST /api/v1/registrations/{registrationId}/scores
   Authorization: Bearer {token}
   {
     "bonusPointId": "{point1Id}",
     "claimedAt": "2025-06-01T10:30:00Z",
     "photoUrl": "https://example.com/photo1.jpg"
   }
   ```
   Repeat for each visited point
   Expected: 201 Created for each score

4. **Complete Rally** - Submit all bonus points in combination

**Success Criteria**: All bonus points successfully claimed, photos attached, ready for scoring

---

## Test Scenario 4: Scorer Evaluation and Results

**Objective**: Validate FR-020, FR-021, FR-022, FR-023, FR-024, FR-025 - Scoring workflow

**Steps**:
1. **Register Scorer Account**
   ```bash
   POST /api/v1/auth/register
   {
     "username": "testscorer",
     "email": "scorer@test.com",
     "password": "password123",
     "firstName": "Test", 
     "lastName": "Scorer",
     "roles": ["SCORER"]
   }
   ```
   Expected: 201 Created

2. **Login as Scorer**
   ```bash
   POST /api/v1/auth/login
   {
     "username": "testscorer", 
     "password": "password123"
   }
   ```
   Expected: 200 OK with JWT token

3. **Get Rally Scoring Data**
   ```bash
   GET /api/v1/rallies/{rallyId}/scoring
   Authorization: Bearer {token}
   ```
   Expected: 200 OK with array of submitted rider scores

4. **Find Rider by Number** (UI simulation)
   - Search for rider by number or name
   - Verify quick location of rider submissions

5. **Review and Approve Scores**
   ```bash
   PUT /api/v1/scores/{scoreId}
   Authorization: Bearer {token}
   {
     "isApproved": true,
     "scorerNotes": "Valid photo and location confirmed"
   }
   ```
   Expected: 200 OK with updated score

6. **Handle Disputed Score**
   ```bash
   PUT /api/v1/scores/{scoreId}
   Authorization: Bearer {token}
   {
     "isApproved": false,
     "scorerNotes": "Photo unclear, GPS coordinates inconsistent"
   }
   ```
   Expected: 200 OK with score marked not approved

7. **Generate Final Results**
   ```bash
   GET /api/v1/rallies/{rallyId}/results
   Authorization: Bearer {token}
   ```
   Expected: 200 OK with calculated leaderboard showing:
   - Individual bonus point totals
   - Combination completion bonuses
   - Final positions/rankings

**Success Criteria**: 
- Combination bonus awarded correctly (100 points for completing 3-point combo)
- Disputed score excluded from final tally
- Leaderboard shows accurate rankings

---

## Test Scenario 5: Offline Functionality Validation

**Objective**: Validate FR-004, FR-027, FR-028 - Local/server data synchronization

**Steps**:
1. **Create Rally with LOCAL Storage**
   - Rally Master creates rally with storageMode=LOCAL
   - Verify rally data stored locally only
   - Export rally data to JSON file

2. **Import Rally Data** (Rider perspective)
   - Import rally JSON file manually
   - Plan route using offline data
   - Export route for GPS device

3. **Offline Scoring Simulation**
   - Scorer works offline with local data
   - Multiple scorers score different riders simultaneously
   - Export scoring data to JSON files

4. **Data Merge and Synchronization**
   - Merge multiple scorer JSON files
   - Resolve any conflicts using timestamp strategy
   - Verify final results consistency

**Success Criteria**: 
- Local and server data modes work correctly
- Offline workflow produces identical results to online workflow
- Data synchronization handles conflicts appropriately

---

## Performance Validation

**Objective**: Validate performance constraints from technical context

**Tests**:
1. **Map Rendering Performance**
   - Load rally with 86 bonus points
   - Measure initial map render time
   - Target: <2 seconds

2. **Navigation Responsiveness** 
   - Simulate continuous GPS updates during navigation
   - Verify UI updates maintain 60fps
   - Test route recalculation performance

3. **API Response Times**
   - Load test rally endpoints with 100 concurrent requests
   - Target: <500ms average response time

4. **Offline Storage Constraints**
   - Download maps for 50-mile radius around rally
   - Verify cache size <100MB per region
   - Test cleanup of unused tiles

**Success Criteria**: All performance targets met under load testing

---

## Integration Test Execution Order

1. **Contract Tests First** - API endpoints return expected schemas
2. **Integration Tests** - Database operations with real PostgreSQL
3. **End-to-End Tests** - Full user scenarios above
4. **Performance Tests** - Load and stress testing
5. **Cross-Platform Tests** - iOS, Android, Desktop, Web consistency

**Exit Criteria**: All tests pass, performance targets met, ready for production deployment