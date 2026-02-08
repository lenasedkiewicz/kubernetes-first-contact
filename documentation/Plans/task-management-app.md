# Task Management Application - Implementation Plan

## Overview

Transform the project from a simple hello-world demo into a Task Management application with:
- **taskboard-api**: REST API with H2 file persistence (renamed from helloworld)
- **frontend**: Thymeleaf Kanban board (new service)
- **ping**: Unchanged (for future use)

## Architecture

```
┌─────────────────────┐         ┌─────────────────────┐
│     frontend        │  REST   │   taskboard-api     │
│   (Thymeleaf UI)    │ ──────> │   (Spring Boot)     │
│     Port: 8081      │         │     Port: 5100      │
│                     │         │                     │
│  - Kanban Board     │         │  - Task CRUD API    │
│  - Task Forms       │         │  - H2 File DB       │
└─────────────────────┘         └─────────────────────┘
                                         │
                                         v
                                ┌─────────────────┐
                                │  /data/taskboard│
                                │   (H2 file)     │
                                └─────────────────┘
```

## Phase 1: Transform helloworld to taskboard-api

### 1.1 Folder Rename
- Rename `helloworld/` to `taskboard-api/`

### 1.2 Dependencies Added (pom.xml)
- `spring-boot-starter-data-jpa`
- `h2` (runtime scope)
- `spring-boot-starter-validation`
- Changed artifactId: `helloworld` to `taskboard-api`

### 1.3 Package Structure
```
taskboard-api/src/main/java/com/lenasedkiewicz/taskboard/
├── TaskboardApiApplication.java
├── config/
│   └── CorsConfig.java
├── controller/
│   └── TaskController.java
├── dto/
│   ├── TaskCreateRequest.java
│   ├── TaskUpdateRequest.java
│   ├── StatusUpdateRequest.java
│   └── TaskResponse.java
├── entity/
│   └── Task.java
├── enums/
│   ├── Priority.java (LOW, MEDIUM, HIGH)
│   └── Status.java (TO_DO, IN_PROGRESS, DONE)
├── repository/
│   └── TaskRepository.java
└── service/
    └── TaskService.java
```

### 1.4 REST API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tasks` | List all tasks |
| GET | `/api/tasks/{id}` | Get task by ID |
| POST | `/api/tasks` | Create task |
| PUT | `/api/tasks/{id}` | Update task |
| PATCH | `/api/tasks/{id}/status` | Update status only |
| DELETE | `/api/tasks/{id}` | Delete task |

### 1.5 Task Entity
- id (Long, auto-generated)
- name (String, required, max 255)
- durationMinutes (Integer, required, min 1)
- priority (Priority enum)
- status (Status enum, default TO_DO)
- createdAt (LocalDateTime)
- updatedAt (LocalDateTime)

## Phase 2: Create Frontend Service

### 2.1 Project Setup
- New Spring Boot project in `frontend/` folder
- Port: 8081

### 2.2 Dependencies
- `spring-boot-starter-web`
- `spring-boot-starter-thymeleaf`
- `spring-boot-starter-webflux` (for WebClient)
- `spring-boot-starter-validation`

### 2.3 Project Structure
```
frontend/src/main/java/com/lenasedkiewicz/frontend/
├── FrontendApplication.java
├── controller/
│   └── KanbanController.java
├── dto/
│   ├── TaskDto.java
│   └── TaskFormDto.java
├── enums/
│   ├── Priority.java
│   └── Status.java
└── service/
    └── TaskboardApiClient.java

frontend/src/main/resources/
├── application.properties
├── static/css/
│   └── kanban.css
└── templates/
    └── kanban.html
```

### 2.4 Kanban Board UI Features
- 3-column Kanban layout (TO DO, IN PROGRESS, DONE)
- Task cards with name, duration, priority badge
- Arrow buttons to move tasks between columns
- Edit/Delete buttons per task
- Modal form for create/edit
- Priority color coding (LOW=green, MEDIUM=yellow, HIGH=red)

## Phase 3: Update Deployment Files

### 3.1 docker-compose.yml
- Added `taskboard-api` service with volume for H2 persistence
- Added `frontend` service with environment variable for API URL
- Added `taskboard-data` volume

### 3.2 Kubernetes Files

**Created:**
- `k8s/taskboard-pvc.yaml` - PersistentVolumeClaim for H2
- `k8s/taskboard-api-deployment.yaml` - with volume mount
- `k8s/taskboard-api-service.yaml` - ClusterIP
- `k8s/frontend-deployment.yaml` - with API URL env var
- `k8s/frontend-service.yaml` - LoadBalancer

**Deleted:**
- `k8s/helloworld-deployment.yaml`
- `k8s/helloworld-service.yaml`

## Verification

### Docker Compose
```bash
docker-compose up --build
# Frontend: http://localhost:8081
# API: http://localhost:5100/api/tasks
```

### Kubernetes
```bash
# Build images
docker build -t taskboard-api:latest ./taskboard-api
docker build -t frontend:latest ./frontend

# Deploy
kubectl apply -f k8s/

# Access frontend via LoadBalancer
# http://localhost:8081
```
