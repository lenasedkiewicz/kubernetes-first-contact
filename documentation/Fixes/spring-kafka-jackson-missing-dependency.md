# Fix: Spring Kafka Jackson Missing Dependency

## Error

```
[ERROR] /root/.m2/repository/org/springframework/kafka/spring-kafka/4.0.1/spring-kafka-4.0.1.jar
(/org/springframework/kafka/support/serializer/JsonDeserializer.class):
error: Cannot attach type annotations @org.jspecify.annotations.Nullable to
org.springframework.kafka.support.serializer.JsonDeserializer.<init>:
  class file for com.fasterxml.jackson.core.type.TypeReference not found

[ERROR] cannot access com.fasterxml.jackson.databind.JavaType
  class file for com.fasterxml.jackson.databind.JavaType not found
```

Build fails when compiling a Spring Boot application that uses `spring-kafka` with `JsonSerializer` or `JsonDeserializer`.

## Root Cause

The `spring-kafka` library uses Jackson for JSON serialization/deserialization of Kafka messages, but **does not declare Jackson as a transitive dependency**.

When using:
- `JsonSerializer<T>` for Kafka producers
- `JsonDeserializer<T>` for Kafka consumers

The code requires Jackson classes (`ObjectMapper`, `JavaType`, `TypeReference`) at compile time, but these are not automatically included.

**Common scenario:** This often occurs when:
1. Using `spring-boot-starter` without `spring-boot-starter-web` (web starter includes Jackson)
2. Or when the dependency tree doesn't pull in Jackson for another reason

## Solution

Add explicit Jackson dependency to `pom.xml`:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

This single dependency pulls in:
- `jackson-databind` - core data binding
- `jackson-core` - low-level JSON processing (transitive)
- `jackson-annotations` - annotations (transitive)

### Full Example for Kafka Producer/Consumer

**Producer service (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

**Consumer service (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

## Alternative

If you already have `spring-boot-starter-web` in your dependencies, Jackson should be included automatically. Check if it's missing or excluded somewhere in your dependency tree:

```bash
mvn dependency:tree | grep jackson
```

## Key Takeaway

When using `spring-kafka` with JSON serialization (`JsonSerializer`/`JsonDeserializer`), always ensure Jackson is available in your classpath. Either:
1. Add `jackson-databind` explicitly, or
2. Include `spring-boot-starter-web` which brings Jackson transitively
