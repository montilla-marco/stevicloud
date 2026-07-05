# Observability Stack

Full LGTM stack deployed via ArgoCD. Covers the three pillars of observability: **metrics**, **logs**, and **traces**.

---

## Architecture

```
Your microservice
    │
    └─ OTLP (gRPC :4317 / HTTP :4318)
           │
           ▼
  ┌─────────────────────┐
  │   OTel Collector    │  ← central hub (otelcol-contrib)
  └────────┬────────────┘
           │
           ▼
  ┌─────────────────────┐
  │    Grafana Tempo    │  ← trace storage (local backend, 10Gi)
  └─────────────────────┘

  ┌─────────────────────┐
  │   Prometheus        │  ← scrapes /actuator/prometheus from pods
  └─────────────────────┘

  ┌─────────────────────┐
  │   Loki + Promtail   │  ← Promtail tails container logs on every node
  └─────────────────────┘

  ┌─────────────────────┐
  │   Grafana           │  ← unified UI, NodePort 30300
  └─────────────────────┘
```

---

## Deployed Components

| ArgoCD Application | Helm Chart | Version | Namespace |
|---|---|---|---|
| `kube-prometheus-stack` | prometheus-community/kube-prometheus-stack | 87.6.0 | monitoring |
| `loki-stack` | grafana/loki-stack | 2.10.3 | monitoring |
| `tempo` | grafana/tempo | 1.24.4 | monitoring |
| `otel-collector` | open-telemetry/opentelemetry-collector | 0.162.0 | monitoring |

All Applications use `syncPolicy.automated` with `prune: true` and `selfHeal: true`.

---

## Access

| Service | URL | Credentials |
|---|---|---|
| Grafana | `http://k8s-control-plane.orb.local:30300` | admin / grafana |
| Prometheus | `http://k8s-worker-node1.orb.local:30090` | — |
| Alertmanager | `http://k8s-worker-node1.orb.local:30093` | — |

---

## Grafana Datasources

| Name | Type | UID | Default |
|---|---|---|---|
| Prometheus | prometheus | `prometheus` | yes |
| Alertmanager | alertmanager | `alertmanager` | no |
| Loki | loki | `loki` | no |
| Tempo | tempo | `tempo` | no |

Tempo is configured with trace-to-logs correlation pointing to Loki (`datasourceUid: loki`).

### Recommended Dashboards

Import these by ID from Grafana → Dashboards → Import:

| Dashboard | ID | Datasource | Coverage |
|---|---|---|---|
| Node Exporter Full | `1860` | Prometheus | per-node CPU, RAM, disk, network |
| Kubernetes Cluster Overview | `15760` | Prometheus | pods, deployments, namespaces |
| Kubernetes / Compute Resources / Pod | `17781` | Prometheus | per-pod resource breakdown |
| PostgreSQL Database | `9628` | Prometheus | Golden Signals (when postgres-exporter is deployed) |
| Loki Dashboard | `13639` | Loki | log volume, error rates by namespace |

---

## OTLP Endpoints (for microservices)

| Protocol | Endpoint | Use |
|---|---|---|
| OTLP gRPC | `otel-collector-opentelemetry-collector.monitoring:4317` | preferred for Java/Spring |
| OTLP HTTP | `otel-collector-opentelemetry-collector.monitoring:4318` | REST-friendly |

**Spring Boot 4 — `application.yaml`:**

```yaml
management:
  tracing:
    sampling:
      probability: 1.0   # 100% in dev; use 0.1 in prod
  otlp:
    tracing:
      endpoint: http://otel-collector-opentelemetry-collector.monitoring:4318/v1/traces
```

**`build.gradle.kts` dependencies:**

```kotlin
implementation("org.springframework.boot:spring-boot-starter-actuator")
implementation("io.micrometer:micrometer-tracing-bridge-otel")
implementation("io.opentelemetry:opentelemetry-exporter-otlp")
```

**Verify the trace pipeline:**

```bash
kubectl run otel-test --image=curlimages/curl:latest --rm -it --restart=Never -n monitoring -- \
  curl -s -X POST http://otel-collector-opentelemetry-collector.monitoring:4318/v1/traces \
  -H 'Content-Type: application/json' \
  -d '{"resourceSpans":[{"resource":{"attributes":[{"key":"service.name","value":{"stringValue":"test"}}]},"scopeSpans":[{"spans":[{"traceId":"aabbccddeeff00112233445566778899","spanId":"aabbccddeeff0011","name":"test","startTimeUnixNano":"1000000000000000000","endTimeUnixNano":"1000000500000000000","status":{}}]}]}]}'

kubectl port-forward svc/tempo -n monitoring 3200:3200
curl http://localhost:3200/api/traces/aabbccddeeff00112233445566778899
```

