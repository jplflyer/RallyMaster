# RallyMaster Desktop Application

A cross-platform desktop application for motorcycle rally planning, ride planning, and scoring built with Kotlin and Compose Desktop.

## Features Implemented

### Core Infrastructure
- ✅ Kotlin + Compose Desktop application framework
- ✅ Multi-screen navigation (Splash → Login → Home)
- ✅ Application state management
- ✅ Logging with Logback

### Authentication & Preferences
- ✅ User login and registration
- ✅ Auto-login on startup using stored credentials
- ✅ Preferences management using `java.util.prefs.Preferences`
- ✅ Platform-specific secure credential storage:
  - macOS: Keychain via `security` command
  - Windows: Credential Manager via `cmdkey` command
- ✅ Configurable server URL (Settings screen)

### Server Communication
- ✅ REST client for RallyServer API
- ✅ JWT token-based authentication
- ✅ Token refresh capability
- ✅ Reuses DTOs from RallyCommon module

### UI Screens
- ✅ Splash screen with loading indicator (3 second minimum)
- ✅ Login/Register screen
- ✅ Home screen with navigation to main features
- ✅ Settings screen for server configuration
- ✅ Placeholder screens for future features

### Mapping & Routing (Planned)
- ✅ JXMapViewer dependency for mapping
- ✅ GraphHopper dependency for offline routing
- ✅ Basic routing service abstraction with Haversine distance calculation
- ⏳ Full GraphHopper integration (requires OSM map data)

## Project Structure

```
RallyDesktop/
├── src/main/kotlin/org/showpage/rallydesktop/
│   ├── Main.kt                           # Application entry point
│   ├── model/
│   │   └── AppState.kt                   # Application state management
│   ├── service/
│   │   ├── PreferencesService.kt         # Non-sensitive preferences
│   │   ├── CredentialService.kt          # Secure credential storage
│   │   ├── RallyServerClient.kt          # REST API client
│   │   └── RoutingService.kt             # Route calculation (placeholder)
│   └── ui/
│       ├── RallyMasterApp.kt             # Main app component
│       ├── SplashScreen.kt               # Startup splash screen
│       ├── LoginScreen.kt                # Login/registration
│       ├── HomeScreen.kt                 # Main navigation screen
│       ├── SettingsScreen.kt             # Settings configuration
│       └── PlaceholderScreen.kt          # Placeholder for future screens
└── src/main/resources/
    └── logback.xml                       # Logging configuration
```

## Building and Running

### Build the Application
```bash
./gradlew :RallyDesktop:build
```

### Run the Application
```bash
./gradlew :RallyDesktop:run
```

### Create Native Distributions
```bash
# macOS DMG
./gradlew :RallyDesktop:packageDmg

# Windows MSI
./gradlew :RallyDesktop:packageMsi

# Linux DEB
./gradlew :RallyDesktop:packageDeb
```

## Configuration

### Server URL
The default server URL is `http://localhost:8080` for development.

To change it:
1. Launch the application
2. Log in
3. Navigate to Settings
4. Update the Server URL
5. Click Save

The server URL is stored in platform-specific preferences:
- macOS: `~/Library/Preferences/com.apple.java.util.prefs.plist`
- Windows: Registry under `HKEY_CURRENT_USER\Software\JavaSoft\Prefs`

### Credentials Storage
Login credentials are stored securely:
- macOS: Keychain with service name "RallyMaster"
- Windows: Credential Manager with target "RallyMaster:{email}"

## Dependencies

- **Kotlin 1.9.22** - Programming language
- **Compose Desktop 1.5.12** - UI framework
- **Material 3** - UI components
- **JXMapViewer 2.8** - Mapping library
- **GraphHopper 8.0** - Routing engine (core only, requires map data for full functionality)
- **OkHttp 4.12.0** - HTTP client
- **Jackson 2.16.1** - JSON processing
- **JNA 5.14.0** - Native library access for credential managers
- **Logback 1.4.14** - Logging framework
- **Kotlin Coroutines 1.7.3** - Async operations

## Next Steps

### Immediate TODOs
1. **Add Splash Screen Image**: Replace the placeholder emoji with an actual motorcycle/map graphic
2. **Windows Password Retrieval**: Implement proper Windows Credential Manager password retrieval using JNA
3. **Rally Planning Screen**: Implement the full Rally Planning interface with map integration
4. **Ride Planning Screen**: Implement the Ride Planning interface
5. **Scoring Screen**: Implement the Scoring interface

### GraphHopper Integration
To enable full offline routing:
1. Download OSM map data files for your region (e.g., north-america-latest.osm.pbf from Geofabrik)
2. Configure GraphHopper to use the downloaded map data
3. Implement proper routing calculations in `RoutingService.kt`
4. Add map data management UI (download/update maps)

### Additional Features
- Import/export rally data as JSON
- Import bonus points from CSV/KMZ
- CSV export for combinations
- Route planning with drag-and-drop waypoints
- Time estimation with per-stop customization
- Bonus point combination highlighting
- Nearby bonus point suggestions

## Platform Support

- **macOS**: Fully supported (tested on macOS)
- **Windows**: Supported (credential retrieval needs enhancement)
- **Linux**: Basic support (no secure credential storage implemented)

## Troubleshooting

### Cannot connect to server
- Verify the server is running at the configured URL
- Check Settings → Server URL configuration
- Check firewall settings

### Auto-login fails
- Credentials may have been manually deleted from Keychain/Credential Manager
- Re-login to store new credentials

### Build fails
- Ensure Java 21 is installed and configured
- Run `./gradlew clean` and try again

## License

Part of the RallyMaster project.
