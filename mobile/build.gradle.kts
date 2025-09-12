plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.android.application") version "8.1.4" apply false
}

kotlin {
    jvm("desktop") {
        jvmToolchain(21)
        withJava()
    }
    
    // Note: iOS and Android targets will be configured when ready to implement those platforms
    // This initial setup focuses on desktop and common code
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                
                // Networking
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
                
                // JSON Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                
                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                
                // UUID
                implementation("com.benasher44:uuid:0.8.2")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
        
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("io.ktor:ktor-client-okhttp:2.3.7")
                
                // Desktop-specific dependencies
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
            }
        }
        
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
        
        // Placeholder for future Android/iOS sourcesets
        // Will be uncommented when implementing those platforms:
        /*
        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-android:2.3.7")
                implementation("androidx.activity:activity-compose:1.8.2")
            }
        }
        
        val iosMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.7")
            }
        }
        */
    }
}

compose.desktop {
    application {
        mainClass = "com.rallymaster.MainKt"
        
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "Rally Master"
            packageVersion = "1.0.0"
            description = "Motorcycle Rally Management System"
            copyright = "© 2025 Rally Master. All rights reserved."
            vendor = "Rally Master Development Team"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}