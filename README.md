# Invoicing Manager

A Spring Boot web application for managing invoices and customers, with user authentication and per-user data scoping.

## Tech Stack

- **Java 17**
- **Spring Boot 3.4** (Web, Security, Data JPA, Thymeleaf, Validation)
- **MySQL 8+**
- **Lombok**
- **Maven**

## Prerequisites

- JDK 17+
- Maven 3.8+
- MySQL 8+ running locally on port `3306`


## Build

```bash
mvn clean package -DskipTests
```

The JAR will be produced at `target/invoicing-manager-0.0.1-SNAPSHOT.jar`.

## Run

**Option 1 — Maven wrapper (development)**

```bash
mvn spring-boot:run
```

**Option 2 — JAR**

```bash
java -jar target/invoicing-manager-0.0.1-SNAPSHOT.jar
```




## Running Tests

```bash
mvn test
```