---

## PromQL

### Cluster Health

```promql
# Node CPU usage (%)
100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# Node RAM available (%)
node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes * 100

# Node disk available (%)
node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"} * 100

# System load average
node_load1
node_load5

# Network I/O per node
rate(node_network_receive_bytes_total[5m])
rate(node_network_transmit_bytes_total[5m])

# Disk I/O per node
rate(node_disk_read_bytes_total[5m])
rate(node_disk_written_bytes_total[5m])
```

### Kubernetes Golden Signals

```promql
# --- TRAFFIC ---
# Bytes received per namespace
sum(rate(container_network_receive_bytes_total[5m])) by (namespace)

# --- ERRORS ---
# Pods not Running (exclude Succeeded)
count by (namespace, phase) (kube_pod_status_phase{phase!="Running",phase!="Succeeded"})

# CrashLoopBackOff pods
kube_pod_container_status_waiting_reason{reason="CrashLoopBackOff"} == 1

# OOMKilled pods
kube_pod_container_status_last_terminated_reason{reason="OOMKilled"} == 1

# Container restarts in the last hour
increase(kube_pod_container_status_restarts_total[1h]) > 0

# --- LATENCY ---
# CPU throttling per pod
rate(container_cpu_throttled_seconds_total[5m]) > 0

# --- SATURATION ---
# RAM usage vs limit >80%
container_memory_working_set_bytes / container_spec_memory_limit_bytes > 0.8

# PVC usage >80%
kubelet_volume_stats_used_bytes / kubelet_volume_stats_capacity_bytes > 0.8
```

### Spring Boot / ms-hello

```promql
# HTTP request rate
rate(http_server_requests_seconds_count[5m])

# HTTP error rate (5xx)
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# P95 response time
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# JVM heap usage
jvm_memory_used_bytes{area="heap"}

# Active HTTP connections
tomcat_connections_current_total
```

### PostgreSQL Golden Signals (when postgres-exporter is deployed)

```promql
# Transactions per second
rate(pg_stat_database_xact_commit_total[5m]) + rate(pg_stat_database_xact_rollback_total[5m])

# Rollbacks per second (error signal)
rate(pg_stat_database_xact_rollback_total[5m])

# Deadlocks
rate(pg_stat_database_deadlocks_total[5m])

# Cache hit ratio (should be > 0.99)
pg_stat_database_blks_hit_total / (pg_stat_database_blks_hit_total + pg_stat_database_blks_read_total)

# Active connections vs max
pg_stat_activity_count / pg_settings_max_connections

# Connections by state
pg_stat_activity_count{state="active"}
pg_stat_activity_count{state="idle in transaction"}

# Database size
pg_database_size_bytes

# Rows modified per second
rate(pg_stat_database_tup_inserted_total[5m])
rate(pg_stat_database_tup_updated_total[5m])
rate(pg_stat_database_tup_deleted_total[5m])
```

### Alerting Expressions

```promql
# Disk below 15% free
node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"} < 0.15

# RAM below 10% free
node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes < 0.10

# Pod down for 5+ minutes
kube_pod_status_phase{phase="Failed"} == 1

# PVC above 85%
kubelet_volume_stats_used_bytes / kubelet_volume_stats_capacity_bytes > 0.85
```

---

## LogQL

### Basic

```logql
# All logs in monitoring namespace
{namespace="monitoring"}

# All namespaces
{namespace=~".+"}

# Specific pod
{namespace="monitoring", pod=~"kube-prometheus-stack-grafana.*"}

# Specific container
{namespace="monitoring", container="prometheus"}
```

### By Level

```logql
# All errors cluster-wide
{namespace=~".+"} | detected_level="error"

# Warnings in monitoring
{namespace="monitoring"} | detected_level="warning"

# Errors and warnings only (exclude info/debug/unknown)
{namespace=~".+"} | detected_level=~"error|warning"
```

### Keyword Search

```logql
{namespace=~".+"} |= "exception"
{namespace=~".+"} |= "timeout"
{namespace=~".+"} |= "connection refused"
{namespace=~".+"} |= "OOMKilled"
{namespace=~".+"} |= "CrashLoopBackOff"

# Spring Boot startup
{namespace=~".+"} |= "Started" |= "seconds"
```

### Microservices

```logql
# Logs for ms-hello
{namespace="ms-hello"}

# HTTP 5xx errors from JSON logs
{namespace="ms-hello"} | json | status >= 500

# Slow requests (>1s, if logs include duration)
{namespace="ms-hello"} | json | duration > 1000

# Correlate with a specific trace
{namespace=~".+"} |= "traceId=aabbccddeeff00112233445566778899"
```

