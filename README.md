# SWP391-G1

Common technical skeleton for the SWP391-G1 team project.

## Technology Stack

| Concern     | Technology                           |
| ----------- |--------------------------------------|
| Language    | Java 21                              |
| Framework   | Jakarta EE Web API                   |
| Web         | Jakarta Servlet                      |
| View        | JSP / JSTL                           |
| Persistence | JDBC                                 |
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
DAO
    ↓
JDBC
    ↓
SQL Server
```

View rendering: Servlet → JSP / JSTL → HTML.

* Servlets are the Controller layer (registered with `@WebServlet`).
* JSP is the View layer and is rendered by the JSP engine provided by Tomcat..
* Services contain the business logic.
* DAO classes handle database access.
* Thymeleaf is not used.

JSP views live under `src/main/webapp/WEB-INF/views/`.

## Team Conventions

* Controllers (Servlets) remain thin.
* Business logic belongs in services.
* Database access belongs in DAO classes.
* Use DTOs only when they provide a meaningful boundary.
* Do not create business classes before requirements exist.
* Do not add dependencies or technologies without a project requirement and team decision.

## Run

```bash
mvn clean test
Run the web application using Apache Tomcat 10.1.
```

```bash
Tomcat
```

Open http://localhost:8080/home
