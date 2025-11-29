# Rally Planning Implementation Plan

## Overview
The Rally Planning screen is a comprehensive workspace for rally organizers to manage all aspects of a rally after initial creation. This document outlines the phased implementation approach.

## Phase 1: Basic Rally Creation ✅ COMPLETED
**Status**: Completed and tested

Created a basic rally creation form that captures:
- Required fields: name, description, city, state
- Optional fields: start/end dates, coordinates, country code
- Visibility settings: isPublic, pointsPublic, ridersPublic, organizersPublic
- Full validation and error handling
- Navigation integration with HomeScreen

**Files Created/Modified**:
- `RallyDesktop/src/main/kotlin/org/showpage/rallydesktop/ui/RallyFormScreen.kt` (new)
- `RallyDesktop/src/main/kotlin/org/showpage/rallydesktop/service/RallyServerClient.kt` (added createRally)
- `RallyDesktop/src/main/kotlin/org/showpage/rallydesktop/model/AppState.kt` (added RALLY_FORM screen)
- `RallyDesktop/src/main/kotlin/org/showpage/rallydesktop/ui/RallyMasterApp.kt` (wired navigation)

## Phase 2: Rally Planning Screen Structure ✅ COMPLETED
**Status**: Completed and tested

Create the main Rally Planning workspace with a 4-panel layout:

### Panel Layout
```
+------------------+------------------+
|                  |                  |
|  Rally Info      |  Bonus Points    |
|  (read-only      |  (CRUD list)     |
|   summary)       |                  |
|                  |                  |
+------------------+------------------+
|                  |                  |
|  Combinations    |  Map             |
|  (CRUD list)     |  (JXMapViewer)   |
|                  |                  |
+------------------+------------------+
```

### Implementation Details
- **Top-left panel**: Rally basic info display (read-only summary)
  - Name, dates, location
  - Edit button to return to RallyFormScreen
  - Visibility settings display

- **Top-right panel**: Bonus Points list
  - Scrollable list/table of bonus points
  - Add/Edit/Delete buttons (placeholder)
  - Shows: code, name, value, category

- **Bottom-left panel**: Combinations list
  - Scrollable list/table of combinations
  - Add/Edit/Delete buttons (placeholder)
  - Shows: name, value, constituent points

- **Bottom-right panel**: Map placeholder
  - Empty panel with "Map (Coming Soon)" text
  - Will use JXMapViewer in Phase 5

### Navigation
- Click on rally in HomeScreen → navigate to Rally Planning
- Need to pass rallyId to Rally Planning screen
- After creating rally, navigate to Rally Planning (not HOME)

### Files Created/Modified
- `RallyDesktop/src/main/kotlin/org/showpage/rallydesktop/ui/RallyPlanningScreen.kt` (new - 4-panel layout)
- `RallyDesktop/src/main/kotlin/org/showpage/rallydesktop/model/AppState.kt` (added currentRallyId, navigateToRallyPlanning, navigateToRallyForm)
- `RallyDesktop/src/main/kotlin/org/showpage/rallydesktop/service/RallyServerClient.kt` (added getRally method)
- `RallyDesktop/src/main/kotlin/org/showpage/rallydesktop/ui/HomeScreen.kt` (wire rally click to navigation)
- `RallyDesktop/src/main/kotlin/org/showpage/rallydesktop/ui/RallyMasterApp.kt` (wired Rally Planning screen)
- `RallyCommon/src/main/java/org/showpage/rallyserver/ui/UiRally.java` (added latitude, longitude, isPublic fields)
- `RallyServer/src/main/java/org/showpage/rallyserver/service/DtoMapper.java` (map new rally fields)

## Phase 3: Bonus Points CRUD ✅ COMPLETED
**Status**: Completed and tested

Implement full CRUD operations for bonus points.

### API Integration
- Client methods for:
  - `searchBonusPoints(rallyId)` → List<UiBonusPoint>
  - `createBonusPoint(rallyId, request)` → UiBonusPoint
  - `updateBonusPoint(rallyId, pointId, request)` → UiBonusPoint
  - `deleteBonusPoint(rallyId, pointId)` → Result<Unit>

### UI Components
- **BonusPointDialog**: Modal form for add/edit
  - Fields: code, name, value, category, latitude, longitude
  - Validation for required fields
  - Save/Cancel buttons

- **BonusPointsList**: Table/list with columns
  - Code, Name, Value, Category
  - Click row to edit
  - Delete button with confirmation dialog
  - Add button to open dialog

### Features
- Load points when rally planning screen opens
- Real-time list updates after add/edit/delete
- Error handling and user feedback
- Sort by code or name

