package org.stevidigital.productcatalog.domain.event;

import org.stevidigital.productcatalog.domain.model.ProductId;

import java.time.Instant;

/**
 * Domain Event — a Product transitioned from DRAFT to PUBLISHED.
 *
 * [Evans] Ch.8: "An event is a full-fledged part of the domain model,
 * a representation of something that happened in the domain."
 *
 * This event is the integration contract between ProductCatalog and downstream BCs:
 * - Pricing BC listens to set an initial price      (Episode 2)
 * - Inventory BC listens to allocate stock          (Episode 3)
 * - Search BC listens to index the product          (future)
 *
 * [Ford-BEA] Ch.3: "Choreography vs Orchestration" — each BC reacts independently.
 * No ProductCatalog code knows about Pricing. The event decouples them.
 */
public record ProductPublished(
        String eventId,
        Instant occurredOn,
        ProductId productId
) implements DomainEvent {

    public static ProductPublished of(ProductId productId) {
        return new ProductPublished(DomainEvent.newEventId(), Instant.now(), productId);
    }
}
