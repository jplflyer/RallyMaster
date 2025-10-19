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

# Coding Guidelines
- When adding new files to the project, perform `git add`.

## Java
- When coding new objects, include @Builder when reasonable.
- When possible, code should prefer use of Builders when creating new objects.
- Lombok is configured to chain setters, so you do foo.setA("A").setB("B").
- When method signatures are long, split them to be readable per the example further below.

## Integration Tests
- When writing integration tests, we use the @TestMethodOrder(MethodOrderer.OrderAnnotation.class) on the
class and @Order on the individual tests.
  - @Order should be in file order, so test 10 is always above test 20.
  - Leave gaps so new tests do not require reordering the entire file. I start at 10 and increment by 10.
  - Related tests should appear together
  - When there are multiple groups of related tests, I start the next on by jumping to the next multiple of 100.
  - For instance, 10, 20, 30, 100, 110, 200, 300, 310

### Long Method signature
When a method signature will be more than about 120 characters, then please split it into multiple lines. This
can take one of two forms:

You can use this one when it's only too long because of the throws clauses. Please note the location of the
open brace.

```
public UiSomeReturnType aNotBadMethod(Member currentMember, List<Foo> aListOfThings)
    throws NotFoundException, ValidationException
{
    ...
}
```

For readability when the argument list is just really wide, more than 3 items long, or when some of them
have complicated datatypes, format it like this. Please note the location of the open brace.
```
public UiSomeReturnType aComplicatedMethod(
    Member currentMember,
    List<Foo> aListOfThings,
    Float someValue,
    String someOtherValue)
    throws NotFoundException, ValidationException
{
    ...
}
```

If there's a lengthy argument list but no throws clause, I still prefer this. Use this form if there are more
than three arguments, if their datatypes are complicated and new lines will make it more readable, or if it's
just going to be over 120 characters. Note the location and indentation of the closing parenthesis and opening
brace. I find this more readable than other placements, as it does a better job separating the signature from
the first line of code.
```
public UiSomeReturnType aComplicatedMethod(
    Member currentMember,
    List<Foo> aListOfThings,
    Float someValue,
    String someOtherValue
) {
    ...
}
```
