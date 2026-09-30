# SWP391-G1

Common technical skeleton for the SWP391-G1 team project.

## Technology Stack

| Concern            | Technology                                |
| ------------------ | ----------------------------------------- |
| Language           | Java 21                                   |
| Web                | Jakarta Servlet 6.0                       |
| View               | JSP / JSTL 3.0                            |
| Architecture       | Layered / Classic MVC (Servlet + JSP)     |
| Database           | Microsoft SQL Server / Azure SQL          |
| Database Access    | JDBC                                      |
| JDBC Driver        | Microsoft JDBC Driver (`mssql-jdbc`)      |
| Build              | Maven                                     |
| Packaging          | WAR                                       |
| Server / Container | Apache Tomcat 10.1                        |
| Testing            | Not configured yet                        |

## Architecture

The project uses a layered MVC architecture:

```text
Browser
    ↓
Servlet / Controller
    ↓
Service
    ↓
DAO
    ↓
JDBC
    ↓
SQL Server
```

### Current implemented flow (`HomeServlet`)

```text
GET /home
    ↓
HomeServlet  (@WebServlet("/home"), com.swp391.g1.controller)
    ↓
RequestDispatcher.forward(...)
    ↓
/WEB-INF/views/home/home.jsp
```

View rendering: Servlet → JSP → HTML.

* Servlets handle HTTP requests and act as Controllers.
* JSP handles the View layer.
* Business logic belongs in Services.
* Database access belongs in DAOs.
* JDBC is used for database connectivity.
* JSP views are stored under src/main/webapp/WEB-INF/views/.

## Database Access

```text
SQL Server / Azure SQL
    ↓
Microsoft JDBC Driver (mssql-jdbc)
    ↓
JDBC DriverManager
    ↓
DBContext
```

`DBContext` loads the connection settings from `src/main/resources/ConnectDB.properties` and opens connections with `java.sql.DriverManager`. Database credentials are developer-local and must never be committed to the repository or documented in the README.

## Team Conventions

* Controllers (Servlets) remain thin.
* Business logic belongs in services.
* Database access belongs in DAO classes.
* Use DTOs only when they provide a meaningful boundary.
* Do not create business classes before requirements exist.
* Do not add dependencies or technologies without a project requirement and team decision.

## Run

The project is a standard Maven WAR application that runs on an external Apache Tomcat 10.1 server.

### Build

mvn clean package

The generated WAR file is:

target/swp391-g1.war

### Deploy

Deploy the generated WAR file to your local Apache Tomcat 10.1 instance and start Tomcat.

The repository does not define a project-wide server port or context path; these depend on your local Tomcat configuration.

Default Tomcat port: 8080
Default WAR context path: /swp391-g1

The application URL is typically:

http://localhost:8080/swp391-g1/home