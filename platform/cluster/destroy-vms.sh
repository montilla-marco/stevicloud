#!/usr/bin/env bash
# destroy-vms.sh — Deletes all cluster machines and cleans up local state.
# Equivalent to: vagrant destroy -f

set -euo pipefail

RED="\033[1;31m"
GREEN="\033[1;32m"
NC="\033[0m"

MACHINES=(k8s-control-plane k8s-worker-node1 k8s-worker-node2)

echo -e "${RED}=== Destroying cluster machines ===${NC}"
for name in "${MACHINES[@]}"; do
  if orb info "$name" &>/dev/null 2>&1; then
    echo "  Deleting $name..."
    orb delete "$name" -f
  else
    echo "  $name not found — skipping"
  fi
done

# Clean up local artefacts generated during provisioning
rm -f ansible/inventory.ini /tmp/k8s-join-command.sh kubeconfig

echo -e "\n${GREEN}Done. Run ./provision-vms.sh to start fresh.${NC}"
