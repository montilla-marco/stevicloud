# ADR-003 — Apache Kafka for Cross-BC Domain Event Integration

**Status:** Proposed (scheduled for Episode 02)  
**Date:** 2025-07-04  
**Episode:** 02 — Pricing BC + Kafka  
**Authors:** stevidigital team

---

## Context

Episode 01 established that bounded contexts communicate via Domain Events (not direct calls). `ProductPublished` is the first event that must cross the ProductCatalog → Pricing boundary.

In Episode 01 this is a no-op (LoggingDomainEventPublisher). In Episode 02 it becomes a real distributed messaging problem:
- ProductCatalog publishes `ProductPublished` when a product transitions from DRAFT to PUBLISHED.
- Pricing BC must consume it and set an initial price (`PriceSet` event).
- OrderManagement (OMS, Episode 3) must also consume `ProductPublished` to update its catalog view.
- Events must be replayable (new consumers should be able to bootstrap from past events).

## Options Considered

| Option | Pattern | Replay | Ordering | Cluster-native | Decision |
|--------|---------|--------|----------|----------------|----------|
| REST webhooks | Point-to-point HTTP | ✗ | ✗ | ✗ | Rejected — tight coupling, retry complexity |
| RabbitMQ | Queue/Exchange (AMQP) | Limited (streams) | Per-queue | ✓ | Rejected — not log-based by default, weaker replay |
| Apache Pulsar | Log-based (segments) | ✓ | ✓ | ✓ (operator) | Considered — good, but smaller ecosystem |
| **Apache Kafka (Strimzi)** | Distributed log | ✓ | ✓ per partition | ✓ (Strimzi operator) | **Selected** |

## Decision

**Use Apache Kafka deployed via Strimzi operator on the K8s cluster** as the event bus.

Topic naming convention:
```
stevidigital.product-catalog.events    ← ProductCreated, ProductPublished, ProductDiscontinued
stevidigital.pricing.events            ← PriceSet, PriceUpdated
stevidigital.oms.events                ← OrderPlaced, OrderConfirmed
```

**Partitioning key:** `productId` — ensures all events for a given product land in the same partition (ordering guarantee per aggregate).

**Consumer group convention:** `<bc-name>-consumer` (e.g., `pricing-consumer`).

### Transactional Outbox Pattern (required for Episode 02)

The `LoggingDomainEventPublisher` (Episode 01) has a dual-write risk: if the process crashes between `repository.save()` and `publisher.publish()`, the event is lost.

In Episode 02, we adopt the **Transactional Outbox Pattern** [Kleppmann Ch.11]:

```
1. @Transactional:
   a. repository.save(product)               → writes to products table
   b. outboxRepository.save(event)           → writes to outbox table (same TX)
2. Outbox relay (Debezium CDC or polling):
   a. reads committed outbox rows
   b. publishes to Kafka
   c. marks rows as published
```

This guarantees at-least-once delivery even on process crash. Consumers implement **Idempotent Consumer** [EIP p.549] using the `eventId` UUID.

## Event Schema

Events are serialized as JSON. Schema evolution follows **backward compatibility** rules:
- New fields are always optional with defaults.
- Fields are never removed without a deprecation cycle.
- `eventId` (UUID) and `occurredOn` (ISO-8601) are always present.

```json
{
  "eventType": "ProductPublished",
  "eventId": "uuid-v4",
  "occurredOn": "2025-07-04T12:00:00Z",
  "payload": {
    "productId": "..."
  }
}
```

## Consequences

**Positive:**
- Decoupled BCs: ProductCatalog does not know which services consume its events.
- Replay: new BCs can bootstrap from `productId` offset 0.
- Partitioning by `productId` gives causal ordering for all events on a product.
- Strimzi operator manages Kafka lifecycle declaratively via ArgoCD.

**Negative / Trade-offs:**
- Operational complexity: Kafka broker, Zookeeper/KRaft, consumer group offset management.
- Local development requires Kafka running (or Testcontainers in integration tests).
- Schema evolution discipline required — Jackson polymorphism or Schema Registry.
- Outbox relay adds infrastructure (Debezium or a polling job).

## References

- Kleppmann, M. (2017). *Designing Data-Intensive Applications* — Ch.11: Stream Processing
- Hohpe & Woolf (2003). *Enterprise Integration Patterns* — Idempotent Consumer p.549, Dead Letter Channel p.175
- [Evans] Ch.14 Context Map — Customer/Supplier (Pricing → ProductCatalog)
- [Strimzi](https://strimzi.io/) — Kafka Operator for Kubernetes
