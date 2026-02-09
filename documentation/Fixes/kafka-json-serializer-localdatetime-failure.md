# Fix: Kafka JsonSerializer Fails to Serialize LocalDateTime

## Error

```
Failed to create task: 500 Internal Server Error from POST http://taskboard-api:5100/api/tasks
```

Creating a task via the frontend results in a 500 Internal Server Error from the `taskboard-api` service. The error is visible as a flash message on the Kanban board.

## Root Cause

The `TaskEvent` class contains a `LocalDateTime timestamp` field:

```java
public class TaskEvent {
    private String eventType;
    private String eventId;
    private Long taskId;
    private TaskPayload payload;
    private LocalDateTime timestamp; // <-- problematic field
}
```

The `KafkaProducerConfig` specified `JsonSerializer.class` via Kafka config properties:

```java
configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
return new DefaultKafkaProducerFactory<>(configProps);
```

When configured this way, Kafka instantiates the `JsonSerializer` via reflection. The serializer's internal `ObjectMapper` did not have `JavaTimeModule` registered, so it could not serialize `java.time.LocalDateTime`. This caused a `SerializationException` thrown synchronously from `kafkaTemplate.send()`.

Since `TaskController.createTask()` had no error handling, the exception propagated as a raw 500:

```java
@PostMapping
public ResponseEntity<Void> createTask(@Valid @RequestBody TaskCreateRequest request) {
    taskService.createTask(request);  // exception thrown here, no try-catch
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
}
```

### Why it wasn't obvious

- The `spring-boot-starter-web` dependency brings `jackson-datatype-jsr310` transitively, so `JavaTimeModule` is on the classpath
- Spring Boot's auto-configured `ObjectMapper` has `JavaTimeModule` registered and works fine for REST endpoints
- But the Kafka `JsonSerializer` creates its **own** `ObjectMapper` instance, separate from Spring's
- Whether this internal `ObjectMapper` auto-discovers classpath modules depends on the Spring Kafka version's `JacksonUtils.enhancedObjectMapper()` implementation

## Solution

### 1. Add explicit `jackson-datatype-jsr310` dependency

In both `taskboard-api/pom.xml` and `tasks-service/pom.xml`:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

### 2. Configure JsonSerializer with explicit ObjectMapper (producer)

In `KafkaProducerConfig.java`, create an `ObjectMapper` with `JavaTimeModule` and pass it to the `JsonSerializer` directly:

```java
@Bean
public ProducerFactory<String, TaskEvent> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    JsonSerializer<TaskEvent> jsonSerializer = new JsonSerializer<>(objectMapper);

    return new DefaultKafkaProducerFactory<>(
            configProps,
            new StringSerializer(),
            jsonSerializer
    );
}
```

Key changes:
- Serializer instances are passed directly to `DefaultKafkaProducerFactory` instead of via config props
- `ObjectMapper` explicitly registers `JavaTimeModule`
- `WRITE_DATES_AS_TIMESTAMPS` is disabled so dates serialize as ISO strings (e.g., `"2026-02-09T10:30:00"`) instead of arrays

### 3. Configure JsonDeserializer with explicit ObjectMapper (consumer)

In `KafkaConsumerConfig.java`, same approach for the consumer:

```java
ObjectMapper objectMapper = new ObjectMapper();
objectMapper.registerModule(new JavaTimeModule());
objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

JsonDeserializer<TaskEvent> deserializer = new JsonDeserializer<>(TaskEvent.class, objectMapper);
```

### 4. Add error handling to the controller

In `TaskController.java`, wrap the create call in try-catch to log the actual error and prevent raw 500s:

```java
@PostMapping
public ResponseEntity<Void> createTask(@Valid @RequestBody TaskCreateRequest request) {
    try {
        taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    } catch (Exception e) {
        logger.error("Failed to create task: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
```

## Files Changed

| File | Change |
|------|--------|
| `taskboard-api/pom.xml` | Added `jackson-datatype-jsr310` dependency |
| `tasks-service/pom.xml` | Added `jackson-datatype-jsr310` dependency |
| `taskboard-api/.../config/KafkaProducerConfig.java` | Explicit `ObjectMapper` with `JavaTimeModule`, serializers passed directly |
| `tasks-service/.../config/KafkaConsumerConfig.java` | Explicit `ObjectMapper` with `JavaTimeModule` for deserializer |
| `taskboard-api/.../controller/TaskController.java` | Added logging and try-catch to `createTask()` |

## Key Takeaway

When using Spring Kafka's `JsonSerializer`/`JsonDeserializer` with Java Time types (`LocalDateTime`, `Instant`, etc.), always configure the `ObjectMapper` explicitly with `JavaTimeModule`. Do not rely on auto-discovery — pass serializer instances directly to the factory with a properly configured `ObjectMapper`. This makes the configuration explicit and version-independent.
