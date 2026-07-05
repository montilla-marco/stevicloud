package org.stevidigital.productcatalog.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker interface for Domain Events in the ProductCatalog bounded context.
 *
 * [Evans] Ch.8: "Model information about activity in the domain as a series of
 * discrete events. Represent each event as a domain object."
 *
 * Key properties of a Domain Event:
 * - Immutable: it describes something that already happened (past tense name)
 * - Named in Ubiquitous Language: ProductCreated, not EntitySaved
 * - Carries enough data for consumers to act without querying back
 *
 * [Kleppmann] Ch.11 p.457: "The immutability of events is what makes them
 * safe to use as the basis for derived data."
 */
public interface DomainEvent {
    String eventId();
    Instant occurredOn();

    static String newEventId() {
        return UUID.randomUUID().toString();
    }
}
