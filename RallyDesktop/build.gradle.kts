import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.compose") version "1.5.12"
}

group = "org.showpage"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // RallyCommon for shared DTOs
    implementation(project(":RallyCommon"))

    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // JXMapViewer for mapping
    implementation("org.jxmapviewer:jxmapviewer2:2.8")

    // GraphHopper for routing (core library for now, will add OSM reader when needed)
    // Note: Full routing implementation will require map data files
    implementation("com.graphhopper:graphhopper-core:8.0")

    // HTTP Client for server communication
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON processing
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")

    // JNA for native credential manager access
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("org.slf4j:slf4j-api:2.0.9")

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

compose.desktop {
    application {
        mainClass = "org.showpage.rallydesktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "RallyMaster"
            packageVersion = "1.0.0"
            description = "RallyMaster Desktop Application"
            vendor = "ShowPage"

            macOS {
                bundleID = "org.showpage.rallymaster"
                iconFile.set(project.file("src/main/resources/icon.icns"))
            }

            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
                menuGroup = "RallyMaster"
                upgradeUuid = "B5A7C8E4-2D3F-4E9A-8F6B-1C4D5E6F7A8B"
            }

            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
