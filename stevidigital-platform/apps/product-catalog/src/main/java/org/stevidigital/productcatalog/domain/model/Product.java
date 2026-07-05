package org.stevidigital.productcatalog.domain.model;

import org.stevidigital.productcatalog.domain.event.DomainEvent;
import org.stevidigital.productcatalog.domain.event.ProductCreated;
import org.stevidigital.productcatalog.domain.event.ProductDiscontinued;
import org.stevidigital.productcatalog.domain.event.ProductPublished;

import java.util.ArrayList;
import java.util.List;

/**
 * AGGREGATE ROOT — the central object of the ProductCatalog bounded context.
 *
 * [Evans] Ch.6 p.125: "An AGGREGATE is a cluster of associated objects that we treat
 * as a unit for the purpose of data changes. Each AGGREGATE has a root and a boundary.
 * The root is a single, specific ENTITY contained in the AGGREGATE."
 *
 * Rules enforced here (invariants):
 * 1. A Product is always born in DRAFT status.
 * 2. Only DRAFT products can be published.
 * 3. Only PUBLISHED products can be discontinued.
 * 4. No direct field mutation from outside — all changes go through methods.
 *
 * [Evans] Ch.6 p.128: "The root ENTITY has global identity and is ultimately
 * responsible for checking invariants."
 *
 * Domain Events are collected internally and drained by the application layer
 * AFTER the transaction commits, so events are never emitted for a failed save.
 * [Kleppmann] Ch.11: "Transactional outbox pattern" — in Episode 2 we'll persist
 * events to an outbox table in the same DB transaction.
 */
public class Product {

    private final ProductId id;
    private ProductName name;
    private Category category;
    private ProductStatus status;

    // Internal event buffer — never exposed directly
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // Private constructor — creation goes through factory methods only.
    // [Evans] Ch.6: "FACTORIES encapsulate the knowledge needed to create complex objects."
    private Product(ProductId id, ProductName name, Category category, ProductStatus status) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.status = status;
    }

    /**
     * Factory method — the ONLY way to bring a new Product into existence.
     * Ensures the DRAFT invariant and registers the ProductCreated event atomically.
     */
    public static Product create(ProductId id, ProductName name, Category category) {
        var product = new Product(id, name, category, ProductStatus.DRAFT);
        product.registerEvent(ProductCreated.of(id, name, category));
        return product;
    }

    /**
     * Reconstitutes a Product from persistence without firing events.
     * Used by the repository when loading — the events already happened in the past.
     *
     * [Evans] Ch.6: Repositories reconstruct aggregates from stored data.
     * The factory that created the aggregate is different from the factory
     * that reconstitutes it from storage.
     */
    public static Product reconstitute(ProductId id, ProductName name, Category category, ProductStatus status) {
        return new Product(id, name, category, status);
    }

    /**
     * Lifecycle transition: DRAFT → PUBLISHED.
     * A published product is visible to customers and can be priced (Pricing BC).
     *
     * [Evans] Ch.6: Invariant enforcement is the aggregate's responsibility.
     * This rule — "you can only publish a draft" — lives HERE, not in a service.
     */
    public void publish() {
        if (this.status != ProductStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT products can be published. Current status: " + this.status);
        }
        this.status = ProductStatus.PUBLISHED;
        registerEvent(ProductPublished.of(this.id));
    }

    /**
     * Lifecycle transition: PUBLISHED → DISCONTINUED.
     * Discontinued products remain in the catalog for historical queries but
     * are no longer orderable.
     */
    public void discontinue() {
        if (this.status != ProductStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "Only PUBLISHED products can be discontinued. Current status: " + this.status);
        }
        this.status = ProductStatus.DISCONTINUED;
        registerEvent(ProductDiscontinued.of(this.id));
    }

    /**
     * Drains the internal event buffer. Called by the application layer after
     * a successful repository.save() to dispatch events to the message broker.
     *
     * Returns an immutable copy and clears the buffer — idempotent drain pattern.
     * Prevents double-dispatch if called twice (e.g., during retry logic).
     */
    public List<DomainEvent> pullDomainEvents() {
        var snapshot = List.copyOf(domainEvents);
        domainEvents.clear();
        return snapshot;
    }

    // ── Queries (no mutation, no side effects) ────────────────────────────────

    public ProductId id()       { return id; }
    public ProductName name()   { return name; }
    public Category category()  { return category; }
    public ProductStatus status() { return status; }
    public boolean isDraft()         { return status == ProductStatus.DRAFT; }
    public boolean isPublished()     { return status == ProductStatus.PUBLISHED; }
    public boolean isDiscontinued()  { return status == ProductStatus.DISCONTINUED; }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }
}
