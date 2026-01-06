# Kubernetes First Contact

A simple microservices demo application consisting of two Spring Boot services that communicate with each other, deployable using Docker Compose or Kubernetes.

## Project Structure

```
kubernetes-first-contact/
├── helloworld/          # Service that returns "Hello world!" on port 5100
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── ping/                # Service that calls helloworld service on port 8080
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── k8s/                 # Kubernetes manifests
│   ├── helloworld-deployment.yaml
│   ├── helloworld-service.yaml
│   ├── ping-deployment.yaml
│   └── ping-service.yaml
└── docker-compose.yml   # Docker Compose configuration
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
# Build and start both services
docker-compose up --build

# Test the ping service (calls helloworld internally)
curl http://localhost:8080

# Cleanup
docker-compose down
```

### Option 2: Run with Kubernetes

See the [Kubernetes Deployment](#kubernetes-deployment) section below.

## Building the Applications

### Build helloworld service

```bash
cd helloworld
./mvnw clean package
docker build -t helloworld:latest .
cd ..
```

### Build ping service

```bash
cd ping
./mvnw clean package
docker build -t ping:latest .
cd ..
```

## Kubernetes Deployment

### 1. Ensure Kubernetes is running

```bash
# Check if kubectl is configured
kubectl cluster-info

# Check current context
kubectl config current-context

# List all nodes
kubectl get nodes
```

### 2. Deploy helloworld service

```bash
# Apply helloworld deployment and service
kubectl apply -f k8s/helloworld-deployment.yaml
kubectl apply -f k8s/helloworld-service.yaml

# Verify deployment
kubectl get deployments
kubectl get pods
kubectl get services
```

### 3. Deploy ping service

```bash
# Apply ping deployment and service
kubectl apply -f k8s/ping-deployment.yaml
kubectl apply -f k8s/ping-service.yaml

# Verify deployment
kubectl get deployments
kubectl get pods
kubectl get services
```

### 4. Test the application

```bash
# Get the ping service details
kubectl get service ping

# Port-forward to access ping service locally
kubectl port-forward service/ping 8080:8080

# In another terminal, test the service
curl http://localhost:8080
```

Expected output: `Hello world!`

## Useful Kubernetes Commands

### Viewing Resources

```bash
# List all resources in default namespace
kubectl get all

# Get detailed info about a pod
kubectl describe pod <pod-name>

# View pod logs
kubectl logs <pod-name>

# Follow logs in real-time
kubectl logs -f <pod-name>

# Get logs from a specific container in a pod
kubectl logs <pod-name> -c <container-name>
```

### Managing Deployments

```bash
# Restart a deployment (useful after rebuilding Docker image)
kubectl rollout restart deployment helloworld
kubectl rollout restart deployment ping

# Check rollout status
kubectl rollout status deployment helloworld

# Scale a deployment
kubectl scale deployment helloworld --replicas=3

# Delete a deployment
kubectl delete deployment helloworld
```

### Debugging

```bash
# Execute commands inside a pod
kubectl exec -it <pod-name> -- /bin/sh

# Test connectivity between pods
kubectl exec -it <ping-pod-name> -- curl http://helloworld:5100/helloworld

# Check service endpoints
kubectl get endpoints
```

### Cleanup

**IMPORTANT: Stop Kubernetes resources when done (especially before shutting down your computer)**

```bash
# Delete all resources using manifest files (Recommended)
kubectl delete -f k8s/

# Or delete individually
kubectl delete deployment helloworld ping
kubectl delete service helloworld ping

# Verify everything is deleted
kubectl get all
```

**If you want to completely stop Kubernetes:**
1. Open Docker Desktop
2. Go to Settings → Kubernetes
3. Uncheck "Enable Kubernetes"
4. Click "Apply & Restart"

## Troubleshooting

### Issue: kubectl commands not working or wrong cluster

**Solution: Switch to Docker Desktop Kubernetes context**

```bash
# List all available contexts
kubectl config get-contexts

# Switch to Docker Desktop context
kubectl config use-context docker-desktop

# Verify the switch
kubectl config current-context

# Check if cluster is accessible
kubectl cluster-info
```

### Issue: Connection refused error from ping to helloworld

**Symptoms:**
```
java.net.ConnectException: finishConnect(..) failed with error(-111): Connection refused
```

**Solutions:**

1. **Check if helloworld pod is running:**
   ```bash
   kubectl get pods
   ```
   Expected: helloworld pod should be in `Running` state

2. **Check helloworld pod logs:**
   ```bash
   kubectl logs <helloworld-pod-name>
   ```
   Verify that the application started successfully and is listening on port 5100

3. **Verify service endpoints:**
   ```bash
   kubectl get endpoints helloworld
   ```
   Should show the pod IP and port 5100

4. **Test connectivity from ping pod:**
   ```bash
   kubectl exec -it <ping-pod-name> -- curl http://helloworld:5100/helloworld
   ```

5. **Rebuild and redeploy if needed:**
   ```bash
   cd helloworld
   ./mvnw clean package
   docker build -t helloworld:latest .
   kubectl rollout restart deployment helloworld
   cd ..
   ```

### Issue: ImagePullBackOff error

**Symptoms:**
```
ErrImagePull or ImagePullBackOff in pod status
```

**Solutions:**

1. **Verify Docker images exist locally:**
   ```bash
   docker images | grep -E "helloworld|ping"
   ```

2. **Rebuild the Docker images:**
   ```bash
   # Build helloworld
   cd helloworld
   ./mvnw clean package
   docker build -t helloworld:latest .
   cd ..

   # Build ping
   cd ping
   ./mvnw clean package
   docker build -t ping:latest .
   cd ..
   ```

3. **Ensure imagePullPolicy is set to Never** (already configured in the manifests)

### Issue: Pods stuck in Pending state

**Check cluster resources:**
```bash
kubectl describe pod <pod-name>
kubectl top nodes  # Requires metrics-server
```

### Issue: Port already in use (Docker Compose)

**Solution:**
```bash
# Find process using the port
netstat -ano | findstr :8080
netstat -ano | findstr :5100

# Kill the process or use docker-compose down
docker-compose down
```

### Issue: Native library warning from Netty

**Symptoms:**
```
WARNING: A restricted method in java.lang.System has been called
WARNING: java.lang.System::loadLibrary has been called by io.netty...
```

**Note:** This is just a warning in newer Java versions and does not affect functionality. Can be safely ignored or suppressed with JVM args if needed.

## Docker Desktop Kubernetes Setup

### Enable Kubernetes in Docker Desktop

1. Open Docker Desktop
2. Go to Settings → Kubernetes
3. Check "Enable Kubernetes"
4. Click "Apply & Restart"
5. Wait for Kubernetes to start (green indicator)

### Verify Setup

```bash
kubectl config use-context docker-desktop
kubectl cluster-info
kubectl get nodes
```

## Architecture

```
┌─────────────────┐
│                 │
│   ping:8080     │  HTTP GET /
│                 │
└────────┬────────┘
         │
         │ HTTP GET http://helloworld:5100/helloworld
         │
         ▼
┌─────────────────┐
│                 │
│ helloworld:5100 │  Returns "Hello world!"
│                 │
└─────────────────┘
```

## Service Endpoints

- **ping service**: `http://localhost:8080/` - Entry point that calls helloworld
- **helloworld service**: `http://localhost:5100/helloworld` - Returns "Hello world!"

In Kubernetes:
- **ping**: `http://ping:8080/` (ClusterIP)
- **helloworld**: `http://helloworld:5100/helloworld` (ClusterIP)

## Notes

- Both services use `imagePullPolicy: Never` in Kubernetes to use local Docker images
- The services communicate within the Kubernetes cluster using DNS (service names)
- Port 5100 is used by helloworld (configured in `application.properties`)
- Port 8080 is used by ping (Spring Boot default)
