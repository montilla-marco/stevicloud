package org.stevidigital.productcatalog.domain.event;

import org.stevidigital.productcatalog.domain.model.ProductId;

import java.time.Instant;

/**
 * Domain Event — a PUBLISHED Product was discontinued and is no longer available.
 *
 * Downstream reactions:
 * - Pricing BC may archive price history
 * - Inventory BC may prevent new stock reservations
 * - Search BC removes or marks the product as unavailable
 *
 * [Evans] Ch.8: Events capture history. Discontinuing a product is not a deletion —
 * it's a business event that must be auditable ("who discontinued it and when").
 */
public record ProductDiscontinued(
        String eventId,
        Instant occurredOn,
        ProductId productId
) implements DomainEvent {

    public static ProductDiscontinued of(ProductId productId) {
        return new ProductDiscontinued(DomainEvent.newEventId(), Instant.now(), productId);
    }
}
