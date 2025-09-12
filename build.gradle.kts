plugins {
    kotlin("jvm") version "1.9.20" apply false
    kotlin("multiplatform") version "1.9.20" apply false
    kotlin("plugin.spring") version "1.9.20" apply false
    kotlin("plugin.jpa") version "1.9.20" apply false
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    id("org.jetbrains.compose") version "1.5.11" apply false
    id("com.diffplug.spotless") version "6.23.3" apply false
}

allprojects {
    group = "com.rallymaster"
    version = "1.0.0"
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            ktlint("0.50.0")
            trimTrailingWhitespace()
            endWithNewline()
        }
        
        java {
            target("**/*.java")
            googleJavaFormat("1.17.0")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}