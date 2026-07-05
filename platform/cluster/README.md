# Local Kubernetes Cluster — OrbStack + Ansible

A fully automated, reproducible multi-node Kubernetes cluster for Apple Silicon. Two commands create the VMs and bootstrap the full cluster with Flannel CNI and ArgoCD. If a node breaks, one command rebuilds it without touching the rest.

---

## Prerequisites

Install once on your Mac:

```bash
# 1. OrbStack — lightweight Linux VMs on Apple Silicon (replaces Docker Desktop, Multipass, VMware)
brew install --cask orbstack

# 2. Ansible — runs playbooks from your Mac into the VMs
brew install ansible

# 3. Ansible collections used by the roles
ansible-galaxy collection install -r ansible/requirements.yml
```

Verify:

```bash
orb version                # OrbStack CLI
ansible --version          # 2.15+
```

---

## Architecture

```
┌──────────────────────────────────────── macOS Host (Apple Silicon) ─────────────────────────────────────────┐
│                                                                                                              │
│   provision-vms.sh  ──► creates OrbStack VMs + inventory.ini                                                │
│   ansible/site.yml  ──► configures everything inside VMs                                                    │
│                                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────────────────────────────┐   │
│  │  OrbStack (Apple Virtualization Framework — native ARM64)                                            │   │
│  │                                                                                                      │   │
│  │  ┌────────────────────────┐   ┌────────────────────────┐   ┌────────────────────────┐              │   │
│  │  │  k8s-control-plane     │   │  k8s-worker-node1      │   │  k8s-worker-node2      │              │   │
│  │  │  192.168.139.x         │   │  192.168.139.x         │   │  192.168.139.x         │              │   │
│  │  │  4 CPU · 2 GB          │   │  8 CPU · 8 GB          │   │  8 CPU · 8 GB          │              │   │
│  │  │                        │   │                        │   │                        │              │   │
│  │  │  containerd            │   │  containerd            │   │  containerd            │              │   │
│  │  │  kubelet               │   │  kubelet               │   │  kubelet               │              │   │
│  │  │  kubeadm               │   │  kubeadm               │   │  kubeadm               │              │   │
│  │  │  ──────────────────    │   └────────────────────────┘   └────────────────────────┘              │   │
│  │  │  kube-apiserver        │                │                          │                            │   │
│  │  │  etcd                  │◄───────────────┴────── Flannel CNI ───────┘                            │   │
│  │  │  kube-scheduler        │       (pod network: 10.244.0.0/16, host-gw backend)                    │   │
│  │  │  kube-controller       │                                                                         │   │
│  │  │  ArgoCD                │                                                                         │   │
│  │  └────────────────────────┘                                                                         │   │
│  │               OrbStack internal network: 192.168.139.0/24                                           │   │
│  └──────────────────────────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                                              │
│   ./kubeconfig  ◄── fetched automatically after provisioning                                                │
└──────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### What each layer owns

| Layer             | Tool                            | Responsibility                                              |
| ----------------- | ------------------------------- | ----------------------------------------------------------- |
| VM lifecycle      | `provision-vms.sh` / `destroy-vms.sh` | Create, configure, destroy OrbStack VMs             |
| VM hardware       | OrbStack (Apple Virt. Framework) | Native ARM64 VMs, internal networking                      |
| OS configuration  | Ansible `common` role           | kernel modules, sysctl, containerd, kubeadm/kubelet/kubectl |
| Cluster bootstrap | Ansible `control_plane` role    | `kubeadm init`, Flannel, kube-proxy patch, join-command     |
| Node join         | Ansible `worker` role           | `kubeadm join`                                              |
| GitOps            | Ansible `argocd` role + ArgoCD  | All app deployments after cluster is up                     |

### SSH routing

OrbStack routes SSH via the `@orb` proxy: `<macOS-user>@<machine-name>@orb`. The Ansible inventory uses this pattern. The OrbStack SSH key is at `~/.orbstack/ssh/id_ed25519`.

---

## Cluster Operations

### Bring up everything from scratch

```bash
cd infra-platforms/vagrant

# Step 1 — create OrbStack VMs and generate inventory
./provision-vms.sh          # default: 1 control-plane + 2 workers
./provision-vms.sh 3        # or N workers

# Step 2 — bootstrap the cluster
ansible-playbook -i ansible/inventory.ini ansible/site.yml
```

What happens in order during `ansible-playbook`:

1. **common** on all nodes: kernel modules, sysctl, containerd, kubeadm/kubelet/kubectl, kubelet EXTRA_ARGS
2. **control_plane**: `kubeadm init`, kube-proxy conntrack patch, Flannel v0.26.7 (host-gw), fetches `kubeconfig` to your Mac
3. **worker** on both workers: joins the cluster using the saved join-command
4. **argocd**: installs ArgoCD and prints the initial admin password

Expected time: ~15–20 minutes on first run (package downloads). Re-runs on existing VMs: ~3 minutes (most tasks are idempotent).

### Access the cluster from your Mac

After provisioning, `kubeconfig` is written to this directory:

```bash
export KUBECONFIG=$(pwd)/kubeconfig
kubectl get nodes -o wide
```

Or merge into your default config:

```bash
KUBECONFIG=~/.kube/config:$(pwd)/kubeconfig kubectl config view --flatten > /tmp/merged
mv /tmp/merged ~/.kube/config
kubectl config use-context kubernetes-admin@kubernetes
```

---

### Stop the cluster (preserve VM state)

```bash
orb stop k8s-control-plane
orb stop k8s-worker-node1
orb stop k8s-worker-node2
```

### Restart after stop

```bash
orb start k8s-control-plane
orb start k8s-worker-node1
orb start k8s-worker-node2
```

Kubernetes recovers automatically: etcd restores cluster state, pods reschedule, Flannel re-establishes routes. No manual intervention needed.

### Rebuild a single broken node

If a worker node becomes unresponsive or its disk is corrupted:

```bash
export KUBECONFIG=$(pwd)/kubeconfig

