# Fix: Kubernetes Full Rebuild and Kafka CrashLoopBackOff

## Context

After a full teardown and cache-free rebuild of the Kubernetes cluster, multiple issues prevented services from starting correctly.

## Issue 1: Docker Image Names Mismatch (`ErrImageNeverPull`)

### Error

```
Warning  ErrImageNeverPull  Container image "tasks-service:latest" is not present with pull policy of Never
```

### Root Cause

K8s deployments reference images as `tasks-service:latest`, `taskboard-api:latest`, etc. with `imagePullPolicy: Never`. However, `docker compose build` names images with the project folder prefix: `kubernetes-first-contact-tasks-service:latest`.

### Solution

After building with `docker compose build`, tag images to match what K8s expects:

```bash
docker tag kubernetes-first-contact-tasks-service:latest tasks-service:latest
docker tag kubernetes-first-contact-taskboard-api:latest taskboard-api:latest
docker tag kubernetes-first-contact-frontend:latest frontend:latest
```

Alternatively, build directly with the correct names:

```bash
docker build --no-cache -t tasks-service:latest ./tasks-service
docker build --no-cache -t taskboard-api:latest ./taskboard-api
docker build --no-cache -t frontend:latest ./frontend
```

## Issue 2: PVC Not Found

### Error

```
Warning  FailedScheduling  0/1 nodes are available: persistentvolumeclaim "tasks-service-pvc" not found
```

### Root Cause

The PVC manifest (`tasks-service-pvc.yaml`) was not applied before the deployment that references it.

### Solution

Always apply all K8s manifests together:

```bash
kubectl apply -f k8s/
```

## Issue 3: Kafka CrashLoopBackOff — Missing `KAFKA_LISTENERS`

### Error

```
port is deprecated. Please use KAFKA_ADVERTISED_LISTENERS instead.
```

Kafka pod enters `CrashLoopBackOff`. The `tasks-service` logs show endless reconnection attempts:

```
Connection to node -1 (kafka/10.103.134.144:9092) could not be established. Node may not be available.
Bootstrap broker kafka:9092 (id: -1 rack: null isFenced: false) disconnected
```

### Root Cause

The Kafka deployment (`k8s/kafka-deployment.yaml`) had `KAFKA_ADVERTISED_LISTENERS` configured but was missing `KAFKA_LISTENERS`. Without it, `cp-kafka:7.5.0` does not know which interface and port to bind to, causing it to crash on startup.

Note: In `docker-compose.yml` this worked because two listeners were configured (`PLAINTEXT` on 29092 and `PLAINTEXT_HOST` on 9092). The K8s config simplified to a single listener but omitted the bind address.

### Solution

Add `KAFKA_LISTENERS` to `k8s/kafka-deployment.yaml`:

```yaml
env:
- name: KAFKA_LISTENERS
  value: "PLAINTEXT://0.0.0.0:9092"
- name: KAFKA_ADVERTISED_LISTENERS
  value: "PLAINTEXT://kafka:9092"
- name: KAFKA_LISTENER_SECURITY_PROTOCOL_MAP
  value: "PLAINTEXT:PLAINTEXT"
```

Then re-apply:

```bash
kubectl apply -f k8s/kafka-deployment.yaml
kubectl rollout restart deployment kafka
```

## Full Clean Rebuild Procedure

For reference, the complete teardown and rebuild sequence:

```bash
# 1. Delete all K8s resources
kubectl delete -f k8s/

# 2. Prune Docker build cache and volumes
docker builder prune -a -f
docker volume rm kubernetes-first-contact_tasks-data

# 3. Rebuild images with no cache
docker compose build --no-cache

# 4. Tag images for K8s
docker tag kubernetes-first-contact-tasks-service:latest tasks-service:latest
docker tag kubernetes-first-contact-taskboard-api:latest taskboard-api:latest
docker tag kubernetes-first-contact-frontend:latest frontend:latest

# 5. Deploy to K8s
kubectl apply -f k8s/
```

## Key Takeaway

When rebuilding from scratch, remember that `docker compose build` and K8s use different image naming conventions. Always verify images exist with the expected names (`docker images <name>`) before deploying. For Kafka in K8s, always explicitly set `KAFKA_LISTENERS` alongside `KAFKA_ADVERTISED_LISTENERS`.
