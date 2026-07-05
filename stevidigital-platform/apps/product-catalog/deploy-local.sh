#!/usr/bin/env bash
# deploy-local.sh — build, push to Zot, bump manifest tag, push to git
# Usage: ./deploy-local.sh <version>
# Example: ./deploy-local.sh 0.1.0
set -euo pipefail

VERSION=${1:?usage: ./deploy-local.sh <version>}
BUILD_NODE="k8s-worker-node2"
ZOT="192.168.139.239:30500"
IMAGE="${ZOT}/product-catalog:${VERSION}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
MANIFEST="${SCRIPT_DIR}/k8s-manifests/01-deployment.yaml"

echo "▶ Building JAR..."
cd "${REPO_ROOT}"
./gradlew :product-catalog:bootJar -q

JAR=$(find "${SCRIPT_DIR}/build/libs" -name "*.jar" ! -name "*-plain.jar" | head -1)
[[ -z "$JAR" ]] && { echo "ERROR: JAR not found"; exit 1; }
echo "  JAR: ${JAR}"

echo "▶ Copying build context to ${BUILD_NODE}..."
ssh -o StrictHostKeyChecking=no ${BUILD_NODE}@orb "mkdir -p /tmp/ctx-product-catalog"
scp -o StrictHostKeyChecking=no "${JAR}" ${BUILD_NODE}@orb:/tmp/ctx-product-catalog/app.jar
scp -o StrictHostKeyChecking=no "${SCRIPT_DIR}/Dockerfile" ${BUILD_NODE}@orb:/tmp/ctx-product-catalog/Dockerfile

echo "▶ Building and pushing image ${IMAGE}..."
ssh -o StrictHostKeyChecking=no ${BUILD_NODE}@orb "
  docker build -t ${IMAGE} /tmp/ctx-product-catalog/ &&
  docker push ${IMAGE} &&
  rm -rf /tmp/ctx-product-catalog
"

echo "▶ Bumping image tag in manifest..."
sed -i '' "s|image: ${ZOT}/product-catalog:.*|image: ${IMAGE}|" "${MANIFEST}"

echo "▶ Committing and pushing to git..."
cd "${REPO_ROOT}"
git add "${MANIFEST}"
git commit -m "chore: bump product-catalog to ${VERSION}"
git push

echo ""
echo "✓ Done. ArgoCD will sync the new image shortly."
echo "  Image : ${IMAGE}"
echo "  API   : http://$(ssh -o StrictHostKeyChecking=no k8s-worker-node1@orb hostname -I | awk '{print $1}' 2>/dev/null || echo '<worker-node1-ip>'):30080"
