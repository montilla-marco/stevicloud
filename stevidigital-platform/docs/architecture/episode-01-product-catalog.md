# Episode 01 — ProductCatalog Bounded Context

> "A thin, vertical slice that is production-quality from day one."
> The goal is not to build something complete — it is to build something **correct**
> and then grow it iteratively. [Ford-BEA] Ch.3.
>
> *In this series each Episode is one vertical slice: a bounded context fully implemented
> from domain model to REST adapter, with observability wired from line one.*

---

## What we built

A Spring Boot 4 microservice implementing the **ProductCatalog Bounded Context**
in an e-commerce/banking platform. It exposes a REST API to create, query, and
publish products, with full observability wired from the first line of code.

**Architecture style:** Hexagonal Architecture (Ports & Adapters)
**Domain paradigm:** Domain-Driven Design (DDD) — Aggregates, Value Objects, Domain Events
**Stage:** 1 — in-memory persistence, log-based event publishing

---

## Bounded Context Map

```
┌─────────────────────────────────────────────────────────────────┐
│                      stevidigital platform                       │
│                                                                  │
│  ┌──────────────────┐    ProductPublished    ┌───────────────┐  │
│  │  ProductCatalog  │ ──────────────────────▶│    Pricing    │  │
│  │  (this service)  │                         │  (Episode 2)  │  │
│  └──────────────────┘    ProductPublished    └───────────────┘  │
│           │          ──────────────────────▶ ┌───────────────┐  │
│           │                                  │   Inventory   │  │
│           │                                  │  (Episode 3)  │  │
│           │          ProductPublished         └───────────────┘  │
│           └──────────────────────────────── ▶ ┌───────────────┐  │
│                                              │    Search     │  │
│                                              │   (future)    │  │
│                                              └───────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

**Context Map patterns used** [Evans] Ch.14:
- **Published Language**: Domain Events (ProductCreated, ProductPublished) are the
  integration contract. Each BC consumes them independently — no shared domain model.
- **Customer/Supplier**: ProductCatalog is the Supplier; Pricing, Inventory, Search
  are Customers. The Supplier defines the event schema; Customers adapt.
- **Anti-Corruption Layer (ACL)**: Each BC translates the Published Language into its
  own internal model. Pricing's `ProductId` is its own type, not ProductCatalog's.

---

## Hexagonal Architecture

```
                         ┌─────────────────────────────┐
                         │         HTTP Client          │
                         └──────────────┬──────────────┘
                                        │ REST
                         ┌──────────────▼──────────────┐
                         │       ProductController      │  ← Adapter (Driving)
                         │      infrastructure/web      │
                         └──────────────┬──────────────┘
                                        │ Commands/Queries
              ┌─────────────────────────▼───────────────────────────┐
              │                 APPLICATION LAYER                    │
              │   CreateProductUseCase   PublishProductUseCase       │
              │   GetProductUseCase      DomainEventPublisher (port) │
              └───────────┬─────────────────────────────┬───────────┘
                          │                             │
              ┌───────────▼───────────┐   ┌────────────▼────────────┐
              │      DOMAIN LAYER     │   │   ProductRepository      │
              │                       │   │   (port, interface only) │
              │  Product (Aggregate)  │   └────────────┬────────────┘
              │  ProductId, Name,     │                │
              │  Category, Status     │   ┌────────────▼────────────┐
              │  DomainEvent types    │   │ InMemoryProductRepository│  ← Adapter
              └───────────────────────┘   │  infrastructure/persist │    (Driven)
                                          └─────────────────────────┘
                         ┌─────────────────────────────┐
                         │ LoggingDomainEventPublisher  │  ← Adapter (Driven)
                         │  infrastructure/messaging   │
                         │  → Kafka in Episode 2       │
                         └─────────────────────────────┘
