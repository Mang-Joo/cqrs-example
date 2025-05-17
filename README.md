# Bank Account Management System (CQRS + Event Sourcing)

This project is an example for learning **CQRS** and **Event Sourcing** patterns.
It provides basic bank account management features such as account creation, deposit, withdrawal, and transfer.

## Key Features

-   **CQRS (Command Query Responsibility Segregation)** pattern applied
-   All state changes are stored as events based on **Event Sourcing**
-   In-memory DB (H2) and in-memory EventStore used
-   Built with Spring Boot 3.4.5 and Java 21

## Tech Stack

-   Java 21
-   Spring Boot 3.4.5
-   Spring Data JPA
-   H2 Database (in-memory)
-   Lombok
-   JUnit + AssertJ (test)

## Getting Started

1. Clone the project in a Java 21 environment
2. Build and run with Gradle
    ```bash
    ./gradlew bootRun
    ```
3. H2 Console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

## Main Folder Structure

-   `cqrs.bankaccount.command`
    Command services and DTOs for account creation, deposit, withdrawal, and transfer

-   `cqrs.bankaccount.query`
    Query services and DTOs for account information retrieval

-   `cqrs.bankaccount.model`
    Domain models and event definitions for accounts

-   `cqrs.common`
    Common interfaces and base classes such as Event, Aggregate

-   `cqrs.infrastructure.eventstore`
    Event store implementation (in-memory/JPA)

-   `src/test/java`
    Service unit test code

## Notes

-   This is an example project for learning RESTful API, CQRS, and Event Sourcing
