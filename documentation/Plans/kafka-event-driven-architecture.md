# Kafka Event-Driven Architecture - Implementation Plan

## Overview

Transform the application from a direct database access pattern to an event-driven architecture using Apache Kafka:
- **taskboard-api**: Stateless REST API gateway (produces events to Kafka, reads from tasks-service)
- **tasks-service**: Event consumer with H2 + Liquibase persistence (renamed from ping)
- **frontend**: Unchanged (still calls taskboard-api)

## Architecture

```
┌─────────────────────┐         ┌─────────────────────┐         ┌─────────────────────┐
│     frontend        │  REST   │   taskboard-api     │  Kafka  │   tasks-service     │
│   (Thymeleaf UI)    │ ──────> │   (Stateless API)   │ ──────> │  (Event Consumer)   │
│     Port: 8081      │         │     Port: 5100      │         │     Port: 8082      │
│                     │         │                     │         │                     │
│  - Kanban Board     │         │  - REST endpoints   │         │  - Kafka Consumer   │
│  - Task Forms       │         │  - Kafka Producer   │         │  - H2 + Liquibase   │
└─────────────────────┘         │  - REST Client      │         │  - REST endpoints   │
                                └─────────────────────┘         └─────────────────────┘
                                         │                               │
                                         │      REST (reads)             │
                                         └───────────────────────────────┘

                                ┌─────────────────────┐
                                │       Kafka         │
                                │    (Zookeeper +     │
                                │     Broker)         │
                                │                     │
                                │  Topic: task-events │
                                └─────────────────────┘
```

## Data Flow

### Write Operations (Create/Update/Delete)
1. Frontend sends REST request to taskboard-api
2. taskboard-api publishes event to Kafka topic `task-events`
3. taskboard-api returns 202 Accepted immediately
4. tasks-service consumes event and persists to H2 database

### Read Operations (Get tasks)
1. Frontend sends REST request to taskboard-api
2. taskboard-api calls tasks-service REST endpoint
3. tasks-service queries H2 database
4. Response flows back through taskboard-api to frontend

## Phase 1: Rename ping to tasks-service

### 1.1 Folder and Package Rename
- Rename `ping/` folder to `tasks-service/`
- Update package from `com.lenasedkiewicz.ping` to `com.lenasedkiewicz.tasks`
- Update artifactId in pom.xml

### 1.2 Update References
- docker-compose.yml
- k8s/ping-deployment.yaml → k8s/tasks-service-deployment.yaml
- k8s/ping-service.yaml → k8s/tasks-service-service.yaml

## Phase 2: Add Kafka Infrastructure

### 2.1 Docker Compose
Add Zookeeper and Kafka services:
```yaml
zookeeper:
  image: confluentinc/cp-zookeeper:7.5.0
  ports:
    - "2181:2181"

kafka:
  image: confluentinc/cp-kafka:7.5.0
  ports:
    - "9092:9092"
  depends_on:
    - zookeeper
```

### 2.2 Kubernetes
- k8s/zookeeper-deployment.yaml
- k8s/zookeeper-service.yaml
- k8s/kafka-deployment.yaml
- k8s/kafka-service.yaml

## Phase 3: Transform taskboard-api (Producer)

### 3.1 Dependencies Change
Remove:
- spring-boot-starter-data-jpa
- h2

Add:
- spring-kafka
- spring-boot-starter-webflux (for WebClient)

### 3.2 New Components
```
taskboard-api/src/main/java/com/lenasedkiewicz/taskboard/
├── TaskboardApiApplication.java
├── config/
│   ├── CorsConfig.java
│   └── KafkaConfig.java (new)
├── controller/
│   └── TaskController.java (modified)
├── dto/
│   ├── TaskCreateRequest.java
│   ├── TaskUpdateRequest.java
│   ├── StatusUpdateRequest.java
│   └── TaskResponse.java
├── event/
│   └── TaskEvent.java (new)
└── service/
    ├── TaskEventProducer.java (new)
    └── TasksServiceClient.java (new)
```

