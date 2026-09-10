# SWP391-G1

Common technical skeleton for the SWP391-G1 team project.

The business requirements are still being finalized, so the repository currently focuses on the shared technical foundation and basic authentication.

## Technology Stack

| Concern     | Technology                                      |
| ----------- | ------------------------------------------------|
| Language    | Java 21                                         |
| Framework   | Spring Boot 3.5.16                              |
| Web         | Spring MVC                                      |
| View        | Thymeleaf                                       |
| Security    | Spring Security 6.5.11 (managed by Spring Boot) |
| Persistence | Spring Data JPA / Hibernate                     |
| Database    | Microsoft SQL Server                            |
| JDBC        | Microsoft JDBC Driver (`mssql-jdbc`)            |
| Build       | Maven                                           |
| Testing     | JUnit 5 / Mockito                               |

## Current Foundation (v1.0)

* Session-based authentication
* Form login at /login
* BCrypt password encoding
* CSRF protection
* Protected /home page
* Logout via POST /logout
* Basic 404 and 500 error pages
* Authentication and error-handling tests

## Temporary Development Account

The current authentication uses one temporary in-memory account configured through app.auth.dev-account.*.

Default development account:

Username: admin
Password: admin123

This account is for development only and will be replaced when the final business account model is available.

Do not use real credentials in the repository.

## Current Scope Limitations

Business features will be added after the project requirements and database model are finalized.

* Business user/account persistence
* Roles and authorization
* Registration
* Password reset
* Profile management
* Business modules and workflows

## Architecture

Business request flow

```text
Browser
    ↓
Controller
    ↓
Service
    ↓
Repository
    ↓
Spring Data JPA
    ↓
Hibernate
    ↓
JDBC
    ↓
Microsoft SQL Server
```

Authentication flow

```text
Browser
    ↓
Spring Security
    ↓
Controller
    ↓
Thymeleaf
```

## Database Convention

`src/main/resources/application.properties` keeps

```properties
spring.jpa.hibernate.ddl-auto=none
```

Hibernate must not create, update, or drop the database schema. Database schema changes are managed separately by the team.

## Team Conventions

* Keep controllers thin.
* Put business logic in services.
* Put database access in repositories.
* Prefer constructor injection.
* Use DTOs only when they provide a meaningful boundary.
* Do not create business classes before requirements exist.
* Keep `spring.jpa.hibernate.ddl-auto=none`.
* Do not add dependencies or technologies without a project requirement and team decision.

## Run

```bash
mvn clean test
```

```bash
mvn spring-boot:run
```
