# Kubernetes First Contact

A Task Management application with a Kanban board UI, demonstrating microservices architecture deployable using Docker Compose or Kubernetes.

## Project Structure

```
kubernetes-first-contact/
├── taskboard-api/       # REST API for task management (port 5100)
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── frontend/            # Thymeleaf Kanban board UI (port 8081)
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── ping/                # Utility service (port 8080)
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── k8s/                 # Kubernetes manifests
│   ├── taskboard-api-deployment.yaml
│   ├── taskboard-api-service.yaml
│   ├── taskboard-pvc.yaml
│   ├── frontend-deployment.yaml
│   ├── frontend-service.yaml
│   ├── ping-deployment.yaml
│   └── ping-service.yaml
├── documentation/       # Project documentation
│   ├── Plans/
│   └── Fixes/
└── docker-compose.yml   # Docker Compose configuration
```

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
                                │   (H2 file DB)  │
                                └─────────────────┘
```

## Prerequisites

- Java 25
- Maven 3.6+ (Maven 4.0 works fine)
- Docker Desktop
- kubectl (for Kubernetes deployment)
- Kubernetes cluster (Docker Desktop includes one)

## Quick Start

### Option 1: Run with Docker Compose

```bash
# Build and start all services
docker-compose up --build

# Access the application
# Frontend (Kanban Board): http://localhost:8081
# API: http://localhost:5100/api/tasks

# Cleanup
docker-compose down
```

### Option 2: Run with Kubernetes

```bash
# Build Docker images
docker build -t taskboard-api:latest ./taskboard-api
docker build -t frontend:latest ./frontend

# Deploy to Kubernetes
kubectl apply -f k8s/

# Access the application
# Frontend: http://localhost:8081

# Cleanup
kubectl delete -f k8s/
```

## Services

### taskboard-api (Port 5100)

REST API for task management with H2 file-based persistence.

**API Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tasks` | List all tasks |
| GET | `/api/tasks/{id}` | Get task by ID |
| POST | `/api/tasks` | Create task |
| PUT | `/api/tasks/{id}` | Update task |
| PATCH | `/api/tasks/{id}/status` | Update status only |
| DELETE | `/api/tasks/{id}` | Delete task |

**Task Model:**
- `name` - Task name (required, max 255 chars)
- `durationMinutes` - Estimated duration in minutes (required, min 1)
- `priority` - LOW, MEDIUM, or HIGH
- `status` - TO_DO, IN_PROGRESS, or DONE

**Example API Usage:**
```bash
# Create a task
curl -X POST http://localhost:5100/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"name":"My Task","durationMinutes":30,"priority":"HIGH"}'

# List all tasks
curl http://localhost:5100/api/tasks

# Move task to IN_PROGRESS
curl -X PATCH http://localhost:5100/api/tasks/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}'
```

### frontend (Port 8081)

Thymeleaf-based Kanban board UI.

**Features:**
- 3-column Kanban board (TO DO, IN PROGRESS, DONE)
- Create new tasks via modal form
- Edit existing tasks
- Move tasks between columns with arrow buttons
- Delete tasks
- Priority color coding (red=HIGH, yellow=MEDIUM, green=LOW)

### ping (Port 8080)

Utility service for testing connectivity.

## Building the Applications

### Build all services

```bash
# Build taskboard-api
cd taskboard-api
./mvnw clean package
docker build -t taskboard-api:latest .
cd ..

# Build frontend
cd frontend
./mvnw clean package
docker build -t frontend:latest .
cd ..

# Build ping
cd ping
./mvnw clean package
docker build -t ping:latest .
cd ..
```

## Kubernetes Deployment

### 1. Ensure Kubernetes is running

```bash
kubectl cluster-info
kubectl config current-context
kubectl get nodes
```

### 2. Deploy all services

```bash
kubectl apply -f k8s/

# Verify deployment
kubectl get pods
kubectl get services
kubectl get pvc
```

### 3. Access the application

```bash
# Frontend is exposed via LoadBalancer
# Open http://localhost:8081 in your browser

# Or use port-forward for the API
kubectl port-forward service/taskboard-api 5100:5100
```

## Useful Commands

### Viewing Resources

```bash
kubectl get all
kubectl get pods
kubectl logs deployment/frontend
kubectl logs deployment/taskboard-api
```

### Managing Deployments

```bash
# Restart after rebuilding images
kubectl rollout restart deployment frontend
kubectl rollout restart deployment taskboard-api

# Scale a deployment
kubectl scale deployment frontend --replicas=2
```

### Cleanup

```bash
# Delete all resources
kubectl delete -f k8s/

# Verify cleanup
kubectl get all
```

## Troubleshooting

### Issue: Frontend shows 500 error

**Check frontend logs:**
```bash
kubectl logs deployment/frontend
```

**Common cause:** Template parsing errors with Thymeleaf. See `documentation/Fixes/thymeleaf-enum-parsing-error.md`.

### Issue: Frontend can't connect to API

**Verify API is running:**
```bash
kubectl get pods
kubectl logs deployment/taskboard-api
```

**Test API connectivity:**
```bash
kubectl port-forward service/taskboard-api 5100:5100
curl http://localhost:5100/api/tasks
```

### Issue: ImagePullBackOff error

**Rebuild Docker images:**
```bash
docker build -t taskboard-api:latest ./taskboard-api
docker build -t frontend:latest ./frontend
kubectl rollout restart deployment taskboard-api frontend
```

### Issue: Data not persisting (Kubernetes)

**Check PVC status:**
```bash
kubectl get pvc
kubectl describe pvc taskboard-pvc
```

## Documentation

- **Plans:** `documentation/Plans/` - Implementation plans and architecture decisions
- **Fixes:** `documentation/Fixes/` - Solutions to issues encountered during development

## Notes

- All services use `imagePullPolicy: Never` in Kubernetes to use local Docker images
- H2 database files are stored in `/data/taskboard` (persisted via PVC in Kubernetes, volume in Docker Compose)
- Frontend communicates with API using service name `taskboard-api` within the cluster