```

**Key rule:** The domain layer imports NOTHING from Spring, JPA, or any framework.
Test the domain with plain JUnit — no Spring context, no mocks needed.

---

## Domain Model

### Value Objects

Value Objects have no identity — equality is by value, not by reference.
[Evans] Ch.5 p.97: "When you care only about the attributes, classify it as a VALUE OBJECT. Make it immutable."

| Class | Invariants enforced |
|-------|---------------------|
| `ProductId` | Non-null, non-blank UUID string |
| `ProductName` | Non-null, non-blank, max 200 chars, trimmed |
| `Category` | Finite enum (ELECTRONICS, CLOTHING, BOOKS, HOME, SPORTS, FOOD, OTHER) |
| `ProductStatus` | Finite enum (DRAFT, PUBLISHED, DISCONTINUED) |

Why not `String`? A `String` accepts `null`, `""`, `"x".repeat(10000)`.
`ProductName` makes invalid states **unrepresentable** at the type level.
[Evans] Ch.9 — Making Implicit Concepts Explicit.

### Aggregate Root: Product

[Evans] Ch.6 p.125: "An AGGREGATE is a cluster of associated objects that we treat
as a unit for the purpose of data changes. [...] The root is ultimately responsible
for checking invariants."

**Lifecycle transitions** (enforced by the aggregate, not a service):

```
         Product.create()
              │
              ▼
           [DRAFT]
              │
         publish()  ← only from DRAFT, throws otherwise
              │
              ▼
         [PUBLISHED]
              │
        discontinue()  ← only from PUBLISHED, throws otherwise
              │
              ▼
       [DISCONTINUED]
```

**Domain Events registered** (drained by application layer after save):
- `ProductCreated` — on `Product.create()`
- `ProductPublished` — on `product.publish()`
- `ProductDiscontinued` — on `product.discontinue()`

**Reconstitution vs Creation**: Two factory methods exist:
- `Product.create()` — new product, fires events, starts at DRAFT
- `Product.reconstitute()` — loaded from DB, no events (they already happened)

---

## Domain Events

[Evans] Ch.8: "Something happened that domain experts care about."

Named in **past tense** (ProductCreated, not CreateProduct) — they are facts,
not intents. Immutable records. Carry full state so consumers don't query back.

[Kleppmann] Ch.11 p.462: "Self-contained events include all data needed for
downstream processing. This avoids the 'dual-write problem' where the event
is stale by the time the consumer acts on it."

```
DomainEvent (interface)
├── eventId(): String        — UUID, for idempotency in consumers [EIP] Idempotent Consumer
└── occurredOn(): Instant    — when the fact happened (not when it was sent)

ProductCreated implements DomainEvent
├── productId: ProductId
├── name: ProductName
└── category: Category

ProductPublished implements DomainEvent
└── productId: ProductId

ProductDiscontinued implements DomainEvent
└── productId: ProductId
```

**Dispatch order in application layer:**

```
1. product = Product.create(...)       ← domain: fire invariant, register event
2. repository.save(product)            ← infrastructure: persist to DB/memory
3. events = product.pullDomainEvents() ← drain buffer (idempotent)
4. events.forEach(publisher::publish)  ← dispatch (log now, Kafka in Episode 2)
```

Events are dispatched AFTER save. If save fails, no events escape.
In Episode 2 we'll use the **Transactional Outbox Pattern** [Kleppmann] Ch.11
to guarantee exactly-once delivery even if the process crashes between step 2 and 4.

---

## Observability

Every request generates:

| Signal | What | Where |
|--------|------|-------|
| **Traces** | HTTP request span + child spans per use case | Tempo via OTel Collector |
| **Metrics** | `http_server_requests_seconds`, JVM, custom | Prometheus → Grafana |
| **Logs** | Structured JSON (key=value) | Loki via container stdout |

OTel auto-instrumentation (Spring Boot starter) adds trace context to every log line
(`trace_id`, `span_id`). In Grafana: open a trace → click "Logs for this span" →
jumps to Loki with the same trace ID. [Majors] Ch.7: "Traces as the organizing
structure for all other telemetry."

**Spring Boot application.yaml OTel config:**
```yaml
otel:
  exporter:
    otlp:
      endpoint: http://otel-collector.monitoring.svc.cluster.local:4318
      protocol: http/protobuf
  resource:
    attributes:
      service.name: product-catalog
```

**Golden Signals for this service** [Majors] Ch.2:

```promql
# Request Rate
rate(http_server_requests_seconds_count{application="product-catalog"}[5m])

# Error Rate (4xx/5xx)
rate(http_server_requests_seconds_count{application="product-catalog",status=~"4..|5.."}[5m])

# P99 Latency
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{application="product-catalog"}[5m]))

# Domain Events published per minute (leading indicator of business activity)
rate(logback_events_total{application="product-catalog",level="info"}[1m])
```

---

## API Contract

```
POST   /api/v1/products
  Body:  { "name": "Wireless Keyboard", "category": "ELECTRONICS" }
  201 Created  + Location: /api/v1/products/{id}
  400 Bad Request  if name blank or category unknown
  422 Unprocessable if invariant violated