### 3.3 TaskEvent Structure
```java
public class TaskEvent {
    private String eventType; // CREATE, UPDATE, DELETE
    private String eventId;   // UUID for idempotency
    private Long taskId;      // null for CREATE
    private TaskPayload payload;
    private LocalDateTime timestamp;
}
```

### 3.4 Controller Changes
- POST /api/tasks → Publish CREATE event, return 202 Accepted
- PUT /api/tasks/{id} → Publish UPDATE event, return 202 Accepted
- PATCH /api/tasks/{id}/status → Publish STATUS_UPDATE event, return 202 Accepted
- DELETE /api/tasks/{id} → Publish DELETE event, return 202 Accepted
- GET /api/tasks → Call tasks-service REST endpoint
- GET /api/tasks/{id} → Call tasks-service REST endpoint

## Phase 4: Transform tasks-service (Consumer)

### 4.1 Dependencies
Add:
- spring-boot-starter-data-jpa
- spring-boot-starter-web
- spring-kafka
- h2 (runtime)
- liquibase-core

Remove:
- spring-boot-starter-webflux (no longer needed)

### 4.2 New Components
```
tasks-service/src/main/java/com/lenasedkiewicz/tasks/
├── TasksServiceApplication.java
├── config/
│   └── KafkaConsumerConfig.java
├── controller/
│   └── TaskReadController.java
├── dto/
│   └── TaskResponse.java
├── entity/
│   └── Task.java
├── enums/
│   ├── Priority.java
│   └── Status.java
├── event/
│   ├── TaskEvent.java
│   └── TaskEventConsumer.java
├── repository/
│   └── TaskRepository.java
└── service/
    └── TaskPersistenceService.java
```

### 4.3 Liquibase Setup
```
tasks-service/src/main/resources/
├── application.properties
└── db/
    └── changelog/
        ├── db.changelog-master.yaml
        └── changes/
            └── 001-create-tasks-table.yaml
```

### 4.4 REST Endpoints (for reads)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tasks` | List all tasks |
| GET | `/api/tasks/{id}` | Get task by ID |

## Phase 5: Update Configuration

### 5.1 taskboard-api application.properties
```properties
spring.application.name=taskboard-api
server.port=5100

# Kafka
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Tasks Service
tasks.service.base-url=${TASKS_SERVICE_URL:http://localhost:8082}
```

### 5.2 tasks-service application.properties
```properties
spring.application.name=tasks-service
server.port=8082

# Kafka
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.consumer.group-id=tasks-service
spring.kafka.consumer.auto-offset-reset=earliest

# H2 Database
spring.datasource.url=jdbc:h2:file:/data/tasks;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver

# Liquibase
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml
```

## Phase 6: Update Infrastructure Files

### 6.1 docker-compose.yml
- Add zookeeper service
- Add kafka service
- Rename ping to tasks-service
- Add volume for tasks-service data
- Add environment variables for Kafka bootstrap servers
- Update depends_on chains

### 6.2 Kubernetes Files
- Add Zookeeper deployment and service
- Add Kafka deployment and service
- Rename ping files to tasks-service
- Add ConfigMap for Kafka settings
- Update PVC for tasks-service

## Verification

### Docker Compose
```bash
docker-compose up --build

# Wait for Kafka to be ready, then test:
# Frontend: http://localhost:8081
# taskboard-api: http://localhost:5100/api/tasks
# tasks-service: http://localhost:8082/api/tasks
```

### Test Flow
1. Create a task via frontend
2. Response is 202 (async)
3. Refresh page - task appears (consumed from Kafka, stored in tasks-service)
4. Edit/delete operations work similarly

## Notes

- **Eventual Consistency**: Write operations are asynchronous. There may be a brief delay before new/updated tasks appear.
- **Idempotency**: TaskEvent includes eventId (UUID) to handle potential duplicate message delivery.
- **Error Handling**: Failed event processing should be logged; consider adding dead-letter topic for production.
