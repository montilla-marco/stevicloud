package org.stevidigital.productcatalog.domain.event;

import org.stevidigital.productcatalog.domain.model.Category;
import org.stevidigital.productcatalog.domain.model.ProductId;
import org.stevidigital.productcatalog.domain.model.ProductName;

import java.time.Instant;

/**
 * Domain Event — a Product was created in DRAFT status.
 *
 * [Evans] Ch.8: "Something happened that domain experts care about."
 * Named in past tense (ProductCreated, NOT CreateProduct) because events are facts:
 * they describe what already happened, not an intent.
 *
 * Carries the full initial state so downstream consumers (e.g. Search BC indexing
 * a new product) don't need to query back. [Kleppmann] Ch.11 p.462:
 * "Self-contained events [...] include all data needed for downstream processing."
 *
 * [EIP] Hohpe & Woolf: Event Message pattern — the event itself IS the message payload.
 * When we publish to Kafka in Episode 2, this record will serialize directly.
 */
public record ProductCreated(
        String eventId,
        Instant occurredOn,
        ProductId productId,
        ProductName name,
        Category category
) implements DomainEvent {

    public static ProductCreated of(ProductId productId, ProductName name, Category category) {
        return new ProductCreated(DomainEvent.newEventId(), Instant.now(), productId, name, category);
    }
}
