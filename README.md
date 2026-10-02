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

## Coding Conventions

Follow the existing package layout under `com.swp391.g1`:

```text
controller/          HTTP Servlet controllers
service/             Service interfaces
service/impl/        Service implementations
dao/                 DAO interfaces and shared DAO contracts
dao/impl/            DAO implementations
model/               Entity/domain objects, generally one per database table
dto/request/         Data received from a client or form
dto/response/        Data prepared for a view or client
```

### DAO

* Define a DAO interface in `com.swp391.g1.dao` for each entity/repository, for example `IStudentDAO`.
* Put its JDBC implementation in `com.swp391.g1.dao.impl`, named `<Entity>DAOImpl` (for example, `StudentDAOImpl`), and have it implement the interface.
* Declare persistence operation signatures in the interface; keep SQL and JDBC code in the implementation. Reuse `IGenericDAO<T, K>` for common CRUD operations where appropriate.
* DAO methods are responsible for database reads/writes only. They must not contain business rules or Servlet/request handling.
* Use `PreparedStatement` for values in SQL, close JDBC resources with try-with-resources, and report database failures through the project's data-access exception pattern.

### Service

* Define a service interface in `com.swp391.g1.service`, named `I<Entity>Service` (for example, `IStudentService`).
* Put the implementation in `com.swp391.g1.service.impl`, named `<Entity>ServiceImpl` (for example, `StudentServiceImpl`), and have it implement the interface.
* Keep use-case method declarations in the interface. Implementations coordinate DAO calls and own business rules, validation that depends on business rules, and transaction-level workflows.
* Expose methods in terms of application use cases rather than SQL operations. Keep persistence details out of service signatures.
* Keep services independent of Servlet API types; pass validated values or DTOs as appropriate.

### Controllers and Servlets

* Every HTTP controller must be a Jakarta `HttpServlet` and its class name must end in `Servlet` (for example, `StudentServlet`), including controllers currently named `*Controller`.
* Keep Servlets thin: map the URL and HTTP method, read and validate request-format data, call a service, then forward/redirect or write the response.
* Do not put SQL or business workflows in a Servlet. Store JSP files under `src/main/webapp/WEB-INF/views/` so they are rendered through a controller.
* Use the existing authentication/filter mechanisms for access control rather than duplicating authorization checks across handlers.

### Models and DTOs

* Keep a model/entity focused on one domain entity or database table; do not use it as a container for unrelated joined data.
* Create a DTO when a view or use case needs data combined from multiple tables, or when request/response data should differ from the entity shape. Put request DTOs in `dto/request/` and response/read DTOs in `dto/response/`.
* Give DTOs names that describe their purpose, such as `StudentProfileResponseDTO`; do not expose database entities directly when that would couple the client or view to persistence details.
* Do not create DTOs or business classes without a concrete use case.

### General

* Use PascalCase for class/interface names and camelCase for fields and methods. Use descriptive names and keep one public top-level type per file.
* Keep layers moving in one direction: Servlet → Service interface → DAO interface → database. Implementations may depend on lower-layer interfaces; DAOs must not call services or controllers.
* Use constants or enums for shared fixed values instead of repeating string literals. Avoid magic numbers and duplicate validation/business rules.
* Handle errors consistently: do not silently swallow exceptions, and do not expose SQL details or stack traces to end users.
* Add or update tests when adding business rules or changing behavior; keep changes scoped to the feature.
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