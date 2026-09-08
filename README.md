# SWP391-G1

Common technical skeleton for the SWP391-G1 team project.

The business domain and requirements are **not finalized yet**, so this repository
contains **no business functionality**: no business entities, controllers, services,
repositories, DTOs, validation, authentication, or authorization. It provides only the
shared technical foundation the team will build on.

## Technology Stack

| Concern     | Technology                           |
| ----------- | ------------------------------------ |
| Language    | Java 21                              |
| Framework   | Spring Boot 3.5.16                   |
| Web         | Spring MVC                           |
| View        | Thymeleaf                            |
| Persistence | Spring Data JPA / Hibernate          |
| Database    | Microsoft SQL Server                 |
| JDBC        | Microsoft JDBC Driver (`mssql-jdbc`) |
| Build       | Maven                                |
| Testing     | JUnit 5 / Mockito                    |

## Architecture

Request flow:

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

Thymeleaf is used for server-side rendering: Controller → Model → Thymeleaf → HTML.

## Team Conventions

* Controllers remain thin.
* Business logic belongs in services.
* Database access belongs in repositories.
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
