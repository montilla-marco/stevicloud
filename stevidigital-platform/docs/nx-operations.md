# Nx Monorepo — Operations Guide

> stevidigital-platform · Nx 23 · Java 25 / Gradle 9.6.1 · Node 22 / pnpm 11

---

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java (Temurin) | 25 LTS | `sdk install java 25-tem` (SDKMAN) |
| Gradle | 9.6.1 | Wrapper in repo — `./gradlew` |
| Node.js | 22 LTS | `nvm install 22 && nvm use 22` |
| pnpm | 11.9.0 | `npm install -g pnpm@11.9.0` |
| Nx | 23 (workspace) | installed via `pnpm install` |

**SDKMAN setup:**
```bash
sdk install java 25-tem
sdk use java 25-tem          # sets JAVA_HOME for current shell
echo 'export JAVA_HOME=$(sdk home java current)' >> ~/.zshrc
```

**gradle.properties must point at SDKMAN java:**
```properties
org.gradle.java.home=/Users/<you>/.sdkman/candidates/java/current
```

---

## First-time setup

```bash
cd stevidigital-platform
pnpm install                 # installs Nx + TS deps, creates node_modules
./gradlew dependencies       # warms Gradle cache
nx graph                     # opens browser with dependency graph
```

---

## Daily Commands

### Build

```bash
# Build everything
nx run-many --target=build --all

# Build one project
nx build product-catalog
nx build oms

# Build only what changed vs main
nx affected --target=build --base=main
```

### Test

```bash
# Test everything
nx run-many --target=test --all

# Test one project
nx test product-catalog

# Test only affected projects
nx affected --target=test --base=main --head=HEAD

# Run a specific Gradle test class (bypass Nx)
cd stevidigital-platform
./gradlew :product-catalog:test --tests "*.ProductTest"
```

### Dev (TypeScript services)

```bash
nx dev oms          # NestJS watch mode on :3001
nx dev billing      # NestJS watch mode on :3002
```

### Lint

```bash
nx run-many --target=lint --projects=tag:lang:typescript
```

---

## Dependency Graph

```bash
nx graph                     # open interactive graph in browser
nx graph --focus=oms         # focus on oms and its dependencies
nx show projects             # list all known projects
nx show project product-catalog   # show targets for one project
```

The graph reflects `implicitDependencies` in each `project.json`. E.g., `billing` depends on `shared-contracts` and `oms`.

---

## Nx Cache

Nx caches task outputs based on:
1. Source files matching `inputs` in `project.json` / `nx.json`
2. The task's command hash
3. Environment variables in `nx.json` `implicitDependencies`

```bash
nx reset                     # clear local cache (fixes stale-cache bugs)
nx run product-catalog:build --skip-nx-cache    # force rebuild
```

**Cache storage:** `node_modules/.cache/nx` (local). Not shared by default.

**Tip:** If you get `undefined` errors from `nx show` after dependency changes, run `nx reset` first.

---

## Tags and Filtering

Tags are defined in `project.json` under `"tags"`. Current taxonomy:

| Tag | Meaning | Example |
|-----|---------|---------|
| `lang:java` | Gradle/Java project | `product-catalog` |
| `lang:typescript` | pnpm/TS project | `oms`, `billing`, `shared-contracts` |
| `type:app` | Deployable service | `product-catalog`, `oms` |
| `type:lib` | Shared library | `shared-contracts` |
| `bc:product-catalog` | Bounded context | `product-catalog` |
| `scope:*` | (future) Lint boundaries | — |

```bash
# Build only Java services
nx run-many --target=build --projects=tag:lang:java

# Test only TypeScript
nx run-many --target=test --projects=tag:lang:typescript

# Show only apps
nx run-many --target=build --projects=tag:type:app
```

---

## Adding a New Episode (Project)

### Java service

1. Create directory: `apps/<name>/`
2. Copy `build.gradle.kts` from `apps/product-catalog/`
3. Add to `settings.gradle.kts`:
   ```kotlin
   include("name")
   project(":name").projectDir = file("apps/name")
   ```
4. Create `apps/<name>/project.json`:
   ```json
   {
     "name": "<name>",
     "projectType": "application",
     "sourceRoot": "apps/<name>/src",
     "targets": {
       "build": {
         "executor": "nx:run-commands",
         "cache": true,
         "inputs": ["javaFiles"],
         "outputs": ["{projectRoot}/build/libs"],
         "options": {
           "command": "../../gradlew :<name>:bootJar",
           "cwd": "{workspaceRoot}/apps/<name>"
         },
         "dependsOn": ["^build"]
       },
       "test": {
         "executor": "nx:run-commands",
         "cache": true,
         "inputs": ["javaFiles"],
         "options": {
           "command": "../../gradlew :<name>:test",
           "cwd": "{workspaceRoot}/apps/<name>"
         }
       }
     },
     "tags": ["lang:java", "type:app", "bc:<name>"]
   }
   ```
5. `nx reset && nx show projects` — verify it appears.

### TypeScript service (NestJS)

1. Create directory: `apps/<name>/`
2. Create `package.json`, `tsconfig.json`, `project.json` (copy from `apps/oms/`)
3. Update `pnpm-workspace.yaml` if the dir isn't already covered by `apps/*`.
4. Run `pnpm install` from workspace root.
5. Add `@stevidigital/shared-contracts: "workspace:*"` to `dependencies` if needed.
6. `nx show projects` — verify it appears.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `nx show` returns `undefined` | Stale Nx graph cache | `nx reset` |
| `ERR_PNPM_FETCH_404` for `@stevidigital/*` | Missing `workspace:*` protocol | Change `"*"` → `"workspace:*"` in package.json |
| `ERR_PNPM_IGNORED_BUILDS` | pnpm 11 security policy | Add package to `allowBuilds` in `pnpm-workspace.yaml` |
| Gradle picks up wrong Java version | `org.gradle.java.home` not set | Add to `gradle.properties`, point to SDKMAN current |
| Gradle daemon registry errors | Stale daemon entries (363+) | `find ~/.gradle/daemon -name "registry.bin" -delete` |
| `@nx/gradle` plugin timeout | Plugin runs `projectReportAll` during startup | Remove plugin from `nx.json`, use explicit `project.json` targets |
| OOM on dev machine | VSCode java LSP with `-Xmx2G` | Set `java.jdt.ls.vmargs` to `-Xmx512m` in VSCode settings |

---

## Gradle Directly (bypassing Nx)

When you don't need Nx cache/affected logic:

```bash
# From workspace root (stevidigital-platform/)
./gradlew :product-catalog:bootJar     # build runnable JAR
./gradlew :product-catalog:test        # run tests
./gradlew :product-catalog:test --tests "*.ProductTest" --info
./gradlew dependencies                 # print dependency tree
./gradlew --stop                       # stop all daemons
```

---

## pnpm Directly (bypassing Nx)

```bash
pnpm install                           # install all workspace deps
pnpm --filter @stevidigital/oms run dev        # run oms dev server
pnpm --filter @stevidigital/billing add lodash # add dep to billing only
pnpm -r run build                      # build all packages in topology order
pnpm update                            # update lockfile
```

---

## CI Pipeline Sketch (future)

```yaml
# On PR:
nx affected --target=lint   --base=origin/main
nx affected --target=test   --base=origin/main
nx affected --target=build  --base=origin/main

# On merge to main:
nx run-many --target=build --all
# → push images to Zot OCI registry
# → ArgoCD detects new image tag → deploys to cluster
```