GET    /api/v1/products/{id}
  200 OK  { "id": "...", "name": "...", "category": "...", "status": "DRAFT" }
  404 Not Found

GET    /api/v1/products
  200 OK  [ ... ]

PUT    /api/v1/products/{id}/publish
  204 No Content
  422 Unprocessable if product not in DRAFT (invariant)
```

---

## Package Structure

```
product-catalog/
└── src/
    ├── main/java/org/stevidigital/productcatalog/
    │   ├── ProductCatalogApplication.java       ← Spring Boot entry point
    │   ├── domain/
    │   │   ├── event/
    │   │   │   ├── DomainEvent.java             ← interface (marker + eventId/occurredOn)
    │   │   │   ├── ProductCreated.java
    │   │   │   ├── ProductPublished.java
    │   │   │   └── ProductDiscontinued.java
    │   │   ├── model/
    │   │   │   ├── Product.java                 ← AGGREGATE ROOT
    │   │   │   ├── ProductId.java               ← Value Object
    │   │   │   ├── ProductName.java             ← Value Object
    │   │   │   ├── Category.java                ← Value Object (enum)
    │   │   │   └── ProductStatus.java           ← Value Object (enum)
    │   │   └── repository/
    │   │       └── ProductRepository.java       ← Port (interface)
    │   ├── application/
    │   │   ├── command/
    │   │   │   ├── CreateProductCommand.java
    │   │   │   └── PublishProductCommand.java
    │   │   └── usecase/
    │   │       ├── CreateProductUseCase.java
    │   │       ├── PublishProductUseCase.java
    │   │       ├── GetProductUseCase.java
    │   │       └── DomainEventPublisher.java    ← Port (interface)
    │   └── infrastructure/
    │       ├── config/
    │       │   └── ApplicationConfig.java       ← Spring @Bean wiring
    │       ├── messaging/
    │       │   └── LoggingDomainEventPublisher.java  ← Adapter (Stage 1)
    │       ├── persistence/
    │       │   └── InMemoryProductRepository.java   ← Adapter (Stage 1)
    │       └── web/
    │           ├── ProductController.java
    │           ├── CreateProductRequest.java
    │           └── ProductResponse.java
    └── test/java/org/stevidigital/productcatalog/
        ├── domain/model/
        │   └── ProductTest.java                 ← Aggregate invariant tests
        └── application/usecase/
            └── CreateProductUseCaseTest.java    ← Use case orchestration tests
```

---

## What comes next — Episode 02: Pricing BC + Kafka

1. Deploy Kafka to the cluster (Strimzi operator)
2. Create `pricing` subproject in `stevidigital-java-services`
3. Replace `LoggingDomainEventPublisher` with `KafkaDomainEventPublisher`
4. Pricing BC subscribes to `product.events` topic, listens for `ProductPublished`
5. Creates initial price (Value Object: `Money`) — triggers `PriceSet` event
6. EIP patterns introduced: **Idempotent Consumer** (eventId deduplication),
   **Dead Letter Queue** (malformed events)

**Bibliography references for Episode 02:**
- [Kleppmann] Ch.11: "Stream Processing" — exactly-once, consumer groups, offsets
- [EIP] Hohpe & Woolf: Idempotent Consumer p.549, Dead Letter Channel p.175
- [Evans] Ch.14 Context Map: Customer/Supplier relationship Pricing→ProductCatalog

---

## Bibliography

| Ref | Full title |
|-----|-----------|
| [Evans] | Eric Evans — *Domain-Driven Design: Tackling Complexity in the Heart of Software* (2003) |
| [Kleppmann] | Martin Kleppmann — *Designing Data-Intensive Applications* (2017) |
| [EIP] | Gregor Hohpe & Bobby Woolf — *Enterprise Integration Patterns* (2003) |
| [Majors] | Charity Majors, Liz Fong-Jones, George Miranda — *Observability Engineering* (O'Reilly, 2022) |
| [Ford-FSA] | Neal Ford, Mark Richards et al. — *Fundamentals of Software Architecture* (2020) |
| [Ford-BEA] | Neal Ford, Rebecca Parsons, Patrick Kua — *Building Evolutionary Architectures* (2nd ed, 2022) |
