# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RallyMaster is a multi-module Gradle project consisting of:
- **RallyServer**: Spring Boot REST API server using Spring Web, Spring Data JPA, PostgreSQL, and Spring Security
- **RallyCommon**: Shared Java library for common functionality

## Common Commands

### Build and Run
- `./gradlew build` - Build all modules
- `./gradlew bootRun` - Run the Spring Boot server (RallyServer)
- `./gradlew clean` - Clean all build artifacts
- `./gradlew bootJar` - Create executable JAR

### Testing
- `./gradlew test` - Run all tests across modules
- `./gradlew check` - Run all verification tasks including tests

### Development
- `./gradlew tasks` - List all available tasks
- `./gradlew bootTestRun` - Run server with test runtime classpath

## Architecture

### Module Structure
- Root project configures shared dependencies (JUnit 5) and Java 21 toolchain
- RallyServer depends on RallyCommon via `implementation(project(":RallyCommon"))`
- Spring Boot plugins applied only to RallyServer module

### Database Configuration
- PostgreSQL database: `jdbc:postgresql://localhost:5432/rallymaster`
- Default credentials: username `rallymaster`, password `rallyhq`
- Hibernate DDL mode: `validate` (expects existing schema)
- JPA open-in-view disabled for performance
- Flyway migrations enabled with baseline-on-migrate
- Migration scripts location: `RallyServer/src/main/resources/db/migration/`

### Authentication System
- Uses Spring Security with Basic authentication
- JWT tokens for authentication (short-lived)
- Refresh tokens (long-lived, use-once)
- Member table stores user credentials
- Requires implementation of `/login` and `/token` endpoints in LoginController

### Technology Stack
- Java 21 (configured via toolchain)
- Spring Boot 3.5.6
- Spring Security
- Spring Data JPA
- PostgreSQL driver
- Lombok for boilerplate reduction
- JUnit 5 for testing

## Package Structure
- Server main class: `org.showpage.rallyserver.ServerApplication`
- Group ID: `org.showpage` for both modules