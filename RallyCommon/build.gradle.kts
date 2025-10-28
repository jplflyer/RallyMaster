plugins {
    id("java-library")
    id("io.freefair.lombok") version "8.10"
}

group = "org.showpage"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.6"))

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names")

    // Helpful modules (optional but common)
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")       // java.time
    api("com.fasterxml.jackson.module:jackson-module-parameter-names")  // ctor binding

    // Lombok for boilerplate reduction
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // SLF4J for logging
    implementation("org.slf4j:slf4j-api")

    // Swagger/OpenAPI annotations
    implementation("io.swagger.core.v3:swagger-annotations:2.2.20")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
