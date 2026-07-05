# SteviCloud — Cloud-Native Platform

A self-hosted cloud-native platform running on a local Kubernetes cluster (OrbStack, Apple Silicon).
Built as a series of production-quality vertical slices — one bounded context per episode, from cluster provisioning to deployed service.

> **Stack:** Kubernetes (kubeadm) · Flannel · ArgoCD · Zot Registry · Prometheus · Loki · Grafana · Tempo · OTel Collector · Spring Boot 4 · Java 25 · Gradle 9 · Nx 23

---

## Architecture

```
macOS Host (Apple Silicon / OrbStack)
┌─────────────────────────────────────────────────────────────────┐
│  k8s-control-plane      k8s-worker-node1      k8s-worker-node2  │
│  4 CPU · 2 GB           8 CPU · 8 GB           8 CPU · 8 GB     │
│                  ──── Flannel CNI (host-gw) ────                 │
└─────────────────────────────────────────────────────────────────┘

Platform layer (platform/)
  ArgoCD · Zot OCI Registry · LGTM observability stack · OTel Collector

Application layer (stevidigital-platform/)
  Nx polyglot monorepo — Java microservices + custom metriq SDK
```

---

## Repository Layout

```
stevidigital-platform/     Nx monorepo — microservices and SDK
  apps/
    product-catalog/       E-01 · Spring Boot 4 · DDD + Hexagonal Architecture
  sdk/
    metriq/                Custom frequency-distribution metrics SDK (Spring Boot starter)
  docs/
    adr/                   Architecture Decision Records
    c4/                    C4 diagrams (L1 – L3)

platform/
  cluster/                 Ansible playbooks — OrbStack VMs + kubeadm bootstrap
  apps/                    ArgoCD Application manifests (GitOps)
    monitoring/            kube-prometheus-stack · Loki · Tempo · OTel Collector
```

---

## Getting Started

### 1. Provision the cluster

```bash
cd platform/cluster
./provision-vms.sh
ansible-playbook -i ansible/inventory.ini ansible/site.yml
```

See [platform/cluster/README.md](platform/cluster/README.md) for full details.

### 2. Deploy platform apps (ArgoCD)

```bash
kubectl apply -f platform/apps/
```

ArgoCD then reconciles each application from this repository.

### 3. Build System

All microservices share a single Gradle multi-project root at `stevidigital-platform/`.

```bash
cd stevidigital-platform
./gradlew build                                    # build all modules
./gradlew :apps:product-catalog:bootJar            # runnable JAR
./gradlew test                                     # all tests
```

The version catalog lives at `stevidigital-platform/gradle/libs.versions.toml`.

---

## Episode Roadmap

| Episode | Bounded Context | Key concepts |
|---------|----------------|--------------|
| **E-01** | Product Catalog | DDD · Hexagonal · OpenAPI · Micrometer · OTel |
| **E-02** | Pricing | Kafka (Strimzi) · Transactional Outbox · PostgreSQL |
| **E-03** | Inventory | MongoDB · Polyglot persistence |
| **E-04** | Order Management | Saga · Process Manager |
| **E-05** | Billing | Event Sourcing |
| **E-06** | Hardening | Woodpecker CI · Kaniko · Keycloak OIDC · Istio mTLS |

---

## Observability

| Signal | Tool | Access |
|--------|------|--------|
| Metrics | Prometheus + Grafana | `k8s-control-plane.orb.local:30300` |
| Logs | Loki (via Grafana) | datasource `loki` |
| Traces | Tempo (via Grafana) | datasource `tempo` |
| Collector | OTel Collector | gRPC `:4317` · HTTP `:4318` |

OTLP endpoint for services: `otel-collector-opentelemetry-collector.monitoring:4317`

---

## License

[MIT](LICENSE)
