# platform/

GitOps-oriented infrastructure for the local Kubernetes development platform.

```
platform/
├── cluster/    ← One-time cluster bootstrap (OrbStack VMs + Ansible)
└── apps/       ← Declarative app manifests managed by ArgoCD
    ├── monitoring/
    │   ├── prometheus/     ← kube-prometheus-stack (Prometheus + Grafana + Alertmanager)
    │   ├── loki/           ← Loki + Promtail (log aggregation)
    │   ├── tempo/          ← Grafana Tempo (distributed tracing)
    │   └── otel-collector/ ← OpenTelemetry Collector (OTLP hub)
    ├── zot/                ← Zot OCI registry (NodePort 30500)
    ├── postgres/           ← (pending migration)
    ├── keycloak/           ← (pending migration)
    ├── jenkins/            ← (pending migration)
    └── ms-hello/           ← (pending migration)
```

---

## cluster/

Creates and configures the Kubernetes cluster. Runs imperatively once (and on rebuilds). See [cluster/README.md](./cluster/README.md).

```bash
cd platform/cluster
./provision-vms.sh
ansible-playbook -i ansible/inventory.ini ansible/site.yml
```

---

## apps/

Kubernetes manifests and Helm values for each workload. ArgoCD watches these directories and reconciles the cluster state whenever changes are pushed to `main`.

Each app directory contains:
- `Application.yaml` — ArgoCD Application resource (source of truth)
- `values.yaml` — Helm values mirror (for readability; ArgoCD uses inline values in Application.yaml)

### Currently deployed

| App | Chart | Access |
|---|---|---|
| Zot OCI registry | project-zot/zot 0.1.119 | `<worker-node2>:30500` |
| kube-prometheus-stack | prometheus-community 87.6.0 | Grafana: `k8s-control-plane.orb.local:30300` |
| loki-stack | grafana/loki-stack 2.10.3 | via Grafana |
| tempo | grafana/tempo 1.24.4 | via Grafana |
| otel-collector | open-telemetry/opentelemetry-collector 0.162.0 | `*.monitoring:4317/4318` |

See [apps/monitoring/README.md](./apps/monitoring/README.md) for full observability stack documentation.

### Adding a new app

1. Create `platform/apps/<name>/Application.yaml` pointing to a Helm chart or Git path
2. `kubectl apply -f platform/apps/<name>/Application.yaml`
3. ArgoCD picks it up, syncs, and keeps it reconciled

> Workloads in `infra-platforms/kubernetes/` (Keycloak, Jenkins, PostgreSQL) are being migrated
> progressively to `platform/apps/`.
