# SWP391-G1

Common technical skeleton for the SWP391-G1 team project.

## Technology Stack

| Concern     | Technology                           |
| ----------- | ------------------------------------ |
| Language    | Java 21                              |
| Framework   | Spring Boot 3.5.16                   |
| Web         | Jakarta Servlet                      |
| View        | JSP / JSTL                           |
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
Servlet
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
SQL Server
```

View rendering: Servlet → JSP / JSTL → HTML.

* Servlets are the Controller layer (registered with `@WebServlet`).
* JSP is the View layer (rendered by the embedded Tomcat Jasper engine).
* Services contain the business logic.
* Repositories own the database access.
* Spring MVC `@Controller` is not used for application controllers.
* Thymeleaf is not used.

JSP views live under `src/main/webapp/WEB-INF/views/`.

## Team Conventions

* Controllers (Servlets) remain thin.
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

Open http://localhost:8080/home