### Log Metrics

```logql
# Log line rate by pod (last 5m)
sum by (pod) (rate({namespace="monitoring"}[5m]))

# Error count by namespace (last 24h)
sum by (namespace) (count_over_time({namespace=~".+"} | detected_level="error" [24h]))

# Top 5 noisiest pods
topk(5, sum by (pod) (rate({namespace=~".+"}[5m])))

# Log volume (bytes) by namespace (last 1h)
sum by (namespace) (bytes_over_time({namespace=~".+"}[1h]))
```

---

## TraceQL (Grafana Explore → Tempo datasource)

```
# All traces from ms-hello
{ resource.service.name = "ms-hello" }

# Slow spans (>500ms)
{ duration > 500ms }

# Failed spans
{ status = error }

# Slow failures from ms-hello
{ resource.service.name = "ms-hello" && duration > 200ms && status = error }

# Spans hitting the /hello endpoint
{ name = "GET /hello" }

# Find spans with a specific HTTP status
{ span.http.status_code = 500 }
```

---

## Diagnostic Commands

```bash
# Full stack status
kubectl get pods -n monitoring
kubectl top pods -n monitoring
kubectl top nodes

# ArgoCD app status
kubectl get applications -n argocd

# Check all ServiceMonitors (what Prometheus is scraping)
kubectl get servicemonitor -n monitoring

# Check Prometheus scrape targets
kubectl port-forward svc/kube-prometheus-stack-prometheus -n monitoring 9090:9090
# → open http://localhost:9090/targets

# Check PrometheusRules (alert definitions)
kubectl get prometheusrule -n monitoring

# Prometheus Operator logs
kubectl logs -n monitoring deploy/kube-prometheus-stack-operator --tail=20

# OTel Collector logs (trace activity)
kubectl logs -n monitoring deploy/otel-collector-opentelemetry-collector --tail=30

# Loki logs
kubectl logs loki-0 -n monitoring --tail=20

# Promtail logs (one per node)
kubectl logs -n monitoring -l app.kubernetes.io/name=promtail --tail=20

# Grafana sidecar logs (datasource ConfigMap loading)
kubectl logs -n monitoring -l app.kubernetes.io/name=grafana -c grafana-sc-datasources --tail=20

# Verify Loki labels available
kubectl run loki-test --image=curlimages/curl:latest --rm -it --restart=Never -n monitoring -- \
  curl -s http://loki.monitoring:3100/loki/api/v1/labels | python3 -m json.tool

# Verify Tempo health
kubectl run tempo-test --image=curlimages/curl:latest --rm -it --restart=Never -n monitoring -- \
  curl -s http://tempo.monitoring:3200/ready
```

---

## Storage

All persistent volumes use `local-path` StorageClass (Rancher local-path-provisioner,
stored at `/opt/local-path-provisioner/` on worker nodes).

| Component | PVC Size | Retention |
|---|---|---|
| Prometheus | 20Gi | 15 days |
| Grafana | 5Gi | — |
| Alertmanager | 2Gi | — |
| Loki | 10Gi | — |
| Tempo | 10Gi | — |

---

## Grafana DB Notes

Grafana persists datasource UIDs in SQLite at `/var/lib/grafana/grafana.db` on the Grafana PVC.
When a datasource UID needs to change after initial provisioning (Grafana ignores UID updates
in ConfigMaps for existing datasources), patch it directly on the worker node:

```bash
# 1. Get the PV name
PV=$(kubectl get pvc kube-prometheus-stack-grafana -n monitoring -o jsonpath='{.spec.volumeName}')
NODE=$(kubectl get pod -n monitoring -l app.kubernetes.io/name=grafana -o jsonpath='{.items[0].spec.nodeName}')
echo "PV $PV on $NODE"

# 2. Patch via SSH (Python is available on Ubuntu worker nodes)
ssh -F ~/.orbstack/ssh/config marcomontilla@${NODE}@orb \
  "sudo python3 -c \"
import sqlite3
db = '/opt/local-path-provisioner/${PV}_monitoring_kube-prometheus-stack-grafana/grafana.db'
conn = sqlite3.connect(db)
conn.execute(\\\"UPDATE data_source SET uid='loki' WHERE name='Loki'\\\")
conn.commit()
print(conn.execute(\\\"SELECT name, uid, url FROM data_source\\\").fetchall())
conn.close()
\""

# 3. Restart Grafana to reload from DB
kubectl rollout restart deployment/kube-prometheus-stack-grafana -n monitoring
```
