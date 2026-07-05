# ADR-004 — pnpm 11 as JavaScript Package Manager

**Status:** Accepted  
**Date:** 2025-07-04  
**Authors:** stevidigital team

---

## Context

The stevidigital Nx monorepo has TypeScript services (oms, billing) and a shared TS library (shared-contracts). We need a JS package manager that:
1. Supports workspace linking (`packages/shared-contracts` → `apps/oms/apps/billing` without publishing).
2. Works cleanly with Nx 23 (Nx itself is a JS package installed at the workspace root).
3. Avoids phantom dependencies (a major source of subtle runtime bugs in monorepos).
4. Is fast.

## Decision

**Use pnpm 11** with `pnpm-workspace.yaml`.

**`pnpm-workspace.yaml`:**
```yaml
packages:
  - 'apps/*'
  - 'packages/*'
allowBuilds:
  nx: true
  unrs-resolver: true
```

**Local package linking** — use `workspace:*` protocol:
```json
"@stevidigital/shared-contracts": "workspace:*"
```
pnpm 11 enforces this; bare `"*"` causes `ERR_PNPM_FETCH_404` because pnpm 11 no longer accepts unqualified version ranges for workspace packages.

**pnpm 11 security policy:** `allowBuilds` in `pnpm-workspace.yaml` is required for any package that runs `postinstall` scripts. Nx and its native resolver (`unrs-resolver`) both need this. pnpm 11 blocks build scripts by default unless explicitly listed.

## Alternatives Considered

| Manager | Workspace support | Phantom deps | pnpm speed vs | Decision |
|---------|-------------------|--------------|---------------|----------|
| **npm** | ✓ (workspaces) | Hoisting leaks | Slowest | Rejected |
| **yarn berry** | ✓ (workspaces) | Plug'n'Play (stricter) | Comparable | Considered — PnP is more complex to debug |
| **pnpm 11** | ✓ (`workspace:*`) | Strict by design | Fastest (hardlinks) | **Selected** |

## Key pnpm 11 Behaviors to Know

- **Content-addressable store:** packages are stored once at `~/.pnpm-store`, hardlinked into `node_modules`. Disk use is minimal even with many projects.
- **Strict hoisting:** each package only sees its declared dependencies. Unlike npm/yarn, transitive imports not in `package.json` fail at import time.
- **`allowBuilds`:** must list packages that run `postinstall` build steps. Check `pnpm install` warnings for new packages that need adding.
- **Lockfile:** `pnpm-lock.yaml` at workspace root. Commit it. Do not mix with `package-lock.json`.

## Consequences

**Positive:**
- No phantom dependencies — if it's not in `package.json`, it can't be imported (catches real bugs).
- `workspace:*` makes local package versions explicit and traceable.
- Faster CI due to hardlink store caching.
- `pnpm -r run build` runs `build` across all workspace packages respecting topology.

**Negative / Trade-offs:**
- Some npm scripts assume hoisted `node_modules` layout; may need `--shamefully-hoist` for edge cases.
- pnpm 11 breaking change on `allowBuilds` requires attention when adding new packages with postinstall.
- Developers used to npm need to learn `pnpm add`, `pnpm -r`, `pnpm --filter`.

## Common pnpm Commands in This Repo

```bash
pnpm install                        # install all workspace packages
pnpm --filter @stevidigital/oms add express   # add dep to one package
pnpm -r run build                   # build all packages (topology order)
pnpm --filter @stevidigital/oms run dev       # dev one service
```
