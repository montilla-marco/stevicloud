# ADR-001 — Hexagonal Architecture (Ports & Adapters)

**Status:** Accepted  
**Date:** 2025-07-04  
**Episode:** 01 — ProductCatalog BC  
**Authors:** stevidigital team

---

## Context

Building the first bounded context of the stevidigital platform (ProductCatalog). The service will:
- Start with in-memory persistence (Episode 1) and migrate to PostgreSQL (Episode 2).
- Start with log-based event publishing and migrate to Kafka (Episode 2).
- Be tested at the domain level with zero framework overhead.
- Be reviewed and defended in a technical interview context — structure must be explainable.

The team considered three architecture styles for the service internals:

| Style | Description |
|-------|-------------|
| **Layered (Classic)** | web → service → repository. Simple, but domain leaks framework annotations. |
| **Clean Architecture** | Entity / Use Case / Interface Adapters / Frameworks. More ceremony, two abstraction rings. |
| **Hexagonal (Ports & Adapters)** | Domain + Application at the core; adapters at the edges. Explicit port interfaces. |

## Decision

**Use Hexagonal Architecture** as described by Alistair Cockburn (2005).

Package structure per bounded context:

```
org.stevidigital.<bc>/
  domain/
    model/      ← Aggregates, Value Objects, Enums  (zero framework imports)
    event/      ← Domain Events (marker interface only)
    repository/ ← Port: ProductRepository (interface)
  application/
    usecase/    ← Use Cases, DomainEventPublisher port (interface)
    command/    ← Input DTOs for use cases
  infrastructure/
    web/        ← Driving adapter: Spring MVC Controller
    persistence/← Driven adapter: InMemoryProductRepository → PostgresProductRepository
    messaging/  ← Driven adapter: LoggingDomainEventPublisher → KafkaDomainEventPublisher
    config/     ← Spring @Configuration wiring ports to adapters
```

**Key rule enforced:** `domain/` may not import anything outside its own package. No Spring, no JPA, no Jakarta. This is verified by the team convention and can be enforced with ArchUnit in Episode 3.

## Consequences

**Positive:**
- Domain layer is unit-testable with plain JUnit — no Spring context, no mocks, sub-second tests.
- Adapters are swappable without touching domain or application logic (InMemory → Postgres in one file).
- The port interfaces make dependencies explicit and visible in code review.
- Directly maps to the C4 L3 Component diagram, making the architecture easy to explain and defend.
- `Product.reconstitute()` vs `Product.create()` distinction emerges naturally from the pattern.

**Negative / Trade-offs:**
- More initial file count vs. a flat Layered approach.
- Developers unfamiliar with the pattern need onboarding (mitigated by the Episode docs).
- Risk of "hexagonal in name only" — must enforce the no-framework-in-domain rule consistently.

## References

- Cockburn, A. (2005). *Hexagonal Architecture*. alistair.cockburn.us/hexagonal-architecture/
- Evans, E. (2003). *Domain-Driven Design* — Ch.5 (Value Objects), Ch.6 (Aggregates)
- [Episode 01 Architecture Doc](../architecture/episode-01-product-catalog.md)