### Files Created/Modified
- `RallyDesktop/src/main/kotlin/org/showpage/rallydesktop/ui/BonusPointComponents.kt` (new - dialog and list)
- `RallyDesktop/src/main/kotlin/org/showpage/rallydesktop/service/RallyServerClient.kt` (added bonus point CRUD methods)
- `RallyDesktop/src/main/kotlin/org/showpage/rallydesktop/ui/RallyPlanningScreen.kt` (wired BonusPointsList)

### Data Model
Refer to existing `UiBonusPoint` in RallyCommon:
- id, rallyId, code, name, value
- category, latitude, longitude
- flavor text, answer, aux data

## Phase 4: Combinations CRUD
**Status**: Not started

Implement full CRUD operations for combinations.

### API Integration
- Client methods for:
  - `searchCombinations(rallyId)` → List<UiCombination>
  - `createCombination(rallyId, request)` → UiCombination
  - `updateCombination(rallyId, comboId, request)` → UiCombination
  - `deleteCombination(rallyId, comboId)` → Result<Unit>

### UI Components
- **CombinationDialog**: Modal form for add/edit
  - Name field
  - Value field
  - Multi-select for constituent bonus points
  - Description/rules field
  - Save/Cancel buttons

- **CombinationsList**: Table/list with columns
  - Name, Value, Points Count
  - Click row to edit
  - Delete button with confirmation
  - Add button to open dialog

### Features
- Display constituent point codes in list
- Can only select existing bonus points
- Validation: must have at least 2 points
- Real-time updates

### Data Model
Refer to existing `UiCombination` in RallyCommon:
- id, rallyId, name, value
- bonusPoints (list of UiBonusPoint)

## Phase 5: Map Integration
**Status**: Not started

Add interactive map to bottom-right panel using JXMapViewer.

### Dependencies
Add to `RallyDesktop/build.gradle.kts`:
```kotlin
implementation("org.jxmapviewer:jxmapviewer2:2.6")
```

### Features
- Display OpenStreetMap tiles
- Plot rally location (if coordinates provided)
- Plot all bonus points with coordinates
- Click marker to see point details
- Zoom controls
- Pan with mouse drag

### Implementation
- Use Compose `AndroidView` equivalent (or Swing interop)
- Create `MapPanel` composable
- Load bonus points and filter those with valid coordinates
- Custom markers for rally vs bonus points
- Tooltip on hover showing code/name

### Nice-to-Have
- Color-code markers by category
- Show combinations as connected lines
- Filter points by category

## Phase 6: Import/Export
**Status**: Not started

Add import/export functionality for bonus points and combinations.

### Export Formats
- **CSV**: Bonus points as spreadsheet
  - Headers: code, name, value, category, lat, lng
  - One row per point

- **JSON**: Full rally data export
  - Rally info, all points, all combinations
  - For backup/sharing

- **KMZ**: Google Earth format
  - Rally and bonus point locations
  - Point details in description

### Import Formats
- **CSV**: Bulk import bonus points
  - Validate columns and data types
  - Show preview before import
  - Error reporting for invalid rows

- **JSON**: Import from exported data
  - Validate structure
  - Option to merge or replace

### UI
- Add Import/Export buttons to Rally Planning toolbar
- File chooser dialog
- Progress indicator for large imports
- Success/error notifications

## Technical Notes

### State Management
- Rally Planning screen maintains:
  - Current rally data
  - List of bonus points
  - List of combinations
  - Loading states for each panel
  - Error states

### Data Flow
1. User selects rally from HomeScreen
2. Navigate to Rally Planning with rallyId
3. Load rally details, bonus points, and combinations in parallel
4. User performs CRUD operations
5. Re-fetch affected data after mutations
6. Map updates automatically when points change

### Error Handling
- Network errors: Show retry button
- Validation errors: Inline error messages
- Delete confirmations: Modal dialog
- Optimistic updates where appropriate

### Performance Considerations
- Lazy load map tiles
- Cache rally data while on planning screen
- Debounce search/filter operations
- Paginate bonus points if > 500 points

## Future Enhancements (Post-MVP)
- Undo/redo for operations
- Bulk edit bonus points
- Duplicate rally/points
- Point templates/presets
- Collaboration (multiple organizers editing simultaneously)
- Real-time validation of combination logic
- Generate QR codes for bonus points
- Preview rally as rider would see it

## References
- Desktop.md: Lines 176-237 (Rally Planning requirements)
- CreateRally.md: Rally creation flow and field definitions
- Existing API endpoints in RallyController.java
- DTO definitions in RallyCommon/ui/
