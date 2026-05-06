# Employee Management System (Learning Project)

This is a Spring Boot learning project for an employee management system. It contains example controllers, services, and JPA entities to demonstrate common patterns used in web applications.

## Quick facts
- Java: 21
- Spring Boot: 4.0.5 (parent in `pom.xml`)
- Packaging: Maven
- Main package: `com.example.employee_management_system`

## Key technologies and libraries used
- Spring Boot (web, security, data-jpa)
- Spring MVC (`spring-boot-starter-webmvc`)
- Spring Security (`spring-boot-starter-security`)
- Spring Data JPA (`spring-boot-starter-data-jpa`)
- Spring Cache (`spring-boot-starter-cache`) + Caffeine
- OpenAPI UI: `springdoc-openapi-starter-webmvc-ui` (for API docs)
- JWT: `jjwt`
- JAXB (xml binding), MapStruct, Lombok (compile-time), DevTools, PostgreSQL driver
- Validation: `spring-boot-starter-validation`

Dependencies are declared in `pom.xml` (see the `dependencies` section for full details).

## Run locally
From the project root, using PowerShell:

```powershell
mvn clean package
mvn spring-boot:run
```

Or run the `EmployeeManagementSystemApplication` class from your IDE.

## Run tests
```powershell
mvn test
```

---
Generated on: May 6, 2026

