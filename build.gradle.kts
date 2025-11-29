plugins {
    // Keep Spring plugins here for central versioning; applied only in :server
    id("org.springframework.boot") version "3.5.8" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false

    // Kotlin can be added later like this (kept false at root):
    // kotlin("jvm") version "2.0.20" apply false
}

allprojects {
    group = "com.example"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

tasks.register("printVersion") {
    doLast { println(project.version.toString()) }
}

subprojects {
    plugins.apply("java")

    // Configure Java toolchain
    extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
        toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // Use string-based dependency adds to avoid the accessor timing issue
    dependencies {
        add("testImplementation", platform("org.junit:junit-bom:5.11.0"))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
    }
}


