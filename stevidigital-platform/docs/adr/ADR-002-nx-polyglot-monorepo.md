# ADR-002 — Nx 23 Polyglot Monorepo

**Status:** Accepted  
**Date:** 2025-07-04  
**Authors:** stevidigital team

---

## Context

The stevidigital platform has two language ecosystems in a single platform:
- **Java / Gradle** — Spring Boot services (product-catalog, payments, etc.)
- **TypeScript / Node** — NestJS services (oms, billing) and shared libraries (shared-contracts)

We need a single build orchestration layer that:
1. Understands project dependencies (if shared-contracts changes, rebuild oms and billing).
2. Runs only the affected subset on CI/pre-push (avoid rebuilding product-catalog for a TS-only change).
3. Provides a visual dependency graph (`nx graph`) suitable for architecture reviews.
4. Works with Gradle multi-project builds without replacing Gradle.

## Options Considered

| Tool | Language support | Affected analysis | Gradle support | Decision |
|------|-----------------|-------------------|----------------|----------|
| **Turborepo** | JS/TS only | ✓ (JS only) | ✗ | Rejected — Java blind |
| **Gradle multi-project** | Java only | Via Gradle tasks | ✓ | Rejected — TS blind |
| **Bazel** | Polyglot | ✓ | Via rules_jvm | Rejected — extreme complexity, overkill for 5 services |
| **Nx 23** | Polyglot (project.json) | ✓ | Via explicit targets | **Selected** |

## Decision

**Use Nx 23 as the polyglot build orchestration layer**, with pnpm 11 as the JS package manager.

### Key configuration choices

**No `@nx/gradle` plugin**: The Nx Gradle plugin auto-discovers Gradle projects by running `projectReportAll` / `projectReport` during startup. On OrbStack (Apple Silicon VM-based cluster), this consistently times out during daemon warm-up. We use explicit `project.json` targets instead:

```json
{
  "targets": {
    "build": {
      "executor": "nx:run-commands",
      "inputs": ["javaFiles"],
      "outputs": ["{projectRoot}/build/libs"],
      "options": { "command": "../../gradlew :product-catalog:bootJar", "cwd": "{workspaceRoot}" }
    }
  }
}
```

**`namedInputs` by language** in `nx.json`:
```json
{
  "namedInputs": {
    "javaFiles": ["{projectRoot}/src/**/*.java", "{projectRoot}/build.gradle.kts", ...],
    "typeScriptFiles": ["{projectRoot}/src/**/*.ts", "{projectRoot}/tsconfig.json", ...]
  }
}
```

This ensures Java changes don't invalidate TS caches and vice versa.

**Tags convention:**
- `lang:java`, `lang:typescript`
- `type:app`, `type:lib`
- `bc:product-catalog`, `bc:oms`, `bc:billing`
- `scope:*` — for lint/boundary enforcement (future)

## Consequences

**Positive:**
- `nx affected --target=test` runs only tests for projects touched by a given commit.
- `nx graph` renders a live dependency diagram — excellent for architecture reviews and interviews.
- Each language keeps its own toolchain (Gradle stays authoritative for Java, pnpm/TS for Node).
- Tags enable `nx run-many --target=build --projects=tag:lang:java` for language-specific CI steps.

**Negative / Trade-offs:**
- Two build systems (Gradle + Nx) — developers must understand both.
- `nx:run-commands` targets are more verbose than `@nx/gradle` auto-discovery would be (when it works).
- pnpm workspace adds a second dependency lockfile (`pnpm-lock.yaml`) alongside Gradle lockfiles.
- Nx cache is local by default; distributed Nx Cloud cache requires an additional setup step.

## Nx → Gradle Bridge Pattern

```
nx build product-catalog
    └─→ nx:run-commands: cd <root> && ./gradlew :product-catalog:bootJar
            └─→ Gradle resolves dependencies, runs annotation processors, builds JAR
```

Gradle remains the source of truth for Java compilation, dependency resolution, and test reporting. Nx provides the graph, cache, and affected analysis layer.

## References

- [Nx docs — project.json](https://nx.dev/reference/project-configuration)
- [Nx docs — namedInputs](https://nx.dev/recipes/running-tasks/configure-inputs-for-task-caching)
- [README — Build System section](../../README.md#build-system)
