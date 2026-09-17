package com.swp391.g1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * Entry point of the SWP391-G1 application.
 *
 * Spring Boot stays the application foundation. {@code @WebServlet} components
 * are scanned and registered with the embedded Tomcat container: Jakarta
 * Servlets act as the Controller layer and JSP as the View layer.
 */
@ServletComponentScan
@SpringBootApplication
public class ProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectApplication.class, args);
    }
}