# Fix: Kafka CrashLoopBackOff Due to Kubernetes Service Links

## Error

```
kafka-xxx   0/1   CrashLoopBackOff   3 (45s ago)   2m
```

Kafka pod logs show only:

```
===> User
uid=1000(appuser) gid=1000(appuser) groups=1000(appuser)
===> Configuring ...
Running in Zookeeper mode...
port is deprecated. Please use KAFKA_ADVERTISED_LISTENERS instead.
```

Container exits with code 1 immediately after the deprecation warning.

## Root Cause

Kubernetes automatically injects environment variables for every Service into every pod in the same namespace. When a Service is named `kafka`, Kubernetes creates variables like:

```
KAFKA_PORT=tcp://10.96.x.x:9092
KAFKA_SERVICE_HOST=10.96.x.x
KAFKA_SERVICE_PORT=9092
KAFKA_PORT_9092_TCP=tcp://10.96.x.x:9092
```

The Confluent `cp-kafka` Docker image's startup script (`/etc/confluent/docker/configure`) checks for deprecated environment variables. When it finds `KAFKA_PORT` is set, it assumes it's a deprecated Kafka broker configuration and **exits with code 1**:

```bash
if [[ -n "${KAFKA_PORT-}" ]]
then
  echo "port is deprecated. Please use KAFKA_ADVERTISED_LISTENERS instead."
  exit 1
fi
```

The same check exists for `KAFKA_HOST`, `KAFKA_ADVERTISED_PORT`, and `KAFKA_ADVERTISED_HOST`.

This is a naming collision between Kubernetes Service auto-injected environment variables and Confluent Kafka's expected environment variable namespace.

## Solution

Add `enableServiceLinks: false` to the Kafka pod spec in `k8s/kafka-deployment.yaml`:

```yaml
spec:
  template:
    spec:
      enableServiceLinks: false    # <-- prevents K8s from injecting service env vars
      containers:
      - name: kafka
        image: confluentinc/cp-kafka:7.5.0
        ...
```

Then apply and restart:

```bash
kubectl apply -f k8s/kafka-deployment.yaml
```

## Why This Only Happens in Kubernetes

- **Docker Compose**: No automatic environment variable injection. Only explicitly defined `environment:` variables exist. No conflict.
- **Kubernetes**: Service discovery injects `<SERVICE_NAME>_PORT`, `<SERVICE_NAME>_SERVICE_HOST`, etc. into all pods. A Service named `kafka` creates `KAFKA_PORT` which collides with Confluent's deprecated config check.

## Key Takeaway

When running Confluent Kafka in Kubernetes with a Service named `kafka`, always set `enableServiceLinks: false` in the pod spec. This prevents Kubernetes from injecting `KAFKA_PORT` and other service link variables that collide with Confluent's configuration namespace.

This is a known issue that affects any Confluent Kafka Docker image version that checks for deprecated `KAFKA_PORT`/`KAFKA_HOST` environment variables.
