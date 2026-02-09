# Rebuild and Deploy Guide

How to rebuild Docker images and deploy the application after code changes.

## Architecture Overview

The application consists of 6 services:

| Service | Port | Image Name | Source |
|---------|------|------------|--------|
| frontend | 8081 | `frontend:latest` | `./frontend` |
| taskboard-api | 5100 | `taskboard-api:latest` | `./taskboard-api` |
| tasks-service | 8082 | `tasks-service:latest` | `./tasks-service` |
| kafka | 9092 | `confluentinc/cp-kafka:7.5.0` | Docker Hub |
| zookeeper | 2181 | `confluentinc/cp-zookeeper:7.5.0` | Docker Hub |

Only the first three are custom images that need rebuilding after code changes.

## Option A: Docker Compose (Local Development)

### Rebuild and restart all services

```bash
docker compose build --no-cache
docker compose up -d
```

### Rebuild a single service

```bash
docker compose build --no-cache taskboard-api
docker compose up -d taskboard-api
```

### View logs

```bash
# All services
docker compose logs -f

# Single service
docker compose logs -f taskboard-api
```

### Full teardown and fresh start

```bash
docker compose down -v
docker compose build --no-cache
docker compose up -d
```

The `-v` flag removes volumes (including the H2 database data). Omit it to preserve data.

## Option B: Kubernetes (Minikube)

### Step 1: Build Docker images

Build images directly with the names that K8s deployments expect (defined in `k8s/*-deployment.yaml` with `imagePullPolicy: Never`):

```bash
docker build --no-cache -t tasks-service:latest ./tasks-service
docker build --no-cache -t taskboard-api:latest ./taskboard-api
docker build --no-cache -t frontend:latest ./frontend
```

**Alternative:** If building via Docker Compose, the images get a project prefix that must be re-tagged:

```bash
docker compose build --no-cache
docker tag kubernetes-first-contact-tasks-service:latest tasks-service:latest
docker tag kubernetes-first-contact-taskboard-api:latest taskboard-api:latest
docker tag kubernetes-first-contact-frontend:latest frontend:latest
```

### Step 2: Deploy to Kubernetes

**First deployment** (applies all manifests at once):

```bash
kubectl apply -f k8s/
```

**After rebuilding images** (restart deployments to pick up new images):

```bash
kubectl rollout restart deployment tasks-service
kubectl rollout restart deployment taskboard-api
kubectl rollout restart deployment frontend
```

### Step 3: Verify deployment

```bash
# Check all pods are running
kubectl get pods

# Check services
kubectl get services

# Watch pods until ready
kubectl get pods -w
```

Expected output — all pods should show `Running` with `1/1` ready:

```
NAME                              READY   STATUS    RESTARTS   AGE
zookeeper-xxx                     1/1     Running   0          5m
kafka-xxx                         1/1     Running   0          5m
tasks-service-xxx                 1/1     Running   0          2m
taskboard-api-xxx                 1/1     Running   0          2m
frontend-xxx                      1/1     Running   0          2m
```

### Step 4: Access the application

```bash
# Port-forward the frontend to access from browser
kubectl port-forward service/frontend 8081:8081
```

Then open http://localhost:8081 in the browser.

### Viewing logs in Kubernetes

```bash
# Logs for a specific service
kubectl logs -f deployment/taskboard-api
kubectl logs -f deployment/tasks-service
kubectl logs -f deployment/frontend
kubectl logs -f deployment/kafka
```

## Rebuilding a Single Service

When only one service has changed, rebuild and restart only that service.

### Docker Compose

```bash
docker compose build --no-cache <service-name>
docker compose up -d <service-name>
```

### Kubernetes

```bash
docker build --no-cache -t <image-name>:latest ./<service-dir>
kubectl rollout restart deployment <deployment-name>
```

Example for `taskboard-api`:

```bash
docker build --no-cache -t taskboard-api:latest ./taskboard-api
kubectl rollout restart deployment taskboard-api
```

## Full Clean Rebuild (Kubernetes)

When you need to start completely fresh:

```bash
# 1. Delete all K8s resources
kubectl delete -f k8s/

# 2. Prune Docker build cache and volumes
docker builder prune -a -f
docker volume rm kubernetes-first-contact_tasks-data 2>$null

# 3. Rebuild all images
docker build --no-cache -t tasks-service:latest ./tasks-service
docker build --no-cache -t taskboard-api:latest ./taskboard-api
docker build --no-cache -t frontend:latest ./frontend

# 4. Deploy everything
kubectl apply -f k8s/

# 5. Verify
kubectl get pods -w
```

## Troubleshooting

### Pod stuck in `ErrImageNeverPull`

The image name doesn't match what K8s expects. Verify with:

```bash
docker images | grep -E "tasks-service|taskboard-api|frontend"
```

Images must exist as `<name>:latest` (without project prefix).

### Pod stuck in `CrashLoopBackOff`

Check the logs for the failing pod:

```bash
kubectl logs <pod-name>
kubectl describe pod <pod-name>
```

### Services can't communicate

Verify K8s services are created:

```bash
kubectl get services
```

Expected services: `kafka` (9092), `zookeeper` (2181), `tasks-service` (8082), `taskboard-api` (5100), `frontend` (8081).

### Database reset needed

The tasks-service uses H2 with a PersistentVolumeClaim. To reset the database:

```bash
kubectl delete pvc tasks-service-pvc
kubectl apply -f k8s/tasks-service-pvc.yaml
kubectl rollout restart deployment tasks-service
```
