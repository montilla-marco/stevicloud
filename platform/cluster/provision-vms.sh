#!/usr/bin/env bash
# provision-vms.sh — Creates OrbStack Linux machines and generates Ansible inventory.
# Replaces Vagrantfile. Idempotent: skips machines that already exist.
#
# Usage:
#   ./provision-vms.sh          # create cluster (2 workers)
#   ./provision-vms.sh 3        # create cluster with N workers

set -euo pipefail

UBUNTU_IMAGE="ubuntu:22.04"
NUM_WORKERS=${1:-2}
CONTROL_PLANE="k8s-control-plane"
# OrbStack always creates a user matching the macOS username
ORB_USER="$(id -un)"

GREEN="\033[1;32m"
BLUE="\033[1;34m"
YELLOW="\033[1;33m"
NC="\033[0m"

create_machine() {
  local name=$1
  if orb info "$name" &>/dev/null 2>&1; then
    echo -e "  ${YELLOW}$name already exists — skipping${NC}"
  else
    echo -e "  ${BLUE}Creating $name...${NC}"
    orb create "$UBUNTU_IMAGE" "$name"
  fi
}

wait_for_ssh() {
  local name=$1
  echo -ne "  Waiting for $name SSH..."
  # Use OrbStack @orb routing: <user>@<machine>@orb — requires Host orb in ~/.orbstack/ssh/config
  until ssh -o StrictHostKeyChecking=no -o ConnectTimeout=3 -o BatchMode=yes \
    "${ORB_USER}@${name}@orb" "echo ok" &>/dev/null 2>&1; do
    echo -n "."
    sleep 2
  done
  echo -e " ${GREEN}ready${NC}"
}

# ── Create machines ──────────────────────────────────────────────────────────
echo -e "${BLUE}=== Creating OrbStack machines ===${NC}"
create_machine "$CONTROL_PLANE"
for i in $(seq 1 "$NUM_WORKERS"); do
  create_machine "k8s-worker-node${i}"
done

# ── Wait for SSH ─────────────────────────────────────────────────────────────
echo -e "\n${BLUE}=== Waiting for SSH on all nodes ===${NC}"
wait_for_ssh "$CONTROL_PLANE"
for i in $(seq 1 "$NUM_WORKERS"); do
  wait_for_ssh "k8s-worker-node${i}"
done

# ── Generate Ansible inventory ───────────────────────────────────────────────
echo -e "\n${BLUE}=== Generating ansible/inventory.ini ===${NC}"

INVENTORY_FILE="$(dirname "$0")/ansible/inventory.ini"

{
  echo "[control_plane]"
  echo "${CONTROL_PLANE} ansible_host=orb ansible_user=${ORB_USER}@${CONTROL_PLANE}"
  echo ""
  echo "[workers]"
  for i in $(seq 1 "$NUM_WORKERS"); do
    echo "k8s-worker-node${i} ansible_host=orb ansible_user=${ORB_USER}@k8s-worker-node${i}"
  done
  echo ""
  echo "[k8s_cluster:children]"
  echo "control_plane"
  echo "workers"
} > "$INVENTORY_FILE"

echo -e "  Written to: $INVENTORY_FILE"

# ── Done ─────────────────────────────────────────────────────────────────────
echo -e "\n${GREEN}=== Machines ready ===${NC}"
echo -e "Run the cluster setup:"
echo -e "  ${YELLOW}ansible-playbook -i ansible/inventory.ini ansible/site.yml${NC}"
