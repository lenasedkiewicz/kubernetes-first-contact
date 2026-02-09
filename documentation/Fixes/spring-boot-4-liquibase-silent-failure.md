# Fix: Liquibase Silent Failure in Spring Boot 4

## Error

```
org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "TASKS" not found (this database is empty)
```

The application starts successfully, Hibernate initializes, but all queries fail because the database tables were never created. No Liquibase output appears in the startup logs — Liquibase is silently skipped.

## Root Cause

Spring Boot 4 modularized its auto-configuration. The raw `liquibase-core` dependency no longer triggers Liquibase auto-configuration automatically. In Spring Boot 3.x, adding `liquibase-core` was sufficient — Spring Boot's `LiquibaseAutoConfiguration` (in `org.springframework.boot.autoconfigure.liquibase`) would detect it and run migrations.

In Spring Boot 4.x:
- The auto-configuration class moved to `org.springframework.boot.liquibase.autoconfigure`
- Raw library dependencies like `liquibase-core` no longer get auto-configured
- You must use the new **starter** dependency instead

This is especially dangerous because:
- There are no errors or warnings in the logs
- The application starts normally
- Liquibase is simply never invoked
- The only symptom is "table not found" errors at runtime

## Solution

In `pom.xml`, replace:

```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

with:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-liquibase</artifactId>
</dependency>
```

The `spring.liquibase.*` application properties remain the same — no changes needed in `application.properties`.

## How to Diagnose

1. Check startup logs for any mention of "liquibase" — if there is none, Liquibase auto-configuration is not active
2. The H2 database file will exist but contain no tables (the database is created by the JDBC connection, but Liquibase never populates it)
3. The `DATABASECHANGELOG` and `DATABASECHANGELOGLOCK` tables (created by Liquibase) will be absent

## Affected Services

- `tasks-service` — uses Liquibase with H2 for schema management

## Key Takeaway

When upgrading to Spring Boot 4, replace all raw library dependencies with their corresponding starters. This applies to Liquibase, Flyway, and other libraries that relied on Spring Boot auto-configuration. If auto-configuration silently stops working after an upgrade, check if the dependency needs to be switched to a starter variant.

## References

- [Spring Boot 4 Modularization: Fix Auto-Configuration Issues](https://www.danvega.dev/blog/2025/12/12/spring-boot-4-modularization)
- [LiquibaseAutoConfiguration (Spring Boot 4.0 API)](https://docs.spring.io/spring-boot/4.0-SNAPSHOT/api/java/org/springframework/boot/liquibase/autoconfigure/LiquibaseAutoConfiguration.html)
