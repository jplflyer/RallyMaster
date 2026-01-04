import org.gradle.api.artifacts.Configuration

plugins {
    java
    id("org.springframework.boot") version "3.5.8"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.freefair.lombok") version "9.1.0"
}

group = "org.showpage"
version = "1.0-SNAPSHOT"
description = "RallyServer"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(project(":RallyCommon"))
    implementation("org.springframework.boot:spring-boot-starter-web")
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
    runtimeOnly("org.postgresql:postgresql")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("net.datafaker:datafaker:2.5.2")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

// Separate unit tests (*Test.java) from integration tests (*IT.java)
// Unit tests run with: ./gradlew test
// Integration tests run with: ./gradlew integrationTest
// All tests run with: ./gradlew check

sourceSets {
    create("integrationTest") {
        java {
            compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
            runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
            srcDir("src/test/java")
        }
        resources {
            srcDir("src/test/resources")
        }
    }
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

val integrationTestRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
}

// Unit test task - runs *Test.java files
tasks.test {
    useJUnitPlatform()
    filter {
        includeTestsMatching("*Test")
        excludeTestsMatching("*IT")
    }
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Integration test task - runs *IT.java files
val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests (*IT.java)"
    group = "verification"

    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath

    useJUnitPlatform()
    filter {
        includeTestsMatching("*IT")
        excludeTestsMatching("*Test")
    }
    testLogging {
        events("passed", "skipped", "failed")
    }

    shouldRunAfter(tasks.test)
}

// Make 'check' run both unit and integration tests
tasks.check {
    dependsOn(integrationTest)
}