# Drain and remove the node from the cluster
kubectl drain k8s-worker-node1 --ignore-daemonsets --delete-emptydir-data
kubectl delete node k8s-worker-node1

# Destroy the broken VM and recreate it
orb delete k8s-worker-node1 -f
orb create ubuntu:22.04 k8s-worker-node1

# Re-run Ansible — it detects the node is unjoined and runs the full join
ansible-playbook -i ansible/inventory.ini ansible/site.yml
```

### Rebuild the entire cluster

```bash
./destroy-vms.sh
./provision-vms.sh
ansible-playbook -i ansible/inventory.ini ansible/site.yml
```

### Re-run Ansible only (no VM rebuild)

Useful after editing a role — applies changes without recreating VMs:

```bash
ansible-playbook -i ansible/inventory.ini ansible/site.yml
```

---

## ArgoCD

ArgoCD is installed during provisioning. To access the UI:

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
# Open https://localhost:8080
# User: admin
# Password: shown at the end of provisioning, or retrieve with:
kubectl get secret argocd-initial-admin-secret -n argocd \
  -o jsonpath='{.data.password}' | base64 -d && echo
```

> The `argocd-dex-server` pod will be in CrashLoopBackOff — this is expected when SSO connectors
> (GitHub, LDAP, etc.) are not configured. It does not affect login with the local `admin` user.

Point ArgoCD at this Git repository to manage all platform app deployments (Zot, Keycloak, PostgreSQL, Jenkins). See `../kubernetes/` for the existing Helm-based deployments that can be converted to ArgoCD `Application` manifests.

---

## Customizing the Cluster

### Change node count

```bash
./provision-vms.sh 3        # creates k8s-worker-node3 if it doesn't exist
ansible-playbook -i ansible/inventory.ini ansible/site.yml
```

### Change Kubernetes version

Edit `ansible/roles/common/defaults/main.yml` and `ansible/roles/control_plane/defaults/main.yml`:

```yaml
kubernetes_version: "1.33"   # change here
```

Then destroy and reprovision:

```bash
./destroy-vms.sh && ./provision-vms.sh && ansible-playbook -i ansible/inventory.ini ansible/site.yml
```

### Change pod network CIDR

Edit `pod_network_cidr` in `ansible/roles/control_plane/defaults/main.yml`. This value is passed to both `kubeadm init` and the Flannel manifest.

---

## OrbStack-Specific Notes

OrbStack's Apple Virtualization Framework kernel has two restrictions that require workarounds (both handled automatically by the Ansible roles):

**1. Swap cannot be disabled** — `swapoff -a` returns an error (rc=32/64). The `common` role uses `--fail-swap-on=false` in KUBELET_EXTRA_ARGS and `--ignore-preflight-errors=Swap` in `kubeadm init`.

**2. kube-proxy cannot write `/proc/sys/net/netfilter/nf_conntrack_max`** — The container gets `permission denied`. The `control_plane` role patches the kube-proxy ConfigMap to set `maxPerCore: 0` and `min: 0`, which disables conntrack size management.

---

## Further Reading

### OrbStack

- [OrbStack documentation](https://docs.orbstack.dev/) — Linux machines, networking, SSH
- [OrbStack Linux machines](https://docs.orbstack.dev/machines/) — `orb create`, `orb run`, SSH routing

### Ansible

- [Ansible Documentation](https://docs.ansible.com/ansible/latest/index.html)
- Key concepts: Inventory → Playbooks → Variables → Roles → Handlers
- [Ansible for DevOps](https://www.ansiblefordevops.com/) by Jeff Geerling — best hands-on book
- [Galaxy — ansible.posix collection](https://docs.ansible.com/ansible/latest/collections/ansible/posix/index.html)
- [Galaxy — community.general collection](https://docs.ansible.com/ansible/latest/collections/community/general/index.html)

### Kubernetes internals

- [Kubernetes the Hard Way](https://github.com/kelseyhightower/kubernetes-the-hard-way) — builds the cluster by hand; reading it demystifies what kubeadm automates
- [Official kubeadm reference](https://kubernetes.io/docs/reference/setup-tools/kubeadm/)
- [Kubernetes Networking](https://kubernetes.io/docs/concepts/cluster-administration/networking/)

### ArgoCD

- [ArgoCD Getting Started](https://argo-cd.readthedocs.io/en/stable/getting_started/)
- [App of Apps pattern](https://argo-cd.readthedocs.io/en/stable/operator-manual/cluster-bootstrapping/)
